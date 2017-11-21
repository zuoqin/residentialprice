(ns shelters.unitdetail  (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST PUT DELETE]]
            [clojure.string :as str]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! timeout]]
            [om-bootstrap.input :as i]
            [cljs-time.core :as tm]
            [cljs-time.format :as tf]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(def jquery (js* "$"))

(enable-console-print!)

(def ch (chan (dropping-buffer 2)))

(def iconBase "/images/")

(defonce app-state (atom  {:device {} :marker nil :isinsert false :view 1 :current "Device Detail"} ))

(defn comp-groups
  [group1 group2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)

(defn drop-nth [n coll]
   (keep-indexed #(if (not= %1 n) %2) coll))


(defn handle-chkbsend-change [e]
  (let [
      id (str/join (drop 9 (.. e -currentTarget -id)))
      groups (:groups (:device @app-state))
      newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! app-state assoc-in [:device :groups] newgroups)
  )
)


(defn handleChange [e]
  (.log js/console (.. e -nativeEvent -target)  )  
  (.log js/console (.. e -nativeEvent -target -step))
  (swap! app-state assoc-in [:device (keyword (.. e -nativeEvent -target -id))] (if (= "" (.. e -nativeEvent -target -step)) (.. e -nativeEvent -target -value) (js/parseFloat (.. e -nativeEvent -target -value))))
)


(defn OnDeleteUnitError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteUnitSuccess [response]
  (let [
      units (:devices @shelters/app-state)
      newunits (remove (fn [unit] (if (= (:id unit) (:id (:device @app-state)) ) true false)) units)
    ]
    ;(swap! tripcore/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:devices] newunits)
    ;(shelters/goDashboard "")
    (js/window.history.back)
  )
)

(defn OnUpdateUnitError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateUnitSuccess [response]
  (let [
      units (:devices @shelters/app-state)
      delunit (remove (fn [unit] (if (= (:id unit) (:id (:device @app-state)) ) true false  )) units)
      addunit (conj delunit (:device @app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:devices] addunit)
    ;(shelters/goDashboard nil)
    (js/window.history.back)
  )
)


(defn deleteUnit []
  (DELETE (str settings/apipath  "deleteUnit?unitId=" (:id (:device @app-state))) {
    :handler OnDeleteUnitSuccess
    :error-handler OnDeleteUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateUnit []
  (PUT (str settings/apipath  "updateUnit") {
    :handler OnUpdateUnitSuccess
    :error-handler OnUpdateUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:device @app-state)) :controllerId (:controller (:device @app-state)) :name (:name (:device @app-state)) :parentGroups (:groups (:device @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip "1.2.3.4" :port (:port (:device @app-state)) :latitude (:lat (:device @app-state)) :longitude (:lon (:device @app-state)) :details [{:key "address" :value (:address (:device @app-state))} {:key "phone" :value (:tel (first (:contacts (:device @app-state))))}]}})
)


(defn OnCreateUnitError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn map-unit [unit]
  (let [
    controller (str (get unit "controllerId"))
    name (if (nil? (get unit "name")) controller (get unit "name"))
    port (get unit "port")
    status (case (get unit "status") "Normal" 0 3)
    lat (get unit "latitude")
    lon (get unit "longitude")
    groups (get unit "parentGroups")
    unitid (str (get unit "unitId"))    
    address (get (first (filter (fn [x] (if (= (get x "key") "address") true false)) (get unit "details"))) "value" )
    phone (get (first (filter (fn [x] (if (= (get x "key") "phone") true false)) (get unit "details"))) "value" )
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :lat lat :lon lon :port port :groups groups :contacts [{:tel phone}]}
    ]
    ;
    result
  )
)

(defn OnCreateUnitSuccess [response]
  (let [
      unit (map-unit response)
      units (:devices @shelters/app-state)
      addunit (conj units unit)
    ]
    (swap! shelters/app-state assoc-in [:devices] addunit)
    ;(shelters/goDashboard "")
    (js/window.history.back)
  )
)

(defn createUnit []
  (POST (str settings/apipath  "addUnit") {
    :handler OnCreateUnitSuccess
    :error-handler OnCreateUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:device @app-state)) :controllerId (:controller (:device @app-state)) :name (:name (:device @app-state)) :parentGroups (:groups (:device @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip "1.2.3.4" :port (:port (:device @app-state)) :latitude (:lat (:device @app-state)) :longitude (:lon (:device @app-state)) :details [{:key "address" :value (:address (:device @app-state))} {:key "phone" :value (:tel (first (:contacts (:device @app-state))))}]}})
)


(defn onDropDownChange [id value]
  (let [
    newid (js/parseInt (subs id 7))


    addcontact (first (filter (fn [x] (if (= (:id x) value) true false)) (:contacts @shelters/app-state)))

    ;tr1 (.log js/console (str "id=" id " newid=" newid " value=" value " add=" addcontact))
    newcontacts (conj (take newid (:contacts (:device @app-state))) addcontact)

    newcontacts (flatten (if (> (count (:contacts (:device @app-state))) (+ newid 1)) (reverse (conj newcontacts (drop (+ newid 1) (:contacts (:device @app-state))))) (reverse newcontacts)))
    ]
    (swap! app-state assoc-in [:device :contacts] newcontacts)
  )
  ;(.log js/console (str "id=" id " value=" value))  
)


(defn setContactsDropDown []
  (doall
    (map (fn [item num]
      (let []
        (jquery
          (fn []
            (-> (jquery (str "#contact" num))
              (.selectpicker {})
            )
          )
        )
        (jquery
          (fn []
            (-> (jquery (str "#contact" num))
              (.selectpicker "val" (:id (first (filter (fn [x] (if (= (:id x) (:id item)) true false)) (:contacts @shelters/app-state)))))
              (.on "change"
                (fn [e]
                  (
                    onDropDownChange (.. e -target -id) (.. e -target -value)
                  )
                )
              )
            )
          )
        )
      ))
      (:contacts @shelters/app-state) (range)
    )
  ) 
)

(defn setNewUnitValue [key val]
  (swap! app-state assoc-in [(keyword key)] val)
)



(defn setcontrols [value]
  (case value
    46 (setContactsDropDown)
    ;; 43 (go
    ;;      (<! (timeout 100))
    ;;      (addsearchbox)
    ;;    )
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

(defn array-to-string [element]
  (let [
      newdata {:empname (get element "empname") } 
    ]
    (:empname newdata)
  )
)

(defn setUnit []
  (let [
        users (:users @shelters/app-state)
        user (first (filter (fn [user] (if (= (:login @app-state) (:login user)  )  true false)) (:users @shelters/app-state )))
        ]
    (swap! app-state assoc-in [:login ]  (:login user) ) 
    (swap! app-state assoc-in [:role ]  (:role user) ) 
    (swap! app-state assoc-in [:password] (:password user) )
  )
)




(defn OnError [response]
  (let [     
      newdata { :error (get (:response response)  "error") }
    ]
    (.log js/console (str  response )) 
    
  )
  
  
)


(defn getUnitDetail []
  ;(.log js/console (str "token: " " " (:token  (first (:token @t5pcore/app-state)))       ))
  (if
    (and 
      (not= (:login @app-state) nil)
      (not= (:login @app-state) "")
    )
    (setUnit)
  
  )
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  (.log js/console "The change ....")

)


(defn onMount [data]
  (swap! app-state assoc-in [:current] 
    "Unit Detail"
  )
  (set! (.-title js/document) "Unit Detail")
  (getUnitDetail)
  (setcontrols 46)
  (put! ch 43)
)


(defn handle-change [e owner]
  (.log js/console e)
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  )
)


(defn buildContactList [data owner]
  (map
    (fn [text]
      (dom/option {:key (:id text) :value (:id text)
                    :onChange #(handle-change % owner)} (:name text))
    )
    (:contacts @shelters/app-state )
  )
)


(defcomponent showcontacts-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item num]
        (dom/div
          (dom/div {:className "row"}
            (dom/div {:className "col-xs-5"})
            (dom/div {:className "col-xs-2"} (dom/h5 (str "Contact " (+ num 1) ":")))
            (dom/div {:className "col-xs-2"}
              (omdom/select #js {:id (str "contact" num)
                                 :className "selectpicker"
                                 :data-show-subtext "true"
                                 :data-live-search "true"
                                 :onChange #(handle-change % owner)
                                 }
                (buildContactList data owner)
              )
            )
          )
          ;; (dom/b
          ;;   (dom/i {:className "fa fa-user"} (:name item))
          ;; )
          ;; (dom/p (:tel item))
        )
      )
      (:contacts (:device @app-state)) (range))
    )
  )
)

