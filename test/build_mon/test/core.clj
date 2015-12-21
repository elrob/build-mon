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
            required-favicon-filenames (map c/favicon-filename c/states)]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about generating html"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04"}
             successful-build-html (c/generate-html build succeeded-build "great commit" 20)]
         (fact "build number is displayed"
               successful-build-html => (contains "2015.12.17.04"))
         (fact "commit message is displayed"
               successful-build-html => (contains "great commit"))
         (fact "favicon filename is included in link tag"
               successful-build-html => (contains "<link href=\"favicon_succeeded.ico\"")))

       (tabular
         (fact "correct status is displayed, correct favicon is used, body has correct css class"
               (let [html-string (c/generate-html ?build ?previous :anything :anything)]
                 html-string => (contains (str "<h1 class=\"status\">" ?status-text "</h1>"))
                 html-string => (contains (str  "<link href=\"favicon_" ?state ".ico\""))
                 html-string => (contains (str "body class=\"" ?state))))
         ?build             ?previous         ?status-text   ?state
         succeeded-build    :anything         "succeeded"    "succeeded"
         failed-build       :anything         "failed"       "failed"
         in-progress-build  succeeded-build   "inProgress"   "in-progress"
         in-progress-build  failed-build      "inProgress"   "in-progress-after-failed")

       (fact "refresh html script tags are generated when refresh value is passed"
             (c/generate-html succeeded-build succeeded-build "commit" 20) => (contains "refreshSeconds = 20")
             (c/generate-html succeeded-build succeeded-build "commit" 60) => (contains "refreshSeconds = 60"))
       (fact "refresh line is not generated if nil refresh value is passed"
             (c/generate-html succeeded-build succeeded-build "commit "nil) =not=> (contains "script")))
