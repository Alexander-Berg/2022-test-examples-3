# -*- coding: utf-8 -*-
from datetime import datetime

from common.models.schedule import RThread, Supplier
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.lib.unittests import replace_now
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.lib.unittests.testcase import TestCase
from cysix.tests.utils import get_test_filepath


class CysixArrivalAndDepartureCorrectionTest(TestCase, CheckThreadMixin):
    class_fixtures = [
        'travel.rasp.admin.tester.fixtures.www:countries.yaml',
        'travel.rasp.admin.tester.fixtures.www:regions.yaml',
        'settlement.yaml',
        'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
        'travel.rasp.admin.tester.fixtures.www:stations.yaml',

        'test_arrival_and_departure_correction.yaml',
    ]

    def setUp(self):
        olven = Supplier.objects.get(title=u'Олвен')

        self.package = create_tsi_package(supplier=olven)
        self.package.add_default_filters()

        self.factory = self.package.get_two_stage_factory()

        super(CysixArrivalAndDepartureCorrectionTest, self).setUp()

    def reimport(self, filename):
        with open(get_test_filepath('data', 'test_arrival_and_departure_correction', filename)) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

    @replace_now(datetime(2013, 5, 15))
    def testImportAllWithCheckAndCorrectionByTime(self):
        self.reimport('cysix.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 11)

    @replace_now(datetime(2013, 5, 15))
    def testCorrectionByTime(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_time',
            't_max': 480,
            't_super_max': 1440,

            'correction': 'by_time',
            'use_on_time_correction': 'equal_time',
            't_const': 5,
        })

        self.reimport('cysix_by_time_correction.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 3)

        for thread in threads:
            if thread.hidden_number == '3x32':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,  0,   'Europe/Moscow'),
                        (19,   20,   'Europe/Moscow'),
                        (39,   40,   'Europe/Moscow'),
                        (59,   None, 'Europe/Moscow'),
                    ]
                )

            elif thread.hidden_number == '3x32_a_d_incorrect':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,  0,   'Europe/Moscow'),
                        (19,   20,   'Europe/Moscow'),
                        (39,   40,   'Europe/Moscow'),
                        (59,   None, 'Europe/Moscow'),
                    ]
                )

            elif thread.hidden_number == '3x32_big_times':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,   0,  'Europe/Moscow'),
                        (240,  241,  'Europe/Moscow'),
                        (600,  601,  'Europe/Moscow'),
                        (606,  None, 'Europe/Moscow'),
                    ]
                )

            else:
                self.fail(u'Странный номер маршрута "%s"' % thread.hidden_number)

    @replace_now(datetime(2013, 5, 15))
    def testCorrectionByConstTime(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_time',
            't_max': 480,
            't_super_max': 1440,

            'correction': 'by_time',
            'use_on_time_correction': 'const_time',
            't_const': 5,
        })

        self.reimport('cysix_by_time_correction.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 3)

        for thread in threads:
            if thread.hidden_number == '3x32':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,  0,   'Europe/Moscow'),
                        ( 5,    6,   'Europe/Moscow'),
                        (11,   12,   'Europe/Moscow'),
                        (59,   None, 'Europe/Moscow'),
                    ]
                )

            elif thread.hidden_number == '3x32_a_d_incorrect':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,  0,   'Europe/Moscow'),
                        ( 5,    6,   'Europe/Moscow'),
                        (11,   12,   'Europe/Moscow'),
                        (59,   None, 'Europe/Moscow'),
                    ]
                )

            elif thread.hidden_number == '3x32_big_times':
                self.assertThreadStopsTimes(
                    thread,
                    [
                        (None,   0,  'Europe/Moscow'),
                        (240,  241,  'Europe/Moscow'),
                        (600,  601,  'Europe/Moscow'),
                        (606,  None, 'Europe/Moscow'),
                    ]
                )

            else:
                self.fail(u'Странный номер маршрута "%s"' % thread.hidden_number)

    @replace_now(datetime(2013, 5, 15))
    def testCorrectionBySupplierDistance(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_time',
            't_max': 480,
            't_super_max': 1440,

            'correction': 'by_supplier_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_by_distance_correction.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                (10,   11,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (27,   28,   'Asia/Yekaterinburg'),
                (33,   34,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testCorrectionByOurDistance(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_time',
            't_max': 480,
            't_super_max': 1440,

            'correction': 'by_our_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_by_distance_correction.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                ( 1,    2,   'Asia/Yekaterinburg'),
                ( 9,   10,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (26,   27,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testCheckAndCorrectionByOurDistance(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_supplier_distance',

            'correction': 'by_supplier_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_by_distance_correction.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                (10,   11,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (27,   28,   'Asia/Yekaterinburg'),
                (33,   34,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testCorrectionByCombineDistance(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'ratio_s_min': 1.0,
            'ratio_s_max': 1.3,

            'check': 'by_time',
            't_max': 480,
            't_super_max': 1440,

            'correction': 'by_combine_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_by_distance_correction.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                ( 1,    2,   'Asia/Yekaterinburg'),
                ( 9,   10,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (33,   34,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testCheckAndCorrectionByCombineDistance(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'ratio_s_min': 1.0,
            'ratio_s_max': 1.3,

            'check': 'by_combine_distance',

            'correction': 'by_combine_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_by_distance_correction.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                ( 1,    2,   'Asia/Yekaterinburg'),
                ( 9,   10,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (33,   34,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testSeveralEqualTimes(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_our_distance',

            'correction': 'by_our_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_several_equal_times.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                ( 1,    2,   'Asia/Yekaterinburg'),
                ( 9,   10,   'Asia/Yekaterinburg'),
                (21,   22,   'Asia/Yekaterinburg'),
                (26,   27,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testDayShift(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_our_distance',

            'correction': 'by_our_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_day_shift.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        self.assertThreadStopsTimes(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg'),
                ( 4,    5,   'Asia/Yekaterinburg'),
                (14,   15,   'Asia/Yekaterinburg'),
                (24,   25,   'Asia/Yekaterinburg'),
                (34,   35,   'Asia/Yekaterinburg'),
                (40,   None, 'Asia/Yekaterinburg'),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testNonstopAndTechnical(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_our_distance',

            'correction': 'by_our_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_nonstop_technical.xml')

        rthreads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(rthreads), 1)

        usual_flags = {
            'is_fuzzy': False,
            'is_searchable_to': True,
            'is_searchable_from': True,
            'in_station_schedule': True,
        }

        nonstop_flags = technical_flags = {
            'is_fuzzy': False,
            'is_searchable_to': False,
            'is_searchable_from': False,
            'in_station_schedule': False,
        }

        self.assertThreadStopsTimesAndFlags(
            rthreads[0],
            [
                (None,  0,   'Asia/Yekaterinburg', usual_flags),
                ( 3,    5,   'Asia/Yekaterinburg', usual_flags),
                (13,   13,   'Asia/Yekaterinburg', nonstop_flags),
                (23,   25,   'Asia/Yekaterinburg', usual_flags),
                (33,   35,   'Asia/Yekaterinburg', technical_flags),
                (40,   None, 'Asia/Yekaterinburg', usual_flags),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testZero(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'do_verbose_logging': True,

            'check': 'by_our_distance',

            'correction': 'by_our_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 60,
        })

        self.reimport('cysix_zero_bug.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 5)

        for thread in threads:
            self.assertThreadStopsTimes(
                thread,
                [
                    (None,  0,   'Asia/Yekaterinburg'),
                    ( 1,    6,   'Asia/Yekaterinburg'),
                    (15,   16,   'Asia/Yekaterinburg'),
                    (25,   26,   'Asia/Yekaterinburg'),
                    (35,   36,   'Asia/Yekaterinburg'),
                    (40,   None, 'Asia/Yekaterinburg'),
                ]
            )

    @replace_now(datetime(2013, 5, 15))
    def testTrustDayShiftTrue(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'trust_day_shift': True,

            'check': 'by_supplier_distance',
            'v_min': 5,
            'v_max': 200,

            'correction': 'by_supplier_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 1,
        })

        self.reimport('cysix_trust_day_shift.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package,
                                         hidden_number='incorrect_day_shift')

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimesAndFlags(
            threads[0],
            [
                (None,              0,         'Asia/Yekaterinburg', None),
                (1440 + 60 - 1,     1440 + 60, 'Asia/Yekaterinburg', None),

                # нельзя добавлять сутки, поэтому время добавляется к предыдущей исходя из скорости v_avg
                (1440 + 60 + 60*60, None,      'Asia/Yekaterinburg', {'is_fuzzy': True}),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testTrustDayShiftFalse(self):
        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'trust_day_shift': False,

            'check': 'by_supplier_distance',
            'v_min': 5,
            'v_max': 200,

            'correction': 'by_supplier_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 1,
        })

        self.reimport('cysix_trust_day_shift.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package,
                                         hidden_number='incorrect_day_shift')

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimesAndFlags(
            threads[0],
            [
                (None,           0,         'Asia/Yekaterinburg', None),
                (1440 + 60 - 1,  1440 + 60, 'Asia/Yekaterinburg', None),

                # Можно просто добавлять сутки
                (1440 + 60 + 60, None,      'Asia/Yekaterinburg', {'is_fuzzy': False}),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testTrustDayShiftCheckIndependence(self):
        """
        Установим для проверки времен стоянки trust_day_shift, который переопределит can_add_day_*
        Он не должен повлиять на установки correct_departure_and_arrival
        """
        self.package.set_filter_parameter('correct_stop_time', 'trust_day_shift', False)

        self.package.set_filter_parameters('correct_departure_and_arrival', {
            'trust_day_shift': True,

            'check': 'by_supplier_distance',
            'v_min': 5,
            'v_max': 200,

            'correction': 'by_supplier_distance',
            'use_on_distance_correction': 'const_speed',
            'v_avg': 1,
        })

        self.reimport('cysix_trust_day_shift.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package,
                                         hidden_number='incorrect_day_shift')

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimesAndFlags(
            threads[0],
            [
                (None,              0,         'Asia/Yekaterinburg', None),
                (1440 + 60 - 1,     1440 + 60, 'Asia/Yekaterinburg', None),

                # нельзя добавлять сутки, поэтому время добавляется к предыдущей исходя из скорости v_avg
                (1440 + 60 + 60*60, None,      'Asia/Yekaterinburg', {'is_fuzzy': True}),
            ]
        )

    @replace_now(datetime(2013, 5, 15))
    def testSkipStopWithOnlyAddDay(self):
        self.package.set_filter_parameter('correct_stop_time', 'only_add_day', True)
        self.package.set_filter_parameter('correct_departure_and_arrival', 'only_add_day', True)

        self.reimport('cysix_skip_stop_with_only_add_day.xml')

        threads = RThread.objects.filter(route__two_stage_package=self.package)

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimes(
            threads[0],
            [
                (None,  0,   'Europe/Moscow'),
                # вторая (из 4) остановка пропущена
                (44,   45,   'Europe/Moscow'),
                (60,   None, 'Europe/Moscow'),
            ]
        )
