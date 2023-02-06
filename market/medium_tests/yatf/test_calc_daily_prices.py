#!/usr/bin/env python
# -*- coding: utf-8 -*-

'''
Тест проверяет калькуляцию дневных цен.
Есть два режима - с фильтрацией через price labs и без. Переключаются наличием таблицы с фильтрами.

Всего 3 теста -
1. 'smoke' - проверяет тупой вход-выход из 1го офера
2. 'no_oldprice' - проверяет что может не быть oldprice
3. 'offer_filter' - проверяет что один из оферов отфильтруется
'''

import datetime
import pytest
import uuid
import json

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.test_envs.yql_env import BaseEnv
from market.idx.yatf.resources.yql_resource import YtResource
from yt.wrapper import ypath_join

import market.idx.pylibrary.mindexer_core.price_history.price_history as price_history


DEFAULT_OFFER_FIELDS = {
    'feed_id': 4,
    'offer_id': 'offer4',
    'ware_md5': 'kOijrFDJE5Z8xj1nHxMi8g',
    'classifier_magic_id': 'a45fdf664414df5e4f8db3413f59cf69',
    'model_id': 100,
    'cluster_id': 200,
    'title': 'tool four',
    'category_id': 300,
}


def create_offer(data, blue=False):
    import copy
    result = copy.deepcopy(DEFAULT_OFFER_FIELDS)
    if blue:
        result.update({'market_sku': 10201})
    result.update(data)
    return result


class CalcDailyPricesTestEnv(BaseEnv):
    def __init__(self, **resources):
        try:
            self._yt = resources.pop('yt')  # prevents deepcopying Yt stuff
        except KeyError:
            self._yt = YtResource()

        self._genlog = resources.get('genlog', None)
        self._price_history = resources.get('price_history', None)
        self._sessions = resources.get('sessions', None)
        self._offer_filter = resources.get('offer_filter', None)
        self.output = None

        super(CalcDailyPricesTestEnv, self).__init__(**resources)

    def execute(self, blue_history=False, total_days=None):
        import logging

        yt_client = self._yt.yt_stuff.get_yt_client()

        if self._price_history:
            price_history_dir = '/'.join(self._price_history.get_path().split('/')[:-1])
        else:
            price_history_dir = ypath_join('//tmp', 'price_history', str(uuid.uuid4()))
            yt_client.create('map_node', price_history_dir, recursive=True)

        if self._genlog:
            genlog_gendir = '/'.join(self._genlog.get_path().split('/')[:-1])
        else:
            genlog_gendir = ypath_join('//tmp', 'genlog', str(uuid.uuid4()))
            yt_client.create('map_node', genlog_gendir, recursive=True)

        context = price_history.PriceHistoryContext(
            yt_client=yt_client,
            yt_price_history_dir=price_history_dir,
            yt_genlog_gendir=genlog_gendir,
            blue_history=blue_history
        )

        DAYS_TO_EXPIRE = 1
        TOTAL_DAYS = 1
        log = logging.getLogger()
        yyyymmdd = price_history.get_yyyymmdd()

        price_history._get_existing_history_tables(
            context,
            days_to_expire=DAYS_TO_EXPIRE,
            days_from_today=0,
            days_need=total_days if total_days else TOTAL_DAYS,
            log=log
        )

        self.output = YtTableResource(
            self._yt.yt_stuff, ypath_join(price_history_dir, yyyymmdd), load=True)


def _get_timestamp(time):
    import calendar
    dt = datetime.datetime.strptime(
        '{}_{}'.format(price_history.get_yyyymmdd(), time), price_history.GENLOG_DT_FORMAT)
    return int(calendar.timegm(dt.utctimetuple()))


def _get_yyyy_mm_dd(days_shift=0):
    YT_DT_FORMAT = '%Y-%m-%d'
    return (datetime.datetime.now() + datetime.timedelta(days=days_shift)).strftime(YT_DT_FORMAT)


