# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from datetime import datetime, timedelta

import hamcrest
import mock
import pytest
import requests

import travel.rasp.train_api.train_purchase.tasks.unhold_invalid_payments
from common.data_api.billing.trust_client import TrustClient, TrustPaymentStatuses
from common.tester.utils.datetime import replace_now
from common.utils.date import MSK_TZ
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo, TicketInfo
from travel.rasp.train_api.train_purchase.core.enums import OperationStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.tasks.unhold_invalid_payments import (
    BILLING_CHECK_DELAY, ORDER_FILTER_INTERVAL, unhold_payments
)

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]

SAMPLE_DT = datetime(2000, 1, 1)


@pytest.yield_fixture(autouse=True)
def m_get_order_info():
    with mock.patch.object(travel.rasp.train_api.train_purchase.tasks.unhold_invalid_payments, 'get_order_info',
                           autospec=True) as m_get_order_info:
        m_get_order_info.return_value = OrderInfoResult(
            buy_operation_id=None, expire_set_er=None, status=OperationStatus.FAILED, order_num=None,
            passengers=[]
        )
        yield m_get_order_info


@pytest.yield_fixture
def dummy_trust_url():
    url = 'http://trust.example.org/'
    with mock.patch.object(TrustClient, '_build_trust_url', return_value=url):
        yield url


def timeout_exception_callback(*args, **kwargs):
    raise requests.Timeout('Connection timed out for example.')


@replace_now(SAMPLE_DT)
def test_unhold_invalid_payments_orders_filter(httpretty, dummy_trust_url, m_get_order_info):
    end_dt = MSK_TZ.localize(SAMPLE_DT - BILLING_CHECK_DELAY)

    # заказ, вызывающий исключение
    bad_unhold = TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1),
                                   payments=[dict(purchase_token='i_will_raise_but_its_ok')],
                                   partner_data={}, invalid_payments_unholded=False)
    # подходящий заказ
    good_unhold = TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1),
                                    payments=[dict(purchase_token='check_me')],
                                    partner_data={})
    # слишком старый заказ
    no_unhold = TrainOrderFactory(reserved_to=end_dt - ORDER_FILTER_INTERVAL - timedelta(seconds=1),
                                  payments=[dict(purchase_token='do_not_check_me')],
                                  partner_data={})
    # слишком новый заказ
    TrainOrderFactory(reserved_to=end_dt + timedelta(seconds=1),
                      payments=[dict(purchase_token='do_not_check_me')],
                      partner_data={})
    # заказ с order_num
    TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1),
                      payments=[dict(purchase_token='do_not_check_me')],
                      partner_data={'order_num': '100500'})
    # заказ без purchase_token
    TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1))
    # уже анхолженый заказ
    TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1),
                      payments=[dict(purchase_token='do_not_check_me')],
                      partner_data={}, invalid_payments_unholded=True)

    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/i_will_raise_but_its_ok',
                           body=timeout_exception_callback)
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/check_me',
                           body=json.dumps({'status': 'success', 'payment_status': 'canceled'}))

    unhold_payments()

    assert bad_unhold.reload().invalid_payments_unholded is False
    assert good_unhold.reload().invalid_payments_unholded
    assert no_unhold.reload().invalid_payments_unholded is None

    hamcrest.assert_that(httpretty.latest_requests, hamcrest.contains(
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/i_will_raise_but_its_ok')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/check_me')),
    ))
    checked_oredes = [call[0][0] for call in m_get_order_info.call_args_list]
    hamcrest.assert_that(checked_oredes, hamcrest.contains_inanyorder(good_unhold, bad_unhold))


