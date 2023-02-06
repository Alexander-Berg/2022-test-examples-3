# -*- coding: utf-8 -*-

import os
import pytest
import logging
import json
import time

from report.functional.web.base import BaseFuncTest


_DOCUMENTS_KEYS = {
    'id',
    'name',
    'description',
    'url',
    'clickUrl',
    'categoryId',
    'categoryParents',
    'price',
    'currencyId',
    'vendor',
    'snippet',
    'mobileSnippet',
    'origSnippet',
    'parameters',
    'parameterList',
    'available',
    'oldPrice',
}


@pytest.mark.skipif(os.environ.get('REPORT_INVERTED') == '1', reason="SERP-67197")
class TestCatalogsearch(BaseFuncTest):
    @pytest.mark.skipif(os.environ.get('AB_MODE') == '1', reason="")
    @pytest.mark.ticket('REPORTINFRA-252', 'REPORTINFRA-359')
    @pytest.mark.parametrize("url_path, prms", [
        ('/search/catalogsearch', {}),
    ])
    def test_catalogsearch_api_response(self, query, url_path, prms):
        query.set_url(url_path)
        query.set_params({
            'text': u'фара',
            'searchid': '3971208',
            'format': 'json',
        })
        query.add_params(prms)
        query.set_internal()

        non_empty_test_passed = False

        for attempt in xrange(10):
            resp = self.request(query)
            answer = json.loads(resp.content)
            docs_total = answer.get('docsTotal', 0)
            if not docs_total:
                logging.warning("Received zero docsTotal, maybe REPORTINFRA-359 strikes back [attempt %s]", attempt)
                time.sleep(5)
                continue

            assert 'documents' in answer
            assert len(answer['documents']) <= docs_total

            for document in answer.get('documents', []):
                for key in document:
                    assert key in _DOCUMENTS_KEYS

            non_empty_test_passed = True
            break

        assert non_empty_test_passed
