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
                      html-string (html/generate-build-monitor-html single-build :anything "/FAVICON_PATH.ico")]
                  (fact "title is included"
                        html-string => (contains "<title>"))
                  (fact "stylesheet is included"
                        html-string => (contains "style.css"))
                  (fact "build status is displayed"
                        html-string => (contains "<h1 class=\"status\">succeeded</h1>"))
                  (fact "favicon is included"
                        html-string => (contains "<link href=\"/FAVICON_PATH.ico\""))
                  (fact "build-panel has state as a css-class"
                        html-string => (contains "div class=\"build-panel succeeded"))
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
                      html-string (html/generate-build-monitor-html two-builds :anything :anything)]
                  (fact "body includes a panel-count class"
                        html-string => (contains "<body class=\"panel-count-2\""))
                  (fact "includes ids of build definitions"
                        html-string => (contains "build-definition-id-10")
                        html-string => (contains "build-definition-id-20"))
                  (fact "includes names of build definitions"
                        html-string => (contains "BD1")
                        html-string => (contains "BD2"))
                  (fact "included commit messages from build definitions"
                        html-string => (contains "change things")
                        html-string => (contains "break things"))
                  (fact "includes links to monitor each build definition"
                        html-string => (contains "href=\"/build-definitions/10\"")
                        html-string => (contains "href=\"/build-definitions/20\"")))))

       (fact "body includes a panel-count class with the correct number of build definitions"
             (let [b {:state :succeeded}]
               (html/generate-build-monitor-html [b] :anything :anything) => (contains "panel-count-1")
               (html/generate-build-monitor-html [b b] :anything :anything) => (contains "panel-count-2")
               (html/generate-build-monitor-html [b b b] :anything :anything) => (contains "panel-count-3")
               (html/generate-build-monitor-html [b b b b] :anything :anything) => (contains "panel-count-4")))

       (facts "with refresh info"
              (let [b {:state :succeeded}
                    refresh-info {:refresh-interval 60 :build-definition-ids [10 20]}
                    html-string (html/generate-build-monitor-html [b] refresh-info :anything)]
                (fact "buildDefinitionIds value is set"
                      html-string => (contains "window.buildDefinitionIds = [10,20];"))
                (fact "refreshSeconds value is set"
                      html-string => (contains "window.refreshSeconds = 60;"))
                (fact "refresh.js is included"
                      html-string => (contains "src=\"/refresh.js\""))
                (fact "font awesome is included"
                      html-string => (contains "font-awesome"))))

       (facts "without refresh info"
              (let [html-string (html/generate-build-monitor-html [{:state :succeeded}] nil :anything)]
                (fact "refresh script is not included"
                      html-string =not=> (contains "script"))
                (fact "font awesome is not included"
                      html-string =not=> (contains "font-awesome")))))
