# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base.train_details.utils import (
    BRAND_SAPSAN, BRAND_LASTOCHKA, OWNER_DOSS, can_segregate_pets,
)

OWNER_FPK = 'ФПК'
BRAND_BAIKAL = 'Байкал'


class Coach(object):
    def __init__(self, pet_places_only):
        self.pet_places_only = pet_places_only


@pytest.mark.parametrize('coach_owner, train_brand, coaches, expected', (
    (OWNER_FPK, BRAND_BAIKAL, [Coach(False), Coach(False)], False),
    (OWNER_FPK, BRAND_LASTOCHKA, [Coach(False), Coach(False)], False),
    (OWNER_DOSS, BRAND_BAIKAL, [Coach(False), Coach(False)], False),
    (OWNER_FPK, BRAND_BAIKAL, [Coach(False), Coach(True)], True),
    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False), Coach(False)], True),
    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False), Coach(True)], True),
    (OWNER_DOSS, BRAND_SAPSAN, [Coach(False), Coach(False)], True),
    (OWNER_DOSS, BRAND_SAPSAN, [Coach(False), Coach(True)], True)
))
def test_is_pets_allowed(coach_owner, train_brand, coaches, expected):
    assert can_segregate_pets(coach_owner, train_brand, coaches) == expected
