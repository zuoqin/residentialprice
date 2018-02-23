(ns shelters.reportcomands
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

(defonce app-state (atom  {:data [] :state 0 :filter {:controller "" :statuses "1" :user "" :status 1 :fromdate (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") (str (tf/unparse (tf/formatter "yyyy-MM-dd") (tl/local-now)) " 00:00:00")) :todate (te/from-long (- (+ (te/to-long (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") (str (tf/unparse (tf/formatter "yyyy-MM-dd") (tl/local-now)) " 00:00:00"))) (* 24 3600 1000)) (* 1 1 1) 1))


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
       (-> (jquery "#user")
         (.selectpicker {})
       )
     )
   )


   (jquery
     (fn [])
       (-> (jquery "#user")
         (.selectpicker "val" (:user (:filter @app-state)))
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
                "CommandId" (assoc result :commandid val)
                "Time" (assoc result :time (if (> (count val) 0) (tf/parse (tf/formatter "yyyy-MM-dd HH:mm:ss") val) nil))
                "ControllerId" (assoc result :controller val)
                "ResponsibleUser" (assoc result :user val)
                "CommandName" (assoc result :command (str (t :he shelters/main-tconfig (keyword (str "commands/" val)))))
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
    ;status (js/parseInt (:statuses (:filter @app-state)))
    ;user (:user (:filter @app-state))
    ]
    (swap! app-state assoc-in [:state] 1)
    (POST (str settings/apipath "createReport") {
      :handler OnCreateReport
      :error-handler error-handler
      :headers {
        :token (str (:token (:token @shelters/app-state))) }
      :format :json
      :params {:reportId 2 :filter [{:column "Time" :minValue (tf/unparse shelters/custom-formatter2 (:fromdate (:filter @app-state))) :maxValue (tf/unparse shelters/custom-formatter2 (:todate (:filter @app-state)))} {:column "ControllerId" :likeValue (:controller (:filter @app-state))} {:column "ResponsibleUser" :likeValue (if (= 0 (:user (:filter @app-state))) "" (:user (:filter @app-state)) )}] }
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



(defn buildUsersList [data owner]
  (map
    (fn [item]
      (let [
        ;tr1 (.log js/console (str  "name=" (:name text) ))
        ]
        (dom/option {:key (:userid item) :data-width "100px" :value (:login item) :onChange #(handle-change % owner)} (str (:firstname item) " " (:lastname item)))
      )
    )
    (flatten (conj [{:lastname "הכל" :userid "0" :login "0"}]  (:users @shelters/app-state)))
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
      (dom/div {:className "panel-body" :style {:flex 1 :overflow-y "scroll" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :border-right "1px solid lightgrey"}}
        (map (fn [item num]
          (let [
              
              ;tr1 (.log js/console (str "indication=" (:indication item) "; newval=" (:newval item)))
            ]
            (dom/div {:className "row tablerow" :style {:userSelect "none" :margin-left "0px" :margin-right "0px" :border-bottom "1px solid lightgrey"}}
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px"}}
                num
              )
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px"}}
                (:controller item)
              )
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px"}}
                (:name item)
              )
              (dom/div {:className "col-md-4" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :height "36px" :overflow-y "hidden" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px"}}
                (:address item)
              )

              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px" :height "36px"}}
                (:command item)
              )

              (dom/div {:className "col-md-2" :style {:text-align "center" :border-left "1px solid lightgrey" :padding-left "0px" :padding-right "0px" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px" :height "36px"}}
                (:user item)
              )

              (dom/div {:className "col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                ;; (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px" :overflow-x "hidden" :padding-top "3px" :padding-bottom "3px" :height "26px"}}
                ;;   ((keyword (:oldval item)) (:words @shelters/app-state))
                ;; )


                (dom/div {:className "col-md-12" :style {:padding-left "0px" :padding-right "0px"}}
                  (dom/div {:className "col-md-12" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :border-left "1px solid lightgrey" :overflow-x "hidden" :padding-top "8px" :padding-bottom "8px" :height "36px"}}
                    (tf/unparse shelters/custom-formatter1 (:time item))
                  )

                )
              )
            )
          ))
          (sort (comp comp-data) (:data @data)) (map inc (range))
        )
      )
    )
  )
)


