# coding: utf-8

""" Проверяем шардирование белых и синих офферов, а так же синих офферов в таблице blue_offers_urls.
Белые оффера должны шардироваться по мску в случае, если их категория включена в данный режим шардирования,
либо же если включена настройка все белые офферы шардировать по мску.
Белые офферы без мску продолжают шардироваться по feed-offer-id в любом случае.
Синие и дсбс (с мску) офферы, как и всегда, шардируются по мску.
"""

import pytest
import datetime
import time


from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    has_items,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    ApiSnapshotTable,
    BlueOffersRawTable,
    Offers2ParamTable,
    Offers2ModelTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    CpaStatus,
    make_offer_proto_str,
    make_uc_proto_str,
    make_msku_contex_dict,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
SESSION_ID = int(time.time())
MI3_TYPE = 'main'

SHARDS = 16
BLUE_SHARDS = 8

WHITE_OFFER1 = 'padding___white_offer1'
WHITE_OFFER2 = 'padding___white_offer2'
WHITE_OFFER3 = 'padding___white_offer3'
WHITE_OFFER4 = 'padding___white_offer4'
WHITE_OFFER5 = 'padding___white_offer5'
WHITE_OFFER6 = 'padding___white_offer6'
WHITE_OFFER7 = 'padding___white_offer7'
BLUE_OFFER1 = 'padding____blue_offer1'
BLUE_OFFER2 = 'padding____blue_offer2'
DSBS_OFFER1 = 'padding____dsbs_offer1'
DSBS_OFFER2 = 'padding____dsbs_offer2'
MSKU1 = 'wxdbP0Y7RDCTk1EnsixTf1'
MSKU2 = 'wxdbP0Y7RDCTk1EnsixTf2'
MSKU3 = 'wxdbP0Y7RDCTk1EnsixTf3'

WHITE_OFFER1_FEED_OFFER_ID_SHARD = 0
WHITE_OFFER2_FEED_OFFER_ID_SHARD = 1
WHITE_OFFER3_FEED_OFFER_ID_SHARD = 6
WHITE_OFFER4_FEED_OFFER_ID_SHARD = 7
WHITE_OFFER5_FEED_OFFER_ID_SHARD = 8
WHITE_OFFER6_FEED_OFFER_ID_SHARD = 9
WHITE_OFFER7_FEED_OFFER_ID_SHARD = 15
MSKU1_SHARD = 3
MSKU2_SHARD = 6
MSKU111_SHARD = 8

MSKU1_BLUE_SHARD = 1
MSKU2_BLUE_SHARD = 3
MSKU111_BLUE_SHARD = 4

ROOT_CATEGORY_ID = 1
MSKU_SHARDING_CATEGORY_ID_PARENT = 2
MSKU_SHARDING_CATEGORY_ID_CHILD = 3
FEED_OFFER_ID_SHARDING_CATEGORY_ID = 4


def __make_offer(feed, msku, offer_id, ware_md5, category_id, is_blue_offer=False, is_dsbs_offfer=False, is_dsbs_for_blue_shard=False):
    result = {
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku if msku >= 0 else 0,
            ware_md5=ware_md5,
            is_blue_offer=is_blue_offer,
            offer_flags=OfferFlags.BLUE_OFFER.value if is_blue_offer else None,
            cpa=CpaStatus.REAL if is_dsbs_offfer else CpaStatus.NO,
            url=ware_md5,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
            category_id=category_id,
        ),
    }

    if is_blue_offer or is_dsbs_for_blue_shard and msku:
        result['msku'] = msku
    return result


def _make_blue_offer(feed, msku, offer_id, ware_md5, category_id):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id, is_blue_offer=True)


def _make_white_offer(feed, msku, offer_id, ware_md5, category_id):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id)


