# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import CTXS


class TestReask():
    @pytest.mark.ticket('SERP-57212')
    @pytest.mark.parametrize(("flag"), [
        {'skip_reask': 1},
        {'skip_reask': 0}
    ])
    @TSoY.yield_test
    def test_skip_reask(self, query, flag):
        query.SetDumpFilter(resp=[CTXS.WEB_SEARCH_TEMPLATE_DATA])
        query.SetFlags(flag)
        query.SetParams({
            # нужно подобрать запрос по которому ничего не находится
            'text': '"фумадримапил"'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        js = tmpl[0]['data']
        size = len(js['searchdata']['docs'])
        assert (not bool(size)) == flag['skip_reask']

    @pytest.mark.ticket('SERP-46784')
    @TSoY.yield_test
    def test_unquote_site(self, query):
        """
        SERP-46784 - Проверка правильной группировки при полном перезадании запроса
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'text': '"яндекс новости" site:yandex.ru'
        })
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        data = tmpl[0]['data']
        reask = data['searchdata']['reask']
        if reask is not None:
            assert reask['rule'] is None
            assert reask['show_message'] == 0
