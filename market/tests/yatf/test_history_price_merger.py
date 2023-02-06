import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = "history_price_merger"
BERU_VIRTUAL_FEED_ID = 475690

YT_SERVER = None


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


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


def test_history_price_calcer():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    currency_table = "{}/currency_rates".format(get_yt_prefix())
    white_history_table = "{}/prices/hprices/last_complete".format(get_yt_prefix())
    blue_history_table = "{}/blue/prices/hprices/last_complete".format(get_yt_prefix())
    mapped_blue_history_table = "{}/prices/pricedrops_input/blue/hprices".format(get_yt_prefix())
    price_history_table = "{}/prices/pricedrops_input/hprices".format(get_yt_prefix())

    create_table(yt, currency_table)
    create_table(yt, white_history_table, schema=[
        dict(name='feed_id', type='uint64'),
        dict(name='offer_id', type='string'),
        dict(name='history_price', type='int64'),
        dict(name='offer', type='string'),
        dict(name='genlog', type='string'),
    ])
    create_table(yt, blue_history_table, schema=[
        dict(name='msku', type='uint64'),
        dict(name='history_price', type='int64'),
        dict(name='offer', type='string'),
        dict(name='genlog', type='string'),
    ])

    white_history_data = [
        {"feed_id": 1205, "offer_id": "00001", "history_price": 1},
        {"feed_id": 1205, "offer_id": "00002", "history_price": 2},
        {"feed_id": 1205, "offer_id": "00003", "history_price": 3},
    ]

    yt.write_table(white_history_table, white_history_data)
    yt.write_table(blue_history_table, [
        {"msku": 41, "history_price": 10},
        {"msku": 42, "history_price": 20},
    ])
    cmdlist = [
        yatest.common.binary_path('market/idx/promos/pricedrops/bin/yt_pricedrops_cpp'),
        "--specific-step", STEP_NAME,
        "--server", YT_SERVER.get_server(),
        "--mstat-currency-table-path", currency_table,
        "--price-history-white-table-path", white_history_table,
        "--price-history-blue-table-path", blue_history_table,
        "--price-history-mapped-blue-table-path", mapped_blue_history_table,
        "--price-history-table-path", price_history_table,
    ]
    run_bin(cmdlist)

    blue_history_mapped_data = [
        {"feed_id": BERU_VIRTUAL_FEED_ID, "offer_id": "41", "history_price": 10},
        {"feed_id": BERU_VIRTUAL_FEED_ID, "offer_id": "42", "history_price": 20},
    ]

    check_table(blue_history_mapped_data, yt.read_table(mapped_blue_history_table))
    check_table(sorted(white_history_data + blue_history_mapped_data), sorted(yt.read_table(price_history_table)))
