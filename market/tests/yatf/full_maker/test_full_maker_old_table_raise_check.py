# coding: utf-8
import pytest
from hamcrest import assert_that, calling, raises
from yatest.common.process import ExecutionError

from datetime import datetime

from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import create_meta

from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.or3offers_table import Or3OffersTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampOutOffersTable,
    DataCampOutStatsTable,
    DataCampPartnersTable,
)
from market.idx.yatf.resources.msku_table import MskuTable


NOW = datetime.now()
GENERATION_PS = NOW.strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
VIRTUAL_SHOP_ID = 431782


PARTNERS_DATA = [
    {
        'shop_id': 111,
        'status': 'publish',
        'shops_dat_info': [
            {
                'business_id': 1,
                'shop_id': 111,
                'feed_id': 12321,
                'warehouse_id': 149,
                'is_push_partner': 'true',
            },
        ]
    },
    {
        'shop_id': 222,
        'status': 'publish',
        'shops_dat_info': [
            {
                'business_id': 1,
                'disabled': True,
                'shop_id': 222,
                'feed_id': 45654,
                'warehouse_id': 149,
                'is_push_partner': 'true',
                'is_tested': 'true',
            },
        ]
    },
    {
        'shop_id': 333,
        'status': 'publish',
        'shops_dat_info': [
            {
                'business_id': 1,
                'shop_id': 333,
                'feed_id': 1069,
                'warehouse_id': 149,
                'is_push_partner': 'true',
            },
        ]
    },
]


@pytest.fixture(scope="module")
def datacamp_offers_table_data():
    result = []

    for shop in PARTNERS_DATA:
        for feed_data in shop['shops_dat_info']:
            result.append({
                'business_id': 1,
                'offer_id': 'push_offer_01',
                'shop_id': shop['shop_id'],
                'offer': DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        # 'supplemental_id': feed_data['warehouse_id'],
                        shop_id=shop['shop_id'],
                        business_id=1,
                        offer_id='push_offer_01',
                        extra=DTC.OfferExtraIdentifiers(
                            recent_feed_id=feed_data['feed_id'],
                            recent_warehouse_id=145,
                            shop_sku='push_offer_01',
                            ware_md5='101010101010101010101',
                        )
                    ),
                    content=DTC.OfferContent(
                        binding=DTC.ContentBinding(
                            approved=DTC.Mapping(
                                market_sku_id=12345,
                            ),
                        ),
                    ),
                    partner_info=DTC.PartnerInfo(
                        fulfillment_feed_id=feed_data['feed_id'],
                        supplier_id=shop['shop_id'],
                    ),
                    meta=create_meta(0, color=DTC.BLUE),
                ).SerializeToString()
            })

    return result


@pytest.fixture(scope="module")
def or3_config_ps(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    return {
        'yt': {
            'home_dir': home_dir,
            'mi3_type': MI3_TYPE,
        },
        'blue_datacamp': {
            'indexation_enabled': 'true',
            'united_offers_tablepath': ypath_join(home_dir, 'blue/datacamp/offers_table'),
            'united_stats_tablepath': ypath_join(home_dir, 'blue/datacamp/stats/recent'),
        },
        'datacamp': {
            'partners_path': ypath_join(home_dir, 'blue/datacamp/partners_table'),
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'united_offers_table_ttl': '0ms',
        },
        'feeds': {
            'status_set': "'check', 'system'",
        },
        'fullmaker': {
            'fill_genlog_in_fullmaker': 'true'
        },
    }


@pytest.fixture(scope="module")
def blue_datacamp_partners_table(yt_server, or3_config_ps, datacamp_offers_table_data):
    return DataCampPartnersTable(
        yt_server,
        or3_config_ps['datacamp']['partners_path'],
        []
    )


@pytest.fixture(scope='module')
def blue_datacamp_offers_table(yt_server, or3_config_ps, datacamp_offers_table_data):
    return DataCampOutOffersTable(
        yt_server,
        or3_config_ps['blue_datacamp']['united_offers_tablepath'],
        data=datacamp_offers_table_data
    )


@pytest.fixture(scope='module')
def blue_datacamp_stats_table(yt_server, or3_config_ps):
    return DataCampOutStatsTable(
        yt_server,
        or3_config_ps['blue_datacamp']['united_stats_tablepath'],
        []
    )


@pytest.fixture(scope='module')
def resources(
        yt_server,
        or3_config_ps,
        blue_datacamp_offers_table,
        blue_datacamp_stats_table,
        blue_datacamp_partners_table
):
    return {
        'config': Or3Config(**or3_config_ps),
        'united_datacamp_offers_table': blue_datacamp_offers_table,
        'united_datacamp_stats_table': blue_datacamp_stats_table,
        'datacamp_partners_table': blue_datacamp_partners_table,
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_ps['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_ps['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'offers': Or3OffersTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_ps['yt']['home_dir'], 'or3', 'offers', GENERATION_PS),
            data=[]
        ),
        'msku': MskuTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_ps['yt']['home_dir'], 'in', 'msku', 'recent'),
            data=[]
        ),
    }


def test_old_table_drop_check(yt_server, resources):
    """Проверяем что full maker упал на старой табличке"""
    with Or3FullMakerTestEnv(yt_server, GENERATION_PS, mi3_type=MI3_TYPE, **resources) as fm:
        fm.verify()
        assert_that(calling(fm.execute), raises(ExecutionError))
