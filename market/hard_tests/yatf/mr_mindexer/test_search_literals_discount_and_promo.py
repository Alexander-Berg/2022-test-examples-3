# coding: utf-8

import pytz
import pytest

from datetime import datetime, timedelta
from hamcrest import assert_that, all_of
import time
import yatest.common

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.resources.offers_indexer.promo_details import PromoDetails
from market.idx.offers.yatf.utils.fixtures import(
    generate_default_blue_3p_promo,
    generate_default_msku,
    default_blue_genlog,
    generate_binary_price_dict,
    binary_promos_md5_base64
)
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.yatf.utils.mmap.promo_indexer_write_mmap import write_promo_json_to_mmap
from market.pylibrary.const.offer_promo import PromoType
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


history_price_date = 20180919


@pytest.fixture(scope="module")
def genlog_rows():
    # offer with msku for good promo, good price (match promo, actual literal, and in statistics)
    blue_offer_1 = default_blue_genlog()
    blue_offer_1['offer_id'] = '0'
    blue_offer_1['market_sku'] = 112201
    blue_offer_1['binary_price'] = generate_binary_price_dict(150)

    # offer with msku for expired promo (match promo, no literal, not in statistics)
    blue_offer_2 = default_blue_genlog()
    blue_offer_2['offer_id'] = '1'
    blue_offer_2['market_sku'] = 112202
    blue_offer_2['binary_price'] = generate_binary_price_dict(240)

    # offer with msku for good promo, but higher price
    blue_offer_3 = default_blue_genlog()
    blue_offer_3['offer_id'] = '2'
    blue_offer_3['market_sku'] = 112203
    blue_offer_3['binary_price'] = generate_binary_price_dict(360)

    # offer with no promo for msku
    blue_offer_4 = default_blue_genlog()
    blue_offer_4['offer_id'] = '3'
    blue_offer_4['market_sku'] = 112204
    blue_offer_4['binary_price'] = generate_binary_price_dict(450)

    # offer with promo with close end date (match promo, actual literal, but not in statistics)
    blue_offer_5 = default_blue_genlog()
    blue_offer_5['offer_id'] = '4'
    blue_offer_5['market_sku'] = 112205
    blue_offer_5['binary_price'] = generate_binary_price_dict(450)

    # offer with old price from shop, msku, good promo for msku, price > Market's price
    blue_offer_6 = default_blue_genlog()
    blue_offer_6['offer_id'] = '5'
    blue_offer_6['market_sku'] = 112206
    blue_offer_6['binary_price'] = generate_binary_price_dict(600)
    blue_offer_6['binary_oldprice'] = generate_binary_price_dict(6000)
    blue_offer_6['binary_unverified_oldprice'] = generate_binary_price_dict(6000)

    # offer with old price from shop, msku, good promo for msku, price < Market's price (match promo, actual literal, in statistics)
    blue_offer_7 = default_blue_genlog()
    blue_offer_7['offer_id'] = '6'
    blue_offer_7['market_sku'] = 112207
    blue_offer_7['binary_price'] = generate_binary_price_dict(600)
    blue_offer_7['binary_oldprice'] = generate_binary_price_dict(7000)
    blue_offer_7['binary_unverified_oldprice'] = generate_binary_price_dict(7000)

    # offer with valid old price, old price <= history && old price <= dco
    blue_offer_8 = default_blue_genlog()
    blue_offer_8['offer_id'] = '7'
    blue_offer_8['market_sku'] = 112208
    blue_offer_8['binary_price'] = generate_binary_price_dict(600)
    blue_offer_8['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_8['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_8['binary_history_price'] = generate_binary_price_dict(price=800)

    # offer with valid old price, old price <= history && old price > dco
    blue_offer_9 = default_blue_genlog()
    blue_offer_9['offer_id'] = '8'
    blue_offer_9['market_sku'] = 112209
    blue_offer_9['binary_price'] = generate_binary_price_dict(600)
    blue_offer_9['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_9['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_9['binary_history_price'] = generate_binary_price_dict(price=800)

    # offer with valid old price, old price > history && old price <= dco
    blue_offer_10 = default_blue_genlog()
    blue_offer_10['offer_id'] = '9'
    blue_offer_10['market_sku'] = 112210
    blue_offer_10['binary_price'] = generate_binary_price_dict(600)
    blue_offer_10['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_10['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_10['binary_history_price'] = generate_binary_price_dict(price=600)

    # offer with invalid old price, old price > history && old price > dco
    blue_offer_11 = default_blue_genlog()
    blue_offer_11['offer_id'] = '10'
    blue_offer_11['market_sku'] = 112211
    blue_offer_11['binary_price'] = generate_binary_price_dict(600)
    blue_offer_11['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_11['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_11['binary_history_price'] = generate_binary_price_dict(price=600)

    # offer with invalid old price, empty history && empty dco
    blue_offer_12 = default_blue_genlog()
    blue_offer_12['offer_id'] = '11'
    blue_offer_12['market_sku'] = 112212
    blue_offer_12['binary_price'] = generate_binary_price_dict(600)
    blue_offer_12['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_12['binary_unverified_oldprice'] = generate_binary_price_dict(700)

    # offer with valid old price, old price <= history empty && empty dco
    blue_offer_13 = default_blue_genlog()
    blue_offer_13['offer_id'] = '12'
    blue_offer_13['market_sku'] = 112213
    blue_offer_13['binary_price'] = generate_binary_price_dict(600)
    blue_offer_13['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_13['binary_unverified_oldprice'] = generate_binary_price_dict(700)
    blue_offer_13['binary_history_price'] = generate_binary_price_dict(price=800)

    # offer with valid old price, history empty && old price <= dco
    blue_offer_14 = default_blue_genlog()
    blue_offer_14['offer_id'] = '13'
    blue_offer_14['market_sku'] = 112214
    blue_offer_14['binary_price'] = generate_binary_price_dict(600)
    blue_offer_14['binary_oldprice'] = generate_binary_price_dict(700)
    blue_offer_14['binary_unverified_oldprice'] = generate_binary_price_dict(700)

    # offer with valid promo, feed price does not pass promo conditions
    blue_offer_15 = default_blue_genlog()
    blue_offer_15['offer_id'] = '14'
    blue_offer_15['market_sku'] = 112215
    blue_offer_15['binary_price'] = generate_binary_price_dict(1000)
    blue_offer_15['binary_oldprice'] = generate_binary_price_dict(1300)
    blue_offer_15['binary_unverified_oldprice'] = generate_binary_price_dict(1300)

    # offer with valid promo, feed price passes promo conditions
    blue_offer_16 = default_blue_genlog()
    blue_offer_16['offer_id'] = '15'
    blue_offer_16['market_sku'] = 112216
    blue_offer_16['binary_price'] = generate_binary_price_dict(900)
    blue_offer_16['binary_oldprice'] = generate_binary_price_dict(1300)
    blue_offer_16['binary_unverified_oldprice'] = generate_binary_price_dict(1300)

    # offer with valid old price, history empty && old price <= dco
    blue_offer_17 = default_blue_genlog()
    blue_offer_17['offer_id'] = '16'
    blue_offer_17['market_sku'] = 112217
    blue_offer_17['binary_price'] = generate_binary_price_dict(1600)
    blue_offer_17['binary_oldprice'] = generate_binary_price_dict(1700)
    blue_offer_17['binary_unverified_oldprice'] = generate_binary_price_dict(1700)
    blue_offer_17['binary_history_price'] = generate_binary_price_dict(price=1800)
    blue_offer_17['promo_type'] = PromoType.BLUE_CASHBACK
    blue_offer_17['binary_promos_md5_base64']=[binary_promos_md5_base64(b'id1'), binary_promos_md5_base64(b'id2')]

    return [blue_offer_1, blue_offer_2, blue_offer_3, blue_offer_4, blue_offer_5,
            blue_offer_6, blue_offer_7, blue_offer_8, blue_offer_9, blue_offer_10,
            blue_offer_11, blue_offer_12, blue_offer_13, blue_offer_14, blue_offer_15,
            blue_offer_16, blue_offer_17]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
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
        {
            'promo_md5': '10204',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(good_start_promo_time + timedelta(hours=2*delay))
            ),
        },
        {
            'promo_md5': '10206',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(good_start_promo_time - timedelta(hours=2*delay))
            ),
        },
        {
            'promo_md5': '10208',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(good_start_promo_time + timedelta(hours=delay))
            ),
        },
        {
            'promo_md5': '10210',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(now + timedelta(hours=2*delay))
            ),
        },
        {
            'promo_md5': '10212',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(good_start_promo_time + timedelta(hours=2*delay))
            ),
        },
        {
            'promo_md5': '10214',
            'promo_details': generate_default_blue_3p_promo(
                start_date=-1,
                end_date=to_timestamp(good_start_promo_time + timedelta(hours=2*delay))
            ),
        },
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
        {
            'msku': '112215',
            'msku_details': generate_default_msku(
                market_promo_price=950,
                market_old_price=1000,
                source_promo_id='10212'
            ),
        },
        {
            'msku': '112216',
            'msku_details': generate_default_msku(
                market_promo_price=950,
                market_old_price=1000,
                source_promo_id='10214'
            ),
        },
    ]

    json_path = yatest.common.output_path('yt_promo_details.json')

    return PromoDetails(write_promo_json_to_mmap, json_path, promos)


