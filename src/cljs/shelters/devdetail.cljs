(ns shelters.devdetail
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [shelters.core :as shelters]
            [ajax.core :refer [GET POST PUT DELETE]]
            [clojure.string :as str]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]
            [om.dom :as omdom :include-macros true]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! timeout]]
            [om-bootstrap.input :as i]
            [cljs-time.core :as tm]
            [cljs-time.format :as tf]
            [shelters.settings :as settings]
  )
  (:import goog.History)
)

(def jquery (js* "$"))

(enable-console-print!)

(def ch (chan (dropping-buffer 2)))

(def iconBase "images/")

(defonce app-state (atom  {:device {} :showmap 1 :marker nil :isinsert false :view 1 :current "Device Detail"} ))

(defn comp-groups
  [group1 group2]
  ;(.log js/console group1)
  ;(.log js/console group2)
  (if (> (compare (:name group1) (:name group2)) 0)
      false
      true
  )
)

(defn drop-nth [n coll]
   (keep-indexed #(if (not= %1 n) %2) coll))


(defn handle-chkbsend-change [e]
  (let [
      id (str/join (drop 9 (.. e -currentTarget -id)))
      groups (:groups (:device @app-state))
      newgroups (if (= true (.. e -currentTarget -checked)) (conj groups id) (remove (fn [x] (if (= x id) true false)) groups))
    ]
    (.stopPropagation e)
    (.stopImmediatePropagation (.. e -nativeEvent) )
    (swap! app-state assoc-in [:device :groups] newgroups)
  )
)


(defn handleChange [e]
  ;(.log js/console (.. e -nativeEvent -target)  )  
  ;(.log js/console (.. e -nativeEvent -target -step))
  (swap! app-state assoc-in [:device (keyword (.. e -nativeEvent -target -id))] (if (= "" (.. e -nativeEvent -target -step)) (.. e -nativeEvent -target -value) (js/parseFloat (.. e -nativeEvent -target -value))))
)


(defn OnDeleteUnitError [response]
  (let [     
      
    ]

  )
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnDeleteUnitSuccess [response]
  (let [
      units (:devices @shelters/app-state)
      newunits (remove (fn [unit] (if (= (:id unit) (:id (:device @app-state)) ) true false)) units)
    ]
    (swap! shelters/app-state assoc-in [:devices] newunits)
    ;(shelters/goDashboard "")
    (js/window.history.back)
  )
)

(defn OnUpdateUnitError [response]
  (let [     
    ]

  )
  ;;(.log js/console (str  (get (first response)  "Title") ))
)


(defn OnUpdateUnitSuccess [response]
  (let [
      units (:devices @shelters/app-state)
      delunit (remove (fn [unit] (if (= (:id unit) (:id (:device @app-state)) ) true false  )) units)
      addunit (conj delunit (:device @app-state)) 
    ]
    (swap! shelters/app-state assoc-in [:devices] addunit)
    ;(shelters/goDashboard nil)
    (js/window.history.back)
  )
)


(defn deleteUnit []
  (DELETE (str settings/apipath  "deleteUnit?unitId=" (:id (:device @app-state))) {
    :handler OnDeleteUnitSuccess
    :error-handler OnDeleteUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json})
)



(defn updateUnit []
  (PUT (str settings/apipath  "updateUnit") {
    :handler OnUpdateUnitSuccess
    :error-handler OnUpdateUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:device @app-state)) :controllerId (:controller (:device @app-state)) :name (:name (:device @app-state)) :parentGroups (:groups (:device @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip (:ip (:device @app-state)) :port (:port (:device @app-state)) :latitude (:lat (:device @app-state)) :longitude (:lon (:device @app-state)) :details [{:key "address" :value (:address (:device @app-state))} {:key "contact1" :value (nth (:contacts (:device @app-state)) 0)}  {:key "contact2" :value (nth (:contacts (:device @app-state)) 1)}]}})
)


(defn OnCreateUnitError [response]
  (let [     
    ]

  )
  ;; TO-DO: Delete Trip from Core
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn map-unitindication [indication]
  (let [
    ;tr1 (.log js/console (str "update=" (get indication "lastUpdateTime")))
    ]
    {:id (get indication "indicationId") :isok (get indication "isOk") :value (get indication "value") :lastupdate (tf/parse shelters/custom-formatter3 (get indication "lastUpdateTime"))}
  )
)

(defn map-unit [unit]
  (let [
    controller (str (get unit "controllerId"))
    name (if (nil? (get unit "name")) controller (get unit "name"))
    port (get unit "port")
    port (if (nil? port) 5050 port)
    ip (get unit "ip")
    if (if (nil? ip) "1.1.1.1" ip)
    status (case (get unit "status") "Normal" 0 3)
    lat (get unit "latitude")
    lon (get unit "longitude")
    groups (get unit "parentGroups")
    unitid (str (get unit "unitId"))    
    address (get (first (filter (fn [x] (if (= (get x "key") "address") true false)) (get unit "details"))) "value" )
    phone (get (first (filter (fn [x] (if (= (get x "key") "phone") true false)) (get unit "details"))) "value" )

    contact1 (get (first (filter (fn [x] (if (= (get x "key") "contact1") true false)) (get unit "details"))) "value")
    contact2 (get (first (filter (fn [x] (if (= (get x "key") "contact2") true false)) (get unit "details"))) "value")


    indications (map map-unitindication (get unit "indications"))
    indications (if (> (count indications) 0) indications [{:id 1, :isok false, :value "open"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}])

    ;tr1 (.log js/console (str  "username=" username ))
    result {:id unitid :controller controller :name name :status status :address address :ip ip :lat lat :lon lon :port port :groups groups :indications (if (nil? indications) [] indications) :contacts [(first (filter (fn [x] (if (= contact1 (:id x)) true false)) (:contacts @shelters/app-state))) (first (filter (fn [x] (if (= contact1 (:id x)) true false)) (:contacts @shelters/app-state)))]}
    ]
    ;
    result
  )
)

(defn OnCreateUnitSuccess [response]
  (let [
      unit (map-unit response)
      units (:devices @shelters/app-state)
      addunit (conj units unit)
    ]
    (swap! shelters/app-state assoc-in [:devices] addunit)
    ;(shelters/goDashboard "")
    (js/window.history.back)
  )
)

(defn createUnit []
  (POST (str settings/apipath  "addUnit") {
    :handler OnCreateUnitSuccess
    :error-handler OnCreateUnitError
    :headers {
      :token (str (:token (:token @shelters/app-state)))}
    :format :json
    :params {:unitId (:id (:device @app-state)) :controllerId (:controller (:device @app-state)) :indications (:indications (:device @app-state)) :name (:name (:device @app-state)) :parentGroups (:groups (:device @app-state)) :owners [] :responsibleUser (:userid (:token @shelters/app-state)) :unitType 1 :ip (:ip (:device @app-state)) :port (:port (:device @app-state)) :latitude (:lat (:device @app-state)) :longitude (:lon (:device @app-state)) :details [{:key "address" :value (:address (:device @app-state))}  {:key "contact1" :value (nth (:contacts (:device @app-state)) 0)} {:key "contact2" :value (nth (:contacts (:device @app-state)) 1)}]}})
)


(defn onDropDownChange [id value]
  (let [
    newid (js/parseInt (subs id 7))


    ;addcontact (first (filter (fn [x] (if (= x value) true false)) (:contacts @shelters/app-state)))

    tr1 (.log js/console (str "id=" id " newid=" newid " value=" value))
    newcontacts (conj (take newid (:contacts (:device @app-state))) value)

    newcontacts (flatten (if (> (count (:contacts (:device @app-state))) (+ newid 1)) (reverse (conj newcontacts (drop (+ newid 1) (:contacts (:device @app-state))))) (reverse newcontacts)))
    ]
    (swap! app-state assoc-in [:device :contacts] newcontacts)
  )
  ;(.log js/console (str "id=" id " value=" value))  
)


(defn setContactsDropDown []
  (doall
    (map (fn [num]
      (let []
        (jquery
          (fn []
            (-> (jquery (str "#contact" num))
              (.selectpicker {})
            )
          )
        )
        (jquery
          (fn []
            (-> (jquery (str "#contact" num))
              (.selectpicker "val" (nth (:contacts (:device @app-state)) num))
              (.on "change"
                (fn [e]
                  (
                    onDropDownChange (.. e -target -id) (.. e -target -value)
                  )
                )
              )
            )
          )
        )
      ))
      (range 2) 
    )
  ) 
)

(defn addplace [place]
  (let [
    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})

    ;tr1 (.log js/console place)
    marker-options (clj->js {"position" (.. place -geometry -location) "map" (:map @app-state) "icon" image "title" (.. place -name)})

    marker (js/google.maps.Marker. marker-options)
    ]
    (.panTo (:map @app-state) (.. place -geometry -location))

    (if (not (nil? (:marker @app-state))) (.setMap (:marker @app-state) nil))
    (swap! app-state assoc-in [:marker] marker)
    (swap! app-state assoc-in [:device :lat] (.lat (.. place -geometry -location)))
    (swap! app-state assoc-in [:device :lon] (.lng (.. place -geometry -location)))
    (swap! app-state assoc-in [:device :address] (.. place -formatted_address))
  )
)


