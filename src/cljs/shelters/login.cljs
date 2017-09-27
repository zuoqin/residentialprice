(ns shelters.login  (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [shelters.core :as shelterscore]
            [shelters.settings :as settings]
            [shelters.map :as mappage]
            [shelters.users :as users]
            [shelters.roles :as roles]
            [shelters.devs :as devs]
            [shelters.devdetail :as devdetail]
            [shelters.userdetail :as userdetail]
            [shelters.roledetail :as roledetail]
            [ajax.core :refer [GET POST]]
            [om-bootstrap.input :as i]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
	    
            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]
  )
  (:import goog.History)
)

(enable-console-print!)


(def application
  (js/document.getElementById "app"))

(defn set-html! [el content]
  (aset el "innerHTML" content))


(sec/set-config! :prefix "#")

(let [history (History.)
      navigation EventType/NAVIGATE]
  (goog.events/listen history
                     navigation
                     #(-> % .-token sec/dispatch!))
  (doto history (.setEnabled true)))


(def ch (chan (dropping-buffer 2)))
(def jquery (js* "$"))
(defonce app-state (atom  {:error "" :modalText "Modal Text" :modalTitle "Modal Title" :state 0} ))


(defn setLoginError [error]
  (swap! app-state assoc-in [:error] 
    (:error error)
  )

  (swap! app-state assoc-in [:modalTitle] 
    (str "Login Error")
  ) 

  (swap! app-state assoc-in [:modalText] 
    (str (:error error))
  ) 

  (swap! app-state assoc-in [:state] 0)
 
  ;;(.log js/console (str  "In setLoginError" (:error error) ))
  (jquery
    (fn []
      (-> (jquery "#loginModal")
        (.modal)
      )
    )
  )
)


(defn onLoginError [ response]
  (let [     
      newdata { :error (get response (keyword "response")) }
    ]
   
    (setLoginError newdata)
  )
  
  ;(.log js/console (str  response ))
)



(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)

(defn map-role [role]
  (let [
    level (get role "roleLevel")
    description (get role "roleDescription")
    rolename (get role "roleName")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:name rolename :level level :description description}
    ]
    ;
    result
  )
)


(defn OnGetRoles [response]
  (swap! shelterscore/app-state assoc-in [:roles] (map map-role response) )
  ;(reqsecurities)
  (put! ch 42)
)




(defn reqroles []
  (GET (str settings/apipath "getRoles")
       {:handler OnGetRoles
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :Authorization (str "Bearer " (:token  (:token @shelterscore/app-state))) }
       })
)



(defn map-user [user]
  (let [
    login (get (get user "credentials") "userName")
    username (get (get user "details") "key")
    role (get (get user "role") "roleName")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:name username :role role :login login}
    ]
    ;
    result
  )
)

(defn OnGetUsers [response]
  (swap! shelterscore/app-state assoc-in [:users] (map map-user response) )
  (swap! app-state assoc-in [:state] 0)
  ;(swap! shelterscore/app-state assoc-in [:view] 1 )
  ;(aset js/window "location" "#/positions")
  (reqroles)
  
)

(defn requsers []
  (GET (str settings/apipath "getUsers")
       {:handler OnGetUsers
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :Authorization (str "Bearer " (:token  (:token @shelterscore/app-state))) }
       })
)



(defn setUser [theUser]
  (let [cnt (count (:users @shelterscore/app-state))]
    (swap! shelterscore/app-state assoc-in [:users cnt] {:role (nth theUser 1)  :login (nth theUser 0) :password (nth theUser 2)})
  )
  

  ;;(.log js/console (nth theUser 0))
  ;;(.log js/console (:login (:user @shelterscore/app-state) ))
  (if (= (nth theUser 0) (:login (:user @shelterscore/app-state) ))   
    (swap! shelterscore/app-state assoc-in [:user :role] (nth theUser 1) )
  )
  
)


(defn OnGetUser [response]
  ;(.log js/console (str "In On GetUser"))
  (doall (map setUser response))
  ;(reqclients)  
)


(defn requser [token]
  ;(.log js/console (str "In requser with token " (:token  (:token @shelterscore/app-state))))
  (GET (str settings/apipath "api/user") {
    :handler OnGetUser
    :error-handler error-handler
    :headers {:content-type "application/json" :Authorization (str "Bearer "  (:token  (:token @shelterscore/app-state))) }
  })
)

