(ns shelters.groupstouser (:use [net.unit8.tower :only [t]])
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
            [shelters.devdetail :as devdetail]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:user {} :groups []}))

(def jquery (js* "$"))
(def ch (chan (dropping-buffer 2)))


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
          isselected (if (and (nil? (:groups (:selecteduser @shelters/app-state)))) false (if (> (.indexOf (:groups (:selecteduser @shelters/app-state)) (:id item)) -1) true false))
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
                        groups (:groups (:selecteduser @shelters/app-state))
                        newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))

                        tr1 (.log js/console (str "groupid=" id))
                      ]
                      (.stopPropagation e)
                      ;(.stopImmediatePropagation (.. e -nativeEvent) )
                      (swap! shelters/app-state assoc-in [:selecteduser :groups] newgroups)
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
;;       groups (:groups (:user @app-state))
;;       newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))
;;     ]
;;     (.stopPropagation e)
;;     (.stopImmediatePropagation (.. e -nativeEvent) )
;;     (swap! app-state assoc-in [:user :groups] newgroups)
;;   )
;; )

(defcomponent showgroups-view [data owner]
  (render
    [_
      
    ]
    (let [
      ;tr1 (.log js/console data)
      ]
      (dom/div {:className "panel-body" :style {:max-height "300px" :overflow-y "scroll" :display "block"}}
        (map (fn [item]
          (let [

            isselected (if (and (nil? (:groups (:selecteduser @data)))) false (if (> (.indexOf (:groups (:selecteduser @data)) (:id item)) -1) true false))         
            ]
            (dom/div {:className "row"}
              (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}

              )
              (dom/div {:className "col-xs-4" }
                (dom/h4 {:className "list-group-item-heading"}
                  (:name item)
                )
              )

              (dom/div {:className "col-xs-3" :style {:text-align "center"}}
                (dom/h5 (case isselected true "On" "Off"))
              )

              (dom/div {:className "col-xs-3" :style {:text-align "center"}}
                (dom/label {:className "checkbox-inline"}              
                  (dom/input {:id (str "chckbgroup" (:id item)) :type "checkbox" :checked isselected :data-toggle "toggle" :data-size "large" :data-width "100" :data-height "34" ;:onChange (fn [e] (handle-chkgroup-change e ))
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
  (swap! shelters/app-state assoc-in [:current] "Groups to Unit")
  (put! ch 47)
)

(defn updateGroups []
  (let [
      users (:users @shelters/app-state)
      deluser (remove (fn [user] (if (= (:userid user) (:userid (:user @app-state)) ) true false  )) users)
      adduser (conj deluser (:user @app-state))
    ]
    (swap! shelters/app-state assoc-in [:users] adduser)
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




(sec/defroute groups-page "/groupstouser/:userid" [userid]
  (let [
    user (first (filter (fn [x] (if (= (str userid) (:userid x)) true false)) (:users @shelters/app-state)))
    tr1 (.log js/console (:firstname user))
    ]
    (swap! app-state assoc-in [:user] user)
    (swap! app-state assoc-in [:current] (str "Assign Groups to " (:firstname user)))
    (om/root groups-view
           app-state
           {:target (. js/document (getElementById "app"))})
  )
)