(defn addsearchbox []
  (let [
    size (js/google.maps.Size. 48 48)
    image (clj->js {:url (str iconBase "green_point.ico") :scaledSize size})
    marker-options (clj->js {"position" (google.maps.LatLng. (:lat (:device @app-state)), (:lon (:device @app-state))) "map" (:map @app-state) "icon" image "title" (:name (:device @app-state))})

    marker (js/google.maps.Marker. marker-options)
    
    ;;Create the search box and link it to the UI element.
    input (. js/document (getElementById "pac-input"))
    searchbox (js/google.maps.places.SearchBox. input)
    ;tr1 (.log js/console input)
    ]
    (swap! app-state assoc-in [:marker] marker)
    (.push (aget (.-controls (:map @app-state)) 1) input)
    (if (not (nil? (:marker @app-state))) (.setMap (:marker @app-state) (:map @app-state)))
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
    (.panTo (:map @app-state) (google.maps.LatLng. (:lat (:device @app-state)), (:lon (:device @app-state))))    
  )
)


(defn setNewUnitValue [key val]
  (swap! app-state assoc-in [(keyword key)] val)
)



(defn setcontrols [value]
  (case value
    46 (setContactsDropDown)
    43 (go
         (<! (timeout 100))
         (addsearchbox)
       )

    44 (swap! app-state assoc-in [:showmap] -1)
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

(defn setUnit []
  (let [
        users (:users @shelters/app-state)
        user (first (filter (fn [user] (if (= (:login @app-state) (:login user)  )  true false)) (:users @shelters/app-state )))
        ]
    (swap! app-state assoc-in [:login ]  (:login user) ) 
    (swap! app-state assoc-in [:role ]  (:role user) ) 
    (swap! app-state assoc-in [:password] (:password user) )
  )
)




(defn OnError [response]
  (let [     
      newdata { :error (get (:response response)  "error") }
    ]
    (.log js/console (str  response )) 
    
  )
  
  
)


(defn getUnitDetail []
  ;(.log js/console (str "token: " " " (:token  (first (:token @t5pcore/app-state)))       ))
  (if
    (and 
      (not= (:login @app-state) nil)
      (not= (:login @app-state) "")
    )
    (setUnit)
  
  )
)

(defn handleFromChange [e]
  ;;(.log js/console e  )  
  ;(.log js/console "The change ....")

)


(defn onMount [data]
  (swap! app-state assoc-in [:current] 
    "Unit Detail"
  )
  (set! (.-title js/document) "Unit Detail")
  (getUnitDetail)
  (setcontrols 46)
  (put! ch 43)
  (swap! shelters/app-state assoc-in [:view] 7)
  (set! (.-title js/document) (str "יחידה:" (:name (:device @app-state))))
)


(defn handle-change [e owner]
  ;(.log js/console e)
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  )
)


(defn buildContactList [data owner]
  (map
    (fn [text]
      (dom/option {:key (:userid text) :data-subtext (str (:firstname text) " " (:lastname text)) :value (:userid text)
                    :onChange #(handle-change % owner)} (:login text))
    )
    (:users @shelters/app-state )
  )
)

(defcomponent parentgroups-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item]
        (let [            
            isparent (if (and (nil? (:groups (:device @app-state)))) false (if (> (.indexOf (:groups (:device @app-state)) (:id item)) -1) true false))
          ]
          (dom/form
            (dom/label
              (:name item)
              (dom/input {:id (str "chckgroup" (:id item)) :type "checkbox" :checked isparent :onChange (fn [e] (handle-chkbsend-change e ))})
            )
          )
        )
      )
      (sort (comp comp-groups) (:groups @shelters/app-state)))
    )
  )

)

