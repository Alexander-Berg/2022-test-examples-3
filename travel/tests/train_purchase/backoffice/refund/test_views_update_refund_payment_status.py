# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_entry, anything, has_properties

import travel.rasp.train_api.train_purchase.backoffice.refund.views
from common.data_api.billing.trust_client import TrustRefundStatuses, TrustClientException, TrustPaymentInfo
from common.tester.matchers import has_json
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory
from travel.rasp.train_api.train_purchase.core.models import RefundPaymentStatus

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.fixture
def m_trust_client():
    with mock.patch.object(travel.rasp.train_api.train_purchase.backoffice.refund.views, 'TrustClient') as m_trust_client:
        yield m_trust_client


def test_refund_not_found(backoffice_client):
    response = backoffice_client.post('/ru/train-purchase-backoffice/refund/12345/update_refund_payment_status/')

    assert response.status_code == 404
    assert_that(response.content, has_json(has_entry('errors', anything())))


def test_cant_get_refund_status(backoffice_client, m_trust_client):
    m_get_refund_status = m_trust_client.return_value.get_refund_status
    m_get_refund_status.side_effect = TrustClientException('some error!')
    refund_payment = RefundPaymentFactory(factory_extra_params={'create_order': True},
                                          refund_payment_status=RefundPaymentStatus.UNKNOWN)

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    assert response.status_code == 500
    assert_that(response.content, has_json(has_entry('errors', anything())))
    refund_payment.reload()
    assert refund_payment.refund_payment_status == RefundPaymentStatus.UNKNOWN
    assert not refund_payment.refund_payment_finished_at
    m_get_refund_status.assert_called_once_with(refund_payment.trust_refund_id)


@replace_now('2018-06-24 12:00:00')
@pytest.mark.parametrize('trust_refund_status, expected_update', (
    (TrustRefundStatuses.WAIT_FOR_NOTIFICATION, False),
    (TrustRefundStatuses.SUCCESS, True),
))
def test_update_status(backoffice_client, m_trust_client, trust_refund_status, expected_update):
    m_get_refund_status = m_trust_client.return_value.get_refund_status
    m_get_refund_status.return_value = trust_refund_status
    refund_payment = RefundPaymentFactory(factory_extra_params={'create_order': True})

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    refund_payment.reload()
    assert response.status_code == 200
    if not expected_update:
        assert_that(response.content, has_json(has_entry('errors', anything())))
        assert refund_payment.refund_payment_status == RefundPaymentStatus.UNKNOWN
        assert not refund_payment.refund_payment_finished_at
    else:
        assert_that(response.content, has_json(has_entry('ok', True)))
        assert refund_payment.refund_payment_status == RefundPaymentStatus.DONE
        assert refund_payment.refund_payment_finished_at == environment.now_utc()
    m_get_refund_status.assert_called_once_with(refund_payment.trust_refund_id)


def test_no_trust_refund_id(backoffice_client):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        trust_refund_id=None,
        payment_resized=False,
    )

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    assert response.status_code == 400
    assert_that(response.content, has_json(has_entry('errors', anything())))
    refund_payment.reload()
    assert refund_payment.refund_payment_status == RefundPaymentStatus.UNKNOWN
    assert not refund_payment.refund_payment_finished_at


def test_invalid_payment_status(backoffice_client):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        refund_payment_status=RefundPaymentStatus.FAILED,
        payment_resized=False,
    )

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    assert response.status_code == 400
    assert_that(response.content, has_json(has_entry('errors', anything())))
    refund_payment.reload()
    assert refund_payment.refund_payment_status == RefundPaymentStatus.FAILED
    assert not refund_payment.refund_payment_finished_at


@replace_now('2018-06-24 12:00:00')
def test_resized_ok(backoffice_client, m_trust_client):
    m_get_payment_info = m_trust_client.return_value.get_payment_info
    m_get_payment_info.return_value = TrustPaymentInfo(payment_info={
        'payment_status': 'authorized',
        'reversal_id': 'some-reversal-id',
    })
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        payment_resized=True,
        trust_reversal_id=None,
    )

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    refund_payment.reload()
    assert response.status_code == 200
    assert_that(response.content, has_json(has_entry('ok', True)))
    assert_that(refund_payment, has_properties(
        refund_payment_status=RefundPaymentStatus.DONE,
        refund_payment_finished_at=environment.now_utc(),
        trust_reversal_id='some-reversal-id',
    ))
    m_get_payment_info.assert_called_once_with(refund_payment.purchase_token)


def test_resized_no_reversal_id(backoffice_client, m_trust_client):
    m_get_payment_info = m_trust_client.return_value.get_payment_info
    m_get_payment_info.return_value = TrustPaymentInfo(payment_info={
        'payment_status': 'authorized',
    })
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        payment_resized=True,
        trust_reversal_id=None,
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
    )

    response = backoffice_client.post(
        '/ru/train-purchase-backoffice/refund/{}/update_refund_payment_status/'.format(refund_payment.refund_uuid))

    refund_payment.reload()
    assert response.status_code == 200
    assert_that(response.content, has_json(has_entry('errors', anything())))
    assert_that(refund_payment, has_properties(
        refund_payment_status=RefundPaymentStatus.UNKNOWN,
        refund_payment_finished_at=None,
        trust_reversal_id=None,
    ))
    m_get_payment_info.assert_called_once_with(refund_payment.purchase_token)
