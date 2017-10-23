(ns shelters.devslist (:use [net.unit8.tower :only [t]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]


            [om-bootstrap.button :as b]
            [clojure.string :as str]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:users [] }))

(defn printDevices []
  (.print js/window)
)

(defn OnGetUsers [response]
   (swap! app-state assoc :users  (get response "Users")  )
   (.log js/console (:users @app-state)) 

)

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)


(defn handleChange [e]
  (let [
    tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! shelters/app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
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
  (if (> (compare (:name dev1) (:name dev2)) 0)
      false
      true
  )
)

(defn goDevice [devid]
  ;;(aset js/window "location" (str "#/devdetail/" devid) )
  (swap! shelters/app-state assoc-in [:view] 7)
  (set! (.-title js/document) (str "יחידה:" devid) )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]

    (dom/tbody
      (map (fn [item]
        (dom/tr {:role "row" :className "odd"}
          (dom/td
            (dom/input { :type "checkbox" :className "device_checkbox" :value (:id item)})
          )
          (dom/td
            (dom/a {:href (str "#/devdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
              (dom/i {:className "fa fa-hdd-o"})
              (:name item)
            )
          )

          (dom/td
            (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
            (case (:status item) 3 "Inactive" "Active")
          )


          (dom/td
            (:address item)
          )

          (dom/td
            (:address item)
          )

          (dom/td
            (dom/i {:className (case (:status item) 3 "fa fa-bullhorn" "fa fa-battery-three-quarters") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
            ;(case (:status item) 3 "Inactive" "Active")
          )


          (dom/td
            
            ;(case (:status item) 3 "Inactive" "Active")
          )

          (dom/td
            (:address item)
          )

          (dom/td
            (:address item)
          )
        )
        )(sort (comp comp-devs) (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (:search @data))) true false)) (:devices @data )))
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
      ;styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "0px" :width "100%"}}
          (dom/div {:className "col-md-12"}
            (dom/div {:className "row"}
              (dom/div
                (b/button {:className "btn btn-primary" :onClick (fn [e] (-> js/document
              .-location
              (set! "#/devdetail")))} "Add New")
              )
              (dom/div
                (dom/button {:className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"}}
                  (dom/i {:className "fa fa-info-circle"}) "Help"
                )
                (dom/a {:href "/download/gg" :className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"}}
                  (dom/i {:className "fa fa-file-excel-o"}) "Export to CSV"
                )
                (dom/button {:className "btn btn-default btn-sm pull-right" :style {:margin-top "-6px" :margin-right "5px"} :onClick (fn [e] (printDevices) )}
                  (dom/i {:className "fa fa-print"}) "Print"
                )

                (dom/h4 {:className "pull-left" :style {:margin-top "0px"}}
                  (dom/i {:className "fa fa-table"}) "EQ - Devices table"
                  (dom/span "(1375)")
                )
              )
            )
            (dom/div {:className "table-responsive" :style {:padding-top "10px"}}
              (dom/div {:className "floatThead-wrapper" :style {:position "relative" :clear "both"}}
                (dom/label
                  (dom/input {:id "search" :type "search" :className "form-control" :placeholder "Search" :onChange (fn [e] (handleChange e ))})
                )


                (dom/table {:id "devicesTable" :className "table table-hover table-responsive table-bordered floatThead-table"}
                  (dom/thead
                    (dom/tr {:className "info" :role "row"}
                      (dom/th {:className "sorting_asc" :style {:width "15px" :valign "middle" }}
                        (dom/i {:className "fa fa-square-o"})
                      )
                      (dom/th {:className "sorting" :style {:width "100px"}}
                        (dom/i {:className "fa fa-bullseye"})
                        (dom/b "Device")
                      )

                      (dom/th {:className "sorting" :style {:width "70px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullseye"})
                        (dom/b "Status")
                      )

                      (dom/th {:className "sorting" :style {:width "184px" :text-align "center"}}
                        (dom/i {:className "fa fa-map-marker"})
                        (dom/b "Location")
                      )

                      (dom/th {:className "sorting" :style {:width "184px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "Alert")
                      )

                      (dom/th {:className "sorting" :style {:width "70px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullhorn"})
                        (dom/b "Bars")
                      )

                      (dom/th {:className "sorting" :style {:width "70px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullhorn"})
                        (dom/b "Practice")
                      )

                      (dom/th {:className "sorting" :style {:width "127px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullhorn"})
                        (dom/b "Contact 1")
                      )

                      (dom/th {:className "sorting" :style {:width "127px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullhorn"})
                        (dom/b "Contact 2")
                      )
                    )
                  )

                  (dom/colgroup
                    (dom/col {:style {:width "30px"}})
                    (dom/col {:style {:width "91px"}})
                    (dom/col {:style {:width "68px"}})
                    (dom/col {:style {:width "342px"}})
                    (dom/col {:style {:width "184px"}})
                    (dom/col {:style {:width "70px"}})
                    (dom/col {:style {:width "70px"}})
                    (dom/col {:style {:width "127px"}})
                    (dom/col {:style {:width "127px"}})
                  )
                  (om/build showdevices-view  data {})
                  (
                  )
                )
              )
            )
          )
        )
        (dom/div {:className "panel panel-primary"} ;;:onClick (fn [e](println e))
        
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
        )
      ) 
    )
  )
)




(sec/defroute dashboard-page "/devslist" []
  (om/root dashboard-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


