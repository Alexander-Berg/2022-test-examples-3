# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.models.core.geo.country import Country
from travel.rasp.library.python.common23.models.core.geo.region import Region
from travel.rasp.library.python.common23.tester.factories import create_country, create_region, create_settlement

from travel.rasp.blablacar.blablacar.service.restriction import is_direction_banned, is_settlement_banned

pytestmark = [pytest.mark.dbuser]


def test_is_direction_banned():
    point_russia = create_settlement(country_id=Country.RUSSIA_ID)
    point_crimea = create_settlement(region=create_region(id=Region.CRIMEA_REGION_ID))
    finland = create_country(id=Country.FINLAND_ID)
    point_finland_1, point_finland_2 = create_settlement(country=finland), create_settlement(country=finland)

    assert is_direction_banned(point_crimea, point_russia)
    assert is_direction_banned(point_russia, point_crimea)
    assert is_direction_banned(point_finland_1, point_finland_2)
    assert not is_direction_banned(point_russia, point_finland_1)


def test_is_settlement_banned():
    point_russia = create_settlement(country_id=Country.RUSSIA_ID)
    point_crimea = create_settlement(region=create_region(id=Region.CRIMEA_REGION_ID))
    point_finland = create_settlement(country=create_country(id=Country.FINLAND_ID))

    assert is_settlement_banned(point_crimea)
    assert is_settlement_banned(point_finland)
    assert not is_settlement_banned(point_russia)
