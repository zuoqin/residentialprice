(ns shelters.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [ajax.core :refer [GET POST]]
            [om.dom :as omdom :include-macros true]

            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [cljs.core.async :refer [put! dropping-buffer chan take! <! >! timeout]]
            [om-bootstrap.button :as b]
            [shelters.settings :as settings]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
  )
  (:import goog.History)
)

(enable-console-print!)

(def custom-formatter (tf/formatter "dd/MM/yyyy"))
(def custom-formatter2 (tf/formatter "dd/MM/yyyy hh:mm:ss"))

(defn tableheight [count] 
  (+ 14 (* 34 (min count 10)))
)

;;{:id "1602323" :city 1 :name "tek aviv sfs" :status 3 :address "נחלת בנימין 24-26, תל אביב יפו, ישראל" :lat 32.08088 :lon 34.78057 :contacts [{:tel "1235689" :name "Alexey"} {:tel "7879787" :name "Oleg"}]} {:id "2" :city 2 :name "The second device" :status 2 :address "נחלת בנימין 243-256, תל אביב יפו, ישראל" :lat 31.92933 :lon 34.79868 }

(defonce app-state (atom {:map nil :state 0 :selectedunits [] :search "" :isalert false :isnotification false :user {:role "admin"} :selectedcenter {:lat 31.7683 :lon 35.2137}, :contacts [{:id "1" :name "Alexey" :phone "+79175134855" :email "zorchenkov@gmail.com"} {:id "2" :name "yulia" :phone "+9721112255" :email "yulia@gmail.com"} {:id "3" :name "Oleg" :phone "+8613946174558" :email "oleg@yahoo.com"}]
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


(defn comp-alerts [alert1 alert2]
  (if (> (:open alert1) (:open alert2)) true false

    ;(if (and (= (:open alert1) (:open alert2)) (> )))
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



(defn doLogout [data]
  (swap! app-state assoc-in [:view] 0)
)

(defn goUserDetail [e]
  ;(aset js/window "location" "#/userdetail")
  (swap! app-state assoc-in [:view] 4)
)

(defn goContactDetail [e]
  ;(aset js/window "location" "#/userdetail")
  (swap! app-state assoc-in [:view] 4)
)


(defn goRoleDetail [e]
  ;(aset js/window "location" "#/portfolios/0")
  (swap! app-state assoc-in [:view] 6)
)

(defn goGroupDetail [e]
  ;(aset js/window "location" "#/portfolios/0")
  (swap! app-state assoc-in [:view] 6)
)

(defn goGroups [e]
  (aset js/window "location" "#/groups")
  (set! (.-title js/document) "ניהול קבוצות")
  (swap! app-state assoc-in [:view] 8)
)

(defn goMap [e]
  (aset js/window "location" "#/map")
  (set! (.-title js/document) "מפה")
  (swap! app-state assoc :state 1)
  (swap! app-state assoc-in [:view] 2)
)

(defn goDashboard [e]
  (aset js/window "location" "/#/dashboard")
  (set! (.-title js/document) "Dashboard")
  (swap! app-state assoc-in [:view] 8)
)

(defn goDevslist [e]
  (aset js/window "location" "/#/devslist")
  (set! (.-title js/document) "רשימה יחידות")
  (swap! app-state assoc-in [:view] 8)
)

(defn goUsers [data]
  (swap! app-state assoc-in [:view] 3)
  (aset js/window "location" "#/users")
  (set! (.-title js/document) "משתמשים והרשאות")
)

(defn goRoles [data]
  (swap! app-state assoc-in [:view] 3)
)

(defn goSysSettings [data]
  ;(aset js/window "location" "#/syssettings")
  (swap! app-state assoc-in [:view] 5)
)

(defn goSettings [data]
  ;;(swap! app-state assoc-in [:view] 5 ) settings/apipath
  (.open js/window (str settings/apipath "tradeidea/" (:token (:token @app-state))))
)

(defcomponent notifications-navbar [data owner]
  (render [_]
    (dom/ul {:className "dropdown-menu dropdown-alerts"}
      (map (fn [item]
        (let []
          (dom/li
            (dom/a {:href (str "/#/notedetail/" (:id item)) }
              (dom/div
                (dom/i {:className "fa fa-comment fa-fw"})
                (:text item)
                (dom/span {:className "pull-right text-muted small"}
                  "4 minutes ago"
                )
              )
            )
          )
        ))
      (:notifications @app-state))
    )
  )
)

(defcomponent notifications-table [data owner]
  (render [_]
    (dom/div {:className "panel-body" :style {:padding-top "0px" :padding-left "0px" :padding-right "0px" :padding-bottom "0px" :height (str (tableheight (count (:notifications @data))) "px") :overflow-y "scroll"}}
      (map (fn [item]
        (let [
          unit (first (filter (fn [x] (if (= (:id x) (:unitid item)) true false)) (:devices @app-state)))

          user (first (filter (fn [x] (if (= (:userid x) (:userid item)) true false)) (:users @app-state)))
          ]
          (dom/div {:className "row" :style { :border-bottom "1px solid" :border-right "1px solid" :display "flex" :margin-left "0px" :margin-right "0px"}}
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid"}}
              (b/button {:className "btn btn-primary" :onClick (fn [e])} "ראיתי")
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }                
                (:id item)
              )
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }                
                (:controller unit)
              )
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }                
                (:name unit)
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:address unit)                
              )
            )

            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                "Failure"                
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (tf/unparse custom-formatter2 (:open item))              
              )
            )

            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:status item)               
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (str (:firstname user) " " (:lastname user))
              )
            )
          )
        ))
        (sort (comp comp-alerts) (:notifications @data))
      )
    )
  )
)


