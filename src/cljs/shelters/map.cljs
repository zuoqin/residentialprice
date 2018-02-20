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

(defonce app-state (atom  {}))

(def jquery (js* "$"))

(def custom-formatter (tf/formatter "dd/MM/yyyy"))

;(def custom-formatter2 (tf/formatter "MM/dd/yyyy hh:mm:ss"))

(def custom-formatter1 (tf/formatter "MMM dd yyyy hh:mm:ss"))

(def ch (chan (dropping-buffer 2)))



(defn tableheight [count] 
  (+ 100 (* 27 (min count 10)))
)


;; (defn onDropDownChange [id value]
;;   (let [
;;     ;sattus (first (filter (fn [x] (if (= value (:id x)) true false)) (:roles @shelters/app-state)))
;;     ]
;;     (swap! app-state assoc-in [:selectedstatus] id)
;;   )
;;   ;(.log js/console e)
;; )








;; (defn map-city-node [city]
;;   {:text (:id city)  :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded false :selected false} :nodes (into [] (concat (buildCities (:id city)) (buildUnits (:id city)))) }
;; )


(defn comp-cities [city1 city2]
  (if (< (compare (:text city1) (:text city2)) 0)
    true false
  )
)

(defn buildCities [id]
  (let [
    children (filter (fn [x] (if (and (nil? id) (nil? (:parents x))) true (if (nil? (:parents x)) false (if (> (.indexOf (:parents x) id) -1)  true false)))) (:groups @shelters/app-state))
    nodes (into [] (map (fn [x] (
      let [
        childs (into [] (concat (buildCities (:id x)) (shelters/buildUnits (:id x))))
        ]
        (if (> (count childs) 0) {:text (:name x) :groupid (:id x) :icon "fa fa-users" :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes childs} {:text (:name x) :groupid (:id x) :icon "glyphicon glyphicon-user" :selectedIcon "glyphicon glyphicon-ok" :selectable true :state {:checked false :disabled false :expanded true :selected false}})
      ) ) children))
    ;tr1 (.log js/console nodes)
    ]
    (sort (comp comp-cities) nodes)
  )
)

(defn buildTreeGroups []
  ;(.log js/console (str "groups=" (count (:groups @shelters/app-state)) "; units=" (count (:devices @shelters/app-state))))
  ;(swap! shelters/app-state assoc-in [:selectedunits] [])
  (do (clj->js {:multiSelect true :searchResultBackColor "#337ab7" :searchResultColor "#FFFFFF" :data [{:text "כל ישראל" :icon "fa-flag-israel" :selectedIcon "glyphicon glyphicon-stop" :selectable true :state {:checked false :disabled false :expanded true :selected false} :nodes (into [] (concat (buildCities nil) (shelters/buildUnits nil)))}]}))
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
  ;(.log js/console "setting tree to nil")
  ;(swap! app-state assoc-in [:treeview] nil)
  (set! (.-title js/document) "מפה")
  (swap! shelters/app-state assoc :state 0)
  (swap! shelters/app-state assoc-in [:view] 2)
)



(defn markerinfo [device]
  (dom/h3 {:id (str "mark" (:id device))} "jhgjhjg")
)