(defcomponent showcontacts-view [data owner]
  (render
    [_]
    (dom/div
      (map (fn [item num]
        (dom/div
          (dom/div {:className "row":style {:margin-top "5px"}}
            ;(dom/div {:className "col-xs-5"})
            (dom/div {:className "col-xs-3"} (dom/h5 (str "איש קשר " (+ num 1) ":")))
            (dom/div {:className "col-md-3" :style {:padding-top "7px"}}
              (omdom/select #js {:id (str "contact" num)
                                 :className "selectpicker"
                                 :data-show-subtext "true"
                                 :data-live-search "true"
                                 :onChange #(handle-change % owner)
                                 }
                (buildContactList data owner)
              )
            )
          )
          ;; (dom/b
          ;;   (dom/i {:className "fa fa-user"} (:name item))
          ;; )
          ;; (dom/p (:tel item))
        )
      )
      (:contacts (:device @app-state)) (range))
    )
  )
)

(defn createMap []
  (let [
      map-canvas (. js/document (getElementById "map"))
      map-options (clj->js {"center" {:lat (:lat (:selectedcenter @shelters/app-state)) :lng (:lon (:selectedcenter @shelters/app-state))} "zoom" 12})
      map (js/google.maps.Map. map-canvas map-options)
      tr1 (swap! app-state assoc-in [:map] map)
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
            map (:map @app-state)
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
                    (if (not (nil? (:marker @app-state))) (.setMap (:marker @app-state) nil))
                    ;(.log js/console (str "LatLng=" (.. e -latLng)))

                    (swap! app-state assoc-in [:device :lat] (.lat (.. e -latLng)))
                    (swap! app-state assoc-in [:device :lon] (.lng (.. e -latLng)))
                    (swap! app-state assoc-in [:marker] marker)
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
      (dom/div {:style {:padding-top "70px"}}
        (om/build shelters/website-view shelters/app-state {})
        
        (dom/h3 {:style {:text-align "center"}}
          (dom/i {:className "fa fa-cube"})
          (if (:isinsert @data)
            (str "הוספת יחידה חדשה")
            (str "פרטי היחידה - " (:controller (:device @app-state)) )
          )
        )
        
        (dom/div {:className "col-xs-4"})
        (dom/div {:className "col-xs-4"}

            (if (:isinsert @data)
              (dom/div {:className "row"}
                (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                  (dom/h5 "מזהה יחידה:"))

                (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                  (dom/input {:id "controller" :type "text" :readOnly (if (:isinsert @data) false true) :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:controller (:device @data))}
                  )
                )
                (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                  (dom/span {:className "asterisk"} "*")
                )
              )
            )


            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 "שם יחידה:")
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "name" :type "text" :style {:width "100%"} :required true :onChange (fn [e] (handleChange e)) :value (:name (:device @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )



            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "כתובת: "))
              )
              (dom/div {:className "col-md-8" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "address" :style {:width "100%"} :type "text" :required true :onChange (fn [e] (handleChange e)) :value (:address (:device @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "כתובת ה - IP:"))
              )
              (dom/div {:className "col-md-4" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "ip" :style {:width "100%"} :type "text" :required true :onChange (fn [e] (handleChange e)) :value (:ip (:device @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}       
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/div {:className "row"}
              (dom/div {:className "col-md-3 offset-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/h5 (str "מספר יציאת IP:"))
              )
              (dom/div {:className "col-md-2" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px"}}
                (dom/input {:id "port" :style {:width "100%"} :type "number" :step "1" :onChange (fn [e] (handleChange e)) :required true :value (:port (:device @data))})
              )
              (dom/div {:className "col-md-1" :style {:margin-top "4px" :padding-right "0px" :padding-left "0px" :text-align "right"}}
                (dom/span {:className "asterisk"} "*")
              )
            )

            (dom/h5 {:style {:display:inline true}} "קו רוחב: "
               (:lat (:device @data))
               ;(dom/input {:id "lat" :type "number" :step "0.00001" :onChange (fn [e] (handleChange e)) :value (:lat (:device @data))} )
            )
            (dom/h5 {:style {:display:inline true}} "קו האורך: "
               (:lon (:device @data))
               ;(dom/input {:id "lon" :type "number" :step "0.00001" :onChange (fn [e] (handleChange e)) :value (:lon (:device @data))} )
            )
            (dom/input {:id "pac-input" :className "controls" :type "text" :placeholder "תיבת חיפוש" })

            (dom/div {:style {:margin-bottom "10px"}}
              (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (swap! app-state assoc-in [:showmap] (- (:showmap @data))))} (case (:showmap @data) -1 "Show map" "Hide Map"))
            )
            (dom/div {:className "row maprow" :style {:padding-top "0px" :height "400px" :display (case (:showmap @data) -1 "none" "block")}}
              (dom/div  {:className "col-12 col-sm-12" :id "map" :style {:margin-top "0px" :height "100%"}})
              ;(b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (addMarkers))} "Add marker")
            )
            (dom/h4
              (dom/i {:className "fa fa-phone"} "אנשים קשר")
            )
            (om/build showcontacts-view data {})

            ;; (dom/div
            ;;   (b/button {:className "btn btn-primary colbtn" :onClick (fn [e] (aset js/window "location" (str "#/groupstounit/" (:id (:device @data)))))} "Assign to groups")
            ;; )
            ;(om/build parentgroups-view data {})

            (dom/div {:style {:margin-top "10px"}}

              (b/button {:className "btn btn-default" :style {:margin "5px"} :disabled? (or (and (:isinsert @app-state) (> (count (filter (fn [x] (if (= (:controller x) (:controller (:device @data))) true false)) (:devices @shelters/app-state))) 0)) (< (count (:controller (:device @data))) 1)  (< (count (:address (:device @data))) 1) (< (count (:ip (:device @data))) 1) (< (:port (:device @data)) 1) (< (count (:name (:device @data))) 1) ) :onClick (fn [e] (if (:isinsert @app-state) (createUnit) (updateUnit)) )} (if (:isinsert @app-state) "Insert" "Update"))
              (b/button {:className "btn btn-danger" :style {:display (if (:isinsert @app-state) "none" "inline") :margin "5px"} :onClick (fn [e] (deleteUnit))} "Delete")

              (b/button {:className "btn btn-info" :style {:margin "5px"} :onClick (fn [e]
                                        ;(shelters/goDashboard e)
                                                                                      (js/window.history.back)
                                                                                      )  } "Cancel"
              )
            )

        )

        (dom/div {:className "col-xs-9"}
	)
      )
    )
  )
)