(defcomponent alerts-navbar [data owner]
  (render [_]
    (dom/ul {:className "dropdown-menu dropdown-alerts"}
      (map (fn [item]
        (let []
          (dom/li
            (dom/a {:href (str "/#/unitdetail/" (:id item)) }
              (dom/div
                (dom/i {:className "fa fa-comment fa-fw"})
                (:text item)
                (dom/span {:className "pull-right text-muted small"}
                  "  4 minutes ago"
                )
              )
            )
          )
        ))

      (:alerts @app-state))
    )
  )
)


(defcomponent alerts-table [data owner]
  (render [_]
    (dom/div {:className "panel-body" :style {:padding-top "0px" :padding-left "0px" :padding-right "0px" :padding-bottom "0px" :height (str (tableheight (count (:alerts @data))) "px") :overflow-y "scroll"}}
      (map (fn [item]
        (let [
          unit (first (filter (fn [x] (if (= (:id x) (:unitid item)) true false)) (:devices @app-state)))

          user (first (filter (fn [x] (if (= (:userid x) (:userid item)) true false)) (:users @app-state)))
          ]
          (dom/div {:className "row" :style {:border-bottom "1px solid" :border-right "1px solid" :display "flex" :margin-left "0px" :margin-right "0px"}}
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid"}}
              (b/button {:className "btn btn-primary" :onClick (fn [e])} "ראיתי")
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:id item)                
              )
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:controller unit)
              )
            )
            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:name unit)                
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:address unit)                
              )
            )

            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                "Alert"                
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (tf/unparse custom-formatter2 (:open item))              
              )
            )

            (dom/div {:className "col-xs-1" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (:status item)               
              )
            )

            (dom/div {:className "col-xs-2" :style { :border-left "1px solid" :padding-left "0px" :padding-right "0px" :text-align "center"}}
              (dom/a {:href (str "/#/unitdetail/" (:id unit)) }
                (str (:firstname user) " " (:lastname user))
              )
            )
          )
        ))
      (sort (comp comp-alerts) (:alerts @data)))
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

