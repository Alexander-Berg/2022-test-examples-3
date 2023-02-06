# coding: utf-8

import unittest

import zake.fake_client

from components_app.component.new_zk_storage import ZkStorage
from components_app.component.new_zk_storage import NoData
from components_app.component import Zk
from components_app.configs.test import zk_storage as config


class TestInitZkStorage(unittest.TestCase):
    def test_init_with_values(self):
        zk = Zk()
        storage = ZkStorage(zk, None, {'a': 'b', 'c': 'd'})
        storage.load_config(config)
        zk.client = zake.fake_client.FakeClient(
            # ','.join(config['zk']['hosts']),
        )
        storage.start()

        self.assertEqual(storage['a'], 'b')
        self.assertEqual(storage['k'], None)


class TestZkStorage(unittest.TestCase):
    def setUp(self):
        zk = Zk()
        self.storage = ZkStorage(zk=zk)
        self.storage.load_config(config)
        zk.client = zake.fake_client.FakeClient()
        self.storage.start()

    def tearDown(self):
        self.storage.zk.client.delete(self.storage.node, recursive=True)
        self.storage.stop()

    def test_set_get_value_to_zk(self):
        self.storage['test'] = 'test_value'
        self.assertEqual(self.storage['test'], 'test_value')

        self.storage.push()
        self.storage.clear()
        self.assertIsNone(self.storage['test'])

        self.storage.load()
        self.assertEqual(self.storage['test'], 'test_value')

    def test_lock(self):
        with self.storage.lock:
            pass

    def test_node_with_b(self):
        self.storage.zk.client.create('{}/test'.format(self.storage.node))
        self.storage.load()

        self.assertEqual(self.storage['test'], None)

    def test_none(self):
        self.storage.load()
        self.assertIsNone(self.storage['test'])

        self.storage['test'] = None
        self.storage.push()

        self.assertIsNone(self.storage['test'])
        self.storage.load()

        self.assertIsNone(self.storage['test'])

    def test_nodata(self):
        self.storage['test'] = NoData(timestamp=100, meta={'a': 'b'})
        self.storage.push()

        self.storage.start_loading()
        self.assertIsNone(self.storage['test'])
        self.storage.wait_loading_end()

        self.assertIsInstance(self.storage['test'], NoData)
        self.assertEqual(self.storage['test'].timestamp, 100)
        self.assertEqual(self.storage['test'].meta, {'a': 'b'})

    def test_to_dict(self):
        self.storage['test'] = 1
        self.storage['test2'] = 'ert'
        data = dict(self.storage)

        self.assertEqual(data, {'test': 1, 'test2': 'ert'})
