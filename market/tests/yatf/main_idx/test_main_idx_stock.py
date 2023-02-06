# coding: utf-8


import pytest
import datetime

from hamcrest import (
    assert_that,
    equal_to,
    is_not,
    has_key,
    has_items,
    has_entries,
)

from yt.wrapper import ypath_join

from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable,
    BlueOffersRawTable,
    ApiSnapshotTable,
    Offer2StockTable,
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    FeedColor,
    FeedInfo,
    create_shops_dat,
    make_offer_proto_str,
    make_uc_proto_str,
    DISABLED_MARKET_STOCK
)

from market.idx.pylibrary.offer_flags.flags import OfferFlags, DisabledFlags
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'
COUNT_SHARDS = 1
HALF_MODE = False

WAREHOUSE_145 = 145
BLUE_OFFER_SAME_MSKU = 10000
STOCK_DELTA = 10

WHITE_FEED_INFO = FeedInfo(feed_id=1, warehouse_id=WAREHOUSE_145)
BLUE_FEED_INFO = FeedInfo(feed_id=2, warehouse_id=WAREHOUSE_145, color=FeedColor.BLUE)
FAKE_MSKU_FEED_INFO = FeedInfo(feed_id=5, color=FeedColor.BLUE)
BLUE_FEED_IGNORE_STOCKS_INFO = FeedInfo(feed_id=6, warehouse_id=WAREHOUSE_145, color=FeedColor.BLUE)

# Синий оффер с is_available = true, но с available_amount > 0
BLUE_OFFER_WITH_STOCK_INFO = 1
# Синий оффер с is_available = true, но с available_amount == 0
BLUE_OFFER_WITH_ZERO_STOCK = 2
# Синий оффер с is_available = true, но с available_amount < 0
BLUE_OFFER_WITH_NEGAIVE_STOCK = 3
# Синий оффер с is_available = false, но с available_amount > 0
BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK = 4
# Синий оффер без информации о стоке
BLUE_OFFER_WITHOUT_STOCK = 5
# Синий оффер с установленным признаком has_gone, у которого есть товары на стоке
BLUE_OFFER_WITH_STOCK_AND_HAS_GONE = 6
# Синие офферы под одним msku
BLUE_OFFER_SAME_MSKU_WITH_STOCK_1 = 7
BLUE_OFFER_SAME_MSKU_WITH_STOCK_2 = 8
BLUE_OFFER_SAME_MSKU_WITH_STOCK_3 = 9
BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_1 = 10
BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_2 = 11
BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_3 = 12
# Синий оффер от магазина с установленной опцией ignore_stocks, но без данных о стоке
BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS = 15


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
#            'yt_mstat_stock_table': ypath_join(home_dir, 'mstat', 'stock_sku'),
        },
        'misc': {
            'blue_offers_enabled': 'true',
        }
    }


@pytest.yield_fixture(scope="module")
def or3_config(or3_config_data):
    return Or3Config(**or3_config_data)


def make_offer_id(feed_id, offer_index):
    """ Создаем уникальный offer_id для каждого оффера из теста (для удобства поиска) """
    return 'feed_{feed_id}_offer_{index}'.format(feed_id=feed_id, index=offer_index)


def make_shop_sku(feed_id, offer_index):
    return make_offer_id(feed_id, offer_index) + "_shop_sku"


@pytest.fixture(scope="module")
def shops_dat():
    return ShopsDat([
        create_shops_dat(BLUE_FEED_IGNORE_STOCKS_INFO, ignore_stocks=True),
    ])


@pytest.yield_fixture(scope="module")
def source_offers_raw():
    return []


