# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty

import pytest
from hamcrest import assert_that, contains, has_properties

from common.tester.utils.replace_setting import ReplaceAttr
from travel.rasp.suburban_selling.selling.aeroexpress import client as client_module
from travel.rasp.suburban_selling.selling.aeroexpress.client import (
    get_client, AeroExpressClient, AEROEXPRESS_URL, AeroExpressClientError
)


DIR_NAME = 'aeroexpress'
WSDL_PATH = DIR_NAME + '/aero.wsdl'
FAKE_CRT = DIR_NAME + '/file.crt'
FAKE_KEY = DIR_NAME + '/file.key'
MOCK_URL = 'http://api.aeroexpress.ru:48000/aeapi/services/TicketServiceService.TicketServiceServicePort/'

get_agent_balance_response = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
    <soapenv:Body>
        <ns2:getAgentBalanceResponse xmlns:ns2="http://service.ticket.lynx.ru" xmlns:ns3="http://model.ticket.lynx.ru">
            <ns2:AEXGetAgentBalanceResponse>
                <balance>-666</balance>
                <status>OK</status>
            </ns2:AEXGetAgentBalanceResponse>
        </ns2:getAgentBalanceResponse>
    </soapenv:Body>
</soapenv:Envelope>
"""

get_menu_response = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns2:getWwwMenuResponse xmlns:ns2="http://service.ticket.lynx.ru" xmlns:ns3="http://model.ticket.lynx.ru">
         <ns2:getWwwMenuResponse>
            <items>
               <item>
                  <id>1</id>
                  <name>Стандарт</name>
                  <seatsSelect>false</seatsSelect>
                  <children />
                  <lastId>true</lastId>
                  <price>420</price>
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


@httpretty.activate
def test_get_agent_balance():
    httpretty.register_uri(httpretty.POST, uri=MOCK_URL, body=get_agent_balance_response)
    aeroexpress_client = AeroExpressClient(url=WSDL_PATH, crt_file=FAKE_CRT, key_file=FAKE_KEY)
    result = aeroexpress_client.get_agent_balance()
    assert result['balance'] == -666
    assert result['status'] == 'OK'


@httpretty.activate
def test_get_menu():
    httpretty.register_uri(httpretty.POST, uri=MOCK_URL, body=get_menu_response)
    aeroexpress_client = AeroExpressClient(url=WSDL_PATH, crt_file=FAKE_CRT, key_file=FAKE_KEY)

    result = aeroexpress_client.get_menu()
    assert len(result) == 2
    assert_that(result, contains(
        has_properties({
            'price': 420,
            'name': 'Стандарт',
            'orderType': 25
        }),
        has_properties({
            'price': 840,
            'name': 'Туда Обратно',
            'orderType': 34
        })
    ))


@httpretty.activate
def test_get_client():
    client = get_client(crt_file=FAKE_CRT, key_file=FAKE_KEY, url=WSDL_PATH)
    assert client.url == WSDL_PATH

    with pytest.raises(AeroExpressClientError):
        httpretty.register_uri(httpretty.GET, uri=AEROEXPRESS_URL)
        get_client(crt_file=FAKE_CRT, key_file=FAKE_KEY)

    with ReplaceAttr('AEROEXPRESS_URL', 'http://aeroex/', client_module):
        httpretty.register_uri(httpretty.GET, uri='http://aeroex/', body='<soapenv/>')
        client = get_client(crt_file=FAKE_CRT, key_file=FAKE_KEY)
    assert client.url == 'http://aeroex/'
