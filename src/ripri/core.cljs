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

(defn component-main [state]
  [:div
   [:h1 "RICE Prioritization Framework"]
   [:p (:blurb @state)]
   [:p [:input (merge {:placeholder "Project name"} (make-change-handler state [:project-name]))]]
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
      [:th "Reach"]
      [:th "Impact"]
      [:th "Confidence"]
      [:th "Effort"]
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
   [:button {:on-click #(swap! state update-in [:rows] sort-rows)} "sort"]
   [:footer
    [:p [:a {:href "mailto:chris@mccormick.cx?subject=riceprioritization.com feedback"} "Feature request?"]]   
    [:p "Made with 🤖 by " [:a {:href "https://twitter.com/mccrmx" :taret "_BLANK"} "@mccrmx"]]]])

(defn start {:dev/after-load true} []
  (rdom/render [component-main state]
               (js/document.getElementById "app")))

(defn init []
  (let [blurb (-> (.querySelector js/document "#app>p") (aget "textContent"))]
    (js/console.log blurb)
    (swap! state assoc :blurb blurb))
  (start))
