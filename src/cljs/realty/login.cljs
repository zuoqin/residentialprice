(ns realty.login
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [realty.core :as realty]
            [realty.main :as main]
            [realty.settings :as settings]


            [cljs-time.format :as tf]
            [cljs-time.core :as tc]
            [cljs-time.coerce :as te]
            [cljs-time.local :as tl]

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

;(def iconBase "/images/")
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



(defn onLoginSuccess [response]
  (
    let [
      response1 (js->clj response)
      tr1 (.log js/console (str  "In LoginSuccess token: " (get response "token") ))


      ;tr1 (.log js/console newdata)
    ]
    (swap! app-state assoc-in [:state] 0)
    (swap! realty/app-state assoc-in [:state] 0)
    ;(.log js/console (str (:token newdata)))
    (swap! realty/app-state assoc-in [:token] (get response "token") )
    ;(swap! realty/app-state assoc-in [:view] 2 )
    (aset js/window "location" "#/main")
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
    (swap! realty/app-state assoc-in [:user :login] username)

    ;; currently selected user
    (swap! realty/app-state assoc-in [:selecteduser] username)


    (POST (str settings/apipath "login")
      {:handler OnLogin
       :error-handler onLoginError
       :format :json
       :params {:username username :password password} 
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
  (set! (.-title js/document) "Вход в систему оценки жилой недвижимости")
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
    (dom/div {:className "container" :style {:width "100%" :padding-top "10px" :margin-top "100px" :backgroundImage "url(images/fincaselogo.png)" :backgroundRepeat "no-repeat" :backgroundPosition "top center"}  }
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



;; (defn setcontrols [value]
;;   (case value
;;     42 (gotomap 0)
;;     45 (initsocket)
;;   )
;; )

(defn initqueue []
  (doseq [n (range 1000)]
    (go ;(while true)
      (take! ch(
        fn [v] (
           ;setcontrols v
           (.log js/console v)
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
      (:view (if (= data nil) @realty/app-state @data ))
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
  (let [
    theurl (new js/URL (.-href (.-location js/window)))
    username (.get (.-searchParams theurl) "username")
    password (.get (.-searchParams theurl) "password")
    ]
    (if (and (not (nil? username)) (not (nil? password))) 
      ;(swap! realty/app-state assoc-in [:user :login] username)
      (dologin username password)
      (-> js/document .-location (set! "#/login"))
    )
  )
)

(main)

