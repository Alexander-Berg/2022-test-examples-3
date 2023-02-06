# coding=utf-8
"""
Проверяем что офферы из таблицы Директа не привышают установленный лимит
"""

import pytest
import time
from datetime import datetime
from hamcrest import assert_that, has_entries, equal_to, all_of

from market.idx.pylibrary.offer_flags.flags import OfferFlags

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
from market.idx.generation.yatf.utils.fixtures import make_offer_proto_str

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

from yt.wrapper import ypath_join


BUSINESS_ID_DIRECT = 1000
SHOP_DIRECT = 2000
SHOP_DIRECT1 = 2001
FEED_DIRECT = 3000
OFFER_DIRECT = 'direct_offer_1'
OFFER_LIMIT = 2
MIN_OFFERS_FOR_SHOP = 100

GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
SESSION_ID = int(time.time())


@pytest.fixture(scope='module')
def or3_config_data(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir
        },
        'direct_datacamp': {
            'indexation_enabled': 'true',
            'united_offers_tablepath': ypath_join(home_dir, 'datacamp/united/direct_out/recent'),
            'partners_path': ypath_join(home_dir, 'datacamp/direct/partners'),
            'max_direct_offers': OFFER_LIMIT,
        },
        'feeds': {
            'status_set': "'mock', 'publish'",
        },
    }
    return config


@pytest.fixture(scope='module')
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.fixture(scope='module')
def source_offers_raw():
    offers = []
    for i in range(1, 110, 1):
        offers.append({
            'feed_id': FEED_DIRECT,
            'offer_id': str(i),
            'session_id': SESSION_ID,
            'offer': make_offer_proto_str(
                URL='https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik',
                classifier_good_id='81a25999cc4f1d161a6ff97582a693bd',
                classifier_magic_id2='d90928147985beb16facf6f00a617a9c',
                description='Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик.',
                feed_id=FEED_DIRECT,
                offer_flags64=str(OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.CPC | OfferFlags.IS_DIRECT),
                offer_flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.CPC,
                picURLS='https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik/1.jpg',
                price=20001,
                supplier_feed_id=FEED_DIRECT,
                title='Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте',
                ware_md5='x1LWMo-JhRjsqcDqMEB-aw',
                yx_ds_id=SHOP_DIRECT,
                yx_shop_offer_id=OFFER_DIRECT,
            ),
        })

    offers.append({
        'feed_id': FEED_DIRECT,
        'offer_id': OFFER_DIRECT,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            URL='https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik',
            classifier_good_id='81a25999cc4f1d161a6ff97582a693bd',
            classifier_magic_id2='d90928147985beb16facf6f00a617a9c',
            description='Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик.',
            feed_id=FEED_DIRECT,
            offer_flags64=str(OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.CPC | OfferFlags.IS_DIRECT),
            offer_flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.CPC,
            picURLS='https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik/1.jpg',
            price=20001,
            supplier_feed_id=FEED_DIRECT,
            title='Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте',
            ware_md5='x1LWMo-JhRjsqcDqMEB-aw',
            yx_ds_id=SHOP_DIRECT1,
            yx_shop_offer_id=OFFER_DIRECT,
        ),
    })
    return offers


@pytest.fixture(scope='module')
def source_yt_tables(yt_server, or3_config, source_offers_raw):
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
            data=[]
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=[],
        ),
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data=[]
        ),
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
    }


@pytest.yield_fixture(scope='module')
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name='Все товары',
            name='Все товары',
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([])


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

    resources = {
        'config': or3_config,
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree),
    }

    with Or3MainIdxTestEnv(yt_server, GENERATION, MI3_TYPE, 1, False, **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_direct_limit(main_idx):
    """Лимит оферов в main_idx сильно меньше чем в фиде, мы оставляем всегда минимум по 100 на магазин.
       В тестовых данных два shop_id, один больше 100, его должны обрезать до 100 и один с 1 офером, его должны оставить.
    """
    offers = main_idx.outputs['offers_by_offer_id']
    assert_that(len(offers), equal_to(MIN_OFFERS_FOR_SHOP + 1))
    assert_that(
        offers,
        has_entries({
            OFFER_DIRECT: all_of(
                has_entries({
                    'offer': all_of(
                            has_entries({
                            'yx_ds_id': SHOP_DIRECT1,
                        }),
                    )
                })
            )
        })
    )
    assert_that(
        offers,
        has_entries({
            OFFER_DIRECT: all_of(
                has_entries({
                    'offer': all_of(
                            has_entries({
                            'yx_ds_id': SHOP_DIRECT1,
                        }),
                    )
                })
            )
        })
    )
