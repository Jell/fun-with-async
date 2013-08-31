(ns fun-with-async.core
  (:require [com.keminglabs.jetty7-websockets-async.core :as ws]
            [clojure.core.async :refer [chan go <! >!]]
            [clojure.core.match :refer [match]]
            [compojure.core :refer [routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.edn :as edn]))

(def state (atom []))
(def connections (atom []))
(def serial (atom 0))

(defn new-color []
  (apply str (take 6 (shuffle (seq "0123456789ABCDEF")))))

(defn new-player [id]
  {:id id :pos [5 5] :color (new-color)})

(defn move-player [id motion]
  (swap! state
         (fn [s]
           (mapv
            (fn [object]
              (if (= id (object :id))
                (update-in object [:pos] #(mapv + % motion))
                object))
            s))))

(defn remove-player [id]
  (swap! state
         (fn [s]
           (remove #(= id (% :id)) s))))

(defn on-key-press [id msg]
  (prn "key pressed")
  (case msg
    "39" (move-player id [ 1  0])  ; right
    "38" (move-player id [ 0 -1])  ; up
    "37" (move-player id [-1  0])  ; left
    "40" (move-player id [ 0  1])) ; down
  (prn (str "ID: " id ", Key: " msg)))

(defn new-connection [connection]
  (match [connection]
         [{:uri uri :in outgoing :out incoming}]
         (go
          (prn "connected")
          (let [id (swap! serial inc)]
            (swap! state conj (new-player id))
            (swap! connections conj outgoing)
            (doseq [out @connections]
              (>! out (str @state)))
            (loop []
              (when-let [msg (<! incoming)]
                (on-key-press id msg)
                (prn @state)
                (doseq [out @connections]
                  (>! out (str @state)))
                (recur)))
            (remove-player id)
            (swap! connections disj outgoing)
            (doseq [out @connections]
              (>! out (str @state)))
            (prn "disconnected")))))

(defn register-ws-app!
  [conn-chan]
  (go (while true (new-connection (<! conn-chan)))))

(def app
  (routes
   (route/files "/" {:root "public"})))

(def server
  (let [c (chan)]
    (register-ws-app! c)
    (run-jetty app
               {:join? false :port 8080
                :configurator (ws/configurator c)})))
