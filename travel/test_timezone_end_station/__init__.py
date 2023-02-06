# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.geo import Station
from common.models.schedule import Supplier
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_timezone_end_station', filename)


class TestTimezoneEndStation(TestCase):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests:template_package_base.yaml',
        'travel.rasp.admin.cysix.tests:currency.yaml',
    ]

    def setUp(self):
        station_3 = Station.objects.get(title=u'Станция 3')
        station_3.settlement_id = 54
        station_3.time_zone = 'Asia/Yekaterinburg'
        station_3.save()

        self.supplier = Supplier.objects.get(code='supplier_1')
        super(TestTimezoneEndStation, self).setUp()

    @replace_now(datetime(2013, 5, 15))
    def testEndStation(self):
        package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=package)
        with open(get_test_filepath('cysix.xml')) as f:
            package.package_file = f

            factory = package.get_two_stage_factory()
            data_provider = factory.get_data_provider()

            cysix_route = data_provider.get_supplier_route_iter().next()

        for srts in cysix_route.supplier_rtstations:
            self.assertEqual(srts.get_pytz(cysix_route.context).zone, 'Asia/Yekaterinburg')
