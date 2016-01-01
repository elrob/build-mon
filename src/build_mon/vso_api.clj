(ns build-mon.vso-api
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(defn- retrieve-commit-message [account token build]
  (let [repository-id (-> build :repository :id)
        source-version (:sourceVersion build)
        commit-url (str "https://" account ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                        repository-id "/commits/" source-version "?api-version=1.0")]
    (try (-> (client/get commit-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :comment)
         (catch Exception e))))

(defn- retrieve-last-two-builds [account project token build-definition-id]
  (let [last-two-builds-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                 project "/_apis/build/builds?api-version=2.0&$top=2"
                                 "&definitions=" build-definition-id)]
    (try (-> (client/get last-two-builds-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))

(defn retrieve-build-info [account project token build-definition-id]
  (let [[build previous-build] (retrieve-last-two-builds account project token build-definition-id)
        commit-message (retrieve-commit-message account token build)]
    {:build build
     :previous-build previous-build
     :commit-message commit-message}))

(defn retrieve-build-definitions [account project token]
  (let [build-definitions-url (str "https://" account  ".visualstudio.com/defaultcollection/"
                                   project "/_apis/build/definitions?api-version=2.0")]
    (try (-> (client/get build-definitions-url {:basic-auth ["USERNAME CAN BE ANY VALUE" token]})
             :body (json/parse-string true) :value)
         (catch Exception e))))
