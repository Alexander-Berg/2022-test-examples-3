# -*- coding: utf-8 -*-

import datetime

from common.models.geo import StationMajority
from common.models.schedule import Route, RThread
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now, MaskComparisonMixIn
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station
from cysix.tests.utils import get_test_filepath


class CysixImportCommentTest(TestCase, LogHasMessageMixIn, MaskComparisonMixIn):
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

        super(CysixImportCommentTest, self).setUp()

    @replace_now(datetime.datetime(2013, 6, 8))
    def testCommentInSchedule(self):
        with open(get_test_filepath('data', 'test_import_comment', 'comment_in_schedule.xml')) as f:
            self.package.package_file = f

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        self.assertEqual(len(routes), 1)

        rthreads = RThread.objects.filter(route=routes[0])
        self.assertEqual(len(rthreads), 1)

        comment = rthreads[0].comment

        self.assertEqual(comment, u'Комментарий в расписании')

    @replace_now(datetime.datetime(2013, 6, 8))
    def testCommentInIntervalSchedule(self):
        with open(get_test_filepath('data', 'test_import_comment', 'comment_in_interval_schedule.xml')) as f:
            self.package.package_file = f

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        self.assertEqual(len(routes), 1)

        rthreads = RThread.objects.filter(route=routes[0])
        self.assertEqual(len(rthreads), 1)

        comment = rthreads[0].comment

        self.assertEqual(comment, u'Комментарий в расписании')

    @replace_now(datetime.datetime(2013, 6, 8))
    def testCommentInThread(self):
        with open(get_test_filepath('data', 'test_import_comment', 'comment_in_thread.xml')) as f:
            self.package.package_file = f

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        self.assertEqual(len(routes), 1)

        rthreads = RThread.objects.filter(route=routes[0])
        self.assertEqual(len(rthreads), 1)

        comment = rthreads[0].comment

        self.assertEqual(comment, u'Комментарий в нитке')

    @replace_now(datetime.datetime(2013, 6, 8))
    def testCommentInGroup(self):
        with open(get_test_filepath('data', 'test_import_comment', 'comment_in_group.xml')) as f:
            self.package.package_file = f

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        self.assertEqual(len(routes), 1)

        rthreads = RThread.objects.filter(route=routes[0])
        self.assertEqual(len(rthreads), 1)

        comment = rthreads[0].comment

        self.assertEqual(comment, u'Комментарий в группе')

    @replace_now(datetime.datetime(2013, 6, 8))
    def testDoNotGlueSchedulesWithDifferentComments(self):
        with open(get_test_filepath('data', 'test_import_comment', 'do_not_glue_schedules_with_different_comments.xml')) as f:
            self.package.package_file = f

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        rthreads = RThread.objects.filter(route__in=routes)
        self.assertEqual(len(rthreads), 2)
