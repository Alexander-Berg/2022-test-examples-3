import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = 'sales_finder'

YT_SERVER = None


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def create_table(yt, table_name, schema=None):
    if schema is None:
        schema = []
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=dict(
            schema=schema
        )
    )


def run_bin(cmdlist):
    try:
        subprocess.check_call(cmdlist)
    except Exception as e:
        print e
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


def test_sales_finder():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    daily_promos_dir_path = '{}/daily_promos'.format(get_yt_prefix())
    history_promos_output_table_path = '{}/history_promos/20010704'.format(get_yt_prefix())
    sales_finder_dir_path = '{}/sales_finder'.format(get_yt_prefix())
    last_day_daily_promos_path = '{}/20010704'.format(daily_promos_dir_path)
    working_dir_path = '{}/workdir'.format(get_yt_prefix())

    daily_promos_table_schema = [
        dict(name='category_id', type='uint64', sort_order='ascending'),
        dict(name='discounts_count', type='uint64'),
        dict(name='promos_count', type='uint64'),
    ]

    history_promos_table_schema = [
        dict(name='category_id', type='uint64', sort_order='ascending'),
        dict(name='history_promos', type='uint64'),
    ]

    create_table(yt, last_day_daily_promos_path, daily_promos_table_schema)
    create_table(yt, history_promos_output_table_path, history_promos_table_schema)

    yt.write_table(history_promos_output_table_path, [
        {'category_id': 1, 'history_promos': 400},
        {'category_id': 2, 'history_promos': 700},
        {'category_id': 3, 'history_promos': 1000},
        {'category_id': 4, 'history_promos': 2},
    ])
    yt.write_table(last_day_daily_promos_path, [
        # greater -> found sale
        {'category_id': 1, 'discounts_count': 300, 'promos_count': 300},
        # equal
        {'category_id': 2, 'discounts_count': 400, 'promos_count': 300},
        # less
        {'category_id': 3, 'discounts_count': 444, 'promos_count': 333},
        # less than threshold
        {'category_id': 4, 'discounts_count': 299, 'promos_count': 200},
    ])
    cmdlist = [
        yatest.common.binary_path('market/idx/promos/white_salesinfo/bin/yt_salesinfo'),
        '--specific-step', STEP_NAME,
        '--server', YT_SERVER.get_server(),
        '--daily-promos-dir-path', daily_promos_dir_path,
        '--history-promos-output-table-path', history_promos_output_table_path,
        '--sales-finder-dir-path', sales_finder_dir_path,
        '--working-dir-path', working_dir_path,
        '--start-date', '2001-07-04',
        '--sum-threshold', '500'
    ]
    run_bin(cmdlist)

    assert len(yt.list(sales_finder_dir_path)) == 2

    check_table([
        {'category_id': 1, 'percent_over_history': 50},
    ], yt.read_table('{}/{}'.format(sales_finder_dir_path, '20010704')))
    check_table([
        {'category_id': 1, 'percent_over_history': 50},
    ], yt.read_table('{}/{}'.format(sales_finder_dir_path, 'recent')))