@pytest.yield_fixture(scope="module")
def source_blue_offers_raw():
    def create_blue_offer(feed_info, offer_index, msku=None, is_blue_offer=True, disabled_flag_sources=DISABLED_MARKET_STOCK, **kwargs):
        offer_id = make_offer_id(feed_info.feed_id, offer_index)
        shop_sku = make_shop_sku(feed_info.feed_id, offer_index)

        if 'offer_flags' in kwargs:
            kwargs['offer_flags'] |= OfferFlags.BLUE_OFFER.value
        else:
            kwargs['offer_flags'] = OfferFlags.BLUE_OFFER.value

        if 'offer_flags64' in kwargs:
            kwargs['offer_flags64'] |= OfferFlags.BLUE_OFFER.value
        else:
            kwargs['offer_flags64'] = OfferFlags.BLUE_OFFER.value

        if msku is None:
            msku = offer_index

        return {
            'msku': msku,
            'feed_id': feed_info.feed_id,
            'offer_id': offer_id,
            'session_id': feed_info.session_id,
            'offer': make_offer_proto_str(
                supplier_id=feed_info.supplier_id,
                warehouse_id=feed_info.warehouse_id,
                shop_sku=shop_sku,
                is_blue_offer=is_blue_offer,
                stock_store_count=offer_index,
                disabled_flag_sources=disabled_flag_sources,
                **kwargs
            ),
            'uc': make_uc_proto_str(),
        }

    return [
        # Синий оффер с товарами на стоке
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_INFO),
        # Синий оффер без товаров на стоке
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITH_NEGAIVE_STOCK),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITH_ZERO_STOCK),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK),
        # Синий оффер без данных о стоке (с установленным признаком скрытия по одному источнику)
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITHOUT_STOCK,
                          disabled_flags=DisabledFlags.PUSH_PARTNER_FEED.value,
                          disabled_flag_sources=0,
                          ),
        # Синий оффер с установленным признаком has_gone, у которого есть товары на стоке
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_AND_HAS_GONE, has_gone=True,
                          offer_flags=OfferFlags.OFFER_HAS_GONE.value),
        # Синие офферы под одним msku
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_1, BLUE_OFFER_SAME_MSKU),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_2, BLUE_OFFER_SAME_MSKU),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_3, BLUE_OFFER_SAME_MSKU),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_1, BLUE_OFFER_SAME_MSKU),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_2, BLUE_OFFER_SAME_MSKU),
        create_blue_offer(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_3, BLUE_OFFER_SAME_MSKU),
        # Синий оффер от магазина с установленной опцией ignore_stocks, но без данных о стоке
        create_blue_offer(BLUE_FEED_IGNORE_STOCKS_INFO, BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS, disabled_flag_sources=0),
    ]


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    def create_msku(offer_index, msku=None):
        if msku is None:
            msku = offer_index

        return {
            'msku': msku,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': FAKE_MSKU_FEED_INFO.feed_id,
            'offer_id': 'MS000{}'.format(offer_index),
            'offer': make_offer_proto_str(is_fake_msku_offer=True),
            'uc': make_uc_proto_str(),
        }

    # NB: Для всех синих офферов в blue_offers_raw должны быть заполнены данные в таблице msku
    return [
        create_msku(BLUE_OFFER_WITH_STOCK_INFO),
        create_msku(BLUE_OFFER_WITH_NEGAIVE_STOCK),
        create_msku(BLUE_OFFER_WITH_ZERO_STOCK),
        create_msku(BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK),
        create_msku(BLUE_OFFER_WITHOUT_STOCK),
        create_msku(BLUE_OFFER_WITH_STOCK_AND_HAS_GONE),
        create_msku(BLUE_OFFER_SAME_MSKU_WITH_STOCK_1, BLUE_OFFER_SAME_MSKU),
        create_msku(BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS),
    ]


