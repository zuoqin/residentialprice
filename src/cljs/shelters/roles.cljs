(ns shelters.roles (:use [net.unit8.tower :only [t]])
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

(defonce app-state (atom  {:roles []}))


(defn OnGetRoles [response]
   (swap! app-state assoc :roles  (get response "Roles")  )
   (.log js/console (:roles @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn getRoles [data] 
  (GET (str settings/apipath "api/user") {
    :handler OnGetRoles
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token  (first (:token @shelters/app-state)))) }
  })
)


(defn comp-roles
  [role1 role2]
  (if (> (compare (:name role1) (:name role2)) 0)
      false
      true
  )
)


(defcomponent showroles-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/span
          (dom/a {:className "list-group-item" :href (str "#/roledetail/" (:name item)) :onClick (fn [e] (shelters/goRoleDetail e))}
            (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:description item)}} nil)
            ;(dom/h4 {:className "list-group-item-heading"} (get item "subject"))
            ;(dom/h6 {:className "paddingleft2"} (get item "senddate"))
            ;(dom/p  #js {:className "list-group-item-text paddingleft2" :dangerouslySetInnerHTML #js {:__html (get item "body")}} nil)
          ) 
        )                  
        )(sort (comp comp-roles) (:roles @shelters/app-state ))
      )
    )
  )
)



(defn onMount [data]
  ; (getRoles data)
  (swap! shelters/app-state assoc-in [:current] 
    "Roles"
  )
)



(defcomponent roles-view [data owner]
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
            (b/button {:className "btn btn-primary" :onClick (fn [e] (-> js/document
          .-location
          (set! "#/roledetail")))} "Add New")
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
          (om/build showroles-view  data {})
        )
      )
    )
  )
)




(sec/defroute roles-page "/roles" []
  (om/root roles-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