def _get_price(price, precision=7):
    """ Get price as fixed point value """
    return int(price * (10**precision))


def _get_price_string(price, currency='RUR'):
    """ Get price formatted as string: currency + 7 digit fixed point value """
    return '%s %s' % (currency, _get_price(price))


def _get_pickup_options(pickup_costs):
    options = []
    for pickup_cost in pickup_costs:
        option = {}
        option['Cost'] = pickup_cost
        options.append(option)
    return json.dumps(options)


def _get_delivery_options(delivery_costs, region_id):
    result = []
    elem = {}
    result.append(elem)

    elem['RegionId'] = region_id
    options = []
    for delivery_cost in delivery_costs:
        option = {}
        option['price'] = int(delivery_cost * (10**7))
        options.append(option)
    elem['DeliveryOptions'] = options

    return json.dumps(result)


@pytest.fixture(
    scope='module',
    params=[
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'oldprice': _get_price_string(2000),
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1001',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                })
            ],
        },
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1002',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                })
            ],
        },
        # {
        #     'genlog_data': [
        #         create_offer({
        #             'price': _get_price_string(1000),
        #             'oldprice': _get_price_string(2000),
        #         }),
        #         create_offer({
        #             'offer_id': 'offer5',
        #             'price': _get_price_string(1000),
        #             'oldprice': _get_price_string(2000),
        #         }),
        #     ],
        #     'genlog_name': price_history.get_yyyymmdd() + '_1003',
        #     'sessions': [
        #         {'timestamp': _get_timestamp('0950')},
        #         {'timestamp': _get_timestamp('1010')},
        #     ],
        #     'offer_filter': [
        #         {'feed_id': 4, 'offer_id': 'offer5', 'timestamp': _get_timestamp('0950')},
        #         {'feed_id': 4, 'offer_id': 'offer5', 'timestamp': _get_timestamp('1010')},
        #     ],
        #     'expected': [
        #         create_offer({
        #             'currency': 'RUR',
        #             'price_min': _get_price(1000),
        #             'price_average': _get_price(1000),
        #             'price_max': _get_price(1000),
        #             'oldprice_min': 226616726394,
        #             'oldprice_average': 226616726394,
        #             'oldprice_max': 226616726394,
        #         }),
        #     ],
        # },
        {
            'price_history_data': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                })
            ],
            'price_history_name': price_history.get_yyyymmdd(-1),
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                })
            ],
            'total_days': 1,
        },
        {
            'price_history_data': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                })
            ],
            'price_history_name': price_history.get_yyyymmdd(-3),
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                })
            ],
            'total_days': 3,
        },

        # pickup_cheapest
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'pickup_options': _get_pickup_options([100, 150]),
                    'delivery_options': _get_delivery_options([200, 500], 1),
                    'priority_regions': '1'
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1004',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': _get_price(100),
                    'delivery_price_average': _get_price(100),
                    'delivery_price_max': _get_price(100),
                })
            ],
        },

        # no_pickup
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'mbi_delivery_options': _get_delivery_options([200, 500], 1),
                    'priority_regions': '1'
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1005',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': _get_price(200),
                    'delivery_price_average': _get_price(200),
                    'delivery_price_max': _get_price(200),
                })
            ],
        },

        # delivery_cheapest
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'pickup_options': _get_pickup_options([100, 150]),
                    'delivery_options': _get_delivery_options([50, 500], 1),
                    'priority_regions': '1'
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1006',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': _get_price(50),
                    'delivery_price_average': _get_price(50),
                    'delivery_price_max': _get_price(50),
                })
            ],
        },

        # no_delivery
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'pickup_options': _get_pickup_options([100, 150]),
                    'priority_regions': '1'
                })
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1007',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': _get_price(100),
                    'delivery_price_average': _get_price(100),
                    'delivery_price_max': _get_price(100),
                })
            ],
        },
    ],
    ids=[
        'smoke',
        'no_oldprice',
        # 'offer_filter',
        'no_genlog_for_today',
        'no_genlog_for_three_previous_days',
        'pickup_cheapest',
        'no_pickup',
        'delivery_cheapest',
        'no_delivery',
    ]
)
def calc_daily_prices_data(request):
    return request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'oldprice': _get_price_string(2000),
                }, blue=True)
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1004',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True)
            ],
        },
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                }, blue=True)
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1005',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': None,
                    'oldprice_average': None,
                    'oldprice_max': None,
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True)
            ],
        },
        {
            'genlog_data': [
                create_offer({
                    'price': _get_price_string(1000),
                    'oldprice': _get_price_string(2000),
                }, blue=True),
                create_offer({
                    'offer_id': 'offer5',
                    'market_sku': 10202,
                    'price': _get_price_string(1000),
                    'oldprice': _get_price_string(2000),
                }, blue=True),
            ],
            'genlog_name': price_history.get_yyyymmdd() + '_1006',
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True),
                create_offer({
                    'offer_id': 'offer5',
                    'market_sku': 10202,
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True),
            ]
        },
        {
            'price_history_data': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                }, blue=True)
            ],
            'price_history_name': price_history.get_yyyymmdd(-1),
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True)
            ],
            'total_days': 1,
        },
        {
            'price_history_data': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                }, blue=True)
            ],
            'price_history_name': price_history.get_yyyymmdd(-3),
            'expected': [
                create_offer({
                    'currency': 'RUR',
                    'price_min': _get_price(1000),
                    'price_average': _get_price(1000),
                    'price_max': _get_price(1000),
                    'oldprice_min': _get_price(2000),
                    'oldprice_average': _get_price(2000),
                    'oldprice_max': _get_price(2000),
                    'delivery_price_min': None,
                    'delivery_price_average': None,
                    'delivery_price_max': None,
                }, blue=True)
            ],
            'total_days': 3,
        },
    ],
    ids=[
        'smoke',
        'no_oldprice',
        'offer_filter',
        'no_genlog_for_today',
        'no_genlog_for_three_previous_days'
    ]
)
def calc_blue_daily_prices_data(request):
    return request.param


