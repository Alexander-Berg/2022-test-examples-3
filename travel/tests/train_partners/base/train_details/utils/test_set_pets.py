# coding: utf-8

from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base.train_details.utils import set_pets, OWNER_DOSS, BRAND_LASTOCHKA

OWNER_FPK = 'ФПК'
BRAND_BAIKAL = 'Байкал'


class Coach(object):
    def __init__(self, pet_in_coach, pet_places_only):
        self.pet_in_coach = pet_in_coach
        self.pet_places_only = pet_places_only
        self.pets_allowed = False
        self.pets_segregated = False


class ExpectedCoach(object):
    def __init__(self, pets_allowed, pets_segregated):
        self.pets_allowed = pets_allowed
        self.pets_segregated = pets_segregated


@pytest.mark.parametrize('coach_owner, train_brand, coaches, expected_coaches', (
    (OWNER_FPK, BRAND_BAIKAL, [Coach(False, False), Coach(False, False)],
     [ExpectedCoach(False, False), ExpectedCoach(False, False)]),

    (OWNER_FPK, BRAND_BAIKAL, [Coach(False, False), Coach(True, False)],
     [ExpectedCoach(False, False), ExpectedCoach(True, False)]),

    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False, False), Coach(False, False)],
     [ExpectedCoach(False, False), ExpectedCoach(False, False)]),

    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False, False), Coach(True, False)],
     [ExpectedCoach(False, False), ExpectedCoach(False, True)]),

    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False, False), Coach(False, True)],
     [ExpectedCoach(False, False), ExpectedCoach(True, True)]),

    (OWNER_DOSS, BRAND_LASTOCHKA, [Coach(False, False), Coach(True, False), Coach(False, True)],
     [ExpectedCoach(False, False), ExpectedCoach(False, True), ExpectedCoach(True, True)])
))
def test_set_pets(coach_owner, train_brand, coaches, expected_coaches):
    coaches_with_pets = set_pets(coach_owner, train_brand, coaches)

    for index, coach in enumerate(coaches_with_pets):
        check_coach(coach, expected_coaches[index])


def check_coach(actual_coach, expected_coach):
    assert actual_coach.pets_allowed == expected_coach.pets_allowed
    assert actual_coach.pets_segregated == expected_coach.pets_segregated
