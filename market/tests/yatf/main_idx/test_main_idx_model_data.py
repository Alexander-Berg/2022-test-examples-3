# coding: utf-8


import pytest
import datetime

from hamcrest import (
    assert_that,
    equal_to,
    is_not,
    has_property,
    has_length,
    has_key,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    ApiSnapshotTable,
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    OffersRawTable,
    ModelsTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.offers.yatf.utils.fixtures import (
    make_proto_lenval_pictures,
    genererate_default_pictures
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)

from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)


GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False

FEED_ID = 2000
SESSION_ID = 30
PRICE = 1000

OFFER_1_MODEL_TITLE_OK = 'offer_1'
OFFER_2_MODEL_TITLE_OK = 'offer_2'
OFFER_3_MODEL_TITLE_OK_CLUSTER = 'offer_3'  # todo - нет тайтла у модели
OFFER_4_NO_MODEL = 'offer_4'
OFFER_5_NO_TITLE_IN_MODEL_TABLE = 'offer_5'
BLUE_OFFER_1_MODEL_TITLE_OK = 'blue_offer_1'
BLUE_OFFER_2_NO_OFFER2MODEL_LINK = 'blue_offer_2'
MSKU_1_NO_MODEL_TITLE = 'msku_1'
MSKU_2_NO_MODEL_TITLE = 'msku_2'

OFFER_6_MODEL_PIC_OK = 'offer_6'
OFFER_7_MODEL_HAS_NOT_PIC = 'offer_7'
OFFER_8_MODEL_PIC_NOT_MERGED_FROM_CLUSTER = 'offer_8'


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
            'blue_offers_enabled': 'true',
        },

    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


def make_default_uc_proto_str():
    data = {
        'model_id': 125,
    }
    return make_uc_proto_str(**data)


def create_offer(offer_id, model_id, feed_id=2000):
    return {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(price=PRICE),
        'uc': make_uc_proto_str(
            model_id=model_id,
        ),
    }


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        create_offer(offer_id=OFFER_1_MODEL_TITLE_OK, model_id=1),
        create_offer(offer_id=OFFER_2_MODEL_TITLE_OK, model_id=2),
        create_offer(offer_id=OFFER_3_MODEL_TITLE_OK_CLUSTER, model_id=3),
        create_offer(offer_id=OFFER_4_NO_MODEL, model_id=4),
        create_offer(offer_id=OFFER_5_NO_TITLE_IN_MODEL_TABLE, model_id=4),

        create_offer(offer_id=OFFER_6_MODEL_PIC_OK, model_id=6),
        create_offer(offer_id=OFFER_7_MODEL_HAS_NOT_PIC, model_id=7),
        create_offer(offer_id=OFFER_8_MODEL_PIC_NOT_MERGED_FROM_CLUSTER, model_id=8),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    def create_blue_offer(msku, feed_id, offer_id, model_id):
        return {
            'msku': msku,
            'feed_id': feed_id,
            'offer_id': offer_id,
            'session_id': SESSION_ID,
            'offer': make_offer_proto_str(
                price=PRICE,
                is_blue_offer=True
            ),
            'uc': make_uc_proto_str(model_id=model_id),
        }

    return [
        create_blue_offer(111, 3000, BLUE_OFFER_1_MODEL_TITLE_OK, 1),
        create_blue_offer(111, 3003, BLUE_OFFER_2_NO_OFFER2MODEL_LINK, 2),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    def create_msku(msku, feed_id, offer_id, model_id):
        return {
            'msku': msku,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': feed_id,
            'offer_id': offer_id,
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_uc_proto_str(model_id=model_id),
        }

    return [
        create_msku(111, 99999, MSKU_1_NO_MODEL_TITLE, 1),
        create_msku(129, 99999, MSKU_2_NO_MODEL_TITLE, 2),  # no blue offers
    ]


@pytest.yield_fixture(scope="module")
def source_models():
    def create_model(model_id, title, pic=None):
        return {
            'id': model_id,
            'pic': pic,
            'title': title,
        }

    return [
        create_model(1, 'model1'),
        create_model(2, 'model2'),
        create_model(3, 'model3'),
        create_model(5, None),

        create_model(6, 'model6', pic=make_proto_lenval_pictures(genererate_default_pictures())),
        create_model(7, 'model7'),
        create_model(8, 'model8', pic=make_proto_lenval_pictures(genererate_default_pictures())),
    ]


@pytest.yield_fixture(scope="module")
def source_offer2model():
    def create_offer2model(feed_id, offer_id, model_id, is_cluster=False):
        return {
            'feed_id': feed_id,
            'offer_id': offer_id,
            'model_id': model_id,
            'is_cluster': is_cluster,
        }

    return [
        create_offer2model(2000, OFFER_1_MODEL_TITLE_OK, 1),
        create_offer2model(2000, OFFER_2_MODEL_TITLE_OK, 2),
        create_offer2model(2000, OFFER_3_MODEL_TITLE_OK_CLUSTER, 3, is_cluster=True),
        create_offer2model(2000, OFFER_5_NO_TITLE_IN_MODEL_TABLE, 5),
        create_offer2model(3000, BLUE_OFFER_1_MODEL_TITLE_OK, 1),
        create_offer2model(99999, MSKU_1_NO_MODEL_TITLE, 1),
        create_offer2model(99999, 'msku_3', 3),

        create_offer2model(2000, OFFER_6_MODEL_PIC_OK, 6),
        create_offer2model(2000, OFFER_7_MODEL_HAS_NOT_PIC, 7),
        create_offer2model(2000, OFFER_8_MODEL_PIC_NOT_MERGED_FROM_CLUSTER, 8, is_cluster=True),
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_offers_raw,
    source_blue_offers_raw,
    source_msku_contex,
    source_models,
    source_offer2model,
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
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data=source_offer2model
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
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data={}
        ),
        'models': ModelsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'models', 'recent'),
            data=source_models
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
        mi.execute()
        yield mi


