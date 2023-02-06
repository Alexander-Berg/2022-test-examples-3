import functools
import unittest

from django.test import TestCase

from cars.carsharing.models.car_registry_document import CarRegistryDocument
from cars.carsharing.core.documents_manager import DocumentFilesManager


class DocumentFilesManagerTestCase(TestCase):

    def setUp(self):
        self.manager = DocumentFilesManager.from_settings()
        self.manager._mds_client = unittest.mock.MagicMock()
        self.mds_mock = self.manager._mds_client

    def test_find_osago_by_type(self):
        finder = functools.partial(
            self.manager._find_document_by_type,
            type_=DocumentFilesManager.DocumentFileType('osago'),
        )
        crd1 = CarRegistryDocument.objects.create(osago_number='XXX00123456')
        CarRegistryDocument.objects.create(osago_number='XXX0042')
        self.assertEqual(
            finder('OSAGO_00123456'),
            crd1,
        )

    def test_find_transfer_receipt_by_type(self):
        finder = functools.partial(
            self.manager._find_document_by_type,
            type_=DocumentFilesManager.DocumentFileType('transfer_receipt'),
        )
        crd1 = CarRegistryDocument.objects.create(number='п123иу')
        CarRegistryDocument.objects.create(osago_number='ф456иу')
        self.assertEqual(
            finder('APP_П123ИУ'),
            crd1,
        )

    def test_find_registration_by_type(self):
        finder = functools.partial(
            self.manager._find_document_by_type,
            type_=DocumentFilesManager.DocumentFileType('registration'),
        )
        crd1 = CarRegistryDocument.objects.create(registration_id='42')
        CarRegistryDocument.objects.create(registration_id='123')
        self.assertEqual(
            finder('STS_42'),
            crd1,
        )

    def test_save_document(self):
        CarRegistryDocument.objects.create(registration_id='42')
        crd = CarRegistryDocument.objects.create(registration_id='123')

        self.manager.save_document(
            filename='STS_123',
            type_=DocumentFilesManager.DocumentFileType('registration'),
            data='foo'
        )
        self.assertEqual(
            self.mds_mock.put_object.call_args_list,
            [
                unittest.mock.call(
                    bucket='carsharing-car-documents',
                    body='foo',
                    key='registration/STS_123',
                )
            ]
        )
        crd.refresh_from_db()
        self.assertEqual(
            crd.registration_mds_key,
            'registration/STS_123',
        )
