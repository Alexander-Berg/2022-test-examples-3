# -*- coding: utf-8 -*-
""" This test module needs 'zookeeper' package installed and run on local host """

import os
import unittest

from market.idx.marketindexer.marketindexer import miconfig
from market.idx.pylibrary.mindexer_core.zkmaster.zkmaster import ZkMaster, CURRENT_HOSTNAME, LockError

import context


class TestZkMaster(context.HbaseTestCase):
    def setUp(self):
        self.GLOBAL_LOCK_DIR = miconfig.default().get('zookeeper', 'global_lock_dir', '/for_tests/milock')
        self.PARENT_DIR = os.path.dirname(self.GLOBAL_LOCK_DIR)
        self.zk_access = context.create_zk()
        print('\nRECREATION TEST ENVIRONMENT\n')
        self.zk_access.rmtree(self.PARENT_DIR)
        self.zk_access.create_dir(self.PARENT_DIR)

    def test_zkmaster(self):
        config = miconfig.default()
        with ZkMaster() as Master:
            with Master.lock_publishing():
                self.assertEqual(CURRENT_HOSTNAME, self.zk_access.get(os.path.join(miconfig.default().get('zookeeper', 'current_master_file')))[0])
                self.assertEqual('superstrat', self.zk_access.get(miconfig.default().get('zookeeper', 'current_master_mitype_file'))[0])
            self.assertEqual([], self.zk_access.get_children(config.get('zookeeper', 'publish_lock_dir')))

    def test_zkmaster_complex(self):
        with ZkMaster() as master:
            with master.lock_publishing():
                pass

        with ZkMaster() as master2:
            with master2.lock_publishing():
                pass

    def test_zkmaster_complex2(self):
        with ZkMaster() as master:
            with master.lock_publishing():

                with ZkMaster() as master2:
                    self.assertRaises(LockError, master2.lock_publishing(timeout=None).__enter__)

    def test_zkmaster_complex3(self):
        with ZkMaster() as master:
            with master.lock_publishing():

                with ZkMaster() as master2:
                    master2.lock_delta_publishing()

    def test_master_info(self):
        with ZkMaster() as master:
            with master.lock_publishing():
                self.assertEqual(CURRENT_HOSTNAME, master._get_locked_host(master._publish_lock_dir))
                self.assertEqual(None, master._get_locked_host(master._delta_publish_lock_dir))

            with master.lock_delta_publishing():
                self.assertEqual(None, master._get_locked_host(master._publish_lock_dir))
                self.assertEqual(CURRENT_HOSTNAME, master._get_locked_host(master._delta_publish_lock_dir))

    def test_master_is_changeable_during_publishing(self):
        with ZkMaster() as master:
            with master.lock_publishing():
                master.make_me_master()

    def test_am_i_master(self):
        with ZkMaster() as Master:
            mitype = 'superstrat'
            Master.set_master('this.is.not.yandex.machine', 'wrong type')
            self.assertFalse(Master.am_i_master())
            self.assertNotEqual(mitype, Master.current_master_mitype())

            Master.make_me_master()
            self.assertEqual(mitype, Master.current_master_mitype())
            self.assertTrue(Master.am_i_master())
            self.assertEqual(CURRENT_HOSTNAME, Master.current_master())

    def test_raise_inside_zkMaster(self):
        def must_raise():
            with ZkMaster():
                raise Exception('kakashka y zk')

        self.assertRaises(Exception, must_raise)


if '__main__' == __name__:
    unittest.main()
