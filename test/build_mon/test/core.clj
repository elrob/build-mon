(ns build-mon.test.core
  (:require [clojure.test :refer :all]
            [build-mon.core :as c]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(deftest determine-background-colour
  (is (=  "green"  (c/determine-background-colour succeeded-build :anything)))
  (is (=  "red"    (c/determine-background-colour failed-build :anything)))
  (is (=  "yellow" (c/determine-background-colour in-progress-build succeeded-build)))
  (is (=  "orange" (c/determine-background-colour in-progress-build failed-build))))

(deftest determine-status-text
  (is (= "succeeded"  (c/determine-status-text succeeded-build)))
  (is (= "failed"     (c/determine-status-text failed-build)))
  (is (= "inProgress" (c/determine-status-text in-progress-build))))

(deftest determine-build-author
  (is (= "Bob" (c/determine-build-author {:requestedFor {:displayName "Bob"}})))
  (is (= "N/A" (c/determine-build-author {}))))

(deftest determine-text
  (let [build {:buildNumber "123"
               :result "succeeded"
               :requestedFor {:displayName "Bob"}}]
  (is (= "123 – Bob – succeeded" (c/determine-text build)))))

(deftest determine-refresh-rate
  (let [default-rate 20]
    (are [params               expected] (= expected (c/determine-refresh-rate params))
         {}                    default-rate
         {"refresh" "true"}    default-rate
         {"refresh" "yes"}     default-rate
         {"refresh" "30"}      30
         {"refresh" "false"}   nil
         {"refresh" "no"}      nil
         {"refresh" "rubbish"} nil)))
