# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from copy import deepcopy
from datetime import date, timedelta, datetime
from decimal import Decimal
from itertools import repeat

import httpretty as httpretty_module
import mock
import pytest
from django.conf import settings
from django.utils.encoding import force_bytes
from hamcrest import assert_that, has_entries, contains_inanyorder
from six.moves import zip as izip

from common.dynamic_settings.default import conf
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.workflow import run_process
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.base import IM_DATETIME_FORMAT
from travel.rasp.train_api.train_partners.im.factories.order_info import make_im_customer, make_im_order_item, ImOrderInfoFactory
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im, mock_im_500
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_partners.im.update_order import IM_UPDATE_BLANKS_METHOD
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PartnerDataFactory, TrainRefundFactory, PassengerFactory, TicketFactory, InsuranceFactory,
    TicketPaymentFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundStatus
from travel.rasp.train_api.train_purchase.tasks import check_office_refunds
from travel.rasp.train_api.train_purchase.tasks.check_office_refunds import (
    ORDERS_ENDPOINT, check_im_office_refunds, TooEarlyError, check_for_external_refunds
)
from travel.rasp.train_api.train_purchase.workflow.ticket_refund import TICKET_REFUND_PROCESS

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]

_dt = datetime(2018, 1, 20)


def generate_date_time():
    global _dt
    _dt += timedelta(minutes=1)
    return _dt


def make_order_info(order, *refund_order_items):
    customers = [make_im_customer(p) for p in order.passengers]
    order_info = {
        'OrderItems': [make_im_order_item(order, {'create_dt': generate_date_time()})],
        'OrderId': order.current_partner_data.im_order_id,
        'OrderCustomers': customers,
        'ContactPhone': '',
    }

    for roi in refund_order_items:
        roi.update({
            '$type': 'ApiContracts.Order.V1.Info.OrderItem.Railway.RailwayFullOrderItemInfo, ApiContracts',
            'OperationType': 'Return',
            'OrderItemCustomers': deepcopy(order_info['OrderItems'][0]['OrderItemCustomers']),
            'CreateDateTime': datetime.strftime(generate_date_time(), IM_DATETIME_FORMAT),
        })
        for customer in roi['OrderItemCustomers']:
            for refund_blank in roi['OrderItemBlanks']:
                if customer['OrderItemBlankId'] == refund_blank['PreviousOrderItemBlankId']:
                    customer['OrderItemBlankId'] = refund_blank['OrderItemBlankId']

        order_info['OrderItems'].append(roi)

    return order_info


def make_refund_order_item(operation_id, blank_ids, is_external=False, operation_status='Succeeded', amounts=None):
    amounts = amounts or repeat(1000.1)
    return {
        'SimpleOperationStatus': operation_status,
        'IsExternallyLoaded': is_external,
        'OperationType': 'Return',
        'ConfirmTimeLimit': '2016-01-12T01:15:00',
        'OrderItemBlanks': [
            {
                "PreviousOrderItemBlankId": int(bid),
                "Amount": am,
                "OrderItemBlankId": int(bid) + 10000,
            }
            for bid, am in izip(blank_ids, amounts)
        ],
        'OrderItemId': operation_id,
    }


def make_order_item(blank_ids):
    return {
        'OrderItemId': 1,
        'OrderItemBlanks': [
            {'PreviousOrderItemBlankId': int(blank_id)}
            for blank_id in blank_ids
        ],
        'ConfirmDateTime': '2017-01-20T12:00:00',
    }


def make_order(order_id, *order_items):
    return {
        'OrderItems': order_items,
        'OrderId': order_id,
    }


def make_response(*orders):
    return httpretty_module.Response(force_bytes(json.dumps({'Orders': orders})))


@replace_now('2017-01-20 23:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_has_no_office_refunds(httpretty):
    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response()])
    check_im_office_refunds()

    assert_that(httpretty.last_request.parsed_body, has_entries({
        'Date': '2017-01-19T00:00:00',
        'OperationType': 'Return',
        'IsExternallyLoaded': True
    }))
    assert len(httpretty.latest_requests) == 1


