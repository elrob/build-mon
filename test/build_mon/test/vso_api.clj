(ns build-mon.test.vso-api
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api :as api]))

(def account "VSO_ACCOUNT_NAME")
(def project "VSO_PROJECT_NAME")

(def logger {:log-exception (fn [message exception] (println "Exception:" message))})

(fact "vso-api-get-fn wraps an api token and returns a function which makes a HTTP GET request on a url"
      (let [result (api/vso-api-get-fn "VSO_API_TOKEN")]
        (fn? result) => truthy
        (result "NOT A REAL URL") => (throws java.net.MalformedURLException)))

(fact "vso-api-fns returns a map of the exposed functions"
      (let [get-fn (fn [url] "API RESPONSE")
            result (api/vso-api-fns logger get-fn account project)]
        (fn? (:retrieve-build-info result)) => truthy
        (fn? (:retrieve-build-definitions result)) => truthy))

(defn get-fn-stub-requests [stubbed-url->stubbed-response-map]
  (fn [url]
    (get stubbed-url->stubbed-response-map url
         {:URL :MISMATCH :url url :stubbed-urls (keys stubbed-url->stubbed-response-map)})))

(facts "retrieve-build-definitions"
       (let [expected-url (str "https://" account ".visualstudio.com/defaultcollection/" project
                               "/_apis/build/definitions?api-version=2.0")]
         (fact "calls VSO api, parses response and extracts build definitions"
               (let [stub-json-body "{\"count\":2,\"value\":[
                                    {\"A_BUILD_DEFINITION_KEY\": \"A_VALUE\"},
                                    {\"ANOTHER_BUILD_DEFINITION_KEY\": \"ANOTHER_VALUE\"}]}"
                     stub-response {:status 200 :body stub-json-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => [{:A_BUILD_DEFINITION_KEY "A_VALUE"}
                                                             {:ANOTHER_BUILD_DEFINITION_KEY "ANOTHER_VALUE"}]))
         (fact "when response status is not 200, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => nil))
         (fact "when response body cannot be parsed as JSON, returns nil"
               (let [stub-response {:status 200 :body "~~INVALID_JSON~~"}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => nil))))

(facts "retrieve-build-info for a build definition"
       (let [build-definition-id 666
             repository-id "ABC-123"
             source-version "XYZ-456"
             expected-builds-url (str "https://" account ".visualstudio.com/defaultcollection/" project
                                      "/_apis/build/builds?api-version=2.0&$top=2&definitions="
                                      build-definition-id)
             expected-commit-url (str "https://" account
                                      ".visualstudio.com/defaultcollection/_apis/git/repositories/"
                                      repository-id "/commits/" source-version "?api-version=1.0")
             stub-builds-body (str "{\"count\":2,\"value\":["
                                   "{\"repository\": {\"id\": \"" repository-id "\"},"
                                   "\"sourceVersion\": \"" source-version "\"},"
                                   "{\"some_previous_build_key\": \"some_value\"}]}")
             stub-builds-response {:status 200 :body stub-builds-body}]
         (fact "calls VSO api for builds and commit message, parses responses and extracts build info"
               (let [stub-commit-response {:status 200 :body "{\"comment\": \"SOME COMMIT MESSAGE\"}"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response
                                                           expected-commit-url stub-commit-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id)
                 => {:build {:repository {:id repository-id}
                             :sourceVersion source-version}
                     :previous-build {:some_previous_build_key "some_value"}
                     :commit-message "SOME COMMIT MESSAGE"}))
         (fact "when there is no previous build, returns current build and a nil previous build"
               (let [stub-builds-body-with-no-previous-build
                     (str "{\"count\":1,\"value\":["
                          "{\"repository\": {\"id\": \"" repository-id "\"},"
                          "\"sourceVersion\": \"" source-version "\"}]}")
                     stub-builds-response {:status 200 :body stub-builds-body-with-no-previous-build}
                     stub-commit-response {:status 200 :body "{\"comment\": \"SOME COMMIT MESSAGE\"}"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response
                                                           expected-commit-url stub-commit-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id)
                 => {:build {:repository {:id repository-id}
                             :sourceVersion source-version}
                     :previous-build nil
                     :commit-message "SOME COMMIT MESSAGE"}))
         (fact "when builds response status is not 200, returns nil"
               (let [stub-builds-response {:status 503}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id) => nil))
         (fact "when builds response cannot be parsed as JSON, returns nil"
               (let [stub-builds-response {:status 200 :body "~~INVALID_JSON~~"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id) => nil))
         (fact "when commit response status is not 200, returns builds with a nil commit message"
               (let [stub-commit-response {:status 503}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response
                                                           expected-commit-url stub-commit-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id) =>
                 {:build {:repository {:id repository-id}
                          :sourceVersion source-version}
                  :previous-build {:some_previous_build_key "some_value"}
                  :commit-message nil}))
         (fact "when commit response cannot be parsed as JSON, returns builds with a nil commit message"
               (let [stub-commit-response {:status 200 :body "~~INVALID_JSON~~"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-response
                                                           expected-commit-url stub-commit-response})
                     vso-api (api/vso-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id) =>
                 {:build {:repository {:id repository-id}
                          :sourceVersion source-version}
                  :previous-build {:some_previous_build_key "some_value"}
                  :commit-message nil}))))
