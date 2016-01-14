(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [bidi.bidi :as bidi]
            [cheshire.core :as json]
            [build-mon.vso-api :as api]
            [build-mon.html :as html])
  (:gen-class))

(def states-ordered-worst-first [:failed :in-progress-after-failed :in-progress :succeeded])

(def default-refresh-interval 20)
(def minimum-refresh-interval 5)

(defn succeeded? [build] (= (:result build) "succeeded"))

(defn in-progress? [build] (nil? (:result build)))

(defn get-state [build previous-build]
  (cond (succeeded? build) :succeeded
        (and (in-progress? build) (succeeded? previous-build)) :in-progress
        (and (in-progress? build) (not (succeeded? previous-build))) :in-progress-after-failed
        :default :failed))

(defn get-status-text [build]
  (if (in-progress? build) (:status build) (:result build)))

(defn tryparse-refresh [refresh-interval-string]
  (try (let [refresh-interval (Integer. refresh-interval-string)]
         (if (< refresh-interval minimum-refresh-interval)
           minimum-refresh-interval
           refresh-interval))
       (catch Exception e nil)))

(defn refresh-interval [params]
  (let [refresh (get params "refresh")]
    (if refresh
      (tryparse-refresh refresh)
      default-refresh-interval)))

(defn generate-build-info [build previous-build commit-message]
  (let [state (get-state build previous-build)]
    {:build-definition-name (-> build :definition :name)
     :build-definition-id (-> build :definition :id)
     :build-number (:buildNumber build)
     :commit-message commit-message
     :status-text (get-status-text build)
     :state state}))

(defn retrieve-build-info [account project token build-definition-id]
  (let [{:keys [build previous-build commit-message]}
        (api/retrieve-build-info account project token build-definition-id)]
    (when build
      (generate-build-info build previous-build commit-message))))

(defn build-info [account project token request]
  (let [build-definition-id (-> request :route-params :build-definition-id)
        build-info (retrieve-build-info account project token build-definition-id)]
    (when build-info
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string build-info)})))

(defn get-favicon-path [state]
  (str "/favicon_" (name state) ".ico"))

(defn get-favicon-path-for-multiple-build-definitions [build-info-maps]
  (let [current-states (remove nil? (map :state build-info-maps))
        sorting-map (into {} (map-indexed (fn [idx itm] [itm idx]) states-ordered-worst-first))]
    (get-favicon-path (first (sort-by sorting-map current-states)))))

(defn build-monitor-for-build-definition-ids [account project token request build-definition-ids]
  (let [build-info-maps (remove nil? (map #(retrieve-build-info account project token %) build-definition-ids))
        build-definition-ids-with-build-info (remove nil? (map :build-definition-id build-info-maps))
        refresh-interval (refresh-interval (:query-params request))
        refresh-info (when refresh-interval
                       {:refresh-interval refresh-interval
                        :build-definition-ids build-definition-ids-with-build-info})]
    (when (not-empty build-info-maps)
      (let [favicon-path (get-favicon-path-for-multiple-build-definitions build-info-maps)]
        {:status 200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (html/generate-build-monitor-html build-info-maps refresh-info favicon-path)}))))

(defn build-definition-monitor [account project token request]
  (let [build-definition-id (-> request :route-params :build-definition-id Integer.)]
    (build-monitor-for-build-definition-ids account project token request [build-definition-id])))

(defn build-monitor [account project token request]
  (let [build-definitions (api/retrieve-build-definitions account project token)
        build-definition-ids (map :id build-definitions)]
    (build-monitor-for-build-definition-ids account project token request build-definition-ids)))

(def routes ["/" {"" :build-monitor
                  ["build-definitions/" [#"\d+" :build-definition-id]] :build-definition-monitor
                  ["ajax/build-definitions/" [#"\d+" :build-definition-id]] :build-info}])

(defn wrap-routes [handlers]
  (fn [request]
    (when-let [route-m (bidi/match-route routes (:uri request))]
      (when-let [handler (-> route-m :handler handlers)]
        (handler (merge request (select-keys route-m [:route-params])))))))

(defn handlers [account project token]
  {:build-monitor
   (partial build-monitor account project token)
   :build-definition-monitor
   (partial build-definition-monitor account project token)
   :build-info
   (partial build-info account project token)})

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [wrapped-handler (-> (handlers vso-account vso-project vso-personal-access-token)
                                wrap-routes
                                (resource/wrap-resource "public")
                                (params/wrap-params))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
