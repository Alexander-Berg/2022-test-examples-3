# coding: utf-8
"""
Тесты для проверки дедупликации ware_md5 в main-idx.

Схема работы:
- управляется через флаг ware_md5_deduplicator_enabled
- запускает MapReduce для построения таблиц с офферами (feed_id, offer_id), у которых есть дубли по ware_md5 в рамках
  фида
- таблицы на выходе две: одна для BlueEnrich (построение синего шарда), другая для FinalReduce (для белого шарда)
- офферам, у которых есть дубли, устаналивается rejected_by_duplicated_ware_md5, который приводит к удалению оффера из
  поколения в offers-processor

Зачем:
- офферы из хранилища лишины проверки на уникальность ware_md5, т.к. они могу создаваться вне сессии парсинга фида
- для синих офферов ware_md5 считается от offer_id, поэтому он уникальный (с точностью до коллизий md5)
- для белых офферов ware_md5 считается от контента, поэтому офферы из хранилища легко можно задублирвать
- dsbs - это белый оффер, но попадает в синий шард, поэтому необходима дедупликация при построении синего шарда
"""

import pytest
import datetime

from hamcrest import (
    assert_that,
    equal_to,
    has_items,
    is_not,
    has_key,
    any_of,
    has_entries,
)

from yt.wrapper import ypath_join
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    OffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    ApiSnapshotTable,
    ModelsTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.pylibrary.offer_flags.flags import DisabledFlags

from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)


DATE = datetime.datetime.now().strftime('%Y-%m-%d')
GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False
SESSION_ID = 30


@pytest.yield_fixture(scope="module")
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


@pytest.fixture(scope="module")
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'ware_md5_deduplicator_enabled': 'true'
        },
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


def create_offer(market_sku, yx_ds_id, feed_id, offer_id, offer_flags=None, url=None, ware_md5=None):
    return {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            yx_ds_id=yx_ds_id,
            feed_id=feed_id,
            offer_id=offer_id,
            offer_flags=offer_flags,
            url=url,
            ware_md5=ware_md5
        ),
        'uc': make_uc_proto_str(market_sku_id=market_sku),
        'ware_md5': ware_md5,
    }


def create_blue_offer(market_sku, supplier_id, feed_id, offer_id, ware_md5, contex_info=None):
    return {
        'msku': market_sku,
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            supplier_id=supplier_id,
            ware_md5=ware_md5,
            feed_id=feed_id,
            offer_id=offer_id,
            is_fake_msku_offer=False,
            disabled_flag_sources=DisabledFlags.MARKET_STOCK.value,
            contex_info=contex_info),
        'uc': make_uc_proto_str(),
        'ware_md5': ware_md5,
        'disabled_flags': 1
    }


def create_msku_contex(market_sku, feed_id, offer_id, ware_md5):
    return {
        'msku': market_sku,
        'msku_exp': 0,
        'msku_experiment_id': '',
        'feed_id': feed_id,
        'offer_id': offer_id,
        'offer': make_offer_proto_str(
            ware_md5=ware_md5,
            feed_id=feed_id,
            offer_id=offer_id,
            is_fake_msku_offer=True),
        'uc': make_uc_proto_str(),
    }


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='white1', ware_md5='hc1cVZiClnllcxjhGX0_cQ'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='white2', ware_md5='hc2cVZiClnllcxjhGX0_cQ'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='white3', ware_md5='hc2cVZiClnllcxjhGX0_cQ'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='white5', ware_md5='hc2cVZiClnllcxjhGX0_cQ'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='white4', ware_md5='hc4cVZiClnllcxjhGX0_cQ'),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        create_blue_offer(market_sku=1, supplier_id=101000, feed_id=8111, offer_id='blue1', ware_md5='hc1cVZiClnllcxjhGX0_cQ'),
        create_blue_offer(market_sku=2, supplier_id=101000, feed_id=8111, offer_id='blue2', ware_md5='hc3cVZiClnllcxjhGX0_cQ'),
        create_blue_offer(market_sku=2, supplier_id=101000, feed_id=8111, offer_id='blue3', ware_md5='hc3cVZiClnllcxjhGX0_cQ'),
        create_blue_offer(market_sku=2, supplier_id=101000, feed_id=8111, offer_id='blue4', ware_md5='hc5cVZiClnllcxjhGX0_cQ'),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        create_msku_contex(market_sku=1, feed_id=9999, offer_id='fake_blue1', ware_md5='hc1cVZiClnllcxjhGX0_cQ'),
        create_msku_contex(market_sku=2, feed_id=9999, offer_id='fake_blue2', ware_md5='hc1cVZiClnllcxjhGX0_cQ'),
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(
        yt_server,
        or3_config,
        source_offers_raw,
        source_blue_offers_raw,
        source_msku_contex
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        # stubs
        'promos_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'promos_raw'),
            data=[]
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data=[]
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data=[]
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data=[]
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data={}
        ),
        'models': ModelsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'models', 'recent'),
            data=[]
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

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
    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute(generate_blue_urls_table=True)
        yield mi


@pytest.yield_fixture(scope="module")
def result_offers(main_idx):
    return main_idx.outputs['offers_by_offer_id']


