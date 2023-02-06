# -*- coding: utf-8 -*-

from django.core.files.base import ContentFile

from common.models.geo import Station, StationMajority
from common.models.transport import TransportType
from cysix.models import Filter, PackageFilter
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportThread
from tester.factories import create_station
from tester.testcase import TestCase


xml_content = u"""<?xml version='1.0' encoding='utf8'?>
<channel t_type="bus" version="1.0" station_code_system="vendor" timezone="start_station">
  <group code="g1">
    <stations>
      <station code="1" title="Начало" />
      <station code="2" title="Конец" />
      <station code="3" title="Новое название для Еще дальше" />
    </stations>
    <threads>

      <thread title="t-title" number="always-good-thread">
        <stoppoints>
          <stoppoint station_title="Начало" station_code="1" departure_shift="0"/>
          <stoppoint station_title="Конец" station_code="2" arrival_shift="600"/>
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="12" times="01:00:"/>
        </schedules>
      </thread>

      <thread title="t-title" number="good-only-if-allow-station-mapping-by-code">
        <stoppoints>
          <stoppoint station_title="Новое название для Еще дальше" station_code="3" arrival_shift="600"/>
          <stoppoint station_title="Начало" station_code="1" departure_shift="0"/>
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="34" times="02:00"/>
        </schedules>
      </thread>

     </threads>
  </group>
</channel>"""


class AllowStationMappingByCode(TestCase):
    def setUp(self):
        self.package = create_tsi_package()
        first_station = create_station(t_type=TransportType.PLANE_ID, title=u'Начало',
                                       majority=StationMajority.IN_TABLO_ID, time_zone='Europe/Moscow')
        second_station = create_station(t_type=TransportType.BUS_ID, title=u'Конец',
                                        majority=StationMajority.NOT_IN_TABLO_ID, time_zone='UTC')
        last_station = create_station(t_type=TransportType.BUS_ID, title=u'Еще дальше',
                                      majority=StationMajority.NOT_IN_TABLO_ID, time_zone='Asia/Yekaterinburg')
        StationMapping.objects.create(station=first_station, code='g1_vendor_1', title=u'Начало',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=second_station, code='g1_vendor_2', title=u'Конец',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=last_station, code='g1_vendor_3', title=u'Еще дальше',
                                      supplier=self.package.supplier)

        self.factory = self.package.get_two_stage_factory()
        self.package.package_file = ContentFile(name=u'cysix.xml', content=xml_content)

        filter_ = Filter.objects.get(code='allow_station_mapping_by_code')
        PackageFilter.objects.get_or_create(
            package=self.package,
            filter=filter_,
            defaults={
                'parameters': filter_.default_parameters,
                'use': filter_.default_use,
            }
        )

        super(AllowStationMappingByCode, self).setUp()

    def testFilterOff(self):
        self.package.set_filter_use('allow_station_mapping_by_code', False)

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        tsi_threads = list(TwoStageImportThread.objects.filter(package=self.package))

        self.assertEqual(len(tsi_threads), 2)

        numbers_of_mapped_stations_in_threads = set(len(tsi_thread.get_stations()) for tsi_thread in tsi_threads)
        self.assertEqual(numbers_of_mapped_stations_in_threads, {1, 2})

    def testFilterOn(self):
        self.package.set_filter_use('allow_station_mapping_by_code', True)

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        tsi_threads = list(TwoStageImportThread.objects.filter(package=self.package))

        self.assertEqual(len(tsi_threads), 2)

        numbers_of_mapped_stations_in_threads = set(len(tsi_thread.get_stations()) for tsi_thread in tsi_threads)
        self.assertEqual(numbers_of_mapped_stations_in_threads, {2})

    def testFilterOnButHas2DifferentMappings(self):
        self.package.set_filter_use('allow_station_mapping_by_code', True)

        mapping = StationMapping.objects.get(title=u'Еще дальше')

        mapping.pk = None
        mapping.title = u'Еще дальше 2'
        mapping.station = Station.objects.get(title=u'Начало')

        mapping.save()

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        tsi_threads = list(TwoStageImportThread.objects.filter(package=self.package))

        self.assertEqual(len(tsi_threads), 2)

        numbers_of_mapped_stations_in_threads = set(len(tsi_thread.get_stations()) for tsi_thread in tsi_threads)
        self.assertEqual(numbers_of_mapped_stations_in_threads, {1, 2})
