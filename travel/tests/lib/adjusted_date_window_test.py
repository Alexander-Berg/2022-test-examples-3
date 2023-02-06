from datetime import date
from unittest import TestCase

from mock import patch

from travel.avia.price_index.lib.adjusted_date_window import AdjustedDateWindow


class DateRangeIteratorTest(TestCase):
    def test_day_close_to_today(self):
        with patch('travel.avia.price_index.lib.adjusted_date_window.date') as mock_date:
            mock_date.today.return_value = date(2018, 6, 7)
            adjusted_date_window = AdjustedDateWindow(day=date(2018, 6, 10), window_size=10)
            assert adjusted_date_window.left_boundary == date(2018, 6, 7)
            assert adjusted_date_window.right_boundary == date(2018, 6, 27)

    def test_day_far_in_the_future(self):
        with patch('travel.avia.price_index.lib.adjusted_date_window.date') as mock_date:
            mock_date.today.return_value = date(2018, 6, 7)
            adjusted_date_window = AdjustedDateWindow(day=date(2018, 9, 10), window_size=10)
            assert adjusted_date_window.left_boundary == date(2018, 8, 31)
            assert adjusted_date_window.right_boundary == date(2018, 9, 20)
