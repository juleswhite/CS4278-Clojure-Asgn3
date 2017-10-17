(ns engn-web.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [reagent.core :as reagent :refer [atom]]
            [reagent-material-ui.core :as ui]
            [engn-web.core :as engn-web]))


(def isClient (not (nil? (try (.-document js/window)
                              (catch js/Object e nil)))))

(def rflush reagent/flush)

(defn add-test-div [name]
  (let [doc     js/document
        body    (.-body js/document)
        div     (.createElement doc "div")]
    (.appendChild body div)
    div))

(defn with-mounted-component [comp f]
  (when isClient
    (let [div (add-test-div "_testreagent")]
      (let [comp [ui/MuiThemeProvider comp]
            comp (reagent/render-component comp div #(f comp div))]
        (reagent/unmount-component-at-node div)
        (reagent/flush)
        (.removeChild (.-body js/document) div)))))


(defn found-in [re div]
  (let [res (.-innerHTML div)]
    (if (re-find re res)
      true
      (do (println "Not found: " res)
          false))))


(deftest test-msg
  (with-mounted-component (engn-web/message-component {:msg "Foo" :user {:name "Jules"}})
    (fn [c div]
      (is (found-in #"Jules" div))
      (is (found-in #"Foo" div)))))
