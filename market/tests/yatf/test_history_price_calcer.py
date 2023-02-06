import yatest
import subprocess

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig
STEP_NAME = "history_price_calcer"

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


def create_table(yt, table_name):
    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True
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
    daily_prices_dir_path = "{}/prices".format(get_yt_prefix())
    history_prices_dir_path = "{}/hprices".format(daily_prices_dir_path)
    first_day_table = "{}/20010701".format(daily_prices_dir_path)
    second_day_table = "{}/20010702".format(daily_prices_dir_path)
    third_day_table = "{}/20010703".format(daily_prices_dir_path)

    create_table(yt, currency_table)
    create_table(yt, first_day_table)
    create_table(yt, second_day_table)
    create_table(yt, third_day_table)

    yt.write_table(first_day_table, [
        {'feed_id': 1205, 'offer_id': '00001', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00002', 'price_average': 3, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00003', 'price_average': 1, 'delivery_price_average': 30, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00004', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00005', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        # not enough days to have history price
        {'feed_id': 1205, 'offer_id': '00006', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00007', 'price_average': 100, 'delivery_price_average': 0, 'currency': 'BYN', 'title': 'entry'},
    ])
    yt.write_table(second_day_table, [
        {'feed_id': 1205, 'offer_id': '00001', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        # max(min(1, 2), min(2, 3)) -> 2
        {'feed_id': 1205, 'offer_id': '00002', 'price_average': 2, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        # delivery price takes part in calculation
        {'feed_id': 1205, 'offer_id': '00003', 'price_average': 3, 'delivery_price_average': 20, 'currency': 'RUR', 'title': 'entry'},
        # changed title
        {'feed_id': 1205, 'offer_id': '00004', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'another entry'},
        {'feed_id': 1205, 'offer_id': '00005', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00007', 'price_average': 100, 'delivery_price_average': 0, 'currency': 'BYN', 'title': 'entry'},
    ])
    yt.write_table(third_day_table, [
        {'feed_id': 1205, 'offer_id': '00001', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00002', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00003', 'price_average': 2, 'delivery_price_average': 10, 'currency': 'RUR', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00004', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'RUR', 'title': 'entry'},
        # changed currency
        {'feed_id': 1205, 'offer_id': '00005', 'price_average': 1, 'delivery_price_average': 0, 'currency': 'BYN', 'title': 'entry'},
        {'feed_id': 1205, 'offer_id': '00007', 'price_average': 100, 'delivery_price_average': 0, 'currency': 'BYN', 'title': 'entry'},
    ])

    yt.write_table(currency_table, [
        {"currency_from": "BYN", "currency_to": "RUR", "rate": 29.18},
    ])

    cmdlist = [
        yatest.common.binary_path('market/idx/promos/pricedrops/bin/yt_pricedrops_cpp'),
        "--specific-step", STEP_NAME,
        "--server", YT_SERVER.get_server(),
        "--mstat-currency-table-path", currency_table,
        "--daily-prices-dir-path", daily_prices_dir_path,
        "--price-history-dir-path", history_prices_dir_path,
        "--price-history-start-date", "2001-07-03",
        "--price-history-days-total", "3",
        "--price-history-window-size", "2",
    ]
    run_bin(cmdlist)

    history_price_tables = yt.list(history_prices_dir_path)
    assert len(history_price_tables) == 1
    history_price_table = history_price_tables[0]

    check_table([
        {'feed_id': 1205, 'offer_id': '00001', 'history_price': 1},
        {'feed_id': 1205, 'offer_id': '00002', 'history_price': 2},
        {'feed_id': 1205, 'offer_id': '00003', 'history_price': 3},
        {'feed_id': 1205, 'offer_id': '00004', 'history_price': 0},
        {'feed_id': 1205, 'offer_id': '00006', 'history_price': 0},
        {'feed_id': 1205, 'offer_id': '00007', 'history_price': 2918},
    ], yt.read_table("{}/{}".format(history_prices_dir_path, history_price_table)))
