(ns build-mon.vso-api.releases
  (:require [build-mon.vso-api.util :as util]))

(defn- retrieve-release [release vso-release-api-data]
  (when-let [release-url (-> release :_links :self :href)]
    ((:get-fn vso-release-api-data) release-url)))

(defn- retrieve-last-two-releases [vso-release-api-data release-definition-id]
  (let [{:keys [get-fn account project]} vso-release-api-data
        last-two-releases-url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                                   project "/_apis/release/releases?api-version=3.0-preview.2&$top=2"
                                   "&definitionId=" release-definition-id)]
    (-> (get-fn last-two-releases-url) :value)))

(defn- retrieve-release-info [vso-release-api-data release-definition-id]
  (try (let [last-two-releases (retrieve-last-two-releases vso-release-api-data release-definition-id)
             [release previous-release] (map #(retrieve-release % vso-release-api-data) last-two-releases)]
         {:release release :previous-release previous-release})
       (catch Exception e
         ((-> vso-release-api-data :logger :log-exception)
          (format "Bad Response when attempting to retrieve release %s." release-definition-id)
          e))))

(defn- retrieve-release-definitions [vso-release-api-data]
  (let [{:keys [get-fn account project logger]} vso-release-api-data
        url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                 project "/_apis/release/definitions?api-version=3.0-preview.2")]
    (try (-> (get-fn url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve release definitions." e)))))

(defn vso-release-api-fns [logger get-fn account project]
  (let [vso-release-api-data {:get-fn get-fn :logger logger
                              :account account :project project}]
    {:retrieve-release-info (partial retrieve-release-info vso-release-api-data)
     :retrieve-release-definitions (partial retrieve-release-definitions vso-release-api-data)}))
