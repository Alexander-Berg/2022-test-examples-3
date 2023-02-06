# coding: utf-8

import datetime
import pytest
import base64
from collections import namedtuple
from copy import deepcopy
from hamcrest import (
    all_of,
    assert_that,
    equal_to,
    has_entries,
    has_item,
    has_items,
    has_length,
)

from yt.wrapper import ypath_join

from market.idx.datacamp.proto.offer.OfferMapping_pb2 import Mapping as MappingPb
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.blue_promo_table import BluePromoDetailsTable
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueBuyboxElasticityTable,
    BlueDcoUploadTable,
    BlueGoldenMatrixTable,
    BlueHistoryPriceDropsTable,
    BlueHistoryPriceTable,
    BlueOffersRawTable,
    BluePriceLimitTable,
    DiscountCategoryRestrictions,
    DiscountMskuRestrictions,
    HonestDiscountTable,
    OfferConversionDataTable,
    Offers2ModelTable,
    OffersRawTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import (
    BlueOffer2PicTable,
    Offer2PicTable,
)
from market.idx.generation.yatf.utils.fixtures import (
    Serialization,
    make_dco_datacamp_dict,
    make_msku_contex_dict,
    serialize_proto,
)

from market.idx.pylibrary.offer_flags.flags import OfferFlags, DisabledFlags

from market.idx.yatf.matchers.user_friendly_dict_matcher import user_friendly_dict_equal_to
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampDcoTable
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.resources.yt_tables.lbdumper_tables import (
    LbDumperAmoreTable,
    LbDumperAmoreBeruSupplierTable,
    LbDumperAmoreBeruVendorTable
)

from market.proto.content.pictures_pb2 import Picture
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as OfferPb
from market.proto.indexer.GenerationLog_pb2 import Record
from market.proto.ir.UltraController_pb2 import EnrichedOffer as EnrichedOfferPb
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
BERU_SHOP = 431782
BERU_FEED = 475690
HALF_MODE = False

SHOP_ID = 10263793

CATEGORY_WITH_RESTRICTED_DISCOUNT = 1
MSKU_WITH_RESTRICTED_DISCOUNT = 111
OFFER_FROM_TITLE = "Офферный тайтл"

BARCODE_FROM_MSKU = '100500'

MARKET_SKU_TYPE_FAST = MappingPb.MarketSkuType.MARKET_SKU_TYPE_FAST


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


def make_hprice_dict(history_price):
    return {
        "date_yyyymmdd": 20180831,
        "depth": 5,
        "min_price_date_yyyymmdd": 20180831,
        "min_price_expression": "RUR {0}".format(history_price),
        "price_expression": "RUR {0}".format(history_price)
    }


def make_blue_history_pricedrops_dict(history_price, is_valid):
    history_price_dict = make_hprice_dict(history_price)
    history_price_dict.update({'is_valid': is_valid})
    return history_price_dict


def make_raw_blue_offer_dict(
        msku,
        feed_id,
        offer_id,
        shop_id=SHOP_ID,
        title=OFFER_FROM_TITLE,
        serialization=Serialization.STRING,
        price=1234,
        offer_flags=0,
        offer_flags64=0,
        disabled_flags=0,
        disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
        barcode=None,
        market_sku_type=None,
        picURLS=None,
        picUrlIds=None,
        max_price=None,
        max_old_price=None,
):
    offer = {
        'Comment': "",
        'DeliveryBucketIds': [1, 2, 3],
        'DeliveryCalcGeneration': 444,
        'DeliveryCurrency': "USD",
        'HasDelivery': True,
        'MinQuantity': 1,
        'StepQuantity': 1,
        'URL': "",
        'UseYmlDelivery': False,
        'autobroker_enabled': 1,
        'barcode': barcode or '100131944720',
        'binary_price': {'price': price, 'id': 'RUR', 'ref_id': 'RUR'},
        'classifier_good_id': "0a3fbe800d556c6f93361dd169c162b5",
        'classifier_magic_id2': "107f289d902ecc67fa0c5d6a4bfe0f2f",
        'cpa': 4,
        'datasource_name': "horns and hooves datasource",
        'deliveryIncluded': 0,
        'description': "",
        'feed_id': int(feed_id),
        'fulfillment_shop_id': int(BERU_SHOP),
        'height': 15.0,
        'is_blue_offer': True,
        'length': 46.0,
        'market_sku': int(msku),
        'offer_flags': offer_flags | OfferFlags.BLUE_OFFER.value,
        'offer_flags64': offer_flags64 | OfferFlags.BLUE_OFFER.value,
        'disabled_flags': disabled_flags,
        'disabled_flag_sources': disabled_flag_sources,
        'offer_params': "bla-bla",
        'phone': "",
        'pickup': "true",
        'price_expression': "{}.000000 1 0 RUR RUR".format(price),
        'price_scheme': "10;9': 5;",
        'priority_regions': "213",
        'regionAttributes': "213",
        'ru_price': price,
        'shop_name': "ООО Рога и копыта",
        'shop_sku': str(offer_id),
        'store': "true",
        'title': title,
        'type': 11,
        'ware_md5': "hc1cVZiClnllcxjhGX0_cQ",
        'weight': 0.5,
        'width': 21.0,
        'yx_bid': 0,
        'yx_cbid': 0,
        'yx_ds_id': shop_id,
        'yx_money': 0,
        'yx_shop_isbn': "",
        'yx_shop_name': "ООО Рога и копыта",
        'yx_shop_offer_id': str(offer_id),
        'market_sku_type': market_sku_type,
        'max_price': max_price,
        'max_old_price': max_old_price,
    }

    if picURLS:
        offer['picURLS'] = picURLS
    if picUrlIds:
        offer['picUrlIds'] = picUrlIds

    uc = {
        'category_id': 555,
        'classification_type_value': 7,
        'classifier_category_id': 0,
        'cluster_created_timestamp': 0,
        'cluster_id': 0,
        'clutch_type': 0,
        'clutch_vendor_id': 0,
        'configuration_id': 0,
        'contains_bad_words': False,
        'duplicate_offer_group_id': 0,
        'enrich_type': 1,
        'guru_category_id': 13920627,
        'light_match_type': 2,
        'light_model_id': 0,
        'light_modification_id': 0,
        'long_cluster_id': 0,
        'mapped_active': False,
        'mapped_id': 0,
        'mapper_tovar_category_id': 0,
        'market_category_name': "Качели и шезлонги для малышей",
        'market_model_name': "Jetem Dreamer",
        'market_sku_id': int(msku),
        'market_sku_name': "Jetem Шезлонг Jetem Dreamer red",
        'matched_category_id': 555,
        'matched_id': 6546,
        'matched_vendor_id': 2345,
        'model_id': 453,
        'not_approved_sku_id': -1,
        'old_category_id': 0,
        'price_check': 0.0,
        'probability': 0.0,
        'processed_time': 1517750597448,
        'second_category_id': 0,
        'second_classification_type_value': 8,
        'second_probability': 0.0,
        'sku': "deprecated",
        'skuStatus': 1,
        'tovar_category_id': 10130,
        'vendor_id': 123,
        'params': [
            {
                'param_id': 55555L,
                'number_value': 100.0,
            }
        ],
    }
    offer['genlog'] = GenlogRow.fromOfferData(offer, default_data=False)
    offer = serialize_proto(offer, OfferPb, serialization)
    uc = serialize_proto(uc, EnrichedOfferPb, serialization)

    return {
        'msku': int(msku),
        'feed_id': int(feed_id),
        'offer_id': str(offer_id),
        'session_id': 200,
        'offer': offer,
        'uc': uc,
    }


