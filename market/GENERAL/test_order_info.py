#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import Contains
from market.click_n_collect.proto.order_info_pb2 import TResponse
from market.click_n_collect.mock.goods.proto.status_pb2 import TResponseV2
import json


class T(env.TestSuite):

    @classmethod
    def get_headers(cls, headers={}):
        default = {'X-Ya-Service-Ticket': T.tvm_client_secret}
        default.update(headers)
        return default

    @classmethod
    def get_request(cls, request={}):
        default = {'checkoutOrderId': '20008784146'}
        default.update(request)
        return default

    @classmethod
    def prepare(cls):
        cls.click_n_collect.config.OrderInfoHandler = True

        cls.click_n_collect.with_tvmapi = True
        cls.click_n_collect.config.ServerTvm.RemoteId.extend([cls.tvm_client_id])

        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.config.GoodsRequester.Host = ''         # it will be filled by beam

        status = TResponseV2()
        status.Success = True
        status.CustomerStatus.StatusId = 'FFGT00F'
        status.CustomerStatus.StatusName = 'Получен'
        status.DetailStatus.StatusId = 'RCOMPLT'
        status.DetailStatus.StatusName = 'Доставлен'
        status.OrderId = 'shopId1'
        status.MerchantOrderId = 'merchantId1'
        cls.click_n_collect.goods_server.config.Statuses['20008784146'].CopyFrom(status)

    def test_positive(self):
        headers = T.get_headers()

        request = T.get_request()
        response = self.click_n_collect.request_json('order_info', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'status': 'ok',
                'statusName': 'Получен',
            },
        )

    def test_error_code(self):
        headers = T.get_headers()

        request = T.get_request({'checkoutOrderId': 'nan'})
        response = self.click_n_collect.request_json('order_info', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Contains('can\'t get status: error while parsing goods.ru response'),
                'code': TResponse.BAD_RESPONSE_FROM_PROVIDER
            },
        )

    def test_tvm_ticket(self):
        request = T.get_request()

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('order_info: someone came without "X-Ya-Service-Ticket" header'), 'WARN')
        response = self.click_n_collect.request_json('order_info', method='POST', body=json.dumps(request), fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'no "X-Ya-Service-Ticket" header'
            }
        )

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('order_info: TVM ticket check failed: Malformed ticket'), 'ERRR')
        response = self.click_n_collect.request_json('order_info', method='POST', body=json.dumps(request), headers={'X-Ya-Service-Ticket': ''}, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'TVM ticket check failed: Malformed ticket'
            }
        )


if __name__ == '__main__':
    env.main()
