# coding=utf-8

"""
Проверяем что офферы из Датакемповой выгрузки Лавки lavka_out складываются в сырые белые офферы
"""


from datetime import datetime
import hashlib
import os
import pytest

from hamcrest import assert_that, has_entries, has_items

from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable
from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv

# from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampOutOffersTable


GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
ROOT_CATEGORY = 90401
FEED_LAVKA = 333
SHOP_LAVKA = 444
BUSINESS_LAVKA = 555

LAVKA_OFFER = dict(
    business_id=BUSINESS_LAVKA,
    shop_id=SHOP_LAVKA,
    offer_id='lavka_offer_1',
    offer=DTC.Offer(**{
        "content": {
            "partner": {
                "actual": {
                    "description": {
                        "value": "cookies made in PRC"
                    },
                    "title": {
                        "value": "cookies"
                    }
                },
            },
        },
        "identifiers": {
            "business_id": BUSINESS_LAVKA,
            "extra": {
                "ware_md5": "wTG5hGWxKj1Gw5PfDz9ORw",
                "recent_feed_id": FEED_LAVKA,
            },
            "feed_id": FEED_LAVKA,
            "offer_id": "lavka_offer_1",
            "shop_id": SHOP_LAVKA,
            "warehouse_id": 0,
        },
        "meta": {
            "rgb": DTC.LAVKA,
            "scope": 2,
        },
        "price": {
            "basic": {
                "binary_price": {
                    "id": "RUR",
                    "plus": 0,
                    "price": 450000000,
                    "rate": "1",
                    "ref_id": "RUR"
                },
            },
        },
    }).SerializeToString(),
)


def make_mbi_params(shop_id, feed_id):
    params = {
        'shop_id': str(shop_id),
        'datafeed_id': str(feed_id),
        'is_enabled': 'true',
        'tariff': 'CLICKS',
        'blue_status': 'NO',
        'is_push_partner': 'true',
        'supplier_type': '3',
        'regions': '213;',
        'is_lavka': 'true',
    }

    mbi_result = ''
    for k, v in params.iteritems():
        mbi_result += k + '\t' + v + '\n'
    mbi_params = [mbi_result]
    return '\n\n'.join(mbi_params)


def make_feed(shop_id, feed_id):
    mbi_params = make_mbi_params(shop_id, feed_id)
    md5 = hashlib.md5()
    md5.update(mbi_params)
    return {
        'feed': feed_id,
        'status': 'publish',
        'ttr': 18446744073709551615,  # max uint64
        'mbi': str(mbi_params),
        'mbi_md5': str(md5.hexdigest())
    }


@pytest.yield_fixture(scope="module")
def or3_config_data():
    home_dir = get_yt_prefix()
    return dict(
        yt={
            'home_dir': home_dir,
        },
        feeds={
            'status_set': "'publish'",
        },
        lavka={
            'indexation_enabled': 'true',
            'offers_table_path': ypath_join(home_dir, 'in', 'lavka', 'offers', 'recent'),
        },
        fullmaker={
            'fill_genlog_in_fullmaker': 'true'
        }
    )


@pytest.fixture(scope='module')
def lavka_shops_dat():
    return ShopsDat([{
        'datafeed_id': FEED_LAVKA,
        'shop_id': SHOP_LAVKA,
        'is_lavka': 'true',
        'cpa': 'REAL',
        'cpc': 'NO',
    }], filename='eats-and-lavka-partners.dat')


@pytest.fixture(scope="module")
def full_maker(yt_server, or3_config_data, lavka_shops_dat):
    resources = {
        'config': Or3Config(**or3_config_data),
        'eats_and_lavka_shops_dat': lavka_shops_dat,
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=os.path.join(or3_config_data['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=os.path.join(or3_config_data['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'lavka_offers': DataCampOutOffersTable(
            yt_stuff=yt_server,
            path=os.path.join(or3_config_data['yt']['home_dir'], 'in', 'lavka', 'offers', 'recent'),
            data=[LAVKA_OFFER]
        ),
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, 'main', **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def test_lavka_offer_flags(or3_config_data, full_maker):
    """ Проверяем, что офферы из Лавки попадают в таблицу raw_offers
        с флагом IS_LAVKA и корневой категорией """
    assert_that(
        full_maker.offers_raw_corrected,
        has_items(
            has_entries({
                'feed_id': FEED_LAVKA,
                'offer_id': 'lavka_offer_1',
                'offer': has_entries({
                    'yx_shop_offer_id': 'lavka_offer_1',
                    'yx_ds_id': SHOP_LAVKA,
                    # поправить вместе с полным переводом full-maker на genlog
                    # 'offer_flags64': str(OfferFlags.IS_LAVKA | OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.DELIVERY | OfferFlags.CPA),
                    'title': 'cookies',
                    'description': 'cookies made in PRC',
                    'ware_md5': 'wTG5hGWxKj1Gw5PfDz9ORw',
                }),
            }),
        )
    )
