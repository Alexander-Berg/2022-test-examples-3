# coding: utf-8

import pytz
import pytest

from datetime import datetime, timedelta
import time
import yatest.common

from market.idx.yatf.utils.mmap.promo_indexer_write_mmap import write_promo_json_to_mmap
from market.idx.offers.yatf.resources.offers_indexer.promo_details import PromoDetails
from market.idx.offers.yatf.utils.fixtures import generate_binary_price_dict, generate_default_blue_3p_promo, \
    generate_default_msku, default_blue_genlog, default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from hamcrest import assert_that, is_not, all_of
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt

history_price_date = 20180919


@pytest.fixture(scope="module")
def genlog_rows():
    # offer with msku for good promo, good price (match promo, actual literal, and in statistics)
    blue_offer_1 = default_blue_genlog()
    blue_offer_1['offer_id'] = '1'
    blue_offer_1['market_sku'] = 112201
    blue_offer_1['binary_price'] = generate_binary_price_dict(150)

    # offer with msku for expired promo (match promo, no literal, not in statistics)
    blue_offer_2 = default_blue_genlog()
    blue_offer_2['offer_id'] = '2'
    blue_offer_2['market_sku'] = 112202
    blue_offer_2['binary_price'] = generate_binary_price_dict(240)

    # offer with msku for good promo, but higher price
    blue_offer_3 = default_blue_genlog()
    blue_offer_3['offer_id'] = '3'
    blue_offer_3['market_sku'] = 112203
    blue_offer_3['binary_price'] = generate_binary_price_dict(360)

    # offer with no promo for msku
    blue_offer_4 = default_blue_genlog()
    blue_offer_4['offer_id'] = '4'
    blue_offer_4['market_sku'] = 112204
    blue_offer_4['binary_price'] = generate_binary_price_dict(450)

    # offer with promo with close end date (match promo, actual literal, but not in statistics)
    blue_offer_5 = default_blue_genlog()
    blue_offer_5['offer_id'] = '5'
    blue_offer_5['market_sku'] = 112205
    blue_offer_5['binary_price'] = generate_binary_price_dict(450)

    # offer with old price from shop, msku, good promo for msku, price > Market's price
    blue_offer_6 = default_blue_genlog()
    blue_offer_6['offer_id'] = '6'
    blue_offer_6['market_sku'] = 112206
    blue_offer_6['binary_price'] = generate_binary_price_dict(600)
    blue_offer_6['binary_oldprice'] = generate_binary_price_dict(6000)
    blue_offer_6['binary_unverified_oldprice'] = generate_binary_price_dict(6000)

    # offer with old price from shop, msku, good promo for msku, price < Market's price (match promo, actual literal, in statistics)
    blue_offer_7 = default_blue_genlog()
    blue_offer_7['offer_id'] = '7'
    blue_offer_7['market_sku'] = 112207
    blue_offer_7['binary_price'] = generate_binary_price_dict(600)
    blue_offer_7['binary_oldprice'] = generate_binary_price_dict(7000)
    blue_offer_7['binary_unverified_oldprice'] = generate_binary_price_dict(7000)

    # offer with valid old price, old price <= history && old price <= dco
    blue_offer_8 = default_blue_genlog()
    blue_offer_8['offer_id'] = '8'
    blue_offer_8['market_sku'] = 112208
    blue_offer_8['binary_price'] = generate_binary_price_dict(600)
    blue_offer_8['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_8['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_8['binary_history_price'] = generate_binary_price_dict(800)
    blue_offer_8['history_price_is_valid'] = True  # True is default value for history price
    blue_offer_8['binary_reference_old_price'] = generate_binary_price_dict(800)

    # offer with valid old price, old price <= history && old price > dco
    blue_offer_9 = default_blue_genlog()
    blue_offer_9['offer_id'] = '9'
    blue_offer_9['market_sku'] = 112209
    blue_offer_9['binary_price'] = generate_binary_price_dict(600)
    blue_offer_9['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_9['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_9['binary_history_price'] = generate_binary_price_dict(800)
    blue_offer_9['history_price_is_valid'] = True
    blue_offer_9['binary_reference_old_price'] = generate_binary_price_dict(600)

    # offer with valid old price, old price > history && old price <= dco
    blue_offer_10 = default_blue_genlog()
    blue_offer_10['offer_id'] = '10'
    blue_offer_10['market_sku'] = 112210
    blue_offer_10['binary_price'] = generate_binary_price_dict(600)
    blue_offer_10['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_10['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_10['binary_history_price'] = generate_binary_price_dict(600)
    blue_offer_10['history_price_is_valid'] = True
    blue_offer_10['binary_reference_old_price'] = generate_binary_price_dict(800)

    # offer with invalid old price, old price > history && old price > dco
    blue_offer_11 = default_blue_genlog()
    blue_offer_11['offer_id'] = '11'
    blue_offer_11['market_sku'] = 112211
    blue_offer_11['binary_price'] = generate_binary_price_dict(600)
    blue_offer_11['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_11['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_11['binary_history_price'] = generate_binary_price_dict(600)
    blue_offer_11['history_price_is_valid'] = True
    blue_offer_11['binary_reference_old_price'] = generate_binary_price_dict(600)

    # offer with invalid old price, empty history && empty dco
    blue_offer_12 = default_blue_genlog()
    blue_offer_12['offer_id'] = '12'
    blue_offer_12['market_sku'] = 112212
    blue_offer_12['binary_price'] = generate_binary_price_dict(600)
    blue_offer_12['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_12['binary_unverified_oldprice'] = generate_binary_price_dict(700)

    # offer with valid old price, old price <= history empty && empty dco
    blue_offer_13 = default_blue_genlog()
    blue_offer_13['offer_id'] = '13'
    blue_offer_13['market_sku'] = 112213
    blue_offer_13['binary_price'] = generate_binary_price_dict(600)
    blue_offer_13['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_13['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_13['binary_history_price'] = generate_binary_price_dict(800)
    blue_offer_13['history_price_is_valid'] = True

    # offer with valid old price, history empty && old price <= dco
    blue_offer_14 = default_blue_genlog()
    blue_offer_14['offer_id'] = '14'
    blue_offer_14['market_sku'] = 112214
    blue_offer_14['binary_price'] = generate_binary_price_dict(600)
    blue_offer_14['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_14['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_14['binary_reference_old_price'] = generate_binary_price_dict(800)

    # offer with price <= price_limit
    blue_offer_15 = default_blue_genlog()
    blue_offer_15['offer_id'] = '15'
    blue_offer_15['market_sku'] = 112215
    blue_offer_15['binary_price'] = generate_binary_price_dict(600)
    blue_offer_15['binary_price_limit'] = generate_binary_price_dict(600)

    # offer with price > price_limit
    blue_offer_16 = default_blue_genlog()
    blue_offer_16['offer_id'] = '16'
    blue_offer_16['market_sku'] = 112216
    blue_offer_16['binary_price'] = generate_binary_price_dict(600)
    blue_offer_16['binary_price_limit'] = generate_binary_price_dict(500)

    # no oldprice, but there is history price and dco price, history_price = max(history_price, dco_price)
    blue_offer_17 = default_blue_genlog()
    blue_offer_17['offer_id'] = '17'
    blue_offer_17['market_sku'] = 112217
    blue_offer_17['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_17['binary_history_price'] = generate_binary_price_dict(1500)
    blue_offer_17['history_price_is_valid'] = True
    blue_offer_17['binary_reference_old_price'] = generate_binary_price_dict(1200)

    # no oldprice, but there is history price and dco price, dco_price = max(history_price, dco_price)
    blue_offer_18 = default_blue_genlog()
    blue_offer_18['offer_id'] = '18'
    blue_offer_18['market_sku'] = 112218
    blue_offer_18['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_18['binary_history_price'] = generate_binary_price_dict(1200)
    blue_offer_18['history_price_is_valid'] = True
    blue_offer_18['binary_reference_old_price'] = generate_binary_price_dict(1500)

    # no oldprice, but there is dco price and no history_price
    blue_offer_19 = default_blue_genlog()
    blue_offer_19['offer_id'] = '19'
    blue_offer_19['market_sku'] = 112219
    blue_offer_19['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_19['binary_reference_old_price'] = generate_binary_price_dict(1500)

    # no oldprice, but there is history_price and no dco price
    blue_offer_20 = default_blue_genlog()
    blue_offer_20['offer_id'] = '20'
    blue_offer_20['market_sku'] = 112220
    blue_offer_20['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_20['binary_history_price'] = generate_binary_price_dict(1500)
    blue_offer_20['history_price_is_valid'] = True

    blue_offer_21 = default_blue_genlog()
    blue_offer_21['offer_id'] = '21'
    blue_offer_21['market_sku'] = 112221
    blue_offer_21['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_21['binary_history_price'] = generate_binary_price_dict(1001)
    blue_offer_21['history_price_is_valid'] = True
    blue_offer_21['binary_reference_old_price'] = generate_binary_price_dict(1001)

    # offer with valid old price, old price <= history price && empty dco, but restricted discounts
    blue_offer_22 = default_blue_genlog()
    blue_offer_22['offer_id'] = '22'
    blue_offer_22['market_sku'] = 112222
    blue_offer_22['binary_price'] = generate_binary_price_dict(600)
    blue_offer_22['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_22['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_22['binary_history_price'] = generate_binary_price_dict(800)
    blue_offer_22['history_price_is_valid'] = True

    blue_offer_22['flags'] = OfferFlags.DISCOUNT_RESTRICTED.value

    # offer with valid old price, old price <= history price && empty dco, but history price is not valid by pricedrops
    blue_offer_23 = default_blue_genlog()
    blue_offer_23['offer_id'] = '23'
    blue_offer_23['market_sku'] = 112223
    blue_offer_23['binary_price'] = generate_binary_price_dict(600)
    blue_offer_23['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_23['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_23['binary_history_price'] = generate_binary_price_dict(800)
    blue_offer_23['history_price_is_valid'] = False

    return [blue_offer_1, blue_offer_2, blue_offer_3, blue_offer_4, blue_offer_5,
            blue_offer_6, blue_offer_7, blue_offer_8, blue_offer_9, blue_offer_10,
            blue_offer_11, blue_offer_12, blue_offer_13, blue_offer_14, blue_offer_15, blue_offer_16,
            blue_offer_17, blue_offer_18, blue_offer_19, blue_offer_20, blue_offer_21, blue_offer_22,
            blue_offer_23]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def dsbs_genlog_rows():
    # dsbs-offer with history price that does not pass the check (historyPrice < oldPrice).
    # We don't check history price for dsbs-offers, so this offer should be written to genlog
    dsbs_offer_1 = default_genlog()
    dsbs_offer_1['offer_id'] = 'dsbs_1'
    dsbs_offer_1['market_sku'] = 512201
    dsbs_offer_1['binary_price'] = generate_binary_price_dict(123)
    dsbs_offer_1['binary_oldprice'] = generate_binary_price_dict(456)
    dsbs_offer_1['binary_unverified_oldprice'] = generate_binary_price_dict(456)
    dsbs_offer_1['binary_history_price'] = generate_binary_price_dict(price=450)
    dsbs_offer_1['history_price_is_valid'] = True
    dsbs_offer_1['cpa'] = 4

    # dsbs-offer with history price that passes the check (historyPrice < oldPrice).
    # We don't check history price for dsbs-offers, so this offer should be written to genlog
    dsbs_offer_2 = default_genlog()
    dsbs_offer_2['offer_id'] = 'dsbs_2'
    dsbs_offer_2['market_sku'] = 512202
    dsbs_offer_2['binary_price'] = generate_binary_price_dict(124)
    dsbs_offer_2['binary_oldprice'] = generate_binary_price_dict(457)
    dsbs_offer_2['binary_unverified_oldprice'] = generate_binary_price_dict(457)
    dsbs_offer_2['binary_history_price'] = generate_binary_price_dict(500)
    dsbs_offer_2['history_price_is_valid'] = True
    dsbs_offer_2['cpa'] = 4

    return [dsbs_offer_1, dsbs_offer_2]


@pytest.fixture(scope="module")
def dsbs_genlog_table(yt_server, dsbs_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0001'), dsbs_genlog_rows)
    genlog_table.dump()
    return genlog_table


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


@pytest.fixture(scope="module")
def promo_details():
    # convert time to Moscow time zone as generation stamp will be generated
    now = datetime.now(tz=pytz.timezone('Europe/Moscow'))
    delay = 3  # 3 hours
    good_start_promo_time = now + timedelta(hours=delay)

    promos = [
        generate_default_blue_3p_promo(
            promo_md5='10204',
            start_date=to_timestamp(good_start_promo_time - timedelta(hours=2*delay)),
            end_date=to_timestamp(good_start_promo_time + timedelta(hours=2*delay))
        ),
        generate_default_blue_3p_promo(
            promo_md5='10206',
            start_date=-1,
            end_date=to_timestamp(good_start_promo_time - timedelta(hours=2*delay))
        ),
        generate_default_blue_3p_promo(
            promo_md5='10208',
            start_date=to_timestamp(good_start_promo_time - timedelta(hours=delay)),
            end_date=to_timestamp(good_start_promo_time + timedelta(hours=delay))
        ),
        generate_default_blue_3p_promo(
            promo_md5='10210',
            start_date=to_timestamp(now - timedelta(hours=4)),
            end_date=to_timestamp(now - timedelta(hours=2))
        ),
        {
            'msku': '112201',
            'msku_details': generate_default_msku(
                market_promo_price=150,
                market_old_price=1500,
                source_promo_id='10204'
            ),
        },
        {
            'msku': '112202',
            'msku_details': generate_default_msku(
                market_promo_price=250,
                market_old_price=2500,
                source_promo_id='10206'
            ),
        },
        {
            'msku': '112203',
            'msku_details': generate_default_msku(
                market_promo_price=350,
                market_old_price=3500,
                source_promo_id='10208'
            ),
        },
        {
            'msku': '112205',
            'msku_details': generate_default_msku(
                market_promo_price=450,
                market_old_price=4500,
                source_promo_id='10210'
            ),
        },
        {
            'msku': '112206',
            'msku_details': generate_default_msku(
                market_promo_price=550,
                market_old_price=5500,
                source_promo_id='10204'
            ),
        },
        {
            'msku': '112207',
            'msku_details': generate_default_msku(
                market_promo_price=650,
                market_old_price=6500,
                source_promo_id='10204'
            ),
        },
    ]

    json_path = yatest.common.output_path('yt_promo_details.json')

    return PromoDetails(write_promo_json_to_mmap, json_path, promos)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, promo_details):
    generation = datetime.now().strftime("%Y%m%d_%H%M")
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'yt_promo_details_mmap': promo_details,
    }

    with OffersProcessorTestEnv(
            yt_server,
            generation=generation,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def dsbs_workflow(yt_server, dsbs_genlog_table):
    generation = datetime.now().strftime("%Y%m%d_%H%M")
    input_table_paths = [dsbs_genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            generation=generation,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


def test_old_price_for_blue_offer_with_good_promo(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112201,
                'binary_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
            }
        )
    )


def test_old_price_for_blue_offer_with_expired_promo(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112202,
                'binary_oldprice': {'price': yt.yson.YsonUint64(0)},
            }
        )
    )


def test_old_price_for_blue_offer_with_wrong_price(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112203,
                'binary_oldprice': {'price': yt.yson.YsonUint64(0)},
            }
        )
    )


def test_old_price_for_blue_offer_with_no_promo(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112204,
                'binary_oldprice': {'price': yt.yson.YsonUint64(0)},
            }
        )
    )


def test_old_price_for_blue_offer_with_promo_with_close_expire(workflow):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112205,
                'binary_oldprice': {'price': yt.yson.YsonUint64(0)},
            }
        )
    )


def test_skip_offer_old_price_for_blue_3p_flash_disount_no_promo(workflow):
    """Отбрасываем OldPrice от магазина, если для MSKU Синего Оффера есть актуальная акция.
    Маркетные акции - приоритетнее. Оффер не проходит по условиям маркетной акции, у него не будет скидки."""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112206,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_oldprice': {'price': yt.yson.YsonUint64(0)},
            }
        )
    )


def test_skip_offer_old_price_for_blue_3p_flash_disount_with_promo(workflow):
    """Отбрасываем OldPrice от магазина, если для MSKU Синего Оффера есть актуальная акция.
    Маркетные акции - приоритетнее. Оффер проходит по условиям маркетной акции, его oldPrice берется из маркетной акции."""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112207,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_oldprice': {'price': yt.yson.YsonUint64(65000000000)},
            }
        )
    )


