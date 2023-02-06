#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest

from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.test_envs.yql_env import YqlTestEnv
from market.idx.yatf.resources.yql_resource import (
    YtResource,
    YqlRequestResource,
)
from yt.wrapper import ypath_join

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as ProtoOffer
import market.idx.pylibrary.mindexer_core.price_history.price_history as price_history


def create_history_offer(
        feed_id,
        offer_id,
        currency,
        price_average,
        delivery_price_average,
        title,
        category_id
):
    offer = dict(
        feed_id=feed_id,
        offer_id=offer_id,
        currency=currency,
        price_average=price_average,
        title=title,
        category_id=category_id
    )
    if delivery_price_average is not None:
        offer['delivery_price_average'] = delivery_price_average
    return offer


def create_history_blue_offer(
        feed_id,
        offer_id,
        currency,
        price_average,
        delivery_price_average,
        title,
        category_id,
        market_sku
):
    result = create_history_offer(
        feed_id,
        offer_id,
        currency,
        price_average,
        delivery_price_average,
        title,
        category_id
    )
    result.update(dict(market_sku=market_sku))
    return result


def _create_history_table_schema():
    return [
        dict(type='uint64', name='feed_id'),
        dict(type='string', name='offer_id'),
        dict(type='string', name='currency'),
        dict(type='double', name='price_average'),
        dict(type='int64', name='delivery_price_average'),
        dict(type='string', name='title'),
        dict(type='uint64', name='category_id'),
    ]


def _create_blue_history_table_schema():
    schema = _create_history_table_schema()
    schema.append(dict(type='uint64', name='market_sku'))
    return schema


def _create_table(yt_client, table_path, schema):
    yt_client.create(
        'table',
        table_path,
        recursive=True,
        attributes=dict(schema=schema)
    )


def create_history_table(yt_client, table_path):
    _create_table(
        yt_client,
        table_path,
        _create_history_table_schema()
    )


def create_blue_history_table(yt_client, table_path):
    _create_table(
        yt_client,
        table_path,
        _create_blue_history_table_schema()
    )


def get_history_path():
    return ypath_join(get_yt_prefix(), 'history')


def get_daily_genlog_path():
    return ypath_join(get_yt_prefix(), 'offers', '%s_0000' % price_history.get_yyyymmdd())


