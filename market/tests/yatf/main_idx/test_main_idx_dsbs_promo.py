# -*- coding: utf-8 -*-

''' Проверяем что промоакции корректно матчатся к dsbs-офферам в main-idx
'''

import pytest
import datetime
import time
from hamcrest import (
    assert_that,
    has_entries,
    has_item,
    all_of,
    is_not,
    has_key,
    contains_inanyorder,
)

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
from market.idx.generation.yatf.utils.fixtures import (
    CpaStatus,
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.offers.yatf.utils.fixtures import (
    make_proto_lenval_pictures,
    genererate_default_pictures
)

from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.proto.feedparser.Promo_pb2 import (
    OfferPromo,
    PromoDetails
)
from market.pylibrary.const.offer_promo import PromoType
from market.proto.common.common_pb2 import ESupplierFlag

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
SESSION_ID = int(time.time())
MI3_TYPE = 'main'

WHITE_FEED_ID = 666
MSKU_FEED_ID = 9999

WHITE_OFFER_ID_1 = 'white_offer_1'
WHITE_OFFER_ID_2 = 'white_offer_2'
WHITE_OFFER_ID_3 = 'white_offer_3'
WHITE_OFFER_ID_4 = 'white_offer_4'
WHITE_OFFER_ID_5 = 'white_offer_5'
WHITE_OFFER_ID_6 = 'white_offer_6'
WHITE_OFFER_ID_7 = 'white_offer_7'
WHITE_OFFER_ID_8 = 'white_offer_8'
WHITE_OFFER_ID_9 = 'white_offer_9'
WHITE_OFFER_ID_10 = 'white_offer_10'
WHITE_OFFER_ID_11 = 'white_offer_11'

ROOT_HID = 90401
DEFAULT_HID = 13
LEAF_HID_2 = 14
LEAF_HID_3 = 15
DEFAULT_VENDOR = 7
DEFAULT_SHOP_ID = 50
SHOP_ID_2 = 51
DEFAULT_WAREHOUSE_ID = 25


def make_offer(feed_id, offer_id, msku, category_id, shop_id, flags, is_dsbs, promo, warehouse_id):
    result = {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            offer_flags=flags,
            offer_flags64=flags,
            cpa=CpaStatus.REAL if is_dsbs is True else CpaStatus.NO,
            is_blue_offer=bool(flags & OfferFlags.BLUE_OFFER.value),
            is_fake_msku_offer=False,
            yx_ds_id=shop_id,
            warehouse_id=warehouse_id,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
            category_id=category_id,
        ),
    }
    if msku is not None:
        result['msku'] = msku
    if promo is not None:
        result['promo'] = promo.SerializeToString()

    return result


def make_white_offer(feed_id, offer_id, category_id, shop_id, flags, is_dsbs, promo, warehouse_id=DEFAULT_WAREHOUSE_ID):
    return make_offer(feed_id, offer_id, None, category_id, shop_id, flags, is_dsbs, promo, warehouse_id)


@pytest.yield_fixture(scope="module")
def source_white_offers(promo_blue_set):
    return [
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_1, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, promo_blue_set),
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_2, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_3, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        # WHITE_OFFER_ID_4 is not dsbs-offer, so it will not have matched promos
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_4, DEFAULT_HID, DEFAULT_SHOP_ID, 0, False, None),
        # WHITE_OFFER_ID_5 is dsbs-offer which doesn't have corresponding
        # matching rules, so it will not have matched promos
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_5, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        # Dsbs offer with promo code matched by feed_id+offer_id
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_6, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        # Dsbs offer with promo code matched by supplier_id
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_7, DEFAULT_HID, SHOP_ID_2, 0, True, None),
        # Dsbs offer with promo code matched by category
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_8, LEAF_HID_2, DEFAULT_SHOP_ID, 0, True, None),
        # Dsbs offer with promo code matched by feed_id+offer_id
        # This offer participates in promo from datacamp
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_9, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        # Dsbs offer with directDiscount matched by feed_id+offer_id
        # This offer participates in promo from datacamp
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_10, DEFAULT_HID, DEFAULT_SHOP_ID, 0, True, None),
        # Dsbs offer with dsbs supplier restriction
        make_white_offer(WHITE_FEED_ID, WHITE_OFFER_ID_11, LEAF_HID_3, DEFAULT_SHOP_ID, 0, True, None),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers():
    return []


