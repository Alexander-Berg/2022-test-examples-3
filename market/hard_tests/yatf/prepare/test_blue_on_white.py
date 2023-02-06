# coding: utf-8

import pytest
import datetime
import time

from hamcrest import (
    assert_that,
    is_not,
    has_key,
    all_of,
    has_length
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
COUNT_WHITE_SHARDS = 1
COUNT_BLUE_SHARDS = 1

WHITE_OFFER_ID = 'white'
BLUE_OFFER_ID = 'blue'


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
    (1, 1, BLUE_OFFER_ID, '1111111111111111111111'),
]]


WHITE_OFFERS = [__make_offer(feed,  offer_id, ware_md5) for (feed, offer_id, ware_md5) in [
    (2, WHITE_OFFER_ID, '1111111111111111111112'),
]]


@pytest.fixture(
    scope="module",
    params=[
        False,
        True,
    ],
    ids=[
        'add_blue_offers_to_white_shards_false',
        'add_blue_offers_to_white_shards_true'
    ]
)
def add_blue_offers_to_white_shards_value(request):
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


@pytest.fixture()
def or3_config_data(yt_server, add_blue_offers_to_white_shards_value):
    home_dir = get_yt_prefix()

    add_blue_offers_to_white_shards = 'false'
    if add_blue_offers_to_white_shards_value:
        add_blue_offers_to_white_shards = 'true'

    return {
        'yt': {
            'home_dir': home_dir,
        },
        'general': {
            'add_blue_offers_to_white_shards': add_blue_offers_to_white_shards,
        },

    }


@pytest.yield_fixture()
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture()
def source_msku_contex():
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
        for blue_offer in BLUE_OFFERS
    ]


@pytest.yield_fixture()
def source_yt_tables(
        yt_server,
        or3_config,
        source_msku_contex,
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=WHITE_OFFERS
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
            data=BLUE_OFFERS
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


@pytest.yield_fixture()
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
            shards=COUNT_WHITE_SHARDS,
            half_mode=False,
            blue_shards=COUNT_BLUE_SHARDS,
            **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_has_blue_offers(main_idx, add_blue_offers_to_white_shards_value):
    '''
    При general.add_blue_offers_to_white_shards=true (это значение по умолчанию)
    в белый шард не должны попадать синие оффера.
    '''

    if add_blue_offers_to_white_shards_value:
        assert_that(
            main_idx.outputs['offers_by_offer_id'],
            all_of(
                has_length(2),
                has_key(BLUE_OFFER_ID),
                has_key(WHITE_OFFER_ID),
            )
        )

    else:
        assert_that(
            main_idx.outputs['offers_by_offer_id'],
            all_of(
                has_length(1),
                is_not(has_key(BLUE_OFFER_ID)),
                has_key(WHITE_OFFER_ID),
            )
        )
