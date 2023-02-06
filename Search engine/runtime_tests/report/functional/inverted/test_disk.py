# -*- coding: utf-8 -*-

import time
import json

import os
import pytest

from report.const import *
from report.functional.web.base import BaseFuncTest
from report.functional.conftest import create_runtime_test_data, remove_runtime_test_data, create_permanent_test_data, remove_permanent_test_data


@pytest.mark.skipif(not os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestITS(BaseFuncTest):
    def test_no_its_flags_with_no_flags(self, query, write_its_flags):
        write_its_flags({'its_location': 'some'})
        query.add_params({ 'no-flags': 1 })
        resp = self.json_request(query)
        assert 'its_location' not in resp.data['reqdata']['flags']
