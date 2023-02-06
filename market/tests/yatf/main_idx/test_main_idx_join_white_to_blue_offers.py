# coding: utf-8
import pytest
import datetime

from hamcrest import (
    assert_that,
    equal_to,
    has_items,
    has_entries
)

from yt.wrapper import ypath_join
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    BlueOffersRawTable,
    OffersRawTable,
    WhiteShopToBlueSupplierTable,
    Offers2ModelTable,
    Offers2ParamTable,
    ApiSnapshotTable,
    ModelsTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable

from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
)

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.proto.feedparser.OffersData_pb2 import ContexInfo


DATE = datetime.datetime.now().strftime('%Y-%m-%d')
GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False
SESSION_ID = 30


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
            'join_white_to_blue_offers': 'true'
        },
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


def create_offer(market_sku, yx_ds_id, feed_id, offer_id, offer_flags=None, url=None, ware_md5=None):
    return {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            yx_ds_id=yx_ds_id,
            feed_id=feed_id,
            offer_id=offer_id,
            offer_flags=offer_flags,
            url=url,
            ware_md5=ware_md5,
            market_sku=market_sku if market_sku >=0 else 0,
        ),
        'uc': make_uc_proto_str(market_sku_id=market_sku),
    }


def create_blue_offer(market_sku, supplier_id, feed_id, offer_id, ware_md5, contex_info=None):
    return {
        'msku': market_sku,
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            supplier_id=supplier_id,
            ware_md5=ware_md5,
            feed_id=feed_id,
            offer_id=offer_id,
            is_fake_msku_offer=False,
            contex_info=contex_info),
        'uc': make_uc_proto_str(),
    }


def create_msku_contex(market_sku, feed_id, offer_id, ware_md5, url=None):
    return {
        'msku': market_sku,
        'msku_exp': 0,
        'msku_experiment_id': '',
        'experimental_model_id': 0,
        'feed_id': feed_id,
        'offer_id': offer_id,
        'offer': make_offer_proto_str(
            ware_md5=ware_md5,
            feed_id=feed_id,
            offer_id=offer_id,
            url=url,
            offer_flags=OfferFlags.MARKET_SKU.value,
            market_sku=market_sku if market_sku >=0 else 0,
            is_fake_msku_offer=True),
        'uc': make_uc_proto_str()
    }


def create_shop_to_supplier(shop_id, supplier_id):
    return {
        'datasource_id': shop_id,
        'supplier_id': supplier_id,
        'can_stop_blue_price': 1,
        'date': DATE
    }


def create_link(feed_id, offer_id, blue_md5):
    return {'feed_id': feed_id, 'offer_id': offer_id, 'blue_md5': blue_md5}


