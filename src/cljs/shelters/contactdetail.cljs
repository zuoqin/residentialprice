(ns shelters.contactdetail  (:use [net.unit8.tower :only [t]])
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

(defonce app-state (atom  {:contact {} :isinsert false :view 1 :current "Contact Detail"}))

(defn handleChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [:contact (keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn OnDeleteContactError [response]
  (let [     
      newdata {:id (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete User from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteContactSuccess [response]
  (let [
      contacts (:contacts @shelters/app-state)
      newcontacts (remove (fn [contact] (if (= (:id contact) (:id (:contact @app-state))) true false)) contacts)
    ]
    ;(swap! shelters/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:contacts] newcontacts)
    (js/window.history.back)
  )
)

(defn OnUpdateContactError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateContactSuccess [response]
  (let [
      contacts (:contacts @shelters/app-state)  
      delcontact (remove (fn [contact] (if (= (:id contact) (:id (:contact @app-state))) true false  )) contacts)
      addcontact (conj delcontact {:name (:name (:contact @app-state)) :email (:email (:contact @app-state)) :id (:id (:contact @app-state))}) 
    ]
    (swap! shelters/app-state assoc-in [:contacts] addcontact)
    (js/window.history.back)
  )
)


(defn deleteContact [login]
  (DELETE (str settings/apipath  (str "deleteUser?userId=" (:userid (:user @app-state))) ) {
    :handler OnDeleteContactSuccess
    :error-handler OnDeleteContactError
    :headers {
      :token (:token (:token @shelters/app-state))
    }
    :format :json})
)



(defn updateContact []
  (PUT (str settings/apipath  "updateContact") {
    :handler OnUpdateContactSuccess
    :error-handler OnUpdateContactError
    :headers {
      :token (str (:token (:token @shelters/app-state)))
    }
    :format :json
    :params { :userName (:login (:user @app-state)) :userId (:userid (:user @app-state)) :token (:token (:token @shelters/app-state)) :role {:roleId (:id (:role (:user @app-state))) :roleName (:name (:role (:user @app-state))) :roleLevel (:level (:role (:user @app-state))) :roleDescription (:description (:role (:user @app-state)))} :details [{:key "firstName" :value (:firstname (:user @app-state))} {:key "lastName" :value (:lastname (:user @app-state))} {:key "email" :value (:email (:user @app-state))}] }})
)


(defn OnCreateContactError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  (.log js/console (str response))
)


(defn OnCreateContactSuccess [response]
  (let [
      tr1 (swap! app-state assoc-in [:contact :id] (get response "contactId"))
      contacts (:contacts @shelters/app-state)  
      addcontact (conj contacts (:contact @app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:contacts] addcontact)
    (js/window.history.back)
  )
)

(defn createContact []
  (let [
    ;tr1 (.log js/console (str "In createUser"))
    ]
    (POST (str settings/apipath  "addContact") {
      :handler OnCreateContactSuccess
      :error-handler OnCreateContactError
      :headers {
        :token (str (:token (:token @shelters/app-state)) )}
      :format :json
      :params { :credentials {:userName (:login (:user @app-state)) :password (:password (:user @app-state))} :profile {:userId "" :userName (:login (:user @app-state)) :token (:token (:token @shelters/app-state)) :role {:roleId (:id (:role (:user @app-state))) :roleName (:name (:role (:user @app-state))) :roleLevel (:level (:role (:user @app-state))) :roleDescription (:description (:role (:user @app-state)))} :details [{:key "firstName" :value (:firstname (:user @app-state))} {:key "lastName" :value (:lastname (:user @app-state))} {:key "email" :value (:email (:user @app-state))}]}  }})
  )
)


(defn setNewUserValue [key val]
  (swap! app-state assoc-in [(keyword key)] val)
)



(defn setcontrols [value]
  (case value
    46 (.log js/console (str value))
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

(defn setContact []
  (let [
        contacts (:contacts @shelters/app-state)
        contact (first (filter (fn [contact] (if (= (:id @app-state) (:id contact)) true false)) (:contacts @shelters/app-state )))
        ]
    (swap! app-state assoc-in [:name]  (:name contact) ) 
    (swap! app-state assoc-in [:phone]  (:phone contact) ) 
    (swap! app-state assoc-in [:email] (:email contact) )
  )
)




(defn OnError [response]
  (let [     
      newdata { :error (get (:response response)  "error") }
    ]
    (.log js/console (str  response )) 
    
  )
  
  
)


(defn getContactDetail []
  ;(.log js/console (str "token: " " " (:token  (first (:token @t5pcore/app-state)))       ))
  (if
    (and 
      (not= (:login @app-state) nil)
      (not= (:login @app-state) "")
    )
    (setContact)
  
  )
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  (.log js/console "The change ....")

)


(defn onMount [data]
  (swap! app-state assoc-in [:current] "Contact Detail")
  (getContactDetail)
  (setcontrols 46)
)


(defn handle-change [e owner]
  (.log js/console e)
  (swap! app-state assoc-in [:contact (keyword (.. e -target -id))] 
    (.. e -target -value)
  )
)


(defn buildRolesList [data owner]
  (map
    (fn [item]
      (dom/option {:key (:id item) :value (:id item)
                    :onChange #(handle-change % owner)} (:name item))
    )
    (:roles @shelters/app-state )
  )
)



(defcomponent contactdetail-page-view [data owner]
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
      (dom/div {:style {:padding-top "70px"}}
        (om/build shelters/website-view shelters/app-state {})
        (dom/div {:id "contact-detail-container" :style {:text-align "center"}}
(dom/div {:className "panel panel-default" :id "divContactInfo"}              
          (dom/div {:className "panel-heading"}
            (if (not (:isinsert @app-state))
              (dom/h5 "ID: " (:id (:contact @app-state)))
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-5"})
              (dom/div {:className "col-xs-1"} (dom/h5 "Login: "))
              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}}
                (dom/input {:id "name" :type "text" :onChange (fn [e] (handleChange e)) :value (:name (:contact @app-state))}                  
                )
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )
              

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-5"})
              (dom/div {:className "col-xs-1"} (dom/h5 "First Name: "))
              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}} (dom/input {:id "phone" :type "text" :onChange (fn [e] (handleChange e)) :value (:phone (:contact @app-state))}))

              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-5"})
              (dom/div {:className "col-xs-1"} (dom/h5 "Last Name: "))
              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}} (dom/input {:id "email" :type "text" :onChange (fn [e] (handleChange e)) :value (:email (:contact @app-state))}))

              (dom/div {:className "col-xs-1" :style {:margin-top "4px"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )
          )              
        )
        )
        (dom/nav {:className "navbar navbar-default" :role "navigation"}
          (dom/div {:className "navbar-header"}
            (b/button {:className "btn btn-default" :disabled? (or (< (count (:name (:contact @data))) 1) (< (count (:phone (:contact @data))) 1) (< (count (:phone (:contact @data))) 1)) :onClick (fn [e] (if (:isinsert @app-state) (createContact) (updateContact)) )} (if (:isinsert @app-state) "Insert" "Update"))
            (b/button {:className "btn btn-danger" :style {:visibility (if (= (:isinsert @app-state) true) "hidden" "visible")} :onClick (fn [e] (deleteContact (:id @app-state)))} "Delete")

            (b/button {:className "btn btn-info" :onClick
              (fn [e] (
                (js/window.history.back)
              )
            )} "Cancel")
          )
        )
      )
    )

  )
)



(sec/defroute contactdetail-page "/contactdetail/:id" {id :id}
  (let [
    contact (first (filter (fn [x] (if (= id (:id x)) true false)) (:contacts @shelters/app-state)))

    tr1 (swap! app-state assoc-in [:contact] contact)
    ]
    ;(swap! app-state assoc-in [:userid]  userid) 
    (swap! app-state assoc-in [:isinsert]  false )
    (swap! shelters/app-state assoc-in [:view] 4)
    (om/root contactdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(sec/defroute contactdetail-new-page "/contactdetail" {}
  (
    let [
       emptycontact {:id "" :name "" :phone "" :email ""}
    ]
    (swap! app-state assoc-in [:contact] emptycontact)
    (swap! app-state assoc-in [:isinsert]  true)
    (swap! shelters/app-state assoc-in [:view] 4)
    ;(swap! app-state assoc-in [:role ]  "user" ) 
    ;(swap! app-state assoc-in [:password] "" )


    (om/root contactdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