@pytest.yield_fixture(scope="module")
def source_stock_sku_table():
    def create_stock_sku(
            feed_info,
            offer_index,
            is_available=True,
            available_amount=0,
            is_preorder=False,
            weight=1.0,
            width=2,
            height=3,
            length=4,
    ):
        shop_sku = make_shop_sku(feed_info.feed_id, offer_index)
        return {
            'warehouse_id': feed_info.warehouse_id,
            'supplier_id': feed_info.supplier_id,
            'shop_sku': shop_sku,
            'is_available': is_available,
            'available_amount': available_amount,
            'is_preorder': is_preorder,
            'weight': weight,
            'width': width,
            'height': height,
            'length': length,
        }

    return [
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_INFO,
                         available_amount=BLUE_OFFER_WITH_STOCK_INFO + STOCK_DELTA),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_WITH_ZERO_STOCK, is_preorder=True),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_WITH_NEGAIVE_STOCK,
                         available_amount=-(BLUE_OFFER_WITH_NEGAIVE_STOCK + STOCK_DELTA)),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK, is_available=False,
                         available_amount=BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK + STOCK_DELTA),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_AND_HAS_GONE,
                         available_amount=BLUE_OFFER_WITH_STOCK_AND_HAS_GONE + STOCK_DELTA),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_1,
                         available_amount=BLUE_OFFER_SAME_MSKU_WITH_STOCK_1 + STOCK_DELTA),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_2,
                         available_amount=BLUE_OFFER_SAME_MSKU_WITH_STOCK_2 + STOCK_DELTA),
        create_stock_sku(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_3,
                         available_amount=BLUE_OFFER_SAME_MSKU_WITH_STOCK_3 + STOCK_DELTA),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offer2stock_table():
    def create_blue_offer2stock(feed_info, offer_index, msku=None):
        offer_id = make_offer_id(feed_info.feed_id, offer_index)
        shop_sku = make_shop_sku(feed_info.feed_id, offer_index)

        if msku is None:
            msku = offer_index

        return {
            'warehouse_id': feed_info.warehouse_id,
            'supplier_id': feed_info.supplier_id,
            'shop_sku': shop_sku,
            'feed_id': feed_info.feed_id,
            'offer_id': offer_id,
            'msku': msku
        }

    # В таблице blue_offer2stock_unsorted хранится результат map-а:
    # (warehouse_id, supplier_id, shop_sku) <=> (feed_id, offer_id)
    return [
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_INFO),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITH_ZERO_STOCK),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITH_NEGAIVE_STOCK),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITHOUT_STOCK),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_WITH_STOCK_AND_HAS_GONE),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_1, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_2, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITH_STOCK_3, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_1, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_2, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_INFO, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_3, BLUE_OFFER_SAME_MSKU),
        create_blue_offer2stock(BLUE_FEED_IGNORE_STOCKS_INFO, BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS),
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config,
                     source_offers_raw,
                     source_blue_offers_raw,
                     source_msku_contex,
                     source_stock_sku_table,
                     source_blue_offer2stock_table):

    yt_home_path = or3_config.options['yt']['home_dir']
    return {
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
        'api_snapshot': ApiSnapshotTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'api_snapshots', 'prices'),
            data={}
        ),
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
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'blue_offer2stock_unsorted': Offer2StockTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offer2stock_unsorted'),
            data=source_blue_offer2stock_table,
            blue=True
        )
        # 'stock_sku': StockSkuTable(
        #     yt_stuff=yt_server,
        #     path=ypath_join(yt_home_path, 'mstat', 'stock_sku'),
        #     data=source_stock_sku_table
        # )
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, tovar_tree):
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


@pytest.yield_fixture(scope="module")
def result_offers(main_idx):
    offers = main_idx.outputs['offers_by_offer_id']
    return offers


def test_offers_count(result_offers, source_msku_contex, source_offers_raw, source_blue_offers_raw):
    assert_that(
        len(result_offers),
        equal_to(
            len(source_msku_contex) +
            len(source_offers_raw) +
            len(source_blue_offers_raw)
        )
    )


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer2stock_reduced(main_idx):
    """ Проверяем, что создались все промежуточные таблицы с корректным числом офферов
        В таблицу blue_offer2stock_reduced попадают все реальные синие офферы, информация о которых есть в таблице
        стоков
    """
    rows = main_idx.outputs['blue_offer_to_stock']
    blue_offer2stock_reduced = [row['offer_id'] for row in rows]

    # Офферы попадающие в таблицу blue_offer2stock_reduced
    blue_offer2stock_reduced_expected = [
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_STOCK_AND_HAS_GONE),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_STOCK_INFO),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_NEGAIVE_STOCK),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_ZERO_STOCK),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_1),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_2),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_3),
        make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK),
    ]
    assert_that(len(rows), equal_to(len(blue_offer2stock_reduced_expected)))
    assert_that(blue_offer2stock_reduced, has_items(*blue_offer2stock_reduced_expected))


