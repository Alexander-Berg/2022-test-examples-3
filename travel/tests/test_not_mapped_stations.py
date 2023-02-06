# -*- coding: utf-8 -*-
from datetime import datetime

from common.models.schedule import Route, RThread, Supplier
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.tests.utils import get_test_filepath


class NotMappedStationsTest(TestCase):
    class_fixtures = ['test_arrival_and_departure_correction.yaml', 'settlement.yaml']

    def setUp(self):
        olven = Supplier.objects.get(title=u'Олвен')

        self.package = create_tsi_package(supplier=olven)
        self.package.add_default_filters()

        self.factory = self.package.get_two_stage_factory()

        super(NotMappedStationsTest, self).setUp()

    def reimport(self, filename):
        with open(get_test_filepath('data', 'test_not_mapped_stations', filename)) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testFirstNotMapped(self):
        self.reimport('first_not_mapped_station.xml')

        routes = Route.objects.filter(two_stage_package=self.package)
        self.assertEqual(len(routes), 0)

    @replace_now(datetime(2013, 5, 15))
    def testMiddleNotMapped(self):
        self.reimport('middle_not_mapped_station.xml')

        routes = Route.objects.filter(two_stage_package=self.package)
        self.assertEqual(len(routes), 1)

        rthreads = RThread.objects.filter(route__in=routes)
        self.assertEqual(len(rthreads), 1)

        rthread = RThread.objects.get(route=routes[0])

        rtstations = rthread.path

        self.assertEqual(len(rtstations), 5)

    @replace_now(datetime(2013, 5, 15))
    def testLastNotMapped(self):
        self.reimport('last_not_mapped_station.xml')

        routes = Route.objects.filter(two_stage_package=self.package)
        self.assertEqual(len(routes), 0)
