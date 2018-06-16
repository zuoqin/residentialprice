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

(def params [{:id "RoomsNum" :name "Количество комнат"} {:id "Storey" :name "Этаж"} {:id "StoreysNum" :name "Количество этажей"} {:id "RawAddress" :name "Адрес"} {:id "MicroDistrict" :name "Район"} {:id "RepairRaw" :name "Ремонт"} {:id "BuildingYear" :name "Год постройки"} {:id "LivingSpaceArea" :name "Жилая площадь"} {:id "KitchenArea" :name "Площадь кухни"} {:id "SubwayTime" :name "Расстояние до метро"}])
;(def cities [{:id 1 :name "Самара"} {:id 2 :name "Ростов-на-Дону"} {:id 3 :name "Краснодарский край"} {:id 4 :name "Уфа"} {:id 5 :name "Новосибирская область"} {:id 6 :name "Севастополь"} {:id 7 :name "Ростовская область"} {:id 8 :name "Республика Башкортостан"} {:id 9 :name "Московская область"} {:id 10 :name "Нижний Новгород"} {:id 11 :name "Москва"} {:id 12 :name "Нижегородская область"} {:id 13 :name "Ленинградская область"} {:id 14 :name "Калининградская область"} {:id 15 :name "Санкт-Петербург"} {:id 16 :name "Новосибирск"}])

(def cities [{:id 9 :name "Московская область"} {:id 11 :name "Москва"}])


(defn error-handler [{:keys [status status-text]}]
  (swap! realty/app-state assoc-in [:state] 0)
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
    9  (if (or (> (:index analog1) (:index analog2))
        (and (= (:index analog1) (:index analog2)) (> (:pricepermetr analog1) (:pricepermetr analog2)))
      )
      false
      true
    )
    10  (if (or (> (:index analog1) (:index analog2))
        (and (= (:index analog1) (:index analog2)) (> (:pricepermetr analog1) (:pricepermetr analog2)))
        )
      true
      false
    )
  )
)

(defn drop-nth [n coll]
   (keep-indexed #(if (not= %1 n) %2) coll))


(defn openreportdialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#reportModal")
          (.modal)
        )
      )
    )
  )
)

(defn openimagedialog []
  (let [
    ;tr1 (.log js/console (:device @dev-state))
    ]
    (jquery
      (fn []
        (-> (jquery "#imageModal")
          (.modal)
        )
      )
    )
  )
)


(defn showimage [number]
  (swap! realty/app-state assoc-in [:selectedimage] number)
  (put! ch 49)
)

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

