# -*- coding: utf-8 -*-
import itertools

import pytest

from travel.avia.library.python.common.utils.marketstat import DsvLog, DsvSimpleLog, Log

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


@pytest.mark.parametrize('log_cls, userip, key, val', itertools.product(
    [Log, DsvLog],
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
