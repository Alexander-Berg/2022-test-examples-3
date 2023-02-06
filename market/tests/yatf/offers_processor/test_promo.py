#!/usr/bin/env python
# coding: utf-8

import pytest

from hamcrest import (
    assert_that,
    is_not,
    all_of,
)

from market.idx.pylibrary.offer_flags.flags import (
    OfferFlags,
)
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecordRecursive,
)
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    generate_binary_price_dict,
    binary_promos_md5_base64
)
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.pylibrary.const.offer_promo import PromoType

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        # offer with enabled auto discounts
        default_genlog(
            offer_id='37',
            enable_auto_discounts=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # White offer with multiple promos of different sources and types.
        # Price history for this offer is set to invalid in order to test the return
        # value of HasSourceOfPromo() function call. History price for genlog record
        # will depend on this return value. Should be tested in main-idx!
        default_genlog(
            offer_id='promo_invalid_history_price',
            binary_price=generate_binary_price_dict(50),
            binary_oldprice=generate_binary_price_dict(100),
            binary_unverified_oldprice=generate_binary_price_dict(100),
            binary_history_price=generate_binary_price_dict(200),
            history_price_is_valid=False,
            promo_type=PromoType.N_PLUS_M | PromoType.BLUE_CASHBACK,
            binary_promos_md5_base64=[
                binary_promos_md5_base64(b'promo_md5_n_plus_m'),
                binary_promos_md5_base64(b'promo_md5_blue_cashback')
            ],
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        # White offer with FLASH_DISCOUNT promo overlapped with BLUE_CASHBACK promo.
        # Price history for this offer is set to invalid in order to test the return
        # value of HasSourceOfPromo() function call. History price for genlog record
        # will depend on this return value. Should be tested in main-idx!
        # FLASH_DISCOUNT пересекается с BLUE_CASHBACK. Она не магазинная, поэтому history_price не выставляется
        # см https://a.yandex-team.ru/arc/trunk/arcadia/market/library/libpromo/common.h?rev=r9226787#L191 и
        # https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/offers/lib/loaders/load_biz_logic.cpp?rev=r9232935#L408
        default_genlog(
            offer_id='promo_no_history_price',
            binary_price=generate_binary_price_dict(50),
            binary_oldprice=generate_binary_price_dict(100),
            binary_unverified_oldprice=generate_binary_price_dict(100),
            history_price_is_valid=False,
            promo_type=PromoType.FLASH_DISCOUNT | PromoType.BLUE_CASHBACK,
            binary_promos_md5_base64=[
                binary_promos_md5_base64(b'promo_md5_flash_discount'),
                binary_promos_md5_base64(b'promo_md5_blue_cashback')
            ],
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
        # offer with datacamp_promos
        default_genlog(
            offer_id='offer_with_datacamp_promos',
            ware_md5='000000000000000000030w',
            is_blue_offer=True,
            datacamp_promos='123',
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
            binary_unverified_oldprice=generate_binary_price_dict(300),
            # discount validation is performed only for offers with non-empty price_history
            binary_history_price=generate_binary_price_dict(400),
            promo_type=PromoType.DIRECT_DISCOUNT | PromoType.BLUE_CASHBACK,
            binary_promos_md5_base64=[
                binary_promos_md5_base64(b'promo_md5_direct_discount'),
                binary_promos_md5_base64(b'promo_md5_blue_cashback')
            ],
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
        ),
        # Offer with directDiscount promo. Promo should not overwrite oldprice in indexer.
        default_genlog(
            offer_id='promo_should_not_overwrite_oldprice',
            ware_md5='000000000000000000047w',
            is_blue_offer=True,
            binary_price=generate_binary_price_dict(50),
            binary_oldprice=generate_binary_price_dict(90),
            binary_unverified_oldprice=generate_binary_price_dict(90),
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
            binary_unverified_oldprice=generate_binary_price_dict(100),
            promo_type=PromoType.SECRET_SALE,
            binary_promos_md5_base64=[binary_promos_md5_base64(b'promo_md5_secret_sale')],
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


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def blue_workflow(yt_server, blue_genlog_table):
    input_table_paths = [blue_genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


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


def test_offer_with_datacamp_promos(blue_workflow):
    assert_that(
        blue_workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'offer_with_datacamp_promos',
            'datacamp_promos': '123',
        })
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


def test_history_price_for_white_offer_with_promo_overlap(workflow):
    '''
    Проверяем, что в таблицы генлогов попадает запись исторических цен для белого оффера, который участвует
    одновременно в нескольких промоакциях. Причём, источник промоакций (в терминологии enum'a EPromoSource)
    у этих акций разный: одна из них магазинная (n_plus_m), другая - нет (blue_cashback).
    Отдельно проверяем случай пересечения акции flash-discount: эта акция является магазинной, но при выставлении
    исторических цен мы её не учитываем.
    '''
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'offer_id': 'promo_invalid_history_price',
                    'binary_price': {'price': long(50 * (10 ** 7))},
                    'promo_type': PromoType.N_PLUS_M | PromoType.BLUE_CASHBACK,
                    'history_price_is_valid': False,
                    'binary_history_price': {'price': long(200 * (10 ** 7))},
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'offer_id': 'promo_no_history_price',
                    'binary_price': {'price': long(50 * (10 ** 7))},
                    'promo_type': PromoType.FLASH_DISCOUNT | PromoType.BLUE_CASHBACK,
                    'history_price_is_valid': False,
                    'binary_history_price': {'price': long(200 * (10 ** 7))},
                }
            ))
        )
    )


def test_promo_does_not_overwrite_oldprice(blue_workflow):
    '''
    Проверяем, что в таблицы генлогов записывается oldprice для синего оффера, который участвует в акции directDiscount
    '''
    assert_that(
        blue_workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'offer_id': 'promo_should_not_overwrite_oldprice',
                    'binary_price': {'price': long(50 * (10 ** 7))},
                    'binary_oldprice': {'price': long(90 * (10 ** 7))},
                    'promo_type': PromoType.DIRECT_DISCOUNT,
                }
            ),
            HasGenlogRecordRecursive(
                {
                    'offer_id': 'secretSale_promo_should_not_overwrite_oldprice',
                    'binary_price': {'price': long(60 * (10 ** 7))},
                    'binary_oldprice': {'price': long(100 * (10 ** 7))},
                    'promo_type': PromoType.SECRET_SALE,
                }
            ),
        )
    )


def test_white_partner_cashback_promo_ids(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'offer_with_partner_cashback_promo_ids',
            'partner_cashback_promo_ids': '321',
        })
    )
