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
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
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
      users (:users @shelters/app-state    )  
      newdevs (remove (fn [user] (if (= (:login user) (:login @app-state) ) true false  )) users)
    ]
    ;(swap! tripcore/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:devs] newdevs)
  )

    (-> js/document
      .-location
      (set! "#/users"))
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
      users (:users @shelters/app-state    )  
      deluser (remove (fn [user] (if (= (:login user) (:login @app-state) ) true false  )) users)
      adduser (into [] (conj deluser {:login (:login @app-state) :password (:password @app-state) :role (:role @app-state)}  )) 
    ]
    (swap! shelters/app-state assoc-in [:users] adduser)

    (-> js/document
      .-location
      (set! "#/users"))

  )
)


(defn deleteUnit [login]
  (DELETE (str settings/apipath  "api/user?login=" login) {
    :handler OnDeleteUnitSuccess
    :error-handler OnDeleteUnitError
    :headers {
      :content-type "application/json" 
      :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateUnit []
  (PUT (str settings/apipath  "updateUnit") {
    :handler OnUpdateUnitSuccess
    :error-handler OnUpdateUnitError
    :headers {
      :content-type "application/json" 
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:device @app-state)) :controllerId (:name (:device @app-state)) :name (:name (:device @app-state)) :parentGroups (:groups (:device @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip "1.2.3.4" :port 5000 :latitude (:lat (:device @app-state)) :longitude (:lon (:device @app-state)) :details [{:key "address" :value (:address (:device @app-state))} {:key "phone" :value (:tel (first (:contacts (:device @app-state))))}]}})
)


(defn OnCreateUnitError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnCreateUnitSuccess [response]
  (let [
      users (:users @shelters/app-state    )  
      adddev (into [] (conj users {:login (:login @app-state) :password (:password @app-state) :role (:role @app-state)} )) 
    ]
    (swap! shelters/app-state assoc-in [:users] adddev)

    (-> js/document
      .-location
      (set! "#/users"))

  )
)

(defn createUnit []
  (POST (str settings/apipath  "api/user") {
    :handler OnCreateUnitSuccess
    :error-handler OnCreateUnitError
    :headers {
      :content-type "application/json" 
      :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
    :format :json
    :params { :login (:login @app-state) :password (:password @app-state) :role (:role @app-state) }})
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
    (onMount data)
  )
  (did-update [this prev-props prev-state]
    (.log js/console "Update happened") 

    ;(put! ch 46)
  )
  (render
    [_]
    (let [style {:style {:margin "10px;" :padding-bottom "0px;"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view shelters/app-state {})
        (dom/div {:style {:margin-top "70px"}})
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
               (dom/input {:id "id" :type "text" :disabled (if (:isinsert @data) false true) :onChange (fn [e] (handleChange e)) :value (:name (:device @data))} )

             )          

            (dom/h5 {:style {:display:inline true}} "Status: "
              (dom/i {:className "fa fa-toggle-off" :style {:color "#ff0000"}})
            )
            (dom/h5 {:style {:display:inline true}} (str "Name: " (:name (:device @app-state))))
            (dom/h5 {:style {:display:inline true}} (str "Address: " (:address (:device @app-state))))
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
              (b/button {:className "btn btn-danger" :style {:visibility (if (:isinsert @app-state) "hidden" "visible")} :onClick (fn [e] (deleteUnit (:name @app-state)))} "Delete")

              (b/button {:className "btn btn-info"  :onClick (fn [e] (shelters/goDashboard e))  } "Cancel")
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
    (swap! app-state assoc-in [:device]  {} ) 
    (swap! app-state assoc-in [:isinsert]  true )
    (swap! shelters/app-state assoc-in [:view] 7) 
    ;(swap! app-state assoc-in [:group ]  "group" ) 
    ;(swap! app-state assoc-in [:password] "" )


    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
