(ns build-mon.test.builds
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [build-mon.builds :as b]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(facts "about generating-build-info"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04"
                    :definition {:name "My CI Build" :id 10}}]
         (b/generate-build-info build succeeded-build "great commit")
         => {:build-definition-name "My CI Build"
             :build-definition-id 10
             :build-number "2015.12.17.04"
             :commit-message "great commit"
             :state :succeeded})

       (tabular
        (fact "returns correct state based on current and previous build"
              (let [build-info (b/generate-build-info ?build ?previous "commit message")]
                (:state build-info) => ?state))
        ?build             ?previous         ?state
        succeeded-build    :any              :succeeded
        failed-build       :any              :failed
        in-progress-build  succeeded-build   :in-progress
        in-progress-build  failed-build      :in-progress-after-failed))