def _daily_prices_genlog_table_schema():
    return [
        dict(type='uint64', name='feed_id'),
        dict(type='string', name='offer_id'),
        dict(type='string', name='price'),
        dict(type='string', name='oldprice'),
        dict(type='string', name='ware_md5'),
        dict(type='string', name='classifier_magic_id'),
        dict(type='uint64', name='model_id'),
        dict(type='uint64', name='cluster_id'),
        dict(type='string', name='title'),
        dict(type='uint64', name='category_id'),
        dict(type='string', name='pickup_options'),
        dict(type='string', name='priority_regions'),
        dict(type='string', name='delivery_options'),
        dict(type='string', name='mbi_delivery_options'),
    ]


def _blue_daily_prices_genlog_table_schema():
    schema = _daily_prices_genlog_table_schema()
    schema.append(dict(type='uint64', name='market_sku'))
    return schema


@pytest.fixture(scope='module')
def calc_daily_prices_genlog_table(yt_server, calc_daily_prices_data):
    if 'genlog_data' not in calc_daily_prices_data:
        return None

    table_path = ypath_join(get_yt_prefix(), 'genlog', calc_daily_prices_data['genlog_name'])
    schema = _daily_prices_genlog_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_daily_prices_data['genlog_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def calc_blue_daily_prices_genlog_table(yt_server, calc_blue_daily_prices_data):
    if 'genlog_data' not in calc_blue_daily_prices_data:
        return None

    table_path = ypath_join(get_yt_prefix(), 'genlog', 'blue', calc_blue_daily_prices_data['genlog_name'])
    schema = _blue_daily_prices_genlog_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_blue_daily_prices_data['genlog_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def calc_start_price_history_table(yt_server, calc_daily_prices_data):
    if 'price_history_data' not in calc_daily_prices_data:
        return None

    table_path = ypath_join('//tmp', 'price_history', str(uuid.uuid4()), calc_daily_prices_data['price_history_name'])
    schema = price_history._daily_prices_schema(blue_history=False)

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_daily_prices_data['price_history_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def calc_start_blue_price_history_table(yt_server, calc_blue_daily_prices_data):
    if 'price_history_data' not in calc_blue_daily_prices_data:
        return None

    table_path = ypath_join('//tmp', 'price_history', 'blue', str(uuid.uuid4()), calc_blue_daily_prices_data['price_history_name'])
    schema = price_history._daily_prices_schema(blue_history=True)

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_blue_daily_prices_data['price_history_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def sessions_table(yt_server, calc_daily_prices_data):  # data is used to generate new path for each test
    if 'sessions' not in calc_daily_prices_data:
        return None
    sessions_path = ypath_join(get_yt_prefix(), 'offer-filter', 'stream', 'sessions', _get_yyyy_mm_dd())

    schema = [dict(name="timestamp", type="uint64")]

    return YtTableResource(yt_stuff=yt_server, path=sessions_path, data=calc_daily_prices_data['sessions'], attributes={'schema': schema, 'dynamic': True})


@pytest.fixture(scope='module')
def filter_table(yt_server, calc_daily_prices_data):
    if 'offer_filter' not in calc_daily_prices_data:
        return None
    filter_path = ypath_join(get_yt_prefix(), 'offer-filter', 'stream', _get_yyyy_mm_dd())
    schema = [
        dict(name="feed_id", type="uint64"),
        dict(name="offer_id", type="string"),
        dict(name="timestamp", type="uint64"),
    ]

    return YtTableResource(yt_stuff=yt_server, path=filter_path, data=calc_daily_prices_data['offer_filter'], attributes={'schema': schema, 'dynamic': True})


@pytest.yield_fixture(scope='module')
def calc_daily_prices_workflow(yt_server, calc_daily_prices_data, calc_daily_prices_genlog_table, calc_start_price_history_table, sessions_table, filter_table):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }

    if 'genlog_data' in calc_daily_prices_data:
        resources.update({'genlog': calc_daily_prices_genlog_table})

    if 'price_history_data' in calc_daily_prices_data:
        resources.update({'price_history': calc_start_price_history_table})

    total_days = None
    if 'total_days' in calc_daily_prices_data:
        total_days = calc_daily_prices_data['total_days']

    if sessions_table and filter_table:
        resources.update({'sessions': sessions_table})
        resources.update({'offer_filter': filter_table})

    with CalcDailyPricesTestEnv(**resources) as test_env:
        test_env.execute(total_days=total_days)
        yield test_env


def test_calc_daily_prices(calc_daily_prices_workflow, calc_daily_prices_data):
    assert(calc_daily_prices_workflow.output.data == calc_daily_prices_data['expected'])


@pytest.yield_fixture(scope='module')
def calc_daily_blue_prices_workflow(yt_server, calc_blue_daily_prices_data, calc_blue_daily_prices_genlog_table, calc_start_blue_price_history_table):
    resources = {
        'yt': YtResource(yt_stuff=yt_server),
    }

    if 'genlog_data' in calc_blue_daily_prices_data:
        resources.update({'genlog': calc_blue_daily_prices_genlog_table})

    if 'price_history_data' in calc_blue_daily_prices_data:
        resources.update({'price_history': calc_start_blue_price_history_table})

    total_days = None
    if 'total_days' in calc_blue_daily_prices_data:
        total_days = calc_blue_daily_prices_data['total_days']

    with CalcDailyPricesTestEnv(**resources) as test_env:
        test_env.execute(blue_history=True, total_days=total_days)
        yield test_env


def test_blue_calc_daily_prices(calc_daily_blue_prices_workflow, calc_blue_daily_prices_data):
    assert(calc_daily_blue_prices_workflow.output.data == calc_blue_daily_prices_data['expected'])
