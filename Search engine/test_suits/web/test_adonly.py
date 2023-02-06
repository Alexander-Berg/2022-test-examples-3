# -*- coding: utf-8 -*-

from util.tsoy import TSoY
import pytest
from util.const import HNDL, TLD, CTXS, EXTERNAL_IP


class TestAdonly():
    """
    Проверяем ручку adonly.
    """

    def rambler_params(self):
        params = {
            'text': 'холодильник',
            'lr': 2,
            'bpage': 324760,
            'title-length-limit': 10,
            'title-font-id': 1,
            'title-font-size': 2,
            'title-pixel-length-limit': 9,
            'stat-id': 3,
            'guid': 'guid',
            'ext-uniq-id': 'ext-uniq-id',
            'page': 0,
            'parent-reqid': '1588869343618254-296399882053487373500132-hamster-app-host-vla-web-yp-28',
        }
        return params

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_adonly_bad_request(self, query):
        """
        Запрос без параметра bpage - 400
        """
        query.SetPath(HNDL.SEARCH_ADONLY)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(400)

        yield query

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @TSoY.yield_test
    def test_adonly_params1(self, query):
        """
        БК проверяет соответсвие bpage и реферера. Если нет соответсвие получаем 403
        """
        query.SetPath(HNDL.SEARCH_ADONLY)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(403)
        query.SetParams(self.rambler_params())

        resp = yield query
        assert resp.json() == {'args': None, 'data': None}

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket('SEARCH-9883')
    @pytest.mark.parametrize(('referer', 'targetRef'), [
        [None, 'https://rambler.ru/'],
        ['https://rambler.ru/', None]
    ])
    @TSoY.yield_test
    def test_adonly_params2(self, query, referer, targetRef):
        """
        БК проверяет соответсвие bpage и реферера, реферер можно передать параметром target-ref
        """
        query.SetPath(HNDL.SEARCH_ADONLY)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)
        params = self.rambler_params()
        if referer:
            query.SetHeaders({
                'Referer': 'https://rambler.ru/'
            })
        else:
            params['target-ref'] = targetRef

        query.SetParams(params)

        resp = yield query
        data = resp.json()["data"]
        for k in ['direct_halfpremium', 'direct_premium', 'stat']:
            assert k in data

    def _check_common_yabs_params(self, yabs_setup):
        check_keys = (
            'title-length-limit',
            'title-font-id',
            'title-font-size',
            'title-pixel-length-limit',
            'stat-id',
            'guid',
            'ext-uniq-id',
            'parent-reqid',
        )
        params = self.rambler_params()
        for key in (check_keys):
            assert str(params[key]) == yabs_setup[0][key][0]

        assert str(params['page']+1) == yabs_setup[0]['page-no'][0]
        assert '0' == yabs_setup[0]['bad-ref'][0]

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.parametrize("targetRef", [None, 'https://rambler.ru/from_cgi_params?q=1'])
    @TSoY.yield_test
    def test_adonly_setup_params(self, query, targetRef):
        """
        проверить что из сетапа выходят правильные параметры
        """
        query.SetPath(HNDL.SEARCH_ADONLY)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)
        referer_val = 'https://rambler.ru/from_headers?q=1'
        query.SetHeaders({
            'Referer': referer_val
        })
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP])
        params = self.rambler_params()
        if targetRef:
            params['target-ref'] = targetRef
        query.SetParams(params)

        resp = yield query
        yabs_setup = resp.GetCtxs()['yabs_setup']

        self._check_common_yabs_params(yabs_setup)
        assert str(self.rambler_params()['lr']) == yabs_setup[0]['tune-region-id'][0]
        assert yabs_setup[0]['target-ref'][0] == targetRef if targetRef else referer_val

    @pytest.mark.soy_http('RUNTIMETESTS-96')
    @pytest.mark.ticket("LAAS-1799")
    @TSoY.yield_test
    def test_adonly_region_by_ip(self, query):
        """
        проверить выставление региона по заголовку X-Forwarded-For
        """
        query.SetPath(HNDL.SEARCH_ADONLY)
        query.SetDomain(TLD.RU)
        query.SetRequireStatus(200)
        # EXTERNAL_IP now it is 87 region
        referer_val = 'https://rambler.ru/from_headers?q=1'
        query.SetHeaders({
            'Referer': referer_val,
        })
        # change to SetRwrHeaders if apphost laas+uaas is enabled
        query.SetHeaders({
            'X-Real-IP': EXTERNAL_IP,
            'X-Forwarded-For': EXTERNAL_IP,
        })
        query.SetDumpFilter(resp=[CTXS.YABS_SETUP, 'INIT'])
        params = self.rambler_params()
        params.pop('lr')
        query.SetParams(params)

        resp = yield query

        tuned_region = resp.GetCtxs()['region'][0]['tuned']

        # RUNTIMETESTS-140: checks that ip somewhere in USA
        assert tuned_region['country']['en_name'] == 'United States'

        yabs_setup = resp.GetCtxs()['yabs_setup']
        self._check_common_yabs_params(yabs_setup)
        assert yabs_setup[0]['target-ref'][0] == referer_val
        assert tuned_region['id'] == yabs_setup[0]['tune-region-id'][0]
