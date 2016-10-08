(ns build-mon.test.vso-api.builds
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [build-mon.vso-api.builds :as api]))

(def account "VSO_ACCOUNT_NAME")
(def project "VSO_PROJECT_NAME")

(fact "vso-api-fns returns a map of the exposed functions"
      (let [get-fn (fn [url] "API RESPONSE")
            result (api/vso-api-fns get-fn account project)]
        (fn? (:retrieve-build-info result)) => truthy
        (fn? (:retrieve-build-definitions result)) => truthy))

(defn get-fn-stub-requests [stubbed-url->stubbed-response-map]
  (fn [url]
    (get stubbed-url->stubbed-response-map url
         {:URL :MISMATCH :url url :stubbed-urls (keys stubbed-url->stubbed-response-map)})))

(facts "retrieve-build-definitions"
       (let [expected-url (str "https://" account ".visualstudio.com/defaultcollection/" project
                               "/_apis/build/definitions?api-version=2.0")]
         (fact "calls VSO api for build defitions and returns the value of the response"
               (let [some-build-definitions [:some-build-definition :some-other-build-definition]
                     stub-response-body {:value some-build-definitions}
                     get-fn-stubbed (get-fn-stub-requests {expected-url stub-response-body})
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 ((:retrieve-build-definitions vso-api)) => some-build-definitions))
         (fact "when get-fn throws exception, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (fn [url] (throw (Exception. "some exception")))
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 (log/with-level :fatal ((:retrieve-build-definitions vso-api))) => nil))))

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
             some-build {:repository {:id repository-id} :sourceVersion source-version}
             some-previous-build {:some-previous-build-key :some-other-value}
             stub-build-response-body {:count 2 :value [some-build some-previous-build]}]
         (fact "calls VSO api for builds and commit message and extracts build info"
               (let [stub-commit-response-body {:comment "SOME COMMIT MESSAGE"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-build-response-body
                                                           expected-commit-url stub-commit-response-body})
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id)
                 => {:build some-build
                     :previous-build some-previous-build
                     :commit-message "SOME COMMIT MESSAGE"}))
         (fact "when there is no previous build, returns current build and a nil previous build"
               (let [stub-builds-body-with-one-build {:count 1 :value [some-build]}
                     stub-commit-response-body {:comment "SOME COMMIT MESSAGE"}
                     get-fn-stubbed (get-fn-stub-requests {expected-builds-url stub-builds-body-with-one-build
                                                           expected-commit-url stub-commit-response-body})
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 ((:retrieve-build-info vso-api) build-definition-id)
                 => {:build some-build
                     :previous-build nil
                     :commit-message "SOME COMMIT MESSAGE"}))
         (fact "when get-fn throws exception for builds request, returns nil"
               (let [stub-response {:status 503}
                     get-fn-stubbed (fn [url] (throw (Exception. "some exception")))
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 (log/with-level :fatal ((:retrieve-build-info vso-api) build-definition-id)) => nil))
         (fact "when get-fn throws exception for commit request, returns builds with a nil commit message"
               (let [stub-commit-response {:status 503}
                     get-fn-stubbed (fn [url] (if (= url expected-builds-url)
                                                stub-build-response-body
                                                (throw (Exception. "some exception"))))
                     vso-api (api/vso-api-fns get-fn-stubbed account project)]
                 (log/with-level :fatal ((:retrieve-build-info vso-api) build-definition-id)) =>
                 {:build some-build
                  :previous-build some-previous-build
                  :commit-message nil}))))
