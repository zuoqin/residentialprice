(ns shelters.login  (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [shelters.core :as shelters]
            [shelters.settings :as settings]
            [shelters.map :as mappage]
            [shelters.users :as users]
            [shelters.roles :as roles]
            [shelters.devs :as devs]
            [shelters.devslist :as devslist]
            [shelters.devdetail :as devdetail]
            [shelters.unitdetail :as unitdetail]
            [shelters.groups :as groups]
            [shelters.groupdetail :as groupdetail]
            [shelters.contacts :as contacts]
            [shelters.contactdetail :as contactdetail]
            [shelters.userdetail :as userdetail]


            [shelters.groupstounit :as groupstounit]
            [shelters.reportunits :as reportunits]
            [shelters.notedetail :as notedetail]
            [shelters.roledetail :as roledetail]
            [ajax.core :refer [GET POST]]
            [om-bootstrap.input :as i]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
	    [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >!]]
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


(defn map-indication [indication]
  (let [
    id (str (get indication "indicationId"))
    name (get indication "name")
    okicon (get indication "normalIconPath")
    failicon (get indication "failureIconPath")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name name :okicon okicon :failicon failicon}
    ]
    ;
    result
  )
)



(defn OnGetIndications [response]
  (swap! shelters/app-state assoc-in [:indications] (map map-indication response) )
  ;(reqsecurities)
  (swap! app-state assoc-in [:state] 0)
  (put! ch 42)
)


(defn reqindications []
  (GET (str settings/apipath "getUnitIndications")
    {:handler OnGetIndications
     :error-handler error-handler
     :headers {
       :content-type "application/json"
       :token (str (:token  (:token @shelters/app-state)))
       }
    }
  )
)


(defn map-command [command]
  (let [
    id (str (get command "commandId"))
    name (get command "commandName")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name name}
    ]
    ;
    result
  )
)



(defn OnGetCommands [response]
  (swap! shelters/app-state assoc-in [:commands] (map map-command (case (count response) 0 [{"commandId" 1 "commandName" "פתח דלת"}] response)))
  ;(reqsecurities)
  ;(swap! app-state assoc-in [:state] 0)
  (reqindications)
)


(defn reqcommands []
  (GET (str settings/apipath "getCommands")
    {:handler OnGetCommands
     :error-handler error-handler
     :headers {
       :content-type "application/json"
       :token (str (:token  (:token @shelters/app-state)))
       }
    }
  )
)


(defn map-group [group]
  (let [
    id (str (get group "groupId"))
    name (get group "groupName")
    parents (get group "parentGroups")
    owners (get group "owners")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name name :parents parents :owners owners}
    ]
    ;
    result
  )
)



(defn OnGetGroups [response]
  (swap! shelters/app-state assoc-in [:groups] (map map-group response) )
  ;(reqsecurities)
  (swap! app-state assoc-in [:state] 0)
  (reqcommands)
)




(defn reqgroups []
  (GET (str settings/apipath "getGroups")
       {:handler OnGetGroups
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :token (str (:token  (:token @shelters/app-state))) }
       })
)

(defn map-unitindication [indication]
  {:id (get indication "indicationId") :isok (get indication "isOk") :value (get indication "value")}
)

(defn map-unit [unit]
  (let [
    controller (str (get unit "controllerId"))
    name (if (nil? (get unit "name")) controller (get unit "name"))
    port (get unit "port")
    port (if (nil? port) 5050 port)
    ip (get unit "ip")
    ip (if (nil? ip) "1.1.1.1" ip)

    status (case (get unit "status") "Normal" 0 3)
    lat (get unit "latitude")
    lon (get unit "longitude")
    groups (get unit "parentGroups")
    unitid (str (get unit "unitId"))    
    address (get (first (filter (fn [x] (if (= (get x "key") "address") true false)) (get unit "details"))) "value" )
    phone (get (first (filter (fn [x] (if (= (get x "key") "phone") true false)) (get unit "details"))) "value")

    indications (map map-unitindication (get unit "indications"))
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts [{:id "1" :phone "+79175134855" :name "Alexey" :email "zorchenkov@gmail.com"} {:id "2" :phone "+9721112255" :name "yulia" :email "yulia@gmail.com"}]}
    ]
    ;
    result
  )
)


(defn OnGetUnits [response]
  (swap! shelters/app-state assoc-in [:devices] (map map-unit response) )
  ;(reqsecurities)
  (swap! app-state assoc-in [:state] 0)
  (reqgroups)
)




(defn requnits []
  (GET (str settings/apipath "getUnits" ;"?userId="(:userid  (:token @shelters/app-state))
       )
       {:handler OnGetUnits
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :token (str (:token  (:token @shelters/app-state))) }
       })
)

(defn map-role [role]
  (let [
    id (get role "roleId")
    level (get role "roleLevel")
    description (get role "roleDescription")
    rolename (get role "roleName")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name rolename :level level :description description}
    ]
    ;
    result
  )
)


(defn OnGetRoles [response]
  (swap! shelters/app-state assoc-in [:roles] (map map-role response) )
  ;(reqsecurities)
  (requnits)
)




(defn reqroles []
  (GET (str settings/apipath "getRoles")
       {:handler OnGetRoles
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :token (str (:token  (:token @shelters/app-state))) }
       })
)



