# -*- coding: utf-8 -*-

import pytest

from util.tsoy import TSoY
from util.const import HNDL, CTXS


class TestAlice():
    """
    Проверяем ручку для хождения Алисы в веб Поиск.
    """

    @pytest.mark.ticket('RUNTIMETESTS-93')
    @TSoY.yield_test
    def test_simple_request(self, query):
        query.SetPath(HNDL.SEARCH_REPORT_ALICE)
        query.SetRequireStatus(200)
        query.SetInternal()
        query.SetParams({
            'text': 'google',
            'banner_ua': 'Mozilla%5C/5.0+%28Linux;+Android+6.0.1;+Station+Build%5C/MOB30J;+wv%29+AppleWebKit%5C/537.36+%28KHTML%2C'
                         '+like+Gecko%29+Version%5C/4.0+Chrome%5C/61.0.3163.98+Safari%5C/537.36+YandexStation%5C/2.3.0.3.373060213.20190204.develop.ENG+%28YandexIO%29',
            'flags': 'blender_tmpl_data=1',
            'init_meta': 'yandex_tier_disabled',
            'pers_suggest': 0,
            'service': 'megamind.yandex',
            'ui': 'quasar',
        })

        resp = yield query

        data = resp.json()
        assert 'tmpl_data' in data
        assert 'searchdata' in data['tmpl_data']
        assert len(data['tmpl_data']['searchdata']['docs']) != 0

    @pytest.mark.ticket('BEGEMOT-2762')
    @TSoY.yield_test
    def test_begemot_flags(self, query):
        query.SetPath(HNDL.SEARCH_REPORT_ALICE)
        query.SetRequireStatus(200)
        query.SetDumpFilter(req=[CTXS.BEGEMOT_WORKERS])

        resp = yield query

        tree = resp.GetCtxs()
        assert tree['bg-bert']
        return  # TODO enable once the graph is released / BEGEMOT-2762
        assert any(c.get('project') == 'alice-web' for c in tree['begemot_config'])  # for .Vertical rules annotation
        assert tree['bg-alice']  # for graph edges