def make_full_blue_offer_dict(
    msku,
    feed_id,
    offer_id,
    title,
    shop_id=SHOP_ID,
    history_price=None,
    reference_oldprice=None,
    history_price_is_valid=None,
    serialization=Serialization.DICT,
    buybox_offer=True,
    params=None,
    price_limit=None,
    ref_min_price=None,
    offer_flags=0,
    offer_flags64=0,
    disabled_flags=0,
    disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
    price=1234,
    market_category_id=None,
    offer_conversion=None,
    amore_data=None,
    amore_beru_supplier_data=None,
    amore_beru_vendor_data=None,
    contex_info=None,
    buybox_elasticity=None,
    barcode=None,
    market_sku_type=None,
    max_price=None,
    max_old_price=None,
    virtual_shop_id=None,
    picURLS=None,
    picUrlIds=None,
    pic=None,
):
    offer = {
        'Comment': "",
        'DeliveryBucketIds': [1, 2, 3],
        'DeliveryCalcGeneration': 444,
        'DeliveryCurrency': "USD",
        'HasDelivery': True,
        'MinQuantity': 1,
        'StepQuantity': 1,
        'URL': "https://market.yandex.ru/product/1713074440?sku=" + str(msku),
        'UseYmlDelivery': False,
        'autobroker_enabled': 1,
        'barcode': barcode or '100131944720',
        'binary_price': {'price': price, 'id': 'RUR', 'ref_id': 'RUR'},
        'classifier_good_id': "0a3fbe800d556c6f93361dd169c162b5",
        'classifier_magic_id2': "107f289d902ecc67fa0c5d6a4bfe0f2f",
        'cpa': 4,
        'datasource_name': "Яндекс.Маркет",
        'deliveryIncluded': 0,
        'description': "Не содержит изотопов урана 235 и других радионуклидов",
        'feed_id': int(feed_id),
        'fulfillment_shop_id': int(BERU_SHOP),
        'height': 15.0,
        'is_blue_offer': True,
        'length': 46.0,
        'market_sku': int(msku),
        'offer_params': "",
        'offer_flags': offer_flags,
        'offer_flags64': offer_flags64,
        'disabled_flags': disabled_flags,
        'disabled_flag_sources': disabled_flag_sources,
        'phone': "",
        'pickup': "true",
        'price_expression': "{}.000000 1 0 RUR RUR".format(price),
        'price_scheme': "10;9': 5;",
        'priority_regions': "213",
        'regionAttributes': "213",
        'ru_price': price,
        'shop_name': "Беру",
        'shop_sku': str(offer_id),
        'store': "true",
        'title': title,
        'type': 11,
        'ware_md5': "hc1cVZiClnllcxjhGX0_cQ",
        'weight': 0.5,
        'width': 21.0,
        'yx_bid': 0,
        'yx_cbid': 0,
        'yx_ds_id': int(shop_id),
        'yx_money': 0,
        'yx_shop_isbn': "",
        'yx_shop_name': "Беру",
        'yx_shop_offer_id': str(offer_id),
        'quality_rating': 5,
        'picURLS': "https://avatars.mds.yandex.net/get-mpic/175985/img_id456/orig\t"
                   "https://avatars.mds.yandex.net/get-mpic/175985/img_id789/orig",
        'vendor': '',
        'model': 'SKU со всеми данными',
        'market_category_id': market_category_id if market_category_id else 12345,
        'sales_notes': '',
        'buybox_offer': buybox_offer,
    }

    if history_price:
        offer['price_history'] = make_hprice_dict(history_price)

    if reference_oldprice:
        offer['binary_reference_oldprice'] = {'price': reference_oldprice, 'id': 'RUR', 'ref_id': 'RUR'}

    if history_price_is_valid is not None:
        offer['price_history'] = make_blue_history_pricedrops_dict(history_price, history_price_is_valid)

    if price_limit:
        offer['binary_price_limit'] = {'price': price_limit}

    if ref_min_price:
        offer['binary_ref_min_price'] = {'price': ref_min_price}

    if amore_data:
        offer['amore_data'] = amore_data

    if offer_conversion:
        offer['offer_conversion'] = offer_conversion

    if amore_beru_supplier_data:
        offer['amore_beru_supplier_data'] = amore_beru_supplier_data

    if amore_beru_vendor_data:
        offer['amore_beru_vendor_data'] = amore_beru_vendor_data

    if contex_info:
        offer['contex_info'] = contex_info

    if buybox_elasticity:
        offer['buybox_elasticity'] = buybox_elasticity

    if max_price:
        offer['max_price'] = max_price

    if max_price:
        offer['max_old_price'] = max_old_price

    if market_sku_type:
        offer['market_sku_type'] = market_sku_type

    if picURLS:
        offer['picURLS'] = picURLS
    if picUrlIds:
        offer['picUrlIds'] = picUrlIds

    uc = {
        'category_id': 12345,
        'classification_type_value': 7,
        'classifier_category_id': 12345,
        'cluster_created_timestamp': 0,
        'cluster_id': -1,
        'clutch_type': 0,
        'clutch_vendor_id': 0,
        'configuration_id': 0,
        'contains_bad_words': False,
        'duplicate_offer_group_id': 0,
        'enrich_type': 1,
        'guru_category_id': 0,
        'light_match_type': 2,
        'light_model_id': 0,
        'light_modification_id': 0,
        'long_cluster_id': 0,
        'mapped_active': False,
        'mapped_id': 0,
        'mapper_tovar_category_id': 0,
        'market_category_name': "Качели и шезлонги для малышей",
        'market_model_name': "Jetem Dreamer",
        'market_sku_id': int(msku),
        'market_sku_name': "Jetem Шезлонг Jetem Dreamer red",
        'matched_category_id': 12345,
        'matched_id': 1713074440L,
        'matched_vendor_id': 966973L,
        'model_id': 1713074440L,
        'not_approved_sku_id': -1,
        'old_category_id': 0,
        'price_check': 0.0,
        'probability': 0.0,
        'processed_time': 1517750597448,
        'second_category_id': 0,
        'second_classification_type_value': 8,
        'second_probability': 0.0,
        'sku': "deprecated",
        'skuStatus': 1,
        'tovar_category_id': 12345,
        'vendor_id': 966973L,
        'matched_type_value': EnrichedOfferPb.MATCH_OK,
        'params': [
            {
                'param_id': 1000001L,
                'number_value': 127.0,
            },
            {
                'param_id': 1000002L,
                'value_id': 12976298L,
            },
            {
                'param_id': 1000003L,
                'value_id': 12109936L,
            }
        ],
    }
    genlog_dict = GenlogRow.fromOfferData(offer, default_data=False)
    if virtual_shop_id:
        genlog_dict['virtual_shop_id'] = virtual_shop_id

    offer = serialize_proto(offer, OfferPb, serialization)
    offer.update(genlog_dict)
    uc = serialize_proto(uc, EnrichedOfferPb, serialization)

    result = {
        'feed_id': int(feed_id),
        'offer_id': str(offer_id),
        'session_id': 200,
        'offer': offer,
        'uc': uc,
        'params': params,
        'ware_md5': "hc1cVZiClnllcxjhGX0_cQ",
        'experiment_id': contex_info['experiment_id'] if contex_info is not None and contex_info.get('is_experimental', None) else None,
        'is_dsbs': False,
        'cpa': 4,
        'genlog': genlog_dict,
    }

    if pic:
        result['pic'] = pic

    return result


def make_full_blue_offer_dict_fast_sku(
    msku,
    feed_id,
    offer_id,
    title,
    buybox_offer=True,
    serialization=Serialization.DICT,
    params=None,
    offer_flags=0,
    offer_flags64=0,
    disabled_flags=0,
    disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
    price=1234,
    contex_info=None,
    barcode=None,
    market_sku_type=None,
    picURLS=None,
    picUrlIds=None,
    pic=None,
):
    raw_blue_offer = make_raw_blue_offer_dict(
        msku=msku,
        feed_id=feed_id,
        offer_id=offer_id,
        shop_id=SHOP_ID,
        title=title,
        serialization=serialization,
        price=price,
        offer_flags=offer_flags,
        offer_flags64=offer_flags64,
        disabled_flags=disabled_flags,
        disabled_flag_sources=disabled_flag_sources,
        barcode=barcode,
        market_sku_type=market_sku_type,
        picURLS=picURLS,
        picUrlIds=picUrlIds
    )
    offer = raw_blue_offer['offer']
    genlog = GenlogRow.fromOfferData(offer, default_data=False)
    uc = raw_blue_offer['uc']

    offer['buybox_offer'] = buybox_offer

    result = {
        'feed_id': int(feed_id),
        'offer_id': str(offer_id),
        'session_id': 200,
        'offer': offer,
        'uc': uc,
        'params': params,
        'ware_md5': "hc1cVZiClnllcxjhGX0_cQ",
        'experiment_id': contex_info['experiment_id'] if contex_info is not None and contex_info.get('is_experimental', None) else None,
        'is_dsbs': False,
        'cpa': 4,
        'genlog': genlog,
    }

    if pic:
        result['pic'] = pic

    return result


