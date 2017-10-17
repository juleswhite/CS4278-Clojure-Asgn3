(ns engn-web.channels-test
  (:require [clojure.test :refer :all]
            [engn-web.local-messaging :refer :all]))

(deftest test-messages-add
  (testing "Correct addition of messages to a channel"
    (is (= {"a" [{:msg "1"}]} (messages-add {} "a" {:msg "1"})))
    (is (= {"a" [{:msg "3"} {:msg "4"} {:msg "2"}]}
           (messages-add {"a" [{:msg "4"} {:msg "2"}]} "a" {:msg "3"})))
    (is (= {"a" [{:msg "b1"} {:msg "b2"}]
            "b" [{:msg "c2"}]}
           (-> {}
               (messages-add "a" {:msg "b2"})
               (messages-add "b" {:msg "c2"})
               (messages-add "a" {:msg "b1"}))))))


(deftest test-messages-get
   (testing "Correct retrieval of messages for a channel"
     (let [msgs {:a [{:msg "3"} {:msg "2"} {:msg "1"}]
                 :b [{:msg "bar"}]
                 :c []
                 :d (repeatedly 100 (fn [] {:msg (str (rand-int 10))}))}]
      (is (= [{:msg "3"} {:msg "2"} {:msg "1"}] (messages-get msgs :a)))
      (is (= [{:msg "bar"}] (messages-get msgs :b)))
      (is (= [] (messages-get msgs :c)))
      (is (= 100 (count (messages-get msgs :d)))))))
