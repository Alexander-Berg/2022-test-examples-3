import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = 'history_promos_calcer'

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


def test_history_promos_calcer():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    daily_promos_dir_path = '{}/daily_promos'.format(get_yt_prefix())
    history_promos_dir_path = '{}/history_promos'.format(get_yt_prefix())
    working_dir_path = '{}/workdir'.format(get_yt_prefix())
    first_day_daily_promos_path = '{}/20010701'.format(daily_promos_dir_path)
    second_day_daily_promos_path = '{}/20010702'.format(daily_promos_dir_path)
    third_day_daily_promos_path = '{}/20010703'.format(daily_promos_dir_path)
    fourth_day_daily_promos_path = '{}/20010704'.format(daily_promos_dir_path)

    daily_promos_table_schema = [
        dict(name='category_id', type='uint64', sort_order='ascending'),
        dict(name='discounts_count', type='uint64'),
        dict(name='promos_count', type='uint64'),
    ]

    create_table(yt, first_day_daily_promos_path, daily_promos_table_schema)
    create_table(yt, second_day_daily_promos_path, daily_promos_table_schema)
    create_table(yt, third_day_daily_promos_path, daily_promos_table_schema)
    create_table(yt, fourth_day_daily_promos_path, daily_promos_table_schema)

    yt.write_table(first_day_daily_promos_path, [
        {'category_id': 1, 'discounts_count': 0, 'promos_count': 0},
        {'category_id': 2, 'discounts_count': 1, 'promos_count': 0},
        {'category_id': 3, 'discounts_count': 0, 'promos_count': 0},
    ])
    yt.write_table(second_day_daily_promos_path, [
        {'category_id': 1, 'discounts_count': 3, 'promos_count': 2},
        {'category_id': 2, 'discounts_count': 1, 'promos_count': 0},
        {'category_id': 3, 'discounts_count': 0, 'promos_count': 0},
    ])
    yt.write_table(third_day_daily_promos_path, [
        {'category_id': 1, 'discounts_count': 2, 'promos_count': 2},
        {'category_id': 2, 'discounts_count': 0, 'promos_count': 0},
        {'category_id': 3, 'discounts_count': 0, 'promos_count': 0},
    ])
    yt.write_table(fourth_day_daily_promos_path, [
        {'category_id': 1, 'discounts_count': 0, 'promos_count': 0},
        {'category_id': 2, 'discounts_count': 0, 'promos_count': 0},
        {'category_id': 3, 'discounts_count': 0, 'promos_count': 0},
    ])

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/white_salesinfo/bin/yt_salesinfo'),
        '--specific-step', STEP_NAME,
        '--server', YT_SERVER.get_server(),
        '--daily-promos-dir-path', daily_promos_dir_path,
        '--history-promos-dir-path', history_promos_dir_path,
        '--working-dir-path', working_dir_path,
        '--start-date', '2001-07-04',
        '--days-total', '4',
        '--window-size', '2',
    ]
    run_bin(cmdlist)

    assert len(yt.list(history_promos_dir_path)) == 1

    check_table([
        {'category_id': 1, 'history_promos': 4},
        {'category_id': 2, 'history_promos': 1},
        {'category_id': 3, 'history_promos': 0},
    ], yt.read_table('{}/{}'.format(history_promos_dir_path, '20010704')))
