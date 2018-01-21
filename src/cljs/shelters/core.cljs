(ns shelters.core
  (:use [net.unit8.tower :only [t]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [ajax.core :refer [GET PUT POST]]
            [om.dom :as omdom :include-macros true]
            [cljs-time.core :as tc]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as te]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout close!]]
            [om-bootstrap.button :as b]
            [shelters.settings :as settings]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
  )
  (:import goog.History)
)

(enable-console-print!)

(def indicators ["lockState" "doorState" "lastCommunication" "batteryState" "tamper" "communicationStatus"])

(def custom-formatter (tf/formatter "dd/MM/yyyy"))
(def custom-formatter1 (tf/formatter "dd/MM/yyyy HH:mm:ss"))
(def custom-formatter2 (tf/formatter "yyyy-MM-dd HH:mm:ss"))
(def custom-formatter3 (tf/formatter "yyyy-MM-dd HH:mm:ss"))
(def ch (chan (dropping-buffer 2)))

(defn tableheight [count] 
  (+ 0 (* 27 (min count 10)))
)

(def main-tconfig
  {:dictionary ; Map or named resource containing map
    {:he 
      {
        :alerts
        {
          :Alert                    "התרא"
          :notification             "התרא"
          :Failure                  "תקלת"
          :alert                    "תקלת"
          :Unknown                  "תקלת"
          :New                      "פתוח"
          :new                      "פתוח"
          :Closed                   "סגור"
          :closed                   "סגור"
          :Accepted                 "בטיפול"
          :seen                     "בטיפול"
        }
        :indicators
        {           
          :lockState                "בריח"
          :lockStateok              "סגור"
          :lockStatefail            "פתוח"

          :doorState                "דלת"
          :doorStateok              "סגור"
          :doorStatefail            "פתוח"

          :lastCommunication        "ארון תקשורת"
          :lastCommunicationok      "כן"
          :lastCommunicationfail    "לא"

          :batteryState             "סוללה"
          :batteryStateok           "ok"
          :batteryStatefail         "fail"

          :tamper                   "גלאי"
          :tamperok                 "אין אירוע"
          :tamperfail               "יש אירוע"

          :communicationStatus      "איבוד תקשורת"
          :communicationStatusok    "יש תקשור"
          :communicationStatusfail  "אין תקשור"

        }
        :commands
        {           
          :unlock                 "פתח מנעל"
          :getUnitStatus          "בדיקת תקשורת"
        }
        :missing  "missing"
      }
    }
   :dev-mode? true ; Set to true for auto dictionary reloading
   :fallback-locale :he
  }
)

;;{:id "1602323" :bcity 1 :name "tek aviv sfs" :status 3 :address "נחלת בנימין 24-26, תל אביב יפו, ישראל" :lat 32.08088 :lon 34.78057 :contacts [{:tel "1235689" :name "Alexey"} {:tel "7879787" :name "Oleg"}]} {:id "2" :city 2 :name "The second device" :status 2 :address "נחלת בנימין 243-256, תל אביב יפו, ישראל" :lat 31.92933 :lon 34.79868 }

(defonce app-state (atom {:ws_channel nil :selectedusers [] :markers [] :selectedstatus -1 :map nil :state 0 :selectedunits [] :search "" :isalert false :isnotification false :user {:role "admin"} :selectedcenter {:lat 31.9321 :lon 34.8013},

;:contacts [{:id "1" :name "Alexey" :phone "+79175134855" :email "zorchenkov@gmail.com"} {:id "2" :name "yulia" :phone "+9721112255" :email "yulia@gmail.com"} {:id "3" :name "Oleg" :phone "+8613946174558" :email "oleg@yahoo.com"}]

:statuses [{:id 1 :name "הכל" :eng "All"} {:id 2 :name "בטיפול" :eng "Accepted"} {:id 3 :name "פתוח" :eng "New"} {:id 4 :name  "סגור" :eng "Closed"}]
:alerts [] ;; [{:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }


           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }


           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13") }

           ;;  {:unitid "e8ebaeb8-5f77-47de-9085-6ad033efc621" :userid "1ecc9d4b-6766-4109-94a5-07885e2e6ac6" :status "Failure" :id "67867887687" :open (tf/parse custom-formatter2 "11/01/2017 09:12:13")}
           ;;  ]
;[{:id 1001 :type "error" :text "In device theer is an error"} {:id 1002 :type "common" :text "In That device no error"}]
:notifications [] ;[{:id 101 :type "error" :text "This device works incorrect"} {:id 102 :type "common" :text "That device working properly"}]
:devices [] :users []}))





(def jquery (js* "$"))


