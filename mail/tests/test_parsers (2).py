import pytest

from hamcrest import assert_that, equal_to
from unistat.parsers import parse_ymod_httpclient_log_event


@pytest.mark.parametrize(('line', 'expected'), (
    (
        ('[2018-Dec-20 18:41:35.912706] thread=158 http_client ZfS4RA0Fa4Y1:c9630b6d3fd3f096c1cd1c0a1c4420cd'
         + ' conn=0x106cd8080 req=203 event=start'
         + ' uri="resize.yandex.net/320.mail:779387872.E1498475:2785537293111982580042109023762?url=http"'
         + ' headers=[X-Request-Id: c9630b6d3fd3f096c1cd1c0a1c4420'
         + 'cd<cr><lf>X-Request-Attempt: 0<cr><lf>X-Request-Timeout: 10000<cr><lf>]'),
        {
            'req': '203',
            'uri': 'resize.yandex.net/320.mail:779387872.E1498475:2785537293111982580042109023762?url=http',
            'session_id': 'ZfS4RA0Fa4Y1',
            'headers': ('X-Request-Id: c9630b6d3fd3f096c1cd1c0a1c4420cd<cr><lf>'
                        + 'X-Request-Attempt: 0<cr><lf>X-Request-Timeout: 10000<cr><lf>'),
            'time': '2018-Dec-20 18:41:35.912706',
            'event': 'start',
            'conn': '0x106cd8080',
        }
    ),
    (
        ('[2018-Dec-20 18:41:36.221171] thread=157 http_client ZfS4RA0Fa4Y1:c9630b6d3fd3f096c1cd1c0a1c4420cd'
         + ' conn=0x106cd8080 req=203 event=fin tm={0.000, 0.000, 0.000, 0.308} status=200 attempt=0'
         + ' bytes_out=652 bytes_in=13005'),
        {
            'total_time': '0.308',
            'status': '200',
            'attempt': '0',
            'tls_time': '0.000',
            'connect_time': '0.000',
            'req': '203',
            'session_id': 'ZfS4RA0Fa4Y1',
            'bytes_out': '652',
            'time': '2018-Dec-20 18:41:36.221171',
            'bytes_in': '13005',
            'resolve_time': '0.000',
            'event': 'fin',
            'conn': '0x106cd8080',
        }
    ),
))
def test_parse_ymod_httpclient_log_event(line, expected):
    assert_that(parse_ymod_httpclient_log_event(line), equal_to(expected))
