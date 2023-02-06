# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import datetime

import httpretty
import mock
import os
import pytest

from django.conf import settings

import library.python.resource
from common.db.mds.clients import mds_s3_common_client
from common.models.geo import StationMajority
from common.models.schedule import Route, RThread
from common.models.transport import TransportType
from common.settings.configuration import Configuration
from common.tester.factories import create_supplier
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.unittests import LogHasMessageMixIn, replace_now, MaskComparisonMixIn
from travel.rasp.admin.lib.unittests.testcase import TestCase
from tester.factories import create_station
from cysix.tests.utils import get_test_filepath


@pytest.mark.usefixtures('http_allowed')
class CysixImportTest(TestCase, LogHasMessageMixIn, MaskComparisonMixIn):
    def setUp(self):
        self.supplier = create_supplier(code='code_666')
        self.package = create_tsi_package(id=666, supplier=self.supplier)
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
        self.factory = self.package.get_two_stage_factory()

        super(CysixImportTest, self).setUp()

    @replace_now(datetime.datetime(2013, 2, 4))
    def testTimes(self):
        with open(get_test_filepath('data', 'test_import', 'test_times.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 1)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testTimes2(self):
        with open(get_test_filepath('data', 'test_import', 'test_times_2.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 1)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testTimes3(self):
        with open(get_test_filepath('data', 'test_import', 'test_times_3.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 1)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testTimesError(self):
        with open(get_test_filepath('data', 'test_import', 'test_times_error.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 0)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testTimesError2(self):
        with open(get_test_filepath('data', 'test_import', 'test_times_error_2.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 0)

    @replace_now(datetime.datetime(2013, 2, 4))
    def testMiddleImport(self):
        with open(get_test_filepath('data', 'test_import', 'threads.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assertEqual(Route.objects.filter(two_stage_package=self.package).count(), 1)

    @httpretty.activate
    @replace_now(datetime.datetime(2013, 2, 4))
    def testImportDaysMasks(self):
        httpretty.register_uri(
            httpretty.GET,
            uri='https://calendar.yandex.ru/export/holidays.xml?start_date=2013-01-01&end_date=2013-12-31&country_id=225&out_mode=all',  # noqa
            content_type='text/xml; charset=utf-8',
            body=library.python.resource.find('tester/data/yandex_calendar_2013_rus.xml')
        )

        with open(get_test_filepath('data', 'test_import', 'test_days_masks.xml')) as f:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.reimport_package()

        self.assert_log_has_message(
            u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" '
            u'number="every-over-day-without-start" sourceline=110 '
            u'<Group title="" code="g1" sourceline=3>>: Пропускаем маршрут: '
            u'Через день всегда должен сопровождаться модификатором "с <дата>"'
        )
        self.assert_log_has_message(
            u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" '
            u'number="every-third-day-without-start" sourceline=132 '
            u'<Group title="" code="g1" sourceline=3>>: Пропускаем маршрут: '
            u'Через n дней всегда должен сопровождаться модификатором "с <дата>"'
        )
        self.assert_log_has_message(
            u'ERROR: <CysixXmlThread: title="Караганда - Егиндыбулак" '
            u'number="invalid-mask" sourceline=143 '
            u'<Group title="" code="g1" sourceline=3>>: Пропускаем маршрут: '
            u'Не поддеживаемый формат, маски дней хождений "ошибочная маска дней хождения"'
        )
        self.assert_log_has_message(
            u'ERROR: Пропускаем <CysixXmlThread: title="Караганда - Егиндыбулак" '
            u'number="empty-mask" sourceline=154 '
            u'<Group title="" code="g1" sourceline=3>>: '
            u'Пустая маска у маршрута'
        )

        mask_range = datetime.date(2013, 2, 4), datetime.date(2013, 2, 18)
        mask_range_with_holiday = datetime.date(2013, 3, 1), datetime.date(2013, 3, 12)

        threads = RThread.objects.filter(route__two_stage_package=self.package)
        self.assertEqual(len(threads), 23)

        for thread in threads:
            number = thread.hidden_number

            if number in ('1234567', 'daily', 'daily-en'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4  # 5  # 6  # 7  # 8  # 9  #10
#11  #12  #13  #14  #15  #16  #17
#18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number in ('even', 'even-en'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4    5  # 6    7  # 8    9  #10
 11  #12   13  #14   15  #16   17
#18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number in ('odd', 'odd-en'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
  4  # 5    6  # 7    8  # 9   10
#11   12  #13   14  #15   16  #17
 18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number == 'every-over-day-from-8':
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
  4    5    6    7  # 8    9  #10
 11  #12   13  #14   15  #16   17
#18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number == 'daily-except-15-16':
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4  # 5  # 6  # 7  # 8  # 9  #10
#11  #12  #13  #14   15   16  #17
#18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            # через 2 дня (каждый третий день, начиная с 4)
            elif number == 'every-third-day-from-4':
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4    5    6  # 7    8    9  #10
 11   12  #13   14   15  #16   17
 18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number in ('workdays', 'workdays-2', 'workdays-3', 'workdays-4'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range_with_holiday[0]),
                u"""
  март 2013
 пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3
# 4  # 5  # 6  # 7    8    9   10
#11  #12   13   14   15   16   17
 18   19   20   21   22   23   24
 25   26   27   28   29   30   31
                """,
                                                            mask_range_with_holiday)

            elif number in ('weekends-and-holidays', 'weekends-and-holidays-2', 'weekends-and-holidays-3'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range_with_holiday[0]),
                u"""
  март 2013
 пн   вт   ср   чт   пт   сб   вс
                      1  # 2  # 3
  4    5    6    7  # 8  # 9  #10
 11   12   13   14   15   16   17
 18   19   20   21   22   23   24
 25   26   27   28   29   30   31
                """,
                                                            mask_range_with_holiday)

            elif number in ('days-delimiter', 'days-delimiter-2', 'days-delimiter-3'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
  4  # 5  # 6  # 7    8    9   10
 11   12   13   14   15   16   17
 18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            elif number in ('exclude-days-delimiter', 'exclude-days-delimiter-2', 'exclude-days-delimiter-3'):
                self.assert_mask_equal_description_in_range(thread.get_mask(mask_range[0]),
                u"""
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4    5    6    7  # 8  # 9  #10
#11  #12  #13  #14  #15  #16  #17
#18   19   20   21   22   23   24
 25   26   27   28
                """,
                                                            mask_range)

            else:
                self.fail(u'Неизвестный номер рейса %s' % number)

    @replace_setting('MDS_ENABLE_WRITING', True)
    @replace_setting('APPLIED_CONFIG', Configuration.TESTING)
    @replace_now(datetime.datetime(2021, 2, 2))
    def test_import_data_upload(self):
        file_path = get_test_filepath('data', 'test_import', 'test_times.xml')
        with open(file_path) as f, \
                mock.patch.object(mds_s3_common_client.client, 'upload_file') as m_upload_file:
            self.package.package_file = f

            importer = self.factory.get_two_stage_importer()

            importer.check_xml()

            assert m_upload_file.mock_calls == [
                mock.call(
                    Key='schedule-temporary/code_666/666/2021-02-02/travel/rasp/admin/cysix/tests/data/test_import/test_times.xml',
                    Filename=os.path.join(settings.SCHEDULE_TEMPORARY_PATH, 'code_666/666/2021-02-02', file_path),
                    Bucket=mds_s3_common_client.bucket
                )
            ]
