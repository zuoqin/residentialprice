(ns shelters.userdetail  (:use [net.unit8.tower :only [t]])
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
(defonce app-state (atom  {:login "" :password "" :roles [{:name "admin"} {:name "manager"} {:name "user"}] :isinsert false :role "admin" :view 1 :current "User Detail"} ))

(defn handleChange [e]
  ;(.log js/console e  )  
  ;(.log js/console "The change ....")
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn OnDeleteUserError [response]
  (let [     
      newdata {:userid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete User from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteUserSuccess [response]
  (let [
      users (:users @shelters/app-state    )  
      newusers (remove (fn [user] (if (= (:login user) (:login @app-state) ) true false  )) users)
    ]
    ;(swap! shelters/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:users] newusers)
  )

    (-> js/document
      .-location
      (set! "#/users"))
)

(defn OnUpdateUserError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateUserSuccess [response]
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


(defn deleteUser [login]
  (DELETE (str settings/apipath  "api/user?login=" login) {
    :handler OnDeleteUserSuccess
    :error-handler OnDeleteUserError
    :headers {
      :content-type "application/json" 
      :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateUser []
  (PUT (str settings/apipath  "api/user") {
    :handler OnUpdateUserSuccess
    :error-handler OnUpdateUserError
    :headers {
      :content-type "application/json" 
      :Authorization (str "Bearer "  (:token (:token @shelters/app-state)))}
    :format :json
    :params {:login (:login @app-state) :password (:password @app-state) :role (:role @app-state) }})
)


(defn OnCreateUserError [response]
  (let [     
      newdata {:tripid (get response (keyword "tripid") ) }
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnCreateUserSuccess [response]
  (let [
      users (:users @shelters/app-state    )  
      adduser (into [] (conj users {:login (:login @app-state) :password (:password @app-state) :role (:role @app-state)} )) 
    ]
    (swap! shelters/app-state assoc-in [:users] adduser)

    (-> js/document
      .-location
      (set! "#/users"))

  )
)

(defn createUser []
  (POST (str settings/apipath  "api/user") {
    :handler OnCreateUserSuccess
    :error-handler OnCreateUserError
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


(defn setNewUserValue [key val]
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

(defn setUser []
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


(defn getUserDetail []
  ;(.log js/console (str "token: " " " (:token  (first (:token @t5pcore/app-state)))       ))
  (if
    (and 
      (not= (:login @app-state) nil)
      (not= (:login @app-state) "")
    )
    (setUser)
  
  )
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  (.log js/console "The change ....")

)


(defn onMount [data]
  (swap! app-state assoc-in [:current] 
    "User Detail"
  )
  (getUserDetail)
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



(defcomponent userdetail-page-view [data owner]
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
        (om/build shelters/website-view data {})
        (dom/div {:id "user-detail-container"}
          (dom/span
            (dom/div  (assoc styleprimary  :className "panel panel-default"  :id "divUserInfo")
              
              (dom/div {:className "panel-heading"}
                (dom/h5 "Login: " 
                  (dom/input {:id "login" :type "text" :disabled (if (= (:isinsert @app-state) true) false true)  :onChange (fn [e] (handleChange e)) :value (:login @app-state)} )

                )
                
                (dom/h5 "Password: "
                  (dom/input {:id "password" :type "password" :onChange (fn [e] (handleChange e)) :value (:password @app-state)})
                )
                ;; (dom/h5 "Role: "
                ;;   (dom/input {:id "role" :type "text" :value (:role @app-state)})
                ;; )

                (dom/div {:className "form-group"}
                  (dom/p
                    (dom/label {:className "control-label" :for "roles" }
                      "Role: "
                    )
                  
                  )
                 
                  (omdom/select #js {:id "roles"
                                     :className "selectpicker"
                                     :data-show-subtext "true"
                                     :data-live-search "true"
                                     :onChange #(handle-change % owner)
                                     }                
                    (buildRolesList data owner)
                  )
                  
                )                
              )              
            )
          )
        )
        (dom/nav {:className "navbar navbar-default" :role "navigation"}
          (dom/div {:className "navbar-header"}
            (b/button {:className "btn btn-default" :onClick (fn [e] (if (= (:isinsert @app-state) 0) (createUser) (updateUser)) )} (if (= (:isinsert @app-state) true) "Insert" "Update"))
            (b/button {:className "btn btn-danger" :style {:visibility (if (= (:isinsert @app-state) true) "hidden" "visible")} :onClick (fn [e] (deleteUser (:login @app-state)))} "Delete")

            (b/button {:className "btn btn-info"  :onClick (fn [e] (-> js/document
      .-location
      (set! "#/users")))  } "Cancel")
          )
        )
      )
    )

  )
)



(sec/defroute userdetail-page "/userdetail/:login" {login :login}
  (
    (swap! app-state assoc-in [:login]  login ) 
    (swap! app-state assoc-in [:isinsert]  false )
    (om/root userdetail-page-view
             shelters/app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(sec/defroute userdetail-new-page "/userdetail" {}
  (
    (swap! app-state assoc-in [:login]  "" ) 
    (swap! app-state assoc-in [:isinsert]  true )
 
    (swap! app-state assoc-in [:role ]  "user" ) 
    (swap! app-state assoc-in [:password] "" )


    (om/root userdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
