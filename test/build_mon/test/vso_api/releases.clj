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
         (fact "calls VSO api and extracts release definitions"
               (let [a-release-definition {:a-release-definition "some-value"}
                     another-release-definition {:another-release-definition "some-other-value"}
                     stub-response-body {:count 2 :value [a-release-definition another-release-definition]}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response-body})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-definitions api-fns)) => [a-release-definition another-release-definition]))
         (fact "when get-fn throws exception, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (fn [url] (throw (Exception. "some exception")))
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-definitions api-fns)) => nil))))

(facts "retrieve-release-info for a release definition"
       (let [release-definition-id 666
             expected-releases-url (str "https://" account ".vsrm.visualstudio.com/defaultcollection/" project
                                        "/_apis/release/releases?api-version=3.0-preview.2&$top=2&definitionId="
                                        release-definition-id)]
         (fact "calls VSO api for last two releases, then for the individual releases and extracts release info"
               (let [release-url "https://SOME_RELEASE.URL"
                     previous-release-url "https://SOME_OTHER_RELEASE.URL"
                     stub-releases-response {:count 2 :value [{:_links {:self {:href release-url}}}
                                                              {:_links {:self {:href previous-release-url}}}]}
                     a-release {:a 1}
                     a-previous-release {:b 2}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url a-release
                                                           previous-release-url a-previous-release})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release a-release
                     :previous-release a-previous-release}))
         (fact "when there is no previous release, returns current release and nil previous release"
               (let [release-url "https://SOME_RELEASE.URL"
                     stub-releases-response {:count 1 :value [{:_links {:self {:href release-url}}}]}
                     a-release {:a 1}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response
                                                           release-url a-release})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release {:a 1} :previous-release nil}))
         (fact "when there are no releases, returns nil current and previous release"
               (let [stub-releases-response {:count 0 :value []}
                     get-fn-stubbed (get-fn-stub-requests {expected-releases-url stub-releases-response})
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id)
                 => {:release nil :previous-release nil}))
         (fact "when get-fn throws exception, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (fn [url] (throw (Exception. "some exception")))
                     api-fns (api/vso-release-api-fns logger get-fn-stubbed account project)]
                 ((:retrieve-release-info api-fns) release-definition-id) => nil))))