(defn downloadreport []
  (aset js/window "location" (str "/report"))
  (put! ch 57)
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
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @realty/app-state) "title" (.. place -name)})

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
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat (:device @realty/app-state)), (:lon (:device @realty/app-state))) "map" (:map @realty/app-state) "title" (:name (:device @realty/app-state))})

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
    (case id
      "param" (let [
       oldvalue (:param (:object @realty/app-state))


       newvalues (loop [result [] nums (range 9)]
                (if (seq nums)
                  (let [
                        num (first nums)
                        ;tr1 (.log js/console (str "num: " num "selected: " (.-selected (aget (.-options (js/document.getElementById id)) num))))
                        ]
                    (recur (if (= (.-selected (aget (.-options (js/document.getElementById id)) num)) true) (conj result (nth realty/allparams num)) result)
                         (rest nums))
                  )
                  result)
        )

        tr1 (.log js/console (clj->js newvalues))
       ;newvalue (doall (map (fn [x] (if (= (.-selected (aget (.-options (js/document.getElementById id)) x)) true) (conj oldvalue) )) (range 9))) 
          ]
         ;(remove (fn [y] (if (= y value) true false)) oldvalue)
         (swap! realty/app-state assoc-in [:object (keyword id)] newvalues)
      )
      (swap! realty/app-state assoc-in [:object (keyword id)] value)
    )
    
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
       (-> (jquery "#param" )
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
       (-> (jquery "#param")
         (.selectpicker "val" (clj->js (:param (:object @realty/app-state))))
         (.on "change"
           (fn [e]
             (let []
               (onDropDownChange (.. e -target -id) (.. e -target -value))
               (.log js/console (str (.. e -target -id) "val: " (.. e -target -value)))
             )
             
               ;(.log js/console e)
           )
         )
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



(defn handle-change [e owner]
  ;(.log js/console e)
  (swap! realty/app-state assoc-in [:object (keyword (.. e -target -id))] 
    (.. e -target -value)
  )

  (.log js/console "jhghghghj")
)


(defn handle-chkb-change [e]
  (let [
    id (js/parseInt (subs (.. e -currentTarget -id) 8))
    isinclude (.. e -currentTarget -checked)
    newanalogs (map (fn [x] (if (= id (:id x)) (assoc-in x [:isinclude] isinclude) x)) (:calcanalogs (:object @realty/app-state)))
    ]
    ;(.log js/console (str "id:" id ";" isinclude))
    (swap! realty/app-state assoc-in [:object :calcanalogs] newanalogs)
  )
)


(defcomponent showanalogs-view [data owner]
  (render
    [_
      
    ]
    (let [
      ;tr1 (.log js/console  (:key (om/get-state owner)))
      
      ]
      (if (> (count (:analogs (:object @realty/app-state))) 0)
        (dom/div {:className "panel panel-info"}
          (dom/div {:className "panel panel-heading" :style {:margin-bottom "0px"}}
            (dom/div {:className "row"} 
              ;; (dom/div {:className "col-xs-5  col-xs-offset-0" :style {:text-align "center"}}
              ;;   "Адрес"
              ;; )
              (dom/div {:className "col-xs-5 col-xs-offset-0" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @data) 1 "url(images/sort_asc.png" 2 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 1 2 1)))}
                          "Адрес"
                        )
              (dom/div {:className "col-xs-1"}
                "Тип дома"
              )

              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @data) 3 "url(images/sort_asc.png" 4 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 3 4 3)))}
                          "Общая площадь"
                        )

              (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                "Этаж"
              )
              (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                "Год постройки"
              )

              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @data) 5 "url(images/sort_asc.png" 6 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 5 6 5)))}
                          "Цена"
                        )


              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @data) 7 "url(images/sort_asc.png" 8 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 7 8 7)))}
                          "Цена за метр"
                        )

              (dom/div {:className "col-xs-1" :style {:cursor "pointer" :padding-left "0px" :padding-right "3px" :padding-top "0px" :text-align "center" :background-image (case (:sort-list @data) 9 "url(images/sort_asc.png" 10 "url(images/sort_desc.png" "url(images/sort_both.png") :background-repeat "no-repeat" :background-position "right center"} :onClick (fn [e] (swap! realty/app-state assoc-in [:sort-list] (case (:sort-list @data) 9 10 9)))}
                "Релевантность"
              )
            )
          )
          (dom/div {:className "panel panel-body" :style {:padding "0px"}}
            (map-indexed (fn [idx item]
              (let [ 
                square (str (:totalsquare item) (if (> (:leavingsquare item) 0) (str "/" (:leavingsquare item)) "") (if (> (:kitchensquare item) 0) (str "/" (:kitchensquare item)) "") )
                ]
                (dom/div {:className "row tablerow" :style {:margin-right "0px" :margin-left "0px"}}
                  (dom/div {:className "col-xs-5  col-xs-offset-0" :style {:text-align "left" :border "1px solid lightgrey" :padding-top "6px" :overflow "hidden" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item))) }
                    (str (+ idx 1) ". "   (:address item))

                    ))
                  )

                  (dom/div {:className "col-xs-1" :style {:text-align "left" :border "1px solid lightgrey" :padding-top "6px" :overflow "hidden" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (:housetype item)

                    ))
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (:totalarea item)

                    ))
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (str (:floor item) "/" (:floors item))

                    ) )
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (if (= 0 (:buildyear item)) "не известно" (:buildyear item))

                    ) )
                  )
                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (realty/split-thousands (gstring/format "%.0f" (:price item)))

                    ))
                  )

                  (dom/div {:className "col-xs-1" :style {:text-align "right" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (dom/a {:className "list-group-item" :style {:padding "0px" :border "none" :background "transparent"} :href (str "#/" (:id item)) :onClick (fn [e] (showimage (:id item)))}
                    (realty/split-thousands (gstring/format "%.0f" (:pricepermetr item) ))

                    ))
                  )

                  (case (:key (om/get-state owner))
                     "calcanalogs"  (dom/div {:className "col-xs-1" :style {:text-align "center"}}
                    (dom/input {:id (str "isanalog" (:id item)) :type "checkbox" :style {:height "32px" :margin-top "1px"} :checked (:isinclude item) :onChange (fn [e] (handle-chkb-change e ))})
                  )
                   (dom/div {:className "col-xs-1" :style {:text-align "center" :border "1px solid lightgrey" :padding-top "6px" :padding-bottom "6px"}}
                    (dom/h4 {:className "list-group-item-heading" :style {:font-weight "normal" :white-space "nowrap"}} (realty/split-thousands (gstring/format "%.0f" (:index item) )))
                  ) 
                  )
                  
                 
                )
              )
              )(sort (comp comp-analogs) ((keyword (:key (om/get-state owner))) (:object @realty/app-state)))
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
    city (nth analog 3)
    lat (nth analog 4)
    lon (nth analog 5)
    id (nth analog 6)
    roomsnum (nth analog 7)
    floor (nth analog 8)
    floors (nth analog 9)
    address (nth analog 10)
    district (nth analog 11)    
    repair (nth analog 12)
    buildyear (nth analog 13)
    livingsquare (nth analog 14)
    kitchensquare (nth analog 15)
    metrodistance (nth analog 16)
    status (nth analog 17)
    pricepermetr (/ price totalarea)
    isinclude true
    index (nth analog 18)
    screenshot (nth analog 19)
    ]
    (.log js/console screenshot)
    {:id id :housetype housetype :price price :totalarea totalarea :city city :lat lat :lon lon :roomsnum roomsnum :floor floor :floors floors :address address :district district :repair repair :buildyear buildyear :livingsquare livingsquare :kitchensquare kitchensquare :metrodistance metrodistance :status status :pricepermetr pricepermetr :index index :isinclude true :screenshot screenshot}
  )
)


