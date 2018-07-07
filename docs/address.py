#!/usr/bin/python
# -*- coding: utf-8 -*-

import random
from flask import Flask, jsonify, request
from flasgger import Swagger
from datetime import datetime, timedelta
import pandas as pd
from flask_jwt_extended import (create_access_token,
    create_refresh_token, jwt_required, jwt_refresh_token_required,
    get_jwt_identity, get_raw_jwt)

app = Flask(__name__)
Swagger(app)

import pickle
import os.path
from flask_jwt_extended import JWTManager
app.config['JWT_SECRET_KEY'] = 'jwt-secret-string'
jwt = JWTManager(app)


from sklearn.neighbors import KNeighborsClassifier
from sklearn.neighbors import NearestNeighbors

cbt = 1
cby = 1
cta = 1
cd = 1


import sys
import urllib.request
import urllib.parse
from urllib.parse import urlencode
import json

from flask_restful import reqparse, request, abort, Api, Resource
from flask_cors import CORS



CORS(app)


def normalize(df):
    result = df.copy()
    coeff = 1
    for feature_name in df.columns:
        max_value = df[feature_name].max()
        min_value = df[feature_name].min()
        if feature_name == 'BuildingType':
           coeff = cbt
        elif feature_name == 'BuildingYear':
           coeff = cby
        elif feature_name == 'Lon':
           coeff = cd
        elif feature_name == 'Lat':
           coeff = cd
        elif feature_name == 'TotalArea':
           coeff = cta
        result[feature_name] = (df[feature_name] - min_value) / (max_value - min_value)
        result[feature_name] = coeff * result[feature_name]
    return result

def getzkh(address):
  import MySQLdb
  db = MySQLdb.connect("localhost","root","Qwerty123","fincase" )
  db.set_character_set('utf8')
  #print('will search zkh for address: ' + address)
  project = ''
  house = ''
  foundation = ''
  housetype = ''
  buildingyear = 0
  sql = 'SELECT project_type, house_type, foundation_type, wall_material, \
      floor_type, floor_count_max, built_year, \
      exploitation_start_year, formalname_region from zkh where fulladdress like "' + address + '"'
  cursor = db.cursor()
  cursor.execute('SET NAMES utf8;')
  cursor.execute('SET CHARACTER SET utf8;')
  cursor.execute('SET character_set_connection=utf8;')
  #print(sql)
  cursor.execute(sql)
  # Fetch all the rows in a list of lists.
  results = cursor.fetchall()

  storeysnum = 9
  if len(results) > 0:
    #print('Retrieved data from zkh')
    for data in results:

      if data[3] == 'Каменные, кирпичные':
        housetype = "кирпичный"
      elif data[3] == 'Панельные':
        housetype = 'панельный'
      elif data[3] == 'Монолитные':
        housetype = 'монолитный'
      elif data[3] == 'Блочные':
        housetype = 'блочный'
      elif data[3] == 'Деревянные':
        housetype = 'деревянный'
      elif data[3] == 'Смешанные':
        housetype = 'кирпично-монолитный'
      else:
        housetype = 'сталинский'

      buildingyear = data[6]
      storeysnum = data[5]
      project = data[0]
      house = data[1]
      foundation = data[2]
      region = data[8]
  return {'project': project, 'house': house, 'foundation': foundation,
    'housetype': housetype, 'storeysnum' : storeysnum, 'buildingyear': buildingyear,
    'region': region}


