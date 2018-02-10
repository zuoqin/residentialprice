(ns shelters.groupstogroup
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]
            [clojure.string :as str]

            [om-bootstrap.button :as b]
            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]
            [shelters.settings :as settings]
            ;[shelters.groups :as devdetail]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:groups []}))

(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))
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

(defn setcheckboxtoggle []
  (let [
    tr1 (.log js/console "Calling setcheckboxtoggle")
    ]
    (doall
      (map (fn [item]
        (let [
          isselected (if (and (nil? (:parents (:selectedgroup @shelters/app-state)))) false (if (> (.indexOf (:parents (:selectedgroup @shelters/app-state)) (:id item)) -1) true false))
          ]
          (jquery
            (fn []
               (-> (jquery (str "#chckbgroup" (:id item)))
                 (.bootstrapToggle (clj->js {:on "הוסיף" :off "לא נוסף"}))
               )
            )
          )

          (jquery
            (fn []
               (-> (jquery (str "#chckbgroup" (:id item)))
                 (.bootstrapToggle (case isselected true "on" "off"))
               )
            )
          )

          (jquery
            (fn []
              (-> (jquery (str "#chckbgroup" (:id item)))
                (.on "change"
                  (fn [e]
                    (let [
                        id (str/join (drop 10 (.. e -currentTarget -id)))
                        groups (:parents (:selectedgroup @shelters/app-state))
                        newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))

                        tr1 (.log js/console (str "id=" id "; checked:" (.. e -currentTarget -checked)))
                      ]
                      (.stopPropagation e)
                      ;(.stopImmediatePropagation (.. e -nativeEvent) )
                      (swap! shelters/app-state assoc-in [:selectedgroup :parents] newgroups)
                    )
                  )
                )
              )
            )
          )
        )
        )
        (:groups @shelters/app-state)
      )
    )
  )
)


;; (defn handle-chkgroup-change [e]
;;   (let [
;;       id (str/join (drop 10 (.. e -currentTarget -id)))
;;       groups (:parents (:selectedgroup @shelters/app-state))
;;       newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))
;;       tr1 (.log js/console (str "new parents:" newgroups))
;;     ]
;;     (.stopPropagation e)
;;     (.stopImmediatePropagation (.. e -nativeEvent) )
;;     (swap! shelters/app-state assoc-in [:selectedgroup :parents] newgroups)
;;   )
;; )

(defcomponent showgroups-view [data owner]
  (render
    [_
      
    ]
    (let [
      ;tr1 (.log js/console data)
      ]
      (dom/div {:className "list-group" :style {:display "block"}}
        (map (fn [item]
          (let [
            isselected (if (and (nil? (:parents (:selectedgroup @data)))) false (if (> (.indexOf (:parents (:selectedgroup @data)) (:id item)) -1) true false))   
            ]
            (dom/div {:className "row"}
              (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}

              )
              (dom/div {:className "col-md-4" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/a {:className "list-group-item" :href (str "#/groupdetail/" (:id item))}
                  (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:name item)}} nil)
                  ;(dom/h4 {:className "list-group-item-heading"} (get item "subject"))
                  ;(dom/h6 {:className "paddingleft2"} (get item "senddate"))
                  ;(dom/p  #js {:className "list-group-item-text paddingleft2" :dangerouslySetInnerHTML #js {:__html (get item "body")}} nil)
                )
              )

              (dom/div {:className "col-xs-3" :style {:text-align "center"}}
                (dom/h5 (case isselected true "On" "Off"))
              )

              (dom/div {:className "col-xs-3" :style {:text-align "center"}}
                (dom/label {:className "checkbox-inline"}              
                  (dom/input {:id (str "chckbgroup" (:id item)) :type "checkbox" :checked isselected :data-toggle "toggle" :data-size "large" :data-width "100" :data-height "34"
;:onChange (fn [e] (handle-chkgroup-change e ))
                    }
                  )
                )
              )

              (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
              )
            )
          )
          )(sort (comp comp-groups) (:groups @shelters/app-state ))
        )
      )
    )

  )
)


(defn setcontrols [value]
  (case value
    ;46 (setGroup)
    47 (setcheckboxtoggle)
  )
)


(defn initqueue []
  (doseq [n (range 1000)]
    (go ;(while true)
      (take! ch(
        fn [v] (
           ;.log js/console v
           ;(setcalculatedfields) 
           setcontrols v
           
           ;(.log js/console v)  
          )
        )
      )
    )
  )
)


(initqueue)


(defn onMount [data]
  ; (getGroups data)
  (swap! shelters/app-state assoc-in [:current] "Groups to Group")
  (put! ch 47)
)

(defn updateGroups []
  (let [
      groups (:groups @shelters/app-state)
      delgroup (remove (fn [group] (if (= (:id group) (:id (:selectedgroup @shelters/app-state)) ) true false  )) groups)
      addgroup (conj delgroup (:selectedgroup @shelters/app-state))
    ]
    (swap! shelters/app-state assoc-in [:groups] addgroup)
  )
 
  ;(swap! shelters/app-state assoc-in [:device :groups] (:groups (:device @app-state)))
  (js/window.history.back)
)

(defcomponent groups-view [data owner]
  (did-mount [_]
    (onMount data)
  )
  (render [_]
    (let [
      style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:padding-top "70px"}}      
      ]
      (dom/div
        (om/build shelters/website-view shelters/app-state {})
        (dom/div  (assoc styleprimary  :className "panel panel-primary" ;;:onClick (fn [e](println e))
        )
          (dom/h1 {:style {:text-align "center"}} (:current @data))
          (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
            (dom/div {:className "row"}
              (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
                
              )
              (dom/div {:className "col-xs-5 col-md-5" :style {:text-align "center" :border-left "1px solid"}}
                (dom/h5 "Name")
              )

              (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "1px solid"}}
                (dom/h5 "Selection")
              )

              (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "1px solid"}}
                (dom/h5 "Selected")
              )
            )

          )
          (om/build showgroups-view  data {})
        )

        (dom/div

          (b/button {:className "btn btn-default" :onClick (fn [e] (updateGroups))} "Update")

          (b/button {:className "btn btn-info" :onClick (fn [e]
              (js/window.history.back))} "Cancel"
          )
        )
      )
    )
  )
)




(sec/defroute groups-page "/groupstogroup/:groupid" [groupid]
  (let [
    group (first (filter (fn [x] (if (= groupid (:id x)) true false)) (:groups @shelters/app-state)))
    ;tr1 (.log js/console (:name group))
    ]
    (swap! app-state assoc-in [:group] group)
    (swap! app-state assoc-in [:current] (str "Assign Groups to " (:name group)))
    (om/root groups-view
           app-state
           {:target (. js/document (getElementById "app"))})
  )
)
