# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder

from common.apps.suburban_events.factories import HourEventsRateFactory
from common.apps.suburban_events.models import LVGD01_TR2PROC_feed, LVGD01_TR2PROC_query, HourEventsRate, CompanyCrash
from common.apps.suburban_events.scripts.update_companies_crashes import (
    update_companies_events_rate, calc_companies_crashes, check_time_without_events
)
from common.models.geo import CodeSystem
from common.models.schedule import CompanyMarker
from common.tester.factories import create_thread, create_station, create_company, create_station_code
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date import environment

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def create_rzd_row_event(i=0, **kwargs):
    row = {
        'ID_TRAIN': i,
        'IDTR': i,
        'IDRASP': i,
        'STORASP': i,
        'STOEX': i,
        'NAMESTO': '{}_{}'.format(i, i),
        'STNRASP': i,
        'STNEX': i,
        'NAMESTN': '{}_{}'.format(i, i),
        'NOMPEX': '{}_{}'.format(i, i),
        'NAMEP': '{}_{}'.format(i, i),
        'SOURCE': i,
        'KODOP': i,
        'DOR': i,
        'OTD': i,
        'NOMRP': i,
        'STOPER': i,
        'STOPEREX': i,
        'STNAME': '{}_{}'.format(i, i),
        'TIMEOPER_N': environment.now(),
        'TIMEOPER_F': environment.now(),
        'KM': i,
        'PRSTOP': i,
        'PRIORITY': i,
        'PRIORITY_RATING': i,
    }
    row.update(kwargs)

    return LVGD01_TR2PROC_feed.objects.create(**row)


@replace_now('2018-01-17 19:00:00')
def test_update_companies_events_rate():
    station_1, station_2, station_3 = create_station(id=121), create_station(id=122), create_station(id=123)
    esr_code_system = CodeSystem.objects.get(code='rzd_esr')
    st_1_code = create_station_code(station=station_1, system=esr_code_system, code='121')
    st_2_code = create_station_code(station=station_2, system=esr_code_system, code='122')
    st_3_code = create_station_code(station=station_3, system=esr_code_system, code='123')

    company_1, company_2 = create_company(id=42), create_company(id=43)
    CompanyMarker.objects.create(station=station_1, company=company_1)
    CompanyMarker.objects.create(station=station_2, company=company_1)
    CompanyMarker.objects.create(station=station_2, company=company_2)
    CompanyMarker.objects.create(station=station_3, company=company_2)

    q_1 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': datetime(2018, 1, 10, 15, 20),
        'query_from': datetime(2018, 1, 10, 15, 10),
        'query_to': datetime(2018, 1, 10, 15, 20),
    })

    q_2 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': datetime(2018, 1, 10, 15, 30),
        'query_from': datetime(2018, 1, 10, 15, 20),
        'query_to': datetime(2018, 1, 10, 15, 30),
    })

    q_3 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': datetime(2018, 1, 3, 15, 20),
        'query_from': datetime(2018, 1, 3, 15, 10),
        'query_to': datetime(2018, 1, 3, 15, 20),
    })

    q_4 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': datetime(2018, 1, 3, 16, 10),
        'query_from': datetime(2018, 1, 3, 16),
        'query_to': datetime(2018, 1, 3, 16, 10),
    })

    create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 15),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 14),
        STOPER=st_1_code.code,
        query=q_1
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 13),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 13),
        STOPER=st_3_code.code,
        query=q_1
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 27),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 26),
        STOPER=st_2_code.code,
        query=q_2
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 23),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 23),
        STOPER=st_3_code.code,
        query=q_2
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2017, 12, 20, 15, 17),
        TIMEOPER_F=datetime(2017, 12, 20, 15, 17),
        STOPER=st_2_code.code,
        query=q_3
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2017, 12, 20, 15, 16),
        TIMEOPER_F=datetime(2017, 12, 20, 15, 16),
        STOPER=st_1_code.code,
        query=q_3
    )

    create_rzd_row_event(
        TIMEOPER_N=datetime(2017, 12, 20, 16, 8),
        TIMEOPER_F=datetime(2017, 12, 20, 16, 5),
        STOPER=st_2_code.code,
        query=q_4
    )

    with replace_dynamic_setting('SUBURBAN_WEEKS_DEEP', 4):
        update_companies_events_rate()

    rates = list(HourEventsRate.objects.all())
    assert len(rates) == 4
    assert_that(rates, contains_inanyorder(
        has_properties({
            'company': company_1.id,
            'hour': 15,
            'rate': 1.5
        }),
        has_properties({
            'company': company_2.id,
            'hour': 15,
            'rate': 1.25
        }),
        has_properties({
            'company': company_2.id,
            'hour': 16,
            'rate': 1.
        }),
        has_properties({
            'company': company_1.id,
            'hour': 16,
            'rate': 1.
        }),
    ))


