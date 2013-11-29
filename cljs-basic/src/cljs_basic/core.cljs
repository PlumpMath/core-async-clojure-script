(ns cljs-basic.core
  (:require [cljs.core.async :refer [chan close! timeout <! >! put!] :as async]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs-basic.utils :as utils :refer [add-child! by-id conj-take cycle-chan go-add-points go-app-drawing go-log-loop iterate-chan point-div render-proc-log set-html!]])
  (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]]))


(defn listen-chan [el type]
  (let [c (chan)]
    (events/listen
      el
      (name type)
      (fn [event]
        (put! c
              {:x (.-offsetX event)
               :y (.-offsetY event)
               :type type})))
    c))

(defn mouse-event-chan [el]
  (let [c1 (listen-chan el :mouseup)
        c2 (listen-chan el :mousedown)
        c3 (listen-chan el :mousemove)]
    (async/merge [c1 c2 c3])))

;; draw event

(defn next-mouse-down? [mouse-event mouse-down?]
  (condp = (:type mouse-event)
    :mousedown true
    :mouseup false
    :mousemove mouse-down?))

(defn to-draw-event [mouse-event mouse-down?]
  (condp = (:type mouse-event)
    :mousedown (assoc mouse-event :type :draw-start)
    :mouseup (assoc mouse-event :type :draw-end)
    :mousemove (if mouse-down? (assoc mouse-event :type :draw))))

(defn draw-event-chan [mouse-event-chan]
  (let [c (chan)]
    (go-loop [mouse-down? false]
             (let [mouse-event (<! mouse-event-chan)]
               (if-let [draw-event (to-draw-event mouse-event mouse-down?)]
                 (>! c draw-event))
               (recur (next-mouse-down? mouse-event mouse-down?))))
    c))

;; 

(defn new-draw? [draw-event]
  (= (:type draw-event) :draw-start))

(defn color-event-chan [draw-event-chan color-chan]
  (let [c (chan)]
    (go-loop [color :green]
             (let [draw-event (<! draw-event-chan)
                   next-color (if (new-draw? draw-event)
                                (<! color-chan)
                                color)]
               (>! c (assoc draw-event :color next-color))
               (recur next-color)))
    c))

(defn selected-color [el]
  (keyword (.-value el)))

(defn selected-color-chan [el]
  (iterate-chan (fn [i] (selected-color el))))

(defn generate-draw-event [i]
  (let [x (mod i 10)
        type (cond
               (= x 0) :draw-start
               (= x 9) :draw-end
               :else :draw)]
    {:x (* x 15) :y (* (int (/ i 10)) 15) :type type}))

(defn generate-draw-event-chan []
  (let [c (chan)]
    (go-loop [i 0]
            (<! (timeout 200))
            (>! c (generate-draw-event i))
            (recur (inc i)))
    c))

;; main loop

(let [log (by-id "log")
      events (by-id "events")
      sandbox (by-id "sandbox")
      draw-events (generate-draw-event-chan) ;; (draw-event-chan (mouse-event-chan events))
      colors (selected-color-chan (by-id "color-selector")) ;; (cycle-chan [:red :blue :green :purple :yellow])
      color-events (color-event-chan draw-events colors)
      c (async/mult color-events)]
  (go-add-points sandbox (async/tap c (chan)))
  (go-log-loop log (async/tap c (chan))))
