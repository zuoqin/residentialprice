(ns shelters.groups
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST PUT DELETE]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! timeout]]
            [clojure.string :as str]
            [om-bootstrap.button :as b]
            [shelters.groupstogroup :as groupstogroup]
            [shelters.unitstogroup :as unitstogroup]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:action 1}))
(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))

(defn handleChange [e]
  (let [
    ;tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! shelters/app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)

(defn handlechange [e]
  (let [
    ;tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! shelters/app-state assoc-in [:selectedgroup (keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn comp-groups
  [group1 group2]
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)

(defn openNameDialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#nameModal")
          (.modal)
        )
      )
    )
  )
)

(defn opengroupsdialog []
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
    (groupstogroup/setcheckboxtoggle)
  )
)

(defn openunitsdialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#unitsModal")
          (.modal)
        )
      )
    )
    (unitstogroup/setcheckboxtoggle)
  )
)


(defn OnUpdateGroupError [response]
  (let [     
      ;newdata {:tripid (get response (keyword "tripid") ) }
      tr1 (.log js/console (str "In OnUpdateGroupError " response))
    ]
  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateGroupSuccess [response]
  (let [
      groups (:groups @shelters/app-state)
      delgroup (remove (fn [group] (if (= (:id group) (:id (:selectedgroup @shelters/app-state))) true false)) groups)
      addgroup (conj delgroup (:selectedgroup @shelters/app-state))

      ;tr1 (.log js/console (str "In OnUpdateGroupSuccess " response))
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)
    (swap! shelters/app-state assoc-in [:state] 0)
    (-> (jquery "#nameModal .close")
          (.click)
    )

    (-> (jquery "#groupsModal .close")
          (.click)
    )

    (-> (jquery "#unitsModal .close")
          (.click)
    )
  )
)

(defn OnCreateGroupError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnCreateGroupSuccess [response]
  (let [
      tr1 (swap! shelters/app-state assoc-in [:selectedgroup :id] (get response "groupId"))
      groups (:groups @shelters/app-state)
      addgroup (conj groups (:selectedgroup @shelters/app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)

    (swap! shelters/app-state assoc-in [:state] 0)
    (-> (jquery "#nameModal .close")
          (.click)
    )

    (-> (jquery "#groupsModal .close")
          (.click)
    )

    (-> (jquery "#unitsModal .close")
          (.click)
    )
  )
)


(defn savegroup []
  (let [
    units (:childs (:selectedgroup @shelters/app-state))
    ]
    (doall (map (fn [unitid]
      (let[
        unit (first (filter (fn [x] (if (= (:id x) unitid) true false)) (:devices @shelters/app-state)))
        ;tr1 (.log js/console (str "updating unitid=" unitid) )
        groups (:groups unit)
        groups (if (> (.indexOf (if (nil? groups) [] groups) (:id (:selectedgroup @shelters/app-state))) -1) groups (conj (if (nil? groups) [] groups) (:id (:selectedgroup @shelters/app-state))))
        unit (assoc unit :groups groups)

        units (:devices @shelters/app-state)
        delunit (remove (fn [x] (if (= unitid (:id x)) true false)) units)
        addunit (conj delunit unit)
      ]
      (swap! shelters/app-state assoc-in [:devices] addunit)
      )) units)
    )

    (swap! shelters/app-state assoc-in [:state] 1)
    ;(set! ( . (.getElementById js/document "btnsavegroups") -disabled) true)
    (if (= (:action @shelters/app-state) 1)
      (PUT (str settings/apipath  "updateGroup") {
        :handler OnUpdateGroupSuccess
        :error-handler OnUpdateGroupError
        :headers {
          :token (:token (:token @shelters/app-state))}
        :format :json
        :params {:groupName (:name (:selectedgroup @shelters/app-state)) :groupId (:id (:selectedgroup @shelters/app-state)) :childEntities (:childs (:selectedgroup @shelters/app-state)) :parentGroups (:parents (:selectedgroup @shelters/app-state)) :owners (if (nil? (:owners (:selectedgroup @shelters/app-state))) [] (:owners (:selectedgroup @shelters/app-state))) :responsibleUser (:userid (:token @shelters/app-state)) :details [{:key "key1" :value "44"} {:key "key2" :value "444"}]}})

      (POST (str settings/apipath  "addGroup") {
        :handler OnCreateGroupSuccess
        :error-handler OnCreateGroupError
        :headers {
          :token (str "" (:token (:token @shelters/app-state)))}
        :format :json
        :params { :groupName (:name (:selectedgroup @shelters/app-state)) :groupId "" :childEntities (:childs (:selectedgroup @shelters/app-state)) :parentGroups (if (nil? (:parents (:selectedgroup @shelters/app-state))) [] (:parents (:selectedgroup @shelters/app-state)))  :owners (if (nil? (:owners (:selectedgroup @shelters/app-state))) [] (:owners (:selectedgroup @shelters/app-state))) :responsibleUser (:userid (:token @shelters/app-state)) :details [{:key "key" :value "value"}] }})
    )
  )
)

(defn OnDeleteGroupError [response]
  (let [     
      newdata {:groupid (get response (keyword "groupid") ) }
    ]

  )
  ;; TO-DO: Delete Group from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteGroupSuccess [response]
  (let [
      groups (:groups @shelters/app-state)
      newgroups (remove (fn [group] (if (= (:id group) (:id (:selectedgroup @shelters/app-state))) true false  )) groups)
    ]
    ;(swap! sbercore/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:groups] newgroups)
  )
)


(defn deletegroup [id]
  (let [
      group (first (filter (fn [x] (if (= (:id x) id) true false)) (:groups @shelters/app-state)))
    ]
    (swap! shelters/app-state assoc-in [:selectedgroup] group)
    (DELETE (str settings/apipath  "deleteGroup?groupId=" id) {
      :handler OnDeleteGroupSuccess
      :error-handler OnDeleteGroupError
      :headers {
        :token (str (:token (:token @shelters/app-state)))}
      :format :json})
  )
)

(defcomponent addmodalgroups [data owner]
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

                (dom/h1 {:style {:text-align "center"}} (:current (:selectedgroup @data)))
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-1" :style {:text-align "center" }}
                    )
                    (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px"}}
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

                (om/build groupstogroup/showgroups-view data {})
              )
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "ביטול")
                )

                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:id "btnsavegroup" :disabled? (if (= (:state @shelters/app-state) 1) true false) :type "button" :className (if (= (:state @shelters/app-state) 0) "btn btn-default" "btn btn-default m-progress" ) :onClick (fn [e] (savegroup))} "שמור")
                )
              )
            )
          )
        )
      )
    )
  )

)

