(ns build-mon.test.vso-api
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api :as api]))

(def account "VSO_ACCOUNT_NAME")
(def project "VSO_PROJECT_NAME")
(def token "VSO_API_TOKEN")

(fact "vso-api-get-fn wraps an api token and returns a function which makes a HTTP GET request on a url"
      (let [result (api/vso-api-get-fn token)]
        (fn? result) => truthy
        (result "NOT A REAL URL") => (throws java.net.MalformedURLException)))

(fact "vso-api-fns returns a map of the exposed functions"
      (let [get-fn (fn [url] "API RESPONSE")
            result (api/vso-api-fns get-fn account project)]
        (fn? (:retrieve-build-info result)) => truthy
        (fn? (:retrieve-build-definitions result)) => truthy))

(defn get-fn-stub-request [stubbed-url response]
  (fn [url]
    (when (= stubbed-url url) response)))

(facts "retrieve-build-definitions"
       (let [expected-url "https://VSO_ACCOUNT_NAME.visualstudio.com/defaultcollection/VSO_PROJECT_NAME/_apis/build/definitions?api-version=2.0"]
         (fact "calls VSO api, parses response and extracts build definitions"
               (let [stub-json-body "{\"count\":2,\"value\":[{\"A_BUILD_DEFINITION_KEY\": \"A_VALUE\"},
                                    {\"ANOTHER_BUILD_DEFINITION_KEY\": \"ANOTHER_VALUE\"}]}"
                     stub-response {:status 200 :body stub-json-body}
                     get-fn-stubbed (get-fn-stub-request expected-url stub-response)
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => [{:A_BUILD_DEFINITION_KEY "A_VALUE"}
                                                             {:ANOTHER_BUILD_DEFINITION_KEY "ANOTHER_VALUE"}]))
         (fact "when response status is not 200, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (get-fn-stub-request expected-url stub-response)
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]

                 ((:retrieve-build-definitions vso-api)) => nil))
         (fact "when response body cannot be parsed as JSON, returns nil"
               (let [stub-response {:status 200 :body "~~INVALID_JSON~~"}
                     get-fn-stubbed (get-fn-stub-request expected-url stub-response)
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => nil))))