def make_blue_hprice_dict(msku, history_price, serialization=Serialization.STRING):
    offer = {
        "price_history": make_hprice_dict(history_price),
        "pricedrops_tests_passed": True,
    }

    offer = serialize_proto(offer, OfferPb, serialization)

    return {
        "msku": int(msku),
        "offer": offer,
        "history_price": int(history_price),
    }


def make_blue_pricedrops_dict(msku, history_price, serialization=Serialization.STRING, is_valid_history=False):
    offer = {
        "price_history": make_blue_history_pricedrops_dict(history_price, is_valid_history),
        "pricedrops_tests_passed": True,
    }

    genlog = serialize_proto(offer, Record, serialization)
    offer = serialize_proto(offer, OfferPb, serialization)

    return {
        "msku": int(msku),
        "offer": offer,
        "genlog": genlog,
        "history_price": int(history_price),
    }


def make_blue_price_limit_dict(msku, price_limit):
    return {
        "market_sku": int(msku),
        "max_avaliable_price": float(price_limit),
    }


def make_blue_ref_min_price_dict(msku, ref_min_price, ref_min_price_warning, max_warning_level):
    return {
        "market_sku": int(msku),
        "ref_min_price": float(ref_min_price),
        "ref_min_price_warning": int(ref_min_price_warning),
        "max_warning_lvl": int(max_warning_level),
    }


@pytest.fixture(scope="module")
def source_blue_offers_raw():
    return [
        # regular offers with real msku
        make_raw_blue_offer_dict(msku=111, feed_id=11, offer_id="knownOffer1"),
        make_raw_blue_offer_dict(msku=222, feed_id=11, offer_id="knownOffer2"),
        make_raw_blue_offer_dict(msku=111, feed_id=22, offer_id="knownOffer3"),
        # the smallest price for msku, but is has_gone offer
        make_raw_blue_offer_dict(msku=111, feed_id=33, offer_id="knownOffer4", price=1000,
                                 offer_flags=OfferFlags.OFFER_HAS_GONE.value,
                                 offer_flags64=OfferFlags.OFFER_HAS_GONE.value),
        # only one offer for msku, but is has_gone, no buybox offer for this msku
        make_raw_blue_offer_dict(msku=444, feed_id=33, offer_id="knownOffer5",
                                 disabled_flags=DisabledFlags.MARKET_STOCK.value),
        # only one offer for msku, but with price greater than price_limit
        make_raw_blue_offer_dict(msku=555, feed_id=33, offer_id="knownOffer6", price=1000000000),
        make_raw_blue_offer_dict(msku=666, feed_id=11, offer_id="knownOffer7", price=10990000000),
        # ref_min_price is absent
        make_raw_blue_offer_dict(msku=777, feed_id=11, offer_id="noRefMinOffer", price=1000000000),
        # ref_min_price_warning is too high
        make_raw_blue_offer_dict(msku=888, feed_id=22, offer_id="highPriceWarnOffer", price=2000000000),
        # golden_matrix msku 1 offer 1
        make_raw_blue_offer_dict(msku=999, feed_id=11, offer_id="goldenMatrix1_1", price=2000000000),
        # golden_matrix msku 1 offer 2
        make_raw_blue_offer_dict(msku=999, feed_id=22, offer_id="goldenMatrix1_2", price=3000000000),
        # golden_matrix msku 2 offer 1
        make_raw_blue_offer_dict(msku=1000, feed_id=11, offer_id="goldenMatrix2_1", price=4000000000),
        # ref_min_price_warning is too high with additional setup warning level
        make_raw_blue_offer_dict(msku=1111, feed_id=22, offer_id="highPriceWarnOfferSetup", price=2000000000),
        # offer for blue-on-white promo test
        make_raw_blue_offer_dict(msku=1, feed_id=BERU_FEED, offer_id="blue_offer_1"),
        make_raw_blue_offer_dict(msku=1, feed_id=BERU_FEED, offer_id="blue_offer_2"),
        # offer for original and experimental msku
        make_raw_blue_offer_dict(msku=3000, feed_id=11, offer_id="offer_under_msku_with_contex"),
        # offer for buybox elasticity
        make_raw_blue_offer_dict(msku=1001, feed_id=BERU_FEED, offer_id="buyboxElasticityOfferSetup", price=2000000000),
        # offer with unknown msku
        make_raw_blue_offer_dict(msku=112, feed_id=22, offer_id="offerWithUnknownSku"),
        make_raw_blue_offer_dict(msku=113, feed_id=33, offer_id="offerWithUnknownSku2"),
        # offer with FAST msku
        make_raw_blue_offer_dict(
            msku=114, feed_id=33, offer_id="offerWithFastSku", market_sku_type=MARKET_SKU_TYPE_FAST,
            picURLS="http://ya.ru/2.jpeg\thttp://ya.ru/3.jpeg\thttp://ya.ru/1.jpeg",
            picUrlIds=[
                'some_picture_id_2',
                'some_picture_id_3',
                'some_picture_id_1',
            ]
        ),
        # regular offer with real msku and FAST flag
        make_raw_blue_offer_dict(msku=222, feed_id=44, offer_id="knownOfferWithFastSku", price=1235, market_sku_type=MARKET_SKU_TYPE_FAST),
        # max_old_price, max_price
        make_raw_blue_offer_dict(msku=9993, feed_id=11, offer_id="maxOldPrice_1", price=2000000000),
        # new offer with IS_RESALE
        make_raw_blue_offer_dict(
            msku=115, feed_id=33, offer_id="offerWithIsResale",
            offer_flags64=OfferFlags.IS_RESALE.value,
            picURLS="http://ya.ru/2.jpeg\thttp://ya.ru/3.jpeg\thttp://ya.ru/1.jpeg",
            picUrlIds=[
                'some_picture_id_2',
                'some_picture_id_3',
                'some_picture_id_1',
            ]
        ),
        # regular IS_RESALE offer
        make_raw_blue_offer_dict(
            msku=222, feed_id=44, offer_id="knownOfferWithIsResale", price=1235,
            offer_flags64=OfferFlags.IS_RESALE.value,
            picURLS="http://ya.ru/2.jpeg\thttp://ya.ru/3.jpeg\thttp://ya.ru/1.jpeg",
            picUrlIds=[
                'some_picture_id_2',
                'some_picture_id_3',
                'some_picture_id_1',
            ]
        ),
    ]


