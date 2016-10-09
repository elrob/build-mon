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
            [build-mon.favicon :as favicon]
            [build-mon.html :as html])
  (:gen-class))

(defn- release-not-started? [release] (= (:status release) "notStarted"))
(defn- release-succeeded? [release] (= (:status release) "succeeded"))
(defn- release-in-progress? [release] (= (:status release) "inProgress"))

(defn- get-release-state [release-env previous-release-env]
  (cond (release-succeeded? release-env) :succeeded
        (and (release-in-progress? release-env)
             (release-succeeded? previous-release-env)) :in-progress
        (and (release-in-progress? release-env)
             (not (release-succeeded? previous-release-env))) :in-progress-after-failed
        (release-not-started? release-env) :not-started
        :default :failed))

(defn- generate-release-environments [release previous-release]
  (let [environments (:environments release)
        previous-environments (:environments previous-release)]
    (map (fn [env]
      ; need to explicitly grab first item here because filter returns a collection
           (let [prev-env-release (first (filter (fn [prev-env]
                                                   (= (:name env) (:name prev-env)))
                                                 previous-environments))
                 release-state (get-release-state env prev-env-release)]
             {:env-name (:name env) :state release-state}))
         environments)))

(defn- generate-release-info [release previous-release]
  {:release-definition-name (-> :releaseDefinition release :name)
   :release-definition-id (:id release)
   :release-number (:name release)
   :release-environments (generate-release-environments release previous-release)})

(defn retrieve-release-info [vso-release-api release-definition-id]
  (let [{:keys [release previous-release]}
        ((:retrieve-release-info vso-release-api) release-definition-id)]
    (when release
      (generate-release-info release previous-release))))

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
    (when (and (not-empty build-info-maps) (not-empty release-info-maps))
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

(defn -main [& [vso-account vso-project vso-personal-access-token port]]
  (let [port (Integer. (or port 3000))]
    (if (and vso-account vso-project vso-personal-access-token port)
      (let [account (codec/url-encode vso-account)
            project (codec/url-encode vso-project)
            get-fn (api-util/vso-api-get-fn vso-personal-access-token)
            vso-api (builds-api/vso-api-fns get-fn account project)
            vso-release-api (releases-api/vso-release-api-fns get-fn account project)
            wrapped-handler (-> (request-handlers vso-api vso-release-api)
                                wrap-routes
                                (resource/wrap-resource "public")
                                (params/wrap-params))]
        (ring-jetty/run-jetty wrapped-handler {:port port}))
      (log/error "App didn't start due to missing parameters."))))
