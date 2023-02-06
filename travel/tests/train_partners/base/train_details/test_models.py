# coding: utf8

from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from travel.rasp.train_api.train_partners.base.train_details.models import CoachSchema


class TestCoachSchema(TestCase):
    def test_places(self):
        coach_schema = CoachSchema.objects.create(
            name='Схема плацкарта',
            _schema='1,0,10\n'
                    '2,0,0\n'
                    '3,10,10\n'
                    '4,10,0\n'
                    '5,20,10\n'
                    '6,20,0\n'
                    '7,30,10\n'
                    '8,30,0\n'
                    '9,40,10\n'
                    '10,40,0\n'
                    '11,50,10\n'
                    '12,50,0\n'
                    '13,45,110\n'
                    '14,45,100\n'
                    '15,25,110\n'
                    '16,25,100\n'
                    '17,5,110\n'
                    '18,5,100',
            _place_groups='1, 2, 3,4, 17 ,18\n'
                          '5, 6, 7, 8, 15, 16\n'
                          '9, 10, 11, 12, 13, 14',
            details='{'
                    '"placeFlags": {'
                    '"upper": [2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38, '
                    '40, 42, 44, 46, 48, 50, 52, 54],'
                    '"side": [37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54],'
                    '"nearToilet": [33, 34, 35, 36, 37, 38]'
                    '}, '
                    '"hidePlaceNumbers": true'
                    '}'
        )

        places = coach_schema.places
        assert places

        expected_places = [
            {'number': 1, 'group_number': 1, 'geometry': {'left': 0, 'top': 10}},
            {'number': 2, 'group_number': 1, 'geometry': {'left': 0, 'top': 0}},
            {'number': 3, 'group_number': 1, 'geometry': {'left': 10, 'top': 10}},
            {'number': 4, 'group_number': 1, 'geometry': {'left': 10, 'top': 0}},
            {'number': 5, 'group_number': 2, 'geometry': {'left': 20, 'top': 10}},
            {'number': 6, 'group_number': 2, 'geometry': {'left': 20, 'top': 0}},
            {'number': 7, 'group_number': 2, 'geometry': {'left': 30, 'top': 10}},
            {'number': 8, 'group_number': 2, 'geometry': {'left': 30, 'top': 0}},
            {'number': 9, 'group_number': 3, 'geometry': {'left': 40, 'top': 10}},
            {'number': 10, 'group_number': 3, 'geometry': {'left': 40, 'top': 0}},
            {'number': 11, 'group_number': 3, 'geometry': {'left': 50, 'top': 10}},
            {'number': 12, 'group_number': 3, 'geometry': {'left': 50, 'top': 0}},
            {'number': 13, 'group_number': 3, 'geometry': {'left': 45, 'top': 110}},
            {'number': 14, 'group_number': 3, 'geometry': {'left': 45, 'top': 100}},
            {'number': 15, 'group_number': 2, 'geometry': {'left': 25, 'top': 110}},
            {'number': 16, 'group_number': 2, 'geometry': {'left': 25, 'top': 100}},
            {'number': 17, 'group_number': 1, 'geometry': {'left': 5, 'top': 110}},
            {'number': 18, 'group_number': 1, 'geometry': {'left': 5, 'top': 100}}
        ]

        for place_index in range(len(places)):
            check_coach_place(places[place_index], expected_places[place_index])

        assert coach_schema.place_flags == {
            'upper': [2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36, 38,
                      40, 42, 44, 46, 48, 50, 52, 54],
            'side': [37, 38, 39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54],
            'nearToilet': [33, 34, 35, 36, 37, 38]
        }
        assert coach_schema.hide_place_numbers is True


def check_coach_place(place, expected_values):
    assert place.number == expected_values['number']
    assert place.group_number == expected_values['group_number']
    assert place.geometry['left'] == expected_values['geometry']['left']
    assert place.geometry['top'] == expected_values['geometry']['top']
