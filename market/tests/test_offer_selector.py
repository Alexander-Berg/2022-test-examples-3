from rules_assortment import select_cheapest_offer
from tables.index import only_3p_in_msku_reducer
from market.dynamic_pricing.pricing.library.constants import BERU_SUPPLIER_ID


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        import yatest.common
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


DATETIME = '2021-01-01T20:01:01'


def test_cheapest_offer():
    records = [
        {
            'current_price': 101,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 1,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '1',
            'date': DATETIME
        },
        {
            'current_price': 100,
            'supplier_id': 2,
            'shop_id': 2,
            'warehouse_id': 2,
            'shop_sku': '2',
            'offer_id': '2',
            'ware_md5': '2',
            'date': DATETIME
        }
    ]
    record = select_cheapest_offer(records)
    assert len(record) == 1
    assert record[0]['offer_id'] == '2'

    records = [
        {
            'current_price': 101,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 1,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '1',
            'date': DATETIME
        }
    ]
    record = select_cheapest_offer(records)
    assert len(record) == 1
    assert record[0]['offer_id'] == '1'


def test_cheapest_offer_randomize_correct_and_give_asme_results():
    records = [
        {
            'current_price': 100,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 1,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '1',
            'date': DATETIME
        },
        {
            'current_price': 100,
            'supplier_id': 2,
            'shop_id': 2,
            'warehouse_id': 2,
            'shop_sku': '2',
            'offer_id': '2',
            'ware_md5': '2',
            'date': DATETIME
        },
        {
            'current_price': 100,
            'supplier_id': 3,
            'shop_id': 3,
            'warehouse_id': 3,
            'shop_sku': '3',
            'offer_id': '3',
            'ware_md5': '3'
        }
    ]
    record = select_cheapest_offer(records)
    record1 = select_cheapest_offer(records)
    record2 = select_cheapest_offer(records)
    assert record is not None
    assert record1 is not None
    assert record2 is not None
    assert record == record1
    assert record1 == record2


def test_cheapest_offer_multioffers():
    records = [
        {
            'current_price': 100,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 1,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '1',
            'date': DATETIME
        },
        {
            'current_price': 100,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 2,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '2',
            'date': DATETIME
        },
        {
            'current_price': 100,
            'supplier_id': 1,
            'shop_id': 1,
            'warehouse_id': 1,
            'shop_sku': '1',
            'offer_id': '1',
            'ware_md5': '1',
            'date': DATETIME
        }
    ]
    record = select_cheapest_offer(records)
    assert len(record) == 2
    assert 1 in [x['warehouse_id'] for x in record]
    assert 2 in [x['warehouse_id'] for x in record]


def test_only_3p():
    input_data = [
        (
            {
                'market_sku': 1,
            },
            [
                {
                    'current_price': 100,
                    'supplier_id': BERU_SUPPLIER_ID,
                    'warehouse_id': 1,
                    'shop_sku': '1',
                    'offer_id': '1',
                    'ware_md5': '1'
                },
                {
                    'current_price': 101,
                    'supplier_id': 1,
                    'warehouse_id': 2,
                    'shop_sku': '2',
                    'offer_id': '2',
                    'ware_md5': '2'
                }
            ]
        )
    ]
    record = only_3p_in_msku_reducer(input_data)
    assert not list(record)

    input_data = [
        (
            {
                'market_sku': 1,
            },
            [
                {
                    'current_price': 101,
                    'supplier_id': 1,
                    'warehouse_id': 2,
                    'shop_sku': '2',
                    'offer_id': '2',
                    'ware_md5': '2'
                }
            ]
        )
    ]
    record = only_3p_in_msku_reducer(input_data)
    assert len(list(record)) == 1
