# coding: utf-8

import datetime
import pytest

from copy import deepcopy
from google.protobuf.json_format import MessageToDict
from hamcrest import (
    assert_that,
    equal_to,
    has_item,
    has_entries,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
    BlueHistoryPriceTable,
    BlueHistoryPriceDropsTable,
    Offers2ModelTable,
    Offers2ParamTable,
    BluePriceLimitTable,
    BlueDcoUploadTable,
    BlueGoldenMatrixTable,
    DiscountCategoryRestrictions,
    DiscountMskuRestrictions,
    OfferConversionDataTable,
    BlueBuyboxElasticityTable,
)
from market.idx.yatf.resources.yt_tables.lbdumper_tables import (
    LbDumperAmoreTable,
    LbDumperAmoreBeruSupplierTable,
    LbDumperAmoreBeruVendorTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    make_dco_datacamp_dict,
    make_msku_contex_dict,
    Serialization,
    serialize_proto,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags, DisabledFlags

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as OfferPb
from market.proto.ir.UltraController_pb2 import EnrichedOffer as EnrichedOfferPb

from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampDcoTable
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)


from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
BERU_SHOP = 431782
BERU_FEED = 475690
HALF_MODE = False

CATEGORY_WITH_RESTRICTED_DISCOUNT = 1
MSKU_WITH_RESTRICTED_DISCOUNT = 111

BARCODE_FROM_MSKU = '100500'


@pytest.yield_fixture(scope='module')
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name='Все товары',
            name='Все товары',
            output_type=MboCategory.GURULIGHT,
        ),
    ]


def make_hprice_dict(history_price):
    return {
        'date_yyyymmdd': 20180831,
        'depth': 5,
        'min_price_date_yyyymmdd': 20180831,
        'min_price_expression': 'RUR {0}'.format(history_price),
        'price_expression': 'RUR {0}'.format(history_price)
    }


def make_blue_history_pricedrops_dict(history_price, is_valid):
    history_price_dict = make_hprice_dict(history_price)
    history_price_dict.update({'is_valid': is_valid})
    return history_price_dict