(defn addMarker [device]
  (let [
    ;tr1 (.log js/console (str "token: " (:token (:token @shelters/app-state)) ))
    ;tr1 (.log js/console (str "command1= " (:name (nth (:commands @shelters/app-state) 1))))
    wnd1  (str "<div id=\"content\" style=\" width: 200px;  \" >"
      "<h5 style=\"text-align: center\">" (:name device) "</h5>"
      "<h5 style=\"text-align: center; margin-bottom: 0px; margin-top: 5px  \">" (:address device) "</h5>"

      "<div style=\"justify-content: space-evenly; text-align: justify; display: flex; flex-wrap: wrap; width: 100%; margin-top: 0px;\">"
      (shelters/add-marker-indications device)
      "</div>"

      "<div class=\"row\" style=\"text-align: center; margin-top: 5px; margin-bottom: 10px; margin-left: 0px; margin-right: 0px \">"
        "<button type=\"button\" class=\"btn btn-primary\" style=\"margin-left: 10px; padding-left: 0px; padding-right: 0px; width: 86.31px \" onclick=\"sendcommand('"
          settings/apipath "', '"
          (:token (:token @shelters/app-state))
          "', '"
          (:id device)
          "', "
          (:id (nth (:commands @shelters/app-state) 0))
          ")\">"
          (t :he shelters/main-tconfig (keyword (str "commands/" (:name (nth (:commands @shelters/app-state) 0)))))
        "</button>"


        "<button type=\"button\" class=\"btn btn-primary\" style=\"margin-left: 5px; padding-left: 0px; padding-right: 0px; \" onclick=\"sendcommand('"
          settings/apipath "', '"
          (:token (:token @shelters/app-state))
          "', '"
          (:id device)
          "', "
          (:id (nth (:commands @shelters/app-state) 1))
          ")\">"
          (t :he shelters/main-tconfig (keyword (str "commands/" (:name (nth (:commands @shelters/app-state) 1)))))
        "</button>"
      "</div>"
      "</div>")

    window-options (clj->js {"content" wnd1})
    infownd (js/google.maps.InfoWindow. window-options)
    ;tr1 (.log js/console (str  "Lan=" (:lon device) " Lat=" (:lat device)))
    

    status (:isok (first (filter (fn [x] (if (= (:id x) 12) true false)) (:indications device))))



    image (shelters/calcunitimage device)


    ;image (clj->js {:url (str iconBase (case status true "green_point.ico" "red_point.ico")) :scaledSize size})


    marker-options (clj->js {"position" (google.maps.LatLng. (:lat device), (:lon device)) "icon" image "map" (:map @shelters/app-state) "title" (:name device) "unitid" (:id device)})
    marker (js/google.maps.Marker. marker-options)

    infownds (map (fn [x] (if (= (:id x) (:id device)) (assoc x :info infownd) x)) (:infownds @shelters/app-state))

    ;tr1 (.log js/console (str "info counts = " (count (filter (fn[x] (if (= (:id device) (:id x)) true false)) (:infownds @shelters/app-state)))))
    infownds (if (> (count (filter (fn[x] (if (= (:id device) (:id x)) true false)) (:infownds @shelters/app-state))) 0) infownds (conj infownds {:id (:id device) :info infownd}))

    delmarker (remove (fn [x] (if (= (.. x -unitid) (:id device)) true false)) (:markers @shelters/app-state))
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
    (swap! shelters/app-state assoc-in [:markers] (conj (:markers @shelters/app-state) marker))
    

    (swap! shelters/app-state assoc-in [:infownds] infownds)
  )
)

(defn addplace [place]
  (let [
    ;tr1 (.log js/console place)
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @shelters/app-state) "icon" (str shelters/iconBase "green_point.png") "title" (.. place -name)})

    ;If need to add marker on the map:
    ;marker (js/google.maps.Marker. marker-options)
    ]
    (.panTo (:map @shelters/app-state) (.. place -geometry -location))
  )
)


(defn uncheckall []
  (let [
    selected (js->clj (-> (jquery "#tree" ) (.treeview "getSelected")))
    ]
    (swap! shelters/app-state assoc-in [:selectedunits] [])
    (doall (map (fn [x]
      (let [
        nodeid (get x "nodeId")
        options (clj->js [nodeid {:silent true}])
        ;tr1 (.log js/console (str "nodeId=" nodeid))
        ]
        (-> (jquery "#tree" )
          (.treeview "unselectNode" options)
        )
      )
    ) selected))

    (-> (jquery "#tree" )
      (.treeview "clearSearch")
    )
    (put! ch 47)
  )
)

