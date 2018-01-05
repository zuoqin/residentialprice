(ns shelters.settings
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [secretary.core :as sec :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
  )
  (:import goog.History)
)

(enable-console-print!)

;;(def apipath "http://10.30.60.102:3000/")
;;(def apipath "https://api.sberpb.com/")


(def apipath "http://18.220.17.183:5050/")
(def socketpath "ws://18.220.17.183:5060")

(def demouser "beeper")
(def demopassword "123456")
(def dispatcherrole "1629d218-9949-431c-b8ab-e4bc374faed3")
