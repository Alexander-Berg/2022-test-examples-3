# -*- coding: utf-8 -*-

import os
import json

import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
@pytest.mark.unstable
class TestDistribution(BaseFuncTest):

    def test_promofooter(self, query):
        resp = self.json_test(query)
        data = resp.data
        assert data['promofooter']
        assert data['promofooter']['name'] == 'promofooter'
        assert isinstance(data['promofooter']['docs'], list)
        assert data['promofooter']['docs']

    def test_promofooter_empty(self, query):
        resp = self.json_test(query)
        assert not resp.data['promofooter']
