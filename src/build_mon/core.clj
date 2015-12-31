(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [bidi.bidi :as bidi]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [hiccup.core :as hiccup]
            [clojure.string :as s])
  (:gen-class))

(def states #{:succeeded :failed :in-progress :in-progress-after-failed})

(def default-refresh-interval 20)
(def minimum-refresh-interval 5)

(def refresh-icon [:div.refresh-icon.hidden [:i.fa.fa-refresh.fa-spin.fa-3x]])

(def error-modal [:div.error-modal.hidden
                  [:div.error-modal-background]
                  [:h1.error-modal-text "Build Monitor Unreachable"]])

(defn succeeded? [build] (= (:result build) "succeeded"))

(defn in-progress? [build] (nil? (:result build)))

(defn get-state [build previous-build]
  (cond (succeeded? build) :succeeded
        (and (in-progress? build) (succeeded? previous-build)) :in-progress
        (and (in-progress? build) (not (succeeded? previous-build))) :in-progress-after-failed
        :default :failed))

(defn get-favicon-path [state]
  (str "/favicon_" (name state) ".ico"))

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
     :state (name state)
     :favicon-path (get-favicon-path state)}))

(defn generate-build-panel [{:keys [build-definition-name build-definition-id build-number
                                    status-text state commit-message]}]
  [:a {:href (str "/build-definitions/" build-definition-id)}
   [:div {:id (str "build-definition-id-" build-definition-id) :class (str "build-panel " state)}
    [:h1.status status-text]
    [:h1.build-definition-name build-definition-name]
    [:h1.build-number build-number]
    [:div.commit-message commit-message]]])

(defn generate-build-definition-html [build-info refresh-info]
  (hiccup/html
    [:head
     [:title "Build Status"]
     [:link {:rel "shortcut icon" :href (:favicon-path build-info)}]
     (when refresh-info
       (list [:link {:rel "stylesheet" :href
                     "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}]
             [:script
              (str "window.buildDefinitionIds = [\"" (s/join "\",\"" (:build-definition-ids refresh-info)) "\"];")
              (str "window.refreshSeconds = " (:refresh-interval refresh-info) ";")]
             [:script {:src "/refresh.js" :defer "defer"}]))
     [:link {:rel "stylesheet ":href "/style.css" :type "text/css"}]]
    [:body refresh-icon error-modal (generate-build-panel build-info)]))

(defn retrieve-commit-message [account token build]
  (let [repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (client/get commit-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :comment)
         (catch Exception e))))

(defn retrieve-last-two-builds [account project token build-definition-id]
  (let [last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (client/get last-two-builds-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn retrieve-build-definitions [account project token]
  (let [build-definitions-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                   project "/_apis/build/definitions?api-version=2.0")]
    (try (-> (client/get build-definitions-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn retrieve-build-info [account project token build-definition-id]
  (let [[build previous-build] (retrieve-last-two-builds account project token build-definition-id)
        commit-message (retrieve-commit-message account token build)]
    (when build
      (generate-build-info build previous-build commit-message))))

(defn build-definition-screen [account project token request]
  (let [build-definition-id (-> request :route-params :build-definition-id)
        build-info (retrieve-build-info account project token build-definition-id)
        refresh-interval (refresh-interval (:query-params request))
        refresh-info (when refresh-interval
                       {:refresh-interval refresh-interval
                        :build-definition-ids [build-definition-id]})]
    (when build-info
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (generate-build-definition-html build-info refresh-info)})))

(defn build-info [account project token request]
  (let [build-definition-id (-> request :route-params :build-definition-id)
        build-info (retrieve-build-info account project token build-definition-id)]
    (when build-info
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body (json/generate-string build-info)})))

(defn generate-build-monitor-html [build-info-maps]
  (hiccup/html
    [:head
     [:title "Build Monitor"]
     [:link {:rel "stylesheet ":href "/style.css" :type "text/css"}]]
    [:body {:class (str "panel-count-" (count build-info-maps))}
     (map generate-build-panel build-info-maps)]))

(defn build-definition->build-info [account project token build-definition]
  (retrieve-build-info account project token (:id build-definition)))

(defn build-monitor [account project token request]
  (let [build-definitions (retrieve-build-definitions account project token)]
    (when (> (count build-definitions) 0)
      (let [build-info-maps (map (partial build-definition->build-info account project token) build-definitions)]
        {:status 200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (generate-build-monitor-html build-info-maps)}))))

(def routes ["/" {"" :build-monitor
                  ["build-definitions/" [#"\d+" :build-definition-id]] :build-definition
                  ["ajax/build-definitions/" [#"\d+" :build-definition-id]] :build-info}])

(defn wrap-routes [handlers]
  (fn [request]
    (when-let [route-m (bidi/match-route routes (:uri request))]
      (when-let [handler (-> route-m :handler handlers)]
        (handler (merge request (select-keys route-m [:route-params])))))))

(defn handlers [account project token]
  {:build-monitor
   (partial build-monitor account project token)
   :build-definition
   (partial build-definition-screen account project token)
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
