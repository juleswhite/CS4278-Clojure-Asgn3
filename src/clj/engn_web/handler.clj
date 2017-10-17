(ns engn-web.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [config.core :refer [env]]
            [engn-web.middleware :refer [wrap-middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [ring.middleware.json :as json]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [engn-web.local-messaging :as messaging]))

;; ==========================================================================
;; Utility functions for serving up a JSON REST API
;; ==========================================================================

(def json-header
  "Utility function to set the appropriate headers in an
   HTTP response to return JSON to the client"
  {"Content-Type" "application/json"})

(defn json
  "Utility function to return JSON to the client"
  [data]
  {:status 200 :headers json-header :body data})

;; ==========================================================================
;; Functions to render the HTML for the single-page application
;; ==========================================================================

(def mount-target
  "This is the page that is displayed before figwheel
   compiles your application"
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler (this page may self-destruct)"]])

(defn head []
  "Function to generate the <head> tag in the main HTML page that is
   returned to the browser"
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://fonts.googleapis.com/icon?family=Material+Icons")
   (include-css "https://fonts.googleapis.com/css?family=Roboto:300,400,500")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn main-page
   "Generates the main HTML page for the single-page application"
   []
   (html5
     (head)
     [:body
       mount-target
       (include-js "/js/app.js")]))

;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

(defonce state (atom (messaging/messages-initial-state)))

(defn hello [name greeting]
  (json {:hello name :greeting greeting}))

;; Step 1.
;;
;; Add a handler function called "echo-handler" that takes a single
;; parameter, p,  and returns (json {:echo p}) as the result.
;;
;; Step 2.
;;
;; Add a route for this handler that takes a "msg" query parameter and invokes
;; your echo-handler. Set the path of your route to be /echo
;;
;; When you are done with steps 1 & 2, you should be able to point your
;; browser at http://localhost:3450/echo?msg=worked and see the echo. If
;; you are unfamiliar with query params, play around with changing the
;; "msg=worked" part so that msg is assigned different values.


(defn messages-add-handler [channel msgstr]
  (let [msg {:msg msgstr :user {:name "Someone" :nickname "Bob"}}]
    (swap! state messaging/messages-add channel msg)
    (json @state)))

;; Step 3.
;;
;; Look at the handler for message-add-handler above. Add handlers for the
;; following functions in local-messaging (required as messaging):
;;
;; 1. messages-get
;; 2. channels-list
;; 3. channels-add
;;
;; Each handler function that modifies the state of the application (e.g.,
;; adds a message or channel) should take the parameters needed by the corresponding
;; function in local-messaging and use swap! to apply the local-messaging function
;; to "state" (e.g., (swap! state some-function ....)).
;;
;;
;; Any function that reads a value from local-messaging (e.g.,)
;; listing channels or getting messages), should apply the function to
;; the value stored in "state" (e.g., (some-function @state ....))
;;
;; As you add each handler, also add a route for it in the routes definition
;; below and test that it works.
;;
;; Additional information on handlers is available here:
;; https://github.com/ring-clojure/ring/wiki/Concepts

;; ==========================================================================
;; Functions to setup the list of URI routes for the application
;; and setup the appropriate middleware wrappers
;; ==========================================================================

;; This section of the code defines how requests from your browser are routed
;; to function calls.
;;
;; Complete documentation on everything you can do with the routing is available
;; here: https://github.com/weavejester/compojure/wiki/Routes-In-Detail
;;
(defroutes routes
  (GET "/" request (main-page))

  ;; Parameters can be exatracted by a route in two ways;
  ;;
  ;; 1. Parameters can be in the path and denoted by ":" before them, such
  ;;    such as ":name" in the route below. Any request to /hello/<some string>,
  ;;    such as http://localhost:3450/hello/bob or http://localhost:3450/hello/jim
  ;;    will be routed here and the "name" will
  ;;
  ;; 2. Parameters can be passed in the query string, like "greeting" in the
  ;;    hello route. Query parameters can be sent from your browser by
  ;;    adding "?parameter=value" at the end of the URL, like this:
  ;;    http://localhost:3450/hello/paul?greeting=bonjour
  ;;
  (GET "/hello/:name" [name greeting] (hello name greeting))

  ;; You can test this route by pointing your browser at:
  ;; http://localhost:3450/channel/default/add-message?msg=hello
  ;;
  ;; This route takes a path parameter ":channel" and also extracts a query
  ;; parameter "msg"
  ;;
  (GET "/channel/:channel/add-message" [channel msg] (messages-add-handler channel msg))
  (resources "/")
  (not-found "Not Found"))


(def app (->  (wrap-middleware #'routes)
              (json/wrap-json-response)
              (json/wrap-json-params)
              wrap-params
              wrap-cookies))
