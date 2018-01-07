(ns shelters.devs 
  (:use [net.unit8.tower :only [t]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]
            [cljs-time.core :as tc]
            [cljs-time.format :as tf] 
            [om-bootstrap.button :as b]
            [clojure.string :as str]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:users [] }))

(defn printDevices []
  (.print js/window)
)


(defn OnGetUsers [response]
   (swap! app-state assoc :users  (get response "Users")  )
   (.log js/console (:users @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn handleChange [e]
  (let [
    tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! shelters/app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn getUsers [data] 
  (GET (str settings/apipath "api/user") {
    :handler OnGetUsers
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token  (first (:token @shelters/app-state)))) }
  })
)


(defn comp-devs
  [dev1 dev2]
  (if (> (compare (:name dev1) (:name dev2)) 0)
      false
      true
  )
)


(defcomponent showindications-view [unit]
  (render [_]
    (dom/div {:style {:justify-content "space-evenly" :text-align "justify" :display "flex" :flex-wrap "wrap" :width "100%" :margin-top "0px"}}
      (map (fn [item]
        (let [
          indicator (first (filter (fn [x] (if (= (:id x) (:id item)) true false))  (:indications @shelters/app-state)))

         ;;tr1 (.log js/console (str "lastupdate=" (:lastupdate indicator)))
         icon (case (:isok item) true (:okicon indicator) (if (> (count (:failicon indicator)) 0) (:failicon indicator) "fail"))
         name (t :he shelters/main-tconfig (keyword (str "indicators/" (:name indicator))))

         status (case (:isok item) true (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name indicator) "ok")))) (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name indicator) "fail")))))
         time (tf/unparse shelters/custom-formatter1 (:lastupdate item))
          ]
          (dom/div {:style {:border "1px solid rgba(0,0,0,.125)" :border-radius ".25rem" :padding "0px" :min-width "92px" :margin-top "10px" :display "table"}}
            (dom/div { :style { :display "table-row" :text-align "center" :margin-left "0px" :margin-right "0px" :border-top "1px solid rgba(0,0,0,.125)" :height "100%" :background-color "rgba(0,0,0,.03)"}}
               (dom/div {:style {:display "table-cell" :padding-bottom "5px" :padding-top "5px"}}
                 name
               )
            )
            (dom/div {:style {:backgroundColor "white" :text-align "center" :display "table-row"}}
              (dom/a {:href (str "#/devdetail/" (:id item)) }
                (dom/img {:src (str "images/" icon ".png") :style {:margin-top "0px" :margin-bottom "5px" :min-width "90px" :max-width "90px" :font-size "xx-large" :color "green"}})
              )
            )
            (dom/div { :style { :display "table-row" :text-align "center" :margin-left "0px" :margin-right "0px" :border-top "1px solid rgba(0,0,0,.125)" :height "100%" :background-color "white"}}
               (dom/div {:style {:display "table-cell" :padding-bottom "5px"}}
                 ;(str "סטטוס:" status)
                 (str status)
               )
            )
            (dom/div { :style { :text-align "center" :margin-left "0px" :margin-right "0px" :border-top "1px solid rgba(0,0,0,.125)" :height "100%" :background-color "white" :padding-bottom "5px" :padding-top "5px"}}
               (dom/div {:style {:display "table-cell" :max-width "90" :white-space "normal" :word-break "break-word"}}
                 ;; (dom/div {:className "row"}
                 ;;   (str "תאריך:")
                 ;; )
                 (str time)
                 ;; (dom/div {:style {:padding-bottom "5px"}}
                   
                 ;; )
               )
            )
          )
        )
      )
      (filter (fn [x] (if (> (count (filter (fn [y] (if (= (:id y) (:id x)) true false)) (:indications @shelters/app-state))) 0) true false)) (:indications unit)))
    )
  )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]

    (dom/div {:style {:justify-content "space-evenly" :text-align "justify" :display "flex" :flex-wrap "wrap" :width "100%"}}
         (map (fn [item]
           (let []
             (dom/div { :className "panel panel-primary" :style {:display "inline-block" :white-space "nowrap" :border "1px solid #ddd" :margin-left "20px" :margin-top "20px" :max-width "330px" :margin-bottom "0px" :backgroundColor "white"}}
               (dom/div {:className "panel-heading" :style {:padding-top "3px" :padding-bottom "3px"}}
                 (dom/div {:className "row" :style {:max-width "290px" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                   (dom/div {:style {:white-space "normal"}} (str "מזהה יחידה: " (if (or (nil? (:controller item)) (< (count (:controller item)) 1)) "empty" (:controller item))))
                 )

                 (dom/div {:className "row" :style {:max-width "290px" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                   (dom/div {:style {:white-space "normal"}} (str "שם יחידה: " (if (or (nil? (:name item)) (< (count (:name item)) 1)) "empty" (:name item))))
                 )

                 (dom/div {:className "row" :style {:max-width "290px" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                   (dom/div {:style {:white-space "normal"}} (str "כתובת יחידה: " (if (or (nil? (:address item)) (< (count (:address item)) 1)) "empty" (:address item))))
                 )
               )

               (om/build showindications-view item {})


               (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px" :padding-bottom "0px"}}
                 (dom/div {:className "col-xs-6" :style { :text-align "center" :height "55px" :padding-top "10px"}}
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "transparent" :text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "5px" :padding-right "5px"}}
                       ;(dom/span {:className "glyphicon glyphicon-film" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "yellow"}})
                       (b/button {:className "btn btn-block btn-info" :style {:margin-top "0px" :font-size "12px"} :onClick (fn [e] (set! (.-title js/document) "פתח מנעול"))} (t :he shelters/main-tconfig (keyword (str "commands/" (:name (nth (:commands @data) 0))))))
                     )
                   )
                 )

                 (dom/div {:className "col-xs-6" :style { :text-align "center" :height "55px" :padding-top "10px"}}
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "transparent" :text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "5px" :padding-right "5px"}}
                       ;(dom/span {:className "glyphicon glyphicon-film" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "yellow"}})
                       (b/button {:className "btn btn-block btn-info" :style {:margin-top "0px" :font-size "12px" :padding-left "0px" :padding-right "0px"} :onClick (fn [e] (set! (.-title js/document) "בדיקת תקשורת"))} "בדיקת תקשורת")
                     )
                   )
                 )
               )
             )
           )
         ) (sort (comp comp-devs) (filter (fn [x] (if (str/includes? (str/lower-case (if (nil? (:name x)) "" (:name x))) (str/lower-case (:search @data))) true false)) (:devices @data)))
      )
    )
  )
)



