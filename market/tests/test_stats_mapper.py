from market.dynamic_pricing.pricing.dynamic_pricing.prepare_stats.price_stats import (
    stats_mapper_by_white_minimum_count,
)


def test_when_ref_min_price_warning_below_max_then_print_url():
    input_data = [
        {
            'ref_min_price': 500,
            'ref_min_soft_price': 0,
            'white_median_price': 0,
            'ref_median_price': 0,
            'ref_min_regular_price': 0,
            'white_min_regular_price': 0,
            'white_min_price': 0,
            'ozon_min_price': 0,
            'blue_min_3p_price': 0,
            'ref_min_price_warning': 100,
            'ref_min_soft_price_warning': 0,
            'white_median_price_warning': 0,
            'ref_median_price_warning': 0,
            'ref_min_regular_price_warning': 0,
            'white_min_regular_price_warning': 0,
            'white_min_price_warning': 0,
            'ozon_min_price_warning': 0,
            'blue_min_3p_price_warning': 0,
            'url': 'example.com',
            'url_soft': 'example.com/soft',
            'url_ozon': 'ozon.ru/abc',
            'max_warning_lvl': 200,
            'white_cnt_offers': 0,
            'white_median_price': 0
        }
    ]

    stats_mapper = stats_mapper_by_white_minimum_count(-1)
    rec = list(stats_mapper(input_data))[0]
    assert (rec.ref_min_price, rec.url) == (500, 'example.com')


def test_when_ref_min_price_warning_above_max_then_dont_print_url():
    input_data = [
        {
            'ref_min_price': 100,
            'ref_min_soft_price': 0,
            'white_median_price': 0,
            'ref_median_price': 0,
            'ref_min_regular_price': 0,
            'white_min_regular_price': 0,
            'white_min_price': 0,
            'ozon_min_price': 0,
            'blue_min_3p_price': 0,
            'ref_min_price_warning': 100,
            'ref_min_soft_price_warning': 0,
            'white_median_price_warning': 0,
            'ref_median_price_warning': 0,
            'ref_min_regular_price_warning': 0,
            'white_min_regular_price_warning': 0,
            'white_min_price_warning': 0,
            'ozon_min_price_warning': 0,
            'blue_min_3p_price_warning': 0,
            'url': 'example2.com',
            'url_soft': 'example2.com/soft',
            'url_ozon': 'ozon.ru/abc',
            'max_warning_lvl': 50,
            'white_cnt_offers': 0,
            'white_median_price': 0
        }
    ]

    stats_mapper = stats_mapper_by_white_minimum_count(-1)
    rec = list(stats_mapper(input_data))[0]
    assert (rec.ref_min_price, rec.url) == (None, None)
