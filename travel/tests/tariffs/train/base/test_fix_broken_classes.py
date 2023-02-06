# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.apps.train.tariff_error import TariffError
from common.apps.train_order.enums import CoachType
from travel.rasp.train_api.tariffs.train.base.utils import fix_broken_classes


def test_remove_all_unknown():
    assert not fix_broken_classes({CoachType.UNKNOWN.value: [TariffError.TOO_CHEAP.value]})
    assert not fix_broken_classes({CoachType.UNKNOWN.value: [TariffError.UNSUPPORTED_COACH_TYPE.value]})
    assert not fix_broken_classes({CoachType.UNKNOWN.value: [
        TariffError.TOO_CHEAP.value,
        TariffError.UNSUPPORTED_COACH_TYPE.value,
    ]})


def test_remain_known_coach_type():
    broken_classes = {
        CoachType.COMPARTMENT.value: [TariffError.TOO_CHEAP.value],
        CoachType.UNKNOWN.value: [TariffError.TOO_CHEAP.value]
    }

    broken_classes = fix_broken_classes(broken_classes)

    assert broken_classes == {CoachType.COMPARTMENT.value: [TariffError.TOO_CHEAP.value]}


def test_remain_good_reason():
    broken_classes = {CoachType.UNKNOWN.value: [
        TariffError.SOLD_OUT.value,
        TariffError.TOO_CHEAP.value,
        TariffError.UNSUPPORTED_COACH_TYPE.value,
    ]}

    broken_classes = fix_broken_classes(broken_classes)

    assert broken_classes == {CoachType.UNKNOWN.value: [TariffError.SOLD_OUT.value]}


def test_do_nothing():
    broken_classes = {
        CoachType.COMPARTMENT.value: [TariffError.SOLD_OUT.value],
        CoachType.UNKNOWN.value: [TariffError.SOLD_OUT.value]
    }

    broken_classes = fix_broken_classes(broken_classes)

    assert broken_classes == {
        CoachType.COMPARTMENT.value: [TariffError.SOLD_OUT.value],
        CoachType.UNKNOWN.value: [TariffError.SOLD_OUT.value]
    }
