#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import yatest
import json

from market.pylibrary.mindexerlib import util
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix, YtStuff, YtConfig

YT_SERVER = None
MAX_PRICE_VALUE = 9223372036854775807
OVERFLOW_INT64_VAL = MAX_PRICE_VALUE + 1


def setup_group_access(yt):
    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()
    setup_group_access(module.YT_SERVER.get_yt_client())


def create_genlog_table(yt_client, table_path, offers):
    yt_client.create("table", table_path, recursive=True, attributes=dict(
        schema=[
            dict(name="feed_id", type="uint64"),
            dict(name="offer_id", type="string"),
            dict(name="title", type="string"),
            dict(name="model_id", type="uint64"),
            dict(name="cluster_id", type="uint64"),
            dict(name="category_id", type="uint64"),
            dict(name="price", type="string"),
            dict(name="oldprice", type="string"),
            dict(name="ware_md5", type="string"),
            dict(name="classifier_magic_id", type="string"),

            dict(name="pickup_options", type="string"),
            dict(name="delivery_options", type="string"),
            dict(name="mbi_delivery_options", type="string"),
        ]
    ))
    yt_client.write_table(table_path, offers)


def create_currency_table(yt_client, table_path):
    yt_client.create("table", table_path, recursive=True, attributes=dict(
        schema=[
            dict(name="currency_from", type="string"),
            dict(name="currency_to", type="string"),
            dict(name="rate", type="double"),
        ]
    ))


@pytest.yield_fixture(scope="module")
def genlogs_dir():
    return "{}/genlogs".format(get_yt_prefix())


@pytest.yield_fixture(scope="module")
def daily_prices_dir():
    return "{}/daily_prices".format(get_yt_prefix())


@pytest.yield_fixture(scope="module")
def currency_table():
    return "{}/currency".format(get_yt_prefix())


def price_fixed_num(price, set_exact=False):
    if set_exact is True:
        return price
    return int(round(price * (10 ** 7)))


def price_str(price, set_exact=False):
    return "RUR " + str(price_fixed_num(price, set_exact))


def _get_pickup_options(pickup_costs):
    if not pickup_costs:
        return None

    options = []
    for pickup_cost in pickup_costs:
        option = {}
        option["Cost"] = pickup_cost
        options.append(option)

    return json.dumps(options)


def _get_delivery_options(delivery_costs, region_id):
    if not delivery_costs:
        return None

    result = []
    elem = {}
    result.append(elem)

    elem["RegionId"] = region_id
    options = []
    for delivery_cost in delivery_costs:
        option = {}
        option["price"] = int(delivery_cost * (10**7))
        options.append(option)
    elem["DeliveryOptions"] = options

    return json.dumps(result)


def genlog_offer(feed, offer, price, old_price, pickup_options=None, delivery_options=None, mbi_delivery_options=None, model=1,
                 cluster=1, category=1, title="дырки от бублика", ware_md5="0", set_exact_price=False):
    if pickup_options is None:
        pickup_options = []
    if delivery_options is None:
        delivery_options = []
    if mbi_delivery_options is None:
        mbi_delivery_options = []

    return {
        "feed_id": feed,
        "offer_id": str(offer),
        "title": title,
        "model_id": model,
        "cluster_id": cluster,
        "category_id": category,
        "price": price_str(price, set_exact_price),
        "oldprice": price_str(old_price),
        "ware_md5": ware_md5,
        "classifier_magic_id": "0",
        "pickup_options": _get_pickup_options(pickup_options),
        "delivery_options": _get_delivery_options(delivery_options, 1),
        "mbi_delivery_options": _get_delivery_options(mbi_delivery_options, 1),
    }


def daily_offer(feed, offer, price, oldprice, delivery_price=(), model=1, cluster=1, category=1, title="дырки от бублика", ware_md5="0", set_exact_price=False):
    result = {
        "feed_id": feed,
        "offer_id": str(offer),
        "title": title,
        "model_id": model,
        "cluster_id": cluster,
        "category_id": category,
        "price_min": price_fixed_num(price[0], set_exact_price),
        "price_max": price_fixed_num(price[1], set_exact_price),
        "price_average": price_fixed_num(price[2], set_exact_price),
        "oldprice_min": price_fixed_num(oldprice[0]),
        "oldprice_max": price_fixed_num(oldprice[1]),
        "oldprice_average": price_fixed_num(oldprice[2]),
        "ware_md5": ware_md5,
        "classifier_magic_id": "0",
    }
    if delivery_price:
        result["delivery_price_min"] = min(price_fixed_num(delivery_price[0]), MAX_PRICE_VALUE)
        result["delivery_price_max"] = min(price_fixed_num(delivery_price[1]), MAX_PRICE_VALUE)
        result["delivery_price_average"] = min(price_fixed_num(delivery_price[2]), MAX_PRICE_VALUE)

    return result