def getdadata(address):
  dadata_url = "https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address"
  values = {"query": address, "count": 1}


  req = urllib.request.Request(dadata_url)
  req.add_header('Content-Type', 'application/json; charset=utf-8')
  req.add_header('X-Secret', 'd6bc0e31ee4b878691e051e7fb3ac1cd787fe7d9')
  req.add_header('Authorization', 'Token 22a558542ffe2926bc0b384fa349ecad8885365b')

  jsondata = json.dumps(values)
  jsondataasbytes = jsondata.encode('utf-8')
  contents = urllib.request.urlopen(req, jsondataasbytes).read()

  result = json.loads(contents.decode('utf-8'))
  region = result['suggestions'][0]['data']['region']
  area = result['suggestions'][0]['data']['area']
  city = result['suggestions'][0]['data']['city']
  if city == 'Санкт-Петербург' or city == 'Москва' or city == 'Санкт-Петербург' or city == 'Москва':
      city = result['suggestions'][0]['data']['city_area']

  street_type = result['suggestions'][0]['data']['street_type_full']
  if street_type == None or len(street_type) == 0:
      street_type = result['suggestions'][0]['data']['settlement_type_full']
      street_name = result['suggestions'][0]['data']['settlement']
  else:
      street_name = result['suggestions'][0]['data']['street']
  house = result['suggestions'][0]['data']['house']
  building = result['suggestions'][0]['data']['block']
  apartment = result['suggestions'][0]['data']['flat']
  fulladdress = result['suggestions'][0]['value']
  longitude = result['suggestions'][0]['data']['geo_lon']
  latitude = result['suggestions'][0]['data']['geo_lat']

  #print('fulladdress from dadata: ' + fulladdress + '; longitude: ' + longitude + '; latitude: ' + latitude)

  return {'region': region, 'area': area, 'city': city, 'street_name': street_name, \
   'house': house, 'building': building, 'apartment': apartment, \
   'fulladdress': fulladdress, 'longitude': longitude, 'latitude': latitude}


#import psycopg2
#import psycopg2.extras
#conn_string = "host='37.18.75.197' dbname='omnia' user='postgres' password='Qwerty123'"
#print ("Connecting to database\n  ->%s" % (conn_string))
#conn = psycopg2.connect(conn_string)
#sql = 'select id, "Lat", "Lon", "TotalArea", ' + \
#            'case when "BuildingType" = \'элитный\' then 20 ' + \
#            'when "BuildingType" =  \'монолитный\' then 18 ' + \
#            'when "BuildingType" =  \'кирпично-монолитный\' then 19 ' + \
#            'when "BuildingType" =  \'сталинский\' then 12 ' + \
#            'when "BuildingType" =  \'панельный\' then 5 ' + \
#            'when "BuildingType" =  \'кирпичный\' then 15 ' + \
#            'when "BuildingType" =  \'блочный\' then 17  ' + \
#            'when "BuildingType" =  \'металлоконструкции \' then 3 ' + \
#            'when "BuildingType" =  \'железобетон\' then 18 ' + \
#            'else 4 end as "BuildingType",  ' + \
#            '"BuildingYear", "Price"/"TotalArea" as "Price" from "newParsers" where ' + \
#            '"City"=\'Москва\' and "Lat" is not null and "Lon" is not NULL ' + \
#            'and "BuildingYear" is not null and "BuildingYear" > 0 and "TotalArea" is not null and "TotalArea" > 0 ' + \
#            'and "BuildingType" != \'\' and "Price" is not null'
#df_moscow = pd.read_sql(sql, con = conn)

