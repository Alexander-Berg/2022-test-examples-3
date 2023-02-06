# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
from hamcrest import assert_that, contains, has_properties, instance_of

from common.models.currency import Price
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceDetails
from travel.rasp.train_api.train_partners.base.train_details.tariff_calculator.fee import set_tariffs_with_fee
from travel.rasp.train_api.train_purchase.core.factories import ClientContractFactory


@pytest.mark.mongouser
@replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_FEE', '0.09')
def test_set_tarifs_with_fee():
    contract = ClientContractFactory()
    original_adult_tariff1 = Decimal(1100)
    original_adult_tariff2 = Decimal(900)
    original_child_tariff1 = Decimal(450)
    original_child_tariff2 = Decimal(380)
    coach = CoachDetailsStub(
        coach_type='platzkarte',
        can_choose_bedding=True,
        places=[
            PlaceDetails(number=1,
                         original_adult_tariff=original_adult_tariff1, original_child_tariff=original_child_tariff1),
            PlaceDetails(number=2,
                         original_adult_tariff=original_adult_tariff2, original_child_tariff=original_child_tariff2)],
        service_tariff=Decimal(100))

    set_tariffs_with_fee(contract, coach)

    assert_that(coach.places, contains(
        has_properties(number=1,
                       original_adult_tariff=original_adult_tariff1, original_child_tariff=original_child_tariff1,
                       adult_tariff=instance_of(Price), child_tariff=instance_of(Price)),
        has_properties(number=2,
                       original_adult_tariff=original_adult_tariff2, original_child_tariff=original_child_tariff2,
                       adult_tariff=instance_of(Price), child_tariff=instance_of(Price))
    ))
