# coding: utf-8

from datetime import datetime

from django.core.files.base import ContentFile

from common.models.schedule import RThread
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station, create_supplier
from travel.rasp.admin.timecorrection.models import PathSpan
from travel.rasp.admin.timecorrection.utils import Constants
from travel.rasp.admin.www.models.geo import RoutePath


cysix_template = """<?xml version='1.0' encoding='utf8'?>
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
                <!-- valid geometry-->
                <thread number="103x18">
                    <stoppoints>
                        <stoppoint station_title="Москва" station_code="1" {0}/>
                        <stoppoint station_title="Тула" station_code="2" {1}/>
                        <stoppoint station_title="Елец" station_code="3" {2}/>
                        <stoppoint station_title="Воронеж" station_code="4" {3}/>
                        <stoppoint station_title="Саратов" station_code="5" {4}/>
                    </stoppoints>
                    <schedules>
                        <schedule period_start_date="2011-07-29" period_end_date="2016-07-29" days="ежедневно"
                                  times="10:00:00"/>
                    </schedules>
                </thread>
            </threads>
        </group>
    </channel>"""


class CysixArrivalTimeCorrectionByMapTest(TestCase, CheckThreadMixin):
    def setUp(self):
        supplier = create_supplier(title=u'Олвен')
        self.package = create_tsi_package(supplier=supplier)
        self.package.add_default_filters()

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

        # <PathSpan: Москва -> Тула время: 154.84 расстояние 184.29>
        PathSpan.objects.create(station_from=station_a, station_to=station_b, duration=154.84, distance=184.29,
                                is_one_country_path=True)
        # <PathSpan: Тула -> Елец время: 230.3 расстояние 213.63>
        PathSpan.objects.create(station_from=station_b, station_to=station_c, duration=230.3, distance=213.63,
                                is_one_country_path=True)
        # <PathSpan: Елец -> Воронеж время: 136.0 расстояние 133.6>
        PathSpan.objects.create(station_from=station_c, station_to=station_d, duration=136.0, distance=133.6,
                                is_one_country_path=True)
        # <PathSpan: Воронеж -> Саратов время: 955.88 расстояние 940.41>
        PathSpan.objects.create(station_from=station_d, station_to=station_e, duration=955.88, distance=940.41,
                                is_one_country_path=True)

        RoutePath.objects.create(station_from=station_a, station_to=station_b, status_direct=RoutePath.STATUS_CONFIRMED,
                                 for_two_directions=False)
        RoutePath.objects.create(station_from=station_b, station_to=station_c, status_direct=RoutePath.STATUS_CONFIRMED,
                                 for_two_directions=False)
        RoutePath.objects.create(station_from=station_c, station_to=station_d, status_direct=RoutePath.STATUS_CONFIRMED,
                                 for_two_directions=False)
        RoutePath.objects.create(station_from=station_d, station_to=station_e, status_direct=RoutePath.STATUS_CONFIRMED,
                                 for_two_directions=False)

    def reimport(self, template, station_params):
        content = self.make_cysix(template, station_params)

        self.package.package_file = ContentFile(name=u'cysix.xml', content=content)

        self.factory = self.package.get_two_stage_factory()

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

    def make_cysix(self, template, station_params):
        stations_attr = []

        for arrival_shift_in_minutes, departure_shift_in_minutes, _ in station_params:
            attributes = ''

            if arrival_shift_in_minutes is not None:
                attributes += ' arrival_shift="{}"'.format(arrival_shift_in_minutes * Constants.SECONDS_IN_MINUTE)

            if departure_shift_in_minutes is not None:
                attributes += ' departure_shift="{}"'.format(departure_shift_in_minutes * Constants.SECONDS_IN_MINUTE)

            stations_attr.append(attributes)

        return template.format(*stations_attr)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithDisableFilter(self):
        """
        Проверка импорта выключенном фильтре correct_arrival_time_by_map
        """
        self.assertFalse(self.package.can_use_filter('correct_arrival_time_by_map'))

        self.reimport(
            cysix_template,
            [
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (670, None, 'Europe/Moscow'),
            ])

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            if thread.hidden_number == '103x18':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None, 0, 'Europe/Moscow'),
                        (94, 95, 'Europe/Moscow'),
                        (193, 194, 'Europe/Moscow'),
                        (255, 256, 'Europe/Moscow'),
                        (670, None, 'Europe/Moscow'),
                    ]
                )

        self.assertEqual(PathSpan.objects.count(), 4)
        self.assertEqual(threads.count(), 1)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilter(self):
        """
        Проверка импорта включенном фильтре correct_arrival_time_by_map
        """
        self.package.set_filter_use('correct_arrival_time_by_map', True)
        self.assertTrue(self.package.can_use_filter('correct_arrival_time_by_map'))

        self.reimport(
            cysix_template,
            [
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (670, None, 'Europe/Moscow'),
            ])

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            if thread.hidden_number == '103x18':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None, 0, 'Europe/Moscow'),
                        (155, 156, 'Europe/Moscow'),
                        (385, 386, 'Europe/Moscow'),
                        (521, 522, 'Europe/Moscow'),
                        (1477, None, 'Europe/Moscow'),
                    ]
                )

        self.assertEqual(PathSpan.objects.count(), 4)
        self.assertEqual(threads.count(), 1)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilterAndDisableOld(self):
        """
        Проверка импорта включенном фильтре correct_arrival_time_by_map и выключенном correct_departure_and_arrival
        """
        self.package.set_filter_use('correct_arrival_time_by_map', True)
        self.assertTrue(self.package.can_use_filter('correct_arrival_time_by_map'))

        self.package.set_filter_use('correct_departure_and_arrival', False)
        self.reimport(
            cysix_template,
            [
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (670, None, 'Europe/Moscow'),
            ])

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            if thread.hidden_number == '103x18':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None, 0, 'Europe/Moscow'),
                        (155, 156, 'Europe/Moscow'),
                        (385, 386, 'Europe/Moscow'),
                        (521, 522, 'Europe/Moscow'),
                        (1477, None, 'Europe/Moscow'),
                    ]
                )

        self.assertEqual(PathSpan.objects.count(), 4)
        self.assertEqual(threads.count(), 1)

    @replace_now(datetime(2013, 5, 15))
    def testImportWithEnableFilterAndDisableOldWithoutLastStation(self):
        """
        Проверка импорта включенном фильтре correct_arrival_time_by_map и выключенном correct_departure_and_arrival
        В импортируемых данных отсутствует время прибытия на конечную станцию
        """
        self.package.set_filter_use('correct_arrival_time_by_map', True)
        self.assertTrue(self.package.can_use_filter('correct_arrival_time_by_map'))

        self.package.set_filter_use('correct_departure_and_arrival', False)
        self.reimport(
            cysix_template,
            [
                (None, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
                (None, 386, 'Europe/Moscow'),
                (387, None, 'Europe/Moscow'),
                (None, None, 'Europe/Moscow'),
            ])

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        for thread in threads:
            if thread.hidden_number == '103x18':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None, 0, 'Europe/Moscow'),
                        (155, 156, 'Europe/Moscow'),
                        (385, 386, 'Europe/Moscow'),
                        (521, 522, 'Europe/Moscow'),
                        (1477, None, 'Europe/Moscow'),
                    ]
                )

        self.assertEqual(PathSpan.objects.count(), 4)
        self.assertEqual(threads.count(), 1)
