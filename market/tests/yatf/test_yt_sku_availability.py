# coding=utf-8
import pytest
import calendar

from yt.wrapper import ypath_join

from hamcrest import assert_that, equal_to, has_item, has_entries
from datetime import datetime, timedelta
from dateutil import parser

from market.proto.indexer.mdm_item_pb2 import (
    MdmItemInformation,
    MdmItemForWarehouse,
    OfferMdmInfo,
)
from market.idx.datacamp.proto.offer.OfferContent_pb2 import WeightAndDimensions
from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from mapreduce.yt.python.table_schema import extract_column_attributes
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_tables.stock_sku_table import StockSkuTable
from market.idx.yatf.resources.yt_tables.lbdumper_tables import LbDumperMdmTable

NOW = datetime.now()


def make_datetime_string(offset_seconds=0):
    return (NOW + timedelta(seconds=offset_seconds)).isoformat(' ')


def make_timestamp_in_msc(datetime_string):
    dt = parser.parse(datetime_string)
    return int(calendar.timegm(dt.timetuple()) * 1000 + dt.microsecond / 1000)


@pytest.fixture(scope='module')
def stock_sku_items_supplier_1_not_complete():
    return [{
        'shop_sku': 'sku3',
        'warehouse_id': 145,
    }, {
        'supplier_id': 1,
        'warehouse_id': 145,
    }, {
        'supplier_id': 1,
        'shop_sku': 'sku4',
    }, ]


@pytest.fixture(scope='module')
def stock_sku_items_supplier_1(stock_sku_items_supplier_1_not_complete):
    result = [{
        'supplier_id': 1,
        'shop_sku': 'sku1',
        'warehouse_id': 145,
        'available_amount': 1,
        'synced': make_datetime_string(1),
        'ff_updated': make_datetime_string(2),
        'refilled_date': make_datetime_string(3),
    }, {
        'supplier_id': 1,
        'shop_sku': 'sku1',
        'warehouse_id': 146,
    }, {
        'supplier_id': 1,
        'shop_sku': 'sku2',
        'warehouse_id': 145,
    }, ]

    result.extend(stock_sku_items_supplier_1_not_complete)
    return result


@pytest.fixture(scope='module')
def stock_sku_items_supplier_2():
    return [{
        'supplier_id': 2,
        'shop_sku': 'sku1',
        'warehouse_id': 145,
    }, {
        'supplier_id': 2,
        'shop_sku': 'sku1',
        'warehouse_id': 146,
    }, ]


@pytest.fixture(scope='module')
def stock_sku_items_supplier_3():
    return [{
        'supplier_id': 3,
        'shop_sku': 'sku1',
        'warehouse_id': 145,
    }, {
        'supplier_id': 3,
        'shop_sku': 'sku1',
        'warehouse_id': 146,
    }, ]


@pytest.fixture(scope='module')
def stock_sku_items(
    stock_sku_items_supplier_1,
    stock_sku_items_supplier_2,
    stock_sku_items_supplier_3,
):
    result = []
    result.extend(stock_sku_items_supplier_1)
    result.extend(stock_sku_items_supplier_2)
    result.extend(stock_sku_items_supplier_3)
    return result


DIMENSIONS = WeightAndDimensions(
    weight=1.0,
    length=2,
    width=3,
    height=4,
)


DIMENSIONS_SPECIAL = WeightAndDimensions(
    weight=5.0,
    length=6,
    width=7,
    height=8,
)


@pytest.fixture(scope='module')
def mdm_items():
    return [{
        'supplier_id': 1,
        'shop_sku': 'sku1',
        'mdm_info': MdmItemInformation(
            warehouses=[
                MdmItemForWarehouse(
                    information=OfferMdmInfo(
                        dimensions=DIMENSIONS
                    )
                )
            ]
        ).SerializeToString()
    }, {
        'supplier_id': 2,
        'shop_sku': 'sku1',
        'mdm_info': MdmItemInformation(
            warehouses=[
                MdmItemForWarehouse(
                    warehouse_id=145,
                    information=OfferMdmInfo(
                        dimensions=DIMENSIONS_SPECIAL
                    )
                )
            ]
        ).SerializeToString()
    }, {
        'supplier_id': 3,
        'shop_sku': 'sku1',
        'mdm_info': MdmItemInformation(
            warehouses=[
                MdmItemForWarehouse(
                    information=OfferMdmInfo(
                        dimensions=DIMENSIONS
                    )
                ),
                MdmItemForWarehouse(
                    warehouse_id=145,
                    information=OfferMdmInfo(
                        dimensions=DIMENSIONS_SPECIAL
                    )
                )
            ]
        ).SerializeToString()
    }, ]


@pytest.fixture(scope='module')
def stock_sku_table(yt_server, stock_sku_items):
    table = StockSkuTable(
        yt_server,
        ypath_join(get_yt_prefix(), 'mstat', 'stock_sku'),
        data=stock_sku_items
    )

    table.dump()
    return table


