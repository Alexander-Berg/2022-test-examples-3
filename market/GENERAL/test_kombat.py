#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import __classic_import     # noqa

from google.protobuf import text_format
import time
import market.kombat.mt.env as env
from market.kombat.proto.battle_pb2 import TBattleSpec
from market.pylibrary.lite.matcher import Capture, CaptureTo


class T(env.KombatSuite):
    def test_start(self):
        response = self.kombat.request_json('help')
        self.assertFragmentIn(response, {
            '/help GET': 'Brief description about all handles'
        })

    def test_simple_battle(self):
        battle = TBattleSpec()
        battle.Attack.add()
        battle.Result.Transformer.Source = True
        response = self.kombat.request_json('add_battle', body=text_format.MessageToString(battle))
        self._assert_battle_start(response)

    def test_scenarios_battle(self):
        battle = TBattleSpec()
        battle.OnlyScenarios = True
        battle.Attack.add()
        battle.Result.Transformer.Source = True
        response = self.kombat.request_json('add_battle', body=text_format.MessageToString(battle))
        self._assert_battle_start(response)

    def test_in_fight_cancellation(self):
        battle = TBattleSpec()
        battle.Attack.add()
        battle.Result.Transformer.Source = True
        response = self.kombat.request_json('add_battle', body=text_format.MessageToString(battle))
        battle_id = self._capture_battle_id(response)
        self._wait_for_status(battle_id, 'execute')
        response = self.kombat.request_json('cancel_battle?id={}'.format(battle_id))
        self.assertFragmentIn(response, {
            'status': 'ok'
        })
        self._wait_for_status(battle_id, 'cancel')

    def test_simultaneous_battles_limit(self):
        """Проверка ограничения на количество одновременно запускаемых стрельб

        Запускаем стрельбы для owner'a с ограничениями (limited) и без. Смотрим максимальное
        количество стрельб в активном состоянии.
        """
        def add_battle(priority=10, owner=None):
            battle = TBattleSpec()
            battle.Attack.add()
            battle.Result.Transformer.Source = True
            battle.Priority = priority
            if owner is not None:
                battle.Owner = owner
            response = self.kombat.request_json('add_battle', body=text_format.MessageToString(battle))
            return self._capture_battle_id(response)

        limited_owner = 'limited'
        simultaneous_battles_limit = 1
        for _ in range(0, simultaneous_battles_limit * 6):
            add_battle(priority=10, owner='owner')
            add_battle(priority=100, owner=limited_owner)

        limited_battle_max_number = 0
        unlimited_battle_max_number = 0
        while not self._is_all_completed():
            response = self.kombat.request_json('list_battle?status=execute')
            limited_battles = sum(battle['owner'] == limited_owner for battle in response.root)
            limited_battle_max_number = max(limited_battle_max_number, limited_battles)
            unlimited_battles = sum(battle['owner'] != limited_owner for battle in response.root)
            unlimited_battle_max_number = max(unlimited_battle_max_number, unlimited_battles)
            time.sleep(0.1)

        self.assertEqual(simultaneous_battles_limit, limited_battle_max_number)
        self.assertTrue(unlimited_battle_max_number > limited_battle_max_number)

    def _capture_battle_id(self, response):
        battle_id = Capture()
        self.assertFragmentIn(response, {
            'battle_id': CaptureTo(battle_id)
        })
        return battle_id.value

    def _request_battle_status(self, battle_id):
        response = self.kombat.request_json('battle_status?id={}'.format(battle_id))
        return response.root['status']

    def _is_all_completed(self, count=None):
        all_statuses = ['new', 'queue', 'execute', 'complete', 'resource']
        query_statuses = '&'.join(['status={}'.format(s) for s in all_statuses])
        query = 'list_battle?sort-created-desc=1&sort-priority=0&{}'.format(query_statuses)
        if count is not None:
            query += '&count={}'.format(count)
        response = self.kombat.request_json(query)
        return all(battle['status'] == 'complete' for battle in response.root)

    def _wait_for_status(self, battle_id, target_status):
        status = self._request_battle_status(battle_id)
        while status != target_status:
            status = self._request_battle_status(battle_id)
            time.sleep(0.1)

    def _assert_battle_start(self, response):
        battle_id = self._capture_battle_id(response)
        self._wait_for_status(battle_id, 'complete')
        self.kombat.request_json('battle_status?id={}'.format(battle_id))
        '''
            self.assertFragmentIn(status_response, {
                'Source': {'Attack': [{'Error': Absent()}]}
            })
            self.assertFragmentNotIn(status_response, {
                'Error': {}
            })
            self.assertFragmentIn(status_response, {
                'StageDurations': {
                    'Stage': [
                        {
                            'Name': 'full-battle'
                        }
                    ]
                }
            })
            '''


if __name__ == '__main__':
    env.main()
