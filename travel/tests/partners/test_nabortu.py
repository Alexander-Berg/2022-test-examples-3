# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant,
)
from travel.avia.ticket_daemon.ticket_daemon.partners import nabortu


@mock.patch('requests.post', return_value=get_mocked_response('nabortu.xml'))
def test_nabortu_query(mocked_request):
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(**{
                'fare_code': 'OSALE',
                'station_from_iata': 'SVO',
                'local_departure': datetime.datetime(2017, 1, 21, 23, 30),
                'company_iata': 'IK',
                'number': 'IK 8173',
                'local_arrival': datetime.datetime(2017, 1, 22, 3, 30),
                'station_to_iata': 'SVX',
            })],
            backward=[create_flight(**{
                'fare_code': 'O',
                'station_from_iata': 'SVX',
                'local_departure': datetime.datetime(2017, 1, 24, 7, 30),
                'company_iata': 'DP',
                'number': 'DP 406',
                'local_arrival': datetime.datetime(2017, 1, 24, 8, 5),
                'station_to_iata': 'VKO',
            })],
            klass='economy',
            order_data={'proposal': 'eJylk9tuozAQhl8F+bZb1QYToCqRCAQ1PUC2sNnerVziJFYJRsa0ap9+x0k2h+5J2uUGz4znn8MH\nVxPN19ZUiYqHyAl8/AljZE26WK6fRMPnISpVz5E1Y7WYMy1VLOdwE1maKbFYfGPzvtaQ6dAAWdWK\nKc1ViBas7vhGZuspWl6lTEFiuo0kQvFKC9mUby14szwbIyvr108mGxq4lprXM9ZXq4Pc8CqtxXKl\nu+HVpultJ5NbZBnpjK3ByovobqsUIp94DrJyJZaiCVExy6Es77RomClsPI/G00KHIbIx8c4xObdJ\naTuXDr40a4iUEi/8KGiXeB+840uIkJsou8+/brQeZK/5B1/Sq125aWlfH+z7IkSejc0DdYRqa9ZA\nLpye+s6KHJsgq3jrQG3zhnmjquJdNwEiBJux5lxtxgSj4Ms1b7SJgTWS8lk0y7hmHeTD1HnLTVFw\n7Te24zIDiGyfOK0ZlAgR/XHc3geFklerQss2ln2jN3dj2SyEWpsPZAe7EEuYMgjSEU1oEBB3RFNs\n217gp8Qd0BF2Yt+P/epz/yqe5V02tQc0b/X7l7Oz1zBEFydUk+kJ1R1RigfHQB8/AJ3d5r8ASkvs\n/RYoBP1L7B4BpQAPpIHfAeix7xSo496fMg2cn5iOJIfdW57jnfsm8i9Yyd+xmpX9EWvwH1hTNw3s\nxBmMBx4djClJ6dh3YptEqesGcZK49PrhvVBd9g75s/pmQoKzPNpivdj/theG8PA7zk1XZg==\n', 'aviasearch_params': 'KGRwMQpTJ2ZhcmUnCnAyClMnRWNvbm9teScKcDMKc1MnaW5mYW50cycKcDQKSTAKc1MncmV0dXJu\nX2RhdGUnCnA1ClMnMjAxNy0wMS0yNCcKcDYKc1MnYWR1bHRzJwpwNwpJMQpzUydmb3J3YXJkX2Rh\ndGUnCnA4ClMnMjAxNy0wMS0yMScKcDkKc1MnY2hpbGRyZW4nCnAxMApJMApzUydpYXRhX3RvJwpw\nMTEKUydTVlgnCnAxMgpzUydpYXRhX2Zyb20nCnAxMwpTJ01PVycKcDE0CnMu\n'},  # noqa
            tariff=Price(currency='RUR', value=3980.0)
        ),
        ComparableVariant(
            forward=[
                create_flight(**{
                    'fare_code': 'LLTRT',
                    'station_from_iata': 'VKO',
                    'local_departure': datetime.datetime(2017, 1, 21, 10, 40),
                    'company_iata': 'UT',
                    'number': 'UT 161',
                    'local_arrival': datetime.datetime(2017, 1, 21, 12, 10),
                    'station_to_iata': 'GOJ',
                }),
                create_flight(**{
                    'fare_code': 'LLTRT',
                    'station_from_iata': 'GOJ',
                    'local_departure': datetime.datetime(2017, 1, 21, 13, 25),
                    'company_iata': 'UT',
                    'number': 'UT 112',
                    'local_arrival': datetime.datetime(2017, 1, 21, 19, 45),
                    'station_to_iata': 'SVX',
                }),
            ],
            backward=[
                create_flight(**{
                    'fare_code': 'LLTRT',
                    'station_from_iata': 'SVX',
                    'local_departure': datetime.datetime(2017, 1, 24, 10, 5),
                    'company_iata': 'UT',
                    'number': 'UT 111',
                    'local_arrival': datetime.datetime(2017, 1, 24, 12, 35),
                    'station_to_iata': 'GOJ',
                }),
                create_flight(**{
                    'fare_code': 'LLTRT',
                    'station_from_iata': 'GOJ',
                    'local_departure': datetime.datetime(2017, 1, 24, 13, 45),
                    'company_iata': 'UT',
                    'number': 'UT 162',
                    'local_arrival': datetime.datetime(2017, 1, 24, 15, 10),
                    'station_to_iata': 'VKO',
                }),
            ],
            klass='economy',
            order_data={'proposal': 'eJzFlltzmkAUx78Ks8/NuBcWxJHMIJdq4m2E2L51CK7KFMHCmpl8+x7AeEtM0mqnvsCe3T0Xfv85\nx3ZPipUyzuNImIhoWGdfiI6UXmFnq8c4FTMTeWFSCKRMwySehTLL7WwGZx8CpMgwj+fzH+Fsk0gT\nNWkTIyVahrkUuYnm9TXwVFv8tYi8MBc7h06ci0jGWRo8r8E6HA1dpAw3q8fyNiUqUrqZFMk03ETL\nvcPbtpfEi6UsbttV6vtsSufDcAWrfj+YBJWvsiaClFEeL+LURNP7EcQVhYzTsIxsoq+ju9KyhhQh\nKCb6DSY3lAQEt1TcwlCQlefxkzjepC1Sb/bFosz1zhoORt/86XekTLKNFEc2qsIrPGEFoTb5NvI4\nIF2GB3vTwDcRV3H5g6hxvk7CFDxZwUTRKVL858JErHpC3VYUiaLoAR6jLG8m8qpauOmLxUqkstyC\nVSfLfsbpwk7CAq734exalOHAtPtwW0BToBnuLo6TECJU7uvX+jzkG4ho6ctsbWebVFZn7Sydx/mq\nFMuWuh8voESnoxk2Y8xzHd3Alsc6doe7uqE7lsGwawePM33Fc2vyy99QjUi5HD8tLdNEjc/CJXQP\nd4vyAG4F5C24rEX5WbhGS+WXw1W79BQu4dpldMnV6fKP6JLzdD0du6qm2i4FmLqrM2o7VLMsi7gd\ntaPSs3Rv2y9Beuk8K46XWyR1PvcP3gsjJ5QnnDSAVHGqCb91gFcHQE2Nk4iNUl6fFtlBB9lK6sMO\nopYdBJ8RmVp2EHYoskMV/ZHIXnUQotFrthDyv0VWfjSONUZsmxq0ybHnuLbuqYRRCzv6PxUZcCIt\nws+LrAJZU75UZNq7nWw7uF6LjL00q7dExo/H1F+KjHQpPx1T5JqN7Boau2RMEcdrOqpOKW161VwC\nkbmGS3TMscExe29MNXb/RmravwG0gskD\n', 'aviasearch_params': 'KGRwMQpTJ2ZhcmUnCnAyClMnRWNvbm9teScKcDMKc1MnaW5mYW50cycKcDQKSTAKc1MncmV0dXJu\nX2RhdGUnCnA1ClMnMjAxNy0wMS0yNCcKcDYKc1MnYWR1bHRzJwpwNwpJMQpzUydmb3J3YXJkX2Rh\ndGUnCnA4ClMnMjAxNy0wMS0yMScKcDkKc1MnY2hpbGRyZW4nCnAxMApJMApzUydpYXRhX3RvJwpw\nMTEKUydTVlgnCnAxMgpzUydpYXRhX2Zyb20nCnAxMwpTJ01PVycKcDE0CnMu\n'},  # noqa
            tariff=Price(currency='RUR', value=16073.17)
        ),
    ]

    test_query = get_query()
    variants = list(nabortu.query(test_query))
    assert_variants_equal(expected_variants, variants[0])
