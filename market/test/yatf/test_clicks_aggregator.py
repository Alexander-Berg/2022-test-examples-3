import yatest
import subprocess
from operator import itemgetter

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig

YT_SERVER = None


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def create_table(yt, table_name):
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
    )


def run_bin(cmdlist):
    try:
        subprocess.check_call(cmdlist)
    except Exception as e:
        print(e)
        raise


def check_table(expected, table):
    given = [x for x in table]
    assert len(expected) == len(given)
    for i in range(0, len(expected)):
        for key, value in expected[i].items():
            if isinstance(value, list):
                assert set(value) == set(given[i][key])
            else:
                assert value == given[i][key]


def test_clicks_aggregator():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    clicks_path = '{}/clicks'.format(get_yt_prefix())
    yesterday_clicks_table = '{}/2001-07-02'.format(clicks_path)
    today_clicks_table = '{}/2001-07-03'.format(clicks_path)
    result_path = '{}/result'.format(get_yt_prefix())

    create_table(yt, yesterday_clicks_table)
    create_table(yt, today_clicks_table)

    yt.write_table(yesterday_clicks_table, [
        {'shop_id': 1, 'geo_id': 213, 'iso_eventtime': '2001-07-02 21:10:00'} for _ in range(150)
    ] + [
        {'shop_id': 3, 'geo_id': 213, 'iso_eventtime': '2001-07-02 21:30:01'} for _ in range(120)
    ])
    yt.write_table(today_clicks_table, [
        {'shop_id': 2, 'geo_id': 213, 'iso_eventtime': '2001-07-03 14:19:23'} for _ in range(100)
    ] + [
        {'shop_id': 2, 'geo_id': 213, 'iso_eventtime': '2001-07-03 14:35:02'} for _ in range(200)
    ] + [
        {'shop_id': 2, 'geo_id': 2, 'iso_eventtime': '2001-07-03 14:36:02'} for _ in range(220)
    ] + [
        {'shop_id': 4, 'geo_id': 213, 'iso_eventtime': '2001-07-03 16:00:45'} for _ in range(99)
    ] + [
        {'shop_id': 5, 'geo_id': 213, 'iso_eventtime': '2001-07-03 21:00:45'} for _ in range(16000)
    ] + [
        {'shop_id': 6, 'geo_id': 213, 'iso_eventtime': '2001-07-03 22:10:45'} for _ in range(230)
    ])

    cmdlist = [
        yatest.common.binary_path('market/idx/generation/clicks-aggregator/bin/clicks-aggregator'),
        '--yt-server', YT_SERVER.get_server(),
        '--yt-clicks-path', clicks_path,
        '--yt-result-path', result_path,
        '--start-date', '2001-07-03'
    ]
    run_bin(cmdlist)

    assert len(yt.list(result_path)) == 2

    for table in ['2001-07-03', 'recent']:
        check_table([
            {'shop_id': 1, 'geo_id': 213, 'window': 0, 'count': 150},
            {'shop_id': 2, 'geo_id': 2, 'window': 29, 'count': 220},
            {'shop_id': 2, 'geo_id': 2, 'window': 30, 'count': 220},
            {'shop_id': 2, 'geo_id': 2, 'window': 31, 'count': 220},
            {'shop_id': 2, 'geo_id': 2, 'window': 32, 'count': 220},
            {'shop_id': 2, 'geo_id': 2, 'window': 33, 'count': 220},
            {'shop_id': 2, 'geo_id': 2, 'window': 34, 'count': 220},
            {'shop_id': 2, 'geo_id': 213, 'window': 28, 'count': 100},
            {'shop_id': 2, 'geo_id': 213, 'window': 29, 'count': 300},
            {'shop_id': 2, 'geo_id': 213, 'window': 30, 'count': 300},
            {'shop_id': 2, 'geo_id': 213, 'window': 31, 'count': 300},
            {'shop_id': 2, 'geo_id': 213, 'window': 32, 'count': 300},
            {'shop_id': 2, 'geo_id': 213, 'window': 33, 'count': 300},
            {'shop_id': 2, 'geo_id': 213, 'window': 34, 'count': 200},
            {'shop_id': 3, 'geo_id': 213, 'window': 0, 'count': 120},
            {'shop_id': 3, 'geo_id': 213, 'window': 1, 'count': 120},
            {'shop_id': 5, 'geo_id': 213, 'window': 42, 'count': 10000},
            {'shop_id': 5, 'geo_id': 213, 'window': 43, 'count': 10000},
            {'shop_id': 5, 'geo_id': 213, 'window': 44, 'count': 10000},
            {'shop_id': 5, 'geo_id': 213, 'window': 45, 'count': 10000},
            {'shop_id': 5, 'geo_id': 213, 'window': 46, 'count': 10000},
            {'shop_id': 5, 'geo_id': 213, 'window': 47, 'count': 10000},
            {'shop_id': 6, 'geo_id': 213, 'window': 44, 'count': 230},
            {'shop_id': 6, 'geo_id': 213, 'window': 45, 'count': 230},
            {'shop_id': 6, 'geo_id': 213, 'window': 46, 'count': 230},
            {'shop_id': 6, 'geo_id': 213, 'window': 47, 'count': 230},
        ], sorted(
            yt.read_table('{}/{}'.format(result_path, table)),
            key=itemgetter('shop_id', 'geo_id', 'window', 'count')
        ))
