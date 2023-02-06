# -*- coding: utf-8 -*-
from datetime import datetime

from common.models.geo import StationMajority
from common.models.transport import TransportType, TransportModel
from cysix.base import CysixTransportModelFinder

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import TransportModelMapping, StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station
from cysix.tests.utils import get_test_filepath


class TrasnportModelFinderTest(TestCase, LogHasMessageMixIn):
    class_fixtures = ['test_vehicles.yaml']

    @replace_now(datetime(2013, 2, 4))
    def setUp(self):
        super(TrasnportModelFinderTest, self).setUp()
        self.package = create_tsi_package()
        self.supplier = self.package.supplier
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
        self.t_model = TransportModel.objects.get(title='1')
        TransportModelMapping.objects.create(t_model=self.t_model, title='1', code='g1_vendor_1',
                                             supplier=self.package.supplier)

        self.factory = self.package.get_two_stage_factory()

        with open(get_test_filepath('data', 'test_vehicles', 'cysix.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

    def testAll(self):
        self._testSupportedSystems()
        self._testMappingVendor()
        self._testExistedTitleVendor()
        self._testCreateVendor()
        self._testNotFoundRefVendor()
        self._testBadTitleVendor()
        self._testCreateLocal()

    def _testSupportedSystems(self):
        for system in CysixTransportModelFinder.SUPPORTED_CODE_SYSTEMS:
            number_code = '%s_with_title' % system
            t_model = self.get_obj_from_fixture('test_vehicles.yaml', 'www.transportmodel', number_code)
            thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
            self.assertEqual(thread.t_model, t_model)

            number_code = '%s_without_title' % system
            t_model = self.get_obj_from_fixture('test_vehicles.yaml', 'www.transportmodel', number_code)
            thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
            self.assertEqual(thread.t_model, t_model)

        self.assertTrue(TransportModelMapping.objects.get(title='aaa', code='g1_oag_22'))

    def _testMappingVendor(self):
        number_code = 'mapping_vendor'
        thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
        self.assertEqual(thread.t_model, self.t_model)

    def _testExistedTitleVendor(self):
        number_code = 'existed_title_vendor'
        t_model = self.get_obj_from_fixture('test_vehicles.yaml', 'www.transportmodel', number_code)
        thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
        self.assertTrue(TransportModelMapping.objects.filter(title=u"2", code="g1_vendor_2"))
        self.assertEqual(thread.t_model, t_model)

    def _testCreateVendor(self):
        number_code = 'create_vendor'
        mapping = TransportModelMapping.objects.get(title=u"3", code="g1_vendor_3")
        thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
        self.assertEqual(thread.t_model, mapping.t_model)

    def _testNotFoundRefVendor(self):
        #self.assertLogHasMessage(u"ERROR: Пропускаем <CysixXmlThread: title='Караганда - Егиндыбулак' number='not_found_ref_vendor' <Group: code='g1'>>: Не нашли в справочнике модели транспорта с кодом 4 система vendor")
        pass

    def _testBadTitleVendor(self):
        self.assert_log_has_message(u'ERROR: Пропускаем <CysixXmlThread: title="Караганда - Егиндыбулак" number="test_bad_title_vendor" sourceline=186 <Group title="" code="g1" sourceline=3>>: Не указано название у модели транспорта с кодом 5 система vendor')

    def _testCreateLocal(self):
        number_code = 'create_local'
        mapping = TransportModelMapping.objects.get(title=u"33", code="g1_local")
        thread = RThread.objects.get(hidden_number=number_code, route__supplier=self.supplier)
        self.assertEqual(thread.t_model, mapping.t_model)
