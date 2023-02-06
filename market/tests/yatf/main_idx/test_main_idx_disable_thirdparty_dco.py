# coding: utf-8

import datetime
import time
import pytest
from collections import namedtuple


from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
)
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
CATEGORY_WITH_RESTRICTED_DISCOUNT = 1
SESSION_ID = int(time.time())
MI3_TYPE = 'main'

BLUE_FEED_ID = 8765
BLUE_FEED_ID_2 = 2222


def __make_blue_offer(feed, msku, offer_id):
    return {
        'msku': msku,
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            offer_flags=OfferFlags.BLUE_OFFER.value
        ),
        'uc': make_uc_proto_str(),
    }


BLUE_OFFERS = [__make_blue_offer(feed, msku, offer_id) for (feed, msku, offer_id) in [
    (BLUE_FEED_ID, 1, '111'),
    (BLUE_FEED_ID_2, 2, '222'),
    (BLUE_FEED_ID, 2, '333'),
    ]]


PipelineParams = namedtuple('PipelineParams', [
    'shards',
    'blue_shards',
    'white_offer_count',
    'blue_offer_count',
])


@pytest.fixture(
    params=[
        PipelineParams(
            shards=1,
            blue_shards=1,
            white_offer_count=0,
            blue_offer_count=len(BLUE_OFFERS)
        ),
    ],
    ids=[
        'blue_pipeline',
    ]
)
def pipeline_params(request):
    return request.param


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


@pytest.fixture(scope="module")
def or3_config_data(yt_server):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'blue_price_validation_enabled': 'true',
            'thirdparty_dco_enabled': 'false'
        },
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture()
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
            data={}
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
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
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
    }


@pytest.yield_fixture()
def main_idx(
    yt_server,
    or3_config,
    source_yt_tables,
    pipeline_params,
    tovar_tree
):
    resources = {
        'config': or3_config,
        'tovar_tree_pb': TovarTreePb(tovar_tree)
    }
    resources.update(source_yt_tables)

    with Or3MainIdxTestEnv(
            yt_stuff=yt_server,
            generation=GENERATION,
            mi3_type=MI3_TYPE,
            shards=pipeline_params.shards,
            half_mode=False,
            blue_shards=pipeline_params.blue_shards,
            **resources
    ) as mi:
        mi.execute()
        mi.verify()
        yield mi


def test_count_blue_offers_merged_table(
        main_idx,
        source_blue_offers_raw,
        source_msku_contex
):
    '''
    Проверяет общее количество офферов = количество всех синих офферов
    Синие оффера - это fake_msku + реальный синие оффера
    '''
    actual_merged_offers_count = len(main_idx.outputs['blue_offers'])
    assert actual_merged_offers_count > 0
    expected_merged_offers_count = len(source_blue_offers_raw) + len(source_msku_contex) - 1
    assert expected_merged_offers_count > 0
    assert actual_merged_offers_count == expected_merged_offers_count