(sec/defroute devdetail-page "/devdetail/:devid" [devid]
  (let[
      dev (first (filter (fn [x] (if (= (str devid) (:id x)) true false)) (:devices @shelters/app-state)))       
      ;tr2 (.log js/console "hjkhkh")
    ]
    (swap! app-state assoc-in [:showmap] 1)
    (swap! app-state assoc-in [:device] dev )
    (swap! app-state assoc-in [:isinsert] false)
    (swap! shelters/app-state assoc-in [:view] 7)
    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)


(sec/defroute devdetail-new-page "/devdetail" {}
  ( let []
    (swap! app-state assoc-in [:device]  {:id "" :ip "1.1.1.1" :port 5050 :lat (:lat (:selectedcenter @shelters/app-state)) :lon (:lon (:selectedcenter @shelters/app-state)) :contacts [(nth (:contacts @shelters/app-state) 0) (nth (:contacts @shelters/app-state) 1)] :indications [{:id 1, :isok true, :value "closed"} {:id 2, :isok true, :value "closed"} {:id 3, :isok true, :value "closed"} {:id 4, :isok true, :value "idle"} {:id 5, :isok true, :value "enabled"} {:id 6, :isok true, :value "normal"} {:id 7, :isok true, :value "normal"} {:id 8, :isok true, :value "idle"} {:id 9, :isok true, :value ""} {:id 10, :isok true, :value "2017-12-31_18:53:05.224"} {:id 12, :isok true, :value "normal"}]})
    (swap! app-state assoc-in [:isinsert]  true )
    (swap! shelters/app-state assoc-in [:view] 7) 
    (swap! app-state assoc-in [:showmap] 1)
    ;(swap! app-state assoc-in [:group ]  "group" ) 
    ;(swap! app-state assoc-in [:password] "" )
    (om/root devdetail-page-view
             app-state
             {:target (. js/document (getElementById "app"))})

  )
)
