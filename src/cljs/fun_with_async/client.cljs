(ns fun-with-async.client
  (:require-macros [cljs.core.async.macros :as m :refer [go]])
  (:require [cljs.core.async :as async
             :refer [<! >! chan close! put! take! sliding-buffer
                     dropping-buffer timeout]]
            [cljs.reader :refer [read-string]]
            goog.net.WebSocket))

(def state (atom nil))
(def host (aget js/window "location" "host"))
(def incoming (chan))
(def outgoing (chan))

(def socket
  (doto (goog.net.WebSocket.)
    (.open (str "ws://" host "/"))
    (.addEventListener goog.net.WebSocket.EventType.MESSAGE
                       (fn [e]
                         (go (>! incoming (.-message e)))))))

(def canvas
  (doto (.getElementById js/document "target")
    (.setAttribute "width"  (.-width  js/document))
    (.setAttribute "height" (.-height js/document))))

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

(defn key-handler [e]
  (go (>! outgoing (.-keyCode e))))

(defn update-state [new-state-str]
  (let [new-state (read-string new-state-str)]
    (reset! state new-state)
    (draw-state new-state)))

(go (update-state (<! incoming)) ; Wait for connection
    (.addEventListener js/document "keydown" key-handler false)
    (go (while true (.send socket (<! outgoing))))
    (go (while true (update-state (<! incoming)))))
