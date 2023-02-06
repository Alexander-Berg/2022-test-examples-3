# coding: utf8
import pytest
import yaml
import yatest

from haproxygraphs.haproxy import HAProxy


@pytest.fixture
def haproxy(metrics):
    return HAProxy(prefix='prefix', metrics=metrics)


@pytest.fixture()
def metrics():
    with open(yatest.common.source_path('market/sre/tools/haproxygraphs/metrics.yml')) as fd:
        return yaml.load(fd)


def test_stats_parser(haproxy):     # noqa
    """ haproxy stats должен парситься в правильные метрики графита """
    lines = [
        '# header1,header2,header3,\n',
        'frontend,1,,\n',
        'backend,2,,\n',
    ]
    assert list(haproxy.parse_stats(lines)) == [
        {
            'header1': 'frontend',
            'header2': '1',
            'header3': '',
            '': '',
        },
        {
            'header1': 'backend',
            'header2': '2',
            'header3': '',
            '': '',
        },
    ]


def test_info_parser(haproxy):
    """ haproxy info должен парситься в правильные метрики графита """
    lines = [
        'int: 10\n',
        'float: 1.1\n',
        'string: string value\n',
        'duplicate_key: one\n',
        'duplicate_key: two\n',
    ]
    assert list(haproxy.parse_info(lines)) == [{
        'int': '10',
        'float': '1.1',
        'string': 'string value',
        'duplicate_key': 'two',
    }]


def test_get_metrics(haproxy):
    assert list(haproxy.get_metrics(
        start_time=0,
        stats=[
            {
                'pxname': 'test.tst.vs.market.yandex.net',
                'svname': 'FRONTEND',
                'req_rate': '100',
                'rtime': '200',
                '': '',
            },
            {
                'pxname': 'test.tst.vs.market.yandex.net',
                'svname': 'BACKEND',
                'req_rate': '100',
                'rtime': '200',
                '': '',
            },
        ],
        info=[{
            'int': '10',
            'float': '1.1',
            'string': 'string value',
            'duplicate_key': 'two',
        }],
    )) == [
        'prefix.haproxy.int 10 0',
        'prefix.haproxy.float 1.1 0',
        'prefix.haproxy.string string value 0',
        'prefix.haproxy.duplicate_key two 0',
        'prefix.frontend.test_tst_vs_market_yandex_net.req_rate 100 0',
        'prefix.backend.test_tst_vs_market_yandex_net.rtime 200 0',
    ]
