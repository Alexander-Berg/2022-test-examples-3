# -*- coding: utf-8 -*-

import pytest
from util.tsoy import TSoY
from util.helpers import Auth
# from util.const import CTXS


class TestLogin(Auth):

    @pytest.mark.ticket('WEBREPORT-737')
    @TSoY.yield_test
    def test_login_avatar(self, query, blackbox):
        self.SetBlackBoxAuth(query, blackbox)
        # query.SetDumpFilter(resp=[CTXS.INIT])

        resp = yield query
        # ctxs = resp.GetCtxs()

        display_name = query.GetPassport()['display_name']
        assert display_name['name'] in str(resp.text) or 'Vasya Pupkin' in str(resp.text)
        assert display_name['avatar']['default'] in str(resp.text)
    #
    #
    # @pytest.skip('TODO: Нужно сделать тест на социальный залогин')
    # @TSoY.yield_test
    # def test_blackbox_social(self, query):
    #     """
    #     SERP-52313: Не приходит login при социальной авторизации
    #     """
        # login = 'Dmitry Savvateev'
        # uid = 482268923
        # resp = self.json_request(
        #         query,
        #         require_status=None,
        #         sources=[
        #             (
        #                 'APP_HOST',
        #                 json.dumps([{
        #                     "meta": {},
        #                     "name": "BLACKBOX",
        #                     "results": [
        #                         {
        #                             'age' : 1857,
        #                             'connection_id' : 'test',
        #                             'display_name' : {
        #                                 'name' : 'Dmitry Savvateev',
        #                             },
        #                             'error' : 'OK',
        #                             'expires_in' : 7774143,
        #                             'karma' : {
        #                                 'value' : 0
        #                                 },
        #                             'karma_status' : {
        #                                 'value' : 0
        #                                 },
        #                             'login' : '',
        #                             'phones' : [],
        #                             'regname' : 'uid-c6brmgt7',
        #                             'session_fraud' : 0,
        #                             'status' : {
        #                                 'id' : 0,
        #                                 'value' : 'VALID'
        #                                 },
        #                             'ttl' : 5,
        #                             'type' : 'blackbox',
        #                             'uid' : {
        #                                 'value' : 482268923
        #                             }
        #                         },
        #                     ]
        #             }])
        #         ),
        #     ]
        # )
        # pas = resp.data['reqdata']['passport']
        # assert pas['logged_in'] == 1, pas
        # assert pas['cookieL']['uid'] == uid, pas
        # assert pas['cookieL']['login'] == login, pas
