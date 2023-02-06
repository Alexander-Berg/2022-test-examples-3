# coding: utf-8

from __future__ import unicode_literals

from datetime import datetime, date
from io import StringIO

import mock
from hamcrest import assert_that, contains

from common.importinfo.models import Express2Country
from common.models.geo import Country, Station, StationMajority
from common.models.schedule import RThread, Supplier, Company, DeLuxeTrain
from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from travel.rasp.admin.lib.mask_builder.bounds import MaskBounds
from travel.rasp.admin.lib.mask_builder.standard_builders import daily_mask
from travel.rasp.admin.scripts.schedule.tis_train import import_tis
from travel.rasp.admin.scripts.schedule.tis_train.import_tis import get_last_train_file_with_date
from travel.rasp.admin.scripts.schedule.tis_train.importer import TisImporter, RZD_ID
from travel.rasp.admin.scripts.schedule.tis_train.parser import parse_train_file
from tester.factories import create_train_schedule_plan, create_station, create_company, create_transport_subtype
from tester.testcase import TestCase
from tester.utils.datetime import replace_now
from travel.rasp.admin.www.models.schedule import Route2Company


def tis_import_fixture():
    Supplier.objects.create(id=2, code='tis')
    Supplier.objects.create(id=4, code='af')

    russia = Country.objects.get(id=Country.RUSSIA_ID)
    Company.objects.create(id=RZD_ID, title=u'РЖД', country=russia)

    create_station(
        id=1000, title='Начальная', country=russia,
        time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
        __=dict(codes={'express': '2030000'})
    )
    create_station(
        id=2000, title='Конечная', country=russia,
        time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
        __=dict(codes={'express': '2004083'})
    )


@mock.patch.object(import_tis, 'get_tis_filelist')
def test_get_last_train_file_with_date(m_get_tis_filelist):
    m_get_tis_filelist.return_value = [
        ('TRAIN.20160101', 'url_1'),
        ('TRAIN.20160103', 'url_1ast'),
        ('TRAIN.20160102', 'url_2'),
    ]

    assert get_last_train_file_with_date() == (date(2016, 1, 3), 'url_1ast')