@pytest.fixture(scope="module")
def source_msku_contex():
    return [
        make_msku_contex_dict(
            msku=1,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Blue-on-White promo MSKU"
        ),
        make_msku_contex_dict(
            msku=111,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Ядерный реактор игрушечный для детей до 5 лет"
        ),
        make_msku_contex_dict(
            msku=222,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Генератор чёрных дыр портативный",
            market_category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
            category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
        ),
        make_msku_contex_dict(
            msku=333,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Кубик рубика с автографом Криштиану Роналду"
        ),
        make_msku_contex_dict(
            msku=444,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="MSKU для которого нету ни одного не has_gone оффера"
        ),
        make_msku_contex_dict(
            msku=555,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="MSKU для которого нет ни одного оффера, с ценой ниже предельной"
        ),
        make_msku_contex_dict(
            msku=666,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="при сравнении price_limit домножается на 10^7"
        ),
        make_msku_contex_dict(
            msku=777,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="минимальная референсная цена отсутствует"
        ),
        make_msku_contex_dict(
            msku=888,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="референсная цена слишком подозрительная"
        ),
        make_msku_contex_dict(
            msku=999,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Товар из золотой матрицы #1"
        ),
        make_msku_contex_dict(
            msku=1000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Товар из золотой матрицы #2"
        ),
        make_msku_contex_dict(
            msku=9993,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Товар с max_price и max_old_price"
        ),
        make_msku_contex_dict(
            msku=1001,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="with buybox elasticity"
        ),
        make_msku_contex_dict(
            msku=1111,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="референсная цена слишком подозрительная по заданным настройкам"
        ),
        make_msku_contex_dict(
            msku=2000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="МСКУ, у которого нет офферов"
        ),
        # оригинальный мску, у которого есть экспериментальный
        make_msku_contex_dict(
            msku=3000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="МСКУ, у которого есть эксперимент",
            contex_info={
                "experiment_id": "experiment_3000",
                "original_msku_id": 3000,
                "experimental_msku_id": 3001,
            }
        ),
        # экспирементальные мску должны иметь такой же id, как у оригинального
        make_msku_contex_dict(
            msku=3000,
            msku_exp=3001,
            msku_experiment_id="",
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Экспериментальный МСКУ",
            contex_info={
                "experiment_id": "experiment_3000",
                "original_msku_id": 3000,
                "experimental_msku_id": 3001,
            }
        ),
    ]


@pytest.fixture(scope="module")
def source_blue_hprice():
    return [
        make_blue_hprice_dict(msku=111, history_price=1000000000),
        make_blue_hprice_dict(msku=222, history_price=2000000000),
        make_blue_hprice_dict(msku=333, history_price=3000000000),
    ]


@pytest.fixture(scope="module")
def source_dco():
    return [
        make_dco_datacamp_dict(msku=111, oldprice=1111),
        make_dco_datacamp_dict(msku=222, oldprice=2222),
    ]


@pytest.fixture(scope="module")
def source_blue_pricedrops():
    return [
        make_blue_pricedrops_dict(msku=111, history_price=1000000000, is_valid_history=True),
        make_blue_pricedrops_dict(msku=222, history_price=2000000000, is_valid_history=False),
        make_blue_pricedrops_dict(msku=333, history_price=3000000000, is_valid_history=True),
    ]


@pytest.fixture(scope="module")
def source_blue_price_limit():
    return [
        make_blue_price_limit_dict(msku=111, price_limit=11111),
        make_blue_price_limit_dict(msku=222, price_limit=22222),
        make_blue_price_limit_dict(msku=555, price_limit=99),
        make_blue_price_limit_dict(msku=666, price_limit=1100.0),
    ]


@pytest.fixture(scope="module")
def source_blue_ref_min_price():
    return [
        make_blue_ref_min_price_dict(msku=111, ref_min_price=11111, ref_min_price_warning=1, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=666, ref_min_price=1100.0, ref_min_price_warning=0, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=777, ref_min_price=-1, ref_min_price_warning=10, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=888, ref_min_price=15.0, ref_min_price_warning=5, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=1111, ref_min_price=15.0, ref_min_price_warning=2, max_warning_level=1),
    ]


@pytest.fixture(scope="module")
def source_discount_category_restrictions():
    return [
        {
            "hid": CATEGORY_WITH_RESTRICTED_DISCOUNT,
        },
    ]


@pytest.fixture(scope="module")
def source_discount_msku_restrictions():
    return [
        {
            "msku": MSKU_WITH_RESTRICTED_DISCOUNT,
        },
    ]


@pytest.fixture(scope="module")
def source_offer_conversion_data():
    """
    Конверсии присоединяются по feed_id+offer_id, а не по msku.
    У офферов knownOffer1 и knownOffer3 msku одинаковые, а конверсии разные.
    """
    return [
        {'conversion_value': 0.2, 'feed_id': 11, 'offer_id': "knownOffer2", 'conversion_type': 'MSK'},
        {'conversion_value': 0.1, 'feed_id': 11, 'offer_id': "knownOffer1", 'conversion_type': 'SPB'},
        {'conversion_value': 0.3, 'feed_id': 22, 'offer_id': "knownOffer3", 'conversion_type': 'SPB'},
        {'conversion_value': 0.2, 'feed_id': 44, 'offer_id': "knownOfferWithFastSku", 'conversion_type': 'SPB'},
    ]


@pytest.fixture(scope="module")
def source_amore_data():
    """
    Данные об автостратегии оффера присоединяются по feed_id+offer_id, а не по msku.
    У офферов knownOffer1 и knownOffer3 msku одинаковые, а данные об автостратегии разные.
    """
    return [
        {'amore_data': 'posdrrcpo', 'feed_id': 11, 'offer_id': "knownOffer1", 'amore_ts_sec': 9000, 'shop_id': 1, 'warehouse_id': 1},
    ]


@pytest.fixture(scope="module")
def source_amore_beru_supplier_data():
    return [
        {'amore_beru_supplier_data': 'posdrrcpo-bs', 'feed_id': 11, 'offer_id': "knownOffer1", 'amore_ts_sec': 9000, 'shop_id': 1, 'warehouse_id': 1},
    ]


@pytest.fixture(scope="module")
def source_amore_beru_vendor_data():
    return [
        {'amore_beru_vendor_data': 'posdrrcpo-bv', 'feed_id': 11, 'offer_id': "knownOffer1", 'amore_ts_sec': 9000, 'shop_id': 1, 'warehouse_id': 1},
    ]


@pytest.fixture(scope="module")
def source_blue_golden_matrix():
    return [
        {'msku': 1000},
        {'msku': 999},
    ]


@pytest.fixture(scope="module")
def source_honest_discount():
    return [
        {'msku': 9993, 'offer_id': 'maxOldPrice_1', 'max_price': 3000000000., 'max_old_price': 4000000000.},
    ]


@pytest.fixture(scope="module")
def source_blue_buybox_elasticity_data():
    return [
        {'sku': 1001, 'price_variant': 101, 'demand_mean': 10.1},
        {'sku': 1001, 'price_variant': 102, 'demand_mean': 9.},
        {'sku': 1001000, 'price_variant': 1020, 'demand_mean': 0.1},
    ]


PipelineParams = namedtuple(
    'PipelineParams', [
        'drop_msku_without_offers',
        'enrich_blue_offers_from_fast_sku',
        'enrich_blue_offers_from_resale',
    ]
)


@pytest.fixture(
    scope="module",
    params=[
        PipelineParams(
            drop_msku_without_offers=False,
            enrich_blue_offers_from_fast_sku=False,
            enrich_blue_offers_from_resale=False,
        ),
        PipelineParams(
            drop_msku_without_offers=True,
            enrich_blue_offers_from_fast_sku=False,
            enrich_blue_offers_from_resale=False,
        ),
        PipelineParams(
            drop_msku_without_offers=False,
            enrich_blue_offers_from_fast_sku=True,
            enrich_blue_offers_from_resale=False,
        ),
        PipelineParams(
            drop_msku_without_offers=False,
            enrich_blue_offers_from_fast_sku=False,
            enrich_blue_offers_from_resale=True,
        ),
    ],
    ids=[
        'regular_main-idx',
        'drop_msku_without_offers',
        'enrich_blue_offers_from_fast_sku',
        'enrich_blue_offers_from_resale',
    ]
)
def pipeline_params(request):
    return request.param


