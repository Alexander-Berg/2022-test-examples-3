# -*- coding: utf-8 -*-
from StringIO import StringIO

from common.models.schedule import RThread
from common.models.transport import TransportSubtype, TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


cysix_template = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="{t_type}" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="B" code="B" />
    </stations>
    <threads>
      <thread title="A - B">
        <schedules><schedule days="1234567" /></schedules>
        <stoppoints>
          <stoppoint station_title="А" station_code="A" departure_time="08:00"/>
          <stoppoint station_title="B" station_code="B" arrival_time="09:30"/>
        </stoppoints>
      </thread>
    </threads>
  </group>
</channel>
"""


class TestSeaRiver(TestCase):
    def setUp(self):
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(title=u'test', supplier=supplier)

        station_a = create_station(title=u'A')
        station_b = create_station(title=u'B')

        StationMapping.objects.create(supplier=supplier, station=station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=station_b, title=u'B', code=u'all_vendor_B')

    def import_package(self, t_type):
        fileobj = StringIO(cysix_template.format(t_type=t_type).encode('utf-8'))
        fileobj.name = 'cysix.xml'
        self.package.package_file = fileobj

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_sea(self):
        self.import_package(t_type='sea')

        thread = RThread.objects.get()
        assert thread.t_type_id == TransportType.WATER_ID
        assert thread.t_subtype_id == TransportSubtype.SEA_ID

    def test_river(self):
        self.import_package(t_type='river')

        thread = RThread.objects.get()
        assert thread.t_type_id == TransportType.WATER_ID
        assert thread.t_subtype_id == TransportSubtype.RIVER_ID
