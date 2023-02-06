#!/usr/bin/env python
# -*- coding: utf-8 -*
import __classic_import     # noqa
import market.click_n_collect.mt.env as env
from market.pylibrary.lite.matcher import Equal, NotEmpty, Contains
from market.click_n_collect.tools.yt_tables_deployer.library.database import create_database, create_config
import json
from urllib import quote_plus


class T(env.TestSuite):
    """ Test sms/mail order status notification """

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

        cls.click_n_collect.with_mail = True
        cls.click_n_collect.config.MailSenderRequester.Host = ''    # it will be filled by beam
        cls.click_n_collect.config.MailSender.Authorization = ''    # it will be filled by beam
        cls.click_n_collect.config.MailSender.Url = ''              # it will be filled by beam

        cls.click_n_collect.with_yasms = True
        cls.click_n_collect.yasms.config.AllowedSenders.append('zuppa-service')
        cls.click_n_collect.config.YaSms.ServiceId = 'zuppa-service'
        cls.click_n_collect.config.YaSmsSenderRequester.Host = ''   # it will be filled by beam

        cls.click_n_collect.config.YtStorage.CopyFrom(create_config('//home'))

        cls.click_n_collect.config.OrderMonitorTask.CheckIntervalSec = 2    # make checks faster

    @classmethod
    def before_server_start(cls):
        cls.yt = T.click_n_collect.yt.connect()

        # create order storage dynamic table
        create_database(cls.click_n_collect.config.YtStorage, T.yt, replicate=False)
        cls.yt.insert_rows(cls.click_n_collect.config.YtStorage.CountersTable, [{'id': 'last_order_id', 'value': 1}])

        cls.goods = T.click_n_collect.goods_server.connect()

    def test_notify_registered(self):
        """ Test that in case of status change CREATED -> REGISTERED we send 1 confirmation sms """

        yt_storage_config = T.click_n_collect.config.YtStorage
        old_mtime = T.get_table_mtime(T.yt, yt_storage_config.OrderStateTable)

        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(T.get_request()), headers=T.get_headers())
        self.assertFragmentIn(response, T.get_result())

        checkout_order_id = response.root['data']['orderId']
        goods_order_id = 'goods_id_1'

        T.common_log.expect('mail sent successfully', 'INFO')
        T.mail_server_common_log.expect('received mail request for Petya market-prj-goods@yandex-team.ru', 'INFO')

        old_mtime = T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        old_mtime_registered = T.get_table_mtime(T.yt, yt_storage_config.RegisteredTable)

        # initially orders status should be CREATED
        order_id = T.get_order_id_by_provider_id(T.yt, checkout_order_id)
        expected = {
            'id': Equal(order_id),
            'status': 'CREATED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        # then we change order status REGISTERED
        response = T.goods.request_json('status_change', method='POST',
                                        body=json.dumps(T.get_status_change_request('REGISTERED', checkout_order_id, goods_order_id)))
        self.assertFragmentIn(response, {'success': True})

        T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)

        # check that info sms sent & status updated in db
        expected_sms_text = 'Petya, ваш заказ на сумму 10 руб. оформлен и находится на подтверждении магазином. ' + \
                            'Вы можете следить за статусом заказа по ссылке ' + \
                            'https://market.yandex.ru/order/' + str(checkout_order_id) + \
                            ' Код для обращения в коллцентр ' + str(goods_order_id)
        T.trace_log._make_expectation(
            target_host=T.click_n_collect.config.YaSmsSenderRequester.Host,
            query_params=Contains(quote_plus(expected_sms_text, safe='/')), http_code=200, type='OUT')
        T.yasms_server_common_log.expect('send sms for 79261830411', 'INFO')

        expected = {
            'id': Equal(order_id),
            'status': 'REGISTERED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        T.wait_table_changed(T.yt, yt_storage_config.RegisteredTable, old_mtime_registered)

        expected = {
            'id': Equal(order_id),
            'provider_id': checkout_order_id,
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.RegisteredTable, expected)

    def test_notify_pickup_ready(self):
        """ Test that in case of status change CREATED -> PICKUP_READY we send 1 confirmation sms """

        yt_storage_config = T.click_n_collect.config.YtStorage
        old_mtime = T.get_table_mtime(T.yt, yt_storage_config.OrderStateTable)

        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(T.get_request()), headers=T.get_headers())
        self.assertFragmentIn(response, T.get_result())

        checkout_order_id = response.root['data']['orderId']
        goods_order_id = 'goods_id_1'
        merchant_order_id = 'merchant_id_1'

        T.common_log.expect('mail sent successfully', 'INFO')
        T.mail_server_common_log.expect('received mail request for Petya market-prj-goods@yandex-team.ru', 'INFO')

        old_mtime = T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)

        # initially orders status should be CREATED
        order_id = T.get_order_id_by_provider_id(T.yt, checkout_order_id)
        expected = {
            'id': Equal(order_id),
            'status': 'CREATED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        old_mtime_ready = T.get_table_mtime(T.yt, yt_storage_config.PickupReadyTable)

        # then we change order status PICKUP_READY
        response = T.goods.request_json('status_change', method='POST',
                                        body=json.dumps(T.get_status_change_request('PICKUP_READY', checkout_order_id, goods_order_id, merchant_order_id)))
        self.assertFragmentIn(response, {'success': True})

        # check that BOTH info & confirmation sms sent & status updated in db
        expected_sms_text = 'Petya, ваш заказ на сумму 10 руб. оформлен и находится на подтверждении магазином. ' + \
                            'Вы можете следить за статусом заказа по ссылке ' + \
                            'https://market.yandex.ru/order/' + str(checkout_order_id) + \
                            ' Код для обращения в коллцентр ' + str(goods_order_id)
        T.trace_log._make_expectation(
            target_host=T.click_n_collect.config.YaSmsSenderRequester.Host,
            query_params=Contains(quote_plus(expected_sms_text, safe='/')), http_code=200, type='OUT')
        T.yasms_server_common_log.expect('send sms for 79261830411', 'INFO')

        expected_sms_text = 'Petya, ваш заказ ' + str(merchant_order_id) + ' на сумму 10 руб. готов к выдаче по адресу: ' \
                            'Washington beach 13, Vice City'
        T.trace_log._make_expectation(
            target_host=T.click_n_collect.config.YaSmsSenderRequester.Host,
            query_params=Contains(quote_plus(expected_sms_text, safe='/')), http_code=200, type='OUT')
        T.yasms_server_common_log.expect('send sms for 79261830411', 'INFO')

        T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        expected = {
            'id': Equal(order_id),
            'status': 'PICKUP_READY',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        T.wait_table_changed(T.yt, yt_storage_config.PickupReadyTable, old_mtime_ready)
        expected = {
            'id': Equal(order_id),
            'provider_id': checkout_order_id,
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.PickupReadyTable, expected)

    def test_notify_cancelled(self):
        """ Test that in case of status change CREATED -> CANCELED we send 1 confirmation sms """

        yt_storage_config = T.click_n_collect.config.YtStorage
        old_mtime = T.get_table_mtime(T.yt, yt_storage_config.OrderStateTable)

        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(T.get_request()), headers=T.get_headers())
        self.assertFragmentIn(response, T.get_result())

        checkout_order_id = response.root['data']['orderId']
        goods_order_id = 'goods_id_1'
        merchant_order_id = 'merchant_id_1'

        T.common_log.expect('mail sent successfully', 'INFO')
        T.mail_server_common_log.expect('received mail request for Petya market-prj-goods@yandex-team.ru', 'INFO')

        old_mtime = T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        old_mtime_failed = T.get_table_mtime(T.yt, yt_storage_config.FailedTable)

        # initially orders status should be CREATED
        order_id = T.get_order_id_by_provider_id(T.yt, checkout_order_id)
        expected = {
            'id': Equal(order_id),
            'status': 'CREATED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        # then we change order status CANCELED
        response = T.goods.request_json('status_change', method='POST',
                                        body=json.dumps(T.get_status_change_request('CANCELED', checkout_order_id, goods_order_id, merchant_order_id)))
        self.assertFragmentIn(response, {'success': True})

        # check that BOTH info & confirmation sms sent & status updated in db
        expected_sms_text = 'Petya, ваш заказ на сумму 10 руб. отменен. Код для обращения в коллцентр ' + str(goods_order_id)
        T.trace_log._make_expectation(
            target_host=T.click_n_collect.config.YaSmsSenderRequester.Host,
            query_params=Contains(quote_plus(expected_sms_text, safe='/')), http_code=200, type='OUT')
        T.yasms_server_common_log.expect('send sms for 79261830411', 'INFO')

        T.wait_table_changed(T.yt, yt_storage_config.OrderStateTable, old_mtime)
        expected = {
            'id': Equal(order_id),
            'status': 'CANCELED',
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.OrderStateTable, expected)

        T.wait_table_changed(T.yt, yt_storage_config.FailedTable, old_mtime_failed)
        expected = {
            'id': Equal(order_id),
            'provider_id': checkout_order_id,
        }
        self.check_yt_table_contains2(T.yt, yt_storage_config.FailedTable, expected)

    def test_notify_error(self):
        """ Test that in case of error make_order we don't send message and don't add order to the db """
        request = T.get_request({'clickNCollectId': 'item2'})
        headers = {'X-Market-Req-Id': 'req1'}
        response = self.click_n_collect.request_json('make_order', method='POST', body=json.dumps(request), headers=headers, fail_on_error=False)
        self.assertFragmentIn(
            response,
            {
                'status': 'error'
            }
        )
        T.common_log.expect(Contains('Error response:'), 'ERRR')

    @classmethod
    def after_server_stop(cls):
        yt = T.click_n_collect.yt.connect()
        yt_storage_config = T.click_n_collect.config.YtStorage
        for row in list(yt.select_rows('* from [{table}]'.format(table=yt_storage_config.OrderStateTable))):
            assert row['market_request_id'] != 'req1'


if __name__ == '__main__':
    env.main()
