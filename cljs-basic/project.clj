(defproject cljs-basic "0.1.0"
  :description "drawing with core.async in javascript"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2060"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]]

  :plugins [[lein-cljsbuild "1.0.0"]]

  :source-paths ["src"]

  :cljsbuild { 
    :builds [{:id "cljs-basic"
              :source-paths ["src"]
              :compiler {
                :output-to "cljs_basic.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
