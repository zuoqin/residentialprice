(ns shelters.login
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


            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [shelters.groupstounit :as groupstounit]
            [shelters.groupstouser :as groupstouser]
            [shelters.reportunits :as reportunits]
            [shelters.notedetail :as notedetail]
            [shelters.roledetail :as roledetail]
            [shelters.localstorage :as ls]
            [ajax.core :refer [GET POST]]
            [om-bootstrap.input :as i]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
	    [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout]]
  )
  (:import goog.History)
)

(enable-console-print!)

(def iconBase "/images/")
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
    id (get indication "indicationId")
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
  (let [
    indicators (filter (fn [x] (if (>= (.indexOf shelters/indicators (:name x)) 0) true false)) (map map-indication response))
    ]
    (swap! shelters/app-state assoc-in [:indications] indicators)
    (ls/set-item! "indicators" (.stringify js/JSON (clj->js indicators)))
  )
)


(defn reqindications []
  (let [
    indicatorsstr (ls/get-item "indicators")
    tmpindicators (js->clj (.parse js/JSON indicatorsstr))
    ;tr1 (.log js/console (str (first tmpindicators)))
    ]

    (GET (str settings/apipath "getUnitIndications")
      {:handler OnGetIndications
       :error-handler error-handler
       :headers {
         :content-type "application/json"
         :token (str (:token  (:token @shelters/app-state)))
         }
      }
    )

    (if (not (nil? tmpindicators))
      (swap! shelters/app-state assoc-in [:indications] tmpindicators )
    )
    (put! ch 42)
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
  (let [
    commands (map map-command (case (count response) 0 [{"commandId" 1 "commandName" "פתח מנעול"}] response))
    ]
    (swap! shelters/app-state assoc-in [:commands] commands)
    (ls/set-item! "commands" (.stringify js/JSON (clj->js commands)))
  )

  ;(swap! app-state assoc-in [:state] 0)
)


(defn reqcommands []
  (let [
    commandsstr (ls/get-item "devices")
    tmpcommands (js->clj (.parse js/JSON commandsstr))
    ;tr1 (.log js/console (str (first tmpcommands)))
    ]
    (GET (str settings/apipath "getCommands")
      {:handler OnGetCommands
       :error-handler error-handler
       :headers {
         :content-type "application/json"
         :token (str (:token  (:token @shelters/app-state)))
         }
      }
    )
    (if (not (nil? tmpcommands))
      (swap! shelters/app-state assoc-in [:commands] tmpcommands )
    )
    (reqindications)
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
  (let [
    groups (map map-group response)
    ]
    (swap! shelters/app-state assoc-in [:groups] groups)
    (ls/set-item! "groups" (.stringify js/JSON (clj->js groups)))
  )
  
  ;(swap! app-state assoc-in [:state] 0)
)


;; (defn updategroup [group]
;;   (let [
;;     parents (:parents group)
;;     ]
;;     (if (nil? parents) (update group :parents (fn [x] [])) group)
;;   )
;; )

(defn reqgroups []
  (let [
    groupsstr (ls/get-item "groups")
    tmpgroups (js->clj (.parse js/JSON groupsstr))
    ;tr1 (.log js/console (str (first tmpgroups)))
    ]
    (GET (str settings/apipath "getGroups")
         {:handler OnGetGroups
          :error-handler error-handler
          :headers {:content-type "application/json"
                    :token (str (:token  (:token @shelters/app-state))) }
         })
    (if (not (nil? tmpgroups))
      (swap! shelters/app-state assoc-in [:group] tmpgroups)
    )
    (reqcommands)
  )
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
    indications (if (> (count indications) 0) indications [{:id 1, :isok false, :value "open"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}])
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts [{:id "1" :phone "+79175134855" :name "Alexey" :email "zorchenkov@gmail.com"} {:id "2" :phone "+9721112255" :name "yulia" :email "yulia@gmail.com"}]}
    ]
    ;
    result
  )
)