def test_blue_offer_valid_old_price_less_than_history_and_dco(workflow):
    """Если OldPrice у Синего офера удовлетворяет условиям OldPrice <= History && OldPrice <= DcoPrice,
    то она проходит и белую валидацию, и синюю."""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112208,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
            }
        )
    )


def test_blue_offer_valid_old_price_less_than_history_only(workflow):
    """Если OldPrice у Синего офера удовлетворяет условиям OldPrice <= History && OldPrice > DcoPrice,
    то она проходит и белую валидацию, и синюю."""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112209,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
            }
        )
    )


def test_blue_offer_valid_old_price_less_than_dco_greate_than_history(workflow):
    """Если OldPrice у Синего офера удовлетворяет условиям OldPrice > History && OldPrice <= DcoPrice,
    то она проходит синюю валидацию, но не проходит белую."""
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112210,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112210,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            )),
        )
    )


def test_blue_offer_invalid_old_price_greater_than_history_and_dco(workflow):
    """Если OldPrice у Синего офера удовлетворяет условиям OldPrice > History && OldPrice > DcoPrice,
    то она не проходит и белую валидацию, и синюю."""
    assert_that(
        workflow,
        is_not(HasGenlogRecordRecursive(
            {
                'market_sku': 112211,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
            }
        ))
    )


