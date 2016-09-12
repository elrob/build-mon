(ns build-mon.test.core
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.core :as c]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(facts "get-status-text"
       (c/get-status-text succeeded-build) => "succeeded"
       (c/get-status-text failed-build) => "failed"
       (c/get-status-text in-progress-build) => "inProgress")

(fact "there are no missing favicons"
      (let [filenames-in-public-directory (map str (.list (io/file (io/resource "public"))))
            required-favicon-paths (map c/construct-favicon-path c/states-ordered-worst-first)
            required-favicon-filenames (map #(.substring % 1) required-favicon-paths)]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about get-favicon-path"
       (let [succeeded {:state :succeeded}
             in-progress {:state :in-progress}
             in-progress-after-failed {:state :in-progress-after-failed}
             failed {:state :failed}
             succeeded-favicon "/favicon_succeeded.ico"
             in-progress-favicon "/favicon_in-progress.ico"
             in-progress-after-failed-favicon "/favicon_in-progress-after-failed.ico"
             failed-favicon "/favicon_failed.ico"
             ; TODO
             release-info-maps {}]
         (tabular
           (fact "worst state is used for favicon"
                 (c/get-favicon-path ?build-info-maps release-info-maps) => ?favicon-path)
           ?build-info-maps                                        ?favicon-path
           [succeeded]                                             succeeded-favicon
           [in-progress]                                           in-progress-favicon
           [in-progress-after-failed]                              in-progress-after-failed-favicon
           [failed]                                                failed-favicon
           [succeeded in-progress]                                 in-progress-favicon
           [in-progress succeeded]                                 in-progress-favicon
           [in-progress in-progress-after-failed]                  in-progress-after-failed-favicon
           [in-progress-after-failed failed]                       failed-favicon
           [succeeded in-progress in-progress-after-failed failed] failed-favicon)))

(facts "about generating-build-info"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04" :definition {:name "My CI Build" :id 10}}]
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
