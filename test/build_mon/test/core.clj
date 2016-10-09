(ns build-mon.test.core
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [build-mon.core :as c]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(facts "get-status-text"
       (c/get-status-text succeeded-build) => "succeeded"
       (c/get-status-text failed-build) => "failed"
       (c/get-status-text in-progress-build) => "inProgress")

(facts "about generating-build-info"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04"
                    :definition {:name "My CI Build" :id 10}}]
         (c/generate-build-info build succeeded-build "great commit")
         => {:build-definition-name "My CI Build"
             :build-definition-id 10
             :build-number "2015.12.17.04"
             :commit-message "great commit"
             :status-text "succeeded"
             :state :succeeded})

       (tabular
        (fact "correct status-text and state are set based on current and previous build"
              (let [build-info (c/generate-build-info ?build ?previous "commit message")]
                (:status-text build-info) => ?status-text
                (:state build-info) => ?state))
        ?build             ?previous         ?status-text   ?state
        succeeded-build    :any              "succeeded"    :succeeded
        failed-build       :any              "failed"       :failed
        in-progress-build  succeeded-build   "inProgress"   :in-progress
        in-progress-build  failed-build      "inProgress"   :in-progress-after-failed))
