(ns shelters.map (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST]]
            [shelters.settings :as settings]
            [om-bootstrap.button :as b]

	    [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout]]

            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]

            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:map nil :markers [] :selectedunits []}))

(def jquery (js* "$"))

(def custom-formatter (tf/formatter "dd/MM/yyyy"))

(def custom-formatter2 (tf/formatter "MM/dd/yyyy hh:mm:ss"))

(def custom-formatter1 (tf/formatter "MMM dd yyyy hh:mm:ss"))

(def ch (chan (dropping-buffer 2)))

(def iconBase "/images/")

(defn map-dev-node [dev]
  {:text (:name dev) :unitid (:id dev) :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false} }
)


(defn buildUnits [id]
  (let [
    devices (if (> (count (:devices @shelters/app-state)) 0) (filter (fn [x] (if (and (not (nil? (:groups x))) (> (.indexOf (:groups x) id) -1))  true false)) (:devices @shelters/app-state)) []) 
    nodes (into [] (map map-dev-node devices))
    ;tr1 (.log js/console nodes)
    ]
    nodes
  )
)


(defn getChildUnits [id children]
  (let [
    childgroups (filter (fn [x] (if (and (nil? id) (nil? (:parents x))) true (if (nil? (:parents x)) false (if (> (.indexOf (:parents x) id) -1)  true false)))) (:groups @shelters/app-state))
    ;(filter (fn [x] (if (> (.indexOf (:parents x) id) -1) true false)) (:groups @shelters/app-state))

    
    childs (concat children (buildUnits id))
        
    childdevs (
      loop [result [] groups childgroups]
        (if (seq groups)
          (let [
            thegroup (first groups)
            tr1 (.log js/console (str "Current group: " (:name thegroup)))
            ]
            (recur (conj result (getChildUnits (:id thegroup) [])) (rest groups))
          )
          result
        )
    )
    ;; childdevs (map (fn [x] (first (filter (fn [y] (if (= (:id y) (:text x)) true false)) (:devices @shelters/app-state)))) childs)

    ;; nextchildunits (distinct (flatten (map (fn [x] (buildUnits (:id x))) childgroups)))

    ;; nextchilddevs (map (fn [x] (first (filter (fn [y] (if (= (:id y) (:text x)) true false)) (:devices @shelters/app-state)))) nextchildunits)
    ]

    (distinct (flatten (concat childs childdevs)))
    ;childgroups
    ;(if (> (count childgroups) 0) (concat ))
  )
)

(defn calcGroupLatLon [id]
  (let [
    ;tr1 (.log js/console (str "id in calcGroupLatLon=" id))
    units (map (fn [x] (first (filter (fn [y] (if (= (:id y) (:unitid x)) true false)) (:devices @shelters/app-state)))) (getChildUnits id [])) 
 
    minlat (if (= (count units) 0) (:lat (:selectedcenter @shelters/app-state)) (apply min (map (fn [x] (:lat x)) units))) 
    maxlat (if (= (count units) 0) (:lat (:selectedcenter @shelters/app-state)) (apply max (map (fn [x] (:lat x)) units)))

    ;tr1 (.log js/console (str "first unit=" (first units)))
    lat (/ (+ minlat maxlat) 2)

    minlon (if (= (count units) 0) (:lon (:selectedcenter @shelters/app-state)) (apply min (map (fn [x] (:lon x)) units)))
    maxlon (if (= (count units) 0) (:lon (:selectedcenter @shelters/app-state)) (apply max (map (fn [x] (:lon x)) units)))


    lon (/ (+ minlon maxlon) 2)
    ]
    {:lat lat :lon lon}
  )
)

;; (defn map-city-node [city]
;;   {:text (:id city)  :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded false :selected false} :nodes (into [] (concat (buildCities (:id city)) (buildUnits (:id city)))) }
;; )

(defn buildCities [id]
  (let [
    children (filter (fn [x] (if (and (nil? id) (nil? (:parents x))) true (if (nil? (:parents x)) false (if (> (.indexOf (:parents x) id) -1)  true false)))) (:groups @shelters/app-state))
    nodes (into [] (map (fn [x] (
      let [
        childs (into [] (concat (buildCities (:id x)) (buildUnits (:id x))))
        ]
        (if (> (count childs) 0) {:text (:name x) :groupid (:id x) :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes childs} {:text (:name x) :groupid (:id x) :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false}})
      ) ) children))
    ;tr1 (.log js/console nodes)
    ]
    nodes
  )
)

