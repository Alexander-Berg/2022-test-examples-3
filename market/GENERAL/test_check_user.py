#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty, Contains
import datetime
import json


class T(env.TestSuite):
    ban_table_path = '//home/ban'
    event_log_path = '//home/event_log'

    @classmethod
    def get_headers(cls, headers={}):
        default = {'X-Ya-Service-Ticket': T.tvm_client_secret}
        default.update(headers)
        return default

    @classmethod
    def get_request(cls, request={}):
        default = {'user': {'yandexuid': '1'}}
        default['user'].update(request)
        return default

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_tvmapi = True
        cls.click_n_collect.config.ServerTvm.RemoteId.extend([cls.tvm_client_id])

        cls.click_n_collect.with_yt = True
        cls.click_n_collect.config.CheckUserHandler = True
        cls.click_n_collect.config.YtAntiFraudReaderTask.BannedUsersPath = cls.ban_table_path
        cls.click_n_collect.config.YtAntiFraudReaderTask.NewDataWaitIntervalSec = 1
        cls.click_n_collect.config.YtEventLogWriterTask.YtLogPath = cls.event_log_path

    @classmethod
    def before_server_start(cls):
        yt = T.click_n_collect.yt.connect()
        yt.create(
            type='table',
            path=T.ban_table_path,
            recursive=True,
            attributes=T.get_antifraud_ban_table_attrs()
        )
        data = [
            {'buyer_yandexuids': ['1b'], 'frauds': ['didnt_wash_their_hands']},
            {'buyer_uids': ['2b'], 'frauds': ['didnt_do_homework']},
            {'buyer_uids': ['3'], 'frauds': []},
            {'buyer_uids': ['4'], 'frauds': ['whitelist']},
        ]
        yt.write_table(T.ban_table_path, data)

    def test_not_banned(self):
        headers = T.get_headers()

        request = T.get_request()
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers=headers)
        self.assertEqual(response.code, 200)

        request = T.get_request({"user": {"yandexuid": "a", "uid": "3"}})
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers=headers)
        self.assertEqual(response.code, 200)

        request = T.get_request({"user": {"yandexuid": "a", "uid": "4"}})
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers=headers)
        self.assertEqual(response.code, 200)

    def test_banned(self):
        headers = T.get_headers()

        request = T.get_request({'yandexuid': '1b'})
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 403)
        self.assertFragmentIn(response, {'error': {'message': 'banned by yandexuid'}})
        T.common_log.expect('Error response: {"error":{"message":"banned by yandexuid"}}, code: 403', 'ERRR')

        request = T.get_request({'yandexuid': '1', 'uid': '2b'})
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 403)
        self.assertFragmentIn(response, {'error': {'message': 'banned by uid'}})
        T.common_log.expect('Error response: {"error":{"message":"banned by uid"}}, code: 403', 'ERRR')

    def test_tvm_ticket(self):
        request = T.get_request()

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('check_user: someone came without "X-Ya-Service-Ticket" header'), 'WARN')
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'no "X-Ya-Service-Ticket" header'
            }
        )

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('check_user: TVM ticket check failed: Malformed ticket'), 'ERRR')
        response = self.click_n_collect.request_json('check_user', method='POST', body=json.dumps(request), headers={'X-Ya-Service-Ticket': ''}, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'TVM ticket check failed: Malformed ticket'
            }
        )

    @classmethod
    def after_server_stop(cls):
        yt = cls.click_n_collect.yt.connect()
        table_path = cls.event_log_path + '/' + datetime.datetime.utcnow().strftime('%Y-%m-%d')
        assert(yt.exists(table_path))

        expected = [
            {
                'timestamp': NotEmpty(),
                'user_info': {'YandexUid': '1b'},
                'ban_reason': {'BannedBy': ['yandexuid']},
                'order_info': None,
            },
            {
                'timestamp': NotEmpty(),
                'user_info': {'YandexUid': '1', 'Uid': '2b'},
                'ban_reason': {'BannedBy': ['uid']},
                'order_info': None,
            },
        ]

        cls.check_yt_table_contains(yt, table_path, expected)


if __name__ == '__main__':
    env.main()