def test_calc_companies_crashes():
    q1_dt = datetime(2018, 1, 10, 15, 20)
    station_1, station_2, station_3 = create_station(id=121), create_station(id=122), create_station(id=123)
    esr_code_system = CodeSystem.objects.get(code='rzd_esr')
    st_1_code = create_station_code(station=station_1, system=esr_code_system, code='121')
    st_2_code = create_station_code(station=station_2, system=esr_code_system, code='122')
    st_3_code = create_station_code(station=station_3, system=esr_code_system, code='123')

    company_1, company_2, company_3 = create_company(id=42), create_company(id=43), create_company(id=44)
    CompanyMarker.objects.create(station=station_1, company=company_1)
    CompanyMarker.objects.create(station=station_2, company=company_1)
    CompanyMarker.objects.create(station=station_2, company=company_2)
    CompanyMarker.objects.create(station=station_3, company=company_2)

    q_1 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': q1_dt,
        'query_from': datetime(2018, 1, 10, 15, 10),
        'query_to': q1_dt,
    })

    r_1 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 15),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 14),
        STOPER=st_1_code.code,
        query=q_1
    )

    r_2 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 13),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 13),
        STOPER=st_2_code.code,
        query=q_1
    )

    r_3 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 16),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 12),
        STOPER=st_3_code.code,
        query=q_1
    )

    HourEventsRateFactory(
        hour=15,
        company=company_1.id,
        rate=4.
    )

    HourEventsRateFactory(
        hour=15,
        company=company_2.id,
        rate=4.1
    )

    HourEventsRateFactory(
        hour=15,
        company=company_3.id,
        rate=1.
    )

    with replace_dynamic_setting('SUBURBAN_COMPANY_CRASH_RATE', 0.5):
        calc_companies_crashes([r_1, r_2, r_3])

    rates = list(CompanyCrash.objects.all())
    assert len(rates) == 2
    assert_that(rates, contains_inanyorder(
        has_properties({
            'company': company_2.id,
            'first_dt': q1_dt,
            'first_rate': 2,
            'avr_rate': 4.1
        }),
        has_properties({
            'company': company_3.id,
            'first_dt': q1_dt,
            'first_rate': 0,
            'avr_rate': 1
        }),
    ))

    q2_dt = datetime(2018, 1, 10, 15, 30)
    q_2 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': q2_dt,
        'query_from': datetime(2018, 1, 10, 15, 20),
        'query_to': q2_dt,
    })

    r_1 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 28),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 28),
        STOPER=st_2_code.code,
        query=q_2
    )

    r_2 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 27),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 27),
        STOPER=st_2_code.code,
        query=q_2
    )

    r_3 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 26),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 26),
        STOPER=st_3_code.code,
        query=q_2
    )

    with replace_dynamic_setting('SUBURBAN_COMPANY_CRASH_RATE', 0.5):
        calc_companies_crashes([r_1, r_2, r_3])

    rates = list(CompanyCrash.objects.all())
    assert len(rates) == 2
    assert_that(rates, contains_inanyorder(
        has_properties({
            'company': company_2.id,
            'first_dt': q1_dt,
            'last_dt': q2_dt,
            'first_rate': 2,
            'last_rate': 3,
            'avr_rate': 4.1
        }),
        has_properties({
            'company': company_3.id,
            'first_dt': q1_dt,
            'last_dt': None,
            'first_rate': 0,
            'last_rate': None,
            'avr_rate': 1
        }),
    ))

    q3_dt = datetime(2018, 1, 10, 15, 40)
    q_3 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': q3_dt,
        'query_from': datetime(2018, 1, 10, 15, 30),
        'query_to': q3_dt,
    })
    r_1 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 35),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 35),
        STOPER=st_2_code.code,
        query=q_3
    )

    CompanyMarker.objects.create(station=station_2, company=company_3)
    with replace_dynamic_setting('SUBURBAN_COMPANY_CRASH_RATE', 0.5):
        calc_companies_crashes([r_1])

    rates = list(CompanyCrash.objects.all())
    assert len(rates) == 3
    assert_that(rates, contains_inanyorder(
        has_properties({
            'company': company_2.id,
            'first_dt': q3_dt,
            'last_dt': None,
            'first_rate': 1,
            'avr_rate': 4.1
        }),
        has_properties({
            'company': company_3.id,
            'first_dt': q1_dt,
            'last_dt': q3_dt,
            'first_rate': 0,
            'last_rate': 1,
            'avr_rate': 1
        }),
        has_properties({
            'company': company_1.id,
            'first_dt': q3_dt,
            'last_dt': None,
            'first_rate': 1,
            'last_rate': None,
            'avr_rate': 4
        }),
    ))


@replace_now('2018-01-10 16:00:00')
def test_check_time_without_events():
    dt = datetime(2018, 1, 10, 15, 20)
    station_1, station_2 = create_station(id=121), create_station(id=122)
    esr_code_system = CodeSystem.objects.get(code='rzd_esr')
    st_1_code = create_station_code(station=station_1, system=esr_code_system, code='121')
    company_1, company_2 = create_company(id=42), create_company(id=43)
    CompanyMarker.objects.create(station=station_1, company=company_1)
    CompanyMarker.objects.create(station=station_2, company=company_2)

    q_1 = LVGD01_TR2PROC_query.objects.create(**{
        'queried_at': dt,
        'query_from': datetime(2018, 1, 10, 15, 10),
        'query_to': dt,
    })

    r_1 = create_rzd_row_event(
        TIMEOPER_N=datetime(2018, 1, 10, 15, 15),
        TIMEOPER_F=datetime(2018, 1, 10, 15, 14),
        STOPER=st_1_code.code,
        query=q_1
    )

    with replace_dynamic_setting('SUBURBAN_ALL_COMPANIES_CRASH_TIME', 30):
        check_time_without_events([r_1], dt)
        assert len(CompanyCrash.objects.all()) == 0

        check_time_without_events([], dt)
        crashes = list(CompanyCrash.objects.all())
        assert len(crashes) == 2
        assert_that(crashes, contains_inanyorder(
            has_properties({
                'company': company_1.id,
                'first_dt': dt,
                'first_rate': 0,
                'last_dt': None,
                'last_rate': None,
            }),
            has_properties({
                'company': company_2.id,
                'first_dt': dt,
                'first_rate': 0,
                'last_dt': None,
                'last_rate': None,
            }),
        ))

    CompanyCrash.drop_collection()
    with replace_dynamic_setting('SUBURBAN_ALL_COMPANIES_CRASH_TIME', 50):
        check_time_without_events([], dt)
        assert len(CompanyCrash.objects.all()) == 0
