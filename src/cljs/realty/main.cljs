(ns realty.main
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :refer-macros [defroute]]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [realty.core :as realty]
            [realty.settings :as settings]
            [om.dom :as omdom :include-macros true]
            [cljs-time.format :as tf]
            [cljs-time.core :as tc]
            [cljs-time.coerce :as te]
            [cljs-time.local :as tl]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]
            [om-bootstrap.input :as i]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
            [goog.string :as gstring]
            [goog.string.format]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout close!]]
  )
  (:import goog.History)
)

(def jquery (js* "$"))

(enable-console-print!)

(def ch (chan (dropping-buffer 2)))

(def iconBase "images/")

(def buildingtypes [{:id 1 :type "блочный"} {:id 2 :type "монолитный"} {:id 3 :type "деревянный"} {:id 4 :type "панельный"} {:id 5 :type "кирпичный"} {:id 6 :type "сталинский"} {:id 7 :type "кирпично-монолитный"}])

(def repairs [{:id 1 :name "отсутсвует"} { :id 2 :name "косметический"} { :id 3 :name "евроремонт"} { :id 4 :name "дизайнерский"}])

;(def cities [{:id 1 :name "Самара"} {:id 2 :name "Ростов-на-Дону"} {:id 3 :name "Краснодарский край"} {:id 4 :name "Уфа"} {:id 5 :name "Новосибирская область"} {:id 6 :name "Севастополь"} {:id 7 :name "Ростовская область"} {:id 8 :name "Республика Башкортостан"} {:id 9 :name "Московская область"} {:id 10 :name "Нижний Новгород"} {:id 11 :name "Москва"} {:id 12 :name "Нижегородская область"} {:id 13 :name "Ленинградская область"} {:id 14 :name "Калининградская область"} {:id 15 :name "Санкт-Петербург"} {:id 16 :name "Новосибирск"}])

(def cities [{:id 9 :name "Московская область"} {:id 11 :name "Москва"}])


(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)

(defn error-handler-zkh [{:keys [status status-text]}]
  ;(.log js/console (str "something bad happened: " status " " status-text))
  (swap! realty/app-state assoc-in [:state] 0)
  (swap! realty/app-state assoc-in [:object :address] "Ошибка получения данных по квартире")
  
)

(defn comp-analogs
  [analog1 analog2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (case (:sort-list @realty/app-state)
    1  (if (> (compare (:address analog1) (:address analog2)) 0)
      false
      true
    )
    2  (if (> (compare (:address analog1) (:address analog2)) 0)
      true
      false
    )
    3  (if (> (:totalarea analog1) (:totalarea analog2))
      false
      true
    )
    4  (if (> (:totalarea analog1) (:totalarea analog2))
      true
      false
    )
    5  (if (> (:price analog1) (:price analog2))
      false
      true
    )
    6  (if (> (:price analog1) (:price analog2))
      true
      false
    )
    7  (if (> (:pricepermetr analog1) (:pricepermetr analog2))
      false
      true
    )
    8  (if (> (:pricepermetr analog1) (:pricepermetr analog2))
      true
      false
    )
  )
)

(defn drop-nth [n coll]
   (keep-indexed #(if (not= %1 n) %2) coll))


(defn handleChange [e]
  ;(.log js/console (.. e -nativeEvent -target)  )  
  ;(.log js/console (.. e -nativeEvent -target -step))
  (swap! realty/app-state assoc-in [:object (keyword (.. e -nativeEvent -target -id))] (if (= "" (.. e -nativeEvent -target -step)) (.. e -nativeEvent -target -value) (js/parseFloat (.. e -nativeEvent -target -value))))
)


(defn OnGetZKHData [response]
  (let[]
    (swap! realty/app-state assoc-in [:object :buildingyear] (get response "buildingyear"))
    (swap! realty/app-state assoc-in [:object :foundation] (get response "foundation"))
    (swap! realty/app-state assoc-in [:object :housetype] (get response "house"))
    (swap! realty/app-state assoc-in [:object :buildingtype] (get response "housetype"))
    (swap! realty/app-state assoc-in [:object :project] (get response "project"))
    (swap! realty/app-state assoc-in [:object :storeysnum] (get response "storeysnum"))
    (swap! realty/app-state assoc-in [:object :city] (if (= (get response "region") "Московская") "Московская область" "Москва"))
    (swap! realty/app-state assoc-in [:state] 0)
    (jquery
      (fn []
        (-> (jquery "#city")
          (.selectpicker "val" (:city (:object @realty/app-state)))
        )
      )
    )
    (jquery
      (fn []
        (-> (jquery "#buildingtype")
          (.selectpicker "val" (:buildingtype (:object @realty/app-state)))
        )
      )
    )
    ;;(.log js/console response)
  )
)

(defn getzkhdata [address]
  (let [
    ;status (js/parseInt (:statuses (:filter @app-state)))
    ;user (:user (:filter @app-state))
    ]
    (swap! realty/app-state assoc-in [:state] 1)
    (GET (str "http://api.residential.eliz.site/api/" "getzkh?address=" address) {
      :handler OnGetZKHData
      :error-handler error-handler-zkh
      :response-format :json
    })
  )
)



(defn addplace [place]
  (let [
    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})

    ;tr1 (.log js/console place)
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @realty/app-state) "icon" image "title" (.. place -name)})

    marker (js/google.maps.Marker. marker-options)
    ]
    (.panTo (:map @realty/app-state) (.. place -geometry -location))

    (if (not (nil? (:marker @realty/app-state))) (.setMap (:marker @realty/app-state) nil))
    (swap! realty/app-state assoc-in [:marker] marker)
    (swap! realty/app-state assoc-in [:object :lat] (.lat (.. place -geometry -location)))
    (swap! realty/app-state assoc-in [:object :lon] (.lng (.. place -geometry -location)))
    (swap! realty/app-state assoc-in [:object :address] (.. place -formatted_address))
    (getzkhdata (.. place -formatted_address))
  )
)


