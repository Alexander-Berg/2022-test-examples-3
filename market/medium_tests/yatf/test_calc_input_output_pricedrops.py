#!/usr/bin/env python
# -*- coding: utf-8 -*-

import pytest
import uuid
import md5
import datetime

from market.idx.yatf.test_envs.yql_env import BaseEnv
from market.idx.yatf.resources.yql_resource import YtResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper import ypath_join

import market.idx.pylibrary.mindexer_core.price_history.price_history as price_history


DATE = price_history.get_yyyymmdd()
CLICK_DATE = (datetime.date.today() - datetime.timedelta(days=1)).strftime(price_history.DAY_CLICK_FORMAT)

DEFAULT_OFFER_FIELDS = {
    'feed_id': 4,
    'offer_id': 'offer_test',
    'ware_md5': 'kOijrFDJE5Z8xj1nHxMi8g',
    'title': 'test title',
    'market_sku': None,
    'is_blue_offer': False,
    'cluster_id': 12345,
}


DEFAULT_CLICK_DATA = {
    'feed_id': '4',
    'offer_id': '10000',
    'msku': None,
    'ware_md5': 'kOijrFDJE5Z8xj1nHxMi8g',
    'supplier_id': None,
    'click_price': 1000,
}


DEFAULT_HISTORY_DATA = {
    'feed_id': 4,
    'offer_id': '10000',
    'history_price': 1000
}


DEFAULT_BLUE_HISTORY_DATA = {
    'msku': 10201,
    'history_price': 1000
}


def create_offer(data, blue=False):
    import copy
    result = copy.deepcopy(DEFAULT_OFFER_FIELDS)
    if blue:
        result.update({'market_sku': 10201, 'is_blue_offer': True})
    result.update(data)
    return result


def create_click(data, blue=False):
    import copy
    result = copy.deepcopy(DEFAULT_CLICK_DATA)
    if blue:
        result.update({'msku': 10201, 'supplier_id': 12345})
    result.update(data)
    return result


def create_history(data):
    import copy
    result = copy.deepcopy(DEFAULT_HISTORY_DATA)
    result.update(data)
    return result


def create_blue_history(data):
    import copy
    result = copy.deepcopy(DEFAULT_BLUE_HISTORY_DATA)
    result.update(data)
    return result


class PrepareInputDataForPriceDropsTestEnv(BaseEnv):
    def __init__(self, **kwargs):
        try:
            self._yt = kwargs.pop('yt')  # prevents deepcopying Yt stuff
        except KeyError:
            self._yt = YtResource()

        self._genlog = kwargs.get('genlog', None)
        self._clicks = kwargs.get('clicks', None)
        self._history_prices = kwargs.get('history_prices', None)
        self._blue_history_prices = kwargs.get('blue_history_prices', None)
        self.output = None

        dst_path = ypath_join('//tmp', str(uuid.uuid4()))

        self.paths = price_history.PrepareTableForPriceDropsPaths(
            yt_genlog_path=self._genlog.get_path() if self._genlog else None,
            yt_genlog_dst_path="{}/genlog".format(dst_path),
            yt_clicks_path=self._clicks.get_path().rsplit('/', 1)[0] if self._clicks else None,
            yt_clicks_dst_path="{}/clicks".format(dst_path),
            yt_price_history_path=self._history_prices.get_path() if self._history_prices else None,
            yt_blue_price_history_path=self._blue_history_prices.get_path() if self._blue_history_prices else None,
            yt_price_history_dst_path="{}/hprices".format(dst_path),
            yt_price_history_blue_dst_path="{}/blue/hprices".format(dst_path)
        )

        super(PrepareInputDataForPriceDropsTestEnv, self).__init__(**kwargs)

    def execute(self):
        pass


"""Test environment for genlog preparation for pricedrops algorithm."""


class PrepareGenlogForPriceDropsTestEnv(PrepareInputDataForPriceDropsTestEnv):
    def __init__(self, **kwargs):
        super(PrepareGenlogForPriceDropsTestEnv, self).__init__(**kwargs)

    def execute(self):
        import logging

        context = price_history.PriceHistoryContext(
            yt_client=self._yt.yt_stuff.get_yt_client(),
        )
        log = logging.getLogger()

        price_history._prepare_genlog_for_pricedrops(
            context,
            self.paths.genlog_path,
            self.paths.genlog_dst_path,
            log
        )

        self.output = YtTableResource(
            self._yt.yt_stuff, ypath_join(self.paths.genlog_dst_path), load=True)