(defn buildTreeGroups []
  (swap! app-state assoc-in [:selectedunits] [])
  (do (clj->js {:multiSelect true :data [{:text "All cities" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes (into [] (concat (buildCities nil) (buildUnits nil)))}]}))
)


;(def js-object (clj->js {:multiSelect true :data [{:text "All cities" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes (into [] (concat (buildCities "00000000-0000-9999-0000-000000000000") (buildUnits "00000000-0000-9999-0000-000000000000")))}]}))



;(def js-object  (do (clj->js  {:multiSelect true :data [ {:text "All cities" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes [{:text "Tel Aviv" :selectedIcon "glyphicon glyphicon-stop" :selectable true :nodes (buildNodes 1) } {:text "Ness Ziona" :selectedIcon "glyphicon glyphicon-stop" :selectable true :nodes (buildNodes 2) } {:text "Jerusalem" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes (buildNodes 3)} ]}]} )))


(defn OnGetPortfolios [response]
  ;;
  ;;(sbercore/setSecsDropDown)
  ;;(.log js/console (:client @app-state)) 
)


(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)




(defn getPortfolios [] 

;;  (if (> (count (:porfolios ((keyword (:selectedsec @sbercore/app-state)) @sbercore/app-state)) 0))
;;    (sbercore/setSecsDropDown)
;;    (GET (str settings/apipath "api/portfolios?security=" (:selectedsec @sbercore/app-state) ) {
;;      :handler OnGetPortfolios
;;      :error-handler error-handler
;;      :headers {
;;        :content-type "application/json"
;;        :Authorization (str "Bearer "  (:token (:token @sbercore/app-state))) }
;;    })
;;  )
)


(defn comp-portfs
  [portf1 portf2]
  (if (or (> (:amount portf1)  (:amount portf2))  (and (<= (:amount portf1)  (:amount portf2)) (> (compare (:name portf1)  (:name portf2)  ) 0) ) ) 
      true
      false
  )
)



(defn onMount [data]
  ;;(getPortfolios)
  (put! ch 42)
  (put! ch 43)
  ;;(swap! sbercore/app-state assoc-in [:current] {:name "Portfolios" :text "Portfolios with this security: "} )

  ;;(swap! sbercore/app-state assoc-in [:view] 2)
  ;;(swap! sbercore/app-state assoc-in [:search] "")
)


(defn addMarker [device]
  (let [
    ;tr1 (.log js/console (str "tran: " tran ))

    wnd1  (str "<div id=\"content\">"
      "<div id=\"siteNotice\">"
      "</div>"
      "<h1 id=\"firstHeading\" class=\"firstHeading\">"
      (:name device)
      "</h1>"
      "<div id=\"bodyContent\">"
      "<p><b>Controller: </b>" (:name device) "</p>"
      "<p>" (:address device) ", <a href=\"/#/devdetail/" (:id device) "\">"
      "Go to device</a>"
      "</p>"
      "</div>"
      "</div>")

    window-options (clj->js {"content" wnd1})
    infownd (js/google.maps.InfoWindow. window-options)
    ;tr1 (.log js/console (str  "Lan=" (:lon device) " Lat=" (:lat device)))
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat device), (:lon device)) "icon" (str iconBase (case (:status device) 3 "red_point.png" "green_point.png")) "map" (:map @app-state) "title" (:name device) "unitid" (:id device)})
    marker (js/google.maps.Marker. marker-options)
    ]
    (jquery
      (fn []
        (-> marker
          (.addListener "click"
            (fn []              
              (.open infownd (:map @app-state) marker)
            )
          )
        )
      )
    )
    (swap! app-state assoc-in [:markers] (conj (:markers @app-state) marker))
  )
)

(defn addplace [place]
  (let [
    ;tr1 (.log js/console place)
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @app-state) "icon" (str iconBase "green_point.png") "title" (.. place -name)})

    ;If need to add marker on the map:
    ;marker (js/google.maps.Marker. marker-options)
    ]
    (.panTo (:map @app-state) (.. place -geometry -location))
  )
)

(defn addsearchbox []
  (let [
    ;;Create the search box and link it to the UI element.
    input (. js/document (getElementById "pac-input"))
    searchbox (js/google.maps.places.SearchBox. input)
    ;tr1 (.log js/console input)
    ]
    (.push (aget (.-controls (:map @app-state)) 1) input)

    (jquery
      (fn []
        (-> searchbox
          (.addListener "places_changed"
            (fn []              
              ;(.log js/console (.getPlaces searchbox))
              (doall (map (fn [x] (addplace x)) (.getPlaces searchbox)))
            )
          )
        )
      )
    )
  )
)


