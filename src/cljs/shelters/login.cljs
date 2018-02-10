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
            [cljs-time.core :as tc]
            [cljs-time.coerce :as te]
            [cljs-time.local :as tl]
            [shelters.groupstounit :as groupstounit]
            [shelters.unitstogroup :as unitstogroup]
            [shelters.groupstouser :as groupstouser]
            [shelters.reportalerts :as reportalerts]
            [shelters.reportsensors :as reportsensors]
            [shelters.notedetail :as notedetail]
            [shelters.roledetail :as roledetail]
            [shelters.localstorage :as ls]
            [ajax.core :refer [GET POST]]
            [om-bootstrap.input :as i]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
	    [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout close!]]
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


(defn map-notification [notification]
  (let [
    id (get notification "notificationId")
    accept (tf/parse shelters/custom-formatter2 (get notification "acceptanceTime"))
    close (tf/parse shelters/custom-formatter2 (get notification "closeTime"))
    indicatorid (get notification "indicationId")
    type (get notification "notificationType")
    open (tf/parse shelters/custom-formatter2 (get notification "openTime"))
    status (get notification "status")
    unitid (get notification "unitId")
    userid (get notification "userId")
    ;tr1 (.log js/console (str  "username=" username ))

    ]
    ;
    {:unitid unitid :userid userid :status status :id id :sensorid indicatorid :open open :close close :accept accept :type type}
  )
)

(defn OnGetNotifications [response]
  (let [
    notifications (map map-notification response)
    ]
    (swap! shelters/app-state assoc-in [:notifications] (filter (fn [x] (if (= (:type x) "Alert") true false)) notifications))
    (swap! shelters/app-state assoc-in [:alerts] (filter (fn [x] (if (or (= (:type x) "Failure") (= (:type x) "Unknown")) true false)) notifications) )
    ;(swap! shelters/app-state assoc-in [:notifications1] notifications)
    (ls/set-item! "notifications" (.stringify js/JSON (clj->js notifications)))
  )
)


(defn reqnotifications []
  (let [
    notificationsstr (ls/get-item "notifications")
    tmpnotifications (js->clj (.parse js/JSON notificationsstr))
    ;tr1 (.log js/console (str (first tmpindicators)))
    ]

    (GET (str settings/apipath "getNotifications")
      {:handler OnGetNotifications
       :error-handler error-handler
       :headers {
         :content-type "application/json"
         :token (str (:token  (:token @shelters/app-state)))
         }
      }
    )

    (if (not (nil? tmpnotifications))
      (swap! shelters/app-state assoc-in [:notifications] (filter (fn [x] (if (= (:type x) "Alert") true false)) tmpnotifications))
      (swap! shelters/app-state assoc-in [:alerts] (filter (fn [x] (if (or (= (:type x) "Failure") (= (:type x) "Unknown")) true false)) tmpnotifications) )
    )
    (put! ch 42)
  )
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

(defn map-indication-local [indication]
  (let [
    id (get indication "id")
    name (get indication "name")
    okicon (get indication "okicon")
    failicon (get indication "failicon")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name name :okicon okicon :failicon failicon}
    ]
    ;
    result
  )
)


(defn OnGetIndications [response]
  (let [
    indicators (filter (fn [x] (if (or (= 1 1) (>= (.indexOf shelters/indicators (:name x)) 0)) true false)) (map map-indication response))
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
      (swap! shelters/app-state assoc-in [:indications] (map map-indication-local tmpindicators))
    )
    (reqnotifications)
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

(defn map-command-local [command]
  (let [
    id (str (get command "id"))
    name (get command "name")
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
    commandsstr (ls/get-item "commands")
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
      (swap! shelters/app-state assoc-in [:commands] (map map-command-local tmpcommands))
    )
    (reqindications)
  )
)


(defn map-group [group]
  (let [
    id (str (get group "groupId"))
    name (get group "groupName")
    parents (get group "parentGroups")
    childs (get group "childEntities")
    owners (get group "owners")
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id id :name name :childs childs :parents parents :owners owners}
    ]
    ;
    result
  )
)

(defn map-group-local [group]
  (let [
    ;tr1 (.log js/console (str "group=" group))
    id (str (get group "id"))
    name (get group "name")
    parents (get group "parents")
    childs (get group "childs")
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
    ;tr1 (.log js/console tmpgroups)
    ]
    (GET (str settings/apipath "getGroups")
         {:handler OnGetGroups
          :error-handler error-handler
          :headers {:content-type "application/json"
                    :token (str (:token  (:token @shelters/app-state))) }
         })
    (if (not (nil? tmpgroups))
      (swap! shelters/app-state assoc-in [:groups] (map map-group-local tmpgroups))
    )
    (reqcommands)
  )
)

(defn map-unitindication [indication]
  (let [
    ;tr1 (.log js/console (str "update=" (get indication "lastUpdateTime")))
    ]
    {:id (get indication "indicationId") :isok (get indication "isOk") :value (get indication "value") :lastupdate (tf/parse shelters/custom-formatter3 (get indication "lastUpdateTime"))}
  )
)

(defn map-unitindication-local [indication]
  (let [
    ;tr1 (.log js/console (str "indication=" indication))
    ]
    {:id (get indication "id") :isok (get indication "isok") :value (get indication "value") :lastupdate (tf/parse (tf/formatters :date-time) (get (get indication "lastupdate") "date"))}
  )
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

    contact1 (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "contact1") true false)
      ) ) (get unit "details"))) "value")

    contact2 (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "contact2") true false)
      ) ) (get unit "details"))) "value")

    indications (map map-unitindication (get unit "indications"))
    indications (if (> (count indications) 0) indications [{:id 1, :isok false, :value "open"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}])
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts [contact1 contact2]}
    ]
    ;
    result
  )
)


