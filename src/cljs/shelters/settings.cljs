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


(def apipath "http://52.14.180.219:5050/")
;(def apipath "https://virtserver.swaggerhub.com/Inocodes/Shelters/1.0.0/")
(def demouser "beeper")
(def demopassword "123456")
