#!/usr/bin/env python
# coding: utf-8

import base64
import pytest

from hamcrest import (
    assert_that,
    is_not,
    all_of,
)

from market.idx.datacamp.proto.offer.OfferMapping_pb2 import Mapping as MappingPb
from market.idx.pylibrary.offer_flags.flags import (
    DisabledFlags,
    OfferFlags,
)
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecord,
    HasGenlogBuyboxRecord,
    HasGenlogRecordRecursive,
)
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    generate_binary_price_dict
)
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml
from market.idx.offers.yatf.resources.offers_indexer.vendor_bids import VendorBids
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv, OffersProcessorModelDumperTestEnv
from market.idx.yatf.resources.abo_offer_filter_detailed import AboOfferFilterDetailed
from market.proto.common.common_pb2 import (
    EConversionType,
)

from market.pylibrary.const.offer_promo import PromoType

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt

# subscriptions do not go into genlog per MARKETINDEXER-12055
MARKET_SUBSCRIPTIONS_CATEG_ID = 15723259

WHITE_CPA_WITH_BAD_URL_WARE_MD5 = 'WBxjfubDtEU49gfweJaA1Q'

MARKET_SKU_TYPE_FAST = MappingPb.MarketSkuType.MARKET_SKU_TYPE_FAST

INSTALLMENT_OPTIONS = [
    {
        'bnpl_available': True,
        'installment_time_in_days': [yt.yson.YsonUint64(45)],
        'group_name': 'group for category'
    },
    {
        'bnpl_available': False,
        'installment_time_in_days': [yt.yson.YsonUint64(60), yt.yson.YsonUint64(90)],
        'group_name': 'group for vendor'
    }
]


