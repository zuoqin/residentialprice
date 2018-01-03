(ns shelters.users
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST PUT DELETE]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! timeout]]
            [cljs-time.core :as tc]
            [cljs-time.format :as tf]
            [shelters.groupstouser :as groupstouser]
            [om-bootstrap.button :as b]
            [clojure.string :as str]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(enable-console-print!)
(def ch (chan (dropping-buffer 2)))
(defonce app-state (atom  {:sort-list 1}))
(def jquery (js* "$"))
(defn handleChange [e]
  (let [
    ;tr1 (.log js/console (str (.. e -nativeEvent -target -id)))
    ]
  )
  (swap! shelters/app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)


(defn goUser [userid]
  ;;(aset js/window "location" (str "#/devdetail/" devid) )
  (swap! shelters/app-state assoc-in [:view] 4)
  (set! (.-title js/document) (str "משתמש: " userid) )
)


(defn OnUpdateUserError [response]
  (let [     
    ]

  )
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn OnUpdateUserSuccess [response]
  (let [
      users (:users @shelters/app-state)
      deluser (remove (fn [user] (if (= (:userid user) (:userid (:selecteduser @shelters/app-state)) ) true false  )) users)
      adduser (conj deluser (:selecteduser @shelters/app-state))
    ]
    (swap! shelters/app-state assoc-in [:users] adduser)
    ;(shelters/goDashboard nil)
    (js/window.history.back)
  )
)


(defn savegroups []
  (let [
    tr1 (swap! app-state assoc-in [:user :addedby] (:userid (:token @shelters/app-state)))
    ]
    (PUT (str settings/apipath  "updateUser") {
      :handler OnUpdateUserSuccess
      :error-handler OnUpdateUserError
      :headers {
        :token (str (:token (:token @shelters/app-state)))
      }
      :format :json
      :params { :userName (:login (:selecteduser @shelters/app-state)) :childEntities (:groups (:selecteduser @shelters/app-state)) :userId (:userid (:selecteduser @shelters/app-state)) :token (:token (:token @shelters/app-state)) :role {:roleId (:id (:role (:selecteduser @shelters/app-state))) :roleName (:name (:role (:selecteduser @shelters/app-state))) :roleLevel (:level (:role (:selecteduser @shelters/app-state))) :roleDescription (:description (:role (:selecteduser @shelters/app-state)))} :details [{:key "firstName" :value (:firstname (:selecteduser @shelters/app-state))} {:key "lastName" :value (:lastname (:selecteduser @shelters/app-state))} {:key "email" :value (:email (:selecteduser @shelters/app-state))} {:key "addedby" :value (if (= (count (:addedby (:selecteduser @shelters/app-state))) 0) (:userid (:token @shelters/app-state)) (:addedby (:selecteduser @shelters/app-state)))} {:key "islocked" :value (if (:islocked (:selecteduser @shelters/app-state)) 1 0)} {:key "iscommand" :value (if (:iscommand (:selecteduser @shelters/app-state)) 1 0)}] }})
  )
)

(defn checkelement [user]
  ;(set! (.-checked (js/document.getElementById (str "checksel" (:id unit)))) true)
  (.click (js/document.getElementById (str "checksel" (:userid user))))
)

(defn selectallusers []
  (doall (map (fn [x] (checkelement x)) (:users @shelters/app-state)))
)


(defn openDialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#groupsModal")
          (.modal)
        )
      )
    )
    (groupstouser/setcheckboxtoggle)
  )
)

(defn onAssignGroups [id]
  (let [
      user (first (filter (fn [x] (if (= (:userid x) id) true false)) (:users @shelters/app-state)))

      tr1 (.log js/console (str user))
    ]
    (swap! shelters/app-state assoc-in [:selecteduser] user)
    (swap! shelters/app-state assoc-in [:selecteduser :current] (str "שייך לקבוצה " (:login user)))
    (put! ch 47)
  )
)


(defcomponent addModal [data owner]
  (render [_]
    (dom/div
      (dom/div {:id "groupsModal" :className "modal fade" :role "dialog"}
        (dom/div {:className "modal-dialog"} 
          ;;Modal content
          (dom/div {:className "modal-content"} 
            (dom/div {:className "modal-header"} 
              (b/button {:type "button" :className "close" :data-dismiss "modal"})
              (dom/h4 {:className "modal-title"} (:modalTitle @app-state) )
            )
            (dom/div {:className "modal-body"}

              (dom/div {:className "panel panel-primary"}

                (dom/h1 {:style {:text-align "center"}} (:current (:selecteddevice @data)))
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                  (dom/div {:className "row"}
                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
                    )
                    (dom/div {:className "col-xs-4 col-md-4" :style {:text-align "center" :border-left "1px solid"}}
                      (dom/h5 "Name")
                    )
                    (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "1px solid"}}
                      (dom/h5 "Selection")
                    )
                    (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :border-left "0px solid"}}
                      (dom/h5 "Selected")
                    )
                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" }}
                    )
                  )
                )

                (om/build groupstouser/showgroups-view data {})
              )
                     ;(om/build groupstounit/showgroups-view dev-state {})
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Close")
                )

                (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal" :onClick (fn [e] (savegroups))} "Save")
                )
              )
            )
          )
        )
      )
    )
  )

)


