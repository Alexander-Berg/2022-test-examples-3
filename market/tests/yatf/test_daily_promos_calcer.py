import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = 'daily_promos_calcer'

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


def test_daily_promos_calcer():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    categories_table_path = '{}/categories/latest'.format(get_yt_prefix())
    genlog_dir_path = '{}/offers'.format(get_yt_prefix())
    daily_promos_dir_path = '{}/daily_promos'.format(get_yt_prefix())
    working_dir_path = '{}/workdir'.format(get_yt_prefix())
    first_day_genlog_path = '{}/20010701_1000'.format(genlog_dir_path)
    second_day_genlog_path = '{}/20010702_1000'.format(genlog_dir_path)
    third_day_genlog_path = '{}/20010703_1000'.format(genlog_dir_path)
    fourth_day_genlog_path = '{}/20010704_1000'.format(genlog_dir_path)

    categories_table_schema = [
        dict(name='hyper_id', type='int64'),
        dict(name='parent_hyper_ids', type='any'),
    ]
    genlog_table_schema = [
        dict(name='category_id', type='uint64'),
        dict(name='oldprice', type='string'),
        dict(name='promo_type', type='uint64'),
    ]

    create_table(yt, categories_table_path, categories_table_schema)
    create_table(yt, first_day_genlog_path, genlog_table_schema)
    create_table(yt, second_day_genlog_path, genlog_table_schema)
    create_table(yt, third_day_genlog_path, genlog_table_schema)
    create_table(yt, fourth_day_genlog_path, genlog_table_schema)

    yt.write_table(categories_table_path, [
        {'hyper_id': 1, 'parent_hyper_ids': []},
        {'hyper_id': 2, 'parent_hyper_ids': [1]},
        {'hyper_id': 3, 'parent_hyper_ids': [1, 2]},
        {'hyper_id': 4, 'parent_hyper_ids': [1, 2, 3]},
        {'hyper_id': 5, 'parent_hyper_ids': [1, 2, 3, 4]},
    ])
    yt.write_table(first_day_genlog_path, [
        {'category_id': 3},
        {'category_id': 4, 'promo_type': 16, 'oldprice': 'RUR 78'},
        # this category promos are counted in its ancestors, excluding root (2, 3, 4)
        {'category_id': 5, 'promo_type': 1024, 'oldprice': 'RUR 56'},
    ])
    yt.write_table(second_day_genlog_path, [
        {'category_id': 2, 'promo_type': 256},
        {'category_id': 2},
        {'category_id': 3, 'oldprice': 'RUR 10'},
        {'category_id': 3, 'oldprice': 'RUR 20'},
    ])
    yt.write_table(third_day_genlog_path, [
        {'category_id': 2},
        {'category_id': 3, 'promo_type': 128},
    ])
    yt.write_table(fourth_day_genlog_path, [
        {'category_id': 2, 'promo_type': 16, 'oldprice': 'RUR 15'},
    ])
    cmdlist = [
        yatest.common.binary_path('market/idx/promos/white_salesinfo/bin/yt_salesinfo'),
        '--specific-step', STEP_NAME,
        '--server', YT_SERVER.get_server(),
        '--categories-table-path', categories_table_path,
        '--genlog-dir-path', genlog_dir_path,
        '--daily-promos-dir-path', daily_promos_dir_path,
        '--working-dir-path', working_dir_path,
        '--start-date', '2001-07-04',
        '--days-total', '4',
        '--categories-depth', '3'
    ]
    run_bin(cmdlist)

    assert len(yt.list(daily_promos_dir_path)) == 4

    check_table([
        {'category_id': 2, 'promos_count': 2, 'discounts_count': 2},
        {'category_id': 3, 'promos_count': 2, 'discounts_count': 2},
        {'category_id': 4, 'promos_count': 2, 'discounts_count': 2},
    ], yt.read_table('{}/{}'.format(daily_promos_dir_path, '20010701')))
    check_table([
        {'category_id': 2, 'promos_count': 1, 'discounts_count': 2},
        {'category_id': 3, 'promos_count': 0, 'discounts_count': 2},
    ], yt.read_table('{}/{}'.format(daily_promos_dir_path, '20010702')))
    check_table([
        {'category_id': 2, 'promos_count': 1, 'discounts_count': 0},
        {'category_id': 3, 'promos_count': 1, 'discounts_count': 0},
    ], yt.read_table('{}/{}'.format(daily_promos_dir_path, '20010703')))
    check_table([
        {'category_id': 2, 'promos_count': 1, 'discounts_count': 1},
    ], yt.read_table('{}/{}'.format(daily_promos_dir_path, '20010704')))
