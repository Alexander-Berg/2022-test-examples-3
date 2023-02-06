# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_company

from travel.rasp.admin.scripts.www_companies import hide_and_restore_companies


@pytest.mark.dbuser
@pytest.mark.parametrize('t_type, company_hidden, thread_hidden, expected', [
        (TransportType.BUS_ID, False, False, False),
        (TransportType.BUS_ID, True, True, True),
        (TransportType.BUS_ID, True, False, False),
        (TransportType.BUS_ID, False, True, True),

        (TransportType.PLANE_ID, False, False, False),
        (TransportType.PLANE_ID, True, True, True),
        (TransportType.PLANE_ID, True, False, True),
        (TransportType.PLANE_ID, False, True, False),

        (TransportType.HELICOPTER_ID, False, False, False),
        (TransportType.HELICOPTER_ID, True, True, True),
        (TransportType.HELICOPTER_ID, True, False, True),
        (TransportType.HELICOPTER_ID, False, True, False)
])
def test_hide_and_restore_companies(t_type, company_hidden, thread_hidden, expected):
    station_from = create_station(title='from')
    station_to = create_station(title='to')

    company = create_company(title='test_company', hidden=company_hidden, t_type=t_type)

    create_thread(
        title='test_thread',
        company=company,
        t_type=t_type,
        year_days=[datetime(2020, 10, 10)],
        hidden=thread_hidden,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    hide_and_restore_companies()
    company.refresh_from_db()

    assert company.hidden == expected
