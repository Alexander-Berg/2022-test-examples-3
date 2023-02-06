# -*- coding: utf-8 -*-
import mock
import time

from mpfs.config import settings
from mpfs.core.services.data_api_service import DataApiDocsService
from test.base import DiskTestCase

DOCS_DATABASES = settings.docs['databases']

data_api_docs_service = DataApiDocsService(DOCS_DATABASES[0])


class DataApiDocsTest(DiskTestCase):
    uid = None
    master_slave_sync_delay = 0.04
    """Время синхронизации мастера (в который всё пишется) со слэйвами (с котороых всё читается)."""
    FILE_PATH = '/disk/doc.docx'
    FILE_PATH_2 = '/disk/2.docx'
    FILE_PATH_3 = '/disk/3.docx'
    SHARING_URL = 'some_url'

    @staticmethod
    def generate_uid():
        return str(int(time.time() * 1000000000))

    def setup_method(self, method):
        self.uid = self.generate_uid()
        super(DataApiDocsTest, self).setup_method(method)
        data_api_docs_service.log = self.log
        data_api_docs_service.log = self.log
        self.login = str(self.uid)

        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False):
            # insert 4 docs to datasync
            self.upload_file(self.uid, self.FILE_PATH)
            self.resource_id = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': 'resource_id'})['meta'][
                'resource_id']
            data_api_docs_service.post_doc_item(self.uid, data={'resource_id': self.resource_id, 'ts': 1})

            self.upload_file(self.uid, self.FILE_PATH_2)
            self.resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH_2, 'meta': 'resource_id'})['meta']['resource_id']
            data_api_docs_service.post_doc_item(self.uid, data={'resource_id': self.resource_id_2, 'ts': 2})

            self.upload_file(self.uid, self.FILE_PATH_3)
            self.resource_id_3 = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH_3, 'meta': 'resource_id'})['meta']['resource_id']
            data_api_docs_service.post_doc_item(self.uid, data={'resource_id': self.resource_id_3, 'ts': 3})

            data_api_docs_service.post_doc_item(self.uid, data={'office_online_sharing_url': self.SHARING_URL, 'ts': 4})


    def test_delete_documents_from_dataapi(self):
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False):
            self.json_ok('trash_append', {'uid': self.uid, 'path': self.FILE_PATH_2})
            self.json_ok('rm', {'uid': self.uid, 'path': self.FILE_PATH_3})
            res = data_api_docs_service.get_all_user_docs(self.uid)
            assert len(res) == 2
            assert res[0]['resource_id'] == self.resource_id
            assert res[1]['office_online_sharing_url'] == self.SHARING_URL

    def test_delete_already_deleted_items(self):
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False):
            self.json_ok('trash_append', {'uid': self.uid, 'path': self.FILE_PATH_2})

            fake_doc_item = {'resource_id': self.resource_id_2, 'ts':1, 'id': '1'}
            with mock.patch('mpfs.core.services.data_api_service.DataApiDocsService.get_all_user_docs', return_value=[fake_doc_item]):
                self.json_ok('rm', {'uid': self.uid, 'path': self.FILE_PATH_3})
            res = data_api_docs_service.get_all_user_docs(self.uid)
            assert len(res) == 3
            assert res[0]['resource_id'] == self.resource_id
            assert res[1]['resource_id'] == self.resource_id_3
