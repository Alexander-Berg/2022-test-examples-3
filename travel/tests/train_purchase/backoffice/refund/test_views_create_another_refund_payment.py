# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_entry

import travel.rasp.train_api.train_purchase.backoffice.refund.views
from common.tester.matchers import has_json
from travel.rasp.train_api.train_purchase.core.factories import TrainRefundFactory, RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_refund_not_found(backoffice_client):
    response = backoffice_client.post('/ru/train-purchase-backoffice/refund/12345/create_another_refund_payment/')

    assert response.status_code == 404
    assert_that(
        response.content,
        has_json(has_entry('errors', has_entry('uuid', 'Refund was not found')))
    )


def test_refund_is_in_unhandled_exception_state(backoffice_client):
    refund = TrainRefundFactory(process={'state': 'unhandled_exception_state'})
    RefundPaymentFactory(refund_uuid=refund.uuid, refund_payment_status=RefundPaymentStatus.FAILED)
    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/create_another_refund_payment/'.format(refund.uuid))

    assert response.status_code == 409
    assert_that(
        response.content,
        has_json(has_entry('errors', has_entry('refund', 'Refund is in unhandled exception state')))
    )


@pytest.mark.parametrize('payment_status, expected_new_refund_payment_started', (
    (RefundPaymentStatus.FAILED, True),
    (RefundPaymentStatus.TRY_LATER, True),
    (RefundPaymentStatus.UNKNOWN, True),
    (RefundPaymentStatus.DONE, False),
    (RefundPaymentStatus.NEW, False),
))
def test_ok(backoffice_client, payment_status, expected_new_refund_payment_started):
    refund = TrainRefundFactory()
    RefundPaymentFactory(refund_uuid=refund.uuid, refund_payment_status=payment_status)
    with mock.patch.object(travel.rasp.train_api.train_purchase.backoffice.refund.views, 'send_event_to_refund') \
            as m_send_event_to_refund:
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/refund/{}/create_another_refund_payment/'.format(refund.uuid))

        if not expected_new_refund_payment_started:
            assert not m_send_event_to_refund.called
            assert response.status_code == 409
        else:
            assert m_send_event_to_refund.called
            assert response.status_code == 200
            assert_that(
                response.content,
                has_json(has_entry('ok', True))
            )


def test_resize(backoffice_client):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        refund_payment_status=RefundPaymentStatus.FAILED,
        payment_resized=True,
    )
    with mock.patch.object(travel.rasp.train_api.train_purchase.backoffice.refund.views, 'send_event_to_refund') \
            as m_send_event_to_refund:
        response = backoffice_client.post(
            '/ru/train-purchase-backoffice/refund/{}/create_another_refund_payment/'.format(refund_payment.refund_uuid))

        assert not m_send_event_to_refund.called
        assert response.status_code == 400