(defn addMarker [analog]
  (let [
    ;tr1 (.log js/console (str "token: " (:token (:token @shelters/app-state)) ))
    ;tr1 (.log js/console (str "command1= " (:name (nth (:commands @shelters/app-state) 1))))
    wnd1  (str "<div id=\"content\" style=\" width: 200px;  \" >"
        "<h5 style=\"text-align: center; margin-bottom: 0px; margin-top: 5px  \">" (realty/split-thousands (gstring/format "%.0f" (:price analog))) " р." "</h5>"
      "<h5 style=\"text-align: center; margin-bottom: 0px; margin-top: 5px  \">" (:address analog) "</h5>"

      "<h5 style=\"text-align: center; margin-bottom: 0px; margin-top: 5px  \">" (:housetype analog) ", " (:buildyear analog) " г." "</h5>"

      "<h5 style=\"text-align: center; margin-bottom: 0px; margin-top: 5px  \">" (:totalarea analog) " м² "  (:floor analog) " этаж/" (:floors analog) "</h5>"
      "</div>")

    window-options (clj->js {"content" wnd1})
    infownd (js/google.maps.InfoWindow. window-options)
    ;tr1 (.log js/console (str  "Lan=" (:lon device) " Lat=" (:lat device)))
    

    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})


    marker-options (clj->js {"animation" 2 "position" (google.maps.LatLng. (:lat analog), (:lon analog))  "map" (:map @realty/app-state) "title" (:address analog) "id" (:id analog)})
    marker (js/google.maps.Marker. marker-options)
    tr1 (.log js/console (str (:address analog)))
    infownds (map (fn [x] (if (= (:id x) (:id analog)) (assoc x :info infownd) x)) (:infownds @realty/app-state))

    ;tr1 (.log js/console (str "info counts = " (count (filter (fn[x] (if (= (:id device) (:id x)) true false)) (:infownds @shelters/app-state)))))
    infownds (if (> (count (filter (fn[x] (if (= (:id analog) (:id x)) true false)) (:infownds @realty/app-state))) 0) infownds (conj infownds {:id (:id analog) :info infownd}))

    delmarker (remove (fn [x] (if (= (.. x -unitid) (:id analog)) true false)) (:markers @realty/app-state))
    ]
    (jquery
      (fn []
        (-> marker
          (.addListener "click"
            (fn []              
              (.open infownd (:map @realty/app-state) marker)
            )
          )
        )
      )
    )
    (swap! realty/app-state assoc-in [:markers] (conj (:markers @realty/app-state) marker))
    

    (swap! realty/app-state assoc-in [:infownds] infownds)
  )
)



