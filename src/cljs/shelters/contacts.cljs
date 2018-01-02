(ns shelters.contacts
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

(defonce app-state (atom  {:sort-list 1}))


(defn comp-contacts
  [contact1 contact2]
  (case (:sort-list @app-state)
    1 (if (> (compare (:name contact1) (:name contact2)) 0)
        false
        true
      )

    2 (if (> (compare (:name contact1) (:name contact2)) 0)
        true
        false
      )


    3 (if (> (compare (:phone contact1) (:phone contact2)) 0)
        true
        false
      )

    4 (if (> (compare (:phone contact1) (:phone contact2)) 0)
        false
        true
      )

    5 (if (> (compare (:email contact1) (:email contact2)) 0)
        true
        false
      )

    6 (if (> (compare (:email contact1) (:email contact2)) 0)
        false
        true
      )


    (if (> (compare (:name contact1) (:name contact2)) 0)
        false
        true
    )
  )
)


(defcomponent showcontacts-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/div {:className "row" :style {:border-top "1px solid"}}
          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/contactdetail/" (:id item))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:name item)}} nil)
            )
          )
          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/contactdetail/" (:id item))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:phone item)}} nil)
            )
          )

          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/contactdetail/" (:id item))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:email item)}} nil)
            )
          )
        )   
        )(sort (comp comp-contacts) (filter (fn [x] (if (or (str/includes? (str/lower-case (:name x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:phone x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:email x)) (str/lower-case (:search @data)))) true false)) (:contacts @data)))
      )
    )
  )
)



(defn onMount [data]
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] "Contacts")
  (swap! shelters/app-state assoc-in [:view] 4)
)



(defcomponent contacts-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div {:style {:padding-top "70px"}}
        (om/build shelters/website-view data {})
        (dom/div {:className "panel panel-primary"} ;;:onClick (fn [e](println e))        
          (dom/div
            (b/button {:className "btn btn-primary" :onClick (fn [e] (
              (-> js/document .-location (set! "#/contactdetail"))
))} "הוסף איש קשרמ חדש")
          )
          (dom/div {:className "panel-heading" :style {:padding "0px"}}
            (dom/div {:className "row"}

              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1))
  (shelters/doswaps)
                ))} "Name") (case (:sort-list @app-state) 1 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 2 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))


              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3))
  (shelters/doswaps)
                ))} "Telephone") (case (:sort-list @app-state) 3 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 4 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))


              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3))
  (shelters/doswaps)
                ))} "email") (case (:sort-list @app-state) 3 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 4 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))
            )
          )
          (om/build showcontacts-view  data {})
        )
      )
    )
  )
)




(sec/defroute users-page "/contacts" []
  (om/root contacts-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


