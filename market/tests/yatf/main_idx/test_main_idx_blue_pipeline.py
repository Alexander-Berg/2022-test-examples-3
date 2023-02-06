# -*- coding: utf-8 -*-

'''
Тест для проверки синего pipeline
'''

import pytest
import datetime
import time
from hamcrest import assert_that

from collections import namedtuple
from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.express_table import YtExpressTable
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.blue_promo_table import (
    BluePromoTable,
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
    WAREHOUSE_145,
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.proto.feedparser.Promo_pb2 import (
    OfferPromo,
    PromoDetails,
    Predicate
)
from market.proto.common.common_pb2 import ESupplierFlag
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
BLUE_FEED_ID_2 = 2222
BLUE_FEED_ID_3 = 3333
BLUE_FEED_ID_4 = 4444
BLUE_FEED_ID_5 = 5555
BLUE_FEED_ID_10 = 10

MSKU_FEED_ID = 475690

ROOT_HID = 90401
NON_LEAF_HID_1 = 80400
NON_LEAF_HID_2 = 80401
DEFAULT_LEAF_HID = 123456
LEAF_HID_1 = 102030
LEAF_HID_2 = 102031
LEAF_HID_3 = 102032
LEAF_HID_4 = 102033
LEAF_HID_5 = 102034
LEAF_HID_6 = 102035
LEAF_HID_7 = 102036
LEAF_HID_8 = 102037
LEAF_HID_9 = 102038
LEAF_HID_10 = 102039
NON_LEAF_FOR_HID_LOGIC=10204
LEAF_1_FOR_HID_LOGIC=102041
LEAF_2_FOR_HID_LOGIC=102042
LEAF_3_FOR_HID_LOGIC=102043

MSKU_FOID1 = 200001
MSKU_FOID2 = 200002
MSKU_FOID3 = 200003
MSKU_FOID4 = 200004
MSKU_FOID5 = 200005
MSKU_FOID6 = 200006
BLUE_CASHBACK_MSKU_1 = 198773
BLUE_CASHBACK_MSKU_HID_EXCLUDED = 198774
BLUE_CASHBACK_MSKU_HID_INCLUDED = 198775
BLUE_CASHBACK_MSKU_HID_INCLUDED_FOR_1 = 198776
PROMOCODE_MSKU_1 = 201001
PROMOCODE_MSKU_2 = 201003
PROMOCODE_MSKU_3 = 201004
DD_DATACAMP_MSKU = 201002
BLUE_FLASH_MSKU = 104
PERSONAL_PROMOCODE_MSKU = 201007
MSKU_WAREHOUSE_EXPRESS = 201008
MSKU_WAREHOUSE_NORMAL = 201009
MSKU_WAREHOUSE_ID_1 = 201010
MSKU_WAREHOUSE_ID_2 = 201011
MSKU_DSBS = 201012
MSKU_NOT_DSBS = 201013

SUPPLIER_ID_1 = 4001
SUPPLIER_ID_2 = 4002

# дефолтный вендор всего
ACME_CORP_VENDOR = 1923
VENDOR_ID_1 = 5001
VENDOR_ID_2 = 5002
WAREHOUSE_ID_EXPRESS = 10001
WAREHOUSE_ID_NORMAL = 10002
WAREHOUSE_ID_DSBS = 10004
WAREHOUSE_ID_1 = 10003
WAREHOUSE_ID_2 = 10004


def __make_blue_offer(feed, msku, offer_id, supplier_id, warehouse_id):
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
            warehouse_id=warehouse_id,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
        ),
    }