(defcomponent exercise-table [data owner]
  (render [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (let []
          (dom/div {:className "row" :style {}}
            (dom/div {:className "col-xs-4" :style {}}
              "4 minutes ago"
            )
            (dom/div {:className "col-xs-4" :style {}}
              (:id item)
            )
            (dom/div {:className "col-xs-4" :style {}}
              (:text item)
            )
          )
        ))
      (:alerts @shelters/app-state))
    )
  )
)


(defcomponent alerts-table [data owner]
  (render [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (let []
          (dom/div {:className "row" :style {}}
            (dom/div {:className "col-xs-4" :style {}}
              (b/button {:className "btn btn-primary" :onClick (fn [e])} "seen")
            )
            (dom/div {:className "col-xs-4" :style {}}
              (dom/a {:href (str "/#/unitdetail/" (:id (first (:devices @app-state)))) }                
                (:id item)
              )
            )
            (dom/div {:className "col-xs-4" :style {}}
              (dom/a {:href (str "/#/unitdetail/" (:id (first (:devices @app-state)))) }                
                (:text item)
                (dom/span {:className "pull-right text-muted small"}
                  "  4 minutes ago"
                )
              )
            )
          )
        ))

      [])
    )
  )
)

(defcomponent devdetail-page-view [data owner]
  (did-mount [_]
    (let [
      ]
      (onMount data)
    )
  )
  (did-update [this prev-props prev-state]
    (.log js/console "Update happened") 

    ;(put! ch 46)
  )
  (render
    [_]
    (let [style {:style {:margin "10px;" :padding-bottom "0px;"}}
      ]
      (dom/div {:style {:padding-top "70px"}}
        (om/build shelters/website-view shelters/app-state {})
        
        (dom/h3 {:style {:text-align "center"}}
          (dom/i {:className "fa fa-cube"})
          (str "Device Info - " (:controller (:device @app-state)) )
        )
        (dom/div {:className "row"}
          (dom/div {:className "col-xs-3"}
            (dom/div {:className "row"}
              (dom/h5 "Controller Id: " (:controller (:device @data)))

              (dom/h5 {:style {:display:inline true}} "Status: " (dom/i {:className "fa fa-toggle-off" :style {:color "#ff0000"}})
              )

              (dom/h5 "Name: " (:name (:device @data)))

              (dom/h5 "Address: " (:address (:device @data)))
            )

            (dom/div {:className "row"}
              (dom/h5 "Controller Id: " (:controller (:device @data)))

              (dom/h5 {:style {:display:inline true}} "Status: " (dom/i {:className "fa fa-toggle-off" :style {:color "#ff0000"}})
              )

              (dom/h5 "Name: " (:name (:device @data)))

              (dom/h5 "Address: " (:address (:device @data)))
            )
          )

          (dom/div {:className "col-xs-8"}
            (dom/div {:className "row"}
              (dom/div {:style {:text-align "center"}} (dom/h2 (str "Alerts History")))
              (dom/div {:className "col-xs-6 panel panel-primary" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin "0px"}}
                  "Exercises"
                )
                (om/build exercise-table data {})
              )

              (dom/div {:className "col-xs-6 panel panel-primary" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin "0px"}}
                  "Real world"
                )
                (om/build alerts-table data {})
              )
            )


            (dom/div {:className "row"}
              (dom/div {:style {:text-align "center"}} (dom/h2 (str "Notifications History")))
              (dom/div {:className "col-xs-6 panel panel-primary" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin "0px"}}
                  "Exercises"
                )
                (om/build exercise-table data {})
              )

              (dom/div {:className "col-xs-6 panel panel-primary" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin "0px"}}
                  "Real world"
                )
                (om/build alerts-table data {})
              )
            )

          )

          (dom/div {:className "col-xs-1"}
          )

        )


        (dom/div {:className "row"}
          (b/button {:className "btn btn-default" :disabled? (or (< (count (:controller (:device @data))) 1)  (< (count (:address (:device @data))) 1) (< (count (:name (:device @data))) 1) ) :onClick (fn [e] (if (:isinsert @app-state) (createUnit) (updateUnit)) )} (if (:isinsert @app-state) "Insert" "Update"))
          (b/button {:className "btn btn-danger" :style {:visibility (if (:isinsert @app-state) "hidden" "visible")} :onClick (fn [e] (deleteUnit))} "Delete")

          (b/button {:className "btn btn-info" :onClick (fn [e]
            ;(shelters/goDashboard e)
            (js/window.history.back)
            )  } "Cancel"
          )
        )
      )
    )
  )
)





(sec/defroute unitdetail-page "/unitdetail/:devid" [devid]
  (let[
      dev (first (filter (fn [x] (if (= (str devid) (:id x)) true false)) (:devices @shelters/app-state)))       
      ;tr2 (.log js/console "hjkhkh")
    ]
    (swap! app-state assoc-in [:device] dev )
    (swap! app-state assoc-in [:isinsert] false)
    (swap! shelters/app-state assoc-in [:view] 7)
    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
