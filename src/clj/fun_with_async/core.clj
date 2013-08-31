(ns fun-with-async.core
  (:require [com.keminglabs.jetty7-websockets-async.core :as ws]
            [clojure.core.async :refer [chan go <! >!]]
            [clojure.core.match :refer [match]]
            [compojure.core :refer [routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.edn :as edn]))

(def players (atom {}))
(def connections (atom []))
(def serial (atom 0))

(defn new-color []
  (apply str (take 6 (shuffle (seq "0123456789ABCDEF")))))

(defn new-player []
  {:pos [5 5] :color (new-color)})

(defn move-player [id motion]
  (swap! players update-in [id :pos] #(mapv + % motion)))

(defn on-key-press [id msg]
  (prn (str "ID: " id ", Key: " msg))
  (case msg
    "39" (move-player id [ 1  0])   ; right
    "38" (move-player id [ 0 -1])   ; up
    "37" (move-player id [-1  0])   ; left
    "40" (move-player id [ 0  1]))) ; down

(defn send-players []
  (let [players (vals @players)]
    (doseq [out @connections]
      (go (>! out (str players))))))

(defn new-connection [connection]
  (match [connection]
         [{:uri uri :in outgoing :out incoming}]
         (go
          (let [id (swap! serial inc)]
            (prn (str "Id: " id " connected"))
            (swap! players assoc id (new-player))
            (swap! connections conj outgoing)
            (send-players)
            (loop []
              (when-let [msg (<! incoming)]
                (on-key-press id msg)
                (prn @players)
                (send-players)
                (recur)))
            (swap! players dissoc id)
            (swap! connections outgoing)
            (send-players)
            (prn (str "Id: " id " connected"))))))

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
