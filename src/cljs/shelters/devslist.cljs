מזהה יחידה(ns shelters.devslist (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]

            [cljs.core.async :refer [put! dropping-buffer chan take! <! timeout]]
            [ajax.core :refer [GET POST PUT DELETE]]
            [shelters.groupstounit :as groupstounit]
            [om-bootstrap.button :as b]
            [clojure.string :as str]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:sort-list 1}))
(defonce dev-state (atom  {}))
(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))

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

(defn OnUpdateUnitError [response]
  (let [     
    ]

  )
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn OnUpdateUnitSuccess [response]
  (let [
      units (:devices @shelters/app-state)
      delunit (remove (fn [unit] (if (= (:id unit) (:id (:selecteddevice @shelters/app-state)) ) true false  )) units)
      addunit (conj delunit (:selecteddevice @shelters/app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:devices] addunit)
    ;(shelters/goDashboard nil)
    (js/window.history.back)
  )
)

(defn savegroups []
  (PUT (str settings/apipath  "updateUnit") {
    :handler OnUpdateUnitSuccess
    :error-handler OnUpdateUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:selecteddevice @shelters/app-state)) :controllerId (:controller (:selecteddevice @shelters/app-state)) :name (:name (:selecteddevice @shelters/app-state)) :parentGroups (:groups (:selecteddevice @shelters/app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip (:ip (:selecteddevice @shelters/app-state)) :port (:port (:selecteddevice @shelters/app-state)) :latitude (:lat (:selecteddevice @shelters/app-state)) :longitude (:lon (:selecteddevice @shelters/app-state)) :details [{:key "address" :value (:address (:selecteddevice @shelters/app-state))} {:key "phone" :value (:tel (first (:contacts (:selecteddevice @shelters/app-state))))}]}})
)



(defn openDialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#groupsModal")
          (.modal)
        )
      )
    )
    (groupstounit/setcheckboxtoggle)
  )
)

(defn onAssignGroups [id]
  (let [
      dev (first (filter (fn [x] (if (= (:id x) id) true false)) (:devices @shelters/app-state)))
      ]
    (swap! shelters/app-state assoc-in [:selecteddevice] dev)
    (swap! shelters/app-state assoc-in [:selecteddevice :current] (str "שייך לקבוצה " (:name dev)))
    (put! ch 47)
  )
)


(defcomponent addModal [data owner]
  (render [_]
    (dom/div
      (dom/div {:id "groupsModal" :className "modal fade" :role "dialog"}
        (dom/div {:className "modal-dialog"} 
          ;;Modal content
          (dom/div {:className "modal-content"} 
            (dom/div {:className "modal-header"} 
              (b/button {:type "button" :className "close" :data-dismiss "modal"})
              (dom/h4 {:className "modal-title"} (:modalTitle @app-state) )
            )
            (dom/div {:className "modal-body"}

              (dom/div {:className "panel panel-primary"}

                (dom/h1 {:style {:text-align "center"}} (:current (:selecteddevice @data)))
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                  (dom/div {:className "row"}
                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
                    )
                    (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}}
                      (dom/h5 "Name")
                    )
                    (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "1px solid"}}
                      (dom/h5 "Selection")
                    )
                    (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "0px solid"}}
                      (dom/h5 "Selected")
                    )
                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
                    )
                  )
                )

                (om/build groupstounit/showgroups-view data {})
              )
                     ;(om/build groupstounit/showgroups-view dev-state {})
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Close")
                )

                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal" :onClick (fn [e] (savegroups))} "Save")
                )
              )
            )
          )
        )
      )
    )
  )

)