def test_blue_offer_invalid_old_price_empty_history_and_dco(workflow):
    """Если Синего офера есть OldPrice, но нету ни History, ни OldPrice,
    то она не проходит и белую валидацию, и синюю."""
    assert_that(
        workflow,
        is_not(HasGenlogRecordRecursive(
            {
                'market_sku': 112212,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
            }
        ))
    )


def test_blue_offer_valid_old_price_less_than_history_empty_dco(workflow):
    """Если OldPrice у Синего офера удовлетворяет условию OldPrice <= History, но у него нету DcoPrice,
    то она проходит и белую валидацию, и синюю."""
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112213,
                'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
            }
        )
    )


def test_blue_offer_valid_old_price_less_than_dco_only(workflow):
    """Если OldPrice у Синего офера удовлетворяет условию OldPrice <= DcoPrice, но у него нету HistoryPrice,
    то она проходит синюю валидацию, но не проходит белую."""
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112214,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112214,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            )),
        )
    )


def test_blue_offer_price_limit(workflow):
    """При цене выше лимита оферу выставляется отдельный флаг скрытия"""
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112216,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_price_limit': {'price': yt.yson.YsonUint64(5000000000)},
                    'disabled_by_price_limit': True,
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112215,
                    'disabled_by_price_limit': True,
                }
            )),
        )
    )