(defn map-unit-local [unit]
  (let [
    controller (str (get unit "controller"))
    name (if (nil? (get unit "name")) controller (get unit "name"))
    port (get unit "port")
    port (if (nil? port) 5050 port)
    ip (get unit "ip")
    ip (if (nil? ip) "1.1.1.1" ip)

    status (case (get unit "status") "Normal" 0 3)
    lat (get unit "lat")
    lon (get unit "lon")
    groups (get unit "groups")
    unitid (str (get unit "id"))
    address (get unit "address")
    contacts (get unit "contacts")

    indications (map map-unitindication-local (get unit "indications"))
    indications (if (> (count indications) 0) indications [{:id 1, :isok false, :value "open"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}])
    ;tr1 (.log js/console (str  "unit=" name "; address= " address))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts contacts}
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
    
    ;(.log js/console (str "received: " (first (filter (fn [x] (if (= (:name x) "zuoqin") true false)) units))))
    (ls/set-item! "devices" (.stringify js/JSON (clj->js units)))
  )
)




(defn requnits []
  (let [
    unitsstr (ls/get-item "devices")
    tmpunits (js->clj (.parse js/JSON unitsstr))
    ;tr1 (.log js/console (str (first tmpunits)))
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

		(try
		  (swap! shelters/app-state assoc-in [:devices] (map map-unit-local tmpunits))
		  (catch js/Error e
		    (.clear js/localStorage)
		  )
	    )
      )
    )
    (reqgroups)
  )
)


(defn map-user [user]
  (let [
    username (get user "userName")
    groups (get user "childEntities")
    userid (get user "userId")
    role (first (filter (fn [x] (if (= (:id x) (get (get user "role") "roleId")) true false)) (:roles @shelters/app-state)))

    ;tr1 (.log js/console (str "In map user roleid=" (get (get user "role") "roleId") "; role=" role "; count=" (count (:roles @shelters/app-state))))

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

    phone (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "phone") true false)
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
    result {:login username :groups groups :userid userid :role role :firstname (if (nil? firstname) "" firstname) :lastname (if (nil? lastname) "" lastname) :email (if (nil? email) "" email) :phone (if (nil? phone) "" phone) :addedby (if (nil? addedby) "" addedby) :islocked islocked :iscommand iscommand}
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
    (requnits)
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
    (go
      (<! (timeout 300))
      (requsers)
    )
  )
)


(defn map-word [word]
  (let [
    key (get word "key")
    value (get word "value")
    ]
    ;
    {(keyword key) value}
  )
)


(defn OnGetTranslations [response]
  (let [
    words (loop [result {} words response]
      (if (seq words)
        (let [
            word (first words)
            key (get word "key")
            value (get word "value")
          ]
          (recur (assoc result (keyword key) value) (rest words))
        )
        result
      )
    )
    ]
    (swap! shelters/app-state assoc-in [:words] words)
    (ls/set-item! "words" (.stringify js/JSON (clj->js words)))
  ) 
)




(defn reqtransaltions []
  (let [
    words (ls/get-item "words")
    tmpwords (js->clj (.parse js/JSON words))
  ]
    (swap! shelters/app-state assoc-in [:words] tmpwords)
  )
  (GET (str settings/apipath "getLanguage?languageCode=he") {
    :handler OnGetTranslations
    :error-handler error-handler
    :headers {:content-type "application/json"}
  })
)

