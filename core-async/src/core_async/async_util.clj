(ns core-async.async-util
  (:require [clojure.core.async :as async :refer [>! <! <!! alts! chan close! go put! take! timeout]]))

;; vectors

(defn peekn
  "Returns vector of (up to) n items from the end of vector v"
  [v n]
  (if (> (count v) n)
    (subvec v (- (count v) n))
    v))

(defn conj-take [v n el]
  (-> v (conj el) (peekn n)))

;; channels

(defn iterate-chan
  ([] (let [c (chan)]
       (async/onto-chan c (iterate inc 0))
       c))
  ([f]
     (let [c (iterate-chan)]
       (async/map< f c))))

(defn atom-chan [atom]
  (iterate-chan (fn [x] @atom)))

(defn repeatedly-chan [f]
  (iterate-chan (fn [i] (f))))


;;;; 

(comment
  
  (do
    (java.lang.Thread/sleep 3000)
    (println "Hello Stan"))

  (go
    (java.lang.Thread/sleep 3000)
    (println "Hello Stan"))
  
  
  (<!!
    (go
      (java.lang.Thread/sleep 3000)
      3))

  (def c (chan))
  
  (go
    (println "Sending")
    (>! c 2)
    (println "done!"))
  
  (go (loop [p []]
        (println p)
        (recur (conj-take p 10 (<! c)))))
  
  (go
    (while true
      (<! (timeout 250))
      (>! c 1)))
  
  (go
    (while true
      (<! (timeout 1000))
      (>! c 2)))

  (go
    (while true
      (<! (timeout 2666))
      (>! c 3))))