(defn select-units-group [element]
  (let [
    nodes (get (js->clj element) "nodes")
    ]
    ;(.log js/console "selecting group with " (count nodes) " childs")
    (doall (map (fn [x] (let [
      ;tr1 (.log js/console (str "selecting nodeid=" (get x "nodeId")))
      ]
      (-> (jquery "#tree" )
        (.treeview "selectNode" (clj->js [ (get x "nodeId") {:silent true}]))
      )
      (select-units-group x)
     )) nodes))
  )
)

(defn deselect-units-group [element]
  (let [
    nodes (get (js->clj element) "nodes")
    ]
    ;(.log js/console "selecting group with " (count nodes) " childs")
    (doall (map (fn [x] (let [
      ;tr1 (.log js/console (str "selecting nodeid=" (get x "nodeId")))
      unitid (get x "unitid")
      ;tr1 (.log js/console "unitid = " unitid)
      ]

      (if (not (nil? unitid))
        (if (> (.indexOf (:selectedunits @shelters/app-state) unitid) -1)
          (swap! shelters/app-state assoc-in [:selectedunits] (remove (fn [x] (if (= unitid x) true false)) (:selectedunits @shelters/app-state)))
        )
      )
      (-> (jquery "#tree" )
        (.treeview "unselectNode" (clj->js [ (get x "nodeId") {:silent true}]))
      )
      (deselect-units-group x)
     )) nodes))
  )
)


(defn search-units-tree [select]
  (let [
    options (clj->js [select {:ignoreCase true :exactMatch false :revealResults true}])
    ]
    (-> (jquery "#tree" )
      (.treeview "clearSearch")
    )
    (-> (jquery "#tree" )
        (.treeview "search" options)
    )
  )
)

(defn select-units-tree []
  (doall (map (fn [x]
    (let [
      node (-> (jquery "#tree" ) (.treeview "getNode" x))
    ]
    ;(.log js/console (.-unitid node))
    (if (not (nil? (.-unitid node)))
      (if (> (.indexOf (:selectedunits @shelters/app-state) (.-unitid node)) -1)
        (-> (jquery "#tree" ) (.treeview "selectNode" (clj->js [x {:silent true}])))
        (-> (jquery "#tree" ) (.treeview "unselectNode" (clj->js [x {:silent true}])))
      )
    ))
  ) (range 0 (* (count (:groups @shelters/app-state)) (count (:devices @shelters/app-state))))))
)


(defn locatedevice [device]
  (let [
    ]
    (shelters/setcenterbydevice (:id device))
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
                (let [first (first (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (.-value input))) true false)) (:devices @shelters/app-state)))
                  ]
                  (swap! shelters/app-state assoc-in [:selectedunits] (map (fn [x] (:id x)) (filter (fn [x] (if (str/includes? (str/upper-case (:name x)) (str/upper-case (.-value input))) true false)) (:devices @shelters/app-state))) )
                  (locatedevice first)
                  

                  (put! ch 47)
                  ;(.log js/console (str "trying to check " (:name first) " liunit" (:id first)))
                  ;(.checked (:treeview @app-state) true (jquery (str "liunit" (:id first))))
                )
                
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
       
    ]
    (doall (map (fn [x] (.setMap x nil)) (:markers @shelters/app-state)))
    (swap! shelters/app-state assoc-in [:markers] [])
    (doall (map addMarker (:devices @shelters/app-state)))
    (go
         (<! (timeout 100))
         (addsearchbox)
    )
  )
)

(defn addtoselected [unitid]
  ;(.log js/console (str "unitid=" unitid))
  (if (= (.indexOf (:selectedunits @shelters/app-state) unitid) -1)
    (swap! shelters/app-state assoc-in [:selectedunits] (conj (:selectedunits @shelters/app-state) unitid))
  )
)

(defn add-group-to-selected [city]
  (let [
    units (map (fn [x] (:unitid x)) (shelters/getChildUnits city []))
    ]
    (doall (map addtoselected units))
    ;(.log js/console (str "total in group:" (count units)))
  )
)

