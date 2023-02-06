# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.geo import Station
from common.models.schedule import RThread
from common.models.tariffs import ThreadTariff
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_fare_is_min_price', filename)


class TestFareIsMinPrice(TestCase):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests:template_package_base.yaml',
        'travel.rasp.admin.cysix.tests:currency.yaml',
    ]

    @replace_now(datetime(2013, 5, 15))
    def setUp(self):
        self.station_1 = Station.objects.get(title=u'Станция 1')
        self.station_2 = Station.objects.get(title=u'Станция 2')
        self.station_3 = Station.objects.get(title=u'Станция 3')

        package = TwoStageImportPackage.objects.get(supplier__code='supplier_1')
        TSISetting.objects.get_or_create(package=package)
        package.tsisetting.set_number = True
        package.tsisetting.save()

        factory = package.get_two_stage_factory()

        with open(get_test_filepath('cysix.xml')) as f:
            package.package_file = f

            importer = factory.get_two_stage_importer()
            importer.reimport_package()

        self.thread = RThread.objects.get(number='t-number')

        super(TestFareIsMinPrice, self).setUp()

    def get_tariff(self, station_number_from, station_number_to):
        return ThreadTariff.objects.get(
            thread_uid=self.thread.uid,
            station_from=getattr(self, 'station_' + str(station_number_from)),
            station_to=getattr(self, 'station_' + str(station_number_to))
        )

    @replace_now(datetime(2013, 5, 15))
    def testIsMinPrice(self):
        # <price price="12" currency="USD" is_min_price="1">
        #   <stop_from station_code="1"/>
        #   <stop_to   station_code="2"/>
        # </price>
        # <price price="20" currency="USD" is_min_price="0">
        #   <stop_from station_code="1"/>
        #   <stop_to   station_code="3"/>
        # </price>
        # <price price="9" currency="USD">
        #   <stop_from station_code="2"/>
        #   <stop_to   station_code="3"/>
        # </price>

        self.assertEqual(self.get_tariff(1, 2).is_min_tariff, True)
        self.assertEqual(self.get_tariff(1, 3).is_min_tariff, False)
        self.assertEqual(self.get_tariff(2, 3).is_min_tariff, False)
