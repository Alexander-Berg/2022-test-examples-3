# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from itertools import chain

import pytest
from django.core.exceptions import ValidationError
from django.db import transaction
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.common23.models.core.geo.station import Station
from travel.rasp.library.python.common23.models.core.geo.settlement import Settlement
from travel.rasp.library.python.common23.models.core.geo.station_type import StationType
from travel.rasp.library.python.common23.tester.factories import create_settlement, create_station, create_country, create_region
from travel.rasp.library.python.common23.models.core.geo.point_slugs import (
    slugify, VALID_SLUG_RE, make_station_slug_variant, make_settlement_slug_variant,
    find_by_slug, find_all_by_slug_startswith
)


def test_slugify():
    res = slugify('Съешь же ещё этих мягких французских булок да выпей чаю')
    assert VALID_SLUG_RE.match(res)
    assert VALID_SLUG_RE.match(slugify('El viejo Señor Gómez pedía queso, kiwi y habas, pero le ha tocado un saxofón.'))
    assert VALID_SLUG_RE.match(slugify(
        'Quizdeltagerne spiste jordbær med fløde, mens cirkusklovnen Walther spillede på xylofon'))
    assert VALID_SLUG_RE.match(slugify('321 знаки "@№:,.  -_пробелы   '))


@pytest.mark.dbuser
def test_make_station_slug_variant():
    country = create_country(title='РФ', code='RF')
    region = create_region(title='MO', title_ru='MO', country=country)
    settlement1 = create_settlement(title='Екатеринбург', region=region, country=country)
    station1 = create_station(settlement=settlement1, title='Депо', region=region, country=country,
                              station_type_id=StationType.STATION_ID)
    station2 = create_station(title='ОП 21км', region=region, country=country, station_type_id=StationType.STATION_ID)
    assert make_station_slug_variant(station1) == 'depo'
    assert make_station_slug_variant(station1, use_country=True) == 'depo-rf'
    assert make_station_slug_variant(station1, use_region=True) == 'depo-ekaterinburg'
    assert make_station_slug_variant(station1, use_type=True) == 'depo-station'
    assert make_station_slug_variant(station1, use_id=True) == 'depo-s{}'.format(station1.id)
    assert make_station_slug_variant(station1, use_country=True, use_region=True, use_id=True, use_type=True) == \
        'depo-station-ekaterinburg-rf-s{}'.format(station1.id)
    assert make_station_slug_variant(station2) == 'op-21km'
    assert make_station_slug_variant(station2, use_region=True) == 'op-21km-mo'


@pytest.mark.dbuser
def test_make_settlement_slug_variant():
    country = create_country(title='РФ', code='RF')
    region = create_region(title='MO', title_ru='MO', country=country)
    settlement1 = create_settlement(title='Екатеринбург', region=region, country=country)
    assert make_settlement_slug_variant(settlement1) == 'ekaterinburg'
    assert make_settlement_slug_variant(settlement1, use_type=True) == 'ekaterinburg'
    assert make_settlement_slug_variant(settlement1, use_country=True) == 'ekaterinburg-rf'
    assert make_settlement_slug_variant(settlement1, use_region=True) == 'ekaterinburg-mo'
    assert make_settlement_slug_variant(settlement1, use_id=True) == 'ekaterinburg-c{}'.format(settlement1.id)
    assert make_settlement_slug_variant(settlement1, use_country=True, use_region=True, use_id=True, use_type=True) == \
        'ekaterinburg-mo-rf-c{}'.format(settlement1.id)


@pytest.mark.dbuser
def test_find_by_slug():
    settlement1 = create_settlement(title='Екатеринбург', slug='ekaterinburg')
    create_settlement(title='Екатеринбург312', slug='ekaterinburg312')
    station1 = create_station(title='Пассажирский', slug='passazhirskii')
    create_station(title='Пассажирский', slug='passazhirskii-ekaterinburg')
    assert find_by_slug(settlement1.slug) == settlement1
    assert find_by_slug(station1.slug) == station1
    assert find_by_slug('pass') is None


