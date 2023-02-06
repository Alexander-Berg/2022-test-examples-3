# -*- coding: utf-8 -*-

'''
Тест для проверки белого pipeline
'''

import pytest
import datetime
import time
from hamcrest import assert_that

from collections import namedtuple
from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.blue_promo_table import (
    BluePromoDetailsTable
)
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.pylibrary.proto_utils import proto_to_dict
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.proto.feedparser.Promo_pb2 import (
    OfferPromo,
    PromoDetails
)
from market.pylibrary.const.offer_promo import PromoType

from market.idx.yatf.matchers.user_friendly_dict_matcher import user_friendly_dict_equal_to
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
SESSION_ID = int(time.time())
MI3_TYPE = 'main'

BLUE_FEED_ID = 8765

MSKU_FEED_ID = 475690

ROOT_HID = 90401
DEFAULT_LEAF_HID = 123456

MSKU_1 = 200001
MSKU_2 = 200002
MSKU_2 = 200003

SUPPLIER_ID_1 = 4001

DEFAULT_VENDOR_ID = 5001


def __make_blue_offer(feed, msku, offer_id, supplier_id, vendor_id):
    return {
        'msku': msku,
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            supplier_id=supplier_id,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
            vendor_id=vendor_id,
        ),
    }


offersdata = [
    {
        'msku': 1,
        'offers': [
            # оффер без промо
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_without_promo',
            },
        ]
    },
    {
        'msku': 3,
        'offers': [
            # оффер с промокодом по feed_offer_id
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'foid2'
            },
        ]
    },
]


def get_msku_offers(msku):
    return [(offer['feed_id'], msku['msku'], offer['offer_id'], offer.get('supplier_id', None), msku.get('vendor', DEFAULT_VENDOR_ID)) for offer in msku['offers']]


# feed, msku, offer_id, supplier_id, vendor_id
BLUE_OFFERS = [__make_blue_offer(*offer_params) for offer_params in sum([get_msku_offers(msku) for msku in offersdata],  [])]


PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2 = PromoDetails(
    shop_promo_id='promocode_feed_offer_id_2_shop_promo_id',
    binary_promo_md5='promocode_feed_offer_id_2_md5',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid2'),
                ]
            )
        ),
    ]
)


OFFER_PROMO_PROMOCODE_FEED_OFFER_ID_2 = OfferPromo(
    shop_promo_id=PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2.shop_promo_id, ],
    binary_promo_md5=PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)


PipelineParams = namedtuple('PipelineParams', [
    'shards',
    'blue_shards',
    'white_offer_count',
    'blue_offer_count',
])


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=ROOT_HID,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(
    params=[
        PipelineParams(
            shards=1,
            blue_shards=1,
            white_offer_count=0,
            blue_offer_count=len(BLUE_OFFERS)
        ),
    ],
    ids=[
        'white_pipeline',
    ]
)
def pipeline_params(request):
    return request.param


@pytest.fixture(
    params=[
        'collected_promo_details',
    ],
    ids=[
        'collected_promo_details_output_dir_is_not_empty',
    ]
)
def collected_promo_details_output_dir(request):
    return request.param


@pytest.fixture()
def or3_config(yt_server, collected_promo_details_output_dir):
    home_dir = get_yt_prefix()

    misc = {
        'blue_promo_reduce_enabled': True,
    }

    yt = {
        'home_dir': home_dir,
        'yt_collected_promo_details_output_dir': collected_promo_details_output_dir,
    }

    return Or3Config(**{
        'yt': yt,
        'misc': misc,
    })


@pytest.fixture()
def shops_dat():
    return ShopsDat([
        {"datafeed_id": BLUE_FEED_ID},
    ])


@pytest.yield_fixture()
def source_blue_offers_raw(pipeline_params):
    return BLUE_OFFERS[: pipeline_params.blue_offer_count]


@pytest.yield_fixture()
def source_msku_contex(source_blue_offers_raw):
    return [
        {
            'msku': msku['msku'],
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': MSKU_FEED_ID,
            'offer_id': "msku_{}".format(msku['msku']),
            'session_id': SESSION_ID,
            'offer': make_offer_proto_str(
                market_sku=msku["msku"],
                offer_flags=OfferFlags.BLUE_OFFER.value,
                category_id=msku.get("hid", DEFAULT_LEAF_HID),
            ),
            'uc': make_uc_proto_str(
                category_id=msku.get("hid", DEFAULT_LEAF_HID),
                vendor_id=msku.get("vendor", DEFAULT_VENDOR_ID)
            ),
        }
        for msku in offersdata
    ]


@pytest.yield_fixture()
def collected_promo_details_data():
    return [
        {
            'promo_id': 'blue_promocode_2',
            'promo': PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_2.SerializeToString(),
        },
    ]


@pytest.yield_fixture()
def source_yt_tables(yt_server,
                     or3_config,
                     source_blue_offers_raw,
                     source_msku_contex,
                     collected_promo_details_data,
                     collected_promo_details_output_dir,
                     ):
    yt_home_path = or3_config.options['yt']['home_dir']
    res = {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data={}
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[],
            sort_key="id"
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
    }
    if collected_promo_details_output_dir:
        res['collected_promo_details_table'] = BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, collected_promo_details_output_dir, 'recent'),
            data=collected_promo_details_data
        )
    return res


@pytest.yield_fixture()
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, pipeline_params, tovar_tree):
    resources = {
        'config': or3_config,
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree)
    }
    resources.update(source_yt_tables)

    with Or3MainIdxTestEnv(
            yt_stuff=yt_server,
            generation=GENERATION,
            mi3_type=MI3_TYPE,
            shards=pipeline_params.shards,
            half_mode=False,
            blue_shards=pipeline_params.blue_shards,
            **resources
    ) as mi:
        mi.execute()
        mi.verify()
        yield mi


def test_count_blue_offers_merged_table(
        main_idx,
        source_blue_offers_raw,
        source_msku_contex
):
    '''
    Проверяет общее количество офферов = количество всех синих офферов
    Синие оффера - это fake_msku + реальный синие оффера
    '''
    actual_merged_offers_count = len(main_idx.outputs['blue_offers'])
    assert actual_merged_offers_count > 0
    expected_merged_offers_count = len(source_blue_offers_raw) + len(source_msku_contex)
    assert expected_merged_offers_count > 0
    assert actual_merged_offers_count == expected_merged_offers_count


def test_blue_offers_promos(main_idx, collected_promo_details_output_dir):
    result_offers = main_idx.outputs['blue_offers_by_offer_id']

    assert 'promo' not in result_offers['offer_without_promo']

    if collected_promo_details_output_dir:
        assert_that(
            result_offers['foid2']['promo'],
            user_friendly_dict_equal_to(
                proto_to_dict(OFFER_PROMO_PROMOCODE_FEED_OFFER_ID_2)
            ),
            'PROMOCODE_FEED_OFFER_ID_2',
        )
    else:
        assert 'promo' not in result_offers['foid2']
