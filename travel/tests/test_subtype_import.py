# -*- coding: utf-8 -*-
from StringIO import StringIO

from common.models.schedule import RThread
from common.models.transport import TransportSubtype, TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


cysix_template = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" subtype="{global_subtype}" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="B" code="B" />
    </stations>
    <threads>
      <thread title="A - B" subtype="{local_subtype}">
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


SUBTYPE_GAZEL = 'gazel'
SUBTYPE_PAZIK = 'pazik'
SUBTYPE_VAGON = 'vagon'


class TestSubtype(TestCase):
    def setUp(self):
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(supplier=supplier)

        station_a = create_station(title=u'A')
        station_b = create_station(title=u'B')

        StationMapping.objects.create(supplier=supplier, station=station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=station_b, title=u'B', code=u'all_vendor_B')

        t_bus = TransportType.objects.get(code='bus')
        TransportSubtype.objects.create(code=SUBTYPE_GAZEL, title_ru=u'Газель', t_type=t_bus)
        TransportSubtype.objects.create(code=SUBTYPE_PAZIK, title_ru=u'Пазик', t_type=t_bus)
        TransportSubtype.objects.create(code=SUBTYPE_VAGON, title_ru=u'Пазик',
                                        t_type=TransportType.objects.get(code='train'))

    def import_package(self, global_subtype='', local_subtype=''):
        fileobj = StringIO(
            cysix_template.format(global_subtype=global_subtype, local_subtype=local_subtype).encode('utf-8')
        )
        fileobj.name = 'cysix.xml'
        self.package.package_file = fileobj

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_global_subtype(self):
        self.import_package(global_subtype=SUBTYPE_GAZEL)

        thread = RThread.objects.get()
        assert thread.t_subtype_id
        assert thread.t_subtype.code == SUBTYPE_GAZEL

    def test_local_subtype(self):
        self.import_package(global_subtype=SUBTYPE_GAZEL, local_subtype=SUBTYPE_PAZIK)

        thread = RThread.objects.get()
        assert thread.t_subtype_id
        assert thread.t_subtype.code == SUBTYPE_PAZIK

    def test_only_local_subtype(self):
        self.import_package(local_subtype=SUBTYPE_PAZIK)

        thread = RThread.objects.get()
        assert thread.t_subtype_id
        assert thread.t_subtype.code == SUBTYPE_PAZIK

    def test_no_subtype(self):
        self.import_package()

        thread = RThread.objects.get()
        assert thread.t_subtype_id is None

    def test_invalid_subtype(self):
        self.import_package(local_subtype=SUBTYPE_VAGON)
        thread = RThread.objects.get()
        assert thread.t_subtype_id is None