def make_raw_blue_offer_dict(
        msku,
        feed_id,
        offer_id,
        serialization=Serialization.STRING,
        price=1234,
        offer_flags=0,
        offer_flags64=0,
        disabled_flags=0,
        disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
):
    offer = {
        'Comment': '',
        'DeliveryBucketIds': [1, 2, 3],
        'DeliveryCalcGeneration': 444,
        'DeliveryCurrency': 'USD',
        'HasDelivery': True,
        'MinQuantity': 1,
        'StepQuantity': 1,
        'URL': '',
        'UseYmlDelivery': False,
        'autobroker_enabled': 1,
        'barcode': '100131944720',
        'binary_price': {'price': price, 'id': 'RUR', 'ref_id': 'RUR'},
        'classifier_good_id': '0a3fbe800d556c6f93361dd169c162b5',
        'classifier_magic_id2': '107f289d902ecc67fa0c5d6a4bfe0f2f',
        'cpa': 4,
        'datasource_name': 'horns and hooves datasource',
        'deliveryIncluded': 0,
        'description': '',
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
        'offer_params': 'bla-bla',
        'phone': '',
        'pickup': 'true',
        'price_expression': '{}.000000 1 0 RUR RUR'.format(price),
        'price_scheme': "10;9': 5;",
        'priority_regions': '213',
        'regionAttributes': '213',
        'ru_price': price,
        'shop_name': 'ООО Рога и копыта',
        'shop_sku': str(offer_id),
        'store': 'true',
        'title': '',
        'type': 11,
        'ware_md5': 'hc1cVZiClnllcxjhGX0_cQ',
        'weight': 0.5,
        'width': 21.0,
        'yx_bid': 0,
        'yx_cbid': 0,
        'yx_ds_id': 10263793,
        'yx_money': 0,
        'yx_shop_categid': '1',
        'yx_shop_isbn': '',
        'yx_shop_name': 'ООО Рога и копыта',
        'yx_shop_offer_id': str(offer_id),
    }

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
        'market_category_name': 'Качели и шезлонги для малышей',
        'market_model_name': 'Jetem Dreamer',
        'market_sku_id': int(msku),
        'market_sku_name': 'Jetem Шезлонг Jetem Dreamer red',
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
        'sku': 'deprecated',
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
    history_price=None,
    reference_oldprice=None,
    history_price_is_valid=None,
    serialization=Serialization.STRING,
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
    uc_model_id=1713074440L,
    buybox_elasticity=None,
    barcode=None,
    msku_exp=0,
):
    msku_id = msku_exp if msku_exp != 0 else msku
    offer = {
        'Comment': '',
        'DeliveryBucketIds': [1, 2, 3],
        'DeliveryCalcGeneration': 444,
        'DeliveryCurrency': 'USD',
        'HasDelivery': True,
        'MinQuantity': 1,
        'StepQuantity': 1,
        'URL': 'https://market.yandex.ru/product/' + str(uc_model_id) + "?sku=" + str(msku),
        'UseYmlDelivery': False,
        'autobroker_enabled': 1,
        'barcode': barcode or '100131944720',
        'binary_price': {'price': price, 'id': 'RUR', 'ref_id': 'RUR'},
        'classifier_good_id': '0a3fbe800d556c6f93361dd169c162b5',
        'classifier_magic_id2': '107f289d902ecc67fa0c5d6a4bfe0f2f',
        'cpa': 4,
        'datasource_name': 'Яндекс.Маркет',
        'deliveryIncluded': 0,
        'description': 'Не содержит изотопов урана 235 и других радионуклидов',
        'feed_id': int(feed_id),
        'fulfillment_shop_id': int(BERU_SHOP),
        'height': 15.0,
        'is_blue_offer': True,
        'length': 46.0,
        'market_sku': int(msku_id),
        'offer_conversion': offer_conversion,
        'offer_params': '',
        'offer_flags': offer_flags | OfferFlags.BLUE_OFFER.value,
        'offer_flags64': offer_flags64 | OfferFlags.BLUE_OFFER.value,
        'disabled_flags': disabled_flags,
        'disabled_flag_sources' : disabled_flag_sources,
        'phone': '',
        'pickup': 'true',
        'price_expression': '{}.000000 1 0 RUR RUR'.format(price),
        'price_scheme': "10;9': 5;",
        'priority_regions': '213',
        'regionAttributes': '213',
        'ru_price': price,
        'shop_name': 'Беру',
        'shop_sku': str(offer_id),
        'store': 'true',
        'title': title,
        'type': 11,
        'ware_md5': 'hc1cVZiClnllcxjhGX0_cQ',
        'weight': 0.5,
        'width': 21.0,
        'yx_bid': 0,
        'yx_cbid': 0,
        'yx_ds_id': int(BERU_SHOP),
        'yx_money': 0,
        'yx_shop_categid': '1',
        'yx_shop_isbn': '',
        'yx_shop_name': 'Беру',
        'yx_shop_offer_id': str(offer_id),
        'quality_rating': 5,
        'picURLS': 'https://avatars.mds.yandex.net/get-mpic/175985/img_id456/orig\t'
                   'https://avatars.mds.yandex.net/get-mpic/175985/img_id789/orig',
        'vendor': '',
        'model': 'SKU со всеми данными',
        'market_category_id': market_category_id if market_category_id else 12345,
        'sales_notes': '',
        'buybox_offer': buybox_offer,
        'buybox_elasticity': buybox_elasticity,
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

    if amore_beru_supplier_data:
        offer['amore_beru_supplier_data'] = amore_beru_supplier_data

    if amore_beru_vendor_data:
        offer['amore_beru_vendor_data'] = amore_beru_vendor_data

    if contex_info:
        offer['contex_info'] = contex_info

    uc = {
        'category_id': market_category_id if market_category_id else 12345,
        'classification_type_value': 7,
        'classifier_category_id': market_category_id if market_category_id else 12345,
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
        'market_category_name': 'Качели и шезлонги для малышей',
        'market_model_name': 'Jetem Dreamer',
        'market_sku_id': int(msku),
        'market_sku_name': 'Jetem Шезлонг Jetem Dreamer red',
        'matched_category_id': market_category_id if market_category_id else 12345,
        'matched_id': int(uc_model_id),
        'matched_vendor_id': 966973L,
        'model_id': int(uc_model_id),
        'not_approved_sku_id': -1,
        'old_category_id': 0,
        'price_check': 0.0,
        'probability': 0.0,
        'processed_time': 1517750597448,
        'second_category_id': 0,
        'second_classification_type_value': 8,
        'second_probability': 0.0,
        'sku': 'deprecated',
        'skuStatus': 1,
        'tovar_category_id': market_category_id if market_category_id else 12345,
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

    offer['genlog'] = GenlogRow.fromOfferData(offer, default_data=False)
    offer = serialize_proto(offer, OfferPb, serialization)
    uc = serialize_proto(uc, EnrichedOfferPb, serialization)

    return {
        'feed_id': int(feed_id),
        'offer_id': str(offer_id),
        'session_id': 200,
        'offer': offer,
        'uc': uc,
        'params': params,
        'ware_md5': 'hc1cVZiClnllcxjhGX0_cQ',
    }


def make_blue_hprice_dict(msku, history_price, serialization=Serialization.STRING):
    offer = {
        'price_history': make_hprice_dict(history_price),
        'pricedrops_tests_passed': True,
    }

    offer = serialize_proto(offer, OfferPb, serialization)

    return {
        'msku': int(msku),
        'offer': offer,
        'history_price': int(history_price),
    }


def make_blue_pricedrops_dict(msku, history_price, serialization=Serialization.STRING, is_valid_history=False):
    offer = {
        'price_history': make_blue_history_pricedrops_dict(history_price, is_valid_history),
        'pricedrops_tests_passed': True,
    }

    offer = serialize_proto(offer, OfferPb, serialization)

    return {
        'msku': int(msku),
        'offer': offer,
        'history_price': int(history_price),
    }


def make_blue_price_limit_dict(msku, price_limit):
    return {
        'market_sku': int(msku),
        'max_avaliable_price': float(price_limit),
    }


def make_blue_ref_min_price_dict(msku, ref_min_price, ref_min_price_warning, max_warning_level):
    return {
        'market_sku': int(msku),
        'ref_min_price': float(ref_min_price),
        'ref_min_price_warning': int(ref_min_price_warning),
        'max_warning_lvl': int(max_warning_level)
    }


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
    result['msku'] = int(offer['market_sku'])
    result['is_fake_msku_offer'] = True
    result['cpa'] = 0

    return result


@pytest.yield_fixture(scope='module')
def source_blue_offers_raw():
    return [
        make_raw_blue_offer_dict(msku=111, feed_id=11, offer_id='knownOffer1'),
        make_raw_blue_offer_dict(msku=222, feed_id=11, offer_id='knownOffer2'),
        make_raw_blue_offer_dict(msku=111, feed_id=22, offer_id='knownOffer3'),

        # offer for original and experimental msku
        make_raw_blue_offer_dict(
            msku=3000,
            feed_id=11,
            offer_id='offer_under_msku_with_contex',
        ),
    ]


@pytest.yield_fixture(scope='module')
def source_msku_contex():
    return [
        # оригинальный мску, у которого есть экспериментальный
        make_msku_contex_dict(
            msku=3000,
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title='МСКУ, у которого есть эксперимент',
            contex_info={
                'experimental_msku_id': 3001,
            },
            uc_params=[
                {
                    'param_id': 1000001L,
                    'number_value': 127.0,
                },
                {
                    'param_id': 1000003L,
                    'value_id': 12109936L,
                }
            ],
            uc_model_id=50000L,
        ),
        # экспирементальные мску должны иметь такой же id, как у оригинального
        make_msku_contex_dict(
            msku=3000,
            msku_exp=3001,
            msku_experiment_id='',
            feed_id=BERU_FEED,
            shop_id=BERU_SHOP,
            title='Экспериментальный МСКУ',
            contex_info={
                'experiment_id': 'experiment_3000',
                'original_msku_id': 3000,
                'is_experimental': True,
            },
            uc_params=[
                {
                    'param_id': 1000002L,
                    'value_id': 12976298L,
                },
                {
                    'param_id': 1000003L,
                    'value_id': 12109936L,
                }
            ],
            uc_model_id=60000L,
        ),
    ]


@pytest.yield_fixture(scope='module')
def source_blue_hprice():
    return [
        make_blue_hprice_dict(msku=111, history_price=1000000000),
        make_blue_hprice_dict(msku=222, history_price=2000000000),
        make_blue_hprice_dict(msku=333, history_price=3000000000),
    ]


@pytest.yield_fixture(scope='module')
def source_dco():
    return [
        make_dco_datacamp_dict(msku=111, oldprice=1111),
        make_dco_datacamp_dict(msku=222, oldprice=2222),
    ]


@pytest.yield_fixture(scope='module')
def source_blue_pricedrops():
    return [
        make_blue_pricedrops_dict(msku=111, history_price=1000000000, is_valid_history=True),
        make_blue_pricedrops_dict(msku=222, history_price=2000000000, is_valid_history=False),
        make_blue_pricedrops_dict(msku=333, history_price=3000000000, is_valid_history=True),
    ]


@pytest.yield_fixture(scope='module')
def source_blue_price_limit():
    return [
        make_blue_price_limit_dict(msku=111, price_limit=11111),
        make_blue_price_limit_dict(msku=222, price_limit=22222),
        make_blue_price_limit_dict(msku=555, price_limit=99),
        make_blue_price_limit_dict(msku=666, price_limit=1100.0),
    ]


@pytest.yield_fixture(scope='module')
def source_blue_ref_min_price():
    return [
        make_blue_ref_min_price_dict(msku=111, ref_min_price=11111, ref_min_price_warning=1, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=666, ref_min_price=1100.0, ref_min_price_warning=0, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=777, ref_min_price=-1, ref_min_price_warning=10, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=888, ref_min_price=15.0, ref_min_price_warning=5, max_warning_level=3),
        make_blue_ref_min_price_dict(msku=1111, ref_min_price=15.0, ref_min_price_warning=2, max_warning_level=1),
    ]


@pytest.yield_fixture(scope='module')
def source_discount_category_restrictions():
    return [
        {
            'hid': CATEGORY_WITH_RESTRICTED_DISCOUNT,
        },
    ]


@pytest.yield_fixture(scope='module')
def source_discount_msku_restrictions():
    return [
        {
            'msku': MSKU_WITH_RESTRICTED_DISCOUNT,
        },
    ]


@pytest.yield_fixture(scope='module')
def source_offer_conversion_data():
    '''
    Конверсии присоединяются по feed_id+offer_id, а не по msku.
    У офферов knownOffer1 и knownOffer3 msku одинаковые, а конверсии разные.
    '''
    return [
        {'conversion_value': 0.2, 'feed_id': 11, 'offer_id': 'knownOffer2', 'conversion_type': 'MSK'},
        {'conversion_value': 0.1, 'feed_id': 11, 'offer_id': 'knownOffer1', 'conversion_type': 'SPB'},
        {'conversion_value': 0.3, 'feed_id': 22, 'offer_id': 'knownOffer3', 'conversion_type': 'SPB'},
    ]


@pytest.yield_fixture(scope='module')
def source_amore_data():
    '''
    Данные об автостратегии оффера присоединяются по feed_id+offer_id, а не по msku.
    У офферов knownOffer1 и knownOffer3 msku одинаковые, а данные об автостратегии разные.
    '''
    return [
        {
            'amore_data': 'posdrrcpo',
            'feed_id': 11,
            'offer_id': 'knownOffer1',
            'amore_ts_sec': 9000,
            'shop_id': 1,
            'warehouse_id': 1
        },
    ]


@pytest.yield_fixture(scope="module")
def source_amore_beru_supplier_data():
    return [
        {'amore_beru_supplier_data': 'posdrrcpo-bs', 'feed_id': 11, 'offer_id': "knownOffer1", 'amore_ts_sec': 9000, 'shop_id': 1, 'warehouse_id': 1},
    ]


@pytest.yield_fixture(scope="module")
def source_amore_beru_vendor_data():
    return [
        {'amore_beru_vendor_data': 'posdrrcpo-bv', 'feed_id': 11, 'offer_id': "knownOffer1", 'amore_ts_sec': 9000, 'shop_id': 1, 'warehouse_id': 1},
    ]


@pytest.yield_fixture(scope='module')
def source_blue_golden_matrix():
    return [
        {'market_sku_id': 1000},
        {'market_sku_id': 999},
    ]


@pytest.yield_fixture(scope='module')
def source_blue_buybox_elasticity_data():
    return [
        {'sku': 111, 'price_variant': 101, 'demand_mean': 10.1},
        {'sku': 111, 'price_variant': 102, 'demand_mean': 9.},
        {'sku': 666, 'price_variant': 1020, 'demand_mean': 0.1},
    ]


@pytest.fixture(scope='module')
def or3_config_data(yt_server):
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
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'blue_price_validation_enabled': 'true',
            'thirdparty_dco_enabled': 'true',
            'buybox_elasticity_enabled': 'true',
        }
    }


