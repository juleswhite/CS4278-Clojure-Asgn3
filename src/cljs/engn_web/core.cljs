(ns engn-web.core
  "
  This module is a bit of a mess, which we will use to motivate some of the
  things that we will learn in the next few weeks.
  "
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [cljs-time.core :as time]
              [cljs-time.format :as time-format]
              [cljs-time.coerce :as time-coerce]
              [clojure.string :as string]
              [reagent-material-ui.core :as ui]
              [ajax.core :refer [GET POST]]
              [engn-web.local-messaging :as messaging]))


;; ==========================================================================
;; Utility functions
;; ==========================================================================

(enable-console-print!)

(defn log
  "Log a message to the Javascript console"
  [& msg]
  (.log js/console (apply str msg)))

(defn error-handler
  "Error handler for AJAX calls to print out errors to the console"
  [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(def el reagent/as-element)
(defn icon-span [nme] [ui/FontIcon {:className "material-icons"} nme])
(defn icon [nme] (el [:i.material-icons nme]))
(defn color [nme] (aget ui/colors nme))
(defn scroll-window-to-bottom []
  (.setTimeout js/window #(.scrollTo js/window 0 (+ 0 (.-scrollHeight (.-body js/document)))) 100))


;; ==========================================================================
;; App State
;;
;; This is messy right now, which will motivate something we learn
;; later in class
;; ==========================================================================

(defonce messaging-state (atom (messaging/messages-initial-state)))

(defonce channels (atom []))
(defonce msgs (atom []))
(defonce msg-entry (atom ""))
(defonce search-entry (atom ""))
(defonce chnl-entry (atom ""))
(defonce current-channel (atom ""))
(defonce scroll-msgs (atom false))
(defonce nav-open? (atom true))
(defonce add-channel-dialog-open? (atom false))
(defonce user {:name "You" :nickname "Your Name"})


;; ==========================================================================
;; Functions to send / receive messages and list channels
;;
;; This is messy right now, which will motivate something we learn
;; later in class
;; ==========================================================================

(defn message-create [msg]
  {:msg msg
   :user {:name "Your Name" :nickname "You"}
   :time (time/now)})

(defn messages-get! [channel]
  (reset! msgs (messaging/messages-get @messaging-state channel)))

(defn messages-search! [search]
   (let [msgs-raw (messaging/messages-get @messaging-state @current-channel)]
     (if (not (string/blank? search))
         (reset! msgs (messaging/messages-search msgs-raw search))
         (reset! msgs msgs-raw))))

(defn open-channel! [channel]
   (reset! current-channel channel)
   (messages-get! channel)
   (scroll-window-to-bottom))

(defn list-channels! []
  (reset! channels (messaging/channels-list @messaging-state)))

(defn add-msg! []
  (let [channel @current-channel
        msg     (message-create @msg-entry)]
    (swap! messaging-state messaging/messages-add channel msg)
    (reset! msgs (messaging/messages-get @messaging-state channel))
    (reset! msg-entry "")
    (scroll-window-to-bottom)))

(defn add-channel! [channel]
  (swap! messaging-state messaging/channels-add channel)
  (reset! channels (messaging/channels-list @messaging-state))
  (open-channel! channel))

(list-channels!)
(open-channel! "default")

;; ==========================================================================
;; View components
;; ==========================================================================

(defn message [m]
  (let [name (:nickname (:user m))
        text (:msg m)
        class (if @nav-open? "message open" "message closed")
        formatter (time-format/formatter "MM/dd/yyyy hh:mm:ss")
        formatted-time (time-format/unparse formatter (time-coerce/from-long (:time m)))]
      [ui/Card {:class class}
       [ui/CardHeader {:title name
                       :subtitle formatted-time
                       :avatar "https://s.gravatar.com/avatar/a9edda10d0e6fb75561f057d167a9077?s=480&r=pg&d=https%3A%2F%2Fcdn.auth0.com%2Favatars%2Fyu.png";"http://placehold.it/50/55C1E7/fff&text=U"
                       :actAsExpander true
                       :showExpandableButton false}]
       [ui/CardText text]]))

(defn messages [ms]
  [:div {:style {:margin-bottom "100px"}}
    (for [msg ms]
      ^{:key msg} [message msg])])

(defn channel [c]
  [ui/ListItem {:leftIcon (icon "label_outline")
                ;:rightIcon [icon "chat"]
                :primaryText (str "#" c)
                :onTouchTap #(open-channel! c)}])


(defn channel-list []
  [ui/List
   [ui/Subheader "Channels"
    [ui/ListItem {:leftIcon (icon "add_circle_outline")
                  :primaryText "Add Channel"
                  :onTouchTap #(do (println @add-channel-dialog-open?)(reset! add-channel-dialog-open? true))}]
    (for [c @channels]
         ^{:key c} [channel c])]])

(defn channel-add-dialog []
  (let [hide-dialog #(reset! add-channel-dialog-open? false)]
    [ui/Dialog {:open @add-channel-dialog-open?
                :title "Tell me the channel to add."
                :actions [ (el [ui/FlatButton {:label "Cancel" :primary false :onTouchTap hide-dialog}])
                           (el [ui/FlatButton {:label "OK" :primary true :onTouchTap #(do
                                                                                        (add-channel! @chnl-entry)
                                                                                        (hide-dialog))}])]
                :modal false}
      [ui/TextField
           {:style {:width "400px" :margin-left "20px"}
            :floatingLabelText "Channel name"
            :floatingLabelFixed false
            :onChange #(reset! chnl-entry (-> % .-target .-value))}]]))

(defn simple-nav []
  (let [close #(reset! nav-open? false)]
    (fn []
      [:div#header
       [ui/AppBar {:title "chat engn" :onLeftIconButtonTouchTap #(reset! nav-open? true)}
        [ui/IconMenu {:iconButtonElement (el [ui/IconButton {:style {:color "#FFF"}} (icon "more_vert")])
                                         :targetOrigin {:horizontal "right" :vertical "top"}
                                         :anchorOrigin {:horizontal "right" :vertical "top"}}
          [ui/MenuItem {:primaryText "Sign out" :onTouchTap #(aset js/document "location" "/logout")}]]]
       [ui/Drawer {:open @nav-open?
                   :docked true
                   :style {:z-index 10001}
                   :containerStyle {:z-index 10001}}
        [ui/AppBar {:title "chat engn" :onLeftIconButtonTouchTap #(reset! nav-open? false)}]

        [ui/List
         [ui/ListItem {:disabled true}
             [ui/Avatar {:size 150 :src (:picture user) :style {:margin-left "30px"}}]]
         [ui/ListItem {:leftIcon (icon "person_outline")
                       :primaryText (:nickname user)}]
         [ui/ListItem {:leftIcon (icon "person_outline")
                       :primaryText (:name user)}]]
        [ui/List ;{:style {:background-color "#F9FBE7" :height "100%"}}
         [ui/Divider]
         [channel-list]]]])))

(defn messages-search []
  (let [class (if @nav-open? "message open" "message closed")]
    [ui/Card {:class class :style {:margin-bottom "20px" :margin-top "20px"}}
     [ui/CardHeader {:title ""
                     :showExpandableButton false}
       [ui/TextField
            {;:style {:width "800px" :margin-left "20px"}
             :floatingLabelText "I want to find..."
             :floatingLabelFixed false
             :onChange #(messages-search! (-> % .-target .-value))}]]]))
             ;:on-key-press (fn [e](if (= 13 (.-charCode e)) (add-msg!)))}]]]))

(defn main-page []
  (let [class (if @nav-open? "message open" "message closed")]
   [ui/MuiThemeProvider ;theme-defaults
    [:div
     [channel-add-dialog]
     [simple-nav]
     [:div {:style {:padding "10px 10px 10px 10px"}}
      (messages-search)
      [ui/Card {:class class}
        [ui/CardHeader {:title (str "#" @current-channel)}]]
      (messages (reverse @msgs))]
     [:footer {:class class}
       [ui/Toolbar {:style {:align "center"}}
         [ui/ToolbarGroup {:style {:width "70%"}}
           [:div {:style {:width "10px"}}]
           [ui/ToolbarSeparator]
           [ui/TextField
                {:style {:width "800px" :margin-left "20px"}
                 :floatingLabelText "What would you like to say..."
                 :floatingLabelFixed false
                 :onChange #(reset! msg-entry (-> % .-target .-value))
                 :value @msg-entry
                 :on-key-press (fn [e](if (= 13 (.-charCode e)) (add-msg!)))}]
           [ui/RaisedButton {:label "Send" :primary true :onTouchTap add-msg!}]]]]]]))



;; -------------------------
;; Routes

(def page (atom #'main-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'main-page))


;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
