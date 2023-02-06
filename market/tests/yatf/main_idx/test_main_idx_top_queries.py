# coding: utf-8

import datetime
import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    Offers2ParamTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.pylibrary.proto_utils import proto_to_dict
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto,
    make_offer_proto_str,
    make_uc_proto,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.proto.feedparser.Promo_pb2 import OfferPromo as OfferPromoPb
from market.proto.feedparser.TopQueries_pb2 import (
    TopQueriesStat as TopQueriesStatPb,
    TopQueriesStatRecord as TopQueriesStatRecordPb,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 2
HALF_MODE = False
CATEGORY_ID = 624627
ABSENT_CATEGORY_ID = 5236262
PARAM_1_ID = 111
PARAM_2_ID = 222
ABSENT_PARAM_ID = 333
UNKNOWN_PARAM_ID = 444


def make_default_uc_proto():
    data = {
        'model_id': 125,
        'category_id': CATEGORY_ID,
        'params': [
            dict(param_id=PARAM_1_ID),
            dict(param_id=PARAM_2_ID),
            dict(param_id=UNKNOWN_PARAM_ID),
        ],
    }
    return make_uc_proto(**data)


def make_default_uc_proto_str():
    return make_default_uc_proto().SerializeToString()


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


@pytest.yield_fixture(scope="module")
def source_promos_raw():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'withoutTopQueriesStat',
            'offer': None,  # в проде есть такие записи, у которых отсутствует offer - это разные promos и проч.
            'session_id': 30,
            'uc': None,
            'promo': OfferPromoPb(offer_id='offerWithHole').SerializeToString(),
        }
    ]


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'withTopQueriesOfferStat',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20001),
            'uc': make_default_uc_proto_str(),
            'ware_md5': 'md5withTopQueriesOfferStat'
        },
        {
            'feed_id': 2000,
            'offer_id': 'withTopQueriesAllStat',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20002),
            'uc': make_default_uc_proto_str(),
            'ware_md5': 'md5withTopQueriesAllStat',
        },
        {
            'feed_id': 2000,
            'offer_id': 'withoutTopQueriesStat',
            'session_id': 30,
            'offer': make_offer_proto_str(price=20003),
            'uc': make_default_uc_proto_str(),
            'ware_md5': 'md5withoutTopQueriesStat',
        },
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        {
            'msku': 111,
            'feed_id': 3000,
            'offer_id': 'blueOffer',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True
            ),
            'uc': make_default_uc_proto_str(),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        {
            'msku': 111,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS111',
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        },
    ]


@pytest.fixture(scope="module")
def source_top_queries_offer_stats():
    return [
        {
            'feed_id': 2000,
            'ware_md5': 'md5withTopQueriesOfferStat',
            'all_shows': 5,
            'offer_shows': 3,
            'original_query': 'some random query',
            'position_avg': 3.4,
        },
        # Статистика для не существующего оффера
        {
            'feed_id': 2,
            'ware_md5': 'md5ImNotExist',
            'all_shows': 4,
            'offer_shows': 3,
            'original_query': 'another random query',
            'position_avg': 1999.,
        },
    ]


@pytest.fixture(scope="module")
def source_top_queries_all_stats():
    return [
        {
            'feed_id': 2000,
            'ware_md5': 'md5withTopQueriesAllStat',
            'all_shows': 15,
            'offer_shows': 13,
            'original_query': 'some another query',
            'position_avg': 3.14,
        },
        {
            'feed_id': 3000,
            'ware_md5': 'md5withoutTopQueriesStat',
            'all_shows': 25,
            'offer_shows': 23,
            'original_query': 'yet another query',
            'position_avg': 3.24,
        },
    ]


@pytest.fixture(scope="module")
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'use_promo_table': 'true',
            'use_top_queries': 'true',
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def source_yt_tables(
        yt_server,
        or3_config,
        source_offers_raw,
        source_promos_raw,
        source_msku_contex,
        source_blue_offers_raw,
        source_top_queries_offer_stats,
        source_top_queries_all_stats,
):
    yt_home_path = or3_config.options['yt']['home_dir']
    yt_dir_prefix = ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION)
    return {
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw,
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_dir_prefix, 'work', 'offer2pic_unsorted'),
            data=[]
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_dir_prefix, 'work', 'offer2param_unsorted'),
            data=[]
        ),
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_dir_prefix, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'promos_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'promos_raw'),
            data=source_promos_raw
        ),
        'top_queries_offer_stats': YtTableResource(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mstat', 'top_queries_offer_stats'),
            data=source_top_queries_offer_stats
        ),
        'top_queries_all_stats': YtTableResource(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mstat', 'top_queries_all_stats'),
            data=source_top_queries_all_stats
        ),
    }