def test_blue_autodiscounts_with_max_history(workflow):
    """ Если HistoryPrice=max(HistoryPrice, ReferenceOldPrice), то на Синем и Белом Маркетах оффер будет показан со
        скидкой от HistoryPrice.
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112217,
                'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                'blue_autodiscounts_oldprice': True,
            }
        )
    )


def test_blue_autodiscounts_with_max_dco(workflow):
    """ Если ReferenceOldPrice=max(HistoryPrice, ReferenceOldPrice), то на Синем оффер будет показан со скидкой от
        ReferenceOldPrice, а на Белом Маркете - со скидкой от HistoryPrice.
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112218,
                'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(12000000000)},
                'blue_autodiscounts_oldprice': True,
            }
        )
    )


def test_blue_autodiscounts_with_dco_price_only(workflow):
    """ Если есть только ReferenceOldPrice, то на Синем оффер будет показан со скидкой от ReferenceOldPrice, а на
        Белом Маркете - без скидки.
    """
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112219,
                    'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                    'blue_autodiscounts_oldprice': True,
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112219,
                    'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                }
            )),
        )
    )


def test_blue_autodiscounts_with_history_price_only(workflow):
    """ Если есть только HistoryPrice, то на Синем и Белом Маркетах оффер будет показан со скидкой от HistoryPrice.
    """
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'market_sku': 112220,
                'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                'binary_blue_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                'binary_white_oldprice': {'price': yt.yson.YsonUint64(15000000000)},
                'blue_autodiscounts_oldprice': True,
            }
        )
    )


