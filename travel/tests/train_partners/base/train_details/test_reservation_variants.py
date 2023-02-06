# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, contains_inanyorder

from common.apps.train.models import TariffInfo
from travel.rasp.train_api.train_partners.base.train_details.reservation_variants import ReservationVariant


def test_generate_passengers_for_place_count_1_max_4():
    result = ReservationVariant.generate_passengers_for_place_count(1, 4)
    assert_that(list(result), contains_inanyorder(
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 0},
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 1},
    ))


def test_generate_passengers_for_place_count_2_max_3():
    result = ReservationVariant.generate_passengers_for_place_count(2, 3)
    assert_that(list(result), contains_inanyorder(
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 1, TariffInfo.BABY_CODE: 0},
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 1, TariffInfo.BABY_CODE: 1},
        {TariffInfo.FULL_CODE: 2, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 0},
        {TariffInfo.FULL_CODE: 2, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 1},
    ))


def test_generate_passengers_for_place_count_2_max_4():
    result = ReservationVariant.generate_passengers_for_place_count(2, 4)
    assert_that(list(result), contains_inanyorder(
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 1, TariffInfo.BABY_CODE: 0},
        {TariffInfo.FULL_CODE: 1, TariffInfo.CHILD_CODE: 1, TariffInfo.BABY_CODE: 1},
        {TariffInfo.FULL_CODE: 2, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 0},
        {TariffInfo.FULL_CODE: 2, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 1},
        {TariffInfo.FULL_CODE: 2, TariffInfo.CHILD_CODE: 0, TariffInfo.BABY_CODE: 2},
    ))
