# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from logging import Logger

import mock
from mock import Mock
from redis import Redis

from travel.rasp.bus.scripts.cache_reseter.cache_reseter import CacheReseter
from travel.rasp.bus.scripts.cache_reseter.redis_provider import IRedisProvider


class FakeRedisProvider(IRedisProvider):
    def __init__(self, read_redis_cache, write_redis_cache):
        self._read_redis_cache = read_redis_cache
        self._write_redis_cache = write_redis_cache

    def get_read_client(self):
        return self._read_redis_cache

    def get_write_client(self):
        return self._write_redis_cache


class TestCacheReseter(object):
    def setup_method(self, method):
        self._fake_logger = Mock(Logger)

        self._fake_redis = Mock(Redis)
        self._redis_provider = FakeRedisProvider(
            read_redis_cache=None,
            write_redis_cache=self._fake_redis,
        )
        self._fake_cache_reseter = CacheReseter(
            redis_provider=self._redis_provider,
            logger=self._fake_logger,
        )

    def test_reset_segments_for_particular_suppliers(self):
        self._fake_redis.scan_iter = Mock(return_value=[
            'task.fake_ok.segments',
            'task.fake_noy.segments',
            'task.fake_etraffic.segments',
        ])
        supplyers = ['fake_ok', 'fake_noy']
        self._fake_cache_reseter.reset_segments_for(supplyers, dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.*.segments',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_ok.segments',
            ),
            mock.call(
                'task.fake_noy.segments',
            ),
        ]

        assert self._fake_redis.publish.call_args_list == [
            mock.call(
                'channel.task.fake_ok.segments', 'FORCE',
            ),
            mock.call(
                'channel.task.fake_noy.segments', 'FORCE',
            ),
        ]

    def test_reset_segments_for_all_suppliers(self):
        self._fake_redis.scan_iter = Mock(return_value=[
            'task.fake_ok.segments',
            'task.fake_noy.segments',
            'task.fake_etraffic.segments',
        ])
        supplyers = []
        self._fake_cache_reseter.reset_segments_for(supplyers, dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.*.segments',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_ok.segments',
            ),
            mock.call(
                'task.fake_noy.segments',
            ),
            mock.call(
                'task.fake_etraffic.segments',
            ),
        ]

        assert self._fake_redis.publish.call_args_list == [
            mock.call(
                'channel.task.fake_ok.segments', 'FORCE',
            ),
            mock.call(
                'channel.task.fake_noy.segments', 'FORCE',
            ),
            mock.call(
                'channel.task.fake_etraffic.segments', 'FORCE',
            ),
        ]

    def test_dry_reset_segments_for_all_suppliers(self):
        self._fake_redis.scan_iter = Mock(return_value=[
            'task.fake_ok.segments',
            'task.fake_noy.segments',
        ])
        supplyers = ['fake_ok']
        self._fake_cache_reseter.reset_segments_for(supplyers, dry=True)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.*.segments',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
        ]

        assert self._fake_redis.publish.call_args_list == [
        ]

    def test_reset_search_data(self):
        self._fake_redis.scan_iter = Mock(return_value=[
            'task.fake_noy.search:c213:c54:2018-09-01',
        ])
        self._fake_cache_reseter.reset_search_data('fake_noy', 'c213', 'c54', '2018-09-01', dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c54:2018-09-01',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_noy.search:c213:c54:2018-09-01',
            ),
        ]

    def test_reset_search_data_but_key_does_not_exist(self):
        self._fake_redis.scan_iter = Mock(return_value=[
        ])
        self._fake_cache_reseter.reset_search_data('fake_noy', 'c213', 'c54', '2018-09-01', dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c54:2018-09-01',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
        ]

    def test_dry_reset_search_data(self):
        self._fake_redis.scan_iter = Mock(return_value=[
            'task.fake_noy.search:c213:c54:2018-09-01',
        ])
        self._fake_cache_reseter.reset_search_data('fake_noy', 'c213', 'c54', '2018-09-01', dry=True)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c54:2018-09-01',
                count=100,
            )
        ]

        assert self._fake_redis.delete.call_args_list == [
        ]

    def test_reset_search_data_by_point(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                    'task.fake_noy.search:c213:c2:2018-09-01',
                    'task.fake_noy.search:c213:c54:2018-09-02',
                ],
                [
                    'task.fake_noy.search:c2:c213:2018-09-01',
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_point('fake_noy', 'c213', dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:*:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:*:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_noy.search:c213:c2:2018-09-01',
                'task.fake_noy.search:c213:c54:2018-09-02',
                'task.fake_noy.search:c2:c213:2018-09-01',
            ),
        ]

    def test_reset_search_data_by_point_but_keys_does_not_exist(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                ],
                [
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_point('fake_noy', 'c213', dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:*:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:*:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == []

    def test_dry_reset_search_data_by_point(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                    'task.fake_noy.search:c213:c2:2018-09-01',
                    'task.fake_noy.search:c213:c54:2018-09-02',
                ],
                [
                    'task.fake_noy.search:c2:c213:2018-09-01',
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_point('fake_noy', 'c213', dry=True)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:*:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:*:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == []

    def test_reset_search_data_by_one_way_direction(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                    'task.fake_noy.search:c213:c2:2018-09-01',
                    'task.fake_noy.search:c213:c2:2018-09-02',
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_direction('fake_noy', 'c213', 'c2', two_way=False, dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c2:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_noy.search:c213:c2:2018-09-01',
                'task.fake_noy.search:c213:c2:2018-09-02',
            ),
        ]

    def test_reset_search_data_by_two_way_direction(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                    'task.fake_noy.search:c213:c2:2018-09-01',
                    'task.fake_noy.search:c213:c2:2018-09-02',
                ],
                [
                    'task.fake_noy.search:c2:c213:2018-09-01',
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_direction('fake_noy', 'c213', 'c2', two_way=True, dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c2:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:c2:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == [
            mock.call(
                'task.fake_noy.search:c213:c2:2018-09-01',
                'task.fake_noy.search:c213:c2:2018-09-02',
                'task.fake_noy.search:c2:c213:2018-09-01',
            ),
        ]

    def test_reset_search_data_by_direction_but_keys_does_not_exist(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                ],
                [
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_direction('fake_noy', 'c213', 'c2', two_way=True, dry=False)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c2:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:c2:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == [
        ]

    def test_dry_reset_search_data_by_direction(self):
        self._fake_redis.scan_iter = Mock(
            side_effect=[
                [
                    'task.fake_noy.search:c213:c2:2018-09-01',
                    'task.fake_noy.search:c213:c2:2018-09-02',
                ],
                [
                    'task.fake_noy.search:c2:c213:2018-09-01',
                ],
            ]
        )
        self._fake_cache_reseter.reset_search_data_by_direction('fake_noy', 'c213', 'c2', two_way=True, dry=True)
        assert self._fake_redis.scan_iter.call_args_list == [
            mock.call(
                match='task.fake_noy.search:c213:c2:*',
                count=100,
            ),
            mock.call(
                match='task.fake_noy.search:c2:c213:*',
                count=100,
            ),
        ]

        assert self._fake_redis.delete.call_args_list == [
        ]
