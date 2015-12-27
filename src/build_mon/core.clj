(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [bidi.bidi :as bidi]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [hiccup.core :as hiccup])
  (:gen-class))

(def states #{:succeeded :failed :in-progress :in-progress-after-failed})

(def default-refresh-interval 20)
(def minimum-refresh-interval 5)

(defn succeeded? [build] (= (:result build) "succeeded"))

(defn in-progress? [build] (nil? (:result build)))

(defn get-state [build previous-build]
  (cond (succeeded? build) :succeeded
        (and (in-progress? build) (succeeded? previous-build)) :in-progress
        (and (in-progress? build) (not (succeeded? previous-build))) :in-progress-after-failed
        :default :failed))

(defn favicon-path [state]
  (str "/favicon_" (name state) ".ico"))

(defn status-text [build]
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

(defn generate-build-monitor-html [build previous-build commit-message refresh-info]
  (let [state (get-state build previous-build)]
    (hiccup/html
      [:head
       [:title "Build Status"]
       [:link {:rel "shortcut icon" :href (favicon-path state)}]
       [:link {:rel "stylesheet ":href "/style.css" :type "text/css"}]
       (when refresh-info (list [:script
                                 (str "window.refreshPath = \"" (:refresh-path refresh-info) "\";")
                                 (str "window.refreshSeconds = " (:refresh-interval refresh-info) ";")]
                                [:script {:src "/refresh.js" :defer "defer"}]))]
      [:body
       [:div {:class (str "build-panel " (name state))}
        [:h1.status (status-text build)]
        [:h1.build-definition-name (-> build :definition :name)]
        [:h1.build-number (:buildNumber build)]
        [:div.commit-message commit-message]]])))

(defn get-commit-message [account token build]
  (let [repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (client/get commit-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :comment)
         (catch Exception e))))

(defn get-last-two-builds [account project token build-definition-id]
  (let [last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (client/get last-two-builds-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn get-build-definitions [account project token]
  (let [build-definitions-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                   project "/_apis/build/definitions?api-version=2.0")]
    (try (-> (client/get build-definitions-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn build-definition-screen [account project token request]
  (let [build-definition-id (-> request :route-params :build-definition-id)
        [build previous-build] (get-last-two-builds account project token build-definition-id)
        commit-message (get-commit-message account token build)
        refresh-interval (refresh-interval (:query-params request))
        refresh-info (when refresh-interval
                       {:refresh-interval refresh-interval
                        :refresh-path (:uri request)})]
    (when build
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (generate-build-monitor-html build previous-build commit-message refresh-info)})))

(defn generate-index-html [build-definitions]
  (hiccup/html
    [:head
     [:title "Choose the Build"]]
    [:body
     [:ul (for [definition build-definitions]
            [:li
             [:a {:href (str "/build-definitions/" (:id definition))} (:name definition)]])]]))

(defn index [account project token request]
  (let [build-definitions (get-build-definitions account project token)]
    (when (> (count build-definitions) 0)
      {:status 200
       :headers {"Content-Type" "text/html; charset=utf-8"}
       :body (generate-index-html build-definitions)})))

(def routes ["/" {"" :index
                  ["build-definitions/" [#"\d+" :build-definition-id]] :build-definition}])

(defn wrap-routes [handlers]
  (fn [request]
    (when-let [route-m (bidi/match-route routes (:uri request))]
      (when-let [handler (-> route-m :handler handlers)]
        (handler (merge request (select-keys route-m [:route-params])))))))

(defn handlers [account project token]
  {:index
   (partial index account project token)
   :build-definition
   (partial build-definition-screen account project token)})

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [wrapped-handler (-> (handlers vso-account vso-project vso-personal-access-token)
                                wrap-routes
                                (resource/wrap-resource "public")
                                (params/wrap-params))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (prn "App didn't start due to missing parameters."))))
