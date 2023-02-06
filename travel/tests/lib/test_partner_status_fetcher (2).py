# -*- coding: utf-8 -*-
import functools

from mock import Mock, patch

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.jsonrpc.lib.result import collect_statuses
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.library.python.tester.testcase import TestCase


def fake_get_many(mock_statuses, keys):
    result = {}
    for p_code, value in mock_statuses.items():
        for key in keys:
            if p_code in key:
                result[key] = value
    return result


def fake_unpack(packed_value):
    if packed_value == 'raise_exception':
        raise Exception('can\'t deserialize status')
    return packed_value


class CollectStatusTests(TestCase):
    def setUp(self):
        reset_all_caches()
        self.query = create_query()

        self._mock_statuses = {}
        self._patchers = {
            'deserialize': patch('travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.utils._deserialize', side_effect=fake_unpack),
            'get_many': patch('travel.avia.ticket_daemon_api.jsonrpc.lib.caches.shared_cache.get_many', side_effect=functools.partial(fake_get_many, self._mock_statuses)),
        }
        for p in self._patchers.values():
            p.start()

        self.partners = self.p1, self.p2, self.p3 = [
            create_partner(code='p1'),
            create_partner(code='p2'),
            create_partner(code='p3'),
        ]

    def tearDown(self):
        for p in self._patchers.values():
            p.stop()

    def test_some_partners_not_in_cache(self):
        """
        Сценарий:
        1) поиск был не так давно, но по части партнеров статусы исчезли
        2) статусы исчезли из кэша
        """
        self._mock_statuses.update({
            self.p1.code: Mock(), self.p2.code: None
        })

        actual_statuses = collect_statuses(
            self.query.qkey, [self.p1.code, self.p2.code],
        )

        assert actual_statuses == {
            'p1': self._mock_statuses[self.p1.code],
            'p2': self._mock_statuses[self.p2.code],
        }

    def test_all_partners_not_in_cache(self):
        """
        Сценарий:
        1) поиск был давно
        2) статусы исчезли из кэша
        """
        self._mock_statuses.update({
            self.p1.code: None, self.p2.code: None
        })

        actual_statuses = collect_statuses(
            self.query.qkey, [self.p1.code, self.p2.code],
        )

        assert actual_statuses == {
            'p1': self._mock_statuses[self.p1.code],
            'p2': self._mock_statuses[self.p2.code],
        }

    def test_all_partners_in_cache(self):
        """
        Сценарий:
        1) предыдущий поиск был недавно
        2) статусы по каждому партнеру еще в кэша
        """
        self._mock_statuses[self.p1.code] = Mock()

        actual_statuses = collect_statuses(
            self.query.qkey, [self.p1.code],
        )

        assert actual_statuses == {
            'p1': self._mock_statuses[self.p1.code],
        }

    def test_cant_deserialize_partners_status(self):
        """
        Сценарий:
        1) формат статусов изменился/криво записался
        2) статус по этому партнеру становится None
        """
        self._mock_statuses.update({
            self.p1.code: Mock(), self.p2.code: 'raise_exception', self.p3.code: Mock()
        })

        actual_statuses = collect_statuses(
            self.query.qkey, [self.p1.code, self.p2.code, self.p3.code],
        )

        assert actual_statuses == {
            'p1': self._mock_statuses[self.p1.code],
            'p2': None,
            'p3': self._mock_statuses[self.p3.code],
        }
