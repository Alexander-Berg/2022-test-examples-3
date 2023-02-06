# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

import travel.rasp.train_api.train_purchase.workflow.payment.refund_payment
from common.data_api.billing.trust_client import (
    TrustClientInvalidStatus, TrustRefundStatuses
)
from common.tester.utils.datetime import replace_now
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.factories import (
    RefundUserInfoFactory, RefundPaymentFactory
)
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus
from travel.rasp.train_api.train_purchase.workflow.payment.refund_payment import (
    RefundPaymentEvents, RefundPayment
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _refund_payment_process(refund):
    events = (RefundPaymentEvents.DONE, RefundPaymentEvents.FAILED)
    refund.payment.update(current_refund_payment_id=refund.id)
    return process_state_action(RefundPayment, events, refund.payment)


@pytest.fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.workflow.payment.refund_payment,
                           'TrustClient', autospec=True) as m_trust_client:
        yield m_trust_client


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_ok(m_trust_client):
    m_trust_client.return_value.start_refund.return_value = None
    m_trust_client.return_value.get_refund_status.return_value = TrustRefundStatuses.SUCCESS
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.CREATED, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id='some-trust-refund-id',
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.DONE
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_finished_at == datetime(2000, 1, 1, 9)
    assert refund.refund_payment_status == RefundPaymentStatus.DONE


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_start_refund_failed(m_trust_client):
    m_trust_client.return_value.start_refund.side_effect = TrustClientInvalidStatus('start refund error')
    # m_trust_client.return_value.get_refund_status.return_value = TrustRefundStatuses.
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.CREATED, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id='some-trust-refund-id',
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.FAILED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_finished_at is None
    assert refund.refund_payment_status == RefundPaymentStatus.FAILED


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_check_refund_status_unknown(m_trust_client):
    m_trust_client.return_value.start_refund.return_value = None
    m_trust_client.return_value.get_refund_status.return_value = TrustRefundStatuses.WAIT_FOR_NOTIFICATION
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.CREATED, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id='some-trust-refund-id',
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.FAILED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_finished_at is None
    assert refund.refund_payment_status == RefundPaymentStatus.UNKNOWN


@replace_now('2000-01-01 12:00:00')
def test_refund_payment_check_refund_status_error(m_trust_client):
    m_trust_client.return_value.start_refund.return_value = None
    m_trust_client.return_value.get_refund_status.side_effect = TrustClientInvalidStatus('check refund error')
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.CREATED, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id='some-trust-refund-id',
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.FAILED
    assert refund.trust_refund_id == 'some-trust-refund-id'
    assert refund.refund_payment_finished_at is None
    assert refund.refund_payment_status == RefundPaymentStatus.FAILED


def test_check_refund_payment_no_uid(m_trust_client):
    m_trust_client.return_value.start_refund.return_value = None
    m_trust_client.return_value.get_refund_status.return_value = TrustRefundStatuses.SUCCESS
    user_info = RefundUserInfoFactory(uid='100500')
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.CREATED, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id='some-trust-refund-id', user_info=user_info,
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.DONE
    assert refund.refund_payment_status == RefundPaymentStatus.DONE
    m_trust_client.assert_called_once_with(user_ip=user_info.ip, user_region_id=user_info.region_id)


@replace_now('2000-01-01 12:00:00')
def test_skip_refund_with_status_done(m_trust_client):
    refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.DONE, refund_blank_ids=['1'],
        factory_extra_params={'create_order': True},
        trust_refund_id=None,
    )

    event, payment = _refund_payment_process(refund)
    refund.reload()

    assert event == RefundPaymentEvents.DONE
    assert refund.trust_refund_id is None
    assert refund.refund_payment_finished_at == datetime(2000, 1, 1, 9)
    assert refund.refund_payment_status == RefundPaymentStatus.DONE

    assert not m_trust_client.called