@pytest.yield_fixture(scope='module')
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope='module')
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
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data={}
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
            path=ypath_join(yt_home_path, 'history', 'blue', 'prices', 'hprices', '20180831'),
            data=source_blue_hprice,
        ),
        'dco': DataCampDcoTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'datacamp', 'blue', 'dco'),
            data=source_dco,
        ),
        'blue_pricedrops': BlueHistoryPriceDropsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'history', 'blue', 'prices', 'hprices_pricedrops', '20180831'),
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
    }


@pytest.yield_fixture(scope='module')
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree):
    yt_client = yt_server.get_yt_client()

    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_client.exists(path), 'Table {} doesn\'t exist'.format(path))

    blue_hprice_table = source_yt_tables['blue_hprice']
    yt_client.link(blue_hprice_table.get_path(),
                   or3_config.options['yt']['yt_blue_price_validation_table'])
    blue_pricedrops_table = source_yt_tables['blue_pricedrops']
    yt_client.link(blue_pricedrops_table.get_path(),
                   ypath_join(or3_config.options['yt']['home_dir'],
                              or3_config.options['yt']['yt_blue_pricedrops_validation_table']))

    yt_home_path = or3_config.options['yt']['home_dir']
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }
    with Or3MainIdxTestEnv(
            yt_server,
            GENERATION,
            MI3_TYPE,
            COUNT_SHARDS,
            HALF_MODE,
            enable_contex=True,
            **resources
    ) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offers_count(main_idx):
    offers = main_idx.outputs['blue_offers']
    assert_that(
        len(offers),
        equal_to(4),  # MS3000, MS3001 + 2 * offer_under_msku_with_contex
    )


