(ns ripri.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [alandipert.storage-atom :refer [local-storage]]))

(defonce state (local-storage (r/atom {}) :app-state))

(defn log [& args]
  (apply js/console.log (clj->js args))
  (last args))

(defn compute-score [row]
  (let [score (/ (* (:reach row) (:impact row) (:confidence row)) (:effort row))]
    (if (js/isNaN score) 0 score)))

(defn sort-rows [rows]
  (into {}
        (->> rows
             (sort-by (fn [[idx v]] (compute-score v)))
             reverse
             (map-indexed (fn [idx [k v]] {idx v})))))

(defn make-change-handler [state coords]
  {:on-change #(swap! state assoc-in coords (-> % .-target .-value))
   :value (get-in @state coords)})

(defn component-column [state r k props]
  [:td.name [:input (merge {:class k
                            :size 1}
                           (make-change-handler state [:rows r k])
                           props)]])

(defn component-app [state]
  [:section#interface
   [:h2 "Priority score calculator"]
   [:p [:input (merge {:placeholder "Things being ranked"} (make-change-handler state [:project-name]))]]
   [:p "Effort in "
    [:select (make-change-handler state [:units :effort])
     [:option "---"]
     [:option "person months"]
     [:option "person weeks"]
     [:option "person days"]]]
   [:p "Reach in " [:input (merge {:placeholder "people"} (make-change-handler state [:units :reach]))]
    " per " [:select (make-change-handler state [:units :reach-time])
             [:option "---"]
             [:option "year"]
             [:option "month"]
             [:option "week"]
             [:option "day"]]]
   [:table
    [:thead
     [:tr
      [:th "Name"]
      [:th [:a {:href "#reach"} "Reach"]]
      [:th [:a {:href "#impact"} "Impact"]]
      [:th [:a {:href "#confidence"} "Confidence"]]
      [:th [:a {:href "#effort"} "Effort"]]
      [:th "Score"]]]
    [:tbody
     (for [r (range (inc (count (@state :rows))))]
       (let [row (get-in @state [:rows r])
             score (.toFixed (compute-score row) 0)]
         [:tr
          [component-column state r :name {:placeholder "Name..."}]
          [component-column state r :reach {:type :number :placeholder "1000"}]
          [:td.impact
           [:select (make-change-handler state [:rows r :impact])
            [:option {:value nil} "---"]
            [:option {:value "3"} "huge"]
            [:option {:value "2"} "high"]
            [:option {:value "1"} "mid"]
            [:option {:value "0.5"} "low"]
            [:option {:value "0.25"} "tiny"]]]
          [:td.confidence 
           [:select (make-change-handler state [:rows r :confidence])
            [:option {:value nil} "---"]
            [:option {:value "100"} "high"]
            [:option {:value "80"} "mid"]
            [:option {:value "50"} "low"]]]
          [component-column state r :effort {:type :number :placeholder "1000"}]
          [:td.score (if (js/isNaN score) "" score)]]))]]
   [:div#sort
    [:button {:on-click #(swap! state update-in [:rows] sort-rows)} "sort"]]])

(defn component-documentation [state]
  [:section#documentation
   [:h2 "Documentation"]
   [:h3 [:a {:name "reach"}] "Reach"]
   [:p "How many " (or (-> @state :units :reach) "people") " this will affect per " (or (-> @state :units :reach-time) " time period") "?"]
   [:p "For example this might be \"100 customers per quarter\". Or \"1000\" hits per month."]
   [:h3 [:a {:name "impact"}] "Impact"]
   [:p "How much will this impact the reach?"]
   [:p "For example, \"increase adoption when a person uses this\". Choose from huge (3), high (2), mid (1), low (0.5), or tiny (0.25) impact."]
   [:h3 [:a {:name "confidence"}] "Confidence"]
   [:p "How confident are you about your estimates for Reach, Impact, and Effort?"]
   [:p "The values are high (100%), mid (80%), and low (50%).
       Anything below this is too uncertain. Gather more data and come back."]
   [:h3 [:a {:name "effort"}] "Effort"]
   [:p "How much effort is this going to require in " (or (-> @state :units :effort) "people time-units") "?"]
   [:p "For example two people taking four weeks would be 8 person weeks of effort."]
   [:p "You can enter any value here but try to stick to whole numbers, or use a smaller person-time-unit."]
   [:h3 "Calculation"]
   [:p "Finally, here's a reminder of the RICE score calculation: " [:code "(R x I x C) / E"]]
   [:p "The calculator above will use your numbers to sort your options into priority order."]])

(defn component-main [state]
  [:div
   [:header
    [:h1 "RICE Prioritization Framework"]
    [:p (:blurb @state)]
    [:p "The basic formula this prioritization method uses is to multiply Reach, Impact, and Confidence, and then divide by Effort."
     [:pre "(R x I x C) / E"]
     "This gives a single score that you can use to rank projects, tasks, or other options.
     Use the calculator below to find your score and priority order for yours."]
    [:p "You can read more about how RICE works "
     [:a {:href "https://www.intercom.com/blog/rice-simple-prioritization-for-product-managers/"}
      "in this blog post"] ". Or see the documentation below."]]
   [component-app state]
   [component-documentation state]
   [:footer
    [:p [:a {:href "mailto:chris@mccormick.cx?subject=riceprioritization.com feedback"} "Feature request?"]]   
    [:p "Made with 🤖 by " [:a {:href "https://twitter.com/mccrmx" :taret "_BLANK"} "@mccrmx"]]
    [:p [:a {:href "https://github.com/chr15m/riceprioritization.com"} "source code"]]]])

(defn start {:dev/after-load true} []
  (rdom/render [component-main state]
               (js/document.getElementById "app")))

(defn init []
  (let [blurb (-> (.querySelector js/document "#app>p") (aget "textContent"))]
    (js/console.log blurb)
    (swap! state assoc :blurb blurb))
  (start))
