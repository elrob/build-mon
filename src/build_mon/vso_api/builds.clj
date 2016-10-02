(ns build-mon.vso-api.builds
  (:require [clostache.parser :as c]))

(def build-definitions-url
  "https://{{account}}.visualstudio.com/defaultcollection/{{project}}/_apis/build/definitions?api-version=2.0")

(def last-two-builds-url
  "https://{{account}}.visualstudio.com/defaultcollection/{{project}}/_apis/build/builds?api-version=2.0&$top=2&definitions={{build}}")

(def commit-url
  "https://{{account}}.visualstudio.com/defaultcollection/_apis/git/repositories/{{repo}}/commits/{{version}}?api-version=1.0")

(defn- retrieve-commit-message [vso-api-data build]
  (let [{:keys [get-fn account logger]} vso-api-data
        url (c/render commit-url {:account account
                                  :repo (-> build :repository :id)
                                  :version (:sourceVersion build)})]
    (try (-> (get-fn url) :comment)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve commit message." e)))))

(defn- retrieve-last-two-builds [vso-api-data build-definition-id]
  (let [{:keys [get-fn account project logger]} vso-api-data
        url (c/render last-two-builds-url {:account account :project project :build build-definition-id})]
    (try (-> (get-fn url) :value)
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
  (let [{:keys [get-fn logger]} vso-api-data
        url (c/render build-definitions-url vso-api-data)]
    (try (-> (get-fn url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve build definitions." e)))))

(defn vso-api-fns [logger get-fn account project]
  (let [vso-api-data {:get-fn get-fn :logger logger
                      :account account :project project}]
    {:retrieve-build-info (partial retrieve-build-info vso-api-data)
     :retrieve-build-definitions (partial retrieve-build-definitions vso-api-data)}))
