# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_stoppoint_is_combined', filename)


class TestStopPointIsCombined(TestCase):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests:template_package_base.yaml',
        'travel.rasp.admin.cysix.tests:currency.yaml',
    ]

    def setUp(self):
        super(TestStopPointIsCombined, self).setUp()
        self.supplier = Supplier.objects.get(code='supplier_1')

        self.reimport()

    def tearDown(self):
        return super(TestStopPointIsCombined, self).tearDown()

    @replace_now(datetime(2013, 5, 15))
    def reimport(self):
        package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=package)
        package.tsisetting.set_number = True
        package.tsisetting.save()

        factory = package.get_two_stage_factory()

        with open(get_test_filepath('cysix.xml')) as f:
            package.package_file = f

            importer = factory.get_two_stage_importer()
            importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testIsCombined(self):
        thread = RThread.objects.get(number='t-combined')

        path = list(thread.path)

        self.assertEqual(thread.is_combined, True)

        self.assertEqual(path[0].is_combined, False)
        self.assertEqual(path[1].is_combined, True)
        self.assertEqual(path[2].is_combined, False)

    @replace_now(datetime(2013, 5, 15))
    def testSimple(self):
        thread = RThread.objects.get(number='t-simple')

        path = list(thread.path)

        self.assertEqual(thread.is_combined, False)

        self.assertEqual(path[0].is_combined, False)
        self.assertEqual(path[1].is_combined, False)
        self.assertEqual(path[2].is_combined, False)