class TestImportTIS(TestCase):
    @classmethod
    def setUpTestData(cls):
        tis_import_fixture()

    @replace_now(datetime(2016, 1, 1))
    def test_simple(self):
        example = """
            TRAIN001AJ016001203000020040833200200000000000000001AJ0016365
            2030000000001000000010
            2004083006502000000000
        """

        Express2Country.objects.create(code_re='.*', country_id=Country.RUSSIA_ID)
        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()

        threads = list(RThread.objects.filter(route__supplier__code='tis'))
        assert len(threads) == 1
        thread = threads[0]
        assert thread.number == '001А'

        assert thread.import_uid == '001A_2-1-9c99a2d80e673bc82011ff5e25ff7325-31b9a172b0b5f6cea05bc42b7515b026', (
            'Поменялась логика генерации import_uid! '
            'Правьте данный assert, только если логика генерации import_uid была изменена сознательно.'
        )

        original_import_uid = thread.import_uid
        assert original_import_uid == thread.gen_import_uid(), 'При перегенерации import_uid должен сохранятся'

        rtstations = list(thread.path)
        assert rtstations[0].station == Station.objects.get(title='Начальная')
        assert rtstations[1].station == Station.objects.get(title='Конечная')

        start_dt = datetime.combine(date(2016, 1, 1), thread.tz_start_time)

        assert rtstations[0].get_departure_dt(start_dt).replace(tzinfo=None) == datetime(2016, 1, 1, 10)
        assert rtstations[1].get_arrival_dt(start_dt).replace(tzinfo=None) == datetime(2016, 1, 1, 20)

    def import_single_thread(self, example):
        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()

        threads = list(RThread.objects.filter(route__supplier__code='tis'))
        assert len(threads) <= 1
        return threads[0] if threads else None

    @replace_now(datetime(2016, 1, 1))
    def test_mask_with_last_date(self):
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ0016365
            2030000000001000000010
            2004083006502000000000
        """)

        bounds = MaskBounds(date(2016, 1, 1), date(2016, 3, 1))
        assert thread.get_mask() & daily_mask(bounds)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_no_last_date_no_schedule_plans(self):
        assert not self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_no_last_date_old_schedule(self):
        create_train_schedule_plan(start_date=date(2015, 1, 1), end_date=date(2015, 1, 11))

        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

        bounds = MaskBounds(date(2016, 1, 1), date(2016, 3, 1))
        assert (thread.get_mask() & daily_mask(bounds)) == daily_mask(bounds)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_no_last_date_actual_schedule(self):
        create_train_schedule_plan(start_date=date(2015, 1, 1), end_date=date(2016, 1, 31))
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

        today = environment.today()

        bounds = MaskBounds(date(2016, 1, 1), date(2016, 1, 31))
        assert (thread.get_mask() & daily_mask(bounds)) == daily_mask(bounds)
        assert not daily_mask(MaskBounds(date(2016, 2, 1), date(2016, 12, 31)), today) & thread.get_mask(today)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_no_last_date_actual_schedule_too_old_start_date(self):
        create_train_schedule_plan(start_date=date(2015, 6, 1), end_date=date(2016, 1, 31))
        assert not self.import_single_thread("""
            TRAIN001AJ015001203000020040833200200000000000000001AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_no_last_date_future_schedule(self):
        create_train_schedule_plan(start_date=date(2016, 6, 1), end_date=date(2016, 12, 31))
        assert not self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000000AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

    @replace_now(datetime(2016, 1, 1))
    def test_mask_parse_calendar_first_half_of_year(self):
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000000AJ0016365
            2030000000001000000010
            2004083006502000000000
            KALEN011000000000100000000000000000000
            KALEN021000000000100000000000000000000
            KALEN031000000000100000000000000000000
            KALEN041000000000100000000000000000000
            KALEN051000000000100000000000000000000
            KALEN061000000000100000000000000000000
            KALEN071000000000100000000000000000000
            KALEN081000000000100000000000000000000
            KALEN091000000000100000000000000000000
            KALEN101000000000100000000000000000000
            KALEN111000000000100000000000000000000
            KALEN121000000000100000000000000000000
        """)

        today = environment.today()
        bounds = MaskBounds(date(2016, 1, 1), date(2016, 6, 30))
        first_half_of_year_mask = thread.get_mask(today) & daily_mask(bounds, today)
        assert_that(first_half_of_year_mask.dates(), contains(
            date(2016, 1, 1), date(2016, 1, 11),
            date(2016, 2, 1), date(2016, 2, 11),
            date(2016, 3, 1), date(2016, 3, 11),
            date(2016, 4, 1), date(2016, 4, 11),
            date(2016, 5, 1), date(2016, 5, 11),
            date(2016, 6, 1), date(2016, 6, 11),
        ))

    @replace_now(datetime(2016, 7, 1))
    def test_mask_parse_calendar_second_half_of_year(self):
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000000AJ0016365
            2030000000001000000010
            2004083006502000000000
            KALEN011000000000100000000000000000000
            KALEN021000000000100000000000000000000
            KALEN031000000000100000000000000000000
            KALEN041000000000100000000000000000000
            KALEN051000000000100000000000000000000
            KALEN061000000000100000000000000000000
            KALEN071000000000100000000000000000000
            KALEN081000000000100000000000000000000
            KALEN091000000000100000000000000000000
            KALEN101000000000100000000000000000000
            KALEN111000000000100000000000000000000
            KALEN121000000000100000000000000000000
        """)

        today = environment.today()
        bounds = MaskBounds(date(2016, 7, 1), date(2016, 12, 31))
        second_half_of_year_mask = thread.get_mask(today) & daily_mask(bounds, today)
        assert_that(second_half_of_year_mask.dates(), contains(
            date(2016, 7, 1), date(2016, 7, 11),
            date(2016, 8, 1), date(2016, 8, 11),
            date(2016, 9, 1), date(2016, 9, 11),
            date(2016, 10, 1), date(2016, 10, 11),
            date(2016, 11, 1), date(2016, 11, 11),
            date(2016, 12, 1), date(2016, 12, 11),
        ))

    @replace_now(datetime(2016, 1, 9))
    def test_mask_no_start_date_with_last_date(self):
        thread = self.import_single_thread("""
            TRAIN001AJ095999203000020040833200200000000000000001AJ0016365
            2030000000001000000010
            2004083006502000000000
        """)

        bounds = MaskBounds(date(2016, 1, 2), date(2016, 3, 1))
        assert thread.get_mask() & daily_mask(bounds)
        assert not thread.get_mask()[date(2016, 1, 1)]

    @replace_now(datetime(2016, 1, 1))
    def test_mask_with_last_date_no_every_day_flag(self):
        assert not self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000000AJ0016365
            2030000000001000000010
            2004083006502000000000
        """)

        assert self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ0016365
            2030000000001000000010
            2004083006502000000000
        """)

    @replace_now(datetime(2016, 1, 1))
    def test_technical_station(self):
        example = """
            TRAIN001AJ016001203000020040833200200000000000000001AJ0016365
            203000000000100000001000
            203001000000150001201200
            200408300650200000000000
        """

        create_station(
            title='Техническая', country=Country.RUSSIA_ID,
            time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
            __=dict(codes={'express': '2030010'})
        )

        thread = self.import_single_thread(example)

        rtstations = list(thread.path)
        assert rtstations[0].station == Station.objects.get(title='Начальная')
        assert rtstations[1].station == Station.objects.get(title='Техническая')
        assert rtstations[2].station == Station.objects.get(title='Конечная')

        start_dt = datetime.combine(date(2016, 1, 1), thread.tz_start_time)

        assert rtstations[1].get_departure_dt(start_dt).replace(tzinfo=None) == datetime(2016, 1, 1, 15)
        assert rtstations[1].get_arrival_dt(start_dt).replace(tzinfo=None) == datetime(2016, 1, 1, 13)
        assert rtstations[1].is_technical_stop
        assert not rtstations[0].is_technical_stop
        assert not rtstations[2].is_technical_stop

    @replace_now(datetime(2016, 1, 1))
    def test_technical_station_with_zero_stop_time(self):
        """
        Пропускаем технические с 0 временем стоянки
        """
        example = """
            TRAIN001AJ016001203000020040833200200000000000000001AJ0016365
            203000000000100000001000
            203001000000150000001200
            203001000000000000001200
            200408300650200000000000
        """

        create_station(
            title='Техническая', country=Country.RUSSIA_ID,
            time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
            __=dict(codes={'express': '2030010'})
        )

        thread = self.import_single_thread(example)

        rtstations = list(thread.path)
        assert len(rtstations) == 2
        assert rtstations[0].station == Station.objects.get(title='Начальная')
        assert rtstations[1].station == Station.objects.get(title='Конечная')

    @replace_now(datetime(2016, 1, 1))
    def test_get_company_by_code(self):
        example = """
            TRAIN001AJ016001203000020040833200770000000000000001AJ8816365
            2030000000001000000010
            2004083006502000000000
        """

        company = create_company(tis_code='77_88')

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()

        thread = RThread.objects.get(route__supplier__code='tis')
        assert thread.company == company

    @replace_now(datetime(2016, 1, 1))
    def test_get_company_by_country_code_rzd(self):
        example = """
            TRAIN001AJ016001203000020040833200770000000000000001AJ8816365
            2030000000001000000010
            2004083006502000000000
        """

        russia = Country.objects.get(id=Country.RUSSIA_ID)
        company = Company.objects.get(id=RZD_ID)
        Express2Country.objects.create(code_re='77', country=russia, time_zone='Europe/Moscow')

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()

        thread = RThread.objects.get(route__supplier__code='tis')
        assert thread.company == company

    @replace_now(datetime(2016, 1, 1))
    def test_get_company_priority(self):
        example = """
            TRAIN001AJ016001203000020040833200770000000000000001AJ8816365
            2030000000001000000010
            2004083006502000000000
        """

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()
        assert not RThread.objects.get(route__supplier__code='tis').company_id

        russia = Country.objects.get(id=Country.RUSSIA_ID)
        default_russia_company = Company.objects.get(id=RZD_ID)
        Express2Country.objects.create(code_re='77', country=russia, time_zone='Europe/Moscow')

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()
        assert RThread.objects.get(route__supplier__code='tis').company == default_russia_company

        company_by_code = create_company(tis_code='77_88')

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()
        assert RThread.objects.get(route__supplier__code='tis').company == company_by_code

        company_by_number = create_company()
        Route2Company.objects.create(number='001А', company=company_by_number)

        importer = TisImporter(parse_train_file(StringIO(example)))
        importer.do_import()
        assert RThread.objects.get(route__supplier__code='tis').company == company_by_number

    @replace_now(datetime(2016, 1, 1))
    def test_kaz_suburban(self):
        kazakhstan = Country.objects.create(id=Country.KAZAKHSTAN_ID, title='Казахстан')
        Company.objects.create(id=111, title=u'КТЖ', country=kazakhstan, tis_code='20_00')

        from_station = create_station(
            id=3000, title='НачальнаяКаз', country=kazakhstan,
            time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
            __=dict(codes={'express': '2030001'})
        )
        to_station = create_station(
            id=4000, title='КонечнаяКаз', country=kazakhstan,
            time_zone='Europe/Moscow', majority=StationMajority.IN_TABLO_ID,
            __=dict(codes={'express': '2004084'})
        )

        thread = self.import_single_thread("""
            TRAIN852AJ016001203000120040843200200000000000000001AJ0016365
            2030001000001000000010
            2004084006502000000000
        """)
        rtstations = list(thread.path)
        assert rtstations[0].station == from_station
        assert rtstations[1].station == to_station
        assert thread.t_type_id == TransportType.SUBURBAN_ID

        thread = self.import_single_thread("""
            TRAIN852AJ016001203000120040833200200000000000000001AJ0016365
            2030001000001000000010
            2004083006502000000000
        """)
        rtstations = list(thread.path)
        assert rtstations[0].station == from_station
        assert rtstations[1].station_id == Station.objects.get(id=2000).id
        assert thread.t_type_id == TransportType.TRAIN_ID

    @replace_now(datetime(2016, 1, 1))
    def test_thread_t_subtype(self):
        t_subtype = create_transport_subtype(t_type=TransportType.objects.get(id=TransportType.TRAIN_ID))
        DeLuxeTrain.objects.create(numbers='001А/001В/002С', t_subtype=t_subtype)
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ00163650000000119000000000АБ
            203000000000100000001000001А001А
            200408300650200000000000001А001А
        """)
        assert thread.t_subtype == t_subtype

        thread = self.import_single_thread("""
            TRAIN002BJ016001203000020040833200200000000000000001AJ00163650000000119000000000АБ
            203000000000100000001000001А001А
            200408300650200000000000001А001А
        """)
        assert thread.t_subtype is None

    @replace_now(datetime(2016, 1, 1))
    def test_canonical_uid(self):
        create_station(id=3000, __=dict(codes={'express': '4444444'}))
        create_station(id=4000, __=dict(codes={'express': '7777777'}))
        create_company(tis_code='20_99', id=999)

        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833200200000000000000001AJ99163650000000119000000000АБ
            203000000000100000001000001А001А
            200408300650200000000000001А001А
        """)
        assert thread.canonical_uid == 'R_001A_999'

        thread = self.import_single_thread("""
            TRAIN001AJ216001444444477777773200200000000000000001AJ99163650000000119000000000АБ
            444444400000100000001000001А001А
            777777700650200000000000001А001А
        """)
        assert thread.canonical_uid == 'R_001A_3000_4000_999'

    @replace_now(datetime(2016, 1, 1))
    def test_bus_thread(self):
        create_train_schedule_plan(start_date=date(2015, 1, 1), end_date=date(2016, 1, 31))
        thread = self.import_single_thread("""
            TRAIN001AJ016001203000020040833400200000000000000001AJ0095999
            2030000000001000000010
            2004083006502000000000
        """)

        assert thread.t_type_id == TransportType.BUS_ID
