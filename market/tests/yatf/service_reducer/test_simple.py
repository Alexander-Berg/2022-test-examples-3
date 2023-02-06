# coding: utf-8

import pytest

from hamcrest import assert_that, has_item, is_not, has_length, empty, has_entries

from market.idx.generation.yatf.test_envs.service_reducer import ServiceReducerTestEnv
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow


# см market/idx/pylibrary/offer_flags/flags.py
IS_COLLAPSE = 1 << 51  # Оффер, который учавствует в схлопывании для мультискладовости


# должны быть отсортированы по шарду и sequence_number (здесь называется id)
@pytest.fixture(scope="module")
def offers(request):
    return [
        GenlogRow(
            id=0,
            offer_yabs_id=123,
            feed_id=1,
            offer_id=1,
            warehouse_id=15,
            shard_id=0,
            business_id=5,
            flags=IS_COLLAPSE,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=True,
            is_blue_offer=True,
            supplier_id=None,
            classifier_magic_id="0fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=["hello"],
            promo_price=21352,
            benefit_price=252
        ),
        GenlogRow(
            id=1,
            offer_yabs_id=130,
            feed_id=2,
            offer_id=1,
            warehouse_id=7,
            shard_id=0,
            business_id=5,
            flags=IS_COLLAPSE,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=True,
            is_blue_offer=True,
            supplier_id=None,
            classifier_magic_id="1fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=["hello", "world"],
            promo_price=3246,
            benefit_price=235215
        ),
        GenlogRow(
            id=2,
            offer_yabs_id=1225,
            feed_id=2,
            offer_id=1,
            warehouse_id=15,
            shard_id=0,
            business_id=2,
            flags=IS_COLLAPSE,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=False,
            is_blue_offer=True,
            supplier_id=None,
            classifier_magic_id="2fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=[],
            promo_price=100,
            benefit_price=100
        ),
        GenlogRow(
            id=3,
            offer_yabs_id=2515,
            feed_id=3,
            offer_id=6,
            warehouse_id=3,
            shard_id=0,
            business_id=5,
            flags=IS_COLLAPSE,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=False,
            is_blue_offer=True,
            supplier_id=None,
            classifier_magic_id="3fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=["abc", "def"],
            promo_price=125235,
            benefit_price=2
        ),
        GenlogRow(
            id=4,
            offer_yabs_id=235,
            feed_id=4,
            offer_id=9,
            warehouse_id=2,
            shard_id=0,
            business_id=5,
            flags=0,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=False,
            is_blue_offer=True,
            supplier_id=None,
            classifier_magic_id="4fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=[],
            promo_price=100,
            benefit_price=100
        ),
        GenlogRow(
            id=5,
            offer_yabs_id=235,
            feed_id=4,
            offer_id=10,
            warehouse_id=2,
            shard_id=0,
            business_id=5,
            flags=0,
            is_express=True,
            vat=7,
            in_stock_count=15,
            has_gone=False,
            is_blue_offer=True,
            supplier_id=None,
            is_fake_msku_offer=True,
            classifier_magic_id="5fbe47313ffe5944a78acb6b170cf6a",
            binary_promos_md5_base64=[],
            promo_price=100,
            benefit_price=100
        ),
    ]


# должны быть отсортированы по шарду и sequence_number (здесь называется id)
@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers):
    with ServiceReducerTestEnv(yt_server, offers, False) as env:
        env.execute()
        env.verify()
        yield env


def offer_relation(master_offer, service_offer):
    result = {
        'sequence_number': master_offer['sequence_number'],
        'business_id': master_offer['business_id'],

        'feed_id': master_offer['feed_id'],
        'ware_md5': master_offer['ware_md5'],
        'offer_id': master_offer['offer_id'],
        'shop_id': master_offer['shop_id'],
        'supplier_id': master_offer['supplier_id'],

        'service_sequence_number': service_offer['sequence_number'],
        'service_offer_yabs_id': service_offer['offer_yabs_id'],
        'service_warehouse_id': service_offer['warehouse_id'],

        'service_feed_id': service_offer['feed_id'],
        'service_ware_md5': service_offer['ware_md5'],
        'service_offer_id': service_offer['offer_id'],
        'service_shop_id': service_offer['shop_id'],

        'service_price': service_offer['binary_price']['price'],
        'service_oldprice': service_offer['binary_oldprice']['price'],
        'service_currency_id': 3,  # RUB
        'service_disabled_flags': service_offer['disabled_flags'],
        'service_is_express': service_offer['is_express'],

        'service_vat': service_offer['vat'],
        'service_in_stock_count': service_offer['in_stock_count'],
        'service_has_gone': service_offer['has_gone'],
        'service_is_blue_offer': service_offer['is_blue_offer'],
        'service_flags': service_offer['flags'],

        'service_classifier_magic_id': service_offer['classifier_magic_id'],

        'service_promo_keys': service_offer['binary_promos_md5_base64'],
        'service_promo_price': service_offer['promo_price'],
        'service_benefit_price': service_offer['benefit_price']
    }

    return result


def test_no_collapse_flag(workflow, offers):
    assert_that(workflow.output_rows, is_not(has_item(
        offer_relation(offers[4], offers[4])
    )))


def test_service_itself(workflow, offers):
    assert_that(workflow.output_rows, has_length(4))

    # business offer is service offer for itself
    assert_that(workflow.output_rows, has_item(
        offer_relation(offers[2], offers[2])
    ))

    assert_that(workflow.output_rows, has_item(
        offer_relation(offers[3], offers[3])
    ))

    assert_that(workflow.output_rows, has_item(
        offer_relation(offers[0], offers[0])
    ))


def test_collapsing(workflow, offers):
    assert_that(workflow.output_rows, has_item(
        offer_relation(offers[0], offers[1])
    ))


def test_tables_non_empty(workflow):
    assert_that(workflow.collapse_genlog_rows, is_not(empty()))
    assert_that(workflow.filtered_genlog_rows, is_not(empty()))
    assert_that(workflow.relation_output_rows, is_not(empty()))
    assert_that(workflow.genlog_output_rows, is_not(empty()))


def test_msku_filtering(workflow):
    assert_that(workflow.filtered_genlog_rows, is_not(has_item(has_entries({'is_fake_msku_offer': True}))))