(defn onDropDownChange [id value]
  (let [
        code (:code (first (filter (fn[x] (if (= (:id x) (js/parseInt value) ) true false)) (:clients @app-state)))  )
        ;;tr1 (.log js/console (str "value=" value " code=" code))
        
        ]
    (swap! app-state assoc :state 1 )
    (swap! app-state assoc-in [:dealspage] -1)
    (swap! app-state assoc-in [:nomoredeals] false)
    (swap! app-state assoc-in [:selectedclient] code)
    (if (nil? (:positions ((keyword value) @app-state)))
      (getPositions)
    )

    (if (nil? (:deals ((keyword code) @app-state)))
      (getDeals)
    )
  )
  

  ;;(.log js/console value)  

)


(defn comp-clients
  [client1 client2]
  (if (> (compare (:code client1) (:code client2)) 0) 
      false
      true
  )
)

(defn comp-secs
  [security1 security2]
  (if (> (compare (:acode security1) (:acode security2)) 0) 
      false
      true
  )
)


(defn buildClientsList [data owner]
  (map
    (fn [text]
      (dom/option {:key (:code text) :value (:id text)
                    :onChange #(handle-change % owner)} (:name text))
    )
    (sort (comp comp-clients) (:clients @app-state )) 
  )
)


(defn buildSecsList [data owner]
  (map
    (fn [text]
      (dom/option {:key (:id text) :value (:id text)
                    :onChange #(handle-change % owner)} (:acode text))
    )
    (sort (comp comp-secs) (filter (fn [x] (if (or (= 10 (:assettype x)) (= true (:ismatured x))  (and (= 4 (:view @data)) (= 15 (:assettype x))))  false true)) (:securities @app-state )))
  )
)



(defn setSecsDropDown []
  (jquery
     (fn []
       (-> (jquery "#securities" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#securities" )
         (.selectpicker "val" (:id (first (filter (fn [x] (if (= (:id x) (:selectedsec @app-state)) true false )) (:securities @app-state)) )) )
         (.on "change"
           (fn [e]
             (
               onSecsDropDownChange (.. e -target -id) (.. e -target -value)
             )
           )
         )
       )
     )
   )
)

(defn setCalcSecsDropDown []
  (swap! app-state assoc :state 0)
  ;(set! ( . (.getElementById js/document "btnrefresh") -disabled) true)
  (jquery
     (fn []
       (-> (jquery "#securities" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#securities" )
         (.selectpicker "val" (:id (first (filter (fn [x] (if (= (:id x) (:selectedsec @app-state)) true false )) (:securities @app-state)) )) )
         (.on "change"
           (fn [e]
             (
               onCalcSecsDropDownChange (.. e -target -id) (.. e -target -value)
             )
           )
         )
       )
     )
   )



  (jquery
     (fn []
       (-> (jquery "#currencies" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#currencies" )
         (.selectpicker "val" (:selectedcurrency @app-state))
         (.on "change"
           (fn [e]
             (
               onCalcCurrenciesDropDownChange (.. e -target -id) (.. e -target -value)
             )
           )
         )
       )
     )
   )
)

(defn setClientsDropDown []  
  (jquery
     (fn []
       (-> (jquery "#clients" )
         (.selectpicker {})
       )
     )
   )
   (jquery
     (fn []
       (-> (jquery "#clients" )
         (.selectpicker "val" (:id (first (filter (fn [x] (if (= (:code x) (:selectedclient @app-state)) true false )) (:clients @app-state)) )) )
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



(defn onDidUpdate [data]
  (setClientsDropDown)
    ;; (jquery
    ;;   (fn []
    ;;     (-> (jquery "#side-menu")
    ;;       (.metisMenu)
    ;;     )
    ;;   )
    ;; )

)

(defn onMount [data]
  (.log js/console "Mount core happened")
  (setClientsDropDown)
)


(defcomponent users-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/div {:className "navbar navbar-default navbar-fixed-top navbar-inverse" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )


        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}
          (dom/ul {:className "nav navbar-nav" :style {:margin-top "9px"}}
            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )


            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (dom/li
              (dom/a {:href "/#/users"}
                (dom/i {:className "fa fa-key"})
                "משתמשים והרשאות"
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "ניהול מערכת"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                        (dom/i {:className "fa fa-users"})
                        "ניהול קבוצות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/devices" :className "menu_item"}
                        (dom/i {:className "fa fa-hdd-o"})
                        "מאגר יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/contacts" :className "menu_item"}
                        (dom/i {:className "fa fa-phone"})
                        "אנשי קשר"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/polygons" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "ניהול Polygons"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
                      )
                    )
                  )
                )
              )
            )
            (dom/li
              (dom/h5 {:style {:margin-left "5px" :margin-right "5px" :height "32px" :margin-top "1px"}} " "
      (dom/input {:id "search" :type "text" :placeholder "Search by last first login" :style {:height "24px" :margin-top "10px"} :value  (:search @app-state) :onChange (fn [e] (handleChange e))}))
            )
          )        
        )
      )
    )
  )
)


