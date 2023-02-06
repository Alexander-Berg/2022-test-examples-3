# -*- coding: utf-8 -*-

from datetime import datetime

from common.models.schedule import Supplier
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.tests.utils import get_test_filepath


class TestCysixTemplate(TestCase):
    """
    Все что нужно для тестирования пакета общего формата в минимальном варианте
    """

    class_fixtures = ['template_package_base.yaml',
                      'currency.yaml',
                      'travel.rasp.admin.tester.fixtures.www:countries.yaml',
                      'travel.rasp.admin.tester.fixtures.www:regions.yaml',
                      'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
                      'travel.rasp.admin.tester.fixtures.www:stations.yaml']

    def setUp(self):
        self.supplier = Supplier.objects.get(code='supplier_1')
        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.factory = self.package.get_two_stage_factory()

        self.package.package_file = open(get_test_filepath('data', 'cysix_test_template', 'cysix.xml'))
        super(TestCysixTemplate, self).setUp()

    def tearDown(self):
        self.package.package_file.close()

        return super(TestCysixTemplate, self).tearDown()

    @replace_now(datetime(2013, 5, 15))
    def testTemplate(self):
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()