@pytest.fixture(scope="module")
def expected_offers():
    return {
        # Есть содержимое для поля top_queries_offer
        'withTopQueriesOfferStat': {
            'feed_id': 2000,
            'offer_id': 'withTopQueriesOfferStat',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=20001,
                    top_queries_offer=TopQueriesStatPb(
                        record=[
                            TopQueriesStatRecordPb(
                                query='some random query',
                                offer_shows_by_query=3,
                                offer_position=3.4,
                                offer_shows=5
                            )
                        ]
                    ),
                )
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'ware_md5': '',
            'msku': 0,
            'is_fake_msku_offer': False,
            'cpa': 0,
        },
        # Есть содержимое для поля top_queries_all
        'withTopQueriesAllStat': {
            'feed_id': 2000,
            'offer_id': 'withTopQueriesAllStat',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=20002,
                    top_queries_all=TopQueriesStatPb(
                        record=[
                            TopQueriesStatRecordPb(
                                query='some another query',
                                offer_shows_by_query=13,
                                offer_position=3.14,
                                offer_shows=15
                            )
                        ]
                    ),
                )
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'ware_md5': '',
            'msku': 0,
            'is_fake_msku_offer': False,
            'cpa': 0,
        },
        # Есть содержимое для поля top_queries_all с тем же ware_md5, но другим feed_id.
        'withoutTopQueriesStat': {
            'feed_id': 2000,
            'offer_id': 'withoutTopQueriesStat',
            'session_id': 30,
            'offer': proto_to_dict(
                make_offer_proto(
                    price=20003,
                )
            ),
            'promo': proto_to_dict(
                OfferPromoPb(offer_id='offerWithHole')
            ),
            'uc': proto_to_dict(make_default_uc_proto()),
            'ware_md5': '',
            'msku': 0,
            'is_fake_msku_offer': False,
            'cpa': 0,
        },
    }


@pytest.yield_fixture(scope="module")
def main_idx(
        yt_server,
        or3_config,
        source_yt_tables,
        tovar_tree
):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(
            yt_server.get_yt_client().exists(path),
            "Table {} doesn\'t exist".format(path)
        )

    yt_home_path = or3_config.options['yt']['home_dir']
    or3_config.options['yt'].update({
        'yt_mstat_top_queries_offer_table': source_yt_tables['top_queries_offer_stats'].table_path,
        'yt_mstat_top_queries_all_table': source_yt_tables['top_queries_all_stats'].table_path,
    })
    resources = {
        'config': or3_config,
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


@pytest.yield_fixture(scope="module")
def result_offers(main_idx):
    res = main_idx.outputs['offers_by_offer_id']
    return res


def test_offers_count(result_offers):
    assert_that(len(result_offers), equal_to(5))  # 3 white + 1 blue + 1 msku


@pytest.mark.parametrize(
    'offer_id', [
        'withTopQueriesOfferStat',
        'withTopQueriesAllStat',
        'withoutTopQueriesStat',
    ]
)
def test_offer(result_offers, expected_offers, offer_id):
    if 'top_queries_offer' in expected_offers[offer_id]['offer']:
        assert_that(result_offers[offer_id]['genlog']['top_queries_offer'], equal_to(expected_offers[offer_id]['offer']['top_queries_offer']))

    if 'top_queries_all' in expected_offers[offer_id]['offer']:
        assert_that(result_offers[offer_id]['genlog']['top_queries_all'], equal_to(expected_offers[offer_id]['offer']['top_queries_all']))


def test_blue_offer(result_offers):
    actual_blue_offer = result_offers['blueOffer']
    assert('top_queries_offer' not in actual_blue_offer['genlog'])
    assert('top_queries_all' not in actual_blue_offer['genlog'])
