# -*- coding: utf-8 -*-

import zlib
import json
import os
from copy import deepcopy

import pytest

from report.const import *
from report.proto import meta_pb2
from report.functional.web.base import BaseFuncTest


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestNoapache(BaseFuncTest):
    @pytest.mark.skip("")
    def test_random_log(self, query):
        """
        SERP-38011 Изменить обработку параметра factors=random_log
        Проверяем, что маркер RandomLog пишется в reqans_log
        """

        query.set_query_type(GATEWAY_TOUCH)
        query.add_flags({'noapache_json_req': 0, 'noapache_json_res':0})

        marker = 'RandomLog=ABCDEF1234567890'
        #подготавливаем ответ noapache
        #в сохраненный ответ подкладываем нужные нам маркеры
        data = PB_AddMarker(self.noapache_resp(query), grouping='d', marker=marker)

        resp = self.request(query, source=('UPPER', self.get_config(data)))

        reqans = resp.reqans_log()
        assert reqans['url']
        for line in reqans['url']:
            assert line.find(marker) != -1, line

    @pytest.mark.skip("")
    def test_factors_for_report(self, query):
        """
        SERP-38011 Изменить обработку параметра factors=random_log
        Проверяем, что маркер FactorsForReport не пишется в reqans_log
        """

        query.set_query_type(GATEWAY_TOUCH)
        query.add_flags({'noapache_json_req': 0, 'noapache_json_res':0})
        query.add_params({'factors': 'random_log'})

        marker = 'FactorsForReport=ABCDEF1234567890'

        #подготавливаем ответ noapache
        #в сохраненный ответ подкладываем нужные нам маркеры
        data = PB_AddMarker(self.noapache_resp(query), grouping='d', marker=marker)

        resp = self.request(query, source=('UPPER', self.get_config(data)))

        reqans = resp.reqans_log()
        assert reqans['url']
        for line in reqans['url']:
            assert line.find(marker) == -1, line