(defcomponent roledetail-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/div {:className "navbar navbar-default navbar-fixed-top" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}
          (dom/ul {:className "nav navbar-nav" :style {:margin-top "9px"}}
            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )


            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (dom/li
              (dom/a {:href "/#/users"}
                (dom/i {:className "fa fa-key"})
                "משתמשים והרשאות"
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "ניהול מערכת"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                        (dom/i {:className "fa fa-users"})
                        "ניהול קבוצות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/devices" :className "menu_item"}
                        (dom/i {:className "fa fa-hdd-o"})
                        " מאגר יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/contacts" :className "menu_item"}
                        (dom/i {:className "fa fa-phone"})
                        "אנשי קשר"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/polygons" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "ניהול Polygons"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
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


(defn notificationsclick []
  (let [
    tr1 (.log js/console (str "gggg"))
    ]

    (swap! app-state assoc :isnotification (if (:isnotification @app-state) false true))
    (swap! app-state assoc :isalert false)
    (go
      (<! (timeout 100))
      (js/google.maps.event.trigger (:map @app-state) "resize")
    )
  )
)

(defn alertsclick []
  (let []
                                                                                   (swap! app-state assoc :isalert (if (:isalert @app-state) false true))
                                                                                   (swap! app-state assoc :isnotification false)
     
    (go
      (<! (timeout 100))
      (js/google.maps.event.trigger (:map @app-state) "resize")
    )
  )
)

(defcomponent map-navigation-view [data owner]  
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ;tr1 (.log js/console (str "in map navigation"))
      role (:id (:role (first (filter (fn [x] (if (= (:userid x) (:userid (:token @app-state))) true false)) (:users @app-state)))))
      ]
      (dom/div {:className "navbar navbar-toggleable-md navbar-inverse bg-inverse navbar-fixed-top" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}

          (dom/ul {:className "nav navbar-nav"}

            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )

            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (if (not= role settings/dispatcherrole)
              (dom/li {:className "dropdown"}
                (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                  (dom/span {:className "caret"})
                  (dom/i {:className "fa fa-archive"})
                  "ניהול מערכת"
                )
                (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                  (dom/li
                    (dom/div {:className "row"}
                      (dom/div {:className "col-md-12"}
                        (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                          (dom/i {:className "fa fa-users"})
                          "ניהול קבוצות"
                        )
                      )
                    )
                  )

                  (if (not= role settings/dispatcherrole)
                    (dom/li
                      (dom/a {:href "/#/users" :onClick (fn [e] (goUsers e))}
                        (dom/i {:className "fa fa-key"})
                        "משתמשים והרשאות"
                      )
                    )
                  ) 


                  (dom/li
                    (dom/div {:className "row"}
                      (dom/div {:className "col-md-12"}
                        (dom/a {:href "/devices" :className "menu_item"}
                          (dom/i {:className "fa fa-hdd-o"})
                          " מאגר יחידות"
                        )
                      )
                    )
                  )

                  (dom/li
                    (dom/div {:className "row"}
                      (dom/div {:className "col-md-12"}
                        (dom/a {:href "/#/roles" :className "menu_item" :onClick (fn [e] (goRoles e))}
                          (dom/i {:className "fa fa-phone"})
                          "אנשי קשר"
                        )
                      )
                    )
                  )

                  (dom/li
                    (dom/div {:className "row"}
                      (dom/div {:className "col-md-12"}
                        (dom/a {:href "/polygons" :className "menu_item"}
                          (dom/i {:className "fa fa-globe"})
                          "ניהול Polygons"
                        )
                      )
                    )
                  )
                )
              )


            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
                      )
                    )
                  )
                )
              )
            )


            (dom/li {:className "dropdown" :style {:margin-right "0px" :background-color "grey"}}
              (dom/a {:className "dropdown-toggle" :data-toggle "dropdown" :href "#" :aria-expanded "false" :style {:color "red"}}
                (b/button {:className "btn btn-danger" :style {:border-radius "25px" :margin-top "-40px"} :onClick (fn [e] (notificationsclick))} (str (count (:notifications @data))))
                (dom/span {:style {:color "white"} :onClick (fn [e] (alertsclick))} " התראות ")
                (dom/i {:className "fa fa-bell fa-fw" :style {:font-size "24px" :color "red"}})   
              )
              ;(om/build notifications-navbar data {})
            )

            (dom/li {:className "dropdown" :style {:background-color "grey" :margin-right "0px"}}
              (dom/a {:className "dropdown-toggle" :data-toggle "dropdown" :href "#" :aria-expanded "false" :style {:color "red"}}

                (b/button {:className "btn btn-danger" :style {:border-radius "25px" :margin-top "-40px"} :onClick (fn [e] (alertsclick))} (str (count (:alerts @data))))
                (dom/span {:style {:color "white"}} " תקלות ")
                (dom/i {:className "fa fa-exclamation-circle fa-fw" :style {:color "red" :font-size "24px"}})
                
              )
              ;(om/build alerts-navbar data {})
            )
          )

          (dom/ul {:className "nav navbar-nav navbar-left"}

            (dom/li (dom/h5 {:style {:padding-top "10px" :color "blue"}} "שירות לקוחות 03-123-456-789"))
            (dom/li (dom/a {:href "/#/login" :style {:padding-top "18px"}} "יְצִיאָה"))
          )
        )
      )
    )
  )
)

