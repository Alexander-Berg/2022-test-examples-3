# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder, contains
from rest_framework import status

from common.tester.matchers import has_json
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date import environment
from common.workflow import registry
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TrainPartner
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainRefundFactory, TicketFactory, PassengerFactory, ClientContractsFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundStatus, TrainPurchaseSmsVerification
from travel.rasp.train_api.train_purchase.views import generate_refund_sms_action_name
from travel.rasp.train_api.train_purchase.views.test_utils import create_order
from travel.rasp.train_api.train_purchase.workflow.ticket_refund import TICKET_REFUND_PROCESS

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


@pytest.fixture(autouse=True)
def fix_now():
    with replace_now('2017-09-05 10:00:00'):
        yield


def get_sms_validation_code(order, blank_ids):
    sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 9, 5, 9, 59), code='1111',
                                                    action_name=generate_refund_sms_action_name(blank_ids),
                                                    action_data={'uid': order.uid,
                                                                 'blank_ids': sorted(blank_ids)},
                                                    phone=order.user_info.phone, message='1')
    sms_verification.save()
    return sms_verification.code


def create_prepared_order(status, refund_params=None, blank_ids=('1',), already_refunded_blank_ids=('1',)):
    if refund_params is None:
        order = create_order(
            status=status,
            passengers=[
                PassengerFactory(tickets=[TicketFactory(blank_id=blank_id, rzhd_status=RzhdStatus.REMOTE_CHECK_IN)])
                for blank_id in blank_ids
            ])
    else:
        refund = TrainRefundFactory(
            factory_extra_params={
                'create_order': True,
                'create_order_kwargs': {'partner': TrainPartner.UFS, 'status': status, 'blank_ids': blank_ids}
            },
            blank_ids=already_refunded_blank_ids,
            created_at=environment.now_utc() - timedelta(1),
            **refund_params
        )
        order = refund.order

    return order


@pytest.fixture
def m_run_process():
    with mock.patch.object(registry, 'run_process') as m_run_process:
        yield m_run_process


def test_bad_params():
    order = create_prepared_order(OrderStatus.DONE)
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/train-purchase/orders/{}/refund/'.format(order.uid), '{}',
                             content_type='application/json')

    assert response.status_code == 400
    assert 'blankIds' in json.loads(response.content)['errors']


@pytest.mark.parametrize('order_status, refund_params, blank_ids, already_refunded_blank_ids, result_status', [
    (OrderStatus.DONE, None, ['1'], [], 200),
    (OrderStatus.DONE, {'status': RefundStatus.DONE}, ['1', '2'], ['2'], 200),
    (OrderStatus.DONE, {'status': RefundStatus.FAILED}, ['1', '2'], ['2'], 200),
    (OrderStatus.DONE, {'status': RefundStatus.NEW, 'is_active': True}, ['1', '2'], ['2'], status.HTTP_409_CONFLICT),
    (OrderStatus.RESERVED, {'status': RefundStatus.FAILED}, ['1', '2'], ['2'], status.HTTP_409_CONFLICT),
])
def test_refund_process(m_run_process, order_status, refund_params, blank_ids, already_refunded_blank_ids,
                        result_status):
    order = create_prepared_order(order_status, refund_params, blank_ids, already_refunded_blank_ids)
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/train-purchase/orders/{}/refund/'.format(order.uid), json.dumps({
        'blankIds': ['1'],
        'userInfo': {'ip': '1.1.1.1', 'region_id': 50},
        'SMSVerificationCode': get_sms_validation_code(order, ['1']),
    }), content_type='application/json')

    assert response.status_code == result_status
    if response.status_code == 200:
        order.reload()
        refund = order.last_refund
        assert refund.created_at == environment.now_utc()
        m_run_process.apply_async.assert_called_once_with([TICKET_REFUND_PROCESS, str(refund.id),
                                                           {'order_uid': order.uid}])
    else:
        assert not m_run_process.apply_async.called


def test_second_refund(m_run_process):
    order = create_prepared_order(OrderStatus.DONE, refund_params={'status': RefundStatus.DONE},
                                  blank_ids=['1', '2'], already_refunded_blank_ids=['1'])
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/train-purchase/orders/{}/refund/'.format(order.uid), json.dumps({
        'blankIds': ['2'],
        'userInfo': {'ip': '1.1.1.1', 'region_id': 50},
        'SMSVerificationCode': get_sms_validation_code(order, ['2'])
    }), content_type='application/json')

    assert response.status_code == 200
    order.reload()
    refunds = list(order.iter_refunds())
    assert_that(refunds, contains_inanyorder(
        has_properties(status=RefundStatus.NEW, blank_ids=contains('2'), is_active=True),
        has_properties(status=RefundStatus.DONE, blank_ids=contains('1'))
    ))


def test_try_to_refund_invalid_ticket(m_run_process):
    order = create_prepared_order(OrderStatus.DONE, refund_params={'status': RefundStatus.DONE},
                                  blank_ids=['1', '2'], already_refunded_blank_ids=['1', '2'])
    ClientContractsFactory(partner=order.partner)

    response = Client().post('/ru/api/train-purchase/orders/{}/refund/'.format(order.uid), json.dumps({
        'blankIds': ['1', '2'],
        'userInfo': {'ip': '1.1.1.1', 'region_id': 50},
        'SMSVerificationCode': get_sms_validation_code(order, ['1', '2'])
    }), content_type='application/json')

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert_that(response.content, has_json(
        has_entries(errors=has_entries(blankIds='Some of blank_ids are in invalid status'))
    ))
    assert not m_run_process.called


@replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', '')
def test_refund_not_active_partner(m_run_process):
    order = create_prepared_order(OrderStatus.DONE, refund_params=None,
                                  blank_ids=['1', '2'], already_refunded_blank_ids=[])

    response = Client().post('/ru/api/train-purchase/orders/{}/refund/'.format(order.uid), json.dumps({
        'blankIds': ['1', '2'],
        'userInfo': {'ip': '1.1.1.1', 'region_id': 50},
        'SMSVerificationCode': get_sms_validation_code(order, ['1', '2'])
    }), content_type='application/json')

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert_that(response.content, has_json(
        has_entries(errors='Partner is not active')
    ))
    assert not m_run_process.called
