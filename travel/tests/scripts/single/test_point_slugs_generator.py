# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from itertools import chain

import mock
import pytest

from common.models.geo import Station, Settlement
from common.tester.factories import create_settlement, create_station, create_country, create_region
from travel.rasp.admin.scripts.single.point_slugs_generator import fill_slug_all


mock_make_settlement_slug = mock.patch(
    'travel.rasp.library.python.common23.models.core.geo.point_slugs.make_settlement_slug',
    return_value=None
)

mock_make_station_slug = mock.patch(
    'travel.rasp.library.python.common23.models.core.geo.point_slugs.make_station_slug',
    return_value=None
)


@pytest.mark.dbuser
def test_fill_slug_all():
    with mock_make_settlement_slug, mock_make_station_slug:
        settlement1 = create_settlement(title='Екатеринбург')
        create_station(settlement=settlement1, title='Пассажирский')
        create_station(settlement=settlement1, title='Екатеринбург', hidden=True)

        station2 = create_station(settlement=settlement1, title='Екатеринбург', hidden=False)
        settlement_to = create_settlement(title='Челябинск')
        create_station(settlement=settlement_to, title='Пассажирский')

    fill_slug_all()

    slugs = set()
    for point in chain(Settlement.objects.all(), Station.objects.all()):
        assert point.slug
        assert point.slug not in slugs
        slugs.add(point.slug)

    station2.refresh_from_db()
    assert station2.slug == 'ekaterinburg-bus-stop'


@pytest.mark.dbuser
def test_different_majority():
    with mock_make_settlement_slug, mock_make_station_slug:
        country = create_country(title='РФ', code='RF')
        region = create_region(title='MO', title_ru='MO')
        settlement1 = create_settlement(title='Екб', majority=1, region=region, country=country)
        settlement2 = create_settlement(title='Екб', majority=5, region=region, country=country)

    fill_slug_all()

    settlement1.refresh_from_db()
    settlement2.refresh_from_db()
    assert settlement1.slug == 'ekb'
    assert settlement2.slug != 'ekb'


@pytest.mark.dbuser
def test_same_majority_and_title():
    with mock_make_settlement_slug, mock_make_station_slug:
        country = create_country(title='РФ', code='RF')
        region = create_region(title='MO', title_ru='MO', country=country)
        country2 = create_country(title='USA', code='US')
        region2 = create_region(title='LA', title_en='LA', country=country2)
        region3 = create_region(title='ЛО', title_en='LO', country=country)
        settlement1 = create_settlement(title='Екб', majority=1, region=region, country=country)
        settlement2 = create_settlement(title='Екб', majority=1, region=region2, country=country2)
        settlement3 = create_settlement(title='Екб', majority=1, region=region3, country=country)

    fill_slug_all()

    settlement1.refresh_from_db()
    settlement2.refresh_from_db()
    settlement3.refresh_from_db()

    assert settlement1.slug == 'ekb-mo'
    assert settlement2.slug == 'ekb-us'
    assert settlement3.slug == 'ekb-lo'
