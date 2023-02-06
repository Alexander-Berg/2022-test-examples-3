# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest

from common.data_api.sendr.api import Attachment
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus, RefundPayment
from travel.rasp.train_api.train_purchase.tasks.finish_refund_payment import send_fail_email
from travel.rasp.train_api.train_purchase.tasks.finish_refund_payment.send_fail_email import (
    send_failed_refund_payment_email, MAX_INTERVAL_FOR_RETRIEVE_UNKNOWN_PAYMENTS, MAX_INTERVAL_FOR_RETRY_PAYMENTS
)
from travel.rasp.train_api.train_purchase.workflow.payment.refund_payment import REFUND_FAILED_MESSAGE, REFUND_UNKNOWN_MESSAGE

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]

TEST_EMAIL = 'problem@example.com'
PAYMENT_INFO_RESULT = {'get_raw_payment_info': 'some-payment-info'}
REFUND_INFO_RESULT = {'get_refund_info': 'some-refund-info'}
PAYMENT_CONTENT = json.dumps(PAYMENT_INFO_RESULT, indent=2, ensure_ascii=False, sort_keys=True).encode('utf-8')
REFUND_CONTENT = json.dumps(REFUND_INFO_RESULT, indent=2, ensure_ascii=False, sort_keys=True).encode('utf-8')


@pytest.fixture
def m_send():
    with mock.patch.object(send_fail_email.campaign, 'send', autospec=True) as m_send:
        yield m_send


@pytest.fixture(autouse=True)
def m_trust_client():
    with mock.patch.object(send_fail_email, 'TrustClient', autospec=True) as m_trust_client:
        m_trust_client_object = m_trust_client.return_value
        m_trust_client_object.get_raw_payment_info.return_value = PAYMENT_INFO_RESULT
        m_trust_client_object.get_refund_info.return_value = REFUND_INFO_RESULT
        yield m_trust_client


@pytest.fixture(autouse=True)
def replace_errors_email():
    with replace_dynamic_setting('TRAIN_PURCHASE_ERRORS_EMAIL', TEST_EMAIL):
        yield


@pytest.mark.parametrize('already_sent', [True, False])
def test_failed_refund_payment(already_sent, m_send):
    failed_refund_payment = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.FAILED,
        is_email_sent=already_sent
    )

    send_failed_refund_payment_email()

    updated_failed_refund_payment = RefundPayment.objects.get(refund_uuid=failed_refund_payment.refund_uuid)
    assert updated_failed_refund_payment.is_email_sent
    if already_sent:
        assert not m_send.called
    else:
        m_send.assert_called_once_with(
            TEST_EMAIL,
            args={
                'order_uid': failed_refund_payment.order_uid,
                'error_message': REFUND_FAILED_MESSAGE,
                'blank_ids': failed_refund_payment.refund_blank_ids,
                'trust_refund_id': failed_refund_payment.trust_refund_id,
                'refund_uuid': failed_refund_payment.refund_uuid,
                'purchase_token': failed_refund_payment.purchase_token,
                'refund_payment_status': RefundPaymentStatus.FAILED,
            },
            attachments=[
                Attachment(filename='trust_refund_info.json', mime_type='application/json', content=REFUND_CONTENT),
                Attachment(filename='trust_payment_info.json', mime_type='application/json', content=PAYMENT_CONTENT),
            ]
        )


@pytest.mark.parametrize('already_sent', [True, False])
def test_truly_unknown_refund_payment(already_sent, m_send):
    unknown_refund_payment = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        refund_created_at=now_utc() - MAX_INTERVAL_FOR_RETRIEVE_UNKNOWN_PAYMENTS,
        is_email_sent=already_sent
    )

    send_failed_refund_payment_email()

    updated_unknown_refund_payment = RefundPayment.objects.get(refund_uuid=unknown_refund_payment.refund_uuid)
    assert updated_unknown_refund_payment.is_email_sent
    if already_sent:
        assert not m_send.called
    else:
        m_send.assert_called_once_with(
            TEST_EMAIL,
            args={
                'order_uid': updated_unknown_refund_payment.order_uid,
                'error_message': REFUND_UNKNOWN_MESSAGE,
                'blank_ids': updated_unknown_refund_payment.refund_blank_ids,
                'trust_refund_id': updated_unknown_refund_payment.trust_refund_id,
                'refund_uuid': updated_unknown_refund_payment.refund_uuid,
                'purchase_token': updated_unknown_refund_payment.purchase_token,
                'refund_payment_status': RefundPaymentStatus.UNKNOWN,
            },
            attachments=[
                Attachment(filename='trust_refund_info.json', mime_type='application/json', content=REFUND_CONTENT),
                Attachment(filename='trust_payment_info.json', mime_type='application/json', content=PAYMENT_CONTENT),
            ]
        )


def test_temporary_unknown_refund_payment(m_send):
    unknown_refund_payment = RefundPaymentFactory(
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        refund_created_at=now_utc(),
        is_email_sent=False
    )

    send_failed_refund_payment_email()

    unknown_refund_payment = RefundPayment.objects.get(refund_uuid=unknown_refund_payment.refund_uuid)
    assert not unknown_refund_payment.is_email_sent
    assert not m_send.called


@pytest.mark.parametrize('already_sent, refund_payment_status', [
    (True, RefundPaymentStatus.TRY_LATER),
    (True, RefundPaymentStatus.NEW),
    (False, RefundPaymentStatus.TRY_LATER),
    (False, RefundPaymentStatus.NEW),
])
def test_expired_try_later_refund_payment(already_sent, refund_payment_status, m_send):
    try_later_refund_payment = RefundPaymentFactory(
        refund_payment_status=refund_payment_status,
        refund_created_at=now_utc() - MAX_INTERVAL_FOR_RETRY_PAYMENTS,
        is_email_sent=already_sent
    )

    send_failed_refund_payment_email()

    try_later_refund_payment.reload()
    assert try_later_refund_payment.is_email_sent
    if already_sent:
        assert not m_send.called
    else:
        assert try_later_refund_payment.refund_payment_status == RefundPaymentStatus.FAILED
        m_send.assert_called_once_with(
            TEST_EMAIL,
            args={
                'order_uid': try_later_refund_payment.order_uid,
                'error_message': REFUND_FAILED_MESSAGE,
                'blank_ids': try_later_refund_payment.refund_blank_ids,
                'trust_refund_id': try_later_refund_payment.trust_refund_id,
                'refund_uuid': try_later_refund_payment.refund_uuid,
                'purchase_token': try_later_refund_payment.purchase_token,
                'refund_payment_status': refund_payment_status,
            },
            attachments=[
                Attachment(filename='trust_refund_info.json', mime_type='application/json', content=REFUND_CONTENT),
                Attachment(filename='trust_payment_info.json', mime_type='application/json', content=PAYMENT_CONTENT),
            ]
        )


@pytest.mark.parametrize('refund_payment_status', [RefundPaymentStatus.TRY_LATER, RefundPaymentStatus.NEW])
def test_not_expired_try_later_refund_payment(refund_payment_status, m_send):
    try_later_refund_payment = RefundPaymentFactory(
        refund_payment_status=refund_payment_status,
        refund_created_at=now_utc(),
        is_email_sent=False
    )

    send_failed_refund_payment_email()

    try_later_refund_payment = RefundPayment.objects.get(refund_uuid=try_later_refund_payment.refund_uuid)
    assert not try_later_refund_payment.is_email_sent
    assert not m_send.called