@replace_now(SAMPLE_DT)
def test_unhold_invalid_payments_status_check(httpretty, dummy_trust_url, m_get_order_info):
    end_dt = MSK_TZ.localize(SAMPLE_DT - BILLING_CHECK_DELAY)

    order1 = TrainOrderFactory(
        reserved_to=end_dt - timedelta(seconds=1),
        payments=[
            dict(
                purchase_token='canceled_token',
                trust_created_at=SAMPLE_DT - timedelta(minutes=1)
            ),
            dict(
                purchase_token='canceled_token_in_history',
                trust_created_at=SAMPLE_DT - timedelta(minutes=2)
            )],
        partner_data={'order_num': 'order_num_is_not_null'})
    order2 = TrainOrderFactory(
        reserved_to=end_dt - timedelta(seconds=1),
        payments=[
            dict(
                purchase_token='not_authorized_token',
                trust_created_at=SAMPLE_DT - timedelta(minutes=1)
            ),
            dict(
                purchase_token='not_authorized_token_in_history',
                trust_created_at=SAMPLE_DT - timedelta(minutes=2)
            )],
        partner_data={})
    order3 = TrainOrderFactory(
        reserved_to=end_dt - timedelta(seconds=1),
        payments=[
            dict(
                purchase_token='unhold_me',
                trust_created_at=SAMPLE_DT - timedelta(minutes=1)
            ),
            dict(
                purchase_token='canceled_token_in_history',
                trust_created_at=SAMPLE_DT - timedelta(minutes=2)
            ),
            dict(
                purchase_token='not_authorized_token_in_history',
                trust_created_at=SAMPLE_DT - timedelta(minutes=3)
            )],
        partner_data={})

    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/canceled_token',
                           body=json.dumps({'status': 'success', 'payment_status': 'canceled'}))
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/canceled_token_in_history',
                           body=json.dumps({'status': 'success', 'payment_status': 'canceled'}))
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/not_authorized_token',
                           body=json.dumps({'status': 'success', 'payment_status': 'not_authorized'}))
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/not_authorized_token_in_history',
                           body=json.dumps({'status': 'success', 'payment_status': 'not_authorized'}))
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/unhold_me',
                           body=json.dumps({'status': 'success', 'payment_status': 'authorized'}))
    httpretty.register_uri(httpretty.POST, dummy_trust_url + 'payments/unhold_me/unhold',
                           body=json.dumps({'status': 'success', 'status_code': 'payment_is_updated'}))

    unhold_payments()

    hamcrest.assert_that(httpretty.latest_requests, hamcrest.contains_inanyorder(
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/canceled_token_in_history')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/canceled_token_in_history')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/not_authorized_token')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/not_authorized_token_in_history')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/not_authorized_token_in_history')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/unhold_me')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/unhold_me/unhold')),
    ))
    assert order1.reload().invalid_payments_unholded
    assert order2.reload().invalid_payments_unholded
    assert order2.payments[0].status == TrustPaymentStatuses.NOT_AUTHORIZED.value
    assert order2.payments[1].status == TrustPaymentStatuses.NOT_AUTHORIZED.value
    assert order3.reload().invalid_payments_unholded
    assert order3.payments[0].status == TrustPaymentStatuses.CANCELED.value
    assert order3.payments[1].status == TrustPaymentStatuses.CANCELED.value
    assert order3.payments[2].status == TrustPaymentStatuses.NOT_AUTHORIZED.value
    checked_orders = [call[0][0] for call in m_get_order_info.call_args_list]
    hamcrest.assert_that(checked_orders, hamcrest.contains_inanyorder(order2, order3))


@replace_now(SAMPLE_DT)
def test_skip_last_pay_success_order(httpretty, dummy_trust_url, m_get_order_info):
    end_dt = MSK_TZ.localize(SAMPLE_DT - BILLING_CHECK_DELAY)

    order = TrainOrderFactory(reserved_to=end_dt - timedelta(seconds=1),
                              payments=[dict(
                                  purchase_token='success_paid',
                                  trust_created_at=SAMPLE_DT - timedelta(minutes=1)
                              ), dict(
                                  purchase_token='unhold_me',
                                  trust_created_at=SAMPLE_DT - timedelta(minutes=2)
                              )],
                              partner_data={})
    m_get_order_info.return_value = OrderInfoResult(
        buy_operation_id=None, expire_set_er=None, status=OperationStatus.OK, order_num='123',
        passengers=[PassengerInfo(doc_id=123456, blank_id=None)],
        tickets=[TicketInfo('12121212', RzhdStatus.REMOTE_CHECK_IN, 3000, False)]
    )
    httpretty.register_uri(httpretty.GET, dummy_trust_url + 'payments/unhold_me',
                           body=json.dumps({'status': 'success', 'payment_status': 'authorized'}))
    httpretty.register_uri(httpretty.POST, dummy_trust_url + 'payments/unhold_me/unhold',
                           body=json.dumps({'status': 'success'}))
    unhold_payments()

    assert order.reload().invalid_payments_unholded
    hamcrest.assert_that(httpretty.latest_requests, hamcrest.contains_inanyorder(
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/unhold_me')),
        hamcrest.has_properties(path=hamcrest.starts_with('/payments/unhold_me/unhold')),
    ))
