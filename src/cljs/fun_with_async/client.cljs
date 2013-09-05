(ns fun-with-async.client
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [cljs.reader :refer [read-string]]
            goog.net.WebSocket))

(def state (atom []))
(def host (aget js/window "location" "host"))
(def incoming (chan))
(def outgoing (chan))
(def keyevents (chan))

(def socket
  (doto (goog.net.WebSocket.)
    (.open (str "ws://" host "/"))
    (.addEventListener goog.net.WebSocket.EventType.MESSAGE
                       (fn [e]
                         (put! incoming (.-message e))))))

(def canvas
  (.getElementById js/document "target"))

(def context
  (.getContext canvas "2d"))

(defn reset-canvas []
  (doto context
    (.beginPath)
    (.setFillColor "FFFFFF")
    (.fillRect 0 0 (.-width canvas) (.-height canvas))
    (.closePath)))

(defn draw_square [x y color]
  (doto context
    (.beginPath)
    (.setFillColor color)
    (.fillRect (* 10 x) (* 10 y) 10 10)
    (.closePath)))

(defn draw-state [s]
  (reset-canvas)
  (doseq [{color :color [x y] :pos} s]
    (draw_square x y color)))

(def key-codes {"38" "up"
                "87" "up"
                "83" "down"
                "40" "down"
                "37" "left"
                "65" "left"
                "39" "right"
                "68" "right"})

(defn update-state [new-state-str]
  (let [new-state (read-string new-state-str)]
    (reset! state new-state)
    (draw-state new-state)))

(defn resize-canvas []
  (doto canvas
    (.setAttribute "width"  (.-innerWidth  js/window))
    (.setAttribute "height" (.-innerHeight js/window)))
  (draw-state @state))

(resize-canvas)
(set! (.-onresize js/window) resize-canvas)

(go (update-state (<! incoming)) ; Wait for connection
    (.addEventListener js/document "keydown" (fn [e] (put! keyevents)) false)
    (go (while true (.send socket (<! outgoing))))
    (go (while true (update-state (<! incoming))))
    (go (while true
          (let [key (<! keyevents)
                code (str (.-keyCode e))]
            (.log js/console code)
            (when-let [motion (get key-codes code)]
              (>! outgoing motion))))))