offersdata = [
    {
        'msku': 1,
        'offers': [
            # оффер без промо
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'no_promo_1',
            },
        ]
    },
    {
        'msku': 2,
        'offers': [
            # оффер без промо
            {
                'feed_id': BLUE_FEED_ID_2,
                'offer_id': 'no_promo_2'
            },
            # офферы для промо 'generic_bundle'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_gb_1_primary'
            }
        ]
    },
    {
        'msku': 3,
        'hid': LEAF_HID_1,
        'offers': [
            # офферы для промо 'direct_discount'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'direct_discount_1'
            },
        ]
    },
    {
        'msku': 4,
        'offers': [
            # оффер для промо 'generic_bundle'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_gb_1_secondary'
            },
        ]
    },
    {
        'msku': 5,
        'hid': LEAF_HID_4,
        'offers': [
            # офферы для промо 'direct_discount'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'direct_discount_2'
            },
        ]
    },
    {
        'msku': 6,
        'hid': LEAF_HID_5,
        'offers': [
            # офферы для промо 'direct_discount'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'direct_discount_3'
            },
        ]
    },
    {
        'msku': 7,
        'hid': LEAF_HID_6,
        'offers': [
            # офферы для промо 'direct_discount'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'direct_discount_4'
            },
        ]
    },
    {
        'msku': 8,
        'hid': LEAF_HID_7,
        'offers': [
            # офферы, участвующие одновременно в промо 'direct_discount','blue_cashback', 'promocode'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'multiple_promos_overlap'
            },
        ]
    },
    {
        'msku': 10,
        'offers': [
            # оффер для промо 'cheapest_ag_gift'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_cag_10'
            },
        ]
    },
    {
        'msku': 11,
        'offers': [
            # оффер для промо 'cheapest_ag_gift'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_cag_11'
            },
        ]
    },
    {
        'msku': 12,
        'offers': [
            # оффер для промо 'cheapest_ag_gift'
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_cag_12'
            },
        ]
    },
    {
        'msku': 20,
        'offers': [
            # оффер для промо datacamp
            {
                'feed_id': BLUE_FEED_ID_10,
                'offer_id': 'promo_cag_20'
            },
        ]
    },
    {
        'msku': 100,
        'offers': [
            {
                # оффер для комбинации промо
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_combo'
            },
        ]
    },
    {
        'msku': 101,
        'hid': LEAF_HID_2,
        'offers': [
            {
                # оффер для промо 'blue_cashback'
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'blue_cashback_1'
            },
        ]
    },
    {
        'msku': 102,
        'hid': LEAF_HID_3,
        'offers': [
            {
                # оффер для промо 'blue_cashback'
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'blue_cashback_2'
            },
        ]
    },
    {
        'msku': 103,
        'hid': LEAF_HID_8,
        'offers': [
            {
                # оффер для промо 'promocode'
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'blue_promocode_1'
            },
        ]
    },
    {
        'msku': BLUE_FLASH_MSKU,
        'offers': [
            {
                # оффер для проверки допустимых типов промо с правилами
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_with_flash_with_rules'
            },
        ]
    },
    {
        'msku': MSKU_FOID1,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid1'}],
    },
    {
        'msku': MSKU_FOID2,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid2'}],
    },
    {
        'msku': MSKU_FOID3,
        'hid': LEAF_HID_9,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid3_excluded'}],
    },
    {
        'msku': MSKU_FOID4,
        'hid': LEAF_HID_9,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid4'}],
    },
    {
        'msku': MSKU_FOID5,
        'hid': LEAF_HID_10,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid5'}],
    },
    {
        'msku': MSKU_FOID6,
        'hid': LEAF_HID_10,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'foid6'}],
    },
    {
        'msku': DD_DATACAMP_MSKU,
        'hid': LEAF_HID_10,
        'offers': [{'feed_id': BLUE_FEED_ID, 'offer_id': 'dd_datacamp_offer_id'}],
    },
    {
        'msku': BLUE_CASHBACK_MSKU_1,
        'offers': [
            {
                # оффер для промо 'blue_cashback'
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'blue_cashback_3'
            },
        ]
    },
    {
        'msku': 200,
        'offers': [
            {
                # оффер для белого промо - промо обрабатываться не должно
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_white'
            },
        ]
    },
    {
        'msku': BLUE_CASHBACK_MSKU_HID_INCLUDED,
        'hid': LEAF_1_FOR_HID_LOGIC,
        'offers': [
            {
                # синий кэшбэк в разрешённой для акции категории
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_for_hid_included'
            },
        ]
    },
    {
        'msku': BLUE_CASHBACK_MSKU_HID_EXCLUDED,
        'hid': LEAF_2_FOR_HID_LOGIC,
        'offers': [
            {
                # синий кэшбэк в запрещённой во всех условиях категории
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_for_hid_excluded'
            },
        ]
    },
    {
        'msku': BLUE_CASHBACK_MSKU_HID_INCLUDED_FOR_1,
        'hid': LEAF_3_FOR_HID_LOGIC,
        'offers': [
            {
                # синий оффер в категории, разрешённой в одном из правил
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_for_hid_excluded_for_one'
            },
        ]
    },
    {
        'msku': 654321,
        'offers': [
            {
                # синий оффер, к которому будет привязан direct discount через feed_offer_id
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_dd_by_feedofferid'
            },
        ]
    },
    {
        'msku': PROMOCODE_MSKU_1,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_with_supplier_id_1',
                'supplier_id': SUPPLIER_ID_1,
            },
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_with_supplier_id_2',
                'supplier_id': SUPPLIER_ID_2,
            }
        ]
    },
    {
        'msku': PROMOCODE_MSKU_2,
        'vendor': VENDOR_ID_1,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_with_vendor_id_1',
            },
        ]
    },
    {
        'msku': PROMOCODE_MSKU_3,
        'vendor': VENDOR_ID_2,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'promo_with_excluded_vendor_id_2',
            },
        ]
    },
    {
        'msku': PERSONAL_PROMOCODE_MSKU,
        'vendor': ACME_CORP_VENDOR,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_with_personal_promocode',
            },
        ]
    },
    {
        'msku': MSKU_WAREHOUSE_EXPRESS,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID_2,
                'offer_id': 'offer_with_express_warehouse',
                'warehouse_id': WAREHOUSE_ID_EXPRESS,
            },
        ]
    },
    {
        'msku': MSKU_WAREHOUSE_NORMAL,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_with_normal_warehouse',
                'warehouse_id': WAREHOUSE_ID_NORMAL,
            },
        ]
    },
    {
        'msku': MSKU_WAREHOUSE_ID_1,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID_3,
                'offer_id': 'offer_with_included_warehouse_1',
                'warehouse_id': WAREHOUSE_ID_1,
            },
        ]
    },
    {
        'msku': MSKU_WAREHOUSE_ID_2,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID_4,
                'offer_id': 'offer_with_excluded_warehouse_2',
                'warehouse_id': WAREHOUSE_ID_2,
            },
        ]
    },
    {
        'msku': MSKU_DSBS,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID_5,
                'offer_id': 'offer_with_dsbs_supplier',
                'warehouse_id': WAREHOUSE_ID_DSBS,
            },
        ]
    },
    {
        'msku': MSKU_NOT_DSBS,
        'offers': [
            {
                'feed_id': BLUE_FEED_ID,
                'offer_id': 'offer_with_not_dsbs_supplier',
                'warehouse_id': WAREHOUSE_ID_NORMAL,
            },
        ]
    },
]


def get_msku_offers(msku):
    return [(
        offer['feed_id'], msku['msku'], offer['offer_id'], offer.get('supplier_id', None), offer.get('warehouse_id', WAREHOUSE_145)
    ) for offer in msku['offers']]


# feed, msku, offer_id, supplier_id, vendor_id
BLUE_OFFERS = [__make_blue_offer(*offer_params) for offer_params in sum([get_msku_offers(msku) for msku in offersdata],  [])]