(defn setcenterbycity [city]
  (let [
    thecity (first (filter (fn [x] (if (= (:id x) city) true false)) (:groups @shelters/app-state)))

    latlon (shelters/calcGroupLatLon (:id thecity))

    
    ;tr1 (.log js/console (str "city=" city " obj=" thecity " latlon=" latlon))
    ]
    (swap! shelters/app-state assoc-in [:selectedcenter] {:lat (:lat latlon) :lon (:lon latlon) })
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
               
               
               res (js->clj data)
               unitid (get res "unitid")
             ]
             ;(.log js/console (str "unitid=" unitid))
             ;(.log js/console data)
             ;(.log js/console (str "parentid=" (get res "parentId") " text=" (get res "text")))
             (if (nil? unitid) 
               (let []
                 (select-units-group data)
                 (setcenterbycity (get res "groupid"))
               )

               (shelters/setcenterbydevice unitid))

             (if (nil? unitid)
               (add-group-to-selected (get res "groupid"))
               (addtoselected unitid)
             )
             ;(gotoSelection (first res)) 
             (put! ch 47)
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
             ;(.log js/console (str "unitid=" unitid))
             ;(.log js/console (str "parentid=" (get res "parentId") " text=" (get res "text")))
             ;(if (nil? unitid) (setcenterbycity (get res "groupid")) (setcenterbydevice unitid))
             
             (if (nil? unitid)
               (deselect-units-group data)
               (if (> (.indexOf (:selectedunits @shelters/app-state) unitid) -1)
                 (swap! shelters/app-state assoc-in [:selectedunits] (remove (fn [x] (if (= unitid x) true false)) (:selectedunits @shelters/app-state)))
               )
             )
             (put! ch 47)
            )
          )
        )
      )      
    )
  )
  (put! ch 47)
)


;; (defn setTreeControl []
;;   (let [
;;     ;; tv (.swidget (-> (jquery "#treeview" )
;;     ;;      (.shieldTreeView (clj->js {:checkboxes {:enabled true :children true}}))
;;     ;;      )
;;     ;;   ) 
;;     ]
;;     ;(swap! app-state assoc-in [:treeview] tv)
;;     (jquery
;;       (fn []
;;         (-> (jquery "#treeview" )
;;           (.shieldTreeView)
;;         )      
;;       )
;;     )
;;   ) 
;; )

(defn updatemarkers []
  (let [

  ]
  (doall (map (fn [num] 
    (let [
      x (nth (:markers @shelters/app-state) num)
      unit (first (filter (fn [y] (if (= (.. x -unitid) (:id y)) true false)) (:devices @shelters/app-state)))
      ;tr1 (.log js/console "unitod = " (:id unit))
      ;size (js/google.maps.Size. 48 48)
      ;indstatus (:isok (first (filter (fn [z] (if (= (:id z) 12) true false)) (:indications unit))))

      
      image (shelters/calcunitimage unit)

      ;tr1 (.log js/console "unit=" (:name unit) "; image=" (get (js->clj image) "url") "; old= " (.-url (.getIcon x)))
      ]
      (if (not= (.-url (.getIcon x)) (get (js->clj image) "url")) 
        (.setIcon x image)
      )
      
      ;(swap! shelters/app-state assoc-in [:markers] newmarkers)
    ))
    (range 0 (count (:markers @shelters/app-state))) ))
  )
)