def test_original_msku(main_idx):
    '''
    Проверяем, оригинальную мску
    '''
    result_offers = main_idx.outputs['offers_by_offer_id']
    original_msku = deepcopy(result_offers['MS3000'])
    expected_msku = make_msku_contex_dict_ware_md5(
        msku=3000,
        feed_id=BERU_FEED,
        shop_id=BERU_SHOP,
        title='МСКУ, у которого есть эксперимент',
        set_msku_row=False,
        uc_params=[
            {
                'param_id': 1000001L,
                'number_value': 127.0,
            },
            {
                'param_id': 1000003L,
                'value_id': 12109936L,
            }
        ],
        uc_model_id=50000L,
        contex_info={
            'experiment_id': 'experiment_3000',
            'experimental_msku_id': 3001,
        },
    )
    for k in expected_msku['offer']['genlog']:
        assert expected_msku['offer']['genlog'][k] == original_msku['genlog'][k], k


def test_experimental_msku(main_idx):
    '''
    Проверяем, экспериментальную мску
    '''
    result_offers = main_idx.outputs['offers_by_offer_id']
    experimental_msku = deepcopy(result_offers['MS3001'])
    expected_msku = make_msku_contex_dict_ware_md5(
        msku=3000,
        msku_exp=3001,
        feed_id=BERU_FEED,
        shop_id=BERU_SHOP,
        title='Экспериментальный МСКУ',
        set_msku_row=False,
        uc_params=[
            {
                'param_id': 1000002L,
                'value_id': 12976298L,
            },
            {
                'param_id': 1000003L,
                'value_id': 12109936L,
            }
        ],
        uc_model_id=50000L,
        contex_info={
            'original_msku_id': 3000,
            'is_experimental': True,
            'experiment_id': 'experiment_3000',
        },
    )
    for k in expected_msku['offer']['genlog']:
        assert expected_msku['offer']['genlog'][k] == experimental_msku['genlog'][k], k


