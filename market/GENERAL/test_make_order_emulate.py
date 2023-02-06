#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import NotEmpty, Contains
from market.click_n_collect.tools.yt_tables_deployer.library.database import create_database, create_config
import json


class T(env.TestSuite):
    @classmethod
    def get_headers(cls, headers={}):
        default = {'X-Ya-Service-Ticket': T.tvm_client_secret}
        default.update(headers)
        return default

    @classmethod
    def get_request(cls, request={}):
        default = {
            'locationId': 'loc1',
            'merchantId': '12',
            'clickNCollectId': 'item1',
            'quantity': 5,
            'customer': {
                'comment': 'Комментарий',
                'firstName': 'market-cnc-test',
                'lastName': 'Фамилия',
                'thirdName': 'Отчество',
                'email': 'ivanov@gyandex.ru',
                'phone': '79261830411'
            },
            'user': {
                'yandexuid': '1',
            }
        }
        default.update(request)
        return default

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_tvmapi = True
        cls.click_n_collect.config.ServerTvm.RemoteId.extend([cls.tvm_client_id])

        cls.click_n_collect.with_yt = True
        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.goods_server.config.Prices['item1'] = 10
        cls.click_n_collect.goods_server.config.Remainings['item1'] = 1
        cls.click_n_collect.goods_server.config.Locations['loc1'].Name = 'Location 1'
        cls.click_n_collect.goods_server.config.Locations['loc1'].Address = 'Washington beach 13, Vice City'

        cls.click_n_collect.goods_server.config.Locations['loc_without_stock'].Name = 'This loc hasn\'t stock'
        cls.click_n_collect.goods_server.config.Locations['loc_without_stock'].Address = 'Grove street 13, San Andreas'

        cls.click_n_collect.config.MakeOrderHandler = True
        cls.click_n_collect.config.MakeOrderRequester.Host = ''    # it will be filled by beam

        cls.click_n_collect.config.YtStorage.CopyFrom(create_config('//home'))

    @classmethod
    def before_server_start(cls):
        yt = T.click_n_collect.yt.connect()

        # create order storage dynamic table
        create_database(cls.click_n_collect.config.YtStorage, yt, replicate=False)
        yt.insert_rows(cls.click_n_collect.config.YtStorage.CountersTable, [{'id': 'last_order_id', 'value': 1}])

    def test_emulate_ok(self):
        headers = T.get_headers()
        request = T.get_request({'emulate': {'success': True}})
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'status': 'ok',
                'data': {
                    'orderId': NotEmpty(),
                    'location': NotEmpty(),
                    'totalPrice': NotEmpty(),
                    'orderQuantity': NotEmpty(),
                }
            }
        )

    def test_emulate_error(self):
        headers = T.get_headers()
        request = T.get_request({'emulate': {'error': 1}})
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'errorCode': 1,
                'message': 'К сожалению, товар уже разобрали. Попробуйте выбрать аналогичный другой и повторите заказ',
            }
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    @classmethod
    def after_server_stop(cls):
        # since all orders emulated we should not modify storage
        yt = T.click_n_collect.yt.connect()

        actual = list(yt.select_rows('* FROM [{table}]'.format(table=T.click_n_collect.config.YtStorage.OrderStateTable)))
        assert actual == []


if __name__ == '__main__':
    env.main()
