# -*- coding: utf-8 -*-

import urlparse
from contextlib import nested

from hamcrest import assert_that, equal_to
from mock import mock
from nose_parameterized import parameterized
from requests.models import Response, Request

from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.core.services.tvm_2_0_service import TVM2Ticket
from mpfs.platform.v1.psbilling.permissions import PsBillingInappPermission
from test.base_suit import UserTestCaseMixin
from test.helpers.matchers import has_same_url_params
from test.helpers.utils import construct_requests_resp, URLHelper
from test.parallelly.api.disk.base import DiskApiTestCase


class PsBillingApiTestCase(DiskApiTestCase, UserTestCaseMixin):
    ps_billing_test_host = 'https://ps-billing-web.qloud.dst.yandex.net/'
    ps_billing_test_handler = ''

    api_mode = tags.platform.INTERNAL
    method = 'GET'
    api_url = ''
    ps_billing_api_400_resp = Response()
    tvm_ticket_mock = mock.patch('mpfs.engine.process.get_tvm_2_0_service_ticket_for_client',
                                 return_value=TVM2Ticket.build_tvm_ticket('service_ticket'))
    body = '{"receipt": "some_receipt"}'

    def setup_method(self, method):
        super(PsBillingApiTestCase, self).setup_method(method)
        url = self.ps_billing_test_host + self.ps_billing_test_handler
        request = Request('GET', url, params='uid=%s' % self.uid, data=None, headers={}, json=self.body).prepare()

        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = request
        self.ok_response = {}
        self.success_psbilling_api_resp._content = to_json(self.ok_response)

        self.ps_billing_api_400_resp.status_code = 400
        self.ps_billing_api_400_resp.request = request
        self.ps_billing_api_400_resp._content = '{"error":{"name":"bad-request","message":"failed to process"}}'
        self.ps_billing_api_400_resp.body = self.ps_billing_api_400_resp._content
        self.tvm_ticket_mock.start()

    def teardown_method(self, method):
        super(PsBillingApiTestCase, self).teardown_method(method)
        self.tvm_ticket_mock.stop()

    def headers_test(self, lang, expected_lang):
        headers = {}
        if lang is not None:
            headers['Accept-Language'] = lang

        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            self.client.request(self.method, self.api_url, uid=self.uid, headers=headers)

        url = mocked_ps_billing.call_args[0][1]
        query_parameters = urlparse.parse_qs(urlparse.urlparse(url).query)
        lang_values = query_parameters['lang']

        assert lang_values
        actual_lang = lang_values[0]
        assert actual_lang == expected_lang

    def mock_response(self, response):
        return nested(
            self.specified_client(scopes=PsBillingInappPermission.scopes),
            mock.patch('mpfs.core.services.ps_billing_service.PsBillingService.request', return_value=response))


# psbilling/users/passport/subscriptions/statuses
class V1GetSubscriptionsStatusesForPassportTestCase(PsBillingApiTestCase):
    api_version = 'v1'
    method = 'GET'
    url = 'psbilling/users/passport/subscriptions/statuses'
    ps_billing_test_handler = '/v1/users/passport/subscriptions_statuses'

    def setup_method(self, method):
        super(V1GetSubscriptionsStatusesForPassportTestCase, self).setup_method(method)

        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = self.request
        self.success_psbilling_api_resp._content = to_json({'360': True, 'disk': False})

    def test_default(self):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request(self.method, self.url, uid=self.uid)
        url = mocked_ps_billing.call_args[0][1]
        assert url.startswith(self.ps_billing_test_handler)
        assert 'lang=ru' in url
        assert '__uid=%s' % self.uid in url
        assert from_json(resp.content) == {'360': True, 'disk': False}


