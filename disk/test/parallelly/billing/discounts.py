# -*- coding: utf-8 -*-
import datetime
import time

from hamcrest import calling
from hamcrest.core import assert_that
from hamcrest.core.core import raises
from mock import mock
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.common.static.tags.billing import (
    PRIMARY_2019_V4,
    PRIMARY_2019_V4_DISCOUNT_10,
    PRIMARY_2019_V4_DISCOUNT_20,
    PRIMARY_2019_V4_DISCOUNT_30,
)
from mpfs.core.billing.product.catalog import (
    primary_p_2019_v4,
    primary_p_2019_v4_discount_10,
    primary_p_2019_v4_discount_20,
    primary_p_2019_v4_discount_30,
    yandex_plus_upsale_2019)
from mpfs.core.promo_codes.logic.errors import AttemptToActivateDiscountRepeatedly
from mpfs.core.promo_codes.logic.discount_manager import DiscountManager
from mpfs.core.promo_codes.logic.discount import DiscountArchive
from test.base import time_machine
from test.helpers.mediabilling_responses_data import NativeProducts
from test.helpers.stubs.services import BigBillingStub, MediaBillingStub
from test.parallelly.billing.base import BaseBillingTestCase


class DiscountsTestCase(BaseBillingTestCase):
    def setup_method(self, method):
        super(DiscountsTestCase, self).setup_method(method)
        self.bind_user_to_market(market='RU')

    @parameterized.expand([
        (PRIMARY_2019_V4, primary_p_2019_v4, None, None),
        (PRIMARY_2019_V4_DISCOUNT_10, primary_p_2019_v4_discount_10, 10, None),
        (PRIMARY_2019_V4_DISCOUNT_20, primary_p_2019_v4_discount_20, 20, None),
        (PRIMARY_2019_V4_DISCOUNT_30, primary_p_2019_v4_discount_30, 30, None),
        (PRIMARY_2019_V4_DISCOUNT_10, primary_p_2019_v4_discount_10, 10, 1000),
        (PRIMARY_2019_V4_DISCOUNT_20, primary_p_2019_v4_discount_20, 20, 1000),
        (PRIMARY_2019_V4_DISCOUNT_30, primary_p_2019_v4_discount_30, 30, 1000),
    ])
    def test_list_discounted_products(self, line, correct_products, expected_percentage, delta_ts):
        cur_ts = int(time.time())
        with time_machine(datetime.datetime.fromtimestamp(cur_ts)), \
             MediaBillingStub(content=NativeProducts.DEFAULT):
            self.give_discount(self.uid, line, delta_ts)
            resp = self.billing_ok('verstka_products', {'uid': self.uid, 'locale': 'ru'})

        actual_ids = {period['product_id']
                      for item in resp['items']
                      for period in item['periods'].values()}

        # не все продукты доступны для покупки
        without_card_ids = {'ru.yandex.web.disk.native.1month.autorenewable.notrial.disk_100gb.69'}
        ids_from_catalog = {x['id']
                            for x in correct_products + yandex_plus_upsale_2019
                            if x['id'] not in without_card_ids}
        assert ids_from_catalog == actual_ids

        if line == PRIMARY_2019_V4:
            return

        discount_info = resp['items'][0]['discount']
        assert discount_info['percentage'] == expected_percentage
        if delta_ts is None:
            return

        assert cur_ts + delta_ts == discount_info['active_until_ts']

    @parameterized.expand([
        (PRIMARY_2019_V4, '100gb_1m_2019_v4'),
        (PRIMARY_2019_V4_DISCOUNT_10, '100gb_1m_2019_v4_discount_10'),
    ])
    def test_can_buy_product_of_current_offer(self, line, pid):
        self.give_discount(self.uid, line)
        args = {
            'uid': self.uid,
            'pid': pid,
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
        }
        with BigBillingStub():
            self.billing_ok('buy', args)

    @parameterized.expand([
        (PRIMARY_2019_V4, '100gb_1m_2019_V4_discount_10'),
        (PRIMARY_2019_V4_DISCOUNT_10, '100gb_1m_2019_V4'),
    ])
    def test_cannot_buy_product_of_different_offer(self, line, pid):
        self.give_discount(self.uid, line)
        args = {
            'uid': self.uid,
            'pid': pid,
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
        }
        with BigBillingStub():
            self.billing_error('buy', args, code=codes.BILLING_CANNOT_BUY_PRODUCT_FROM_NOT_CHEAPEST_LINE_AVAILABLE)

    def test_provide_several_discounts(self):
        from mpfs.core.metastorage.control import disk_info
        self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10)
        assert len(disk_info.find_one_by_field(self.uid, {'key': '/active_discounts'})['data']) == 1
        self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_20)
        assert len(disk_info.find_one_by_field(self.uid, {'key': '/active_discounts'})['data']) == 2

    def test_list_available_discounts_archives_outdated_ones(self):
        cur_datetime = datetime.datetime.now()
        with time_machine(cur_datetime):
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10)
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_20, period=1000)
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_30, period=1)
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 3
        with time_machine(cur_datetime + datetime.timedelta(seconds=10)):
            assert DiscountManager.get_cheapest_available_line(self.uid) == PRIMARY_2019_V4_DISCOUNT_20
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 2

    def test_archive_discount_by_end_datetime(self):
        cur_datetime = datetime.datetime.now()
        with time_machine(cur_datetime):
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_20,
                               end_datetime=cur_datetime + datetime.timedelta(seconds=1))
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 1
        with time_machine(cur_datetime + datetime.timedelta(seconds=10)):
            assert DiscountManager.get_cheapest_available_line(self.uid) != PRIMARY_2019_V4_DISCOUNT_20
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 0

    def test_providing_new_discount_archives_outdated_ones(self):
        cur_datetime = datetime.datetime.now()
        with time_machine(cur_datetime):
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10)
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_30, period=1)
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 2
        with time_machine(cur_datetime + datetime.timedelta(seconds=10)):
            self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_20, period=1000)
            assert len(DiscountManager._get_all_user_discounts(self.uid)) == 2
            assert DiscountManager.get_cheapest_available_line(self.uid) == PRIMARY_2019_V4_DISCOUNT_20

    def test_cannot_activate_already_active_for_user_discount(self):
        d_id = self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10)
        assert_that(calling(self.give_discount).with_args(self.uid, PRIMARY_2019_V4_DISCOUNT_10, discount_template_id=d_id), raises(AttemptToActivateDiscountRepeatedly))

    def test_cannot_activate_already_archived_for_user_discount(self):
        d_id = self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10, period=1)
        with time_machine(datetime.datetime.now() + datetime.timedelta(seconds=10)):
            # листинг скидок перенесет скидку в архив
            assert DiscountManager.get_cheapest_available_line(self.uid) == PRIMARY_2019_V4
        assert_that(calling(self.give_discount).with_args(self.uid, PRIMARY_2019_V4_DISCOUNT_10, discount_template_id=d_id), raises(AttemptToActivateDiscountRepeatedly))

    def test_buying_discount_product_stores_discount_template_id_in_order(self):
        d_id = self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10, period=1)
        args = {
            'uid': self.uid,
            'pid': '100gb_1m_2019_v4_discount_10',
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
        }
        with BigBillingStub():
            self.billing_ok('buy', args)
            order = self.billing_ok('order_list_current', {'uid': self.uid, 'ip': 'ip'})[0]
        assert 'discount_template_id' in order
        assert order['discount_template_id'] == d_id

        with BigBillingStub(), mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('order_process_callback',
                            {'uid': self.uid, 'number': order['number'], 'status': 'success', 'status_code': ''})
            order = self.billing_ok('order_list_history', {'uid': self.uid, 'ip': 'ip'})[0]
        assert 'discount_template_id' in order
        assert order['discount_template_id'] == d_id

    def test_buying_service_by_reusable_discount_does_not_use_user_discount(self):
        d_id = self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10, disposable=False)
        args = {
            'uid': self.uid,
            'pid': '100gb_1m_2019_v4_discount_10',
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
        }
        with BigBillingStub(), mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            order_number = self.billing_ok('buy', args)['order_number']
            self.billing_ok('order_process_callback',
                            {'uid': self.uid, 'number': order_number, 'status': 'success', 'status_code': ''})
        assert DiscountManager.find_discount(self.uid, d_id)
        assert not DiscountArchive.exists(self.uid, d_id)

    def test_buying_service_by_disposable_discount_use_user_discount(self):
        d_id = self.give_discount(self.uid, PRIMARY_2019_V4_DISCOUNT_10)
        args = {
            'uid': self.uid,
            'pid': '100gb_1m_2019_v4_discount_10',
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
        }
        with BigBillingStub(), mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            order_number = self.billing_ok('buy', args)['order_number']
            self.billing_ok('order_process_callback',
                            {'uid': self.uid, 'number': order_number, 'status': 'success', 'status_code': ''})
        assert not DiscountManager.find_discount(self.uid, d_id)
        assert DiscountArchive.exists(self.uid, d_id)
