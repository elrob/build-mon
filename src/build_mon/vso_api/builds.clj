(ns build-mon.vso-api.builds
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [build-mon.vso-api.util :as util]))

(defn- retrieve-commit-message [vso-api-data build]
  (let [{:keys [get-fn account logger]} vso-api-data
        repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (util/get-json-body get-fn commit-url) :comment)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve commit message." e)))))

(defn- retrieve-last-two-builds [vso-api-data build-definition-id]
  (let [{:keys [get-fn account project logger]} vso-api-data
        last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (util/get-json-body get-fn last-two-builds-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve last two builds." e)))))

(defn- retrieve-build-info [vso-api-data build-definition-id]
  (let [[build previous-build] (retrieve-last-two-builds vso-api-data build-definition-id)
        commit-message (retrieve-commit-message vso-api-data build)]
    (when build
      {:build build
       :previous-build previous-build
       :commit-message commit-message})))

(defn- retrieve-build-definitions [vso-api-data]
  (let [{:keys [get-fn account project logger]} vso-api-data
        build-definitions-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                   project "/_apis/build/definitions?api-version=2.0")]
    (try (-> (util/get-json-body get-fn build-definitions-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve build definitions." e)))))

(defn vso-api-fns [logger get-fn account project]
  (let [vso-api-data {:get-fn get-fn :logger logger
                      :account account :project project}]
    {:retrieve-build-info (partial retrieve-build-info vso-api-data)
     :retrieve-build-definitions (partial retrieve-build-definitions vso-api-data)}))
