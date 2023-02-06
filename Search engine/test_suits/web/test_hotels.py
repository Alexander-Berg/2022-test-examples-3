# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import PLATFORM, CTXS
import datetime
from datetime import timedelta
import json


class TestHotels():

    reqs = {
        "1org": "рэдиссон славянская",
        "carousel": "отели в москве"
    }

    params = (   # ag_dynamic for adresa-geolocation
        json.dumps({
            "middle_yandex_travel_UseSearcher": 1,
            "middle_yandex_travel_RequestId": 0,
            "middle_yandex_travel_filter": {
                "Date": "%s",
                "Nights": "1",
                "Ages": "88,88"
            },
            "parent_reqid": "1554213040786589-330975426879031751100035-vla1-1881",
            "middle_yandex_travel_deep_hotel_mode": 1
        }),
        json.dumps({
            "middle_yandex_travel_filter": {
                "Date": "%s",
                "Nights": "1",
                "Ages": "88,88"
            },
            "parent_reqid": "1554213040786589-330975426879031751100035-vla1-1881"
        }),
    )

    def get_gms_from_snippets(self, docs):
        for doc in docs:
            try:
                return doc["snippets"]["full"]["data"]["GeoMetaSearchData"]
            except:
                pass
        for doc in docs:
            try:
                return doc["snippets"]["full"]["GeoMetaSearchData"]
            except:
                pass
        return None

    def check_gms_data(self, gms):
        assert "YandexTravel" in gms["features"][0]["properties"]
        yts = [feature["properties"]["YandexTravel"]
                for feature in gms["features"]
                    if "YandexTravel" in feature["properties"]]
        assert len(yts) > 0
        assert "YandexTravel" in gms["properties"]["ResponseMetaData"]["SearchResponse"]["InternalResponseInfo"]
        yat = gms["properties"]["ResponseMetaData"]["SearchResponse"]["InternalResponseInfo"]["YandexTravel"]
        assert "WasFound" in yat
        assert "Operators" in yat
        assert "IsFinished" in yat
        assert "Nights" in yat
        assert "Date" in yat

    @pytest.mark.skip('TRAVELBACK-2153')
    @pytest.mark.ticket('GEOSEARCH-5532')
    @TSoY.yield_test
    def test_hotels_desktop_carousel(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.DESKTOP)
        query.SetParams({
            'text': self.reqs["carousel"],
            'lr': 213,
            'srcparams': 'GEOV:source=YandexTravel:http://travel-hotels-offercache-test.yandex.net'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        gms = js["wizplaces"]["carousel"][0]["data"]["GeoMetaSearchData"]
        self.check_gms_data(gms)

    @pytest.mark.ticket('GEOSEARCH-5532')
    @TSoY.yield_test
    def test_hotels_desktop_1org(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.DESKTOP)
        query.SetParams({
            'text': self.reqs["1org"],
            'lr': 213,
            'srcparams': 'GEOV:source=YandexTravel:http://travel-hotels-offercache-test.yandex.net'
        })

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        # Something's wrong with docs_right (probably due to EXPERIMENTS-34443)
        gms_data = self.get_gms_from_snippets(js["searchdata"]["docs_right"])
        if gms_data is None:
            gms_data = self.get_gms_from_snippets(js["searchdata"]["docs"])

        self.check_gms_data(gms_data)

    @pytest.mark.ticket('GEOSEARCH-5532')
    @pytest.mark.parametrize("req", ["1org", "carousel"])
    @TSoY.yield_test
    def test_hotels_touch(self, query, req):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetQueryType(PLATFORM.TOUCH)
        query.SetParams({
            'text': self.reqs[req],
            'lr': 213,
            'srcparams': 'GEOV:source=YandexTravel:http://travel-hotels-offercache-test.yandex.net'
        })

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        docs = js["searchdata"]["docs"]
        self.check_gms_data(self.get_gms_from_snippets(docs))

    @pytest.mark.ticket('GEOSEARCH-5532')
    @pytest.mark.parametrize(("device", "path"), [
        (PLATFORM.DESKTOP, "/search/result"),
        (PLATFORM.TOUCH,   "/search/result/touch")
    ])
    @pytest.mark.parametrize("param", [0, 1])
    @pytest.mark.parametrize("req", ["1org", "carousel"])
    @TSoY.yield_test
    def test_hotels_ajax(self, query, device, path, param, req):
        query.SetDumpFilter(resp=[CTXS.HANDLER_OUTPUT])
        query.SetQueryType(device)
        query.SetPath(path)
        ag_dynamic = self.params[param] % (datetime.date.today() + timedelta(3))
        query.SetParams({
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

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        docs = js["app_host"]["result"]["docs"]
        self.check_gms_data(self.get_gms_from_snippets(docs))
