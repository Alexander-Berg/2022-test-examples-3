# -*- coding: utf-8 -*-

from datetime import datetime

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from tester.testcase import TestCase
from tester.factories import create_supplier, create_station
from tester.utils.datetime import replace_now


class ReplaceLocalTimeByShift(TestCase):
    @replace_now(datetime(2015, 9, 15))
    def test_replace_local_time_by_shift(self):
        supplier = create_supplier()
        package = create_tsi_package(supplier=supplier)
        package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="in_russia"/>
            <station code="2" title="in_finland"/>
        </stations>
        <threads>
            <thread title="R-F" t_type="bus" number="RF">
              <stoppoints>
                <stoppoint station_code="1" departure_time="10:00"/>
                <stoppoint station_code="2" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2015-09-01" period_end_date="2015-11-31" days="1234567"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
            """.strip())

        station_rus = create_station(time_zone='Europe/Moscow', title='rus', settlement=None)
        station_fin = create_station(time_zone='Europe/Helsinki', title='fin', settlement=None)

        StationMapping.objects.create(supplier=supplier, station=station_rus, code='1_vendor_1', title='in_russia')
        StationMapping.objects.create(supplier=supplier, station=station_fin, code='1_vendor_2', title='in_finland')

        factory = package.get_two_stage_factory()
        importer = factory.get_two_stage_importer()
        importer.reimport_package()

        thread = RThread.objects.get(hidden_number='RF')

        rtstations = list(thread.path)

        assert rtstations[0].station.time_zone == 'Europe/Moscow'
        assert rtstations[1].station.time_zone == 'Europe/Helsinki'

        assert rtstations[0].time_zone == 'Europe/Moscow'
        assert rtstations[1].time_zone == 'Europe/Helsinki'

        assert rtstations[0].tz_departure == 0
        assert rtstations[1].tz_arrival == 10 * 60
