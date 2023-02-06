# -*- coding: utf-8 -*-

from StringIO import StringIO
from datetime import datetime, timedelta

import pytest

from common.models.schedule import RThread
from travel.rasp.library.python.common23.date import environment
from common.utils.date import daterange
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from tester.factories import create_supplier, create_station
from tester.utils.datetime import replace_now


xml_data = u"""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="1"/>
            <station code="2" title="2"/>
        </stations>
        <threads>
            <thread title="1 - 2" t_type="bus">
              <stoppoints>
                <stoppoint station_code="1" departure_time="10:00"/>
                <stoppoint station_code="2" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
""".strip().encode('utf-8')


@replace_now(datetime(2015, 10, 1))
@pytest.mark.dbuser
@pytest.mark.parametrize('max_forward_days', [321, 121, 2])
def test_max_forward_days(max_forward_days):
    today = environment.today()

    supplier = create_supplier()
    package = create_tsi_package(supplier=supplier)
    package.tsisetting.max_forward_days = max_forward_days
    package.tsisetting.save()

    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_1', title='1')
    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_2', title='2')

    fileobj = StringIO(xml_data)
    fileobj.name = 'cysix.xml'
    package.package_file = fileobj

    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    mask = RThread.objects.get().get_mask(today=today)
    assert mask.dates(past=False) == list(daterange(today, today + timedelta(days=max_forward_days), include_end=True))
