# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.const import HNDL, USER_AGENT, CTXS


class TestGranny():
    @pytest.mark.ticket('SERP-43251')
    @TSoY.yield_test
    def test_granny_banner(self, query):
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetParams({
            'text': 'пластиковые окна'
        })
        query.SetFlags({
            'serp3_granny': 1,
            'serp3_granny_https': 1
        })
        query.SetPath(HNDL.SEARCH)
        query.SetUserAgent(USER_AGENT.GRANNY)
        query.SetRequireStatus(200)

        resp = yield query

        tmpl = resp.GetCtxs()['template_data']
        assert len(tmpl) != 0 and 'data' in tmpl[0]

        data = tmpl[0]['data']
        assert data['banner']

    @pytest.mark.ticket('SERP-36585', 'SERP-32482', 'SERP-36784')
    @pytest.mark.parametrize(('https', 'g_https_flag', 'g_flag', 'internal'), [
        (1, 1, 1, 0),
        (0, 1, 1, 1),
        (0, 0, 1, 0),
        (1, 0, 0, 0)
    ])
    @TSoY.yield_test
    def test_search_granny(self, query, https, g_https_flag, g_flag, internal):
        """
        SERP-36585 - [granny] Добавить возможно видеть бабулю по https с флагом
        case: https + serp3_granny_https + serp3_granny => https 200
        case: internal - внутренняя ручка, поэтому редиректы отключены
        """
        query.SetDumpFilter(resp=[CTXS.BLENDER])
        query.SetFlags({
            'serp3_granny_https': g_https_flag,
            'serp3_granny': g_flag
        })
        if internal:
            query.SetInternal()
        query.SetUserAgent(USER_AGENT.GRANNY)
        query.SetScheme('https' if https > 0 else 'http')
        query.SetRequireStatus(200)

        resp = yield query
        template_data = resp.GetCtxs()['template_data'][0]

        assert template_data['name'] == 'granny:desktop'
