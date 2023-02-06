# -*- coding: utf-8 -*-

from common.models.geo import StationMajority
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn
from travel.rasp.admin.lib.unittests.testcase import TestCase
from common.tester.factories import create_station
from cysix.tests.utils import get_test_filepath


class StationBuildTest(TestCase, LogHasMessageMixIn):
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

        super(StationBuildTest, self).setUp()

    def testParseErrorsWarnings(self):
        with open(get_test_filepath('data', 'test_stations', 'stations.xml')) as f:
            self.package.package_file = f

            data_provider = self.factory.get_data_provider()

            pathes = list(data_provider.get_supplier_path_iter())

        self.assertEqual(len(pathes), 2)

        self.assert_log_has_message(u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" number="102" sourceline=40 <Group title="" code="2" sourceline=33>>: Станции <StopPoint: station_title="Егиндыбулак" station_code="30" station_code_system="local"> нет в блоке stations')

        self.assert_log_has_message(u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" number="Пропускаем этот маршрут" sourceline=49 <Group title="" code="2" sourceline=33>>: Станции <StopPoint: station_title="" station_code="20" station_code_system="local"> нет в блоке stations')

        self.assert_log_has_message(u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" number="110" sourceline=68 <Group title="" code="2" sourceline=33>>: Не указан station_code')

    def testPath1(self):
        with open(get_test_filepath('data', 'test_stations', 'stations.xml')) as f:
            self.package.package_file = f

            data_provider = self.factory.get_data_provider()

            path = list(data_provider.get_supplier_path_iter())[0]

        self.assertEqual(len(path), 6)

        station = path[0]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('DMD', 'iata', u"Кокпекты/Свердлова", '1_iata_DMD')
        )

        station = path[1]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('SVO', 'iata', u"", '1_iata_SVO')
        )

        station = path[2]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('SVO', 'iata', u"", '1_iata_SVO')
        )

        station = path[3]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('1012191195', 'vendor', u"Караганда", '1_vendor_1012191195')
        )

        station = path[4]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('101195', 'vendor', u"Караганда2", '1_vendor_101195')
        )

        station = path[5]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('3974282634', 'local', u"Кокпекты/Свердлова", '1_local')
        )

    def testPath2(self):
        with open(get_test_filepath('data', 'test_stations', 'stations.xml')) as f:
            self.package.package_file = f

            data_provider = self.factory.get_data_provider()

            path = list(data_provider.get_supplier_path_iter())[1]

        self.assertEqual(len(path), 2)

        station = path[0]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('SVO', 'iata', u"", '2_iata_SVO')
        )

        station = path[1]
        self.assertEqual(
            (station.station_code, station.station_code_system, station.title, station.code),
            ('SVX', 'iata', u"", '2_iata_SVX')
        )
