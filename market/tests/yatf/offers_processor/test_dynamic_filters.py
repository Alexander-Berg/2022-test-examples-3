#!/usr/bin/env python
# coding: utf-8

"""Проверяем работу DynamicFilterWorker'a офферс-процессора
DynamicFilterWorker получает на вход актуальные на момент индексации файлы динамиков и проставляет офферам,
которые были бы отфильтрованы этими динамиками в репорте, поле disabled_by_dynamic (не выкидывая из индекса)
Такие оффера хочется не учитывтаь в статистиках
"""

import pytest

from market.idx.offers.yatf.utils.fixtures import default_genlog, default_blue_genlog, default_shops_dat

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.filter_db import FilterDb
from market.idx.yatf.resources.market_sku_filters_pbuf import MarketSkuFiltersPbuf
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.proto.abo.BlueOfferFilter_pb2 import BlueOfferInfo, BlueOfferFilter
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that, equal_to


GOOD_CMAGIC_ID1 = '0315fea222b77e0aafe2768ee3fdd8d3'
FILTERED_CMAGIC_ID1 = '08919beecceda2fc1cc5063cca14fafd'
GOOD_CMAGIC_ID2 = '0e37baa7bc100800bcea83d9e9bbe496'
FILTERED_CMAGIC_ID2 = '13692b7b01377172a4e413c6a39d793f'

BLUE_SUPPLIER_ID_GOOD = 10271823
BLUE_SUPPLIER_ID_FILTERED = 10313976

BLUE_CROSSDOCK_SUPPLIER_ID_GOOD = 10464783
BLUE_CROSSDOCK_SUPPLIER_ID_FILTERED = 10595849

CPC_SHOP_ID_GOOD = 92702
CPC_SHOP_ID_FILTERED = 104520
CPA_SHOP_ID_GOOD = 314485
CPA_SHOP_ID_FILTERED = 216218
CPA_FEED_GOOD = 100400
CPA_FEED_FILTERED = 100500

MARKET_SKU_GOOD = 1
SUPPLIER_ID_GOOD = 10
SHOP_SKU_GOOD = '100'
MARKET_SKU_FILTERED1 = 2
SUPPLIER_ID_FILTERED1 = 20
SUPPLIER_ID_FILTERED2 = 30
SHOP_SKU_FILTERED2 = '300'

