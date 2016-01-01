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

(tabular
    (facts "refresh-interval"
           (c/refresh-interval ?params) => ?expected)
    ?params               ?expected
    {"refresh" nil}       c/default-refresh-interval
    {"refresh" "30"}      30
    {"refresh" "5"}       c/minimum-refresh-interval
    {"refresh" "4"}       c/minimum-refresh-interval
    {"refresh" "1"}       c/minimum-refresh-interval
    {"refresh" "notInt"}  nil
    {"refresh" "0.5"}     nil)

(fact "there are no missing favicons"
      (let [filenames-in-public-directory (map str (.list (io/file (io/resource "public"))))
            required-favicon-paths (map c/get-favicon-path c/states-ordered-worst-first)
            required-favicon-filenames (map #(.substring % 1) required-favicon-paths)]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about generating-build-info"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04" :definition {:name "My CI Build" :id 10}}]
         (c/generate-build-info build succeeded-build "great commit")
         => {:build-definition-name "My CI Build"
             :build-definition-id 10
             :build-number "2015.12.17.04"
             :commit-message "great commit"
             :status-text "succeeded"
             :state :succeeded
             :favicon-path "/favicon_succeeded.ico"})

       (tabular
         (fact "correct status-text, state and favicon-path are set based on current and previous build"
               (let [build-info (c/generate-build-info ?build ?previous "commit message")]
                 (:status-text build-info) => ?status-text
                 (:state build-info) => ?state
                 (:favicon-path build-info) => (str "/favicon_" (name ?state) ".ico")))
         ?build             ?previous         ?status-text   ?state
         succeeded-build    :any              "succeeded"    :succeeded
         failed-build       :any              "failed"       :failed
         in-progress-build  succeeded-build   "inProgress"   :in-progress
         in-progress-build  failed-build      "inProgress"   :in-progress-after-failed))

(facts "about generating build monitor html"
       (let [succeeded-build-info {:build-definition-name "BD1"
                                   :build-definition-id 10
                                   :build-number "2015.12.23.03"
                                   :commit-message "change things"
                                   :status-text "succeeded"
                                   :state :succeeded
                                   :favicon-path "/favicon_succeeded.ico"}
             failed-build-info {:build-definition-name "BD2"
                                :build-definition-id 20
                                :build-number "403"
                                :commit-message "break things"
                                :status-text "failed"
                                :state :failed
                                :favicon-path "/favicon_failed.ico"}]
         (facts "for a single build"
                (let [single-build [succeeded-build-info]
                      html-string (c/generate-build-monitor-html single-build :anything)]
                  (fact "title is included"
                        html-string => (contains "<title>"))
                  (fact "stylesheet is included"
                        html-string => (contains "style.css"))
                  (fact "build status is displayed"
                        html-string => (contains "<h1 class=\"status\">succeeded</h1>"))
                  (fact "favicon is included"
                        html-string => (contains "<link href=\"/favicon_succeeded.ico\""))
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
                      html-string (c/generate-build-monitor-html two-builds :anything)]
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

       (let [s {:state :succeeded}
             ip {:state :in-progress}
             ipaf {:state :in-progress-after-failed}
             f {:state :failed}
             s-favicon "/favicon_succeeded.ico"
             ip-favicon "/favicon_in-progress.ico"
             ipaf-favicon "/favicon_in-progress-after-failed.ico"
             f-favicon "/favicon_failed.ico"]
         (tabular
           (fact "worst state is used for favicon"
                 (c/generate-build-monitor-html ?build-info-maps :anything) => (contains ?favicon-path))
           ?build-info-maps     ?favicon-path
           [s]                  s-favicon
           [ip]                 ip-favicon
           [ipaf]               ipaf-favicon
           [f]                  f-favicon
           [s ip]               ip-favicon
           [ip s]               ip-favicon
           [ip ipaf]            ipaf-favicon
           [ipaf f]             f-favicon
           [s ip ipaf f]        f-favicon))

       (fact "body includes a panel-count class with the correct number of build definitions"
             (let [b {:state :succeeded}]
               (c/generate-build-monitor-html [b] :anything) => (contains "panel-count-1")
               (c/generate-build-monitor-html [b b] :anything) => (contains "panel-count-2")
               (c/generate-build-monitor-html [b b b] :anything) => (contains "panel-count-3")
               (c/generate-build-monitor-html [b b b b] :anything) => (contains "panel-count-4")))

       (facts "with refresh info"
              (let [b {:state :succeeded}
                    refresh-info {:refresh-interval 60 :build-definition-ids [10 20]}
                    html-string (c/generate-build-monitor-html [b] refresh-info)]
                (fact "buildDefinitionIds value is set"
                      html-string => (contains "window.buildDefinitionIds = [10,20];"))
                (fact "refreshSeconds value is set"
                      html-string => (contains "window.refreshSeconds = 60;"))
                (fact "refresh.js is included"
                      html-string => (contains "src=\"/refresh.js\""))
                (fact "font awesome is included"
                      html-string => (contains "font-awesome"))))

       (facts "without refresh info"
              (let [html-string (c/generate-build-monitor-html [{:state :succeeded}] nil)]
                (fact "refresh script is not included"
                      html-string =not=> (contains "script"))
                (fact "font awesome is not included"
                      html-string =not=> (contains "font-awesome")))))