def _make_dsbs_offer(feed, msku, offer_id, ware_md5, category_id, for_blue_shard=False):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id, is_dsbs_offfer=True, is_dsbs_for_blue_shard=for_blue_shard)


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
        MboCategory(
            hid=MSKU_SHARDING_CATEGORY_ID_PARENT,
            parent_hid=1,
            tovar_id=0,
            unique_name="Parent category for msku sharding",
            name="Parent category for msku sharding",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=MSKU_SHARDING_CATEGORY_ID_CHILD,   # шардирование по мску не включится при задании родительской категории
            parent_hid=MSKU_SHARDING_CATEGORY_ID_PARENT,
            tovar_id=0,
            unique_name="Child category for msku sharding",
            name="Child category for msku sharding",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=FEED_OFFER_ID_SHARDING_CATEGORY_ID,
            parent_hid=1,
            tovar_id=0,
            unique_name="Caterory for sharding by feed-offer-id",
            name="Caterory for sharding by feed-offer-id",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(scope="module")
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    misc = {
        'blue_offers_enabled': 'true',
    }

    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': misc,
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def expected_shards_white():
    return {
        # шардируем белые по мску
        WHITE_OFFER5: MSKU111_SHARD,
        # нет мску - в любом случае шардирование по feed-offer-id
        WHITE_OFFER6: WHITE_OFFER6_FEED_OFFER_ID_SHARD,
        # msku < 0 - в любом случае шардирование по feed-offer-id
        WHITE_OFFER7: WHITE_OFFER7_FEED_OFFER_ID_SHARD,
        # мску, синие, дсбс с мску - шардирование по мску
        MSKU1: MSKU1_SHARD,
        MSKU2: MSKU2_SHARD,
        MSKU3: MSKU111_SHARD,
        BLUE_OFFER1: MSKU111_SHARD,
        BLUE_OFFER2: MSKU111_SHARD,
        DSBS_OFFER1: MSKU2_SHARD,
        DSBS_OFFER2: MSKU111_SHARD,
    }


@pytest.yield_fixture(scope="module")
def expected_shards_blue():
    """Шардирование всегда по мску"""
    return {
        MSKU1: MSKU1_BLUE_SHARD,
        MSKU2: MSKU2_BLUE_SHARD,
        MSKU3: MSKU111_BLUE_SHARD,
        BLUE_OFFER1: MSKU111_BLUE_SHARD,
        BLUE_OFFER2: MSKU111_BLUE_SHARD,
        DSBS_OFFER2: MSKU111_BLUE_SHARD,
    }


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    # https://a.yandex-team.ru/arc/trunk/arcadia/market/library/algorithm/offers_sharding_ut.cpp?rev=8117252&blame=true#L8
    return [
        _make_white_offer(418656, 1, '1115112', WHITE_OFFER1, ROOT_CATEGORY_ID),
        _make_white_offer(418656, 2, '1097602', WHITE_OFFER2, MSKU_SHARDING_CATEGORY_ID_PARENT),
        _make_white_offer(418656, 1, '1126379', WHITE_OFFER3, MSKU_SHARDING_CATEGORY_ID_CHILD),
        _make_white_offer(418656, 111, '7982163', WHITE_OFFER4, FEED_OFFER_ID_SHARDING_CATEGORY_ID),
        _make_white_offer(418656, 111, '1074153', WHITE_OFFER5, category_id=None),  # нет категории
        _make_white_offer(418656, None, '1133022', WHITE_OFFER6, MSKU_SHARDING_CATEGORY_ID_PARENT),  # нет msku
        _make_dsbs_offer(418656, 2, '1026681', DSBS_OFFER1, FEED_OFFER_ID_SHARDING_CATEGORY_ID),
        _make_white_offer(418656, -1, '1106521', WHITE_OFFER7, MSKU_SHARDING_CATEGORY_ID_PARENT),  # msku = -1
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        _make_blue_offer(418656, 111, '1087631', BLUE_OFFER1, FEED_OFFER_ID_SHARDING_CATEGORY_ID),
        _make_blue_offer(418656, 111, '1032020', BLUE_OFFER2, FEED_OFFER_ID_SHARDING_CATEGORY_ID),
        _make_dsbs_offer(418656, 111, '0198037', DSBS_OFFER2, FEED_OFFER_ID_SHARDING_CATEGORY_ID, for_blue_shard=True),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex(source_blue_offers_raw):
    return [
        make_msku_contex_dict(
            msku=1,
            title='msku1',
            feed_id=418656,
            shop_id=300,
            ware_md5=MSKU1,
            category_id=MSKU_SHARDING_CATEGORY_ID_PARENT,
        ),
        make_msku_contex_dict(
            msku=2,
            title='msku2',
            feed_id=418656,
            shop_id=301,
            ware_md5=MSKU2,
            category_id=MSKU_SHARDING_CATEGORY_ID_PARENT,
        ),
        make_msku_contex_dict(
            msku=111,
            title='msku3',
            feed_id=418656,
            shop_id=302,
            ware_md5=MSKU3,
            category_id=MSKU_SHARDING_CATEGORY_ID_PARENT,
        ),
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_offers_raw,
    source_blue_offers_raw,
    source_msku_contex,
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'promos_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'promos_raw'),
            data=[]
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
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
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data={}
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
    with Or3MainIdxTestEnv(
            yt_stuff=yt_server,
            generation=GENERATION,
            mi3_type=MI3_TYPE,
            shards=SHARDS,
            half_mode=False,
            blue_shards=BLUE_SHARDS,
            **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_sharding_white(main_idx, expected_shards_white):
    """Проверяем шардирование белых офферов
    """
    assert_that(len(main_idx.outputs['offers']), equal_to(14))  # 7 white + 2 blue + 3 msku + 2 dsbs

    ware_md5_to_shard = dict()
    for shard_num, table in enumerate(main_idx.outputs['offers_shards']):
        for offer in table:
            ware_md5_to_shard[offer['ware_md5']] = shard_num

    assert_that(ware_md5_to_shard, has_entries(expected_shards_white))


def test_sharding_blue(main_idx, expected_shards_blue):
    """Проверяем шардирование синих офферов
    """
    assert_that(len(main_idx.outputs['blue_offers']), equal_to(6))  # 2 blue + 1 dsbs + 3 msku

    ware_md5_to_shard = dict()
    for shard_num, table in enumerate(main_idx.outputs['blue_offers_shards']):
        for offer in table:
            ware_md5_to_shard[offer['ware_md5']] = shard_num

    assert_that(ware_md5_to_shard, has_entries(expected_shards_blue))


def test_blue_offers_urls(main_idx, expected_shards_blue):
    """Проверяем шардирование синих офферов в blue_offers_urls
    """
    assert_that(len(main_idx.outputs['blue_offers_urls']), equal_to(len(expected_shards_blue)))
    assert_that(
        main_idx.outputs['blue_offers_urls'],
        has_items(*[has_entries({'ware_md5': x, 'part': expected_shards_blue[x]}) for x in expected_shards_blue])
    )
