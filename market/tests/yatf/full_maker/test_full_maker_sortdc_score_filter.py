# coding: utf-8

import pytest

import hashlib
import os
import tempfile

from datetime import datetime, timedelta
from hamcrest import assert_that, has_item
from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    Offer as DatacampOffer,
    BLUE, WHITE,
)
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat

from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.or3offers_table import Or3OffersTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable
from market.idx.yatf.resources.resource import FileGeneratorResource

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampOutOffersTable,
    DataCampOutStatsTable,
    DataCampPartnersTable,
)
from market.idx.yatf.resources.msku_table import MskuTable

from market.pylibrary.proto_utils import message_from_data

from robot.sortdc.protos.user_pb2 import EUser

from yt.yson.yson_types import YsonEntity

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


SORTDC_CONFIG_PATH = os.path.join(tempfile.mkdtemp(), 'batch_exporter_config.pb.txt')
SORTDC_CONFIG_TEXT = """Policies {
    Id: 0
    Name: "goods_main"
    Quota: 2
    User: GOODS
}

Policies {
    Id: 1
    Name: "goods_test"
    Quota: 4
    User: GOODS
}

Policies {
    Id: 2
    Name: "goods_probe"
    Quota: 4
    User: GOODS
    RankType: R_STABLE_RANDOM
    Disabled: true

    FeedOfferGeneratorConfig {
        Disabled: true
    }

    InternetOfferGeneratorConfig {
        EnableRecentlyCrawledFilter: false
        EnableWhitelistFilter: false
    }
}
"""


class SortDCConfig(FileGeneratorResource):
    def __init__(self, config, filename='batch_exporter_config.pb.txt'):
        super(SortDCConfig, self).__init__(filename=filename)
        self._config = config
        self.dump(path=SORTDC_CONFIG_PATH)

    def dump(self, path):
        super(SortDCConfig, self).dump(path)
        dirname = os.path.dirname(path)
        if not os.path.exists(dirname):
            os.makedirs(dirname)
        with open(path, 'w') as f:
            f.write(self._config)


@pytest.fixture(scope='module')
def sortdc_config():
    return SortDCConfig(SORTDC_CONFIG_TEXT)


def create_meta(ts=None, color=None, scope=None, vertical_approved_flag=None, has_context=None, has_export_items=None, offer_score=None, policy=None):
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
    if has_context:
        sortdc_context = {
            'sortdc_context': {}
        }
        if has_export_items:
            sortdc_context['sortdc_context']['export_items'] = [{
                'offer_score': offer_score,
                'user': EUser.GOODS,
                'policy_id': policy
            }]
        meta.update(sortdc_context)
    return meta


def get_md5(data):
    md5 = hashlib.md5()
    md5.update(data)
    return str(md5.hexdigest())


class OfferScoreTestCase:
    def __init__(self, score, policy, has_context, has_export_items):
        self.score = score
        self.policy = policy
        self.has_context = has_context
        self.has_export_items = has_export_items


@pytest.fixture(scope="module")
def datacamp_offers_table_data(ts=OLD_TIME):
    result = []
    offer_score_idx = 0
    test_cases = [
        OfferScoreTestCase(score=10, policy=0, has_context=True, has_export_items=False),
        OfferScoreTestCase(score=11, policy=1, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=12, policy=0, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=13, policy=None, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=None, policy=0, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=15, policy=1, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=16, policy=0, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=17, policy=1, has_context=False, has_export_items=True),
        OfferScoreTestCase(score=18, policy=1, has_context=True, has_export_items=True),
        OfferScoreTestCase(score=19, policy=2, has_context=True, has_export_items=True),
    ]
    for color, offer_id in zip([BLUE, WHITE], [OFFER_BLUE, OFFER_WHITE]):
        for feed_data in PARTNERS_DATA[color]['shops_dat_info']:
            business_id = feed_data['business_id']
            shop_id = feed_data['shop_id']
            feed_id = feed_data['feed_id']
            for i in range(5):
                shop_sku = offer_id + '_' + str(offer_score_idx)
                result.append({
                    'business_id': business_id,
                    'offer_id': shop_sku,
                    'shop_id': shop_id,
                    'offer': message_from_data(
                        {
                            'identifiers': {
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
                            },
                            'partner_info': {
                                'fulfillment_feed_id': feed_id,
                                'supplier_id': shop_id,
                            },
                            'meta': create_meta(
                                ts=ts,
                                color=color,
                                has_context=test_cases[offer_score_idx].has_context,
                                has_export_items=test_cases[offer_score_idx].has_export_items,
                                offer_score=test_cases[offer_score_idx].score,
                                policy=test_cases[offer_score_idx].policy,
                            ),
                        }, DatacampOffer()
                    ).SerializeToString()
                })
                offer_score_idx += 1
    return result


