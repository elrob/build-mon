(ns build-mon.feature-test.test
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [org.httpkit.fake :as http]
            [build-mon.core :as core]))

(def a-build-definitions-url
  (str "https://a-vso-account.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/build/definitions?api-version=2.0"))

(def a-build-definitions-response
  "{ \"count\": 2,
     \"value\": [ { \"id\": 111, \"name\": \"a-build-definition\" },
                  { \"id\": 222, \"name\": \"another-build-definition\" } ] }")

(def build-definition-111-url
  (str "https://a-vso-account.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/build/builds"
       "?api-version=2.0&$top=2&definitions=111"))

(def build-definition-111-response
  "{ \"count\": 2,
    \"value\": [
    {
      \"buildNumber\": \"build-123\",
      \"status\": \"completed\",
      \"result\": \"succeeded\",
      \"definition\": { \"id\": 111, \"name\": \"a-build-definition\" },
      \"sourceVersion\": \"A_SOURCE_VERSION_GUID\",
      \"repository\": { \"id\": \"A_REPOSITORY_GUID\" }
    },
    {
      \"buildNumber\": \"build-456\",
      \"status\": \"completed\",
      \"result\": \"succeeded\",
      \"definition\": { \"id\": 111, \"name\": \"a-build-definition\" }
    } ] }")

(def build-definition-111-commit-url
  (str "https://a-vso-account.visualstudio.com"
       "/defaultcollection/_apis/git/repositories"
       "/A_REPOSITORY_GUID/commits/A_SOURCE_VERSION_GUID?api-version=1.0"))

(def build-definition-111-commit-response "{ \"comment\": \"A_COMMIT_MESSAGE\" }")

(def build-definition-222-url
  (str "https://a-vso-account.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/build/builds"
       "?api-version=2.0&$top=2&definitions=222"))

(def build-definition-222-response
  "{ \"count\": 2,
    \"value\": [
    {
      \"buildNumber\": \"build-1234\",
      \"status\": \"completed\",
      \"result\": \"succeeded\",
      \"definition\": { \"id\": 222, \"name\": \"another-build-definition\" },
      \"sourceVersion\": \"ANOTHER_SOURCE_VERSION_GUID\",
      \"repository\": { \"id\": \"ANOTHER_REPOSITORY_GUID\" }
    },
    {
      \"buildNumber\": \"build-5678\",
      \"status\": \"completed\",
      \"result\": \"succeeded\",
      \"definition\": { \"id\": 222, \"name\": \"another-build-definition\" }
    } ] }")

(def build-definition-222-commit-url
  (str "https://a-vso-account.visualstudio.com"
       "/defaultcollection/_apis/git/repositories"
       "/ANOTHER_REPOSITORY_GUID/commits/ANOTHER_SOURCE_VERSION_GUID?api-version=1.0"))

(def build-definition-222-commit-response
  "{ \"comment\": \"ANOTHER_COMMIT_MESSAGE\" }")

(def a-release-definitions-url
  (str "https://a-vso-account.vsrm.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/release/definitions?api-version=3.0-preview.2"))

(fact "feature test"
      (http/with-fake-http
        [a-build-definitions-url a-build-definitions-response
         build-definition-111-url build-definition-111-response
         build-definition-222-url build-definition-222-response
         build-definition-111-commit-url build-definition-111-commit-response
         build-definition-222-commit-url build-definition-222-commit-response
         a-release-definitions-url 200]
        (-> (k/session (core/app "a-vso-account" "a-vso-project" "an-access-token"))
            (k/visit "/")
            :response :status)) => 200)