@pytest.yield_fixture(scope="module")
def yt_mdm_info_table(yt_server, mdm_items):
    table = LbDumperMdmTable(
        yt_server,
        ypath_join(get_yt_prefix(), 'lbdumper', 'mdm'),
        data=mdm_items,
    )

    table.dump()
    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, stock_sku_table, yt_mdm_info_table):
    with YtDataUploadTestEnv() as env:
        env.execute(
            yt_server,
            type='sku_availability_yt',
            output_table="//home/indexer/sku_availability",
            input_table=stock_sku_table.get_path(),
            second_input_table=yt_mdm_info_table.get_path(),
        )
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    """ Проверяем структуру выходной таблицы """
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, "name": "warehouse_id", "type": "uint32"},
                    {'required': False, "name": "sku", "type": "string"},
                    {'required': False, "name": "shop_id", "type": "uint32"},
                    {'required': False, "name": "refill_timestamp", "type": "uint64"},
                    {'required': False, "name": "ss_timestamp", "type": "uint64"},
                    {'required': False, "name": "ff_timestamp", "type": "uint64"},
                    {'required': False, "name": "available_count", "type": "uint32"},
                    {'required': False, "name": "width", "type": "double"},
                    {'required': False, "name": "height", "type": "double"},
                    {'required': False, "name": "length", "type": "double"},
                    {'required': False, "name": "weight", "type": "double"},
                ]), "Schema is incorrect")


def test_result_table_row_count(result_yt_table, stock_sku_items, stock_sku_items_supplier_1_not_complete):
    """ Проверяем количество строк в выходной таблице
        Записи во входной таблице, в которых не указан warehouse_id/supplier_id/shop_sku пропускаются
    """
    assert_that(len(result_yt_table.data),
                equal_to(len(stock_sku_items) - len(stock_sku_items_supplier_1_not_complete)),
                "Rows count equal count of models in file")


def test_data_conversion(result_yt_table, stock_sku_items_supplier_1):
    """ Проверяем корректность конвертации всех полей """
    stock_sku = stock_sku_items_supplier_1[0]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 1,
            'ss_timestamp': make_timestamp_in_msc(stock_sku['synced']),
            'ff_timestamp': make_timestamp_in_msc(stock_sku['ff_updated']),
            'refill_timestamp': make_timestamp_in_msc(stock_sku['refilled_date']),
            'weight': DIMENSIONS.weight,
            'length': DIMENSIONS.length,
            'width': DIMENSIONS.width,
            'height': DIMENSIONS.height,
        }))
    )


def test_common_dimensions(result_yt_table, stock_sku_items_supplier_1):
    """ Общие для товара ВГХ проставляются для всех складов """
    stock_sku = stock_sku_items_supplier_1[1]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': DIMENSIONS.weight,
            'length': DIMENSIONS.length,
            'width': DIMENSIONS.width,
            'height': DIMENSIONS.height,
        }))
    )


def test_default_values(result_yt_table, stock_sku_items_supplier_1):
    """ Проверяем дефолтные значения для всех полей """
    stock_sku = stock_sku_items_supplier_1[2]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': 0.0,
            'length': 0.0,
            'width': 0.0,
            'height': 0.0,
        }))
    )


def test_special_dimensions(result_yt_table, stock_sku_items_supplier_2):
    """ Если есть только специфичные для склада ВГХ, то они используются только для товаров на этом складе """
    stock_sku = stock_sku_items_supplier_2[0]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': DIMENSIONS_SPECIAL.weight,
            'length': DIMENSIONS_SPECIAL.length,
            'width': DIMENSIONS_SPECIAL.width,
            'height': DIMENSIONS_SPECIAL.height,
        }))
    )

    stock_sku = stock_sku_items_supplier_2[1]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': 0,
            'length': 0,
            'width': 0,
            'height': 0,
        }))
    )


def test_special_and_default_dimensions(result_yt_table, stock_sku_items_supplier_3):
    """ Если есть специфичные для склада ВГХ, то используются они. Иначе, общие ВГХ для товара """
    stock_sku = stock_sku_items_supplier_3[0]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': DIMENSIONS_SPECIAL.weight,
            'length': DIMENSIONS_SPECIAL.length,
            'width': DIMENSIONS_SPECIAL.width,
            'height': DIMENSIONS_SPECIAL.height,
        }))
    )

    stock_sku = stock_sku_items_supplier_3[1]
    assert_that(
        result_yt_table.data,
        has_item(has_entries({
            'warehouse_id': stock_sku['warehouse_id'],
            'shop_id': stock_sku['supplier_id'],
            'sku': stock_sku['shop_sku'],
            'available_count': 0,
            'ss_timestamp': 0,
            'ff_timestamp': 0,
            'refill_timestamp': 0,
            'weight': DIMENSIONS.weight,
            'length': DIMENSIONS.length,
            'width': DIMENSIONS.width,
            'height': DIMENSIONS.height,
        }))
    )
