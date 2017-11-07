(ns shelters.notedetail  (:use [net.unit8.tower :only [t]])
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

(defonce app-state (atom  {:note {} :search "" :view 1 :current "Notification Detail"} ))


(defn comp-notifications
  [note1 note2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:type note1) (:type note2)) 0)
      false
      true
  )
)

(defn comp-users
  [user1 user2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:login user1) (:login user2)) 0)
      false
      true
  )
)


(defn handleChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [:note (keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)

(defn handleSrcChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))

  (put! ch 47)
)

(defn OnDeleteNoteError [response]
  (let [     
      newdata {:noteid (get response (keyword "noteid") ) }
    ]

  )
  ;; TO-DO: Delete Group from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteNoteSuccess [response]
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

(defn OnUpdateNoteError [response]
  (let [     
      ;newdata {:tripid (get response (keyword "tripid") ) }
      tr1 (.log js/console (str "In OnUpdateNoteError " response))
    ]
  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateNoteSuccess [response]
  (let [
      notes (:notifications @shelters/app-state)
      delnote (remove (fn [note] (if (= (:id note) (:id (:note @app-state))) true false)) notes)
      addnote (conj delnote (:note @app-state))

      tr1 (.log js/console (str "In OnUpdateNoteSuccess " response))
    ]
    (swap! shelters/app-state assoc-in [:notifications] addnote)
    ;(shelters/goGroups nil)
  )
)


(defn deleteNote [note]
  (DELETE (str settings/apipath  "deleteGroup?groupId=" (:id (:group @app-state))) {
    :handler OnDeleteNoteSuccess
    :error-handler OnDeleteNoteError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateNote []
  (let [
    ;tr1 (.log js/console (str "In updateGroup"))
    ]
    (PUT (str settings/apipath  "updateNotification") {
      :handler OnUpdateNoteSuccess
      :error-handler OnUpdateNoteError
      :headers {
        :token (:token (:token @shelters/app-state))}
      :format :json
      :params {:notificationId (:id (:note @app-state)) :notificationType (:type (:note @app-state)) :unitId (:unitid (:note @app-state)) :openTime (:openTime (:note @app-state)) :acceptanceTime (:acceptanceTime (:note @app-state)) :closeTime (:closeTime (:note @app-state)) :userId (:userid (:token @shelters/app-state)) :status "new"}})
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

(defn setcheckboxtoggle []
  (let []
    (doall
      (map (fn [item]
        (let []
          (jquery
            (fn []
               (-> (jquery (str "#chckgroup" (:id item)))
                 (.bootstrapToggle (clj->js {:on "הוסיף" :off "לא נוסף"}))
               )
            )
          )

          (jquery
            (fn []
              (-> (jquery (str "#chckgroup" (:id item)))
                (.on "change"
                  (fn [e]
                    (let [
                        id (str/join (drop 9 (.. e -currentTarget -id)))
                        groups (:parents (:group @app-state))
                        newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))

                        ;tr1 (.log js/console "gg")
                      ]
                      (.stopPropagation e)
                      ;(.stopImmediatePropagation (.. e -nativeEvent) )
                      (swap! app-state assoc-in [:group :parents] newgroups)
                    )
                  )
                )
              )
            )
          )
        )
        )
        (:groups @shelters/app-state)
      )
    )

    (doall
      (map (fn [item]
        (let []
          (jquery
            (fn []
               (-> (jquery (str "#chckbuser" (:userid item)))
                 (.bootstrapToggle (clj->js {:on "הוסיף" :off "לא נוסף"}))
               )
            )
          )

          (jquery
            (fn []
              (-> (jquery (str "#chckbuser" (:userid item)))
                (.on "change"
                  (fn [e]
                    (let [
                        id (str/join (drop 9 (.. e -currentTarget -id)))
                        users (:owners (:group @app-state))
                        newusers (if (= true (.. e -currentTarget -checked)) (conj users id) (remove (fn [x] (if (= x id) true false)) users))

                        ;tr1 (.log js/console "gg")
                      ]
                      (.stopPropagation e)
                      ;(.stopImmediatePropagation (.. e -nativeEvent) )
                      (swap! app-state assoc-in [:group :owners] newusers)
                    )
                  )
                )
              )
            )
          )
        )
        )
        (:users @shelters/app-state)
      )
    )
  )
)

(defn setcontrols [value]
  ;; (case value
  ;;   46 (setGroup)
  ;;   47 (setcheckboxtoggle)
  ;; )
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
  (swap! app-state assoc-in [:current] "Group Detail")
  ;(getGroupDetail)
  ;(put! ch 46)
  ;(put! ch 47)
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

      tr1 (.log js/console "gg")
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! app-state assoc-in [:group :parents] newgroups)
  )
)

(defn handle-chkuser-change [e]
  (let [
      id (str/join (drop 9 (.. e -currentTarget -id)))
      users (:owners (:group @app-state))
      newusers (if (= true (.. e -currentTarget -checked)) (conj users id) (remove (fn [x] (if (= x id) true false)) users))
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! app-state assoc-in [:group :owners] newusers)
  )
)


(defcomponent parentgroups-view [data owner]
  (render
    [_]
    (dom/div {:className "checkbox"}
      (map (fn [item]
        (let [            
            isparent (if (and (nil? (:parents (:group @app-state)))) false (if (> (.indexOf (:parents (:group @app-state)) (:id item)) -1) true false))
          ]
          (dom/form {:style {:padding-top "5px"}}
            (dom/label {:className "checkbox-inline"}
              (dom/input {:id (str "chckgroup" (:id item)) :type "checkbox" :checked isparent :data-toggle "toggle" :data-size "large" :data-width "100" :data-height "34" :onChange (fn [e] (handle-chkbsend-change e ))})
              (str "    " (:name item) "    ") 
            )
          )
        )
      )
      (sort (comp comp-notifications) (filter (fn [x] (if (= (:id x) (:id (:group @app-state))) false true)) (:groups @shelters/app-state))))
    )
  )
)

(defcomponent owners-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (let [
          isowner (if (and (nil? (:owners (:group @app-state)))) false (if (> (.indexOf (:owners (:group @app-state)) (:userid item)) -1) true false))
          ]
          (dom/div {:className "row" :style {:border-top "1px solid"}}
            (dom/div {:className "col-xs-3"}
              (dom/label {:className "checkbox-inline"}              
                (dom/input {:id (str "chckbuser" (:userid item)) :type "checkbox" :checked isowner :data-toggle "toggle" :data-size "large" :data-width "100" :data-height "34" :onChange (fn [e] (handle-chkuser-change e ))})
              )
            )
            (dom/div {:className "col-xs-3" :style { :border-left "1px solid" :height "34px"}}
              (dom/h4 {:className "list-group-item-heading"} (:login item))
            )
            (dom/div {:className "col-xs-3" :style { :border-left "1px solid" :height "34px"}}
              (:firstname item)
            )

            (dom/div {:className "col-xs-3" :style { :border-left "1px solid" :height "34px"}}
              (:lastname item)
            )
          )
        )
        )(sort (comp comp-users) (filter (fn [x] (if (or (str/includes? (str/lower-case (:firstname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:lastname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:login x)) (str/lower-case (:search @data)))) true false)) (:users @shelters/app-state)))
      )
    )
  )
)


(defcomponent notedetail-page-view [data owner]
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
              )
              ;; (dom/div {:className "checkbox"}
              ;;   (dom/label
              ;;     (dom/input {:id "toggle-one" :type "checkbox" :data-toggle "toggle"})
              ;;     "Option one is enabled"
              ;;   )
              ;; )
            )
          )
        )
        (dom/nav {:className "navbar navbar-default" :role "navigation"}
          (dom/div {:className "navbar-header"}
            (b/button {:className "btn btn-default" :onClick (fn [e] (updateNote))} "Update")
            (b/button {:className "btn btn-info" :onClick (fn [e] (-> js/document
              .-location
             (set! "#/groups")))  } "Cancel")
          )
        )
      )
    )

  )
)



(sec/defroute notedetail-page "/notedetail/:noteid" {noteid :noteid}  
  (let [
    thenote (first (filter (fn [x] (if (= (:id x) noteid) true false)) (:notifications @shelters/app-state)))
    ]
    (swap! app-state assoc-in [:note] thenote)
    ;(setNote)
    (om/root notedetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
