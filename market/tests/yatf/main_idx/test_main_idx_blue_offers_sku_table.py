# coding: utf-8


import pytest
import datetime
import time
from collections import namedtuple

from hamcrest import (
    assert_that,
    has_entries,
    has_items,
    has_length,
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
from market.idx.generation.yatf.resources.prepare.blue_promo_table import BluePromoDetailsTable
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
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


def __make_blue_offer(feed, msku, offer_id, ware_md5):
    return {
        'msku': msku,
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            offer_flags=OfferFlags.BLUE_OFFER.value,
            ware_md5=ware_md5,
        ),
        'uc': make_uc_proto_str(),
    }


def __make_offer(feed, offer_id, ware_md5):
    return {
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            ware_md5=ware_md5,
        ),
        'uc': make_uc_proto_str(),
    }


BLUE_OFFERS = [__make_blue_offer(feed, msku, offer_id, ware_md5) for (feed, msku, offer_id, ware_md5) in [
    (1000, 1, 'offer1', 'hc1cVZiClnllcxjhGX0_c1'),
    (1000, 2, 'offer2', 'hc1cVZiClnllcxjhGX0_c2'),
    (2000, 2, 'offer3', 'hc1cVZiClnllcxjhGX0_c3'),
    (1000, 3, 'offer4', 'hc1cVZiClnllcxjhGX0_c4'),
    ]]


WHITE_OFFERS = [__make_offer(feed,  offer_id, ware_md5) for (feed, offer_id, ware_md5) in [
    (1000, 'offer1', 'hc1cVZiClnllcxjhGX0_c1'),
    ]]


PipelineParams = namedtuple('PipelineParams', [
    'shards',
    'blue_shards',
    'white_offer_count',
    'blue_offer_count',
])


@pytest.yield_fixture(scope="module")
def pipeline_params(request):
    return PipelineParams(
        shards=1,
        blue_shards=1,
        white_offer_count=0,
        blue_offer_count=len(BLUE_OFFERS)
    )


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


@pytest.fixture()
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': 'collected_promo_details',
        },
        'misc': {
            'blue_offers_enabled': 'true',
        },

    }


@pytest.yield_fixture()
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture()
def source_offers_raw(pipeline_params):
    return WHITE_OFFERS[: pipeline_params.white_offer_count]


@pytest.yield_fixture()
def source_blue_offers_raw(pipeline_params):
    return BLUE_OFFERS[: pipeline_params.blue_offer_count]


@pytest.yield_fixture()
def source_msku_contex(source_blue_offers_raw):
    return [
        {
            'msku': blue_offer['msku'],
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': blue_offer['feed_id'],
            'offer_id': blue_offer['offer_id'],
            'session_id': blue_offer['session_id'],
            'offer': blue_offer['offer'],
            'uc': blue_offer['uc'],
        }
        for blue_offer in source_blue_offers_raw
    ]


@pytest.yield_fixture()
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
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'collected_promo_details', 'recent'),
            data=[],
        ),
    }


@pytest.yield_fixture()
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree, pipeline_params):
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
            shards=pipeline_params.shards,
            half_mode=False,
            blue_shards=pipeline_params.blue_shards,
            **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_blue_offers_sku_count(main_idx):
    """Проверяем кол-во записей в таблице blue_offers_sku
    """
    offers = main_idx.outputs['blue_offers_sku']
    assert_that(offers, has_length(7))  # blue + msku


def test_blue_offers_sku_table(main_idx):
    """Проверяем данные из таблицы blue_offers_sku
    """
    offers = main_idx.outputs['blue_offers_sku']

    expected = [   # интересуют только синие офферы
        {'sku': 1L, 'ware_md5': 'hc1cVZiClnllcxjhGX0_c1', 'part': 0L},
        {'sku': 2L, 'ware_md5': 'hc1cVZiClnllcxjhGX0_c2', 'part': 0L},
        {'sku': 2L, 'ware_md5': 'hc1cVZiClnllcxjhGX0_c3', 'part': 0L},
        {'sku': 3L, 'ware_md5': 'hc1cVZiClnllcxjhGX0_c4', 'part': 0L},
    ]

    assert_that(
        offers,
        has_items(*[
            has_entries(entry) for entry in expected
        ])
    )
