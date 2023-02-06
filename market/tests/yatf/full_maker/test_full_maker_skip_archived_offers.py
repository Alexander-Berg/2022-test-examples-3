# coding: utf-8

import pytest

import hashlib
from datetime import datetime, timedelta
from hamcrest import assert_that, has_item, not_
from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    Offer as DatacampOffer,
    SupplyPlan,
    BLUE, WHITE,
)
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat

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

from market.pylibrary.proto_utils import message_from_data

time_pattern = "%Y-%m-%dT%H:%M:%SZ"

OLD_TIME = datetime.utcnow() - timedelta(hours=6)
NOW = datetime.now()
GENERATION = NOW.strftime(time_pattern)
MI3_TYPE = 'main'

FEED_ONE = 12321
FEED_TWO = 45654

BUSINESS_ONE = 1234
BUSINESS_TWO = 4321

OFFER_BLUE = 'blue_push_offer'
OFFER_WHITE = 'white_push_offer'

SHOP_ONE = 111
SHOP_TWO = 222

WAREHOUSE_ID = 149

PARTNERS_DATA = {
    BLUE: {
        'shop_id': SHOP_ONE,
        'status': 'publish',
        'shops_dat_info': [
            {
                'business_id': BUSINESS_ONE,
                'disabled': False,
                'shop_id': SHOP_ONE,
                'feed_id': FEED_ONE,
                'warehouse_id': WAREHOUSE_ID,
                'is_push_partner': 'true',
            },
        ]
    },
    WHITE: {
        'shop_id': SHOP_TWO,
        'status': 'publish',
        'shops_dat_info': [
            {
                'business_id': BUSINESS_TWO,
                'disabled': False,
                'shop_id': SHOP_TWO,
                'feed_id': FEED_TWO,
                'warehouse_id': WAREHOUSE_ID,
                'is_push_partner': 'true',
            },
        ]
    },
}


def create_meta(ts=None, color=None, scope=None, vertical_approved_flag=None):
    meta = {}
    if ts is not None:
        meta.update({'ts_created': ts.strftime(time_pattern)})
    if color:
        meta.update({'rgb': color})
    if scope:
        meta.update({'scope': scope})
    if vertical_approved_flag:
        meta.update({
            'vertical_approved_flag': {
                'value': vertical_approved_flag
            }
        })
    return meta


def get_md5(data):
    md5 = hashlib.md5()
    md5.update(data)
    return str(md5.hexdigest())


@pytest.fixture(scope="module")
def datacamp_offers_table_data(ts=OLD_TIME):
    result = []
    for color, offer_id in zip([BLUE, WHITE], [OFFER_BLUE, OFFER_WHITE]):
        for feed_data in PARTNERS_DATA[color]['shops_dat_info']:
            business_id = feed_data['business_id']
            shop_id = feed_data['shop_id']
            feed_id = feed_data['feed_id']
            for sp in {SupplyPlan.UNKNOWN, SupplyPlan.WILL_SUPPLY, SupplyPlan.WONT_SUPPLY, SupplyPlan.ARCHIVE}:
                shop_sku = offer_id + str(sp)
                result.append({
                    'business_id': business_id,
                    'offer_id': shop_sku,
                    'shop_id': shop_id,
                    'offer': message_from_data(
                        {
                            'identifiers': {
                                # 'supplemental_id': feed_data['warehouse_id'],
                                'shop_id': shop_id,
                                'business_id': business_id,
                                'offer_id': shop_sku,
                                'extra': {
                                    'recent_feed_id': feed_id,
                                    'recent_warehouse_id': 145,
                                    'shop_sku': shop_sku,
                                    'ware_md5': get_md5(shop_sku),
                                }
                            },
                            'content': {
                                'binding': {
                                    'approved': {
                                        'market_sku_id': shop_id,
                                    }
                                },
                                'partner': {
                                    'original_terms': {
                                        'supply_plan': {
                                            'value': sp
                                        },
                                    },
                                },
                            },
                            'partner_info': {
                                'fulfillment_feed_id': feed_id,
                                'supplier_id': shop_id,
                            },
                            'meta': create_meta(ts=ts, color=color),
                        }, DatacampOffer()
                    ).SerializeToString()
                })
    return result