(defn updateMarkers []
  (let[
       tr1 (.log js/console (str "starting update markers: " (count (:calcanalogs @realty/app-state) ))) 
    ]
    (doall (map (fn [x] (.setMap x nil)) (:markers @realty/app-state)))
    (swap! realty/app-state assoc-in [:markers] [])
    (doall (map addMarker  (:calcanalogs (:object @realty/app-state))))
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
    (swap! realty/app-state assoc-in [:object :calcanalogs] (map-indexed map-analog (get response "calcanalogs")))
    (swap! realty/app-state assoc-in [:state] 0)
    (put! ch 45)
    ;;(.log js/console response)
  )
)


(defn OnClarifySuccess [response]
  (let[]
    ;(swap! realty/app-state assoc-in [:object :pricePerMetr] (get response "pricePerMetr"))
    ;(swap! realty/app-state assoc-in [:object :houseAvrgPrice] (get response "houseAvrgPrice"))
    ;(swap! realty/app-state assoc-in [:object :regionAvrgPrice] (get response "regionAvrgPrice"))
    ;(swap! realty/app-state assoc-in [:object :cityAvrgPrice] (get response "cityAvrgPrice"))
    (swap! realty/app-state assoc-in [:object :data] (get response "data"))
    ;(swap! realty/app-state assoc-in [:object :analogs] (map-indexed map-analog (get response "analogs")))
    (swap! realty/app-state assoc-in [:object :calcanalogs] (map-indexed map-analog (get response "calcanalogs")))
    (swap! realty/app-state assoc-in [:state] 0)
    ;;(.log js/console response)
  )
)

(defn de-map-analog [analog]
   [(:housetype analog) (:price analog) (:totalarea analog) (:city analog) (:lat analog) (:lon analog) (:roomsnum analog) (:floor analog) (:floors analog) (:address analog) (:district analog) (:repair analog) (:buildyear analog) (:livingsquare analog) (:kitchensquare analog) (:metrodistance analog) (:status analog) (:index analog)]
)

(defn clarifyprice []
  (let [
    filtered (filter (fn [x] (:isinclude x)) (:calcanalogs (:object @realty/app-state)))
    ]
    (POST (str settings/apipath  "clarify") {
      :handler OnClarifySuccess
      :error-handler error-handler
      ;; :headers {
      ;;   :token (str (:token (:token @shelters/app-state)))}
      :format :json
      :params { :analogs (map de-map-analog filtered) :params (:param (:object @realty/app-state)) :roomsnum (:roomsnum (:object @realty/app-state)) :storey (js/parseInt (:storey (:object @realty/app-state))) :storeysnum (js/parseInt (:storeysnum (:object @realty/app-state))) :housetype (:buildingtype (:object @realty/app-state)) :totalsquare (js/parseFloat (:totalsquare (:object @realty/app-state)))  :city (:city (:object @realty/app-state)) :microdistrict "" :repair (:repair (:object @realty/app-state)) :latitude (:lat (:object @realty/app-state)) :longitude (:lon (:object @realty/app-state)) :buildingyear (:buildingyear (:object @realty/app-state)) :livingsquare (js/parseFloat (:leavingsquare (:object @realty/app-state)))  :kitchensquare (js/parseFloat (:kitchensquare (:object @realty/app-state)))  :metrodistance (js/parseInt (:metrodistance (:object @realty/app-state))) :analogscount (count filtered)}}) 
  )
)