BLUE_PROMO_COMBO_GB = OfferPromo(
    shop_promo_id='promo_combo_gb',
    shop_promo_ids=['promo_combo_gb'],
    binary_promo_md5='BLUE_PROMO_COMBO_GB',
    binary_promo_md5s=['BLUE_PROMO_COMBO_GB'],
    promo_type=PromoType.GENERIC_BUNDLE,
    )

BLUE_PROMO_COMBO_CAG = OfferPromo(
    shop_promo_id='promo_combo_cag',
    shop_promo_ids=['promo_combo_cag'],
    binary_promo_md5='BLUE_PROMO_COMBO_CAG',
    binary_promo_md5s=['BLUE_PROMO_COMBO_CAG'],
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    )

WHITE_PROMO_GWP = OfferPromo(
    shop_promo_id='white_promo_gwp',
    shop_promo_ids=['white_promo_gwp'],
    binary_promo_md5='WHITE_PROMO_GWP',
    binary_promo_md5s=['WHITE_PROMO_GWP'],
    promo_type=PromoType.GIFT_WITH_PURCHASE,
    )

BLUE_PROMO_CASHBACK_HID_EXCLUDED = OfferPromo(
    shop_promo_id='blue_cashback_nonleaf_hid_excluded',
    shop_promo_ids=['blue_cashback_nonleaf_hid_excluded'],
    binary_promo_md5='blue_cashback_nonleaf_hid_excluded',
    binary_promo_md5s=['blue_cashback_nonleaf_hid_excluded'],
    promo_type=PromoType.BLUE_CASHBACK,
)


OFFER_PROMO_FOID = OfferPromo(
    shop_promo_id='pd_foid',
    shop_promo_ids=['pd_foid'],
    binary_promo_md5='pd_foid',
    binary_promo_md5s=['pd_foid'],
    promo_type=PromoType.DIRECT_DISCOUNT,
)


PROMO_DETAILS_FOID = PromoDetails(
    shop_promo_id='pd_foid',
    binary_promo_md5='pd_foid',
    type=PromoType.DIRECT_DISCOUNT,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid1'),
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid2'),
                ]
            )
        ),
    ]
)


BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED = PromoDetails(
    shop_promo_id='promocode_feed_offer_id_excluded_shop_promo_id',
    binary_promo_md5='promocode_feed_offer_id_excluded_md5',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_9,
                ]
            ),
            excluded_feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid3_excluded'),
                ]
            )
        ),
    ]
)


OFFER_PROMO_PROMOCODE_FEED_OFFER_ID_EXCLUDED = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED.shop_promo_id],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED.binary_promo_md5],
    promo_type=PromoType.PROMO_CODE,
)


BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY = PromoDetails(
    shop_promo_id='direct_discount_leaf_category_shop_promo_id',
    binary_promo_md5='direct_discount_leaf_category_binary_promo_md5',
    type=PromoType.DIRECT_DISCOUNT,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_1,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY = PromoDetails(
    shop_promo_id='direct_discount_nonleaf_category_shop_promo_id',
    binary_promo_md5='direct_discount_nonleaf_category_binary_promo_md5',
    type=PromoType.DIRECT_DISCOUNT,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    NON_LEAF_HID_2,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP = PromoDetails(
    shop_promo_id='direct_discount_for_leaf_category_with_multiple_promos_overlap_shop_promo_id',
    binary_promo_md5='direct_discount_for_leaf_category_with_multiple_promos_overlap_binary_promo_md5',
    type=PromoType.DIRECT_DISCOUNT,
    direct_discount=PromoDetails.DirectDiscount(
        discounts_by_category=[
            PromoDetails.DirectDiscount.DiscountByCategory(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        LEAF_HID_7,
                    ]
                ),
                discount_percent=10
            ),
        ]
    )
)

BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP = PromoDetails(
    shop_promo_id='blue_cashback_leaf_category_with_multiple_promos_overlap_shop_promo_id',
    binary_promo_md5='blue_cashback_leaf_category_with_multiple_promos_overlap_binary_promo_md5',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_7,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP = PromoDetails(
    shop_promo_id='blue_promocode_leaf_category_with_multiple_promos_overlap_shop_promo_id',
    binary_promo_md5='blue_promocode_leaf_category_with_multiple_promos_overlap_binary_promo_md5',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_7,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY = PromoDetails(
    shop_promo_id='promocode_leaf_category_shop_promo_id',
    binary_promo_md5='promocode_leaf_category_binary_promo_md5',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_8,
                ]
            )
        ),
    ]
)


BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES = PromoDetails(
    shop_promo_id='direct_discount_different_discounts_for_leaf_categories_shop_promo_id',
    binary_promo_md5='direct_discount_different_discounts_for_leaf_categories_binary_promo_md5',
    type=PromoType.DIRECT_DISCOUNT,
    direct_discount=PromoDetails.DirectDiscount(
        discounts_by_category=[
            PromoDetails.DirectDiscount.DiscountByCategory(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        LEAF_HID_5,
                    ]
                ),
                discount_percent=10
            ),
            PromoDetails.DirectDiscount.DiscountByCategory(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        LEAF_HID_6,
                    ]
                ),
                discount_percent=15
            )
        ]
    )
)

BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY = PromoDetails(
    shop_promo_id='blue_cashback_leaf_category_shop_promo_id',
    binary_promo_md5='blue_cashback_leaf_category_binary_promo_md5',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    LEAF_HID_2,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_CASHBACK_NONLEAF_CATEGORY = PromoDetails(
    shop_promo_id='blue_cashback_nonleaf_category_shop_promo_id',
    binary_promo_md5='blue_cashback_nonleaf_category_binary_promo_md5',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    NON_LEAF_HID_1,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_CASHBACK_MSKU_1 = PromoDetails(
    shop_promo_id='blue_cashback_msku1_shop_promo_id',
    binary_promo_md5='blue_cashback_msku1_binary_promo_md5',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    BLUE_CASHBACK_MSKU_1
                ]
            )
        ),
    ]
)