(defn map-user [user]
  (let [


    ;tr1 (.log js/console "In map user")
    username (get user "userName")
    userid (get user "userId")
    role (first (filter (fn [x] (if (= (:id x) (get (get user "role") "roleId")) true false)) (:roles @shelters/app-state)))

    firstname (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "firstName") true false)
      ) ) (get user "details"))) "value")
    
    lastname (get (first (filter (fn [x] (if (= (get x "key") "lastName") true false)) (get user "details"))) "value")

    email (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "email") true false)
      ) ) (get user "details"))) "value")


    addedby (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "addedby") true false)
      ) ) (get user "details"))) "value")

    islocked (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "islocked") true false)
      ) ) (get user "details"))) "value")

    islocked (if (or (nil? islocked) (= islocked 0)) false true)

    iscommand (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "iscommand") true false)
      ) ) (get user "details"))) "value")

    iscommand (if (or (nil? iscommand) (= iscommand 0)) false true)

    ;tr1 (.log js/console (str  "islocked=" islocked))
    result {:login username :userid userid :role role :firstname (if (nil? firstname) "" firstname) :lastname (if (nil? lastname) "" lastname) :email (if (nil? email) "" email) :addedby (if (nil? addedby) "" addedby) :islocked islocked :iscommand iscommand}
    ]
    ;
    result
  )
)

(defn OnGetUsers [response]
  (swap! shelters/app-state assoc-in [:users] (map map-user response) )
  ;(swap! shelters/app-state assoc-in [:view] 1 )
  ;(aset js/window "location" "#/positions")
  (reqroles)
  
)

(defn requsers []
  (GET (str settings/apipath "getUsers")
       {:handler OnGetUsers
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :token (str (:token  (:token @shelters/app-state)))}
       }
  )
)



(defn setUser [theUser]
  (let [cnt (count (:users @shelters/app-state))]
    (swap! shelters/app-state assoc-in [:users cnt] {:role (nth theUser 1)  :login (nth theUser 0) :password (nth theUser 2)})
  )
  

  ;;(.log js/console (nth theUser 0))
  ;;(.log js/console (:login (:user @shelters/app-state) ))
  (if (= (nth theUser 0) (:login (:user @shelters/app-state) ))   
    (swap! shelters/app-state assoc-in [:user :role] (nth theUser 1) )
  )
  
)


(defn OnGetUser [response]
  ;(.log js/console (str "In On GetUser"))
  (doall (map setUser response))
  ;(reqclients)  
)


(defn requser [token]
  ;(.log js/console (str "In requser with token " (:token  (:token @shelters/app-state))))
  (GET (str settings/apipath "api/user") {
    :handler OnGetUser
    :error-handler error-handler
    :headers {:content-type "application/json" :Authorization (str "Bearer "  (:token  (:token @shelters/app-state))) }
  })
)

(defn onLoginSuccess [response]
  (
    let [
      ;response1 (js->clj response)
      tr1 (.log js/console (str  "In LoginSuccess token: " (get response "token") ))
      newdata {:token (get response "token") :userid (get response "userId" ) }
    ]
    (swap! app-state assoc-in [:state] 0)
    ;(.log js/console (str (:token newdata)))
    (swap! shelters/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:view] 2 )
    (swap! shelters/app-state assoc-in [:users] [] )
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
  (swap! shelters/app-state assoc-in [:user :login] username)

  ;; currently selected user
  (swap! shelters/app-state assoc-in [:selecteduser] username)


  (POST (str settings/apipath "verifyUser")
    {:handler OnLogin
     :error-handler onLoginError
     :format :json
     :params {:userName username :password password} 
    }
  )
)




(defn checklogin [owner e]
  (let [
    theusername (-> (om/get-node owner "txtUserName") .-value)
    thepassword (-> (om/get-node owner "txtPassword") .-value)
    ]
    (.preventDefault (.. e -nativeEvent))
    (.stopPropagation (.. e -nativeEvent))
    (.stopImmediatePropagation (.. e -nativeEvent))
    ;(aset js/window "location" "http://localhost:3449/#/something")
    (.log js/console (str "user=" theusername " password=" thepassword))
    (dologin (str theusername) (str thepassword)) 
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

(defn onMount [data owner]
  (.focus (om/get-node owner "txtUserName" ))
  (set! (.-title js/document) "Beeper Login")
)


(defcomponent login-page-view [data owner]
  (did-update [this prev-props prev-state]
    (.log js/console "starting login screen" ) 
    
  )
  (did-mount [_]
    (onMount data owner)
  )
  (render
    [_]
    (dom/div {:className "container" :style {:width "100%" :padding-top "10px" :backgroundImage "url(/images/loginbackground.png)" :backgroundSize "224px 105px" :backgroundRepeat "no-repeat" :backgroundPosition "top center"}  }
      ;(om/build t5pcore/website-view data {})
      ;(dom/h1 "Login Page")
      ;(dom/img {:src "images/LogonBack.jpg" :className "img-responsive company-logo-logon"})
      (dom/form {:className "form-signin" :style {:padding-top "150px"}}
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
    42 (shelters/goMap 0)
    ;43 (requser @shelters/app-state)
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
      (:view (if (= data nil) @shelters/app-state @data ))
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