(defn comp-alerts
  [alert1 alert2]
  (let [
    unit1 (first (filter (fn [x] (if (= (:id x) (:unitid alert1)) true false)) (:devices @app-state)))

    user1 (first (filter (fn [x] (if (= (:userid x) (:userid alert1)) true false)) (:users @app-state)))

    indicator1 (str (t :he main-tconfig (keyword (str "indicators/" (:name (first (filter (fn [x] (if (= (:id x) (:sensorid alert1)) true false))  (:indications @app-state))))))))

    unit2 (first (filter (fn [x] (if (= (:id x) (:unitid alert2)) true false)) (:devices @app-state)))

    user2 (first (filter (fn [x] (if (= (:userid x) (:userid alert2)) true false)) (:users @app-state)))

    indicator2 (str (t :he main-tconfig (keyword (str "indicators/" (:name (first (filter (fn [x] (if (= (:id x) (:sensorid alert2)) true false))  (:indications @app-state))))))))
    ]
    (case (:sort-alerts @app-state)
      1 (if (> (:id alert1) (:id alert2))
          false
          true
        )

      2 (if (> (:id alert1) (:id alert2))
          true
          false
        )


      3 (if (> (compare (:controller unit1) (:controller unit2)) 0)
          false
          true
        )

      4 (if (> (compare (:controller unit1) (:controller unit2)) 0)
          true
          false
        )

      5 (if (> (compare (:name unit1) (:name unit2)) 0)
          false
          true
        )

      6 (if (> (compare (:name unit1) (:name unit2)) 0)
          true
          false
        )


      7 (if (> (compare (:address unit1) (:address unit2)) 0)
          false
          true
        )

      8 (if (> (compare (:address unit1) (:address unit2)) 0)
          true
          false
        )

      9 (if (> (compare indicator1 indicator2) 0)
          false
          true
        )

      10 (if (> (compare indicator1 indicator2) 0)
          true
          false
        )

      11 (if (> (te/to-long (:open alert1)) (te/to-long (:open alert2)))
          false
          true
        )

      12 (if (> (te/to-long (:open alert1)) (te/to-long (:open alert2)))
          true
          false
        )

      13 (if (> (te/to-long (:close alert1)) (te/to-long (:close alert2)))
          false
          true
        )

      14 (if (> (te/to-long (:close alert1)) (te/to-long (:close alert2)))
          true
          false
        )

      15 (if (> (compare (str (:firstname user1) " " (:lastname user1)) (str (:firstname user2) " " (:lastname user2))) 0)
          false
          true
        )

      16 (if (> (compare (str (:firstname user1) " " (:lastname user1)) (str (:firstname user2) " " (:lastname user2))) 0)
          true
          false
        )


      17 (if (> (compare (t :he main-tconfig (keyword (str "alerts/" (:status alert1))))  (t :he main-tconfig (keyword (str "alerts/" (:status alert2))))) 0)
          false
          true
        )

      18 (if (> (compare (t :he main-tconfig (keyword (str "alerts/" (:status alert1)))) (t :he main-tconfig (keyword (str "alerts/" (:status alert2))))) 0)
          true
          false
        )


      (if (> (compare (:name unit1) (:name unit2)) 0)
          false
          true
      )
    )
  )
)


(defn setVersionInfo [info]
  (swap! app-state assoc-in [:verinfo] 
    (:info info)
  )

  (swap! app-state assoc-in [:versionTitle]
    (str "Информация о текущей версии")
  ) 

  (swap! app-state assoc-in [:versionText]
    (str (:info info))
  ) 

  ;;(.log js/console (str  "In setLoginError" (:error error) ))
  (jquery
    (fn []
      (-> (jquery "#versioninfoModal")
        (.modal)
      )
    )
  )
)

(defn handleChange [e]
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -value))
)



(defn onVersionInfo []
  (let [     
      newdata { :info "Global Asset Management System пользовательский интерфейс обновлен 01.09.2017 09:28" }
    ]
   
    (setVersionInfo newdata)
  )
  ;(.log js/console (str  response ))
)

(defn addVersionInfo []
  (dom/div
    (dom/div {:id "versioninfoModal" :className "modal fade" :role "dialog"}
      (dom/div {:className "modal-dialog"} 
        ;;Modal content
        (dom/div {:className "modal-content"} 
          (dom/div {:className "modal-header"} 
                   (b/button {:type "button" :className "close" :data-dismiss "modal"})
                   (dom/h4 {:className "modal-title"} (:versionTitle @app-state) )
                   )
          (dom/div {:className "modal-body"}
                   (dom/p (:versionText @app-state))
                   )
          (dom/div {:className "modal-footer"}
                   (b/button {:type "button" :className "btn btn-default" :data-dismiss "modal"} "Close")
          )
        )
      )
    )
  )
)

(defn doswaps []
  (let [a (rand-int 26)
        b (rand-int 26)
        c (rand-int 26)
    ]
    (swap! app-state assoc-in [:fake] (str a b c))
  )
)

