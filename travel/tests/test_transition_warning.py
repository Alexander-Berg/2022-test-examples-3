# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import, division, print_function

import datetime

from django.core.files.base import ContentFile

from common.models.geo import StationMajority
from common.models.schedule import RThread
from common.models.transport import TransportType
from common.tester.factories import create_station
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


WARNING_XML = """<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="g1">
        <stations>
            <!-- Europe/Moscow UTC Asia/Yekaterinburg -->
            <station code="1" title="Начало"/>
            <station code="2" title="Конец"/>
            <station code="3" title="Еще дальше"/>
        </stations>
        <threads>

            <thread title="Караганда - Егиндыбулак" t_type="bus" number="102">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01"/>
              </schedules>
            </thread>

        </threads>
    </group>
</channel>
"""

NO_WARNING_XML = """<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="g1">
        <stations>
            <!-- Europe/Moscow UTC Asia/Yekaterinburg -->
            <station code="1" title="Начало"/>
            <station code="2" title="Конец"/>
            <station code="3" title="Еще дальше"/>
        </stations>
        <threads>
            <!-- Нет варнинга так, как для нитки переопределена временная зона -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="102" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как для стоппоинтов переопределена временная зона -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="103">
              <stoppoints>
                <stoppoint station_code="1" timezone="Asia/Yekaterinburg"/>
                <stoppoint station_code="2" arrival_time="10:00" timezone="Asia/Yekaterinburg"/>
                <stoppoint station_code="3" arrival_time="20:00" timezone="Asia/Yekaterinburg"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как для стоппоинтов переопределена временная зона, первый тоже МСК -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="104">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00" timezone="Europe/Moscow"/>
                <stoppoint station_code="3" arrival_time="20:00" timezone="Europe/Moscow"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как есть конкретные дни хождения без перевода часов-->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="105" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="2014-10-10;2014-10-20" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как есть конкретные дни хождения без перевода часов -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="106" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="2014-10-20" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как есть конкретные дни хождения без перевода часов -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="107" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="2014-10-28;2015-02-28" times="00:01"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как есть ограничение на дни хождения без перевода часов -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="108" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01" period_start_date="2014-08-01" period_end_date="2014-10-25"/>
              </schedules>
            </thread>

            <!-- Нет варнинга так, как есть ограничение на дни хождения без перевода часов -->
            <thread title="Караганда - Егиндыбулак" t_type="bus" number="109" timezone="Asia/Yekaterinburg">
              <stoppoints>
                <stoppoint station_code="1"/>
                <stoppoint station_code="2" arrival_time="10:00"/>
                <stoppoint station_code="3" arrival_time="20:00"/>
              </stoppoints>
              <schedules>
                <schedule days="1234567" times="00:01" period_start_date="2014-10-28" period_end_date="2015-02-25"/>
              </schedules>
            </thread>

        </threads>
    </group>
</channel>
"""


class CysixTransitionWarning(TestCase, LogHasMessageMixIn):
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
        self.package.tsisetting.max_forward_days = 90
        self.package.tsisetting.save()
        self.factory = self.package.get_two_stage_factory()

        super(CysixTransitionWarning, self).setUp()

    @replace_now(datetime.datetime(2014, 10, 1))
    def testWarning(self):
        self.package.package_file = ContentFile(name='warning.xml', content=WARNING_XML)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(threads.count(), 1)

        assert self.log_has_this_in_messages('После перевода времени 2014-10-26 изменится время в пути')

    @replace_now(datetime.datetime(2014, 10, 1))
    def testNoWarning(self):
        self.package.package_file = ContentFile(name='no_warning.xml', content=NO_WARNING_XML)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(threads.count(), 8)

        assert self.log_does_not_have_this_in_messages('После перевода времени 2014-10-26 изменится время в пути')