(defn getdata []
  (let [
    ;status (js/parseInt (:statuses (:filter @app-state)))
    ;user (:user (:filter @app-state))
    ]
    (swap! realty/app-state assoc-in [:state] 1)
    (swap! realty/app-state assoc-in [:object :calcanalogs] [])
    (swap! realty/app-state assoc-in [:object :analogs] [])
    (GET (str settings/apipath "estimate?totalsquare=" (:totalsquare (:object @realty/app-state)) "&repairRaw=" (:repair (:object @realty/app-state)) "&longitude=" (:lon (:object @realty/app-state)) "&latitude=" (:lat (:object @realty/app-state)) "&housetype=" (:buildingtype (:object @realty/app-state)) "&city=" (:city (:object @realty/app-state)) "&buildingyear=" (:buildingyear (:object @realty/app-state)) "&ceilingheight" (:ceilingheight (:object @realty/app-state)) "&storey=" (:storey (:object @realty/app-state)) "&storeysnum=" (:storeysnum (:object @realty/app-state)) "&metrodistance=" (:metrodistance (:object @realty/app-state)) "&leavingsquare=" (:leavingsquare (:object @realty/app-state)) "&kitchensquare=" (:kitchensquare (:object @realty/app-state)) "&roomsnum=" (:roomsnum (:object @realty/app-state)) "&analogscount=" (:analogscount (:object @realty/app-state)) "&param=" (:param (:object @realty/app-state))) {
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

(defn buildParams [data owner]
  (map
    (fn [param]
      (dom/option {:key (:id param)  :value (:id param)} (:name param))
    )
    params
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


(defn setcontrols [value]
  (case value
    46 (setDropDowns)
    43 (go
         (<! (timeout 100))
         (addsearchbox)
       )
    49 (go
         (<! (timeout 10))
         (openimagedialog)
       )
    44 (swap! realty/app-state assoc-in [:showmap] -1)
    45 (updateMarkers)
    56 (go
         (<! (timeout 5000))
         (downloadreport)
       )
    57 (go
         (<! (timeout 500))
         (swap! realty/app-state assoc-in [:isloading] false)
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
  (swap! realty/app-state assoc-in [:current] 
    "Object detail"
  )
  (set! (.-title js/document) "Price calculation")
  (setcontrols 46)
  (put! ch 43)
  (swap! realty/app-state assoc-in [:view] 1)
  (set! (.-title js/document) (str "Независимая оценка квартиры" (if (> (count (:address (:object @realty/app-state))) 0) ": ") (:address (:object @realty/app-state))))
)

(defcomponent addmodalreport [data owner]
  (render [_]
    (dom/div
      (dom/div {:id "reportModal" :className "modal fade" :role "dialog"}
        (dom/div {:className "modal-dialog modal-lg"} 
          ;;Modal content
          (dom/div {:className "modal-content"} 
            (dom/div {:className "modal-header"} 
              (b/button {:type "button" :className "close" :data-dismiss "modal"})
              (dom/h4 {:className "modal-title" :style {:text-align "center"}} "Отчет" )
            )
            (dom/div {:className "modal-body"}

              (dom/div {:className "panel panel-primary"}


                ;(dom/embed {:src "http://5.189.157.176:81/report_valuation.pdf" :width "300" :height "715"})
                (dom/iframe {:src "http://5.189.157.176:81/report_valuation.pdf#zoom=100" :style {:width "870px" :height "610px"} :frameborder "0"})
              )
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-12" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Закрыть")
                )
              )
            )
          )
        )
      )
    )
  )
)

(defcomponent addmodalimage [data owner]
  (render [_]
    (dom/div
      (dom/div {:id "imageModal" :className "modal fade" :role "dialog"}
        (dom/div {:className "modal-dialog"} 
          ;;Modal content
          (dom/div {:className "modal-content"} 
            (dom/div {:className "modal-header"} 
              (b/button {:type "button" :className "close" :data-dismiss "modal"})
              (dom/h4 {:className "modal-title"} "Объявление" )
            )
            (dom/div {:className "modal-body"}

              (dom/div {:className "panel panel-primary"}


                (dom/img {:style {:width "100%"} :src (str settings/screenpath (:selectedimage @realty/app-state))})
              )
            )
            (dom/div {:className "modal-footer"}
              (dom/div {:className "row"}
                (dom/div {:className "col-xs-12" :style {:text-align "center"}}
                  (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Закрыть")
                )
              )
            )
          )
        )
      )
    )
  )
)


(defn readurl [input]
  (if (not (nil? (.-files input)))
    (let [
      size (.-size (aget (.-files input) 0))
        
      ]
      (.log js/console (str "file size=" (.-size (aget (.-files input) 0))))
      (if (> size 200000)
        (let []
          (swap! realty/app-state assoc-in [:modalTitle]
            (str "Upload file error")
          )
          ;(swap! socialcore/app-state assoc-in [:modalText] "File size should be less than 200KBytes")
          ;(showmessage)
        )
        (let []
          (swap! realty/app-state assoc-in [:isloading] true)
          (put! ch 56)
        )  
      )
    )
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

                    marker-options (clj->js {"position" (google.maps.LatLng. (.lat (.. e -latLng)), (.lng (.. e -latLng))) "map" map })
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
    (let [
        ;tr1 (.log js/console (clj->js (:param (:object @data))))
      ]
      (dom/div {:style {:padding-top "10px"}}
        ;(om/build realty/website-view realty/app-state {})
        (dom/div {:id "header"}
          (dom/div {:className "header_ramka"}
            (dom/table {:className "tab_header" :cellPadding "0" :cellSpacing "0"}
              (dom/tbody
                (dom/tr
                  (dom/td {:style {:text-align "left" :width "300px"}}
                    (dom/div {:id "logo"}
                      (dom/a {:href "https://f-case.ru/"}
                        (dom/img {:src "https://f-case.ru/content/photo/full/20140610101509.png" :alt "Компания «FinCase»" :title "Компания «FinCase»"})
                      )
                    )
                  )
                  (dom/td
                    (dom/div {:style {:border-left "1px solid #ccc" :padding-left "35px" :width "200px" :font-size "18px" :color "#666" :line-height "16px" :position "relative" :top "4px"}} "Инновации на страже Ваших Интересов")
                  )
                  (dom/td {:style {:text-align "right"}}
                    (dom/div {:className "tel_top"}
                      "7 "
                      (dom/span "(499)")
                      " 653-61-53"
                    )
                  )
                )
              )
            )
          )

        )
        (dom/h3 {:style {:text-align "center"}}
          (dom/i {:className "fa fa-cube"})
          (str "Параметры квартиры")
        )
        (om/build addmodalimage data {})
        (om/build addmodalreport data {})
        (dom/div {:className "row" :style {:width "100%" :margin-left "15px" :margin-right "15px"}}


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Общая площадь:")
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "totalsquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:totalsquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Жилая площадь:")
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "leavingsquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:leavingsquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "Площадь кухни:")
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "kitchensquare" :class "form-control" :type "number" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:kitchensquare (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Тип ремонта:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
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
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Город:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
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
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Тип дома:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
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
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Год постройки:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "buildingyear"  :class "form-control" :required true :type "number" :step "1" :onChange (fn [e] (handleChange e)) :value (:buildingyear (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Количество комнат:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "roomsnum" :type "number" :min 1 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:roomsnum (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            ;; (dom/div {:className "row"}
            ;;   (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
            ;;     (dom/h5 (str "Высота потолков:"))
            ;;   )
            ;;   (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
            ;;     (dom/input {:id "ceilingheight" :class "form-control" :type "number" :step "0.1" :onChange (fn [e] (handleChange e)) :value (:ceilingheight (:object @data))})
            ;;   )
            ;;   (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
            ;;     (dom/span {:className "asterisk"} "*")
            ;;   )
            ;; )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Этаж:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "storey" :type "number" :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:storey (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Этажность дома:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "storeysnum" :type "number" :min 1 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:storeysnum (:object @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Расстояние до метро:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "metrodistance" :type "number" :min 10 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:metrodistance (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Количество аналогов:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "analogscount" :type "number" :min 10 :max 100 :class "form-control" :step "1" :onChange (fn [e] (handleChange e)) :value (:analogscount (:object @data))})
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-sm-2 col-sm-offset-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "Параметры:"))
              )
              (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (omdom/select #js {:id "param"
                                   :value (clj->js (:param (:object @data)))
                                   :multiple true
                                   :className "selectpicker"
                                   :data-width "100%"
                                   :data-style "btn-default"
                                   :data-show-subtext "false"
                                   :data-live-search "true"
                                   :onChange #(handle-change % owner)
                                   }                
                  (buildParams data owner)
                )
              )
              (dom/div {:className "col-xs-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "30px" :text-align "left"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-xs-3 col-xs-offset-0 col-ms-2 col-sm-offset-3"}
                (dom/input {:type "file" :style {:margin-top "50px" :width "220px"} :onChange (fn [e] (readurl (.. e -target))) :name "file" :value ""})
              )
              (if (= true (:isloading @data))
                (dom/div {:className "col-xs-8 col-sm-4" :style {:margin-top "20px"}}
                  (dom/div {:className "loader"})
                )
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

            (dom/div {:className "row" :style {:margin-left "15px" :margin-right "35px" :padding-top "0px" :height "400px" :display "block"}}
              (dom/div  {:className "col-xs-12" :id "map" :style {:margin-top "0px" :height "100%"}})
            )


            (dom/div {:className "row" :style {:padding-top "10px" :display "block"}}
              (dom/div {:className "col-xs-4" :style {:text-align "center"}}
                (b/button {:className (if (= (:state @data) 0) "btn btn-primary" "btn btn-primary m-progress") :onClick (fn [e] (getdata))} "Получить стоимость")
              )
              (dom/div {:className "col-xs-4" :style {:text-align "center"}}
                (if (> (count (:calcanalogs (:object @data))) 0)
                  (b/button {:className (if (= (:state @data) 0) "btn btn-primary" "btn btn-primary m-progress") :onClick (fn [e] (clarifyprice))} "Уточнить стоимость")
                )
              )
              (dom/div {:className "col-xs-4" :style {:text-align "center"}}
                (b/button {:className (if (= (:state @data) 0) "btn btn-primary" "btn btn-primary m-progress") :onClick (fn [e] (openreportdialog))} "Аналитическая отчетность")
              )
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
        (om/build showanalogs-view  data {:state {:key "calcanalogs"}})

        (if (> (count (:analogs (:object @data))) 0)

          (dom/div {:className "panel panel-primary"}
            (dom/div {:className "panel panel-heading" :style {:text-align "center" :margin-bottom "0px"}}
                "Все релевантные аналоги"
            )
            (dom/div {:className "panel panel-body"}
                (om/build showanalogs-view  data {:state {:key "analogs"}})
            )
          )
        )

        

        (dom/div {:id "footer" :style {:margin-top "20px"}}
          (dom/div {:id "footer2"}
            (dom/table {:className "footer_tab" :cellSpacing "0" :cellPadding "0" :style {:font "20px 'PT Sans Narrow', sans-serif"}}
              (dom/tbody
                (dom/tr
                  (dom/td {:style {:width "500px" :padding-top "20px"}} "2018 ©" 

                    (dom/b "Компания «FinCase»")
                  )
                )
                (dom/tr
                  (dom/td {:style {:width "500px"}}
                    "Адрес: Москва, ул. Большая Полянка, 2/10 стр1"

                  )
                )
                (dom/tr
                  (dom/td {:style {:width "500px"}}
                    "Тел.: 7 (499) 653-61-53"
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
