# -*- coding: utf-8 -*-

from StringIO import StringIO

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package, create_cysix_group_filter, create_trusted_station
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.trusted_station import TrustedStation
from travel.rasp.admin.importinfo.models.two_stage_import import CysixGroupFilter
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


class TestStationIsTrusted(TestCase):
    def setUp(self):
        supplier = create_supplier(code='test_supplier')
        self.package = create_tsi_package(supplier=supplier)

        self.station_a = create_station(title=u'A')
        self.station_b = create_station(title=u'B')
        self.station_c = create_station(title=u'C')

        StationMapping.objects.create(supplier=supplier, station=self.station_a, title=u'A', code=u'all_vendor_A')
        StationMapping.objects.create(supplier=supplier, station=self.station_b, title=u'B', code=u'all_vendor_B')
        StationMapping.objects.create(supplier=supplier, station=self.station_c, title=u'C', code=u'all_vendor_C')

    def import_package(self):
        fileobj = StringIO(cysix_data.encode('utf-8'))
        fileobj.name = 'cysix.xml'
        self.package.package_file = fileobj

        importer = self.package.get_two_stage_factory().get_two_stage_importer()
        importer.reimport_package()

    def test_is_trusted_first(self):
        """ Если опорный автовокзал первый, то признаки выставлять не надо """

        self.station_a.is_base = True
        self.station_b.is_base = False
        self.station_c.is_base = False
        self.station_a.save()
        self.station_b.save()
        self.station_c.save()
        self.trusted_station = create_trusted_station(station=self.station_a, tsi_package=self.package)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from

    def test_is_trusted_after_base(self):
        """ с базового автовокзала признаки False, с опорного True, начальный не базовый хвостик берется в поиск"""

        self.station_a.is_base = False
        self.station_b.is_base = True
        self.station_c.is_base = True
        self.station_a.save()
        self.station_b.save()
        self.station_c.save()
        self.trusted_station = create_trusted_station(station=self.station_c, tsi_package=self.package)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from

    def test_is_trusted_last(self):
        """ Если базовый автовокзал первый, но есть опорные в пакете, то начало нитки не берется в поиск """

        self.station_a.is_base = True
        self.station_b.is_base = False
        self.station_c.is_base = True
        self.station_a.save()
        self.station_b.save()
        self.station_c.save()
        self.trusted_station = create_trusted_station(station=self.station_c, tsi_package=self.package)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert not path[0].in_station_schedule
        assert not path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from

    def test_is_trusted_with_group(self):
        """ Если базовый автовокзал первый, но есть опорные в пакете, то начало нитки не берется в поиск """

        self.station_a.is_base = True
        self.station_b.is_base = False
        self.station_c.is_base = True
        self.station_a.save()
        self.station_b.save()
        self.station_c.save()

        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')

        self.trusted_station = create_trusted_station(station=self.station_c, tsi_package=self.package,
                                                      tsi_package_group=self.tsi_group)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert not path[0].in_station_schedule
        assert not path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from

    def test_new_trusted_another_group_anther_package(self):
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.package.new_trusted = True
        self.package.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')
        another_package = create_tsi_package()
        tsi_group = create_cysix_group_filter(package=self.package, code='allb', title='allb')

        # другая группа в этом пакете
        self.trusted_station = create_trusted_station(
            station=self.station_a, tsi_package=self.package, tsi_package_group=tsi_group
        )
        # этот пакет
        self.trusted_station = create_trusted_station(station=self.station_b, tsi_package=self.package)
        # другой пакет
        self.trusted_station = create_trusted_station(station=self.station_c, tsi_package=another_package)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert not path[0].in_station_schedule
        assert not path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from

    def test_new_trusted_current_group_middle(self):
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.package.new_trusted = True
        self.package.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')
        tsi_group = create_cysix_group_filter(package=self.package, code='allb', title='allb')

        # другая группа в этом пакете
        self.trusted_station = create_trusted_station(
            station=self.station_a, tsi_package=self.package, tsi_package_group=tsi_group
        )
        # текущая группа этом пакете
        self.trusted_station = create_trusted_station(
            station=self.station_b, tsi_package=self.package, tsi_package_group=self.tsi_group
        )
        self.import_package()

        path = list(RThread.objects.get().path)
        assert not path[0].in_station_schedule
        assert not path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert path[2].in_station_schedule
        assert path[2].is_searchable_from

    def test_new_trusted_current_group_first(self):
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.package.new_trusted = True
        self.package.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')
        another_package = create_tsi_package()

        # текущая группа этом пакете
        self.trusted_station = create_trusted_station(
            station=self.station_a, tsi_package=self.package, tsi_package_group=self.tsi_group
        )
        # другой пакет
        self.trusted_station = create_trusted_station(station=self.station_c, tsi_package=another_package)
        self.import_package()

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from

    def test_new_trusted_another_group_end(self):
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.package.new_trusted = True
        self.package.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')
        another_package = create_tsi_package()
        tsi_group = create_cysix_group_filter(package=another_package, code='allb', title='allb')

        # группа в другом пакете
        self.trusted_station = create_trusted_station(
            station=self.station_c, tsi_package=another_package, tsi_package_group=tsi_group
        )

        self.import_package()

        path = list(RThread.objects.get().path)
        assert path[0].in_station_schedule
        assert path[0].is_searchable_from
        assert path[1].in_station_schedule
        assert path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from

    def test_new_trusted_another_package_first(self):
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.package.new_trusted = True
        self.package.save()

        self.tsi_group = create_cysix_group_filter(package=self.package, code='all', title='all')
        another_package = create_tsi_package()

        # другой пакет
        self.trusted_station = create_trusted_station(station=self.station_a, tsi_package=another_package)

        self.import_package()

        path = list(RThread.objects.get().path)
        assert not path[0].in_station_schedule
        assert not path[0].is_searchable_from
        assert not path[1].in_station_schedule
        assert not path[1].is_searchable_from
        assert not path[2].in_station_schedule
        assert not path[2].is_searchable_from


class TestTrustedStationsQueries(TestCase):
    def test_new_trusted_stations(self):
        station_1 = create_station(title='1', id=545)
        station_2 = create_station(title='2', id=546)
        station_3 = create_station(title='3', id=547)
        package_1 = create_tsi_package()
        package_2 = create_tsi_package()
        tsi_group_1 = create_cysix_group_filter(package=package_1, code='1', title='1')
        create_trusted_station(station=station_1, tsi_package=package_1)
        create_trusted_station(station=station_2, tsi_package=package_1, tsi_package_group=tsi_group_1)
        create_trusted_station(station=station_3, tsi_package=package_2)

        assert package_1.new_trusted_stations(tsi_group_1) == {545, 546}
        assert package_1.new_not_trusted_stations(tsi_group_1) == {547}
        assert package_1.new_trusted_stations() == {545}
        assert package_1.new_not_trusted_stations() == {546, 547}

        assert package_2.new_trusted_stations() == {547}
        assert package_2.new_not_trusted_stations() == {545, 546}