BLUE_PROMO_DETAILS_CASHBACK_HID_EXCLUDED = PromoDetails(
    shop_promo_id='blue_cashback_nonleaf_hid_excluded',
    binary_promo_md5='blue_cashback_nonleaf_hid_excluded',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    NON_LEAF_FOR_HID_LOGIC,
                ],
                excluded_categories=[
                    LEAF_2_FOR_HID_LOGIC,
                    LEAF_3_FOR_HID_LOGIC,
                ]
            )
        ),
        PromoDetails.OffersMatchingRule(
            category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                categories=[
                    NON_LEAF_FOR_HID_LOGIC,
                ],
                excluded_categories=[
                    LEAF_2_FOR_HID_LOGIC,
                ]
            )
        ),
    ]
)

BLUE_PROMO_DATACAMP_CAG = OfferPromo(
    shop_promo_id='promo_datacamp_cag',
    shop_promo_ids=['promo_datacamp_cag'],
    binary_promo_md5='BLUE_PROMO_DATACAMP_CAG_10',
    binary_promo_md5s=['BLUE_PROMO_DATACAMP_CAG_10'],
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    )


# промо с пустыми правилами не должна привязываться через механизм правил
BLUE_PROMO_DETAILS_NO_RULES = PromoDetails(
    shop_promo_id='blue_cashback_empty_rules_no_offers',
    binary_promo_md5='blue_cashback_empty_rules_no_offers',
    type=PromoType.BLUE_CASHBACK,
    offers_matching_rules=[]
)

# промо с ограничением по supplier_id
BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID = PromoDetails(
    shop_promo_id='blue_promo_promocode_supplier_id',
    binary_promo_md5='blue_promo_promocode_supplier_id',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    PROMOCODE_MSKU_1,
                ]
            ),
            suppliers=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    SUPPLIER_ID_1,
                ]
            )
        ),
    ]
)

# промо с ограничением по vendor_id
BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID = PromoDetails(
    shop_promo_id='blue_promo_promocode_vendor_id',
    binary_promo_md5='blue_promo_promocode_vendor_id',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    PROMOCODE_MSKU_2,
                ]
            ),
            vendors=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    VENDOR_ID_1,
                ]
            )
        ),
    ]
)

# промо с исключением по vendor_id
BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID = PromoDetails(
    shop_promo_id='blue_promo_promocode_excluded_vendor_id',
    binary_promo_md5='blue_promo_promocode_excluded_vendor_id',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    PROMOCODE_MSKU_3,
                ]
            ),
            excluded_vendors=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    VENDOR_ID_2,
                ]
            )
        ),
    ]
)

# промо с ограничением по express warehouse
BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS = PromoDetails(
    shop_promo_id='blue_promo_promocode_express',
    binary_promo_md5='blue_promo_promocode_express',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_WAREHOUSE_EXPRESS,
                    MSKU_WAREHOUSE_NORMAL,
                ]
            ),
            supplier_flag_restriction=PromoDetails.OffersMatchingRule.SupplierFlagRestriction(
                supplier_flags=ESupplierFlag.EXPRESS_WAREHOUSE,
            )
        ),
    ]
)

# промо с исключением по express warehouse
BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS = PromoDetails(
    shop_promo_id='blue_promo_promocode_excluded_express',
    binary_promo_md5='blue_promo_promocode_excluded_express',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_WAREHOUSE_EXPRESS,
                    MSKU_WAREHOUSE_NORMAL,
                ]
            ),
            supplier_flag_restriction=PromoDetails.OffersMatchingRule.SupplierFlagRestriction(
                excluded_supplier_flags=ESupplierFlag.EXPRESS_WAREHOUSE,
            )
        ),
    ]
)

# промо с ограничением по warehouse
BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID = PromoDetails(
    shop_promo_id='blue_promo_promocode_warehouse_id',
    binary_promo_md5='blue_promo_promocode_warehouse_id',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_WAREHOUSE_ID_1,
                    MSKU_WAREHOUSE_ID_2,
                ]
            ),
            warehouses=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    WAREHOUSE_ID_1,
                ]
            )
        ),
    ]
)

# промо с ограничением по excluded warehouse
BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID = PromoDetails(
    shop_promo_id='blue_promo_promocode_excluded_warehouse_id',
    binary_promo_md5='blue_promo_promocode_excluded_warehouse_id',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_WAREHOUSE_ID_1,
                    MSKU_WAREHOUSE_ID_2,
                ]
            ),
            excluded_warehouses=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    WAREHOUSE_ID_1,
                ]
            )
        ),
    ]
)

# промо с ограничением по dsbs supplier
BLUE_PROMO_DETAILS_PROMOCODE_DSBS = PromoDetails(
    shop_promo_id='blue_promo_promocode_dsbs',
    binary_promo_md5='blue_promo_promocode_dsbs',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_DSBS,
                    MSKU_NOT_DSBS,
                ]
            ),
            supplier_flag_restriction=PromoDetails.OffersMatchingRule.SupplierFlagRestriction(
                supplier_flags=ESupplierFlag.DSBS_SUPPLIER,
            )
        ),
    ]
)