(defn onLoginSuccess [response]
  (
    let [     
      ;response1 (js->clj response)
      tr1 (.log js/console (str  "In LoginSuccess token: " (get response "token") ))
      newdata {:token (get response "token")  :expires (get response "expires_in" ) }
    ]
    (swap! app-state assoc-in [:state] 0)    
    ;(.log js/console (str (:token newdata)))
    (swap! shelterscore/app-state assoc-in [:token] newdata )
    (swap! shelterscore/app-state assoc-in [:view] 2 )
    (swap! shelterscore/app-state assoc-in [:users] [] )
    (requsers)
    ;;(requser {:token newdata})
    ;;(put! ch 43)
    ;(put! ch 42)
  )
)

(defn OnLogin [response]
  (if (= (count response) 0)
    (onLoginError {:response "Incorrect username or password"} )
    (onLoginSuccess response)
  )
  
  ;;(.log js/console (str  (response) ))
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn dologin [username password]
  (swap! app-state assoc-in [:state] 1)
  ;; currently logged in user
  (swap! shelterscore/app-state assoc-in [:user :login] username)

  ;; currently selected user
  (swap! shelterscore/app-state assoc-in [:selecteduser] username)


  (POST (str settings/apipath "verifyUser") {:handler OnLogin
                                            :error-handler onLoginError
                                            :headers {:content-type "application/json"}
                                            :body (str "\"username\":\"" username "\",\"password\":\"" password "\"") 
                                            })
)




(defn checklogin [owner e]
  (let [
    theUserName (-> (om/get-node owner "txtUserName") .-value)
    thePassword (-> (om/get-node owner "txtPassword") .-value)
    ]
    (.preventDefault (.. e -nativeEvent))
    (.stopPropagation (.. e -nativeEvent))
    (.stopImmediatePropagation (.. e -nativeEvent))
    ;(aset js/window "location" "http://localhost:3449/#/something")
    ;(.log js/console owner )
    (dologin (str theUserName) (str thePassword)) 
  )
)


(defn addModal []
  (dom/div
    (dom/div {:id "loginModal" :className "modal fade" :role "dialog"}
      (dom/div {:className "modal-dialog"} 
        ;;Modal content
        (dom/div {:className "modal-content"} 
          (dom/div {:className "modal-header"} 
                   (b/button {:type "button" :className "close" :data-dismiss "modal"})
                   (dom/h4 {:className "modal-title"} (:modalTitle @app-state) )
                   )
          (dom/div {:className "modal-body"}
                   (dom/p (:modalText @app-state))
                   )
          (dom/div {:className "modal-footer"}
                   (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Close")
          )
        )
      )
    )
  )
)



(defcomponent login-page-view [data owner]
  (did-update [this prev-props prev-state]
    (.log js/console "starting login screen" ) 
    
  )
  (did-mount [_]
    (.focus (om/get-node owner "txtUserName" ))
  )
  (render
    [_]
    (dom/div {:className "container" :style {:width "100%" :padding-top "283px" :backgroundImage "url(/images/loginbackground.png)" :backgroundSize "cover"}  }
      ;(om/build t5pcore/website-view data {})
      ;(dom/h1 "Login Page")
      ;(dom/img {:src "images/LogonBack.jpg" :className "img-responsive company-logo-logon"})
      (dom/form {:className "form-signin"}
        (dom/input #js {:type "text" :ref "txtUserName"
           :defaultValue  settings/demouser  :className "form-control" :placeholder "User Name" } )
        (dom/input {:className "form-control" :ref "txtPassword" :id "txtPassword" :defaultValue settings/demopassword :type "password"  :placeholder "Password"} )
        (dom/button {:className (if (= (:state @app-state) 0) "btn btn-lg btn-primary btn-block" "btn btn-lg btn-primary btn-block m-progress" ) :onClick (fn [e](checklogin owner e)) :type "submit" } "Login")
        
      )
      (addModal)
      (dom/div {:style {:margin-bottom "700px"}})
    )
  )
)

(defn setcontrols [value]
  (case value
    42 (shelterscore/goMap 0)
    ;43 (requser @shelterscore/app-state)
  )
)

(defn initqueue []
  (doseq [n (range 1000)]
    (go ;(while true)
      (take! ch(
        fn [v] (
           setcontrols v
          )
        )
      )
    )
  )
)

(initqueue)



(defmulti website-view
  (
    fn [data _]
      (:view (if (= data nil) @shelterscore/app-state @data ))
  )
)

(defmethod website-view 0
  [data owner] 
  (login-page-view data owner)
)

(defmethod website-view 1
  [data owner] 
  (login-page-view data owner)
)

(sec/defroute login-page "/login" []
  (om/root login-page-view 
           app-state
           {:target (. js/document (getElementById "app"))}
  )
)


(defn main []
  (-> js/document
      .-location
      (set! "#/login"))

  ;;(aset js/window "location" "#/login")
)


(main)