@pytest.fixture(scope="module")
def or3_config_data(pipeline_params):
    # Здесь pipeline_params необходим для независимых запусков main-idx
    home_dir = get_yt_prefix()
    blue_price_table = ypath_join(home_dir, 'history', 'blue', 'prices', 'hprices', 'last_complete')
    blue_pricedrops_validation_table = ypath_join(home_dir, 'history', 'blue', 'prices', 'hprices_pricedrops', 'last_complete')
    blue_price_limit_table = ypath_join(home_dir, 'in', 'blue', 'max_blue_prices', 'last_complete')
    blue_dco_upload_table = ypath_join(home_dir, 'in', 'blue', 'dco_upload_table', 'last_complete')
    blue_golden_matrix_table = ypath_join(home_dir, 'in', 'blue', 'golden_matrix_table', 'last_complete')
    blue_dco_table = ypath_join(home_dir, 'datacamp', 'blue', 'dco')
    discount_category_restrictions = ypath_join(home_dir, 'common', 'discounts', 'category_restrictions', 'recent')
    discount_msku_restrictions = ypath_join(home_dir, 'common', 'discounts', 'msku_restrictions', 'recent')
    offer_conversion_table = ypath_join(home_dir, 'blue', 'offer_conversion', 'recent')
    amore_data_table = ypath_join(home_dir, 'blue', 'amore', 'recent')
    amore_beru_supplier_data_table = ypath_join(home_dir, 'blue', 'amore_bs', 'recent')
    amore_beru_vendor_data_table = ypath_join(home_dir, 'blue', 'amore_bv', 'recent')
    buybox_elasticity_table = ypath_join(home_dir, 'in', 'blue', 'elasticity_for_buybox', 'recent')
    honest_discount_table = ypath_join(home_dir, 'in', 'blue', 'honest_discount', 'recent')
    return {
        'yt': {
            'home_dir': home_dir,
            'yt_blue_price_validation_table': blue_price_table,
            'yt_blue_pricedrops_validation_table': blue_pricedrops_validation_table,
            'yt_blue_datacamp_dco_prices': blue_dco_table,
            'yt_blue_price_limit_external': blue_price_limit_table,
            'yt_blue_dco_upload_table': blue_dco_upload_table,
            'yt_blue_golden_matrix_table': blue_golden_matrix_table,
            'yt_discount_category_restrictions': discount_category_restrictions,
            'yt_discount_msku_restrictions': discount_msku_restrictions,
            'yt_blue_offer_conversions': offer_conversion_table,
            'yt_blue_amore': amore_data_table,
            'yt_amore_beru_supplier_data': amore_beru_supplier_data_table,
            'yt_amore_beru_vendor_data': amore_beru_vendor_data_table,
            'yt_buybox_elasticity_table': buybox_elasticity_table,
            'yt_collected_promo_details_output_dir': 'collected_promo_details',
            'yt_honest_discount_table': honest_discount_table,
            'yt_use_new_honest_discount_algo': 'true',
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'blue_price_validation_enabled': 'true',
            'thirdparty_dco_enabled': 'true',
            'buybox_elasticity_enabled': 'true',
            'enrich_blue_offers_from_fast_sku': pipeline_params.enrich_blue_offers_from_fast_sku,
            'enrich_blue_offers_from_resale': pipeline_params.enrich_blue_offers_from_resale,
        }
    }


@pytest.fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_blue_offers_raw,
    source_msku_contex,
    source_blue_hprice,
    source_dco,
    source_blue_pricedrops,
    source_blue_price_limit,
    source_blue_ref_min_price,
    source_blue_golden_matrix,
    source_discount_category_restrictions,
    source_discount_msku_restrictions,
    source_offer_conversion_data,
    source_amore_data,
    source_amore_beru_supplier_data,
    source_amore_beru_vendor_data,
    source_blue_buybox_elasticity_data,
    source_honest_discount
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=[]
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=[]
        ),
        'blue_offer2pic_unsorted': BlueOffer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offer2pic_unsorted'),
            data=[
                {
                    'msku': 115,
                    'feed_id': 33,
                    'offer_id': 'some_offer_id',
                    'id': 'anoter_picture_id',
                },
                {
                    'msku': 114,
                    'feed_id': 33,
                    'offer_id': 'offerWithFastSku',
                    'id': 'some_picture_id_1',
                },
                {
                    'msku': 114,
                    'feed_id': 33,
                    'offer_id': 'offerWithFastSku',
                    'id': 'some_picture_id_2',
                },
                {
                    'msku': 114,
                    'feed_id': 33,
                    'offer_id': 'offerWithFastSku',
                    'id': 'some_picture_id_3',
                },
                {
                    'msku': 115,
                    'feed_id': 33,
                    'offer_id': 'offerWithIsResale',
                    'id': 'some_picture_id_1',
                },
                {
                    'msku': 115,
                    'feed_id': 33,
                    'offer_id': 'offerWithIsResale',
                    'id': 'some_picture_id_2',
                },
                {
                    'msku': 115,
                    'feed_id': 33,
                    'offer_id': 'offerWithIsResale',
                    'id': 'some_picture_id_3',
                },
                {
                    'msku': 222,
                    'feed_id': 44,
                    'offer_id': 'knownOfferWithIsResale',
                    'id': 'some_picture_id_1',
                },
                {
                    'msku': 222,
                    'feed_id': 44,
                    'offer_id': 'knownOfferWithIsResale',
                    'id': 'some_picture_id_2',
                },
                {
                    'msku': 222,
                    'feed_id': 44,
                    'offer_id': 'knownOfferWithIsResale',
                    'id': 'some_picture_id_3',
                },
            ]
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data=[]
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'blue_hprice': BlueHistoryPriceTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'history', 'blue', 'prices', 'hprices', 'last_complete'),
            data=source_blue_hprice,
        ),
        'dco': DataCampDcoTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'datacamp', 'blue', 'dco'),
            data=source_dco,
        ),
        'blue_pricedrops': BlueHistoryPriceDropsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'history', 'blue', 'prices', 'hprices_pricedrops', 'last_complete'),
            data=source_blue_pricedrops
        ),
        'blue_price_limit': BluePriceLimitTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'max_blue_prices', 'last_complete'),
            data=source_blue_price_limit,
        ),
        'ref_min_price': BlueDcoUploadTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'dco_upload_table', 'last_complete'),
            data=source_blue_ref_min_price,
        ),
        'golden_matrix': BlueGoldenMatrixTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'golden_matrix_table', 'last_complete'),
            data=source_blue_golden_matrix,
        ),
        'honest_discount': HonestDiscountTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'honest_discount', 'recent'),
            data=source_honest_discount,
        ),
        'discount_category_restrictions': DiscountCategoryRestrictions(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'common', 'discounts', 'category_restrictions', 'recent'),
            data=source_discount_category_restrictions,
        ),
        'discount_msku_restrictions': DiscountMskuRestrictions(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'common', 'discounts', 'msku_restrictions', 'recent'),
            data=source_discount_msku_restrictions,
        ),
        'offer_conversion_table': OfferConversionDataTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'blue', 'offer_conversion', 'recent'),
            data=source_offer_conversion_data,
        ),
        'amore_data_table': LbDumperAmoreTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'blue', 'amore', 'recent'),
            data=source_amore_data,
        ),
        'amore_beru_supplier_data_table': LbDumperAmoreBeruSupplierTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'blue', 'amore_bs', 'recent'),
            data=source_amore_beru_supplier_data,
        ),
        'amore_beru_vendor_data_table': LbDumperAmoreBeruVendorTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'blue', 'amore_bv', 'recent'),
            data=source_amore_beru_vendor_data,
        ),
        'blue_buybox_elasticity_table': BlueBuyboxElasticityTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'elasticity_for_buybox', 'recent'),
            data=source_blue_buybox_elasticity_data,
        ),
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'collected_promo_details', 'recent'),
            data=[],
        ),
    }


@pytest.fixture(scope="module")
def create_source_yt_tables(yt_server, source_yt_tables):
    yt_client = yt_server.get_yt_client()

    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_client.exists(path), "Table {} doesn\'t exist".format(path))


@pytest.fixture(scope="module")
def in_picrobot_success_data():
    return [
        {
            'id': 'some_picture_id_1',
            'is_good_size_pic': True,
            'pic': Picture(width=900, height=1200, crc='crc_test_picture_1').SerializeToString()
        },
        {
            'id': 'some_picture_id_2',
            'is_good_size_pic': True,
            'pic': Picture(width=800, height=1000, crc='crc_test_picture_2').SerializeToString()
        },
        {
            'id': 'some_picture_id_3',
            'is_good_size_pic': True,
            'pic': Picture(width=600, height=600, crc='crc_test_picture_3').SerializeToString()
        },
    ]