@pytest.yield_fixture(scope="module")
def source_white_shop_to_blue_sullpier_raw():
    return [
        create_shop_to_supplier(shop_id=101, supplier_id=101000),
        create_shop_to_supplier(shop_id=102, supplier_id=102000),
        create_shop_to_supplier(shop_id=103, supplier_id=103000),

        # магазин имеет 2 клона на белом и 2 клона на синем - все они связаны
        create_shop_to_supplier(shop_id=20101, supplier_id=201001),
        create_shop_to_supplier(shop_id=20101, supplier_id=201002),
        create_shop_to_supplier(shop_id=20102, supplier_id=201001),
        create_shop_to_supplier(shop_id=20102, supplier_id=201002),
    ]


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        # два оффера из одного магазина могут поматчиться на один синий оффер, т.к. у них распознался один market_sku
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='shop101_white1_v1'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='shop101_white1_v2'),

        # duplicate offers to test MARKETINDEXER-36748
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='shop101_white1_v1'),
        create_offer(market_sku=1, yx_ds_id=101, feed_id=1111, offer_id='shop101_white1_v2'),

        # из того же магазина оффер с другим market_sku
        create_offer(market_sku=2, yx_ds_id=101, feed_id=1111, offer_id='shop101_white2'),
        # из другого магазина оффер с тем же market_sku
        create_offer(market_sku=2, yx_ds_id=102, feed_id=2222, offer_id='shop102_white2'),

        # оффер у которого есть market_sku но на синем нет доступных офферов с тем же market_sku от этого магазина
        create_offer(market_sku=3, yx_ds_id=103, feed_id=3333, offer_id='shop103_white3'),
        # оффер у которого есть market_sku но на синем только "фейковый" синий оффер
        create_offer(market_sku=4, yx_ds_id=103, feed_id=3333, offer_id='shop103_white4'),

        # оффер у которого нет market_sku
        create_offer(market_sku=None, yx_ds_id=103, feed_id=3333, offer_id='shop103_white_none'),
        # оффер у которого невалидный market_sku
        create_offer(market_sku=-1, yx_ds_id=103, feed_id=3333, offer_id='shop103_white_invalid'),
        # оффер у которого нулевой market_sku
        create_offer(market_sku=0, yx_ds_id=103, feed_id=3333, offer_id='shop103_white_zero'),

        # магазин не размещающийся на синем
        create_offer(market_sku=5, yx_ds_id=104, feed_id=4444, offer_id='shop104_white5'),
        create_offer(market_sku=5, yx_ds_id=104, feed_id=4444, offer_id='shop104_white5'),

        # офферы из региональных клонов
        create_offer(market_sku=201, yx_ds_id=20101, feed_id=20101111, offer_id='shop20101_white201'),
        create_offer(market_sku=202, yx_ds_id=20102, feed_id=20102222, offer_id='shop20102_white202'),
        create_offer(market_sku=203, yx_ds_id=20101, feed_id=20101111, offer_id='shop20101_white203'),
        create_offer(market_sku=204, yx_ds_id=20102, feed_id=20102222, offer_id='shop20102_white204'),
        create_offer(market_sku=205, yx_ds_id=20103, feed_id=20102223, offer_id='shop20102_white205',
                     offer_flags=OfferFlags.BLUE_OFFER.value, url='www.beru.ru/product/205', ware_md5='finallyThisTestWorks11'),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        # офферы магазинов 101 и 102 которые также размещаются на синем
        create_blue_offer(market_sku=1, supplier_id=101000,
                          feed_id=8111, offer_id='sup101_blue1', ware_md5='wxdbP0Y7RDCTk1EnsixTfA'),
        create_blue_offer(market_sku=2, supplier_id=101000,
                          feed_id=8111, offer_id='sup101_blue2', ware_md5='d57B1qljye1EZfYoff9ZSg'),
        create_blue_offer(market_sku=1, supplier_id=102000,
                          feed_id=8222, offer_id='sup102_blue1', ware_md5='T9OfnvJgFfiVN8JhjSPhBA'),
        create_blue_offer(market_sku=2, supplier_id=102000,
                          contex_info=ContexInfo(experiment_id='exp', experimental_msku_id=1000),
                          feed_id=8222, offer_id='sup102_blue2', ware_md5='s0kaGJ1RPfWpw4fnaIL1rA'),

        create_blue_offer(market_sku=2, supplier_id=102000,
                          contex_info=ContexInfo(experiment_id='exp', original_msku_id=2, is_experimental=True),
                          feed_id=8222, offer_id='sup102_blue2', ware_md5='s0kaGJ1RPfWpw4fnaIL1rA'),


        # на синем нет офферов с market_sku=3 и есть только фейковый оффер с market_sku=4
        # есть оффер с market_sku=5 но на белом нет подходящего оффера от этого же магазина
        create_blue_offer(market_sku=5, supplier_id=103000,
                          feed_id=8333, offer_id='sup103_blue5', ware_md5='wY_ieUCwZveu6yvZJJgPLw'),
        # есть оффер с market_sku=5 но магазин не присутствует на белом маркете
        create_blue_offer(market_sku=5, supplier_id=444000,
                          feed_id=8444, offer_id='sup444_blue5', ware_md5='7Iu4vKBUHXT1OBhMlj_Xmg'),

        # офферы из региональных клонов
        create_blue_offer(market_sku=201, supplier_id=201001,
                          feed_id=8201, offer_id='sup201001_blue201', ware_md5='ldXjBt11n1o_201_201001'),
        create_blue_offer(market_sku=202, supplier_id=201001,
                          feed_id=8201, offer_id='sup201001_blue202', ware_md5='ldXjBt11n1o_202_201001'),
        create_blue_offer(market_sku=203, supplier_id=201002,
                          feed_id=8202, offer_id='sup201002_blue203', ware_md5='ldXjBt11n1o_203_201002'),
        create_blue_offer(market_sku=204, supplier_id=201002,
                          feed_id=8202, offer_id='sup201002_blue204', ware_md5='ldXjBt11n1o_204_201002')

    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        create_msku_contex(market_sku=1, feed_id=9999, offer_id='fake_blue1', ware_md5='kqM5fPVfwmaD7SlmlH39Zw',  url='www.beru.ru/product/1'),
        create_msku_contex(market_sku=2, feed_id=9999, offer_id='fake_blue2', ware_md5='EuIk8Esqusmj7D3Ugqu0xw'),
        # на синем нет market_sku=3
        create_msku_contex(market_sku=4, feed_id=9999, offer_id='fake_blue4', ware_md5='6AD9pYqSUDNjq_HI27VpCQ'),
        create_msku_contex(market_sku=5, feed_id=9999, offer_id='fake_blue5', ware_md5='_d49OxgdD9fJHrQOmBIC1w'),

        # офферы из региональных клонов
        create_msku_contex(market_sku=201, feed_id=9999, offer_id='fake_blue201', ware_md5='ldXjBt11n1o68PUiCJt201'),
        create_msku_contex(market_sku=202, feed_id=9999, offer_id='fake_blue202', ware_md5='ldXjBt11n1o68PUiCJt202'),
        create_msku_contex(market_sku=203, feed_id=9999, offer_id='fake_blue203', ware_md5='ldXjBt11n1o68PUiCJt203'),
        create_msku_contex(market_sku=204, feed_id=9999, offer_id='fake_blue204', ware_md5='ldXjBt11n1o68PUiCJt204'),
    ]


