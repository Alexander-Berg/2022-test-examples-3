# -*- coding: utf-8 -*-

from StringIO import StringIO

from common.models.geo import Country, StationMajority
from common.models.schedule import RThread
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.utils.import_file_storage import remove_schedule_temporary_today_dir
from tester.factories import create_supplier, create_station


class CysixImportTest(TestCase, CheckThreadMixin):
    @classmethod
    def teardown_class_rasp(cls):
        remove_schedule_temporary_today_dir(cls.package)

    @classmethod
    def setup_class_rasp(cls):
        supplier = create_supplier()

        ekb = create_station(title=u'test_station Екатеринбург АВ', time_zone='Asia/Yekaterinburg',
                             t_type=TransportType.BUS_ID, majority=StationMajority.IN_TABLO_ID)
        nt = create_station(title=u'test_station Нижняя Тура', time_zone='Asia/Yekaterinburg',
                            t_type=TransportType.BUS_ID, majority=StationMajority.IN_TABLO_ID)
        kach = create_station(title=u'test_station Качканар', time_zone='Asia/Yekaterinburg',
                              t_type=TransportType.BUS_ID, majority=StationMajority.IN_TABLO_ID)
        StationMapping.objects.create(station=ekb, supplier=supplier, code='all_vendor_00005', title=u'_test Екатеринбург АВ')
        StationMapping.objects.create(station=nt, supplier=supplier, code='all_vendor_00002', title=u'_test Нижняя Тура')
        StationMapping.objects.create(station=kach, supplier=supplier, code='all_vendor_00001', title=u'_test Качканар')

        cls.package = create_tsi_package(supplier=supplier, country=Country.RUSSIA_ID)
        cls.package.add_default_filters()

        cls.package.set_filter_parameter('correct_stop_time', 't_max', 1200)

        cls.package.set_filter_parameters('correct_departure_and_arrival', {
            'do_verbose_logging': True,
            't_max': 4800,
            't_super_max': 14400,
            'check': 'by_time',
            'correction': 'by_time',
        })

        factory = cls.package.get_two_stage_factory()

        fileobj = StringIO()

        fileobj.write(u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" t_type="bus" station_code_system="vendor" timezone="start_station">
  <group title="all" code="all">
    <stations>
      <station title="_test Екатеринбург АВ" code="00005">
        <legacy_station code="00005" type="raw" title="_test Екатеринбург АВ"/>
      </station>
      <station title="_test Нижняя Тура" code="">
        <legacy_station code="" type="raw" title="_test Нижняя Тура"/>
      </station>
      <station title="_test Качканар" code="00001">
        <legacy_station code="00001" type="raw" title="_test Качканар"/>
      </station>
      <station title="_test Нижняя Тура" code="00002">
        <legacy_station code="00002" type="raw" title="_test Нижняя Тура"/>
      </station>
    </stations>
    <threads>

      <thread title="Екатеринбург — Качканар" number="day_shift">
        <schedules>
          <schedule days="1234567"/>
        </schedules>
        <stoppoints>
          <stoppoint station_title="_test Екатеринбург АВ" station_code="00005" departure_time="08:00:00"/>
          <stoppoint station_title="_test Нижняя Тура" station_code="00002" departure_time="08:50:00" distance="39.6"/>
          <stoppoint station_title="_test Качканар" station_code="00001" arrival_day_shift="1" arrival_time="12:30:00" distance="77.7"/>
        </stoppoints>
      </thread>

      <thread title="Екатеринбург — Качканар" number="day_shift_2">
        <schedules>
          <schedule days="1234567"/>
        </schedules>
        <stoppoints>
          <stoppoint station_title="_test Екатеринбург АВ" station_code="00005" departure_time="08:05:00"/>
          <stoppoint station_title="_test Нижняя Тура" station_code="00002" arrival_day_shift="1" arrival_time="08:50:00" departure_time="10:50:00" departure_day_shift="1" distance="39.6"/>
          <stoppoint station_title="_test Качканар" station_code="00001" arrival_day_shift="2" arrival_time="03:30:00" distance="77.7"/>
        </stoppoints>
      </thread>

      <thread title="Екатеринбург — Качканар" number="day_shift_3">
        <schedules>
          <schedule days="1234567"/>
        </schedules>
        <stoppoints>
          <stoppoint station_title="_test Екатеринбург АВ" station_code="00005" departure_time="08:10"/>
          <stoppoint station_title="_test Нижняя Тура" station_code="00002" arrival_day_shift="1" arrival_time="08:55" departure_time="10:55" distance="39.6"/>
          <stoppoint station_title="_test Качканар" station_code="00001" arrival_day_shift="2" arrival_time="03:35" distance="77.7"/>
        </stoppoints>
      </thread>

      <thread title="Екатеринбург — Качканар" number="day_shift_4">
        <schedules>
          <schedule days="1234567"/>
        </schedules>
        <stoppoints>
          <stoppoint station_title="_test Екатеринбург АВ" station_code="00005" departure_time="08:15"/>
          <stoppoint station_title="_test Нижняя Тура" station_code="00002" arrival_day_shift="1" arrival_time="09:00" departure_time="11:00" distance="39.6"/>
          <stoppoint station_title="_test Качканар" station_code="00001" arrival_time="03:40" distance="77.7"/>
        </stoppoints>
      </thread>

    </threads>
  </group>
</channel>
        """.encode('utf-8'))

        fileobj.seek(0)
        fileobj.name = 'test_day_shift.xml'

        cls.package.package_file = fileobj

        importer = factory.get_two_stage_importer()

        importer.reimport_package()

    def testDayShift(self):
        number = 'day_shift'

        threads = RThread.objects.filter(route__two_stage_package=self.package, hidden_number=number)

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:00
        # 8:50
        # 12:30+1
        self.assertThreadStopsTimes(
            threads[0],
            [
                (None,                0,    'Asia/Yekaterinburg'),
                (49,                  50,   'Asia/Yekaterinburg'),
                (24*60 + 4*60 + 30,   None, 'Asia/Yekaterinburg'),
            ]
        )

    def testDayShift2(self):
        number = 'day_shift_2'

        threads = RThread.objects.filter(route__two_stage_package=self.package, hidden_number=number)

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:05
        # 8:50+1/10:50+1
        # 3:30+2
        self.assertThreadStopsTimes(
            threads[0],
            [
                (None,                               0,                   'Asia/Yekaterinburg'),
                (24*60 + 45,                         24*60 + 2*60 + 45,   'Asia/Yekaterinburg'),
                (2*24*60 - (8*60 + 5) + 3*60 + 30,   None,                'Asia/Yekaterinburg'),
            ]
        )

    def testDayShift3(self):
        number = 'day_shift_3'

        threads = RThread.objects.filter(route__two_stage_package=self.package, hidden_number=number)

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:10
        # 8:55+1/10:55 Отсутствие второго +! исправляется фильтром коррекции времени стоянки
        # 3:35+2
        self.assertThreadStopsTimes(
            threads[0],
            [
                (None,                               0,                   'Asia/Yekaterinburg'),
                (24*60 + 45,                         24*60 + 2*60 + 45,   'Asia/Yekaterinburg'),
                (2*24*60 - (8*60 + 5) + 3*60 + 30,   None,                'Asia/Yekaterinburg'),
            ]
        )

    def testDayShift4(self):
        number = 'day_shift_4'

        threads = RThread.objects.filter(route__two_stage_package=self.package, hidden_number=number)

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:15
        # 9:00+1/11:00 Отсутствие второго +1 исправляется фильтром коррекции времени стоянки
        # 3:40 Отсутствие +2  исправляется фильтром коррекции времен
        self.assertThreadStopsTimes(
            threads[0],
            [
                (None,                               0,                   'Asia/Yekaterinburg'),
                (24*60 + 45,                         24*60 + 2*60 + 45,   'Asia/Yekaterinburg'),
                (2*24*60 - (8*60 + 5) + 3*60 + 30,   None,                'Asia/Yekaterinburg'),
            ]
        )
