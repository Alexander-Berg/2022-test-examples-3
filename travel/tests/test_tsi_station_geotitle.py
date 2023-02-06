# -*- coding: utf-8 -*-

from django.core.files.base import ContentFile

from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportStation

from tester.testcase import TestCase
from tester.factories import create_supplier


class GeocodeTitleTSIStationTest(TestCase):
    def setUp(self):
        self.supplier = create_supplier()
        self.package = create_tsi_package(supplier=self.supplier)

        self.package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="1">
        <stations>
            <station code="1" title="Караганда" _geocode_title="Караганда для Геокодирования"/>
            <station code="2" title="Валында"/>
        </stations>
        <threads>
            <thread title="Караганда - Егиндыбулак" t_type="bus">
              <stoppoints>
                <stoppoint station_code="1" arrival_shift="" />
                <stoppoint station_code="2" arrival_shift="20"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2013-02-04" period_end_date="2013-02-18" days="1234567" times="08:30:00"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
            """.strip())

        self.factory = self.package.get_two_stage_factory()
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        super(GeocodeTitleTSIStationTest, self).setUp()

    def test_geo_title(self):
        tsi_station = TwoStageImportStation.objects.get(title=u'Караганда', package=self.package)
        assert tsi_station.geocode_title == u'Караганда для Геокодирования'
