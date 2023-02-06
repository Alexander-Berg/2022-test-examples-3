# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import (
    default_price,
    default_genlog,
    default_blue_genlog,
    generate_binary_price_dict,
    get_binary_ware_md5
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from market.idx.generation.yatf.matchers.genlog_dumper.env_matchers import HasBaseOfferPropsFbRecursive
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id='1',
            ware_md5='1irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('1irstOffer0V7gLLUBANyg'),
            binary_price=generate_binary_price_dict(1200),
            stock_store_count=yt.yson.YsonUint64(1),
            from_webmaster=True,
            flags=OfferFlags.IS_CUTPRICE,
            purchase_price=161.0,
        ),
        default_genlog(
            offer_id='2',
            ware_md5='2irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('2irstOffer0V7gLLUBANyg'),
            binary_price=generate_binary_price_dict(1400),
            stock_store_count=yt.yson.YsonUint64(10),
            flags=(
                OfferFlags.IS_CUTPRICE |
                OfferFlags.LIKE_NEW |
                OfferFlags.DISCOUNT_RESTRICTED |
                OfferFlags.IS_PREORDER
            ),
            purchase_price=-161.0,
        ),
        default_genlog(
            offer_id='3',
            ware_md5='3irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('3irstOffer0V7gLLUBANyg'),
            binary_price=generate_binary_price_dict(1600),
            binary_history_price=generate_binary_price_dict(800),
            history_price_is_valid=True,
            enable_auto_discounts=True,
            from_webmaster=False,
            flags=OfferFlags.PREVIOUSLY_USED | OfferFlags.IS_CUTPRICE | OfferFlags.LIKE_NEW,
            purchase_price=161.0,
        ),
        default_blue_genlog(
            offer_id='4',
            ware_md5='4irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('4irstOffer0V7gLLUBANyg'),
            binary_price=generate_binary_price_dict(1800),
            binary_oldprice=generate_binary_price_dict(2800),
            binary_unverified_oldprice=generate_binary_price_dict(2800),
            binary_history_price=generate_binary_price_dict(900),
            history_price_is_valid=False,
            binary_reference_old_price=generate_binary_price_dict(800),
            cargo_types=[1, 2, 3],
            binary_price_limit=generate_binary_price_dict(1000),
            binary_ref_min_price=generate_binary_price_dict(900),
            flags=OfferFlags.IS_FULFILLMENT | OfferFlags.BLUE_OFFER | OfferFlags.AVAILABLE,
            disabled_flags=DisabledFlags.MARKET_STOCK,
            purchase_price=161.0,
        ),
        default_genlog(
            offer_id='5',
            ware_md5='5irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('5irstOffer0V7gLLUBANyg'),
            cargo_types=[],
        ),
        default_genlog(
            offer_id='7',
            ware_md5='6irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('6irstOffer0V7gLLUBANyg'),
            is_blue_offer=True,
            flags=OfferFlags.IS_GOLDEN_MATRIX
        ),
        default_genlog(
            offer_id='8',
            ware_md5='8irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('8irstOffer0V7gLLUBANyg'),
            is_blue_offer=True,
            flags=OfferFlags.IS_MIN_REF_PROMO_SUB_3P
        ),
        # published msku
        default_genlog(
            offer_id='published_msku',
            ware_md5='9irstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('9irstOffer0V7gLLUBANyg'),
            market_sku=210987654321,
            is_fake_msku_offer=True,
            flags=OfferFlags.IS_MSKU_PUBLISHED
        ),
        # unpublished msku
        default_genlog(
            offer_id='unpublished_msku',
            ware_md5='9zrstOffer0V7gLLUBANyg',
            binary_ware_md5=get_binary_ware_md5('9zrstOffer0V7gLLUBANyg'),
            market_sku=210987654322,
            is_fake_msku_offer=True,
        ),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        env.verify()
        yield env


def offer_to_gl_record(offer_dict):
    fields = [
        "cargo_types",
        "credit_templates",
        "binary_price",
        "is_blue_offer",
        "binary_raw_oldprice",
        "binary_oldprice",
        "binary_history_price",
        "binary_reference_old_price",
        "history_price_is_valid",
        "stock_store_count",
        "forbidden_market_mask",
        "binary_price_limit",
        "flags",
        "binary_ref_min_price",
        "purchase_price",
        "disabled_flags",
        "binary_ware_md5",
    ]

    record_dict = {}
    for field in fields:
        record_dict[field] = offer_dict.get(field)
    record = make_gl_record(**record_dict)

    return record


@pytest.yield_fixture(scope="module")
def records(offers_processor_workflow):
    records = []
    for offer in offers_processor_workflow.genlog_dicts:
        records.append(offer_to_gl_record(offer))
    return records