(defcomponent addmodalunits [data owner]
  (render [_]
    (dom/div
      (dom/div {:id "unitsModal" :className "modal fade" :role "dialog"}
        (dom/div {:className "modal-dialog"} 
          ;;Modal content
          (dom/div {:className "modal-content"} 
            (dom/div {:className "modal-header"} 
              (b/button {:type "button" :className "close" :data-dismiss "modal"})
              (dom/h4 {:className "modal-title"} (:modalTitle @app-state) )
            )
            (dom/div {:className "modal-body"}

              (dom/div {:className "panel panel-primary"}

                (dom/h1 {:style {:text-align "center"}} (:current (:selectedgroup @data)))
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-1" :style {:text-align "center" }}
                    )
                    (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px"}}
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

                (om/build unitstogroup/showunits-view data {})
              )
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "ביטול")
                )

                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:id "btnsavegroup" :disabled? (if (= (:state @shelters/app-state) 1) true false) :type "button" :className (if (= (:state @shelters/app-state) 0) "btn btn-default" "btn btn-default m-progress" ) :onClick (fn [e] (savegroup))} "שמור")
                )
              )
            )
          )
        )
      )
    )
  )
)

(defcomponent addmodalname [data owner]
  ;; (will-mount
  ;;   (transact! data :selectedgroup  (fn [_] {:id "9ce9fce2-58d7-47bd-8e3e-0b4e2f8ee6c9", :name "jhghgjh", :parents nil, :owners nil, :current "khjhjkhjk"}))
  ;; )
  (render [_]
    (let [
      ;tr1 (.log js/console (str "name=" (:name (:selectedgroup @data))))
      ]
      (dom/div
        (dom/div {:id "nameModal" :className "modal fade" :role "dialog"}
          (dom/div {:className "modal-dialog"} 
            ;;Modal content
            (dom/div {:className "modal-content"} 
              (dom/div {:className "modal-header"} 
                (b/button {:type "button" :className "close" :data-dismiss "modal"})
                (dom/h4 {:className "modal-title"} (:modalTitle @app-state) )
              )
              (dom/div {:className "modal-body"}

                (dom/div {:className "panel panel-primary"}
                  (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                    (dom/h1 {:style {:text-align "center"}} (:current (:selectedgroup @data)))
                  )
                  (dom/div {:className "panel-body"}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-md-2" :style {:text-align "left"}}
                        "שם קבוצה"
                      )
                      (dom/div {:className "col-md-10"}
                        (dom/input {:id "name" :className "form-control" :type "text" :placeholder "שם קבוצה" :onChange (fn [e] (handlechange e)) :required true :value (:name (:selectedgroup @data))})
                      )
                    )
                  )
                )
              )
              (dom/div {:className "modal-footer"}
                (dom/div {:className "row"}
                  (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                    (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "ביטול")
                  )

                  (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                    (b/button {:id "btnsavegroup" :disabled? (if (= (:state @data) 1) true false) :type "button" :className (if (= (:state @data) 0) "btn btn-default" "btn btn-default m-progress" ) :onClick (fn [e] (savegroup))} "שמור")
                  )
                )
              )
            )
          )
        )
      )
    )
  )
)