def getparams(region, lat, lon, totalarea, buildingtype, buildingyear, analogscount):
    #address = request.args.get('address', type = str)
    import psycopg2
    import psycopg2.extras
    conn_string = "host='localhost' dbname='omnia' user='postgres' password='Qwerty123'"
    #print ("Connecting to database\n  ->%s" % (conn_string))
    conn = psycopg2.connect(conn_string)
    #print('lat=' + str(lat) + '; lon=' + str(lon))
    lat1 = lat - 0.03
    lat2 = lat + 0.03
    lon1 = lon - 0.03
    lon2 = lon + 0.03
    filename = 'calcdata/knn_' + region + '_' + str(analogscount) + '.data'
    dffile = 'calcdata/df_' + region + '_' + '.data'
    if os.path.isfile(dffile):
        data = pickle.load(open(dffile, "rb"))
        print('loaded df from file')

    if os.path.isfile(filename):
        neigh = pickle.load(open(filename, "rb"))
        print('loaded neigh from file')
    else:
        sql = 'select id, "Lat", "Lon", "TotalArea", ' + \
            'case when "BuildingType" = \'элитный\' then 20 ' + \
            'when "BuildingType" =  \'монолитный\' then 18 ' + \
            'when "BuildingType" =  \'кирпично-монолитный\' then 19 ' + \
            'when "BuildingType" =  \'сталинский\' then 12 ' + \
            'when "BuildingType" =  \'панельный\' then 5 ' + \
            'when "BuildingType" =  \'кирпичный\' then 15 ' + \
            'when "BuildingType" =  \'блочный\' then 17  ' + \
            'when "BuildingType" =  \'металлоконструкции \' then 3 ' + \
            'when "BuildingType" =  \'железобетон\' then 18 ' + \
            'else 4 end as "BuildingType",  ' + \
            '"BuildingYear", "Price"/"TotalArea" as "Price" from "newParsers" where ' + \
            '"Region"=\'' + region + '\' and "Lat" is not null and "Lon" is not NULL ' + \
            'and "BuildingYear" is not null and "BuildingYear" > 0 and "TotalArea" is not null and "TotalArea" > 0 ' + \
            'and "BuildingType" != \'\' and "Price" is not null'
        df = pd.read_sql(sql, con = conn) #df_moscow #.loc[df_moscow['BuildingType'] == buildingtype]  #pd.read_sql(sql, con = conn)
        if os.path.isfile(dffile) == False:
            data = {}

            data['maxlat'] = df['Lat'].max()
            data['minlat'] = df['Lat'].min()

            data['maxlon'] = df['Lon'].max()
            data['minlon'] = df['Lon'].min()

            data['maxarea'] = df['TotalArea'].max()
            data['minarea'] = df['TotalArea'].min()

            data['maxyear'] = df['BuildingYear'].max()
            data['minyear'] = df['BuildingYear'].min()

            data['maxtype'] = df['BuildingType'].max()
            data['mintype'] = df['BuildingType'].min()
            print(data)
            data['ids'] = df['id']

            pickle.dump(data, open(dffile, "wb"))
        normdf = normalize(df)
        #neigh = KNeighborsClassifier(n_neighbors=10)
        X = normdf.drop(['id', 'Price'], axis=1).values.tolist()   #, 'BuildingType'
        y = normdf['Price'].values.tolist()

        neigh = NearestNeighbors(n_neighbors=analogscount)
        neigh.fit(X)
        pickle.dump(neigh, open(filename, "wb"))

    max_value = data['maxlat']
    min_value = data['minlat']
    newlat = cd * (lat - min_value)/(max_value - min_value)

    max_value = data['maxlon']
    min_value = data['minlon']
    newlon = cd * (lon - min_value)/(max_value - min_value)

    max_value = data['maxarea']
    min_value = data['minarea']
    newtotalarea = cta * (totalarea - min_value)/(max_value - min_value)

    max_value = data['maxyear']
    min_value = data['minyear']
    newbuildingyear = cby * (buildingyear - min_value) / (max_value - min_value)

    max_value = data['maxtype']
    min_value = data['mintype']
    newbuildingtype = cbt * (buildingtype - min_value) / (max_value - min_value)

    res = neigh.kneighbors([[newlat, newlon, newtotalarea, newbuildingtype, newbuildingyear]])
    wstr = ''
    for i in range(len(res[1][0])):
        if len(wstr) > 0:
            wstr = wstr + ' or '
        wstr = wstr + ' id = ' + str(data['ids'][res[1][0][i]]) #str(df.iloc[res[1][0][i]]['id'])

    #sql = 'select "BuildingType", "Price", "TotalArea", "City", "Lat", "Lon", "id", "RoomsNum", "Storey", \
    #  "StoreysNum", "RawAddress", "RegionDistrict", "RepairRaw", "BuildingPeriod", "LivingSpaceArea", \
    #  "KitchenArea", "SubwayDistance", 0, 0, "ScreenshotFilePath" from public."newParsers" where \
    #  "Lat" > ' + str(lat1)  + ' and "Lat" < ' + str(lat2) + ' and "Lon" > ' + str(lon1) +  \
    #  ' and "Lon" < ' +  str(lon2) + ' and "ScreenshotFilePath" != \'\' limit 10'
    sql = 'select "BuildingType", "Price", "TotalArea", "City", "Lat", "Lon", "id", "RoomsNum", "Storey", \
      "StoreysNum", "RawAddress", "RegionDistrict", "RepairRaw", "BuildingYear" as "BuildingPeriod", "LivingSpaceArea", \
      "KitchenArea", "SubwayDistance", 0, 0, "ScreenshotFilePath" from public."newParsers" where '
    sql = sql + wstr
    #' and "Lon" > ' + str(lon1) + ' and "Lon" < ' +  str(lon2) 
    df = pd.read_sql_query(sql, con=conn)
    df['pricepermetr']=df['Price']/df['TotalArea']
    df = df.sort_values(by=['pricepermetr'])
    totalcount = df.count().BuildingType
    calcdf = df[(int (totalcount / 2) - 5):(int (totalcount / 2) + 5)]
    analogs = []
    calcanalogs = []
    pricepermetr = 0
    for i in range(totalcount):
        pricepermetr += df.iloc[i][1] / df.iloc[i][2]
        housetype =  df.iloc[i][0]
        price = df.iloc[i][1]
        totalsquare = df.iloc[i][2]
        city = df.iloc[i][3]
        lat = df.iloc[i][4]
        lon = df.iloc[i][5]
        id = df.iloc[i][6]
        roomsnum = df.iloc[i][7]
        storey = df.iloc[i][8]
        storeysnum = df.iloc[i][9]
        fulladdress = df.iloc[i][10]
        district = df.iloc[i][11]
        repair = df.iloc[i][12]
        buildingyear = df.iloc[i][13]
        leavingsquare = df.iloc[i][14]
        kitchensquare = df.iloc[i][15]
        metrodistance = df.iloc[i][16]
        analogstatus = df.iloc[i][17]
        analogindex = df.iloc[i][18]
        screenshot  = df.iloc[i][19]
        analogs.append( [housetype, price, totalsquare, city, lat, lon, id, roomsnum, 
          storey, storeysnum, fulladdress, district, repair, buildingyear, leavingsquare,
          kitchensquare, metrodistance, analogstatus, analogindex,screenshot ] )


    for i in range(calcdf.count().Price):
        pricepermetr += calcdf.iloc[i][1] / df.iloc[i][2]
        housetype =  calcdf.iloc[i][0]
        price = calcdf.iloc[i][1]
        totalsquare = calcdf.iloc[i][2]
        city = calcdf.iloc[i][3]
        lat = calcdf.iloc[i][4]
        lon = calcdf.iloc[i][5]
        id = calcdf.iloc[i][6]
        roomsnum = calcdf.iloc[i][7]
        storey = calcdf.iloc[i][8]
        storeysnum = calcdf.iloc[i][9]
        fulladdress = calcdf.iloc[i][10]
        district = calcdf.iloc[i][11]
        repair = calcdf.iloc[i][12]
        buildingyear = calcdf.iloc[i][13]
        leavingsquare = calcdf.iloc[i][14]
        kitchensquare = calcdf.iloc[i][15]
        metrodistance = calcdf.iloc[i][16]
        analogstatus = calcdf.iloc[i][17]
        analogindex = calcdf.iloc[i][18]
        screenshot  = calcdf.iloc[i][19]
        calcanalogs.append( [housetype, price, totalsquare, city, lat, lon, id, roomsnum, 
          storey, storeysnum, fulladdress, district, repair, buildingyear, leavingsquare,
          kitchensquare, metrodistance, analogstatus, analogindex,screenshot ] )

    result = {'analogs': analogs, 'pricePerMetr': 8888, 'houseAvrgPrice': 6666,
      'regionAvrgPrice': 7777, 'cityAvrgPrice': 2222,
      'data': pricepermetr/df.count().BuildingType * totalarea,
      'calcanalogs': calcanalogs}
    return result

