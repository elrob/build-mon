(ns build-mon.test.html
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.html :as html]))

(facts "about generating build monitor html"
       (let [succeeded-build-info {:build-definition-name "BD1"
                                   :build-definition-id 10
                                   :build-number "2015.12.23.03"
                                   :commit-message "change things"
                                   :status-text "succeeded"
                                   :state :succeeded}
             failed-build-info {:build-definition-name "BD2"
                                :build-definition-id 20
                                :build-number "403"
                                :commit-message "break things"
                                :status-text "failed"
                                :state :failed}]
         (facts "for a single build"
                (let [single-build [succeeded-build-info]
                      html-string (html/generate-build-monitor-html single-build [] "/FAVICON_PATH.ico")]
                  (fact "title is included"
                        html-string => (contains "<title>"))
                  (fact "stylesheet is included"
                        html-string => (contains "style.css"))
                  (fact "favicon is included"
                        html-string => (contains "<link href=\"/FAVICON_PATH.ico\""))
                  (fact "build-panel has state as a css-class"
                        html-string => (contains "div class=\"panel succeeded"))
                  (fact "build-panel has build definition id in css-id"
                        html-string => (contains "id=\"build-definition-id-10\""))
                  (fact "build definition name is displayed"
                        html-string => (contains "BD1"))
                  (fact "build number is displayed"
                        html-string => (contains "2015.12.23.03"))
                  (fact "commit message is displayed"
                        html-string => (contains "change things"))))
         (facts "for multiple builds"
                (let [two-builds [succeeded-build-info failed-build-info]
                      html-string (html/generate-build-monitor-html two-builds [] :anything)]
                  (fact "includes ids of build definitions"
                        html-string => (contains "build-definition-id-10")
                        html-string => (contains "build-definition-id-20"))
                  (fact "includes names of build definitions"
                        html-string => (contains "BD1")
                        html-string => (contains "BD2"))
                  (fact "included commit messages from build definitions"
                        html-string => (contains "change things")
                        html-string => (contains "break things"))))))
