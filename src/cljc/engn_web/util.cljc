(ns engn-web.util)


(let [message-text (atom "foo")
      current-text @message-text]
     (reset! message-text "food")

     (println @message-text)
     (println current-text))


(println "foo")
