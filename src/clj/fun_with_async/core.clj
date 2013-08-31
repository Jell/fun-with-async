(ns fun-with-async.core
  (:require [clojure.core.async :as async :refer :all]))

(comment
  "channels:"
  chan
  close!
  timeout

  "sync:"
  >!!
  <!!
  alts!!

  "async:" (:requires go)
  >!
  <!
  alts!)
