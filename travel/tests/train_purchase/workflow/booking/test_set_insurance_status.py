# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_purchase.core.enums import InsuranceStatus
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory, InsuranceProcessFactory
from travel.rasp.train_api.train_purchase.workflow.booking.set_insurance_status import SetInsuranceStatus, SetInsuranceStatusEvents

pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


def _process(order):
    return process_state_action(
        SetInsuranceStatus,
        (SetInsuranceStatusEvents.DONE, SetInsuranceStatusEvents.SKIPPED, SetInsuranceStatusEvents.FAILED),
        order,
    )


@pytest.mark.parametrize('insurance_status, insurance_enabled', [
    (None, True),
    (InsuranceStatus.DECLINED, True),
    (InsuranceStatus.ACCEPTED, False),
    (InsuranceStatus.CHECKED_OUT, True),
])
def test_set_insurance_status_skipped(insurance_status, insurance_enabled):
    original_order = TrainOrderFactory(
        insurance=InsuranceProcessFactory(status=insurance_status) if insurance_status else None,
        insurance_enabled=insurance_enabled,
    )
    event, order = _process(original_order)

    assert event == SetInsuranceStatusEvents.SKIPPED


def test_set_insurance_status_done():
    original_order = TrainOrderFactory(
        insurance=InsuranceProcessFactory(status=InsuranceStatus.ACCEPTED),
        insurance_enabled=True,
    )
    event, order = _process(original_order)

    assert event == SetInsuranceStatusEvents.DONE
    assert order.insurance.status == InsuranceStatus.CHECKING_OUT


def test_set_insurance_status_failed():
    original_order = TrainOrderFactory(
        insurance=InsuranceProcessFactory(status=InsuranceStatus.CHECKING_OUT),
        insurance_enabled=True,
    )
    event, order = _process(original_order)

    assert event == SetInsuranceStatusEvents.FAILED
    assert order.insurance.status == InsuranceStatus.CHECKING_OUT
