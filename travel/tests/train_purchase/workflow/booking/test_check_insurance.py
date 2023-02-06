# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import InsuranceStatus, OrderStatus, RebookingStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, InsuranceProcessFactory, PaymentFactory, RebookingInfoFactory,
)
from travel.rasp.train_api.train_purchase.workflow.booking.check_insurance import CheckInsurance, CheckInsuranceEvents

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def _process(order):
    return process_state_action(
        CheckInsurance,
        (CheckInsuranceEvents.OK, CheckInsuranceEvents.FAILED),
        order,
    )


@pytest.mark.parametrize(
    'insurance_status, purchase_token, expected_event, expected_status, expected_rebooking_status',
    [
        (InsuranceStatus.CHECKED_OUT, None, CheckInsuranceEvents.OK, OrderStatus.RESERVED, RebookingStatus.DONE),
        (InsuranceStatus.FAILED, None, CheckInsuranceEvents.OK, OrderStatus.RESERVED, RebookingStatus.DONE),
        (InsuranceStatus.CHECKED_OUT, 'token', CheckInsuranceEvents.OK, OrderStatus.RESERVED, RebookingStatus.DONE),
        (InsuranceStatus.FAILED, 'token', CheckInsuranceEvents.FAILED, OrderStatus.CONFIRM_FAILED,
         RebookingStatus.FAILED),
    ],
)
def test_check_insurance_event(insurance_status, purchase_token, expected_event, expected_status,
                               expected_rebooking_status):
    original_order = TrainOrderFactory(
        insurance=InsuranceProcessFactory(status=insurance_status),
        payments=[PaymentFactory(purchase_token=purchase_token)],
        rebooking_info=RebookingInfoFactory(status=RebookingStatus.DONE),
    )

    event, order = _process(original_order)

    assert order.status == expected_status
    assert event == expected_event
    assert order.rebooking_info.status == expected_rebooking_status
