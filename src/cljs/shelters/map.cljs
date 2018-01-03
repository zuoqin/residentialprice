(ns shelters.map
  (:use [net.unit8.tower :only [t]])
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
            [om.dom :as omdom :include-macros true]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]

            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:markers []}))

(def jquery (js* "$"))

(def custom-formatter (tf/formatter "dd/MM/yyyy"))

(def custom-formatter2 (tf/formatter "MM/dd/yyyy hh:mm:ss"))

(def custom-formatter1 (tf/formatter "MMM dd yyyy hh:mm:ss"))

(def ch (chan (dropping-buffer 2)))

(def iconBase "/images/")

(defn tableheight [count] 
  (+ 100 (* 34 (min count 10)))
)



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
        (if (> (count childs) 0) {:text (:name x) :groupid (:id x) :icon "glyphicon glyphicon-user" :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes childs} {:text (:name x) :groupid (:id x) :icon "glyphicon glyphicon-user" :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false}})
      ) ) children))
    ;tr1 (.log js/console nodes)
    ]
    nodes
  )
)

(defn buildTreeGroups []
  (swap! shelters/app-state assoc-in [:selectedunits] [])
  (do (clj->js {:multiSelect true :searchResultBackColor "#0000FF" :searchResultColor "#FFFFFF" :data [{:text "כל ישראל" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes (into [] (concat (buildCities nil) (buildUnits nil)))}]}))
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




(defn onMount [data]
  ;;(getPortfolios)
  (put! ch 42)
  (put! ch 43)

  (set! (.-title js/document) "מפה")
  (swap! shelters/app-state assoc :state 1)
  (swap! shelters/app-state assoc-in [:view] 2)
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
      "<p>" (:address device) ", <a href=\"#/devdetail/" (:id device) "\">"
      "Go to device</a>"
      "</p>"
      "</div>"
      "</div>")

    window-options (clj->js {"content" wnd1})
    infownd (js/google.maps.InfoWindow. window-options)
    ;tr1 (.log js/console (str  "Lan=" (:lon device) " Lat=" (:lat device)))
    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase (case (:status device) 3 "red_point.ico" "green_point.png")) :scaledSize size})
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat device), (:lon device)) "icon" image "map" (:map @shelters/app-state) "title" (:name device) "unitid" (:id device)})
    marker (js/google.maps.Marker. marker-options)
    ]
    (jquery
      (fn []
        (-> marker
          (.addListener "click"
            (fn []              
              (.open infownd (:map @shelters/app-state) marker)
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
    tr1 (.log js/console place)
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @shelters/app-state) "icon" (str iconBase "green_point.png") "title" (.. place -name)})

    ;If need to add marker on the map:
    ;marker (js/google.maps.Marker. marker-options)
    ]
    (.panTo (:map @shelters/app-state) (.. place -geometry -location))
  )
)

(defn setcenterbydevice [device]
  (let [
    thedev (first (filter (fn [x] (if (= (:id x) device) true false)) (:devices @shelters/app-state)))

    ;tr1 (.log js/console (str "device=" device " obj=" thedev))
    tr1 (swap! shelters/app-state assoc-in [:selectedcenter] {:lat (:lat thedev) :lon (:lon thedev) }  )
    ]
    (.panTo (:map @shelters/app-state) (google.maps.LatLng. (:lat thedev), (:lon thedev)))
  )
)

(defn uncheckall []
  (let [
    selected (js->clj (-> (jquery "#tree" ) (.treeview "getSelected")))
    ]
    (doall (map (fn [x]
      (let [
        nodeid (get x "nodeId")
        options (clj->js [nodeid {:silent true}])
        tr1 (.log js/console (str "nodeId=" nodeid))
        ]
        (-> (jquery "#tree" )
          (.treeview "unselectNode" options)
        )
      )
    ) selected))
  )
)

(defn locatedevice [device]
  (let [
    options (clj->js [(:name device) {:ignoreCase true :exactMatch false :revealResults true}])
    ]
    (setcenterbydevice (:id device))
    (-> (jquery "#tree" )
      (.treeview "clearSearch")
    )
    (-> (jquery "#tree" )
        (.treeview "search" options)
    )
  )
)