@pytest.fixture(scope="module")
def main_idx(yt_server, or3_config, create_source_yt_tables, pipeline_params, tovar_tree, in_picrobot_success_data):
    yt_home_path = ypath_join(or3_config.options['yt']['home_dir'])
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=in_picrobot_success_data
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'blue_offer2pic': BlueOffer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offer2pic'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
        'drop_msku_without_offers': pipeline_params.drop_msku_without_offers,
    }
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def make_msku_contex_dict_ware_md5(**kwargs):
    result = make_msku_contex_dict(serialization=Serialization.DICT, **kwargs)
    offer = result['offer']
    genlog = offer.get('genlog', None)
    result['ware_md5'] = genlog['ware_md5']
    result['experiment_id'] = None
    if genlog:
        contex_info = genlog.get('contex_info', None)
        if contex_info:
            result['offer']['contex_info'] = deepcopy(contex_info)
            if contex_info.get('is_experimental', None):
                result['experiment_id'] = contex_info['experiment_id']

    result['is_dsbs'] = False
    result['cpa'] = 0
    result['genlog'] = genlog

    return result


def get_result_offer_by_offer_id(main_idx, offer_id):
    result_offers = main_idx.outputs['offers_by_offer_id']
    offer = deepcopy(result_offers.get(offer_id, None))
    if offer and offer.get('offer', None):
        del offer['offer']['randx']
    if offer:
        del offer['msku']
        del offer['is_fake_msku_offer']
    return offer


def compare_genlog_field(actual, expected):
    if expected is None:
        assert actual is None
        return
    for k in expected['genlog']:
        if k in ['url']:
            continue
        if k in ['amore_data', 'amore_beru_vendor_data', 'amore_beru_supplier_data']:
            assert base64.b64encode(expected['genlog'][k]) == actual['genlog'][k], k
            continue

        if k in ['fulfillment_shop_id', 'market_sku', 'flags']:
            assert str(expected['genlog'][k]) == actual['genlog'][k], k
            continue

        if k in ['mbi_delivery_bucket_ids']:
            expected_value = [str(x) for x in expected['genlog'][k]]
            assert expected_value == actual['genlog'][k], k
            continue

        if k in ['binary_reference_old_price', 'binary_price', 'binary_ref_min_price', 'binary_price_limit', 'max_price', 'max_old_price']:
            if 'id' in actual['genlog'][k]:
                assert expected['genlog'][k]['id'] == actual['genlog'][k]['id'], k
            if 'ref_id' in actual['genlog'][k]:
                assert expected['genlog'][k]['ref_id'] == actual['genlog'][k]['ref_id'], k
            assert str(expected['genlog'][k]['price']) == actual['genlog'][k]['price'], k
            continue

        if k in ['contex_info']:
            assert expected['genlog'][k]['experiment_id'] == actual['genlog'][k]['experiment_id'], k
            assert str(expected['genlog'][k]['original_msku_id']) == actual['genlog'][k]['original_msku_id'], k
            assert str(expected['genlog'][k]['experimental_msku_id']) == actual['genlog'][k]['experimental_msku_id'], k
            continue

        assert expected['genlog'][k] == actual['genlog'][k], k


@pytest.mark.parametrize("output_path", ["offers", "blue_offers", "blue_offers_sku"])
def test_offers_count(main_idx, pipeline_params, source_msku_contex, source_blue_offers_raw, output_path):
    """
    Проверяем количество офферов на выходе main-idx в белых и синих шардах,
    а также в таблице blue_offers_sku
    """
    offers = main_idx.outputs[output_path]

    # в результирующей таблице всегда нет:
    #   - офферов, для которых нет msku или быстрой sku (offerWithUnknownSku, offerWithUnknownSku)
    #   - экспериментальных msku (MS3001)
    # blue + msku - 2 (offerWithUnknownSku, offerWithUnknownSku) - 1 (MS3001)
    expected = len(source_blue_offers_raw) + len(source_msku_contex) - 3 - 1

    if pipeline_params.drop_msku_without_offers:
        # также в результирующей таблице нет msku, под которыми нет офферов (MS2000 and MS333)
        expected -= 2

    if not pipeline_params.enrich_blue_offers_from_fast_sku:
        # также в результирующей таблице нет оффера с быстрой sku (offerWithFastSku)
        expected -= 1

    assert_that(offers, has_length(expected))


def test_blue_offers_enrich_with_msku(main_idx):
    """
    Проверяем обогащение синих оферов данными msku
    """

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer1'),
        make_full_blue_offer_dict(
            msku=111,
            feed_id=11,
            offer_id='knownOffer1',
            title="Ядерный реактор игрушечный для детей до 5 лет",
            history_price=1000000000,
            reference_oldprice=1111,
            history_price_is_valid=True,
            price_limit=111110000000,
            ref_min_price=111110000000,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.DISCOUNT_RESTRICTED.value | OfferFlags.BLUE_OFFER.value,
            offer_conversion=[
                {
                    "conversion_value": 0.1,
                    "conversion_type": "SPB"
                }
            ],
            amore_data=b'posdrrcpo',
            amore_beru_supplier_data=b'posdrrcpo-bs',
            amore_beru_vendor_data=b'posdrrcpo-bv',
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer2'),
        make_full_blue_offer_dict(
            msku=222,
            feed_id=11,
            offer_id='knownOffer2',
            title="Генератор чёрных дыр портативный",
            history_price=2000000000,
            reference_oldprice=2222,
            history_price_is_valid=False,
            price_limit=222220000000,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
            market_category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.DISCOUNT_RESTRICTED.value | OfferFlags.BLUE_OFFER.value,
            offer_conversion=[
                {
                    "conversion_value": 0.2,
                    "conversion_type": "MSK"
                }
            ],
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer3'),
        make_full_blue_offer_dict(
            msku=111,
            feed_id=22,
            offer_id='knownOffer3',
            title="Ядерный реактор игрушечный для детей до 5 лет",
            history_price=1000000000,  # from from yt_blue_price_validation_table with msku=111
            reference_oldprice=1111,
            history_price_is_valid=True,
            buybox_offer=False,
            price_limit=111110000000,
            ref_min_price=111110000000,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.DISCOUNT_RESTRICTED.value | OfferFlags.BLUE_OFFER.value,
            offer_conversion=[
                {
                    "conversion_value": 0.3,
                    "conversion_type": "SPB"
                }
            ],
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer4'),
        make_full_blue_offer_dict(
            msku=111,
            feed_id=33,
            offer_id='knownOffer4',
            title="Ядерный реактор игрушечный для детей до 5 лет",
            history_price=1000000000,  # from from yt_blue_price_validation_table with msku=111
            reference_oldprice=1111,
            history_price_is_valid=True,
            buybox_offer=False,
            price_limit=111110000000,
            ref_min_price=111110000000,
            offer_flags=OfferFlags.OFFER_HAS_GONE.value | OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.OFFER_HAS_GONE | OfferFlags.DISCOUNT_RESTRICTED | OfferFlags.BLUE_OFFER.value,
            price=1000,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer5'),
        make_full_blue_offer_dict(
            msku=444,
            feed_id=33,
            offer_id='knownOffer5',
            title="MSKU для которого нету ни одного не has_gone оффера",
            history_price=None,
            buybox_offer=False,
            price_limit=None,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
            disabled_flags=DisabledFlags.MARKET_STOCK.value,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_msku_in_offers_shards(main_idx):
    """
    Проверяем что реальные msku попадают в шардированные таблицы офферов
    """
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'MS111'),
        make_msku_contex_dict_ware_md5(
            msku=111,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Ядерный реактор игрушечный для детей до 5 лет",
            set_msku_row=False,
            price_limit=111110000000,
            ref_min_price=111110000000,
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'MS222'),
        make_msku_contex_dict_ware_md5(
            msku=222,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Генератор чёрных дыр портативный",
            set_msku_row=False,
            market_category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
            price_limit=222220000000,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
        )
    )


@pytest.mark.parametrize("offer_id", [
    "offerWithUnknownSku",
    "offerWithUnknownSku2"
])
def test_filter_offers_with_unknown_sku(main_idx, offer_id):
    """
    Проверяем что офферы с неизвестными sku отфильтровываются
    """
    assert_that(
        get_result_offer_by_offer_id(main_idx, offer_id),
        user_friendly_dict_equal_to(None),
        'offer with unknown sku not exists in results',
    )


def test_blue_offers_enrich_with_fast_sku(main_idx, pipeline_params):
    """
    Проверяем обогащение синих оферов данными, если выставлен флаг быстрой sku
    """

    if pipeline_params.enrich_blue_offers_from_fast_sku:
        # оффер обогащается из быстрой sku (т.е. данными из оффера),
        # а также обогащается shop_id от виртуального магазина Beru
        # получает офферную картинку
        offerWithFastSku = make_full_blue_offer_dict_fast_sku(
            msku=114,
            feed_id=33,
            offer_id='offerWithFastSku',
            title=OFFER_FROM_TITLE,
            serialization=Serialization.DICT,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            market_sku_type=MARKET_SKU_TYPE_FAST,
            picURLS='http://ya.ru/2.jpeg\thttp://ya.ru/3.jpeg\thttp://ya.ru/1.jpeg',
            picUrlIds=[
                'some_picture_id_2',
                'some_picture_id_3',
                'some_picture_id_1',
            ],
            pic=[
                {'crc': 'crc_test_picture_2', 'height': 1000, 'width': 800},
                {'crc': 'crc_test_picture_3', 'height': 600, 'width': 600},
                {'crc': 'crc_test_picture_1', 'height': 1200, 'width': 900},
            ],
        )
    else:
        # оффер выбрасывается
        offerWithFastSku = None

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'offerWithFastSku'),
        offerWithFastSku,
    )

    # быстрая карточка никогда не пишется в таблицу офферов
    assert_that(
        get_result_offer_by_offer_id(main_idx, 'MS114'),
        user_friendly_dict_equal_to(None),
        'msku114',
    )

    # оффер с реальной msku всегда обогащается из неё, даже если имеет флаг быстрой sku
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOfferWithFastSku'),
        make_full_blue_offer_dict(
            msku=222,
            feed_id=44,
            offer_id='knownOfferWithFastSku',
            title="Генератор чёрных дыр портативный",
            history_price=2000000000,
            reference_oldprice=2222,
            history_price_is_valid=False,
            price_limit=222220000000,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
            market_category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.DISCOUNT_RESTRICTED.value | OfferFlags.BLUE_OFFER.value,
            offer_conversion=[
                {
                    "conversion_value": 0.2,
                    "conversion_type": "SPB"
                }
            ],
            barcode=BARCODE_FROM_MSKU,
            buybox_offer=False,
            price=1235,
            market_sku_type=MARKET_SKU_TYPE_FAST,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_skip_experimental_msku(main_idx):
    """
    Проверяем, что экспериментальные пока что не портят оригинальные
    """

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'offer_under_msku_with_contex'),
        make_full_blue_offer_dict(
            msku=3000,
            feed_id=11,
            offer_id='offer_under_msku_with_contex',
            title="МСКУ, у которого есть эксперимент",
            contex_info={
                "experiment_id": "experiment_3000",
                "original_msku_id": 3000,
                "experimental_msku_id": 3001,
            },
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'MS3000'),
        make_msku_contex_dict_ware_md5(
            msku=3000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title='МСКУ, у которого есть эксперимент',
            set_msku_row=False,
            contex_info={
                "experiment_id": "experiment_3000",
                "original_msku_id": 3000,
                "experimental_msku_id": 3001,
            },
        )
    )

    assert_that(
        get_result_offer_by_offer_id(main_idx, 'MS3001'),
        user_friendly_dict_equal_to(None),
        'msku3001',
    )