@pytest.fixture(scope="module")
def expected_links():
    return [
        # два оффера от одного магазина 101 и одним и тем же market_sku=1 поматчились на один и тот же синий оффер поставщика 101000
        create_link(feed_id=1111, offer_id='shop101_white1_v1', blue_md5='wxdbP0Y7RDCTk1EnsixTfA'),
        create_link(feed_id=1111, offer_id='shop101_white1_v2', blue_md5='wxdbP0Y7RDCTk1EnsixTfA'),
        # оффер от магазина 101 и market_sku=2 поматчился на синий оффер от поставщика 101000
        create_link(feed_id=1111, offer_id='shop101_white2', blue_md5='d57B1qljye1EZfYoff9ZSg'),
        # оффер от магазина 102 и market_sku=2 поматчился на синий оффер от поставщика 102000
        create_link(feed_id=2222, offer_id='shop102_white2', blue_md5='s0kaGJ1RPfWpw4fnaIL1rA'),

        # ссылок на market_sku=3,4,5 не присуствует, т.к. не выполняются условия при которых офферы могут быть поматчены
        # market_sku=3 - нет подходящих офферов на синем
        # market_sku=4 - есть только фейковый синий оффер
        # market_sku=5 - магазин 104 не размещается на синем маркете


        # любой клон белого может матчиться с любым клоном синего
        # (на самом деле связей может быть более одной - но в конечном итоге выберется один случайный оффер)
        create_link(feed_id=20101111, offer_id='shop20101_white201', blue_md5='ldXjBt11n1o_201_201001'),
        create_link(feed_id=20102222, offer_id='shop20102_white202', blue_md5='ldXjBt11n1o_202_201001'),
        create_link(feed_id=20101111, offer_id='shop20101_white203', blue_md5='ldXjBt11n1o_203_201002'),
        create_link(feed_id=20102222, offer_id='shop20102_white204', blue_md5='ldXjBt11n1o_204_201002'),
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(
        yt_server,
        or3_config,
        source_offers_raw,
        source_blue_offers_raw,
        source_white_shop_to_blue_sullpier_raw,
        source_msku_contex
):
    yt_home_path = or3_config.options['yt']['home_dir']
    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_offers_raw
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers_raw
        ),
        'shop_to_supplier': WhiteShopToBlueSupplierTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'white_blue_ds_mapping', 'latest'),
            data=source_white_shop_to_blue_sullpier_raw,
        ),
        # stubs
        'promos_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'promos_raw'),
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
            data=[]
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, tovar_tree):
    for table in source_yt_tables.values():
        table.create()
        path = table.get_path()
        assert_that(yt_server.get_yt_client().exists(path), "Table {} doesn\'t exist".format(path))

    yt_home_path = or3_config.options['yt']['home_dir']
    or3_config.options['yt'].update({
        'yt_mstat_white_blue_ds_mapping_table': source_yt_tables['shop_to_supplier'].table_path
    })

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
        mi.execute(generate_blue_urls_table=True)
        yield mi


def test_sharded_blue_urls_as_in_white_has_all_columns(main_idx):
    assert_that(
        main_idx.outputs['sharded_blue_urls_as_in_white'],
        has_items(
            has_entries({'ware_md5': 'finallyThisTestWorks11', 'url': 'beru.ru/product/205', 'path': '/product/205', 'msku': 205, 'is_fake_msku_offer': False}),
            has_entries({'ware_md5': 'kqM5fPVfwmaD7SlmlH39Zw', 'url': 'beru.ru/product/1', 'path': '/product/1', 'msku': 1, 'is_fake_msku_offer': True})
        ),
        'Has correct record in \'sharded_blue_urls_as_in_white\' table'
    )


def test_white_offers_count(main_idx):
    """
    see MARKETINDEXER-36748
    """

    offers = main_idx.outputs['offers']
    assert_that(
        len(offers),
        equal_to(33),
    )
