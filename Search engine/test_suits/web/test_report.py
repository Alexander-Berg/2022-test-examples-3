# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
# import requests
from util.const import CTXS


class TestReport():
    @pytest.mark.ticket('SERP-46462')
    @pytest.mark.parametrize(('l10n', 'language'), [
        ('ru', 'ru'),
        ('uz', 'uz'),
        ('foo', 'ru')
    ])
    @TSoY.yield_test
    def test_cgi_l10n(self, query, l10n, language):
        # флаг не работает
        # query.SetFlags({'l10n': l10n})
        query.SetParams({'l10n': l10n})
        query.SetDumpFilter(resp=[CTXS.BLENDER_TEMPLATE_DATA])
        query.SetRequireStatus(200)

        resp = yield query
        ctxs = resp.GetCtxs()
        template_data = ctxs['template_data'][0]['data']

        assert template_data['reqdata']['language'] == language

    @TSoY.yield_test
    def test_metahost2(self, query):
        query.SetDumpFilter(req=[CTXS.BLENDER])

        metahost2 = [
            'GOSUSLUGI:mob.search.yandex.net:11003',
            'GOSUSLUGI:localhost:11003',
            '*~FASTSNIPS!fastsnips-000:ws36-630.search.yandex.net:7300',
        ]

        query.SetParams({
            'metahost2': metahost2
        })

        resp = yield query
        ctxs = resp.GetCtxs()

        assert len(ctxs['flags']) > 0
        for f in ctxs['flags']:
            assert f['all']['metahost2'].sort() == metahost2.sort()

        assert len(ctxs['context_rewrite']) > 0
        for f in ctxs['context_rewrite']:
            assert f['rewrite']['UPPER']['metahost2'].sort() == metahost2.sort()

        assert len(ctxs['noapache_setup']) > 0
        for f in ctxs['noapache_setup']:
            assert f['global_ctx']['metahost2'].sort() == metahost2.sort()
