# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.schedule import Route
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.fileutils import get_current_file_data_dir
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


data_file_path = os.path.join('travel', 'rasp', 'admin', 'tests', 'importinfo', 'data', 'test_tsi_thread_station_flags', 'threads.xml')


class TSIThreadStationFlagTest(TestCase):
    class_fixtures = ['travel.rasp.admin.tests.importinfo:test_tsi_thread_station_flags.yaml']

    def setUp(self):
        self.package = TwoStageImportPackage.objects.get(title=u"Test Package")
        TSISetting.objects.get_or_create(package=self.package)
        f = self.package.get_filter("correct_departure_and_arrival")
        if f:
            f.use = False
            f.save()

        self.factory = self.package.get_two_stage_factory()

    @replace_now(datetime(2013, 2, 4))
    def testIsFuzzy(self):
        tsi_importer = self.factory.get_two_stage_importer()

        with open(data_file_path) as f:
            self.package.package_file = f

            tsi_importer.reimport_package()

        route = Route.objects.get(two_stage_package=self.package)

        thread = route.rthread_set.all()[0]

        rtss = list(thread.rtstation_set.all())

        self.assertEqual(rtss[0].is_fuzzy, False)
        self.assertEqual(rtss[1].is_fuzzy, True)
        self.assertEqual(rtss[2].is_fuzzy, False)

    @replace_now(datetime(2013, 2, 4))
    def testIsSearchableFrom(self):
        tsi_importer = self.factory.get_two_stage_importer()

        with open(data_file_path) as f:
            self.package.package_file = f

            tsi_importer.reimport_package()

        route = Route.objects.get(two_stage_package=self.package)

        thread = route.rthread_set.all()[0]

        rtss = list(thread.rtstation_set.all())

        self.assertEqual(rtss[0].is_searchable_from, True)
        self.assertEqual(rtss[1].is_searchable_from, True)  # Хоть и fuzzy RASP-14462
        self.assertEqual(rtss[2].is_searchable_from, True)

    @replace_now(datetime(2013, 2, 4))
    def testIsSearchableTo(self):
        tsi_importer = self.factory.get_two_stage_importer()

        with open(data_file_path) as f:
            self.package.package_file = f

            tsi_importer.reimport_package()

        route = Route.objects.get(two_stage_package=self.package)

        thread = route.rthread_set.all()[0]

        rtss = list(thread.rtstation_set.all())

        self.assertEqual(rtss[0].is_searchable_to, False)
        self.assertEqual(rtss[1].is_searchable_to, True)
        self.assertEqual(rtss[2].is_searchable_to, True)

    @replace_now(datetime(2013, 2, 4))
    def testInStationSchedule(self):
        tsi_importer = self.factory.get_two_stage_importer()

        with open(data_file_path) as f:
            self.package.package_file = f

            tsi_importer.reimport_package()

        route = Route.objects.get(two_stage_package=self.package)

        thread = route.rthread_set.all()[0]

        rtss = list(thread.rtstation_set.all())

        self.assertEqual(rtss[0].in_station_schedule, True)
        self.assertEqual(rtss[1].in_station_schedule, False)
        self.assertEqual(rtss[2].in_station_schedule, True)
