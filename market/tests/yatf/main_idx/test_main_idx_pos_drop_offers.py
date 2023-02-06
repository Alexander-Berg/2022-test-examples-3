# coding: utf-8


import pytest
import datetime
import itertools

from hamcrest import (
    assert_that,
    equal_to,
    has_items,
    is_not,
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
    PoSTable
)
from market.idx.generation.yatf.resources.prepare.blue_promo_table import BluePromoDetailsTable
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    make_offer_proto_str,
    make_uc_proto_str,
    make_price_expression_str,
    make_offer_promo_str,
)
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer as OfferPb

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
    scope="module",
    params=[
        (3, 12, 6, 0, 0, False),
        (12, 12, 6, 2, 1, False),
        (100, 18, 0, 0, 1, False),
        (100, 12, 6, 6, 1, False),
        (100, 18, 0, 6, 0, False),
        (100, 18, 0, 1000, 0, False),
        (12, 12, 6, 2, 1, True),
        (100, 12, 6, 6, 1, True),
    ],  # (16, 16, 2) тест не проможеточное значение мигает из-за недетерменированной работы семлпирования на малых таблицах
    ids=[
        "M3",      # лимит в максимум 3 офера, проверяем что выкинем не больше PoS и останется 12
        "M12",     # лимит в максимум 12 оферов, проверямем что останется ровно 12
        "M100",    # лимит в 100 оферов, поэтому остануться все 18
        "M100-1",  # лимит в 100 оферов, но принудительно выкинем 6
        "M100-2",  # лимит в 100 оферов, хотим выкинуть 6, но не больше 0% от pos, т.о оставим все
        "M100-3",  # лимит в 100 оферов, хотим выкинть 1000 но не больше 0% от pos - оставляем все
        "MR12",    # лимит в 12, включаена рандомная обрезалка в конце, убеждаемся, что она ничего не вырежет
        "MR100",   # Лимит в 100, включена рандомная обреразлка и хотим выкинуть 6 из pos
    ]
)
def max_expected_offers_count(request):
    '''
    (
     максимальное количество,
     ожидаемое количество,
     количество папавшее в таблицу откинутых,
     сколько хотим принудительно выкинуть,
     максимальный процент от pos который хотим выкинуть
     использовать ли рандомное обрезание в конце
    )
    '''
    return request.param


@pytest.fixture(scope="module")
def or3_config_data(yt_server, max_expected_offers_count):
    max_offers_count, _, _, drop_down, share, use_random = max_expected_offers_count
    home_dir = get_yt_prefix()
    return {
        'general': {
            'max_num_offers': max_offers_count,
            'main_idx_use_drop_down': 'true',
        },
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': 'collected_promo_details',
        },
        'misc': {
            'blue_offers_enabled': 'true',
            'filter_webmaster_offers': 'false',
            'use_promo_table': 'true',
            'main_idx_use_new_pipeline': 'true',
        },
        'dropdown': {
            'yt_pos_table': ypath_join(home_dir, 'in', 'pos', 'recent'),
            'use_pos': 'true',
            'drop_down_from_pos': drop_down,
            'max_share_of_pos': share,
            'enable_random': str(use_random)
        }

    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


def make_default_uc_proto_str():
    data = {
        'model_id': 125,
    }
    return make_uc_proto_str(**data)


def create_offer(offer_id, price, feed_id=2000, session_id=30, red_status=1, from_webmaster=False):
    return {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': session_id,
        'offer': make_offer_proto_str(
            price=price*10000000.0,
            red_status=red_status,
            from_webmaster=from_webmaster
        ),
        'uc': make_default_uc_proto_str(),
    }


def create_promo(offer_id, price, feed_id=2000):
    return {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'promo': make_offer_promo_str(
            offer_id=offer_id,
            price=price,
            currency='RUR'
        )
    }


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        create_offer(offer_id='offer_1', price=10),
        create_offer(offer_id='offer_2', price=100),
        create_offer(offer_id='offer_3', price=200),
        create_offer(offer_id='offer_4', price=250),
        create_offer(offer_id='offer_5', price=300),
        create_offer(offer_id='offer_6', price=500),
        create_offer(offer_id='offer_7', price=5000),
        create_offer(offer_id='offer_8', price=50000),
        create_offer(offer_id='offer_9', price=500000),
        create_offer(offer_id='offer_10', price=600000),
        create_offer(offer_id='red_offer_2', price=10000, red_status=4),
        create_offer(offer_id='red_offer_1', price=100, red_status=4),
        create_offer(offer_id='webmaster_offer_1', price=100, from_webmaster=True),
    ]