@pytest.mark.parametrize('check_days_in_past', (1, 2, 10))
@replace_now('2017-01-20 23:00:00')
def test_days_in_past(httpretty, check_days_in_past):
    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response()])
    with mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', check_days_in_past):
        check_im_office_refunds()
    assert_that([r.parsed_body for r in httpretty.latest_requests], contains_inanyorder(
        *[
            has_entries({
                'Date': (environment.today() - timedelta(day)).strftime('%Y-%m-%dT00:00:00'),
                'OperationType': 'Return',
                'IsExternallyLoaded': True
            })
            for day in range(1, check_days_in_past + 1)
        ]
    ))
    assert len(httpretty.latest_requests) == check_days_in_past


@pytest.mark.parametrize('day, now', [
    (date(2017, 1, 19), '2017-01-20 08:00:00'),
    (date(2017, 1, 20), '2017-01-20 08:00:00'),
    (date(2017, 1, 20), '2017-01-20 16:00:00'),
    (date(2017, 1, 21), '2017-01-20 08:00:00'),
    (date(2017, 1, 21), '2017-01-20 22:00:00'),
])
def test_too_early_error(day, now):
    with pytest.raises(TooEarlyError), replace_now(now):
        check_for_external_refunds(day)


@pytest.mark.parametrize('refund_status, has_refund', [
    (None, True),

    (RefundStatus.NEW, False),
    (RefundStatus.PARTNER_REFUND_DONE, False),
    (RefundStatus.PARTNER_REFUND_UNKNOWN, False),
    (RefundStatus.DONE, False),
    (RefundStatus.FAILED, True),
])
@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_external_refund_with_1_blank(m_apply_async, httpretty, refund_status, has_refund):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data=PartnerDataFactory(im_order_id=200),
                              passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])])
    if refund_status:
        TrainRefundFactory(order_uid=order.uid, blank_ids=['1'], status=refund_status,
                           created_at=environment.now_utc() - timedelta(1))

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=make_order_info(order, make_refund_order_item(
        222, ['1'], amounts=[100.10], is_external=True
    )))

    check_im_office_refunds()

    refund = order.last_refund
    if has_refund:
        m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])
    else:
        assert not m_apply_async.called


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_external_refund_with_interrupted_status(m_apply_async, httpretty):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data=PartnerDataFactory(im_order_id=200),
                              passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])])

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    order_info = make_order_info(order, make_refund_order_item(222, ['1'], amounts=[100.10], is_external=True))
    order_info['OrderItems'][0]['OrderItemBlanks'][0]['BlankStatus'] = 'TripWasInterrupted'
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info)

    check_im_office_refunds()

    order.reload()
    assert next(order.iter_tickets()).rzhd_status == RzhdStatus.INTERRUPTED
    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_external_refund_with_1_blank_in_2_order_item(m_apply_async, httpretty):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data=PartnerDataFactory(im_order_id=200),
                              passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])])

    mock_im(httpretty, ORDERS_ENDPOINT,
            responses=[make_response(make_order(200, make_order_item(['2']), make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=make_order_info(order, make_refund_order_item(
        222, ['1'], amounts=[100.10], is_external=True
    )))

    check_im_office_refunds()
    order.reload()

    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_order_with_insurance_external_refund_without_insurance(m_apply_async, httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=200), passengers=[PassengerFactory(
        tickets=[TicketFactory(blank_id='1')],
        insurance=InsuranceFactory(operation_id='11')
    )])

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory.add_refund_item(
        ImOrderInfoFactory(train_order=order), blank_id_to_refund=1, IsExternallyLoaded=True
    ))

    check_im_office_refunds()

    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])
    assert refund.blank_ids == ['1']
    assert not refund.insurance_ids


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-22 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
@pytest.mark.parametrize('finished_at, expected_refund_yandex_fee', [
    (None, Decimal(0)),
    (datetime(2017, 1, 19), Decimal(0)),
    (datetime(2017, 1, 20), Decimal(15)),
])
def test_order_with_refund_yandex_fee(m_apply_async, httpretty, finished_at, expected_refund_yandex_fee):
    order = TrainOrderFactory(
        finished_at=finished_at,
        partner_data=PartnerDataFactory(im_order_id=200),
        passengers=[PassengerFactory(tickets=[TicketFactory(
            blank_id='1',
            payment=TicketPaymentFactory(partner_fee=Decimal(30), partner_refund_fee=Decimal(25))
        )])],
    )

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    order_info_response = ImOrderInfoFactory(train_order=order)
    order_info_response = ImOrderInfoFactory.add_refund_item(
        order_info_response, blank_id_to_refund=1, IsExternallyLoaded=True)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info_response)

    check_im_office_refunds()
    order.reload()

    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])
    assert refund.blank_ids == ['1']
    assert order.passengers[0].tickets[0].refund.refund_yandex_fee_amount == expected_refund_yandex_fee


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_order_with_insurance_external_refund_with_insurance(m_apply_async, httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=200), passengers=[PassengerFactory(
        tickets=[TicketFactory(blank_id='1')],
        insurance=InsuranceFactory(operation_id='11')
    )])

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    order_info_response = ImOrderInfoFactory(train_order=order)
    order_info_response = ImOrderInfoFactory.add_refund_item(
        order_info_response, blank_id_to_refund=1, IsExternallyLoaded=True)
    order_info_response = ImOrderInfoFactory.add_refund_insurance_item(order_info_response, operation_id_to_refund=11)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info_response)

    check_im_office_refunds()

    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])
    assert refund.blank_ids == ['1']
    assert refund.insurance_ids == ['11']


