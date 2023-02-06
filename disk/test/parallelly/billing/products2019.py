# -*- coding: utf-8 -*-
import mock
from nose_parameterized import parameterized
from requests.exceptions import ConnectionError

from mpfs.common.util.experiments.logic import experiment_manager
from mpfs.core.billing import Market
from mpfs.core.billing import ProductCard
from test.helpers.mediabilling_responses_data import NativeProducts
from test.helpers.size_units import GB, TB
from test.helpers.stubs.services import UAASStub, PassportStub, MediaBillingStub
from test.parallelly.billing.base import BaseBillingTestCase

from mpfs.common.static.tags.billing import *
from mpfs.config import settings


class Products2019TestCase(BaseBillingTestCase):
    def assert_verstka_products_contains_only(self, resp, expected_products):
        actual_products = []
        for item in resp['items']:
            for period in ('month', 'year'):
                actual_products.append(item['periods'][period]['product_id'])
        assert set(actual_products) == set(expected_products)

    def filter_product_by_pid(self, pid, products):
        for item in products:
            for period in ('month', 'year'):
                if item['periods'][period]['product_id'] == pid:
                    return item['periods'][period]
        else:
            return None

    def test_products_order(self):
        products = ProductCard.get_current_products_for(self.uid)
        assert products['items'][0]['space'] == 100*GB
        assert products['items'][1]['space'] == 1*TB
        assert products['items'][2]['space'] == 3*TB

    def test_new_line_by_default(self):
        self.json_ok('user_init', {'uid': self.uid})
        self.bind_user_to_market(market='RU', uid=self.uid)
        with MediaBillingStub(content=NativeProducts.DEFAULT):
            resp = self.billing_ok('verstka_products', {'uid': self.uid, 'locale': 'ru'})

        self.assert_verstka_products_contains_only(resp, ['100gb_1m_2019_v4',
                                                          '100gb_1y_2019_v4',
                                                          '1tb_1m_2019_v4',
                                                          '1tb_1y_2019_v4',
                                                          'ru.yandex.web.disk.native.1month.autorenewable.notrial.disk_basic.750',
                                                          'ru.yandex.web.disk.native.1year.autorenewable.notrial.disk_basic.6500'
                                                          ])

    def test_new_line_with_discount(self):
        u"""Проверяем, что пользователю со скидкой вернутся новые продукты скидочные и нескидочный плюсовый тариф"""
        expected_products = ['100gb_1m_2019_v4_discount_30',
                             '100gb_1y_2019_v4_discount_30',
                             '1tb_1m_2019_v4_discount_30',
                             '1tb_1y_2019_v4_discount_30',
                             'ru.yandex.web.disk.native.1month.autorenewable.notrial.disk_basic.750',
                             'ru.yandex.web.disk.native.1year.autorenewable.notrial.disk_basic.6500'
                             ]
        self.json_ok('user_init', {'uid': self.uid})
        self.bind_user_to_market(market='RU', uid=self.uid)
        self.give_discount(self.uid, line=PRIMARY_2018_DISCOUNT_30)
        with MediaBillingStub(content=NativeProducts.DEFAULT):
            resp = self.billing_ok('verstka_products', {'uid': self.uid, 'locale': 'ru'})

        self.assert_verstka_products_contains_only(resp, expected_products)

    @parameterized.expand([
        ('100gb_1m_2019', 99, PRIMARY_2019, 'RU', 'ru'),
        ('100gb_1y_2019', 990, PRIMARY_2019, 'RU', 'ru'),
        ('1tb_1m_2019', 350, PRIMARY_2019, 'RU', 'ru'),
        ('1tb_1y_2019', 3500, PRIMARY_2019, 'RU', 'ru'),
        ('100gb_1m_2019', 2, PRIMARY_2019, 'COM', 'en'),
        ('100gb_1y_2019', 20, PRIMARY_2019, 'COM', 'en'),
        ('1tb_1m_2019', 10, PRIMARY_2019, 'COM', 'en'),
        ('1tb_1y_2019', 100, PRIMARY_2019, 'COM', 'en'),
        ('100gb_1m_2019_discount_10', 89, PRIMARY_2019_DISCOUNT_10, 'RU', 'ru'),
        ('100gb_1y_2019_discount_10', 890, PRIMARY_2019_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1m_2019_discount_10', 315, PRIMARY_2019_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1y_2019_discount_10', 3150, PRIMARY_2019_DISCOUNT_10, 'RU', 'ru'),
        ('100gb_1m_2019_discount_10', 1.8, PRIMARY_2019_DISCOUNT_10, 'COM', 'en'),
        ('100gb_1y_2019_discount_10', 18, PRIMARY_2019_DISCOUNT_10, 'COM', 'en'),
        ('1tb_1m_2019_discount_10', 9, PRIMARY_2019_DISCOUNT_10, 'COM', 'en'),
        ('1tb_1y_2019_discount_10', 90, PRIMARY_2019_DISCOUNT_10, 'COM', 'en'),
        ('100gb_1m_2019_discount_20', 79, PRIMARY_2019_DISCOUNT_20, 'RU', 'ru'),
        ('100gb_1y_2019_discount_20', 790, PRIMARY_2019_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1m_2019_discount_20', 280, PRIMARY_2019_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1y_2019_discount_20', 2800, PRIMARY_2019_DISCOUNT_20, 'RU', 'ru'),
        ('100gb_1m_2019_discount_20', 1.6, PRIMARY_2019_DISCOUNT_20, 'COM', 'en'),
        ('100gb_1y_2019_discount_20', 16, PRIMARY_2019_DISCOUNT_20, 'COM', 'en'),
        ('1tb_1m_2019_discount_20', 8, PRIMARY_2019_DISCOUNT_20, 'COM', 'en'),
        ('1tb_1y_2019_discount_20', 80, PRIMARY_2019_DISCOUNT_20, 'COM', 'en'),
        ('100gb_1m_2019_discount_30', 69, PRIMARY_2019_DISCOUNT_30, 'RU', 'ru'),
        ('100gb_1y_2019_discount_30', 690, PRIMARY_2019_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1m_2019_discount_30', 245, PRIMARY_2019_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1y_2019_discount_30', 2450, PRIMARY_2019_DISCOUNT_30, 'RU', 'ru'),
        ('100gb_1m_2019_discount_30', 1.4, PRIMARY_2019_DISCOUNT_30, 'COM', 'en'),
        ('100gb_1y_2019_discount_30', 14, PRIMARY_2019_DISCOUNT_30, 'COM', 'en'),
        ('1tb_1m_2019_discount_30', 7, PRIMARY_2019_DISCOUNT_30, 'COM', 'en'),
        ('1tb_1y_2019_discount_30', 70, PRIMARY_2019_DISCOUNT_30, 'COM', 'en'),
        ('1tb_1m_2019_v2', 300, PRIMARY_2019_V2, 'RU', 'ru'),
        ('1tb_1y_2019_v2', 3000, PRIMARY_2019_V2, 'RU', 'ru'),
        ('1tb_1m_2019_v3', 250, PRIMARY_2019_V3, 'RU', 'ru'),
        ('1tb_1y_2019_v3', 2500, PRIMARY_2019_V3, 'RU', 'ru'),
        ('1tb_1m_2019_v4', 300, PRIMARY_2019_V4, 'RU', 'ru'),
        ('1tb_1y_2019_v4', 2500, PRIMARY_2019_V4, 'RU', 'ru'),
        ('1tb_1m_2019_v2_discount_10', 270, PRIMARY_2019_V2_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1y_2019_v2_discount_10', 2700, PRIMARY_2019_V2_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1m_2019_v3_discount_10', 225, PRIMARY_2019_V3_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1y_2019_v3_discount_10', 2250, PRIMARY_2019_V3_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1m_2019_v4_discount_10', 270, PRIMARY_2019_V4_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1y_2019_v4_discount_10', 2250, PRIMARY_2019_V4_DISCOUNT_10, 'RU', 'ru'),
        ('1tb_1m_2019_v2_discount_20', 240, PRIMARY_2019_V2_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1y_2019_v2_discount_20', 2400, PRIMARY_2019_V2_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1m_2019_v3_discount_20', 200, PRIMARY_2019_V3_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1y_2019_v3_discount_20', 2000, PRIMARY_2019_V3_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1m_2019_v4_discount_20', 240, PRIMARY_2019_V4_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1y_2019_v4_discount_20', 2000, PRIMARY_2019_V4_DISCOUNT_20, 'RU', 'ru'),
        ('1tb_1m_2019_v2_discount_30', 210, PRIMARY_2019_V2_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1y_2019_v2_discount_30', 2100, PRIMARY_2019_V2_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1m_2019_v3_discount_30', 175, PRIMARY_2019_V3_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1y_2019_v3_discount_30', 1750, PRIMARY_2019_V3_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1m_2019_v4_discount_30', 210, PRIMARY_2019_V4_DISCOUNT_30, 'RU', 'ru'),
        ('1tb_1y_2019_v4_discount_30', 1750, PRIMARY_2019_V4_DISCOUNT_30, 'RU', 'ru'),
    ])
    def test_prices(self, pid, expected_price, line, market_name, locale):
        market = Market(market_name)
        products = ProductCard.get_products(line, market, locale)
        product = self.filter_product_by_pid(pid, products)
        assert product is not None, u'Продукт должен быть в выдаче'
        assert product['price'] == expected_price

    @classmethod
    def teardown_class(cls):
        PassportStub.reset_users_info()
        experiment_manager.load_experiments_from_conf()
        super(Products2019TestCase, cls).teardown_class()
