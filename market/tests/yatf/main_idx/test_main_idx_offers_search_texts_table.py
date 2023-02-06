# coding: utf-8


import pytest
import datetime
import time


from hamcrest import (
    assert_that,
    equal_to,
    has_item,
    has_entries,
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
    CpaStatus,
    make_offer_proto_str,
    make_uc_proto_str,
    make_msku_contex_dict,
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

SHARDS = 1
BLUE_SHARDS = 1

WHITE_OFFER1 = 'white_offer1_hc1cVZiCl'
WHITE_OFFER2 = 'white_offer2_hc1cVZiCl'
BLUE_OFFER1 = 'blue_offer1_nllcxjhGX0'
BLUE_OFFER2 = 'blue_offer2_nllcxjhGX0'
DSBS_OFFER1 = 'dsbs_offer1_hc1cVZiCln'
DSBS_OFFER2 = 'dsbs_offer2_hc1cVZiCln'
MSKU1 = 'hc1cVZiClnllcxjhGX0_m1'
MSKU2 = 'hc1cVZiClnllcxjhGX0_m2'
MSKU3 = 'hc1cVZiClnllcxjhGX0_m3'

CATEGORY_ID = 200


def __get_title(ware_md5):
    return 'title_' + ware_md5


def __get_description(ware_md5):
    return 'description_' + ware_md5


def __get_supplier_description(ware_md5):
    return 'supplier_description_' + ware_md5


def __get_add_search_text(ware_md5):
    return 'add_search_text_' + ware_md5


def __get_book_authors(ware_md5):
    return 'book_authors_' + ware_md5


def __get_vendor_code(ware_md5):
    return 'vendor_code_' + ware_md5


def __get_url(ware_md5):
    return 'url_' + ware_md5


def __make_offer(feed, msku, offer_id, ware_md5, category_id, is_blue_offer=False, is_dsbs_offfer=False, is_dsbs_for_blue_shard=False):
    if ware_md5 is None:
        ware_md5 = 'hc3cVZiClnllcxjhGX0_cQ'
    result = {
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            ware_md5=ware_md5,
            url=__get_url(ware_md5),
            title=__get_title(ware_md5),
            description=__get_description(ware_md5),
            additional_search_text=__get_add_search_text(ware_md5),
            book_authors=__get_book_authors(ware_md5),
            vendor_code=__get_vendor_code(ware_md5),
            supplier_description=__get_supplier_description(ware_md5),
            is_blue_offer=is_blue_offer,
            offer_flags=OfferFlags.BLUE_OFFER.value if is_blue_offer else None,
            cpa=CpaStatus.REAL if is_dsbs_offfer else CpaStatus.NO,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
            category_id=category_id,
        ),
    }

    if is_blue_offer or is_dsbs_for_blue_shard:
        result['msku'] = msku
    return result


def _make_blue_offer(feed, msku, offer_id, ware_md5, category_id):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id, is_blue_offer=True)


def _make_white_offer(feed, msku, offer_id, ware_md5, category_id):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id)