# supplier-filter.db
SUPPLIER_FILTER_FILTERED_BLUE = 'blue offer filtered by supplier filter'
SUPPLIER_FILTER_NOT_FILTERED_BLUE = 'blue offer not filtered by supplier filter'
SUPPLIER_FILTER_NOT_APPLIED_WHITE = 'white offer cannot be filtered by supplier filter'
# supplier-crossdock-filter.db
SUPPLIER_CROSSDOCK_FILTER_FILTERED_BLUE = 'blue offer filtered by supplier crossdock filter'
SUPPLIER_CROSSDOCK_FILTER_NOT_FILTERED_BLUE = 'blue offer not filtered by supplier crossdock filter'
SUPPLIER_CROSSDOCK_FILTER_NOT_APPLIED_WHITE = 'white offer cannot be filtered by supplier crossdock filter'
SUPPLIER_CROSSDOCK_FILTER_NOT_APPLIED_RED = 'red offer cannot be filtered by supplier crossdock filter'
# shop-cpc-filter.db + shop-cpa-filter.db
SHOP_FILTER_CPC_FILTERED_WHITE = 'white cpc offer filtered by shop filter'
SHOP_FILTER_CPC_NOT_FILTERED_WHITE = 'white cpc offer not filtered by shop filter'
SHOP_FILTER_CPA_FILTERED_WHITE = 'white cpa offer filtered by shop filter'
SHOP_FILTER_CPA_NOT_FILTERED_WHITE = 'white cpa offer not filtered by shop filter'
SHOP_FILTER_NOT_APPLIED_BLUE = 'blue offer cannot be filtered by shop filter'
# market-sku-filters.pbuf
MARKET_SKU_FILTER_NOT_FILTERED = 'blue offer not filtered by market-sku-filters'
MARKET_SKU_FILTER_FILTERED_BY_MSKU_AND_SUPPLIER = 'blue offer filtered by market-sku-filters by market_sku'
MARKET_SKU_FILTER_FILTERED_BY_SUPPLIER_AND_SHOP_SKU = 'blue offer filtered by market-sku-filters by supplier and shop sku'


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_blue_genlog(
            title=SUPPLIER_FILTER_NOT_FILTERED_BLUE,
            supplier_id=BLUE_SUPPLIER_ID_GOOD,
        ),
        default_blue_genlog(
            title=SUPPLIER_FILTER_FILTERED_BLUE,
            supplier_id=BLUE_SUPPLIER_ID_FILTERED
        ),
        default_genlog(
            title=SUPPLIER_FILTER_NOT_APPLIED_WHITE,
            supplier_id=BLUE_SUPPLIER_ID_FILTERED
        ),
        default_blue_genlog(
            title=SUPPLIER_CROSSDOCK_FILTER_NOT_FILTERED_BLUE,
            supplier_id=BLUE_CROSSDOCK_SUPPLIER_ID_GOOD,
        ),
        default_blue_genlog(
            title=SUPPLIER_CROSSDOCK_FILTER_FILTERED_BLUE,
            supplier_id=BLUE_CROSSDOCK_SUPPLIER_ID_FILTERED
        ),
        default_genlog(
            title=SUPPLIER_CROSSDOCK_FILTER_NOT_APPLIED_WHITE,
            supplier_id=BLUE_CROSSDOCK_SUPPLIER_ID_FILTERED
        ),
        default_genlog(
            title=SHOP_FILTER_CPC_NOT_FILTERED_WHITE,
            shop_id=CPC_SHOP_ID_GOOD,
        ),
        default_genlog(
            title=SHOP_FILTER_CPC_FILTERED_WHITE,
            shop_id=CPC_SHOP_ID_FILTERED
        ),
        default_genlog(
            feed_id=CPA_FEED_GOOD,
            title=SHOP_FILTER_CPA_NOT_FILTERED_WHITE,
            shop_id=CPA_SHOP_ID_GOOD
        ),
        default_genlog(
            feed_id=CPA_FEED_FILTERED,
            title=SHOP_FILTER_CPA_FILTERED_WHITE,
            shop_id=CPA_SHOP_ID_FILTERED
        ),
        default_blue_genlog(
            title=SHOP_FILTER_NOT_APPLIED_BLUE,
            shop_id=CPC_SHOP_ID_FILTERED,
        ),
        default_blue_genlog(
            title=MARKET_SKU_FILTER_NOT_FILTERED,
            market_sku=MARKET_SKU_GOOD,
            supplier_id=SUPPLIER_ID_GOOD,
            shop_sku=SHOP_SKU_GOOD,
        ),
        default_blue_genlog(
            title=MARKET_SKU_FILTER_FILTERED_BY_MSKU_AND_SUPPLIER,
            market_sku=MARKET_SKU_FILTERED1,
            supplier_id=SUPPLIER_ID_FILTERED1,
            shop_sku=SHOP_SKU_GOOD,
        ),
        default_blue_genlog(
            title=MARKET_SKU_FILTER_FILTERED_BY_SUPPLIER_AND_SHOP_SKU,
            market_sku=MARKET_SKU_GOOD,
            supplier_id=SUPPLIER_ID_FILTERED2,
            shop_sku=SHOP_SKU_FILTERED2,
        ),
    ]


@pytest.fixture(scope="module")
def shops_dat():
    cpa_shop = default_shops_dat()
    cpa_shop['datafeed_id'] = CPA_FEED_GOOD
    cpa_shop['shop_id'] = CPA_SHOP_ID_GOOD
    cpa_shop['name'] = 'cpa shop good'
    cpa_shop['cpa'] = 'REAL'
    cpa_shop['cpc'] = 'NO'

    cpa_shop2 = default_shops_dat()
    cpa_shop2['datafeed_id'] = CPA_FEED_FILTERED
    cpa_shop2['shop_id'] = CPA_SHOP_ID_FILTERED
    cpa_shop2['name'] = 'cpa shop filtered'
    cpa_shop2['cpa'] = 'REAL'
    cpa_shop2['cpc'] = 'NO'
    return ShopsDat(shops=[cpa_shop, cpa_shop2])


