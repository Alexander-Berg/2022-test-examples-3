# -*- coding: utf-8 -*-

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from common.models.tariffs import ThreadTariff
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.factories import create_supplier, create_station, create_currency
from tester.testcase import TestCase


xml_content = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="B" code="B" />
    </stations>
    <fares>
      <fare code="1">
        <price price="1" currency="USD">
          <stop_from station_code="A"/>
          <stop_to   station_code="B"/>
        </price>
      </fare>
    </fares>
    <threads>
      <thread title="A - B">
        <schedules><schedule days="1234567" /></schedules>
        <fares><fare code="1" days="1234567" /></fares>
        <stoppoints>
          <stoppoint station_title="А" station_code="A" departure_time="08:00"/>
          <stoppoint station_title="B" station_code="B" arrival_time="09:30"/>
        </stoppoints>
      </thread>
    </threads>
  </group>
</channel>
"""


class TestImportTariffsFlag(TestCase):
    def setUp(self):
        create_currency(code='USD')
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(supplier=supplier)

        station_a = create_station(title=u'A')
        station_b = create_station(title=u'B')

        StationMapping.objects.create(supplier=supplier, station=station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=station_b, title=u'B', code=u'all_vendor_B')

    def import_package(self, import_tariffs):
        self.package.package_file = ContentFile(name='cysix.xml', content=xml_content)
        self.package.tsisetting.import_tariffs = import_tariffs
        self.package.tsisetting.save()

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_import_tariffs(self):
        self.import_package(import_tariffs=True)

        thread = RThread.objects.get()
        assert ThreadTariff.objects.filter(thread_uid=thread.uid).exists()

    def test_dont_import_tariffsr(self):
        self.import_package(import_tariffs=False)

        thread = RThread.objects.get()
        assert not ThreadTariff.objects.filter(thread_uid=thread.uid).exists()