(defn addsearchbox []
  (let [
    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat (:device @realty/app-state)), (:lon (:device @realty/app-state))) "map" (:map @realty/app-state) "icon" image "title" (:name (:device @realty/app-state))})

    marker (js/google.maps.Marker. marker-options)
    
    ;;Create the search box and link it to the UI element.
    input (. js/document (getElementById "pac-input"))
    searchbox (js/google.maps.places.SearchBox. input)
    ;tr1 (.log js/console input)
    ]
    (swap! realty/app-state assoc-in [:marker] marker)
    (.push (aget (.-controls (:map @realty/app-state)) 1) input)
    (if (not (nil? (:marker @realty/app-state))) (.setMap (:marker @realty/app-state) (:map @realty/app-state)))
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
    (.panTo (:map @realty/app-state) (google.maps.LatLng. (:lat (:object @realty/app-state)), (:lon (:object @realty/app-state))))    
  )
)


(defn setNewUnitValue [key val]
  (swap! realty/app-state assoc-in [(keyword key)] val)
)

(defn onDropDownChange [id value]
  (let [
    ;value (if (= id "unit") )
    ]
    (swap! realty/app-state assoc-in [:object (keyword id)] value)
  )
  ;(.log js/console (str "id=" id "; value=" value))
)

(defn setDropDowns []
  (jquery
     (fn []
       (-> (jquery "#buildingtype" )
         (.selectpicker {})
       )
     )
   )

  (jquery
     (fn []
       (-> (jquery "#city")
         (.selectpicker {})
       )
     )
   )

  (jquery
     (fn []
       (-> (jquery "#repair")
         (.selectpicker {})
       )
     )
   )

   (jquery
     (fn []
       (-> (jquery "#buildingtype")
         (.selectpicker "val" (:buildingtype (:object @realty/app-state)))
         (.on "change"
           (fn [e]
             (onDropDownChange (.. e -target -id) (.. e -target -value))
               ;(.log js/console e)
           )
         )
       )
     )
   )

   (jquery
     (fn []
       (-> (jquery "#city")
         (.selectpicker "val" (:city (:object @realty/app-state)))
         (.on "change"
           (fn [e]
             (onDropDownChange (.. e -target -id) (.. e -target -value))
               ;(.log js/console e)
           )
         )
       )
     )
   )

   (jquery
     (fn []
       (-> (jquery "#repair")
         (.selectpicker "val" (:repair (:object @realty/app-state)))
         (.on "change"
           (fn [e]
             (onDropDownChange (.. e -target -id) (.. e -target -value))
               ;(.log js/console e)
           )
         )
       )
     )
   )
)

