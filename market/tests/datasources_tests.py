import unittest
from unittest.mock import MagicMock
from datetime import datetime as dt

from time_period import TimePeriod
from yt_datasource import YtDatasource, YtDatasourceError
from yt_table import YtTable
from yt_table_set import YtTableSet
from forecast_yt_client import ForecastYtClient


class Tests(unittest.TestCase):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.time_period = TimePeriod(start_date='2020-01-01', end_date='2020-01-03')
        self.yt_mock = ForecastYtClient('no_cluster')
        self.test_await_time = 1e-3

    def test_init(self):
        self.assertEqual(
            TimePeriod(start_date='2021-01-01', end_date='2021-01-31').end_date,
            TimePeriod(start_date='2021-01-01', today='2021-02-02', lag_days=2).end_date
        )
        with self.assertRaises(ValueError):
            TimePeriod()
        with self.assertRaises(ValueError):
            TimePeriod(start_date='2021-01-01', end_date='2020-01-01')
        with self.assertRaises(ValueError):
            TimePeriod(start_date='2021-01-01', lag_days=2),
        with self.assertRaises(ValueError):
            TimePeriod(start_date='2021-01-01', lag_days=2, date_format='not_supported_format')

    def test_explode(self):
        self.assertEqual(
            self.time_period.explode(),
            {dt(2020, 1, 1), dt(2020, 1, 2), dt(2020, 1, 3)}
        )

    def test_strpdate(self):
        self.assertEqual(
            self.time_period.strpdate('2020-01-01'),
            dt(2020, 1, 1)
        )
        self.assertEqual(
            self.time_period.strpdate(dt(2020, 1, 1)),
            dt(2020, 1, 1)
        )
        self.assertEqual(
            TimePeriod(start_date='2020-01-01', end_date='2020-02-01').strpdate('2022-11-15'),
            dt(2022, 11, 15)
        )

        self.assertEqual(
            TimePeriod(start_date='2020-01-01', end_date='2020-02-01', date_format='%Y-%m').strpdate(dt(2050, 10, 5)),
            dt(2050, 10, 5)
        )

    def test_strfdate(self):
        self.assertEqual(
            self.time_period.strfdate(dt(2050, 10, 5)),
            '2050-10-05'
        )

        self.assertEqual(
            TimePeriod(start_date='2020-01-01', end_date='2020-02-01', date_format='%Y-%m').strfdate(dt(2050, 10, 5)),
            '2050-10'
        )

    def test_single_table_init(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        source = YtDatasource(YtTable('//path/to/table'), self.yt_mock)
        self.assertEqual(str(source), '//path/to/table')

        self.yt_mock.exists = MagicMock(return_value=False)
        with self.assertRaises(YtDatasourceError):
            YtDatasource(YtTable('//path/to/table'), self.yt_mock)

    def test_init_0(self):

        with self.assertRaises(ValueError):
            YtDatasource(YtTableSet('//path/to/table'), self.yt_mock)

    def test_init_1(self):

        with self.assertRaises(ValueError):
            YtDatasource(YtTableSet('//path/to/table/{date}'), self.yt_mock)

    def test_init_2(self):

        with self.assertRaises(YtDatasourceError):
            self.yt_mock.list = MagicMock(return_value=[])
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                time_period=TimePeriod(start_date='2020-01-01', end_date='2021-01-01')
            )
        self.yt_mock.list.assert_called_with('//path/to/table')

    def test_init_3(self):

        self.yt_mock.list = MagicMock(return_value=['2021-01-01', '2021-01-02'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}/suffix', yt_client=self.yt_mock),
            self.yt_mock,
            time_period=TimePeriod(start_date='2021-01-01', end_date='2021-01-02')
        )
        self.assertEqual(
            set(source.to_list()),
            {'//path/to/table/2021-01-01/suffix', '//path/to/table/2021-01-02/suffix'}
        )
        self.yt_mock.list.assert_called_with('//path/to/table')

    def test_init_4(self):

        self.yt_mock.list = MagicMock(return_value=['2021-01-01'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            time_period=TimePeriod(start_date='2021-01-01', end_date='2021-01-01')
        )
        self.assertEqual(
            str(source),
            '//path/to/table/2021-01-01'
        )

    def test_init_5(self):
        with self.assertRaises(ValueError):
            YtDatasource(
                YtTableSet('//path/to/table/{date}', 'blablabla', yt_client=self.yt_mock),
                self.yt_mock,
                time_period=TimePeriod(start_date='2021-01-01', end_date='2021-01-01')
            )

    def test_init_6(self):

        with self.assertRaises(ValueError):
            YtDatasource(
                YtTableSet('//path/to/table/{date}', 'blablabla', yt_client=self.yt_mock),
                self.yt_mock,
                time_period=TimePeriod(start_date='2021-01-01', end_date='2021-01-01')
            )

    def test_await_0(self):

        self.yt_mock.exists = MagicMock(side_effect=[False, False, False, True])
        YtDatasource(
            YtTable('//path/to/table'),
            self.yt_mock,
            await_source=True,
            await_params={'sleep_time': self.test_await_time, 'max_retries': 3},
            time_period=TimePeriod(start_date='2021-01-01', end_date='2021-01-01')
        )

    def test_await_1(self):

        self.yt_mock.exists = MagicMock(side_effect=[False, False, False, True])
        self.yt_mock.list = MagicMock(side_effect=[[], [], [], [], ['2020-02-01'], ['2020-02-01']])
        YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            await_source=True,
            await_params={'sleep_time': self.test_await_time, 'max_retries': 4},
            time_period=TimePeriod(start_date='2020-02-01', end_date='2020-02-01')
        )

    def test_await_2(self):

        with self.assertRaises(YtDatasourceError):

            self.yt_mock.list = MagicMock(side_effect=[[]]*6)
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                await_source=True,
                await_params={'sleep_time': self.test_await_time, 'max_retries': 3},
                time_period=TimePeriod(start_date='2020-02-01', end_date='2020-02-01')
            )

    def test_fallback_0(self):
        self.yt_mock.list = MagicMock(return_value=['2015-01-30'])
        YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            fallback_strategy='reuse_last',
            time_period=TimePeriod(start_date='2015-02-01', end_date='2015-02-01', fallback_days=2)
        )

    def test_fallback_1(self):
        with self.assertRaises(YtDatasourceError):
            self.yt_mock.list = MagicMock(return_value=['2016-01-30'])
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                fallback_strategy='reuse_last',
                time_period=TimePeriod(start_date='2016-02-01', end_date='2016-02-01', fallback_days=1)
            )

    def test_fallback_2(self):

        self.yt_mock.list = MagicMock(return_value=['2016-01-30', '2016-01-31', '2016-02-01'])
        YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            fallback_strategy='reuse_last',
            time_period=TimePeriod(start_date='2016-02-01', end_date='2016-02-02', fallback_days=1)
        )

    def test_fallback_3(self):
        with self.assertRaises(YtDatasourceError):
            self.yt_mock.list = MagicMock(return_value=['2016-12-30', '2016-12-31'])
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                time_period=TimePeriod(start_date='2016-01-01', end_date='2016-02-01', fallback_days=0)
            )

    def test_fallback_5(self):
        self.yt_mock.exists = MagicMock(side_effect=[False, False, False, True])
        self.yt_mock.list = MagicMock(return_value=['2018-01-30', '2018-01-31', '2018-02-01'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}/test', yt_client=self.yt_mock),
            self.yt_mock,
            await_source=True,
            await_params={'sleep_time': self.test_await_time, 'max_retries': 4},
            time_period=TimePeriod(start_date='2018-02-01', end_date='2018-02-02', fallback_days=1)
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2018-02-01/test']
        )

    def test_fallback_6(self):
        with self.assertRaises(YtDatasourceError):
            self.yt_mock.exists = MagicMock(side_effect=[False, False, False, True])
            self.yt_mock.list = MagicMock(return_value=['2018-01-29', '2018-01-30', '2018-01-31'])
            YtDatasource(
                YtTableSet('//path/to/table/{date}/test', yt_client=self.yt_mock),
                self.yt_mock,
                await_source=True,
                await_params={'sleep_time': self.test_await_time, 'max_retries': 4},
                time_period=TimePeriod(start_date='2018-02-01', end_date='2018-02-02', fallback_days=0)
            )

    def test_fallback_7(self):
        self.yt_mock.exists = MagicMock(side_effect=[False, False, False, True])
        self.yt_mock.list = MagicMock(return_value=['2019-01-25', '2019-01-30', '2019-01-31', 'random_name'])
        YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            fallback_strategy='reuse_last',
            time_period=TimePeriod(start_date='2019-02-01', end_date='2019-02-02', fallback_days=5)
        )

    def test_monthly_0(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2019-01', '2019-02'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            date_key='date',
            time_period=TimePeriod(start_date='2019-02-15', end_date='2019-02-15', date_format='%Y-%m', kind='monthly')
        )
        self.assertEqual(
            source._dates,
            ['2019-02']
        )

    def test_monthly_1(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2019-01', '2019-02', '2019-03'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            date_key='date',
            time_period=TimePeriod(
                start_date='2019-02-15',
                end_date='2019-03-15',
                date_format='%Y-%m',
                kind='monthly'
            )
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2019-02', '//path/to/table/2019-03']
        )

    def test_weekly_0(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2021-07-26', '2021-08-02', '2021-08-09'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            date_key='date',
            time_period=TimePeriod(
                start_date='2021-08-01',
                end_date='2021-08-09',
                date_format='%Y-%m-%d',
                kind='weekly'
            )
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2021-07-26', '//path/to/table/2021-08-02', '//path/to/table/2021-08-09']
        )

    def test_weekly_1(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2021-07-30', '2021-08-06', '2021-08-13'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            date_key='date',
            time_period=TimePeriod(
                start_date='2021-08-01',
                end_date='2021-08-09',
                date_format='%Y-%m-%d',
                kind='weekly',
                first_week_day=4  # Пт
            )
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2021-07-30', '//path/to/table/2021-08-06']
        )

    def test_monthly_fallback_0(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2019-01'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            fallback_strategy='reuse_last',
            date_key='date',
            time_period=TimePeriod(
                start_date='2019-02-01',
                end_date='2019-02-05',
                date_format='%Y-%m',
                kind='monthly',
                fallback_days=5
            )
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2019-01']
        )

    def test_monthly_fallback_1(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2019-01'])
        with self.assertRaises(YtDatasourceError):
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                date_key='date',
                fallback_strategy='reuse_last',
                time_period=TimePeriod(
                    start_date='2019-02-01',
                    end_date='2019-02-05',
                    date_format='%Y-%m',
                    kind='monthly',
                    fallback_days=4
                )
            )

    def test_weekly_fallback_0(self):
        self.yt_mock.exists = MagicMock(return_value=True)
        self.yt_mock.list = MagicMock(return_value=['2021-07-30', '2021-08-06'])
        source = YtDatasource(
            YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
            self.yt_mock,
            date_key='date',
            time_period=TimePeriod(
                start_date='2021-08-01',
                end_date='2021-08-09',
                date_format='%Y-%m-%d',
                kind='weekly',
                fallback_days=2,
                first_week_day=4  # Пт
            )
        )
        self.assertEqual(
            source.to_list(),
            ['//path/to/table/2021-07-30', '//path/to/table/2021-08-06']
        )

    def test_weekly_fallback_1(self):
        with self.assertRaises(YtDatasourceError):
            self.yt_mock.exists = MagicMock(return_value=True)
            self.yt_mock.list = MagicMock(return_value=['2021-07-30', '2021-08-06'])
            YtDatasource(
                YtTableSet('//path/to/table/{date}', yt_client=self.yt_mock),
                self.yt_mock,
                fallback_strategy='reuse_last',
                date_key='date',
                time_period=TimePeriod(
                    fallback_days=2,
                    start_date='2021-08-01',
                    end_date='2021-08-15',
                    kind='weekly',
                    first_week_day=4  # Пт
                )
            )


if __name__ == '__main__':
    unittest.main()