@pytest.fixture(scope='module')
def promo_details_gb():
    details = []
    json_path = yatest.common.output_path('yt_promo_details_generic_bundle.json')
    return PromoDetails(write_promo_json_to_mmap, json_path, details)


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table, promo_details, promo_details_gb):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'yt_promo_details_mmap': promo_details,
        'yt_promo_details_gb_mmap': promo_details_gb,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, offers_processor_workflow):
    resources = {
    }

    with MrMindexerBuildTestEnv(**resources) as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        yield env


def test_has_discount_literal_for_blue_offer_with_promo(mr_mindexer_direct):
    assert_that(
        mr_mindexer_direct,
        HasLiterals('#has_discount="1', ['0', '6', '16'])
    )


def test_has_discount_literal_for_blue_offer_with_no_promo(mr_mindexer_direct):
    assert_that(
        mr_mindexer_direct,
        HasNoLiterals('#has_discount="1', ['1', '2', '3', '5'])
    )


def test_promo_search_literal_for_blue_offers(mr_mindexer_direct):
    """Для нужд Черной Пятницы для каждого синего оффера попадающего в акцию добавляется одноменный с
    id акцией литерал. Проверяем, что литерал проставился для нужных офферов."""
    assert_that(mr_mindexer_direct, all_of(
        HasLiterals('#match_blue_promo="10204', ['0', '6']),
        HasLiterals('#match_blue_promo="10210', ['4'])
    ))


def test_no_promo_search_literal_for_blue_offers_without_promo(mr_mindexer_direct):
    """Для нужд Черной Пятницы для каждого синего оффера попадающего в акцию добавляется одноменный с
    id акцией литерал. Проверяем, что литерал не проставляется на офферы, которые не прошли по условиям акции
    или попали в истекшие акции."""
    assert_that(mr_mindexer_direct, all_of(
        HasNoLiterals('#match_blue_promo="10206', ['1']),
        HasNoLiterals('#match_blue_promo="10208', ['2'])
    ))
