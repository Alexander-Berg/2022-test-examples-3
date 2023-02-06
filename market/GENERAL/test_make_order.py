#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import Equal, NotEmpty, Contains, Regex
from market.click_n_collect.tools.yt_tables_deployer.library.database import create_database, create_config
import datetime
import json


class T(env.TestSuite):
    ban_table_path = '//home/ban'
    event_log_path = '//home/event_log'

    order_id = None
    provider_order_id = None

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
            'paymentType': 'asdf',
            'isAcceptCalling': True,
            'isAcceptRules': True,
            'customer': {
                'comment': 'Комментарий',
                'firstName': 'market-cnc-test',
                'lastName': 'Фамилия',
                'thirdName': 'Отчество',
                'email': 'ivanov@gyandex.ru',
                'phone': '79261830411'
            },
            'marketOfferInfo': {
                'offerId': '1',
                'feedId': '10',
                'productId': 100500,
                'price': 100,
                'showUid': '10001110101100101',
            },
            'user': {
                'yandexuid': '1'
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

        cls.click_n_collect.config.YtAntiFraudReaderTask.BannedUsersPath = cls.ban_table_path
        cls.click_n_collect.config.YtAntiFraudReaderTask.NewDataWaitIntervalSec = 10

        cls.click_n_collect.config.YtEventLogWriterTask.YtLogPath = cls.event_log_path

        cls.click_n_collect.config.YtStorage.CopyFrom(create_config('//home'))

    @classmethod
    def before_server_start(cls):
        yt = T.click_n_collect.yt.connect()

        # create antifraud table & fill it
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

        # create order storage dynamic table
        create_database(cls.click_n_collect.config.YtStorage, yt, replicate=False)
        yt.insert_rows(cls.click_n_collect.config.YtStorage.CountersTable, [{'id': 'last_order_id', 'value': 1}])

    def test_ok(self):
        headers = T.get_headers()

        request = T.get_request()
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers)
        self.assertFragmentIn(
            response,
            {
                'status': 'ok',
                'data': {
                    'orderId': NotEmpty(),
                    'location': {
                        'name': 'Location 1',
                        'address': 'Washington beach 13, Vice City',
                    },
                    'totalPrice': 10,
                    'orderQuantity': 5
                }
            }
        )
        T.order_id = 2
        T.provider_order_id = response.root['data']['orderId']

    def test_error_no_stock(self):
        headers = T.get_headers()

        request = T.get_request()
        request['locationId'] = 'loc_without_stock'
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 500)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'К сожалению, товар уже разобрали. Попробуйте выбрать аналогичный другой и повторите заказ',
                'errorCode': 1
            },
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    def test_error_user_banned(self):
        headers = T.get_headers()

        request = T.get_request({
            'user': {
                'yandexuid': '1b'
            }
        })
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 403)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'user banned by yandexuid',
                'errorCode': 2
            },
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    def test_error_pre_order(self):
        headers = T.get_headers()

        request = T.get_request({
            'quantity': 1,
            'paymentType': 'pre-order'
        })
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Contains('"pre-order" payment type is not allowed'),
                'errorCode': 3
            },
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    def test_error_invalid_request(self):
        headers = T.get_headers()

        request = T.get_request({
            'marketOfferInfo': 'it_is_dict'
        })
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Regex('can\'t parse request body.*expected json map')
            },
        )

        request = T.get_request({
            'user': ''
        })
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': Regex('can\'t parse request body.*expected json map')
            },
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    def test_tvm_ticket(self):
        request = T.get_request()

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('make_order: someone came without "X-Ya-Service-Ticket" header'), 'WARN')
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), fail_on_error=False)
        self.assertEqual(response.code, 400)
        self.assertFragmentIn(
            response,
            {
                'status': 'error',
                'message': 'no "X-Ya-Service-Ticket" header'
            }
        )

        T.common_log.expect(Contains('Error response:'), 'ERRR')
        T.common_log.expect(Contains('make_order: TVM ticket check failed: Malformed ticket'), 'ERRR')
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers={'X-Ya-Service-Ticket': ''}, fail_on_error=False)
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
        yt = T.click_n_collect.yt.connect()
        table_path = cls.event_log_path + '/' + datetime.datetime.utcnow().strftime('%Y-%m-%d')
        assert yt.exists(table_path)

        expected = [
            {
                'timestamp': NotEmpty(),
                'user_info': {'YandexUid': '1'},
                'order_info': {
                    'Success': True,
                    'Request': {
                        'LocationId': 'loc1',
                        'MerchantId': '12',
                        'ClickNCollectId': 'item1',
                        'Quantity': '5',
                        'PaymentType': 'asdf',
                        'IsAcceptCalling': True,
                        'IsAcceptRules': True,
                        'CustomerInfo': {
                            'Comment': 'Комментарий',
                            'FirstName': 'market-cnc-test',
                            'LastName': 'Фамилия',
                            'ThirdName': 'Отчество',
                            'Email': 'ivanov@gyandex.ru',
                            'Phone': '79261830411'
                        },
                        'MarketOfferInfo': {
                            'OfferId': '1',
                            'FeedId': '10',
                            'ProductId': "100500",
                            'Price': 100.0,
                            'ShowUid': '10001110101100101',
                        },
                        'UserInfo': {
                            'YandexUid': '1'
                        }
                    },
                    'GoodsAcceptResponse': {
                        'OrderId': NotEmpty()
                    }
                },
            },
            {
                'timestamp': NotEmpty(),
                'user_info': {'YandexUid': '1b'},
                'ban_reason': {'BannedBy': ['yandexuid']},
                'order_info': None,
            },
        ]

        cls.check_yt_table_contains(yt, table_path, expected)

        # check orders in YT storage
        expected = {
            'id': Equal(T.order_id),
            'status': 'CREATED',
            'provider': 'GOODS',
            'provider_id': T.provider_order_id,
            'market_request_id': NotEmpty(),
            'order_info': NotEmpty(),
            'outlet': Contains('Washington beach 13, Vice City'),
            'market_offer_info': NotEmpty(),
            'customer_info': NotEmpty(),
        }
        cls.check_yt_table_contains2(yt, T.click_n_collect.config.YtStorage.OrderStateTable, expected)


if __name__ == '__main__':
    env.main()
