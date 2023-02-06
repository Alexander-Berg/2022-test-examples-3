# coding: utf-8
'''
Тест проверяет генерацию мета информации о картинки для оффера из еды.
Url картинки из еды не обрабатывается пикроботом, т.к. картинка уже в аватарнице.
Из url картинки мы можем достать group_id и imagename картинки. Остальные параметры
(размеры картинок и namespace) передаются через конфиг БОИ.

В тесте проверяется работа как с оффером из еды, так и с обычным оффером.
'''

import datetime
import pytest

from yt.wrapper import ypath_join

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    Offers2ParamTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.common import create_ware_md5
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
EDA_AVATARS_NAMESPACE = 'eda'
EDA_AVATARS_GROUP_ID = '1'
EDA_AVATARS_IMAGE_ID = 'a'
NUM_EDA_OFFERS = 2
EDA_OFFER_ID_1 = 'eda_offer_1'
EDA_OFFER_1 = {
    'feed_id': 1,
    'offer_id': EDA_OFFER_ID_1,
    'session_id': 1,
    'offer': make_offer_proto_str(
        price=1,
        ware_md5=create_ware_md5(0.1),
        picURLS='https://testing.eda.tst.yandex.net/get-eda/{}/{}/orig'.format(EDA_AVATARS_GROUP_ID, EDA_AVATARS_IMAGE_ID),
        offer_flags64=OfferFlags.IS_EDA
    ),
    'uc': make_uc_proto_str(model_id=1),
}

EDA_OFFER_ID_2 = 'eda_offer_2'
EDA_OFFER_2 = {
    'feed_id': 1,
    'offer_id': EDA_OFFER_ID_2,
    'session_id': 1,
    'offer': make_offer_proto_str(
        price=1,
        ware_md5=create_ware_md5(0.1),
        picURLS='https://avatars.mds.yandex.net/get-eda/{}/{}/orig'.format(EDA_AVATARS_GROUP_ID, EDA_AVATARS_IMAGE_ID),
        offer_flags64=OfferFlags.IS_EDA
    ),
    'uc': make_uc_proto_str(model_id=1),
}

SIMPLE_OFFER_ID = 'offer_2'
SIMPLE_OFFER = {
    'feed_id': 1,
    'offer_id': SIMPLE_OFFER_ID,
    'session_id': 1,
    'offer': make_offer_proto_str(
        price=2,
        ware_md5=create_ware_md5(0.2),
        picURLS='https://testing.eda.tst.yandex.net/get-eda/{}/{}/orig'.format(EDA_AVATARS_GROUP_ID, EDA_AVATARS_IMAGE_ID),
    ),
    'uc': make_uc_proto_str(model_id=1),
}


@pytest.fixture(
    scope='module',
    params=[True, False],
    ids=['EnableEdaPictures', 'DisableEdaPictures']
)
def picture_enable(request):
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


@pytest.fixture(scope="module")
def or3_config_data(picture_enable):
    home_dir = get_yt_prefix()
    return {
        'yt': {
            'home_dir': home_dir,
        },
        'eda': {
            'indexation_enabled': 'true',
            'picture_enable': picture_enable,
            'picture_orig_width': 1600,
            'picture_orig_height': 1200,
            'picture_thumb_mask': 4611686018427430976,
            'avatars_namespace': EDA_AVATARS_NAMESPACE,
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([{"datafeed_id": 1}])


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=[EDA_OFFER_1, EDA_OFFER_2, SIMPLE_OFFER]
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path)

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

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, COUNT_SHARDS, False, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


@pytest.fixture(scope="module")
def main_idx_eda_pictures(picture_enable, main_idx):
    '''
    Фикстура, которая достаёт картинки оффера из еды, если она там генерируется.
    Включение/выключение генерации мета информации для картинки еды происходит
    через флажок в конфиге БОИ
    '''
    if not picture_enable:
        pytest.skip('Skip if eda images convertor is disable', allow_module_level=True)
    r = []
    r += main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_1]['pic']
    r += main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_2]['pic']
    return r


