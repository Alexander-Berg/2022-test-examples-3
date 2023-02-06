# -*- coding: utf-8 -*-

from django.core.files.base import ContentFile

from common.models.geo import StationMajority
from common.models.transport import TransportType
from cysix.models import Filter, PackageFilter
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportThread, TSIThreadStationFlag
from tester.factories import create_station
from tester.testcase import TestCase


xml_content = u"""<?xml version='1.0' encoding='utf8'?>
<channel t_type="bus" version="1.0" station_code_system="vendor" timezone="start_station">
  <group code="g1">
    <stations>
      <station code="1" title="Начало" />
      <station code="2" title="Конец" />
      <station code="3" title="Еще дальше" />
    </stations>
    <threads>

      <thread title="t-title">
        <stoppoints>
          <stoppoint station_title="Начало" station_code="1" departure_shift="0"/>
          <stoppoint station_title="Конец" station_code="2" departure_shift="600"/>
          <stoppoint station_title="Еще дальше" station_code="3" arrival_shift="1200"/>
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="12" times="01:00:"/>
        </schedules>
      </thread>

     </threads>
  </group>
</channel>"""


class TestSetDefaultFlags(TestCase):
    @classmethod
    def setup_class_rasp(cls):
        cls.package = create_tsi_package()
        first_station = create_station(t_type=TransportType.PLANE_ID, title=u'Начало',
                                       majority=StationMajority.IN_TABLO_ID, time_zone='Europe/Moscow')
        second_station = create_station(t_type=TransportType.BUS_ID, title=u'Конец',
                                        majority=StationMajority.NOT_IN_TABLO_ID, time_zone='UTC')
        last_station = create_station(t_type=TransportType.BUS_ID, title=u'Еще дальше',
                                      majority=StationMajority.NOT_IN_TABLO_ID, time_zone='Asia/Yekaterinburg')
        StationMapping.objects.create(station=first_station, code='g1_vendor_1', title=u'Начало',
                                      supplier=cls.package.supplier)
        StationMapping.objects.create(station=second_station, code='g1_vendor_2', title=u'Конец',
                                      supplier=cls.package.supplier)
        StationMapping.objects.create(station=last_station, code='g1_vendor_3', title=u'Еще дальше',
                                      supplier=cls.package.supplier)

        cls.package.package_file = ContentFile(name='cysix.xml', content=xml_content)

        filter_ = Filter.objects.get(code='set_default_flags')

        PackageFilter.objects.get_or_create(
            package=cls.package,
            filter=filter_,
            defaults={
                'parameters': filter_.default_parameters,
                'use': filter_.default_use,
            }
        )

    def setUp(self):
        self.factory = self.package.get_two_stage_factory()

    def testFilterOff(self):
        self.package.set_filter_use('set_default_flags', False)

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        self.assertEqual(TwoStageImportThread.objects.filter(package=self.package).count(), 1)

        # Нет предустановленных флагов
        self.assertEqual(TSIThreadStationFlag.objects.filter(package=self.package).count(), 0)

    def testFilterOn(self):
        self.package.set_filter_use('set_default_flags', True)

        self.package.set_filter_parameters('set_default_flags', {
            'is_fuzzy': 'false',
            'is_searchable_from': 'true',
            'is_searchable_to': 'false',
            'in_station_schedule': 'true',
        })

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package_into_middle_base()

        self.assertEqual(TwoStageImportThread.objects.filter(package=self.package).count(), 1)

        tsi_thread_station_flags = list(TSIThreadStationFlag.objects.filter(package=self.package))

        self.assertEqual(len(tsi_thread_station_flags), 3)

        for flag in tsi_thread_station_flags:
            self.assertEqual(flag.is_fuzzy, False)
            self.assertEqual(flag.is_searchable_from, True)
            self.assertEqual(flag.is_searchable_to, False)
            self.assertEqual(flag.in_station_schedule, True)