@pytest.yield_fixture(scope="module")
def workflow(offers_processor_workflow, records):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'BASE_OFFER_PROPS',
            '--dumper', 'WARE_MD5'
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(records)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def ordered_offers(workflow, genlog_rows):
    offset_by_md5 = {}

    for i, _ in enumerate(genlog_rows):
        offset_by_md5[workflow.ware_md5.get_ware_md5(i)] = i

    return sorted(genlog_rows, key=lambda offer : offset_by_md5[offer['ware_md5']])


def test_price_base_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение price и currencyid правильно попадает в base-offer-props.fb
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        binary_price = offer.get('binary_price')
        if binary_price:
            expected_offer = {
                'Price': binary_price.get('price'),
                'CurrencyId': 3,  # RUR
            }
        else:
            expected_offer = {
                'Price': default_price(),
                'CurrencyId': 3,  # RUR
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected price options'
    )


def test_oldprice_base_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение oldprice правильно попадает в base-offer-props.fb
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        binary_oldprice = offer.get('binary_oldprice')
        if binary_oldprice:
            expected_offer = {
                'OldPrice': {
                    'Value': binary_oldprice.get('price'),
                },
            }
        else:
            expected_offer = {
                'OldPrice': None,
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected oldprice options'
    )


def test_history_price_base_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение исторической цены, ее валюта и валидность правильно попадает в base-offer-props.fb
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        history_price = offer.get('binary_history_price')
        history_price_is_valid = offer.get('history_price_is_valid', False)
        is_blue_offer = offer.get('is_blue_offer', False)
        if history_price and (is_blue_offer or history_price_is_valid):
            expected_offer = {
                'HistoryPrice': {
                    'Value': history_price.get('price')
                },
                'HistoryCurrencyId': 3,  # RUR
                'HistoryPriceIsValid': history_price_is_valid,
            }
        else:
            expected_offer = {
                'HistoryPrice': None,
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected history price options'
    )


def test_stock_store_count_base_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем качественый состав base-offer-props.fb на наличие количества остатков
    """

    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        expected_offer = dict()
        stock_store_count = offer.get('stock_store_count')
        if stock_store_count:
            expected_offer['StockStoreCount'] = {'Value': stock_store_count}

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected stock store count'
    )


def test_cargo_types(workflow, ordered_offers):
    """Проверяется, что cargo_types записываются в base-offer-props.fb"""
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        expected_offer = {}
        if 'cargo_types' in offer and offer['cargo_types']:
            expected_offer['CargoTypes'] = offer['cargo_types']
        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected cargo_types flag'
    )


def test_price_limit_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение price_limit правильно попадает в base-offer-props.fb
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        price_limit = offer.get('binary_price_limit')
        if price_limit:
            expected_offer = {
                'PriceLimit': {
                    'Value': price_limit.get('price')
                },
            }
        else:
            expected_offer = {
                'PriceLimit': None,
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected price limit'
    )


def test_ref_min_price_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение ref_min_price правильно попадает в base-offer-props.fb
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        ref_min_price = offer.get('binary_ref_min_price')
        if ref_min_price:
            expected_offer = {
                'RefMinPrice': {
                    'Value': ref_min_price.get('price')
                },
            }
        else:
            expected_offer = {
                'RefMinPrice': None,
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected reference minimum price'
    )


def test_purchase_price_offer_props_fb(workflow, ordered_offers):
    """
    Проверяем, что значение purchase_price корректно
    """
    expected = dict()
    for idx, offer in enumerate(ordered_offers):
        purchase_price = offer.get('purchase_price')
        if purchase_price and purchase_price >= 0:
            expected_offer = {
                'PurchasePrice': purchase_price * 10**7,
            }
        else:
            expected_offer = {
                'PurchasePrice': None
            }

        expected[idx] = expected_offer

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb contains expected purchase_price options'
    )


def test_is_msku_published_offer_props_fb(workflow, ordered_offers):
    """ Проверяем, что OfferData.is_msku_published передаётся в IsMskuPublished
    """
    expected = dict()
    flags = OfferFlags.MODEL_COLOR_WHITE | \
            OfferFlags.CPC | \
            OfferFlags.MARKET_SKU
    for idx, offer in enumerate(ordered_offers):
        offer_id = offer.get('offer_id')
        if offer_id == 'published_msku':
            expected[idx] = {'Flags64': flags | OfferFlags.IS_MSKU_PUBLISHED}
        elif offer_id == 'unpublished_msku':
            expected[idx] = {'Flags64': flags}

    assert_that(
        workflow,
        HasBaseOfferPropsFbRecursive(expected),
        'base-offer-props.fb has IsMskuPublished flag set from OfferData.is_msku_published'
    )