(defn setcontrols [value]
  (case value
    46 (setDropDowns)
    43 (go
         (<! (timeout 100))
         (addsearchbox)
       )

    44 (swap! realty/app-state assoc-in [:showmap] -1)
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

(defn array-to-string [element]
  (let [
      newdata {:empname (get element "empname") } 
    ]
    (:empname newdata)
  )
)



(defn OnError [response]
  (let [     
      newdata { :error (get (:response response)  "error") }
    ]
    (.log js/console (str  response )) 
    
  )
  
  
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  ;(.log js/console "The change ....")

)


(defn onMount [data]
  (swap! realty/app-state assoc-in [:current] 
    "Object detail"
  )
  (set! (.-title js/document) "Price calculation")
  (setcontrols 46)
  (put! ch 43)
  (swap! realty/app-state assoc-in [:view] 1)
  (set! (.-title js/document) (str "Независимая оценка квартиры" (if (> (count (:address (:object @realty/app-state))) 0) ": ") (:address (:object @realty/app-state))))
)


(defn handle-change [e owner]
  ;(.log js/console e)
  (swap! realty/app-state assoc-in [:object (keyword (.. e -target -id))] 
    (.. e -target -value)
  )

  (.log js/console "jhghghghj")
)


(defn handle-chkb-change [e]
  (let [
    id (subs (.. e -currentTarget -id) 8)
    isinclude (.. e -currentTarget -checked)
    newanalogs (map (fn [x] (let [] x)) (:analogs (:object @realty/app-state)))
    ]
    (.log js/console (str "id:" id ";" isinclude))
    (swap! realty/app-state assoc-in [:object :analogs] newanalogs)
  )
)


(defcomponent showanalogs-view [data owner]
  (render
    [_
      
    ]
    (let [
      ;tr1 (.log js/console data)
      
      ]
      (if (> (count (:analogs (:object @realty/app-state))) 0)
        (dom/div {:className "panel panel-info"}
          (dom/div {:className "panel panel-heading" :style {:margin-bottom "0px"}}
            (dom/div {:className "row"} 
              ;; (dom/div {:className "col-xs-5  col-xs-offset-0" :style {:text-align "center"}}
              ;;   "Адрес"
              ;; )
              (dom/div {:className "col-xs-5 col-xs-offset-0" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @data) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 1 2 1)))}
                          "Адрес"
                        )
              (dom/div {:className "col-xs-1"}
                "Тип дома"
              )

              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @data) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 3 4 3)))}
                          "Общая площадь"
                        )

              (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                "Этаж"
              )
              (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                "Год постройки"
              )

              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @data) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 5 6 5)))}
                          "Цена"
                        )


              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "5px" :text-align "center" :background-image (case (:sort-list @data) 7 "url(images/sort_asc.png" 8 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 7 8 7)))}
                          "Цена за метр"
                        )

              (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                "Включать"
              )
            )
          )
          (dom/div {:className "panel panel-body" :style {:padding "0px"}}
            (map (fn [item]
              (let [ 
                square (str (:totalsquare item) (if (> (:leavingsquare item) 0) (str "/" (:leavingsquare item)) "") (if (> (:kitchensquare item) 0) (str "/" (:kitchensquare item)) "") )
                ]
                (dom/div {:className "row tablerow" :style {:margin-right "0px" :margin-left "0px"}}
                  (dom/div {:className "col-xs-5  col-xs-offset-0" :style {:text-align "left" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (:address item))

                  )

                  (dom/div {:className "col-xs-1" :style {:text-align "left" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (:housetype item))
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (:totalarea item))
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (str (:floor item) "/" (:floors item)) )
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (if (= 0 (:buildyear item)) "не известно" (:buildyear item)) )
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (realty/split-thousands (gstring/format "%.0f" (:price item))))
                  )

                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (realty/split-thousands (gstring/format "%.0f" (:pricepermetr item) )))
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                    (dom/input {:id (str "isanalog" (:id item)) :type "checkbox" :style {:height "32px" :margin-top "1px"} :defaultChecked (:isinclude item) :onChange (fn [e] (handle-chkb-change e ))})
                  )
                )
              )
              )(sort (comp comp-analogs) (:analogs (:object @realty/app-state)))
            )
          )
        )
        (dom/div )
      )
    )
  )
)
(defn map-analog [idx analog]
  (let [
    housetype (nth analog 0)
    price (nth analog 1)
    totalarea (nth analog 2)
    floor (nth analog 7)
    floors (nth analog 8)
    repair (nth analog 11)
    buildyear (nth analog 12)
    address (nth analog 9)
    leavingsquare (nth analog 13)
    kitchensquare (nth analog 14)
    pricepermetr (/ price totalarea)
    isinclude true
    ]
    ;
    {:id idx :floor floor :isinclude true :floors floors :housetype housetype :address address :price price :buildyear buildyear :totalarea totalarea :pricepermetr pricepermetr}
  )
)

