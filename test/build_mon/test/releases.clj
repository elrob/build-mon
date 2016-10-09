(ns build-mon.test.releases
  (:require [midje.sweet :refer :all]
            [build-mon.releases :as r]))

(def succeeded-env   {:status "succeeded" :name "ENV_NAME"})
(def in-progress-env {:status "inProgress" :name "ENV_NAME"})
(def not-started-env {:status "notStarted" :name "ENV_NAME"})
(def failed-env {:status "failed" :name "ENV_NAME"})

(facts "about generating-release-info"
       (let [release {:releaseDefinition {:name "some definition name"}
                      :id 666 :name "some release name"
                      :environments [{:name "env name" :status "succeeded"}
                                     {:name "another env name" :status "inProgress"}]}
             previous-release {:releaseDefinition {:name "some definition name"}
                               :id 666 :name "some release name"
                               :environments [{:name "env name" :status "succeeded"}
                                              {:name "another env name" :status "succeeded"}
                                              {:name "a third env" :status "succeeded"}]}]
         (r/generate-release-info release previous-release)
         => {:release-definition-name "some definition name"
             :release-definition-id 666
             :release-number "some release name"
             :release-environments [{:env-name "env name" :state :succeeded}
                                    {:env-name "another env name" :state :in-progress}]})

       (tabular
        (fact "returns correct state based on current and previous build"
              (let [release {:environments [?release-env]}
                    previous-release {:environments [?previous-release-env]}
                    release-info (r/generate-release-info release previous-release)]
                (-> release-info :release-environments first :state) => ?state))
        ?release-env     ?previous-release-env ?state
        succeeded-env    :any                  :succeeded
        failed-env       :any                  :failed
        in-progress-env  succeeded-env         :in-progress
        in-progress-env  failed-env            :in-progress-after-failed
        not-started-env  :any                  :not-started))
