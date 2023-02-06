import pytest

from hamcrest import (
    assert_that,
    equal_to,
)

from market.idx.marketindexer.marketindexer import dropped_offers


@pytest.fixture(
    scope='module',
    params=[
        {'last_table_size': 100, 'probability': 1,    'generations': 1,  'rot_to_pos': 1,    'expected': 100},
        {'last_table_size': 100, 'probability': 0.1,  'generations': 1,  'rot_to_pos': 1,    'expected': 1000},
        {'last_table_size': 100, 'probability': 0.01, 'generations': 1,  'rot_to_pos': 1,    'expected': 10000},
        {'last_table_size': 0,   'probability': 1,    'generations': 1,  'rot_to_pos': 1,    'expected': 0},
        {'last_table_size': 100, 'probability': 0.1,  'generations': 10, 'rot_to_pos': 1,    'expected': 125},
        {'last_table_size': 100, 'probability': 0.1,  'generations': 6,  'rot_to_pos': 1.25, 'expected': 183},
    ]
)
def pos_size_params(request):
    return request.param


def test_pos_daily_size(pos_size_params):
    table_size = dropped_offers.pos_daily_size(
        last_median_dropped_table_size=pos_size_params['last_table_size'],
        probability=pos_size_params['probability'],
        num_generations=pos_size_params['generations'],
        rot_to_pos=pos_size_params['rot_to_pos'],
    )
    assert_that(table_size, equal_to(pos_size_params['expected']))
