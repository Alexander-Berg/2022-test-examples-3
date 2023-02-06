# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceNumberError, parse_place_number, init_places


def test_parce_place_number():
    assert parse_place_number('42', 'compartment') == (42, None)

    assert parse_place_number('42Ж', 'compartment') == (42, 'female')

    with pytest.raises(PlaceNumberError):
        parse_place_number('42Х', 'compartment')

    with pytest.raises(PlaceNumberError):
        parse_place_number('42И', 'compartment')

    assert parse_place_number('А42', 'common') == (42, None)

    with pytest.raises(PlaceNumberError):
        parse_place_number('А42', 'compartment')


@pytest.mark.parametrize('numbers_string, coach_type, expected_numbers', (
    (None, 'compartment', []),
    ('3, 1, 2', 'compartment', [1, 2, 3]),
    ('1, 2, А3', 'compartment', [1, 2]),
    ('1, 2, А2, А3', 'common', [1, 2, 3]),
))
def test_init_places(numbers_string, coach_type, expected_numbers):
    assert [place_number.number for place_number in init_places(numbers_string, coach_type)] == expected_numbers