(defn addsearchbox []
  (let [
    ;;Create the search box and link it to the UI element.
    input (. js/document (getElementById "pac-input"))
    searchbox (js/google.maps.places.SearchBox. input)
    ;tr1 (.log js/console input)
    ]
    (.push (aget (.-controls (:map @shelters/app-state)) 1) input)
    (.addDomListener js/google.maps.event input "change"
      (fn []
        (let [
          tr1 (.log js/console "onchange value:" (.-value input))
          ]
        )
      )
    )
    (jquery
      (fn []
        (-> searchbox
          (.addListener "places_changed"
            (fn []              
              ;(.log js/console (.getPlaces searchbox))
              (if (> (count (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (.-value input))) true false)) (:devices @shelters/app-state))) 0) 
                (locatedevice (first (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (.-value input))) true false)) (:devices @shelters/app-state))))
                (doall (map (fn [x] (addplace x)) (.getPlaces searchbox)))
              )
              
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
    (.panTo (:map @shelters/app-state) (google.maps.LatLng. (:lat latlon), (:lon latlon)))
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
             (if (= (.indexOf (:selectedunits @shelters/app-state) unitid) -1)
               (swap! shelters/app-state assoc-in [:selectedunits] (conj (:selectedunits @shelters/app-state) unitid))
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
             (if (> (.indexOf (:selectedunits @shelters/app-state) unitid) -1) 
               (swap! shelters/app-state assoc-in [:selectedunits] (remove (fn [x] (if (= unitid x) true false)) (:selectedunits @shelters/app-state)))
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
        :params {:commandId (js/parseInt (:id (first (:commands @shelters/app-state)))) :units (:selectedunits @shelters/app-state)}
    }
  )
)

(defn handle-change [e owner]
  
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  ) 
)