(defn onLoginSuccess [response]
  (
    let [
      ;response1 (js->clj response)
      ;tr1 (.log js/console (str  "In LoginSuccess token: " (get response "token") ))
      firstname (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "firstName") true false)
      ) ) (get response "details"))) "value")

      lastname (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "lastName") true false)
      ) ) (get response "details"))) "value")

      newdata {
          :token (get response "token")
          :userid (get response "userId" ) 
          :roleid (get (get response "role") "roleId")
          :rolename (get (get response "role") "roleName")
          :name (str firstname " " lastname)
        }
      ;tr1 (.log js/console newdata)
    ]
    ;(swap! app-state assoc-in [:state] 0)
    ;(.log js/console (str (:token newdata)))
    (swap! shelters/app-state assoc-in [:token] newdata )
    (swap! shelters/app-state assoc-in [:view] 2 )
    (swap! shelters/app-state assoc-in [:users] [] )
    (reqroles)
    (reqtransaltions)
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
    ;(.log js/console (str "user=" theusername " password=" thepassword))
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
    ;(.log js/console "starting login screen" ) 
    
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

(defn processNotification [notification]
  (let [
    unitid (get notification "unitId")
    userid (get notification "userId")
    status (get notification "status")
    type (get notification "notificationType")
    id (get notification "notificationId")
    indicatorid (get notification "indicationId")
    unit (first (filter (fn [x] (if (= (:id x) unitid) true false)) (:devices @shelters/app-state)))
    ;tr1 (.log js/console (str "unit=" unit))
    open (tf/parse shelters/custom-formatter2 (get notification "openTime"))
    
    ;open (if (= (subs (get notification "openTime") 20) "PM") (tc/from-long (+ (tc/to-long open) (* 1000 12 3600))) open)

        
    close (tf/parse shelters/custom-formatter2 (get notification "closeTime"))
    ;close (if (= (subs (get notification "closeTime") 20) "PM") (tc/from-long (+ (tc/to-long close) (* 1000 12 3600))) close)


    accept (tf/parse shelters/custom-formatter2 (get notification "acceptanceTime"))
    ;accept (if (= (subs (get notification "acceptanceTime") 20) "PM") (tc/from-long (+ (tc/to-long close) (* 1000 12 3600))) accept)


    ;tr1 (.log js/console (str "unitid in Notification: " unitid))
    marker (first (filter (fn [x] (if (= (.. x -unitid) unitid) true false)) (:markers @shelters/app-state)))

    indstatus (case status "Closed" true false)
    
    oldindications (:indications (first (filter (fn [x] (if (= (:id x) unitid) true false)) (:devices @shelters/app-state))))

    newindications (map (fn [ind] (if (= (:id ind) indicatorid) (assoc ind :isok indstatus :value "" :lastupdate (tl/local-now)) ind)) oldindications)

    ;tr1 (.log js/console (str "unit: " (:name unit) "; indicator: " indicatorid "; status=" indstatus))

    newunits (map (fn [x] (if (= (:id x) unitid) (assoc x :indications newindications) x)) (:devices @shelters/app-state))
    
    tr1 (swap! shelters/app-state assoc-in [:devices] newunits)

    tr1 (if (or (= type "Failure") (= type "Unknown"))
        (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:alerts @shelters/app-state)))) (swap! shelters/app-state assoc-in [:alerts] (conj (:alerts @shelters/app-state) {:unitid unitid :userid userid :status status :id id :sensorid indicatorid :open open :close close :accept accept :type type}))
          (let [
              delalert (filter (fn [x] (if (= (:id x) id) false true)) (:alerts @shelters/app-state))
              newalerts (conj delalert {:unitid unitid :userid userid :status status :id id :sensorid indicatorid :open open :close close :accept accept :type type})
            ]
            (swap! shelters/app-state assoc-in [:alerts] newalerts)
          )
        )

        (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:notifications @shelters/app-state)))) (swap! shelters/app-state assoc-in [:notifications] (conj (:notifications @shelters/app-state) {:unitid unitid :userid userid :status status :id id :sensorid indicatorid :open open :close close :accept accept :type type}))
          (let [
              delnotification (filter (fn [x] (if (= (:id x) id) false true)) (:notifications @shelters/app-state))
              newnotifications (conj delnotification {:unitid unitid :userid userid :status status :id id :open open :close close :sensorid indicatorid :accept accept :type type})
            ]
            (swap! shelters/app-state assoc-in [:notifications] newnotifications)
          )
        )
    )
    size (js/google.maps.Size. 48 48)

    
    image (clj->js {:url (str iconBase (case indstatus false "red_point.ico" "green_point.ico")) :scaledSize size})
    ]
    (if (nil? marker)
      (.log js/console (str "did not find a unit for unitid=" unitid " in notification"))
      (if (= indicatorid 12)
        (.setIcon marker image)
      )
    )
  )
)


