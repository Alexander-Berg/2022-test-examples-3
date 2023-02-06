# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from contextlib import contextmanager

import mock
import pytest


from travel.rasp.library.python.db import replica_sync_checker
from travel.rasp.library.python.db.replica_sync_checker import ReplicaSyncChecker, BadReplicasError


class TestWaitReplicasSync(object):
    def test_check_pending(self):
        class State(object):
            """Тестовый стейт реплик - проверка возможности соединения и синхронизации."""
            def __init__(self):
                self.can_connect = False
                self.is_synced = False

                # сохраняем себя, чтобы уметь получать state через cursor в check_sync
                self.cursor = mock.Mock(_state=self)

                self.connection = None

        states = {replica_host: State() for replica_host in ['r1', 'r2', 'r3']}

        def conn_getter(host):
            """Фейковый коннект к реплике"""
            state = states[host]
            if state.can_connect:
                state.connection = mock.MagicMock(cursor=mock.Mock(return_value=state.cursor))
                return state.connection
            else:
                raise Exception("can't connect")

        def check_sync(connection):
            """Фейковая проверялка синхронизации реплики"""
            state = connection.cursor()._state
            if isinstance(state.is_synced, Exception):
                raise state.is_synced
            else:
                return state.is_synced

        def set_state(replica_states):
            for replica_host, (can_connect, is_synced) in replica_states.items():
                state = states[replica_host]
                state.can_connect = can_connect
                state.is_synced = is_synced

        # Сценарий теста. Меняем состояния реплик и проверяем список ожидающих хостов
        scenario = [
            # все реплики доступны, но не синхронизированны
            (
                # Состояние реплик в виде: {'hostname': [can_connect, is_synced]}
                {'r1': [True, False], 'r2': [True, False], 'r3': [True, False]},
                # Ожидаемое состояние ReplicaSyncChecker.pending_hosts
                {'pending': ['r1', 'r2', 'r3']},
            ),
            # не можем подключиться к r1, но для нее осталась одна попытка
            (
                {'r1': [False, False], 'r2': [True, False], 'r3': [True, False]},
                {'pending': ['r1', 'r2', 'r3']},
            ),
            # считаем r1 сдохшей; в это время r2 синхронизировалась
            (
                {'r1': [False, False], 'r2': [True, True], 'r3': [True, False]},
                {'pending': ['r3']},
            ),
            # ждем r3 - коннект к ней есть, но сфейлились на проверке синхронизации
            (
                {'r3': [True, Exception('sync check fail')]},
                {'pending': ['r3']},
            ),
            # r3 все-таки синхронизировалась
            (
                {'r3': [True, True]},
                {'pending': []},
            ),
        ]

        hosts = ['r1', 'r2', 'r3']

        wrs = ReplicaSyncChecker(hosts, conn_getter, check_sync, max_try_count=2)

        # поехали по шагам сценария
        for replica_states, expected in scenario:
            set_state(replica_states)
            wrs.check_pending()

            assert sorted(wrs.pending_hosts) == expected['pending']

            # если соединялись с репликами, то эти соединения должны быть закрыты
            for replica_host, (can_connect, is_synced) in replica_states.items():
                if can_connect:
                    state = states[replica_host]
                    state.connection.close.assert_called_once_with()

    def test_run(self):

        class ScenarioRunner(object):
            def __init__(self, wrs, scenario):
                self.wrs = wrs
                self.scenario_steps = scenario
                self.scenario_iter = iter(self.scenario_steps)
                self.time = 0

                self.log_state = mock.Mock()
                self.check_pending = mock.Mock()

            @contextmanager
            def start(self):
                mock_time = mock.MagicMock(time=self.get_time, sleep=self.next)
                with mock.patch.object(replica_sync_checker, 'time', mock_time), \
                     mock.patch.object(ReplicaSyncChecker, 'check_pending', self.check_pending), \
                     mock.patch.object(ReplicaSyncChecker, 'log_state', ) as self.log_state:

                    self.next()  # устанавливаем начальное состояние сценария
                    yield

            def get_time(self):
                return self.time

            def next(self, *args):
                """ Используем этот вызов, чтобы менять время и pending_hosts по сценарию,
                    т.к. ReplicaSyncChecker.run сидит в бесконечном цикле.
                """
                new_time, pending_hosts = next(self.scenario_iter)
                self.time = new_time
                self.wrs.pending_hosts = pending_hosts

        wait_for_seconds = 600

        # не дождались синхронизации
        wrs = ReplicaSyncChecker([], None, None, wait_for_seconds=wait_for_seconds)
        scenario = ScenarioRunner(wrs, [
            [0, ['r1', 'r2', 'r3']],
            [100, ['r1', 'r2', 'r3']],
            [wait_for_seconds + 1, ['r1']],
        ])
        with scenario.start():
            with pytest.raises(BadReplicasError):
                wrs.run()
                assert scenario.log_state.call_count == 2

        # дождались синхронизации
        wrs = ReplicaSyncChecker([], None, None, wait_for_seconds=3600)
        scenario = ScenarioRunner(wrs, [
            [0, ['r1', 'r2', 'r3']],
            [100, ['r1', 'r2']],
            [200, ['r1']],
            [wait_for_seconds + 1, []],
        ])
        with scenario.start():
            wrs.run()
            assert scenario.log_state.call_count == 3