def test_blue_offer_with_stock(result_offers):
    """ Если у синего оффера есть информация о стоке, то проставляется:
        stock_store_count = available_amount
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_STOCK_INFO)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_WITH_STOCK_INFO))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),  # flags64 is str? wtf?
    }))


def test_blue_offer_without_stock(result_offers):
    """ Если у синего оффера нет информации о стоке, то проставляется:
        stock_store_count = 0, если он был определен в OfferData.proto
        disabled_flags |= DATASOURCE_MARKET_IDX
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITHOUT_STOCK)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer['disabled_flags'], equal_to(DisabledFlags.MARKET_IDX | DisabledFlags.PUSH_PARTNER_FEED))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_with_zero_stock(result_offers):
    """ Если синего оффера нет на складе, но стоит признак is_availbale == True, то проставляется:
        stock_store_count = 0.
        Для оффера с предзаказом в таблице стоков добавляется флаг предзаказа.
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_ZERO_STOCK)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.IS_PREORDER.value | OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_with_negative_stock(result_offers):
    """ Если у синего оффера отрицательные данные о стоке, но стоит признак is_availbale == True, то проставляется:
        stock_store_count = 0
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_NEGAIVE_STOCK)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_with_not_available_but_positive_stock(result_offers):
    """ Если у синего оффера положительные данные о стоке, но установлен is_available == False, то проставляется:
        stock_store_count = 0, если он был определен в OfferData.proto
        disabled_flags |= DATASOURCE_MARKET_STOCK
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id,
                                        BLUE_OFFER_WITH_NOT_AVAILABLE_BUT_POSITIVE_STOCK)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer['disabled_flags'], equal_to(DisabledFlags.MARKET_STOCK.value))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_with_stock_and_has_gone(result_offers):
    """ Если у синего оффера есть данные на складе, но установлен has_gone, то проставляется:
        stock_store_count = available_amount
        но has_gone остается без изменений
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_WITH_STOCK_AND_HAS_GONE)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_WITH_STOCK_AND_HAS_GONE + STOCK_DELTA))
    assert_that(offer['has_gone'], equal_to(True))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value | OfferFlags.OFFER_HAS_GONE.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_same_msku_with_stock(result_offers):
    """ Если под одним msku несколько офферов с данными на стоке, то для всех них проставляется:
        stock_store_count = available_amount
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_1)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_SAME_MSKU_WITH_STOCK_1 + STOCK_DELTA))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))

    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_2)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_SAME_MSKU_WITH_STOCK_2 + STOCK_DELTA))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))

    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITH_STOCK_3)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_SAME_MSKU_WITH_STOCK_3 + STOCK_DELTA))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer, is_not(has_key('disabled_flags')))


@pytest.mark.skip("MARKETINDEXER-39623")
def test_blue_offer_same_msku_without_stock(result_offers):
    """ Если под одним msku несколько офферов без данных о стоке, то для всех них проставляется:
        stock_store_count = 0, если он был определен в OfferData.proto
        disabled_flags |= DATASOURCE_MARKET_IDX
    """
    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_1)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer['disabled_flags'], equal_to(DisabledFlags.MARKET_IDX.value))

    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_2)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer['disabled_flags'], equal_to(DisabledFlags.MARKET_IDX.value))

    offer = result_offers[make_offer_id(BLUE_FEED_INFO.feed_id, BLUE_OFFER_SAME_MSKU_WITHOUT_STOCK_3)]['offer']
    assert_that(offer['stock_store_count'], equal_to(0))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
    assert_that(offer['disabled_flags'], equal_to(DisabledFlags.MARKET_IDX.value))


def test_fake_msku_offer(result_offers, source_msku_contex):
    """ Для MSKU значения stock_store_count и has_gone не меняются """
    for msku in source_msku_contex:
        offer = result_offers[msku['offer_id']]['offer']
        assert_that(offer, is_not(has_key('stock_store_count')))
        assert_that(offer, is_not(has_key('has_gone')))
        assert_that(offer, is_not(has_key('offer_flags')))


def test_blue_offer_ignore_stocks_shop(result_offers):
    """ Если для магазина установлена опция ignore_stocks, то информация о стоках из YT-таблицы игнорируется """
    offer = result_offers[make_offer_id(BLUE_FEED_IGNORE_STOCKS_INFO.feed_id,
                                        BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS)]['offer']
    assert_that(offer['stock_store_count'], equal_to(BLUE_OFFER_WITHOUT_STOCK_BUT_IGNORE_STOCKS))
    assert_that(offer, is_not(has_key('has_gone')))
    assert_that(offer, has_entries({
        'offer_flags': OfferFlags.BLUE_OFFER.value,
        'offer_flags64': str(OfferFlags.BLUE_OFFER.value),
    }))
