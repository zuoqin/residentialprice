(ns shelters.devs (:use [net.unit8.tower :only [t]])
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


(defn comp-devs
  [dev1 dev2]
  (if (> (compare (:id dev1) (:id dev2)) 0)
      false
      true
  )
)


(defcomponent showdevices-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/span
          (dom/a {:className "list-group-item" :href (str  "#/userdetail/" (:login item ) ) }
            (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:login item)}} nil)
            ;(dom/h4 {:className "list-group-item-heading"} (get item "subject"))
            (dom/h6 {:className "paddingleft2"} (get item "senddate"))
            ;(dom/p  #js {:className "list-group-item-text paddingleft2" :dangerouslySetInnerHTML #js {:__html (get item "body")}} nil)
          ) 
        )                  
        )(sort (comp comp-devs) (:devices @data ))
      )
    )
  )
)



(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] 
    "Dashboard"
  )
)



(defcomponent dashboard-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "70px"}}
          (dom/div {:className "col-md-12"}
            (dom/div
              (dom/div
                (dom/button {:className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"}}
                  (dom/i {:className "fa fa-info-circle"}) "Help"
                )
                (dom/a {:href "/download/gg" :className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"}}
                  (dom/i {:className "fa fa-file-excel-o"}) "Export to CSV"
                )
                (dom/button {:className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"}}
                  (dom/i {:className "fa fa-print"}) "Print"
                )

                (dom/h4 {:className "pull-left" :style {:margin-top "0px"}}
                  (dom/i {:className "fa fa-table"}) "EQ - Devices table"
                  (dom/span "(1375)")
                )
              )
            )
            (dom/div {:className "table-responsive"}
              (dom/label
                (dom/input {:type "search" :className "form-control" :placeholder "Search"})
              )
            )
            (dom/table {:className "table table-hover table-responsive table-bordered floatThead-table"}
              (dom/thead
                (dom/tr {:className "info" :role "row"}
                  (dom/th {:style {:width "15px" :valign "middle" }}
                    (dom/i {:className "fa fa-square-o"})
                  )
                  (dom/th {:style {:width "100px"}}
                    (dom/i {:className "fa fa-bullseye"})
                    (dom/b "Device")
                  )
                )
              )
              (dom/tbody
                (dom/tr {:role "row" :className "odd"}
                  (dom/td
                    (dom/input { :type "text" :className "device_checkbox" :value "888"})
                  )
                )
              )

            )

          )

        )
        (dom/div  (assoc styleprimary  :className "panel panel-primary" ;;:onClick (fn [e](println e))
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
          (om/build showdevices-view  data {})
        )
      ) 


    )
  )
)




(sec/defroute dashboard-page "/dashboard" []
  (om/root dashboard-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