"""Test environment for clicks preparation for pricedrops algorithm."""


class PrepareClicksForPriceDropsTestEnv(PrepareInputDataForPriceDropsTestEnv):
    def __init__(self, **kwargs):
        super(PrepareClicksForPriceDropsTestEnv, self).__init__(**kwargs)

    def execute(self):
        import logging

        context = price_history.PriceHistoryContext(
            yt_client=self._yt.yt_stuff.get_yt_client(),
        )
        log = logging.getLogger()

        price_history._prepare_clicks_for_pricedrops(
            context,
            self.paths.clicks_path,
            self.paths.clicks_dst_path,
            log
        )

        self.output = YtTableResource(
            self._yt.yt_stuff, ypath_join(self.paths.clicks_dst_path, CLICK_DATE), load=True)


"""Test environment for hprices preparation for pricedrops algorithm."""


class PrepareHistoryForPriceDropsTestEnv(PrepareInputDataForPriceDropsTestEnv):
    def __init__(self, **kwargs):
        super(PrepareHistoryForPriceDropsTestEnv, self).__init__(**kwargs)

    def execute(self):
        import logging

        context = price_history.PriceHistoryContext(
            yt_client=self._yt.yt_stuff.get_yt_client(),
        )
        log = logging.getLogger()

        price_history._prepare_history_for_pricedrops(
            context,
            self.paths.price_history_path,
            self.paths.blue_price_history_path,
            self.paths.price_history_dst_path,
            self.paths.price_history_blue_dst_path,
            log
        )

        self.output = YtTableResource(
            self._yt.yt_stuff, ypath_join(self.paths.price_history_dst_path), load=True)


class FinalizeDataAfterPriceDropsTestEnv(BaseEnv):
    def __init__(self, **kwargs):
        try:
            self._yt = kwargs.pop('yt')  # prevents deepcopying Yt stuff
        except KeyError:
            self._yt = YtResource()

        self._pricedrops = kwargs.get('pricedrops', None)
        self.output = None
        self.blue_output = None

        dst_path = ypath_join('//tmp', str(uuid.uuid4()))
        self.paths = price_history.PrepareFinalParameters(
            yt_pricedrops_result_table=self._pricedrops.get_path(),
            yt_pricedrops_table="{}/hprices_pricedrops".format(dst_path),
            yt_blue_pricedrops_table="{}/blue/hprices_pricedrops".format(dst_path)
        )

        super(FinalizeDataAfterPriceDropsTestEnv, self).__init__(**kwargs)

    def execute(self):
        import logging

        context = price_history.PriceHistoryContext(
            yt_client=self._yt.yt_stuff.get_yt_client(),
        )
        log = logging.getLogger()

        price_history._finalize_output_tables_for_pricedrops(context,
                                                             self.paths.pricedrops_result_path,
                                                             self.paths.pricedrops_final_path,
                                                             self.paths.pricedrops_blue_final_path,
                                                             log,
                                                             has_yql_proto_field_offer_attr=True)

        self.output = YtTableResource(
            self._yt.yt_stuff, ypath_join(self.paths.pricedrops_final_path), load=True)
        self.blue_output = YtTableResource(
            self._yt.yt_stuff, ypath_join(self.paths.pricedrops_blue_final_path), load=True)