(defn OnGetData [response]
  (let[]
    (swap! realty/app-state assoc-in [:object :pricePerMetr] (get response "pricePerMetr"))
    (swap! realty/app-state assoc-in [:object :houseAvrgPrice] (get response "houseAvrgPrice"))
    (swap! realty/app-state assoc-in [:object :regionAvrgPrice] (get response "regionAvrgPrice"))
    (swap! realty/app-state assoc-in [:object :cityAvrgPrice] (get response "cityAvrgPrice"))
    (swap! realty/app-state assoc-in [:object :data] (get response "data"))
    (swap! realty/app-state assoc-in [:object :analogs] (map-indexed map-analog (get response "analogs")))
    (swap! realty/app-state assoc-in [:state] 0)
    ;;(.log js/console response)
  )
)


(defn getdata []
  (let [
    ;status (js/parseInt (:statuses (:filter @app-state)))
    ;user (:user (:filter @app-state))
    ]
    (swap! realty/app-state assoc-in [:state] 1)
    (GET (str settings/apipath "estimate?totalsquare=" (:totalsquare (:object @realty/app-state)) "&repairRaw=" (:repair (:object @realty/app-state)) "&longitude=" (:lon (:object @realty/app-state)) "&latitude=" (:lat (:object @realty/app-state)) "&housetype=" (:buildingtype (:object @realty/app-state)) "&city=" (:city (:object @realty/app-state)) "&buildingyear=" (:buildingyear (:object @realty/app-state)) "&ceilingheight" (:ceilingheight (:object @realty/app-state)) "&storey=" (:storey (:object @realty/app-state)) "&storeysnum=" (:storeysnum (:object @realty/app-state)) "&metrodistance=" (:metrodistance (:object @realty/app-state)) "&leavingsquare=" (:leavingsquare (:object @realty/app-state)) "&kitchensquare=" (:kitchensquare (:object @realty/app-state)) "&analogscount=" (:analogscount (:object @realty/app-state))) {
      :handler OnGetData
      :error-handler error-handler
      :response-format :json
    })
  )
)


(defn buildRepairList [data owner]
  (map
    (fn [city]
      (dom/option {:key (:id city)  :value (:name city)} (:name city))
    )
    repairs
  )
)

(defn buildCitiesList [data owner]
  (map
    (fn [city]
      (dom/option {:key (:id city)  :value (:name city)} (:name city))
    )
    cities
  )
)

(defn buildBuildingTypesList [data owner]
  (map
    (fn [text]
      (let [
        ;tr1 (.log js/console (str  "name=" (:name text) ))
        ]
        (dom/option {:key (:id text) :data-width "100px" :value (:type text)} (:type text))
      )
    )
    buildingtypes 
  )
)


(defn createMap []
  (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:object @realty/app-state)) :lng (:lon (:object @realty/app-state))} "zoom" 12})
      map (js/google.maps.Map. map-canvas map-options)
      tr1 (swap! realty/app-state assoc-in [:map] map)
      tr1 (.set map "disableDoubleClickZoom" true)
    ]
  )
)

