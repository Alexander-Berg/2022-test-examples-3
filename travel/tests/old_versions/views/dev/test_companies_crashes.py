# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.apps.suburban_events.factories import CompanyCrashFactory
from common.tester.factories import create_company

from travel.rasp.export.tests.v3.helpers import api_get_json

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def test_companies_crashes():
    company_1 = create_company(id=42, title_ru='перевозчик 42')
    company_2 = create_company(id=43, title_ru='перевозчик 43')
    dt_1, dt_2 = datetime(2018, 1, 1, 10), datetime(2018, 1, 1, 13)

    CompanyCrashFactory(
        company=company_1.id,
        first_dt=dt_1,
        last_dt=dt_2,
        first_rate=0,
        avr_rate=5,
        last_rate=5,
    )
    CompanyCrashFactory(
        company=company_2.id,
        first_dt=dt_2,
        avr_rate=1,
        first_rate=2.5,
    )

    response = api_get_json('/dev/companies_crashes')
    assert response == {
        str(company_1.id): {
            'title': 'перевозчик 42',
            'first_dt': dt_1.isoformat(),
            'last_dt': dt_2.isoformat(),
            'first_rate': 0,
            'last_rate': 5,
            'company_average_rate': 5
        },
        str(company_2.id): {
            'title': 'перевозчик 43',
            'first_dt': dt_2.isoformat(),
            'last_dt': None,
            'first_rate': 2.5,
            'last_rate': None,
            'company_average_rate': 1
        }
    }