@pytest.yield_fixture(scope="module")
def promo_offers_raw():
    return [
        create_promo(offer_id='offer_1', price=10),
        create_promo(offer_id='offer_2', price=100),
        create_promo(offer_id='offer_6', price=500),
        create_promo(offer_id='offer_7', price=5000),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    def create_blue_offer(msku, feed_id, offer_id, price, session_id=30):
        return {
            'msku': msku,
            'feed_id': feed_id,
            'offer_id': offer_id,
            'session_id': session_id,
            'offer': make_offer_proto_str(
                price=price,
                is_blue_offer=True
            ),
            'uc': make_default_uc_proto_str(),
        }

    return [
        create_blue_offer(111, 3000, 'blue_offer_1', 100),
        create_blue_offer(444, 3003, 'blue_offer_2', 301),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    def create_msku(msku, feed_id, offed_id):
        return {
            'msku': msku,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': feed_id,
            'offer_id': offed_id,
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_default_uc_proto_str(),
        }

    return [
        create_msku(111, 99999, 'MS111'),
        create_msku(444, 99999, 'MS444'),
        create_msku(129, 99999, 'MS555'),  # no blue offers
    ]


@pytest.yield_fixture(scope="module")
def source_api_snapshot():
    return [
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
def source_pos_table():
    return [
        {
            'feed_id': 2000,
            'offer_id': 'offer_3'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_7'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_9'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_333333333'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_6'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_1'
        },
        {
            'feed_id': 2001,
            'offer_id': 'offer_1'
        },
        {
            'feed_id': 2000,
            'offer_id': 'offer_4'
        },

    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(
    yt_server,
    or3_config,
    source_offers_raw,
    source_blue_offers_raw,
    source_msku_contex,
    source_api_snapshot,
    source_pos_table
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
            data=[]  # promo_offers_raw
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
        'pos_table': PoSTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'pos', 'recent'),
            data=source_pos_table
        ),
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'collected_promo_details', 'recent'),
            data=[],
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


@pytest.yield_fixture(scope="module")
def mi3_dir(main_idx, yt_server):
    yt = yt_server.get_yt_client()
    home_dir = main_idx.resources['config'].options['yt']['home_dir']
    content = yt.list(home_dir)
    assert content and 'mi3' in content
    return ypath_join(home_dir, 'mi3')


@pytest.yield_fixture(scope="module")
def main_dir(mi3_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(mi3_dir)
    assert content and 'main' in content
    return ypath_join(mi3_dir, 'main')


@pytest.yield_fixture(scope="module")
def generation_dir(main_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(main_dir)
    assert content and len(content) == 2  # generation + recent link
    return ypath_join(main_dir, content[0])


@pytest.yield_fixture(scope="module")
def offers_dir(generation_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(generation_dir)
    assert content and 'offers' in content
    return ypath_join(generation_dir, 'offers')


@pytest.yield_fixture(scope="module")
def blue_offers_tables(generation_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(generation_dir)
    assert content and 'blue_offers_shards' in content
    return ypath_join(generation_dir, 'blue_offers_shards')


@pytest.yield_fixture(scope="module")
def dropped_offers_table(generation_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(generation_dir)
    assert content and 'dropped_offers' in content
    return ypath_join(generation_dir, 'dropped_offers')


@pytest.yield_fixture(scope="module")
def offers(offers_dir, yt_server):
    yt = yt_server.get_yt_client()
    content = yt.list(offers_dir)
    assert content and len(content) == COUNT_SHARDS
    return list(
        itertools.chain.from_iterable(
            [
                yt.read_table(ypath_join(offers_dir, shard_dir))
                for shard_dir in content
            ]
        )
    )


@pytest.yield_fixture(scope="module")
def blue_offers(blue_offers_tables, yt_server):
    yt = yt_server.get_yt_client()
    result = []
    for table in yt.list(blue_offers_tables):
        result.append(list(yt.read_table(table)))
    return result


@pytest.yield_fixture(scope="module")
def dropped_offers(dropped_offers_table, yt_server):
    yt = yt_server.get_yt_client()
    return list(yt.read_table(dropped_offers_table))


@pytest.yield_fixture(scope="module")
def result_offers(offers):
    def result(offer):
        offer_pb = OfferPb()
        offer_pb.ParseFromString(offer['offer'])

        return {'offer_id': offer['offer_id'], 'offer': offer_pb}

    return [result(offer) for offer in offers]


def test_offres_count(main_idx, max_expected_offers_count):
    '''
    Проверяем, что в оферах оказжутся все оферы с ценой больше 300, все красные и синие,
     а так же все остальные если попадут в лимит
    '''
    offers = main_idx.outputs['offers']

    _, expected, _, _, _, _ = max_expected_offers_count
    assert_that(len(offers), equal_to(expected))


def test_blue_offers_count(main_idx, source_blue_offers_raw, source_msku_contex):
    '''
    Проверям, что синяя таблица состоит из всех синих и Msku внезависимости от фильтрации по цене
    '''
    blue_offers = main_idx.outputs['blue_offers']

    assert_that(len(blue_offers), equal_to(5))  # blue + msku


def test_for_rotation_offers_positive(main_idx, source_pos_table):
    offers = main_idx.outputs['offers_by_offer_id']

    all_ratation_offer_id = set(offer_id for offer_id in offers if offers[offer_id]['offer'].get('for_rotation'))
    all_pos_table_offer_id = set(o['offer_id'] for o in source_pos_table)

    offer_matchers = [equal_to(id) for id in all_ratation_offer_id]
    assert_that(all_pos_table_offer_id, has_items(*offer_matchers))


def test_for_rotation_offers_negative(main_idx, source_pos_table):
    offers = main_idx.outputs['offers_by_offer_id']

    all_not_rotation_offer_id = set(offer_id for offer_id in offers if not offers[offer_id]['offer'].get('for_rotation'))
    all_pos_table_offer_id = set(o['offer_id'] for o in source_pos_table)

    offer_matchers = [equal_to(id) for id in all_not_rotation_offer_id]
    assert_that(all_pos_table_offer_id, is_not(has_items(*offer_matchers)))


def test_dropped_offers_count(main_idx, max_expected_offers_count):
    '''
    Проверяем размер таблицы выкинутых оферов
    '''
    dropped_offers = main_idx.outputs['dropped_offers']

    _, _, expected, _, _, _ = max_expected_offers_count
    assert_that(len(dropped_offers), equal_to(expected))


def test_droped_offers_table_schema(dropped_offers_table, offers_dir, yt_server):
    '''
    Проверяем что схема таблицы с выкинутыми оферами совпадает содержит proto схему
    '''
    yt = yt_server.get_yt_client()

    assert_that(yt.list(dropped_offers_table + "/@"), has_items(equal_to("_yql_proto_field_offer")))
