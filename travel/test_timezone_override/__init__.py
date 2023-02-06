# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.geo import Station
from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_timezone_override', filename)


class TestTimezoneOverrideFilter(TestCase):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests.fixtures:template_package_base.yaml',
        'travel.rasp.admin.cysix.tests.fixtures:currency.yaml',
    ]

    def setUp(self):
        station_3 = Station.objects.get(title=u'Станция 3')
        station_3.settlement_id = 54
        station_3.time_zone = 'Asia/Yekaterinburg'
        station_3.save()

        self.supplier = Supplier.objects.get(code='supplier_1')
        super(TestTimezoneOverrideFilter, self).setUp()

    def tearDown(self):
        return super(TestTimezoneOverrideFilter, self).tearDown()

    def reimport(self, timezone_override, use_filter=True):
        package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=package)
        package.tsisetting.set_number = True
        package.tsisetting.save()

        package.add_default_filters()
        package.set_filter_parameter('timezone_override', 'timezone', timezone_override)
        package.set_filter_use('timezone_override', use_filter)

        factory = package.get_two_stage_factory()

        with open(get_test_filepath('cysix.xml')) as f:
            package.package_file = f

            importer = factory.get_two_stage_importer()
            importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testNoneOverride(self):
        self.reimport(timezone_override='none')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        # Копирование из общего
        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')

    @replace_now(datetime(2013, 5, 15))
    def testLocalOverride(self):
        self.reimport(timezone_override='local')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')

    @replace_now(datetime(2013, 5, 15))
    def testStartStationOverride(self):
        self.reimport(timezone_override='start_station')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Europe/Moscow')

    @replace_now(datetime(2013, 5, 15))
    def testSpecifiedTZStationOverride(self):
        self.reimport(timezone_override='Europe/Kiev')

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Europe/Kiev')
        self.assertEqual(path[1].time_zone, 'Europe/Kiev')
        self.assertEqual(path[2].time_zone, 'Europe/Kiev')

    @replace_now(datetime(2013, 5, 15))
    def testTurnOffFilterOverride(self):
        self.reimport(timezone_override='Europe/Kiev', use_filter=False)

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        # Копирование из общего
        self.assertEqual(path[0].time_zone, 'Europe/Moscow')
        self.assertEqual(path[1].time_zone, 'Europe/Moscow')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')

    @replace_now(datetime(2013, 5, 15))
    def testNoneOverrideWithSpecifiedContextTimezone(self):
        self.reimport(timezone_override='none')

        thread = RThread.objects.get(number='t-specified-timezone')

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(path[0].time_zone, 'Asia/Yekaterinburg')
        self.assertEqual(path[1].time_zone, 'Asia/Yekaterinburg')
        self.assertEqual(path[2].time_zone, 'Asia/Yekaterinburg')
