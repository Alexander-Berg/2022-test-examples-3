# -*- coding: utf-8 -*-

import os.path
from datetime import datetime

from common.models.schedule import Supplier, RThread
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'test_stoppoint_flags', filename)


class TestStoppointFlags(TestCase):
    """
    Все что нужно для тестирования пакета общего формата в минимальном варианте
    """

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
        self.package = TwoStageImportPackage.objects.get(supplier=self.supplier)
        TSISetting.objects.get_or_create(package=self.package)
        self.package.tsisetting.set_number = True
        self.package.tsisetting.save()

        self.factory = self.package.get_two_stage_factory()

        self.package.package_file = open(
            get_test_filepath('cysix.xml')
        )
        super(TestStoppointFlags, self).setUp()

    def tearDown(self):
        self.package.package_file.close()

        return super(TestStoppointFlags, self).tearDown()

    @replace_now(datetime(2013, 5, 15))
    def testFlags(self):
        importer = self.factory.get_two_stage_importer()
        importer.reimport_package()

        thread = RThread.objects.get(number='t-number')

        path = list(thread.path)
        self.assertEqual(len(path), 5)

        # Копирование из общего
        self.assertEqual(path[0].in_station_schedule, False)
        self.assertEqual(path[0].is_searchable_to,    True)
        self.assertEqual(path[0].is_searchable_from,  True)
        self.assertEqual(path[0].in_thread,           True)

        self.assertEqual(path[1].in_station_schedule, True)
        self.assertEqual(path[1].is_searchable_to,    False)
        self.assertEqual(path[1].is_searchable_from,  False)
        self.assertEqual(path[1].in_thread,           True)

        self.assertEqual(path[2].in_thread,           False)

        # Копирование из нашей станции
        self.assertEqual(path[3].in_thread,           False)

        self.assertEqual(path[4].in_station_schedule, False)
        self.assertEqual(path[4].is_searchable_to,    True)
        self.assertEqual(path[4].is_searchable_from,  False)
        self.assertEqual(path[4].in_thread,           True)