def test_offers_count(main_idx):
    '''
    Проверяем, что в оферах оказжутся все оферы с ценой больше 300, все красные и синие,
     а так же все остальные если попадут в лимит
    '''
    offers = main_idx.outputs['offers']
    assert_that(len(offers), equal_to(12))  # white + blue + msku


def test_blue_offers_count(main_idx):
    '''
    Проверям, что синяя таблица состоит из всех синих и Msku
    '''
    blue_offers = main_idx.outputs['blue_offers']

    assert_that(len(blue_offers), equal_to(4))  # blue + msku


@pytest.fixture(scope="module")
def expected_model_titles(or3_config):
    return {
        OFFER_1_MODEL_TITLE_OK: 'model1',
        OFFER_2_MODEL_TITLE_OK: 'model2',
        OFFER_3_MODEL_TITLE_OK_CLUSTER: 'model3',
        BLUE_OFFER_1_MODEL_TITLE_OK: 'model1',
    }


@pytest.mark.parametrize('test_case', [OFFER_1_MODEL_TITLE_OK,
                                       OFFER_2_MODEL_TITLE_OK,
                                       OFFER_3_MODEL_TITLE_OK_CLUSTER,
                                       BLUE_OFFER_1_MODEL_TITLE_OK,
                                       ])
def test_model_titles(main_idx, expected_model_titles, test_case):
    """
    Проверяем, корректность проставления модельного тайтла белым офферам
    """
    result_offers = main_idx.outputs['offers_by_offer_id']
    assert_that(result_offers[test_case]['offer']['model_title_ext'], equal_to(expected_model_titles[test_case]))


@pytest.mark.parametrize('test_case', [OFFER_4_NO_MODEL,
                                       OFFER_5_NO_TITLE_IN_MODEL_TABLE,
                                       BLUE_OFFER_2_NO_OFFER2MODEL_LINK,
                                       MSKU_1_NO_MODEL_TITLE,
                                       MSKU_2_NO_MODEL_TITLE,
                                       ])
def test_has_not_model_titles(main_idx, test_case):
    """
    Проверяем, случаи, когда модельный тайтл не проставляется
    """
    result_offers = main_idx.outputs['offers_by_offer_id']
    assert_that(result_offers[test_case]['offer'], is_not(has_property('model_title_ext')))


@pytest.mark.parametrize('test_case', [OFFER_6_MODEL_PIC_OK])
def test_has_model_pictures(main_idx, test_case):
    """
    Проверяем, случаи, когда модельная картинка примержилась к офферу, не имеющему картинки
    """
    result_offers = main_idx.outputs['offers_by_offer_id']
    assert_that(result_offers[test_case]['pic'], has_length(1))


@pytest.mark.parametrize('test_case', [OFFER_7_MODEL_HAS_NOT_PIC,
                                       OFFER_8_MODEL_PIC_NOT_MERGED_FROM_CLUSTER,
                                       ])
def test_has_not_model_pictures(main_idx, test_case):
    """
    Проверяем, случаи, когда модельная картинка не примержилась к офферу, тоже не имеющему картинки
    """
    result_offers = main_idx.outputs['offers_by_offer_id']
    assert_that(result_offers[test_case], is_not(has_key('pic')))