@pytest.yield_fixture(scope="module")
def result_blue_offers(main_idx):
    return main_idx.outputs['blue_offers_by_offer_id']


@pytest.yield_fixture(scope="module")
def result_ware_md5_duplicates(main_idx):
    return main_idx.outputs['ware_md5_duplicates']


@pytest.yield_fixture(scope="module")
def result_ware_md5_duplicates_with_msku(main_idx):
    return main_idx.outputs['ware_md5_duplicates_with_msku']


def test_offers_count(result_offers, source_msku_contex, source_offers_raw, source_blue_offers_raw):
    assert_that(
        len(result_offers),
        equal_to(
            len(source_msku_contex) +
            len(source_offers_raw) +
            len(source_blue_offers_raw)
        )
    )


def test_blue_offers_count(result_blue_offers, source_msku_contex, source_blue_offers_raw):
    assert_that(
        len(result_blue_offers),
        equal_to(
            len(source_msku_contex) +
            len(source_blue_offers_raw)
        )
    )


def test_ware_md5_duplicates(result_ware_md5_duplicates):
    """
    Проверяем, что офферы с деблирующимися в рамках фида ware_md5 попали в таблицу ware_md5_duplicates:
    - все скрытые (disabled_flags > 0 для blue2, blue3)
    - все, кроме одно раскрытого (white2, или white3, или white5)
    """
    duplicates = result_ware_md5_duplicates
    assert_that(len(duplicates), equal_to(4))
    assert_that(
        duplicates,
        has_items(
            has_entries({
                'feed_id': 1111,
                'offer_id': any_of('white2', 'white3'),
                'ware_md5': 'hc2cVZiClnllcxjhGX0_cQ'
            }),
            has_entries({
                'feed_id': 1111,
                'offer_id': any_of('white3', 'white5'),
                'ware_md5': 'hc2cVZiClnllcxjhGX0_cQ'
            }),
            has_entries({
                'feed_id': 8111,
                'offer_id': 'blue2',
                'ware_md5': 'hc3cVZiClnllcxjhGX0_cQ'
            }),
            has_entries({
                'feed_id': 8111,
                'offer_id': 'blue3',
                'ware_md5': 'hc3cVZiClnllcxjhGX0_cQ'
            })
        )
    )


def test_ware_md5_duplicates_with_msku(result_ware_md5_duplicates_with_msku):
    """
    Проверяем, что офферы с msku с деблирующимися в рамках фида ware_md5 попали в таблицу ware_md5_duplicates_with_msku:
    - скрытые синие (disabled_flags > 0 для blue2, blue3)
    """
    duplicates = result_ware_md5_duplicates_with_msku
    assert_that(len(duplicates), equal_to(2))
    assert_that(
        duplicates,
        has_items(
            has_entries({
                'msku': 2,
                'feed_id': 8111,
                'offer_id': 'blue2',
                'ware_md5': 'hc3cVZiClnllcxjhGX0_cQ'
            }),
            has_entries({
                'msku': 2,
                'feed_id': 8111,
                'offer_id': 'blue3',
                'ware_md5': 'hc3cVZiClnllcxjhGX0_cQ'
            })
        )
    )


def test_rejected_by_duplicated_ware_md5(result_offers, result_blue_offers):
    """ Проверяем установку rejected_by_duplicated_ware_md5 в OffersData.proto в синие и белые шарды, если ware_md5
        дублируется в рамках фида
    """
    assert_that(result_offers['blue2']['offer']['rejected_by_duplicated_ware_md5'], equal_to(True))
    assert_that(result_offers['blue3']['offer']['rejected_by_duplicated_ware_md5'], equal_to(True))
    assert_that(result_blue_offers['blue2']['offer']['rejected_by_duplicated_ware_md5'], equal_to(True))
    assert_that(result_blue_offers['blue3']['offer']['rejected_by_duplicated_ware_md5'], equal_to(True))
    assert_that(
        any_of(
            result_offers['white2']['offer'], has_key('rejected_by_duplicated_ware_md5'),
            result_offers['white3']['offer'], has_key('rejected_by_duplicated_ware_md5'),
            result_offers['white5']['offer'], has_key('rejected_by_duplicated_ware_md5'),
        )
    )


def test_not_rejected_by_duplicated_ware_md5(result_offers, result_blue_offers):
    """ Проверяем, что rejected_by_duplicated_ware_md5 не устанавливается в OffersData.proto, если ware_md5 не
        дублируется в рамках фида
    """
    assert_that(result_offers['blue1']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_offers['blue4']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_offers['white1']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_offers['white4']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_blue_offers['blue1']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_blue_offers['blue4']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))


def test_not_rejected_by_duplicated_ware_md5_for_msku(result_offers, result_blue_offers):
    """ Проверяем, что rejected_by_duplicated_ware_md5 не устанавливается для msku
    """
    assert_that(result_offers['fake_blue1']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_offers['fake_blue2']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_blue_offers['fake_blue1']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
    assert_that(result_blue_offers['fake_blue2']['offer'], is_not(has_key('rejected_by_duplicated_ware_md5')))
