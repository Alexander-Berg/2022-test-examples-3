# -*- coding: utf-8 -*-
import datetime

from common.models.geo import StationMajority
from common.models.schedule import Route, RThread
from common.models.transport import TransportType
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station
from cysix.tests.utils import get_test_filepath


class CysixNotMapped(TestCase):
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

        super(CysixNotMapped, self).setUp()

    @replace_now(datetime.datetime(2014, 6, 4))
    def testNotMappedInTheMiddle(self):
        with open(get_test_filepath('data', 'test_import_with_not_mapped_station', 'not_mapped_in_the_middle.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        routes = Route.objects.filter(two_stage_package=self.package)

        self.assertEqual(len(routes), 1)

        for route in routes:
            rthreads = RThread.objects.filter(route=route)
            self.assertEqual(len(rthreads), 1)

            self.assertEqual(len(rthreads[0].path), 2)
