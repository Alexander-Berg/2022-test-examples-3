# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import json

import mock
import os
import pytest
from hamcrest import assert_that, has_entries, contains, contains_inanyorder

from travel.rasp.admin.scripts.export.export_bus_station_codes import main
from tester.factories import create_station, create_supplier
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.scripts.utils.file_wrapper.registry import FileType, FILE_PATH


def get_result(tmpdir):
    path = os.path.join(str(tmpdir), 'bus_station_codes.json')

    with mock.patch.dict(FILE_PATH, {FileType.BUS_STATION_CODES: path}):
        main()

    with open(path) as f:
        return json.load(f)


@pytest.mark.dbuser
def test_basic(tmpdir):
    supplier = create_supplier()
    station = create_station()
    StationMapping.objects.create(supplier=supplier, station=station, code='default_vendor_code', title='Станция')
    result = get_result(tmpdir)

    assert result == {
        'suppliers': [{
            'id': supplier.id,
            'station_codes': [{
                'code': 'code',
                'titles': ['Станция'],
                'station_id': station.id,
            }]
        }]
    }


@pytest.mark.dbuser
def test_non_vendor_skip(tmpdir):
    StationMapping.objects.create(supplier=create_supplier(), station=create_station(), code='bad_code')
    result = get_result(tmpdir)

    assert result == {'suppliers': []}


@pytest.mark.dbuser
def test_legacy_skip(tmpdir):
    StationMapping.objects.create(supplier=create_supplier(), station=create_station(),
                                  code='default_vendor_code_legacy_123')
    result = get_result(tmpdir)

    assert result == {'suppliers': []}


@pytest.mark.dbuser
def test_duplicate_station_skip(tmpdir):
    supplier_1 = create_supplier()
    supplier_2 = create_supplier()
    station = create_station()
    StationMapping.objects.create(supplier=supplier_1, station=station, code='default_vendor_code_1', title='Станция')
    StationMapping.objects.create(supplier=supplier_1, station=station, code='default_vendor_code_2', title='Станция')
    StationMapping.objects.create(supplier=supplier_2, station=station, code='default_vendor_code', title='Станция')
    result = get_result(tmpdir)

    assert result == {
        'suppliers': [{
            'id': supplier_2.id,
            'station_codes': [{
                'code': 'code',
                'titles': ['Станция'],
                'station_id': station.id
            }]
        }]
    }


@pytest.mark.dbuser
def test_duplicate_code_skip(tmpdir):
    supplier_1 = create_supplier()
    supplier_2 = create_supplier()
    station = create_station()
    StationMapping.objects.create(supplier=supplier_1, station=create_station(), code='default_vendor_code', title='Станция')
    StationMapping.objects.create(supplier=supplier_1, station=create_station(), code='default_vendor_code', title='Станция')
    StationMapping.objects.create(supplier=supplier_2, station=station, code='default_vendor_code', title='Станция')
    result = get_result(tmpdir)

    assert result == {
        'suppliers': [{
            'id': supplier_2.id,
            'station_codes': [{
                'code': 'code',
                'titles': ['Станция'],
                'station_id': station.id
            }]
        }]
    }


@pytest.mark.dbuser
def test_duplicate_titles(tmpdir):
    supplier_1 = create_supplier()
    supplier_2 = create_supplier()
    station_1 = create_station()
    station_2 = create_station()
    StationMapping.objects.create(supplier=supplier_1, station=station_1, code='default_vendor_code_1', title='Станция')
    StationMapping.objects.create(supplier=supplier_1, station=station_1, code='default_vendor_code_1', title='Станция')
    StationMapping.objects.create(supplier=supplier_1, station=station_2, code='default_vendor_code_2', title='Станция')
    StationMapping.objects.create(supplier=supplier_2, station=station_1, code='default_vendor_code', title='Станция')
    result = get_result(tmpdir)

    assert_that(result, has_entries({
        'suppliers': contains_inanyorder(
            has_entries({
                'id': supplier_1.id,
                'station_codes': contains_inanyorder({
                    'code': 'code_1',
                    'titles': ['Станция'],
                    'station_id': station_1.id
                }, {
                    'code': 'code_2',
                    'titles': ['Станция'],
                    'station_id': station_2.id
                })
            }),
            has_entries({
                'id': supplier_2.id,
                'station_codes': [{
                    'code': 'code',
                    'titles': ['Станция'],
                    'station_id': station_1.id
                }]
            })
        )
    }))


@pytest.mark.dbuser
def test_distinct_titles(tmpdir):
    supplier = create_supplier()
    station = create_station()
    StationMapping.objects.create(supplier=supplier, station=station, code='default_vendor_code_1', title='Станция 1')
    StationMapping.objects.create(supplier=supplier, station=station, code='default_vendor_code_1', title='Станция 2')
    result = get_result(tmpdir)

    assert_that(result, has_entries({
        'suppliers': contains(
            has_entries({
                'id': supplier.id,
                'station_codes': contains(has_entries({
                    'code': 'code_1',
                    'titles': contains_inanyorder('Станция 1', 'Станция 2'),
                    'station_id': station.id
                }))
            })
        )
    }))
