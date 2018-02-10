(ns shelters.reportsensors
  (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]
            [cljsjs.chartjs]
            [om.dom :as omdom :include-macros true]
            [cljs-time.core :as tc]
            [cljs-time.local :as tl]
            [cljs-time.coerce :as te]
            [cljs-time.format :as tf]
            [clojure.string :as str]
            [om-bootstrap.button :as b]
            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:data [] :state 0 :filter {:controller "" :statuses "1" :indication "0" :status 1 :fromdate (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") (str (tf/unparse (tf/formatter "yyyy-MM-dd") (tl/local-now)) " 00:00:00")) :todate (te/from-long (- (+ (te/to-long (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") (str (tf/unparse (tf/formatter "yyyy-MM-dd") (tl/local-now)) " 00:00:00"))) (* 24 3600 1000)) (* 1 1 1) 1))


;(tf/parse (tf/formatter "yyyy-mm-dd") (tf/unparse (tf/formatter "yyyy-mm-dd") (+ (te/to-long (tl/local-now)) (* 24 3600 1000))))
                                             }}))

(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))




(defn onDropDownChange [id value]
  (let [
    ;value (if (= id "unit") )
    ]
    (swap! app-state assoc-in [:filter (keyword id)] value)
  )
  ;(.log js/console (str "id=" id "; value=" value))
)


(defn setDropDowns []
  (jquery
     (fn []
       (-> (jquery "#controller" )
         (.selectpicker {})
       )
     )
   )

  (jquery
     (fn []
       (-> (jquery "#indication")
         (.selectpicker {})
       )
     )
   )


   (jquery
     (fn [])
       (-> (jquery "#indication")
         (.selectpicker "val" (:indication (:filter @app-state)))
         (.on "change"
           (fn [e]
             (onDropDownChange (.. e -target -id) (.. e -target -value))
               ;(.log js/console e)
           )
         )
       )
   )

   (jquery
     (fn []
       (-> (jquery "#controller")
         (.selectpicker "val" (:controller (:filter @app-state)))
         (.on "change"
           (fn [e]
             (onDropDownChange (.. e -target -id) (.. e -target -value))
               ;(.log js/console e)
           )
         )
       )
     )
   )
)

(defn set-datepicker-style [elem]
  (let [
      left (.-left (js/getOffset elem))
      width (.. js/document -body -clientWidth)
    ]
    (set! (.-right (.-style elem)) (str (- width left 240) "px"))
    ;(.log js/console (str "width=" width "; left=" left))
    ;; (if (= "todate" field)
    ;;     (set! (.-right (.-style elem)) "400px")
    ;;     (set! (.-right (.-style elem)) "200px")
    ;; )
  )
  (set! (.-width (.-style elem)) "240px")
)

