from market.dynamic_pricing.pricing.library.utils import (
    price_from_index,
    round_to_base,
    floor_to_base,
    round_to_ending,
    floor_to_ending,
    warehouse_mapper,
    calc_demand,
)
import yatest.common
from market.dynamic_pricing.pricing.library.constants import FULFILLMENT_WAREHOUSES


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


def test_round_to_ending():
    # округляем до 9 вниз
    assert 9 == round_to_ending(12, ending=9)
    # округляем до 9 вверх
    assert 19 == round_to_ending(15, ending=9)
    # округляем до 9 вверх (математическое округление)
    assert 119 == round_to_ending(114, ending=9)
    # округляем до 9, чтобы было положительное
    assert 9 == round_to_ending(2, ending=9)
    # ничего не округляем
    assert 19 == round_to_ending(19, ending=9)
    # убираем копейки
    assert 19 == round_to_ending(19.5, ending=9)
    # убираем копейки вверх
    assert 19 == round_to_ending(18.5, ending=9)


def test_floor_to_ending():
    # округляем до 9 вниз
    assert 9 == floor_to_ending(12, ending=9)
    assert 9 == floor_to_ending(15, ending=9)
    assert 109 == floor_to_ending(114, ending=9)
    # округляем до 9, чтобы было положительное
    assert 9 == floor_to_ending(2, ending=9)
    # ничего не округляем
    assert 19 == floor_to_ending(19, ending=9)
    # убираем копейки
    assert 19 == floor_to_ending(19.5, ending=9)
    # убираем копейки вниз
    assert 9 == floor_to_ending(18.5, ending=9)


def test_round_to_base():
    # округляем до 0 вниз
    assert 10 == round_to_base(12, base=5)
    # округляем до 5 вверх
    assert 15 == round_to_base(13, base=5)
    # округляем до 0 вверх (математическое округление)
    assert 120 == round_to_base(118, base=5)
    # ничего не округляем
    assert 10 == round_to_base(10, base=5)
    # убираем копейки
    assert 10 == round_to_base(10.5, base=5)
    # убираем копейки вверх
    assert 20 == round_to_base(18.5, base=5)


def test_floor_to_base():
    # округляем до 0 вниз
    assert 10 == floor_to_base(12, base=5)
    assert 15 == floor_to_base(18, base=5)
    assert 110 == floor_to_base(114, base=10)
    # не поднимаем до base
    assert 0 == floor_to_base(2, base=5)
    # ничего не округляем
    assert 10 == floor_to_base(10, base=5)
    # убираем копейки
    assert 10 == floor_to_base(12.5, base=5)
    # убираем копейки вниз
    assert 10 == floor_to_base(19.5, base=10)


def test_warehouse_mapper_with_warehouse():
    input_records = []
    input_records.append({
        'warehouse_id': 1,
        'market_sku': 1
    })
    record = list(warehouse_mapper(input_records))[0]
    assert record['warehouse_id'] == 1


def test_warehouse_mapper_without_warehouse():
    input_records = []
    input_records.append({
        'warehouse_id': -1,
        'market_sku': 1
    })
    result = list(warehouse_mapper(input_records))
    for record, wh in zip(result, FULFILLMENT_WAREHOUSES):
        assert record['warehouse_id'] == wh


def test_calc_demand():
    price_demand_list = [(5480.0, 0.04735860823948197), (5514.0, 0.045516815504185404), (5548.0, 0.042346795007280516), (5582.0, 0.04198018201676056)]
    demand = calc_demand(5520, price_demand_list)
    assert demand is not None
    assert demand > 0


def test_price_from_index():
    assert price_from_index('RUR 1230000000') == 123
    assert price_from_index('RUR 26226000000') == 2623
    assert price_from_index('RUR 26224000000') == 2622
    assert price_from_index('BYN 2350000000', currency='BYN') == 235