def binary_promos_md5_base64(promo):
    return base64.b64encode(promo).rstrip('=')


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        default_genlog(
            offer_id='1',
            market_sku=123456789012,
            installment_options=INSTALLMENT_OPTIONS,
            flags=OfferFlags.IS_MSKU_PUBLISHED | OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='2',
            category_id=MARKET_SUBSCRIPTIONS_CATEG_ID
        ),

        # test_skip_link_to_fake_vendor
        # offer without vendor
        default_genlog(
            offer_id='10',
            model_id=1,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with some vendor
        default_genlog(
            offer_id='11',
            vendor_id=1,
            model_id=2,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with fake vendor
        default_genlog(
            offer_id='12',
            vendor_id=2,
            model_id=1,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='13',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # is_recommended_by_vendor = true
        default_genlog(
            offer_id='20',
            vendor_id=3,
            category_id=14369216,
            shop_id=100,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # is_recommended_by_vendor = false
        default_genlog(
            offer_id='21',
            vendor_id=4,
            category_id=14369216,
            shop_id=200,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # is_recommended_by_vendor = undefined = false
        default_genlog(
            offer_id='22',
            vendor_id=5,
            category_id=14369216,
            shop_id=300,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with pickup options
        default_genlog(
            offer_id='30',
            pickup_options=[
                {'Cost': 100.0,
                 'DaysMin': yt.yson.YsonUint64(1),
                 'DaysMax': yt.yson.YsonUint64(4),
                 'OrderBeforeHour': yt.yson.YsonUint64(17),
                 }],
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with vendor_code
        default_genlog(
            offer_id='31',
            vendor_code='Vendor-Code-1',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # non-red offer with vendor string
        default_genlog(
            offer_id='33',
            vendor='test_vendor_33',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with stock store count
        default_genlog(
            offer_id='34',
            stock_store_count=42,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer which is disabled by stock
        default_genlog(
            offer_id='35',
            disabled_flags=DisabledFlags.MARKET_STOCK,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with conversion and amore_data
        default_genlog(
            offer_id='36',
            offer_conversion=[
                {'conversion_value': 0.2, 'conversion_type': EConversionType.Value('SPB')},
                {'conversion_value': 0.3, 'conversion_type': EConversionType.Value('MSK')},
            ],
            amore_data="poscpodrr",
            amore_beru_supplier_data="poscpodrr-bs",
            amore_beru_vendor_data="poscpodrr-bv",
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with enabled auto discounts
        default_genlog(
            offer_id='37',
            enable_auto_discounts=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with from webmaster flag
        default_genlog(
            offer_id='38',
            from_webmaster=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with partner weight and dimensions
        default_genlog(
            offer_id='39',
            partner_weight_dimensions={'weight': 1.0, 'length': 2.0, 'width': 3.0, 'height': 4.0},
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with click_n_collect_id
        default_genlog(
            offer_id='40',
            click_n_collect_id='click_n_collect',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer which is disabled by ABO file filter
        default_genlog(
            offer_id='offer_disabled_by_abo',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with virtual_model_id
        default_genlog(
            offer_id='offer_with_virtual_model_id',
            category_id=12345678,
            virtual_model_id=100500,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with virtual_model_id
        default_genlog(
            offer_id='offer_without_virtual_model_id',
            category_id=12345679,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # published msku
        default_genlog(
            offer_id='published_msku',
            market_sku=210987654321,
            is_fake_msku_offer=True,
            flags=OfferFlags.IS_MSKU_PUBLISHED | OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # unpublished msku
        default_genlog(
            offer_id='unpublished_msku',
            market_sku=210987654322,
            is_fake_msku_offer=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # white cpa with good url
        default_genlog(
            offer_id='white_cpa_good_url_with_path',
            url='https://www.goodurl.ru/product/a039169/',
            ware_md5='XwMatVDskhEwoWXenZRifQ',
            cpa=4,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # white cpa with bad url
        default_genlog(
            offer_id='white_cpa_bad_url_without_path',
            url='https://eda.yandex.ru',
            ware_md5=WHITE_CPA_WITH_BAD_URL_WARE_MD5,
            cpa=4,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with classifier_category_confidence_for_filtering_stupids
        default_genlog(
            offer_id='32',
            classifier_category_confidence_for_filtering_stupids=1.0,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # offer with partner_cashback_promo_ids
        default_genlog(
            offer_id='offer_with_partner_cashback_promo_ids',
            partner_cashback_promo_ids='321',
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
    ),
    ]
    return offers


@pytest.fixture(scope="module")
def blue_genlog_rows():
    offers = [
        # blue 1P offer
        default_genlog(
            offer_id='20',
            ware_md5='000000000000000000020w',
            is_blue_offer=True,
            supplier_type=1,
            warehouse_id=222,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue 3P offer
        default_genlog(
            offer_id='21',
            ware_md5='000000000000000000021w',
            is_blue_offer=True,
            supplier_type=3,
            warehouse_id=111,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue MSKU=100500 first offer
        default_genlog(
            offer_id='22',
            ware_md5='000000000000000000022w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(200),
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue MSKU=100500 second offer minimal price for buybox
        default_genlog(
            offer_id='23',
            ware_md5='000000000000000000023w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(100),
            model_id=100501,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
            url='https://market.yandex.ru/product/100501?offerid=000000000000000000023w&sku=100500'
        ),
        # blue MSKU=100500 third offer
        default_genlog(
            offer_id='24',
            ware_md5='000000000000000000024w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(150),
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue MSKU=100000 single offer
        default_genlog(
            offer_id='25',
            ware_md5='000000000000000000025w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100000,
            binary_price=generate_binary_price_dict(50),
            cargo_types=[1, 2, 3],
            model_id=100501,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
            url='https://market.yandex.ru/product/100501?offerid=000000000000000000025w&sku=100000',
        ),
        # blue 3P offer which is disabled by stock
        default_genlog(
            offer_id='26',
            ware_md5='000000000000000000026w',
            is_blue_offer=True,
            disabled_flags=DisabledFlags.MARKET_STOCK,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer with minimal reference price
        default_genlog(
            offer_id='28',
            ware_md5='000000000000000000028w',
            is_blue_offer=True,
            market_sku=200000,
            binary_ref_min_price=generate_binary_price_dict(100),
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer which is disabled by ABO file filter
        default_genlog(
            offer_id='blue_offer_disabled_by_abo',
            ware_md5='000000000000000000029w',
            is_blue_offer=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer with datacamp_promos
        default_genlog(
            offer_id='offer_with_datacamp_promos',
            ware_md5='000000000000000000030w',
            is_blue_offer=True,
            datacamp_promos='123',
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer with buybox elasticity
        default_genlog(
            offer_id='41',
            ware_md5='000000000000000000031w',
            is_blue_offer=True,
            buybox_elasticity=[
                {'price_variant': yt.yson.YsonUint64(100), 'demand_mean': 2.},
                {'price_variant': yt.yson.YsonUint64(105), 'demand_mean': 1.5},
            ],
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer with multiple promos of different types that were applied in blue indexation pipeline
        default_genlog(
            offer_id='42',
            ware_md5='000000000000000000032w',
            is_blue_offer=True,
            binary_ref_min_price=generate_binary_price_dict(100),

            binary_price=generate_binary_price_dict(200),
            binary_oldprice=generate_binary_price_dict(300),
            # discount validation is performed only for offers with non-empty price_history
            binary_history_price=generate_binary_price_dict(400),
            promo_type=PromoType.DIRECT_DISCOUNT | PromoType.BLUE_CASHBACK,
            binary_promos_md5_base64=[
                binary_promos_md5_base64(b'promo_md5_direct_discount'),
                binary_promos_md5_base64(b'promo_md5_blue_cashback')
            ],
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue psku offer with vendor_name
        default_genlog(
            offer_id='psku_offer_with_vendor_name',
            ware_md5='000000000000000000033w',
            is_blue_offer=True,
            is_psku=True,
            vendor_name='VendorName',
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # published msku
        default_genlog(
            offer_id='blue_published_msku',
            market_sku=310987654321,
            is_fake_msku_offer=True,
            flags=OfferFlags.IS_MSKU_PUBLISHED | OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # unpublished msku
        default_genlog(
            offer_id='blue_unpublished_msku',
            market_sku=310987654322,
            is_fake_msku_offer=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # Offer with directDiscount promo. Promo should not overwrite oldprice in indexer.
        default_genlog(
            offer_id='promo_should_not_overwrite_oldprice',
            ware_md5='000000000000000000047w',
            is_blue_offer=True,
            binary_price=generate_binary_price_dict(50),
            binary_oldprice=generate_binary_price_dict(90),
            promo_type=PromoType.DIRECT_DISCOUNT,
            binary_promos_md5_base64=[binary_promos_md5_base64(b'promo_md5_direct_discount')],
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # Offer with secretSale promo. Promo should not overwrite oldprice in indexer.
        default_genlog(
            offer_id='secretSale_promo_should_not_overwrite_oldprice',
            ware_md5='000000000000000000048w',
            is_blue_offer=True,
            binary_price=generate_binary_price_dict(60),
            binary_oldprice=generate_binary_price_dict(100),
            promo_type=PromoType.SECRET_SALE,
            binary_promos_md5_base64=[binary_promos_md5_base64(b'promo_md5_secret_sale')],
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # blue fast sku offer
        default_genlog(
            offer_id='blue_fast_sku_offer',
            ware_md5='000000000000000000049w',
            market_sku=310987654320,
            is_blue_offer=True,
            market_sku_type=MARKET_SKU_TYPE_FAST,
            is_fast_sku=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # offer with honest discounts thresholds
        default_genlog(
            offer_id='50',
            ware_md5='000000000000000000050w',
            is_blue_offer=True,
            market_sku=22343546342,
            max_price={'price': yt.yson.YsonUint64(100000)},
            max_old_price={'price': yt.yson.YsonUint64(200000)},
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
    ),
    ]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def blue_genlog_table(yt_server, blue_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0001'), blue_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="1" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="2" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.fixture(scope="module")
def vendor_bids():
    vendor_bids = VendorBids()
    # vendor_id, vendor_ds_id, category_id, shop_id, vbid, is_recommended
    vendor_bids.add(3, 30, 14369216, 100, 1234, True)
    vendor_bids.add(4, 40, 14369216, 200, 5678, False)
    return vendor_bids


@pytest.yield_fixture(scope="module")
def abo_offer_filter_detailed():
    abo_filter = AboOfferFilterDetailed(compress=True)
    abo_filter.add(
        feed_id=101967,
        offer_id='offer_disabled_by_abo'
    )
    abo_filter.add(
        feed_id=101967,
        offer_id='blue_offer_disabled_by_abo'
    )
    abo_filter.add(
        feed_id=101967,
    )
    abo_filter.add(
        offer_id='illegal_abo_filter',
    )
    abo_filter.add(
        hiding_reason="BAD_QUALITY",
    )
    yield abo_filter


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, global_vendors, vendor_bids, abo_offer_filter_detailed):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
        'vendor_bids_csv': vendor_bids,
        'offer_filter_detailed_json': abo_offer_filter_detailed,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_model(yt_server, genlog_table, global_vendors, vendor_bids):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
        'vendor_bids_csv': vendor_bids,
    }

    with OffersProcessorModelDumperTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def blue_workflow(yt_server, blue_genlog_table, global_vendors, abo_offer_filter_detailed):
    input_table_paths = [blue_genlog_table.get_path()]
    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
        'offer_filter_detailed_json': abo_offer_filter_detailed,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


def test_generation_log_nrecords(workflow, genlog_rows):
    assert len(workflow.genlog) == len(genlog_rows) - 1
    assert_that(workflow,
                is_not(HasGenlogRecord({'offer_id': '2'})),
                u"GenerationLog doesn't contain offer with subscription category")


def test_generation_log(workflow):
    expected = {
        'url': 'https://yandex.ru',
        'category_id': 90401,
        'feed_id': 101967,
        'is_buyboxes': False
    }
    assert_that(workflow,
                HasGenlogRecord(expected),
                u'GenerationLog contains expected document')


def test_market_sku_green(workflow):
    expected = {
        'offer_id': '1',
        'market_sku': 123456789012,
        'installment_options': INSTALLMENT_OPTIONS,
    }
    assert_that(workflow,
                HasGenlogRecordRecursive(expected),
                u'GenerationLog contains market_sku field')


def test_skip_link_to_fake_vendor(workflow):
    offer_without_vendor = {'offer_id': '10'}
    assert_that(workflow,
                HasGenlogRecord(offer_without_vendor),
                u'GenerationLog contains offer without vendor')

    offer_with_some_vendor = {
        'offer_id': '11',
        'vendor_id': 1,
    }
    assert_that(workflow,
                HasGenlogRecord(offer_with_some_vendor),
                u'GenerationLog contains offer with vendor')

    offer_with_fake_vendor = {'offer_id': '12'}
    assert_that(workflow,
                HasGenlogRecord(offer_with_fake_vendor),
                u'GenerationLog contains offer with fake vendor')

    offer_with_fake_vendor_params = {
        'offer_id': '12',
        'vendor_id': 2,
    }
    assert_that(workflow,
                is_not(HasGenlogRecord(offer_with_fake_vendor_params)),
                u"GenerationLog doesn't contain fake vendor parameters")


@pytest.mark.skip(reason="Bid-related. Don't check until bids are in OP")
def test_is_recommended_by_vendor(workflow):
    recommended_offer = {
        'offer_id': '20',
        'vendor_id': 3,
        'category_id': 14369216,
        'shop_id': 100,
        'is_recommended_by_vendor': True
    }
    not_recommended_offer = {
        'offer_id': '21',
        'vendor_id': 4,
        'category_id': 14369216,
        'shop_id': 200,
        'is_recommended_by_vendor': False
    }
    not_recommended_offer_because_no_data = {
        'offer_id': '22',
        'vendor_id': 5,
        'category_id': 14369216,
        'shop_id': 300,
        'is_recommended_by_vendor': False
    }
    assert_that(workflow,
                HasGenlogRecord(recommended_offer),
                u'GenerationLog contains is_recommended_by_vendor = true field')
    assert_that(workflow,
                HasGenlogRecord(not_recommended_offer),
                u'GenerationLog contains is_recommended_by_vendor  = false field')
    assert_that(workflow,
                HasGenlogRecord(not_recommended_offer_because_no_data),
                u'GenerationLog contains is_recommended_by_vendor = false field because no such field in vendor_bids.csv')


def test_blue_generation_log_nrecords(blue_workflow, blue_genlog_rows):
    assert len(blue_workflow.genlog) == len(blue_genlog_rows)


def test_blue_offer_supplier_type(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogRecord({'offer_id': '20', 'supplier_type': 1}),
        HasGenlogRecord({'offer_id': '21', 'supplier_type': 3}),
    ))


def test_warehouse(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogRecord({'offer_id': '20', 'warehouse_id': 222}),
        HasGenlogRecord({'offer_id': '21', 'warehouse_id': 111}),
    ))


def test_cargo_types(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogRecord({'offer_id': '25', 'cargo_types': [1, 2, 3]}),
    ))


@pytest.mark.skip(reason="in OI skipped because of MARKETINCIDENTS-3245")
def test_buybox(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogBuyboxRecord({
            'offer_id': '23',
            'market_sku': 100500,
            'is_buyboxes': True
        }),
        HasGenlogBuyboxRecord({
            'offer_id': '25',
            'market_sku': 100000,
            'is_buyboxes': True
        }),
        is_not(HasGenlogBuyboxRecord({'offer_id': '22'})),
        is_not(HasGenlogBuyboxRecord({'offer_id': '24'})),
    ))


def test_blue_genlog_url(blue_workflow):
    assert_that(blue_workflow, all_of(
        HasGenlogRecord({
            'offer_id': '23',
            'market_sku': 100500,
            'ware_md5': '000000000000000000023w',
            'url': 'https://market.yandex.ru/product/100501?offerid=000000000000000000023w&sku=100500',
        }),
        HasGenlogRecord({
            'offer_id': '25',
            'market_sku': 100000,
            'ware_md5': '000000000000000000025w',
            'url': 'https://market.yandex.ru/product/100501?offerid=000000000000000000025w&sku=100000',
        }),
    ))


def test_pickup_options(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '30',
                'pickup_options': [
                    {
                        'Cost': 100,
                        'DaysMin': 1,
                        'DaysMax': 4,
                        'OrderBeforeHour': 17
                    }
                ]
            }
        )
    )


def test_vendor_code(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '31',
                'vendor_code': 'Vendor-Code-1'
            }
        )
    )


def test_classifier_category_confidence_for_filtering_stupids(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '32',
                'classifier_category_confidence_for_filtering_stupids': 1.0
            }
        )
    )


def test_vendor_string_nored(workflow):
    """Проверяем, что для не-красных оферов не пробрасывается
       сырой vendor из фида -
       для остальных маркетов он не нужен"""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '33',
                'vendor_string': '',
            }
        )
    )


def test_stock_store_count(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '34',
                'stock_store_count': 42
            }
        )
    )


def test_disabled_flags(workflow):
    """ Проверяем, что у оффера выключенного по стокам проставляется has_gone и disabled_flags, но не проставляется
        флаг OFFER_HAS_GONE
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '35',
                'has_gone': True,
                'disabled_flags': DisabledFlags.MARKET_STOCK,
                'flags':
                    OfferFlags.STORE |
                    OfferFlags.MODEL_COLOR_WHITE |
                    OfferFlags.CPC |
                    OfferFlags.PICKUP,
            }
        )
    )


def test_blue_disabled_flags(blue_workflow):
    """ Проверяем, что у синего оффера выключенного по стокам проставляется has_gone и disabled_flags, но не
        проставляется флаг OFFER_HAS_GONE
    """
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '26',
                'has_gone': True,
                'disabled_flags': DisabledFlags.MARKET_STOCK,
                'flags':
                    OfferFlags.STORE |
                    OfferFlags.AVAILABLE |
                    OfferFlags.MODEL_COLOR_WHITE |
                    OfferFlags.BLUE_OFFER |
                    OfferFlags.CPC |
                    OfferFlags.PICKUP,
            }
        )
    )


def test_offer_conversion_and_amore_data(workflow):
    offer_sorted_conversion = [
        {
            "conversion_value": 0.2,
            "conversion_type": EConversionType.Value('SPB')
        },
        {
            "conversion_value": 0.3,
            "conversion_type": EConversionType.Value('MSK')
        }
    ]
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '36',
                'offer_conversion': offer_sorted_conversion,
                'amore_data': "poscpodrr",
                'amore_beru_supplier_data': "poscpodrr-bs",
                'amore_beru_vendor_data': "poscpodrr-bv",
            }
        )
    )


def test_enable_auto_discounts(workflow):
    """ Проверяем, что у оффера проставляется флаг ENABLE_AUTO_DISCOUNTS
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '37',
                'flags':
                    OfferFlags.DEPOT |
                    OfferFlags.STORE |
                    OfferFlags.MODEL_COLOR_WHITE |
                    OfferFlags.CPC |
                    OfferFlags.ENABLE_AUTO_DISCOUNTS
            }
        )
    )


def test_from_webmaster(workflow):
    """ Проверяем, что у оффера проставляется флаг FROM_WEBMASTER
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '38',
                'flags':
                    OfferFlags.DEPOT |
                    OfferFlags.STORE |
                    OfferFlags.MODEL_COLOR_WHITE |
                    OfferFlags.CPC |
                    OfferFlags.FROM_WEBMASTER
            }
        )
    )


def test_weight_dimensions(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '39',
                'partner_weight_dimensions': {
                    "weight": 1.0,
                    "length": 2.0,
                    "width": 3.0,
                    "height": 4.0
                }
            }
        )
    )


def test_blue_ref_min_price(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '28',
                'binary_ref_min_price': {'price': long(100*(10**7))},
            }
        )
    )


def test_honest_discount_prices(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '50',
                'max_price': {'price': long(100000)},
                'max_old_price': {'price': long(200000)},
            }
        )
    )


def test_unique_model_count(workflow_model):
    assert(workflow_model.model_dumper_out == 'num_models: 2\n')


def test_abo_offer_filter_detailed(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'offer_disabled_by_abo',
            'disabled_flags': DisabledFlags.MARKET_ABO,
        })
    )


def test_offer_with_virtual_model_id(workflow):
    '''
    Проверяем, что виртуальная карточка модели
    прокидывается, если она пришла в офферсдата
    '''
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive({
                'offer_id': 'offer_with_virtual_model_id',
                'virtual_model_id': 100500,
            }),
            HasGenlogRecordRecursive({
                'offer_id': 'offer_without_virtual_model_id',
                'virtual_model_id': None,
            }),
        )
    )


def test_abo_blue_offer_filter_detailed(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'blue_offer_disabled_by_abo',
            'disabled_flags': DisabledFlags.MARKET_ABO,
        })
    )


def test_offer_with_datacamp_promos(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'offer_with_datacamp_promos',
            'datacamp_promos': '123',
        })
    )


def test_offer_buybox_elasticity_data(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '41',
                'buybox_elasticity': [
                    {
                        "price_variant": long(100),
                        "demand_mean": 2.
                    },
                    {
                        "price_variant": long(105),
                        "demand_mean": 1.5
                    }
                ]
            }
        )
    )


def test_blue_indexation_promo_overlap(blue_workflow):
    '''
    Проверяем, что в таблицы генлогов попадает запись для оффера, который участвует
    одновременно в нескольких промоакциях, применённых на этапе индексации в операции BluePromoReduce
    '''
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive(
            {
                'offer_id': '42',
                'binary_price': {'price': 200 * (10 ** 7)},
                'promo_type': PromoType.DIRECT_DISCOUNT | PromoType.BLUE_CASHBACK,
            }
        )
    )


def test_psku_offer_with_vendor_name(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'psku_offer_with_vendor_name',
            'is_psku': True,
            'vendor_name': 'VendorName',
        })
    )


def test_fast_sku_offer(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'blue_fast_sku_offer',
            'market_sku': 310987654320,
            'is_fast_sku': True,
        })
    )


def test_unpublished_msku(workflow):
    expected_published = {
        'offer_id': 'published_msku',
        'market_sku': 210987654321,
        'is_fake_msku_offer': True,
        'flags':
            OfferFlags.PICKUP |
            OfferFlags.STORE |
            OfferFlags.MODEL_COLOR_WHITE |
            OfferFlags.CPC |
            OfferFlags.MARKET_SKU |
            OfferFlags.IS_MSKU_PUBLISHED
    }
    assert_that(workflow,
                HasGenlogRecord(expected_published),
                u'GenerationLog contains market_sku value referring to published msku')
    expected_unpublished = {
        'offer_id': 'unpublished_msku',
        'market_sku': 210987654322,
        'is_fake_msku_offer': True,
        'flags':
            OfferFlags.PICKUP |
            OfferFlags.STORE |
            OfferFlags.MODEL_COLOR_WHITE |
            OfferFlags.CPC |
            OfferFlags.MARKET_SKU
    }
    assert_that(workflow,
                HasGenlogRecord(expected_unpublished),
                u'GenerationLog contains market_sku value referring to unpublished msku')


def test_unpublished_msku_blue(blue_workflow):
    expected_blue_published = {
        'offer_id': 'blue_published_msku',
        'market_sku': 310987654321,
        'is_fake_msku_offer': True,
        'flags':
            OfferFlags.PICKUP |
            OfferFlags.STORE |
            OfferFlags.MODEL_COLOR_WHITE |
            OfferFlags.CPC |
            OfferFlags.MARKET_SKU |
            OfferFlags.IS_MSKU_PUBLISHED
    }
    assert_that(blue_workflow,
                HasGenlogRecord(expected_blue_published),
                u'GenerationLog contains market_sku value referring to published msku')

    expected_blue_unpublished = {
        'offer_id': 'blue_unpublished_msku',
        'market_sku': 310987654322,
        'is_fake_msku_offer': True,
        'flags':
            OfferFlags.PICKUP |
            OfferFlags.STORE |
            OfferFlags.MODEL_COLOR_WHITE |
            OfferFlags.CPC |
            OfferFlags.MARKET_SKU
    }
    assert_that(blue_workflow,
                HasGenlogRecord(expected_blue_unpublished),
                u'GenerationLog contains market_sku value referring to published msku')


def test_white_cpa_url(workflow):
    expected_good_url = {
        'offer_id': 'white_cpa_good_url_with_path',
        'url': 'https://www.goodurl.ru/product/a039169/'
    }
    expected_bad_url = {
        'offer_id': 'white_cpa_bad_url_without_path',
        'url': 'https://market.yandex.ru/offer/' + WHITE_CPA_WITH_BAD_URL_WARE_MD5
    }
    assert_that(workflow,
                all_of(
                    HasGenlogRecord(expected_good_url),
                    HasGenlogRecord(expected_bad_url),
                ),
                u'GenerationLog contains white cpa offers with good url and market url')


def test_white_partner_cashback_promo_ids(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'offer_with_partner_cashback_promo_ids',
            'partner_cashback_promo_ids': '321',
        })
    )