def test_msku_randx(main_idx):
    '''
    Проверяем, что randx оригинальной и экспериментальной мску совпадают
    '''
    result_offers = main_idx.outputs['offers_by_offer_id']
    original_msku_randx = result_offers['MS3000']['offer']['randx']
    experimental_msku_randx = result_offers['MS3001']['offer']['randx']
    assert_that(
        original_msku_randx,
        equal_to(experimental_msku_randx),
        'randx not eqauls'
    )


def test_msku_offers(main_idx):
    '''
    Проверяем, оффер под оригинальной и экспериментальной мску
    '''
    result_offers_all = main_idx.outputs['blue_offers']
    result_offers = [
        offer['offer']
        for offer in result_offers_all
        if offer['offer_id'] == 'offer_under_msku_with_contex'
    ]

    expected = [
        # исходный оффер
        make_full_blue_offer_dict(
            msku=3000,
            feed_id=11,
            offer_id='offer_under_msku_with_contex',
            title='МСКУ, у которого есть эксперимент',
            serialization=Serialization.PROTO,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            contex_info={
                'experiment_id': 'experiment_3000',
                'experimental_msku_id': 3001,
            },
            barcode=BARCODE_FROM_MSKU,
            uc_model_id=50000L,
        )['offer'],

        # клон
        make_full_blue_offer_dict(
            msku=3000,
            msku_exp=3001,
            feed_id=11,
            offer_id='offer_under_msku_with_contex',
            title='Экспериментальный МСКУ',
            serialization=Serialization.PROTO,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            contex_info={
                'experiment_id': 'experiment_3000',
                'original_msku_id': 3000,
                'is_experimental': True,
            },
            barcode=BARCODE_FROM_MSKU,
            uc_model_id=50000L,
        )['offer'],
    ]

    for expected_offer in expected:
        item = MessageToDict(expected_offer, preserving_proto_field_name=True)['genlog']
        assert_that(result_offers, has_item(
            has_entries({
                'contex_info': item['contex_info'],
                'title': item['title'],
                'feed_id': item['feed_id'],
                'yx_shop_offer_id': item['offer_id'],
                'market_sku': item['market_sku'],
            }),
        ))


