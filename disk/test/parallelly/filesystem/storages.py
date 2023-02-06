# -*- coding: utf-8 -*-
import itertools
from nose_parameterized import parameterized

from mpfs.common import errors
from test.parallelly.filesystem.base import CommonFilesystemTestCase
from mpfs.core.factory import SYS_FOLDER_MAP

SYS_FOLDER_MAP = [path for path in SYS_FOLDER_MAP if path != "/share"]


def add_async(l):
    return l + tuple(map(lambda m: "async_" + m, l))

MODIFIERS = (
    'mkdir',
    'rm',
    'set_public',
    'share_create_group',
    'store',
    'trash_append',
)

DOUBLE_MODIFIERS = (
    'copy',
    'move',
)

MODIFIERS = add_async(MODIFIERS)
DOUBLE_MODIFIERS = add_async(DOUBLE_MODIFIERS)

ERRORS = (
    errors.AddressError.code,            # Вызывается в общем случае
    errors.MPFSNotImplemented.code,      # Если нет метода async_
    errors.RmPathError.code,             # rm системного пути
    errors.StorageAddressError.code,     # check path.is_storage
)


def name_function(f, n, p):
    return (f.__name__ + "_" + p.args[0] + p.args[1]).replace('/', '_')


class StoragesFilesystemTestCase(CommonFilesystemTestCase):
    """Проверяем работу модифицирующих ручек с системными ресурсами"""

    @parameterized.expand(itertools.product(MODIFIERS, SYS_FOLDER_MAP), testcase_func_name=name_function)
    def test_modify_storage(self, method, root_path):
        response = self.json_error(method, {'uid': self.uid, method: root_path})
        assert response['code'] in ERRORS

    @parameterized.expand(itertools.product(DOUBLE_MODIFIERS, SYS_FOLDER_MAP), testcase_func_name=name_function)
    def test_dup_storage(self, method, root_path):
        response = self.json_error(method, {'uid': self.uid, "src": root_path, "dst": root_path + "1", 'force': '1'})
        assert response['code'] in ERRORS
