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
(defonce app-state (atom  {:group {} :isinsert false :view 1 :current "Group Detail"} ))


(defn comp-groups
  [group1 group2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)



(defn handleChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [:group (keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
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
      newgroups (remove (fn [group] (if (= (:id group) (:id (:group @app-state))) true false  )) groups)
    ]
    ;(swap! sbercore/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:groups] newgroups)
  )

  (-> js/document
    .-location
    (set! "#/groups"))

  (shelters/goGroups "")
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
      delgroup (remove (fn [group] (if (= (:id group) (:id (:group @app-state))) true false)) groups)
      addgroup (conj delgroup (:group @app-state))

      tr1 (.log js/console (str "In OnUpdateGroupSuccess " response))
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)
    (shelters/goGroups nil)
  )
)


(defn deleteGroup [group]
  (DELETE (str settings/apipath  "deleteGroup?groupId=" (:id (:group @app-state))) {
    :handler OnDeleteGroupSuccess
    :error-handler OnDeleteGroupError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
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
        :token (:token (:token @shelters/app-state))}
      :format :json
      :params {:groupName (:name (:group @app-state)) :groupId (:id (:group @app-state)) :parentGroups (:parents (:group @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :details [{:key "key1" :value "44"} {:key "key2" :value "444"}]}})
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
      tr1 (swap! app-state assoc-in [:group :id] (get response "groupId"))
      groups (:groups @shelters/app-state)
      addgroup (conj groups (:group @app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)
    (-> js/document
      .-location
      (set! "#/groups"))
    (shelters/goGroups "")
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
        :token (str "" (:token (:token @shelters/app-state)))}
      :format :json
      :params { :groupName (:name (:group @app-state)) :groupId "" :parentGroups (:parents (:group @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :details [{:key "key" :value "value"}] }})
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

(defn handle-chkbsend-change [e]
  (let [
      id (str/join (drop 9 (.. e -currentTarget -id)))
      groups (:parents (:group @app-state))
      newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! app-state assoc-in [:group :parents] newgroups)
  )
)

(defcomponent parentgroups-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item]
        (let [            
            isparent (if (and (nil? (:parents (:group @app-state)))) false (if (> (.indexOf (:parents (:group @app-state)) (:id item)) -1) true false))
          ]
          (dom/form
            (dom/label
              (:name item)
              (dom/input {:id (str "chckgroup" (:id item)) :type "checkbox" :checked isparent :onChange (fn [e] (handle-chkbsend-change e ))})
            )
          )
        )
      )
      (sort (comp comp-groups) (filter (fn [x] (if (= (:id x) (:id (:group @app-state))) false true)) (:groups @shelters/app-state))))
    )
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
      styleprimary {:style {:padding-top "70px"}}
      ;tr1 (.log js/console (str "name= " @data))
      ]
      (dom/div
        (om/build shelters/website-view shelters/app-state {})
        (dom/div {:id "user-detail-container"}
          (dom/span
            (dom/div  (assoc styleprimary  :className "panel panel-default"  :id "divUserInfo")
              
              (dom/div {:className "panel-heading"}
                (dom/h5 "Name: " 
                  (dom/input {:id "name" :type "text" :onChange (fn [e] (handleChange e)) :value (:name (:group @data))} )
                )                
                (om/build parentgroups-view data {})
              )
            )
          )
        )
        (dom/nav {:className "navbar navbar-default" :role "navigation"}
          (dom/div {:className "navbar-header"}
            (b/button {:className "btn btn-default" :onClick (fn [e] (if (:isinsert @app-state) (createGroup) (updateGroup)) )} (if (:isinsert @app-state) "Insert" "Update"))
            (b/button {:className "btn btn-danger" :style {:visibility (if (:isinsert @app-state) "hidden" "visible")} :onClick (fn [e] (deleteGroup (:id (:group @data))))} "Delete")

            (b/button {:className "btn btn-info" :onClick (fn [e] (-> js/document
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