# psbilling/users/productsets
class GetUserProductsTestCase(PsBillingApiTestCase):
    method = 'GET'
    api_url = 'psbilling/users/productsets/product_set_key/products'

    def setup_method(self, method):
        super(GetUserProductsTestCase, self).setup_method(method)

        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = self.request
        self.products_response = {'items': [
            {
                u'product_id': u'blabla',
                u'product_id_family': u'bla',
                u'title': u'some title',
                u'best_offer': True,
                u'product_type': u'subscription',
                u'features': [
                    {
                        u'description': u'Приоритетная поддержка',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                    {
                        u'description': u'Без рекламы',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                ],
                u'trial': {
                    u'definition': {
                        u'period': u'day',
                        u'periods_count': 90,
                        u'price_per_user_in_month': 0
                    }
                },
                u'prices': [{
                    u'price_id': u'3c0922e6-e925-4930-aae0-d1315afc6ebc',
                    u'amount': 790,
                    u'currency': u'RUB',
                    u'period': u'year',
                }]
            }
        ]}
        self.success_psbilling_api_resp._content = to_json(self.products_response)

    def default_test(self, expected_url, query=None):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request(self.method, self.api_url, query=query, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert_that(URLHelper(url).path, equal_to(URLHelper(expected_url).path))
        assert_that(url, has_same_url_params(expected_url))

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['items'] == self.products_response['items']
        return data

    def error_handling_test(self, resp_from_psbilling_api, expected_status):
        with self.specified_client(scopes=PsBillingInappPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=resp_from_psbilling_api):
            resp = self.client.request(self.method, self.api_url, self.uid)

        assert resp.status_code == expected_status


# noinspection PyUnusedLocal
class V1GetUserProductsTestCase(GetUserProductsTestCase):
    api_version = 'v1'
    ps_billing_test_handler = '/v2/productsets'

    def test_default(self):
        self.default_test(expected_url='/v2/productsets/product_set_key/products?lang=ru&__uid=128280859')

    @parameterized.expand([
        ('default_language', None, 'ru'),
        ('en', 'en', 'en'),
        ('ru', 'ru', 'ru'),
        ('complex', 'en-US,en;q=0.9,ru;q=0.8', 'en'),
    ])
    def test_headers(self, case_name, lang, expected_lang):
        self.headers_test(lang, expected_lang)

    @parameterized.expand([
        ('bad-request', PsBillingApiTestCase.ps_billing_api_400_resp, 400),
    ])
    def test_error_handling(self, case_name, resp_from_psbilling_api, expected_status):
        self.error_handling_test(resp_from_psbilling_api, expected_status)

    def test_currency(self):
        self.default_test(query={'currency': 'USD'},
                          expected_url='/v2/productsets/product_set_key/products?lang=ru&__uid=128280859&currency=USD')


class V1GetUserProductsResponseStructureTestCase(GetUserProductsTestCase):
    api_version = 'v1'
    ps_billing_test_handler = '/v2/productsets'

    def setup_method(self, method):
        super(V1GetUserProductsResponseStructureTestCase, self).setup_method(method)
        self.products_response = {'items': [
            {
                u'product_id': u'blabla',
                u'product_id_family': u'bla',
                u'title': u'some title',
                u'best_offer': True,
                u'product_type': u'subscription',
                u'features': [
                    {
                        u'description': u'Приоритетная поддержка',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                    {
                        u'description': u'Без рекламы',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                ],
                u'trial': {
                    u'definition': {
                        u'period': u'day',
                        u'periods_count': 90,
                        u'price_per_user_in_month': 0
                    }
                },
                u'prices': [{
                    u'price_id': u'3c0922e6-e925-4930-aae0-d1315afc6ebc',
                    u'amount': 790,
                    u'currency': u'RUB',
                    u'period': u'year',
                    u'start_discount': {
                        'period': {
                            'unit': 'year',
                            'length': 1,
                        },
                        'periods_count': 1,
                        'price': 500,
                    }
                }]
            }],
            'order_status': u'paid',
            'current_subscription': {
                u'service_id': u'uuid - индентификатор сервиса 1',
                u'readonly': False,
                u'creation_date': u'2019-12-01T00:00:000+0300',
                u'subscription_until': u'2021-01-01T12:00:00.000+0300',
                u'status': u'ACTIVE',
                u'order_status': u'paid',
                u'billing_status': u'paid',
                u'auto_prolong_enabled': True,
                u'product': {
                    u'product_id': u'что-то вроде mail_v1_pro_plus_disk',
                    u'product_id_family': u'что-то вроде mail_pro_plus_disk',
                    u'title': u'почта и диск про (или другое локализованное имя)',
                    u'best_offer': True,
                    u'product_type': u'subscription',
                    u'features': [
                        {u'description': u'Отключение рекламы (локализованное)'},
                        {u'description': u'Приоритетная поддержка (локализованное)'},
                        {u'description': u'100500 терабайт диска (локализованное)'}
                    ],
                    u'trial': {
                        u'definition': {
                            u'period': u'day',
                            u'periods_count': 90,
                            u'price_per_user_in_month': 0
                        }
                    },
                    u'price': {
                        u'price_id': u'uuid цены 3',
                        u'amount': 40000,
                        u'currency': u'USD',
                        u'period': u'year',
                        u'start_discount': {
                            'period': {
                                'unit': 'year',
                                'length': 1,
                            },
                            'periods_count': 1,
                            'price': 35000,
                        },
                    }
                }
            },
            'active_promo': {
                u'key': u'new_year',
                u'title': u'Новый год',
                u'active_until_date': u'2019-12-01T00:00:000+0300',
                u'payload': u'[{"region":"ru","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}},{"region":"default","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}}],'
            },
        }
        self.success_psbilling_api_resp._content = to_json(self.products_response)

    def test_has_start_discount_field_in_each_price(self):
        _ = self.default_test(expected_url='/v2/productsets/product_set_key/products?lang=ru&__uid=128280859')
        # внутри default_test уже идет проверка ответа против self.products_response


# noinspection PyUnusedLocal
class V2GetUserProductsTestCase(GetUserProductsTestCase):
    api_version = 'v2'
    ps_billing_test_handler = '/v3/productsets'

    def setup_method(self, method):
        super(V2GetUserProductsTestCase, self).setup_method(method)
        self.products_response = {'items': [
            {
                u'product_id': u'blabla',
                u'product_id_family': u'bla',
                u'title': u'some title',
                u'best_offer': True,
                u'product_type': u'subscription',
                u'features': [
                    {
                        u'description': u'Приоритетная поддержка',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                    {
                        u'description': u'Без рекламы',
                        u'group': u'mail_services',
                        u'value': u''
                    },
                ],
                u'trial': {
                    u'definition': {
                        u'period': u'day',
                        u'periods_count': 90,
                        u'price_per_user_in_month': 0
                    }
                },
                u'prices': [{
                    u'price_id': u'3c0922e6-e925-4930-aae0-d1315afc6ebc',
                    u'amount': 790,
                    u'currency': u'RUB',
                    u'period': u'year',
                }]
            }],
            'order_status': u'paid',
            'current_subscription': {
                u'service_id': u'uuid - индентификатор сервиса 1',
                u'readonly': False,
                u'creation_date': u'2019-12-01T00:00:000+0300',
                u'subscription_until': u'2021-01-01T12:00:00.000+0300',
                u'status': u'ACTIVE',
                u'order_status': u'paid',
                u'billing_status': u'paid',
                u'auto_prolong_enabled': True,
                u'product': {
                    u'product_id': u'что-то вроде mail_v1_pro_plus_disk',
                    u'product_id_family': u'что-то вроде mail_pro_plus_disk',
                    u'title': u'почта и диск про (или другое локализованное имя)',
                    u'best_offer': True,
                    u'product_type': u'subscription',
                    u'features': [
                        {u'description': u'Отключение рекламы (локализованное)'},
                        {u'description': u'Приоритетная поддержка (локализованное)'},
                        {u'description': u'100500 терабайт диска (локализованное)'}
                    ],
                    u'trial': {
                        u'definition': {
                            u'period': u'day',
                            u'periods_count': 90,
                            u'price_per_user_in_month': 0
                        }
                    },
                    u'price': {
                        u'price_id': u'uuid цены 3',
                        u'amount': 40000,
                        u'currency': u'USD',
                        u'period': u'year'
                    }
                }
            },
            'active_promo': {
                u'key': u'new_year',
                u'title': u'Новый год',
                u'active_until_date': u'2019-12-01T00:00:000+0300',
                u'payload': u'[{"region":"ru","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}},{"region":"default","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}}],'
            },
        }
        self.success_psbilling_api_resp._content = to_json(self.products_response)

    def test_default(self):
        data = self.default_test(
            query={'payload_type': 'disk', 'promo_activation': 'true'},
            expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&promo_activation=true&payload_type=disk'
        )
        assert data['current_subscription'] == self.products_response['current_subscription']
        assert data['active_promo'] == self.products_response['active_promo']
        assert data['order_status'] == self.products_response['order_status']

    def test_second_promo_param(self):
        data = self.default_test(
            query={'payload_type': 'disk', 'promoActivation': 'true'},
            expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&promo_activation=true&payload_type=disk'
        )
        assert data['current_subscription'] == self.products_response['current_subscription']
        assert data['active_promo'] == self.products_response['active_promo']
        assert data['order_status'] == self.products_response['order_status']

    def test_empty_active_promo(self):
        self.products_response.pop('active_promo')
        self.success_psbilling_api_resp._content = to_json(self.products_response)
        data = self.default_test(
            expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&payload_type=mobile&payload_version=0')
        assert 'active_promo' not in data

    @parameterized.expand([
        (None, None, '&payload_type=mobile&payload_version=0'),
        ('disk', None, '&payload_type=disk'),
        ('disk', '1', '&payload_type=disk&payload_version=1'),
        ('disk', '2', '&payload_type=disk&payload_version=2'),
        ('mobile', '0', '&payload_type=mobile&payload_version=0'),
        ('mobile', '1', '&payload_type=mobile&payload_version=1'),
    ])
    def test_payload(self, payload_type, payload_version, expected_params):
        query = {}
        if payload_type:
            query['payload_type'] = payload_type
        if payload_version:
            query['payload_version'] = payload_version

        self.success_psbilling_api_resp._content = to_json(self.products_response)
        data = self.default_test(
            expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859' + expected_params,
            query=query)
        assert 'active_promo' in data

        if payload_type is None:
            assert 'mobile_payload' in data['active_promo']
            assert 'payload' not in data['active_promo']
        else:
            assert 'mobile_payload' not in data['active_promo']
            assert 'payload' in data['active_promo']

    def test_old_payload(self):
        self.products_response['active_promo']['mobile_payload'] = self.products_response['active_promo']['payload']
        del self.products_response['active_promo']['payload']

        self.success_psbilling_api_resp._content = to_json(self.products_response)
        data = self.default_test(
            expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&payload_type=mobile&payload_version=0')
        assert 'active_promo' in data

        assert 'mobile_payload' in data['active_promo']
        assert 'payload' not in data['active_promo']

    @parameterized.expand([
        ('default_language', None, 'ru'),
        ('en', 'en', 'en'),
        ('ru', 'ru', 'ru'),
        ('complex', 'en-US,en;q=0.9,ru;q=0.8', 'en'),
    ])
    def test_headers(self, case_name, lang, expected_lang):
        self.headers_test(lang, expected_lang)

    @parameterized.expand([
        ('bad-request', PsBillingApiTestCase.ps_billing_api_400_resp, 400),
    ])
    def test_error_handling(self, case_name, resp_from_psbilling_api, expected_status):
        self.error_handling_test(resp_from_psbilling_api, expected_status)

    def test_package_name(self):
        self.default_test(query={'package_name': 'ru.yandex.mail'},
                          expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&packageName'
                                       '=ru.yandex.mail&payload_type=mobile&payload_version=0')

    def test_currency(self):
        self.default_test(query={'currency': 'USD'},
                          expected_url='/v3/productsets/product_set_key/products?lang=ru&__uid=128280859&payload_type'
                                       '=mobile&payload_version=0&currency=USD')

# psbilling/users/services
# noinspection PyUnusedLocal
class V1GetUserServicesTestCase(PsBillingApiTestCase):
    method = 'GET'
    api_version = 'v1'
    api_url = 'psbilling/users/services?product_owner=some_owner&status=on_hold'
    ps_billing_test_handler = '/v1/users/services'

    def setup_method(self, method):
        super(V1GetUserServicesTestCase, self).setup_method(method)

        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = self.request
        self.services_response = {'items': [
            {
                u'service_id': u'uuid - индентификатор сервиса 1',
                u'readonly': False,
                u'creation_date': u'2019-12-01T00:00:000+0300',
                u'subscription_until': u'2021-01-01T12:00:00.000+0300',
                u'status': u'ACTIVE',
                u'order_status': u'paid',
                u'billing_status': u'paid',
                u'auto_prolong_enabled': True,
                u'product': {
                    u'product_id': u'что-то вроде mail_v1_pro_plus_disk',
                    u'product_id_family': u'что-то вроде mail_pro_plus_disk',
                    u'title': u'почта и диск про (или другое локализованное имя)',
                    u'best_offer': True,
                    u'product_type': u'subscription',
                    u'features': [
                        {u'description': u'Отключение рекламы (локализованное)'},
                        {u'description': u'Приоритетная поддержка (локализованное)'},
                        {u'description': u'100500 терабайт диска (локализованное)'}
                    ],
                    u'trial': {
                        u'definition': {
                            u'period': u'day',
                            u'periods_count': 90,
                            u'price_per_user_in_month': 0
                        }
                    },
                    u'price': {
                        u'price_id': u'uuid цены 3',
                        u'amount': 40000,
                        u'currency': u'USD',
                        u'period': u'year'
                    }
                }
            }
        ]}
        self.success_psbilling_api_resp._content = to_json(self.services_response)

    @parameterized.expand([
        ('bad-request', PsBillingApiTestCase.ps_billing_api_400_resp, 400),
    ])
    def test_error_handling(self, case_name, resp_from_psbilling_api, expected_status):
        with self.specified_client(scopes=PsBillingInappPermission.scopes), \
            mock.patch('requests.Session.send',
                       return_value=resp_from_psbilling_api):
            resp = self.client.request(self.method, self.api_url, self.uid)

        assert resp.status_code == expected_status

    @parameterized.expand([
        ('default_language', None, 'ru'),
        ('en', 'en', 'en'),
        ('ru', 'ru', 'ru'),
        ('complex', 'en-US,en;q=0.9,ru;q=0.8', 'en'),
    ])
    def test_headers(self, case_name, lang, expected_lang):
        self.headers_test(lang, expected_lang)

    def test_default(self):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request(self.method, self.api_url, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        expected_url = '/v1/users/services?lang=ru&__uid=128280859&product_owner=some_owner&status=on_hold'
        assert_that(URLHelper(url).path, equal_to(URLHelper(expected_url).path))
        assert_that(url, has_same_url_params(expected_url))

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['items'] == self.services_response['items']


# psbilling/users/inapp
class PutInappReceiptTestCase(PsBillingApiTestCase):
    method = 'PUT'
    api_url = 'psbilling/users/inapp/receipt?' \
              'price_id=some_price&store_id=APPLE_APPSTORE&package_name=some_package&currency=RUB'

    def ok_response_test(self, response):
        self.success_psbilling_api_resp._content = to_json(response)
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request(self.method, self.api_url, data=self.body, uid=self.uid)

        data = from_json(resp.content)
        assert data == response
        assert resp.status_code == 200

        url = mocked_ps_billing.call_args[0][1]
        assert url == self.ps_billing_test_handler + '?lang=ru&store_id=APPLE_APPSTORE&package_name=some_package' \
                                                     '&__uid=128280859&price_id=some_price&currency=RUB'

    def error_handling_test(self, case_name, resp_from_psbilling_api, expected_status, expected_response=None):
        resp_from_psbilling_api._content = '{"error":{"name":"%s","message":"failed to process"}}' % case_name
        with self.specified_client(scopes=PsBillingInappPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=resp_from_psbilling_api):
            resp = self.client.request(self.method, self.api_url, data=self.body, uid=self.uid)

        assert resp.status_code == expected_status
        data = from_json(resp.content)
        assert data['description'] == expected_response['description']
        assert data['error'] == expected_response['error']


class V1PutInappReceiptTestCase(PutInappReceiptTestCase):
    api_version = 'v1'
    ps_billing_test_handler = '/v1/users/inapp/receipt'

    def test_ok(self):
        self.ok_response_test()

    @parameterized.expand([
        ('invalid-receipt', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Receipt is invalid.', 'error': u'PsBillingInappReceiptIsInvalidError'}),
        ('uid-mismatch', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Uid mismatch.', 'error': u'PsBillingInappUidMismatchError'})
    ])
    def test_error_handling(self, case_name, resp_from_ps_billing_api, expected_status, expected_response=None):
        self.error_handling_test(case_name, resp_from_ps_billing_api, expected_status, expected_response)

    def test_integration(self):
        resp = self.client.request(self.method, self.api_url, query=None, uid=self.uid, data=self.body)
        assert resp == {}
        assert resp.status_code == 400
        data = from_json(resp.content)
        assert data['description'] == u'Receipt is invalid.'
        assert data['error'] == u'PsBillingInappReceiptIsInvalidError'


class V2PutInappReceiptTestCase(PutInappReceiptTestCase):
    api_version = 'v2'
    ps_billing_test_handler = '/v2/users/inapp/receipt'

    def test_ok(self):
        self.ok_response_test()

    @parameterized.expand([
        ('invalid-receipt', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Receipt is invalid.', 'error': u'PsBillingInappReceiptIsInvalidError'}),
        ('multiuser_store', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Multiuser store.', 'error': u'PsBillingInappStoreAccountMismatchError'}),
        ('wrong_app', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Wrong application.', 'error': u'PsBillingInappApplicationMismatchError'}),
        ('multiuser_yandex', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Multiuser Yandex.', 'error': u'PsBillingInappYandexAccountMismatchError'}),
    ])
    def test_error_handling(self, case_name, resp_from_ps_billing_api, expected_status, expected_response=None):
        self.error_handling_test(case_name, resp_from_ps_billing_api, expected_status, expected_response)

    def test_integration(self):
        resp = self.client.request(self.method, self.api_url, query=None, uid=self.uid)
        assert resp.status_code == 200
        data = from_json(resp.content)
        assert 'items' in data


class V1PutInappReceiptTestCase(PutInappReceiptTestCase):
    api_version = 'v1'
    ps_billing_test_handler = '/v1/users/inapp/receipt'

    def test_ok(self):
        response = {'items': [
            {
                u'order_id': u'uuid - индентификатор заказа',
                u'user_service_id': u'id услуги'
            }
        ]}
        self.ok_response_test(response)

    @parameterized.expand([
        ('invalid-receipt', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Receipt is invalid.', 'error': u'PsBillingInappReceiptIsInvalidError'}),
        ('uid-mismatch', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Uid mismatch.', 'error': u'PsBillingInappUidMismatchError'})
    ])
    def test_error_handling(self, case_name, resp_from_ps_billing_api, expected_status, expected_response=None):
        self.error_handling_test(case_name, resp_from_ps_billing_api, expected_status, expected_response)

    def test_integration(self):
        resp = self.client.request(self.method, self.api_url, query=None, uid=self.uid, data=self.body)
        assert resp.status_code == 400
        data = from_json(resp.content)
        assert data['description'] == u'Receipt is invalid.'
        assert data['error'] == u'PsBillingInappReceiptIsInvalidError'


class V2PutInappReceiptTestCase(PutInappReceiptTestCase):
    api_version = 'v2'
    ps_billing_test_handler = '/v2/users/inapp/receipt'

    def test_ok(self):
        response = {
            'items': [
                {
                    u'order_id': u'uuid - индентификатор заказа',
                    u'user_service_id': u'id услуги',
                }
            ],
            'trial_used': True
        }
        self.ok_response_test(response)

    def test_ok_without_trial(self):
        response = {'items': [
            {
                u'order_id': u'uuid - индентификатор заказа',
                u'user_service_id': u'id услуги'
            }
        ]}
        self.ok_response_test(response)

    @parameterized.expand([
        ('invalid-receipt', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Receipt is invalid.', 'error': u'PsBillingInappReceiptIsInvalidError'}),
        ('multiuser_store', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Multiuser store.', 'error': u'PsBillingInappStoreAccountMismatchError'}),
        ('wrong_app', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Wrong application.', 'error': u'PsBillingInappApplicationMismatchError'}),
        ('multiuser_yandex', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Multiuser Yandex.', 'error': u'PsBillingInappYandexAccountMismatchError'}),
    ])
    def test_error_handling(self, case_name, resp_from_ps_billing_api, expected_status, expected_response=None):
        self.error_handling_test(case_name, resp_from_ps_billing_api, expected_status, expected_response)

    def test_integration(self):
        resp = self.client.request(self.method, self.api_url, query=None, uid=self.uid)
        assert resp.status_code == 200
        data = from_json(resp.content)
        assert 'items' in data


class V1GetFuturePromosTestCase(PsBillingApiTestCase):
    api_version = 'v1'

    def setup_method(self, method):
        super(V1GetFuturePromosTestCase, self).setup_method(method)
        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = self.request
        self.services_response = {
            'promos': [
                {
                    u'key': u'new_year_{}'.format(i),
                    u'title': u'Новый год',
                    u'active_until_date': u'2019-12-01T00:00:000+0300',
                    u'payload': u'[{"region":"ru","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}},{"region":"default","promo_page":{"background":"url","title":"Тестовый ключ","description":"Тестовый ключ"},"onboarding":{"first":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"},"second":{"title":"Тестовый ключ","description":"Тестовый ключ","button_text":"Тестовый ключ"}}}],'
                }
                for i in range(1, 10)
            ]
        }
        self.success_psbilling_api_resp._content = to_json(self.services_response)

    @parameterized.expand([
        ({}, ''),
        ({'payload_type': 'disk'}, '&payload_type=disk'),
        ({'payload_type': 'disk', 'payload_version': '5'}, '&payload_type=disk&payload_version=5'),
    ])
    def test_default(self, query, expected_query):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request('GET', 'psbilling/promo/future', query=query, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert url == '/v1/promo/future?lang=ru&__uid=128280859' + expected_query

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['promos'] == self.services_response['promos']


class V1PutInappPaymentInitializedTestCase(PsBillingApiTestCase):
    api_version = 'v1'

    def setup_method(self, method):
        super(V1PutInappPaymentInitializedTestCase, self).setup_method(method)
        self.success_psbilling_api_resp = construct_requests_resp()

    @parameterized.expand([
        ({}, '')
    ])
    def test_default(self, query, expected_query):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request('PUT', 'psbilling/users/inapp/payment_initialized', query=query, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert url == '/v1/users/inapp/payment_initialized?lang=ru&__uid=128280859' + expected_query

        assert resp.status_code == 200


# psbilling/users/subscribe
class V1PostSubscribeTestCase(PsBillingApiTestCase):
    api_version = 'v1'

    def setup_method(self, method):
        super(V1PostSubscribeTestCase, self).setup_method(method)
        self.subscribe_response = {
            'order_id': 'd6b86920-7975-420e-8ce1-5d051fdc4414',
            'payment_form_url': 'http://ya.ru/pay?purchase_token=q1w2e3'
        }
        self.success_psbilling_api_resp._content = to_json(self.subscribe_response)

    @parameterized.expand([
        ({'price_id': 'qwe_123'}, '&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'currency': 'RUB'}, '&currency=RUB&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'domain_suffix': 'ru'}, '&domain_suffix=ru&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'return_path': 'localhost'}, '&return_path=localhost&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'form_type': 'desktop'}, '&form_type=desktop&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'form_type': 'mobile'}, '&form_type=mobile&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'real_user_ip': '1.1.1.1'}, '&real_user_ip=1.1.1.1&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'disable_trust_header': 'true'}, '&disable_trust_header=true&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'login_id': 'my_user'}, '&login_id=my_user&price_id=qwe_123'),
        ({'price_id': 'qwe_123', 'use_template': 'true'}, '&use_template=true&price_id=qwe_123'),
    ])
    def test_default(self, query, expected_query):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request('POST', 'psbilling/users/subscribe', query=query, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert url == '/v1/users/subscribe?lang=ru&__uid=128280859' + expected_query

        data = from_json(resp.content)
        print(data)
        assert data['order_id'] == self.subscribe_response['order_id']
        assert data['payment_form_url'] == self.subscribe_response['payment_form_url']


# psbilling/orders/{id}
class V1GetOrdersTestCase(PsBillingApiTestCase):
    api_version = 'v1'

    def setup_method(self, method):
        super(V1GetOrdersTestCase, self).setup_method(method)
        self.subscribe_response = {
            'order_id': 'd6b86920-7975-420e-8ce1-5d051fdc4413',
            'status': 'paid'
        }
        self.success_psbilling_api_resp._content = to_json(self.subscribe_response)

    def test_default(self):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request('GET', 'psbilling/orders/orders_key', uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert url == '/v1/orders/orders_key?lang=ru&__uid=128280859'

        data = from_json(resp.content)
        print(data)
        assert data['order_id'] == self.subscribe_response['order_id']
        assert data['status'] == self.subscribe_response['status']


class V1PromoCodesTestCase(PsBillingApiTestCase):
    api_version = 'v1'

    def setup_method(self, method):
        super(V1PromoCodesTestCase, self).setup_method(method)
        self.success_psbilling_api_resp = Response()
        self.success_psbilling_api_resp.status_code = 200
        self.success_psbilling_api_resp.request = self.request
        self.services_response = {
            u'is_activated': True,
            u'promocode_type': u'doesn_t_really_matter'
        }
        self.success_psbilling_api_resp._content = to_json(self.services_response)

    @parameterized.expand([
        ('{"promo_code": "xxx"}', 'xxx'),
    ])
    def test_default(self, body, expected_promocode):
        with self.mock_response(self.success_psbilling_api_resp) as (client, mocked_ps_billing):
            resp = self.client.request('POST', 'psbilling/promocode/activate', data=body, uid=self.uid)

        url = mocked_ps_billing.call_args[0][1]
        assert url == '/v1/promocode/codes/' + expected_promocode + '/activate?lang=ru&__uid=' + self.uid

        assert resp.status_code == 200
        data = from_json(resp.content)
        assert data['is_activated'] == self.services_response['is_activated']
        assert data['promocode_type'] == self.services_response['promocode_type']

    def error_handling_test(self, case_name, resp_from_psbilling_api, expected_status, expected_response=None):
        resp_from_psbilling_api._content = '{"error":{"name":"%s","message":"failed to process"}}' % case_name
        with self.specified_client(scopes=PsBillingInappPermission.scopes), \
             mock.patch('requests.Session.send',
                        return_value=resp_from_psbilling_api):
            resp = self.client.request('POST', 'psbilling/promocode/activate', data='{"promo_code": "xxx"}', uid=self.uid)

        assert resp.status_code == expected_status
        data = from_json(resp.content)
        assert data['description'] == expected_response['description']
        assert data['error'] == expected_response['error']

    @parameterized.expand([
        ('promocode-already-activated', PsBillingApiTestCase.ps_billing_api_400_resp, 400,
         {'description': u'Promocode already activated', 'error': u'PsBillingPromocodeAlreadyActivated'})
    ])
    def test_error_handling(self, case_name, resp_from_ps_billing_api, expected_status, expected_response=None):
        self.error_handling_test(case_name, resp_from_ps_billing_api, expected_status, expected_response)
