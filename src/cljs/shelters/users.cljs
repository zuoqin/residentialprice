(ns shelters.users (:use [net.unit8.tower :only [t]])
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
  (case (:sort-list @app-state)
    1 (if (> (compare (:login user1) (:login user2)) 0)
        false
        true
      )

    2 (if (> (compare (:login user1) (:login user2)) 0)
        true
        false
      )


    3 (if (> (compare (:firstname user1) (:firstname user2)) 0)
        true
        false
      )

    4 (if (> (compare (:firstname user1) (:firstname user2)) 0)
        false
        true
      )

    5 (if (> (compare (:lastname user1) (:lastname user2)) 0)
        true
        false
      )

    6 (if (> (compare (:lastname user1) (:lastname user2)) 0)
        false
        true
      )


    (if (> (compare (:login user1) (:login user2)) 0)
        false
        true
    )
  )
)


(defcomponent showusers-view [data owner]
  (render
    [_]
    (dom/div {:className "list-group" :style {:display "block"}}
      (map (fn [item]
        (dom/div {:className "row" :style {:border-top "1px solid"}}
          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:login item)}} nil)
            )
          )
          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:firstname item)}} nil)
            )
          )

          (dom/div {:className "col-xs-4" :style { :border-left "1px solid"}}
            (dom/a {:className "list-group-item" :href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (shelters/goUserDetail e))}
              (dom/h4  #js {:className "list-group-item-heading" :dangerouslySetInnerHTML #js {:__html (:lastname item)}} nil)
            )
          )
        )   
        )(sort (comp comp-users) (filter (fn [x] (if (or (str/includes? (str/lower-case (:firstname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:lastname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:login x)) (str/lower-case (:search @data)))) true false)) (:users @data)))
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
      (dom/div {:style {:padding-top "70px"}}
        (om/build shelters/website-view data {})

        (dom/div {:className "panel panel-primary"}
          (dom/h1 {:style {:text-align "center"}} (:current @data))

          (dom/div
            (b/button {:className "btn btn-primary" :onClick (fn [e] (
              (shelters/goUserDetail e)
              (-> js/document .-location (set! "#/userdetail"))
  ))} "הוסף משתמש חדש")
          )
          (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
            (dom/div {:className "row"}

              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1))
  (shelters/doswaps)
                ))} "קוד משתמש") (case (:sort-list @app-state) 1 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 2 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))


              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3))
  (shelters/doswaps)
                ))} "שם פרטי") (case (:sort-list @app-state) 3 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 4 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))


              (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}} (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (
  (swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5))
  (shelters/doswaps)
                ))} "שם משפחה") (case (:sort-list @app-state) 5 (dom/span {:className "glyphicon glyphicon-arrow-up"}) 6 (dom/span {:className "glyphicon glyphicon-arrow-down"}) (dom/span)))

            )
          )
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


