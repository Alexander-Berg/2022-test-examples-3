# -*- coding: utf-8 -*-

from report.const import *
from report.functional.web.base import BaseFuncTest
import pytest

from tests import TESTS

NOAPACHE_TESTS = [[test['params'], test['noapache_request_schema']] for test in TESTS if test.get('noapache_request_schema')]

class TestNoapacheRequest(BaseFuncTest):
    """
    Тесты на колдунщики: запрос к источнику(проверка запроса по схеме)
    """

    @pytest.mark.unstable
    @pytest.mark.parametrize(('params', 'noapache_request_schema'),
        NOAPACHE_TESTS
    )
    def test_noapache_request(self, query, custom_schema_path, params, noapache_request_schema):
        query.set_params(params)
        clt_ctx = self.with_noapache(query)[0]['client_ctx']

        self.validate_json_sources(clt_ctx, custom_schema_path(noapache_request_schema))