(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] 
    "Dashboard"
  )
  (set! (.-title js/document) "Dashboard")
  (swap! shelters/app-state assoc-in [:view] 8)
)



(defcomponent dashboard-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "0px" :width "100%"}}
          (dom/div {:className "row" :style {:margin-top "70px" :margin-left "30px" :margin-right "15px" :border-bottom "solid 1px" :border-color "#e7e7e7"}}
            (dom/div {:className "col-xs-9" :style {:text-align "right"  :padding-top "5px"}}
              (dom/h2 "תמונת מצב")
            )
            (dom/div {:className "col-xs-3" :style {:margin-top "20px" :text-align "left"}}
               ;(b/button {:className "btn btn-primary" :onClick (fn [e] (-> js/document .-location (set! "#/devdetail")))} "הוספת יחידה חדשה")
            )
          )
          (om/build showdevices-view  data {})
        )
        (dom/div {:className "panel panel-primary" :style {:margin-top "30px"}} ;;:onClick (fn [e](println e))
        
          ; (dom/div {:className "panel-heading"}
          ;   (dom/div {:className "row"}
          ;     ; (dom/div {:className "col-md-10"}
          ;     ;   (dom/span {:style {:padding-left "5px"}} "我的消息")
          ;     ; )
          ;     ; (dom/div {:className "col-md-2"}
          ;     ;   (dom/span {:className "badge" :style {:float "right" }} (str (:msgcount data))  )
          ;     ; )
          ;   )
          ; )          
        )
      ) 
    )
  )
)




(sec/defroute dashboard-page "/dashboard" []
  (om/root dashboard-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


