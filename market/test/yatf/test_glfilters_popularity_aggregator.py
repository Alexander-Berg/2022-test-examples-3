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


def test_glfilters_popularity_aggregator():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    report_logs_path = '{}/report-logs'.format(get_yt_prefix())
    result_path = '{}/result'.format(get_yt_prefix())
    popularity_path = '{}/popularity'.format(result_path)
    start_date = '2001-07-07'

    day1table = '{}/2001-07-01'.format(report_logs_path)
    day2table = '{}/2001-07-02'.format(report_logs_path)
    day3table = '{}/2001-07-03'.format(report_logs_path)
    day4table = '{}/2001-07-04'.format(report_logs_path)
    day5table = '{}/2001-07-05'.format(report_logs_path)
    day6table = '{}/2001-07-06'.format(report_logs_path)
    day7table = '{}/{}'.format(report_logs_path, start_date)
    create_table(yt, day1table)
    create_table(yt, day2table)
    create_table(yt, day3table)
    create_table(yt, day4table)
    create_table(yt, day5table)
    create_table(yt, day6table)
    create_table(yt, day7table)
    yt.write_table(day1table, [
        {'request': 'http://report:17051/yandsearch?place=prime&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=2:2'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=2&glfilter=4:4;5:5'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=blue&hid=1&glfilter=666:666'},
    ])
    yt.write_table(day2table, [
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green_with_blue&hid=1&glfilter=3:3'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=2&glfilter=4:4'},
        {'request': 'http://report:17051/yandsearch?rgb=green&hid=1&glfilter=666:666'},
    ])
    yt.write_table(day3table, [
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=2:2'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=2&glfilter=4:4&glfilter=5:5'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&glfilter=666:666'},
    ])
    yt.write_table(day4table, [
        {'request': 'http://report:17051/yandsearch?place=prime&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=3:3'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green_with_blue&hid=2&glfilter=6:6'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&hid=2&glfilter=666:666'},
    ])
    yt.write_table(day5table, [
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=2:2'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=aaa&glfilter=666:666'},
    ])
    yt.write_table(day6table, [
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=2:2'},
        {'request': 'http://report:17051/yandsearch?place=prime&hid=1&glfilter=3:3'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1'},
    ])
    yt.write_table(day7table, [
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green_with_blue&hid=1&glfilter=1:1'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=2:2'},
        {'request': 'http://report:17051/yandsearch?place=prime&rgb=green&hid=1&glfilter=3:3'},
        {'request': 'http://report:17051/yandsearch?place=productoffers&rgb=green&hid=1&glfilter=666:666'},
    ])

    cmdlist = [
        yatest.common.binary_path('market/idx/generation/glfilters-popularity-aggregator/bin/glfilters-popularity-aggregator'),
        '--yt-server', YT_SERVER.get_server(),
        '--yt-report-logs-path', report_logs_path,
        '--yt-result-path', result_path,
        '--start-date', start_date
    ]
    run_bin(cmdlist)

    for table in [start_date, 'recent']:
        check_table([
            {'hid': 1, 'key': 1, 'value': 1, 'popularity': 0},
            {'hid': 1, 'key': 2, 'value': 2, 'popularity': 1},
            {'hid': 1, 'key': 3, 'value': 3, 'popularity': 2},
            {'hid': 2, 'key': 4, 'value': 4, 'popularity': 0},
            {'hid': 2, 'key': 5, 'value': 5, 'popularity': 1},
            {'hid': 2, 'key': 6, 'value': 6, 'popularity': 2},
        ], sorted(
            yt.read_table('{}/{}'.format(popularity_path, table)),
            key=itemgetter('hid', 'popularity')
        ))
