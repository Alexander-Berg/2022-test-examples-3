# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime

import pytest
from django.core.files.base import ContentFile

from common.models.schedule import RThread, Route
from common.tester.factories import create_supplier, create_station, create_route
from common.tester.utils.datetime import replace_now
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping


def prepare_package():
    supplier = create_supplier()
    package = create_tsi_package(supplier=supplier, tsi_settings={'set_number': True})
    package.package_file = ContentFile(name=u'cysix.xml', content="""
    <?xml version='1.0' encoding='utf-8'?>
    <channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="station_a"/>
            <station code="2" title="station_b"/>
        </stations>
        <threads>
            <thread title="Forward - Route" t_type="bus" number="forward">
              <stoppoints>
                <stoppoint station_code="1" departure_time="10:00"/>
                <stoppoint station_code="2" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
            <thread title="Backward - Route" t_type="bus" number="backward">
              <stoppoints>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="1" departure_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
        </threads>
    </group>
    </channel>
            """.strip())

    station_a = create_station()
    station_b = create_station()

    StationMapping.objects.create(supplier=supplier, station=station_a, code='1_vendor_1', title='station_a')
    StationMapping.objects.create(supplier=supplier, station=station_b, code='1_vendor_2', title='station_b')

    return supplier, package


@replace_now(datetime(2015, 9, 15))
@pytest.mark.dbuser
def test_duplicate_import_uid():
    supplier, package = prepare_package()

    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    assert Route.objects.filter(rthread__number='backward').exists()
    assert Route.objects.filter(rthread__number='forward').exists()
    importer.reimport_package()
    assert Route.objects.filter(rthread__number='backward').exists()
    assert Route.objects.filter(rthread__number='forward').exists()

    route_uid = Route.objects.get(two_stage_package=package, rthread__number='forward').route_uid
    Route.objects.all().delete()

    supplier_otheroute = create_route(route_uid=route_uid, supplier=supplier)
    importer.reimport_package()
    assert not Route.objects.filter(two_stage_package=package).exists()
    supplier_otheroute.delete()

    other_package = create_tsi_package(supplier=supplier)
    create_route(route_uid=route_uid, supplier=supplier, two_stage_package=other_package)
    importer.reimport_package()
    assert not Route.objects.filter(two_stage_package=package).exists()
