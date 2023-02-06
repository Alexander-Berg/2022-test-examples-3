# coding: utf-8

import datetime
import pytest
from collections import namedtuple
from hamcrest import (
    assert_that,
    equal_to,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    OffersRawTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    make_msku_contex_dict,
    make_offer_proto_str,
    make_uc_proto_str,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.pylibrary.offer_flags.flags import OfferFlags


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
TITLE_FROM_OFFER = 'Title from offer'
TITLE_FROM_MSKU = 'Title from msku'
COUNT_SHARDS = 2
BERU_SHOP = 431782
BERU_FEED = 475690
HALF_MODE = False


@pytest.fixture(scope="module")
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


def make_raw_blue_offer_dict(
    msku,
    feed_id,
    offer_id,
    title=TITLE_FROM_OFFER,
):
    return {
        'msku': msku,
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': 30,
        'offer': make_offer_proto_str(
            title=title,
            market_sku=msku,
            is_blue_offer=True,
            offer_flags=OfferFlags.BLUE_OFFER.value,
        ),
        'uc': make_uc_proto_str(model_id=125),
    }


@pytest.fixture(scope="module")
def source_blue_offers_raw():
    return [
        make_raw_blue_offer_dict(msku=111, feed_id=11, offer_id="knownOffer", title=TITLE_FROM_OFFER),
        make_raw_blue_offer_dict(msku=111, feed_id=22, offer_id="offerWithEmptyTitle", title=""),
    ]


@pytest.fixture(scope="module")
def source_msku_contex():
    return [
        make_msku_contex_dict(msku=111, feed_id=BERU_FEED, shop_id=BERU_SHOP, title=TITLE_FROM_MSKU),
    ]


PipelineParams = namedtuple(
    'PipelineParams', [
        'use_original_title_for_blue_offers',
    ]
)


@pytest.fixture(
    scope="module",
    params=[
        PipelineParams(
            use_original_title_for_blue_offers=False,
        ),
        PipelineParams(
            use_original_title_for_blue_offers=True,
        ),
    ],
    ids=[
        'regular_main-idx',
        'use_original_title_for_blue_offers',
    ]
)
def pipeline_params(request):
    return request.param


@pytest.fixture(scope="module")
def or3_config_data(pipeline_params):
    # Здесь pipeline_params необходим для независимых запусков main-idx
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'use_original_title_for_blue_offers': pipeline_params.use_original_title_for_blue_offers,
        }
    }


@pytest.fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_blue_offers_raw,
    source_msku_contex,
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
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
    }


@pytest.fixture(scope="module")
def create_source_yt_tables(yt_server, source_yt_tables):
    yt_client = yt_server.get_yt_client()

    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_client.exists(path), "Table {} doesn\'t exist".format(path))


@pytest.fixture(scope="module")
def main_idx(yt_server, or3_config, create_source_yt_tables, tovar_tree):
    yt_home_path = ypath_join(or3_config.options['yt']['home_dir'])
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


def test_blue_offers_enrich_title(main_idx, pipeline_params):
    """ Проверяет обогащение оффера тайтлом из msku. При включенном флаге обогащение не происходит.
    """

    actual = main_idx.outputs['offers_by_offer_id']['knownOffer']['genlog']['title']
    expected = TITLE_FROM_OFFER if pipeline_params.use_original_title_for_blue_offers else TITLE_FROM_MSKU

    assert_that(
        actual,
        equal_to(expected),
        "Incorrect title",
    )


def test_blue_offers_empty_title(main_idx):
    """ Проверяет, что если у оффера пустой тайтл обогащение происходит независимо от флага.
    """

    actual = main_idx.outputs['offers_by_offer_id']['offerWithEmptyTitle']['genlog']['title']
    expected = TITLE_FROM_MSKU

    assert_that(
        actual,
        equal_to(expected),
        "Incorrect title",
    )
