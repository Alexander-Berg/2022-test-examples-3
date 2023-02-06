# coding: utf-8

import pytest
import datetime

from collections import namedtuple

from hamcrest import (
    assert_that,
    equal_to,
    has_key,
)

from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
    ApiSnapshotTable,
    Offers2ModelTable,
    Offers2ParamTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.common import create_ware_md5
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
    make_price_expression_str,
)

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = True


PipelineParams = namedtuple('PipelineParams', [
    'skip_has_gone',
    'white_offer_count',
    'blue_offer_count',
])


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


@pytest.fixture(
    scope='module',
    params=[
        PipelineParams(
            skip_has_gone=False,
            white_offer_count=16,
            blue_offer_count=12
        ),
       PipelineParams(
            skip_has_gone=True,
            white_offer_count=17,
            blue_offer_count=12
        ),
    ],
    ids=[
        'White',
        'WhiteSkipHasGone'
    ]
)
def pipeline_params(request):
    '''
    Запуск main-idx для белого и синего пайплайна
    Второй параметр - значение аргумента skip-has-gone-offers
    Третье и четвертое числа - количество оферов в белых и синем шардах
    Для Синего пайплайна белых шардов не должно появляться, поэтому -1
    '''
    return request.param


@pytest.fixture(scope="module")
def or3_config_data(yt_server, pipeline_params):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'misc': {
            'blue_offers_enabled': 'true',
        },
        'general': {
            'skip_has_gone_in_half_mode': pipeline_params.skip_has_gone,
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([
        # API
        {"datafeed_id": 2000},
        {"datafeed_id": 2002, "is_mock": "true"},
    ])


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    '''
    Списко оферов с указанными ware_md5. По-умолчанию при запуске в half mode должны отбрасываться белые оферы
    с значением miltiplier > 0.5
    Красные оферы и оферы из фида с флагом is_mock не должны отбрасываться
    '''
    return [
        {
            'feed_id': 2000,
            'offer_id': 'offer_1',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20001,
                ware_md5=create_ware_md5(0.1)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_2',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20002,
                ware_md5=create_ware_md5(0.3)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_3',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.5)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_4',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.7)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_5',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.8)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_6',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.85)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'feed_id': 2000,
            'offer_id': 'offer_7',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.9)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'feed_id': 2000,
            'offer_id': 'offer_8',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.95),
                has_gone=True
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        # у данного фида стоит is_mock - офер не должен быть отфильрован
        {
            'feed_id': 2002,
            'offer_id': 'mock_offer',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=20003,
                ware_md5=create_ware_md5(0.99)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        {
            'msku': 111,
            'feed_id': 3000,
            'offer_id': 'blue_offer_1',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30004,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.1)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 222,
            'feed_id': 3000,
            'offer_id': 'blue_offer_2',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30005,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.2)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 333,
            'feed_id': 3001,
            'offer_id': 'blue_offer_3',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30016,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.3)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 333,
            'feed_id': 3002,
            'offer_id': 'blue_offer_4',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30027,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.4)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 444,
            'feed_id': 3003,
            'offer_id': 'blue_offer_5',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30038,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.51)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 444,
            'feed_id': 3003,
            'offer_id': 'blue_offer_6',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30039,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.6)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },

        {
            'msku': 444,
            'feed_id': 3003,
            'offer_id': 'blue_offer_7',
            'session_id': 30,
            'offer': make_offer_proto_str(
                price=30039,
                is_blue_offer=True,
                ware_md5=create_ware_md5(0.7),
                has_gone=True
            ),
            'uc': make_uc_proto_str(model_id=125),
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
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.1)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'msku': 222,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS222',
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.2)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'msku': 333,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS333',
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.3)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'msku': 444,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS444',
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.4)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
        {
            'msku': 129,  # no blue offers
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': 99999,
            'offer_id': 'MS555',
            'offer': make_offer_proto_str(
                is_fake_msku_offer=True,
                ware_md5=create_ware_md5(0.51)
            ),
            'uc': make_uc_proto_str(model_id=125),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_api_snapshot():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'blue_offer_1',
            'price': make_price_expression_str(price=2000299),
        },

        {
            'feed_id': 2000,
            'offer_id': 'blue_offer_2',
            'price': make_price_expression_str(price=2000399),
            'oldprice': make_price_expression_str(price=20003909),
            'vat': 3,
            'offer_deleted': True,
        },

        {
            'feed_id': 2000,
            'offer_id': 'nonExistingApiWhiteOffer',
            'price': make_price_expression_str(price=2000499),
        },

        {
            'feed_id': 2000,
            'msku': 20001,  # no offers of this msku
            'price': make_price_expression_str(price=123),
        },

        {
            'feed_id': 3000,
            'msku': 222,
            'price': make_price_expression_str(price=3000599),
        },

        {
            'feed_id': 3001,
            'msku': 333,
            'price': make_price_expression_str(price=3001699),
        },

        {
            'feed_id': 3002,
            'msku': 333,
            'price': make_price_expression_str(price=3002799),
        },

        {
            'feed_id': 3003,
            'msku': 444,
            'price': make_price_expression_str(price=3003899),
        },
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config,
                     source_offers_raw,
                     source_blue_offers_raw,
                     source_msku_contex,
                     source_api_snapshot,
                     pipeline_params):

    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
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
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data=source_api_snapshot
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, pipeline_params, tovar_tree):
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
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, HALF_MODE, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offres_count(main_idx, pipeline_params):
    '''
    HalfMode не должен влиять на белые фиды
    '''
    offers = main_idx.outputs['offers_by_offer_id']

    assert_that(
        len(offers),
        equal_to(pipeline_params.white_offer_count)
    )


def test_blue_offers_count(main_idx, pipeline_params):
    '''
    В режиме halfMode выкидываем фиды по ware_md5
    '''
    blue_offers = main_idx.outputs['blue_offers']

    assert_that(
        len(blue_offers),
        equal_to(pipeline_params.blue_offer_count)
    )


def test_blue_offers(main_idx, source_blue_offers_raw, source_msku_contex):
    '''
    Проверяем что все синие и msku пападают в половинчаный белый
    '''

    offers = main_idx.outputs['offers_by_offer_id']

    for offer in source_blue_offers_raw + source_msku_contex:
        assert_that(offers, has_key(offer["offer_id"]))
