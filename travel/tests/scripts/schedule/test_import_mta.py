# -*- coding: utf-8 -*-

import os.path

from common.models.schedule import Supplier
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.bus.import_mta import FtpMtaDataProvider, MtaImporter


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml',
    'travel.rasp.admin.tests.scripts.schedule:test_import_mta.yaml'
]


def get_test_file_path(filename):
    return os.path.join('travel', 'rasp', 'admin', 'tests', 'scripts', 'schedule', 'data', 'import_mta', filename)


class MTATest(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = Supplier.objects.get(code='mta')

    def testGetStations(self):
        dp = self.get_test_provider()

        importer = MtaImporter(dp)

        mta_station = importer.get_mta_station(u"-1099561309", None)

        self.assertEqual(mta_station.title, u'Старый Снопок')
        self.assertEqual(mta_station.code, u"-1099561309")

        self.assertEqual(len(dp.get_stops()), 8)

    def get_test_provider(self):
        dp = FtpMtaDataProvider()
        dp.schedule_file_path = get_test_file_path('schedule.xml')
        dp.stops_file_path = get_test_file_path('stops.xml')

        return dp

    def testGetStationByCode(self):
        dp = self.get_test_provider()

        importer = MtaImporter(dp)

        station = importer.get_station('-608969091', None)

        self.assertEqual(station.title, u"Воловниково")

    def testGetRoutes(self):
        dp = self.get_test_provider()

        routes_data = list(dp.get_routes_data())

        self.assertEqual(len(routes_data), 3)

