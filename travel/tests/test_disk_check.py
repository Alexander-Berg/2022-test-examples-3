#!/usb/bin/env python3

import os
import unittest
import sys
from unittest import mock
from collections import namedtuple


sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))


import disk_free_check as disk_check


assert disk_check.FREE_SPACE_PERCENT == 10.0
assert disk_check.ROOTS == ['/', '/ephemeral']


usage = namedtuple('usage', 'total,used,free')


def mk_usage(total_gb, free_gb):
    gb = 2 ** 30
    total = int(total_gb * gb)
    free = int(free_gb * gb)
    used = total - free
    return usage(total=total, free=free, used=used)


class TestRootCheck(unittest.TestCase):
    @mock.patch.object(disk_check, '_get_free_space', return_value=mk_usage(5, 4))
    def test_has_free_space(self, m_disk_check):
        self.assertTrue(disk_check._root_has_enough_free_space('/'))
        m_disk_check.assert_called_once_with('/')

    @mock.patch.object(disk_check, '_get_free_space', return_value=mk_usage(5, 0.3))
    def test_has_no_free_space(self, m_disk_check):
        self.assertFalse(disk_check._root_has_enough_free_space('/'))
        m_disk_check.assert_called_once_with('/')


class TestExecuteFreeSpaceCheck(unittest.TestCase):
    @mock.patch.object(disk_check, '_get_free_space', side_effect=[mk_usage(5, 4), mk_usage(5, 3)])
    def test_all_roots_free(self, m_disk_check):
        self.assertIsNone(disk_check.execute_free_space_check())
        m_disk_check.assert_has_calls([mock.call('/'), mock.call('/ephemeral')])

    @mock.patch.object(disk_check, '_get_free_space', side_effect=[FileNotFoundError, mk_usage(5, 3)])
    def test_one_free_one_not_exists(self, m_disk_check):
        self.assertIsNone(disk_check.execute_free_space_check())
        m_disk_check.assert_has_calls([mock.call('/'), mock.call('/ephemeral')])

    @mock.patch.object(disk_check, '_get_free_space', side_effect=[mk_usage(5, 3), FileNotFoundError])
    def test_one_free_one_not_exists_2(self, m_disk_check):
        self.assertIsNone(disk_check.execute_free_space_check())
        m_disk_check.assert_has_calls([mock.call('/'), mock.call('/ephemeral')])

    @mock.patch.object(disk_check, '_get_free_space', side_effect=[mk_usage(5, 0.2)])
    def test_no_free_space(self, m_disk_check):
        self.assertEqual(disk_check.execute_free_space_check(), 'Filesystem / is running out of free space')
        m_disk_check.assert_has_calls([mock.call('/')])

    @mock.patch.object(disk_check, '_get_free_space', side_effect=[mk_usage(5, 3), mk_usage(5, 0.1)])
    def test_no_free_space_2(self, m_disk_check):
        self.assertEqual(disk_check.execute_free_space_check(), 'Filesystem /ephemeral is running out of free space')
        m_disk_check.assert_has_calls([mock.call('/'), mock.call('/ephemeral')])


if __name__ == '__main__':
    unittest.main()
