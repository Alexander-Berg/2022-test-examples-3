# -*- coding: utf-8 -*-
import mock
from hamcrest import assert_that, has_item, has_entry, equal_to
from parameterized import parameterized

from mpfs.common.static import tags
from mpfs.common.static.tags.billing import PARTNER, PRIMARY_2018_DISCOUNT_30, APPLE_APPSTORE, PRIMARY_2019, \
    INAPP_ACTIVE, PRIMARY_2019_V4, GOOGLE_PLAY
from mpfs.common.static.tags.experiment_names import DISABLE_ADS_FOR_MAIL
from mpfs.common.util import from_json
from mpfs.common.util.experiments.logic import enable_experiment_for_uid, experiment_manager
from mpfs.core.billing import Client, Product, datetime
from mpfs.core.billing.inapp.sync_logic import TrustInAppSubscription, process_trust_sub
from mpfs.core.billing.processing.common import provide_paid_service_to_client

from test.base import DiskTestCase, time_machine
from test.base_suit import UserTestCaseMixin
from test.fixtures.users import user_with_plus
from test.helpers import products
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.billing.base import BillingTestCaseMixin, BaseBillingTestCase


class BillingApiTestCase(DiskApiTestCase, DiskTestCase, BillingTestCaseMixin, UserTestCaseMixin):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(BillingApiTestCase, self).setup_method(method)
        self.create_user(self.uid)

    def test_success_format(self):
        self.bind_user_to_market_for_uid(self.uid)
        self.create_subscription(self.uid, line=PRIMARY_2019, pid='100gb_1m_2019')
        self.create_service(self.uid, line=PRIMARY_2019, pid='100gb_1m_2019')
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': '1.1.1.1'})
        init_service_sid = None
        sub_service_sid = None
        nonsub_service_sid = None
        for service in services:
            if service['name'] == 'initial_10gb':
                init_service_sid = service['sid']
            elif service['subscription']:
                sub_service_sid = service['sid']
            else:
                nonsub_service_sid = service['sid']
        fake_ip = '192.168.0.1'
        headers = {'X-Forwarded-For': fake_ip}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/subscriptions', headers=headers)
        assert resp.status_code == 200
        result = from_json(resp.content)
        assert len(services) == len(result['items'])
        assert len(services) == result['total']
        assert result['offset'] == 0
        assert result['limit'] == 100

        subscriptions_map = {i['subscription_id']: i for i in result['items']}

        init_subscription = subscriptions_map[init_service_sid]
        assert 'next_charge' not in init_subscription
        assert 'active_until' not in init_subscription
        assert not init_subscription.pop('auto_renewable')
        assert init_subscription['service'].pop('name') == u'За регистрацию Диска'
        assert init_subscription['service'].pop('key') == 'initial_10gb'
        assert init_subscription['service'].pop('space') == 10737418240
        assert not init_subscription.pop('service')
        assert init_subscription.pop('subscription_id') == init_service_sid
        assert init_subscription.pop('order', {}).get('payment_method') is None
        assert not init_subscription

        auto_prolongable_subscription = subscriptions_map[sub_service_sid]
        assert auto_prolongable_subscription.pop('next_charge') == auto_prolongable_subscription.pop('active_until')
        assert auto_prolongable_subscription.pop('auto_renewable')
        assert auto_prolongable_subscription['service'].pop('name') == u'Подписка 100 ГБ на месяц'
        assert auto_prolongable_subscription['service'].pop('key') == '100gb_1m_2019'
        assert auto_prolongable_subscription['service'].pop('space') == 107374182400
        assert not auto_prolongable_subscription.pop('service')
        assert auto_prolongable_subscription.pop('subscription_id') == sub_service_sid
        assert auto_prolongable_subscription.pop('order', {}).get('payment_method') == 'bankcard'
        assert not auto_prolongable_subscription

        non_auto_prolongable_subscription = subscriptions_map[nonsub_service_sid]
        assert 'next_charge' not in non_auto_prolongable_subscription
        assert non_auto_prolongable_subscription.pop('active_until')
        assert not non_auto_prolongable_subscription.pop('auto_renewable')
        assert non_auto_prolongable_subscription['service'].pop('name') == u'100 ГБ на месяц'
        assert non_auto_prolongable_subscription['service'].pop('key') == '100gb_1m_2019'
        assert non_auto_prolongable_subscription['service'].pop('space') == 107374182400
        assert not non_auto_prolongable_subscription.pop('service')
        assert non_auto_prolongable_subscription.pop('subscription_id') == nonsub_service_sid
        assert non_auto_prolongable_subscription.pop('order', {}).get('payment_method') == 'bankcard'
        assert not non_auto_prolongable_subscription

    def test_forwarding_ip(self):
        ip = '192.168.0.1'
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']), \
                mock.patch('mpfs.core.services.mpfsproxy_service.MpfsProxy.open_url') as mpfs_proxy_open_url:
            mpfs_proxy_open_url.return_value = 200, [], {}
            resp = self.client.get('disk/billing/subscriptions', headers={'X-Forwarded-For': ip})
            assert resp.status_code == 200
            assert 'ip=%s' % ip in mpfs_proxy_open_url.call_args[0][0]

    def test_fail_bad_scopes(self):
        headers = {'X-Forwarded-For': '127.0.0.1'}
        with self.specified_client(uid=self.uid, scopes=['yadisk:all']), \
             mock.patch('mpfs.platform.permissions.BasePlatformPermission.is_legacy_internal_auth', return_value=False), \
             mock.patch('mpfs.platform.permissions.BasePlatformPermission.is_conductor_auth_fallback_mode', return_value=False):
            resp = self.client.get('disk/billing/subscriptions', headers=headers)
            assert resp.status_code == 403

    def test_fail_no_uid(self):
        headers = {'X-Forwarded-For': '127.0.0.1'}
        with self.specified_client(uid=None, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/subscriptions', headers=headers)
            assert resp.status_code == 401

    def test_use_eng_language(self):
        headers = {'X-Forwarded-For': '127.0.0.1', 'Accept-Language': 'en'}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/subscriptions', headers=headers)
            result = from_json(resp.content)
            assert result['items'][0]['service']['name'] == 'For registering Disk'

    def test_does_not_auto_initialize_user(self):
        headers = {'X-Forwarded-For': '127.0.0.1', 'Accept-Language': 'en'}
        with self.specified_client(uid='fake_user', scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/subscriptions', headers=headers)
            assert resp.status_code == 404


class BillingProductsApiTestCase(DiskApiTestCase, BaseBillingTestCase, BillingTestCaseMixin, UserTestCaseMixin):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'disk/billing/products'
    expected_schema = {
        "type": "array",
        "minItems": 1,
        "items": {
            "type": "object",
            "properties": {
                "display_space": {"type": "number"},
                "display_space_units": {"type": "string"},
                "periods": {
                    "type": "object",
                    "properties": {
                        "year": {
                            "type": "object",
                            "properties": {
                                "product_id": {"type": "string"},
                                "price": {"type": "number"}
                            },
                            "required": ["product_id", "price"]
                        },
                        "month": {
                            "type": "object",
                            "properties": {
                                "product_id": {"type": "string"},
                                "price": {"type": "number"}
                            },
                            "required": ["product_id", "price"]
                        }
                    }
                },
                "space": {"type": "number"},
                "currency": {"type": "string"},
                "discount": {
                    "type": "object",
                    "properties": {
                        "percentage": {"type": "number"},
                        "active_until_ts": {"type": "number"}
                    },
                    "required": ["percentage"]
                },
            },
            "required": ["display_space", "display_space_units", "periods", "space", "currency"]
        }
    }

    def test_list_products(self):
        headers = {'X-Forwarded-For': '127.0.0.1'}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/products', headers=headers)
            actual_result = from_json(resp.content)
        self.assertHasValidSchema(actual_result['items'], self.expected_schema)

    def test_list_products_float_response(self):
        self.bind_user_to_market(uid=self.uid)
        self.give_discount(self.uid, PRIMARY_2018_DISCOUNT_30)
        headers = {'X-Forwarded-For': '127.0.0.1'}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']), \
                mock.patch('mpfs.core.billing.product.get_default_products_line', return_value=PRIMARY_2019_V4):
            resp = self.client.get('disk/billing/products', query={'locale': 'en'}, headers=headers)
            actual_result = from_json(resp.content)
        product_with_float_price = filter(lambda x: x['periods']['month']['product_id'] == '100gb_1m_2019_v4_discount_30', actual_result['items'])[0]
        assert product_with_float_price['periods']['month']['price'] == 1.4

    @parameterized.expand([('forbidden_for_read', 'cloud_api:disk.read', 403),
                           ('permitted_for_billing', 'cloud_api:disk.billing.read', 200),
                           ('permitted_for_webdav', 'yadisk:all', 200)])
    def test_permissions(self, case_name, scope, expected_status):
        headers = {'X-Forwarded-For': '196.64.1.2'}
        with self.specified_client(uid=self.uid, scopes=[scope]):
            resp = self.client.get('disk/billing/products', headers=headers)
        assert resp.status_code == expected_status

    def test_list_products_with_discount(self):
        self.give_discount(self.uid, PRIMARY_2018_DISCOUNT_30)
        headers = {'X-Forwarded-For': '127.0.0.1'}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get(self.url, headers = headers)
            actual_result = from_json(resp.content)
        self.assertHasValidSchema(actual_result['items'], self.expected_schema)
        assert 'discount' in actual_result['items'][0]
        assert actual_result['items'][0]['discount']['percentage'] == 30

    @parameterized.expand([('ru', u'ГБ'),
                           ('en', u'GB')])
    def test_locale(self, locale, expected_units):
        headers = {'X-Forwarded-For': '196.64.1.2'}
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/products', query={'locale': locale}, headers=headers)
        actual_result = from_json(resp.content)
        assert_that(actual_result['items'], has_item(has_entry('display_space_units', expected_units)))


class BillingInappTestCase(BillingTestCaseMixin, UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(BillingInappTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.bind_user_to_market_for_uid(self.uid)

        with open('fixtures/json/trust_verify_response.json') as fix_file:
            self.verify_response = {'result': {'receipt_info': from_json(fix_file.read()), 'receipt_check_status': 'valid'}}
        self.receipt = self.verify_response['result']['receipt_info']['latest_receipt']

        with open('fixtures/json/trust_subscription_response.json') as fix_file:
            self.trust_subscription_response = from_json(fix_file.read())

        with open('fixtures/json/trust_verify_response_single_app_sub.json') as fix_file:
            self.verify_response_1_serivce = {
                'result': {'receipt_info': from_json(fix_file.read()), 'receipt_check_status': 'valid'}}
        self.receipt_1_serivce = self.verify_response_1_serivce['result']['receipt_info']['latest_receipt']

        self.pid_1 = self.verify_response_1_serivce['result']['receipt_info']['receipt']['in_app'][0]['product_id']
        self.product = Product(pid=self.pid_1)
        self.otid_1 = self.verify_response_1_serivce['result']['receipt_info']['receipt']['in_app'][0]['original_transaction_id']

    def test_process_receipt(self):
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=self.verify_response), \
                self.specified_client(scopes=['cloud_api:disk.write']):
            url = 'disk/billing/in-app/receipt'
            body = {
                'receipt': self.receipt,
            }
            qs = {
                'store_id': APPLE_APPSTORE,
                'package_name': '123',
                'store_product_id': '1tb_1y_apple_appstore_2019',
            }
            resp = self.client.put(url, qs, data=body)
        assert resp.status_code == 200
        self.assertUserHasExactServices(self.uid, ['initial_10gb', '1tb_1m_apple_appstore_2019', '1tb_1y_apple_appstore_2019'])

    def test_process_trust_receipt(self):
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=self.trust_subscription_response), \
                self.specified_client(scopes=['cloud_api:disk.write']), enable_experiment_for_uid('inapp_v2', self.uid):
            url = 'disk/billing/in-app/receipt'
            body = {
                'receipt': self.receipt,
            }
            qs = {
                'store_id': APPLE_APPSTORE,
                'package_name': '123',
                'store_product_id': '1tb_1y_apple_appstore_2019',
            }
            resp = self.client.put(url, qs, data=body)
        assert resp.status_code == 200
        self.assertUserHasExactServices(self.uid, ['initial_10gb', '1tb_1m_apple_appstore_2019', '1tb_1y_apple_appstore_2019'])

    def test_uid_mismatch(self):
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=self.verify_response), \
             self.specified_client(scopes=['cloud_api:disk.write']):
            url = 'disk/billing/in-app/receipt'
            body = {
                'receipt': self.receipt,
            }
            qs = {
                'store_id': APPLE_APPSTORE,
                'package_name': '123',
                'store_product_id': '1tb_1y_apple_appstore_2019',
            }
            resp = self.client.put(url, qs, data=body)
        assert resp.status_code == 200
        self.assertUserHasExactServices(self.uid, ['initial_10gb', '1tb_1m_apple_appstore_2019', '1tb_1y_apple_appstore_2019'])

        self.create_user(self.user_3.uid)
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=self.verify_response), \
             self.specified_client(scopes=['cloud_api:disk.write'], uid=self.user_3.uid):
            resp = self.client.put(url, qs, data=body)
            assert resp.status_code == 400

    def test_filtering_services_by_payment_method(self):
        now_ts = 1562080080  # 2 July 2019 г., 18:08:00

        client = Client(self.uid)
        provide_paid_service_to_client(client, self.product, APPLE_APPSTORE, auto=True, bb_time=now_ts - 1,
                                       receipt='123', original_transaction_id=self.otid_1, state=INAPP_ACTIVE)
        self.create_subscription(self.uid, line=PRIMARY_2019, pid='100gb_1m_2019')

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=self.verify_response_1_serivce), \
                time_machine(datetime.fromtimestamp(now_ts)), \
                self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/subscriptions', query={'payment_methods': APPLE_APPSTORE})

        result = from_json(resp.content)
        assert len(result['items']) == 1
        assert result['total'] == 1
        assert result['items'][0]['service']['key'] == self.pid_1
        assert result['items'][0].get('order', {}).get('payment_method') == APPLE_APPSTORE

    def test_inapp_product_listing_format(self):
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/in-app/products', query={'store_id': APPLE_APPSTORE})

        assert resp.status_code == 200
        result = from_json(resp.content)
        assert len(result['items']) == 2

        assert {'100gb_1m_apple_appstore_2019', '100gb_1y_apple_appstore_2019'} == {x['product_id'] for x in result['items']}
        for p in result['items']:
            if experiment_manager.is_feature_active(DISABLE_ADS_FOR_MAIL):
                assert {'product_id', 'space', 'display_space', 'display_space_units',
                        'is_best_offer', 'is_yandex_plus', 'disables_mail_ads'} == set(p.keys())
            else:
                assert {'product_id', 'space', 'display_space', 'display_space_units', 'is_best_offer', 'is_yandex_plus'} == set(p.keys())

    def test_inapp_products_listing_format_float_values(self):
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/in-app/products', query={'store_id': APPLE_APPSTORE})

        assert resp.status_code == 200
        result = from_json(resp.content)
        assert len(result['items']) == 2

        assert {'100gb_1m_apple_appstore_2019', '100gb_1y_apple_appstore_2019'} == {x['product_id'] for x in result['items']}
        for p in result['items']:
            if experiment_manager.is_feature_active(DISABLE_ADS_FOR_MAIL):
                assert {'product_id', 'space', 'display_space', 'display_space_units',
                        'is_best_offer', 'is_yandex_plus', 'disables_mail_ads'} == set(p.keys())
            else:
                assert {'product_id', 'space', 'display_space', 'display_space_units', 'is_best_offer',
                        'is_yandex_plus'} == set(p.keys())

    def test_listing_nonexistent_inapp_store(self):
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/in-app/products', query={'store_id': '123'})
        assert resp.status_code == 400

    def test_trying_process_invalid_receipt(self):
        invalid_verify_response = {'result': {'receipt_info': {}, 'receipt_check_status': 'invalid'}}

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request', return_value=invalid_verify_response), \
                self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            url = 'disk/billing/in-app/receipt'
            body = {
                'receipt': self.receipt,
            }
            qs = {
                'store_id': APPLE_APPSTORE,
                'package_name': '123',
                'store_product_id': '1tb_1y_apple_appstore_2019',
            }
            resp = self.client.put(url, qs, data=body)

        assert resp.status_code == 400

    @parameterized.expand([(GOOGLE_PLAY,),
                           (APPLE_APPSTORE,)])
    def test_inapp_default_tariffs(self, store_id):
        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/in-app/tariffs', query={'store_id': store_id})

        assert resp.status_code == 200
        result = from_json(resp.content)
        assert 'current_product' not in result
        assert result['payment_method'] == store_id
        assert len(result['items']) == 3
        for item in result['items']:
            assert 'is_best_offer' in item
            assert 'periods' in item
            assert 'id' in item
            periods = item['periods']
            assert 'month' in periods
            assert 'product_id' in periods['month']
            assert 'year' in periods
            assert 'product_id' in periods['year']

    def test_inapp_tariffs_with_current_product(self):
        pid = '1tb_1y_apple_appstore_2019'
        store_id = APPLE_APPSTORE
        raw_trust_subs = {
            "uid": self.uid,
            "sync_dt": "2019-08-01T12:19:17+03:00",
            "store_expiration_dt": "2037-07-30T12:19:17+03:00",
            "state": "ACTIVE",
            "product_id": pid,
            "subs_until_dt": "2027-12-06T12:19:17+03:00",
            "store_subscription_id": "1",
            "id": 1,
            "store_id": store_id,
        }
        process_trust_sub(TrustInAppSubscription(raw_trust_subs))

        with self.specified_client(uid=self.uid, scopes=['cloud_api:disk.billing.read']):
            resp = self.client.get('disk/billing/in-app/tariffs', query={'store_id': store_id})

        assert resp.status_code == 200
        result = from_json(resp.content)
        assert 'current_product' in result
        assert result['current_product']['product_id'] == pid
        assert len(result['items']) == 1
