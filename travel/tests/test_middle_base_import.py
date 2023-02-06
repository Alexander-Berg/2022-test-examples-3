# -*- coding: utf-8 -*-

from django.core.files.base import ContentFile

from common.models.geo import Station
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportThread, TwoStageImportStation
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn
from tester.factories import create_station
from tester.testcase import TestCase


xml_content = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="group_code_1">
        <stations>
            <!-- vendor -->
            <station code="1012191195" title="Караганда" code_system="vendor"/>
            <station code="101195" title="Караганда2"/>

            <!-- iata -->
            <station code="SVX" title="Кокпекты/Свердлова" code_system="iata"/>
            <station code="LED" code_system="iata"/>
            <station code="OMS" code_system="iata"/>

            <!-- local -->
            <station code="3974282634" title="Кокпекты/Свердлова" code_system="local"/>
        </stations>
        <threads>
            <thread timezone="Asia/Almaty" title="Караганда - Егиндыбулак" t_type="bus" number="102">
              <stoppoints>
                <stoppoint station_code="SVX" arrival_shift="" station_title="Караганда" station_code_system="iata"/>
                <stoppoint station_code="LED" arrival_shift="20" station_code_system="iata"/>
                <stoppoint station_code="BOJ" arrival_shift="20" station_code_system="iata"/>

                <stoppoint station_code="1012191195" arrival_shift="1800.0" station_title="Кокпекты/Свердлова"/>
                <stoppoint station_code="101195" arrival_shift="87000.0"/>
                <stoppoint station_code="3974282634" arrival_shift="88000.0" station_code_system="local"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2013-02-04" period_end_date="2013-02-18" days="1234567" times="08:30:00"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
"""

xml_content_with_OOO_code = u"""<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
    <group code="group_code_1">
        <stations>
            <!-- vendor -->
            <station code="1012191195" title="Караганда" code_system="vendor"/>
            <station code="101195" title="Караганда2"/>

            <!-- iata -->
            <station code="SVX" title="Кокпекты/Свердлова" code_system="iata"/>
            <station code="BOJ" code_system="iata"/>
            <station code="OMS" code_system="iata"/>
            <station code="LED" code_system="iata"/>
            <station code="OOO" code_system="iata"/>

            <!-- local -->
            <station code="3974282634" title="Кокпекты/Свердлова" code_system="local"/>
        </stations>
        <threads>
            <thread timezone="Asia/Almaty" title="Караганда - Егиндыбулак" t_type="bus" number="102">
              <stoppoints>
                <stoppoint station_code="SVX" arrival_shift="" station_title="Караганда" station_code_system="iata"/>
                <stoppoint station_code="BOJ" arrival_shift="20" station_code_system="iata"/>
                <stoppoint station_code="LED" arrival_shift="20" station_code_system="iata"/>
                <stoppoint station_code="OOO" arrival_shift="20" station_code_system="iata"/>
                <stoppoint station_code="1012191195" arrival_shift="1800.0" station_title="Кокпекты/Свердлова"/>
                <stoppoint station_code="101195" arrival_shift="87000.0"/>
                <stoppoint station_code="3974282634" arrival_shift="88000.0" station_code_system="local"/>
              </stoppoints>
              <schedules>
                <schedule period_start_date="2013-02-04" period_end_date="2013-02-18" days="1234567" times="08:30:00"/>
              </schedules>
            </thread>
        </threads>
    </group>
</channel>
"""


class MiddleBaseImportTest(TestCase, LogHasMessageMixIn):
    def setUp(self):
        self.package = create_tsi_package()
        self.factory = self.package.get_two_stage_factory()
        for code in ('VKO', 'OMS', 'ODS', 'BOJ', 'VAR', 'SVX', 'LED'):
            create_station(__={'codes': {'iata': code}})

        super(MiddleBaseImportTest, self).setUp()

    def testMiddleImport(self):
        self.package.package_file = ContentFile(name='threads.xml', content=xml_content)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        self.assertEqual(TwoStageImportThread.objects.filter(package=self.package).count(), 1)

    def testIATACysixStations(self):
        self.package.package_file = ContentFile(name='threads.xml', content=xml_content)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        TwoStageImportStation.objects.get(title=u"Кокпекты/Свердлова", code='group_code_1_iata_SVX')
        TwoStageImportStation.objects.get(title="", code='group_code_1_iata_LED')
        TwoStageImportStation.objects.get(title="", code='group_code_1_iata_BOJ')

        self.assertRaises(TwoStageImportStation.DoesNotExist,
            TwoStageImportStation.objects.get, title="", code='group_code_1_iata_SVN')

        ivn = Station.get_by_code('iata', 'SVX')
        prf = Station.get_by_code('iata', 'LED')
        iii = Station.get_by_code('iata', 'BOJ')

        stm_ivn = StationMapping.objects.get(station=ivn)
        stm_prf = StationMapping.objects.get(station=prf)
        stm_iii = StationMapping.objects.get(station=iii)

        self.assertEqual((stm_ivn.title, stm_ivn.code), (u"Кокпекты/Свердлова", 'group_code_1_iata_SVX'))
        self.assertEqual((stm_prf.title, stm_prf.code), (u"", 'group_code_1_iata_LED'))
        self.assertEqual((stm_iii.title, stm_iii.code), (u"", 'group_code_1_iata_BOJ'))

    def testLackIATAStation(self):
        self.package.package_file = ContentFile(name='threads_error_OOO.xml', content=xml_content_with_OOO_code)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        self.assert_log_has_message(u"ERROR: Не нашли станции в системе iata по коду OOO")

    def testLocalCysixStations(self):
        self.package.package_file = ContentFile(name='threads.xml', content=xml_content)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        TwoStageImportStation.objects.get(title=u"Кокпекты/Свердлова", code='group_code_1_local')

    def testVendorCysixStations(self):
        self.package.package_file = ContentFile(name='threads.xml', content=xml_content)
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package_into_middle_base()

        TwoStageImportStation.objects.get(title=u"Караганда", code='group_code_1_vendor_1012191195')
        TwoStageImportStation.objects.get(title=u"Караганда2", code='group_code_1_vendor_101195')