(defn processUnit [unit]
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

    contact1 (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "contact1") true false)
      ) ) (get unit "details"))) "value")

    contact2 (get (first (filter (fn [x] (let [
          ;tr1 (.log js/console (str x))
        ]
        (if (= (get x "key") "contact2") true false)
      ) ) (get unit "details"))) "value")

    indications (map map-unitindication (get unit "indications"))
    indications (if (> (count indications) 0) indications [{:id 1, :isok false, :value "open"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}])
    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts [contact1 contact2]}


    ;tr1 (.log js/console (str "unitid in Notification: " unitid))
    marker (first (filter (fn [x] (if (= (.. x -unitid) unitid) true false)) (:markers @shelters/app-state)))

    newunits (map (fn [x] (if (= (:id x) unitid) (assoc x :indications indications) x)) (:devices @shelters/app-state))
    
    tr1 (swap! shelters/app-state assoc-in [:devices] newunits)

    size (js/google.maps.Size. 48 48)
    indstatus (:isok (first (filter (fn [x] (if (= :id x) 12) true false) indications)))
    
    image (clj->js {:url (str iconBase (case indstatus false "red_point.ico" "green_point.ico")) :scaledSize size})



    infownd (shelters/addMarkerInfo result)
    ]
    
    (if (nil? marker)
      (.log js/console (str "did not find a unit for unitid=" unitid " in notification"))
      (let [
          infownd (:info (first (filter (fn [x] (if (= (:id x) unitid) true false)) (:infownds @shelters/app-state))))
        ]
        ;Remove all click listeners from marker instance.
        ;(.clearListeners js/google.maps.event marker "click")
        (.setContent infownd (shelters/add-marker-info-content result))

        ;; (jquery
        ;;   (fn []
        ;;     (-> marker
        ;;       (.addListener "click"
        ;;         (fn []              
        ;;           (.open infownd (:map @shelters/app-state) marker)
        ;;         )
        ;;       )
        ;;     )
        ;;   )
        ;; )
        
        (.setIcon marker image)
      )
    )
  )
)


(defn processMessage [data]
  (let [
      tr1 (js/console.log "Hooray! Message:" (pr-str data))
      id (get data "notificationId")
    ]
    (if (nil? id)
      (processUnit data)
      (processNotification data)
    )
    ;; (try
      
    ;;   (catch js/Error e
    ;;     (println (str "error while processing message: " e))
    ;;   )
    ;; )
  )
)

(defn restartsocket []
  (if (not (nil? (:ws_channel @shelters/app-state))) (close! (:ws_channel @shelters/app-state)))
  (put! ch 45)
)

(defn receivesocketmsg []
  (go
    (let [
        ;{:keys [ws-channel error]} (<! (ws-ch (str settings/socketpath) {:format :json}))
        {:keys [message error]} (<! (:ws_channel @shelters/app-state))
      ]
      (if error
        (js/console.log "Uh oh:" error)
        (let []
          (if (not (nil? message))
            (let []
              (processMessage message)
              (receivesocketmsg)
            )
            (-> js/document .-location (set! "#/login"))
          )
        )
      )
    )
  )
  ;; (try

  ;;   (catch js/Error e
  ;;     (.log js/console e)
  ;;   )
  ;; )
)


(defn initsocket []
  ;(if (not (nil? (:ws_channel @shelters/app-state))) (close! (:ws_channel @shelters/app-state)))
  (shelters/closesocket)
  (go
    (if (nil? (:ws_channel @shelters/app-state))
      (let [
          ;tr1 (.log js/console (:ws_channel @shelters/app-state))
          ;tr1 (if (not (nil? (:ws_channel @shelters/app-state))) (close! (:ws_channel @shelters/app-state)))
          {:keys [ws-channel error]} (<! (ws-ch (str settings/socketpath) {:format :json}))
          ;{:keys [message error]} (<! ws-channel)        
        ]
        ;(swap! app-state assoc-in [:ws_ch] ws-channel)
        ;(.log js/console (str "token to send in socket: " (:token (:token @shelters/app-state))))
        (if-not error
          (>! ws-channel (:token (:token @shelters/app-state)))
          (js/console.log "Error:" (pr-str error))
        )
        (swap! shelters/app-state assoc-in [:ws_channel] 
          ws-channel
        )
      )
    )
    (receivesocketmsg)
  )
)

(defn gotomap [counter]
  (if (or (> counter 1) (and (> (count (:devices @shelters/app-state)) 0) (> (count (:groups @shelters/app-state)) 0)))

    (let []
      (-> js/document .-location (set! "#/map"))
      ;(aset js/window "location" "#/map")
      (swap! app-state assoc-in [:state] 0)
    )
    
    (go
      (<! (timeout 500))
      (.log js/console (str "count=" (count (:units @shelters/app-state))))
      (swap! app-state assoc-in [:state] 0)
      (gotomap (+ counter 1))
    )
  )
)

(defn setcontrols [value]
  (case value
    42 (gotomap 0)
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

