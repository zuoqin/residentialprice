(ns shelters.groupdetail  (:use [net.unit8.tower :only [t]])
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
(defonce app-state (atom  {:group {} :parentgroup "" :isinsert false :view 1 :current "Group Detail"} ))

(defn handleChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
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
      newgroups (remove (fn [group] (if (= (:group group) (:group @app-state) ) true false  )) groups)
    ]
    ;(swap! sbercore/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:groups] newgroups)
  )

    (-> js/document
      .-location
      (set! "#/groups"))
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
      delgroup (remove (fn [group] (if (= (:name group) (:name (:group @app-state))) true false)) groups)
      addgroup (into [] (conj delgroup {:name (:name (:group @app-state))}))

      tr1 (.log js/console (str "In OnUpdateGroupSuccess " response))
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)

    (-> js/document
      .-location
      (set! "#/groups"))
  )
)


(defn deleteGroup [group]
  (DELETE (str settings/apipath  "api/user?login=" group) {
    :handler OnDeleteGroupSuccess
    :error-handler OnDeleteGroupError
    :headers {
      :content-type "application/json" 
      :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateGroup []
  (let [
    tr1 (.log js/console (str "In updateGroup"))
    ]
    (PUT (str settings/apipath  "updateGroup") {
      :handler OnUpdateGroupSuccess
      :error-handler OnUpdateGroupError
      :headers {
        :content-type "application/json" 
        :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
      :format :json
      :params {:groupName (:name @app-state) :groupLevel 0 :groupDescription (:description @app-state) }})
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
      groups (:groups @shelters/app-state    )  
      addgroup (into [] (conj groups {:group (:group @app-state) :description (:description @app-state)})) 
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)

    (-> js/document
      .-location
      (set! "#/groups"))

  )
)

(defn createGroup []
  (let [
    tr1 (.log js/console (str "In updateGroup"))
    ]
    (POST (str settings/apipath  "addGroup") {
      :handler OnCreateGroupSuccess
      :error-handler OnCreateGroupError
      :headers {
        :content-type "application/json" 
        :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
      :format :json
      :params { :groupName (:name @app-state) :groupLevel 0 :groupDescription (:description @app-state) }})
  )
)


(defn onDropDownChange [id value]
  ;(.log js/console () e)
  (swap! app-state assoc-in [:parentgroup] value) 
)


(defn setGroupsDropDown []
  (jquery
     (fn []
       (-> (jquery "#groups" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#groups" )
         (.selectpicker "val" (:parent (:group @app-state)))
         (.on "change"
           (fn [e]
             (let []
               ;(.log js/console (.val (jquery "#groups" )))
               (onDropDownChange (.. e -target -id) (.val (jquery "#groups" )))
             )             
           )
         )
       )
     )
   )
)


(defn setNewGroupValue [key val]
  (swap! app-state assoc-in [(keyword key)] val)
)

(defn setGroup []
  (let [
    ;roles (:roles @shelters/app-state)        
    group (first (filter (fn [group] (if (= (:name @app-state) (:name group)  )  true false)) (:groups @shelters/app-state )))

    ;tr1 (.log js/console (str "role=" role))
      
    ]
    (setGroupsDropDown)
    ;(swap! app-state assoc-in [:name ]  (:name group) ) 
  )
)


(defn setcontrols [value]
  (case value
    46 (setGroup)
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




(defn OnError [response]
  (let [     
      newdata { :error (get (:response response)  "error") }
    ]
    (.log js/console (str  response )) 
    
  )
  
  
)


(defn getGroupDetail []
  ;(.log js/console (str "token: " " " (:token  (first (:token @t5pcore/app-state)))       ))
  (if
    (and 
      (not= (:group @app-state) nil)
      (not= (:group @app-state) "")
    )
    (setGroup)
  )
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  (.log js/console "The change ....")

)


(defn onMount [data]
  (swap! app-state assoc-in [:current] 
    "Group Detail"
  )
  (getGroupDetail)
  (put! ch 46)
)


(defn handle-change [e owner]
  ;(.log js/console () e)
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  ) 
)


(defn buildGroupsList [data owner]
  (map
    (fn [group]
      (dom/option {:key (:id group) :value (:id group)
                    :onChange #(handle-change % owner)} (:name group))
    )
    (filter (fn [x] (if (= (:id x) (:id (:group @data))) false true)) (:groups @shelters/app-state))
  )
)



(defcomponent groupdetail-page-view [data owner]
  (did-mount [_]
    (onMount data)
  )
  (did-update [this prev-props prev-state]
    (.log js/console "Update happened") 
  )
  (render
    [_]
    (let [style {:style {:margin "10px;" :padding-bottom "0px;"}}
      styleprimary {:style {:margin-top "70px"}}
      ;tr1 (.log js/console (str "name= " @data))
      ]
      (dom/div
        (om/build shelters/website-view shelters/app-state {})
        (dom/div {:id "user-detail-container"}
          (dom/span
            (dom/div  (assoc styleprimary  :className "panel panel-default"  :id "divUserInfo")
              
              (dom/div {:className "panel-heading"}
                (dom/h5 "Name: " 
                  (dom/input {:id "groupname" :type "text" :disabled (if (:isinsert @data) false false) :onChange (fn [e] (handleChange e)) :value (:name (:group @data))} )

                )
                
                (dom/h5 "Description: "
                  (dom/input {:id "description" :type "text" :onChange (fn [e] (handleChange e)) :value (:description @data)})
                )

                (dom/div {:className "form-group"}
                  (dom/p
                    (dom/label {:className "control-label" :for "groups" }
                      "Parent Group: "
                    )
                  
                  )
                 
                  (omdom/select #js {:id "groups"
                                     :multiple true
                                     :className "selectpicker"
                                     :data-show-subtext "true"
                                     :data-live-search "true"
                                     :onChange #(handle-change % owner)
                                     }                
                    (buildGroupsList data owner)
                  )
                  
                )
                ;; (dom/h5 "Group: "
                ;;   (dom/input {:id "role" :type "text" :value (:role @app-state)})
                ;; )
              )
            )
          )
        )
        (dom/nav {:className "navbar navbar-default" :role "navigation"}
          (dom/div {:className "navbar-header"}
            (b/button {:className "btn btn-default" :onClick (fn [e] (if (:isinsert @app-state) (createGroup) (updateGroup)) )} (if (:isinsert @app-state) "Insert" "Update"))
            (b/button {:className "btn btn-danger" :style {:visibility (if (:isinsert @app-state) "hidden" "visible")} :onClick (fn [e] (deleteGroup (:id (:group @data))))} "Delete")

            (b/button {:className "btn btn-info"  :onClick (fn [e] (-> js/document
      .-location
      (set! "#/groups")))  } "Cancel")
          )
        )
      )
    )

  )
)



(sec/defroute groupdetail-page "/groupdetail/:group" {group :group}  
  (let [
    thegroup (first (filter (fn [x] (if (= (:id x) group) true false)) (:groups @shelters/app-state)))
    ]
    (swap! app-state assoc-in [:group] thegroup)
    (swap! app-state assoc-in [:isinsert]  false )
    (setGroup)
    (om/root groupdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(sec/defroute groupdetail-new-page "/groupdetail" {}
  (
    (swap! app-state assoc-in [:group]  {} ) 
    (swap! app-state assoc-in [:isinsert]  true )
 
    ;(swap! app-state assoc-in [:group ]  "group" ) 
    ;(swap! app-state assoc-in [:password] "" )


    (om/root groupdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
