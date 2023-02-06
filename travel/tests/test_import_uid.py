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


class TestImportUid(TestCase):
    @replace_now(datetime(2015, 9, 15))
    def test_import_uid(self):
        supplier = create_supplier(id=88)
        package = create_tsi_package(supplier=supplier)
        package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="station_a"/>
            <station code="2" title="station_b"/>
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

        station_a = create_station(id=9801)
        station_b = create_station(id=9802)

        StationMapping.objects.create(supplier=supplier, station=station_a, code='1_vendor_1', title='station_a')
        StationMapping.objects.create(supplier=supplier, station=station_b, code='1_vendor_2', title='station_b')

        factory = package.get_two_stage_factory()
        importer = factory.get_two_stage_importer()
        importer.reimport_package()

        thread = RThread.objects.get()
        expected_uid = 'empty_f9801t9802_88-1-0c94c351e7da25ebe7d16db1fca81388-b189ed243e4f9b5668cf8d5d6bf63f4b'
        assert thread.import_uid == expected_uid, (
            u'Поменялась логика генерации import_uid! '
            u'Правьте данный assert, только если логика генерации import_uid была изменена сознательно.'
        )

        original_import_uid = thread.import_uid
        assert original_import_uid == thread.gen_import_uid(), u'При перегенерации import_uid должен сохранятся'
