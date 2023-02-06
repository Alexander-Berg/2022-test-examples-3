# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function
from unittest import TestCase as OriginalTestCase
from django.conf import settings

from travel.rasp.library.python.common23.data_api.dbaas.client import HostInfo
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.db_models.base import Base
from travel.rasp.wizards.train_wizard_api.lib.storage_store import StorageStore


class TestCase(OriginalTestCase):
    def setUp(self):
        if settings.DBAAS_TRAIN_WIZARD_API_DB_CLUSTER_ID is not None:
            self.skipTest('DBAAS_TRAIN_WIZARD_API_DB_CLUSTER_ID should be None.')
            return

        if not settings.DBAAS_TRAIN_WIZARD_API_DB_NAME.endswith('_test'):
            self.skipTest('DBAAS_TRAIN_WIZARD_API_DB_NAME should has suffix [_test]. For example: [mangin_test].')
            return

        self._storage_store = StorageStore()
        for alias in ['master', 'slave']:
            self._storage_store.init(
                (
                    HostInfo(settings.DBAAS_TRAIN_WIZARD_API_DB_HOST, 'xxx'),
                ),
                alias=alias,
                logging=True,
                allow_read_only=(alias == 'slave')
            )

        Base.metadata.create_all(self._storage_store.get('master').get_engine())

    def tearDown(self):
        Base.metadata.drop_all(self._storage_store.get('master').get_engine())
