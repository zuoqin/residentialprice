(ns shelters.users (:use [net.unit8.tower :only [t]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]


            [om-bootstrap.button :as b]

            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:users [] :trips [] }))


(defn OnGetUsers [response]
   (swap! app-state assoc :users  (get response "Users")  )
   (.log js/console (:users @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn getUsers [data] 
  (GET (str settings/apipath "api/user") {
    :handler OnGetUsers
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token  (first (:token @shelters/app-state)))) }
  })
)


(defn comp-users
  [user1 user2]
  (if (> (compare (:login user1) (:login user2)) 0)
      false
      true
  )
)


(defcomponent showusers-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/div {:className "row"}
          (dom/div {:className "col-xs-4"}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:login item)}} nil)
            )
          )
          (dom/div {:className "col-xs-4"}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (:firstname item)
            )
          )

          (dom/div {:className "col-xs-4"}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (:lastname item)
            )
          )
        )   
        )(sort (comp comp-users) (:users @shelters/app-state ))
      )
    )
  )
)



(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] "Users")
)



(defcomponent users-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div  (assoc styleprimary  :className "panel panel-primary" ;;:onClick (fn [e](println e))
        )
          (dom/div
            (b/button {:className "btn btn-primary" :onClick (fn [e] (
              (shelters/goUserDetail e)
              (-> js/document .-location (set! "#/userdetail"))
))} "Add New")
          )
          ; (dom/div {:className "panel-heading"}
          ;   (dom/div {:className "row"}
          ;     ; (dom/div {:className "col-md-10"}
          ;     ;   (dom/span {:style {:padding-left "5px"}} "我的消息")
          ;     ; )
          ;     ; (dom/div {:className "col-md-2"}
          ;     ;   (dom/span {:className "badge" :style {:float "right" }} (str (:msgcount data))  )
          ;     ; )
          ;   )
          ; )
          (om/build showusers-view  data {})
        )
      )
    )
  )
)




(sec/defroute users-page "/users" []
  (om/root users-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