@pytest.fixture(
    scope='module',
    params=[
        {
            'genlog_data': [
                create_offer({}),  # no changes for white offer
            ],
            'genlog_name': DATE,
            'expected': [
                create_offer({}),
            ],
        },
        {
            'genlog_data': [
                create_offer({'market_sku': 10201}, blue=True),  # for blue offer should replace offer_id = market_sku, feed_id = 475690 and ware_md5 = md5(market_sku)
            ],
            'genlog_name': DATE,
            'expected': [
                create_offer({
                    'market_sku': 10201,
                    'offer_id': '10201',
                    'feed_id': 475690,
                    'ware_md5': md5.md5('10201').hexdigest()

                }, blue=True)
            ],
        },
    ]
)
def calc_genlog_table_data(request):
    return request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'clicks_data': [
                create_click({}),  # no changes for white offer
            ],
            'clicks_name': CLICK_DATE,
            'expected': [
                create_click({}),
            ],
        },
        {
            'clicks_data': [
                create_click({'msku': 10201, 'supplier_id': 12345}, blue=True),  # for blue click should replace offer_id = market_sku, feed_id = 475690 and ware_md5 = md5(market_sku)
            ],
            'clicks_name': CLICK_DATE,
            'expected': [
                create_click({
                    'msku': 10201,
                    'offer_id': '10201',
                    'feed_id': '475690',
                    'ware_md5': md5.md5('10201').hexdigest(),
                    'supplier_id': 12345

                }, blue=True)
            ],
        },
    ]
)
def calc_clicks_table_data(request):
    return request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'hprices_data': [
                create_history({}),  # no changes for white offer
            ],
            'hprices_name': DATE,
            'expected': [
                create_history({}),
            ]
        },
    ]
)
def calc_hprices_table_data(request):
    return request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'hprices_data': [
                create_blue_history({'msku': 10101}),  # output should be sorted
                create_blue_history({'msku': 10201}),  # for blue hprices should delete msku field, but add offer_id = msku, feed_id = 475690
            ],
            'hprices_name': DATE,
            'expected': [
                create_history({
                    'offer_id': '10101',
                    'feed_id': 475690,
                }),
                create_history({
                    'offer_id': '10201',
                    'feed_id': 475690,
                }),
            ]
        },
    ]
)
def calc_blue_hprices_table_data(request):
    return request.param


@pytest.fixture(
    scope='module',
    params=[
        {
            'pricedrops_data': [
                create_history({}),  # no changes for white offer
                create_history({  # pricedrops for blue offers should be transformed in correct form: msku = offer_id (offer_id and feed_id should be deleted) and sorted by msku
                    'offer_id': '10203',
                    'feed_id': 475690,
                }),
                create_history({  # pricedrops for blue offers should be transformed in correct form: msku = offer_id (offer_id and feed_id should be deleted) and sorted by msku
                    'offer_id': '10201',
                    'feed_id': 475690,
                })
            ],
            'pricedrops_name': DATE,
            'expected_pricedrops': [create_history({})],
            'expected_blue_pricedrops': [
                create_blue_history({'msku': 10201}),
                create_blue_history({'msku': 10203})
            ]
        },
    ]
)
def calc_pricedrops_table_data(request):
    return request.param


def _daily_genlog_table_schema():
    return [
        dict(type='uint64', name='feed_id'),
        dict(type='string', name='offer_id'),
        dict(type='string', name='ware_md5'),
        dict(type='string', name='title'),
        dict(type='boolean', name='is_blue_offer'),
        dict(type='uint64', name='market_sku'),
        dict(type='uint64', name='cluster_id'),
    ]


def _daily_clicks_table_schema():
    return [
        dict(type='string', name='feed_id'),
        dict(type='string', name='offer_id'),
        dict(type='string', name='ware_md5'),
        dict(type='uint64', name='click_price'),
        dict(type='uint64', name='supplier_id'),
        dict(type='uint64', name='msku'),
    ]


def _daily_history_table_schema():
    return [
        dict(type='uint64', name='feed_id', sort_order='ascending'),
        dict(type='string', name='offer_id', sort_order='ascending'),
        dict(type='int64', name='history_price'),
    ]


def _daily_pricedrops_table_schema():
    return [
        dict(type='uint64', name='feed_id'),
        dict(type='string', name='offer_id'),
        dict(type='int64', name='history_price'),
    ]


def _daily_blue_history_table_schema():
    return [
        dict(type='uint64', name='msku', sort_order='ascending'),
        dict(type='int64', name='history_price'),
    ]


@pytest.fixture(scope='module')
def calc_genlog_table(yt_server, calc_genlog_table_data):
    table_path = ypath_join(get_yt_prefix(), 'offers', calc_genlog_table_data['genlog_name'])
    schema = _daily_genlog_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_genlog_table_data['genlog_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def calc_clicks_table(yt_server, calc_clicks_table_data):
    table_path = ypath_join(get_yt_prefix(), 'clicks', calc_clicks_table_data['clicks_name'])
    schema = _daily_clicks_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_clicks_table_data['clicks_data'],
        attributes={'schema': schema}
    )


