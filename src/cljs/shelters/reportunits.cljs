(ns shelters.reportunits (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]
            [cljsjs.chartjs]

            [om-bootstrap.button :as b]
            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:groups []}))

(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))


(defn OnGetGroups [response]
   (swap! app-state assoc :groups  (get response "Groups")  )
   (.log js/console (:groups @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn getGroups [data] 
  (GET (str settings/apipath "api/user") {
    :handler OnGetGroups
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token  (first (:token @shelters/app-state)))) }
  })
)


(defn comp-groups
  [group1 group2]
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)

;(def theChart (js/Chart.))

(defn setPieChart[]
  (let [
    context (.getContext (.getElementById js/document "rev-chartjs") "2d")
    chart-data {:type "bar"
                :options {
                  :legend {
                    :display true
                    :labels {
                      :fontSize 72
                    }
                  }
                  :scales {
                    :xAxes [
                      {
                        :fontSize 72
                      }
                    ] 
                  }
                }
                :data {:labels ["2012" "2013" "2014" "2015" "2016"]
                       :datasets [{:data [5 10 15 20 25]
                                   :label "Rev in MM"
                                   :backgroundColor "#90EE90"}
                                  {:data [3 6 9 12 15]
                                   :label "Cost in MM"
                                   :backgroundColor "#F08080"}]}}
    ]
    (set! (.-defaultFontSize (.-global (.-defaults js/Chart))) 72)
    (set! (.-title js/document) "דו״ח זמינות יחידות")
    (js/Chart. context (clj->js chart-data))
  )
)



(defn onMount [data]
  ; (getGroups data)
  (swap! shelters/app-state assoc-in [:current] 
    "Groups"
  )
  (put! ch 46)
)

(defn setcontrols [value]
  (case value
    46 (setPieChart)
  )
)

(defn initqueue []
  (doseq [n (range 1000)]
    (go ;(while true)
      (take! ch(
        fn [v] (
           ;.log js/console v
           ;(setcalculatedfields) 
           setcontrols v
           
           ;(.log js/console v)  
          )
        )
      )
    )
  )
)


(initqueue)



(defcomponent report-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div  {:className "panel panel-primary" :style {:margin-top "70px" :direction "ltr"}}
          (dom/canvas {:id "rev-chartjs" :width "400" :height "400" :style {:width "400px !important" :height "400px !important"}}
            ;(om/build showreport-view  data {})
          )          
        )
      )
    )
  )
)




(sec/defroute reportunits-page "/reportunits" []
  (om/root report-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


