# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock

from common.tester.factories import create_thread
from common.tester.testcase import TestCase
from mapping.generators.livemap import save_objects
from mapping.models import LiveBus


class TestLiveMap(TestCase):
    def test_collisions(self):
        objects = [
            LiveBus(thread=create_thread(uid="uid1"), lng=0, lat=0, arrival=datetime(2015, 1, 1),
                    departure=datetime(2015, 1, 1)),
            LiveBus(thread=create_thread(uid="uid2"), lng=0, lat=0, arrival=datetime(2015, 1, 1),
                    departure=datetime(2015, 1, 1)),
        ]

        LiveBus.objects.bulk_create(objects)

        hash1 = mock.Mock()
        hash2 = mock.Mock()

        hash1.digest.side_effect = ["0001"]
        hash2.digest.side_effect = ["0001", "0002"]

        with mock.patch('hashlib.md5', side_effect=[hash1, hash2]) as m:
            save_objects(LiveBus.objects.all(), LiveBus)

        assert m.mock_calls == [
            mock.call(),
            mock.call(),
        ]

        assert hash1.mock_calls == [
            mock.call.update('uid1'),
            mock.call.update('2015-01-01'),
            mock.call.digest()
        ]

        assert hash2.mock_calls == [
            mock.call.update('uid2'),
            mock.call.update('2015-01-01'),
            mock.call.digest(),
            mock.call.update(b'\xff'),
            mock.call.digest(),
        ]
