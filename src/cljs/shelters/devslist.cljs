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

(defonce app-state (atom  {}))

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

(defn handle-chkbsend-change [e]
  (let [
        id (str/join (drop 8 (.. e -currentTarget -id)))
        
        devices (:selectedunits @shelters/app-state)


        ;tr2 (.log js/console (.. e -currentTarget) )

        ;tr1 (.log js/console (str "amount1=" amount1 " client=" client))
        
        deldevs (remove (fn [dev] (if (= dev id) true false  )) devices)

        adddev (if (.. e -currentTarget -checked) (into [] (conj deldevs id)) deldevs) 
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! shelters/app-state assoc-in [:selectedunits] adddev)
  )
)


(defn goDevice [devid]
  ;;(aset js/window "location" (str "#/devdetail/" devid) )
  (swap! shelters/app-state assoc-in [:view] 7)
  (set! (.-title js/document) (str "יחידה:" devid) )
)

(defcomponent showstatuses [data owner]
  (render
    [_]
    (dom/td
      (map (fn [item]
        (let []
          (dom/i {:className (case (:isok item) false "fa fa-bullhorn" "fa fa-bullhorn") :style {:color (case (:isok item) false "#dd0000" "#00dd00") :font-size "18px"}})
          ;(dom/i {:className (case (:isok item) false "fa fa-bullhorn" "fa fa-battery-three-quarters") :style {:color (case (:isok item) false "#dd0000" "#00dd00") :font-size "24px"}})
          
        )
        )
        
      (:indications data))
    )
  )
)

(defcomponent showdevices-view [data owner]
  (render
    [_]

    (dom/tbody
      (map (fn [item]
        (let [
          isselected (if (= (.indexOf (:selectedunits @data) (:id item)) -1) false true)
          ;tr1 (.log js/console (str item))
          ]
          (dom/tr {:role "row" :className "odd"}
            (dom/td
              (dom/input { :id (str "checksel" (:id item)) :type "checkbox" :className "device_checkbox" :checked isselected :onChange (fn [e] (handle-chkbsend-change e))})
            )

            (dom/td
              (dom/div { :className "dropdown"}
                (b/button {:className "btn btn-danger dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
                  "☰"
                )
                (dom/ul {:className "dropdown-menu" :aria-labelledby "dropdownMenuButton"}
                  (dom/li {:className "dropdown-item"}
                    (dom/a {:href (str "#/unitdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                      "View"
                    )

                  )
                  (dom/li {:className "dropdown-item" :href "#"}
                    (dom/a {:href (str "#/devdetail/" (:id item))}
                      "Edit"
                    )
                  )
                )
              )
            )


            (dom/td
              (dom/a {:href (str "#/unitdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o"})
                (:controller item)
              )
            )


            (dom/td
              (dom/a {:href (str "#/unitdetail/" (:id item)) :onClick (fn [e] (goDevice (:id item)))}
                (dom/i {:className "fa fa-hdd-o"})
                (:name item)
              )
            )

            (dom/td
              (:address item)
            )


            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )

            (dom/td
              (dom/i {:id (str "status_" (:id item)) :className (case (:status item) 3 "fa-toggle-off fa" "fa-toggle-on fa") :style {:color (case (:status item) 3 "#dd0000" "#00dd00") :font-size "24px"}})
              (case (:status item) 3 "Inactive" "Active")
            )
            ;(om/build showstatuses item {})
          )
          )
        ) (sort (comp comp-devs) (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (:search @data))) true false)) (:devices @data )))
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

(defn OnDoCommand [response] 
  (.log js/console (str response ))
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn sendcommand1 []
  (POST (str settings/apipath "doCommand" ;"?userId="(:userid  (:token @shelters/app-state))
       )
       {:handler OnDoCommand
        :error-handler error-handler
        :format :json
        :headers {:token (str (:token  (:token @shelters/app-state)))}
        :params {:commandId (js/parseInt (:id (first (:commands @shelters/app-state)))) :units (into [] (:selectedunits @shelters/app-state)) }
    }
  )
)

(defcomponent topbuttons-view [data owner]
  (render [_]
    (dom/div {:className "row" :style {:padding-top "70px"}}
      (dom/div {:className "row"}
        (dom/div {:className "col-xs-1"}
          (b/button {:className "btn btn-primary" :onClick (fn [e]
            (-> js/document .-location (set! "#/devdetail")))} "Add New"
          )
        )

        (dom/div {:className "col-xs-1"}
          (b/button {:className "btn btn-primary"
            :disabled? (= (count (:selectedunits @data)) 0)
            :onClick (fn [e] (sendcommand1))} (str (:name (nth (:commands @data) 0)) " (" (count (:selectedunits @data)) ") units")
          )
        )
      )
    )
  )
)

(defcomponent dashboard-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      ;tr1 (.log js/console (:name (first (:commands @data))))
      ]
      (dom/div
        (om/build shelters/website-view data {})
        (dom/div {:className "container" :style {:margin-top "0px" :width "100%"}}
          (dom/div {:className "col-md-12"}
            
            (om/build topbuttons-view data {})

            ;; (dom/div {:className "row":style {:padding-top "10px"}}
            ;;   (dom/div
            ;;     (b/button {:className "btn btn-primary" :onClick (fn [e]
            ;;       (-> js/document
            ;;         .-location
            ;;         (set! "#/devdetail")
            ;;       ))} "Add New"
            ;;     )
            ;;   )
            ;;   (dom/div
 
            ;;   )
            ;; )
            (dom/div {:className "table-responsive" :style {:padding-top "10px"}}
              (dom/div {:className "floatThead-wrapper" :style {:position "relative" :clear "both"}}
                (dom/table {:id "devicesTable" :className "table table-hover table-responsive table-bordered floatThead-table"}
                  (dom/thead
                    (dom/tr {:className "info" :role "row"}
                      (dom/th {:className "sorting_asc" :style {:width "15px" :valign "middle" }}
                        (dom/i {:className "fa fa-square-o"})
                      )

                      (dom/th {:className "sorting_asc" :style {:width "15px" :valign "middle" }}
                        
                      )
                      (dom/th {:className "sorting" :style {:width "150px"}}
                        (dom/i {:className "fa fa-bullseye"})
                        (dom/b "מזהה יחידה")
                      )

                      (dom/th {:className "sorting" :style {:width "150px" :text-align "center"}}
                        ;(dom/i {:className "fa fa-bullseye"})
                        (dom/b "שם יחידה")
                      )

                      (dom/th {:className "sorting" :style {:width "400px" :text-align "center"}}
                        (dom/i {:className "fa fa-map-marker"})
                        (dom/b "כתובת")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
                      )

                      (dom/th {:className "sorting" :style {:width "120px" :text-align "center"}}
                        (dom/i {:className "fa fa-bullhorn"})
                        (dom/b "חיישן בשם")
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
                  (om/build showdevices-view data {})
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


