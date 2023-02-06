import pytest

from travel.avia.api_gateway.application.fetcher.city_to_landing.mapper import _calc_percent_lower


@pytest.mark.parametrize(
    'min_price, avg_price, expected',
    (
        (90, 100, 10),
        (0, 100, 100),
        (100, 100, 0),
        (1, 100, 99),
    ),
)
def test_calc_percent_lower(min_price, avg_price, expected):
    assert expected == _calc_percent_lower(min_price, avg_price)