(defn addMarkers []
  (let[
       tr1 (swap! app-state assoc-in [:markers] [])
    ]
    (doall (map addMarker (:devices @shelters/app-state)))
    (go
         (<! (timeout 100))
         (addsearchbox)
    )
  )
)

(defn setcenterbycity [city]
  (let [
    thecity (first (filter (fn [x] (if (= (:id x) city) true false)) (:groups @shelters/app-state)))

    latlon (calcGroupLatLon (:id thecity)) ;{:lat 32.08088 :lon 34.78057}
    tr1 (.log js/console (str "city=" city " obj=" thecity " latlon=" latlon))
    tr1 (swap! shelters/app-state assoc-in [:selectedcenter] {:lat (:lat latlon) :lon (:lon latlon) }  )
    ]
    (.panTo (:map @app-state) (google.maps.LatLng. (:lat latlon), (:lon latlon)))
  )
)

(defn setcenterbydevice [device]
  (let [
    thedev (first (filter (fn [x] (if (= (:id x) device) true false)) (:devices @shelters/app-state)))

    ;tr1 (.log js/console (str "device=" device " obj=" thedev))
    tr1 (swap! shelters/app-state assoc-in [:selectedcenter] {:lat (:lat thedev) :lon (:lon thedev) }  )
    ]
    (.panTo (:map @app-state) (google.maps.LatLng. (:lat thedev), (:lon thedev)))
  )
)

(defn setTreeControl []
  ;(.log js/console "Set Tree called")
  ;(.log js/console (count (:employees @app-state)))
  (jquery
    (fn []
      (-> (jquery "#tree" )
        (.treeview (buildTreeGroups) ) ;;js-object
        (.on "nodeSelected"
          (fn [event data] (
             let [
               ;table (-> (jquery "#dataTables-example") (.DataTable) )
               ;res (.data (.row table (.. e -currentTarget)) )
               res (js->clj data)
               unitid (get res "unitid")
             ]
             (.log js/console (str "unitid=" unitid))
             ;(.log js/console (str "parentid=" (get res "parentId") " text=" (get res "text")))
             (if (nil? unitid) (setcenterbycity (get res "groupid")) (setcenterbydevice unitid))
             (if (= (.indexOf (:selectedunits @app-state) unitid) -1) 
               (swap! app-state assoc-in [:selectedunits] (conj (:selectedunits @app-state) unitid))
             )
             ;(gotoSelection (first res)) 
            )
          )
        )


        (.on "nodeUnselected"
          (fn [event data] (
             let [
               ;table (-> (jquery "#dataTables-example") (.DataTable) )
               ;res (.data (.row table (.. e -currentTarget)) )
               res (js->clj data)
               unitid (get res "unitid")
             ]
             (.log js/console (str "unitid=" unitid))
             ;(.log js/console (str "parentid=" (get res "parentId") " text=" (get res "text")))
             ;(if (nil? unitid) (setcenterbycity (get res "groupid")) (setcenterbydevice unitid))
             (if (> (.indexOf (:selectedunits @app-state) unitid) -1) 
               (swap! app-state assoc-in [:selectedunits] (remove (fn [x] (if (= unitid x) true false)) (:selectedunits @app-state)))
             )
             ;(gotoSelection (first res))
            )
          )
        )
      )      
    )
  )
)

(defn setcontrols [value]
  (case value
    42 (setTreeControl)
    43 (addMarkers)
  )
  
)


(defn initqueue []
  (doseq [n (range 1000)]
    (go ;(while true)
      (take! ch(
        fn [v] (
           setcontrols v
          )
        )
      )
    )
  )
)

(initqueue)

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
        :params {:commandId (js/parseInt (:id (first (:commands @shelters/app-state)))) :units (:selectedunits @app-state)}
    }
  )
)


