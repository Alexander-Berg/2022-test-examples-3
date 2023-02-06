#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import Equal, NotEmpty, Contains
from market.click_n_collect.tools.yt_tables_deployer.library.database import create_database, create_config
import json
import time
import datetime


class T(env.TestSuite):
    """ Test that we send click to clickdaemon """

    storage_create_context = None
    yt = None
    goods = None

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
                'firstName': 'Petya',
                'lastName': 'Taburetkin',
                'thirdName': '',
                'email': 'market-prj-goods@yandex-team.ru',
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
    def get_result(cls, result={}):
        default = {
            'status': 'ok',
            'data': {
                'orderId': NotEmpty(),
                'location': {
                    'address': NotEmpty(),
                },
                'totalPrice': 10,
                'orderQuantity': 5
            }
        }
        default.update(result)
        return default

    @classmethod
    def get_headers(cls, headers={}):
        default = {'X-Market-Req-Id': 'req0'}
        default.update(headers)
        return default

    @classmethod
    def get_status_change_request(cls, target_status, checkout_id, goods_id, merchant_id=''):
        status_id = None
        if target_status == 'REGISTERED':
            status_id = 'FFCR00F'
        elif target_status == 'PICKUP_READY':
            status_id = 'FFCM00F'
        elif target_status == 'CANCELED':
            status_id = 'FFRJ00F'
        elif target_status == 'SUCCESS':
            status_id = 'FFGT00F'

        return {
            'checkoutOrderId': checkout_id,
            'targetStatus': {
                'success': True,
                'customerStatus': {
                    'statusId': status_id,
                    'statusName': 'Создан',
                },
                'detailStatus': {
                    'statusId': 'RINPRGR',
                    'statusName': 'Выполняется',
                },
                'merchantOrderId': merchant_id,
                'orderId': goods_id
            }
        }

    @classmethod
    def get_order_id_by_provider_id(cls, yt, provider_id):
        rows = list(yt.select_rows('* FROM [%s]' % T.click_n_collect.config.YtStorage.OrderStateTable))
        for row in rows:
            if row['provider_id'] == str(provider_id):
                return row['id']

        raise Exception('can\'t find order with provider_id = ' + str(provider_id))

    @classmethod
    def prepare(cls):
        cls.click_n_collect.with_yt = True

        cls.click_n_collect.with_goods_server = True
        cls.click_n_collect.goods_server.config.Prices['item1'] = 10
        cls.click_n_collect.goods_server.config.Prices['item2'] = 100
        cls.click_n_collect.goods_server.config.Remainings['item1'] = 1
        cls.click_n_collect.goods_server.config.Remainings['item2'] = 0
        cls.click_n_collect.goods_server.config.Locations['loc1'].Address = 'Washington beach 13, Vice City'
        cls.click_n_collect.config.GoodsRequester.Host = ''         # it will be filled by beam

        cls.click_n_collect.config.MakeOrderHandler = True
        cls.click_n_collect.config.MakeOrderRequester.Host = ''     # it will be filled by beam

        cls.click_n_collect.with_clickdaemon = True
        cls.click_n_collect.config.OrderMonitorTask.MoneySettings.ShopId = 1
        cls.click_n_collect.config.OrderMonitorTask.MoneySettings.ClickDaemonUrl = ''   # it will be filled by beam

        cls.click_n_collect.with_yasms = True
        cls.click_n_collect.yasms.config.AllowedSenders.append('zuppa-service')
        cls.click_n_collect.config.YaSms.ServiceId = 'zuppa-service'
        cls.click_n_collect.config.YaSmsSenderRequester.Host = ''   # it will be filled by beam

        cls.click_n_collect.config.YtStorage.CopyFrom(create_config('//home'))

        cls.click_n_collect.config.OrderMonitorTask.CheckIntervalSec = 1    # make checks faster

    @classmethod
    def before_server_start(cls):
        cls.yt = T.click_n_collect.yt.connect()

        # create order storage dynamic table
        create_database(cls.click_n_collect.config.YtStorage, T.yt, replicate=False)
        cls.yt.insert_rows(cls.click_n_collect.config.YtStorage.CountersTable, [{'id': 'last_order_id', 'value': 1}])

        cls.goods = T.click_n_collect.goods_server.connect()

    def test_clickdaemon_click(self):
        """ Test that in case of status change to SUCCESS we make virtual click """

        yt_storage_config = T.click_n_collect.config.YtStorage
        old_mtime = T.get_table_mtime(T.yt, yt_storage_config.OrderStateTable)

        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(T.get_request()), headers=T.get_headers())
        self.assertFragmentIn(response, T.get_result())

        checkout_order_id = response.root['data']['orderId']
        goods_order_id = 'goods_id_1'

        old_mtime = T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        old_mtime_success = T.get_table_mtime(T.yt, yt_storage_config.SuccessTable)

        # initially orders status should be CREATED
        order_id = T.get_order_id_by_provider_id(T.yt, checkout_order_id)
        expected = {
            'id': Equal(order_id),
            'status': 'CREATED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        # move order ts to the future so tests passes
        data = list(T.yt.lookup_rows(yt_storage_config.OrderStateTable, [{'id': order_id}]))[0]
        t = datetime.datetime(2020, 12, 19)
        data["timestamp"] = int(time.mktime(t.timetuple()))
        del data["id_hash"]
        T.yt.insert_rows(yt_storage_config.OrderStateTable, [data])

        old_mtime = T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        old_mtime_success = T.get_table_mtime(T.yt, yt_storage_config.SuccessTable)

        # then we change order status SUCCESS
        response = T.goods.request_json('status_change', method='POST',
                                        body=json.dumps(T.get_status_change_request('SUCCESS', checkout_order_id, goods_order_id)))
        self.assertFragmentIn(response, {'success': True})

        T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)

        # check that info sms sent & status updated in db
        expected = {
            'id': Equal(order_id),
            'status': 'SUCCESS',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        T.wait_table_changed(T.yt, yt_storage_config.SuccessTable, old_mtime_success)

        expected = {
            'id': Equal(order_id),
            'provider_id': checkout_order_id,
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.SuccessTable, expected)

        # check clickdaemon request
        T.common_log.expect('10 rub order ' + str(checkout_order_id) + ' charged from goods.ru', 'INFO')
        T.trace_log._make_expectation(
            target_host=T.click_n_collect.config.OrderMonitorTask.MoneySettings.ClickDaemonUrl,
            request_method=Contains('/redir'), http_code=200, type='OUT')


if __name__ == '__main__':
    env.main()
