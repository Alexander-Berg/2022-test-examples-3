# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import HNDL, TLD, CTXS


class TestTutby():
    """
    Проверяем работу tut.by во фрейме.
    """

    @pytest.mark.ticket('SERP-101441')
    @pytest.mark.parametrize(('referer', 'targetRef'), [
        ['https://search.tut.by/?ab0068551', None],
        ['https://tut.by/?ab0068551', None],
        ['https://yandex.by/?', None],
        ['https://search.tut.by/?ab0068551', 'https://strange.domain.com/'],
    ])
    @TSoY.yield_test
    def test_tutby_ref1(self, query, referer, targetRef):
        query.SetPath(HNDL.SEARCH_TOUCH)
        query.SetDomain(TLD.BY)
        query.SetRequireStatus(200)
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])

        if referer:
            query.SetHeaders({
                'Referer': referer
            })
        params = {
            'text': 'купить телефон',
            'lite': 1,
            'p': 0,
            'promo': 'search.tut.by'
        }
        if targetRef:
            params['target-ref'] = targetRef
        query.SetParams(params)

        resp = yield query
        yabs_setup = resp.GetCtxs()['yabs_setup']
        assert '0' == yabs_setup[0]['bad-ref'][0]
        assert yabs_setup[0]['target-ref'][0] == targetRef if targetRef else referer
        # check bpage
        assert yabs_setup[0]['metahost'][0].endswith('/code/473547?')