(defn setcontrols [value]
  (case value
    42 (go
         (<! (timeout 500))
         (setTreeControl)
       )
    43 (addMarkers)
    47 (let []
         (select-units-tree)
         (updatemarkers)
      )  
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
  (-> (jquery "#confirmModal .close")
          (.click)
  )
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
  (dom/optgroup {:label (if (:isnotification  @data) "סטטוס התראה" "סטטוס תקלה")}
    (map
      (fn [text]
        (let [
          ;tr1 (.log js/console (str  "name=" (:name text) ))
          ]
          (dom/option {:key (:id text) :data-width "100px" :value (:id text) :onChange #(handle-change % owner)} (:name text))
        )
      )
      (:statuses @data) 
    )
  )
)

(defcomponent notifications-view [data owner]
  (render [_]
          (dom/div {:className "row" :style {:padding-top "5px" :bottom "5px" :width "100%" :padding-right "15px" :padding-left "15px" :position "absolute" :background-color "white" :margin-left "0px" :margin-right "0px"}}
            ;(dom/div  {:className "col-3 col-sm-3 tree"})
            (dom/div {:className "col-md-12" :style {:padding-top "0px" :padding-bottom "10px" :padding-left "10px" :padding-right "10px" :border "1px solid lightgrey"}}
              (dom/div {:className "panel panel-default" :style {:margin-left "0px" :margin-right "0px" :margin-top "0px" :margin-bottom "0px" :padding-bottom "10px" :border "none"}}
                (dom/div {:className "panel-heading" :style {:text-align "right" :padding-top "0px" :padding-bottom "0px" :background-image "linear-gradient(to bottom,#337ab7 0,#265a88 100%)" :font-size "large" :color "white"}}
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-11" :style {:padding-top "5px" :padding-bottom "5px"}}
                      (if (:isnotification  @data) "טבלת התראות" "טבלת תקלות")
                    ) 
                    (dom/div {:className "col-md-1" :style {:text-align "left" :padding-top "5px" :padding-bottom "5px"}}
                      (dom/span {:className "glyphicon glyphicon-remove" :style {:margin-left "5px" :cursor "pointer"} :onClick (fn [e] (shelters/notificationsclick 0))})
                    )
                  )

                )
              )
              (dom/div {:className "panel panel-default" :style {:padding "0px" :margin-top "0px" :margin-bottom "0px" :margin-left "15px" :border "none"}}
                (dom/div {:className "panel-heading" :style {:margin-left "-15px" :padding-top "0px" :padding-bottom "0px" :margin-top "0px" :background-color "transparent" :background-image "none" :font-weight "600" :border "1px solid lightgray" :padding-left "0px"}}
                  (dom/div {:className "row" :style {:margin-left "17px" :margin-right "-14px"}}

                    (dom/div {:className "col-md-1" :style {:padding-left "0px" :padding-right "0px"}}
                      (dom/div {:className "row"}
                        (dom/div {:className "col-md-6" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px"}} "ראיתי")
                      (dom/div {:className "col-md-6" :style {:line-height "17px" :text-align "center" :border-left "1px solid" :padding-left "5px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px" :background-image (case (:sort-alerts @shelters/app-state) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 1 2 1)) (shelters/doswaps)))}
                        (dom/p {:style {:margin "0px"}} "מספר") (dom/p {:style {:margin "0px"}} "אירוע"))
                      )
                    )

                    (dom/div {:className "col-xs-1 col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "0px" :padding-bottom "0px" :line-height "17px" :background-image (case (:sort-alerts @shelters/app-state) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 3 4 3)) (shelters/doswaps)))}
                    (dom/p {:style {:margin "0px"}} "מזהה") (dom/p {:style {:margin "0px"}} "יחידה"))


                    (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :background-image (case (:sort-alerts @shelters/app-state) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 5 6 5)) (shelters/doswaps)))}  "שם יחידה")

                    (dom/div {:className "col-md-2" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :background-image (case (:sort-alerts @shelters/app-state) 7 "url(images/sort_asc.png" 8 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 7 8 7)) (shelters/doswaps)))}  "מיקום יחידה")

                    (dom/div {:className "col-md-1" :style {:text-align "center" :border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :background-image (case (:sort-alerts @shelters/app-state) 9 "url(images/sort_asc.png" 10 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 9 10 9)) (shelters/doswaps)))}  "שם אירוע")

                    (dom/div {:className "col-md-3" :style {:text-align "center" :padding-left "0px" :padding-right "0px" :padding-top "0px" :padding-bottom "0px"}}
                      (dom/div {:className "col-md-6" :style {:border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :background-image (case (:sort-alerts @shelters/app-state) 11 "url(images/sort_asc.png" 12 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 11 12 11)) (shelters/doswaps)))}
                        "זמן פתיחה"
                      )
                      (dom/div {:className "col-md-6" :style {:border-left "1px solid" :padding-top "7px" :padding-bottom "7px" :background-image (case (:sort-alerts @shelters/app-state) 13 "url(images/sort_asc.png" 14 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 13 14 13)) (shelters/doswaps)))}
                        "זמן סגירה"
                      )
                    )

                    (dom/div {:className "col-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                      (dom/div {:className "col-md-6" :style {:text-align "right" :border-left "1px solid" :padding-left "0px" :padding-right "0px" :background-image (case (:sort-alerts @shelters/app-state) 17 "url(images/sort_asc.png" 18 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 17 18 17)) (shelters/doswaps)))}
                        (omdom/select #js {:id "statuses"
                                           :title (if (:isnotification  @data) "סטטוס התראה" "סטטוס תקלה")
                                           :data-width "75%"
                                           ;:data-style "btn-default"
                                           :data-show-subtext "false"
                                           :data-live-search "true"
                                           :onChange #(handle-change % owner)
                                           }                
                          (buildStatusesList data owner)
                        )
                      )

                      (dom/div {:className "col-md-6" :style {:text-align "center" :border-left "1px solid transparent" :padding-top "7px" :padding-bottom "7px" :padding-left "0px" :padding-right "0px" :background-image (case (:sort-alerts @shelters/app-state) 15 "url(images/sort_asc.png" 16 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "left center" :cursor "pointer"} :onClick (fn [e] ((swap! shelters/app-state assoc-in [:sort-alerts] (case (:sort-alerts @shelters/app-state) 15 16 15)) (shelters/doswaps)))}  "אירוע טופל ע''י")
                    )
                  )
                )
              )
              (om/build shelters/notifications-table data {})
            )
          )
  )
)