@pytest.yield_fixture(scope="module")
def promo_details_cheapest_as_gift():
    return PromoDetails(
        shop_promo_id='promo_cheapest_as_gift_shop_promo_id',
        binary_promo_md5='promo_cheapest_as_gift_md5',
        type=PromoType.CHEAPEST_AS_GIFT,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_1),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_3),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_4),
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def promo_cheapest_as_gift(promo_details_cheapest_as_gift):
    return OfferPromo(
        shop_promo_id=promo_details_cheapest_as_gift.shop_promo_id,
        shop_promo_ids=[promo_details_cheapest_as_gift.shop_promo_id],
        binary_promo_md5=promo_details_cheapest_as_gift.binary_promo_md5,
        binary_promo_md5s=[promo_details_cheapest_as_gift.binary_promo_md5],
        promo_type=PromoType.CHEAPEST_AS_GIFT,
    )


@pytest.yield_fixture(scope="module")
def promo_details_generic_bundle():
    return PromoDetails(
        shop_promo_id='promo_generic_bundle_shop_promo_id',
        binary_promo_md5='promo_generic_bundle_md5',
        type=PromoType.GENERIC_BUNDLE,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_2),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_3),
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def promo_generic_bundle(promo_details_generic_bundle):
    return OfferPromo(
        shop_promo_id=promo_details_generic_bundle.shop_promo_id,
        shop_promo_ids=[promo_details_generic_bundle.shop_promo_id],
        binary_promo_md5=promo_details_generic_bundle.binary_promo_md5,
        binary_promo_md5s=[promo_details_generic_bundle.binary_promo_md5],
        promo_type=PromoType.GENERIC_BUNDLE,
    )


@pytest.yield_fixture(scope="module")
def promo_details_promo_code():
    return PromoDetails(
        shop_promo_id='promo_code_shop_promo_id',
        binary_promo_md5='promo_code_md5',
        type=PromoType.PROMO_CODE,
        offers_matching_rules=[
            # Правило для матчинга оффера WHITE_OFFER_ID_6 по feedOfferId
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_6),
                    ]
                )
            ),
            # Правило для матчинга оффера WHITE_OFFER_ID_7 по supplier_id
            PromoDetails.OffersMatchingRule(
                suppliers=PromoDetails.OffersMatchingRule.IdsList(
                    ids=[
                        SHOP_ID_2,
                    ]
                )
            ),
            # Правило для матчинга оффера WHITE_OFFER_ID_8 по category_id
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        LEAF_HID_2
                    ],
                ),
            ),
            # Правило для матчинга оффера WHITE_OFFER_ID_11 по category_id и флагу dsbs
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        LEAF_HID_3
                    ],
                ),
                supplier_flag_restriction=PromoDetails.OffersMatchingRule.SupplierFlagRestriction(
                    supplier_flags=int(ESupplierFlag.DSBS_SUPPLIER),
                )
            ),
        ]
    )


# Промокод из партнёрского интерфейса
@pytest.yield_fixture(scope="module")
def promo_details_promo_code_datacamp():
    return PromoDetails(
        shop_promo_id='promo_code_datacamp_shop_promo_id',
        binary_promo_md5='promo_code_md5_datacamp',
        type=PromoType.PROMO_CODE,
        offers_matching_rules=[
            # Правило для матчинга оффера WHITE_OFFER_ID_9 по feedOfferId
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_9),
                    ]
                )
            ),
        ]
    )


# Прямая скидка из партнёрского интерфейса
@pytest.yield_fixture(scope="module")
def promo_details_direct_discount_datacamp():
    return PromoDetails(
        shop_promo_id='direct_discount_datacamp_shop_promo_id',
        binary_promo_md5='direct_discount_md5_datacamp',
        type=PromoType.DIRECT_DISCOUNT,
        offers_matching_rules=[
            # Правило для матчинга оффера WHITE_OFFER_ID_10 по feedOfferId
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_10),
                    ]
                )
            ),
        ]
    )


