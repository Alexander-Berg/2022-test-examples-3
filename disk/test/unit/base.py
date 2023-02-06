# -*- coding: utf-8 -*-

"""
Базовые тестовые классы для юнит-тестов mpfs.
"""

import os
import random
import unittest
import mock
import mongomock

from collections import defaultdict

import mpfs.engine.process
from mpfs.core.services.hbf_service import HbfService

if 'MPFS_PACKAGE' not in os.environ:
    os.environ['MPFS_PACKAGE'] = 'disk'

from mpfs.config import settings


class DummyDBController(object):
    """
    Dummy-версия mongo Controller.

    mongo Controller это инстанс одного из следующих классов:
      * mpfs.metastorage.mongo.single.source.MongoSourceController;
      * mpfs.metastorage.mongo.sharded.source.MongoSourceController.
    """

    def __init__(self):
        self._connection = mongomock.MongoClient()
        self.common_mongo_config = settings.mongo
        self.sharded = False

    def database(self, db_name=None):
        pass

    def connection(self, conn_name=None):
        return self._connection


class NoDBTestCase(unittest.TestCase):
    @classmethod
    def setup_class(cls):
        cls._db_patcher = mock.patch.object(mpfs.engine.process, 'dbctl',
                                            return_value=DummyDBController())
        # Отключаем логирование
        cls._default_log_patcher = mock.patch.object(mpfs.engine.process, 'get_default_log')
        cls._error_log_patcher = mock.patch.object(mpfs.engine.process, 'get_error_log')
        cls._service_log_patcher = mock.patch.object(mpfs.engine.process, 'get_service_log')
        _hbf_cache_file_path = '/tmp/mpfs/cache_%s' % hex(random.getrandbits(8 * 10))[2:-1]
        cls._patchers = [cls._db_patcher,
                         cls._default_log_patcher,
                         cls._error_log_patcher,
                         cls._service_log_patcher]

        if 'HbfService' in settings.services:
            cls._hbf_cache_file_path_patcher = mock.patch.object(HbfService(), 'cache_file_path', _hbf_cache_file_path)
            cls._setting_hbf_cache_file_path_patcher = mock.patch.dict(settings.services['HbfService'],
                                                                       {'cache_file_path': _hbf_cache_file_path})
            cls._patchers.extend([cls._hbf_cache_file_path_patcher,
                                 cls._setting_hbf_cache_file_path_patcher])

        for patcher in cls._patchers:
            patcher.start()

    @classmethod
    def teardown_class(cls):
        for patcher in cls._patchers:
            patcher.stop()