@pytest.yield_fixture(scope="module")
def main_idx_simple_offer(main_idx):
    '''
    Фикстура отдающая обычный оффер (не помеченный, как из еды)
    '''
    yield main_idx.outputs['offers_by_offer_id'][SIMPLE_OFFER_ID]


def test_offers_count(main_idx):
    '''
    Проверям, что обычный оффер и оффер из еды всегда появляются на выходе main-idx
    Вне зависимости от того включена генерация мета информации о картинке еды
    или нет
    '''
    assert len(main_idx.outputs['offers']) == 3


def test_eda_offer_enable_picture_convertor(picture_enable, main_idx):
    '''
    Проверяем, что мы генерируем мета информацию о картинке еды, если
    в конфиге взведён влажок eda.picture_enable
    '''
    if not picture_enable:
        pytest.skip('Skip if eda images convertor is disable', allow_module_level=True)
    assert 'pic' in main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_1]
    assert 'pic' in main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_2]


def test_eda_offer_disable_picture_convertor(picture_enable, main_idx):
    '''
    Проверяем, что мы НЕ генерируем мета информацию о картинке еды, если
    в конфиге НЕ взведён влажок eda.picture_enable
    '''
    if picture_enable:
        pytest.skip('Skip if eda images convertor is enable', allow_module_level=True)
    assert 'pic' not in main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_1]
    assert 'pic' not in main_idx.outputs['offers_by_offer_id'][EDA_OFFER_ID_2]


def test_eda_offer_picture_count(main_idx_eda_pictures):
    '''
    Должен быть один элемент в массиве мета информации на каждый оффер
    (доступные размеры тумбов учтены в thumb_mask)
    '''
    expected_count = 1
    assert len(main_idx_eda_pictures) == expected_count * NUM_EDA_OFFERS


def test_eda_offer_picture_namespace(main_idx_eda_pictures):
    '''
    Проверяем, что у всех картинок еды пофвился один и тот же
    namespace. Он передаётся через конфиг eda.avatars_namespace
    '''
    actual_namespaces = set([p['namespace'] for p in main_idx_eda_pictures])
    assert len(actual_namespaces) == 1
    assert EDA_AVATARS_NAMESPACE in actual_namespaces


def test_eda_offer_picture_group_id(main_idx_eda_pictures):
    '''
    Проверяем, что у всех картинок еды пофвился один и тот же
    group_id. Он берётся из url картинки еды. Обычно на один оффер
    еды приходится один url картинки. Поэтому в тесте проверяется, что
    он один и тот же на всю мета информацию
    '''
    actual_group_id = set([str(p['group_id'])for p in main_idx_eda_pictures])
    assert len(actual_group_id) == 1
    assert EDA_AVATARS_GROUP_ID in actual_group_id


def test_eda_offer_picture_image_id(main_idx_eda_pictures):
    '''
    Проверяем, что у всех картинок еды пофвился один и тот же
    imagename. Он берётся из url картинки еды. Обычно на один оффер
    еды приходится один url картинки. Поэтому в тесте проверяется, что
    он один и тот же на всю мета информацию
    '''
    actual_imagename = set([p['imagename'] for p in main_idx_eda_pictures])
    assert len(actual_imagename) == 1
    assert EDA_AVATARS_IMAGE_ID in actual_imagename


def test_simple_offer_enable_picture_convertor(main_idx_simple_offer):
    '''
    Проверяем, что у обычного оффера не сгенерилась мета информация о картинке.
    Она обычно мёржитсмя из таблиц пикробота, которые в данном тесте пустые.
    Главное, чтобы мы случайно не положили сгенерённую мета иформацию в обычный оффер.
    Её нужно генерить только для оффера из еды.
    '''
    assert 'pic' not in main_idx_simple_offer
