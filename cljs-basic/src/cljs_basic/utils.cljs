(ns cljs-basic.utils
  (:require [cljs.core.async :refer [chan close! timeout <! >! put!] :as async]
            [goog.dom :as dom]
            [goog.events :as events])
  (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]]))

;; dom

(defn by-id
  [id]
  (dom/getElement id))

(defn set-html! [el s]
  (aset el "innerHTML" s))

(defn add-child! [e child]
  (dom/appendChild e child))

;; channels

(defn cycle-chan [v]
  (let [c (chan)]
    (async/onto-chan c (cycle v))
    c))

(defn iterate-chan
  ([] (let [c (chan)]
       (async/onto-chan c (iterate inc 0))
       c))
  ([f]
     (let [c (iterate-chan)]
       (async/map< f c))))

;; procs

(defn render-proc-log [p]
  (apply str
         (for [x p]
           (str "<div class=\"proc" x "\">Process " x "</div>"))))


;; log

(defn event-color [type]
  (condp = type
    :mousemove :red
    :mousedown :blue
    :mouseup :green
    :draw :red
    :draw-start :blue
    :draw-end :green))

(defn render-logs [p]
  (apply str
    (for [{:keys [type] :as e} (reverse p)
          :let [color (event-color type)]]
      (str "<div class=\"log-" (name color) "\">" e "</div>\n"))))

(defn peekn
  "Returns vector of (up to) n items from the end of vector v"
  [v n]
  (if (> (count v) n)
    (subvec v (- (count v) n))
    v))

(defn conj-take [v n el]
  (-> v (conj el) (peekn n)))

(defn go-log-loop [el chan]
  (go-loop [p []]
           (let [e (<! chan)
                 p (conj-take p 15 e)]
             (set-html! el (render-logs p))
             (recur p))))

;; mouse events

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


;;; draw channels

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

(defn point-div [{:keys [color] :as e :or {color :red}}]
  (dom/createDom "div"
                 (js-obj "class" (str "point " (name color))
                         "style" (str "left:" (:x e) ";top: " (:y e)))))

(defn go-add-points [el chan]
  (go (while true
        (let [e (<! chan)]
          (add-child! el (point-div e))))))


;; color chan

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

;; user color

(comment
  				<select size=5 id="color-selector">
					<option value="blue" selected>Blue</option>
					<option value="green">Green</option>
					<option value="purple">Purple</option>
					<option value="red">Red</option>
					<option value="yellow">Yellow</option>
				</select>)

(defn selected-color [el]
  (keyword (.-value el)))

(defn selected-color-chan [el]
  (iterate-chan (fn [i] (selected-color el))))

;; generated draw events

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

(defn go-app-drawing [events-el drawing-el]
  (let [colors (cycle-chan [:red :blue :green :yellow :purple])
        draw-event-chan (draw-event-chan (mouse-event-chan events-el))
        colored-chan (color-event-chan draw-event-chan colors)]
    (go-add-points drawing-el colored-chan)))

#_(let [log (by-id "log")
       sandbox (by-id "sandbox")
       events (by-id "events")
       color-selector (by-id "color-selector")
       color-chan (selected-color-chan color-selector)
       c (async/mult (to-colored-chan
                       (generate-draw-event-chan)
                       color-chan))]
(go-log-loop log (async/tap c (chan)))
(go-add-points sandbox (async/tap c (chan))))