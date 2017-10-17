(ns engn-web.local-messaging
  "Getting Started:

   Make sure that you have leiningnen installed and working per the dev env
   setup assignment. If you do not, please find someone next to you that has
   it working and follow along with them.

   Open a terminal and change to the directory with this project. The directory
   should have a project.clj file at the top level. Once you are in this
   directory, run 'lein figwheel'.

   If you have atom setup correctly, you can open a terminal by clicking the
   + button in the bottom left-hand corner.

   Wait for figwheel to finish starting. You will see a message like 'Prompt
   will show when Figwheel...' when it is done.

   When you see this message, open Google Chrome to http://localhost:3450

   Figwheel will automatically recompile your code and reload it into the
   browser without refreshing the page whenever you save a change to this
   file. You can make changes and then see *most* of them automatically appear
   in the browser without refreshing the page.

   In-class Exercise:
   -------------------

   Complete Steps 1-7 outlined in the doc strings and comments below. If you
   get stuck (most people will!), please ask for help.

   You must complete all of these steps before completing Programming assignment
   1.


   Programming Assignment 1:
   --------------------------

   In addition to completing all of the steps from the in-class exercise, do
   the following:

   1. Modify the code so that there is a function named 'user that returns a
      dictionary with your actual name and a nickname. This function may be
      used for grading, so make sure you accurately enter your name.
   2. Modify all of the other code to ensure that all of the messages that are
      from you have this user attached and not the default 'Your Name' user.
   3. Add a text expansion feature so that you can type special keywords that
      are fully expanded when displayed (e.g., vandy --> Vanderbilt University).
      Your text expansion feature should preserve the abbreviations and only
      expand them on read (e.g., the map returned from messages-add will not
      have the text expanded, but the list returned from messages-get will).
      Your expansions should be defined in a function named expansions that
      returns a map where the keys are the string to match and the values are
      the fully expanded strings to replace them with.

      You must use all of the following in your solution:

      a. map / reduce
      b. let
      c. a separate function, called messages-expand, that takes a msg map and
         a map of expansions and applies all of the expansions to the text
         stored in the msg map's :msg key and returns the updated msg with its
         :msg key set to the expanded text
      d. a separate function, called expansions, that defines the expansions
      e. an import for clojure.string with the name 'string'
      f. list and/or map destructuring

      Hints:

      a. use map to process all of the messages in a channel
      b. use reduce to apply all of the string expansions to a
         message's text
      d. an anonymous or named function can be passed to reduce
         that uses list destructuring on its arguments
      e. the 'seq' function will turn a map into a list where each
         element is a list with the first element being a key and
         the second element being the value that was assigned to the
         key (try this in a repl (seq {:a 1 :b 2 :c 3}))
   "
   (:require [clojure.string :as string]))



;; ==========================================================================
;; Functions to add / list messages and channels
;; ==========================================================================

(defn expansions []
  {"vandy" "Vanderbilt University"
   "jw" "Jules White"})

(defn messages-initial-state
  "Step 3a: Modify this dictionary so that there are at least two channels, which
   should be the keys. Each channel key should have a list of message
   dictionaries associated with it. The starting example has a single 'default'
   channel key and a list with a single message dictionary.

   Step 3b: Modify the channels-list function and replace the static list that
   you created with a statement that applies the 'keys' function to the 'msgs'
   dictionary parameter. If you need help, try googling 'clojure keys' (skip
   the article on destructuring).

   If you complete steps 2a and 2b successfully, you should be able to see the
   channels that you created and the messages that you listed for each
   channel.
   "
  []
  {"default" [{:msg "CS 4278" :time 1 :user {:name "Your Name" :nickname "You"}}
              {:msg "vandy" :time 1 :user {:name "Your Name" :nickname "You"}}]
   "food" [{:msg "Eat at fido" :time 1 :user {:name "Me" :nickname "Me"}}]
   "vandy" [{:msg "Visit the Wondr'y" :time 1 :user {:name "Your Name" :nickname "Bob"}}]})


