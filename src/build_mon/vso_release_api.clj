(ns build-mon.vso-release-api
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn vso-release-api-get-fn [token]
 (fn [url] (client/get url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]
                            :accept :json :follow-redirects false})))

(defn- validate-200-response [response]
  (if (= (:status response) 200)
    response
    (throw (ex-info "Response status was not 200" {:response response}))))

(defn- get-json-body [get-fn url]
  (-> url get-fn validate-200-response :body (json/parse-string true)))





(defn retrieve-commit-message [vso-release-api-data release]
  (let [{:keys [get-fn account logger]} vso-release-api-data
        repository-id (-> release :repository :id)
        source-version (:sourceVersion release)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (get-json-body get-fn commit-url) :comment)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve commit message." e)))))





(defn- map-release-info [release vso-release-api-data]
  (let [release-url (-> release :_links :self :href)
        get-fn (:get-fn vso-release-api-data)]
    (get-json-body get-fn release-url)))
    ; merge environment property into return value


(defn- retrieve-last-two-releases [vso-release-api-data release-definition-id]
  (let [{:keys [get-fn account project logger]} vso-release-api-data
        last-two-releases-url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                                 project "/_apis/release/releases?api-version=3.0-preview.2&definitionId=" release-definition-id "&$top=2")]
    (try (-> (get-json-body get-fn last-two-releases-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve last two releases." e)))))



(defn- retrieve-release-info [vso-release-api-data release-definition-id]
  (let [[release previous-release] (retrieve-last-two-releases vso-release-api-data release-definition-id)
        release-info (map-release-info release vso-release-api-data)
        previous-release-info (map-release-info previous-release vso-release-api-data)
        ;commit-message (retrieve-commit-message vso-release-api-data release)
        ]
    (when release ;when release-info ???
      {:release release-info
       :previous-release previous-release-info
       ;:commit-message commit-message
       })))


(defn- retrieve-release-definitions [vso-release-api-data]
  (let [{:keys [get-fn account project logger]} vso-release-api-data
        build-definitions-url (str "https://" account  ".vsrm.visualstudio.com/defaultcollection/"
                                   project "/_apis/release/definitions?api-version=3.0-preview.2")]
    (try (-> (get-json-body get-fn build-definitions-url) :value)
         (catch Exception e
           ((:log-exception logger) "Bad Response when attempting to retrieve build definitions." e)))))





; API
; =========================

(defn vso-release-api-fns [logger get-fn account project]
 (let [vso-release-api-data {:get-fn get-fn :account account :project project :logger logger}]
   {:retrieve-release-info (partial retrieve-release-info vso-release-api-data)
    :retrieve-release-definitions (partial retrieve-release-definitions vso-release-api-data)}))