@app.route('/api/login', methods=['POST'])
def get_token():
    """
    This is FinCase RealEstate API
    Вызовите этот метод и передайте имя пользователя и пароль в качестве параметра
    ---
    tags:
      - Финкейс Жилая Недвижимость API
    parameters:
      - in: body
        name: user
        schema:
            type: object
            required:
                - username
            properties:
                username:
                    type: string
                password:
                    type: string
    responses:
      500:
        description: Ошибка, адрес некорректный
      200:
        description: Токен для доступа к ресурсам
        schema:
          id: login_params
          properties:
            token:
              type: string
              description: Код досутпа
              default: ''
    """
    parser = reqparse.RequestParser()
    parser.add_argument('username', help = 'This field cannot be blank', required = True)
    parser.add_argument('password', help = 'This field cannot be blank', required = True)
    data = parser.parse_args()

    if (data.username == 'tinkoff' and data.password == 'password') or \
       (data.username == 'fincase' and data.password == 'password'):
        expires = timedelta(days=1)
        access_token = create_access_token(identity = data['username'],
            expires_delta=expires)
    else:
        access_token = ''
    result = {'token': access_token}
    return jsonify(
        result
    )


def map_analog(analog):
    return {'houusetype': analog[0],
        'price': analog[1],
        'totalsquare ': analog[2],
        'city': analog[3],
        'lat': analog[4],
        'lon': analog[5],
        'roomsnum': analog[6],
        'storey': analog[7],
        'storeysnum': analog[8],
        'fulladdress': analog[9],
        'district': analog[10],
        'repair': analog[11],
        'buildingyear': analog[12],
        'leavingsquare': analog[13],
        'kitchensquare': analog[14],
        'metrodistance': analog[15],
        'analogstatus': analog[16],
        'analogindex': analog[17]
    }