(defn split-thousands [n-str]
  (let [index (str/index-of n-str ".")
        lstr (subs n-str 0 (if (nil? index) (count n-str) index))
        rstr (if (nil? index) "" (subs n-str index)) 
        splitstr (->> lstr
          reverse
          (partition 3 3 [])
          (map reverse)
          reverse
          (map #(apply str %))
          (str/join " "))
    ]
    (str splitstr rstr)
  )
)

(defn closesocket []
  (if (not (nil? (:ws_channel @app-state))) (close! (:ws_channel @app-state)))
  ;(close! (:ws_channel @app-state))
  (swap! app-state assoc-in [:ws_channel] nil)
)

(defn doLogout []
  ;(swap! app-state assoc-in [:view] 0)
  (closesocket)
)



(defn OnUpdateNotificationError [response]
  (let [     
    ]

  )
  ;;(.log js/console (str  (get (first response)  "Title") ))
)

(defn OnUpdateNotificationSuccess [response]
  (let [
      ;notifications (:users @app-state)
      ;deluser (remove (fn [user] (if (= (:userid user) (:userid (:selecteduser @app-state)) ) true false  )) users)
      ;adduser (conj deluser (:selecteduser @app-state))
    ]
    ;(swap! app-state assoc-in [:users] adduser)
    ;(js/window.history.back)
  )
)


(defn seennotification [item e]
  (let [
    ;tr1 (swap! app-state assoc-in [:user :addedby] (:userid (:token @shelters/app-state)))
      accept (tf/unparse custom-formatter2 (tc/now))
      newnotifications (map (fn [x] (if (= (:id x) (:id item)) (assoc x :status "Accepted") x)) (if (:isnotification @app-state) (:notifications @app-state) (:alerts @app-state)))
    ]
    (if (:isnotification @app-state) (swap! app-state assoc-in [:notifications] newnotifications) (swap! app-state assoc-in [:alerts] newnotifications))
    ;(set! (.-disabled (.. e -target)) true)
    (PUT (str settings/apipath  "updateNotification") {
      :handler OnUpdateNotificationSuccess
      :error-handler OnUpdateNotificationError
      :headers {
        :token (str (:token (:token @app-state)))
      }
      :format :json
      :params { :notificationId (:id item) :notificationType (:type item) :unitId (:unitid item) :openTime (tf/unparse custom-formatter2 (:open item)) :acceptanceTime accept :closeTime (tf/unparse custom-formatter2 (:close item)) :userid (:userid (:token @app-state)) :status "Accepted"}})
  )
)

(defcomponent notifications-table [data owner]
  (render [_]
    (let [
      ;tr1 (.log js/console (str "notifications " (count (:notifications @data))))
      ]

      (dom/div {:className "panel-body" :style {:padding-top "0px" :padding-left "0px" :padding-right "0px" :padding-bottom "0px" :height (str (tableheight (if (:isalert @data) (count (:alerts @data)) (count (:notifications @data)))) "px") :overflow-y "scroll" :border "1px solid lightgray"}}
        (map (fn [item]
          (let [
            unit (first (filter (fn [x] (if (= (:id x) (:unitid item)) true false)) (:devices @data)))

            user (first (filter (fn [x] (if (= (:userid x) (:userid item)) true false)) (:users @data)))

            indicator (first (filter (fn [x] (if (= (:id x) (:sensorid item)) true false))  (:indications @app-state)))
            ]
            (dom/div {:className "row" :style { :border-bottom "1px solid" :border-right "1px solid transparent" :display "flex" :margin-left "0px" :margin-right "0px" :text-align "center"}}
              (dom/div {:className "col-md-1"}
                (dom/div {:className "row"}
                  (dom/div {:className "col-md-4" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px"}}
                    (b/button {:className "btn btn-default" :disabled? (if (= (:status item) "New") false true) :style {:padding "0px" :margin-top "2px" :margin-bottom "2px"} :onClick (fn [e] (seennotification item e))} "ראיתי")
                  )
                  (dom/div {:className "col-md-8" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :padding-top "3px" :padding-bottom "3px"}}
                    (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }                
                      (:id item)
                    )
                  )
                )
              )
              (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }                
                  (:controller unit)
                )
              )
              (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }                
                  (:name unit)
                )
              )

              (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center" :max-height "27px" :overflow-y "hidden"}}
                (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }
                  (:address unit)                
                )
              )

              (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }
                  (str (t :he main-tconfig (keyword (str "indicators/" (:name indicator)))))
                  ;(str (t :he main-tconfig (keyword (str "alerts/" (:type item)))))
                )
              )

              (dom/div {:className "col-xs-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/div {:className "row" :style {:margin-left "0px" :margin-right "0px"}}
                  (dom/div {:className "col-xs-6" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :padding-top "3px" :padding-bottom "3px" :text-align "center"}}
                    (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }
                      (tf/unparse custom-formatter1 (:open item))
                    )
                  )

                  (dom/div {:className "col-xs-6" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center" :padding-top "3px" :padding-bottom "3px"}}
                    (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) :style {:color (if (= (:status item) "Closed") "black" "transparent" )}}
                      (tf/unparse custom-formatter1 (:close item))
                    )
                  )
                )
              )
              (dom/div {:className "col-md-3" :style {:padding-left "0px" :padding-right "0px"}}
                (dom/div {:className "col-md-6" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center" :padding-top "3px" :padding-bottom "3px"}}
                  (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }
                    (t :he main-tconfig (keyword (str "alerts/" (:status item))))
                  )
                )

                (dom/div {:className "col-md-6" :style { :border-left "1px solid transparent" :padding-left "0px" :padding-right "0px" :text-align "center"}}
                  (dom/a {:className "nolink" :href (str "#/unitdetail/" (:id unit)) }
                    (str (:firstname user) " " (:lastname user))
                  )
                )
              )
            )
          ))
          (sort (comp comp-alerts) (filter (fn [x] (if (= 1 (:selectedstatus @data)) true (if (= (:id (first (filter (fn [y] (if (= (:eng y) (:status x)) true false)) (:statuses @data)))) (:selectedstatus @data)) true false))) (if (:isalert @data) (:alerts @data) (:notifications @data)) ))
        )
      )
    )
  )
)



 
(defcomponent logout-view [_ _]
  (render
    [_]
    (let [style {:style {:margin "10px"}}]
      (dom/div style (dom/a (assoc style :href "#/login") "Login"))
    )
  )
)