(defn messages-expand [msg expansions]
   (let [msg-text        (:msg msg)
         expansion-list  (seq expansions)
         replacer        (fn [text [m r]] (string/replace text m r))
         new-text        (reduce replacer msg-text expansion-list)]
     (assoc msg :msg new-text)))

(defn messages-get
  "Step 2: Update this function so that it looks up the messages stored in
   the 'msgs' dictionary under the 'channel' key and returns them. You will
   need to replace the existing logic in the function.

   If you complete this step successfully, you should be able to refresh
   the chat app in your browser (reload the page) and then see the channels
   and messages that you defined appear when the page is loaded.
  "
  [msgs channel]
  (let [raw-msgs       (get msgs channel)
        expanded-msgs  (map #(messages-expand % (expansions)) raw-msgs)]
    expanded-msgs))

(defn message-search-hint-updater
  "Transforms the text in messages to highlight search results by adding
   '[ ]' around the matched text."
  [msg search]
  (let [search-hint   (str " [ " search " ] ")
        original-text (:msg msg)
        updated-text  (string/replace original-text search search-hint)]
    (assoc msg :msg updated-text)))


(defn messages-search
  "Step 6: Modify this function so that it filters the list of message maps
   passed to it and only returns those that contain the 'search' string in
   the value for the ':msg' text key (hint: use 'filter' and
   clojure.string/includes?).

   If you complete this step successfully, you should be able to filter the
   list of messages in a channel by typing in a search phrase at the top.
   Complete this before moving on to Step 7.

   Step 7: Modify this method to transform the list of messages matching the
   search phrase and insert '[ ]' around the areas of text that match the
   search (hint: use 'map', 'assoc', 'let', and clojure.string/replace).
  "
  [chnl-msgs search]
  (let [search-matcher (fn [msg] (string/includes? (:msg msg) search))
        filtered-msgs  (filter search-matcher chnl-msgs)
        hinted-msgs    (map #(message-search-hint-updater % search) filtered-msgs)]
    hinted-msgs))

(defn messages-add
  "Step 4: Update this function so that it appends the 'msg' passed to the
   function to the 'chnl-msgs' variable declared in the let statement.

   If you complete this step successfully, you should be able to type
   a new message into the 'What would you like to say...' box and hit
   enter to see it added as a chat message.
  "
  [msgs channel msg]
  (let [chnl-msgs   (seq (get msgs channel))
        ;; Step 2: Your code goes here
        ;;
        ;; Replace chnl-msgs below with an expression that adds (conj) msg
        ;; to the chnl-msgs list.
        ;;
        ;; Make sure and preserve the current indentation!
        ;;
        updated-msgs (conj chnl-msgs msg)]
        ;;
        ;; If you are having trouble, try using (println chnl-msgs)
        ;; right before the (assoc msgs channel...) below. You can
        ;; view the result in the Chrome Javascript console by going
        ;; to View->Developer->Javascript Console
        ;;
        ;; End Your Code
        ;;
    (assoc msgs channel updated-msgs)))

(defn channels-list
  "
  Step 1: The return value for this function is currently an empty list.
  Update the return value to be a list of strings of channel names that
  you choose. Make sure to return at least three channel names.

  If you complete this step successfully, you should be able to open the
  chat app and click the left-nav expansion in the upper left-hand corner.
  When the left-hand nav slides out, these strings should be listed beneath
  'Channels'.
  "
  [msgs]
  (keys msgs))

(defn channels-add
  "Step 5: Modify this function so that:

   1. If there is not already a key for the specified channel in the provided
      msgs dictionary,
   2. the channel key is added to the msgs dictionary
   3. the value of the key is initialized to an empty list

  If you complete this step successfully, you should be able to click the
  'Add Channel' button on the left navigation bar, enter a channel name,
   and have it added to the list of channels (and opened)."
  [msgs channel]
  (if (nil? (get msgs channel))
      (assoc msgs channel [])
      msgs))