@app.route('/api/getparams', methods=['GET'])
@jwt_required
def get_params():
    """
    This is FinCase RealEstate API
    Вызовите этот метод и передайте адрес в качестве параметра
    ---
    tags:
      - Финкейс Жилая Недвижимость API
    parameters:
    parameters:
      - in: header
        name: Authorization
        type: string
        required: true
      - name: totalsquare
        in: query
        type: number
        required: true
        description: Площадь квартиры
      - name: repairRaw
        in: query
        type: string
        required: true
        description: Ремонт
      - name: longitude
        in: query
        type: number
        required: true
        description: Долгота расположения
      - name: latitude
        in: query
        type: number
        required: true
        description: Широта расположения
      - name: housetype
        in: query
        type: string
        required: true
        description: Тип дома
      - name: city
        in: query
        type: string
        required: true
        description: Город
      - name: buildingyear
        in: query
        type: integer
        required: true
        description: Год постройки
      - name: storey
        in: query
        type: integer
        required: true
        description: Этаж
      - name: storeysnum
        in: query
        type: integer
        required: true
        description: Количество этажей в здании
      - name: storeysnum
        in: query
        type: integer
        required: true
        description: Количество этажей в здании
    definitions:
      Analog:
            type: object
            properties:
              id:
                type: integer
                description: Идентификатор объекта
                default: 10
              housetype:
                type: string
                description: Материал здания
                default: 'панельный'
              price:
                type: number
                description: Цена объекта
                default: 2000.99
              totalsquare:
                type: float
                description: Общая площадь объета
                default: 100.0
              leavingsquare:
                type: float
                description: Жилая площадь объета
                default: 100.0
              kitchensquare:
                type: float
                description: Площадь кухни объета
                default: 100.0
              city:
                type: string
                description: Город
                default: 'Москва'
              repair:
                type: string
                description: Тип ремонта
                default: 'косметический'
              lat:
                type: float
                description: Широта расположения объекта
                default: 55.89
              lon:
                type: float
                description: Долгота расположения объекта
                default: 37.89
              buildingyear:
                type: integer
                description: Год постройки
                default: 2000
              ceilingheight:
                type: float
                description: Высота потолков
                default: 2.71
              floor:
                type: integer
                description: Этаж расположения
                default: 2
              storeys:
                type: integer
                description: Количество этажей в здании
                default: 9
              fulladdress:
                type: string
                description: Адрес объекта
                default: 'Москва'
    responses:
      500:
        description: Ошибка, адрес некорректный
      200:
        description: Наиболее полный набор параметров и оценка стоимости объекта
        schema:
          id: object_params
          properties:
            analogs:
              type: array
              description: Аналогичные объекты искомому
              items:
                  $ref: '#/definitions/Analog'
            buildingyear:
              type: integer
              description: Год постройки объекта
              default: 2000
            ceilingheight:
              type: float
              description: Высота потолков
              default: 2.70
            city:
              type: string
              description: Город нахождения объекта
              default: 'Москва'
            housetype:
              type: string
              description: Материал здания
              default: 'панельный'
            latitude:
              type: float
              description: Географическая широта объекта
              default: 55.751244
            longitude:
              type: float
              description: Географическая долгота объекта
              default: 37.618423
            storey:
              type: integer
              description: Этаж расположения объекта
              default: 7
            storeysnum:
              type: integer
              description: Количество этажей в здании
              default: 9
            totalsquare:
              type: float
              description: Общая площадь объета
              default: 100.0
            cadCost:
              type: float
              description: Кадастровая стоимость объекта
              default: 1000000.00
            priceDivergency:
              type: float
              description: Отклонение кадастровой стоимости от рыночной в процентах
              default: 3.09
            price:
              type: float
              description: Текущая цена объекта
              default: 1000000.00
            pricePerMetr:
              type: float
              description: Стоимость квадратного метра объекта
              default: 1000000.00
            houseAvrgPrice:
              type: float
              description: Средняя цена квадратного метра в доме
              default: 1000000.00
            regionAvrgPrice:
              type: float
              description: Средняя цена квадратного метра в районе
              default: 1000000.00
            cityAvrgPrice:
              type: float
              description: Средняя цена квадратного метра в городе
              default: 1000000.00

    """

    lat = request.args.get('latitude', default = 55.751244, type = float)
    lon = request.args.get('longitude', default = 37.618423, type = float)
    buildingyear = request.args.get('buildingyear', default = 2000, type = int)
    buildingtype = request.args.get('housetype', default = 'кирпичный', type = str)
    totalarea = request.args.get('totalsquare', default = 37.618423, type = float)
    region = request.args.get('city', default = 'Москва', type = str)
    analogscount = request.args.get('analogscount', default = 30, type = int)
    if buildingtype == 'элитный':
        buildingtype = 20
    elif buildingtype == 'монолитный':
       buildingtype = 18
    elif buildingtype == 'сталинский':
       buildingtype = 12
    elif buildingtype == 'панельный':
       buildingtype = 5
    elif buildingtype == 'кирпичный':
       buildingtype = 15
    elif buildingtype == 'блочный':
       buildingtype = 17
    elif buildingtype == 'металлоконструкции':
       buildingtype = 3
    elif buildingtype == 'железобетон':
       buildingtype = 18
    elif buildingtype == 'кирпично-монолитный':
       buildingtype = 19
    elif buildingtype == 'кирпично-монолитный':
       buildingtype = 19
    else:
       buildingtype = 4
    print('buildingtype:' + str(buildingtype))
    if analogscount < 10:
        analogscount = 10
    result = getparams(region, lat, lon, totalarea, buildingtype, buildingyear, analogscount)
    return jsonify(
        result
    )