# промо с исключением по dsbs supplier
BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS = PromoDetails(
    shop_promo_id='blue_promo_promocode_excluded_dsbs',
    binary_promo_md5='blue_promo_promocode_excluded_dsbs',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    MSKU_DSBS,
                    MSKU_NOT_DSBS,
                ]
            ),
            supplier_flag_restriction=PromoDetails.OffersMatchingRule.SupplierFlagRestriction(
                excluded_supplier_flags=ESupplierFlag.DSBS_SUPPLIER,
            )
        ),
    ]
)

# Персональный промокод с привязкой по msku
PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU = PromoDetails(
    shop_promo_id='personal_promocode_by_msku_shop_promo_id',
    binary_promo_md5='personal_promocode_by_msku_md5',
    type=PromoType.PROMO_CODE,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[
                    PERSONAL_PROMOCODE_MSKU,
                ]
            ),
        ),
    ]
)


PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY = PromoDetails(
    shop_promo_id='direct_discount_with_subsidy',
    binary_promo_md5='direct_discount_with_subsidy',
    type=PromoType.DIRECT_DISCOUNT,
    direct_discount=PromoDetails.DirectDiscount(
        items=[
            PromoDetails.DirectDiscount.Item(
                feed_id=BLUE_FEED_ID,
                offer_id="promo_dd_with_subsidy",
                discount_price=PromoDetails.Money(value=900, currency='RUR'),
                old_price=PromoDetails.Money(value=1000, currency='RUR'),
                subsidy=PromoDetails.Money(value=100, currency='RUR'),
                max_discount_percent=12.5,
                max_discount=PromoDetails.Money(value=200, currency='RUR')
            ),
        ]
    ),
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid5'),
                ]
            )
        ),
    ]
)

PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN = PromoDetails(
    shop_promo_id='direct_discount_yandex_plus_login',
    binary_promo_md5='direct_discount_yandex_plus_login',
    type=PromoType.DIRECT_DISCOUNT,
    direct_discount=PromoDetails.DirectDiscount(
        items=[
            PromoDetails.DirectDiscount.Item(
                feed_id=BLUE_FEED_ID,
                offer_id="promo_dd_yandex_plus_login",
                discount_price=PromoDetails.Money(value=900, currency='RUR'),
                old_price=PromoDetails.Money(value=1000, currency='RUR'),
                max_discount_percent=12.5,
                max_discount=PromoDetails.Money(value=200, currency='RUR')
            ),
        ]
    ),
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='foid6'),
                ]
            )
        ),
    ],
    restrictions=PromoDetails.Restrictions(
        predicates=[Predicate(
            perks=['yandex_plus']
        )]
    )
)

PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP = PromoDetails(
    shop_promo_id='direct_discount_datacamp',
    binary_promo_md5='direct_discount_datacamp',
    type=PromoType.DIRECT_DISCOUNT,
    direct_discount=PromoDetails.DirectDiscount(
        items=[
            PromoDetails.DirectDiscount.Item(
                feed_id=BLUE_FEED_ID,
                offer_id="dd_datacamp_offer_id",
                discount_price=PromoDetails.Money(value=920, currency='RUR'),
                old_price=PromoDetails.Money(value=1020, currency='RUR'),
            ),
        ]
    ),
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                ids=[
                    PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='dd_datacamp_offer_id'),
                ]
            )
        ),
    ]
)

# бессмысленная акция для проверки блокировки
# использования правил привязки по типу
PROMO_DETAILS_BLUE_FLASH_BY_RULES = PromoDetails(
    shop_promo_id='flash_discount_with_rule',
    binary_promo_md5='flash_discount_with_rule',
    type=PromoType.BLUE_FLASH,
    offers_matching_rules=[
        PromoDetails.OffersMatchingRule(
            mskus=PromoDetails.OffersMatchingRule.IdsList(
                ids=[BLUE_FLASH_MSKU]
            )
        ),
    ]
)


OFFER_PROMO_DIRECT_DISCOUNT_DATACAMP = OfferPromo(
    shop_promo_id=PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP.shop_promo_id, ],
    binary_promo_md5=PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP.binary_promo_md5, ],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

OFFER_PROMO_DIRECT_DISCOUNT_WITH_SUBSIDY = OfferPromo(
    shop_promo_id=PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY.shop_promo_id, ],
    binary_promo_md5=PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY.binary_promo_md5, ],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

OFFER_PROMO_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN = OfferPromo(
    shop_promo_id=PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN.shop_promo_id, ],
    binary_promo_md5=PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN.binary_promo_md5, ],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

BLUE_OFFER_PROMO_PROMOCODE_SUPPLIER_ID = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_VENDOR_ID = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_VENDOR_ID = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_EXPRESS = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_EXPRESS = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_BY_WAREHOUSE_ID = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_DSBS = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_DSBS.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_DSBS.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_DSBS.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_DSBS.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_DSBS = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS.shop_promo_id, ],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

OFFER_PROMO_PERSONAL_PROMOCODE_BY_MSKU = OfferPromo(
    shop_promo_id=PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU.shop_promo_id, ],
    binary_promo_md5=PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU.binary_promo_md5, ],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_CASHBACK_LEAF_CATEGORY_RESULT = OfferPromo(
    shop_promo_id='blue_cashback_leaf_category_shop_promo_id',
    shop_promo_ids=['blue_cashback_leaf_category_shop_promo_id', ],
    binary_promo_md5='blue_cashback_leaf_category_binary_promo_md5',
    binary_promo_md5s=['blue_cashback_leaf_category_binary_promo_md5', ],
    promo_type=PromoType.BLUE_CASHBACK,
)

BLUE_CASHBACK_MSKU_1_RESULT = OfferPromo(
    shop_promo_id='blue_cashback_msku1_shop_promo_id',
    shop_promo_ids=['blue_cashback_msku1_shop_promo_id', ],
    binary_promo_md5='blue_cashback_msku1_binary_promo_md5',
    binary_promo_md5s=['blue_cashback_msku1_binary_promo_md5', ],
    promo_type=PromoType.BLUE_CASHBACK,
)

