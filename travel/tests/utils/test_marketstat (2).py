# coding: utf8
import itertools
import json

import pytest

from common.utils.marketstat import DsvLog, DsvSimpleLog, Log, JsonLog

IP_EXAMPLES = [
    ('2a02:6b8:0:1495::10b', '2a02:6b8:0:1495::10b'),
    ('::ffff:87.250.248.93', '87.250.248.93'),
]


def test_dsv_logs(tmpdir):
    tmp_file_path = str(tmpdir.join('marketstat.log'))
    dsv_log = DsvLog(tmp_file_path)
    dsv_simple_log = DsvSimpleLog(tmp_file_path)

    dsv_log.write('message')
    dsv_simple_log.write('message')

    for user_ip, processed_ip in IP_EXAMPLES:
        assert dsv_log.process_userip(user_ip) == processed_ip
        assert dsv_simple_log.process_userip(user_ip) == processed_ip


def test_json_logs(tmpdir):
    tmp_file_path = str(tmpdir.join('marketstat.log'))
    json_log = JsonLog(tmp_file_path)
    json_log.log({'msg': 42, 'deep': {'deep': [43]}})
    json_log.log({'msg': 44, u'что-то': u'страшное'})

    with open(tmp_file_path) as f:
        l1, l2 = f
        assert json.loads(l1) == {'msg': 42, 'deep': {'deep': [43]}}
        assert json.loads(l2) == {'msg': 44, u'что-то': u'страшное'}


@pytest.mark.parametrize('log_cls, userip, key, val', itertools.product(
    [Log, DsvLog, JsonLog],
    itertools.chain.from_iterable(IP_EXAMPLES),
    [u'юникодный_key', 'bytestring_key'],
    [u'юникодный_val', 'bytestring_val'],
))
def test_marketstat_log_write_event(tmpdir, log_cls, userip, key, val):
    tmp_file_path = str(tmpdir.join('marketstat.log'))
    log = log_cls(tmp_file_path)

    log.write_event(
        yandexuid='1234',
        passportuid=None,
        userip=userip,
        extra={key: val},
    )