(defn assigngroups [id]
  (let [
      group (first (filter (fn [x] (if (= (:id x) id) true false)) (:groups @shelters/app-state)))
      ]
    (swap! shelters/app-state assoc-in [:selectedgroup] group)
    (swap! shelters/app-state assoc-in [:state] 0)
    (swap! shelters/app-state assoc-in [:action] 1)
    (swap! shelters/app-state assoc-in [:selectedgroup :current] (str "שייך לקבוצה" (:name group)))
    (put! ch 48)
  )
)

(defn assignunits [id]
  (let [
      group (first (filter (fn [x] (if (= (:id x) id) true false)) (:groups @shelters/app-state)))
      ]
    (swap! shelters/app-state assoc-in [:selectedgroup] group)
    (swap! shelters/app-state assoc-in [:state] 0)
    (swap! shelters/app-state assoc-in [:action] 1)
    (swap! shelters/app-state assoc-in [:selectedgroup :current] (str "שייך לקבוצה" (:name group)))
    (put! ch 49)
  )
)


(defn showgroup [id]
  (let [
      group (first (filter (fn [x] (if (= (:id x) id) true false)) (:groups @shelters/app-state)))
      title (if (= -1 id) "הוסף קבוצה חדשה" (str "שינוי שם111" (:name group)))
     
      ]
    (swap! shelters/app-state assoc-in [:state] 0)
    (swap! shelters/app-state assoc-in [:selectedgroup] (if (nil? group) {:name ""} group))
    (swap! shelters/app-state assoc-in [:action] (if (= -1 id) 2 1))

    (swap! shelters/app-state assoc-in [:selectedgroup :current] title) 
    (put! ch 47)
  )
)


