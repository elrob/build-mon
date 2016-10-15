(ns build-mon.core
  (:require [ring.adapter.jetty :as ring-jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.params :as params]
            [ring.util.codec :as codec]
            [bidi.bidi :as bidi]
            [clj-time.core :as t]
            [taoensso.timbre :as log]
            [build-mon.vso-api.builds :as builds-api]
            [build-mon.vso-api.releases :as releases-api]
            [build-mon.vso-api.util :as api-util]
            [build-mon.builds :as builds]
            [build-mon.releases :as releases]
            [build-mon.favicon :as favicon]
            [build-mon.html :as html])
  (:gen-class))

(defn retrieve-release-info [vso-release-api release-definition-id]
  (let [{:keys [release previous-release]}
        ((:retrieve-release-info vso-release-api) release-definition-id)]
    (when release
      (releases/generate-release-info release previous-release))))

(defn retrieve-build-info [vso-api build-definition-id]
  (let [{:keys [build previous-build commit-message]}
        ((:retrieve-build-info vso-api) build-definition-id)]
    (when build
      (builds/generate-build-info build previous-build commit-message))))

(defn build-monitor-for-definitions [vso-api vso-release-api request
                                     build-definition-ids release-definition-ids]
  (let [build-info-maps (remove nil? (map #(retrieve-build-info vso-api %) build-definition-ids))
        release-info-maps (remove nil?
                                  (map #(retrieve-release-info vso-release-api %) release-definition-ids))]
    (when (or (not-empty build-info-maps) (not-empty release-info-maps))
      (let [favicon-path (favicon/get-favicon-path build-info-maps release-info-maps)]
        {:status 200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (html/generate-build-monitor-html build-info-maps release-info-maps favicon-path)}))))

(defn build-monitor [vso-api vso-release-api request]
  (let [release-definitions ((:retrieve-release-definitions vso-release-api))
        release-definition-ids (map :id release-definitions)
        build-definitions ((:retrieve-build-definitions vso-api))
        build-definition-ids (map :id build-definitions)]
    (build-monitor-for-definitions vso-api vso-release-api request
                                   build-definition-ids release-definition-ids)))

(def routes ["/" :build-monitor])

(defn wrap-routes [handlers]
  (fn [request]
    (let [request-start-time (t/now)]
      (when-let [route (bidi/match-route routes (:uri request))]
        (when-let [handler (-> route :handler handlers)]
          (let [response (handler (merge request (select-keys route [:route-params])))]
            (log/info (format "Response time: %s seconds"
                              (t/in-seconds (t/interval request-start-time (t/now)))))
            response))))))

(defn request-handlers [vso-api vso-release-api]
  {:build-monitor (partial build-monitor vso-api vso-release-api)})

(defn app [vso-account vso-project vso-personal-access-token]
  (let [account (codec/url-encode vso-account)
        project (codec/url-encode vso-project)
        get-fn (api-util/vso-api-get-fn vso-personal-access-token)
        vso-api (builds-api/vso-api-fns get-fn account project)
        vso-release-api (releases-api/vso-release-api-fns get-fn account project)]
    (-> (request-handlers vso-api vso-release-api)
        wrap-routes
        (resource/wrap-resource "public")
        (params/wrap-params))))

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (ring-jetty/run-jetty
       (app vso-account vso-project vso-personal-access-token)
       {:port port})
      (log/error "App didn't start due to missing parameters."))))