(defn handle-chkb-change [e]
  ;(.log js/console (.. e -target -id) )  
  ;(.log js/console "The change ....")
  (.stopPropagation e)
  (.stopImmediatePropagation (.. e -nativeEvent) )
  (swap! app-state assoc-in [(keyword  (.. e -currentTarget -id) )] 
    (if (= true (.. e -currentTarget -checked)  ) 1 0)
  )
  ;(CheckCalcLeave)
  ;(set! (.-checked (.. e -currentTarget)) false)
  ;(dominalib/remove-attr!  (.. e -currentTarget) :checked)
  ;;(dominalib/set-attr!  (.. e -currentTarget) :checked true)
)

(defn handle-change [e owner]
  
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  ) 
)

(defn handle-change-currency [e owner]
  
  (swap! app-state assoc-in [:form (keyword (.. e -target -id))] 
    (.. e -target -value)
  ) 
)

(defn map-deal [deal]
  (let [
        trans (loop [result [] trans (:transactions deal) ]
                (if (seq trans) 
                  (let [
                        tran (first trans)
                        ;tr1 (.log js/console (str "tran: " tran ))
                        ]
                    (recur (conj result {:security (:security deal) :date (:date tran) :direction (:direction tran) :nominal (:nominal tran) :wap (:wap tran) :wapusd (:wapusd tran) :waprub (:waprub tran)})
                         (rest trans))
                  )
                  result)
        )        
        result trans
    ]
    ;
    result
  )
)


(defn map-position [position]
  (let [
    client (first (filter (fn [x] (if (= (:selectedclient @app-state) (:code x)) true false)) (:clients @app-state)))
    secid (js/parseInt (name (nth position 0)))
    security (first (filter (fn [x] (if (= (:id x) secid) true false)) (:securities @app-state)))
    
    posprice (:price (nth position 1))
    price (if (nil? (:price security)) posprice (:price security))


    currency (if (= 0 (compare "GBX" (:currency security))) "GBP" (:currency security))

    
    fxrate (if (or (= "RUB" currency) (= "RUR" currency)) 1 (:price  (first (filter (fn[x] (if( = (:acode x) currency) true false)) (:securities @app-state)))))
    usdrate (:price (first (filter (fn [x] (if (= "USD" (:acode x)) true false)) (:securities @app-state))))


    ;;tr1 (.log js/console (str "client currency: " (:currency client) "position=" (nth position 1)))
    clientcurrencyrate (:price (first (filter (fn [x] (if (= (str/upper-case (:currency client)) (:acode x)) true false)) (:securities @app-state))))

    isbond (if (and (= 5 (:assettype security)) 
                   ;(= "RU" (subs (:isin security) 0 2))
                   )  true false)
    newfxrate (if (= 0 (compare "GBX" (:currency security))) (/ fxrate 100.) fxrate)

    result {:id secid :currency (:currency security) :amount (:amount (nth position 1)) :wap posprice :price price :waprub (:rubprice (nth position 1)) :currubprice (* price newfxrate) :wapusd (:wapusd (nth position 1)) :usdvalue (/ (* (:amount (nth position 1)) (:price security)  (if (= isbond true) (* newfxrate (:multiple security) 0.01 ) newfxrate )  ) usdrate) :posvalue (/ (* (:amount (nth position 1)) (:price security)  (if (= isbond true) (* newfxrate 0.01 (:multiple security)) newfxrate )  ) clientcurrencyrate) }



    ]
    result
  )
)

(defn map-portfolio [item]
  (let [
    ;tr1 (.log js/console item)
    portfid (name (nth item 0))
    portfolio (first (filter (fn [x] (if (= (compare (:code x) portfid) 0) true false)) (:clients @app-state)))

    security (first (filter (fn [x] (if (= (:id x) (:selectedsec @app-state)) true false)) (:securities @app-state)))
    posprice (get (nth item 1) "price")
    price (if (nil? (:price security)) posprice (:price security))

        
    currency (if (= 0 (compare "GBX" (:currency security))) "GBP" (:currency security))

    usdrate (:price (first (filter (fn [x] (if (= "USD" (:acode x)) true false)) (:securities @app-state)))) 

    fxrate (if (or (= "RUB" currency) (= "RUR" currency)) 1 (:price  (first (filter (fn[x] (if( = (:acode x) currency) true false)) (:securities @app-state)))))

    newfxrate (if (= 0 (compare "GBX" (:currency security))) (/ fxrate 100.) fxrate)

    ;;isrusbond (if (and (= 5 (:assettype security)) (= "RUB" (:currency security)))  true false)
    isbond (if (and (= 5 (:assettype security)) 
                   ;(= "RU" (subs (:isin security) 0 2))
                   )  true false)

    result {:id (:id portfolio) :amount (:amount (nth item 1) ) :wapcur (:price (nth item 1) ) :wapusd (:wapusd (nth item 1) ) :waprub (:rubprice (nth item 1) ) :currubprice (* price newfxrate) :usdvalue (/ (* (:amount (nth item 1)) (:price security)  (if (= isbond true) (* newfxrate 0.01 (:multiple security)) newfxrate )  ) usdrate) }

    ]
    result
  )
)