@pytest.fixture(scope='module')
def calc_hprices_table(yt_server, calc_hprices_table_data):
    table_path = ypath_join(get_yt_prefix(), 'hprices', calc_hprices_table_data['hprices_name'])
    schema = _daily_history_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_hprices_table_data['hprices_data'],
        attributes={
            'schema': schema,
            '_yql_proto_field_offer': 'test_yql_proto_field_offer',
        },
    )


@pytest.fixture(scope='module')
def calc_blue_hprices_table(yt_server, calc_blue_hprices_table_data):
    table_path = ypath_join(get_yt_prefix(), 'blue_hprices', calc_blue_hprices_table_data['hprices_name'])
    schema = _daily_blue_history_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_blue_hprices_table_data['hprices_data'],
        attributes={'schema': schema},
    )


@pytest.fixture(scope='module')
def calc_pricedrops_table(yt_server, calc_pricedrops_table_data):
    table_path = ypath_join(get_yt_prefix(), 'pricedrops', calc_pricedrops_table_data['pricedrops_name'])
    schema = _daily_pricedrops_table_schema()

    return YtTableResource(
        yt_stuff=yt_server,
        path=table_path,
        data=calc_pricedrops_table_data['pricedrops_data'],
        attributes={
            'schema': schema,
            '_yql_proto_field_offer': 'test_yql_proto_field_offer',
        }
    )


@pytest.yield_fixture(scope='module')
def prepare_genlog_for_pricedrops_workflow(yt_server, calc_genlog_table):

    resources = {
        'genlog': calc_genlog_table,
        'yt': YtResource(yt_stuff=yt_server),
    }

    with PrepareGenlogForPriceDropsTestEnv(**resources) as test_env:
        test_env.execute()
        yield test_env


@pytest.yield_fixture(scope='module')
def prepare_clicks_for_pricedrops_workflow(yt_server, calc_clicks_table):

    resources = {
        'clicks': calc_clicks_table,
        'yt': YtResource(yt_stuff=yt_server),
    }

    with PrepareClicksForPriceDropsTestEnv(**resources) as test_env:
        test_env.execute()
        yield test_env


@pytest.yield_fixture(scope='module')
def prepare_history_for_pricedrops_workflow(yt_server, calc_hprices_table, calc_blue_hprices_table):

    resources = {
        'history_prices': calc_hprices_table,
        'blue_history_prices': calc_blue_hprices_table,
        'yt': YtResource(yt_stuff=yt_server),
    }

    with PrepareHistoryForPriceDropsTestEnv(**resources) as test_env:
        test_env.execute()
        yield test_env


@pytest.yield_fixture(scope='module')
def finalize_data_after_pricedrops_workflow(yt_server, calc_pricedrops_table):

    resources = {
        'pricedrops': calc_pricedrops_table,
        'yt': YtResource(yt_stuff=yt_server),
    }

    with FinalizeDataAfterPriceDropsTestEnv(**resources) as test_env:
        test_env.execute()
        yield test_env


def test_prepare_genlog_for_pricedrops(prepare_genlog_for_pricedrops_workflow, calc_genlog_table_data):
    assert(prepare_genlog_for_pricedrops_workflow.output.data == calc_genlog_table_data['expected'])


def test_prepare_clicks_for_pricedrops(prepare_clicks_for_pricedrops_workflow, calc_clicks_table_data):
    assert(prepare_clicks_for_pricedrops_workflow.output.data == calc_clicks_table_data['expected'])


def test_prepare_history_for_pricedrops(prepare_history_for_pricedrops_workflow, calc_hprices_table_data, calc_blue_hprices_table_data):
    expected = calc_hprices_table_data['expected']
    expected += calc_blue_hprices_table_data['expected']
    assert(prepare_history_for_pricedrops_workflow.output.data == expected)


def test_finalize_data_after_pricedrops_workflow(finalize_data_after_pricedrops_workflow, calc_pricedrops_table_data):
    assert(finalize_data_after_pricedrops_workflow.output.data == calc_pricedrops_table_data['expected_pricedrops'])
    assert(finalize_data_after_pricedrops_workflow.blue_output.data == calc_pricedrops_table_data['expected_blue_pricedrops'])
