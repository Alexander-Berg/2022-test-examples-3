# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import has_entries, assert_that

from common.tester.utils.replace_setting import replace_setting
from travel.rasp.train_api.train_purchase.backoffice.trust_info import views
from travel.rasp.train_api.train_purchase.core.factories import RefundPaymentFactory

PAYMENT_INFO_RESULT = {
    'status': 'some-status',
    'fiscal_receipt_clearing_url': 'some-receipt-url',
}


@pytest.fixture(autouse=True, scope='module')
def m_trust_client():
    with mock.patch.object(views, 'TrustClient', autospec=True) as m_trust_client:
        m_trust_client_object = m_trust_client.return_value
        m_trust_client_object.get_raw_payment_info.return_value = PAYMENT_INFO_RESULT
        yield m_trust_client


@pytest.mark.dbuser
@pytest.mark.mongouser
@replace_setting('BYPASS_BACKOFFICE_AUTH', True)
def test_trust_info_refund_by_uuid_payment_resized(backoffice_client):
    refund_payment = RefundPaymentFactory(
        factory_extra_params={'create_order': True},
        payment_resized=True,
    )
    response = backoffice_client.get('/ru/train-purchase-backoffice/trust-info/refund/',
                                     {'refundUuid': refund_payment.refund_uuid})
    assert response.status_code == 200
    assert_that(response.data, has_entries(
        status='ok',
        info=has_entries(
            status='some-status',
            fiscal_receipt_url='some-receipt-url',
        ),
    ))
