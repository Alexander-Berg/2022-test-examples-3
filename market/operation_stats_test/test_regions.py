from market.dynamic_pricing.pricing.regional_stats.operation_stats.market_stats import map_index_regionally_wrapper


def test_map_index_regionally():
    count = 0
    rus_region_cast_dict = {
        1: [1],    # Регион сам по себе
        5: [2],    # Например, район Питера кастуется в Питер
        8: [1, 2]  # Вся Россия кастуется в регионы
    }
    index_records = [
        {
            'market_sku': 1,
            'warehouse_id': 1,
            'regions': b'1 5 8',
            'price': b'RUR 1230000000',
            'is_blue_offer': True,
            'is_dsbs': True,
        }
    ]
    map_index_regionally = map_index_regionally_wrapper(rus_region_cast_dict)
    index_mapped = map_index_regionally(index_records)
    for r in index_mapped:
        count += 1
        assert r['market_sku'] == 1
        assert r['warehouse_id'] == 1
        assert r['price'] == 123
        assert r['is_blue_offer'] is True
        assert r['is_dsbs'] is True
        assert r['region_id'] in (1, 2)
    assert count == 4