(defn map-calc-portfolio [item]
  (let [
    portfid 1
    ;; portfolio (first (filter (fn [x] (if (= (compare (:code x) portfid) 0) true false)) (:clients @app-state)))

    ;; security (first (filter (fn [x] (if (= (:id x) (:selectedsec @app-state)) true false)) (:securities @app-state)))
    ;; posprice (get (nth item 1) "price")
    ;; price (if (nil? (:price security)) posprice (:price security))

        
    ;; currency (if (= 0 (compare "GBX" (:currency security))) "GBP" (:currency security))

    ;; usdrate (:price (first (filter (fn [x] (if (= "USD" (:acode x)) true false)) (:securities @app-state)))) 

    ;; fxrate (if (or (= "RUB" currency) (= "RUR" currency)) 1 (:price  (first (filter (fn[x] (if( = (:acode x) currency) true false)) (:securities @app-state)))))

    ;; newfxrate (if (= 0 (compare "GBX" (:currency security))) (/ fxrate 100.) fxrate)
    ;; isrusbond (if (and (= 5 (:assettype security)) 
    ;;                    (= "RU" (subs (:isin security) 0 2))
    ;;                    )  true false)
    ;; isbond (if (and (= 5 (:assettype security)) 
    ;;                ;(= "RU" (subs (:isin security) 0 2))
    ;;                )  true false)

    ;; result {:id (:id portfolio) :amount (:amount (nth item 1) ) :wapcur (:price (nth item 1) ) :wapusd (:price (nth item 1) ) :waprub (:rubprice (nth item 1) ) :currubprice (* price newfxrate) :usdvalue (/ (* (:amount (nth item 1)) (:price security)  (if (= isrusbond true) 10.0 (if (= isbond true) (/ newfxrate 100.0 ) newfxrate ) ) ) usdrate) }

    ]
    ;(.log js/console item)
    item
  )
)

(defn OnGetPortfolios [response]
  (swap! app-state assoc :state 1 )
  (swap! app-state assoc-in [ (keyword (str (:selectedsec @app-state)) ) :portfolios] (map (fn [x] (map-portfolio x)) response) )
)

(defn OnGetCalcPortfolios [response]
  ;(set! ( . (.getElementById js/document "btnrefresh") -disabled) false)
  (swap! app-state assoc :state 1 )
  (swap! app-state assoc-in [ (keyword (str (:selectedsec @app-state)) ) :calcportfs] (map (fn [x] (map-calc-portfolio x)) response) )
)

(defn OnGetPositions [response]
  (swap! app-state assoc :state 1 )
  (swap! app-state assoc-in [(keyword (:selectedclient @app-state)) :positions] (map (fn [x] (map-position x)) (filter (fn [x] (if (= (:amount (nth x 1)) 0.0) false true)) response) ) )
)

(defn OnGetDeals [response]
  (let [
    deals (:deals ((keyword (:selectedclient @app-state)) @app-state))
    ]
    (swap! app-state assoc :state 1 )
    (if (> (count response) 0)
      (swap! app-state assoc-in [(keyword (:selectedclient @app-state)) :deals] (concat deals (flatten (map (fn [x] (map-deal x)) (filter (fn [x] (if (> 1 1) true true)) response) ))))
      (swap! app-state assoc-in [:nomoredeals] true)
    )    
  )
)

(defn update-position [position]
  (let [
    client (first (filter (fn [x] (if (= (:selectedclient @app-state) (:code x)) true false)) (:clients @app-state)))
    secid (:id position)
    security (first (filter (fn [x] (if (= (:id x) secid) true false)) (:securities @app-state)))
    
    posprice (:price position)
    price (if (nil? (:price security)) posprice (:price security))


    currency (if (= 0 (compare "GBX" (:currency security))) "GBP" (:currency security))

    
    fxrate (if (or (= "RUB" currency) (= "RUR" currency)) 1 (:price  (first (filter (fn[x] (if( = (:acode x) currency) true false)) (:securities @app-state)))))
    usdrate (:price (first (filter (fn [x] (if (= "USD" (:acode x)) true false)) (:securities @app-state))))


    ;;tr1 (.log js/console (str "client currency: " (:currency client) "position=" (nth position 1)))
    clientcurrencyrate (:price (first (filter (fn [x] (if (= (str/upper-case (:currency client)) (:acode x)) true false)) (:securities @app-state))))

    isbond (if (and (= 5 (:assettype security)) 
                   ;(= "RU" (subs (:isin security) 0 2))
                   )  true false)
    newfxrate (if (= 0 (compare "GBX" (:currency security))) (/ fxrate 100.) fxrate)

    result {:id secid :currency (:currency security) :amount (:amount position) :wap posprice :price price :waprub (:waprub position) :currubprice (* price newfxrate) :wapusd (:wapusd position) :usdvalue (/ (* (:amount position) (:price security)  (if (= isbond true) (* newfxrate (:multiple security) 0.01 ) newfxrate )  ) usdrate) :posvalue (/ (* (:amount position) (:price security)  (if (= isbond true) (* newfxrate 0.01 (:multiple security)) newfxrate )  ) clientcurrencyrate) }
    ]
    result
  )
)

(defn update-selectedclient []
  (let [
      positions (:positions ((keyword (:selectedclient @app-state)) @app-state))
    ]
    (swap! app-state assoc-in [(keyword (:selectedclient @app-state)) :positions] (map (fn [x] (update-position x)) positions))
    ;;map-position
  )
)

