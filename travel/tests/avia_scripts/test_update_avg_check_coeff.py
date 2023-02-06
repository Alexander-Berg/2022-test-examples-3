from unittest import TestCase

from datetime import date

from travel.avia.admin.avia_scripts.update_avg_check_coeff import (
    _update_daily_numbers_for_yt_table,
    calculate_weighted_avg_per_category,
    last_day_results,
    DailyCoeff,
)


class TestLastDayResults(TestCase):
    def test(self):
        coeffs_per_category = {
            'category1': {
                2: 0.2,
                3: 0.3,
                1: 0.1,
            },
            'category2': {
                '4': 0.4,
                '2': 1.2,
                '3': 1.3,
                '1': 1.1,
            },
        }

        expected = {
            'category1': 0.3,
            'category2': 0.4,
        }

        assert expected == last_day_results(coeffs_per_category)


class FakeYtTable:
    def __init__(self, rows):
        self.rows = rows


class TestUpdateDailyNumbers(TestCase):
    def test(self):
        # label_category, order_date, order_per_pax, day_coeff
        table = FakeYtTable(
            [
                ('redirect-type-1', date(2022, 6, 1), None, 1.5),
                ('redirect-type-1', date(2022, 6, 2), None, 1.24),
                ('redirect-type-1', date(2022, 6, 3), None, 0.68),
            ]
        )
        expected = {
            'redirect-type-1': {
                date(2022, 6, 2): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 1), 1.5),
                ],
                date(2022, 6, 3): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 1), 1.5),
                    DailyCoeff('redirect-type-1', date(2022, 6, 2), 1.24),
                ],
                date(2022, 6, 4): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 2), 1.24),
                    DailyCoeff('redirect-type-1', date(2022, 6, 3), 0.68),
                ],
            },
        }
        assert expected == _update_daily_numbers_for_yt_table(None, table, 2)


class TestCalculateWeightedAvgPerCategory(TestCase):
    def test(self):
        daily_numbers = {
            'redirect-type-1': {
                date(2022, 6, 2): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 1), 1.6),
                ],
                date(2022, 6, 3): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 1), 1.6),
                    DailyCoeff('redirect-type-1', date(2022, 6, 2), 1.24),
                ],
                date(2022, 6, 4): [
                    DailyCoeff('redirect-type-1', date(2022, 6, 2), 1.34),
                    DailyCoeff('redirect-type-1', date(2022, 6, 3), 0.68),
                ],
            },
        }
        output_date_start = date(2022, 6, 3)
        orders_window = 2

        expected = {
            'redirect-type-1': {
                date(2022, 6, 3): 1.36,  # (1.24*64/127+1.6*32/127)/(96/127) = (1.24+1.24+1.6)/3
                date(2022, 6, 4): 0.9,   # (0.68+0.68+1.34)/3
            },
        }

        assert expected == calculate_weighted_avg_per_category(daily_numbers, output_date_start, orders_window, date(2022, 6, 4))

        cut_by_date = {
            'redirect-type-1': {
                date(2022, 6, 3): 1.36,  # (1.24*64/127+1.6*32/127)/(96/127) = (1.24+1.24+1.6)/3
            },
        }

        assert cut_by_date == calculate_weighted_avg_per_category(daily_numbers, output_date_start, orders_window, date(2022, 6, 3))
