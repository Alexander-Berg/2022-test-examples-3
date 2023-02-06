# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.tester.factories import create_settlement

from travel.rasp.blablacar.blablacar.service.distance import BlablacarDistanceError, check_blablacar_distance


@pytest.mark.dbuser
def test_check_blablacar_distance():
    moscow = create_settlement(longitude=37.619899, latitude=55.753676)
    piter = create_settlement(longitude=30.315868, latitude=59.939095)
    himki = create_settlement(longitude=37.451679, latitude=55.894383)
    vladivostok = create_settlement(longitude=131.882421, latitude=43.116391)
    no_coordinates = create_settlement(longitude=None, latitude=None)

    check_blablacar_distance(moscow, piter, 100)

    check_blablacar_distance(moscow, himki, 2)

    with pytest.raises(BlablacarDistanceError):
        check_blablacar_distance(moscow, himki, 100)

    with pytest.raises(BlablacarDistanceError):
        check_blablacar_distance(moscow, vladivostok, 100)

    with pytest.raises(BlablacarDistanceError):
        check_blablacar_distance(moscow, piter, 1000)

    with pytest.raises(BlablacarDistanceError):
        check_blablacar_distance(moscow, no_coordinates, 100)
