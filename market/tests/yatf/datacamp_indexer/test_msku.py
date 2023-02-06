# coding: utf-8

"""
Тест проверяет, что msku корректно передаются из Хранилища в фул-мейкер
"""

import pytest

from datetime import datetime
from yt.wrapper import ypath_join

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.generation.yatf.resources.prepare.blue_promo_table import BluePromoDetailsTable
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampOutOffersTable, DataCampPartnersTable


GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'


BUSINESS_ID = 12345
SHOP_ID = 111
FEED_ID = 1234
BLUE_SHOP_ID = 112
BLUE_FEED_ID = 1235

EXPECTED_MSKU = 1337
ENRICHED_MSKU = 1336


PARTNERS = [
    {
        'shop_id': SHOP_ID,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': SHOP_ID,
            'business_id': BUSINESS_ID,
            'datafeed_id': FEED_ID,
            'is_enabled': True,
            'is_site_market': True,
            'tariff': 'CLICKS',
            'regions': '213;',
            'is_push_partner': True,
        })
    },
    {
        'shop_id': BLUE_SHOP_ID,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': BLUE_SHOP_ID,
            'business_id': BUSINESS_ID,
            'datafeed_id': BLUE_FEED_ID,
            'is_enabled': True,
            'is_push_partner': True,
            'blue_status': 'REAL',
            'tariff': 'FREE',
            'regions': '213;',
        })
    }
]


OFFERS = [
    {
        'business_id': BUSINESS_ID,
        'shop_id': SHOP_ID,
        'offer_id': 'white_offer',
        'warehouse_id': 0,
        'offer': DTC.Offer(**{
            'identifiers': {
                'offer_id': 'white_offer',
                'shop_id': SHOP_ID,
                'business_id': BUSINESS_ID,
                'extra': {
                    'ware_md5': 'LTb0b0kAB9sXXYgQJd6_SQ',
                    'recent_feed_id': FEED_ID,
                },
            },
            'meta': {
                'rgb': DTC.WHITE,
            },
            'content': {
                'binding': {
                    'uc_mapping': {
                        'market_sku_id': EXPECTED_MSKU
                    }
                },
                'market': {
                    'enriched_offer': {
                        'market_sku_id': ENRICHED_MSKU,
                    }
                },
            },
        }).SerializeToString()
    },
    {
        'business_id': BUSINESS_ID,
        'shop_id': BLUE_SHOP_ID,
        'offer_id': 'blue_offer',
        'warehouse_id': 111,
        'offer': DTC.Offer(**{
            'identifiers': {
                'offer_id': 'blue_offer',
                'shop_id': BLUE_SHOP_ID,
                'business_id': BUSINESS_ID,
                'warehouse_id': 111,
                'extra': {
                    'ware_md5': 'LTb0b0kAB9sXXYgQJd6_SP',
                    'recent_feed_id': BLUE_FEED_ID,
                },
            },
            'meta': {
                'rgb': DTC.BLUE,
            },
            'content': {
                'binding': {
                    'approved': {
                        'market_sku_id': EXPECTED_MSKU
                    }
                },
                'market': {
                    'enriched_offer': {
                        'market_sku_id': ENRICHED_MSKU,
                    }
                },
            },
            'partner_info': {
                'is_blue_offer': True,
                'supplier_id': 222,
            },
        }).SerializeToString()
    },
]


@pytest.fixture(scope='module')
def offers():
    return OFFERS


@pytest.fixture(scope='module')
def united_datacamp_offers_table(yt_server, offers):
    return DataCampOutOffersTable(yt_server, '//home/datacamp/out', data=offers)


@pytest.fixture(scope="module")
def or3_config(yt_server, united_datacamp_offers_table, partners_table):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': 'collected_promo_details',
        },
        'datacamp': {
            'partners_path': partners_table.table_path,
            'indexation_enabled': 'true',
            'united_offers_tablepath': united_datacamp_offers_table.table_path,
        },
        'feeds': {
            'status_set': "'mock', 'publish'",
        },
        'misc': {
            'fail_if_datacamp_stats_not_exists': 'false',
        },
    }
    return config


@pytest.fixture(scope="module")
def partners_table(yt_server):
    return DataCampPartnersTable(yt_server, '//home/datacamp/partners', data=PARTNERS)


@pytest.yield_fixture(scope="module")
def full_maker(or3_config, yt_server, united_datacamp_offers_table, partners_table):
    resources = {
        'config': Or3Config(**or3_config),
        'datacamp': united_datacamp_offers_table,
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'datacamp_partners_table': partners_table,
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'collected_promo_details', 'recent'),
            data=[],
        ),
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def test_msku_in_offers_raw(full_maker):
    """
        Проверяет, что все офферы получают msku из маппинов, а не из uc
    """
    for row in full_maker.offers_raw_corrected:
        assert row['offer']['market_sku'] == str(EXPECTED_MSKU)
