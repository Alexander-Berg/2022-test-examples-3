# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from decimal import Decimal

import mock
import pytest
from django.test import Client
from django.test import override_settings
from hamcrest import assert_that, has_entries, contains, all_of, contains_string

import travel.rasp.train_api.train_purchase.utils.order
from common.tester.matchers import has_json
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.base.get_order_info import OrderInfoResult, PassengerInfo
from travel.rasp.train_api.train_partners.base.test_utils import create_blank
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


@pytest.yield_fixture(autouse=True)
def override_url_conf():
    with override_settings(ROOT_URLCONF='travel.rasp.train_api.urls_async'):
        yield


@pytest.yield_fixture(autouse=True)
def m_get_order_info():
    with mock.patch.object(travel.rasp.train_api.train_purchase.utils.order, 'get_order_info',
                           autospec=True) as m_get_order_info:
        m_get_order_info.return_value = OrderInfoResult(
            buy_operation_id=None, expire_set_er=None, status=None, order_num=None,
            passengers=[PassengerInfo(doc_id=123456, blank_id=None)]
        )
        yield m_get_order_info


@pytest.yield_fixture
def m_get_refund_amount():
    with mock.patch.object(travel.rasp.train_api.train_purchase.utils.order, 'get_refund_amount',
                           autospec=True) as m_get_refund_amount:
        yield m_get_refund_amount


@pytest.mark.parametrize('blank_ids', (['1', '2'], ['1', '2', '1']))
def test_calculate_refund_amount(m_get_refund_amount, blank_ids):
    order = create_order()
    order.passengers[0].tickets[0].rzhd_status = RzhdStatus.REMOTE_CHECK_IN
    order.passengers[1].tickets[0].rzhd_status = RzhdStatus.NO_REMOTE_CHECK_IN
    order.save()
    ClientContractsFactory(partner=order.partner)

    m_get_refund_amount.return_value = [
        create_blank('1', Decimal('1000.12')),
        create_blank('2', Decimal('1200.23'))
    ]

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': blank_ids}), content_type='application/json')

    assert m_get_refund_amount.call_count == 1
    assert response.status_code == 200
    assert_that(json.loads(response.content)['order']['passengers'], contains(
        has_entries(tickets=contains(has_entries({
            'rzhdStatus': 'REMOTE_CHECK_IN',
            'refund': has_entries(amount=1000.12),
            'isRefundable': True,
        }))),
        has_entries(tickets=contains(has_entries({
            'rzhdStatus': 'NO_REMOTE_CHECK_IN',
            'refund': has_entries(amount=1200.23),
            'isRefundable': True,
        }))),
    ))


def test_calculate_refund_amount_2_from_3(m_get_refund_amount):
    order = create_order(number_of_passengers=3)
    ClientContractsFactory(partner=order.partner)

    m_get_refund_amount.return_value = [
        create_blank('1', Decimal('1000.12')),
        create_blank('2', Decimal('1200.23'))
    ]

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': ['1', '2']}), content_type='application/json')

    assert response.status_code == 200
    assert m_get_refund_amount.call_count == 2


def test_calculate_refund_amount_1_already_refunded(m_get_refund_amount):
    order = create_order(number_of_passengers=3)
    order.passengers[2].tickets[0].rzhd_status = RzhdStatus.REFUNDED
    order.save()
    ClientContractsFactory(partner=order.partner)

    m_get_refund_amount.return_value = [
        create_blank('1', Decimal('1000.12')),
        create_blank('2', Decimal('1200.23'))
    ]

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': ['1', '2']}), content_type='application/json')

    assert response.status_code == 200
    assert m_get_refund_amount.call_count == 2


def test_calculate_refund_amount_not_refundable(m_get_refund_amount):
    order = create_order(number_of_passengers=1)
    order.passengers[0].tickets[0].rzhd_status = RzhdStatus.REFUNDED
    order.passengers[0].tickets[0].blank_id = '100500'
    order.save()
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': ['100500']}), content_type='application/json')

    assert response.status_code == 400
    assert_that(response.content, has_json(has_entries(errors=all_of(
        contains_string('Non refundable blank ids'),
        contains_string("'100500'")
    ))))
    assert m_get_refund_amount.call_count == 0


def test_calculate_refund_amount_no_blanks(m_get_refund_amount):
    order = create_order(number_of_passengers=1)
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': []}), content_type='application/json')

    assert response.status_code == 400
    assert_that(response.content, has_json(
        has_entries(errors=has_entries(blankIds=contains('Must be minimum one blank.')))
    ))
    assert m_get_refund_amount.call_count == 0


@replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', '')
def test_calculate_refund_amount_partner_is_not_active(m_get_refund_amount):
    order = create_order(number_of_passengers=1)

    response = Client().post('/ru/api/calculate-refund-amount/{}/'.format(order.uid),
                             json.dumps({'blankIds': []}), content_type='application/json')

    assert response.status_code == 400
    assert_that(response.content, has_json(
        has_entries(errors='Partner is not active')
    ))
    assert m_get_refund_amount.call_count == 0
