(ns build-mon.vso-api.releases
  (:require [clostache.parser :as c]
            [taoensso.timbre :as log]))

(def release-definitions-url
  (str "https://{{account}}.vsrm.visualstudio.com"
       "/defaultcollection/{{project}}/_apis/release/definitions"
       "?api-version=3.0-preview.2"))

(def last-two-releases-url
  (str "https://{{account}}.vsrm.visualstudio.com"
       "/defaultcollection/{{project}}/_apis/release/releases"
       "?api-version=3.0-preview.2&$top=2&definitionId={{release}}"))

(defn- retrieve-release [release vso-release-api-data]
  (when-let [release-url (-> release :_links :self :href)]
    ((:get-fn vso-release-api-data) release-url)))

(defn- retrieve-last-two-releases [vso-release-api-data release-definition-id]
  (let [{:keys [get-fn account project]} vso-release-api-data
        url (c/render last-two-releases-url {:account account :project project :release release-definition-id})]
    (:value (get-fn url))))

(defn- retrieve-release-info [vso-release-api-data release-definition-id]
  (try (let [last-two-releases (retrieve-last-two-releases vso-release-api-data release-definition-id)
             [release previous-release] (map #(retrieve-release % vso-release-api-data) last-two-releases)]
         {:release release :previous-release previous-release})
       (catch Exception e
         (log/error e (format "Bad Response when attempting to retrieve release %s."
                              release-definition-id)))))

(defn- retrieve-release-definitions [vso-release-api-data]
  (let [url (c/render release-definitions-url vso-release-api-data)]
    (try (:value ((:get-fn vso-release-api-data) url))
         (catch Exception e
           (log/error e "Bad Response when attempting to retrieve release definitions.")))))

(defn vso-release-api-fns [get-fn account project]
  (let [vso-release-api-data {:get-fn get-fn :account account :project project}]
    {:retrieve-release-info (partial retrieve-release-info vso-release-api-data)
     :retrieve-release-definitions (partial retrieve-release-definitions vso-release-api-data)}))