BLUE_CASHBACK_NONLEAF_CATEGORY_RESULT = OfferPromo(
    shop_promo_id='blue_cashback_nonleaf_category_shop_promo_id',
    shop_promo_ids=['blue_cashback_nonleaf_category_shop_promo_id', ],
    binary_promo_md5='blue_cashback_nonleaf_category_binary_promo_md5',
    binary_promo_md5s=['blue_cashback_nonleaf_category_binary_promo_md5', ],
    promo_type=PromoType.BLUE_CASHBACK,
)

BLUE_OFFER_PROMO_DIRECT_DISCOUNT_LEAF_CATEGORY = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY.shop_promo_id],
    binary_promo_md5=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY.binary_promo_md5],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

BLUE_OFFER_PROMO_DIRECT_DISCOUNT_NONLEAF_CATEGORY = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY.shop_promo_id],
    binary_promo_md5=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY.binary_promo_md5],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

BLUE_OFFER_PROMO_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES.shop_promo_id],
    binary_promo_md5=BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES.binary_promo_md5],
    promo_type=PromoType.DIRECT_DISCOUNT,
)

BLUE_OFFER_PROMO_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP= OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.shop_promo_id,
    shop_promo_ids=[
        BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.shop_promo_id,
        BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.shop_promo_id,
        BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.shop_promo_id,
    ],
    binary_promo_md5=BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.binary_promo_md5,
    binary_promo_md5s=[
        BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.binary_promo_md5,
        BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.binary_promo_md5,
        BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.binary_promo_md5,
    ],
    promo_type=PromoType.DIRECT_DISCOUNT | PromoType.BLUE_CASHBACK | PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_PROMOCODE_LEAF_CATEGORY = OfferPromo(
    shop_promo_id=BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY.shop_promo_id,
    shop_promo_ids=[BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY.shop_promo_id],
    binary_promo_md5=BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY.binary_promo_md5,
    binary_promo_md5s=[BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY.binary_promo_md5],
    promo_type=PromoType.PROMO_CODE,
)

