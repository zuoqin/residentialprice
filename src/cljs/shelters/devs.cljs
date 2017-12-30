(ns shelters.devs (:use [net.unit8.tower :only [t]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]


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

(defn goDevice [devid]
  ;;(aset js/window "location" (str "#/devdetail/" devid) )
  (swap! shelters/app-state assoc-in [:view] 7)
  (set! (.-title js/document) (str "יחידה:" devid) )
)


(defcomponent speedo-view [data owner]
  (render [_]
    (dom/div {:style {:zoom 0.13 :width "640px" :height "480px" :margin-top "0px" :margin-right "0px" :margin-bottom "0px" :margin-left "0px" :overflow "hidden" :backgroundColor "#000" :position "relative"}}
      (dom/div {:className "speedometr"}
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris-w"})
        (dom/div {:className "ris ris-o1"})
        (dom/div {:className "ris ris-o2"})
        (dom/div {:className "ris ris-o3"})
        (dom/div {:className "ris ris-o4"})
        (dom/div {:className "ris ris-o5"})
        (dom/div {:className "ris ris-o6"})
        (dom/div {:className "ris ris-o7"})
        (dom/div {:className "ris ris-o8"})
        (dom/div {:className "ris ris-o9"})
        (dom/div {:className "ris ris-o10"})


        (dom/span {:className "speedo s-0"} "0")
        (dom/span {:className "speedo s-20"} "20")
        (dom/span {:className "speedo s-40"} "40")
        (dom/span {:className "speedo s-60"} "60")
        (dom/span {:className "speedo s-80"} "80")
        (dom/span {:className "speedo s-100"} "100")
        (dom/span {:className "speedo s-120"} "120")
        (dom/span {:className "speedo s-140"} "140")
        (dom/span {:className "speedo s-160"} "160")
        (dom/span {:className "speedo s-180"} "180")
        (dom/span {:className "speedo s-200"} "200")

        (dom/div {:className "strelka"})
      )
      (dom/div {:className "black"})
    )
  )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]

    (dom/div {:style {:justify-content "space-evenly" :text-align "justify" :display "flex" :flex-wrap "wrap" :width "100%"}}
         (map (fn [item]
           (let []
             (dom/div { :className "panel panel-primary" :style {:display "inline-block" :white-space "nowrap" :border "1px solid #ddd" :margin-left "20px" :margin-top "20px" :max-width "290px" :margin-bottom "0px" :backgroundColor "lightgrey"}}
               (dom/div {:className "panel-heading"}
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

               (dom/div {:className "row" :style {:padding-top "5px" :margin-left "0px" :margin-right "0px"}}
                 (dom/div {:className "col-xs-6" :style { :padding-left "7px"}
                   }
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/img {:src "images/door_closed.png" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "red"}})
                       )
                     )

                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                        "דלת"
                     )
                   )
                 )
                 (dom/div {:className "col-xs-6" :style {:padding-right "7px"}
                   }

                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/img {:src "images/lock_closed.png" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "green"}})
                       )
                     )
                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                        "בריח"
                     )
                   )
                 )
               )

               (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                 (dom/div {:className "col-xs-6" :style {:padding-left "7px"}
                   }
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/span {:className "glyphicon glyphicon-star" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "brown"}})
                       )

                     )

                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center"  :margin-left "0px" :margin-right "0px"}}
                        "ארון תקשורת"
                     )
                   )
                 )
                 (dom/div {:className "col-xs-6" :style {:padding-right "7px"}
                   }
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/span {:className "glyphicon glyphicon-save-file" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "blue"}})
                       )

                     )

                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                        "גלאי"
                     )
                   )
                 )
               )

               (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                 (dom/div {:className "col-xs-6" :style { :padding-left "7px" }
                           }
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/span {:className "glyphicon glyphicon-th-large" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "aqua"}})
                       )

                     )
                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                        "תקשורת"
                     )
                   )
                 )
                 (dom/div {:className "col-xs-6" :style {:padding-right "7px"}
                           }
                   (dom/div {:style {:border "solid 1px transparent" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "white" :text-align "center"}}
                       (dom/a {:href (str "/#/devdetail/" (:id item)) }
                         (dom/span {:className "glyphicon glyphicon-film" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "yellow"}})
                       )
                     )

                     (dom/div {:className "row" :style {:backgroundColor "white" :text-align "center" :margin-left "0px" :margin-right "0px"}}
                        "סוללה"
                     )
                   )
                 )

               )

               (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px" :padding-bottom "0px"}}
                 (dom/div {:className "col-xs-6" :style { :text-align "center" :height "75px" :padding-top "10px"}}
                   (dom/div {:style {:border "solid 1px lightgrey" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "transparent" :text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "5px" :padding-right "5px"}}
                       ;(dom/span {:className "glyphicon glyphicon-film" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "yellow"}})
                       (b/button {:className "btn btn-block btn-info" :style {:margin-top "0px" :font-size "12px"} :onClick (fn [e] ((shelters/goUserDetail e) (-> js/document .-location (set! "#/userdetail")) ))} "פתח מנעול")
                     )
                   )
                 )

                 (dom/div {:className "col-xs-6" :style { :text-align "center" :height "75px" :padding-top "10px"}}
                   (dom/div {:style {:border "solid 1px lightgrey" :padding "3px"}}
                     (dom/div {:style {:backgroundColor "transparent" :text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "5px" :padding-right "5px"}}
                       ;(dom/span {:className "glyphicon glyphicon-film" :style {:margin-top "25px" :margin-bottom "25px" :height "30px" :font-size "xx-large" :color "yellow"}})
                       (b/button {:className "btn btn-block btn-info" :style {:margin-top "0px" :font-size "12px"} :onClick (fn [e] ((shelters/goUserDetail e) (-> js/document .-location (set! "#/userdetail"))))} "בדיקת תקשורת")
                     )
                   )

                 )

               )
             )

           )
         ) (sort (comp comp-devs) (filter (fn [x] (if (str/includes? (str/lower-case (:name x)) (str/lower-case (:search @data))) true false)) (:devices @data)))
      )
    )
  )
)



(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] 
    "Dashboard"
  )
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


