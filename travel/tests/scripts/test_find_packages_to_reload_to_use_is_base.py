# -*- coding: utf-8 -*-

from StringIO import StringIO
from datetime import timedelta

import pytest

from travel.rasp.admin.admin.red.metaimport import RedPackageImporter
from travel.rasp.admin.admin.red.models import MetaRouteStation, MetaRoute, Package
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TSIThreadSetting
from travel.rasp.admin.scripts.find_packages_to_reload_to_use_is_base import run, run_red
from tester.factories import create_supplier, create_station
from tester.testcase import TestCase


cysix_data = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="A" code="A" />
      <station title="B" code="B" />
      <station title="C" code="C" />
    </stations>
    <threads>
      <thread title="A - C">
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


class TestFindPackagesToReload(TestCase):
    def setUp(self):
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(supplier=supplier)

        self.station_a = create_station(title=u'A', is_base=True)
        self.station_b = create_station(title=u'B', is_base=False)
        self.station_c = create_station(title=u'C', is_base=False)

        StationMapping.objects.create(supplier=supplier, station=self.station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=self.station_b, title=u'B', code=u'all_vendor_B')
        StationMapping.objects.create(supplier=supplier, station=self.station_c, title=u'C', code=u'all_vendor_C')

        self.setting = TSIThreadSetting.objects.create(package=self.package)

    def import_package(self):
        fileobj = StringIO(cysix_data.encode('utf-8'))
        fileobj.name = 'cysix.xml'
        self.package.package_file = fileobj

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_zero_new_base(self):
        self.import_package()
        data = run()
        assert data == []

    def test_one_new_base(self):
        self.import_package()
        self.station_a.is_base_modified_at = self.package.last_import_datetime + timedelta(1)
        self.station_a.save()

        data = run()
        assert len(data) == 1
        assert data[0]['station_id'] == self.station_a.id
        assert data[0]['package_id'] == self.package.id


@pytest.mark.dbuser
def test_find_red_package_to_reload():
    base_station = create_station(is_base=True)
    red_package = Package.objects.create(title=u'Красный тестовый пакет')
    red_metaroute = MetaRoute.objects.create(
        package=red_package,
        title=u'Красный тестовый рейс',
        scheme=u'7.00',
        t_type_id=TransportType.BUS_ID,
        supplier=create_supplier(),
    )
    MetaRouteStation.objects.create(metaroute=red_metaroute, station=create_station(), arrival=None, departure=0,
                                    order=0)
    MetaRouteStation.objects.create(metaroute=red_metaroute, station=base_station, arrival=10, departure=11,
                                    order=1)
    MetaRouteStation.objects.create(metaroute=red_metaroute, station=create_station(), arrival=20, departure=None,
                                    order=2)

    importer = RedPackageImporter(red_package)
    importer.import_package()

    data = run_red()
    assert data == []

    base_station.is_base_modified_at = red_package.last_import_datetime + timedelta(1)
    base_station.save()

    data = run_red()
    assert len(data) == 1
    assert data[0]['station_id'] == base_station.id
    assert data[0]['package_id'] == red_package.id