(defcomponent group-view [group owner]
  (render [_]
    (let [
      
      ]
      (dom/ul
        (map (fn [item]
          (dom/li
            (dom/b (:name item))
            (om/build group-view item {})
            (map (fn [unit]
              (let [
                theunit (first (filter (fn [x] (if (= (:id x) unit) true false)) (:devices @shelters/app-state)))
                ]
                (dom/li (:name theunit))
              )
            ) (:childs item))
          )
        ) (filter (fn [x] (if (> (.indexOf (:parents x) (:id group)) -1) true false)) (:groups @app-state)))

        (map (fn [unit]
          (let [
            theunit (first (filter (fn [x] (if (= (:id x) unit) true false)) (:devices @shelters/app-state)))
            ]
            (dom/li {:id (str "liunit" (:id theunit))} (:name theunit))
          )
        ) (:childs group))
      )
    )
  )
)

(defcomponent tree-view [data owner]
  (render [_]
    (let []
      (dom/ul {:id "treeview"}
        (map (fn [group]
          (let []
            (dom/li {:data-icon-cls "fa fa-inbox" :data-expanded "true"}
              (om/build group-view group {})
              (:name group)
            )
          )

        ) (filter (fn [x] (if (nil? (:parents x)) true false)) (:groups @shelters/app-state)))
      )
    )
    
  )
)

