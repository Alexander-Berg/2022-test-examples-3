# -*- coding: utf-8 -*-

from datetime import datetime

from common.models.schedule import Supplier, Route
from common.models.tariffs import ThreadTariff
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.tests.utils import get_test_filepath


class TestSalesFlag(TestCase):

    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'travel.rasp.admin.cysix.tests.fixtures:currency.yaml',
        'travel.rasp.admin.cysix.tests.fixtures:template_package_base.yaml',
    ]

    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')
        self.package = create_tsi_package(supplier=self.supplier)
        self.package.tsisetting.import_order_data = True
        self.package.tsisetting.save()

        self.factory = self.package.get_two_stage_factory()

        self.package.package_file = open(get_test_filepath('data', 'test_sales_flag', 'cysix.xml'))
        super(TestSalesFlag, self).setUp()

    def tearDown(self):
        self.package.package_file.close()

        return super(TestSalesFlag, self).tearDown()

    @replace_now(datetime(2013, 5, 15))
    def testEnabledSales(self):
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

        # sales=1
        thread = Route.objects.get(supplier=self.supplier).rthread_set.all()[0]

        tariff = ThreadTariff.objects.get(thread_uid=thread.uid)

        self.assertEqual(tariff.tariff, 10)
        self.assertTrue(tariff.get_order_data())

        # sales не указан
        thread = Route.objects.get(supplier=self.supplier).rthread_set.all()[1]

        tariff = ThreadTariff.objects.get(thread_uid=thread.uid)

        self.assertEqual(tariff.tariff, 10)
        self.assertTrue(tariff.get_order_data())

    @replace_now(datetime(2013, 5, 15))
    def testDisabledSales(self):
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

        # sales=0
        thread = Route.objects.get(supplier=self.supplier).rthread_set.all()[2]

        tariff = ThreadTariff.objects.get(thread_uid=thread.uid)

        self.assertEqual(tariff.tariff, 10)
        self.assertFalse(tariff.get_order_data())
