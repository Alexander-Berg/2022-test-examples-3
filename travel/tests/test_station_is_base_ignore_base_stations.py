# -*- coding: utf-8 -*-

from StringIO import StringIO

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


cysix_template = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="B" code="B" />
      <station title="C" code="C" />
    </stations>
    <threads>
      <thread title="A - C" _ignore_base_stations="{}">
        <schedules><schedule days="1234567" /></schedules>
        <stoppoints>
          <stoppoint station_title="А" station_code="A" departure_time="08:00"/>
          <stoppoint station_title="B" station_code="B" arrival_time="09:00"/>
          <stoppoint station_title="C" station_code="C" arrival_time="10:00"/>
        </stoppoints>
      </thread>
    </threads>
  </group>
</channel>
"""


class TestStationIsBaseIgnoreBaseStations(TestCase):
    def setUp(self):
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(supplier=supplier)

        self.station_a = create_station(title=u'A')
        self.station_b = create_station(title=u'B', is_base=True)
        self.station_c = create_station(title=u'C')

        StationMapping.objects.create(supplier=supplier, station=self.station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=self.station_b, title=u'B', code=u'all_vendor_B')
        StationMapping.objects.create(supplier=supplier, station=self.station_c, title=u'C', code=u'all_vendor_C')

    def import_package(self, ignore_base_stations):
        fileobj = StringIO(cysix_template.format("1" if ignore_base_stations else "0").encode('utf-8'))
        fileobj.name = 'cysix.xml'
        self.package.package_file = fileobj

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_is_base(self):
        self.import_package(ignore_base_stations=False)

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from

    def test_is_base_ignore_base_stations(self):
        self.import_package(ignore_base_stations=True)

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from
