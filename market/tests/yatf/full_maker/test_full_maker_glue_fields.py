# coding=utf-8
"""
Проверяем что работу клеевого конвертера в full-maker
"""

import pytest
from datetime import datetime

from hamcrest import (
    assert_that,
    has_items,
    has_entries,
    only_contains,
)

from yt.wrapper import ypath_join

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampOutOffersTable,
    DataCampOutStatsTable,
    DataCampPartnersTable,
)

from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.glue_config import GlueConfig

from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable


BUSINESS_ID = 1
FEED_ID = 1
SHOP_ID = 1
SUPPLIER_ID=1
OFFER_ID = 'offer_id'
MARKET_SKU = 1
WAREHOUSE_ID = 1
WARE_MD5='101010101010101010101'

GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'


def make_mbi_params(shop_id, feed_id):
    params = {
        'business_id': str(BUSINESS_ID),
        'shop_id': str(shop_id),
        'datafeed_id': str(feed_id),
        'is_enabled': 'true',
        'tariff': 'CLICKS',
        'blue_status': 'REAL',
        'is_push_partner': 'true',
        'supplier_type': '3',
        'regions': '213;',
    }

    mbi_result = ''
    for k, v in params.iteritems():
        mbi_result += k + '\t' + v + '\n'
    mbi_params = [mbi_result]
    return '\n\n'.join(mbi_params)


@pytest.fixture(scope='module')
def or3_config_data(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir
        },
        'datacamp': {
            'indexation_enabled': 'true',
            'partners_path': ypath_join(home_dir, 'datacamp/direct/partners'),
            'united_offers_tablepath': ypath_join(home_dir, 'datacamp/united/white_out/recent'),
            'united_stats_tablepath': ypath_join(home_dir, 'datacamp/united/white_out/stats/recent'),
        },
        'feeds': {
            'status_set': "'mock', 'publish'",
        },
        'fullmaker': {
            'fill_genlog_in_fullmaker': 'true'
        },
        'glue': {
            'use_full_maker': 'true'
        }
    }
    return config


@pytest.fixture(scope='module')
def datacamp_partners_table(yt_server, or3_config_data):
    data = [
        {
            'shop_id': SHOP_ID,
            'status': 'publish',
            'mbi': make_mbi_params(SHOP_ID, FEED_ID),
            'partner_stat': PartnerStat(
                offers_count=1,
            ).SerializeToString()
        }
    ]
    return DataCampPartnersTable(
        yt_server,
        or3_config_data['datacamp']['partners_path'],
        data
    )


@pytest.fixture(scope='module')
def datacamp_offers_table(yt_server, or3_config_data):
    data = [
        dict(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID,
            offer_id='offer_id_1',
            offer=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    shop_id=SHOP_ID,
                    business_id=BUSINESS_ID,
                    offer_id='offer_id_1',
                    extra=DTC.OfferExtraIdentifiers(
                        recent_feed_id=FEED_ID,
                        recent_warehouse_id=WAREHOUSE_ID,
                        shop_sku='offer_id_1',
                        ware_md5='101010101010101010101',
                    )
                ),
                content=DTC.OfferContent(
                    binding=DTC.ContentBinding(
                        approved=DTC.Mapping(
                            market_sku_id=MARKET_SKU,
                        ),
                    ),
                    market=DTC.MarketContent(
                        category_id=12704139,
                        market_category="Туалеты и аксессуары для кошек",
                        market_sku_published_on_blue_market=False,
                        market_sku_published_on_market=False,
                        product_name="",
                        vendor_id=12694697
                    ),
                    partner=DTC.PartnerContent(
                        actual=DTC.ProcessedSpecification(
                            description=DTC.StringValue(
                                value="Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик."
                            ),
                            name=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            title=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            url=DTC.StringValue(
                                value="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik"
                            ),
                            category=DTC.PartnerCategory(
                                id=12704139,
                                path_category_ids="2\\12704139",
                                path_category_names="Все товары\\Туалеты и аксессуары для кошек",
                                parent_id=90401,
                                name="Туалеты и аксессуары для кошек",
                            ),
                        ),
                        original=DTC.OriginalSpecification(
                            description=DTC.StringValue(
                                value="Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик."
                            ),
                            name=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            url=DTC.StringValue(
                                value="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik"
                            ),
                            category=DTC.PartnerCategory(
                                id=12704139,
                                path_category_ids="2\\12704139",
                                path_category_names="Все товары\\Туалеты и аксессуары для кошек",
                                parent_id=90401,
                                name="Туалеты и аксессуары для кошек",
                            ),
                        )
                    )
                ),
                partner_info=DTC.PartnerInfo(
                    fulfillment_feed_id=FEED_ID,
                    supplier_id=SHOP_ID,
                ),
               meta=create_meta(0, color=DTC.BLUE),
            ).SerializeToString(),
        ),
        dict(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID,
            offer_id='offer_id_2',
            offer=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    shop_id=SHOP_ID,
                    business_id=BUSINESS_ID,
                    offer_id='offer_id_2',
                    extra=DTC.OfferExtraIdentifiers(
                        recent_feed_id=FEED_ID,
                        recent_warehouse_id=WAREHOUSE_ID,
                        shop_sku='offer_id_2',
                        ware_md5='202020202020202020202',
                    )
                ),
                content=DTC.OfferContent(
                    binding=DTC.ContentBinding(
                        approved=DTC.Mapping(
                            market_sku_id=MARKET_SKU,
                        ),
                    ),
                    market=DTC.MarketContent(
                        category_id=12704139,
                        market_category="Туалеты и аксессуары для кошек",
                        market_sku_published_on_blue_market=False,
                        market_sku_published_on_market=False,
                        product_name="",
                        vendor_id=12694696
                    ),
                    partner=DTC.PartnerContent(
                        original=DTC.OriginalSpecification(
                            description=DTC.StringValue(
                                value="Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик."
                            ),
                            name=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            url=DTC.StringValue(
                                value="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik"
                            ),
                            category=DTC.PartnerCategory(
                                id=12704139,
                                path_category_ids="2\\12704139",
                                path_category_names="Все товары\\Туалеты и аксессуары для кошек",
                                parent_id=90401,
                                name="Туалеты и аксессуары для кошек",
                            ),
                        )
                    )
                ),
                partner_info=DTC.PartnerInfo(
                    fulfillment_feed_id=FEED_ID,
                    supplier_id=SHOP_ID,
                ),
               meta=create_meta(0, color=DTC.BLUE),
            ).SerializeToString(),
        ),
    ]
    return DataCampOutOffersTable(
        yt_server,
        or3_config_data['datacamp']['united_offers_tablepath'],
        data
    )


