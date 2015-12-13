(ns build-mon.test.core
  (:require [clojure.test :refer :all]
            [build-mon.core :as c]))

(def succeeded-build {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build {:result "failed"})

(deftest determine-background-colour
  (is (= (c/determine-background-colour succeeded-build :anything) "green"))
  (is (= (c/determine-background-colour failed-build :anything) "red"))
  (is (= (c/determine-background-colour in-progress-build succeeded-build) "yellow"))
  (is (= (c/determine-background-colour in-progress-build failed-build) "orange")))

(deftest determine-status-text
  (is (= (c/determine-status-text succeeded-build) "succeeded"))
  (is (= (c/determine-status-text failed-build) "failed"))
  (is (= (c/determine-status-text in-progress-build) "inProgress")))

(deftest determine-refresh-rate
  (let [default-rate 20]
    (is (= (c/determine-refresh-rate {}) default-rate))
    (is (= (c/determine-refresh-rate {"refresh" "true"}) default-rate))
    (is (= (c/determine-refresh-rate {"refresh" "yes"}) default-rate))
    (is (= (c/determine-refresh-rate {"refresh" "false"}) nil))
    (is (= (c/determine-refresh-rate {"refresh" "no"}) nil))
    (is (= (c/determine-refresh-rate {"refresh" "30"}) 30))
    (is (= (c/determine-refresh-rate {"refresh" "rubbish"}) nil))))
