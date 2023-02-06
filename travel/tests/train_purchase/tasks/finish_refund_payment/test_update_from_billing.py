# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_properties

import travel.rasp.train_api.train_purchase.tasks.finish_refund_payment.update_from_billing
from common.data_api.billing.trust_client import TrustRefundStatuses, TrustPaymentInfo, TrustPaymentStatuses
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus, RefundPayment
from travel.rasp.train_api.train_purchase.tasks.finish_refund_payment.update_from_billing import update_refund_payment_status

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.tasks.finish_refund_payment.update_from_billing,
                           'TrustClient', autospec=True) as m_trust_client:
        yield m_trust_client


@replace_now('2018-04-17')
@pytest.mark.parametrize('trust_refund_status, expected_refund_payment_status, has_finished_at', [
    (TrustRefundStatuses.SUCCESS, RefundPaymentStatus.DONE, True),
    (TrustRefundStatuses.WAIT_FOR_NOTIFICATION, RefundPaymentStatus.UNKNOWN, False)
])
def test_update_refund_payment_status(m_trust_client, trust_refund_status,
                                      expected_refund_payment_status, has_finished_at):
    m_trust_client.return_value.get_refund_status.return_value = trust_refund_status
    pending_refund_payment = RefundPaymentFactory(refund_payment_status=RefundPaymentStatus.UNKNOWN)

    update_refund_payment_status()

    updated_pending_refund_payment = RefundPayment.objects.get(refund_uuid=pending_refund_payment.refund_uuid)
    assert_that(updated_pending_refund_payment, has_properties(
        refund_payment_status=expected_refund_payment_status,
        refund_payment_finished_at=environment.now_utc() if has_finished_at else None
    ))
    m_trust_client.return_value.get_refund_status.assert_called_once_with(pending_refund_payment.trust_refund_id)


def test_update_refund_payment_no_trust_refund_id(m_trust_client):
    failed_refund = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        trust_refund_id=None
    )

    update_refund_payment_status()

    updated_failed_refund = RefundPayment.objects.get(refund_uuid=failed_refund.refund_uuid)
    assert updated_failed_refund.refund_payment_status == RefundPaymentStatus.UNKNOWN
    assert not m_trust_client.return_value.get_refund_status.called


def test_update_unknown_resize(m_trust_client):
    refund_payment = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        is_email_sent=False,
        payment_resized=True,
    )
    m_trust_client.return_value.get_payment_info.return_value = TrustPaymentInfo({
        'payment_status': TrustPaymentStatuses.CLEARED,
        'reversal_id': '123123123'
    })
    update_refund_payment_status()
    refund_payment.reload()

    assert_that(refund_payment, has_properties(
        refund_payment_status=RefundPaymentStatus.DONE,
        trust_reversal_id='123123123',
    ))
    m_trust_client.return_value.get_payment_info.assert_called_once_with(refund_payment.purchase_token)