def test_blue_autodiscounts_with_invalid_value(workflow):
    """ Если есть и HistoryPrice, и ReferenceOldPrice, но они не проходят валидацию по правилам Маркета,
        то скидки показано не будет.
    """
    assert_that(
        workflow,
        all_of(
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112221,
                    'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(10010000000)},
                }
            )),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112221,
                    'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(10010000000)},
                }
            )),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112221,
                    'binary_price': {'price': yt.yson.YsonUint64(10000000000)},
                    'blue_autodiscounts_oldprice': True,
                }
            )),
        )
    )


def test_blue_restricted_discounts(workflow):
    """ Если есть OldPrice и HistoryPrice, но есть ограничение на показ скидок, то скидки показаны не будут.
    """
    assert_that(
        workflow,
        all_of(
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112222,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            )),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112222,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            )),
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112222,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                }
            ),
        )
    )


def test_blue_invalid_history_price_on_white(workflow):
    """ Если есть OldPrice и HistoryPrice, но историческая цена не прошла проверку в pricedrops, то скидки показаны
        не будут на белом, но будут показаны на синем.
    """
    assert_that(
        workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 112223,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_blue_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            ),
            is_not(HasGenlogRecordRecursive(
                {
                    'market_sku': 112223,
                    'binary_price': {'price': yt.yson.YsonUint64(6000000000)},
                    'binary_white_oldprice': {'price': yt.yson.YsonUint64(7000000000)},
                }
            )),
        )
    )


def test_dsbs_offers(dsbs_workflow):
    """ dsbs-офферы не проверяются по ограничению historyPrice >= oldPrice и записываются в genlog
    """
    assert_that(
        dsbs_workflow,
        all_of(
            HasGenlogRecordRecursive(
                {
                    'market_sku': 512201,
                    'binary_price': {'price': yt.yson.YsonUint64(123 * (10 ** 7))},
                    'binary_oldprice': {'price': yt.yson.YsonUint64(456 * (10 ** 7))},
                    'history_price_is_valid': True,
                    'binary_history_price': {'price': yt.yson.YsonUint64(450 * (10 ** 7))},
                }
            ),
            HasGenlogRecordRecursive(
                {
                    'market_sku': 512202,
                    'binary_price': {'price': yt.yson.YsonUint64(124 * (10 ** 7))},
                    'binary_oldprice': {'price': yt.yson.YsonUint64(457 * (10 ** 7))},
                    'history_price_is_valid': True,
                    'binary_history_price': {'price': yt.yson.YsonUint64(500 * (10 ** 7))},
                }
            ),
        )
    )
