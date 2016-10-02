(ns build-mon.test.vso-api.releases
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.vso-api.releases :as api]))

(def account "VSO_ACCOUNT_NAME")
(def project "VSO_PROJECT_NAME")

(def logger {:log-exception (fn [message _] (println "Test logger:" message))})

(fact "vso-release-api-fns returns a map of the exposed functions"
      (let [get-fn (fn [url] "API RESPONSE")
            result (api/vso-release-api-fns logger get-fn account project)]
        (fn? (:retrieve-release-info result)) => truthy
        (fn? (:retrieve-release-definitions result)) => truthy))

(defn get-fn-stub-requests [stubbed-url->stubbed-response-map]
  (fn [url]
    (get stubbed-url->stubbed-response-map url
         {:URL :MISMATCH :url url :stubbed-urls (keys stubbed-url->stubbed-response-map)})))

(facts "retrieve-release-definitions"
       (let [expected-url (str "https://" account ".vsrm.visualstudio.com/defaultcollection/" project
                               "/_apis/release/definitions?api-version=3.0-preview.2")]
         (fact "calls VSO api, parses response and extracts release definitions"
               (let [stub-json-body "{\"count\":2,\"value\":[
                                    {\"A_RELEASE_DEFINITION_KEY\": \"A_VALUE\"},
                                    {\"ANOTHER_RELEASE_DEFINITION_KEY\": \"ANOTHER_VALUE\"}]}"
                     stub-response {:status 200 :body stub-json-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-definitions api-fns)) => [{:A_RELEASE_DEFINITION_KEY "A_VALUE"}
                                                               {:ANOTHER_RELEASE_DEFINITION_KEY "ANOTHER_VALUE"}]))
         (fact "when response status is not 200, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-definitions api-fns)) => nil))
         (fact "when response body cannot be parsed as JSON, returns nil"
               (let [stub-response {:status 200 :body "~~INVALID_JSON~~"}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-definitions api-fns)) => nil))))

(facts "retrieve-release-info for a release definition"
       (let [release-definition-id 666
             expected-releases-url (str "https://" account ".vsrm.visualstudio.com/defaultcollection/" project
                                        "/_apis/release/releases?api-version=3.0-preview.2&$top=2&definitionId="
                                        release-definition-id)]
         (fact "calls VSO api for releases, parses responses and extracts release info"
               (let [release-url "https://SOME_RELEASE.URL"
                     previous-release-url "https://SOME_OTHER_RELEASE.URL"
                     stub-releases-body (str "{\"count\":2,\"value\":["
                                             "{\"_links\":{\"self\":{\"href\": \"" release-url "\"}}},"
                                             "{\"_links\":{\"self\":{\"href\": \"" previous-release-url "\"}}}]}")
                     stub-releases-response {:status 200 :body stub-releases-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url {:status 200 :body "{\"a\": 1}"}
                                                           previous-release-url {:status 200 :body "{\"b\": 2}"}})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release {:a 1} :previous-release {:b 2}}))
         (fact "when there is no previous release, returns current release and nil previous release"
               (let [release-url "https://SOME_RELEASE.URL"
                     stub-releases-body (str "{\"count\":1,\"value\":["
                                             "{\"_links\":{\"self\":{\"href\": \"" release-url "\"}}}]}")
                     stub-releases-response {:status 200 :body stub-releases-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url {:status 200 :body "{\"a\": 1}"}})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release {:a 1} :previous-release nil}))
         (fact "when there are no releases, returns nil current and previous release"
               (let [stub-releases-body  "{\"count\":0,\"value\":[]}"
                     stub-releases-response {:status 200 :body stub-releases-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release nil :previous-release nil}))
         (fact "when releases response status is not 200, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id) => nil))
         (fact "when releases response cannot be parsed as JSON, returns nil"
                 (let [stub-response {:status 200 :body "~~INVALID_JSON~~"}
                       get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-response})
                       api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id) => nil))
         (fact "when any release response is not 200, returns nil"
               (let [release-url "https://SOME_RELEASE.URL"
                     stub-releases-body (str "{\"count\":1,\"value\":["
                                             "{\"_links\":{\"self\":{\"href\": \"" release-url "\"}}}]}")
                     stub-releases-response {:status 200 :body stub-releases-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url {:status 503}})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id) => nil))
         (fact "when any release response cannot be parsed as JSON, returns nil"
               (let [release-url "https://SOME_RELEASE.URL"
                     stub-releases-body (str "{\"count\":1,\"value\":["
                                             "{\"_links\":{\"self\":{\"href\": \"" release-url "\"}}}]}")
                     stub-releases-response {:status 200 :body stub-releases-body}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url {:status 200 :body "~~INVALID_JSON~~"}})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id) => nil))))