def test_msku_without_offers(main_idx, pipeline_params):
    """
    Проверяем, что msku без офферов остаются/отфильтровываются в main-idx
    """
    if pipeline_params.drop_msku_without_offers:
        # в результирующих таблицах нет мску, под которым нет офферов
        expected_msku333 = None
        expected_msku2000 = None
    else:
        # в результирующие таблицы попал мску, под которым нет офферов
        expected_msku333 = make_msku_contex_dict_ware_md5(
            msku=333,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="Кубик рубика с автографом Криштиану Роналду",
            set_msku_row=False,
        )
        expected_msku2000 = make_msku_contex_dict_ware_md5(
            msku=2000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title="МСКУ, у которого нет офферов",
            set_msku_row=False,
        )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'MS333'),
        expected_msku333,
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'MS2000'),
        expected_msku2000,
    )


def test_blue_offers_enrich_when_price_greater_than_price_limit(main_idx):
    """
    Проверяем, что офферу не устанавливается признак buybox, если его цена превышает предельную
    """

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer6'),
        make_full_blue_offer_dict(
            msku=555,
            feed_id=33,
            offer_id='knownOffer6',
            title="MSKU для которого нет ни одного оффера, с ценой ниже предельной",
            history_price=None,
            buybox_offer=False,
            price_limit=990000000,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
            price=1000000000,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_blue_offers_compare_price_limit_with_precision(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOffer7'),
        make_full_blue_offer_dict(
            msku=666,
            feed_id=11,
            offer_id='knownOffer7',
            title="при сравнении price_limit домножается на 10^7",
            history_price=None,
            price=10990000000,
            price_limit=11000000000,
            ref_min_price=11000000000,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_blue_offers_without_ref_min_price(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'noRefMinOffer'),
        make_full_blue_offer_dict(
            msku=777,
            feed_id=11,
            offer_id='noRefMinOffer',
            title="минимальная референсная цена отсутствует",
            history_price=None,
            price=1000000000,
            ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_blue_offers_ref_min_price_warning(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'highPriceWarnOffer'),
        make_full_blue_offer_dict(
            msku=888,
            feed_id=22,
            offer_id='highPriceWarnOffer',
            title="референсная цена слишком подозрительная",
            history_price=None,
            price=2000000000,
            ref_min_price=None,
            # для msku у данного оффера уровень "подозрительности" минимальной референсной цены слишком высокий
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_blue_offers_golden_matrix(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'goldenMatrix1_1'),
        make_full_blue_offer_dict(
            msku=999,
            feed_id=11,
            offer_id='goldenMatrix1_1',
            title="Товар из золотой матрицы #1",
            history_price=None,
            price=2000000000,
            ref_min_price=None,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.IS_GOLDEN_MATRIX.value | OfferFlags.BLUE_OFFER.value,
            # принадлежит Золотой матрице
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'goldenMatrix1_2'),
        make_full_blue_offer_dict(
            msku=999,
            feed_id=22,
            offer_id='goldenMatrix1_2',
            title="Товар из золотой матрицы #1",
            history_price=None,
            buybox_offer=False,
            price=3000000000,
            ref_min_price=None,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.IS_GOLDEN_MATRIX.value | OfferFlags.BLUE_OFFER.value,
            # принадлежит Золотой матрице
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'goldenMatrix2_1'),
        make_full_blue_offer_dict(
            msku=1000,
            feed_id=11,
            offer_id='goldenMatrix2_1',
            title="Товар из золотой матрицы #2",
            history_price=None,
            price=4000000000,
            ref_min_price=None,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.IS_GOLDEN_MATRIX.value | OfferFlags.BLUE_OFFER.value,
            # принадлежит Золотой матрице
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'noRefMinOffer'),
        make_full_blue_offer_dict(
            msku=777,
            feed_id=11,
            offer_id='noRefMinOffer',
            title="минимальная референсная цена отсутствует",
            history_price=None,
            price=1000000000,
            ref_min_price=None,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,  # msku не из Золотой матрицы
            barcode=BARCODE_FROM_MSKU,
        )
    )


def test_blue_offers_ref_min_price_warning_with_setup(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'highPriceWarnOfferSetup'),
        make_full_blue_offer_dict(
            msku=1111,
            feed_id=22,
            offer_id='highPriceWarnOfferSetup',
            title="референсная цена слишком подозрительная по заданным настройкам",
            history_price=None,
            price=2000000000,
            ref_min_price=None,  # именно ref_min_price говорит о том, что цена была выфильтрована
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
        )
    )


def test_blue_offers_buybox_elasticity(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'buyboxElasticityOfferSetup'),
        make_full_blue_offer_dict(
            msku=1001,
            feed_id=BERU_FEED,
            offer_id='buyboxElasticityOfferSetup',
            title="with buybox elasticity",
            history_price=None,
            price=2000000000,
            buybox_elasticity=[
                {'price_variant': {'price': 101 * pow(10, 7)}, 'demand_mean': 10.1},
                {'price_variant': {'price': 102 * pow(10, 7)}, 'demand_mean': 9.}
            ],
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
        )
    )


def test_blue_offers_urls(main_idx, pipeline_params, source_msku_contex, source_blue_offers_raw):
    """
    Проверяем количество офферов и контент на выходе main-idx в таблице blue_offers_urls
    """
    offers = main_idx.outputs['blue_offers_urls']

    # в результирующей таблице всегда нет:
    #   - офферов, для которых нет msku или быстрой sku (offerWithUnknownSku, offerWithUnknownSku2)
    #   - экспериментальных msku (MS3001)
    #   - офферов, для которых есть быстая sku (offerWithFastSku) (потому что для неё нет постоянного url)
    # blue + msku - 2 (offerWithUnknownSku, offerWithUnknownSku) - 1 (MS3001) - 1 (offerWithFastSku)
    expected = len(source_blue_offers_raw) + len(source_msku_contex) - 3 - 1 - 1

    if pipeline_params.drop_msku_without_offers:
        # также в результирующей таблице нет msku, под которыми нет офферов (MS2000 and MS333)
        expected -= 2

    assert_that(
        offers,
        all_of(
            has_length(expected),
            has_item(has_entries({'url': 'market.yandex.ru/product/1713074440?sku=1', 'path': '/product/1713074440?sku=1', 'msku': 1, 'is_fake_msku_offer': True})),
            has_item(has_entries({'url': 'market.yandex.ru/product/1713074440?sku=111', 'path': '/product/1713074440?sku=111', 'msku': 111, 'is_fake_msku_offer': False})),
        ),
        'Has record with \'path\' tag in \'blue_offers_urls\' table'
    )


def test_deleted_msku(main_idx, pipeline_params):
    """
    Проверяем, что создается таблица с идентификаторами msku, для офферов которых не удалось найти карточку
    """
    deleted_msku = main_idx.outputs['deleted_msku']

    # всегда кладём в таблицу msku оффера, если она отсутствует в таблице msku и это не быстрая sku
    expected = [
        {'msku': 112},
        {'msku': 113},
        {'msku': 115},
    ]

    if not pipeline_params.enrich_blue_offers_from_fast_sku:
        # также кладём в таблицу быструю sku
        expected.append({'msku': 114})

    assert_that(
        deleted_msku,
        all_of(
            has_length(len(expected)),
            has_items(*[
                has_entries(entry) for entry in expected
            ])
        )
    )


def test_blue_pictures(main_idx, pipeline_params, in_picrobot_success_data):
    """
    Проверяем приклеивание картинок из стэйта пикробота
    """
    if not pipeline_params.enrich_blue_offers_from_fast_sku and not pipeline_params.enrich_blue_offers_from_resale:
        return

    assert_that(
        main_idx.outputs['blue_offer_to_pic'],
        equal_to(
            [
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'anoter_picture_id',
                    'msku': 115,
                    'offer_id': 'some_offer_id',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_1',
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_1',
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                },
                {
                    'descr_url': None,
                    'feed_id': 44,
                    'id': 'some_picture_id_1',
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_2',
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_2',
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                },
                {
                    'descr_url': None,
                    'feed_id': 44,
                    'id': 'some_picture_id_2',
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_3',
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                },
                {
                    'descr_url': None,
                    'feed_id': 33,
                    'id': 'some_picture_id_3',
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                },
                {
                    'descr_url': None,
                    'feed_id': 44,
                    'id': 'some_picture_id_3',
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                },
            ]
        )
    )
    assert_that(
        main_idx.outputs['joined_blue_offer_to_pic'],
        equal_to(
            [
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_1',
                    'is_good_size_pic': True,
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                    'pic': in_picrobot_success_data[0]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_2',
                    'is_good_size_pic': True,
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                    'pic': in_picrobot_success_data[1]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_3',
                    'is_good_size_pic': True,
                    'msku': 114,
                    'offer_id': 'offerWithFastSku',
                    'pic': in_picrobot_success_data[2]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_1',
                    'is_good_size_pic': True,
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                    'pic': in_picrobot_success_data[0]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_2',
                    'is_good_size_pic': True,
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                    'pic': in_picrobot_success_data[1]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 33,
                    'id': 'some_picture_id_3',
                    'is_good_size_pic': True,
                    'msku': 115,
                    'offer_id': 'offerWithIsResale',
                    'pic': in_picrobot_success_data[2]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 44,
                    'id': 'some_picture_id_1',
                    'is_good_size_pic': True,
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                    'pic': in_picrobot_success_data[0]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 44,
                    'id': 'some_picture_id_2',
                    'is_good_size_pic': True,
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                    'pic': in_picrobot_success_data[1]['pic'],
                    'ts': 0,
                },
                {
                    'descr_tags': '',
                    'feed_id': 44,
                    'id': 'some_picture_id_3',
                    'is_good_size_pic': True,
                    'msku': 222,
                    'offer_id': 'knownOfferWithIsResale',
                    'pic': in_picrobot_success_data[2]['pic'],
                    'ts': 0,
                },
            ]
        )
    )


