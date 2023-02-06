# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
from bson.decimal128 import Decimal128
from datetime import datetime

import mock
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting

from travel.rasp.suburban_selling.selling.aeroexpress.update_menu import update_menu
from travel.rasp.suburban_selling.selling.aeroexpress.models import SuburbanTariff, AeroexMenu, STANDARD


DIR_NAME = 'aeroexpress'
WSDL_PATH = DIR_NAME + '/aero.wsdl'
FAKE_CRT = DIR_NAME + '/file.crt'
FAKE_KEY = DIR_NAME + '/file.key'
MOCK_URL = 'http://api.aeroexpress.ru:48000/aeapi/services/TicketServiceService.TicketServiceServicePort/'


get_menu_response = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns2:getWwwMenuResponse xmlns:ns2="http://service.ticket.lynx.ru" xmlns:ns3="http://model.ticket.lynx.ru">
         <ns2:getWwwMenuResponse>
            <items>
               <item>
                  <id>14</id>
                  <name>Стандарт</name>
                  <seatsSelect>false</seatsSelect>
                  <children />
                  <lastId>true</lastId>
                  <price>500</price>
                  <label>Тариф</label>
                  <orderType>25</orderType>
                  <maxTickets>10</maxTickets>
                  <maxDays>90</maxDays>
                  <description>1 поездка в вагоне стандартного класса.
Билет действителен на всех трех направлениях от/до московских аэропортов, на дату, указанную в проездном документе.</description>
               </item>
               <item>
                  <id>2</id>
                  <name>Туда Обратно</name>
                  <seatsSelect>false</seatsSelect>
                  <children />
                  <lastId>true</lastId>
                  <price>840</price>
                  <label>Тариф</label>
                  <orderType>34</orderType>
                  <maxTickets>10</maxTickets>
                  <maxDays>90</maxDays>
                  <description>2 поездки в вагоне стандартного класса.
Билет действителен на всех трех направлениях от/до московских аэропортов в течение 30 дней с даты, указанной в проездном документе (включительно).</description>
               </item>
            </items>
         </ns2:getWwwMenuResponse>
      </ns2:getWwwMenuResponse>
   </soapenv:Body>
</soapenv:Envelope>
"""


get_menu_response_one_item = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns2:getWwwMenuResponse xmlns:ns2="http://service.ticket.lynx.ru" xmlns:ns3="http://model.ticket.lynx.ru">
         <ns2:getWwwMenuResponse>
            <items>
               <item>
                  <id>14</id>
                  <name>Стандарт</name>
                  <seatsSelect>false</seatsSelect>
                  <children />
                  <lastId>true</lastId>
                  <price>500</price>
                  <label>Тариф</label>
                  <orderType>25</orderType>
                  <maxTickets>10</maxTickets>
                  <maxDays>90</maxDays>
                  <description>1 поездка в вагоне стандартного класса.
Билет действителен на всех трех направлениях от/до московских аэропортов, на дату, указанную в проездном документе.</description>
               </item>
            </items>
         </ns2:getWwwMenuResponse>
      </ns2:getWwwMenuResponse>
   </soapenv:Body>
</soapenv:Envelope>
"""


@httpretty.activate
@replace_now('2018-10-20 12:35:00')
def test_get_menu():
    stations_pairs = [
        {
            'from': 9600216,
            'to': 2000005,
        },
        {
            'from': 9881841,
            'to': 2000006,
        },

    ]
    with mock.patch('travel.rasp.suburban_selling.selling.aeroexpress.update_menu.STATION_PAIRS', stations_pairs), \
            replace_setting('TARIFFS_ADDITIONS', {14: STANDARD, 2: {}}):
        httpretty.register_uri(httpretty.POST, uri=MOCK_URL, body=get_menu_response)
        update_menu(url=WSDL_PATH, crt_file=FAKE_CRT, key_file=FAKE_KEY)
        tariffs = list(SuburbanTariff.objects.all())

        assert_that(tariffs, contains_inanyorder(
            has_properties({
                'key': has_properties({
                    'express_type': 'aeroexpress',
                    'company': 162,
                    'station_from': 9600216,
                    'station_to': 2000005
                }),
                'data': has_properties({
                    'menu_id': 14,
                    'price': Decimal128('500.00'),
                    'begin_dt': datetime(2018, 10, 19, 21),
                    'end_dt': datetime(2019, 1, 17, 21),
                    'description': STANDARD['description']
                })
            }),
            has_properties({
                'key': has_properties({
                    'station_from': 9600216,
                    'station_to': 2000005
                }),
                'data': has_properties({
                    'menu_id': 2,
                    'price': Decimal128('840.00'),
                    'begin_dt': datetime(2018, 10, 19, 21),
                    'end_dt': datetime(2019, 1, 17, 21)
                })
            }),
            has_properties({
                'key': has_properties({
                    'station_from': 9881841,
                    'station_to': 2000006
                }),
                'data': has_properties({
                    'menu_id': 14,
                    'price': Decimal128('500.00')
                })
            }),
            has_properties({
                'key': has_properties({
                    'station_from': 9881841,
                    'station_to': 2000006
                }),
                'data': has_properties({
                    'menu_id': 2,
                    'price': Decimal128('840.00')
                })
            })
        ))

        menu = list(AeroexMenu.objects.all())

        assert_that(menu, contains_inanyorder(
            has_properties({
                'menu_id': 14,
                'price': 500.00
            }),
            has_properties({
                'menu_id': 2,
                'price': 840.00
            })
        ))

        httpretty.register_uri(httpretty.POST, uri=MOCK_URL, body=get_menu_response_one_item)
        update_menu(url=WSDL_PATH, crt_file=FAKE_CRT, key_file=FAKE_KEY)
        tariffs = list(SuburbanTariff.objects.all())

        assert_that(tariffs, contains_inanyorder(
            has_properties({
                'key': has_properties({
                    'express_type': 'aeroexpress',
                    'company': 162,
                    'station_from': 9600216,
                    'station_to': 2000005
                }),
                'data': has_properties({
                    'menu_id': 14,
                    'price': Decimal128('500.00'),
                    'max_days': 90
                })
            }),
            has_properties({
                'key': has_properties({
                    'station_from': 9881841,
                    'station_to': 2000006
                }),
                'data': has_properties({
                    'menu_id': 14,
                    'price': Decimal128('500.00'),
                    'max_days': 90
                })
            })
        ))

        menu = list(AeroexMenu.objects.all())

        assert_that(menu, contains_inanyorder(
            has_properties({
                'menu_id': 14,
                'price': 500.00
            })
        ))
