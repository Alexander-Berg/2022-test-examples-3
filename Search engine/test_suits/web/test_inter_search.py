# -*- coding: utf-8 -*-

import pytest

from util.tsoy import TSoY
from util.const import CTXS, HNDL, USER_AGENT


class TestInterSearch():
    """
    Проверяем логику "Распила" выдачи
    """

    CURRENT_INTER_SEARCH_SNIPPETS = 2

    def _get_wizplaces(self, data):
        return data['wizplaces']['important']

    def _get_banners(self, data):
        return data['banner']['data'].get('direct_premium', list())

    def _get_docs(self, data):
        return data['searchdata']['docs']

    @pytest.mark.ticket('RUNTIMETESTS-139')
    @TSoY.yield_test
    def test_common_logic(self, query):
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetUserAgent(USER_AGENT.TOUCH)
        query.SetRequireStatus(200)
        query.SetParams({
            'text': 'cats',
        })
        query.SetDumpFilter(resp=[CTXS.WEB_SEARCH])

        resp = yield query

        data = resp.GetCtxs()
        assert 'template_data' in data
        assert 'banner_template_data' in data

        inter_data = data['banner_template_data'][0]['data']
        post_data = data['template_data'][0]['data']

        wizplaces_num = len(self._get_wizplaces(inter_data))
        banner_num = len(self._get_banners(inter_data))
        docs_num = len(self._get_docs(inter_data))

        assert (wizplaces_num + banner_num + docs_num) >= TestInterSearch.CURRENT_INTER_SEARCH_SNIPPETS
        assert post_data['has-inter-search']

        def _check_inter_search_doc(snippets, inter_num):
            INTER_SEARCH_FLAG = 'inter_search_doc'

            for i in range(len(snippets)):
                if i >= inter_num:
                    assert INTER_SEARCH_FLAG not in snippets[i]
                else:
                    assert snippets[i][INTER_SEARCH_FLAG]

        _check_inter_search_doc(self._get_wizplaces(post_data), wizplaces_num)
        _check_inter_search_doc(self._get_banners(post_data), banner_num)
        _check_inter_search_doc(self._get_docs(post_data), docs_num)

    @pytest.mark.ticket('RUNTIMETESTS-139')
    @TSoY.yield_test
    def test_empty_results(self, query):
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetUserAgent(USER_AGENT.TOUCH)
        query.SetRequireStatus(200)
        query.SetParams({
            'text': '',
        })
        query.SetDumpFilter(resp=[CTXS.WEB_SEARCH])

        resp = yield query

        data = resp.GetCtxs()
        assert 'template_data' in data
        assert 'banner_template_data' not in data
