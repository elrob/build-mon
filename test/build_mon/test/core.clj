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
            required-favicon-paths (map c/get-favicon-path c/states)
            required-favicon-filenames (map #(.substring % 1) required-favicon-paths)]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about generating build monitor html"
       (let [status-text "inProgress"
             state "in-progress-after-failed"
             build-definition-id "10"
             build-definition-name "My CI Build"
             build-number "2015.12.17.04"
             commit-message "great commit"
             favicon-path "/favicon_in-progress-after-failed.ico"
             build-info {:status-text status-text
                         :state state
                         :build-definition-id "10"
                         :build-definition-name build-definition-name
                         :build-number build-number
                         :commit-message commit-message
                         :favicon-path favicon-path}
             html-string (c/generate-build-monitor-html build-info :anything)]
         (fact "build status is displayed"
               html-string => (contains (str "<h1 class=\"status\">" status-text "</h1>")))
         (fact "favicon is included"
               html-string => (contains (str  "<link href=\"" favicon-path "\"")))
         (fact "build-panel has state as a css-class"
               html-string => (contains (str "div class=\"build-panel " state)))
         (fact "build-panel has build definition id in css-id"
               html-string => (contains (str "id=\"build-definition-id-" build-definition-id)))
         (fact "build definition name is displayed"
               html-string => (contains build-definition-name))
         (fact "build number is displayed"
               html-string => (contains build-number))
         (fact "commit message is displayed"
               html-string => (contains commit-message)))

       (facts "with refresh info"
              (let [refresh-info {:refresh-interval 60 :build-definition-ids ["10"]}
                    html-string (c/generate-build-monitor-html :anything refresh-info)]
                (fact "buildDefinitionIds value is set"
                      html-string => (contains "window.buildDefinitionIds = [\"10\"];"))
                (fact "refreshSeconds value is set"
                      html-string => (contains "window.refreshSeconds = 60;"))
                (fact "refresh.js is included"
                      html-string => (contains "src=\"/refresh.js\""))
                (fact "font awesome is included"
                      html-string => (contains "font-awesome"))))

       (facts "without refresh info"
              (let [html-string (c/generate-build-monitor-html :anything nil)]
                (fact "refresh script is not included"
                    html-string =not=> (contains "script"))
                (fact "font awesome is not included"
                    html-string =not=> (contains "font-awesome")))))

(facts "about generating-build-info"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04" :definition {:name "My CI Build" :id "10"}}]
         (c/generate-build-info build succeeded-build "great commit")
         => {:build-definition-name "My CI Build"
             :build-definition-id "10"
             :build-number "2015.12.17.04"
             :commit-message "great commit"
             :status-text "succeeded"
             :state "succeeded"
             :favicon-path "/favicon_succeeded.ico"})

       (tabular
         (fact "correct status-text, state and favicon-path are set based on current and previous build"
               (let [build-info (c/generate-build-info ?build ?previous "commit message")]
                 (:status-text build-info) => ?status-text
                 (:state build-info) => ?state
                 (:favicon-path build-info) => (str "/favicon_" ?state ".ico")))
         ?build             ?previous         ?status-text   ?state
         succeeded-build    :any              "succeeded"    "succeeded"
         failed-build       :any              "failed"       "failed"
         in-progress-build  succeeded-build   "inProgress"   "in-progress"
         in-progress-build  failed-build      "inProgress"   "in-progress-after-failed"))

(facts "about generating index html"
       (let [build-info-maps [{:build-definition-name "BD1"
                               :build-definition-id "10"
                               :build-number "2015.12.23.03"
                               :commit-message "change things"
                               :status-text "succeeded"
                               :state "succeeded"
                               :favicon-path "/favicon_succeeded.ico"}
                              {:build-definition-name "BD2"
                               :build-definition-id "20"
                               :build-number "403"
                               :commit-message "break things"
                               :status-text "failed"
                               :state "failed"
                               :favicon-path "/favicon_failed.ico"}]
             html (c/generate-index-html build-info-maps)]
         html => (contains "<title>")
         html => (contains "style.css")
         (fact "body includes a panel-count class"
              html => (contains "<body class=\"panel-count-2\""))
         (fact "includes ids of build definitions"
               html => (contains "build-definition-id-10")
               html => (contains "build-definition-id-20"))
         (fact "includes names of build definitions"
               html => (contains "BD1")
               html => (contains "BD2"))
         (fact "included commit messages from build definitions"
               html => (contains "change things")
               html => (contains "break things"))
         (fact "includes links to monitor each build definition"
               html => (contains "href=\"/build-definitions/10\"")
               html => (contains "href=\"/build-definitions/20\"")))

       (fact "body includes a panel-count class with the correct number of build definitions"
            (c/generate-index-html [1]) => (contains "panel-count-1")
            (c/generate-index-html [1 2]) => (contains "panel-count-2")
            (c/generate-index-html [1 2 3]) => (contains "panel-count-3")
            (c/generate-index-html [1 2 3 4]) => (contains "panel-count-4")))