(defn setDatepicker [field]
  (let [] 

    (jquery
     (fn []
       (-> (jquery (str "#" field) )
         (.datepicker #js{:format "dd/mm/yyyy" :autoclose true :orientation "left"})
         (.on "show"
           (fn [e]
             (let [
               elem (aget (.getElementsByClassName js/document "datepicker datepicker-dropdown dropdown-menu datepicker-orient-left datepicker-orient-bottom") 0)
               ]
               ;(.log js/console )
               ;(dorun (map set-datepicker-style (js->clj elems)))
               (set-datepicker-style elem)
             )
           )
         )
         (.on "changeDate"
           (fn [e] (let [
             dt (js/Date (.. e -date))

             dtstring (if (= (count (.. e -dates) ) 0)
                    nil
                    (te/from-long (- (+ (te/to-long (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") (str (.format e 0 "yyyy-mm-dd") " 00:00:00"))) (* (case field "fromdate" 0 24) 3600 1000)) (case field "fromdate" 0 1)))
                )
              ]
             ;(.log js/console (str (.. e -date)  ) )
             ;(.log js/console (count (.. e -dates)))
             ;(.log js/console (str "date=" (subs (str (.. e -date)  ) 4 24)))
             (if (not= dtstring nil)
               (swap! app-state assoc-in [:filter (keyword field)] dtstring)
             )
           ))
         )
       )      
     )
    )
  )
  ;(.log js/console (get field  "fieldcode"))
)


(defn setDatepickers []
  (let [fields ["fromdate" "todate"] ]
    ;(.log js/console "Inside SetDate Pickers" )
    ;(.log js/console (get (nth fields 2 ) "fieldcode"    )   )
    (dorun (map setDatepicker fields))
  )
)

(defn map-report-values [row]
  (let [
    res
      (loop [result {} data row]
        (if (seq data)
          (let [
            vals (first data)
            key (get vals "key")
            val (get vals "value")
            ]
            ;(.log js/console (str "vals=" vals "; key=" key "; val=" val "; res=" result))
            (recur 
              (case key
                "Id" (assoc result :id val)
                "ChangeTime" (assoc result :change (if (> (count val) 0) (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") val) nil))
                "ControllerId" (assoc result :controller val)
                "IndicationName" (assoc result :indication val)
                "OldValue" (assoc result :oldval val)
                "NewValue" (assoc result :newval val)
                "UnitAddress" (assoc result :address val)
                "UnitDescription" (assoc result :name val)
              )
              (rest data)
            )
          )
          result
        )
      )
    ;tr1 (.log js/console (str "res=" res))
    ]
    res
  )
)

(defn handleChange [e]
  (let [
    ;tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! app-state assoc-in [:filter (keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn OnCreateReport [response]
   (swap! app-state assoc :data  (map map-report-values response))
   (swap! app-state assoc-in [:state] 0)
   ;(.log js/console (:groups @app-state))
)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn createReport []
  (let [
    status (js/parseInt (:statuses (:filter @app-state)))
    indication (js/parseInt (:indication (:filter @app-state)))
    ]
    (swap! app-state assoc-in [:state] 1)
    (POST (str settings/apipath "createReport") {
      :handler OnCreateReport
      :error-handler error-handler
      :headers {
        :token (str (:token (:token @shelters/app-state))) }
      :format :json
      :params {:reportId 3 :filter [{:column "ChangeTime" :minValue (tf/unparse shelters/custom-formatter2 (:fromdate (:filter @app-state))) :maxValue (tf/unparse shelters/custom-formatter2 (:todate (:filter @app-state)))} {:column "ControllerId" :likeValue (:controller (:filter @app-state))} {:column "IndicationName" :likeValue (if (= 0 indication) "" (nth shelters/indicators (- indication 1)))}] }
    })
  )
)


(defn comp-data
  [item1 item2]
  (case (:sort-list @app-state)
    1 (if (> (:id item1) (:id item2))
        false
        true
      )

    2 (if (> (:id item1) (:id item2))
        true
        false
      )


    3 (if (> (compare (:controller item1) (:controller item2)) 0)
        false
        true
      )

    4 (if (> (compare (:controller item1) (:controller item2)) 0)
        true
        false
      )

    5 (if (> (compare (:indication item1) (:indication item2)) 0)
        false
        true
      )

    6 (if (> (compare (:indication item1) (:indication item2)) 0)
        true
        false
      )

    7 (if (> (compare (:address item1) (:address item2)) 0)
        false
        true
      )

    8 (if (> (compare (:address item1) (:address item2)) 0)
        true
        false
      )


    (if (> (compare (:name item1) (:name item2)) 0)
        false
        true
    )
  )
)



(defn onMount [data]
  (set! (.-title js/document) (str "דו''ח חיווים") )
  (swap! shelters/app-state assoc-in [:current] "Report #3")
  (put! ch 46)
  (put! ch 47)
)

(defn setcontrols [value]
  (case value
    46 (setDatepickers)
    47 (setDropDowns)
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


(defn handle-change [e owner]
  
  (swap! app-state assoc-in [:filter (keyword (.. e -target -id))] 
    (.. e -target -value)
  )
)



(defn buildIndicationsList [data owner]
  (map
    (fn [text num]
      (let [
        ;tr1 (.log js/console (str  "name=" (:name text) ))
        ]
        (dom/option {:key num :data-width "100px" :value num :onChange #(handle-change % owner)} text)
      )
    )
    (flatten (conj ["הכל"] (map (fn [x] ((keyword x) (:words @shelters/app-state))) shelters/indicators))) (range 0 7 1)
  )
)


(defn buildUnitsList [data owner]
  (map
    (fn [item]
      (dom/option {:key (:id item) :value (:controller item)
                    :onChange #(handle-change % owner)} (:name item))
    )
    (:devices @shelters/app-state )
  )
)

(defcomponent show-report [data owner]
  (render [_]
    (let []
      (dom/div {:className "panel-body" :style {:flex 1 :overflow-y "scroll" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px"}}
        (map (fn [item]
          (let [
              
              tr1 (.log js/console (str "indication=" (:indication item) "; newval=" (:newval item)))
            ]
            (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px" :border-bottom "1px solid" :border-right "1px solid"}}
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px"}}
                (:id item)
              )
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px"}}
                (:controller item)
              )
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px"}}
                (:name item)
              )
              (dom/div {:className "col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :max-height "26px" :overflow-y "hidden" :border-left "1px solid" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px"}}
                (:address item)
              )

              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px" :height "26px"}}
                ((keyword (:indication item)) (:words @shelters/app-state))
              )

              (dom/div {:className "col-md-4" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                (dom/div {:className "col-md-6" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px" :height "26px"}}
                  ((keyword (:oldval item)) (:words @shelters/app-state))
                )
                (dom/div {:className "col-md-6" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px" :height "26px"}}
                  ((keyword (:newval item)) (:words @shelters/app-state))
                )
              )



              (dom/div {:className "col-md-2" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/div {:className "col-md-12" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid transparent" :overflow-x "hidden"}}
                  (tf/unparse shelters/custom-formatter1 (:change item))
                )

              )
            )
          ))
          (sort (comp comp-data) (:data @data))
        )
      )
    )
  )
)
(defcomponent header-view [data owner]
  (render [_]
    (dom/div  {:className "panel panel-primary" :style {:margin-top "70px"}}
      (dom/div {:className "panel-heading" :style {:padding-top "0px" :padding-bottom "0px"}}
        (dom/h3 "רשימתת...")
      )
      (dom/div {:className "panel-body"}
        (dom/div {:className "row"}
          (dom/div {:className "col-md-2" :style {:padding-right "0px"}}
            (dom/div {:className "col-md-3" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :margin-top "5px"}} (dom/h5 "Unit"))

            (dom/div {:className "col-md-8" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "left" :padding-top "7px"}}
              (omdom/select #js {:id "controller"
                             :className "selectpicker"
                             :title "בחר אחד מהבאים ..."
                             :data-show-subtext "true"
                             :data-width "100%"
                             :data-live-search "true"
                             :onChange #(handle-change % owner)
                            }
                (buildUnitsList data owner)
              )
            )

            (dom/div {:className "col-md-1" :style {:margin-top "4px" :text-align "right" :padding-right "0px"}}       
              (dom/span {:className "asterisk"} "*")
            )
          )

          (dom/div {:className "col-md-2" :style {:padding-right "15px" :padding-left "0px"}}
            (dom/div {:className "col-md-2" :style {:text-align "left" :padding-left "0px" :padding-right "0px" :margin-top "2px"}} (dom/h5 "From"))
            (dom/div {:className "col-md-9" :style {:margin-left "0px" :padding-left "0px" :padding-right "5px" :text-align "right" :padding-top "7px"}}
              (dom/input {:id "fromdate" :data-date-start-date "-14d" :data-date-end-date "0d" :value (if (nil? (:fromdate (:filter @app-state))) "" (tf/unparse (tf/formatter "dd/MM/yyyy") (:fromdate (:filter @app-state)))) :style {:margin-top "0px" :width "100%"}})
            )
            (dom/div {:className "col-md-1" :style {:margin-top "4px" :text-align "right" :padding-right "0px"}}       
              (dom/span {:className "asterisk"} "*")
            )
          )


          (dom/div {:className "col-md-2" :style {:padding-right "5px"}}
            (dom/div {:className "col-md-2" :style {:text-align "right" :padding-left "0px" :padding-right "0px" :margin-top "2px"}} (dom/h5 "To"))
            (dom/div {:className "col-md-9" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "right" :padding-top "7px"}}
              (dom/input {:id "todate" :data-date-start-date "-14d" :data-date-end-date "0d" :value (if (nil? (:todate (:filter @app-state))) "" (tf/unparse (tf/formatter "dd/MM/yyyy") (:todate (:filter @app-state)))) :style {:margin-top "0px" :width "100%"}} )
            )
            (dom/div {:className "col-md-1" :style {:margin-top "4px" :text-align "right" :padding-right "0px"}}       
              (dom/span {:className "asterisk"} "*")
            )
          )

          (dom/div {:className "col-md-4" :style {:padding-right "5px"}}
            (dom/div {:className "col-md-4" :style {:text-align "left" :padding-left "10px" :padding-right "0px" :margin-top "5px"}} (dom/h5 "Indication"))

            (dom/div {:className "col-md-7" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "left" :padding-top "7px"}}
              (omdom/select #js {:id "indication"
                             :className "selectpicker"
                             :title "בחר אחד מהבאים ..."
                             :data-show-subtext "true"
                             :data-width "100%"
                             :data-live-search "true"
                             :onChange #(handle-change % owner)
                            }
                (buildIndicationsList data owner)
              )
            )
            ;; (dom/div {:className "col-md-9" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "right" :padding-top "7px"}}

            ;;   (dom/input {:id "fault" :value (if (nil? (:fault (:filter @app-state))) "" (:fault (:filter @app-state))) :style {:margin-top "0px" :width "100%"}  :onChange (fn [e] (handleChange e))})
            ;; )
            (dom/div {:className "col-md-1" :style {:margin-top "4px" :text-align "right" :padding-right "0px"}}       
              (dom/span {:className "asterisk"} "*")
            )
          )

        )

        (dom/div {:className "row"}
          (b/button {:className (if (= (:state @app-state) 0) "btn btn-lg btn-primary btn-block" "btn btn-lg btn-primary btn-block m-progress" ) :onClick (fn [e](createReport)) :type "submit" :style {:margin-right "160px" :width "100px" :margin-top "5px"}}  "Submit")
        )
      )          
    )      
  )
)

(defcomponent report-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [
      ;style {:style {:margin "10px" :padding-bottom "0px"}}
      ;styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view shelters/app-state {})
        (dom/div {:className "container" :style {:height "100%" :width "100%"}}
          (dom/div {:style {:height "100%" :display "flex" :flex-direction "column"}}
            (om/build header-view data {})
            (dom/div {:className "panel-primary" :style {:padding-left "16px" :margin-left "-0px"}}
              (dom/div {:className "panel-heading" :style {:padding-top "0px" :padding-bottom "0px" :margin-left "-15px"}}
                (dom/div {:className "row" :style {:margin-left "2px" :margin-right "-15px"}}
                  (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1)) (shelters/doswaps)))}
                    "מספר אירוע"
                  )

                  (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3)) (shelters/doswaps)))}
                    "מזהה יחידה"
                  )
                  (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5)) (shelters/doswaps)))}
                    "שם יחידה"
                  )
                  (dom/div {:className "col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 7 "url(images/sort_asc.png" 8 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 7 8 7)) (shelters/doswaps)))}
                    "מיקום יחידה"
                  )

                  (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 9 "url(images/sort_asc.png" 10 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 9 10 9)) (shelters/doswaps)))}
                    "Indication"
                  )

                  (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px"}}
                    (dom/div {:className "col-md-6" :style {:padding-left "0px" :padding-right "0px" :height "100%" :padding-top "5px" :padding-bottom "5px" :border-left "1px solid" :background-image (case (:sort-list @app-state) 11 "url(images/sort_asc.png" 12 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 11 12 11)) (shelters/doswaps)))}
                      "oldValue"
                    )
                    (dom/div {:className "col-md-6" :style {:padding-left "0px" :padding-right "0px" :height "100%" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 13 "url(images/sort_asc.png" 14 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 13 14 13)) (shelters/doswaps)))}
                      "newValue"
                    )                    
                  )
                  (dom/div {:className "col-md-2" :style {:text-align "center" :border-left "1px solid transparent" :padding-top "5px" :padding-bottom "5px" :padding-left "0px" :padding-right "0px" :background-image (case (:sort-list @app-state) 15 "url(images/sort_asc.png" 16 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 15 16 15)) (shelters/doswaps)))}
                    "Change Time"
                  )
                )
              )
            )
            (om/build show-report data {})
          )
        )
      )
    )
  )
)




(sec/defroute reportsensors-page "/reportsensors" []
  (om/root report-view
           app-state
           {:target (. js/document (getElementById "app"))}
  )
)