(defcomponent dashboard-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/div {:className "navbar navbar-toggleable-md navbar-inverse bg-inverse navbar-fixed-top" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}
          (dom/ul {:className "nav navbar-nav" :style {:margin-top "9px"}}
            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )


            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (dom/li
              (dom/a {:href "/#/users"}
                (dom/i {:className "fa fa-key"})
                "משתמשים והרשאות"
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "ניהול מערכת"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                        (dom/i {:className "fa fa-users"})
                        "ניהול קבוצות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/devices" :className "menu_item"}
                        (dom/i {:className "fa fa-hdd-o"})
                        " מאגר יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/contacts" :className "menu_item"}
                        (dom/i {:className "fa fa-phone"})
                        "אנשי קשר"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/polygons" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "ניהול Polygons"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
                      )
                    )
                  )
                )
              )
            )
            (dom/li
              (dom/h5 {:style {:margin-left "5px" :margin-right "5px" :height "32px" :margin-top "1px"}} " "
      (dom/input {:id "search" :type "text" :placeholder "Search" :style {:height "24px" :margin-top "12px"} :value  (:search @app-state) :onChange (fn [e] (handleChange e )) })  )
            )
          )        
        )
      )
    )
  )
)

(defcomponent userdetail-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/div {:className "navbar navbar-default navbar-fixed-top" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}
          (dom/ul {:className "nav navbar-nav" :style {:margin-top "9px"}}
            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )


            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (dom/li
              (dom/a {:href "/#/users"}
                (dom/i {:className "fa fa-key"})
                "משתמשים והרשאות"
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "ניהול מערכת"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                        (dom/i {:className "fa fa-users"})
                        "ניהול קבוצות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/devices" :className "menu_item"}
                        (dom/i {:className "fa fa-hdd-o"})
                        " מאגר יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/contacts" :className "menu_item"}
                        (dom/i {:className "fa fa-phone"})
                        "אנשי קשר"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/polygons" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "ניהול Polygons"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
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
)

