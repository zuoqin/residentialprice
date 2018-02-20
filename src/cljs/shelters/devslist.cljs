(ns shelters.devslist
  (:use [net.unit8.tower :only [t]])
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

(defonce app-state (atom  {:sort-list 1 :state 0}))
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
    ;tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
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

    (-> (jquery "#groupsModal .close")
          (.click)
    )
    (swap! app-state assoc-in [:state] 0)
    ;(set! ( . (.getElementById js/document "btnsavegroups") -disabled) false)


    ;(js/window.history.back)
  )
)

(defn savegroups []
  (swap! app-state assoc-in [:state] 1)
  ;(set! ( . (.getElementById js/document "btnsavegroups") -disabled) true)
  (go
    (<! (timeout 50))
    (PUT (str settings/apipath  "updateUnit") {
      :handler OnUpdateUnitSuccess
      :error-handler OnUpdateUnitError
      :headers {
        :token (str (:token (:token @shelters/app-state)))}
      :format :json
      :params {:unitId (:id (:selecteddevice @shelters/app-state)) :controllerId (:controller (:selecteddevice @shelters/app-state)) :name (:name (:selecteddevice @shelters/app-state)) :parentGroups (:groups (:selecteddevice @shelters/app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip (:ip (:selecteddevice @shelters/app-state)) :port (:port (:selecteddevice @shelters/app-state)) :latitude (:lat (:selecteddevice @shelters/app-state)) :longitude (:lon (:selecteddevice @shelters/app-state)) :details [{:key "address" :value (:address (:selecteddevice @shelters/app-state))} {:key "phone" :value (:tel (first (:contacts (:selecteddevice @shelters/app-state))))}]}})
  )
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
                  (b/button {:id "btnsavegroups" :disabled? (if (= (:state @app-state) 1) true false) :type "button" :className (if (= (:state @app-state) 0) "btn btn-default" "btn btn-default m-progress" ) :onClick (fn [e] (savegroups))} "שמור")
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

(defn comp-indications [ind1 ind2]
  (if (< (:id ind1) (:id ind2)) true false)
)

(defcomponent showstatuses [data owner]
  (render
    [_]
    (dom/div {:className "row"}
      (map (fn [item]
        (let [

          ]
          (dom/div {:className "col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "3px" :padding-bottom "3px"}}
            (dom/i {:id (str "status_" (:id item)) :className (case (:isok item) true "fa-toggle-on fa" "fa-toggle-off fa") :style {:color (case (:isok item) true "#00dd00" "#dd0000") :font-size "24px"}})
            (dom/p {:style {:margin "0px" :line-height "10px"}}
              (case (:isok item) true "פועל" "כבוי")
            )
          )
        )
        )
        (sort (comp comp-indications) (filter
          (fn [x] (let [
            name (:name (first (filter (fn [y] (if (= (:id y) (:id x)) true false)) (:indications @shelters/app-state))))
            ]
            (if (>= (.indexOf shelters/indicators name) 0)  true false)
          )) (:indications data))
        )
      )
;(filter (fn [x] (if (> (count (filter (fn [y] (if (= (:id y) (:id x)) true false)) (:indications @shelters/app-state))) 0) true false)) (:indications data))
    )
  )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]
    (dom/div {:style {:border-left "1px solid transparent" :border-right "1px solid"}}
      (map (fn [item]
        (let [
          isselected (if (= (.indexOf (:selectedunits @data) (:id item)) -1) false true)
          contact1 (first (filter (fn [x] (if (= (:userid x) (nth (:contacts item) 0)) true false)) (:users @data)))

          contact2 (first (filter (fn [x] (if (= (:userid x) (nth (:contacts item) 1)) true false)) (:users @data)))
          ;tr1 (.log js/console (str "id=" (nth (:contacts item) 0) contact1))
;          tr1 (.log js/console (str ) contact2)
          ]
          (dom/div {:className "row tablerow":style {:border-bottom "1px solid" :padding-top "0px" :margin-right "0px" :margin-left "0px"}}
            (dom/div {:className "col-md-1" :style {:border-left "1px solid"}}
              (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center" :padding-left "15px" :padding-right "0px" :padding-top "10px" :padding-bottom "10px" :border-left "1px solid"}}
                (dom/input { :id (str "checksel" (:id item)) :type "checkbox" :className "device_checkbox" :checked isselected :onChange (fn [e] (handle-chkbsend-change e))})
              )

              (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center" :padding-right "12px" :padding-left "0px"}}
                (dom/div { :className "dropdown"}
                  (b/button {:className "btn btn-danger dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false" :style {:padding-top "3px" :padding-bottom "3px" :padding-left "6px" :padding-right "6px" :margin-top "6px"}}
                    "☰"
                  )
                  (dom/ul {:className "dropdown-menu" :aria-labelledby "dropdownMenuButton" :style {:min-width "100px"}}
                    ;; (dom/li {:className "dropdown-item" :style {:text-align "center"}}
                    ;;   (dom/div {:style {:padding-left "0px" :padding-right "5px" :font-weight "800"}}
                    ;;     "פעולות"
                    ;;   )
                    ;; )
                    ;; (dom/li {:className "divider"}
                    ;; )
                    (dom/li {:className "dropdown-item"}
                      (dom/a {:href (str "#/devdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item))) :style {:padding-left "0px" :padding-right "5px"}}
                        "עדכון נתונים"
                      )
                    )
                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href (str "#/devdetail/" (:id item)) :style {:padding-left "0px" :padding-right "5px"}}
                        "ביטול יחידה"
                      )
                    )

                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href "#" :onClick (fn [e] (onAssignGroups (:id item))) :style {:padding-left "0px" :padding-right "5px"}}
                        "שיוך לקבוצה"
                      )
                    )
                  )
                )
              )
            )

            (dom/div {:className "col-md-1" :style {:border-left "1px solid" :padding-top "0px" :padding-bottom "0px" :height "42px" :line-height "42px" :padding-left "0px" :padding-right "0px" :text-align "center" :overflow "hidden"}}
              (dom/a {:href (str "#/devdetail/" (:id item)) :style {:margin-left "-100px" :margin-right "-100px"} :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o" :style {:margin-left "5px"}} )
                (:controller item)
              )
            )


            (dom/div {:className "col-md-1" :style {:border-left "1px solid" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px" :text-align "center" :height "42px" :overflow "hidden" :line-height "42px"}}
              (dom/a {:href (str "#/devdetail/" (:id item)) :style {:margin-left "-100px" :margin-right "-100px"} :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o" :style {:margin-left "5px"}})
                (:name item)
              )
            )

            (dom/div {:className "col-md-2" :style {:border-left "1px solid" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px" :text-align "center" :height "42px" :overflow "hidden"}}
              (dom/div {:style {:line-height "42px"}}
                (:address item)
              )
            )

            (dom/div {:className "col-md-2" :style {:border-left "1px solid" :padding-top "1px" :padding-bottom "1px" :text-align "center" :height "42px" :overflow "hidden"}}
              (dom/div {:className "row" :style {:height "20px" :overflow "hidden"}}
                (str (:firstname contact1) " " (:lastname contact1) " " (:phone contact1))
              )
              (dom/div {:className "row" :style {:height "20px" :overflow "hidden"}}
                (str (:email contact1))
              )
            )

            (dom/div {:className "col-md-2" :style {:border-left "1px solid" :padding-top "1px" :padding-bottom "1px" :text-align "center" :height "42px"}}
              (dom/div {:className "row" :style {:height "20px" :overflow "hidden"}}
                (str (:firstname contact2) " " (:lastname contact2) " " (:phone contact2))
              )
              (dom/div {:className "row" :style {:height "20px" :overflow "hidden"}}
                (str (:email contact2))
              )
            )


            (dom/div {:className "col-md-3"}
              (om/build showstatuses item {})
            )
          )
          )
        ) (sort (comp comp-devs) (filter (fn [x] (if (or (str/includes? (str/upper-case (:name x)) (str/upper-case (:search @data))) (str/includes? (str/upper-case (:controller x)) (str/upper-case (:search @data))) (str/includes? (str/upper-case (:address x)) (str/upper-case (:search @data)))) true false)) (:devices @data )))
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
  (set! (.-title js/document) "רשימה יחידות")
  (swap! shelters/app-state assoc-in [:view] 8)
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
    (dom/div

      (dom/div {:className "row" :style {:padding-top "60px" :border-bottom "solid 1px" :border-color "#e7e7e7"}}
        (dom/div {:className "col-xs-8" :style { :text-align "right" }}
          (dom/h3 "רשימת יחידות")
        )

        (dom/div {:className "col-xs-4" :style {:margin-top "15px" :text-align "left"}}
          (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
          (b/button {:className "btn btn-primary" :style { :padding-left "5px" :padding-right "5px" :margin-left "10px"} :onClick (fn [e] (-> js/document .-location (set! "#/devdetail")))} "הוספת יחידה חדשה"
          )

          (b/button {:className "btn btn-primary"  :style { :padding-left "5px" :padding-right "5px"}
            :disabled? (= (count (:selectedunits @data)) 0)
            :onClick (fn [e] (sendcommand1))} (str (t :he shelters/main-tconfig (keyword (str "commands/" (:name (nth (:commands @data) 0))))) " (" (count (:selectedunits @data)) ") יחידות")
          )
          )
        )
        ;; (dom/div {:className "col-xs-1" :style {:padding-top "15px"}}

        ;; )

        ;; (dom/div {:className "col-xs-2" :style {:margin-right "0px" :padding-top "15px" :text-align "left"}}

        ;; )
      )

      (dom/div {:className "row" :style {:margin-right "0px"}}
        (dom/input {:id "search" :className "form-control" :type "text" :placeholder "חיפוש" :style {:height "24px" :margin-top "12px"} :value  (:search @shelters/app-state) :onChange (fn [e] (handleChange e )) })
      )
    )
  )
)

(defn checkelement [unit]
  ;(set! (.-checked (js/document.getElementById (str "checksel" (:id unit)))) true)
  (.click (js/document.getElementById (str "checksel" (:id unit))))
)

(defn selectallunits []
  (doall (map (fn [x] (checkelement x)) (:devices @shelters/app-state)))
)

(defcomponent dashboard-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [
      ;style {:style {:margin "10px" :padding-bottom "0px"}}
      ;tr1 (.log js/console (:name (first (:commands @data))))
      indications (sort (comp comp-indications) (filter
          (fn [x] (let [
            name (:name (first (filter (fn [y] (if (= (:id y) (:id x)) true false)) (:indications @shelters/app-state))))
            ]
            (if (>= (.indexOf shelters/indicators name) 0)  true false)
          )) (:indications @data))
        )
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "0px" :width "100%" :padding-left "50px" :padding-right "50px" :height "100%"}}
          (dom/div { :style {:padding-bottom "90px"}}
            (om/build topbuttons-view data {})


            (dom/div {:className "panel-primary" :style {:padding "0px" :margin-top "10px"}}
              (dom/div {:className "panel-heading" :style {:padding-top "3px" :padding-bottom "0px"}}
                (dom/div {:className "row" :style {}}
                  (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding "0px"}}
                    (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center" :padding-top "10px" :padding-bottom "10px" :border-left "1px solid"}}
                      (dom/i {:className "fa fa-square-o" :onClick (fn [e] (selectallunits))})
                    )

                    (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center" :padding-top "10px" :padding-bottom "10px" :padding-left "0px" :padding-right "0px"}}
                      "פעולות"
                    )
                  )


                  (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "7px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-13" :style {:padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @app-state) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3)) (shelters/doswaps)))}
                        "מזהה יחידה"
                      )
                    )
                  )

                  (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "7px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-12" :style {:padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @app-state) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1)) (shelters/doswaps)))}
                        "שם יחידה"
                      )
                    )
                  )

                  (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "7px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-12" :style {:padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @app-state) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center"} :onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5)) (shelters/doswaps)))}
                        "כתובת"
                      )
                    )
                  )

                  (dom/div {:className "col-md-2" :style {:padding-left "0px" :padding-right "3px" :padding-top "10px" :padding-bottom "10px" :border-left "1px solid" :text-align "center"}}
                    "איש קשר 1"
                  )

                  (dom/div {:className "col-md-2" :style {:padding-left "0px" :padding-right "3px" :padding-top "10px" :padding-bottom "10px" :text-align "center" :border-left "1px solid"}}
                    "איש קשר 2"
                  )
                  (dom/div {:className "col-md-3"}
                    (dom/div {:className "row"}
                      (dom/div {:className "col-xs-2 col-md-2" :style {:border-left "1px solid" :text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:display "table-cell" :vertical-align "middle"}}
                          (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 0))))))
                        )
                      )

                      (dom/div {:className "col-xs-2 col-md-2" :style {:border-left "1px solid" :text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:display "table-cell" :vertical-align "middle"}}
                          (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 1))))))
                        )
                      )

                      (dom/div {:className "col-xs-2 col-md-2" :style {:border-left "1px solid" :text-align "center" :padding-left "0px" :padding-right "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:display "table-cell" :vertical-align "middle"}} (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 2))))))
                        )
                      )

                      (dom/div {:className "col-xs-2 col-md-2" :style {:border-left "1px solid" :text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:display "table-cell" :vertical-align "middle"}} (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 3))))))
                        )
                      )

                      (dom/div {:className "col-xs-2 col-md-2" :style {:border-left "1px solid" :text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:display "table-cell" :vertical-align "middle"}} (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 4))))))
                        )
                      )

                      (dom/div {:className "col-xs-2 col-md-2" :style { :text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :display "table" :height "40px"}}
                        (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px" :display "table-cell" :vertical-align "middle"}} (str (t :he shelters/main-tconfig (keyword (str "indicators/" (:name (nth indications 5))))))
                        )
                      )
                    )
                  )
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