@app.route('/api/getzkh', methods=['GET'])
@jwt_required
def get_zkh_by_address():
    """
    This is FinCase RealEstate API
    Вызовите этот метод и передайте адрес в качестве параметра
    ---
    tags:
      - Финкейс Жилая Недвижимость API
    parameters:
      - name: address
        in: query
        type: string
        required: true
        description: Адрес объекта жилой недвижимости в любом формате
    responses:
      500:
        description: Ошибка, адрес некорректный
      200:
        description: Наиболее полный набор параметров БТИ объекта
        schema:
          id: zkh_params
          properties:
            buildingyear:
              type: integer
              description: Год постройки объекта
              default: 2000
            region:
              type: string
              description: Регион нахождения объекта
              default: 'Москва'
            foundation:
              type: string
              description: Основание здания
              default: 'Ленточный'
            house:
              type: string
              description: Тип дома
              default: 'Многоквартирный дом'
            project:
              type: string
              description: Проект или серия дома
              default: 'Индивидуальный проект'
            housetype:
              type: string
              description: Материал здания
              default: 'панельный'
            storeysnum:
              type: integer
              description: Количество этажей в здании
              default: 9
    """

    address = request.args.get('address', default = '*', type = str)
    fulladdress = getdadata(address)['fulladdress']
    flatpos = fulladdress.find(", кв")
    if flatpos >= 0:
      address = fulladdress[:flatpos]
    else:
      address = fulladdress

    result = getzkh(address)
    #print(result)
    return jsonify(
        result
    )

app.run(debug=True,host='0.0.0.0', port=3001)
