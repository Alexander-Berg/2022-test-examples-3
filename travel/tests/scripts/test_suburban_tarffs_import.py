# -*- coding: utf-8 -*-

import pytest

from common.models.tariffs import AeroexTariff, TariffType
from common.utils.fields import MemoryFile
from travel.rasp.admin.importinfo.models import TariffFile
from common.tester import transaction_context
from common.tester.factories import create_station
from travel.rasp.admin.scripts.tariffs.import_suburban import main as import_suburban_main


@pytest.fixture(scope='module')
@transaction_context.transaction_fixture
def test_objects(request):
    tariff_xml = '''\
<channel type="tarif" version="1">
        <tarif station1="806201" station2="806216" price="100" type="etrain" 
	    reverse="1" insearch="1" replace_tariff_type="etrain"/>
</channel>
'''

    return (
        create_station(__={'codes': {'esr': '806201'}}),
        create_station(__={'codes': {'esr': '806216'}}),
        TariffFile.objects.create(
            tariff_rich_file=MemoryFile('test.xml', 'application/xml', tariff_xml)
        ),
    )


@pytest.mark.dbuser
def test_tariff_create(test_objects):
    station_from, station_to, tarifffile = test_objects

    import_suburban_main([
        '--no-run-log', '--tarifffile-ids', str(tarifffile.id)
    ])

    try:
        tariff = AeroexTariff.objects.get(
            station_from=station_from, station_to=station_to,
            type__code='etrain', precalc=False, replace_tariff_type__code='etrain'
        )
    except AeroexTariff.DoesNotExist:
        pytest.fail('tariff not found')

    assert tariff.tariff == 100
    assert tariff.reverse
    assert tariff.suburban_search


@pytest.mark.dbuser
def test_tariff_update(test_objects):
    station_from, station_to, tarifffile = test_objects
    tariff_type = TariffType.objects.get(code='etrain')

    AeroexTariff.objects.create(
        station_from=station_from, station_to=station_to,
        type=tariff_type, precalc=False,
        tariff=10, suburban_search=False
    )

    import_suburban_main([
        '--no-run-log', '--tarifffile-ids', str(tarifffile.id)
    ])

    try:
        tariff = AeroexTariff.objects.get(
            station_from=station_from, station_to=station_to,
            type__code='etrain', precalc=False, replace_tariff_type__code='etrain'
        )
    except AeroexTariff.DoesNotExist:
        pytest.fail('tariff not found')

    assert tariff.tariff == 100
    assert tariff.reverse
    assert tariff.suburban_search
