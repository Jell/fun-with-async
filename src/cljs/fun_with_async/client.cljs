(ns fun-with-async.client
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [cljs.reader :refer [read-string]]
            goog.net.WebSocket))

(def state (atom nil))

(def host
  (aget js/window "location" "host"))

(def incoming (chan))

(def outgoing (chan))

(def socket
  (doto (goog.net.WebSocket.)
    (.open (str "ws://" host "/"))
    (.addEventListener goog.net.WebSocket.EventType.MESSAGE
                       (fn [e]
                         (go (>! incoming (.-message e)))))))

(defn key-handler [e]
  (go (>! outgoing (.-keyCode e))))

(defn update-state [new-state-str]
  (let [new-state (read-string new-state-str)]
    (.log js/console new-state)
    (reset! state new-state)))

(go (update-state (<! incoming)) ; Wait for connection
    (go (while true (.send socket (<! outgoing))))
    (go (while true (update-state (<! incoming)))))

(.addEventListener js/document "keydown" key-handler false)
