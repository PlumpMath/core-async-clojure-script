(ns core-async.async
  (:require [clojure.core.async :as async :refer [>! <! <!! alts! chan close! go put! take! timeout]]
            [core-async.async-util :as u :refer [conj-take]]))
