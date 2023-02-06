# coding=utf-8
"""
Актуально для Товарной Вертикали
https://st.yandex-team.ru/GOODS-2468
При использовании флага use_goods_sm_mapping=true должен использоваться ску от смартматчера, если нет других
"""

import pytest
from datetime import datetime

from hamcrest import (
    assert_that,
    has_items,
    has_entries,
)

from yt.wrapper import ypath_join

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampOutOffersTable,
    DataCampOutStatsTable,
    DataCampPartnersTable,
)

from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC, OfferMeta_pb2 as OM
from market.idx.datacamp.yatf.utils import create_meta
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable


BUSINESS_ID = 1
FEED_ID = 1
SHOP_ID = 1
SUPPLIER_ID=1
MARKET_SKU = 1
SM_SKU = 7
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
            'fill_genlog_in_fullmaker': 'true',
            'use_goods_sm_mapping': 'true'
        },
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
            offer_id='no_market_sku',
            offer=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    shop_id=SHOP_ID,
                    business_id=BUSINESS_ID,
                    offer_id='no_market_sku',
                    extra=DTC.OfferExtraIdentifiers(
                        recent_feed_id=FEED_ID,
                        recent_warehouse_id=WAREHOUSE_ID,
                        shop_sku='offer_id_1',
                        ware_md5='101010101010101010101',
                    )
                ),
                content=DTC.OfferContent(
                    binding=DTC.ContentBinding(
                        goods_sm_mapping=DTC.SMMapping(
                            mapping=DTC.Mapping(
                                market_sku_id=SM_SKU,
                                market_category_id=222,
                                market_model_id=333,
                            ),
                            vendor_id=OM.Ui32Value(
                                value=777,
                            )
                        )
                    ),
                    market=DTC.MarketContent(
                        category_id=12704139,
                        market_category="Туалеты и аксессуары для кошек",
                        market_sku_published_on_blue_market=False,
                        market_sku_published_on_market=False,
                        product_name="",
                        vendor_id=12694697
                    ),
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
            offer_id='has_market_sku',
            offer=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    shop_id=SHOP_ID,
                    business_id=BUSINESS_ID,
                    offer_id='has_market_sku',
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
                        vendor_id=12694696,
                    ),
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
def full_maker(
        yt_server,
        or3_config_data,
        datacamp_partners_table,
        datacamp_offers_table,
        datacamp_stats_table,
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
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def test_use_goods_sm_flag(full_maker):
    assert_that(
        full_maker.offers_raw_corrected,
        has_items(
            has_entries({
                'offer_id': 'no_market_sku',
                'offer' : has_entries({
                    'business_id': BUSINESS_ID,
                    'market_sku': str(SM_SKU),
                    'sku_source': 'EXTERNAL',
                    'model_id': 333,
                    'category_id': 222,
                    'vendor_id': '777'
                }),
            }),
            has_entries({
                'offer_id': 'has_market_sku',
                'offer' : has_entries({
                    'business_id': BUSINESS_ID,
                    'market_sku': str(MARKET_SKU),
                    'sku_source': 'DEFAULT',
                })
            })
        )
    )