def _make_dsbs_offer(feed, msku, offer_id, ware_md5, category_id, for_blue_shard=False):
    return __make_offer(feed, msku, offer_id, ware_md5, category_id, is_dsbs_offfer=True, is_dsbs_for_blue_shard=for_blue_shard)


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
            'generate_offers_search_texts_table': 'true',
        },

    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return [
        _make_white_offer(1000, 1, 'WHITE_OFFER1', WHITE_OFFER1, CATEGORY_ID),
        _make_white_offer(2000, 2, 'WHITE_OFFER2', WHITE_OFFER2, CATEGORY_ID),
        _make_dsbs_offer(3000, 1, 'DSBS_OFFER1', DSBS_OFFER1, CATEGORY_ID),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    return [
        _make_blue_offer(2000, 1, 'BLUE_OFFER1', BLUE_OFFER1, CATEGORY_ID),
        _make_blue_offer(2000, 2, 'BLUE_OFFER2', BLUE_OFFER2, CATEGORY_ID),
        _make_dsbs_offer(3000, 1, 'DSBS_OFFER2', DSBS_OFFER2, CATEGORY_ID, for_blue_shard=True),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex(source_blue_offers_raw):
    return [
        make_msku_contex_dict(
            msku=1,
            feed_id=300,
            shop_id=300,
            title=__get_title(MSKU1),
            ware_md5=MSKU1,
            description=__get_description(MSKU1),
            category_id=CATEGORY_ID,
        ),
        make_msku_contex_dict(
            msku=2,
            feed_id=301,
            shop_id=301,
            title=__get_title(MSKU2),
            ware_md5=MSKU2,
            description=__get_description(MSKU2),
            category_id=CATEGORY_ID,
        ),
        make_msku_contex_dict(
            msku=3,
            feed_id=302,
            shop_id=302,
            title=__get_title(MSKU3),
            ware_md5=MSKU3,
            description=__get_description(MSKU3),
            category_id=CATEGORY_ID,
        ),
    ]


@pytest.yield_fixture(scope="module")
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
    with Or3MainIdxTestEnv(
            yt_stuff=yt_server,
            generation=GENERATION,
            mi3_type=MI3_TYPE,
            shards=SHARDS,
            half_mode=False,
            blue_shards=BLUE_SHARDS,
            **resources) as mi:
        mi.verify()
        mi.execute()
        yield mi


def test_offers_search_texts_count(main_idx):
    """Проверяем кол-во записей в таблице offers_search_texts
    """
    assert_that(len(main_idx.outputs['offers_search_texts']), equal_to(9))  # 2 white + 3 msku + 2 blue + 2 dsbs


def test_blue_offers_search_texts_count(main_idx):
    """Проверяем кол-во записей в таблице blue_offers_search_texts
    """
    assert_that(len(main_idx.outputs['blue_offers_search_texts']), equal_to(6))  # 2 blue + 1 dsbs + 3 msku


def test_blue_offers_search_texts(main_idx):
    """Проверяем данные из таблицы blue_offers_search_texts
    title, description, url, category_id для синих оферов берется из их мску
    """
    expected = [
        {
            'ware_md5': BLUE_OFFER1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(MSKU1), 'url': 'https://market.yandex.ru/product/1713074440?sku=1', 'book_authors': __get_book_authors(BLUE_OFFER1),
            'additional_search_text': __get_add_search_text(BLUE_OFFER1), 'vendor_code': __get_vendor_code(BLUE_OFFER1),
            'description': __get_description(MSKU1), 'supplier_description': __get_supplier_description(BLUE_OFFER1),
        },
        {
            'ware_md5': BLUE_OFFER2, 'table_index': 0, 'msku': 2, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(MSKU2), 'url': 'https://market.yandex.ru/product/1713074440?sku=2', 'book_authors': __get_book_authors(BLUE_OFFER2),
            'additional_search_text': __get_add_search_text(BLUE_OFFER2), 'vendor_code': __get_vendor_code(BLUE_OFFER2),
            'description': __get_description(MSKU2), 'supplier_description': __get_supplier_description(BLUE_OFFER2),
        },
        {
            'ware_md5': DSBS_OFFER2, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(DSBS_OFFER2), 'url': __get_url(DSBS_OFFER2), 'book_authors': __get_book_authors(DSBS_OFFER2),
            'additional_search_text': __get_add_search_text(DSBS_OFFER2), 'vendor_code': __get_vendor_code(DSBS_OFFER2),
            'description': __get_description(DSBS_OFFER2), 'supplier_description': None,
        },
        {'ware_md5': MSKU1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU2, 'table_index': 0, 'msku': 2, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU3, 'table_index': 0, 'msku': 3, 'is_fake_msku_offer': True},
    ]

    for item in expected:
        assert_that(main_idx.outputs['blue_offers_search_texts'], has_item(has_entries(item)))


def test_offers_search_texts(main_idx):
    """Проверяем данные из таблицы offers_search_texts
    """
    expected = [
        {
            'ware_md5': WHITE_OFFER1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(WHITE_OFFER1), 'url': __get_url(WHITE_OFFER1), 'book_authors': __get_book_authors(WHITE_OFFER1),
            'additional_search_text': __get_add_search_text(WHITE_OFFER1), 'vendor_code': __get_vendor_code(WHITE_OFFER1),
            'description': __get_description(WHITE_OFFER1), 'supplier_description': None,
        },
        {
            'ware_md5': WHITE_OFFER2, 'table_index': 0, 'msku': 2, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(WHITE_OFFER2), 'url': __get_url(WHITE_OFFER2), 'book_authors': __get_book_authors(WHITE_OFFER2),
            'additional_search_text': __get_add_search_text(WHITE_OFFER2), 'vendor_code': __get_vendor_code(WHITE_OFFER2),
            'description': __get_description(WHITE_OFFER2), 'supplier_description': None,
        },
         {
            'ware_md5': BLUE_OFFER1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(MSKU1), 'url': 'https://market.yandex.ru/product/1713074440?sku=1', 'book_authors': __get_book_authors(BLUE_OFFER1),
            'additional_search_text': __get_add_search_text(BLUE_OFFER1), 'vendor_code': __get_vendor_code(BLUE_OFFER1),
            'description': __get_description(MSKU1), 'supplier_description': __get_supplier_description(BLUE_OFFER1),
        },
        {
            'ware_md5': BLUE_OFFER2, 'table_index': 0, 'msku': 2, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(MSKU2), 'url': 'https://market.yandex.ru/product/1713074440?sku=2', 'book_authors': __get_book_authors(BLUE_OFFER2),
            'additional_search_text': __get_add_search_text(BLUE_OFFER2), 'vendor_code': __get_vendor_code(BLUE_OFFER2),
            'description': __get_description(MSKU2), 'supplier_description': __get_supplier_description(BLUE_OFFER2),
        },
        {
            'ware_md5': DSBS_OFFER1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(DSBS_OFFER1), 'url': __get_url(DSBS_OFFER1), 'book_authors': __get_book_authors(DSBS_OFFER1),
            'additional_search_text': __get_add_search_text(DSBS_OFFER1), 'vendor_code': __get_vendor_code(DSBS_OFFER1),
            'description': __get_description(DSBS_OFFER1), 'supplier_description': None,
        },
        {
            'ware_md5': DSBS_OFFER2, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': False, 'category_id': CATEGORY_ID,
            'title': __get_title(DSBS_OFFER2), 'url': __get_url(DSBS_OFFER2), 'book_authors': __get_book_authors(DSBS_OFFER2),
            'additional_search_text': __get_add_search_text(DSBS_OFFER2), 'vendor_code': __get_vendor_code(DSBS_OFFER2),
            'description': __get_description(DSBS_OFFER2), 'supplier_description': None,
        },
        {'ware_md5': MSKU1, 'table_index': 0, 'msku': 1, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU2, 'table_index': 0, 'msku': 2, 'is_fake_msku_offer': True},
        {'ware_md5': MSKU3, 'table_index': 0, 'msku': 3, 'is_fake_msku_offer': True},
    ]

    for item in expected:
        assert_that(main_idx.outputs['offers_search_texts'], has_item(has_entries(item)))
