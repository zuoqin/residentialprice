(ns shelters.devdetail  (:use [net.unit8.tower :only [t]])
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
            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]
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

(defonce app-state (atom  {:device {} :isinsert false :view 1 :current "Device Detail"} ))

(defn comp-groups
  [group1 group2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)

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
      ;:content-type "application/json" 
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
  ;(.log js/console () e)
  (swap! app-state assoc-in [:role] value) 
)


(defn setRolesDropDown []
  (jquery
     (fn []
       (-> (jquery "#roles" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#roles" )
         (.selectpicker "val" (:role @app-state))
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


)


(defn setNewUnitValue [key val]
  (swap! app-state assoc-in [(keyword key)] val)
)



(defn setcontrols [value]
  (case value
    46 (setRolesDropDown)
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
  (getUnitDetail)
  (setcontrols 46)
)


(defn handle-change [e owner]
  ;(.log js/console () e)
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  ) 
)


(defn buildRolesList [data owner]
  (map
    (fn [text]
      (dom/option {:key (:name text) :value (:name text)
                    :onChange #(handle-change % owner)} (:name text))
    )
    (:roles @app-state )
  )
)

(defcomponent parentgroups-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item]
        (let [            
            isparent (if (and (nil? (:groups (:device @app-state)))) false (if (> (.indexOf (:groups (:device @app-state)) (:id item)) -1) true false))
          ]
          (dom/form
            (dom/label
              (:name item)
              (dom/input {:id (str "chckgroup" (:id item)) :type "checkbox" :checked isparent :onChange (fn [e] (handle-chkbsend-change e ))})
            )
          )
        )
      )
      (sort (comp comp-groups) (:groups @shelters/app-state)))
    )
  )

)

(defcomponent showcontacts-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item]
        (dom/div
          (dom/b
            (dom/i {:className "fa fa-user"} (:name item))
          )
          (dom/p (:tel item))

        )
      )
      (:contacts (:device @app-state)))
    )
  )
)

(defcomponent devdetail-page-view [data owner]
  (did-mount [_]
    (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:selectedcenter @shelters/app-state)) :lng (:lon (:selectedcenter @shelters/app-state))} "zoom" 12})
      map (js/google.maps.Map. map-canvas map-options)
      tr1 (swap! app-state assoc-in [:map] map)
      tr1 (.set map "disableDoubleClickZoom" true)
      ]
      (onMount data)

      (jquery
        (fn []
          (-> map
            (.addListener "dblclick"
              (fn [e]
                (.log js/console (str "LatLng=" (.. e -latLng)))

                (swap! app-state assoc-in [:device :lat] (.lat (.. e -latLng)))
                (swap! app-state assoc-in [:device :lon] (.lng (.. e -latLng)))
                (.stopPropagation (.. js/window -event))
                (.stopImmediatePropagation (.. js/window -event))
              )
            )
          )
        )
      )
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
        (dom/a {:onClick (fn[e] (.back (.-history js/window))) :className "btn btn-default btn-sm pull-right" :style {:margin-top "15px" :margin-left "5px"}} "Back"
          (dom/i {:className "fa fa-arrow-circle-right" :aria-hidden "true"})
        )
        (dom/button {:className "btn btn-default btn-sm pull-right" :style {:margin-top "15px"} :onClick (fn[e] (.print js/window))}
          (dom/i {:className "fa fa-print" :aria-hidden "true"})
        )
        (if (:isinsert @data)

          (dom/h3
            (dom/i {:className "fa fa-cube"})
            (str "Device Info - " (:id (:device @app-state)) )
          )
        )

        (dom/div {:className "col-xs-3"}
          (dom/div {:style {:border "2px" :min-height "300px" :padding "15px" :border-radius "10px"}}
             (dom/h5 "Controller Id: "
               (dom/input {:id "controller" :type "text" :onChange (fn [e] (handleChange e)) :value (:controller (:device @data))} )
             )          

            (dom/h5 {:style {:display:inline true}} "Status: "
              (dom/i {:className "fa fa-toggle-off" :style {:color "#ff0000"}})
            )
            (dom/h5 {:style {:display:inline true}} "Name: "
               (dom/input {:id "name" :type "text" :onChange (fn [e] (handleChange e)) :value (:name (:device @data))})
            )
            (dom/h5 {:style {:display:inline true}} (str "Address: ")
               (dom/input {:id "address" :type "text" :onChange (fn [e] (handleChange e)) :value (:address (:device @data))})
            )
            (dom/h5 {:style {:display:inline true}} "Latitude: "
               (:lat (:device @data))
               ;(dom/input {:id "lat" :type "number" :step "0.00001" :onChange (fn [e] (handleChange e)) :value (:lat (:device @data))} )
            )
            (dom/h5 {:style {:display:inline true}} "Longitude: "
               (:lon (:device @data))
               ;(dom/input {:id "lon" :type "number" :step "0.00001" :onChange (fn [e] (handleChange e)) :value (:lon (:device @data))} )
            )

            (dom/div {:className "row maprow" :style {:padding-top "0px" :height "400px"}}
              (dom/div  {:className "col-12 col-sm-12" :id "map" :style {:margin-top "0px" :height "100%"}})
              ;(b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (addMarkers))} "Add marker")
            )
            (dom/h4 {:style {:display:inline true}} "Sensors"
              (dom/table {:className "table table-responsive"}
                (dom/tbody
                  (dom/tr
	                (dom/td
	                  (dom/i {:className "fa fa-battery-three-quarters" :id "io1_" :style {:color "#ff0000"}})
	                )
	                (dom/td
	                  "Battery power"
	                )
                  )
                  (dom/tr {:className "hidden"}
                  )
                )
              )

            )
            (dom/h4
              (dom/i {:className "fa fa-phone"} "Contacts:")
            )
            (om/build showcontacts-view data {})
            (om/build parentgroups-view data {})

            (dom/div

              (b/button {:className "btn btn-default" :onClick (fn [e] (if (:isinsert @app-state) (createUnit) (updateUnit)) )} (if (:isinsert @app-state) "Insert" "Update"))
              (b/button {:className "btn btn-danger" :style {:visibility (if (:isinsert @app-state) "hidden" "visible")} :onClick (fn [e] (deleteUnit))} "Delete")

              (b/button {:className "btn btn-info" :onClick (fn [e]
                ;(shelters/goDashboard e)
                (js/window.history.back)
                )  } "Cancel"
              )
            )
          )
        )

        (dom/div {:className "col-xs-9"}
	)
      )
    )
  )
)





(sec/defroute devdetail-page "/devdetail/:devid" [devid]
  (let[
      dev (first (filter (fn [x] (if (= (str devid) (:id x)) true false)) (:devices @shelters/app-state)))
      tr1 (swap! app-state assoc-in [:device] dev )
      ;tr2 (.log js/console "hjkhkh")
    ]
    
    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(sec/defroute devdetail-new-page "/devdetail" {}
  (
    (swap! app-state assoc-in [:device]  {:id "" :lat 32.08088 :lon 34.78057 :port 666 :contacts [{:tel "121234"}]} ) 
    (swap! app-state assoc-in [:isinsert]  true )
    (swap! shelters/app-state assoc-in [:view] 7) 
    ;(swap! app-state assoc-in [:group ]  "group" ) 
    ;(swap! app-state assoc-in [:password] "" )
    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
