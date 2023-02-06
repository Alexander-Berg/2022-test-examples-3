from market.dynamic_pricing.pricing.deadstock_sales.sale_assortment.stage import (
    get_baseline_stock_level,
    stage_mapper
)
import yatest.common


class __YaTestEnv:  # for "ya make -t ..."
    def __init__(self):
        self.python_bin = yatest.common.python_path()
        self.work_dir = yatest.common.work_path()
        self.binary_path = yatest.common.binary_path
        self.source_path = yatest.common.source_path


_env = __YaTestEnv()


DEFAULT_STAGES = {
    'first_stage_until': 7,
    'second_stage_until': 14,
    'third_stage_until': 21,
    'fourth_stage_until': 28
}


def test_full_stock_level_at_start():
    assert 10 == get_baseline_stock_level(
        start_sale_stock=10,
        total_duration=28,
        current_sale_day=1
    )


def test_half_stock_level_in_the_middle():
    assert 5 == get_baseline_stock_level(
        start_sale_stock=10,
        total_duration=28,
        current_sale_day=15
    )


def test_empty_stock_level_at_last_day():
    assert 0 == get_baseline_stock_level(
        start_sale_stock=10,
        total_duration=28,
        current_sale_day=29
    )


def test_empty_stock_level_after_last_day():
    assert 0 == get_baseline_stock_level(
        start_sale_stock=10,
        total_duration=28,
        current_sale_day=35
    )


def test_stage_mapper_first_day():
    input_records = []
    input_records.append({
        'stage': 1,
        'stage_day': 1,
        'sale_duration': 1,
        'new_sale_duration': 1,
        'start_sale_stock': 20,
        'stock': 20
    })
    for rec in input_records:
        rec = rec.update(DEFAULT_STAGES)
    record = list(stage_mapper(input_records))[0]
    assert record['stage'] == 1
    assert record['stage_day'] == 1
    assert record['stock'] == 20


def test_stage_mapper_not_increase_stage():
    input_records = []
    input_records.append({
        'stage': 2,
        'stage_day': 7,
        'sale_duration': 21,
        'new_sale_duration': 22,
        'start_sale_stock': 20,
        'stock': 5
    })
    for rec in input_records:
        rec = rec.update(DEFAULT_STAGES)
    record = list(stage_mapper(input_records))[0]
    assert record['stage'] == 2
    assert record['stage_day'] == 8
    assert record['stock'] == 5


def test_stage_mapper_increase_one_stage():
    input_records = []
    input_records.append({
        'stage': 1,
        'stage_day': 10,
        'sale_duration': 10,
        'new_sale_duration': 11,
        'start_sale_stock': 20,
        'stock': 15
    })
    for rec in input_records:
        rec = rec.update(DEFAULT_STAGES)
    record = list(stage_mapper(input_records))[0]
    assert record['stage'] == 2
    assert record['stage_day'] == 1
    assert record['stock'] == 15


def test_stage_mapper_increase_two_stages():
    input_records = []
    input_records.append({
        'stage': 1,
        'stage_day': 20,
        'sale_duration': 20,
        'new_sale_duration': 21,
        'start_sale_stock': 20,
        'stock': 11
    })
    for rec in input_records:
        rec = rec.update(DEFAULT_STAGES)
    record = list(stage_mapper(input_records))[0]
    assert record['stage'] == 3
    assert record['stage_day'] == 1
    assert record['stock'] == 11


def test_stage_mapper_calc_in_same_day():
    input_records = []
    input_records.append({
        'stage': 2,
        'stage_day': 7,
        'sale_duration': 21,
        'new_sale_duration': 21,
        'start_sale_stock': 20,
        'stock': 5
    })
    for rec in input_records:
        rec = rec.update(DEFAULT_STAGES)
    record = list(stage_mapper(input_records))[0]
    assert record['stage'] == 2
    assert record['stage_day'] == 7
    assert record['stock'] == 5
