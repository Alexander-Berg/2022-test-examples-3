# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from hamcrest import has_entries, assert_that, has_entry, anything

from common.tester.matchers import has_json
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.train_api.train_purchase.backoffice.trust_info import views
from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory

PAYMENT_INFO_RESULT = {'payment_info': 'some-payment-info'}
REFUND_INFO_RESULT = {'refund_info': 'some-refund-info'}
PAYMENT_CONTENT = json.dumps(PAYMENT_INFO_RESULT)
REFUND_CONTENT = json.dumps(REFUND_INFO_RESULT)


@pytest.fixture(autouse=True, scope='module')
def m_trust_client():
    with mock.patch.object(views, 'TrustClient', autospec=True) as m_trust_client:
        m_trust_client_object = m_trust_client.return_value
        m_trust_client_object.get_raw_payment_info.return_value = PAYMENT_INFO_RESULT
        m_trust_client_object.get_refund_info.return_value = REFUND_INFO_RESULT
        yield m_trust_client


@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_trust_info_purchase(backoffice_client):
    response = backoffice_client.get('/ru/train-purchase-backoffice/trust-info/purchase/',
                                     {'purchaseToken': 'bd90bacb0390b6ca1781360780afe42f'})
    assert response.status_code == 200
    assert_that(response.data, has_entries(
        status='ok',
        info=has_entries(PAYMENT_INFO_RESULT)
    ))


@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_trust_info_refund(backoffice_client):
    response = backoffice_client.get('/ru/train-purchase-backoffice/trust-info/refund/',
                                     {'trustRefundId': '59ea0830795be248dc83d7b6'})
    assert response.status_code == 200
    assert_that(response.data, has_entries(
        status='ok',
        info=has_entries(REFUND_INFO_RESULT)
    ))


@pytest.mark.mongouser
@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_trust_info_refund_not_found_by_uuid(backoffice_client):
    response = backoffice_client.get('/ru/train-purchase-backoffice/trust-info/refund/',
                                     {'refundUuid': 'not-existing-uuid'})
    assert response.status_code == 400
    assert_that(response.content, has_json(has_entry('errors', anything())))


@pytest.mark.dbuser
@pytest.mark.mongouser
@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_trust_info_refund_by_uuid(backoffice_client):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        trust_refund_id='59ea0830795be248dc83d7b6',
    )
    response = backoffice_client.get('/ru/train-purchase-backoffice/trust-info/refund/',
                                     {'refundUuid': refund_payment.refund_uuid})
    assert response.status_code == 200
    assert_that(response.data, has_entries(
        status='ok',
        info=has_entries(REFUND_INFO_RESULT)
    ))
