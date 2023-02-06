# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime
from itertools import compress

import pytest

from travel.proto.dicts.buses.common_pb2 import EPointKeyType
from travel.buses.connectors.tests.yabus.common.library.test_utils import supplier_provider_patch, \
    matching_provider_patch

from yabus.common.respfilters import coherence


class TestRespfilter(object):
    @pytest.mark.parametrize('from_uid, to_uid, from_sid, to_sid, date, rides, passages', [
        (
            's1', 's2', 'ps1', 'ps2', datetime(2019, 1, 1),
            [
                {'from': {'id': 's1'}, 'to': {'id': 's2'}, 'departure': '2019-01-01T00:32'},
                {'from': {'id': 's1'}, 'to': {'id': 's3'}, 'departure': '2019-01-01T00:32'},
                {'from': {'id': 's2'}, 'to': {'id': 's1'}, 'departure': '2019-01-01T00:32'},
                {'from': {'id': 's1'}, 'to': {'id': 's2'}, 'departure': '2019-01-02T00:32'},
            ],
            [
                True,
                False,
                False,
                False,
            ],
        ),
        (
            'c3', 'c4', 'ps3', 'ps4', datetime(2020, 11, 12),
            [
                {'from': {'id': 's3'}, 'to': {'id': 's4'}, 'departure': '2020-11-12T10:32'},
                {'from': {'id': 's3'}, 'to': {'id': 's4'}, 'departure': '2020-11-13T10:32'},
            ],
            [
                True,
                False,
            ],
        ),
        (  # https://st.yandex-team.ru/BUSES-1602
            's5', 'c6', 'pc5', 'pc6', datetime(2020, 11, 12),
            [
                {'from': {'id': 's5_1'}, 'to': {'id': 's6_1'}, 'departure': '2020-11-12T10:32'},
                {'from': {'id': 's3'}, 'to': {'id': 's4'}, 'departure': '2020-11-12T11:32'},
                {'from': {'id': 's5'}, 'to': {'id': 's6_1'}, 'departure': '2020-11-12T10:32'},
                {'from': {'id': 's5'}, 'to': {'id': None}, 'departure': '2020-11-12T10:32'},
                {'from': {'id': None}, 'to': {'id': "s6_1"}, 'departure': '2020-11-12T10:32'},
            ],
            [
                True,
                True,
                True,
                True,
                True,
            ],
        ),
        (
            'с7', 'c8', 'ps7', 'ps8', datetime(2020, 11, 12),
            [
                {'from': {'id': 's5_1'}, 'to': {'id': 's6_1'}, 'departure': '2020-11-12T10:32'},
                {'from': {'id': 's3'}, 'to': {'id': 's4'}, 'departure': '2020-11-12T11:32'},
            ],
            [
                True,
                True,
            ],
        ),
    ])
    def test_coherence(self, from_uid, to_uid, from_sid, to_sid, date, rides, passages):
        with supplier_provider_patch('supplier_code'), \
            matching_provider_patch({
                's1': [('ps1', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's2': [('ps2', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c3': [('ps3', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c4': [('ps4', EPointKeyType.POINT_KEY_TYPE_STATION)],
                's5': [('pc5', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
                'c6': [('pc6', EPointKeyType.POINT_KEY_TYPE_SETTLEMENT)],
                'c7': [('ps7', EPointKeyType.POINT_KEY_TYPE_STATION)],
                'c8': [('ps8', EPointKeyType.POINT_KEY_TYPE_STATION)],
            },
        ):
            assert coherence('supplier_code', from_uid, to_uid, from_sid, to_sid, date, rides) == \
                   list(compress(rides, passages))
