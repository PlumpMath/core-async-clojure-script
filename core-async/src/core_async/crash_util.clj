(ns core-async.crash-util)

(+ (* 1 2) (/ 3 4))

(let [a (* 1 2)
      a (/ 1 a)
      b (/ 3 4)]
  (+ a b))

(def a 2)

(defn f [x]
  (+ x 2))

(def v [1 2 3 4 5])

(def m {:x 10 :y 25})

(:x m)

(defn fact1 [n]
     (if (= n 1)
       1
       (* n (fact1 (dec n)))))

(defn fact2 [n]
  (letfn
    [(f [n r]
       (if (= n 1)
         r
         (f (dec n) (* n r))))]
    (f n 1)))

(defn fact3 [n]
  (loop [n n
         r 1]
    (if (= n 1)
      r
      (recur (dec n) (* n r)))))