@pytest.mark.dbuser
def test_find_all_by_slug_startswith():
    settlement1 = create_settlement(title='Екатеринбург', slug='ekaterinburg')
    settlement2 = create_settlement(title='Екатеринбург312', slug='ekaterinburg312')
    station1 = create_station(title='Пассажирский', slug='passazhirskii')
    station2 = create_station(title='Пассажирский', slug='passazhirskii-ekaterinburg')
    station3 = create_station(title='Екатер', slug='ekater')
    assert_that(find_all_by_slug_startswith('ekaterinburg'), contains_inanyorder(settlement1, settlement2))
    assert_that(find_all_by_slug_startswith('ekater'), contains_inanyorder(settlement1, settlement2, station3))
    assert_that(find_all_by_slug_startswith('pass'), contains_inanyorder(station1, station2))
    assert not find_all_by_slug_startswith('empty')


@pytest.mark.dbuser
def test_generate_slug_once_for_new_point():
    country = create_country(title='РФ', code='RF')
    region = create_region(title='MO', title_ru='MO', country=country)
    settlement1 = create_settlement(title='Екатеринбург', majority=1, slug='ekaterinburg', region=region)
    create_station(settlement=settlement1, title='Пассажирский', majority=1, slug='passazhirskii-ekaterinburg',
                   station_type_id=StationType.STATION_ID)
    settlement2 = create_settlement(title='Челябинск', majority=1, slug='cheliabinsk', region=region)
    create_station(settlement=settlement2, title='Пассажирский', majority=1, slug='passazhirskii-cheliabinsk',
                   station_type_id=StationType.STATION_ID)
    country_ug = create_country(title='Уганда', title_ru='Уганда', code='UG')
    region_ug = create_region(title='АА', title_ru='АА', country=country)

    new_settlement = create_settlement(title='Челябинск', country=country_ug, majority=5, region=region_ug)
    new_settlement2 = create_settlement(title='Челябинск', majority=5, region=region)
    new_station = create_station(settlement=settlement2, title='Пассажирский', country=country, majority=1,
                                 station_type_id=StationType.STATION_ID)
    new_station2 = create_station(settlement=settlement1, title='Екатеринбург', country=country, majority=1,
                                  station_type_id=StationType.STATION_ID)

    assert new_station.slug == 'passazhirskii-cheliabinsk-s{}'.format(new_station.id)
    assert new_settlement.slug == 'cheliabinsk-ug'
    assert new_settlement2.slug == 'cheliabinsk-mo-c{}'.format(new_settlement2.id)
    assert new_station2.slug == 'ekaterinburg-station'

    slugs = set()
    for point in chain(Settlement.objects.all(), Station.objects.all()):
        assert point.slug
        assert point.slug not in slugs
        slugs.add(point.slug)

    slug_before_edit = new_station.slug
    new_station.title = 'Шартаж2'
    new_station.save()
    new_station.refresh_from_db()

    assert slug_before_edit == new_station.slug


@pytest.mark.dbuser
def test_validation():
    settlement1 = create_settlement(title='Екатеринбург', majority=1, slug='Ekb')
    settlement1.refresh_from_db()
    assert settlement1.slug == 'ekb'

    with pytest.raises(ValidationError), transaction.atomic():
        create_settlement(title='Екатеринбург', majority=5, slug='ekb')

    with pytest.raises(ValidationError), transaction.atomic():
        create_station(title='Екатеринбург', majority=5, slug='ekb')

    with pytest.raises(ValidationError), transaction.atomic():
        create_station(title='Екатеринбург', majority=5, slug='ekb?')

    with pytest.raises(ValidationError), transaction.atomic():
        create_station(title='Екатеринбург', majority=5, slug='екб')

    with pytest.raises(ValidationError), transaction.atomic():
        create_station(title='Екатеринбург', majority=5, slug='e k b')
