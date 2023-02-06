# -*- coding: utf-8 -*-

from common.models.schedule import Supplier
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.two_stage_import import CysixGroupFilter
from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.tests.utils import get_test_filepath


class TestUseThreadInStationCode(TestCase):
    class_fixtures = ['test_use_thread_in_station_code.yaml']

    def setUp(self):
        self.supplier = Supplier.objects.get(code='olven')
        self.package = create_tsi_package(supplier=self.supplier)
        CysixGroupFilter.objects.create(code='olven_group_with', import_order_data=True, package=self.package,
                                        title=u'Барнаул', tsi_import_available=True, tsi_middle_available=True,
                                        use_thread_in_station_code=True)
        CysixGroupFilter.objects.create(code='olven_group_without', import_order_data=True, package=self.package,
                                        title=u'Владивосток', tsi_import_available=True, tsi_middle_available=True,
                                        use_thread_in_station_code=False)
        self.package.tsisetting.filter_by_group = True
        self.package.tsisetting.save()

        self.factory = self.package.get_two_stage_factory()

        self.package.package_file = open(get_test_filepath('data', 'test_use_thread_in_station_code', 'cysix.xml'))
        super(TestUseThreadInStationCode, self).setUp()

    def tearDown(self):
        self.package.package_file.close()

        return super(TestUseThreadInStationCode, self).tearDown()

    def testWithCode(self):
        data_provider = self.factory.get_data_provider()

        xml_threads = list(data_provider.get_xml_thread_iter_for_middle_import())

        with_code = xml_threads[0]

        sst1, sst2 = [srts.supplier_station for srts in with_code.supplier_rtstations]
        self.assertEqual(sst1.code, u'olven_group_with_vendor_102469_t-title_t-number')
        self.assertEqual(sst2.code, u'olven_group_with_vendor_100659_t-title_t-number')

        leg_sst1 = sst1.legacy_stations[0]
        self.assertEqual(leg_sst1.title, u'Старая Майна КП')
        self.assertEqual(leg_sst1.code, u'olven_group_with_vendor_102469')

        leg_sst2 = sst2.legacy_stations[0]
        self.assertEqual(leg_sst2.title, u'Волостниковка')
        self.assertEqual(leg_sst2.code, u'olven_group_with_vendor_100659')

    def testWithoutCode(self):
        data_provider = self.factory.get_data_provider()

        xml_threads = list(data_provider.get_xml_thread_iter_for_middle_import())

        with_code = xml_threads[1]

        sst1, sst2 = [srts.supplier_station for srts in with_code.supplier_rtstations]
        self.assertEqual(sst1.code, u'olven_group_without_vendor_102469')
        self.assertEqual(sst2.code, u'olven_group_without_vendor_100659')

        self.assertEqual(len(sst1.legacy_stations), 0)
        self.assertEqual(len(sst2.legacy_stations), 0)
