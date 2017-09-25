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

            [cljs.core.async :refer [put! dropping-buffer chan take! <!]]

            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]

            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
  )
  (:import goog.History)
)

(enable-console-print!)

(defonce app-state (atom  {:map nil}))

(def jquery (js* "$"))

(def custom-formatter (tf/formatter "dd/MM/yyyy"))

(def custom-formatter1 (tf/formatter "MMM dd yyyy hh:mm:ss"))

(def ch (chan (dropping-buffer 2)))

(def js-object  (clj->js  {:data [ {:text "All cities"  :nodes [{:text "Tel Aviv" :nodes [{:text "1602323"}]} {:text "Ness Ziona" :nodes [{:text "2"}]} ]}]} ))


(defn OnGetPortfolios [response]
  ;;(swap! sbercore/app-state assoc-in [(keyword (:selectedsec @sbercore/app-state)) :portfolios] response  )
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
      "<h1 id=\"firstHeading\" class=\"firstHeading\">Uluru</h1>"
      "<div id=\"bodyContent\">"
      "<p><b>Uluru</b>, also referred to as <b>Ayers Rock</b>, is a large "
      "sandstone rock formation in the southern part of the "
      "Northern Territory, central Australia. It lies 335&#160;km (208&#160;mi) "
      "south west of the nearest large town, Alice Springs; 450&#160;km "
      "(280&#160;mi) by road. Kata Tjuta and Uluru are the two major "
      "features of the Uluru - Kata Tjuta National Park. Uluru is "
      "sacred to the Pitjantjatjara and Yankunytjatjara, the "
      "Aboriginal people of the area. It has many springs, waterholes, "
      "rock caves and ancient paintings. Uluru is listed as a World "
      "Heritage Site.</p>"
      "<p>" (:address device) ", <a href=\"/#/devdetail/" (:id device) "\">"
      "Go to visit device</a>"
      "(last visited June 22, 2009).</p>"
      "</div>"
      "</div>")

    window-options (clj->js {"content" wnd1})
    infownd (js/google.maps.InfoWindow. window-options)
    tr1 (.log js/console (str  "Lan=" (:lon device) " Lat=" (:lat device)))
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat device), (:lon device)) "map" (:map @app-state) "title" (:name device)})
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
  )
)

(defn addMarkers []
  (let[
    
    ]
    (doall (map addMarker (:devices @shelters/app-state)))
  )
)

(defn setcenterbycity [city]
  (let [
    thecity (first (filter (fn [x] (if (= (:name x) city) true false)) (:cities @shelters/app-state)))

    tr1 (.log js/console (str "city=" city " obj=" thecity))
    ]
    (.panTo (:map @app-state) (google.maps.LatLng. (:lat thecity), (:lat thecity)))
  )
)

(defn setcenterbydevice [device]
)

(defn setTreeControl []
  (.log js/console "Set Tree called")
  ;(.log js/console (count (:employees @app-state)))
  (jquery
    (fn []
      (-> (jquery "#tree" )
        (.treeview js-object)
        (.on "nodeSelected"
          (fn [event data] (
             let [
               ;table (-> (jquery "#dataTables-example") (.DataTable) )
               ;res (.data (.row table (.. e -currentTarget)) )
               res (js->clj data)
               
             ]
             (.log js/console res)
             (.log js/console (str "parentid=" (get res "parentId") " text=" (get res "text")))
             (if (= 0 (get res "parentId")) (setcenterbycity (get res "text")) (setcenterbydevice (get res "text")))
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




(defcomponent map-view [data owner]

  (did-mount [_]
    (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" (google.maps.LatLng. (:lat (:selectedcenter @data)), (:lat (:selectedcenter @data))) "zoom" 8})
      map (js/google.maps.Map. map-canvas map-options)
      tr1 (swap! app-state assoc-in [:map] map  )
      ]   
    )
  )

  (will-mount [_]
    (onMount data)
  )
  (render [_]
    (let [
      stylerow {:style {:margin-left "0px" :margin-right "0px"}}
      styleprimary {:style {:margin-top "70px" :margin-left "0px" :margin-right "0px"}}
      ]
      (dom/div
        (om/build shelters/website-view data {})
        
        (dom/div {:className "row"}
          (dom/div  {:className "col-md-2" :id "tree" :style {:height "500px" :margin-top "70px"}})
          (dom/div  {:className "col-md-10" :id "map" :style {:height "500px" :margin-top "70px"}})
          ;(b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (addMarkers))} "Add marker")
        )
      ) 
    )
  )
)




(sec/defroute map-page "/map" []
   (om/root map-view
            shelters/app-state
            {:target (. js/document (getElementById "app"))}))
