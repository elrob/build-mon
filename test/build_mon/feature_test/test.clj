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
      \"buildNumber\": \"build-4321\",
      \"status\": \"completed\",
      \"result\": \"succeeded\",
      \"definition\": { \"id\": 222, \"name\": \"another-build-definition\" },
      \"sourceVersion\": \"ANOTHER_SOURCE_VERSION_GUID\",
      \"repository\": { \"id\": \"ANOTHER_REPOSITORY_GUID\" }
    },
    {
      \"buildNumber\": \"build-8765\",
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

(def a-release-definitions-response
  "{ \"count\": 2,
     \"value\": [ { \"id\": 555, \"name\": \"a-release-definition\" },
                  { \"id\": 666, \"name\": \"another-release-definition\" } ] }")

(def release-definition-555-url
  (str "https://a-vso-account.vsrm.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/release/releases"
       "?api-version=3.0-preview.2&$top=2&definitionId=555"))

(def release-definition-555-response
  "{ \"count\": 2,
     \"value\": [
         { \"_links\": { \"self\": { \"href\": \"https://a.release555.url\" } } },
         { \"_links\": { \"self\": { \"href\": \"https://a.previous.release555.url\" } } } ] }")

(def release-definition-555-release-url "https://a.release555.url")

(def release-definition-555-release-response
  "{\"id\": 555123,
    \"name\": \"a-release-definition-555-release\",
    \"environments\": [ { \"name\": \"release-555-env-1\", \"status\": \"succeeded\" },
                        { \"name\": \"release-555-env-2\", \"status\": \"succeeded\" } ],
    \"releaseDefinition\": { \"name\": \"release-definition-555-name\" } }")

(def release-definition-555-previous-release-url "https://a.previous.release555.url")

(def release-definition-555-previous-release-response
  "{\"id\": 555456,
    \"name\": \"a-release-definition-555-previous-release\",
    \"environments\": [ { \"name\": \"release-555-env-1\", \"status\": \"succeeded\" },
                        { \"name\": \"release-555-env-2\", \"status\": \"succeeded\" } ],
    \"releaseDefinition\": { \"name\": \"release-definition-555-name\" } }")

(def release-definition-666-url
  (str "https://a-vso-account.vsrm.visualstudio.com"
       "/defaultcollection/a-vso-project/_apis/release/releases"
       "?api-version=3.0-preview.2&$top=2&definitionId=666"))

(def release-definition-666-response
  "{ \"count\": 2,
     \"value\": [
         { \"_links\": { \"self\": { \"href\": \"https://a.release666.url\" } } },
         { \"_links\": { \"self\": { \"href\": \"https://a.previous.release666.url\" } } } ] }")

(def release-definition-666-release-url "https://a.release666.url")

(def release-definition-666-release-response
  "{\"id\": 666123,
    \"name\": \"a-release-definition-666-release\",
    \"environments\": [ { \"name\": \"release-666-env-1\", \"status\": \"succeeded\" },
                        { \"name\": \"release-666-env-2\", \"status\": \"succeeded\" } ],
    \"releaseDefinition\": { \"name\": \"release-definition-666-name\" } }")

(def release-definition-666-previous-release-url "https://a.previous.release666.url")

(def release-definition-666-previous-release-response
  "{\"id\": 666456,
    \"name\": \"a-release-definition-666-previous-release\",
    \"environments\": [ { \"name\": \"release-666-env-1\", \"status\": \"succeeded\" },
                        { \"name\": \"release-666-env-2\", \"status\": \"succeeded\" } ],
    \"releaseDefinition\": { \"name\": \"release-definition-666-name\" } }")

(fact "build and releases are displayed"
      (http/with-fake-http
        [a-build-definitions-url a-build-definitions-response
         build-definition-111-url build-definition-111-response
         build-definition-222-url build-definition-222-response
         build-definition-111-commit-url build-definition-111-commit-response
         build-definition-222-commit-url build-definition-222-commit-response
         a-release-definitions-url a-release-definitions-response
         release-definition-555-url release-definition-555-response
         release-definition-666-url release-definition-666-response
         release-definition-555-release-url release-definition-555-release-response
         release-definition-555-previous-release-url release-definition-555-previous-release-response
         release-definition-666-release-url release-definition-666-release-response
         release-definition-666-previous-release-url release-definition-666-previous-release-response]
        (let [response (-> (core/app "a-vso-account" "a-vso-project" "an-access-token")
                           k/session (k/visit "/") :response)]
          (:status response) => 200
          (:body response) => (contains "a-build-definition")
          (:body response) => (contains "another-build-definition")
          (:body response) => (contains "build-123")
          (:body response) => (contains "build-4321")
          (:body response) => (contains "A_COMMIT_MESSAGE")
          (:body response) => (contains "ANOTHER_COMMIT_MESSAGE")
          (:body response) => (contains "release-definition-555-name")
          (:body response) => (contains "a-release-definition-555-release")
          (:body response) => (contains "release-555-env-1")
          (:body response) => (contains "release-555-env-2")
          (:body response) => (contains "release-definition-666-name")
          (:body response) => (contains "a-release-definition-666-release")
          (:body response) => (contains "release-666-env-1")
          (:body response) => (contains "release-666-env-2"))))

(fact "builds are displayed when there are no release definitions"
      (http/with-fake-http
        [a-build-definitions-url a-build-definitions-response
         build-definition-111-url build-definition-111-response
         build-definition-222-url build-definition-222-response
         build-definition-111-commit-url build-definition-111-commit-response
         build-definition-222-commit-url build-definition-222-commit-response
         a-release-definitions-url "{}"]
        (let [response (-> (core/app "a-vso-account" "a-vso-project" "an-access-token")
                           k/session (k/visit "/") :response)]
          (:status response) => 200
          (:body response) => (contains "a-build-definition")
          (:body response) => (contains "another-build-definition")
          (:body response) => (contains "build-123")
          (:body response) => (contains "build-4321")
          (:body response) => (contains "A_COMMIT_MESSAGE")
          (:body response) => (contains "ANOTHER_COMMIT_MESSAGE"))))

(fact "releases are displayed when there are no build definitions"
      (http/with-fake-http
        [a-build-definitions-url "{}"
         a-release-definitions-url a-release-definitions-response
         release-definition-555-url release-definition-555-response
         release-definition-666-url release-definition-666-response
         release-definition-555-release-url release-definition-555-release-response
         release-definition-555-previous-release-url release-definition-555-previous-release-response
         release-definition-666-release-url release-definition-666-release-response
         release-definition-666-previous-release-url release-definition-666-previous-release-response]
        (let [response (-> (core/app "a-vso-account" "a-vso-project" "an-access-token")
                           k/session (k/visit "/") :response)]
          (:status response) => 200
          (:body response) => (contains "release-definition-555-name")
          (:body response) => (contains "a-release-definition-555-release")
          (:body response) => (contains "release-555-env-1")
          (:body response) => (contains "release-555-env-2")
          (:body response) => (contains "release-definition-666-name")
          (:body response) => (contains "a-release-definition-666-release")
          (:body response) => (contains "release-666-env-1")
          (:body response) => (contains "release-666-env-2"))))