# Синий флеш из партнёрского интерфейса
@pytest.yield_fixture(scope="module")
def promo_details_blue_flash_datacamp():
    return PromoDetails(
        shop_promo_id='blue_flash_datacamp_shop_promo_id',
        binary_promo_md5='blue_flash_md5_datacamp',
        type=PromoType.BLUE_FLASH,
        offers_matching_rules=[
            # Правило для матчинга оффера WHITE_OFFER_ID_10 по feedOfferId
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_10),
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def promo_details_blue_set_with_rules():
    return PromoDetails(
        shop_promo_id='blue_set_shop_promo_id_with_rules',
        binary_promo_md5='blue_set_md5_with_rules',
        type=PromoType.BLUE_SET,
        offers_matching_rules=[
            # Правило для матчинга оффера WHITE_OFFER_ID_6 по feedOfferId
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID_6),
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def offerpromo_promo_code(promo_details_promo_code):
    return OfferPromo(
        shop_promo_id=promo_details_promo_code.shop_promo_id,
        shop_promo_ids=[promo_details_promo_code.shop_promo_id],
        binary_promo_md5=promo_details_promo_code.binary_promo_md5,
        binary_promo_md5s=[promo_details_promo_code.binary_promo_md5],
        promo_type=PromoType.PROMO_CODE,
    )


@pytest.yield_fixture(scope="module")
def offerpromo_promo_code_datacamp(promo_details_promo_code_datacamp):
    return OfferPromo(
        shop_promo_id=promo_details_promo_code_datacamp.shop_promo_id,
        shop_promo_ids=[promo_details_promo_code_datacamp.shop_promo_id],
        binary_promo_md5=promo_details_promo_code_datacamp.binary_promo_md5,
        binary_promo_md5s=[promo_details_promo_code_datacamp.binary_promo_md5],
        promo_type=PromoType.PROMO_CODE,
    )


@pytest.yield_fixture(scope="module")
def offerpromo_direct_discount_datacamp(promo_details_direct_discount_datacamp):
    return OfferPromo(
        shop_promo_id=promo_details_direct_discount_datacamp.shop_promo_id,
        shop_promo_ids=[promo_details_direct_discount_datacamp.shop_promo_id],
        binary_promo_md5=promo_details_direct_discount_datacamp.binary_promo_md5,
        binary_promo_md5s=[promo_details_direct_discount_datacamp.binary_promo_md5],
        promo_type=PromoType.DIRECT_DISCOUNT,
    )


@pytest.yield_fixture(scope="module")
def offerpromo_blue_flash_datacamp(promo_details_blue_flash_datacamp):
    return OfferPromo(
        shop_promo_id=promo_details_blue_flash_datacamp.shop_promo_id,
        shop_promo_ids=[promo_details_blue_flash_datacamp.shop_promo_id],
        binary_promo_md5=promo_details_blue_flash_datacamp.binary_promo_md5,
        binary_promo_md5s=[promo_details_blue_flash_datacamp.binary_promo_md5],
        promo_type=PromoType.BLUE_FLASH,
    )


@pytest.yield_fixture(scope="module")
def promo_details_blue_set():
    return PromoDetails(
        shop_promo_id='promo_blue_set_shop_promo_id',
        binary_promo_md5='promo_blue_set_md5',
        type=PromoType.BLUE_SET,
    )


@pytest.yield_fixture(scope="module")
def promo_blue_set(promo_details_blue_set):
    return OfferPromo(
        shop_promo_id=promo_details_blue_set.shop_promo_id,
        shop_promo_ids=[promo_details_blue_set.shop_promo_id],
        binary_promo_md5=promo_details_blue_set.binary_promo_md5,
        binary_promo_md5s=[promo_details_blue_set.binary_promo_md5],
        promo_type=PromoType.BLUE_SET,
    )


@pytest.yield_fixture(scope="module")
def promo_details_data(promo_details_cheapest_as_gift, promo_details_generic_bundle, promo_details_blue_set_with_rules):
    return [
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_blue_set_with_rules.shop_promo_id,
            'promo': promo_details_blue_set_with_rules.SerializeToString(),
        },
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_cheapest_as_gift.shop_promo_id,
            'promo': promo_details_cheapest_as_gift.SerializeToString(),
        },
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_generic_bundle.shop_promo_id,
            'promo': promo_details_generic_bundle.SerializeToString(),
        },
    ]


