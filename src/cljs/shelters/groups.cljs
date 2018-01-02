(ns shelters.groups
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

(defonce app-state (atom  {:groups []}))


(defn OnGetGroups [response]
   (swap! app-state assoc :groups  (get response "Groups")  )
   (.log js/console (:groups @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn getGroups [data] 
  (GET (str settings/apipath "api/user") {
    :handler OnGetGroups
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token  (first (:token @shelters/app-state)))) }
  })
)


(defn comp-groups
  [group1 group2]
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)


(defcomponent showgroups-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/span
          (dom/a {:className "list-group-item" :href (str "#/groupdetail/" (:id item))}
            (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:name item)}} nil)
            ;(dom/h4 {:className "list-group-item-heading"} (get item "subject"))
            ;(dom/h6 {:className "paddingleft2"} (get item "senddate"))
            ;(dom/p  #js {:className "list-group-item-text paddingleft2" :dangerouslySetInnerHTML #js {:__html (get item "body")}} nil)
          ) 
        )                  
        )(sort (comp comp-groups) (:groups @shelters/app-state ))
      )
    )
  )
)



(defn onMount [data]
  ; (getGroups data)
  (swap! shelters/app-state assoc-in [:current] 
    "Groups"
  )
  (set! (.-title js/document) "ניהול קבוצות")
  (swap! shelters/app-state assoc-in [:view] 8)
)



(defcomponent groups-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:padding-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div  (assoc styleprimary  :className "panel panel-primary" ;;:onClick (fn [e](println e))
        )
          (dom/h1 {:style {:text-align "center"}} (:current @data))
          (dom/div
            (b/button {:className "btn btn-primary" :onClick (fn [e] (-> js/document
          .-location
          (set! "#/groupdetail")))} "Add New")
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
          (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
            (dom/div {:className "row"}
              (dom/div {:className "col-xs-6 col-md-6" :style {:text-align "center" :border-left "1px solid"}}
                (dom/h5 "Name")
              )
            )

          )
          (om/build showgroups-view  data {})
        )
      )
    )
  )
)




(sec/defroute groups-page "/groups" []
  (om/root groups-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