@pytest.fixture(scope="module")
def or3_config(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    return {
        'general': {
            'sortdc_score_filter': 'true'
        },
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
            'fill_genlog_in_fullmaker': 'true',
            'sortdc_config_path': SORTDC_CONFIG_PATH
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
        datacamp_partners_table,
        sortdc_config
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
        'sortdc_config': sortdc_config,
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
            'offer_score': float(row['offer_score']) if row['offer_score'] is not None else None,
            'offer_score_policy_id': int(row['offer_score_policy_id']) if row['offer_score_policy_id'] is not None else None,
        }
        for row in offers_raw
    ])


@pytest.yield_fixture(scope="module")
def offers_data(full_maker, yt_server):
    yt = yt_server.get_yt_client()
    home_dir = full_maker.resources['config'].options['yt']['home_dir']
    offers_raw_path = ypath_join(home_dir, 'mi3', MI3_TYPE, GENERATION,  'work', 'offers_raw')
    blue_offers_raw_path = ypath_join(home_dir, 'mi3', MI3_TYPE, GENERATION,  'work', 'blue_offers_raw')

    result = {}
    result['white'] = {}
    result['white']['data'] = offers_raw_ids_sorted(list(yt.read_table(offers_raw_path)))
    result['white']['bound_offer_score_by_policy'] = yt.get(ypath_join(offers_raw_path, '@bound_offer_score_by_policy'))

    result['blue'] = {}
    result['blue']['data'] = offers_raw_ids_sorted(list(yt.read_table(blue_offers_raw_path)))
    result['blue']['bound_offer_score_by_policy'] = yt.get(ypath_join(blue_offers_raw_path, '@bound_offer_score_by_policy'))
    return result


def check_item(offers_data, score, policy, feed_type, offer_type, offer_id):
    assert_that(offers_data['white']['data'], has_item(
        {
            'feed_id': feed_type,
            'offer_id': offer_type + '_' + str(offer_id),
            'offer_score': score,
            'offer_score_policy_id': policy
        }
    ))
    assert_that(offers_data['blue']['data'], has_item(
        {
            'feed_id': feed_type,
            'offer_id': offer_type + '_' + str(offer_id),
            'offer_score': score,
            'offer_score_policy_id': policy
        }
    ))


def test_sortdc_score_filter(full_maker, offers_data):
    """Проверяем что в offers_raw правильно приехали score и выставлен правильный граничный score"""
    assert len(offers_data['white']['data']) == 10
    assert len(offers_data['blue']['data']) == 10

    for score, policy, offer_id in zip([None, 11, 12, 13, None], [None, 1, 0, None, 0], [0, 1, 2, 3, 4]):
        check_item(offers_data, score, policy, FEED_ONE, OFFER_BLUE, offer_id)

    for score, policy, offer_id in zip([15, 16, None, 18, 19], [1, 0, None, 1, 2], [5, 6, 7, 8, 9]):
        check_item(offers_data, score, policy, FEED_TWO, OFFER_WHITE, offer_id)

    assert offers_data['white']['bound_offer_score_by_policy']['0'] == 12.0
    assert offers_data['white']['bound_offer_score_by_policy']['1'] == 11.0
    assert isinstance(offers_data['white']['bound_offer_score_by_policy']['2'], YsonEntity)

    assert offers_data['blue']['bound_offer_score_by_policy']['0'] == 12.0
    assert offers_data['blue']['bound_offer_score_by_policy']['1'] == 11.0
    assert isinstance(offers_data['blue']['bound_offer_score_by_policy']['2'], YsonEntity)
