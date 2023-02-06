from cars.users.factories import UserFactory
from django.test import TransactionTestCase

from cars.core.datasync import StubDataSyncClient
from cars.users.core import UserProfileUpdater
from cars.users.core.datasync import DataSyncDocumentsClient


class UpdateHashesTest(TransactionTestCase):
    STUB_DS_DATA = {
        'documents': {
            '1': {
                'carsharing': {
                    'doc_value': '1234567890',
                }
            }
        },
        'driving_license': {
            '1': {
                'carsharing': {
                    'number': '0987654321',
                },
            },
        },
        'documents_unverified': {
            '1': {
                'carsharing': {
                    'doc_value': '1234567890',
                }
            }
        },
        'driving_license_unverified': {
            '1': {
                'carsharing': {
                    'number_front': '0987654321',
                },
            },
        }
    }

    def setUp(self):
        self.user = UserFactory.create(uid=1)
        self.user.passport_ds_revision = 'carsharing'
        self.user.driving_license_ds_revision = 'carsharing'
        self.data_sync_client = StubDataSyncClient(self.STUB_DS_DATA)
        self.data_sync_documents_client = DataSyncDocumentsClient(self.data_sync_client)

    def test_work_correctly(self):
        upd = UserProfileUpdater(user=self.user, datasync_client=self.data_sync_documents_client)
        upd.update_document_hashes()

        self.user.refresh_from_db()
        self.assertIsNotNone(self.user.passport_number_hash)
        self.assertIsNotNone(self.user.driving_license_number_hash)
