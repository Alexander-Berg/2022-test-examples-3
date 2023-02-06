# coding: utf-8

import unittest
from avtomatika.components.actions import flapodav
from components_app.component.new_zk_storage import NoData
from components_app.tests.mocks.zk import ZkMock
from avtomatika.components.constants import Levels


class TestFlapodav(unittest.TestCase):
    def setUp(self):
        self.flapodav = flapodav.Flapodav()
        self.flapodav.zk_storage.zk = ZkMock()
        self.flapodav.load_config({
            'valve_time': 100,      # 100s
            'up_valve_time': 0,     # We do not this param here. 0 means 'disabled'
            'up_valve_part': 1,
            'id': 1,
            'name': 'test',
            'zk_storage': {
                'node': '/test_node',
                'zk': {
                    'hosts': '...'      # mocked
                },
                'keys': {1, 2}
            }
        })

        self.flapodav.start()

    def tearDown(self):
        self.flapodav.stop()

    def test_set_init_value(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO,
            2: Levels.WARNING
        }, timestamp=0)

    def test_up_value(self):
        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.INFO)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.WARNING)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.CRITICAL)

    def test_down_value(self):
        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.CRITICAL)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.CRITICAL)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.CRITICAL)

    def test_down_value_with_timeout(self):
        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.CRITICAL)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=150)

        self.assertEqual(new_values[1], Levels.WARNING)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=300)

        self.assertEqual(new_values[1], Levels.INFO)

    def test_no_data(self):
        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: NoData()
        }, timestamp=0)
        self.assertIsInstance(new_values[1], NoData)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)
        self.assertEqual(new_values[1], Levels.INFO)


class TestFlapodavParallels(unittest.TestCase):
    def setUp(self):
        self.flapodav = flapodav.Flapodav()
        self.flapodav.zk_storage.zk = ZkMock()
        self.flapodav.load_config({
            'valve_time': 100,      # 100s
            'up_valve_time': 0,     # We do not this param here. 0 means 'disabled'
            'up_valve_part': 1,
            'id': 1,
            'name': 'test',
            'zk_storage': {
                'node': '/test_node',
                'zk': {
                    'hosts': '...'      # mocked
                },
                'keys': {1, 2},
            }
        })

        self.flapodav.start()

        self.flapodav2 = flapodav.Flapodav()
        self.flapodav2.zk_storage.zk = self.flapodav.zk_storage.zk
        self.flapodav2.load_config({
            'valve_time': 100,      # 100s
            'up_valve_time': 0,     # We do not this param here. 0 means 'disabled'
            'up_valve_part': 1,
            'id': 2,
            'name': 'test2',
            'zk_storage': {
                'node': '/test_node',
                'keys': {1, 2},
            }
        })

        self.flapodav2.start()

    def tearDown(self):
        self.flapodav.stop()
        self.flapodav2.stop()

    def test_init_parallels(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=0)

        new_values = self.flapodav2.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.assertEqual(new_values[1], Levels.WARNING)


class TestUpValveTime(unittest.TestCase):
    def setUp(self):
        self.flapodav = flapodav.Flapodav()
        self.flapodav.zk_storage.zk = ZkMock()
        self.flapodav.load_config({
            'valve_time': 100,      # 100s
            'up_valve_time': 10,    # 10s
            'up_valve_part': 1,     # All values in history must be more than current for up value
            'id': 1,
            'name': 'test',
            'zk_storage': {
                'node': '/test_node',
                'zk': {
                    'hosts': '...'  # mocked
                },
                'keys': {1, 2}
            }
        })

        self.flapodav.start()

    def tearDown(self):
        self.flapodav.stop()

    def test_one_flap(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=5)

        self.assertEqual(new_values[1], Levels.INFO)

    def test_crit_warn(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=10)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=15)

        self.assertEqual(new_values[1], Levels.WARNING)

    def test_warn_crit(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=10)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=25)

        self.assertEqual(new_values[1], Levels.WARNING)

    def test_big_interval(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=5)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=100)

        self.assertEqual(new_values[1], Levels.WARNING)


class TestUpValvePart(unittest.TestCase):
    def setUp(self):
        self.flapodav = flapodav.Flapodav()
        self.flapodav.zk_storage.zk = ZkMock()
        self.flapodav.load_config({
            'valve_time': 100,      # 100s
            'up_valve_time': 60,    # 60s
            'up_valve_part': 0.5,   # if 50% higher than current than value will be upped
            'id': 1,
            'name': 'test',
            'zk_storage': {
                'node': '/test_node',
                'zk': {
                    'hosts': '...'  # mocked
                },
                'keys': {1, 2}
            }
        })

        self.flapodav.start()

    def tearDown(self):
        self.flapodav.stop()

    def test_one_flap(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=5)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=10)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=15)

        self.assertEqual(new_values[1], Levels.INFO)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=20)

        self.assertEqual(new_values[1], Levels.INFO)

    def test_up_value(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=5)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=10)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=15)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=20)

        self.assertEqual(new_values[1], Levels.INFO)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=25)

        self.assertEqual(new_values[1], Levels.WARNING)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=30)

        self.assertEqual(new_values[1], Levels.WARNING)

    def test_middle(self):
        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.INFO
        }, timestamp=0)

        self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.WARNING
        }, timestamp=5)

        new_values = self.flapodav.correct_values(vertex_id_value_map={
            1: Levels.CRITICAL
        }, timestamp=10)

        self.assertEqual(new_values[1], Levels.WARNING)