@pytest.yield_fixture(scope="module")
def promo_code_details_data(promo_details_promo_code):
    return [
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_promo_code.shop_promo_id,
            'promo': promo_details_promo_code.SerializeToString(),
        },
    ]


@pytest.yield_fixture(scope="module")
def promo_code_details_data_datacamp(promo_details_promo_code_datacamp, promo_details_blue_flash_datacamp, promo_details_direct_discount_datacamp):
    return [
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_direct_discount_datacamp.shop_promo_id,
            'promo': promo_details_direct_discount_datacamp.SerializeToString(),
        },
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_blue_flash_datacamp.shop_promo_id,
            'promo': promo_details_blue_flash_datacamp.SerializeToString(),
        },
        {
            'feed_id': WHITE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_promo_code_datacamp.shop_promo_id,
            'promo': promo_details_promo_code_datacamp.SerializeToString(),
        },
    ]


@pytest.yield_fixture(scope='module')
def collected_promo_details_data(promo_details_data, promo_code_details_data, promo_code_details_data_datacamp):
    return sorted(promo_details_data + promo_code_details_data + promo_code_details_data_datacamp, key=lambda x: x['promo_id'])


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
        MboCategory(
            hid=DEFAULT_HID,
            tovar_id=1,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 1",
            name="Leaf hid 1",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_2,
            tovar_id=2,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 2",
            name="Leaf hid 2",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_3,
            tovar_id=3,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 3",
            name="Leaf hid 3",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.yield_fixture(scope="module")
def pipeline_params():
    return dict(
        shards=8,
        blue_shards=8,
    )


@pytest.fixture(scope='module')
def collected_promo_details_output_dir():
    return 'collected_promo_details'


@pytest.yield_fixture(scope="module")
def or3_config(collected_promo_details_output_dir):
    misc = {}

    home_dir = get_yt_prefix()

    return Or3Config(**{
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': collected_promo_details_output_dir,
        },
        'misc': misc,
    })


@pytest.yield_fixture(scope="module")
def shops_dat():
    return ShopsDat([
        {'datafeed_id': WHITE_FEED_ID, 'warehouse_id': DEFAULT_WAREHOUSE_ID, 'is_dsbs': True},
    ])


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        {
            'msku': id,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': MSKU_FEED_ID,
            'offer_id': "msku_" + str(id),
            'session_id': SESSION_ID,
            'offer': make_offer_proto_str(
                market_sku=id,
                offer_flags=OfferFlags.MARKET_SKU.value,
                category_id=DEFAULT_HID,
                is_fake_msku_offer=True,
            ),
            'uc': make_uc_proto_str(
                category_id=DEFAULT_HID,
                vendor_id=DEFAULT_VENDOR,
            ),
            'pic': make_proto_lenval_pictures(genererate_default_pictures()),
        }
        for id in [1]
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config,
                     source_white_offers,
                     source_blue_offers,
                     collected_promo_details_data,
                     collected_promo_details_output_dir,
                     source_msku_contex,
                     ):
    yt_home_path = or3_config.options['yt']['home_dir']

    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_white_offers
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
            data=source_blue_offers
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
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, collected_promo_details_output_dir, 'recent'),
            data=collected_promo_details_data,
        ),
    }


@pytest.yield_fixture(scope="module")
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
            shards=pipeline_params['shards'],
            half_mode=False,
            blue_shards=pipeline_params['blue_shards'],
            **resources
    ) as mi:
        mi.execute()
        mi.verify()
        yield mi


def test_offers_count(
    main_idx,
    source_blue_offers,
    source_white_offers,
    source_msku_contex,
):
    ''' Проверяeм количество офферов в выходной синей и белой таблицах
    '''

    actual_count = len(main_idx.outputs['blue_offers'])
    assert actual_count > 0
    expected_count = len(source_blue_offers) + len(source_msku_contex)
    assert expected_count > 0
    assert actual_count == expected_count

    actual_count = len(main_idx.outputs['offers'])
    assert actual_count > 0
    expected_count = len(source_white_offers) + len(source_blue_offers) + len(source_msku_contex)
    assert expected_count > 0
    assert actual_count == expected_count


