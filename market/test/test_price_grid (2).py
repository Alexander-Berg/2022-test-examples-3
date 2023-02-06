from typing import List, Union
import pandas as pd
from market.forecast.elasticity_price_opt.lib.opt.price_grid import filter_price_grid
from dataclasses import dataclass

Number = Union[int, float]


@dataclass
class PriceTestCase:
    price_grid: List[Number]
    initial_price: Number
    current_price: Number
    expected: List[Number]

    def run(self):
        msku = 1
        price_grid = pd.DataFrame({'msku': [msku] * len(self.price_grid), 'price': self.price_grid})
        prices = pd.DataFrame(
            {'msku': [msku], 'initial_price': [self.initial_price], 'current_price': [self.current_price]}
        )
        result = filter_price_grid(price_grid, prices)
        assert set(result['price']) == set(self.expected)


def test_filter_price_grid_1():
    PriceTestCase(price_grid=[100, 103, 115], initial_price=120, current_price=110, expected=[100, 103]).run()


def test_filter_price_grid_2():
    PriceTestCase(price_grid=[115, 125, 130], initial_price=120, current_price=110, expected=[125, 130]).run()


def test_filter_price_grid_3():
    PriceTestCase(price_grid=[114, 115, 116], initial_price=120, current_price=110, expected=[114, 116]).run()


def test_filter_price_grid_4():
    PriceTestCase(price_grid=[114, 115, 116], initial_price=120, current_price=110, expected=[114, 116]).run()


def test_filter_price_grid_5():
    PriceTestCase(price_grid=[114, 115, 116, 120], initial_price=120, current_price=110, expected=[114, 116, 120]).run()
