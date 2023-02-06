# -*- coding: utf-8 -*-

import pytest
import base64
import urlparse
import json

from report.const import *
from report.functional.experiments import *

class TestExperiments(BaseExperimentTest):
    @pytest.mark.parametrize(("handler", "result"), [
        ("VIDEO", True),
        ("WEB", False),
        ("REPORT", True)
    ])
    def test_experiments_handler_video(self, query, exp, handler, result):
        """
        SERP-34655 - Для UserSplit cделать возможность задавать HANDLER в конфиге
        Для хендлеров VIDEO и REPORT - работает
        Для других - нет
        """
        exp['CONTEXT']['MAIN'] = {"source": {"VIDEO": {"rearr": [MAIN_REARR_TEST_PARAM]}}}
        exp['HANDLER'] = handler

        query.set_url("/video/search")
        query.headers.set_custom_headers({'X-Yandex-ExpBoxes': TEST_ID + ',0,76', 'X-Yandex-ExpFlags': exp.to_base64(), 'X-Yandex-ExpConfigVersion': '3764'})

        resp = self.request(query, source='UPPER')

        self.base_assert(resp.source, result, 'VIDEO')

