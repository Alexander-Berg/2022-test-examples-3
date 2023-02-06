# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

from functools import partial

import pytest
import six

import yt.wrapper as yt
from hamcrest import assert_that, has_entries

from travel.avia.admin.avia_scripts.build_yt_reference import build_station_queryset, build_export_models
from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_company, create_station


pytestmark = [pytest.mark.dbuser]


def create_db_station(id, t_type=TransportType.PLANE_ID, iata=None, icao=None, sirena=None):
    station_spec = {
        'id': id,
        't_type': t_type,
        '__': {'codes': {}},
    }
    if iata:
        station_spec['__']['codes']['iata'] = iata
    if icao:
        station_spec['__']['codes']['icao'] = icao
    if sirena:
        station_spec['__']['codes']['sirena'] = sirena
    return create_station(**station_spec)


create_not_avia_station = partial(create_db_station, t_type=TransportType.BUS_ID)


@pytest.mark.parametrize('stations_data, expected_ids', (
    (
        [dict(id=10)], []
    ),
    (
        [dict(id=1, iata='iata_1')], [1]
    ),
    (
        [dict(id=2, icao='icao_2')], [2]
    ),
    (
        [dict(id=3, sirena='sirena_3')], [3]
    ),
    (
        [dict(id=5, iata='iata_5', icao='icao_5', sirena='sirena_5')], [5]
    ),
    (
        [
            dict(id=10),
            dict(id=5, iata='iata_5', icao='icao_5', sirena='sirena_5'),
        ],
        [5]
    ),
    (
        [
            dict(id=10),
            dict(id=1, iata='iata_1'),
            dict(id=2, icao='icao_2'),
            dict(id=3, sirena='sirena_3'),
            dict(id=5, iata='iata_5', icao='icao_5', sirena='sirena_5'),
        ],
        [1, 2, 3, 5]
    ),
))
def test_build_station_queryset_not_avia_stations(stations_data, expected_ids):
    Station.objects.all().delete()
    for data in stations_data:
        create_not_avia_station(**data)

    qs = build_station_queryset()
    selected_station_ids = list(qs.values_list('id', flat=True))

    assert sorted(selected_station_ids) == sorted(expected_ids)


def _build_yt_reference_for(entities_to_update):
    models = build_export_models()
    for model, objects in six.iteritems(models):
        if model.model.__name__.lower() in entities_to_update:
            model.save(yt, objects)
            return model.yt_path


def test_build_company_reference():
    Company.objects.all().delete()
    company_spec = dict(
        id=100500,
        registration_url='https://example.com/',
        registration_url_ru='https://example.com/ru/',
        registration_url_en='https://example.com/en/',
        registration_url_tr='https://example.com/tr/',
        registration_url_uk='https://example.com/uk',
    )
    create_company(t_type_id=TransportType.PLANE_ID, **company_spec)

    entities_to_update = ['company']
    yt_path = _build_yt_reference_for(entities_to_update)

    yt_records = list(yt.read_table(yt_path, raw=False))
    assert len(yt_records) == 1
    assert_that(yt_records[0], has_entries(**company_spec))