(defcomponent showgroups-view [data owner]
  (render
    [_]
    (let [
      ]
      (dom/div {:className "panel-body" :style {:padding-left "0px" :padding-right "0px" :padding-bottom "0px" :padding-top "0px" :border "1px solid"}}
        (map (fn [item]
          (let [
            unitscnt (count (shelters/getChildUnits (:id item) []))
            userscnt (count (shelters/getChildUsers (:id item) []))
            ]
            (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
              (dom/div {:className "col-md-1" :style {:text-align "center" :padding-right "12px" :padding-left "0px" :border-left "1px solid" :border-bottom "1px solid"}}
                (dom/div { :className "dropdown"}
                  (b/button {:className "btn btn-danger dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false" :style {:padding-top "3px" :padding-bottom "3px" :padding-left "6px" :padding-right "6px" :margin-top "3px" :margin-bottom "3px"}}
                    (dom/i {:className "fa fa-angle-down"}) "☰"
                  )
                  (dom/ul {:className "dropdown-menu" :aria-labelledby "dropdownMenuButton" :style {:min-width "120px"}}
                    ;; (dom/li {:className "dropdown-item" :style {:text-align "center"}}
                    ;;   (dom/div {:style {:padding-left "0px" :padding-right "5px" :font-weight "800"}}
                    ;;     "פעולות"
                    ;;   )
                    ;; )
                    ;; (dom/li {:className "divider"}
                    ;; )
                    (dom/li {:className "dropdown-item"}
                      (dom/span {:onClick (fn [e] (showgroup (:id item))) :style {:padding-left "0px" :padding-right "5px" :cursor "pointer"}}
                        "שינוי שם"
                      )
                    )
                    (dom/li {:className "dropdown-item"}
                      (dom/span { :onClick (fn [e] (assignunits (:id item))) :style {:padding-left "0px" :padding-right "5px" :cursor "pointer"}}
                        "שיוך ליחידה"
                      )
                    )
                    (dom/li {:className "dropdown-item"}
                      (dom/span { :onClick (fn [e] (assigngroups (:id item))) :style {:padding-left "0px" :padding-right "5px" :cursor "pointer"}}
                        "שיוך לקבוצת הורה"
                      )
                    )

                    (dom/li {:className "dropdown-item"}
                      (dom/span { :onClick (fn [e] (deletegroup (:id item))) :style {:padding-left "0px" :padding-right "5px" :cursor "pointer"}}
                        "למחוק קבוצה"
                      )
                    )
                  )
                )
              )
              (dom/div {:className "col-md-4" :style {:border-left "1px solid" :border-bottom "1px solid" :padding-top "7px" :padding-bottom "7px"}}
                (:name item)
              )

              (dom/div {:className "col-md-4" :style {:border-left "1px solid" :border-bottom "1px solid" :padding-top "7px" :padding-bottom "7px"}}
                userscnt
              )

              (dom/div {:className "col-md-3" :style {:border-left "1px solid transparent" :border-bottom "1px solid" :padding-top "7px" :padding-bottom "7px"}}
                unitscnt
              )
            )                  
            )
          )(sort (comp comp-groups) (filter (fn [x] (if (or (str/includes? (str/upper-case (if (nil? (:name x)) "" (:name x))) (str/upper-case (:search @data)))) true false)) (:groups @data)))
        )
      )
    )
  )
)

(defn setcontrols [value]
  (case value
    ;46 (setGroup)
    47 (go
         (<! (timeout 10))
         (openNameDialog)
       )
    48 (go
         (<! (timeout 10))
         (opengroupsdialog)
       )
    49 (go
         (<! (timeout 10))
         (openunitsdialog)
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
  ; (getGroups data)
  (swap! shelters/app-state assoc-in [:current] 
    "Groups"
  )
  (set! (.-title js/document) "ניהול קבוצות")
  (swap! shelters/app-state assoc-in [:view] 8)
)



(defcomponent topbuttons-view [data owner]
  (render [_]
    (dom/div

      (dom/div {:className "row" :style {:padding-top "60px" :border-bottom "solid 1px" :border-color "#e7e7e7"}}
        (dom/div {:className "col-xs-8" :style { :text-align "right" }}
          (dom/h3 "רשימת קבוצות")
        )

        (dom/div {:className "col-xs-4" :style {:margin-top "15px" :text-align "left"}}
          (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
          (b/button {:className "btn btn-primary" :style { :padding-left "5px" :padding-right "5px" :margin-left "10px"} :onClick (fn [e] (showgroup -1))} "הוספת קבוצה חדשה"
          )
          )
        )
      )

      (dom/div {:className "row" :style {:margin-right "0px"}}
        (dom/input {:id "search" :className "form-control" :type "text" :placeholder "חיפוש" :style {:margin-top "12px" :width "25%"} :value  (:search @shelters/app-state) :onChange (fn [e] (handleChange e )) })
      )
    )
  )
)


(defcomponent groups-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:padding-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div  {:className "container" :style {:margin-top "0px" :width "100%" :padding-left "50px" :padding-right "50px" :height "100%"}}
          (dom/div
            (om/build topbuttons-view data {})
            (dom/div {:className "panel-primary"}
              (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}

                  (dom/div {:className "col-md-1" :style {:text-align "center" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px" :border-left "1px solid"}}
                    "פעולות"
                  )
                  (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid"}}
                    (dom/h5 "שם קבוצה")
                  )

                  (dom/div {:className "col-md-4" :style {:text-align "center" :border-left "1px solid"}}
                    (dom/h5 "חברים בקבוצה")
                  )

                  (dom/div {:className "col-md-3" :style {:text-align "center" :border-left "1px solid transparent"}}
                    (dom/h5 "מספר יחידות")
                  )
                )
              )
              (om/build showgroups-view data {})
              (om/build addmodalname data {})
              (om/build addmodalgroups data {})
              (om/build addmodalunits data {})
            )
          )
        )
      )
    )
  )
)




(sec/defroute groups-page "/groups" []
  (om/root groups-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