(defcomponent map-view [data owner]

  (did-mount [_]
    (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:selectedcenter @data)) :lng (:lon (:selectedcenter @data))} "zoom" 12})
      map (js/google.maps.Map. map-canvas map-options)

      
      tr1 (swap! app-state assoc-in [:map] map  )
      ]
    )
  )

  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let []
      (dom/div
        (om/build shelters/website-view data {})


        (dom/div {:className "row maprow" :style {:height (case (or (:isalert @data) (:isnotification @data)) true "80%" "100%")}}
          (dom/div  {:className "col-3 col-sm-3"}
            (b/button {:className "btn btn-primary" :onClick (fn [e] (sendcommand1)) :style {:margin-bottom "5px"}} (:name (first (:commands @data))))
            (dom/div  {:className "tree" :id "tree"})
          )
          
          (dom/input {:id "pac-input" :className "controls" :type "text" :placeholder "Search Box" })
          (dom/div  {:className "col-9 col-sm-9" :id "map" :style {:margin-top "0px"}})
        )


        (if (:isalert @data)
          (dom/div {:className "row" :style {:padding-top "10px" :height (str (+ 55 (* 35 (count (:alerts @data)))) "px")}}
            (dom/div  {:className "col-3 col-sm-3 tree"})
            (dom/div {:className "col-9 col-sm-9"}
              (dom/div {:className "panel panel-primary" :style {:padding "0px" :margin-top "10px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin "0px"}}
                  (dom/div {:className "row"}

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}} "seen")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "device")


                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "name")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "address")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "event")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "open")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "status")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "user")

                  )

                )
              )
              (om/build shelters/alerts-table data {})
            )
          )
        )

        (if (:isnotification @data)
          (dom/div {:className "row" :style {:height (str (+ 55 (* 35 (count (:notifications @data)))) "px") :padding-top "10px"}}
            (dom/div  {:className "col-3 col-sm-3 tree"})
            (dom/div {:className "col-9 col-sm-9"}
              (dom/div {:className "panel panel-primary"}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                  (dom/div {:className "row"}

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}} "seen")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "device")


                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "name")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "address")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "event")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "open")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"}}  "status")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid"}}  "user")

                  )
                )
              )
              (om/build shelters/notifications-table data {})
            )
          )          
        )
      ) 
    )
  )
)




(sec/defroute map-page "/map" []
  (let []
    (swap! shelters/app-state assoc-in [:view] 2)
    (om/root map-view
          shelters/app-state
          {:target (. js/document (getElementById "app"))})
  )
)



;; (go
;;   (let [{:keys [ws-channel error]} (<! (ws-ch "ws://52.14.180.219:5060"))]
;;     (if-not error
;;       (>! ws-channel "Hello server from client!")
;;       (js/console.log "Error:" (pr-str error)))))

(defn updateMarkerIcon [marker icon]
  (let []
  )
)

(defn updateUnit [unit id newunit]
  (if (= (:id unit) id)
    (assoc unit :status (:status newunit))
    unit
  )
)

(defn processMessage [notification]
  (let [
    unitid (get notification "unitId")
    userid (get notification "userId")
    status (get notification "status")
    type (get notification "notificationType")
    id (get notification "notificationId")
    ;tr1 (.log js/console (subs (get notification "openTime") 0 19))
    open (tf/parse custom-formatter2 (subs (get notification "openTime") 0 19))
    
    open (if (= (subs (get notification "openTime") 20) "PM") (tc/from-long (+ (tc/to-long open) (* 1000 12 3600))) open)
    ;tr1 (.log js/console (str "unitid in Notification: " unitid))
    marker (first (filter (fn [x] (if (= (.. x -unitid) unitid) true false)) (:markers @app-state)))

    newunits (map (fn [x] (if (= (:id x) unitid) (assoc x :status status) x)) (:devices @shelters/app-state))
    
    tr1 (if (not (nil? marker)) (swap! shelters/app-state assoc-in [:devices] newunits))

    tr1 (case type "Failure" (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:notifications @shelters/app-state)))) (swap! shelters/app-state assoc-in [:notifications] (conj (:notifications @shelters/app-state) {:unitid unitid :userid userid :status status :id id :open open}))) (if (= 0 (count (filter (fn [x] (if (= (:id x) id) true false)) (:alerts @shelters/app-state)))) (swap! shelters/app-state assoc-in [:alerts] (conj (:alerts @shelters/app-state) {:unitid unitid :userid userid :status status :id id :open open}))))
    
    ]
    (if (nil? marker)
      (.log js/console (str "did not find a unit for unitid=" unitid " in notification"))
      (.setIcon marker (str iconBase (case status 3 "red_point.png" "green_point.png")))
    )
  )
)

(defn processNotification [notification]
  (let [
      tr1 (js/console.log "Hooray! Message:" (pr-str notification))
    ]
    (processMessage notification)
  )
)


(defn initsocket []
  (go
    (let [
        {:keys [ws-channel]} (<! (ws-ch "ws://52.14.180.219:5060" {:format :json}))
        {:keys [message error]} (<! ws-channel)
      ]
      (if error
        (js/console.log "Uh oh:" error)
        (processNotification message)      
      )
      (initsocket)
    )
  )
)

(initsocket)
