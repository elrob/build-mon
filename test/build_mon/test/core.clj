(ns build-mon.test.core
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.core :as c]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(facts "status-text"
       (c/status-text succeeded-build) => "succeeded"
       (c/status-text failed-build) => "failed"
       (c/status-text in-progress-build) => "inProgress")

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
            required-favicon-paths (map c/favicon-path c/states)
            required-favicon-filenames (map #(.substring % 1) required-favicon-paths)]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about generating build monitor html"
       (tabular
         (fact "correct status is displayed, correct favicon is used, body has correct css class"
               (let [html-string (c/generate-build-monitor-html ?build ?previous :anything :anything)]
                 html-string => (contains (str "<h1 class=\"status\">" ?status-text "</h1>"))
                 html-string => (contains (str  "<link href=\"/favicon_" ?state ".ico\""))
                 html-string => (contains (str "body class=\"" ?state))))
         ?build             ?previous         ?status-text   ?state
         succeeded-build    :anything         "succeeded"    "succeeded"
         failed-build       :anything         "failed"       "failed"
         in-progress-build  succeeded-build   "inProgress"   "in-progress"
         in-progress-build  failed-build      "inProgress"   "in-progress-after-failed")

       (let [build {:result "succeeded" :buildNumber "2015.12.17.04" :definition {:name "My CI Build"}}
             refresh-info {:refresh-path "/build-definitions/10" :refresh-interval 60}
             successful-build-html (c/generate-build-monitor-html build succeeded-build "great commit" refresh-info)]
         (fact "build definition name is displayed"
               successful-build-html => (contains "My CI Build"))
         (fact "build number is displayed"
               successful-build-html => (contains "2015.12.17.04"))
         (fact "commit message is displayed"
               successful-build-html => (contains "great commit"))
         (fact "refreshPath value is set"
               successful-build-html => (contains "<script>window.refreshPath = \"/build-definitions/10\";"))
         (fact "refreshSeconds value is set"
               successful-build-html => (contains "window.refreshSeconds = 60;"))
         (fact "refresh.js is included"
               successful-build-html => (contains "src=\"/refresh.js\"")))

       (fact "refresh script is not included if refresh info is nil"
             (c/generate-build-monitor-html succeeded-build succeeded-build "commit" :anything nil)
             =not=> (contains "script")))

(facts "about generating index html"
       (let [html (c/generate-index-html [{:name "BD1" :id 10} {:name "BD2" :id 20}])]
         html => (contains "<title>")
         html => (contains "<body>")
         (fact "includes names of build definitions"
               html => (contains "BD1")
               html => (contains "BD2"))
         (fact "includes links to monitor each build definition"
               html => (contains "href=\"/build-definitions/10\"")
               html => (contains "href=\"/build-definitions/20\""))))
