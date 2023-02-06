# coding: utf-8


import os
import re
import sys
import pytest


STATS_PATH = 'usr/lib/python2.7/dist-packages/mcroutertographite'
sys.path.append(os.path.join(os.path.dirname(__file__), '..', STATS_PATH))


from mcrouter_stats import get_port
from mcrouter_stats import convert
from mcrouter_stats import PortNotFoundException
from mcrouter_stats import get_mcrouter_run_params


def test_get_port():
    run_params = [
        '/usr/bin/mcrouter',
        '--port=11220',
        '--config-file=/etc/yandex/mcrouter/conf-generated/11220-feed_parser.market_cache.conf',
        '--log-path=/var/log/yandex/mcrouter/11220-feed_parser.market_cache.log',
        '--disable-miss-on-get-errors',
        '--use-asynclog-version2',
        '--enable-flush-cmd'
    ]
    assert get_port(run_params) == 11220


def test_get_port_exception():
    run_params = '--config-file={config}' + \
                 ' --log-path={log_path} --disable-miss-on-get-errors' + \
                 ' --use-asynclog-version2 --enable-flush-cmd'

    pytest.raises(PortNotFoundException, get_port, run_params)


def test_convert():
    stat_string = 'STAT cmd_get_out 6506.9375'
    expected_regexp = r'''^one_min\.HOST\.
                          [a-z][a-z0-9-_]+\. # hostname
                          mcrouter\.
                          11220\.            # port
                          cmd_get_out\       # metric name and space
                          6506\.9375\        # metric value and space
                          [0-9]{10}$         # unixtime '''
    regexp = re.compile(expected_regexp, re.VERBOSE)
    print(convert(stat_string, '11220'))
    assert regexp.match(convert(stat_string, '11220'))


def test_get_mcrouter_run_params():
    configs = 'tests/files/mcrouter*.conf'
    params = get_mcrouter_run_params(configs)
    expected_params = [
        [
            u'--port=11223',
            u'--config-file={config}',
            u'--log-path={log_path}',
            u'--disable-miss-on-get-errors',
            u'--use-asynclog-version2',
            u'--enable-flush-cmd'
        ],
        [
            u'--port=11220',
            u'--config-file={config}',
            u'--log-path={log_path}',
            u'--disable-miss-on-get-errors',
            u'--use-asynclog-version2',
            u'--enable-flush-cmd'
        ]
    ]
    assert expected_params == params
