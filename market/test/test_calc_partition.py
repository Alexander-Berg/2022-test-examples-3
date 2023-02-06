from market.idx.export.awaps.prepare_partition.lib.partition import create_category_to_part_map
import pytest


def test_no_oversized_ok():
    max_offers_in_part = 10
    category_to_offers_num = {
        1: 10,
        2: 3,
        7: 5,
        0: 3
    }
    expected_partition = {
        1: 0,
        2: 1,
        7: 1,
        0: 2
    }
    assert create_category_to_part_map(max_offers_in_part, category_to_offers_num) == expected_partition


def test_with_oversized_ok():
    max_offers_in_part = 10
    category_to_offers_num = {
        1: 20,
        2: 3,
        7: 50,
        0: 3,
        8: 4
    }
    expected_partition = {
        2: 0,
        0: 0,
        8: 0,
        1: 2,
        7: 1
    }
    assert create_category_to_part_map(max_offers_in_part, category_to_offers_num) == expected_partition


def test_consistent():
    max_offers_in_part = 10
    category_to_offers_num = {
        2: 10,
        1: 10,
        3: 10,
    }
    expected_partition = {
        2: 0,
        1: 1,
        3: 2
    }
    assert create_category_to_part_map(max_offers_in_part, category_to_offers_num) == expected_partition


def test_invalid_max_offers_exception():
    max_offers_in_part = 0
    with pytest.raises(ValueError):
        create_category_to_part_map(max_offers_in_part, {1: 1})
