# coding: utf-8


import os
import re
import sys
import pytest


STATS_PATH = 'usr/lib/python2.7/dist-packages/mcroutertographite'
sys.path.append(os.path.join(os.path.dirname(__file__), '..', STATS_PATH))


from memcached_stats import get_port
from memcached_stats import get_memcached_configs
from memcached_stats import get_memcached_run_params
from memcached_stats import convert
from memcached_stats import PortNotFoundException


def test_get_port():
    run_params = [
        '-m 64',
        '-p 11211',
        '-u memcache',
        '-l 127.0.0.1'
    ]
    run_params_long_name = [
        '-m 64',
        '--port=11211',
        '-u memcache',
        '-l 127.0.0.1'
    ]

    run_params_additional_symbols = [
        '--memory=64',
        '--port=11211   # some comments',
        '-u memcache',
        '-l 127.0.0.1'
    ]

    run_params_additional_space_in_begin = [
        '--memory=64',
        ' --port=11211   # some comments',
        '-u memcache',
        '-l 127.0.0.1'
    ]

    assert get_port(run_params) == 11211
    assert get_port(run_params_long_name) == 11211
    assert get_port(run_params_additional_symbols) == 11211
    assert get_port(run_params_additional_space_in_begin) == 11211


def test_get_port_exception():
    run_params = [
        '-m 64',
        '-u memcache',
        '-l 127.0.0.1'
    ]
    pytest.raises(PortNotFoundException, get_port, run_params)


def test_get_memcached_configs():
    configs = get_memcached_configs('tests/files/memcached_*.conf')
    for config in configs:
        for line in config:
            assert isinstance(line, str)


def test_get_memcached_run_params():
    expected_all_params = [
        [
            '-d',
            'logfile /var/log/yandex/memcached/feed_parser.log',
            '-m 131072',
            '-p 21220',
            '-u memcache',
            '-l ::',
            '-o modern',
            '-R 2000'
        ],
        [
            '-d',
            'logfile /var/log/yandex/memcached/feed_parser.log',
            '-m 131072',
            '--port=21220',
            '-u memcache',
            '-l ::',
            '--extended=modern,hash_algorithm=jenkins',
            '-R 2000'
        ],
        [
            '-d',
            'logfile /var/log/yandex/memcached/feed_parser.log',
            '-m 131072',
            '--port=21220',
            '-u memcache',
            '-l ::',
            '-o modern',
            '-R 2000'
        ]
    ]
    all_params = get_memcached_run_params('tests/files/memcached_[0-9].conf')
    print(all_params)
    assert expected_all_params == all_params


def test_convert():
    stat_string = 'STAT hash_bytes 33554432'
    expected_regexp = r'''^one_min\.HOST\.
                          [a-z][a-z0-9-_]+\. # hostname
                          memcached\.
                          21220\.            # port
                          hash_bytes\       # metric name and space
                          33554432\        # metric value and space
                          [0-9]{10}$         # unixtime '''
    regexp = re.compile(expected_regexp, re.VERBOSE)
    assert regexp.match(convert(stat_string, '21220'))
