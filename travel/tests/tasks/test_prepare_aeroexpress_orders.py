# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date
from io import BytesIO

import mock
import openpyxl
import pytest

from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting

from travel.rasp.suburban_selling.selling.tasks.prepare_aeroexpress_orders import get_yt_order_data, prepare_excel


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_get_yt_order_data():
    with replace_setting('YT_CPA_SUBURBAN_ORDERS_DIR', 'cpa_dir'):
        with mock.patch(
            'travel.rasp.suburban_selling.selling.tasks.prepare_aeroexpress_orders.YqlClient.query'
        ) as m_query:
            get_yt_order_data(date(2022, 3, 11), date(2022, 3, 21), datetime(2022, 3, 21))

            m_query.assert_called_with('''
        $get_dt_from_ts = ($ts) -> (
            CAST(DateTime::FromMilliseconds($ts * 1000) AS String)
        );
        SELECT provider_order_id, order_amount, departure_date, $get_dt_from_ts(created_at) as sell_dt
        FROM hahn.`cpa_dir`
        WHERE $get_dt_from_ts(created_at) >= '2022-03-10T21:00:00'
              and $get_dt_from_ts(created_at) < '2022-03-20T21:00:00'
              and provider = 'AEROEXPRESS'
              and provider_order_id is not null
              and status = 'confirmed'
    ''')


def test_get_excel():
    meta = {
        'date_from': datetime(2018, 8, 11), 'date_to': datetime(2018, 8, 21),
        'total_sum': 1000, 'fee': 80, 'count': 2, 'order_date': datetime(2018, 8, 11)
    }
    data = [
        {
            'ticket_id': 111,
            'sell_date': '10.08.2018',
            'sell_sum': 500,
            'pay_date': '10.08.2018',
            'number': '',
            'received': 500,
            'trip_date': '11.08.2018',
            'name': 'физическое лицо'.encode('utf8')
        },
        {
            'ticket_id': 112,
            'sell_date': '11.08.2018',
            'sell_sum': 500,
            'pay_date': '11.08.2018',
            'number': '',
            'received': 500,
            'trip_date': '12.08.2018',
            'name': 'физическое лицо'.encode('utf8')
        }
    ]

    with replace_now('2018-08-22'):
        excel_data = prepare_excel(data, meta)
        wb = openpyxl.load_workbook(filename=BytesIO(excel_data))
        rows = list(wb.active.rows)

        assert wb.active.title == 'Продажи Агента'
        assert rows[8][1].value == '111'
        assert rows[8][2].value == '10.08.2018'
        assert rows[8][3].value == 500
        assert rows[8][4].value == '10.08.2018'
        assert rows[8][6].value == 500
        assert rows[8][7].value == '11.08.2018'
        assert rows[8][8].value == 'физическое лицо'

        assert rows[9][1].value == '112'
        assert rows[9][2].value == '11.08.2018'
        assert rows[9][3].value == 500
        assert rows[9][4].value == '11.08.2018'
        assert rows[9][6].value == 500
        assert rows[9][7].value == '12.08.2018'
        assert rows[9][8].value == 'физическое лицо'

        assert rows[10][3].value == '=SUM(D9:D10)'  # total
        assert rows[24][1].value == 80  # fee
        assert rows[18][5].value == 2  # count