def test_msku_offers_in_white(main_idx):
    '''
    Проверяем, оффер под оригинальной и экспериментальной мску в белом индексе
    '''
    result_offers_all = main_idx.outputs['offers']
    result_offers = [
        offer['offer']
        for offer in result_offers_all
        if offer['offer_id'] == 'offer_under_msku_with_contex'
    ]

    assert_that(len(result_offers), equal_to(2))


def test_msku_offers_randx(main_idx):
    '''
    Проверяем, что randx оригинальной и экспериментальной мску совпадают
    '''
    result_offers_all = main_idx.outputs['blue_offers']
    result_offers = [
        offer['offer']['randx']
        for offer in result_offers_all
        if offer['offer_id'] == 'offer_under_msku_with_contex'
    ]
    assert_that(len(result_offers), equal_to(2), 'incorrect length')
    assert_that(
        result_offers[0],
        equal_to(result_offers[1]),
        'randx equals'
    )


def test_msku_offers_uc(main_idx):
    '''
    Проверяем, что model_id оригинальной и экспериментальной мску совпадают
    '''
    result_offers_all = main_idx.outputs['blue_offers']
    result_offers = [
        offer['uc']['model_id']
        for offer in result_offers_all
        if offer['offer_id'] == 'offer_under_msku_with_contex'
    ]

    assert_that(len(result_offers), equal_to(2), 'incorrect length')
    assert_that(
        result_offers[0],
        equal_to(result_offers[1]),
        'model_id equals'
    )