@pytest.fixture(scope="module")
def market_sku_filters():
    return [BlueOfferFilter(Infos=[
        BlueOfferInfo(
            MarketSku=MARKET_SKU_FILTERED1,
            SupplierId=SUPPLIER_ID_FILTERED1,
        ),
        BlueOfferInfo(
            SupplierId=SUPPLIER_ID_FILTERED2,
            ShopSku=SHOP_SKU_FILTERED2
        ),
    ])]


@pytest.fixture(scope="module")
def supplier_filter_db():
    filter = FilterDb(name='supplier-filter.db')
    filter.add(BLUE_SUPPLIER_ID_FILTERED)
    return filter


@pytest.fixture(scope="module")
def supplier_crossdock_filter_db():
    filter = FilterDb(name='supplier-crossdock-filter.db')
    filter.add(BLUE_CROSSDOCK_SUPPLIER_ID_FILTERED)
    return filter


@pytest.fixture(scope="module")
def shop_cpc_filter_db():
    filter = FilterDb(name='shop-cpc-filter.db')
    filter.add(CPC_SHOP_ID_FILTERED)
    return filter


@pytest.fixture(scope="module")
def shop_cpa_filter_db():
    filter = FilterDb(name='shop-cpa-filter.db')
    filter.add(CPA_SHOP_ID_FILTERED)
    return filter


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, supplier_filter_db, supplier_crossdock_filter_db, shop_cpc_filter_db, shop_cpa_filter_db, shops_dat, market_sku_filters):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'shops_utf8_dat': shops_dat,
        'supplier_filter_db': supplier_filter_db,
        'supplier_crossdock_filter_db': supplier_crossdock_filter_db,
        'shop_cpc_filter_db': shop_cpc_filter_db,
        'shop_cpa_filter_db': shop_cpa_filter_db,
        'market_sku_filters_pbuf': MarketSkuFiltersPbuf(market_sku_filters),
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.mark.parametrize(
    'param, expected',
    [
        (SUPPLIER_FILTER_FILTERED_BLUE, True),
        (SUPPLIER_FILTER_NOT_FILTERED_BLUE, False),
        (SUPPLIER_FILTER_NOT_APPLIED_WHITE, False),

        (SUPPLIER_CROSSDOCK_FILTER_FILTERED_BLUE, True),
        (SUPPLIER_CROSSDOCK_FILTER_NOT_FILTERED_BLUE, False),
        (SUPPLIER_CROSSDOCK_FILTER_NOT_APPLIED_WHITE, False),

        (SHOP_FILTER_CPC_FILTERED_WHITE, True),
        (SHOP_FILTER_CPC_NOT_FILTERED_WHITE, False),
        (SHOP_FILTER_CPA_FILTERED_WHITE, True),
        (SHOP_FILTER_CPA_NOT_FILTERED_WHITE, False),
        (SHOP_FILTER_NOT_APPLIED_BLUE, False),

        (MARKET_SKU_FILTER_NOT_FILTERED, False),
        (MARKET_SKU_FILTER_FILTERED_BY_MSKU_AND_SUPPLIER, True),
        (MARKET_SKU_FILTER_FILTERED_BY_SUPPLIER_AND_SHOP_SKU, True),
    ],
)
def test_dynamic_filter(workflow, param, expected):
    """Проверяем работу динамических фильтров - проставление в генлоге поля disabled_by_dynamic
    """
    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'title': param,
                'disabled_by_dynamic': expected,
            }
        )
    )


def test_genlog_has_only_one_record(workflow, genlog_rows):
    """Проверяем, что офферы, не выкидываются из генлогов и индекса динамическими фильтрами,
    а им просто проставляется поле disabled_by_dynamic
    """
    assert_that(len(workflow.genlog), equal_to(len(genlog_rows)))