@pytest.fixture(scope="module")
def or3_config(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    return {
        'yt': {
            'home_dir': home_dir
        },
        'blue_datacamp': {
            'indexation_enabled': 'true',
            'united_offers_tablepath': ypath_join(home_dir, 'datacamp/united/offers_table'),
        },
        'datacamp': {
            'indexation_enabled': 'true',
            'partners_path': ypath_join(home_dir, 'datacamp/united/partners_table'),
            'united_offers_tablepath': ypath_join(home_dir, 'datacamp/united/offers_table'),
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'fail_if_datacamp_stats_not_exists': 'false',
        },
        'feeds': {
            'status_set': "'publish'",
        },
        'fullmaker': {
            'fill_genlog_in_fullmaker': 'true'
        },
    }


@pytest.fixture(scope="module")
def datacamp_partners_table(yt_server, or3_config, datacamp_offers_table_data):
    rows = []
    for color in [BLUE, WHITE]:
        shop = PARTNERS_DATA[color]
        shop_id = shop['shop_id']
        mbi_params = []
        for shops_dat_info in shop['shops_dat_info']:
            mbi_result = ''
            if 'disabled' in shops_dat_info and shops_dat_info['disabled']:
                params = {
                    '#shop_id': str(shops_dat_info['shop_id']),
                }
            else:
                params = {
                    'shop_id': str(shops_dat_info['shop_id']),
                }
            params.update({
                'business_id': str(shops_dat_info['business_id']),
                'datafeed_id': str(shops_dat_info['feed_id']),
                'tariff': 'CLICKS',
                'regions': '213;',
                'is_enabled': 'true',
                'is_push_partner': shops_dat_info['is_push_partner'],
                'blue_status': 'REAL' if color == BLUE else 'NO',
                'is_site_market': 'false' if color == BLUE else 'true',
                'supplier_type': '3',
                'warehouse_id': str(shops_dat_info['warehouse_id']),
            })
            if 'is_tested' in shops_dat_info:
                params['is_tested'] = shops_dat_info['is_tested']
            for k, v in params.iteritems():
                mbi_result += k + '\t' + v + '\n'
            mbi_params.append(mbi_result)
        row = {
            'shop_id': shop_id,
            'status': shop['status'],
            'mbi': '\n\n'.join(mbi_params),
            'partner_stat': PartnerStat(
                offers_count=len([offer for offer in datacamp_offers_table_data if offer['shop_id'] == shop_id]),
            ).SerializeToString()
        }
        rows.append(row)
    print('foobar rows')
    print(rows)

    return DataCampPartnersTable(
        yt_server,
        or3_config['datacamp']['partners_path'],
        rows
    )


@pytest.fixture(scope='module')
def datacamp_offers_table(yt_server, or3_config, datacamp_offers_table_data):
    print('foobar datacamp_offers_table_data')
    print(datacamp_offers_table_data)
    return DataCampOutOffersTable(
        yt_server,
        or3_config['blue_datacamp']['united_offers_tablepath'],
        data=datacamp_offers_table_data
    )


@pytest.yield_fixture(scope="module")
def full_maker(
        or3_config,
        yt_server,
        datacamp_offers_table,
        datacamp_partners_table
):
    resources = {
        'config': Or3Config(**or3_config),
        'united_datacamp_offers_table': datacamp_offers_table,
        'datacamp_partners_table': datacamp_partners_table,
        'datacamp_stats': DataCampOutStatsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'datacamp', 'white', 'stats', 'recent'),
            data=[],
        ),
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
        'offers': Or3OffersTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'or3', 'offers', GENERATION),
            data=[]
        ),
        'msku': MskuTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config['yt']['home_dir'], 'in', 'msku', 'recent'),
            data=[]
        ),
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, mi3_type=MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def offers_raw_ids_sorted(offers_raw):
    return sorted([
        {
            'feed_id': int(row['feed_id']),
            'offer_id': str(row['offer_id']),
        }
        for row in offers_raw
    ])


@pytest.yield_fixture(scope="module")
def offers_raw_ids(full_maker, yt_server):
    yt = yt_server.get_yt_client()
    home_dir = full_maker.resources['config'].options['yt']['home_dir']
    offers_raw_path = ypath_join(home_dir, 'mi3', MI3_TYPE, GENERATION,  'work', 'blue_offers_raw')
    return offers_raw_ids_sorted(list(yt.read_table(offers_raw_path)))


def test_positive(full_maker, offers_raw_ids):
    """Проверяем что в offers_raw возьмутся только оффера партнера без SupplyPlan.ARCHIVE"""
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_ONE,
            'offer_id': OFFER_BLUE + str(SupplyPlan.UNKNOWN),
        }
    ))
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_TWO,
            'offer_id': OFFER_WHITE + str(SupplyPlan.UNKNOWN),
        }
    ))
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_ONE,
            'offer_id': OFFER_BLUE + str(SupplyPlan.WILL_SUPPLY),
        }
    ))
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_TWO,
            'offer_id': OFFER_WHITE + str(SupplyPlan.WILL_SUPPLY),
        }
    ))
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_ONE,
            'offer_id': OFFER_BLUE + str(SupplyPlan.WONT_SUPPLY),
        }
    ))
    assert_that(offers_raw_ids, has_item(
        {
            'feed_id': FEED_TWO,
            'offer_id': OFFER_WHITE + str(SupplyPlan.WONT_SUPPLY),
        }
    ))


def test_negative(full_maker, offers_raw_ids):
    """Проверяем что в offers_raw не возьмутся оффера с SupplyPlan.ARCHIVE"""
    assert_that(offers_raw_ids, not_(has_item(
        {
            'feed_id': FEED_ONE,
            'offer_id': OFFER_BLUE + str(SupplyPlan.ARCHIVE),
        }
    )))
    assert_that(offers_raw_ids, not_(has_item(
        {
            'feed_id': FEED_TWO,
            'offer_id': OFFER_BLUE + str(SupplyPlan.ARCHIVE),
        }
    )))
