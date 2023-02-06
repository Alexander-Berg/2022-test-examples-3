# coding: utf-8

import pytest
from hamcrest import has_properties, assert_that, has_item

from datetime import datetime

from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerStat
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

from market.pylibrary import shopsdat


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
    rows = []
    for shop in PARTNERS_DATA:
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
                'is_mock': 'true',
                'is_push_partner': shops_dat_info['is_push_partner'],
                'blue_status': 'REAL',
                'supplier_type': '3',
                'warehouse_id': str(shops_dat_info['warehouse_id']),
                'ff_program': 'REAL',
                'ff_virtual_id': str(VIRTUAL_SHOP_ID),
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

    return DataCampPartnersTable(
        yt_server,
        or3_config_ps['datacamp']['partners_path'],
        rows
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


@pytest.yield_fixture(scope="module")
def full_maker_ps(
        or3_config_ps,
        yt_server,
        blue_datacamp_offers_table,
        blue_datacamp_stats_table,
        blue_datacamp_partners_table
):
    resources = {
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
    with Or3FullMakerTestEnv(yt_server, GENERATION_PS, mi3_type=MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def offers_raw_ids(offers_raw):
    return sorted([
        {
            'feed_id': int(row['feed_id']),
            'offer_id': str(row['offer_id']),
        }
        for row in offers_raw
    ])


@pytest.yield_fixture(scope="module")
def offers_raw_ids_ps(full_maker_ps, yt_server):
    yt = yt_server.get_yt_client()
    home_dir = full_maker_ps.resources['config'].options['yt']['home_dir']
    offers_raw_path = ypath_join(home_dir, 'mi3', MI3_TYPE, GENERATION_PS,  'work', 'blue_offers_raw')
    return offers_raw_ids(list(yt.read_table(offers_raw_path)))


def test_offer_data_for_ps_indexation(full_maker_ps, offers_raw_ids_ps):
    """Проверяем что на ps в offers_raw возьмутся только оффера партнера в статусе check и system,
    а в статусе publish будут отброшены"""
    expected = [
        {
            'feed_id': 1069,
            'offer_id': 'push_offer_01',
        },
        {
            'feed_id': 45654,
            'offer_id': 'push_offer_01',
        },
    ]
    assert sorted(expected) == offers_raw_ids_ps

    generated_shopsdat = shopsdat.loadfeeds(full_maker_ps.shopsdat_generated_path, status_flags=shopsdat.STATUS_ANY)
    # проверяем, что в сгенеренном шопс дате check фид попадает без знака #
    assert_that(generated_shopsdat,
                has_item(has_properties(
                    {
                        'id':  45654,
                        '_is_commented_out': False,
                    }
                )))
