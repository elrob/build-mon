(ns build-mon.vso-api
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn- validate-200-response [response]
  (if (= (:status response) 200)
    response
    (throw (ex-info "Response status was not 200" {:response response}))))

(defn- get-json-body [get-fn url]
  (-> url get-fn validate-200-response :body (json/parse-string true)))

(defn- log-exception [message exception]
  (prn "=========   ERROR   ==========")
  (prn message)
  (prn exception)
  (prn "=============================="))

(defn- retrieve-commit-message [vso-api-data build]
  (let [{:keys [get-fn account logger]} vso-api-data
        repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (get-json-body get-fn commit-url) :comment)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve commit message." e)))))

(defn- retrieve-last-two-builds [vso-api-data build-definition-id]
  (let [{:keys [get-fn account project logger]} vso-api-data
        last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (get-json-body get-fn last-two-builds-url) :value)
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
    (try (-> (get-json-body get-fn build-definitions-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve build definitions." e)))))

(defn vso-api-get-fn [token]
  (fn [url] (client/get url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]
                             :accept :json :follow-redirects false})))

(defn vso-api-fns [logger get-fn account project]
  (let [vso-api-data {:get-fn get-fn :account account :project project :logger logger}]
    {:retrieve-build-info (partial retrieve-build-info vso-api-data)
     :retrieve-build-definitions (partial retrieve-build-definitions vso-api-data)}))
