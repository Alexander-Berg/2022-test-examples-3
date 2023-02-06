# -*- coding: utf-8 -*-

import json
from datetime import date, datetime

import pytest
from django.core.files.base import ContentFile

from common.models.schedule import RThread
from travel.rasp.library.python.common23.date import environment
from common.utils.date import RunMask, daterange
from cysix.filters.text_schedule_and_extrapolation import Filter as TextScheduleAndExtrapolationFilter
from cysix.models import Filter, PackageFilter
from travel.rasp.admin.importinfo.factories import create_tsi_package
from travel.rasp.admin.importinfo.models.mappings import StationMapping
from travel.rasp.admin.lib.mask_description import run_mask_from_mask_description
from travel.rasp.admin.lib.unittests import MaskComparisonMixIn
from tester.factories import create_thread, create_supplier, create_station
from tester.testcase import TestCase
from tester.utils.datetime import replace_now


class TestCysixTextScheduleAndExtrapolation(TestCase, MaskComparisonMixIn):
    default_params = {
        'known_schedule_length': 20,
        'templates': 'buses',
        'extrapolate': True,
        'min_match_days': 7,
        'max_mismatch_days': 3,
        'extrapolation_limit_length': 320,
        'min_schedule_length_in_range': 7,
        'search_first_day_in_past_days': 0
    }

    @replace_now(datetime(2013, 1, 1))
    def test_normal_way(self):
        params = self.default_params.copy()
        thread = create_thread(year_days=str(run_mask_from_mask_description(u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
     # 1  # 2    3    4    5    6
# 7  # 8  # 9   10   11   12   13
#14  #15  #16   17   18   19   20
#21  #22  #23   24   25   26   27
#28  #29  #30   31
        """)))

        filter_ = TextScheduleAndExtrapolationFilter(params, max_forward_days=300)
        filter_.apply(thread)

        self.assert_mask_equal_description_in_range(RunMask(thread.year_days, today=environment.today()), u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
     # 1  # 2    3    4    5    6
# 7  # 8  # 9   10   11   12   13
#14  #15  #16   17   18   19   20
#21  #22  #23   24   25   26   27
#28  #29  #30   31
 ---
 февраль 2013
 пн   вт   ср   чт   пт   сб   вс
                      1    2    3
# 4  # 5  # 6    7    8    9   10
#11  #12  #13   14   15   16   17
#18  #19  #20   21   22   23   24
#25  #26  #27   28
        """, (date(2013, 1, 1), date(2013, 2, 28)))

    @replace_now(datetime(2013, 1, 1))
    def test_not_enough_days(self):
        params = self.default_params.copy()
        thread = create_thread(year_days=str(run_mask_from_mask_description(u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7    8    9   10   11   12   13
#14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
        """)))

        filter_ = TextScheduleAndExtrapolationFilter(params, max_forward_days=300)
        filter_.apply(thread)

        self.assert_mask_equal_description_in_range(RunMask(thread.year_days, today=environment.today()), u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7    8    9   10   11   12   13
#14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
        """, (date(2013, 1, 1), date(2013, 2, 28)))

    @replace_now(datetime(2013, 1, 1))
    def test_not_enough_days_2(self):
        params = self.default_params.copy()
        params.update({
            'min_schedule_length_in_range': 14
        })
        thread = create_thread(year_days=str(run_mask_from_mask_description(u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7  # 8  # 9  #10  #11  #12  #13
 14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
        """)))

        filter_ = TextScheduleAndExtrapolationFilter(params, max_forward_days=300)
        filter_.apply(thread)

        self.assert_mask_equal_description_in_range(RunMask(thread.year_days, today=environment.today()), u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7  # 8  # 9  #10  #11  #12  #13
 14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
        """, (date(2013, 1, 1), date(2013, 2, 28)))

    @replace_now(datetime(2013, 1, 1))
    def test_less_min_match_days(self):
        params = self.default_params.copy()
        params.update({
            'min_schedule_length_in_range': 14,
            'min_match_days': 2
        })

        thread = create_thread(year_days=str(run_mask_from_mask_description(u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7    8    9   10   11   12   13
#14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
        """)))

        filter_ = TextScheduleAndExtrapolationFilter(params, max_forward_days=300)
        filter_.apply(thread)

        self.assert_mask_equal_description_in_range(RunMask(thread.year_days, today=environment.today()), u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7    8    9   10   11   12   13
#14   15   16   17   18   19   20
#21   22   23   24   25   26   27
#28   29   30   31
        """, (date(2013, 1, 1), date(2013, 1, 31)))

    @replace_now(datetime(2013, 1, 1))
    def test_not_extrapolatable_mask_before_max_forward_days(self):
        """
        Если поставщик явно задал "по", и "по" наступило до максимоальной границы импорта,
        то явно пишем "по" в днях хождения
        """
        params = self.default_params.copy()
        days = daterange(date(2013, 1, 1), date(2013, 1, 20), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=False, year_days=str(mask))

        TextScheduleAndExtrapolationFilter(params, max_forward_days=90).apply(thread)

        assert RunMask(thread.year_days, today=environment.today()) == mask
        for record in json.loads(thread.translated_days_texts):
            assert u' по ' in record['ru']

    @replace_now(datetime(2013, 1, 1))
    def test_not_extrapolatable_mask_after_max_forward_days(self):
        """
        Если поставщик явно задал "по", и "по" наступит только после максимоальной границы импорта,
        то "по" в днях хождения отсутствует
        """
        params = self.default_params.copy()
        params.update({
            'extrapolation_limit_length': 90
        })

        days = daterange(date(2013, 1, 1), date(2013, 1, 20), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=False, year_days=str(mask))

        TextScheduleAndExtrapolationFilter(params, max_forward_days=15).apply(thread)

        assert RunMask(thread.year_days, today=environment.today()) == mask
        for record in json.loads(thread.translated_days_texts):
            assert u' по ' not in record['ru']

    @replace_now(datetime(2013, 1, 1))
    def test_extrapolatable_mask_and_can_extrapolate(self):
        """
        Если у поставщика не задано "по", и получилось экстраполировать,
        то "по" в днях хождения отсутствует
        """
        params = self.default_params.copy()
        params.update({
            'extrapolation_limit_length': 90
        })

        days = daterange(date(2013, 1, 1), date(2013, 1, 25), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=True, year_days=str(mask))

        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread, save=False)

        assert len(RunMask(thread.year_days, today=environment.today()).dates()) > len(mask.dates())
        for record in json.loads(thread.translated_days_texts):
            assert u' по ' not in record['ru']

    @replace_now(datetime(2011, 3, 1))
    def test_extrapolatable_mask_and_can_not_extrapolate(self):
        """
        Если у поставщика не задано "по", и не получилось экстраполировать,
        то явно пишем "по" в днях хождения
        """
        params = self.default_params.copy()

        days = daterange(date(2011, 3, 1), date(2011, 3, 26), include_end=True)  # по дату перевода часов
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=True, year_days=str(mask))

        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread)

        assert RunMask(thread.year_days, today=environment.today()) == mask
        days_texts = json.loads(thread.translated_days_texts)
        for record in days_texts:
            assert u' по ' in record['ru']
        assert u'по 26.03' in days_texts[0]['ru']
        assert u'по 27.03' in days_texts[1]['ru']
        assert u'по 28.03' in days_texts[2]['ru']
        assert u'по 29.03' in days_texts[3]['ru']

    @replace_now(datetime(2015, 1, 1))
    def test_empty_mask(self):
        """
        Проверим, что фильтр не падает при пустой маске
        """
        params = self.default_params.copy()
        thread = create_thread(has_extrapolatable_mask=True, year_days=RunMask.EMPTY_YEAR_DAYS)
        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread)

    @replace_now(datetime(2015, 1, 21))
    def test_mask_in_past(self):
        """
        Проверим, что фильтр не падает и не экстраполирует при днях хождения только в прошлом
        """
        params = self.default_params.copy()
        days = daterange(date(2015, 1, 1), date(2015, 1, 20), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=True, year_days=str(mask))
        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread)
        assert len(RunMask(thread.year_days, today=environment.today()).dates()) == len(mask.dates())

    @replace_now(datetime(2015, 1, 20))
    def test_mask_ends_today(self):
        params = self.default_params.copy()
        days = daterange(date(2015, 1, 1), date(2015, 1, 20), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=True, year_days=str(mask))
        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread)
        assert len(RunMask(thread.year_days, today=environment.today()).dates()) == len(mask.dates())

    @replace_now(datetime(2015, 1, 19))
    def test_mask_ends_tomorrow(self):
        params = self.default_params.copy()
        days = daterange(date(2015, 1, 1), date(2015, 1, 20), include_end=True)
        mask = RunMask(days=days, strict=True, today=environment.today())
        thread = create_thread(has_extrapolatable_mask=True, year_days=str(mask))
        TextScheduleAndExtrapolationFilter(params, max_forward_days=300).apply(thread)
        assert len(RunMask(thread.year_days, today=environment.today()).dates()) == len(mask.dates())

    @replace_now(datetime(2013, 1, 1))
    def test_translated_days_texts(self):
        params = self.default_params.copy()
        params.update({
            'min_match_days': 4
        })

        thread = create_thread(year_days=str(run_mask_from_mask_description(u"""
 январь 2013
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
# 7    8    9   10   11   12   13
#14   15   16   17   18   19   20
#21   22   23   24   25   26   27
#28   29   30   31
        """)))

        filter_ = TextScheduleAndExtrapolationFilter(params, max_forward_days=300)
        filter_.apply(thread)

        days_texts = json.loads(thread.translated_days_texts)
        assert u'вс' in days_texts[0]['ru']
        assert u'пн' in days_texts[1]['ru']
        assert u'вт' in days_texts[2]['ru']


@pytest.mark.dbuser
@replace_now(datetime(2015, 12, 15))
def test_period_end_date_too_far():
    """
    Если поставщик явно задал "по", и "по" наступит только после максимоальной границы импорта,
    то "по" в днях хождения отсутствует
    """

    supplier = create_supplier()
    package = create_tsi_package(supplier=supplier, package_type='cysix')
    package.tsisetting.max_forward_days = 90
    package.tsisetting.save()
    filter_ = Filter.objects.get(code='text_schedule_and_extrapolation')
    PackageFilter.objects.create(package=package, filter=filter_, parameters=filter_.default_parameters, use=True)
    package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
<group code="1">
    <stations>
        <station code="1" title="1"/>
        <station code="2" title="2"/>
    </stations>
    <threads>
        <thread title="1 - 2" t_type="bus">
          <stoppoints>
            <stoppoint station_code="1" departure_time="10:00"/>
            <stoppoint station_code="2" arrival_time="20:00"/>
          </stoppoints>
          <schedules>
            <schedule period_start_date="2015-09-01" period_end_date="2100-01-01" days="1234567"/>
          </schedules>
        </thread>
    </threads>
</group>
</channel>
        """.strip())

    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_1', title='1')
    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_2', title='2')

    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    thread = RThread.objects.get()
    days_texts = json.loads(thread.translated_days_texts)
    for record in days_texts:
        assert u' по ' not in record['ru']


@pytest.mark.dbuser
@replace_now(datetime(2015, 12, 15))
def test_period_end_date_not_too_far():
    """
    Если поставщик явно задал "по", и "по" наступило до максимоальной границы импорта,
    то явно пишем "по" в днях хождения
    """

    supplier = create_supplier()
    package = create_tsi_package(supplier=supplier, package_type='cysix')
    package.tsisetting.max_forward_days = 90
    package.tsisetting.save()
    filter_ = Filter.objects.get(code='text_schedule_and_extrapolation')
    PackageFilter.objects.create(package=package, filter=filter_, parameters=filter_.default_parameters, use=True)
    package.package_file = ContentFile(name=u'cysix.xml', content="""
<?xml version='1.0' encoding='utf-8'?>
<channel version="1.0" station_code_system="vendor" t_type="bus" timezone="local">
<group code="1">
    <stations>
        <station code="1" title="1"/>
        <station code="2" title="2"/>
    </stations>
    <threads>
        <thread title="1 - 2" t_type="bus">
          <stoppoints>
            <stoppoint station_code="1" departure_time="10:00"/>
            <stoppoint station_code="2" arrival_time="20:00"/>
          </stoppoints>
          <schedules>
            <schedule period_start_date="2015-09-01" period_end_date="2015-12-30" days="1234567"/>
          </schedules>
        </thread>
    </threads>
</group>
</channel>
        """.strip())

    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_1', title='1')
    StationMapping.objects.create(supplier=supplier, station=create_station(), code='1_vendor_2', title='2')

    factory = package.get_two_stage_factory()
    importer = factory.get_two_stage_importer()
    importer.reimport_package()

    thread = RThread.objects.get()
    days_texts = json.loads(thread.translated_days_texts)
    for record in days_texts:
        assert u' по ' in record['ru']