(defcomponent addmodalconfirm [data owner]
  ;; (will-mount
  ;;   (transact! data :selectedgroup  (fn [_] {:id "9ce9fce2-58d7-47bd-8e3e-0b4e2f8ee6c9", :name "jhghgjh", :parents nil, :owners nil, :current "khjhjkhjk"}))
  ;; )
  (render [_]
    (let [
      ;tr1 (.log js/console (str "name=" (:name (:selectedgroup @data))))
      ]
      (dom/div
        (dom/div {:id "confirmModal" :className "modal fade" :role "dialog"}
          (dom/div {:className "modal-dialog"} 
            ;;Modal content
            (dom/div {:className "modal-content"} 
              (dom/div {:className "modal-header"} 
                (b/button {:type "button" :className "close" :data-dismiss "modal"})
                (dom/h4 {:className "modal-title"} "Confirmation" )
              )
              (dom/div {:className "modal-body"}

                (dom/div {:className "panel panel-primary"}
                  (dom/div {:className "panel-heading" :style {:padding "0px" :margin-top "10px"}}
                    ;(dom/h1 {:style {:text-align "center"}} "Please confirm")
                  )
                  (dom/div {:className "panel-body"}
                    (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                      (dom/div {:className "col-md-12" :style {}}
                        "האם אתה בטוח רוצה לשלוח פקודה פתח מנעול עבור יחידות שנבחרו?"
                      )
                    )
                  )
                )
              )
              (dom/div {:className "modal-footer"}
                (dom/div {:className "row"}
                  (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                    (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "ביטול")
                  )

                  (dom/div {:className "col-xs-6" :style {:text-align "center"}}
                    (b/button {:id "btnsavegroup" :disabled? (if (= (:state @data) 1) true false) :type "button" :className (if (= (:state @data) 0) "btn btn-default" "btn btn-default m-progress" ) :onClick (fn [e] (sendcommand1))} "לשלוח פקודה")
                  )
                )
              )
            )
          )
        )
      )
    )
  )
)


(defn openConfirmDialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#confirmModal")
          (.modal)
        )
      )
    )
  )
)


(defcomponent map-view [data owner]

  (did-mount [_]
    (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:selectedcenter @data)) :lng (:lon (:selectedcenter @data))} "zoom" 15})
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
      tbl (if (or (:isalert @data) (:isnotification @data)) (tableheight (if (:isalert @data) (count (:alerts @data)) (count (:notifications @data)))) 0)
      screen (.. js/document -body -clientHeight)

      treeheight (if (< (- screen tbl 175) 0) 0 (- screen tbl 175))
      pnlheight (if (< (- screen tbl 175) 0) 0 (- screen tbl 75))
      ]
      (dom/div {:style { :padding-right "0px"}}
        (om/build shelters/website-view data {})
        (dom/div {:className "row maprow" :style {:margin-left "15px" :margin-right "0px" :max-width "100%" :height (case (or (:isalert @data) (:isnotification @data)) true (str (+ 0 (- (.. js/document -body -clientHeight) (tableheight (if (:isalert @data) (count (:alerts @data)) (count (:notifications @data)))) 0)) "px") "100%")}}
          (dom/div  {:className "col-3 col-sm-3" :style {:height "100%" :padding-left "5px"}}
            (dom/div {:className "panel-primary" :style {:border "1px solid darkgrey" :overflow-y "hidden" :height "100%";:max-height (str pnlheight "px")
}}
              (dom/div {:className "panel-heading" :style {:margin-top "3px" :padding-top "3px" :padding-bottom "3px" :margin-left "15px" :margin-right "15px"}} "בחר קבוצה/יחידה")
              (dom/div {:className "panel-body" :style {:margin-top "0px" :padding-top "5px" :padding-bottom "5px" :margin-left "0px" :margin-right "0px"}}
                (dom/div  {:className "tree" :id "tree" :style { :overflow-y "scroll" :height (str treeheight "px") }})
                ;(om/build tree-view data {})
                (dom/div {:className "row" :style{:margin-top "10px" :margin-left "15px" :margin-right "-5px" :bottom "0px"}}
                  (dom/div {:className "col-xs-6" :style {:padding-left "5px" :padding-right "5px"}}
                    (b/button {:className "btn btn-primary btn-danger" :onClick (fn [e] (openConfirmDialog)) :disabled? (if (> (count (:selectedunits @shelters/app-state)) 0) false true) :style {:margin-bottom "5px" :width "100%"}} (t :he shelters/main-tconfig (keyword (str "commands/" (:name (first (:commands @data)))))))
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

        (if (or (:isnotification @data) (:isalert @data))
          (om/build notifications-view data {})
        )
        (om/build addmodalconfirm data {})
      ) 
    )
  )
)




(sec/defroute map-page "/map" []
  (let []
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