BLUE_OFFER_PROMO_BLUE_FLASH_BY_RULES = OfferPromo(
    shop_promo_id=PROMO_DETAILS_BLUE_FLASH_BY_RULES.shop_promo_id,
    shop_promo_ids=[PROMO_DETAILS_BLUE_FLASH_BY_RULES.shop_promo_id],
    binary_promo_md5=PROMO_DETAILS_BLUE_FLASH_BY_RULES.binary_promo_md5,
    binary_promo_md5s=[PROMO_DETAILS_BLUE_FLASH_BY_RULES.binary_promo_md5],
    promo_type=PROMO_DETAILS_BLUE_FLASH_BY_RULES.type,
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
        MboCategory(
            hid=LEAF_HID_1,
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
            hid=NON_LEAF_HID_1,
            tovar_id=3,
            parent_hid=ROOT_HID,
            unique_name="Non leaf hid 1",
            name="Non leaf hid 1",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_3,
            tovar_id=4,
            parent_hid=NON_LEAF_HID_1,
            unique_name="Leaf hid 3",
            name="Leaf hid 3",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=NON_LEAF_HID_2,
            tovar_id=5,
            parent_hid=ROOT_HID,
            unique_name="Non leaf hid 2",
            name="Non leaf hid 2",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_4,
            tovar_id=6,
            parent_hid=NON_LEAF_HID_2,
            unique_name="Leaf hid 4",
            name="Leaf hid 4",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_5,
            tovar_id=7,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 5",
            name="Leaf hid 5",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_6,
            tovar_id=8,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 6",
            name="Leaf hid 6",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_7,
            tovar_id=80,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 7",
            name="Leaf hid 7",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_8,
            tovar_id=81,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 8",
            name="Leaf hid 8",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_9,
            tovar_id=82,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 9",
            name="Leaf hid 9",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_HID_10,
            tovar_id=83,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 10",
            name="Leaf hid 10",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=NON_LEAF_FOR_HID_LOGIC,
            tovar_id=9,
            parent_hid=ROOT_HID,
            unique_name="Root for Hid Logic Test",
            name="Root for Hid Logic Test",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_1_FOR_HID_LOGIC,
            tovar_id=91,
            parent_hid=NON_LEAF_FOR_HID_LOGIC,
            unique_name="Leaf for hid logic 1",
            name="Leaf for hid logic 1",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_2_FOR_HID_LOGIC,
            tovar_id=92,
            parent_hid=NON_LEAF_FOR_HID_LOGIC,
            unique_name="Leaf for hid logic 2",
            name="Leaf for hid logic 2",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=LEAF_3_FOR_HID_LOGIC,
            tovar_id=93,
            parent_hid=NON_LEAF_FOR_HID_LOGIC,
            unique_name="Leaf for hid logic 3",
            name="Leaf for hid logic 3",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture()
def express_table_data():
    return [
        {
            'warehouse_id': WAREHOUSE_ID_EXPRESS,
            'business_id': WAREHOUSE_ID_EXPRESS,
        },
    ]


@pytest.fixture()
def pipeline_params():
    return PipelineParams(shards=1, blue_shards=1, white_offer_count=0, blue_offer_count=len(BLUE_OFFERS))


@pytest.fixture(scope='module')
def collected_promo_details_output_dir():
    return 'collected_promo_details'


@pytest.fixture()
def or3_config(yt_server, collected_promo_details_output_dir):
    home_dir = get_yt_prefix()
    blue_promo_table_datacamp = ypath_join(home_dir, 'promos', 'blue', 'map_reduce_in_datacamp', 'recent')
    express_table = ypath_join(home_dir, 'combinator', 'graph', 'yt_express_warehouse')

    misc = {
        'blue_promo_reduce_enabled': True,
        'match_by_express_flag': True,
    }

    return Or3Config(**{
        'yt': {
            'home_dir': home_dir,
            'yt_blue_promo_table_datacamp': blue_promo_table_datacamp,
            'yt_express_table': express_table,
            'yt_collected_promo_details_output_dir': collected_promo_details_output_dir,
        },
        'misc': misc,
    })


@pytest.fixture()
def shops_dat():
    return ShopsDat([
        {"datafeed_id": BLUE_FEED_ID, "warehouse_id": WAREHOUSE_ID_NORMAL},
        {"datafeed_id": BLUE_FEED_ID_2, "warehouse_id": WAREHOUSE_ID_EXPRESS},
        {"datafeed_id": BLUE_FEED_ID_3, "warehouse_id": WAREHOUSE_ID_1},
        {"datafeed_id": BLUE_FEED_ID_4, "warehouse_id": WAREHOUSE_ID_2},
        {"datafeed_id": BLUE_FEED_ID_5, "warehouse_id": WAREHOUSE_ID_DSBS, "is_dsbs": True},
        {"datafeed_id": BLUE_FEED_ID_10},
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
                vendor_id=msku.get("vendor", ACME_CORP_VENDOR)
            ),
        }
        for msku in offersdata
    ]


@pytest.yield_fixture()
def blue_promo_data_datacamp():
    return sorted([
        {'feed_id': BLUE_FEED_ID_10, 'offer_id': 'promo_cag_20', 'promo': BLUE_PROMO_DATACAMP_CAG.SerializeToString()},
    ])


@pytest.yield_fixture()
def collected_promo_details_data():
    return sorted([
        {
            'promo_id': 'blue_promo_promocode_excluded_express',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_EXPRESS.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_excluded_vendor_id',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_VENDOR_ID.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_excluded_warehouse_id',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_dsbs',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_DSBS.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_excluded_dsbs',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_EXCLUDED_DSBS.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_express',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_EXPRESS.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_supplier_id',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_SUPPLIER_ID.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_vendor_id',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_VENDOR_ID.SerializeToString()
        },
        {
            'promo_id': 'blue_promo_promocode_warehouse_id',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_BY_WAREHOUSE_ID.SerializeToString()
        },
        {
            'promo_id': 'pd_foid',
            'promo': PROMO_DETAILS_FOID.SerializeToString(),
        },
        {
            'promo_id': 'promo_direct_discount_1',
            'promo': BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_LEAF_CATEGORY.SerializeToString(),
        },
        {
            'promo_id': 'promo_direct_discount_2',
            'promo': BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_NONLEAF_CATEGORY.SerializeToString(),
        },
        {
            'promo_id': 'promo_direct_discount_3',
            'promo': BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES.SerializeToString(),
        },
        {
            'promo_id': 'promo_direct_discount_4',
            'promo': BLUE_PROMO_DETAILS_DIRECT_DISCOUNT_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.SerializeToString(),
        },
        {
            'promo_id': 'promo_promocode_feed_offer_id_excluded',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_FEED_OFFER_ID_EXCLUDED.SerializeToString(),
        },
        {
            'promo_id': 'promo_zflash_by_rules',
            'promo': PROMO_DETAILS_BLUE_FLASH_BY_RULES.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_1',
            'promo': BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_2',
            'promo': BLUE_PROMO_DETAILS_CASHBACK_NONLEAF_CATEGORY.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_3',
            'promo': BLUE_PROMO_DETAILS_CASHBACK_MSKU_1.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_empty_rules_no_offers',
            'promo': BLUE_PROMO_DETAILS_NO_RULES.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_hid_excluded',
            'promo': BLUE_PROMO_DETAILS_CASHBACK_HID_EXCLUDED.SerializeToString(),
        },
        {
            'promo_id': 'blue_cashback_leaf_category_overlap_with_direct_discount',
            'promo': BLUE_PROMO_DETAILS_CASHBACK_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.SerializeToString(),
        },
        {
            'promo_id': 'blue_promocode_1',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY.SerializeToString(),
        },
        {
            'promo_id': 'blue_promocode_2',
            'promo': BLUE_PROMO_DETAILS_PROMOCODE_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP.SerializeToString(),
        },
        {
            'promo_id': 'direct_discount_with_subsidy',
            'promo': PROMO_DETAILS_DIRECT_DISCOUNT_WITH_SUBSIDY.SerializeToString(),
        },
        {
            'promo_id': 'direct_discount_datacamp',
            'promo': PROMO_DETAILS_DIRECT_DISCOUNT_DATACAMP.SerializeToString(),
        },
        {
            'promo_id': 'direct_discount_yandex_plus_login',
            'promo': PROMO_DETAILS_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN.SerializeToString(),
        },
        {
            'promo_id': 'personal_promocode_by_msku',
            'promo': PROMO_DETAILS_PERSONAL_PROMOCODE_BY_MSKU.SerializeToString(),
        },
    ], key=lambda x: x['promo_id'])


@pytest.yield_fixture()
def source_yt_tables(yt_server,
                     or3_config,
                     source_blue_offers_raw,
                     source_msku_contex,
                     blue_promo_data_datacamp,
                     express_table_data,
                     collected_promo_details_data,
                     collected_promo_details_output_dir
                     ):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
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
        'blue_promo_table_datacamp': BluePromoTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'promos', 'blue', 'map_reduce_in_datacamp', 'recent'),
            data=blue_promo_data_datacamp,
        ),
        'express_table': YtExpressTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'combinator', 'graph', 'yt_express_warehouse'),
            data=express_table_data,
        ),
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, collected_promo_details_output_dir, 'recent'),
            data=collected_promo_details_data,
        ),
    }


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