(defn comp-devs
  [dev1 dev2]
  (case (:sort-list @app-state)
    1 (if (> (compare (:name dev1) (:name dev2)) 0)
        false
        true
      )

    2 (if (> (compare (:name dev1) (:name dev2)) 0)
        true
        false
      )


    3 (if (> (compare (:controller dev1) (:controller dev2)) 0)
        false
        true
      )

    4 (if (> (compare (:controller dev1) (:controller dev2)) 0)
        true
        false
      )

    5 (if (> (compare (:address dev1) (:address dev2)) 0)
        false
        true
      )

    6 (if (> (compare (:address dev1) (:address dev2)) 0)
        true
        false
      )


    (if (> (compare (:name dev1) (:name dev2)) 0)
        false
        true
    )
  )
)


(defn handle-chkbsend-change [e]
  (let [
        id (str/join (drop 8 (.. e -currentTarget -id)))
        
        devices (:selectedunits @shelters/app-state)


        ;tr2 (.log js/console (.. e -currentTarget) )

        ;tr1 (.log js/console (str "amount1=" amount1 " client=" client))
        
        deldevs (remove (fn [dev] (if (= dev id) true false  )) devices)

        adddev (if (.. e -currentTarget -checked) (into [] (conj deldevs id)) deldevs) 
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! shelters/app-state assoc-in [:selectedunits] adddev)
  )
)


(defn goDevice [devid]
  ;;(aset js/window "location" (str "#/devdetail/" devid) )
  (swap! shelters/app-state assoc-in [:view] 7)
  (set! (.-title js/document) (str "יחידה:" devid) )
)

(defcomponent showstatuses [data owner]
  (render
    [_]
    (dom/td
      (map (fn [item]
        (let []
          (dom/i {:className (case (:isok item) false "fa fa-bullhorn" "fa fa-bullhorn") :style {:color (case (:isok item) false "#dd0000" "#00dd00") :font-size "18px"}})
          ;(dom/i {:className (case (:isok item) false "fa fa-bullhorn" "fa fa-battery-three-quarters") :style {:color (case (:isok item) false "#dd0000" "#00dd00") :font-size "24px"}})
          
        )
        )
        
      (:indications data))
    )
  )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]

    (dom/div
      (map (fn [item]
        (let [
          isselected (if (= (.indexOf (:selectedunits @data) (:id item)) -1) false true)
          ;tr1 (.log js/console (str item))
          ]
          (dom/div {:className "row tablerow":style {:padding-top "10px" :margin-right "0px" :margin-left "0px"}}
            (dom/div {:className "col-xs-1 col-md-1"}
              (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center"}}
                (dom/input { :id (str "checksel" (:id item)) :type "checkbox" :className "device_checkbox" :checked isselected :onChange (fn [e] (handle-chkbsend-change e))})
              )

              (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center"}}

                (dom/div { :className "dropdown"}
                  (b/button {:className "btn btn-danger dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
                    "☰"
                  )
                  (dom/ul {:className "dropdown-menu" :aria-labelledby "dropdownMenuButton"}
                    (dom/li {:className "dropdown-item"}
                      (dom/a {:href (str "#/devdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                        "עדכון נתונים"
                      )
                    )
                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href (str "#/devdetail/" (:id item))}
                        "ביטול יחידה"
                      )
                    )

                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href "#" :onClick (fn [e] (onAssignGroups (:id item)))}
                        "שיוך לקבוצה"
                      )
                    )
                  )
                )
              )
            )

            (dom/div {:className "col-xs-1 col-md-1"}
              (dom/a {:href (str "#/unitdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o"})
                (:controller item)
              )
            )


            (dom/div {:className "col-xs-1 col-md-1"}
              (dom/a {:href (str "#/unitdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o"})
                (:name item)
              )
            )

            (dom/div {:className "col-xs-3 col-md-3"}

              (:address item)
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style { :color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-right-width "1px"}}
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              ;(case (:status item) 3 "Inactive" "Active")
            )

            ;(om/build showstatuses item {})
          )
          )
        ) (sort (comp comp-devs) (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (:search @data))) true false)) (:devices @data )))
      )
    )
  )
)

(defn setcontrols [value]
  (case value
    ;46 (setGroup)
    47 (go
         (<! (timeout 100))
         (openDialog)
       )
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


(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] 
    "Dashboard"
  )
)