(defcomponent topbuttons-view [data owner]
  (render [_]
    (dom/div

      (dom/div {:className "row" :style {:padding-top "60px" :border-bottom "solid 1px" :border-color "#e7e7e7"}}
        (dom/div {:className "col-xs-10" :style { :text-align "right" }}
          (dom/h3 (:current @data))
        )

        (dom/div {:className "col-xs-2" :style {:padding-top "15px" :text-align "left"}}
          (b/button {:className "btn btn-primary" :style { :padding-left "5px" :padding-right "5px"} :onClick (fn [e] (-> js/document .-location (set! "#/userdetail")))} "הוסף משתמש חדש"
          )
        )
      )

      (dom/div {:className "row" :style {:margin-right "0px"}}
        (dom/input {:id "search" :type "text" :placeholder "חיפוש" :style {:height "24px" :margin-top "12px"} :value  (:search @shelters/app-state) :onChange (fn [e] (handleChange e )) })
      )
    )
  )
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

(defn handle-chkbsend-change [e]
  (let [
        id (str/join (drop 8 (.. e -currentTarget -id)))
        
        users (:selectedusers @shelters/app-state)


        ;tr2 (.log js/console (.. e -currentTarget) )

        ;tr1 (.log js/console (str "amount1=" amount1 " client=" client))
        
        delusers (remove (fn [user] (if (= user id) true false  )) users)

        adduser (if (.. e -currentTarget -checked) (into [] (conj delusers id)) delusers) 
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! shelters/app-state assoc-in [:selectedusers] adduser)
  )
)

(defcomponent showusers-view [data owner]
  (render
    [_]
    (dom/div {:style {:border-left "1px solid" :border-right "1px solid"}}
      (map (fn [item]
        (let [
          creator (first (filter (fn [x] (if (= (:id x) (:addedby item)) true false)) (:users @shelters/app-state)))
          ]
          (dom/div {:className "row tablerow" :style {:border-bottom "1px solid" :padding-top "0px" :margin-right "0px" :margin-left "0px"}}
            (dom/div {:className "col-xs-1 col-md-1" :style {:border-left "1px solid"}}
              (dom/div {:className "col-xs-12 col-md-12" :style {:text-align "center" :padding-right "0px" :padding-left "0px" :padding-bottom "8px"}}
                (dom/div { :className "dropdown"}
                  (b/button {:className "btn btn-danger dropdown-toggle" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false" :style {:padding-top "3px" :padding-bottom "3px" :padding-left "6px" :padding-right "6px" :margin-top "6px"}}
                    "☰"
                  )
                  (dom/ul {:className "dropdown-menu" :aria-labelledby "dropdownMenuButton" :style {:min-width "100px"}}
                    ;; (dom/li {:className "dropdown-item" :style {:text-align "center"}}
                    ;;   (dom/div {:style {:padding-left "0px" :padding-right "5px" :font-weight "800"}}
                    ;;     "פעולות"
                    ;;   )
                    ;; )
                    ;; (dom/li {:className "divider"}
                    ;; )
                    (dom/li {:className "dropdown-item"}
                      (dom/a {:href (str "#/userdetail/" (:userid item)) :onClick (fn [e] (goUser (:userid item))) :style {:padding-left "0px" :padding-right "5px"}}
                        "עדכון נתונים"
                      )
                    )
                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href (str "#/userdetail/" (:userid item)) :style {:padding-left "0px" :padding-right "5px"}}
                        "ביטול משתמש"
                      )
                    )

                    (dom/li {:className "dropdown-item" :href "#"}
                      (dom/a {:href "#" :onClick (fn [e] (onAssignGroups (:userid item))) :style {:padding-left "0px" :padding-right "5px"}}
                        "שיוך לקבוצה"
                      )
                    )
                  )
                )
              )
            )
          (dom/div {:className "col-xs-2" :style {:border-left "1px solid" :padding-top "11px" :padding-bottom "11px"}}
            (dom/a {:href (str "#/userdetail/" (:userid item))}
              ;(dom/i {:className "fa fa-hdd-o"})
              (:login item)
            )
          )

          (dom/div {:className "col-xs-2" :style {:border-left "1px solid" :padding-top "11px" :padding-bottom "11px"}}
            (dom/a {:href (str "#/userdetail/" (:userid item))}
              ;(dom/i {:className "fa fa-hdd-o"})
              (str (:firstname item) " " (:lastname item))
            )
          )

          (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-top "11px" :padding-bottom "11px"}}
            (dom/a {:href (str "#/userdetail/" (:userid item))}
              ;(dom/i {:className "fa fa-hdd-o"})
              (if (nil? (:name (:role item))) "אדמיניסטרטור" (:name (:role item)))
            )
          )

          (dom/div {:className "col-xs-3" :style { :border-left "1px solid" :padding-top "11px" :padding-bottom "11px"}}
            (dom/a {:href (str "#/userdetail/" (:userid item))}
              ;(dom/i {:className "fa fa-hdd-o"})
              (if (nil? creator) "Beeper" (str (:firstname creator) " " (:lastname creator)))
            )
          )

          (dom/div {:className "col-xs-2" :style { :padding-top "11px" :padding-bottom "11px"}}
            (dom/a {:href (str "#/userdetail/" (:userid item))}
              ;(dom/i {:className "fa fa-hdd-o"})
              (tf/unparse shelters/custom-formatter2 (tc/now))
            )
          )
        )
        )
        )(sort (comp comp-users) (filter (fn [x] (if (or (str/includes? (str/lower-case (:firstname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:lastname x)) (str/lower-case (:search @data))) (str/includes? (str/lower-case (:login x)) (str/lower-case (:search @data)))) true false)) (:users @data)))
      )
    )
  )
)


