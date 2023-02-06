# -*- coding: utf-8 -*-

import os
import pytest
import datetime
from datetime import timedelta

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestHotels(BaseFuncTest):

    reqs = {
        "1org": "рэдиссон славянская",
        "carousel": "отели в москве"
    }

    params = (   # ag_dynamic for adresa-geolocation
        '''{"middle_yandex_travel_UseSearcher":1,"middle_yandex_travel_RequestId":0,"middle_yandex_travel_filter":{"Date":"%s","Nights":"1","Ages":"88,88"},"parent_reqid":"1554213040786589-330975426879031751100035-vla1-1881","middle_yandex_travel_deep_hotel_mode":1}''',
        '''{"middle_yandex_travel_filter":{"Date":"%s","Nights":"1","Ages":"88,88"},"parent_reqid":"1554213040786589-330975426879031751100035-vla1-1881"}''',
    )

    def check_gms_data(self, gms):

        assert "YandexTravel" in gms["features"][0]["properties"]

        yts = [ feature["properties"]["YandexTravel"]
                for feature in gms["features"]
                    if "YandexTravel" in feature["properties"] ]

        assert len(yts) > 0

        assert "YandexTravel" in gms["properties"]["ResponseMetaData"]["SearchResponse"]["InternalResponseInfo"]
        yat = gms["properties"]["ResponseMetaData"]["SearchResponse"]["InternalResponseInfo"]["YandexTravel"]
        assert "WasFound" in yat
        assert "Operators" in yat
        assert "IsFinished" in yat
        assert "Nights" in yat
        assert "Date" in yat

    @pytest.mark.ticket('GEOSEARCH-5532')
    @pytest.mark.parametrize("req", ["1org", "carousel"])
    def test_hotels_touch(self, query, req):
        query.set_query_type(TOUCH)
        query.replace_params({ 'text': self.reqs[req], 'lr': 213,
            'srcparams': 'GEOV:source=YandexTravel:http://travel-hotels-offercache-test.yandex.net' })

        resp = self.json_request(query)

        docs = resp.data["searchdata"]["docs"]
        gms = [ doc["snippets"]["full"]["data"]["GeoMetaSearchData"]
            for doc in docs
                if "full" in doc["snippets"] and "data" in doc["snippets"]["full"] and "GeoMetaSearchData" in doc["snippets"]["full"]["data"] ]

        self.check_gms_data(gms[0])

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('GEOSEARCH-5532')
    @pytest.mark.parametrize(("device", "path"), [
        (DESKTOP, "/search/result"),
        (TOUCH,   "/search/result/touch")
    ])
    @pytest.mark.parametrize("param", [0, 1])
    @pytest.mark.parametrize("req", ["1org", "carousel"])
    def test_hotels_ajax(self, query, device, path, param, req):
        query.set_query_type(device)
        query.set_url(path)
        ag_dynamic = self.params[param] % (datetime.date.today() + timedelta(3))
        query.replace_params({
            'text': self.reqs[req],
            'type': "geov",
            'lr': 213,
            'yu': '3981798681528560304',
            'sk': 'ub809a90ff5b7ddd298aef394fadbe09a',
            'reqid': '1554213040786589-330975426879031751100035-vla1-1881',
            'ag_dynamic': ag_dynamic,
            'sll': '37.58710151,55.74949114',
            'sspn': '0.062289,0.035051',
            'yandexuid': '3981798681528560304',
            'srcparams': 'GEOV:source=YandexTravel:http://travel-hotels-offercache-test.yandex.net'
        })

        resp = self.json_request(query)

        gms = resp.data["app_host"]["result"]["docs"][0]["snippets"]["full"]["GeoMetaSearchData"]
        self.check_gms_data(gms)