@pytest.yield_fixture(scope="module")
def data():
    return {
        "genlogs": {
            "20010701_0001":
                [
                    genlog_offer(feed=1, offer=1, price=100, old_price=100, pickup_options=[100, 150], delivery_options=[200, 500]),
                    genlog_offer(feed=1, offer=2, price=100, old_price=120, pickup_options=[100, 150], delivery_options=[200, 500]),
                    # i64 overflow
                    genlog_offer(feed=1, offer=3, price=100, old_price=120, pickup_options=[], mbi_delivery_options=[1374389534720]),
                ],
            "20010701_0101":
                [
                    genlog_offer(feed=1, offer=1, price=200, old_price=200, pickup_options=[200, 150], delivery_options=[300, 500]),
                    genlog_offer(feed=1, offer=2, price=100, old_price=120, pickup_options=[200, 150], mbi_delivery_options=[30, 100]),
                ],

            # 20010702 is absent :(

            "20010703_1501":
                [
                    genlog_offer(feed=1, offer=1, price=100, old_price=100),
                ],
            "20010703_2101":
                [
                    genlog_offer(feed=1, offer=2, price=100, old_price=120),
                ],
            # This offer has price which is a valid int64_t number. It is written to daily_stat table.
            "20010703_2102":
                [
                    genlog_offer(feed=1, offer=3, price=MAX_PRICE_VALUE, old_price=130, set_exact_price=True),
                ],
            # This offer has price which cannot be parsed from string into a valid int64_t number because of overflow.
            # It should not be written to daily_stat table.
            "20010703_2103":
                [
                    genlog_offer(feed=1, offer=4, price=OVERFLOW_INT64_VAL, old_price=140, set_exact_price=True),
                ],
        },

        "daily_stat": {
            "20010701":
                [
                    daily_offer(feed=1, offer=1, price=(100, 200, 200), oldprice=(100, 200, 200), delivery_price=(100, 150, 150)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120), delivery_price=(30, 100, 100)),
                    daily_offer(feed=1, offer=3, price=(100, 100, 100), oldprice=(120, 120, 120),
                                delivery_price=(1374389534720, 1374389534720, 1374389534720)),
                ],
            "20010702":
                [
                    daily_offer(feed=1, offer=1, price=(100, 200, 200), oldprice=(100, 200, 200), delivery_price=(100, 150, 150)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120), delivery_price=(30, 100, 100)),
                    daily_offer(feed=1, offer=3, price=(100, 100, 100), oldprice=(120, 120, 120),
                                delivery_price=(1374389534720, 1374389534720, 1374389534720)),
                ],
            "20010703":
                [
                    daily_offer(feed=1, offer=1, price=(100, 100, 100), oldprice=(100, 100, 100)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120)),
                    daily_offer(feed=1, offer=3, price=(MAX_PRICE_VALUE, MAX_PRICE_VALUE, MAX_PRICE_VALUE), oldprice=(130, 130, 130), set_exact_price=True),
                ],
            "20010704":
                [
                    daily_offer(feed=1, offer=1, price=(100, 100, 100), oldprice=(100, 100, 100)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120)),
                    daily_offer(feed=1, offer=3, price=(MAX_PRICE_VALUE, MAX_PRICE_VALUE, MAX_PRICE_VALUE), oldprice=(130, 130, 130), set_exact_price=True),
                ],
            "20010705":
                [
                    daily_offer(feed=1, offer=1, price=(100, 100, 100), oldprice=(100, 100, 100)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120)),
                    daily_offer(feed=1, offer=3, price=(MAX_PRICE_VALUE, MAX_PRICE_VALUE, MAX_PRICE_VALUE), oldprice=(130, 130, 130), set_exact_price=True),
                ],
            "20010706":
                [
                    daily_offer(feed=1, offer=1, price=(100, 100, 100), oldprice=(100, 100, 100)),
                    daily_offer(feed=1, offer=2, price=(100, 100, 100), oldprice=(120, 120, 120)),
                    daily_offer(feed=1, offer=3, price=(MAX_PRICE_VALUE, MAX_PRICE_VALUE, MAX_PRICE_VALUE), oldprice=(130, 130, 130), set_exact_price=True),
                ],
        }

    }


def check_table(expected, table):
    given = [x for x in table]
    assert len(expected) == len(given)
    for i in range(0, len(expected)):
        for key, value in expected[i].items():
            assert value == given[i][key]


def test_daily_prices(genlogs_dir, daily_prices_dir, currency_table, data):
    global YT_SERVER
    yt_client = YT_SERVER.get_yt_client()

    for timestamp, offers in data["genlogs"].iteritems():
        create_genlog_table(yt_client, genlogs_dir + "/" + timestamp, offers)

    days = ["20010701", "20010702", "20010703", "20010704", "20010705", "20010706"]

    create_currency_table(yt_client, currency_table)

    cmdlist = [
        yatest.common.binary_path("market/idx/promos/pricedrops/bin/yt_pricedrops_cpp"),
        "--specific-step", "daily_prices",
        "--server", YT_SERVER.get_server(),
        "--genlogs-dir-path", genlogs_dir,
        "--daily-prices-dir-path", daily_prices_dir,
        "--mstat-currency-table-path", currency_table,
        "--price-history-days-total", str(len(days)),
        "--price-history-start-date", "2001-07-06",
    ]
    util.watching_check_call(cmdlist)

    assert len(yt_client.list(daily_prices_dir)) == len(data["daily_stat"])
    for day in days:
        daily_table_path = "{}/{}".format(daily_prices_dir, day)
        assert yt_client.exists(daily_table_path)
        check_table(data["daily_stat"][day], yt_client.read_table(daily_table_path))