(defcomponent devdetail-page-view [data owner]
  (did-mount [_]
    (let [

      ]
      (createMap)
      (onMount data)
      (put! ch 44)
      (jquery
        (fn []
          (let [
            map (:map @realty/app-state)
            ]
            (-> map
              (.addListener "dblclick"
                (fn [e]
                  (let [
                    size (js/google.maps.Size. 48 48)
                    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})

                    marker-options (clj->js {"position" (google.maps.LatLng. (.lat (.. e -latLng)), (.lng (.. e -latLng))) "map" map "icon" image})
                    marker (js/google.maps.Marker. marker-options)
                    ]
                    (if (not (nil? (:marker @realty/app-state))) (.setMap (:marker @realty/app-state) nil))
                    ;(.log js/console (str "LatLng=" (.. e -latLng)))

                    (swap! realty/app-state assoc-in [:object :lat] (.lat (.. e -latLng)))
                    (swap! realty/app-state assoc-in [:object :lon] (.lng (.. e -latLng)))
                    (swap! realty/app-state assoc-in [:marker] marker)
                    (.stopPropagation (.. js/window -event))
                    (.stopImmediatePropagation (.. js/window -event))
                  )
                )
              )
            )
          )
        )
      )
    )
  )



  (did-update [this prev-props prev-state]
    ;(.log js/console "Update happened") 

    ;(put! ch 46)
  )
  (render
    [_]
    (let [style {:style {:margin "10px;" :padding-bottom "0px;"}}
      ]
      (dom/div {:style {:padding-top "10px"}}
        ;(om/build realty/website-view realty/app-state {})
        
        (dom/h3 {:style {:text-align "center"}}
          (dom/i {:className "fa fa-cube"})
          (str "Параметры квартиры")
        )
        
        (dom/div {:className "row" :style {:width "50%" :padding-left "200px"}}


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Общая площадь:")
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "totalsquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:totalsquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Жилая площадь:")
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "leavingsquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:leavingsquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Площадь кухни:")
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "kitchensquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:kitchensquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Тип ремонта:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (omdom/select #js {:id "repair"
                                   :className "selectpicker"
                                   :data-width "100%"
                                   :data-style "btn-default"
                                   :data-show-subtext "false"
                                   :data-live-search "true"
                                   :onChange #(handle-change % owner)
                                   }                
                  (buildRepairList data owner)
                )
              )
              ;; (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
              ;;   (dom/input {:id "leavingsquare" :class "form-control" :style {:width "100%"} :type "text" :required true :onChange (fn [e] (handleChange e)) :value (:leavingsquare (:object @data))})
              ;; )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Город:"))
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (omdom/select #js {:id "city"
                                   :className "selectpicker"
                                   :data-width "100%"
                                   :data-style "btn-default"
                                   :data-show-subtext "false"
                                   :data-live-search "true"
                                   :onChange #(handle-change % owner)
                                   }                
                  (buildCitiesList data owner)
                )
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Тип дома:"))
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (omdom/select #js {:id "buildingtype"
                                   :className "selectpicker"
                                   :data-width "100%"
                                   :data-style "btn-default"
                                   :data-show-subtext "false"
                                   :data-live-search "true"
                                   :onChange #(handle-change % owner)
                                   }                
                  (buildBuildingTypesList data owner)
                )
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Год постройки:"))
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "buildingyear"  :class "form-control" :required true :type "number" :step "1" :onChange (fn [e] (handleChange e)) :value (:buildingyear (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Высота потолков:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "ceilingheight" :class "form-control" :type "number" :step "0.1" :onChange (fn [e] (handleChange e)) :value (:ceilingheight (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 offset-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Этаж:"))
              )
              (dom/div {:className "col-xs-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "storey" :type "number" :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:storey (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Этажность дома:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "storeysnum" :type "number" :min 1 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:storeysnum (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Расстояние до метро:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "metrodistance" :type "number" :min 10 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:metrodistance (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Количество аналогов:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "analogscount" :type "number" :min 10 :max 100 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:analogscount (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (if (> (count (:foundation (:object @realty/app-state))) 0)
              (dom/h5 {:style {:display:inline true}} "Основание здания: "
                 (:foundation (:object @data))
              )
            )
            (if (> (count (:housetype (:object @realty/app-state))) 0)
              (dom/h5 {:style {:display:inline true}} "Тип дома: "
                 (:housetype (:object @data))
              )
            ) 
            (if (> (count (:project (:object @realty/app-state))) 0)
              (dom/h5 {:style {:display:inline true}} "Серия, тип здания: "
                 (:project (:object @data))
              )
            ) 
            (dom/h5 {:style {:display:inline true}} "Широта: "
               (:lat (:object @data))
            )
            (dom/h5 {:style {:display:inline true}} "Долгота: "
               (:lon (:object @data))
            )
            (dom/h5 {:style {:display:inline true}} "Адрес: "
               (:address (:object @data))
            )
            (dom/input {:id "pac-input" :className "controls" :type "text" :style {:width "70%"} :placeholder "Поиск по адресу" })

            (dom/div {:className "row" :style {:padding-top "0px" :height "400px" :display "block"}}
              (dom/div  {:className "col-xs-12" :id "map" :style {:margin-top "0px" :height "100%"}})
              
            )


            (dom/div {:className "row" :style {:padding-top "10px" :display "block"}}
              (b/button {:className (if (= (:state @data) 0) "btn btn-primary" "btn btn-primary m-progress") :onClick (fn [e] (getdata))} "Получить стоимость: ")

            )
            (dom/div {:style {:display (if (= (:state @data) 0) "block" "none")}}
              (dom/div {:className "row" :style {:display (if (= 0.0 (:data (:object @realty/app-state))) "none" "block") :padding-top "10px"}}
                (dom/div {:className "panel panel-primary"}
                  (dom/div {:className "panel-heading"}
                    (str "Цена: " (realty/split-thousands (gstring/format "%.2f" (:data (:object @realty/app-state)))))
                  )

                )

              )


              ;; (dom/div {:className "row" :style {:display (if (= 0.0 (:data (:object @realty/app-state))) "none" "block") :padding-top "10px"}}
              ;;   (dom/div {:className "panel panel-primary"}
              ;;     (dom/div {:className "panel-heading"}
              ;;       (str "Цена за метр: " (realty/split-thousands (gstring/format "%.2f" (:pricePerMetr (:object @realty/app-state)))))
              ;;     )
              ;;   )
              ;; )


              ;; (dom/div {:className "row" :style {:display (if (= 0.0 (:data (:object @realty/app-state))) "none" "block") :padding-top "10px"}}
              ;;   (dom/div {:className "panel panel-primary"}
              ;;     (dom/div {:className "panel-heading"}
              ;;       (str "Средняя цена по дому: " (if (< (:houseAvrgPrice (:object @realty/app-state)) 1.0) "не известно" (realty/split-thousands (gstring/format "%.2f" (:houseAvrgPrice (:object @realty/app-state))))))
              ;;     )
              ;;   )
              ;; )



              ;; (dom/div {:className "row" :style {:display (if (= 0.0 (:data (:object @realty/app-state))) "none" "block") :padding-top "10px"}}
              ;;   (dom/div {:className "panel panel-primary"}
              ;;     (dom/div {:className "panel-heading"}
              ;;       (str "Средняя цена по району: " (realty/split-thousands (gstring/format "%.2f" (:regionAvrgPrice (:object @realty/app-state)))))
              ;;     )
              ;;   )
              ;; )

              ;; (dom/div {:className "row" :style {:display (if (= 0.0 (:data (:object @realty/app-state))) "none" "block") :padding-top "10px"}}
              ;;   (dom/div {:className "panel panel-primary"}
              ;;     (dom/div {:className "panel-heading"}
              ;;       (str "Средняя цена по городу: " (realty/split-thousands (gstring/format "%.2f" (:cityAvrgPrice (:object @realty/app-state)))))
              ;;     )
              ;;   )
              ;; )
            )
            ;; (dom/div
            ;;   (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (aset js/window "location" (str "#/groupstounit/" (:id (:device @data)))))} "Assign to groups")
            ;; )
            ;(om/build parentgroups-view data {})



        )
        (om/build showanalogs-view  data {})
      )


    )
  )
)





(sec/defroute devdetail-page "/main" []
  (let[

    ]

    (swap! realty/app-state assoc-in [:view] 1)
    (om/root devdetail-page-view
             realty/app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(defn main []
  (-> js/document
      .-location
      (set! "#/main"))
  (sec/dispatch! "/main")

  ;;(aset js/window "location" "#/main")
)

(main)
