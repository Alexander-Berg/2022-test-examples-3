# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import HNDL

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


class TestCatalogsearch():
    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('REPORTINFRA-252', 'REPORTINFRA-359')
    @pytest.mark.parametrize("url_path, prms", [
        (HNDL.SEARCH_CATALOGSEARCH, {}),
    ])
    @TSoY.yield_test
    def test_catalogsearch_api_response(self, query, url_path, prms):
        query.SetPath(url_path)
        query.SetParams({
            'text': u'фара',
            'searchid': '3971208',
            'format': 'json',
        })
        query.SetParams(prms)
        query.SetInternal()
        query.SetRequireStatus(200)

        resp = yield query
        answer = resp.json()

        docs_total = answer.get('docsTotal', 0)

        assert 'documents' in answer
        assert len(answer['documents']) <= docs_total
        for document in answer.get('documents', []):
            for key in document:
                assert key in _DOCUMENTS_KEYS
