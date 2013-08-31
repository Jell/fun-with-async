(ns fun-with-async.server
  (:require [com.keminglabs.jetty7-websockets-async.core :as ws]
            [clojure.core.async :refer [chan go <! >!]]
            [clojure.core.match :refer [match]]
            [compojure.core :refer [routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [clojure.edn :as edn]))

(def state (atom []))

(defn new-connection [connection]
  (match [connection]
         [{:uri uri :in outgoing :out incoming}]
         (go
          (>! outgoing (str @state))
          (loop []
            (when-let [msg (<! incoming)]
              (case msg
                "39" (swap! state conj "right")
                "38" (swap! state conj "up")
                "37" (swap! state conj "left")
                "40" (swap! state conj "down"))
              (prn msg)
              (>! outgoing (str @state))
              (recur)))
          (prn "disconnected"))))

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