(defcomponent header-view [data owner]
  (render [_]
    (dom/div  {:className "panel panel-primary" :style {:margin-top "70px"}}
      (dom/div {:className "panel-heading" :style {:padding-top "0px" :padding-bottom "0px"}}
        (dom/h3 "דו''ח שליחת פקודות הפעלה")
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
            (dom/div {:className "panel-default" :style {:border-top "solid 1px lightgrey" :padding-left "15px" :margin-left "-0px" :border-left "solid 1px lightgrey" :border-right "solid 1px lightgrey"}}
              (dom/div {:style {:margin-bottom "10px"}}
                (dom/div {:className "row" :style {:margin-left "0px" :margin-right "10px"}}
                  (dom/div {:className "col-md-2" :style {:padding-right "0px"}}
                    (dom/div {:className "col-md-12" :style {:font-weight "700" :text-align "right" :padding-left "0px" :padding-right "0px" :margin-top "5px"}} (dom/div "שם יחידה"))
                  )

                  (dom/div {:className "col-md-2" :style {:padding-right "15px" :padding-left "0px"}}
                    (dom/div {:className "col-md-12" :style {:font-weight "700" :text-align "right" :padding-left "0px" :padding-right "0px" :margin-top "5px"}} (dom/div "תאריך התחלה"))
                  )

                  (dom/div {:className "col-md-2" :style {:padding-right "5px"}}
                    (dom/div {:className "col-md-12" :style {:font-weight "700" :text-align "right" :padding-left "0px" :padding-right "0px" :margin-top "5px"}} (dom/div "תאריך סיום"))
                  )

                  (dom/div {:className "col-md-1" :style {:padding-right "5px"}}
                    (dom/div {:className "col-md-12" :style {:font-weight "700" :text-align "right" :padding-left "0px" :padding-right "0px" :margin-top "5px"}} (dom/div "משתמש"))
                  )

                )
                (dom/div {:className "row" :style {:margin-left "0px" :margin-right "10px"}}
                    (dom/div {:className "col-md-2" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "left" :padding-top "7px"}}
                      (omdom/select #js {:id "controller"
                                     :className "selectpicker"
                                     :title "בחר אחד מהבאים ..."
                                     :data-show-subtext "false"
                                     :data-width "100%"
                                     :data-live-search "true"
                                     :onChange #(handle-change % owner)
                                    }
                        (buildUnitsList data owner)
                      )
                    )

                    (dom/div {:className "col-md-2" :style {:margin-left "0px" :padding-left "0px" :padding-right "5px" :text-align "right" :padding-top "7px"}}
                      (dom/input {:id "fromdate" :className "form-control" :data-date-start-date "-24d" :data-date-end-date "0d" :value (if (nil? (:fromdate (:filter @app-state))) "" (tf/unparse (tf/formatter "dd/MM/yyyy") (:fromdate (:filter @app-state)))) :style {:margin-top "0px" :height "34px"}})
                    )

                    (dom/div {:className "col-md-2" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "right" :padding-top "7px"}}
                      (dom/input {:id "todate" :className "form-control" :data-date-start-date "-24d" :data-date-end-date "0d" :value (if (nil? (:todate (:filter @app-state))) "" (tf/unparse (tf/formatter "dd/MM/yyyy") (:todate (:filter @app-state)))) :style {:margin-top "0px"  :height "34px"}} )
                    )



                    (dom/div {:className "col-md-1" :style {:margin-left "0px" :padding-left "0px" :padding-right "0px" :text-align "left" :padding-top "7px"}}
                      (omdom/select #js {:id "user"
                                     :className "selectpicker"
                                     :title "בחר אחד מהבאים ..."
                                     :data-show-subtext "true"
                                     :data-width "100%"
                                     :data-live-search "true"
                                     :onChange #(handle-change % owner)
                                    }
                        (buildUsersList data owner)
                      )
                    )

                    (dom/div {:className "col-md-2 col-md-offset-2"}
                      (dom/button {:className (if (= (:state @app-state) 0) "btn btn-default btn-block" "btn btn-default btn-block m-progress" ) :onClick (fn [e](createReport)) :type "submit" :style {:margin-right "16px" :margin-top "5px"}}  "הצג דו''ח")
                    )
                )

              )          
              (dom/div {:className "panel-heading" :style {:padding-top "0px" :padding-bottom "0px" :margin-left "-15px" :border-top "solid 1px lightgrey" :border-bottom "solid 2px #337ab7"}}
                (dom/div {:className "row" :style {:margin-left "2px" :margin-right "-15px"}}
                  (dom/div {:className "col-md-1" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1)) (shelters/doswaps)))}
                    "מספר רץ"
                  )

                  (dom/div {:className "col-md-1" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3)) (shelters/doswaps)))}
                    "מזהה יחידה"
                  )
                  (dom/div {:className "col-md-1" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5)) (shelters/doswaps)))}
                    "שם מקלט"
                  )
                  (dom/div {:className "col-md-4" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 7 "url(images/sort_asc.png" 8 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 7 8 7)) (shelters/doswaps)))}
                    "כתובת מקלט"
                  )

                  (dom/div {:className "col-md-1" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 9 "url(images/sort_asc.png" 10 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 9 10 9)) (shelters/doswaps)))}
                    "שם פקודה"
                  )

                  (dom/div {:className "col-md-2" :style {:font-weight "700" :text-align "center" :border-left "1px solid lightgrey" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px"}}
                    (dom/div {:className "col-md-12" :style {:font-weight "700" :padding-left "0px" :padding-right "0px" :height "100%" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 13 "url(images/sort_asc.png" 14 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 13 14 13)) (shelters/doswaps)))}
                      "משתמש"
                    )                    
                  )
                  (dom/div {:className "col-md-2" :style {:font-weight "700" :text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px"}}
                    (dom/div {:className "col-md-12" :style {:border-left "1px solid lightgrey" :padding-top "5px" :padding-bottom "5px" :background-image (case (:sort-list @app-state) 17 "url(images/sort_asc.png" 18 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 17 18 17)) (shelters/doswaps)))}
                      "תאריך ושעה"
                    )
                  )
                )
              )
            )
            (om/build show-report data {})
            (dom/div {:style {:padding-top "10px" :padding-bottom "10px" :padding-right "10px" :background-color "#ecedee" :border-top "1px solid lightgrey" :margin-bottom "10px"}}
              (str "Total: " (count (:data @app-state)) " records")
            )
          )
        )
      )
    )
  )
)

(defcomponent report-view1 [data owner]
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
                    "שם פקודה"
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
                    "תאריך ושעה"
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




(sec/defroute reportcomands-page "/reportcomands" []
  (om/root report-view
           app-state
           {:target (. js/document (getElementById "app"))}
  )
)
