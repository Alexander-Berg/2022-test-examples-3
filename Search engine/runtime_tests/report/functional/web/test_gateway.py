# -*- coding: utf-8 -*-

import copy
import os
import pytest

from report.functional.web.base import BaseFuncTest
from report.const import *


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestGateway(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_sbh(self, query):
        query.set_params({'sbh': 1, 'text': 'путин'})
        resp = self.gateway_request(query)
        props = resp.data['meta']['search_props']['UPPER'][0]['properties']
        assert not props or filter(lambda k: k.endswith('.debug'), props.keys())

    def test_sbh_nodebug(self, query):
        query.set_params({'text': 'путин'})
        resp = self.gateway_request(query)
        props = resp.data['meta']['search_props']['UPPER'][0]['properties']
        assert not filter(lambda k: k.endswith('.debug'), props.keys())

    def gateway_test(self, data):
        serp_schema = os.path.join(self.schema_dir, 'gateway.json')
        with open(serp_schema) as f:
            content = f.read()
            schema = validate_json(content)
            self.validate_json_scheme(data, schema)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_gateway_common(self, query):
        query.set_params({'text': 'мадонна видео'})
        resp = self.gateway_request(query)
        self.gateway_test(resp)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_gateway_via_header(self, query):
        query.set_params({'text': 'мадонна видео'})
        query.headers.set_custom_headers({'X-Yandex-Report-Type': 'gateway'})
        resp = self.request(query)
        self.gateway_test(resp)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '2', reason="")
    def test_gateway_external(self, query):
        query.set_params({'text': 'мадонна видео'})
        query = copy.deepcopy(query)
        query.replace_params({'rpt': 'gateway'})
        self.request(query, require_status=404)

    def test_right_docs(self, query):
        """
        SERP-37529 - Включить searchdata.docsRight в ответ perl-gateway
        проверяем, что 'docs_right' есть
        """
        query.set_query_type(GATEWAY_TOUCH)
        query.replace_params({'text': 'путин'})

        resp = self.json_test(query)
        data = resp.data

        assert 'docs_right' in data['web'], 'No docs_right here'
        assert isinstance(data['web']['docs_right'], list)

    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    def test_kuka_debug_props(self, query):
        query.set_params({'sbh': 1, 'text': 'путин'})
        resp = self.gateway_request(query)
        props = resp.data['meta']['search_props']['UPPER'][0]['properties'].keys()
        assert props
        assert filter(lambda p: p.endswith('.debug'), props)
