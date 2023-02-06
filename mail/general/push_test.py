import unittest

from tools.push.push import parse_xiva_timestamp, appmetrika_tables, xiva_tables, find_subscriptions


class TestPushScript(unittest.TestCase):
    def test_parse_xiva_timestamp(self):
        self.assertEqual('2018-12-13 17:29:40.410607', str(parse_xiva_timestamp('2018-12-13 17:29:40.410607')))

    def test_appmetrica_tables(self):
        self.assertEqual('RANGE([home/logfeller/logs/metrika-mobile-log/30min], "2018-12-16T00:00:00")',
                         appmetrika_tables(date_from='2018-12-16', date_to='2018-12-16'))
        self.assertEqual('RANGE([home/logfeller/logs/metrika-mobile-log/1d], "2018-12-14", "2018-12-15")',
                         appmetrika_tables(date_from='2018-12-14', date_to='2018-12-15'))

    def test_xiva_tables(self):
        self.assertEqual('RANGE([logs/xivahub-log/stream/5min], "2018-12-16T00:00:00")',
                         xiva_tables(corp=False, date_from='2018-12-16', date_to='2018-12-16'))
        self.assertEqual('RANGE([logs/xivahubcorp-log/1d], "2018-12-14", "2018-12-15")',
                         xiva_tables(corp=True, date_from='2018-12-14', date_to='2018-12-15'))

    def test_find_subscriptions(self):
        subscriptions = find_subscriptions('ios', '672771436', True, xiva_token='***')
        for subscription in subscriptions:
            print(subscription)
