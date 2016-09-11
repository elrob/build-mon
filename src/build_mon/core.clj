(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [bidi.bidi :as bidi]
            [cheshire.core :as json]
            [build-mon.vso-api :as api]
            [build-mon.vso-release-api :as release-api]
            [build-mon.html :as html])
  (:gen-class))

(def logger {:log-exception (fn [message exception]
                              (prn "=========   ERROR   ==========")
                              (prn message)
                              (prn exception)
                              (prn "=============================="))})

(def states-ordered-worst-first [:failed :in-progress-after-failed :in-progress :succeeded])

(defn- release-not-started? [release] (= (:status release) "notStarted"))
(defn- release-succeeded? [release] (= (:status release) "succeeded"))
(defn- release-in-progress? [release] (nil? (:status release)))

(defn succeeded? [build] (= (:result build) "succeeded"))
(defn in-progress? [build] (nil? (:result build)))

(defn- get-release-state [release-env previous-release-env]
    (cond (release-succeeded? release-env) :succeeded
          (and (release-in-progress? release-env) (release-succeeded? previous-release-env)) :in-progress
          (and (release-in-progress? release-env) (not (release-succeeded? previous-release-env))) :in-progress-after-failed
          (release-not-started? release-env) :not-started
          :default :failed))

(defn get-state [build previous-build]
  (cond (succeeded? build) :succeeded
        (and (in-progress? build) (succeeded? previous-build)) :in-progress
        (and (in-progress? build) (not (succeeded? previous-build))) :in-progress-after-failed
        :default :failed))

(defn get-status-text [build]
  (if (in-progress? build) (:status build) (:result build)))

(defn- generate-release-environments [release previous-release]
  (let [environments (-> release :environments)
        previous-environments (-> previous-release :environments)]
    (map (fn [env]
      (let [prev-env-release (filter (fn [prev-env]
                                (= (:name env) (:name prev-env)))
                                previous-environments)
        release-state (get-release-state env prev-env-release)]
        {:env-name (:name env) :state release-state}))
     environments)))

(defn- generate-release-info [release previous-release]
   {:release-definition-name (-> :releaseDefinition release :name)
    :release-definition-id (:id release)
    :release-number (:name release)
    :release-environments (generate-release-environments release previous-release)})

(defn generate-build-info [build previous-build commit-message]
  (let [state (get-state build previous-build)]
    {:build-definition-name (-> build :definition :name)
     :build-definition-id (-> build :definition :id)
     :build-number (:buildNumber build)
     :commit-message commit-message
     :status-text (get-status-text build)
     :state state}))

(defn retrieve-release-info [vso-release-api release-definition-id]
  (let [{:keys [release previous-release]}
        ((:retrieve-release-info vso-release-api) release-definition-id)]
    (when release
      (generate-release-info release previous-release))))

(defn retrieve-build-info [vso-api build-definition-id]
  (let [{:keys [build previous-build commit-message]}
        ((:retrieve-build-info vso-api) build-definition-id)]
    (when build
      (generate-build-info build previous-build commit-message))))

(defn get-favicon-path [state]
  (str "/favicon_" (name state) ".ico"))

(defn get-project-favicon-path [build-info-maps release-info-maps]
  (let [build-states (remove nil? (map :state build-info-maps))
        release-states (remove nil? (map :state release-info-maps))
        all-states (distinct (concat build-states release-states))
        sorting-map (into {} (map-indexed (fn [idx itm] [itm idx]) states-ordered-worst-first))]
    (get-favicon-path (first (sort-by sorting-map all-states)))))

(defn universal-monitor-for-definition-ids [vso-api vso-release-api request build-definition-ids release-definition-ids]
  (let [build-info-maps (remove nil? (map #(retrieve-build-info vso-api %) build-definition-ids))
        release-info-maps (remove nil? (map #(retrieve-release-info vso-release-api %) release-definition-ids))]
    (when (and (not-empty build-info-maps) (not-empty release-info-maps))
      (let [favicon-path (get-project-favicon-path build-info-maps release-info-maps)]
        {:status 200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (html/generate-universal-monitor-html build-info-maps release-info-maps favicon-path)}))))

(defn universal-monitor [vso-api vso-release-api request]
  (let [release-definitions ((:retrieve-release-definitions vso-release-api))
        release-definition-ids (map :id release-definitions)
        build-definitions ((:retrieve-build-definitions vso-api))
        build-definition-ids (map :id build-definitions)]
    (universal-monitor-for-definition-ids vso-api vso-release-api request build-definition-ids release-definition-ids)))

(def routes ["/" {"" :universal-monitor}])

(defn wrap-routes [handlers]
  (fn [request]
    (when-let [route-method (bidi/match-route routes (:uri request))]
      (when-let [handler (-> route-method :handler handlers)]
        (handler (merge request (select-keys route-method [:route-params])))))))

(defn handlers [vso-api vso-release-api]
  {:universal-monitor (partial universal-monitor vso-api vso-release-api)})

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [
            vso-api (api/vso-api-fns logger
                                    (api/vso-api-get-fn vso-personal-access-token)
                                     vso-account
                                     vso-project)
            vso-release-api (release-api/vso-release-api-fns
                                    logger
                                    (release-api/vso-release-api-get-fn vso-personal-access-token)
                                    vso-account
                                    vso-project)
            wrapped-handler (-> (handlers vso-api vso-release-api)
                                    wrap-routes
                                    (resource/wrap-resource "public")
                                    (params/wrap-params))]
        (println "==============================================================")
        (println "Starting up... please enjoy your project monitor #repsforrufus")
        (println "==============================================================")
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
