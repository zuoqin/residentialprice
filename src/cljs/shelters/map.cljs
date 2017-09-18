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
  ;;(put! ch 42)
  ;;(swap! sbercore/app-state assoc-in [:current] {:name "Portfolios" :text "Portfolios with this security: "} )

  ;;(swap! sbercore/app-state assoc-in [:view] 2)
  ;;(swap! sbercore/app-state assoc-in [:search] "")
)


(defn setcontrols []
  ;;(sbercore/setSecsDropDown)
  ;;(if (not (= nil (:selectedsec @sbercore/app-state)))
  ;;  (sbercore/getPortfolios)
  ;;)
  ;;(.log js/console "fieldcode" )
)

;;(defn initqueue []
;;  (doseq [n (range 1000)]
;;    (go ;(while true)
;;      (take! ch(
;;        fn [v] (
;;           setcontrols
;;          )
;;        )
;;      )
;;    )
;;  )
;;)

;;(initqueue)

(defn addMarker []
  (let [
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
      "<p>Attribution: Uluru, <a href=\"https://en.wikipedia.org/w/index.php?title=Uluru&oldid=297882194\">"
      "https://en.wikipedia.org/w/index.php?title=Uluru</a>"
      "(last visited June 22, 2009).</p>"
      "</div>"
      "</div>")
      window-options (clj->js {"content" wnd1})
      infownd (js/google.maps.InfoWindow. window-options)

      marker-options (clj->js {"position" (google.maps.LatLng. 32.0853, 34.7818) "map" (:map @app-state) "title" "The best title"})
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

(defcomponent map-view [data owner]

  (did-mount [_]
    (let [map-canvas (. js/document (getElementById "map"))
          map-options (clj->js {"center" (google.maps.LatLng. 32.0853, 34.7818) "zoom" 8})
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
        ;;(om/build sbercore/website-view sbercore/app-state {})
        (dom/div  {:id "map" :style {:height "500px"}})
        (dom/div
          (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (addMarker))} "Add marker")
        )
      ) 
    )
  )
)




(sec/defroute map-page "/map" []
   (om/root map-view
            shelters/app-state
            {:target (. js/document (getElementById "app"))}))
