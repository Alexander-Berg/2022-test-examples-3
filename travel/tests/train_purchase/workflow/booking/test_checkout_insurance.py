# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from django.conf import settings

from common.dynamic_settings.default import conf
from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import InsuranceStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, PassengerFactory, InsuranceFactory
from travel.rasp.train_api.train_purchase.workflow.booking import checkout_insurance
from travel.rasp.train_api.train_purchase.workflow.booking.checkout_insurance import CheckoutInsurance, CheckoutInsuranceEvents

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


def _process(order):
    return process_state_action(
        CheckoutInsurance,
        (CheckoutInsuranceEvents.DONE, CheckoutInsuranceEvents.FAILED),
        order,
    )


@mock.patch.object(checkout_insurance, 'checkout', autospec=True)
def test_checkout_insurance_done(m_checkout):
    original_order = TrainOrderFactory(passengers=[PassengerFactory(insurance=InsuranceFactory())])
    event, order = _process(original_order)

    assert event == CheckoutInsuranceEvents.DONE
    assert order.insurance.status == InsuranceStatus.CHECKED_OUT
    m_checkout.assert_called_once_with(original_order)


@mock.patch.object(checkout_insurance, 'guaranteed_send_email', autospec=True)
@mock.patch.object(checkout_insurance, 'checkout', autospec=True, side_effect=Exception('Bang!'))
def test_checkout_insurance_failed(m_checkout, m_sender):
    original_order = TrainOrderFactory(passengers=[PassengerFactory(insurance=InsuranceFactory())])
    event, order = _process(original_order)

    assert event == CheckoutInsuranceEvents.FAILED
    assert order.insurance.status == InsuranceStatus.FAILED
    m_checkout.assert_called_once_with(original_order)
    m_sender.assert_called_once_with(
        key='insurance_error_email_{}'.format(order.uid),
        to_email=conf.TRAIN_PURCHASE_ERRORS_EMAIL,
        args={'order_uid': order.uid},
        campaign=settings.INSURANCE_ERROR_CAMPAIGN,
        log_context={'order_uid': order.uid},
    )