(defn OnGetSecurities [response]
  (swap! app-state assoc-in [:securities] response )
  (swap! app-state assoc-in [:state] 1)
  ;(swap! sbercore/app-state assoc-in [:view] 1 )
  ;(aset js/window "location" "#/positions")
  ;(:positions ((keyword (:selectedclient @data)) @data))
  (if (not (nil? (:selectedclient @app-state))) (update-selectedclient))
)



(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "something bad happened: " status " " status-text))
)



(defn reqsecurities []
  (swap! app-state assoc :state 2 )
  (GET (str settings/apipath "api/security")
       {:handler OnGetSecurities
        :error-handler error-handler
        :headers {:content-type "application/json"
                  :Authorization (str "Bearer " (:token  (:token @app-state))) }
       })
)



(defn getPositions []
  (swap! app-state assoc :state 2 )
  (GET (str settings/apipath "api/position?client=" (:selectedclient @app-state) ) {
    :handler OnGetPositions
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token (:token @app-state))) }
  })
)

(defn getDeals []
  (swap! app-state update-in [:dealspage] inc)
  (swap! app-state assoc :state 2 )
  (GET (str settings/apipath "api/deals?client=" (:selectedclient @app-state) "&page=" (:dealspage @app-state)) {
    :handler OnGetDeals
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token (:token @app-state)))}
  })
)


(defn getPortfolios [] 
  (GET (str settings/apipath "api/portfolios?security=" (:selectedsec @app-state) ) {
    :handler OnGetPortfolios
    :error-handler error-handler
    :headers {
      :content-type "application/json"
      :Authorization (str "Bearer "  (:token (:token @app-state))) }
  })
)

(defn getCalcPortfolios []
  (let [
      percentage 10.0 ;;(:percentage @app-state)
    ]
    (swap! app-state assoc :state 2 )
    ;(set! ( . (.getElementById js/document "btnrefresh") -disabled) true)
    (GET (str settings/apipath "api/calcshares?security=" (:selectedsec @app-state) "&percentage=" percentage ) {
      :handler OnGetCalcPortfolios
      :error-handler error-handler
      :headers {
        :content-type "application/json"
        :Authorization (str "Bearer "  (:token (:token @app-state))) }
    })
  )
)

(defn onSecsDropDownChange [id value]
  (let [
        code (:id (first (filter (fn[x] (if (= (:id x) (js/parseInt value) ) true false)) (:securities @app-state)))  )
        ]

    (swap! app-state assoc-in [:selectedsec] code)
    (if (nil? (:portfolios ((keyword value) @app-state)))
      (getPortfolios)
    )
  )
  
  ;;(.log js/console value)  
)

(defn onCalcSecsDropDownChange [id value]
  (let [
        code (:id (first (filter (fn[x] (if (= (:id x) (js/parseInt value) ) true false)) (:securities @app-state)))  )
        ]

    (swap! app-state assoc-in [:selectedsec] code)
    (if (nil? (:calcportfs ((keyword value) @app-state))) (getCalcPortfolios))    
  )
  (.log js/console (str "in onCalcSecsDropDownChange value =") value)  
)

(defn onCalcCurrenciesDropDownChange [id value]
  (let [
        code ""
        ]

    (swap! app-state assoc-in [:selectedcurrency] value)
    ;(if (nil? (:calcportfs ((keyword value) @app-state))))
    ;(getCalcPortfolios)
  )
  
  ;;(.log js/console value)  
)




(defn onDidUpdate [data]
  ;(setClientsDropDown)
    ;; (jquery
    ;;   (fn []
    ;;     (-> (jquery "#side-menu")
    ;;       (.metisMenu)
    ;;     )
    ;;   )
    ;; )

)

(defn onMount [data]
  ;(.log js/console "Mount core happened")

  (if (or (:isalert @app-state) (:isnotification @app-state))
    (go
         (<! (timeout 500))
         (put! ch 42)
    )
  )
)



(defn handleCheck [e]
  (.stopPropagation e)
  (.stopImmediatePropagation (.. e -nativeEvent) )
  (swap! app-state assoc-in [(keyword (.. e -nativeEvent -target -id))] (.. e -nativeEvent -target -checked))
)

(defn printMonth []
  (.print js/window)
)


(defn downloadPortfolio [e]
  (aset js/window "location" (str "/clientexcel/" (:selectedclient @app-state)))
)

(defn downloadSecPortfolios [e]
  (aset js/window "location" (str "/secexcel/" (:selectedsec @app-state)))
)

(defn downloadBloombergPortfolio [e]
  (aset js/window "location" (str "/clientbloombergportf/" (:selectedclient @app-state)))
)

(defn downloadBloombergTransactions [e]
  (aset js/window "location" (str "/clientbloombergtrans/" (:selectedclient @app-state)))
)

(defn onDropDownChange [id value]
  (let [
        ;tr1 (.log js/console "id=" id " value=" value)
    ]
    (if (= id "statuses")
      (swap! app-state assoc-in [:selectedstatus] (js/parseInt value))
    )
  )
  ;;(.log js/console value)  

)


