# -*- coding: utf-8 -*-

import os.path
from datetime import datetime, time

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.blacklist import time_in_range
from travel.rasp.admin.importinfo.models.two_stage_import import TSISetting
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.utils import SupplierRoute


data_file_path = os.path.join('travel', 'rasp', 'admin', 'tests', 'importinfo', 'data', 'test_blacklist', 'threads.xml')


class SupplierRouteStub(SupplierRoute):
    def __init__(self, route, supplier_title, supplier_number):
        self.route = route
        self.supplier_title = supplier_title
        self.supplier_number = supplier_number

    def get_route(self):
        self.route.threads = [self.route.rthread_set.all()[0]]
        self.route.threads[0].rtstations = list(self.route.rthread_set.all()[0].rtstation_set.all())

        return self.route


class BlackListTest(TestCase, LogHasMessageMixIn):
    class_fixtures = ['travel.rasp.admin.tests.importinfo:test_blacklist.yaml']

    def setUp(self):
        self.route = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'www.route', '1')
        self.supplier_route = SupplierRouteStub(self.route, u"Козлы - Мозд", u"123")

    def testTimeInRange(self):
        self.assertTrue(time_in_range(time(11, 00), (time(11, 00), time(11, 00))))
        self.assertTrue(time_in_range(time(11 ,00), (time(10, 00), time(12, 00))))
        self.assertTrue(time_in_range(time(11 ,00), (time(22, 00), time(12, 00))))
        self.assertFalse(time_in_range(time(11, 00), (time(22, 00), time(23, 00))))
        self.assertFalse(time_in_range(time(11, 00), (time(22, 00), time(10, 00))))

    def testTypeTTypes(self):
        bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 't_type')
        bl2 = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'type_t_type')
        bl_bad = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_t_type')
        bl_bad2 = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_type_t_type')

        self.assertTrue(bl.match_supplier_route(self.supplier_route))
        self.assertFalse(bl_bad.match_supplier_route(self.supplier_route))
        self.assertTrue(bl2.match_supplier_route(self.supplier_route))
        self.assertFalse(bl_bad2.match_supplier_route(self.supplier_route))

    def testNumber(self):
        bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'number')
        bad_bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_match_number')

        self.assertTrue(bl.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl.match_supplier_route(self.supplier_route))

    def testSupplierNumber(self):
        bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'supplier_number')
        bad_bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_match_supplier_number')

        self.assertTrue(bl.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl.match_supplier_route(self.supplier_route))

    def testSupplierTitle(self):
        bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'supplier_title')
        bad_bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_supplier_title')

        self.assertTrue(bl.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testStartTime(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'start_time_exact')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_start_time_exact')

        bl_msk_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'start_time_msk_exact')
        bad_bl_msk_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_start_time_msk_exact')

        bl_range = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'start_time_range')
        bad_bl_range = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_start_time_range')

        bl_msk_range = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'start_time_msk_range')
        bad_bl_msk_range = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_start_time_msk_range')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

        self.assertTrue(bl_msk_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_msk_exact.match_supplier_route(self.supplier_route))

        self.assertTrue(bl_range.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_range.match_supplier_route(self.supplier_route))

        self.assertTrue(bl_msk_range.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_msk_range.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testFinishTime(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'finish_time_exact')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_finish_time_msk_range')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    def testStartStation(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'start_station')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_start_station')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    def testFinishStation(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'finish_station')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_finish_station')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    def testThreadStation(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'thread_station')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_thread_station')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testThreadStationDeparture(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'thread_station_departure')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_thread_station_departure')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testThreadStationArrival(self):
        bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'thread_station_arrival')
        bad_bl_exact = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_thread_station_arrival')

        self.assertTrue(bl_exact.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_exact.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testThreadStationArrivalDepartureIsNone(self):
        bad_bl_departure = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'thread_station_arrival_none')
        bad_bl_arrival = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'thread_station_departure_none')

        self.assertFalse(bad_bl_departure.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl_arrival.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testThreadStationArrivalDepartureIsNone(self):
        bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'combo')
        bad_bl = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.blacklist', 'not_combo')

        self.assertTrue(bl.match_supplier_route(self.supplier_route))
        self.assertFalse(bad_bl.match_supplier_route(self.supplier_route))

    @replace_now(datetime(2013, 1, 1))
    def testTwoStageImport(self):
        package = self.get_obj_from_fixture('travel.rasp.admin.tests.importinfo:test_blacklist.yaml', 'importinfo.twostageimportpackage', '1')
        TSISetting.objects.get_or_create(package=package)

        with open(data_file_path) as f:
            package.package_file = f

            importer = package.get_two_stage_factory().get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(RThread.objects.filter(route__two_stage_package=package).count(), 1)
