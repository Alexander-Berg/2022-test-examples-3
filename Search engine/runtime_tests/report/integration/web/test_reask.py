# -*- coding: utf-8 -*-

import pytest
import os
import json

from report.base import BaseReportTest
from report.const import *

class TestReask(BaseReportTest):

    @pytest.mark.ticket('SERP-46784')
    @pytest.mark.skipif(os.environ.get('REPORT_INVERTED') != '1', reason="No uninverted installation")
    def test_unquote_site(self, query):
        """
        SERP-46784 - Проверка правильной группировки при полном перезадании запроса
        """
        query.set_params({'text': '"яндекс новости" site:yandex.ru'})

        resp = self.json_request(query)
        reask = resp.data['searchdata']['reask']

        if reask is not None:
            assert reask['rule'] is None
            assert reask['show_message'] == 0
