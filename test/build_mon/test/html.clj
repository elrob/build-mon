(ns build-mon.test.html
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.html :as html]))

(facts "about generating build monitor html"
       (let [html-with-no-builds (html/generate-build-monitor-html [] [] "/FAVICON_PATH.ico")]
         (fact "title is included"
               html-with-no-builds => (contains "<title>"))
         (fact "stylesheet is included"
               html-with-no-builds => (contains "style.css"))
         (fact "favicon is included"
               html-with-no-builds => (contains "<link href=\"/FAVICON_PATH.ico\"")))

       (tabular
        (fact "generates correct panel dimensions for multiple builds"
              (html/generate-build-monitor-html (repeat ?builds :a-build) [] :anything) =>
              (contains ?panel-dimensions-style))
        ?builds ?panel-dimensions-style
        1       ".panel { width:100%; height:100%; }"
        2       ".panel { width:50%; height:100%; }"
        3       ".panel { width:33.3333%; height:100%; }"
        4       ".panel { width:25%; height:100%; }"
        5       ".panel { width:25%; height:50%; }"
        9       ".panel { width:25%; height:33.3333%; }")

       (let [release-with-one-env {:release-environments [:an-env]}
             release-with-two-envs {:release-environments [:an-env :an-env]}]
         (tabular
          (fact  "generates correct dimensions for multiple builds and releases"
                 (html/generate-build-monitor-html [:build] ?releases :anything) =>
                 (contains ?panel-dimensions-style))
          ?releases                ?panel-dimensions-style
          [release-with-one-env]   ".panel { width:50%; height:100%; }"
          [release-with-two-envs]  ".panel { width:33.3333%; height:100%; }"
          [release-with-one-env
           release-with-two-envs]  ".panel { width:25%; height:100%; }"))

       (let [succeeded-build-info {:build-definition-name "BD1" :build-definition-id 10
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
                  (fact "there is a build panel"
                        html-string => (contains "BUILD"))
                  (fact "build-panel has state as a css-class"
                        html-string => (contains "div class=\"panel succeeded"))
                  (fact "build-panel has build definition id in id"
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
                  (fact "there is a build panel per build"
                        (count (re-seq (re-pattern "BUILD") html-string)) => 2)
                  (fact "includes ids of build definitions"
                        html-string => (contains "build-definition-id-10")
                        html-string => (contains "build-definition-id-20"))
                  (fact "includes names of build definitions"
                        html-string => (contains "BD1")
                        html-string => (contains "BD2"))
                  (fact "included commit messages from build definitions"
                        html-string => (contains "change things")
                        html-string => (contains "break things")))))

       (let [release {:release-definition-name "some-definitionName"
                      :release-definition-id 123
                      :release-number "release xxx"
                      :release-environments [{:env-name "some-envName1" :state "some-state1"}
                                             {:env-name "some-envName2" :state "some-state2"}]}]
         (facts "for a single release with two environments"
                (let [single-release [release]
                      html-string (html/generate-build-monitor-html [] single-release "/FAVICON_PATH.ico")]
                  (fact "there is a release panel per environment"
                        (count (re-seq (re-pattern "RELEASE") html-string)) => 2)
                  (fact "release-panels have state as a css-class"
                        html-string => (and (contains "div class=\"panel some-state1")
                                            (contains "div class=\"panel some-state2")))
                  (fact "release-panels have ids"
                        html-string => (and (contains "id=\"release-definition-id-123-some-envName1")
                                            (contains "id=\"release-definition-id-123-some-envName2")))
                  (fact "release-panels display release definition name"
                        html-string => (contains "<h1 class=\"release-definition-name\">some-definitionName"))
                  (fact "release-panels display release number"
                        html-string => (contains "<h1 class=\"release-number\">release xxx"))
                  (fact "release-panels display release environment name"
                        html-string => (and (contains "<h1 class=\"release-env-name\">some-envName1")
                                            (contains "<h1 class=\"release-env-name\">some-envName2")))))
         (facts "for multiple releases"
                (let [two-releases-each-with-two-envs [release release]
                      html-string (html/generate-build-monitor-html [] two-releases-each-with-two-envs :anything)]
                  (fact "there is a release panel per environment"
                        (count (re-seq (re-pattern "RELEASE") html-string)) => 4)))))
