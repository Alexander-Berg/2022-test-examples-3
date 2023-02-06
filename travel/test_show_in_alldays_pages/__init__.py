# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_show_in_alldays_pages', filename)


class TestTimezoneOverrideFilter(TestCase):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests:template_package_base.yaml',
        'travel.rasp.admin.cysix.tests:currency.yaml',
    ]

    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')

        package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=package)
        package.tsisetting.set_number = True
        package.tsisetting.save()

        super(TestTimezoneOverrideFilter, self).setUp()

    def tearDown(self):
        return super(TestTimezoneOverrideFilter, self).tearDown()

    def reimport(self, show_in_alldays_pages):
        package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        package.tsisetting.show_in_alldays_pages = show_in_alldays_pages
        package.tsisetting.save()

        factory = package.get_two_stage_factory()

        with open(get_test_filepath('cysix.xml')) as f:
            package.package_file = f

            importer = factory.get_two_stage_importer()
            importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testShow(self):
        self.reimport(show_in_alldays_pages=True)

        thread = RThread.objects.get(number='t-number')

        assert thread.show_in_alldays_pages

    @replace_now(datetime(2013, 5, 15))
    def testHide(self):
        self.reimport(show_in_alldays_pages=False)

        thread = RThread.objects.get(number='t-number')

        assert not thread.show_in_alldays_pages