@pytest.fixture(scope='module')
def datacamp_stats_table(yt_server, or3_config_data):
    return DataCampOutStatsTable(
        yt_server,
        or3_config_data['datacamp']['united_stats_tablepath'],
        []
    )


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
    {'Fields': [
        {
            'glue_id': 0,
            'declared_cpp_type': 'UINT32',
            'target_name': 'some_business_id',
            'is_from_datacamp': True,
            'source_field_path': 'identifiers.business_id'
        },
        {
            'glue_id': 1,
            'declared_cpp_type': 'INT32',
            'target_name': 'vendor_id',
            'is_from_datacamp': True,
            'source_field_path': 'content.market.vendor_id'
        },
        {
            'glue_id': 11,
            'declared_cpp_type': 'UINT64',
            'target_name': 'actual_category_id',
            'is_from_datacamp': True,
            'source_field_path': 'content.partner.actual.category.id'
        },
        {
            'glue_id': 72,
            'declared_cpp_type': 'STRING',
            'target_name': 'path_category_ids',
            'is_from_datacamp': True,
            'source_field_path': 'content.partner.original.category.path_category_ids',
            'use_as_snippet': True
        },
        {
            "glue_id": 14,
            "declared_cpp_type": "INT64",
            "target_name": "amore_beru_vendor_data_timestamp",
            "is_from_datacamp": True,
            "source_field_path": "bids.amore_beru_vendor_data.meta.timestamp.seconds",
            "reduce_key_schema":  "FULL_OFFER_ID",
            "use_as_genlog_field": False,
            "use_as_named_field": True
        },
        {
            "glue_id": 15,
            "declared_cpp_type": "INT64",
            "target_name": "amore_data_timestamp",
            "is_from_datacamp": True,
            "source_field_path": "bids.amore_data.meta.timestamp.seconds",
            "reduce_key_schema":  "FULL_OFFER_ID",
            "use_as_genlog_field": False,
            "use_as_named_field": True
        }
    ]}, 'glue_config.json')


@pytest.fixture(scope='module')
def full_maker(
        yt_server,
        or3_config_data,
        datacamp_partners_table,
        datacamp_offers_table,
        datacamp_stats_table,
        glue_config
):
    resources = {
        'config': Or3Config(**or3_config_data),
        'shops_dat': ShopsDat([]),
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_data['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_data['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'datacamp_partners_table': datacamp_partners_table,
        'united_datacamp_offers_table': datacamp_offers_table,
        'united_datacamp_stats_table': datacamp_stats_table,
        'glue_config': glue_config
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def test_glue_fields_not_size(full_maker):
    assert_that(
        full_maker.offers_raw_corrected,
        has_items(
            has_entries({
                'offer_id': 'offer_id_1',
                'offer' : has_entries({
                    'business_id': BUSINESS_ID,
                    'glue_fields': only_contains(
                        {'uint32_value': 1, 'glue_id': 0},
                        {'int32_value': 12694697, 'glue_id': 1},
                        {'uint64_value': '12704139', 'glue_id': 11},
                        {'string_value': '2\\12704139', 'glue_id': 72}
                    )
                })
            }),
            has_entries({
                'offer_id': 'offer_id_2',
                'offer' : has_entries({
                    'business_id': BUSINESS_ID,
                    'glue_fields': only_contains(
                        {'uint32_value': 1, 'glue_id': 0},
                        {'int32_value': 12694696, 'glue_id': 1},
                        {'string_value': '2\\12704139', 'glue_id': 72}
                    )
                })
            })
        )
    )