def test_max_price_and_max_old_price(main_idx):
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'maxOldPrice_1'),
        make_full_blue_offer_dict(
            msku=9993,
            feed_id=11,
            offer_id='maxOldPrice_1',
            title="Товар с max_price и max_old_price",
            history_price=None,
            price=2000000000,
            max_price=3000000000.,
            max_old_price=4000000000.,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            offer_flags64=OfferFlags.BLUE_OFFER.value,
            barcode=BARCODE_FROM_MSKU,
            virtual_shop_id=BERU_SHOP
        )
    )


def test_blue_offers_enrich_with_resale(main_idx, pipeline_params):
    """
    Проверяем обогащение синих оферов данными, если выставлен флаг б/у
    """

    knownOfferWithIsResale = make_full_blue_offer_dict(
        msku=222,
        feed_id=44,
        offer_id='knownOfferWithIsResale',
        title="Генератор чёрных дыр портативный",
        history_price=2000000000,
        reference_oldprice=2222,
        history_price_is_valid=False,
        price_limit=222220000000,
        ref_min_price=None,  # для msku у данного оффера нет минимальной референсной цены
        market_category_id=CATEGORY_WITH_RESTRICTED_DISCOUNT,
        offer_flags64=OfferFlags.IS_RESALE.value | OfferFlags.BLUE_OFFER.value | OfferFlags.DISCOUNT_RESTRICTED.value,
        barcode=BARCODE_FROM_MSKU,
        buybox_offer=False,
        price=1235,
        virtual_shop_id=BERU_SHOP,
        picURLS='http://ya.ru/2.jpeg\thttp://ya.ru/3.jpeg\thttp://ya.ru/1.jpeg' if pipeline_params.enrich_blue_offers_from_resale else None,
        picUrlIds=[
            'some_picture_id_2',
            'some_picture_id_3',
            'some_picture_id_1',
        ] if pipeline_params.enrich_blue_offers_from_resale else None,
        pic=[
            {'crc': 'crc_test_picture_2', 'height': 1000, 'width': 800},
            {'crc': 'crc_test_picture_3', 'height': 600, 'width': 600},
            {'crc': 'crc_test_picture_1', 'height': 1200, 'width': 900},
        ] if pipeline_params.enrich_blue_offers_from_resale else None,
    )

    # оффер без карточки не пишется в таблицу офферов
    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'offerWithIsResale'),
        None,
    )

    compare_genlog_field(
        get_result_offer_by_offer_id(main_idx, 'knownOfferWithIsResale'),
        knownOfferWithIsResale
    )