def test_dsbs_promo(
    main_idx,
    promo_cheapest_as_gift,
    promo_generic_bundle,
    promo_blue_set,
    offerpromo_promo_code,
    offerpromo_promo_code_datacamp,
    offerpromo_direct_discount_datacamp,
    offerpromo_blue_flash_datacamp,
    promo_details_blue_set_with_rules,
):
    ''' Проверяeм что в выходной белой таблице промо к белым - применяются'''

    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        offers,
        all_of(
            has_key(WHITE_OFFER_ID_1),
            has_key(WHITE_OFFER_ID_2),
            has_key(WHITE_OFFER_ID_3),
            has_key(WHITE_OFFER_ID_4),
            has_key(WHITE_OFFER_ID_5),
            has_key(WHITE_OFFER_ID_6),
            has_key(WHITE_OFFER_ID_7),
            has_key(WHITE_OFFER_ID_8),
            has_key(WHITE_OFFER_ID_9),
            has_key(WHITE_OFFER_ID_10),
            has_key(WHITE_OFFER_ID_11),
        )
    )

    assert_that(
        offers[WHITE_OFFER_ID_1],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_1,
            'promo': has_entries({
                'shop_promo_ids': contains_inanyorder(
                    promo_blue_set.shop_promo_id,
                    promo_cheapest_as_gift.shop_promo_id,
                ),
            }),
        }),
        'white dsbs offer has new matched cheapest_as_gift promo and original blue_set promo with apply_dsbs_promos flag'
    )
    assert_that(
        offers[WHITE_OFFER_ID_2],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_2,
            'promo': has_entries({
                'shop_promo_ids': [promo_generic_bundle.shop_promo_id],
            }),
        }),
        'white dsbs offer has generic_bundle promo with apply_dsbs_promos flag'
    )
    assert_that(
        offers[WHITE_OFFER_ID_3],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_3,
            'promo': has_entries({
                'shop_promo_ids': contains_inanyorder(
                    promo_generic_bundle.shop_promo_id,
                    promo_cheapest_as_gift.shop_promo_id,
                ),
            }),
        }),
        'white dsbs offer has both generic_bundle and cheapest_as_gift promos with apply_dsbs_promos flag'
    )
    for offer_id in [WHITE_OFFER_ID_6, WHITE_OFFER_ID_7, WHITE_OFFER_ID_8, WHITE_OFFER_ID_11]:
        assert_that(
            offers[offer_id],
            has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': offer_id,
                'promo': has_entries({
                    'shop_promo_ids': has_item(
                        offerpromo_promo_code.shop_promo_id,
                    ),
                }),
            }),
            'white dsbs offer has promo_code matched by rules with apply_dsbs_promos flag'
        )
    assert_that(
        offers[WHITE_OFFER_ID_9],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_9,
            'promo': has_entries({
                'shop_promo_ids': contains_inanyorder(
                    offerpromo_promo_code_datacamp.shop_promo_id,
                ),
            }),
        }),
        'white dsbs offer has promo_code from datacamp with apply_dsbs_promos flag'
    )
    assert_that(
        offers[WHITE_OFFER_ID_10],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_10,
            'promo': has_entries({
                'shop_promo_ids': contains_inanyorder(
                    offerpromo_direct_discount_datacamp.shop_promo_id,
                    offerpromo_blue_flash_datacamp.shop_promo_id,
                ),
            }),
        }),
        'white dsbs offer has direct_discount and blue_flash from datacamp with apply_dsbs_promos flag'
    )
    assert_that(
        offers[WHITE_OFFER_ID_6],
        has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': WHITE_OFFER_ID_6,
            'promo': has_entries({
                'shop_promo_ids': contains_inanyorder(
                    promo_details_blue_set_with_rules.shop_promo_id,
                    'promo_code_shop_promo_id',
                ),
            }),
        }),
        'white dsbs offer got blue_set promo matched by rule'
    )

    for offer_id in [WHITE_OFFER_ID_4, WHITE_OFFER_ID_5]:
        assert_that(
            offers[offer_id],
            all_of(
                has_entries({
                    'feed_id': WHITE_FEED_ID,
                    'offer_id': offer_id,
                }),
                is_not(has_key('promo'))
            ),
            'white offer {} does not have promo for any value of apply_dsbs_promos flag'.format(offer_id)
        )