def test_blue_offers_promos(main_idx):
    result_offers = main_idx.outputs['blue_offers_by_offer_id']

    assert_that(
        result_offers['blue_cashback_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_CASHBACK_LEAF_CATEGORY_RESULT)
        ),
        'BLUE_CASHBACK_LEAF_CATEGORY_RESULT',
    )

    assert_that(
        result_offers['blue_cashback_2']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_CASHBACK_NONLEAF_CATEGORY_RESULT)
        ),
        'BLUE_CASHBACK_NONLEAF_CATEGORY_RESULT',
    )

    assert_that(
        result_offers['blue_cashback_3']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_CASHBACK_MSKU_1_RESULT)
        ),
        'BLUE_CASHBACK_MSKU_1_RESULT',
    )

    assert_that(
        result_offers['direct_discount_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_DIRECT_DISCOUNT_LEAF_CATEGORY)
        ),
        'DIRECT_DISCOUNT_LEAF_CATEGORY',
    )

    assert_that(
        result_offers['direct_discount_2']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_DIRECT_DISCOUNT_NONLEAF_CATEGORY)
        ),
        'DIRECT_DISCOUNT_NONLEAF_CATEGORY',
    )

    assert_that(
        result_offers['direct_discount_3']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES)
        ),
        'DIRECT_DISCOUNT_DIFFERENT_DISCOUNT_FOR_LEAF_CATEGORIES_1',
    )

    assert_that(
        result_offers['direct_discount_4']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_DIRECT_DISCOUNT_DIFFERENT_DISCOUNTS_FOR_LEAF_CATEGORIES)
        ),
        'DIRECT_DISCOUNT_DIFFERENT_DISCOUNT_FOR_LEAF_CATEGORIES_2',
    )

    assert_that(
        result_offers['multiple_promos_overlap']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_FOR_LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP)
        ),
        'LEAF_CATEGORY_WITH_MULTIPLE_PROMOS_OVERLAP'
    )

    assert_that(
        result_offers['blue_promocode_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_LEAF_CATEGORY)
        ),
        'PROMOCODE_LEAF_CATEGORY',
    )

    assert_that(
        result_offers['offer_with_personal_promocode']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_PERSONAL_PROMOCODE_BY_MSKU)
        ),
        'PERSONAL_PROMOCODE_BY_MSKU',
    )

    assert_that(
        result_offers['offer_for_hid_included']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_PROMO_CASHBACK_HID_EXCLUDED)
        ),
        'BLUE_PROMO_CASHBACK_HID_EXCLUDED',
    )

    assert_that(
        result_offers['offer_for_hid_excluded_for_one']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_PROMO_CASHBACK_HID_EXCLUDED)
        ),
        'BLUE_PROMO_CASHBACK_HID_EXCLUDED',
    )

    assert 'promo' not in result_offers['offer_for_hid_excluded']

    assert_that(
        result_offers['foid1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_FOID)
        ),
        'OFFER_PROMO_FOID',
    )

    assert_that(
        result_offers['foid2']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_FOID)
        ),
        'OFFER_PROMO_FOID',
    )

    assert 'promo' not in result_offers['foid3_excluded']

    assert_that(
        result_offers['foid5']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_DIRECT_DISCOUNT_WITH_SUBSIDY)
        ),
        'OFFER_PROMO_DIRECT_DISCOUNT_WITH_SUBSIDY',
    )

    assert_that(
        result_offers['foid6']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN)
        ),
        'OFFER_PROMO_DIRECT_DISCOUNT_YANDEX_PLUS_LOGIN',
    )

    assert_that(
        result_offers['dd_datacamp_offer_id']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_DIRECT_DISCOUNT_DATACAMP)
        ),
        'OFFER_PROMO_DIRECT_DISCOUNT_DATACAMP',
    )

    assert_that(
        result_offers['foid4']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(OFFER_PROMO_PROMOCODE_FEED_OFFER_ID_EXCLUDED)
        ),
        'OFFER_PROMO_PROMOCODE_FEED_OFFER_ID_EXCLUDED',
    )

    assert_that(
        result_offers['promo_with_supplier_id_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_SUPPLIER_ID)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_SUPPLIER_ID',
    )

    assert_that(
        result_offers['promo_with_vendor_id_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_VENDOR_ID)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_VENDOR_ID',
    )

    assert_that(
        result_offers['offer_with_express_warehouse']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_EXPRESS)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_EXPRESS',
    )

    assert_that(
        result_offers['offer_with_normal_warehouse']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_EXPRESS)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_EXPRESS',
    )

    assert_that(
        result_offers['offer_with_included_warehouse_1']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_BY_WAREHOUSE_ID)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_BY_WAREHOUSE_ID',
    )

    assert_that(
        result_offers['offer_with_excluded_warehouse_2']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_BY_EXCLUDED_WAREHOUSE_ID',
    )

    assert_that(
        result_offers['offer_with_dsbs_supplier']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_DSBS)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_DSBS',
    )

    assert_that(
        result_offers['offer_with_not_dsbs_supplier']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_DSBS)
        ),
        'BLUE_OFFER_PROMO_PROMOCODE_EXCLUDED_DSBS',
    )

    assert 'promo' not in result_offers['promo_with_supplier_id_2']

    assert 'promo' not in result_offers['promo_white']

    assert 'promo' not in result_offers['promo_with_excluded_vendor_id_2']

    assert_that(
        result_offers['offer_with_flash_with_rules']['promo'],
        user_friendly_dict_equal_to(
            proto_to_dict(BLUE_OFFER_PROMO_BLUE_FLASH_BY_RULES)
        ),
        'flash_discount_with_rule',
    )
