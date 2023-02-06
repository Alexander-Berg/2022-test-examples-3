# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import HNDL
import requests


class TestSearchAPI():

    @pytest.mark.ticket('SEARCH-9661')
    @TSoY.yield_test
    def test_type_sites(self, query):
        query.SetPath(HNDL.SEARCH_SEARCHAPI)
        query.SetParams({'app_version': 424,
                         'app_platform': 'wp',
                         'app_id': 'A025C540.1606779DE3B6F',
                         'pid': '2d7568934b853a8d40fb6695c437b484',
                         'pid2': '7:2d7568934b853a8d40fb6695c437b484',
                         'scalefactor': 2,
                         'uuid': 'a07db1d31d914a63c3fcbb5931858bec',
                         'lang': 'ru-ru',
                         'clid': 2236849,
                         'text': 'whatismyreferer ',
                         'type': 'sites',
                         'query_source': 'type',
                         'lat': '59.958813',
                         'lon': '30.406109',
                         'location_accuracy': '68.000000'})
        query.SetScheme('https')
        query.SetRequireStatus(200)

        # Send Request
        resp = yield query

        assert resp.status_code in [requests.codes.ok]
        assert resp.json()
