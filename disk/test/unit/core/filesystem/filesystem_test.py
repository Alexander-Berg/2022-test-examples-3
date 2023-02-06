# -*- coding: utf-8 -*-
import mock

from nose_parameterized import parameterized

from hamcrest import assert_that, has_length, is_, equal_to
from test.unit.base import NoDBTestCase

from mpfs.core.address import Address
from mpfs.core.filesystem.base import Filesystem
from mpfs.core.metastorage.control import support_blocked_hids


class FilesystemTestCase(NoDBTestCase):
    def test_check_hids_blockings_batches_requests(self):  # https://st.yandex-team.ru/CHEMODAN-34551
        with mock.patch.object(support_blocked_hids, 'get', return_value=[]) as mocked_method:
            fs = Filesystem()
            fs.check_hids_blockings([x for x in xrange(1, 500001)], 'copy_resource')
        assert_that(mocked_method.call_count, is_(equal_to(2)))
        assert_that(mocked_method.call_args_list[0][0][0], has_length(375000))
        assert_that(mocked_method.call_args_list[1][0][0], has_length(125000))

    @parameterized.expand([
        ('a', 'a (1)'),
        ('a.txt', 'a (1).txt'),
        ('a (1).txt', 'a (1) (1).txt'),
        ('.txt', '.txt (1)'),
        ('.', '. (1)'),
        ('a.', 'a (1).')
    ])
    def test_autosuffix_address_suffixes(self, existing_name, expected_name):
        existing_address = Address('123:' + '/disk/' + existing_name)
        expected_address = Address('123:' + '/disk/' + expected_name)
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=[True, False]):
            result = Filesystem().autosuffix_address(existing_address)
        assert expected_address.id == result.id

    @parameterized.expand([
        ('a', 'a (1)'),
        ('a.txt', 'a (1).txt'),
        ('a (1).txt', 'a (1) (1).txt'),
        ('.txt', '.txt (1)'),
        ('.', '. (1)'),
        ('a.', 'a (1).')
    ])
    def test_autosuffix_addresses_suffixes(self, existing_name, expected_name):
        existing_address = Address('123:/disk/' + existing_name)
        expected_address = Address('123:/disk/' + expected_name)
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=[True, False]):
            result = Filesystem().autosuffix_addresses([existing_address])
        assert expected_address.id == result[0].id

    def test_autosuffix_addresses_two_same_names_when_file_exists(self):
        addr = '123:/disk/test1'
        addr_copy_1 = '123:/disk/test1 (1)'
        addr_copy_2 = '123:/disk/test1 (2)'
        return_values = [True, False, False]
        expected_values = [addr_copy_1, addr_copy_2]
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=return_values) as m:
            result = Filesystem().autosuffix_addresses([Address(addr), Address(addr)])
        assert m.call_count == len(return_values)
        assert expected_values == [addr.id for addr in result]

    def test_autosuffix_addresses_two_same_names_when_file_not_exists(self):
        addr = '123:/disk/test1'
        addr_copy_1 = '123:/disk/test1 (1)'
        return_values = [False, False]
        expected_values = [addr, addr_copy_1]
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=return_values) as m:
            result = Filesystem().autosuffix_addresses([Address(addr), Address(addr)])
        assert m.call_count == len(return_values)
        assert expected_values == [addr.id for addr in result]

    @parameterized.expand([(0, 'a.txt'), (1, 'a (1).txt'), (2, 'a (2).txt')])
    def test_autosuffix_address_iterations(self, existing_n, expected_name):
        original_address = Address('123:/disk/a.txt')
        expected_address = Address('123:/disk/' + expected_name)
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=[True] * existing_n + [False]):
            result = Filesystem().autosuffix_address(original_address)
        assert expected_address.id == result.id

    def test_autosuffix_addresses_complex(self):
        original_addresses = [
            Address('123:/disk/a1.txt'),
            Address('123:/disk/a2.txt'),
            Address('123:/disk/a2.txt'),
            Address('123:/disk/a3.txt'),
        ]
        def files_exist(uid_unused, addres):
            return {
                '123:/disk/a1.txt': False,
                '123:/disk/a2.txt': True,
                '123:/disk/a2 (1).txt': False,
                '123:/disk/a2 (2).txt': False,
                '123:/disk/a3.txt': True,
                '123:/disk/a3 (1).txt': True,
                '123:/disk/a3 (2).txt': True,
                '123:/disk/a3 (3).txt': False,
            }[addres]
        expected_addresses = [
            '123:/disk/a1.txt',
            '123:/disk/a2 (1).txt',
            '123:/disk/a2 (2).txt',
            '123:/disk/a3 (3).txt',
        ]
        with mock.patch('mpfs.core.filesystem.base.Filesystem.exists', side_effect=files_exist):
            result = Filesystem().autosuffix_addresses(original_addresses)
        assert expected_addresses == [addr.id for addr in result]
