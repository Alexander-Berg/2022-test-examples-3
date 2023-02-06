from market.idx.export.awaps.prepare_partition.lib.partition import create_category_to_part_map_group
# import pytest


def test_catstream_no_oversized_ok():
    max_offers_in_part = 10
    catstream_map = {
        1: "DIR",
        2: "Auto",
        3: "DIR",
        4: "Auto",
        5: "DIR",
        6: "Auto"
    }
    category_to_offers_num = {
        1: 7,
        2: 3,
        3: 2,
        4: 3,
        5: 9,
        6: 8
    }
    expected_partition_order_group2 = {
        6: 0,
        2: 1,
        4: 1,
        5: 2,
        1: 3,
        3: 3
    }
    assert create_category_to_part_map_group(max_offers_in_part, category_to_offers_num, catstream_map) == expected_partition_order_group2