(defn OnGetUnits [response]
  (let [
    units (map map-unit response)
    ]
    (swap! shelters/app-state assoc-in [:devices] units )
    
    (ls/set-item! "devices" (.stringify js/JSON (clj->js units)))
    ;(swap! app-state assoc-in [:state] 0)
    ;(reqgroups)
  )
)




(defn requnits []
  (let [
    unitsstr (ls/get-item "devices")
    tmpunits (js->clj (.parse js/JSON unitsstr))
    tr1 (.log js/console (str (first tmpunits)))
    ]
    (GET (str settings/apipath "getUnits" ;"?userId="(:userid  (:token @shelters/app-state))
         )
         {:handler OnGetUnits
          :error-handler error-handler
          :headers {:content-type "application/json"
                    :token (str (:token  (:token @shelters/app-state))) }
         })
    (if (not (nil? tmpunits))
      (let []
        (put! ch 45)
        (swap! shelters/app-state assoc-in [:devices] tmpunits )
      )
    )
    (reqgroups)
  )
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
  (let [
    roles (map map-role response)
    ]
    (swap! shelters/app-state assoc-in [:roles] roles)
    (ls/set-item! "roles" (.stringify js/JSON (clj->js roles)))
  )
  
  ;(requnits)
)




(defn reqroles []
  (let [
    rolesstr (ls/get-item "roles")
    tmproles (js->clj (.parse js/JSON rolesstr))
    ;tr1 (.log js/console (str (first tmproles)))
    ]
    (GET (str settings/apipath "getRoles")
         {:handler OnGetRoles
          :error-handler error-handler
          :headers {:content-type "application/json"
                    :token (str (:token  (:token @shelters/app-state))) }
         })

    (if (not (nil? tmproles))
      (swap! shelters/app-state assoc-in [:roles] tmproles )
    )
    (requnits)
  )
)



(defn map-user [user]
  (let [


    ;tr1 (.log js/console "In map user")
    username (get user "userName")
    groups (get user "childEntities")
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
    result {:login username :groups groups :userid userid :role role :firstname (if (nil? firstname) "" firstname) :lastname (if (nil? lastname) "" lastname) :email (if (nil? email) "" email) :addedby (if (nil? addedby) "" addedby) :islocked islocked :iscommand iscommand}
    ]
    ;
    result
  )
)

(defn OnGetUsers [response]
  (let [
    users (map map-user response)
    ]
    (swap! shelters/app-state assoc-in [:users] users )
    ;(swap! shelters/app-state assoc-in [:view] 1 )

    (ls/set-item! "users" (.stringify js/JSON (clj->js users)))
  )
)