(defn setStatusesDropDown []
       (-> (jquery "#statuses" )
         (.selectpicker)
       )
  ;; (jquery
  ;;    (fn []

  ;;    )
  ;;  )
   (jquery
     (fn []
       (-> (jquery "#statuses" )
         (.selectpicker "val" (:selectedstatus @app-state))
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
)


(defn notificationsclick [todo]
  (let [
    ]

    (case todo
      1 ((swap! app-state assoc-in [:isalert] false) (swap! app-state assoc-in [:isnotification] (if (= true (:isnotification @app-state)) (if (> (.indexOf (.-href (.-location js/window)) "#/map") 0) false true) true)))

      2 ((swap! app-state assoc-in [:isnotification] false) (swap! app-state assoc-in [:isalert] (if (= true (:isalert @app-state)) (if (> (.indexOf (.-href (.-location js/window)) "#/map") 0) false true) true)))

      ( (swap! app-state assoc-in [:isnotification] false)  (swap! app-state assoc-in [:isalert] false))
    )

    (aset js/window "location" "#/map")
    (go
      (<! (timeout 100))
      (js/google.maps.event.trigger (:map @app-state) "resize")
      (put! ch 42)
    )
  )
)

(defcomponent main-navigation-view [data owner]  
  (did-mount [this]
    (let [
      ;tr1 (.log js/console "did mount")
      ]
      (onMount data)
    )    
  )
  (render [_]
    (let [
      user (first (filter (fn [x] (if (= (:userid x) (:userid (:token @app-state))) true false)) (:users @app-state)))
      ;tr1 (.log js/console (str "in map navigation"))
      role (:id (:role user))
      fullname (str (:firstname user) " " (:lastname user))
      role (:name (:role (first (filter (fn[x] (if (= (:userid (:token @data)) (:userid x)) true false)) (:users @data)))))

      ;tr1 (.log js/console (str "role=" role))

      ]
      (dom/div {:className "navbar navbar-default navbar-fixed-top" :role "navigation" :style {:height "70px" :margin-left "15px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px" :padding-left "5px" :padding-right "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "130px" :height "61px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1" :style {
            ;:font-size "larger"
          }}

          (dom/ul {:className "nav navbar-nav"}

            (dom/li
              (dom/a {:className "navbara" :href "#/map" :style {:padding-left "5px" :padding-right "5px"}}
                (dom/i {:className "fa fa-map-o" :style {:margin-left "5px"}})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:className "navbara" :href "#/dashboard" :style {:padding-left "5px" :padding-right "5px"} :onMouseOver (fn [x]
                                                                                        (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "none")
                                                                                        (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none"))}
                (dom/i {:className "fa fa-dashboard" :style {:margin-left "5px"}})
                "Dashboard"
              )
            )

            ;; (dom/li
            ;;   (dom/a {:className "navbara" :href "#/devslist" :onMouseOver (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "none"))}
            ;;     (dom/i {:className "fa fa-building"})
            ;;     "רשימה יחידות"
            ;;   )
            ;; )

            (if (not= role settings/dispatcherrole)
              (dom/li {:className "dropdown" :style {:min-width "160px"}}
                (dom/a { :href "#" :className "navbarasysmanage" :style {:padding-left "0px" :padding-right "0px"}
                    :onMouseOver (fn [x]
                      (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "block")
                      (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none")
                    )
                    ;:onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "none"))
                  }
                  (dom/span {:className "caret"})
                  (dom/i {:className "fa fa-archive" :style {:margin-left "5px"}})
                  "ניהול מערכת"
                )
                (dom/div { :id "navbarulmanage" :className "navbarulmanage" :style {:display "none" :background-color "#f9f9f9" :border-left "2px solid grey" :border-right "2px solid grey" :border-bottom "2px solid grey"} :onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "none"))}
                  (dom/div {:className "row" :style {:margin-top "5px"}}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "#/groups" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                        (dom/i {:className "fa fa-users" :style {:padding-left "5px"}})
                        "ניהול קבוצות"
                      )
                    )
                  )

                  (if (not= role settings/dispatcherrole)
                    (dom/div {:className "row" :style {:margin-top "5px"}}
                      (dom/div {:className "col-md-12"}
                        (dom/a {:href "#/users" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                          (dom/i {:className "fa fa-key" :style {:padding-left "5px"}})
                          "ניהול משתמשים"
                        )
                      )
                    )
                  ) 

                  (dom/div {:className "row" :style {:margin-top "5px"}}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "#/devslist" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                        (dom/i {:className "fa fa-hdd-o" :style {:padding-left "5px"}})
                        "ניהול יחידות"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown" :style {:min-width "200px"}}
              (dom/a { :href "#" :className "navbarareports"
                :onMouseOver (fn [x]
                  (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "block")
                  (set! (.-display (.-style (js/document.getElementById "navbarulmanage")) ) "none")
                )
                ;:onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none"))
                }
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive" :style {:margin-left "5px"}})
                "דו״חות"
              )
              (dom/div { :id "navbarulreports" :className "navbarulreports" :style {:display "none" :background-color "#f9f9f9" :border-left "2px solid grey" :border-bottom "2px solid grey" :border-right "2px solid grey"} :border-top "1px solid #337ab7" :onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none"))}
                (dom/div {:className "row" :style {:margin-top "5px"}}
                  (dom/div {:className "col-md-12"}
                    (dom/a {:href "#/reportunits" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                      (dom/i {:className "fa fa-line-chart" :style {:padding-left "5px"}})
                      "דו''ח תקלות"
                    )
                  )
                )
                (dom/div {:className "row" :style {:margin-top "5px"}}
                  (dom/div {:className "col-md-12"}
                    (dom/a {:href "#/report.triggeredAlerts" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                      (dom/i {:className "fa fa-bullhorn" :style {:padding-left "5px"}})
                      "דו''ח חיווים"
                    )
                  )
                )
                (dom/div {:className "row" :style {:margin-top "5px"}}
                  (dom/div {:className "col-md-12"}
                    (dom/a {:href "#/report.notifications" :className "menu_item" :style {:padding-left "10px" :padding-right "10px"}}
                      (dom/i {:className "fa fa-envelope-o" :style {:padding-left "5px"}})
                      "דו''ח שליחת פקודות הפעלה"
                    )
                  )
                )
              )
            )


            (dom/li {:className "dropdown" :style {:background-color "#555555" :margin-right "0px" :padding "10px" :margin-top "5px" :margin-left "5px" :border-radius "10px"}}
              (dom/a {:style {:padding "0px"} :onClick (fn [e] (notificationsclick 1)) :onMouseOver (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none"))}
                (dom/div {:style {:background-color "grey" :border-radius "5px"}}
                  (b/button {:className "btn btn-danger" :style {:border-radius "15px" :margin-top "-25px" :padding-left "6px" :padding-right "6px" :padding-top "0px" :padding-bottom "0px"}} (str (count (filter (fn [x] (if (= "Closed" (:status x)) false true)) (:notifications @data)))))
                  (dom/span {:style {:cursor "pointer" :color "white"}} "התראות")
                  (dom/i {:className "fa fa-bell fa-fw" :style {:font-size "24px" :color "red"}})
                )
              )
              ;(om/build notifications-navbar data {})
            )

            (dom/li {:className "dropdown" :style {:background-color "#555555" :margin-right "0px"  :padding "10px" :margin-top "5px" :margin-left "5px" :border-radius "10px"}}
              (dom/a {:style {:padding "0px"} :onClick (fn [e] (notificationsclick 2))}
                (dom/div {:style {:background-color "grey" :border-radius "5px"}}
                  (b/button {:className "btn btn-danger" :style {:padding-left "6px" :padding-right "6px" :padding-top "0px" :padding-bottom "0px" :border-radius "25px" :margin-top "-25px"}} (str (count (filter (fn [x] (if (= "Closed" (:status x)) false true)) (:alerts @data)))))
                  (dom/span {:style {:cursor "pointer" :color "white"}} "תקלות")
                  (dom/i {:className "fa fa-exclamation-circle fa-fw" :style {:color "red" :font-size "24px"}})
                )
              )
              ;(om/build alerts-navbar data {})
            )
          )

          (dom/ul {:className "nav navbar-nav navbar-left"}
            (dom/li {:style {:margin-right "0px" :margin-top "10px" :text-align "center"} :onMouseOver (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulexit")) ) "none"))}
              (dom/h5 {:style {:padding-top "0px" :color "#337ab7" }} (str "שירות לקוחות" " " "03-6100066"))
              ;(dom/h5 {:style {:padding-top "0px" :color "#337ab7" }} "03-123-456-789")
            )
            (dom/li {:className "dropdown"}
              (dom/a { :href "#" :className "navbaraexit" :style {:padding-left "10px" :padding-right "0px" :max-width "150px" :text-align "left"}
                :onMouseOver (fn [x]
                  (set! (.-display (.-style (js/document.getElementById "navbarulexit")) ) "block")
                )
                ;:onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulreports")) ) "none"))
                }
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-user-circle" :style {:margin-left "3px" :margin-right "3px"}})
                (str fullname " " role)
              )

              (dom/div { :id "navbarulexit" :className "navbarulexit" :style {:display "none" :background-color "#f9f9f9" :text-align "left"} :onMouseLeave (fn [x] (set! (.-display (.-style (js/document.getElementById "navbarulexit")) ) "none"))}
                (dom/div {:className "row"}
                  (dom/div {:className "col-md-12"}
                    (dom/a {:href "#/login" :className "menu_item" :style {:padding-left "10px" :padding-right "0px"} :onClick (fn [e](doLogout))}
                      (dom/i {:className "fa fa-sign-out" :style {:margin-left "3px" :margin-right "3px"}})
                      "יציאה"
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
)



(defmulti website-view
  (
    fn [data _]   
      (:view (if (= data nil) @app-state @data ))
      ;;(:view @app-state )
  )
)

(defmethod website-view 0
  [data owner] 
  ;(.log js/console "zero found in view")
  (logout-view data owner)
)



(defmethod website-view 2
  [data owner] 
  ;(.log js/console "Two is found in view")
  (main-navigation-view data owner)
)

(defmethod website-view 3
  [data owner] 
  ;(.log js/console "Two is found in view")
  (main-navigation-view data owner)
)

(defmethod website-view 4
  [data owner] 
  ;(.log js/console "One is found in view")
  (main-navigation-view app-state owner)
)


(defmethod website-view 6
  [data owner] 
  ;(.log js/console "One is found in view")
  (main-navigation-view data owner)
)

(defmethod website-view 7
  [data owner] 
  ;(.log js/console "One is found in view")
  (main-navigation-view data owner)
)

(defmethod website-view 8
  [data owner] 
  ;(.log js/console "One is found in view")
  (main-navigation-view data owner)
)

(defn setcontrols [value]
  (case value
    42 ( let [] 
         (setStatusesDropDown)
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