(defn OnDoCommand [response] 
  (.log js/console (str response ))
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn sendcommand1 []
  (POST (str settings/apipath "doCommand" ;"?userId="(:userid  (:token @shelters/app-state))
       )
       {:handler OnDoCommand
        :error-handler error-handler
        :format :json
        :headers {:token (str (:token  (:token @shelters/app-state)))}
        :params {:commandId (js/parseInt (:id (first (:commands @shelters/app-state)))) :units (into [] (:selectedunits @shelters/app-state)) }
    }
  )
)

(defcomponent topbuttons-view [data owner]
  (render [_]
    (dom/div {:className "row" :style {:padding-top "70px" :border-bottom "solid 1px" :border-color "#e7e7e7"}}
      (dom/div {:className "col-xs-9" :style { :text-align "right" }}
        (dom/h2 "רשימת יחידות")
      )

      (dom/div {:className "col-xs-1" :style {:padding-top "15px"}}
        (b/button {:className "btn btn-primary" :onClick (fn [e]
          (-> js/document .-location (set! "#/devdetail")))} "הוספת יחידה חדשה"
        )
      )

      (dom/div {:className "col-xs-2" :style {:margin-right "0px" :padding-top "15px" :text-align "left"}}
        (b/button {:className "btn btn-primary"
          :disabled? (= (count (:selectedunits @data)) 0)
          :onClick (fn [e] (sendcommand1))} (str (:name (nth (:commands @data) 0)) " (" (count (:selectedunits @data)) ") יחידות")
        )
      )
    )
  )
)

(defcomponent dashboard-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      ;tr1 (.log js/console (:name (first (:commands @data))))
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "0px" :width "100%" :padding-left "50px" :padding-right "50px" :height "100%"}}
          (dom/div {:className "panel panel-primary" :style {:padding-bottom "70px"}}
            (om/build topbuttons-view data {})

            ;; (dom/div {:className "row":style {:padding-top "10px"}}
            ;;   (dom/div
            ;;     (b/button {:className "btn btn-primary" :onClick (fn [e]
            ;;       (-> js/document
            ;;         .-location
            ;;         (set! "#/devdetail")
            ;;       ))} "Add New"
            ;;     )
            ;;   )
            ;;   (dom/div
 
            ;;   )
            ;; )
            (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
              (dom/div {:className "row" :style {:margin-right "0px" :margin-left "0px"}}
                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center"}}
                  (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center"}}
                    (dom/i {:className "fa fa-square-o"})
                  )

                  (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center"}}
                    (dom/div "☰")
                  )
                )


                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "5px" :padding-bottom "5px" :white-space "nowrap"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
    (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3))
    (shelters/doswaps) ))} "מזהה יחידה") (case (:sort-list @app-state) 3 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 4 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))


                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "5px" :padding-bottom "5px" :white-space "nowrap"}} (b/button {:className "btn btn-primary colbtn" :style {} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1)) (shelters/doswaps)))} "שם יחידה") (case (:sort-list @app-state) 1 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 2 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span))
                )

                (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "5px" :padding-bottom "5px" :white-space "nowrap"}}
                  (b/button {:className "btn btn-primary colbtn" :style {:width "100%"} :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5))
  (shelters/doswaps)))} "כתובת"
                  ) (case (:sort-list @app-state) 5 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 6 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span))

                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "סטטוס דלת"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "סטטוס בריח"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "מתג ארון תקשורת"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "גלאי"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "תקשורת"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "מצב סוללה"
                )

                (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}
                  "חיישן בשם"
                )

                (dom/div {:className "col-xs-9 col-md-9" :style {:text-align "center" :padding-left "0px" :padding-right "0px"}}  

                )
              )
            )
            (om/build showdevices-view data {})
            (om/build addModal data {})
          )
        )
      ) 
    )
  )
)




(sec/defroute dashboard-page "/devslist" []
  (om/root dashboard-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


