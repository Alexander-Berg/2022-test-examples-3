# -*- encoding: utf-8 -*-

import os.path
from StringIO import StringIO
from zipfile import ZipFile

import httpretty
import library

from common.models.schedule import RThread, Company
from common.models.tariffs import ThreadTariff
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask
from common.utils.title_generator import DASH
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage, TSISetting
from travel.rasp.admin.importinfo.two_stage_import.admin import get_package_importer
from travel.rasp.admin.lib.unittests import replace_now, MaskComparisonMixIn, ClassLogHasMessageMixIn, LogHasMessageMixIn
from travel.rasp.admin.lib.unittests.check_thread_mixin import CheckThreadMixin
from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.utils.import_file_storage import remove_schedule_temporary_today_dir


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml',

    'travel.rasp.admin.cysix.tests:triangle.yaml',
]


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'cysix', 'tests', 'data', 'test_triangle', filename)


def get_tariff_by_indexes(thread, index_from, index_to):
    path = list(thread.path)

    try:
        return ThreadTariff.objects.get(
            thread_uid=thread.uid,
            station_from=path[index_from].station,
            station_to=path[index_to].station,
        ).tariff

    except ThreadTariff.DoesNotExist:
        return None


class TriangleImportTest(TestCase, ClassLogHasMessageMixIn, MaskComparisonMixIn, CheckThreadMixin):
    class_fixtures = module_fixtures

    @classmethod
    @replace_now('2011-03-01 00:00:05')
    def teardown_class_rasp(cls):
        ClassLogHasMessageMixIn.tearDownClass()

        remove_schedule_temporary_today_dir(cls.tsi_package)

    @classmethod
    @httpretty.activate
    @replace_now('2011-03-01 00:00:05')
    def setup_class_rasp(cls):
        ClassLogHasMessageMixIn.setUpClass()
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2011-01-01&end_date=2011-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2011_rus.xml')
        )

        cls.today = environment.today()

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.write(get_test_filepath('test_triangle_import.xls'), arcname='timetable.xls')
            zipped.write(get_test_filepath('carriers.xls'), arcname='carriers.xls')
            zipped.writestr('stations.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        cls.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=cls.tsi_package)
        cls.tsi_package.package_file = zip_fileobj
        cls.tsi_package.save()

        remove_schedule_temporary_today_dir(cls.tsi_package)

        cls.importer = get_package_importer(cls.tsi_package)

        cls.importer.reimport_package()

        cls.thread_numbers = RThread.objects.filter(
            route__two_stage_package=cls.tsi_package).values_list('hidden_number', flat=True)

    def testCarrier(self):
        company = Company.objects.get(title=u'Тестовый треугольный перевозчик')

        self.assertEqual(company.phone, '555-555-555')
        self.assertEqual(company.address, u'Тестовый город, тестовая улица, дом 5, офис 5  http://example.com/')
        self.assertEqual(company.country.code, 'RU')

        number = '100'

        thread = list(RThread.objects.filter(hidden_number=number))[0]

        self.assertEqual(thread.company.id, company.id)

    def testTType(self):
        """ t_type должен браться из двухступенчатого пакета """

        number = '100'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет маршрута %s' % number)

        for thread in threads:
            self.assertEqual(thread.route.t_type.code, 'bus')
            self.assertEqual(thread.t_type.code, 'bus')

    def testStationsOrder(self):
        number = '100'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        for thread in threads:
            path = list(thread.path)

            self.assertEqual(len(path), 3)

            self.assertEqual(path[0].station.title, u'test_station Екатеринбург АВ')
            self.assertEqual(path[1].station.title, u'test_station Нижняя Тура')
            self.assertEqual(path[2].station.title, u'test_station Качканар')

    def testTimes(self):
        number = '100'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        self.assertThreadStopsTimes(threads[0], [
            (None, 0, 'Asia/Yekaterinburg'),
            (59, 60, 'Asia/Yekaterinburg'),
            (120, None, 'Asia/Yekaterinburg'),
        ])

    def testTariff(self):
        number = '100'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]

        self.assertEqual(3, ThreadTariff.objects.filter(thread_uid=thread.uid).count())

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        def get_tariff_by_indexes(index_from, index_to):
            return ThreadTariff.objects.get(
                thread_uid=thread.uid,
                station_from=path[index_from].station,
                station_to=path[index_to].station,
            ).tariff

        self.assertEqual(70.0, get_tariff_by_indexes(0, 1))
        self.assertEqual(350.0, get_tariff_by_indexes(0, 2))
        self.assertEqual(330.0, get_tariff_by_indexes(1, 2))

    def testTitle(self):
        number = '100'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]

        path = list(thread.path)

        self.assertEqual(thread.title, u'{} {} {}'.format(path[0].station.title, DASH, path[-1].station.title))

    def testRouteNotFromFirstStation(self):
        # Можно пропускать и первую станцию, главное проверить цены

        number = 'route_not_from_first_station'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]
        path = list(thread.path)

        self.assertEqual(len(path), 2)

        self.assertEqual(230.0, get_tariff_by_indexes(thread, 0, 1))

    def testArrivalTimes(self):
        number = '101'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        self.assertThreadStopsTimes(threads[0], [
            (None, 0, 'Asia/Yekaterinburg'),
            (55, 60, 'Asia/Yekaterinburg'),
            (115, None, 'Asia/Yekaterinburg'),
        ])

    def testFuzzy(self):
        number = 'fuzzy'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]

        path = list(thread.path)

        self.assertEqual(len(path), 2)

        self.assertFalse(path[0].is_fuzzy)

        self.assertTrue(path[1].is_fuzzy, 'this rtstation must be fuzzy')

    def testSkipOnlyOneThread(self):
        number = 'skip_only_one_thread'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        self.assertEqual(len(threads), 1)

    def testSkippedStationCode(self):
        # TODO(?): Раньше такой маршрут импортировался без этой станции

        number = 'skipped_station_code'

        self.assertTrue(number not in self.thread_numbers)

        self.assert_log_has_message(
            u'ERROR: <CysixXmlThread: title="Екатеринбург — Качканар" '
            u'number="skipped_station_code" sourceline=336 '
            u'<Group title="all" code="all" sourceline=3>>: Не указан station_code'
        )

    def testWrongTariff(self):
        number = 'wrong_tariff'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]

        self.assertEqual(2, ThreadTariff.objects.filter(thread_uid=thread.uid).count())

        path = list(thread.path)
        self.assertEqual(len(path), 3)

        self.assertEqual(120.0, get_tariff_by_indexes(thread, 0, 1))
        self.assertEqual(230.0, get_tariff_by_indexes(thread, 1, 2))

        self.assert_log_has_message(u"ERROR: Неправильно заполнен тариф 'wrong_tariff'")

    def testWrongDistance(self):
        self.assertTrue('wrong_distance' in self.thread_numbers)

        # Проверим, что есть сообщение о неправильном расстоянии
        self.assert_log_has_message(u'WARNING: Не верное значения расстояния not_a_distance')

        # Проверим, что остальные (с точкой и с запятой) расстояния правильные
        self.assert_log_has_no_message(u'WARNING: Не верное значения расстояния 39.6')
        self.assert_log_has_no_message(u'WARNING: Не верное значения расстояния 77,7')

    def testSkipRoutesWithWrongMask(self):
        self.assertTrue('wrong_mask' not in self.thread_numbers)

        self.assert_log_has_message(u"ERROR: Ошибка разбора 'wrong': Не поддеживаемый формат, маски дней хождений \"wrong\"")
        self.assert_log_has_message(u"ERROR: Ошибка разбора 'ро2.34.sd': Не поддеживаемый формат, маски дней хождений \"ро2.34.sd\"")

    def compareMaskForRoute(self, route_number, mask_description):
        threads = list(RThread.objects.filter(hidden_number=route_number))

        if not threads:
            self.fail(u'Нет ниток %s' % route_number)

        for thread in threads:
            self.assert_mask_equal_description(
                RunMask(thread.year_days, today=self.today).portion(self.today, 31),
                mask_description
            )

    def testMasksEveryday(self):
        self.compareMaskForRoute(
            'mask_everyday',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testMasksEverydayFrom2011_01_01(self):
        self.compareMaskForRoute(
            'mask_everyday_from_2011_01_01',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testMasksEverydayFrom2011_03_10(self):
        self.compareMaskForRoute(
            'mask_everyday_from_2011_03_10',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testMaskEverydayFrom2011_03_10To20(self):
        self.compareMaskForRoute(
            'mask_everyday_from_2011_03_10_to_20',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testMaskEverydayTo2011_03_25(self):
        self.compareMaskForRoute(
            'mask_everyday_to_2011_03_25',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25   26   27
                28   29   30   31
            """
        )

    def testMaskEven(self):
        self.compareMaskForRoute(
            'mask_even',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1  # 2    3  # 4    5  # 6
                 7  # 8    9  #10   11  #12   13
               #14   15  #16   17  #18   19  #20
                21  #22   23  #24   25  #26   27
               #28   29  #30   31
            """
        )

    def testMaskOdd(self):
        self.compareMaskForRoute(
            'mask_odd',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
               #21   22  #23   24  #25   26  #27
                28  #29   30  #31
            """
        )

    def testWrongMask678(self):
        self.assertTrue('wrong_mask_678' not in self.thread_numbers)

        self.assert_log_has_message(u"ERROR: Ошибка разбора '678': Не поддеживаемый формат, маски дней хождений \"678\"")

    def testMaskWeekdays167(self):
        self.compareMaskForRoute(
            'mask_weekdays_167',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4  # 5  # 6
               # 7    8    9   10   11  #12  #13
               #14   15   16   17   18  #19  #20
               #21   22   23   24   25  #26  #27
               #28   29   30   31
            """
        )

    def testMaskWithAdditions(self):
        self.compareMaskForRoute(
            'mask_2011_03_18_and_2011_03_29_and_1',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
               # 7    8    9   10   11   12   13
               #14   15   16   17  #18   19   20
               #21   22   23   24   25   26   27
               #28  #29   30   31
            """
        )

    def testWrongMaskWithSemicolon(self):
        self.assertTrue('wrong_mask_127;####' not in self.thread_numbers)

        self.assert_log_has_message(u"ERROR: Ошибка разбора '127;####': Не поддеживаемый формат, маски дней хождений \"####\"")

    def testMaskExcept(self):
        self.compareMaskForRoute(
            'mask_except',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10   11   12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testComplexMask(self):
        self.compareMaskForRoute(
            'complex_mask',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5    6
                 7    8    9   10   11   12   13
                14   15   16   17   18  #19  #20
               #21   22   23   24   25  #26  #27
               #28   29   30   31
            """
        )

    def testMaskEveryOtherDay(self):
        self.compareMaskForRoute(
            'mask_every_other_day',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testMaskEvery2Days(self):
        self.compareMaskForRoute(
            'mask_every_2_days',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3  # 4    5    6
               # 7    8    9  #10   11   12  #13
                14   15  #16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testMaskEvery2DaysMix(self):
        self.compareMaskForRoute(
            'mask_every_2_days_mix',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9   10   11   12   13
                14   15  #16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testMaskEvery3Days(self):
        self.compareMaskForRoute(
            'mask_every_3_days',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3    4  # 5    6
                 7    8  # 9   10   11   12  #13
                14   15   16  #17   18   19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testMaskEvery5Days(self):
        self.compareMaskForRoute(
            'mask_every_5_days',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3    4    5    6
               # 7    8    9   10   11   12  #13
                14   15   16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testWrongMaskEveryNDays(self):
        self.assertTrue('wrong_every_N_days' not in self.thread_numbers)
        self.assert_log_has_message(u"ERROR: Ошибка разбора 'через': Не поддеживаемый формат, маски дней хождений \"через\"")
        self.assert_log_has_message(u'ERROR: Ошибка разбора \'через 1\': Через n дней всегда должен сопровождаться модификатором "с <дата>": "через 1"')
        self.assert_log_has_message(u'ERROR: Ошибка разбора \'через 2\': Через n дней всегда должен сопровождаться модификатором "с <дата>": "через 2"')

    def testComplexMaskWithExcept(self):
        self.compareMaskForRoute(
            'complex_mask_with_except',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
               # 7    8    9   10   11   12   13
               #14   15   16   17   18   19  #20
                21   22   23   24   25   26  #27
                28   29   30   31
            """
        )

    def testMaskWithHolidays(self):
        self.compareMaskForRoute(
            'mask_with_holidays',
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3  # 4  # 5    6
                 7    8    9  #10  #11   12   13
               #14  #15   16  #17  #18   19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testUseSupplierTitleForThread(self):
        number = 'use_supplier_title'

        thread_title = u'Переопределенное название рейса'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет маршрута %s' % number)

        for thread in threads:
            self.assertEqual(thread.title, thread_title)


class TriangleImportTestSkipTimes(TestCase):
    class_fixtures = module_fixtures

    @classmethod
    def teardown_class_rasp(cls):
        remove_schedule_temporary_today_dir(cls.tsi_package)

    @classmethod
    def setup_class_rasp(cls):
        cls.today = environment.today()

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.write(get_test_filepath('test_triangle_import_skip_times.xls'), arcname='timetable.xls')
            zipped.write(get_test_filepath('carriers.xls'), arcname='carriers.xls')
            zipped.writestr('stations.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        cls.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=cls.tsi_package)
        cls.tsi_package.package_file = zip_fileobj
        cls.tsi_package.save()

        remove_schedule_temporary_today_dir(cls.tsi_package)

        cls.importer = get_package_importer(cls.tsi_package)

        cls.importer.reimport_package()

    def testSkipTimes(self):
        number = 'skip_times'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 4)

    def testSkipTimes2(self):
        number = 'skip_times2'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 4)


class TriangleImportTestAddDay(TestCase, CheckThreadMixin):
    class_fixtures = module_fixtures

    @classmethod
    def teardown_class_rasp(cls):
        remove_schedule_temporary_today_dir(cls.tsi_package)

    @classmethod
    def setup_class_rasp(cls):
        cls.today = environment.today()

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.write(get_test_filepath('test_triangle_import_add_day.xls'), arcname='timetable.xls')
            zipped.write(get_test_filepath('carriers.xls'), arcname='carriers.xls')
            zipped.writestr('stations.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        cls.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=cls.tsi_package)
        cls.tsi_package.package_file = zip_fileobj
        cls.tsi_package.add_default_filters()
        cls.tsi_package.save()

        remove_schedule_temporary_today_dir(cls.tsi_package)

        cls.importer = get_package_importer(cls.tsi_package)

        cls.importer.reimport_package()

    def testAddDay(self):
        number = 'add_days'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimes(threads[0], [
            (None,           0,       'Asia/Yekaterinburg'),
            (18 * 60 - 1,    18 * 60, 'Asia/Yekaterinburg'),
            ((24 + 10) * 60, None,    'Asia/Yekaterinburg'),
        ])

    def testAddDay2(self):
        number = 'add_days2'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimes(threads[0], [
            (None,              0,       'Asia/Yekaterinburg'),
            (16 * 60 - 1,       16 * 60, 'Asia/Yekaterinburg'),
            ((6 + 24 + 1) * 60, None,    'Asia/Yekaterinburg'),
        ])

    def testAddDay3(self):
        number = 'add_days3'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimes(threads[0], [
            (None,           0,             'Asia/Yekaterinburg'),
            (23 * 60,        (24 + 1) * 60, 'Asia/Yekaterinburg'),
            ((24 + 10) * 60, None,          'Asia/Yekaterinburg'),
        ])

    def testAddDay4(self):
        number = 'add_days4'

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 1)

        self.assertThreadStopsTimes(threads[0], [
            (None,            0,                    'Asia/Yekaterinburg'),
            ((24 + 8) * 60,   (24 + 10) * 60,       'Asia/Yekaterinburg'),
            ((24*2 + 3) * 60, (24*2 + 3) * 60 + 30, 'Asia/Yekaterinburg'),
            ((24*2 + 4) * 60, None,                 'Asia/Yekaterinburg'),
        ])


class TriangleImportTestFiles(TestCase, LogHasMessageMixIn):
    class_fixtures = module_fixtures

    def setUp(self):
        super(TriangleImportTestFiles, self).setUp()

        self.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=self.tsi_package)
        self.factory = self.tsi_package.get_two_stage_factory()

    def tearDown(self):
        remove_schedule_temporary_today_dir(self.tsi_package)

    def import_with_fileobj(self, fileobj):
        self.tsi_package.package_file = fileobj
        self.tsi_package.save()

        remove_schedule_temporary_today_dir(self.tsi_package)

        importer = self.factory.get_two_stage_importer()

        importer.reimport_package()

    def testNotZipFile(self):
        zip_fileobj = StringIO()

        zip_fileobj.write('not a zip file')
        zip_fileobj.seek(0)

        zip_fileobj.name = 'not_a_zip_file.zip'

        self.import_with_fileobj(zip_fileobj)

        self.assert_log_has_message(u'ERROR: В процессе импорта произошла ошибка. Фаил не является архивом или содержит плохие данные: Ошибка открытия zip файла')

    def testNoTimetablesFile(self):
        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.writestr('stations.csv', ' \n \n')
            zipped.writestr('carriers.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        self.import_with_fileobj(zip_fileobj)

        self.assert_log_has_message(u'ERROR: В процессе импорта произошла ошибка. Архив не содержит необходимых файлов carriers.csv|xls, stations.csv|xls, timetable.csv|xls')


class TriangleImportTestWithFilter(TestCase, ClassLogHasMessageMixIn, MaskComparisonMixIn, CheckThreadMixin):
    class_fixtures = module_fixtures

    @classmethod
    @replace_now('2011-03-01 00:00:05')
    def teardown_class_rasp(cls):
        ClassLogHasMessageMixIn.tearDownClass()

        remove_schedule_temporary_today_dir(cls.tsi_package)

    @classmethod
    @replace_now('2011-03-01 00:00:05')
    def setup_class_rasp(cls):
        ClassLogHasMessageMixIn.setUpClass()

        cls.today = environment.today()

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.write(get_test_filepath('test_triangle_import_with_filters.xls'), arcname='timetable.xls')
            zipped.writestr('stations.csv', ' \n \n')
            zipped.writestr('carriers.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        cls.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=cls.tsi_package)
        cls.tsi_package.add_default_filters()

        cls.tsi_package.set_filter_parameters('correct_departure_and_arrival', {
            'check': 'by_time',
            'correction': 'by_time',
        })

        cls.tsi_package.package_file = zip_fileobj
        cls.tsi_package.save()

        remove_schedule_temporary_today_dir(cls.tsi_package)

        cls.importer = get_package_importer(cls.tsi_package)

        cls.importer.reimport_package()

    def testSkipTimeInTheMiddle(self):
        """
        RASPADMIN-337 Если встретилось пустое врема в середине маршрута,
        то эту станцию нужно пропускать
        """

        number = 'skip_time_in_the_middle'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        thread = threads[0]
        path = list(thread.path)

        self.assertEqual(len(path), 2)

        self.assertEqual(130.0, get_tariff_by_indexes(thread, 0, 1))

    def testDayShift(self):
        number = 'day_shift'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:00
        # 8:50
        # 12:30+1

        self.assertThreadStopsTimes(threads[0], [
            (None, 0, 'Asia/Yekaterinburg'),
            (49, 50, 'Asia/Yekaterinburg'),
            (24*60 + 4*60 + 30, None, 'Asia/Yekaterinburg'),
        ])

    def testDayShift2(self):
        number = 'day_shift_2'

        threads = list(RThread.objects.filter(hidden_number=number))

        if not threads:
            self.fail(u'Нет ниток для маршрута %s' % number)

        # 8:05
        # 8:50+1/10:50  Для отправления +1 добавит фильтр коррекции времени стоянки
        # 3:30+2

        self.assertThreadStopsTimes(threads[0], [
            (None, 0, 'Asia/Yekaterinburg'),
            (24*60 + 45, 24*60 + 2*60 + 45, 'Asia/Yekaterinburg'),
            (2*24*60 - (8*60 + 5) + 3*60 + 30, None, 'Asia/Yekaterinburg'),
        ])


class TriangleImportTestSplitRoutes(TestCase, ClassLogHasMessageMixIn):
    class_fixtures = module_fixtures

    @classmethod
    def teardown_class_rasp(cls):
        ClassLogHasMessageMixIn.tearDownClass()

        remove_schedule_temporary_today_dir(cls.tsi_package)

    @classmethod
    def setup_class_rasp(cls):
        ClassLogHasMessageMixIn.setUpClass()

        cls.today = environment.today()

        zip_fileobj = StringIO()
        with ZipFile(zip_fileobj, 'w') as zipped:
            zipped.write(get_test_filepath('test_split_errors.xls'), arcname='timetable.xls')
            zipped.writestr('stations.csv', ' \n \n')
            zipped.writestr('carriers.csv', ' \n \n')

        zip_fileobj.seek(0)
        zip_fileobj.name = 'test_triangle_import.zip'

        cls.tsi_package = TwoStageImportPackage.objects.get(title='test_tsi_package')
        TSISetting.objects.get_or_create(package=cls.tsi_package)
        cls.tsi_package.package_file = zip_fileobj
        cls.tsi_package.add_default_filters()
        cls.tsi_package.save()

        remove_schedule_temporary_today_dir(cls.tsi_package)

        cls.importer = get_package_importer(cls.tsi_package)

        cls.importer.reimport_package()

        cls.thread_numbers = RThread.objects.filter(
            route__two_stage_package=cls.tsi_package).values_list('hidden_number', flat=True)

    def testSplitRoutes(self):
        number = 'only_good_route'

        self.assertEqual(set(self.thread_numbers), {number})

        threads = list(RThread.objects.filter(hidden_number=number))

        self.assertEqual(len(threads), 1)

        self.assertTrue(self.log_has_this_in_messages(u'ERROR: Пропускаем маршрут. В строке конца маршрута не должно быть данных о станции.'))
        self.assertTrue(self.log_has_this_in_messages(u'ERROR: Пропускаем маршрут, в строчке станции, поля маршрут, название, переовзчик и автобус, должны быть пустыми.'))
        self.assertTrue(self.log_has_this_in_messages(u'ERROR: Пропускаем маршрут. В строке заголовка маршрута, не должно быть данных о станции.'))
        self.assertTrue(self.log_has_this_in_messages(u'ERROR: У последнего маршрута в файле нет признака окончания'))
