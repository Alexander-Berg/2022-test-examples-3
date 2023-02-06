import unittest
from InputParams import InputParams


class TestInputParams(unittest.TestCase):
    def setUp(self):
        self.params = InputParams(
            forecast_start_date='2020-04-01',
            forecast_end_date='2021-05-01',
            history_start_date='2020-01-01',
            history_end_date='2020-03-31',
            dau_white=(5, 10, 1),
            dau_blue=4000000,
            offers_white=2000000,
            offers_blue=0,
            orders_blue=0,
            dc_minus_one=True,
        )

    def test_make_list_from_int(self):
        self.assertEqual(self.params.make_list(42), [42], 'wrong int to list convertion ')

    def test_make_list_fromt_tuple(self):
        self.assertEqual(self.params.make_list((1, 3, 1)), [1, 2, 3], 'wrong tuple to list convertion')

    def test_make_list_fromt_tuple_error(self):
        self.assertRaises(ValueError, self.params.make_list, (1, 3, 1, 4))


if __name__ == '__main__':
    unittest.main()