(defn setcontrols [value]
  (case value
    ;46 (setGroup)
    47 (go
         (<! (timeout 100))
         (openDialog)
       )
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
  ; (getUsers data)
  (swap! shelters/app-state assoc-in [:current] "ניהול משתמשים")
  (swap! shelters/app-state assoc-in [:view] 3)
  (set! (.-title js/document) "משתמשים והרשאות")
)



(defcomponent users-view [data owner]
  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
            ;styleprimary {:style {:margin-top "70px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})


        (dom/div {:className "container" :style {:margin-top "0px" :width "100%" :padding-left "50px" :padding-right "50px" :height "100%"}}
          (dom/div { :style {:padding-bottom "90px"}}
            (om/build topbuttons-view data {})


            (dom/div {:className "panel-primary" :style {:padding "0px" :margin-top "10px"}}
              (dom/div {:className "panel-heading" :style {:padding-top "3px" :padding-bottom "0px"}}
                (dom/div {:className "row" :style {:margin-left "-14px" :margin-right "-14px"}}
                  (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding "0px"}}
                    (dom/div {:className "col-xs-12 col-md-12" :style {:text-align "center" :padding-top "5px" :padding-bottom "5px" :padding-left "0px" :padding-right "0px"}}
                      (dom/div "פעולות")
                    )
                  )


                  (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-12" :style {:padding-left "0px" :padding-right "3px" :padding-top "7px" :padding-bottom "3px" :text-align "center" :background-image (case (:sort-list @app-state) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left"}}
                        (dom/span {:onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 1 2 1)) (shelters/doswaps)))} "קוד משתמש")
                      )
                    )
                  )

                  (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "3px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-12" :style {:padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @app-state) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left"}}
                        (dom/span {:onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 3 4 3)) (shelters/doswaps)))} "שם מלא")
                      )
                    )
                  )

                  (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "3px" :white-space "nowrap" :border-left "1px solid"}}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-xs-12" :style {:padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @app-state) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left"}}
                        (dom/span {:onClick (fn [e] ((swap! app-state assoc-in [:sort-list] (case (:sort-list @app-state) 5 6 5)) (shelters/doswaps)))} "רמת הרשאה")
                      )
                    )
                  )

                  (dom/div {:className "col-xs-3" :style {:padding-left "0px" :padding-right "3px" :padding-top "5px" :padding-bottom "5px" :border-left "1px solid" :text-align "center"}}
                    "יוצר"
                  )

                  (dom/div {:className "col-xs-2" :style {:padding-left "0px" :padding-right "3px" :padding-top "5px" :padding-bottom "5px" :text-align "center"}}
                    "נוצר בתאריך"
                  )
                )              
              )
            )
            (om/build showusers-view data {})
            (om/build addModal data {})
          )
        )
      )
    )
  )
)




(sec/defroute users-page "/users" []
  (om/root users-view
           shelters/app-state
           {:target (. js/document (getElementById "app"))}))


