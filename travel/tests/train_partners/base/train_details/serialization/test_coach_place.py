# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.train_api.train_partners.base.train_details.models import CoachPlace
from travel.rasp.train_api.train_partners.base.train_details.serialization import CoachPlaceSchema


def test_coach_place():
    place = CoachPlace(number=17, group_number=5, geometry={'left': 100, 'top': 10})
    data = CoachPlaceSchema().dump(place).data

    assert data == {
        'number': 17,
        'groupNumber': 5,
        'geometry': {
            'left': 100,
            'top': 10
        }
    }