(defcomponent settings-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/nav {:className "navbar navbar-default navbar-fixed-top" :role "navigation"}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-ex1-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/span {:id "pageTitle"} (:text (:current @data)) )
          )
        )
        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "menu"}
          (dom/ul {:className "nav navbar-nav" :style {:padding-top "17px" :visibility (if (= (compare (:name (:current @app-state))  "Settings") 0) "visible" "hidden")}}
            (dom/li
              (dom/h5 {:style {:margin-left "5px" :margin-right "5px" :height "32px" :margin-top "1px"}} " "
      (dom/input {:id "search" :type "text" :placeholder "Search" :style {:height "32px" :margin-top "1px"} :value  (:search @app-state) :onChange (fn [e] (handleChange e )) })  )
            )
          )

          (dom/ul {:className "nav navbar-nav navbar-right"}
            (dom/li {:className "dropdown"}
              (dom/a {:className "dropdown-toggle" :data-toggle "dropdown"  :aria-expanded "false" }
                 (dom/i {:className "fa fa-exchange"})
                 (dom/i {:className "fa fa-caret-down"})
              )
              (dom/ul {:className "dropdown-menu dropdown-messages"}
                (dom/li 
                  (dom/a {:style {:cursor "pointer" :pointer-events (if (nil? (:selectedclient @app-state)) "none" "all")} :onClick (fn [e] (printMonth) )}
                    (dom/div
                      (dom/i {:className "fa fa-print"})
                      "Печать"
                    )
                  )
                )
              )
            )
            (dom/li {:className "dropdown"}
              (dom/a {:className "dropdown-toggle" :data-toggle "dropdown"  :aria-expanded "false" }
                 (dom/i {:className "fa fa-tasks fa-fw"})
                 (dom/i {:className "fa fa-caret-down"})
              )
              (dom/ul {:className "dropdown-menu dropdown-tasks"}
                (dom/li
                  (dom/a {:href "#/positions" :onClick (fn [e] (goMap e))}
                    (dom/div
                      (dom/i {:className "fa fa-comment fa-fw"})
                      "Позиции"
                    )
                  )
                )
                (dom/li {:className "divider"})
                (dom/li
                  (dom/a {:href "#/portfolios/0" :onClick (fn [e] (goUserDetail e))}
                    (dom/div
                      (dom/i {:className "fa fa-twitter fa-fw"})
                      "Держатели бумаги"
                    )
                  )
                )
                (dom/li {:className "divider"})
                (dom/li
                  (dom/a {:href "#/calcportfs" :onClick (fn [e] (goUserDetail e))}
                    (dom/div
                      (dom/i {:className "fa fa-tasks fa-fw"})
                      "Расчеты"
                    )
                  )
                )


                (dom/li {:className "divider"})
                (dom/li
                  (dom/a {:href (str settings/apipath "tradeidea/" (:token (:token @app-state)))  :target "_blank"}
                    (dom/div
                      (dom/i {:className "fa fa-desktop fa-fw"})
                      "Редактор торговой идеи"
                    )
                  )
                )
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:className "dropdown-toggle" :data-toggle "dropdown"  :aria-expanded "false" }
                 (dom/i {:className "fa fa-user fa-fw"})
                 (dom/i {:className "fa fa-caret-down"})
              )
              (dom/ul {:className "dropdown-menu dropdown-user"}
                (dom/li
                  (dom/a {:href "#/login"}
                    (dom/div
                      (dom/i {:className "fa fa-sign-out fa-fw"})
                      "Выход"
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



(defcomponent devdetail-navigation-view [data owner]
  (render [_]
    (let [style {:style {:margin "10px" :padding-bottom "0px"}}
      stylehome {:style {:margin-top "10px"} }
      ]
      (dom/div {:className "navbar navbar-default navbar-fixed-top" :role "navigation" :style {:height "70px"}}
        (dom/div {:className "navbar-header"}
          (dom/button {:type "button" :className "navbar-toggle"
            :data-toggle "collapse" :data-target ".navbar-collapse"}
            (dom/span {:className "sr-only"} "Toggle navigation")
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
            (dom/span {:className "icon-bar"})
          )
          (dom/a {:className "navbar-brand" :style {:padding-top "5px"}}
            (dom/img {:src "images/loginbackground.png" :className "img-responsive company-logo-logon" :style {:width "113px" :height "53px"}})
            ;;(dom/span {:id "pageTitle"} "Beeper")
          )          
        )

        (dom/div {:className "collapse navbar-collapse navbar-ex1-collapse" :id "bs-example-navbar-collapse-1"}
          (dom/ul {:className "nav navbar-nav" :style {:margin-top "9px"}}
            (dom/li
              (dom/a {:href "/#/map" :onClick (fn [e] (goMap e))}
                (dom/i {:className "fa fa-map-o"})
                "מפה"
              )
            )
            (dom/li
              (dom/a {:href "/#/dashboard" :onClick (fn [e] (goDashboard e))}
                (dom/i {:className "fa fa-dashboard"})
                "Dashboard"
              )
            )


            (dom/li
              (dom/a {:href "/#/devslist" :onClick (fn [e] (goDevslist e))}
                (dom/i {:className "fa fa-building"})
                "רשימה יחידות"
              )
            )

            (dom/li
              (dom/a {:href "/#/users"}
                (dom/i {:className "fa fa-key"})
                "משתמשים והרשאות"
              )
            )

            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "ניהול מערכת"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/groups" :className "menu_item" :onClick (fn [e] (goGroups e))}
                        (dom/i {:className "fa fa-users"})
                        "ניהול קבוצות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/devices" :className "menu_item"}
                        (dom/i {:className "fa fa-hdd-o"})
                        " מאגר יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/contacts" :className "menu_item"}
                        (dom/i {:className "fa fa-phone"})
                        "אנשי קשר"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/polygons" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "ניהול Polygons"
                      )
                    )
                  )
                )
              )
            )



            (dom/li {:className "dropdown"}
              (dom/a {:href "#" :className "dropdown-toggle" :data-toggle "dropdown"}
                (dom/span {:className "caret"})
                (dom/i {:className "fa fa-archive"})
                "דו״חות"
              )
              (dom/ul {:id "login-dp2" :className "dropdown-menu"}
                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/#/reportunits" :className "menu_item"}
                        (dom/i {:className "fa fa-line-chart"})
                        "דו״ח זמינות יחידות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.triggeredAlerts" :className "menu_item"}
                        (dom/i {:className "fa fa-bullhorn"})
                        "דו״ח תרגולים"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.notifications" :className "menu_item"}
                        (dom/i {:className "fa fa-envelope-o"})
                        "דו״ח דיוור התראות"
                      )
                    )
                  )
                )

                (dom/li
                  (dom/div {:className "row"}
                    (dom/div {:className "col-md-12"}
                      (dom/a {:href "/report.senselog" :className "menu_item"}
                        (dom/i {:className "fa fa-globe"})
                        "דו״ רעידות אדמה"
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
  (map-navigation-view data owner)
)

(defmethod website-view 3
  [data owner] 
  ;(.log js/console "Two is found in view")
  (users-navigation-view data owner)
)

(defmethod website-view 4
  [data owner] 
  ;(.log js/console "One is found in view")
  (userdetail-navigation-view app-state owner)
)

(defmethod website-view 5
  [data owner] 
  ;(.log js/console "One is found in view")
  (settings-navigation-view data owner)
)

(defmethod website-view 6
  [data owner] 
  ;(.log js/console "One is found in view")
  (roledetail-navigation-view data owner)
)

(defmethod website-view 7
  [data owner] 
  ;(.log js/console "One is found in view")
  (devdetail-navigation-view data owner)
)

(defmethod website-view 8
  [data owner] 
  ;(.log js/console "One is found in view")
  (dashboard-navigation-view data owner)
)
