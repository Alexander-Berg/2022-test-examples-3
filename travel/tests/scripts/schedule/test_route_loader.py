# -*- coding: utf-8 -*-

from datetime import time, date, timedelta

from django.db import DatabaseError, connection

from common.models.geo import Station
from common.models.schedule import Route, RThread, RTStation, Supplier, RThreadType
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from travel.rasp.admin.lib import tmpfiles
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.scripts.schedule.utils.route_loader import (
    BulkRouteLoader, BulkRouteSaver, RouteUpdater, RouteUpdaterError,
    RouteSaver, CompactThreadNumberBuilder, ThreadMysqlModelUpdaterByImportUid)
from tester.factories import create_supplier, create_route, create_thread
from tester.testcase import TestCase

module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class RouteLoaderTest(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = create_supplier(title="Test Supplier")

    def test_route_loader(self):
        loader = BulkRouteLoader()

        route = Route()
        route.supplier = self.supplier
        route.t_type_id = TransportType.WATER_ID

        thread = RThread()
        thread.route = route
        thread.supplier = self.supplier
        thread.t_type_id = TransportType.WATER_ID
        thread.title = u"Синюши - Пастуши"
        thread.number = '1'
        thread.year_days = RunMask.EMPTY_YEAR_DAYS
        thread.tz_start_time = time(0, 0)
        thread.time_zone = 'Europe/Moscow'
        thread.type_id = RThreadType.BASIC_ID

        route.route_uid = thread.gen_route_uid()
        thread.ordinal_number = 1
        thread.gen_import_uid()
        thread.gen_uid()

        thread.rtstations = self.make_rtss()

        route.threads = [thread]

        loader.add_route(route)

        loader.load()

        self.assertTrue(Route.objects.filter(route_uid=route.route_uid))

    def make_rtss(self):
        rts1 = RTStation()
        rts1.station = Station.objects.all()[0]
        rts1.tz_arrival = None
        rts1.time_zone = 'Europe/Moscow'
        rts1.tz_departure = 0

        rts2 = RTStation()
        rts2.station = Station.objects.all()[1]
        rts2.tz_arrival = 10000
        rts2.time_zone = 'Europe/Moscow'
        rts2.tz_departure = None

        return [rts1, rts2]


class TestRouteSaver(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = create_supplier(title="Test Supplier")

    def test_save_thread_without_route(self):
        route1 = create_route(__={'threads': []})
        thread1 = create_thread(route=route1, number=u's333', tz_start_time=time(0, 0), ordinal_number=1,
                                year_days=RunMask.EMPTY_YEAR_DAYS)
        thread2 = create_thread(route=route1, number=u's333', tz_start_time=time(0, 0), ordinal_number=2,
                                year_days=RunMask.EMPTY_YEAR_DAYS)
        Route.objects.all().delete()

        saver = RouteSaver()

        thread1.route = route1
        saver.save_route(route1)
        saver.save_thread(thread1)

        thread2.route = route1
        saver.save_thread(thread2)

        route = Route.objects.get(route_uid=route1.route_uid)
        self.assertEqual(2, route.rthread_set.all().count())


class TestBulkRouteSaver(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = create_supplier(title="Test Supplier")

    def testCheckMessage(self):
        loader = BulkRouteSaver()
        loader.cursor = connection.cursor()

        loader.check_message("Records: 0  Deleted: 0  Skipped: 0  Warnings: 0")
        loader.check_message("Records: 200  Deleted: 0  Skipped: 0  Warnings: 0")

        self.assertRaises(DatabaseError, loader.check_message,
                          "Records: 200  Deleted: 0  Skipped: 10  Warnings: 0")
        self.assertRaises(DatabaseError, loader.check_message,
                          "Records: 200  Deleted: 0  Skipped: 10  Warnings: 32")
        self.assertRaises(DatabaseError, loader.check_message,
                          "Records: 200  Deleted: 0  Skipped: 0  Warnings: 2")


class TestRouteUpdater(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = create_supplier(title="Test Supplier")

    def testNormalNewRoutes(self):
        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=1))
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        route1.threads = [thread1]

        updater.add_route(route1)

        thread2 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 3)])
        )
        thread2.rtstations = get_path1()

        updater.add_thread(thread2)

        updater.update()

        thread = RThread.objects.get(import_uid=thread1.import_uid)

        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 1), date(2012, 1, 2), date(2012, 1, 3)]
        )

    def testLoadOnExistedRoutes(self):
        route1 = create_route(supplier=self.supplier, script_protected=False, __={'threads': []})

        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        route1.threads = [thread1]

        updater.add_route(route1)

        thread2 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 3)])
        )

        thread2.rtstations = get_path1()

        updater.add_thread(thread2)

        updater.update()

        thread = RThread.objects.get(import_uid=thread1.import_uid)

        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 1), date(2012, 1, 2), date(2012, 1, 3)]
        )

    def testSeveralThreadsWithSameNumber(self):
        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        route1.threads = [thread1]

        thread2 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 3)])
        )
        thread2.rtstations = get_path1()
        route1.threads = [thread1, thread2]

        updater.add_route(route1)
        updater.add_thread(thread2)

        thread3 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 4)])
        )
        thread3.rtstations = get_path3()

        updater.add_thread(thread3)

        updater.update()

        thread = RThread.objects.get(import_uid=thread1.import_uid)

        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 1), date(2012, 1, 2), date(2012, 1, 3)]
        )

        thread = RThread.objects.get(import_uid=thread3.import_uid)

        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 4)]
        )

    def testIgnoredRoutes(self):
        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        route1.threads = [thread1]

        updater.add_route(route1)
        updater.update()

        route = Route.objects.get(route_uid=route1.route_uid)
        route.script_protected = True
        route.save()

        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 11), date(2012, 1, 22)])
        )
        thread1.rtstations = get_path1()

        thread2 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 11), date(2012, 1, 22)])
        )
        thread2.rtstations = get_path2()

        thread1.route = thread2.route = route

        updater.add_route(route1)
        updater.add_thread(thread1)
        updater.add_thread(thread2)
        updater.update()

        route = Route.objects.get(route_uid=route1.route_uid)
        self.assertEqual(1, route.rthread_set.all().count())

        thread = RThread.objects.get(import_uid=thread1.import_uid)
        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 1), date(2012, 1, 2)]
        )

    def testDeleteObsoleteThreads(self):
        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread11 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread11.rtstations = get_path1()
        route1.threads = [thread11]

        thread13 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(9, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )

        route2 = get_route()
        thread21 = get_thread(
            route=route1, number=u'2', title=u'Пастуши - Синюши', tz_start_time=time(10, 30),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread21.rtstations = get_path1()
        route2.threads = [thread21]

        updater.add_route(route1)
        updater.add_route(route2)
        updater.update()

        thread13.route = Route.objects.get(route_uid=route1.route_uid)
        thread13.ordinal_number = 100500
        thread13.gen_uid()
        thread13.gen_import_uid()
        thread13.save()

        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        thread12 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 11), date(2012, 1, 22)])
        )
        thread12.rtstations = get_path2()
        thread12.route = route1

        updater.add_thread(thread12)
        updater.update()

        route = Route.objects.get(route_uid=route1.route_uid)
        self.assertEqual(1, route.rthread_set.all().count())

        thread = RThread.objects.get(import_uid=thread12.import_uid)
        self.assertListEqual(thread.get_mask(today=updater.today).dates(),
            [date(2012, 1, 11), date(2012, 1, 22)]
        )

        self.assertRaises(Route.DoesNotExist, Route.objects.get, route_uid=route2.route_uid)
        self.assertRaises(RThread.DoesNotExist, RThread.objects.get, id=thread13.id)

    def testNotExistedRouteAdd(self):
        updater = RouteUpdater.create_route_updater(Route.objects.filter(supplier=self.supplier))
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        route1.threads = [thread1]

        self.assertRaises(RouteUpdaterError, updater.add_thread, thread1)

    def testCompactThreadNumberBuilder(self):
        updater = RouteUpdater.create_route_updater(
            Route.objects.filter(supplier=self.supplier),
            thread_number_builder=CompactThreadNumberBuilder
        )
        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread1 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(10, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread2 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(11, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread3 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(12, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread1.rtstations = get_path1()
        thread2.rtstations = get_path1()
        thread3.rtstations = get_path1()

        route1.threads = [thread1, thread2, thread3]

        updater.add_route(route1)
        updater.update()

        thread2.delete()

        updater = RouteUpdater.create_route_updater(
            Route.objects.filter(supplier=self.supplier),
            thread_number_builder=CompactThreadNumberBuilder
        )

        updater.today = date(2012, 1, 1)

        route1 = get_route()
        thread4 = get_thread(
            route=route1, number=u'1', title=u'Пастуши - Синюши', tz_start_time=time(13, 0),
            mask=RunMask(days=[date(2012, 1, 1), date(2012, 1, 2)])
        )
        thread4.rtstations = get_path1()

        route1.threads = [thread4]

        updater.add_route(route1)

        assert thread2.ordinal_number is not None
        assert thread4.ordinal_number == thread2.ordinal_number


class ThreadMysqlModelUpdaterByImportUidTest(TestCase):
    class_fixtures = module_fixtures

    def setUp(self):
        self.supplier = create_supplier(title="Test Supplier")

    @replace_now('2015-03-30 10:00:00')
    @tmpfiles.clean_temp
    def testSimple(self):
        today = environment.today()

        route = create_route(supplier=self.supplier, script_protected=False, __={'threads': []})
        thread = create_thread(route=route, number=u'1', title=u'1')

        new_mask = RunMask.range(today, today + timedelta(30), today=today)

        with ThreadMysqlModelUpdaterByImportUid(
            tmpfiles.get_tmp_dir(), fields=('year_days',)
        ) as updater:
            updater.add_dict({
                'id': None,
                'import_uid': thread.import_uid,
                'tz_year_days': str(new_mask)
            })

        thread = RThread.objects.get(pk=thread.id)

        self.assertEqual(thread.get_mask(today=today), new_mask)


def get_route():
    route = Route()
    route.comment = ''
    route.supplier = Supplier.objects.get(title="Test Supplier")
    route.t_type_id = TransportType.WATER_ID

    return route


def get_thread(route, number, title, tz_start_time, mask):
    thread = RThread()
    thread.route = route
    thread.year_days = str(mask)
    thread.tz_start_time = tz_start_time
    thread.time_zone = 'Europe/Moscow'
    thread.number = number
    thread.type_id = RThreadType.BASIC_ID
    thread.supplier = Supplier.objects.get(title="Test Supplier")
    thread.t_type_id = TransportType.WATER_ID
    thread.title = title

    return thread


def get_path1():
    rts1 = RTStation()
    rts1.station = Station.objects.all()[0]
    rts1.time_zone = 'Europe/Moscow'
    rts1.tz_arrival = None
    rts1.tz_departure = 0

    rts2 = RTStation()
    rts2.station = Station.objects.all()[1]
    rts2.time_zone = 'Europe/Moscow'
    rts2.tz_arrival = 10000
    rts2.tz_departure = None

    return [rts1, rts2]


def get_path1_other_times():
    rts1 = RTStation()
    rts1.station = Station.objects.all()[0]
    rts1.time_zone = 'Europe/Moscow'
    rts1.tz_arrival = None
    rts1.tz_departure = 0

    rts2 = RTStation()
    rts2.station = Station.objects.all()[1]
    rts2.time_zone = 'Europe/Moscow'
    rts2.tz_arrival = 6000
    rts2.tz_departure = None

    return [rts1, rts2]


def get_path2():
    rts1 = RTStation()
    rts1.station = Station.objects.all()[0]
    rts1.time_zone = 'Europe/Moscow'
    rts1.tz_arrival = None
    rts1.tz_departure = 0

    rts2 = RTStation()
    rts2.station = Station.objects.all()[1]
    rts2.time_zone = 'Europe/Moscow'
    rts2.tz_arrival = 6000
    rts2.tz_departure = None

    return [rts1, rts2]


def get_path3():
    rts1 = RTStation()
    rts1.station = Station.objects.all()[0]
    rts1.time_zone = 'Europe/Moscow'
    rts1.tz_arrival = None
    rts1.tz_departure = 0

    rts2 = RTStation()
    rts2.station = Station.objects.all()[1]
    rts2.time_zone = 'Europe/Moscow'
    rts2.tz_arrival = 7000
    rts2.tz_departure = None

    return [rts1, rts2]