class HistoryOffersData(object):
    def __init__(self, blue):
        self.offer_data = [
            {
                'prices':          [2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, ],
                'delivery_prices': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
            },
            {
                'prices':          [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 3, 3, 1, 1, 1, ],
                'delivery_prices': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
            },
            {
                'prices':          [2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 1, 1, 1, 1, 1, ],
                'delivery_prices': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
            },
            {
                'prices':          [2, 2, 1, 2, 2, 2, 1, 2, 2, 1, 2, 3, 3, 3, 3, 1, 1, 1, 1, 1, ],
                'delivery_prices': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
            },
            {
                'prices':          [1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 2, 1, 2, 3, 3, 3, 3, 1, 1, 1, ],
                'delivery_prices': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
            },
            # check that delivery price takes into account
            {
                'prices':          [1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
                'delivery_prices': [3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, ],
            },
            # delivery price can be omitted - consider it like delivery for free
            {
                'prices':          [1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, ],
                'delivery_prices': [None, None, None, None, None, None, None, 1, 1, 1, 1, None, None, None, None, None, None, None, None, None, ],
            },
        ]

        self.days = len(self.offer_data[0]['prices'])
        for data in self.offer_data:
            assert len(data['prices']) == self.days
            assert len(data['delivery_prices']) == self.days

        self.tables = ['%s/%s' % (get_history_path(), price_history.get_yyyymmdd(day_i)) for day_i in range(1 - self.days, 1)]

        create_offer = self.create_blue_offer if blue else self.create_offer
        self.offers = [
            [
                create_offer(
                    offer_id=offer_i + 1,
                    price=self.offer_data[offer_i]['prices'][day_i],
                    delivery_price=self.offer_data[offer_i]['delivery_prices'][day_i],
                ) for offer_i in range(len(self.offer_data))
            ]
            for day_i in range(self.days)
        ]

    def create_offer(self, offer_id, price, delivery_price):
        return create_history_offer(
            feed_id=1,
            offer_id=str(offer_id),
            currency='RUR',
            price_average=float(price),
            delivery_price_average=int(delivery_price) if delivery_price is not None else None,
            title='name %d' % (offer_id),
            category_id=1
        )

    def create_blue_offer(self, offer_id, price, delivery_price):
        return create_history_blue_offer(
            feed_id=1,
            offer_id=str(offer_id),
            currency='RUR',
            price_average=float(price),
            delivery_price_average=int(delivery_price) if delivery_price is not None else None,
            title='name %d' % (offer_id),
            category_id=1,
            market_sku=offer_id
        )


@pytest.fixture(scope='module')
def history_offers_data():
    return HistoryOffersData(blue=False)


@pytest.fixture(scope='module')
def history_blue_offers_data():
    return HistoryOffersData(blue=True)


@pytest.fixture(scope='module')
def data_history_price(yt_server, history_offers_data):
    yt_client = yt_server.get_yt_client()
    for t in history_offers_data.tables:
        create_history_table(yt_client, t)

    for i in range(history_offers_data.days):
        off = history_offers_data.offers[i]
        if off:
            yt_client.write_table(history_offers_data.tables[i], off)


@pytest.fixture(scope='module')
def data_blue_history_price(yt_server, history_blue_offers_data):
    yt_client = yt_server.get_yt_client()
    for t in history_blue_offers_data.tables:
        create_blue_history_table(yt_client, t)

    for i in range(history_blue_offers_data.days):
        off = history_blue_offers_data.offers[i]
        if off:
            yt_client.write_table(history_blue_offers_data.tables[i], off)


def patch_yql_template(template):
    def bad_line(line):
        return line == 'USE {yt_server};' or 'INSERT INTO' in line

    splitted = template.split('\n')
    filtered = [line for line in splitted if not bad_line(line)]
    return '\n'.join(filtered)


def get_yql_daily_prices_request(history_group_fields, comma_separated_tables):
    return price_history.yql_daily_prices_common_request_tpl.format(
        history_group_fields=history_group_fields,
        comma_separated_tables=comma_separated_tables
    )


@pytest.yield_fixture(scope="module")
def hprice_workflow(yt_server, data_history_price, history_offers_data):
    patched_tpl = patch_yql_template(price_history.yql_history_prices_request_tpl)

    yyyymmdd = price_history.get_yyyymmdd()
    request = patched_tpl.format(
        days_to_expire=1,
        history_path=get_history_path(),
        yyyymmdd=yyyymmdd,
        window_size=3,
        days_total=10,
        add_days_for_min_price=5,
        pool_name='pool',
        shifttbl=price_history.calc_shift_tbl(yyyymmdd, 20),
        hprices_id_columns='feed_id, offer_id',
        history_group_fields='feed_id, offer_id',
        hprices_sort_columns='feed_id, offer_id',
        yql_daily_prices_request=get_yql_daily_prices_request(
            history_group_fields='feed_id, offer_id',
            comma_separated_tables=',\n'.join(['`%s`' % t for t in history_offers_data.tables]),
        )
    )

    resources = {
        'yt': YtResource(yt_stuff=yt_server),
        'request': YqlRequestResource(request)
    }
    with YqlTestEnv(syntax_version=1, **resources) as test_env:
        test_env.execute()
        yield test_env


@pytest.yield_fixture(scope="module")
def hprice_blue_workflow(yt_server, data_blue_history_price, history_blue_offers_data):
    patched_tpl = patch_yql_template(price_history.yql_history_prices_request_tpl)

    yyyymmdd = price_history.get_yyyymmdd()
    request = patched_tpl.format(
        days_to_expire=1,
        history_path=get_history_path(),
        yyyymmdd=yyyymmdd,
        window_size=3,
        days_total=10,
        add_days_for_min_price=5,
        pool_name='pool',
        shifttbl=price_history.calc_shift_tbl(yyyymmdd, 20),
        hprices_id_columns='market_sku as msku',
        history_group_fields='market_sku',
        hprices_sort_columns='msku',
        yql_daily_prices_request=get_yql_daily_prices_request(
            history_group_fields='market_sku',
            comma_separated_tables=',\n'.join(['`%s`' % t for t in history_blue_offers_data.tables]),
        )
    )

    resources = {
        'yt': YtResource(yt_stuff=yt_server),
        'request': YqlRequestResource(request)
    }
    with YqlTestEnv(syntax_version=1, **resources) as test_env:
        test_env.execute()
        yield test_env


def yt_proto_decode(s):
    return s[2:].decode('hex')  # some magic; hope to find regular way


def get_price(table_cell):
    po = ProtoOffer()
    po.ParseFromString(yt_proto_decode(table_cell))
    return (po.price_history.price_expression, po.price_history.min_price_expression)


@pytest.mark.skip(reason='super flaky')
def test_hprice_calc(hprice_workflow):
    results = hprice_workflow.yql_results
    assert results.is_success, [str(error) for error in results.errors]
    table = list(results)[0]  # get 1st element from a generator
    assert len(table.rows) == 8

    data = [
        [row[i] if i != 3 else get_price(row[i]) for i in range(4)] for row in table.rows
    ]
    assert data[0] == ["1", "1", "1", ("RUR 1", "RUR 3")]
    assert data[1] == ["1", "2", "3", ("RUR 3", "RUR 1")]
    assert data[2] == ["1", "3", "3", ("RUR 3", "RUR 2")]
    assert data[3] == ["1", "4", "3", ("RUR 3", "RUR 2")]
    assert data[4] == ["1", "5", "3", ("RUR 3", "RUR 2")]
    assert data[5] == ["1", "6", "1", ("RUR 1", "RUR 1")]
    assert data[6] == ["1", "7", "2", ("RUR 2", "RUR 1")]
    assert data[7] == ["1", "8", "1", ("RUR 1", "RUR 1")]


@pytest.mark.skip(reason='super flaky')
def test_blue_hprice_calc(hprice_blue_workflow):
    results = hprice_blue_workflow.yql_results
    assert results.is_success, [str(error) for error in results.errors]
    table = list(results)[0]  # get 1st element from a generator
    assert len(table.rows) == 8

    data = [
        [row[i] if i != 2 else get_price(row[i]) for i in range(3)] for row in table.rows
    ]
    assert data[0] == ["1", "1", ("RUR 1", "RUR 3")]
    assert data[1] == ["2", "3", ("RUR 3", "RUR 1")]
    assert data[2] == ["3", "3", ("RUR 3", "RUR 2")]
    assert data[3] == ["4", "3", ("RUR 3", "RUR 2")]
    assert data[4] == ["5", "3", ("RUR 3", "RUR 2")]
    assert data[5] == ["6", "1", ("RUR 1", "RUR 1")]
    assert data[6] == ["7", "2", ("RUR 2", "RUR 1")]
    assert data[7] == ["8", "1", ("RUR 1", "RUR 1")]


@pytest.yield_fixture(
    scope='module',
    params=[
        {
            'input': [
                {'price': 'RUR 50', 'oldprice': 'RUR 100'},
                {'price': 'RUR 100', 'oldprice': 'RUR 200'},
                {'price': 'RUR 200', 'oldprice': 'RUR 300'},
                {'price': 'RUR 300', 'oldprice': 'RUR 400'},
            ],
            'output': {
                'price_max': 300,
                'price_min': 50,
                'price_average': 100.0,
                'oldprice_max': 400,
                'oldprice_min': 100,
                'oldprice_average': 200.0,
                'currency': 'RUR',
            },
        }
    ]
)
def test_calculate_average_data(request):
    return request.param


def test_calculate_average(test_calculate_average_data):
    context = price_history.PriceHistoryContext()
    avarage_data = price_history._calculate_average(context, test_calculate_average_data['input'])
    assert avarage_data == test_calculate_average_data['output']