@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_external_refund_with_autorefunded_insurance(m_apply_async, httpretty):
    order = TrainOrderFactory(partner_data=PartnerDataFactory(im_order_id=200), passengers=[PassengerFactory(
        tickets=[TicketFactory(blank_id='1')],
        insurance=InsuranceFactory(operation_id='11')
    )])
    TrainRefundFactory(order_uid=order.uid, blank_ids=None, insurance_ids=['11'],
                       created_at=environment.now_utc() - timedelta(hours=12))

    mock_im(httpretty, ORDERS_ENDPOINT, responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im(httpretty, IM_UPDATE_BLANKS_METHOD, body='{"Blanks": []}')
    order_info_response = ImOrderInfoFactory(train_order=order)
    order_info_response = ImOrderInfoFactory.add_refund_item(
        order_info_response, blank_id_to_refund=1, IsExternallyLoaded=True)
    order_info_response = ImOrderInfoFactory.add_refund_insurance_item(order_info_response, operation_id_to_refund=11)
    mock_im(httpretty, IM_ORDER_INFO_METHOD, json=order_info_response)

    check_im_office_refunds()

    refund = order.last_refund
    m_apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id), {'order_uid': refund.order_uid}])
    assert refund.blank_ids == ['1']
    assert not refund.insurance_ids


@mock.patch('travel.rasp.train_api.train_purchase.tasks.check_office_refunds.guaranteed_send_email')
@mock.patch.object(run_process, 'apply_async')
@replace_now('2017-01-20 14:00:00')
@mock.patch.object(check_office_refunds, 'CHECK_DAYS_IN_PAST', 1)
def test_send_error_email(m_apply_async, m_guaranteed_send_email, httpretty):
    order = TrainOrderFactory(partner=TrainPartner.IM, partner_data=PartnerDataFactory(im_order_id=200),
                              passengers=[PassengerFactory(tickets=[TicketFactory(blank_id='1')])])

    mock_im(httpretty, ORDERS_ENDPOINT,
            responses=[make_response(make_order(200, make_order_item(['1'])))])
    mock_im_500(httpretty, IM_UPDATE_BLANKS_METHOD)

    check_im_office_refunds()

    m_guaranteed_send_email.assert_called_once_with(
        key='office_refund_error_{}_blanks_{}'.format(order.uid, '1'),
        to_email=conf.TRAIN_PURCHASE_ERRORS_EMAIL,
        args={
            'order_uid': order.uid,
            'error_message': 'Error in OfficeRefund',
            'blank_ids': ['1'],
        },
        campaign=settings.OFFICE_REFUND_ERRORS_CAMPAIGN,
        data={'order_uid': order.uid},
        log_context={'order_uid': order.uid},)
    assert not m_apply_async.called