(defn buildStatusesList [data owner]
  (map
    (fn [text]
      (let [
        ;tr1 (.log js/console (str  "name=" (:name text) ))
        ]
        (dom/option {:key (:id text) :data-width "100px" :value (:id text) :onChange #(handle-change % owner)} (:name text))
      )
    )
    [{:id 1 :name "הכל"} {:id 2 :name  "בטיפול"} {:id 3 :name "פתוח"} {:id 4 :name  "סגור"} ] 
  )
)


(defcomponent map-view [data owner]

  (did-mount [_]
    (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:selectedcenter @data)) :lng (:lon (:selectedcenter @data))} "zoom" 12})
      map (js/google.maps.Map. map-canvas map-options)

      
      tr1 (swap! shelters/app-state assoc-in [:map] map  )
      ]
    )
  )

  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [
      ;tr1 (.log js/console (str (- (.. js/document -body -clientHeight) tableheight 0) "px"))
      ]
      (dom/div {:style { :padding-right "15px"}}
        (om/build shelters/website-view data {})
        (dom/div {:className "row maprow" :style {:max-width "100%" :height (case (or (:isalert @data) (:isnotification @data)) true (str (+ 0 (- (.. js/document -body -clientHeight) (tableheight (if (:isalert @data) (count (:alerts @data)) (count (:notifications @data)))) 0)) "px") "100%")}}
          (dom/div  {:className "col-3 col-sm-3" :style {:height "100%" :padding-left "5px"}}
            (dom/div {:className "panel-default" :style {:border "1px solid darkgrey"}}
              (dom/div {:className "panel-heading" :style {:padding-top "3px" :padding-bottom "3px"}} "בחר קבוצה/יחידה")
              (dom/div {:className "panel-body" :style {:margin-top "0px" :padding-bottom "5px" :margin-left "0px" :margin-right "0px"}}
                (dom/div  {:className "tree" :id "tree" :style { :overflow-y "scroll" :height (case (or (:isalert @data) (:isnotification @data)) true (str (+ (- (.. js/document -body -clientHeight) (tableheight (if (:isalert @data) (count (:alerts @data)) (count (:notifications @data)))) 175) 0 ) "px") (str (+ (- (.. js/document -body -clientHeight) 175) 0) "px")) }})
                (dom/div {:className "row" :style{:margin-top "10px" :margin-left "15px" :margin-right "-5px"}}
                  (dom/div {:className "col-xs-6" :style {:padding-left "5px" :padding-right "5px"}}
                    (b/button {:className "btn btn-primary" :onClick (fn [e] (sendcommand1)) :style {:margin-bottom "5px" :width "100%"}} (t :he shelters/main-tconfig (keyword (str "commands/" (:name (first (:commands @data)))))))
                  )
                  (dom/div {:className "col-xs-6" :style {:padding-right "5px" :padding-left "5px"}}
                    (b/button {:className "btn btn-primary" :onClick (fn [e] (uncheckall)) :style {:margin-bottom "5px" :width "100%"}} "בטל בחירה")
                  )
                )
              )
            )
            

          )
          
          (dom/input {:id "pac-input" :className "controls" :type "text" :placeholder "תיבת חיפוש" })
          (dom/div  {:className "col-9 col-sm-9" :id "map" :style {:margin-top "0px"}})
        )




        (if (:isalert @data)
          (dom/div {:className "row" :style {:padding-top "0px" :bottom "0px" :width "100%"}}
            ;(dom/div  {:className "col-3 col-sm-3 tree"})
            (dom/div {:className "col-12 col-sm-12" :style {:padding-top "5px" :padding-bottom "30px" :padding-left "15px"}}
              (dom/div {:className "panel panel-primary" :style {:padding "0px" :margin-top "10px" :margin-bottom "0px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "0px"}}
                  (dom/div {:className "row" :style {:margin-left "17px" :margin-right "0px"}}

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}} "ראיתי")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "7px"}}  "מספר אירוע")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "מזהה יחידה")


                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "שם יחידה")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "מיקום יחידה")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "שם אירוע")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "תאריך וזמן אירוע")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding "0px"}}
                      (omdom/select #js {:id "statuses"
                                         :className "selectpicker"
                                         :data-width "100px"
                                         :data-style "btn-primary"
                                         :data-show-subtext "false"
                                         :data-live-search "true"
                                         :onChange #(handle-change % owner)
                                         }                
                        (buildStatusesList data owner)
                      )
                    )

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "אירוע טופל ע''י")

                  )
                )
              )
              (om/build shelters/alerts-table data {})
            )
          )
        )

        (if (:isnotification @data)
          (dom/div {:className "row" :style { :padding-top "0px" :bottom "0px" :width "100%"}}
            ;(dom/div  {:className "col-3 col-sm-3 tree"})
            (dom/div {:className "col-12 col-sm-12" :style {:padding-top "5px"  :padding-bottom "10px" :padding-left "15px"}}
              (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                "טבלת התראות"
              )
              (dom/div {:className "panel panel-primary" :style {:padding "0px" :margin-top "10px" :margin-bottom "0px" :margin-left "17px"}}
                (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "0px"}}
                  (dom/div {:className "row" :style {:margin-left "-1px" :margin-right "-1px"}}

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}} "ראיתי")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid"  :padding-left "0px" :padding-right "0px" :padding-top "7px" :padding-bottom "7px"}}  "מספר אירוע")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "מזהה יחידה")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "שם יחידה")

                    (dom/div {:className "col-xs-2 col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "מיקום יחידה")

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px"}}  "שם אירוע")

                    (dom/div {:className "col-xs-3 col-md-3" :style {:text-align "center" :padding-top "0px" :padding-bottom "0px" :padding-left "0px" :padding-right "0px"}}
                      (dom/div {:className "row"}
                        (dom/div {:className "col-xs-6" :style { :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                          "תאריך פתיחה"
                        )

                        (dom/div {:className "col-xs-6" :style { :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                          "זמן סגירה"
                        )
                      )
                    )

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding "0px"}}
                      (omdom/select #js {:id "statuses"
                                         :className "selectpicker"
                                         :data-width "100px"
                                         :data-style "btn-primary"
                                         :data-show-subtext "false"
                                         :data-live-search "true"
                                         :onChange #(handle-change % owner)
                                         }                
                        (buildStatusesList data owner)
                      )
                    )

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px"}}  "אירוע טופל ע''י")

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

