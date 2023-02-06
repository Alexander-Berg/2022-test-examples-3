# -*- coding: utf-8 -*-
import json

import pytest
import requests
import requests_mock

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.common.utils.geosnippetter import GeoSnippetterClient


RESPONSE_XML = u"""<ymaps xmlns="http://maps.yandex.ru/ymaps/1.x" xmlns:gml="http://www.opengis.net/gml">
  <GeoObjectCollection>
    <gml:featureMembers>
      <GeoObject>
        <gml:metaDataProperty>
          <Stops xmlns="http://maps.yandex.ru/snippets/masstransit/1.x">
            <Stop>
              <name xml:lang="ru">Площадь 1905 года</name>
              <Distance>
                <value>551.659</value>
                <text>550 м</text>
              </Distance>
              <Style>
                <color>#0a6f20</color>
              </Style>
              <gml:Point>
                <gml:pos>60.599852 56.836303</gml:pos>
              </gml:Point>
            </Stop>
            <Stop>
              <name xml:lang="ru">Геологическая</name>
              <Distance>
                <value>1268.55</value>
                <text>1,3 км</text>
              </Distance>
              <Style>
                <color>#0a6f20</color>
              </Style>
              <gml:Point>
                <gml:pos>60.603755 56.826717</gml:pos>
              </gml:Point>
            </Stop>
            <Stop>
              <name xml:lang="ru">Динамо</name>
              <Distance>
                <value>1450.55</value>
                <text>1,4 км</text>
              </Distance>
              <Style>
                <color>#0a6f20</color>
              </Style>
              <gml:Point>
                <gml:pos>60.599412 56.847823</gml:pos>
              </gml:Point>
            </Stop>
          </Stops>
        </gml:metaDataProperty>
        <gml:name>-1</gml:name>
        <gml:Point>
          <gml:pos>60.5909 56.8357</gml:pos>
        </gml:Point>
      </GeoObject>
    </gml:featureMembers>
  </GeoObjectCollection>
</ymaps>"""


class TestGeoSnippetter(TestCase):
    def setUp(self):
        self.url = 'http://some.snippetter.yandex.net'
        self.client = GeoSnippetterClient(self.url)

    def test_get(self):
        with requests_mock.Mocker() as m:
            m.get(self.url + '/getsnippets', text=RESPONSE_XML)

            response = self.client.get([{'latitude': 1.0, 'longitude': 2.0, 'id': 'id42'}], 'snippet_type', lang='en-GB')
            assert response.text == RESPONSE_XML

            assert m.call_count == 1
            request = m.last_request
            query = request.qs
            objects_strs = query.pop('objects')
            assert len(objects_strs) == 1

            assert query == {
                'lang': ['en-gb'],
                'snippets': ['snippet_type'],
                'outformat': ['xml'],
            }

            objects = json.loads(objects_strs[0])
            assert objects == [{'ll': [2.0, 1.0], 'k': '', 't': 'geo', 'rid': 'id42'}]

    def test_get_near_metro(self):
        with requests_mock.Mocker() as m:
            m.get(self.url + '/getsnippets', text=RESPONSE_XML)

            expected = [
                {
                    "lang": "ru",
                    "distance": 551.659,
                    "coords": {
                        "latitude": 56.836303,
                        "longitude": 60.599852
                    },
                    "name": u"Площадь 1905 года",
                    "color": "#0a6f20"
                },
                {
                    "lang": "ru",
                    "distance": 1268.55,
                    "coords": {
                        "latitude": 56.826717,
                        "longitude": 60.603755
                    },
                    "name": u"Геологическая",
                    "color": "#0a6f20"
                },
                {
                    "lang": "ru",
                    "distance": 1450.55,
                    "coords": {
                        "latitude": 56.847823,
                        "longitude": 60.599412
                    },
                    "name": u"Динамо",
                    "color": "#0a6f20"
                }
            ]

            metro_stations = self.client.get_near_metro(1.0, 2.0)
            assert metro_stations == expected

    def test_timeout(self):
        with requests_mock.Mocker() as m:
            m.get(self.url + '/getsnippets', exc=requests.exceptions.ConnectTimeout)

            with pytest.raises(GeoSnippetterClient.Error):
                self.client.get([{'latitude': 1.0, 'longitude': 2.0, 'id': 'id42'}], 'snippet_type', lang='en-GB')
