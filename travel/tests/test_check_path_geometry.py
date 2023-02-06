# -*- coding: utf-8 -*-

from datetime import datetime

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station, create_supplier
from travel.rasp.admin.timecorrection.models import PermanentPathData


xml_code = """<?xml version='1.0' encoding='utf8'?>
<channel t_type="bus" version="1.0" station_code_system="vendor" timezone="start_station">
    <group code="default">
        <stations>
            <station code="1" title="Москва" />
            <station code="2" title="Тула" />
            <station code="3" title="Елец" />
            <station code="4" title="Воронеж" />
            <station code="5" title="Саратов" />
        </stations>
        <threads>
            <!-- invalid geometry-->
            <thread number="3x3">
                <stoppoints>
                    <stoppoint station_title="Москва" station_code="1" departure_time="13:00:00"/>
                    <stoppoint station_title="Воронеж" station_code="4"/>
                    <stoppoint station_title="Тула" station_code="2"/>
                    <stoppoint station_title="Елец" station_code="3"/>
                    <stoppoint station_title="Саратов" station_code="5"  arrival_time="14:10:00"/>
                </stoppoints>
                <schedules>
                    <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="135" times="13:00:00"/>
                </schedules>
            </thread>
            <!-- valid geometry-->
            <thread number="103x18">
                <stoppoints>
                    <stoppoint station_title="Москва" station_code="1" departure_time="13:00:00"/>
                    <stoppoint station_title="Тула" station_code="2"/>
                    <stoppoint station_title="Елец" station_code="3"/>
                    <stoppoint station_title="Воронеж" station_code="4"/>
                    <stoppoint station_title="Саратов" station_code="5"  arrival_time="14:10:00"/>
                </stoppoints>
                <schedules>
                    <schedule period_start_date="2011-07-29" period_end_date="2016-07-29" days="ежедневно"
                              times="13:00:00"/>
                </schedules>
            </thread>
        </threads>
    </group>
</channel>"""


class CysixCheckPathGeometryTest(TestCase):
    def setUp(self):
        supplier = create_supplier(title=u'Олвен')
        self.package = create_tsi_package(supplier=supplier)
        self.package.add_default_filters()
        self.package.package_file = ContentFile(name=u'cysix.xml', content=xml_code)

        station_a = create_station(title=u'Москва', latitude=55.715251431239594, longitude=37.6275861640625)
        station_b = create_station(title=u'Тула', latitude=54.17068792209795, longitude=37.66315182476469)
        station_c = create_station(title=u'Елец', latitude=52.60788480678189, longitude=38.49156461244351)
        station_d = create_station(title=u'Воронеж', latitude=51.68544685486367, longitude=39.162569733771484)
        station_e = create_station(title=u'Саратов', latitude=53.182031605461795, longitude=50.16952168412026)

        StationMapping.objects.create(station=station_a, code='default_vendor_1', title=u'Москва',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=station_b, code='default_vendor_2', title=u'Тула',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=station_c, code='default_vendor_3', title=u'Елец',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=station_d, code='default_vendor_4', title=u'Воронеж',
                                      supplier=self.package.supplier)
        StationMapping.objects.create(station=station_e, code='default_vendor_5', title=u'Саратов',
                                      supplier=self.package.supplier)

    def reimport(self):
        self.factory = self.package.get_two_stage_factory()

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testImportWithDisableFilter(self):
        """
        Проверка импорта выключенном фильтре check_path_geometry
        Нитки обновляются.(update_threads == True)
        """
        self.assertTrue(self.package.tsisetting.update_threads)
        self.assertFalse(self.package.can_use_filter('check_path_geometry'))

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            self.assertFalse(thread.hidden)

        self.assertEqual(threads.count(), 2)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilter(self):
        """
        Проверка импорта при включенном фильтре check_path_geometry
        Нитки обновляются.(update_threads == True)
        """
        self.assertTrue(self.package.tsisetting.update_threads)

        self.package.set_filter_use('check_path_geometry', True)
        self.assertTrue(self.package.can_use_filter('check_path_geometry'))

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            permanent_path_data = PermanentPathData.get_from_rtstations(thread.path)
            self.assertEqual(thread.hidden, not permanent_path_data.path_correct)

        self.assertEqual(threads.filter(hidden=True).count(), 1)
        self.assertEqual(threads.count(), 2)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilterAndMemorizingData(self):
        """
        Проверка сохранения предустановленного значения для нитки при переимпорте
        """

        self.package.set_filter_use('check_path_geometry', True)
        self.assertTrue(self.package.can_use_filter('check_path_geometry'))

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            permanent_path_data = PermanentPathData.get_from_rtstations(thread.path)
            self.assertEqual(thread.hidden, not permanent_path_data.path_correct)

        self.assertEqual(threads.filter(hidden=True).count(), 1)
        self.assertEqual(threads.count(), 2)

        permanent_path_data_qs = PermanentPathData.objects.filter(path_correct=False)

        self.assertEqual(permanent_path_data_qs.count(), 1)

        permanent_path_data_qs.update(path_correct=True)

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            permanent_path_data = PermanentPathData.get_from_rtstations(thread.path)
            self.assertEqual(thread.hidden, not permanent_path_data.path_correct)

        self.assertEqual(threads.filter(hidden=True).count(), 0)
        self.assertEqual(threads.count(), 2)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilterAndUpdateMemorizingData(self):
        """
        Проверка обновления предустановленного значения для нитки при переимпорте
        """
        self.package.set_filter_use('check_path_geometry', True)
        self.package.set_filter_parameter('check_path_geometry', 'update_data', True)

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            permanent_path_data = PermanentPathData.get_from_rtstations(thread.path)
            self.assertEqual(thread.hidden, not permanent_path_data.path_correct)

        self.assertEqual(threads.filter(hidden=True).count(), 1)
        self.assertEqual(threads.count(), 2)

        PermanentPathData.objects.filter(path_correct=False).update(path_correct=True)

        self.reimport()

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            permanent_path_data = PermanentPathData.get_from_rtstations(thread.path)
            self.assertEqual(thread.hidden, not permanent_path_data.path_correct)

        self.assertEqual(threads.filter(hidden=True).count(), 1)
        self.assertEqual(threads.count(), 2)
