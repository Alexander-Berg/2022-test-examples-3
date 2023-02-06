# -*- coding: utf-8 -*-

import pytest

from common.utils.blablacar_utils import is_valid_blablacar_direction
from common.models.geo import Country, Settlement, Station
from common.tester.factories import create_settlement


@pytest.mark.parametrize('departure_point_type, arrival_point_type', (
    (Country, Country),
    (Country, Settlement),
    (Settlement, Country),
    (Country, Station),
    (Station, Country),
))
def test_is_valid_blablacar_direction_invalid_types(departure_point_type, arrival_point_type):
    assert not is_valid_blablacar_direction(departure_point_type(), arrival_point_type())


@pytest.mark.dbuser
def test_is_valid_blablacar_direction_no_coordinates():
    assert not is_valid_blablacar_direction(
        create_settlement(latitude=None, longitude=None),
        create_settlement(latitude=None, longitude=None)
    )
    assert not is_valid_blablacar_direction(
        create_settlement(latitude=0, longitude=0),
        create_settlement(latitude=None, longitude=None)
    )
    assert not is_valid_blablacar_direction(
        create_settlement(latitude=None, longitude=None),
        create_settlement(latitude=0, longitude=0),
    )


@pytest.mark.dbuser
def test_is_valid_blablacar_direction_distance():
    assert not is_valid_blablacar_direction(
        create_settlement(latitude=0, longitude=0),
        create_settlement(latitude=0, longitude=0),
    ), u'Не должно подходить направление с близкими точками'
    assert not is_valid_blablacar_direction(
        create_settlement(latitude=0, longitude=0),
        create_settlement(latitude=0, longitude=180),
    ), u'Не должно подходить направление с далёкими точками'
    assert is_valid_blablacar_direction(
        create_settlement(latitude=56.838607, longitude=60.605514),
        create_settlement(latitude=55.160026, longitude=61.40259),
    ), u'Должно подходить направление с координатами Екатеринбург-Челябинск'