(defn requsers []
  (let [
    usersstr (ls/get-item "users")
    tmpusers (js->clj (.parse js/JSON usersstr))
    ;tr1 (.log js/console (str (first tmpusers)))
    ]
    (GET (str settings/apipath "getUsers")
         {:handler OnGetUsers
          :error-handler error-handler
          :headers {:content-type "application/json"
                    :token (str (:token  (:token @shelters/app-state)))}
         }
    )

    (if (not (nil? tmpusers))
      (swap! shelters/app-state assoc-in [:devices] tmpusers )
    )
    (reqroles)
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
      ;tr1 (.log js/console (str  "In LoginSuccess token: " (get response "token") ))
      newdata {:token (get response "token") :userid (get response "userId" ) }
    ]
    ;(swap! app-state assoc-in [:state] 0)
    ;(.log js/console (str (:token newdata)))
    (swap! shelters/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:view] 2 )
    (swap! shelters/app-state assoc-in [:users] [] )
    (requsers)
    ;;(requser {:token newdata})
    ;;(put! ch 43)
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
  (let [

    ]
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
    ;(.log js/console "gg")
    (onMount data owner)
  )
  (render
    [_]
    (dom/div {:className "container" :style {:width "100%" :padding-top "10px" :margin-top "100px" :backgroundImage "url(images/loginbackground.png)" :backgroundSize "224px 105px" :backgroundRepeat "no-repeat" :backgroundPosition "top center"}  }
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

(defn processMessage [notification]
  (let [
    unitid (get notification "unitId")
    userid (get notification "userId")
    status (get notification "status")
    type (get notification "notificationType")
    id (get notification "notificationId")
    indicatorid (get notification "indicationId")
    ;tr1 (.log js/console (subs (get notification "openTime") 0 19))
    open (tf/parse shelters/custom-formatter2 (subs (get notification "openTime") 0 19))
    
    open (if (= (subs (get notification "openTime") 20) "PM") (tc/from-long (+ (tc/to-long open) (* 1000 12 3600))) open)

        
    close (tf/parse shelters/custom-formatter2 (subs (get notification "closeTime") 0 19))
    close (if (= (subs (get notification "closeTime") 20) "PM") (tc/from-long (+ (tc/to-long close) (* 1000 12 3600))) close)


    accept (tf/parse shelters/custom-formatter2 (subs (get notification "acceptanceTime") 0 19))
    accept (if (= (subs (get notification "acceptanceTime") 20) "PM") (tc/from-long (+ (tc/to-long close) (* 1000 12 3600))) accept)


    ;tr1 (.log js/console (str "unitid in Notification: " unitid))
    marker (first (filter (fn [x] (if (= (.. x -unitid) unitid) true false)) (:markers @shelters/app-state)))

    indstatus (case status "Closed" true false)
    
    newindications (map (fn [ind] (if (= (:id ind) indicatorid) (assoc ind :isok indstatus :value "") ind)) (:indications (first (filter (fn [x] (if (= (:id x) unitid) (assoc x :status status) x)) (:devices @shelters/app-state)))))


    newunits (map (fn [x] (if (= (:id x) unitid) (assoc x :indications newindications) x)) (:devices @shelters/app-state))
    
    tr1 (swap! shelters/app-state assoc-in [:devices] newunits)

    tr1 (case type "Failure" (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:alerts @shelters/app-state)))) (swap! shelters/app-state assoc-in [:alerts] (conj (:alerts @shelters/app-state) {:unitid unitid :userid userid :status status :id id :open open :close close :accept accept :type type})))

        (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:notifications @shelters/app-state)))) (swap! shelters/app-state assoc-in [:notifications] (conj (:notifications @shelters/app-state) {:unitid unitid :userid userid :status status :id id :open open :close close :accept accept :type type}))
          (let [
              delnotification (filter (fn [x] (if (= (:id x) id) false true)) (:notifications @shelters/app-state))
              newnotifications (conj delnotification {:unitid unitid :userid userid :status status :id id :open open :close close :accept accept :type type})
            ]
            (swap! shelters/app-state assoc-in [:notifications] newnotifications)
          )
        )
    )
    
    ]
    (if (nil? marker)
      (.log js/console (str "did not find a unit for unitid=" unitid " in notification"))
      (.setIcon marker (str iconBase (case status 3 "red_point.png" "green_point.png")))
    )
  )
)


(defn processNotification [notification]
  (let [
      tr1 (js/console.log "Hooray! Message:" (pr-str notification))
    ]
    (try
      (processMessage notification)
      (catch js/Error e
        (println (str "error while processing message: " e))
      )
    )
  )
)


(defn receivesocketmsg []
  (go
    (let [
        {:keys [ws-channel error]} (<! (ws-ch (str settings/socketpath) {:format :json}))
        {:keys [message error]} (<! ws-channel)
        
      ]
      (if error
        (js/console.log "Uh oh:" error)
        (processNotification message)      
      )
      (receivesocketmsg)
    )
  )
)

(defn initsocket []
  (go
    (let [
        {:keys [ws-channel error]} (<! (ws-ch (str settings/socketpath) {:format :json}))
        ;{:keys [message error]} (<! ws-channel)
        
      ]
      (.log js/console (str "token to send in socket: " (:token (:token @shelters/app-state))))
      (if-not error
        (>! ws-channel (:token (:token @shelters/app-state)))
        (js/console.log "Error:" (pr-str error))
      )
      (receivesocketmsg)
    )
  )
)

(defn setcontrols [value]
  (case value
    42 (go
         (<! (timeout 1500))
         (swap! app-state assoc-in [:state] 0)
         (aset js/window "location" "#/map")
       )
    45 (initsocket)
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

