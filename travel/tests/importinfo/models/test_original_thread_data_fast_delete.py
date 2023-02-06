# -*- coding: utf-8 -*-

from travel.rasp.admin.importinfo.models import OriginalThreadData
from travel.rasp.admin.importinfo.models.two_stage_import import TwoStageImportPackage
from tester.factories import create_supplier
from tester.testcase import TestCase


class TestOriginalThreadData(TestCase):
    def test_fast_delete_by_supplier(self):
        supplier = create_supplier()
        package = TwoStageImportPackage.objects.create(title=u'test', supplier=supplier)

        OriginalThreadData.objects.create(supplier=supplier, package=None, thread_uid='by_supplier')
        OriginalThreadData.objects.create(supplier=supplier, package=package, thread_uid='by_package')

        assert OriginalThreadData.objects.count() == 2

        OriginalThreadData.fast_delete_by_supplier_id(supplier.id)

        remaining_thread_uid = OriginalThreadData.objects.get().thread_uid
        assert remaining_thread_uid == 'by_package'

    def test_fast_delete_by_package(self):
        supplier = create_supplier()
        package = TwoStageImportPackage.objects.create(title=u'test', supplier=supplier)

        OriginalThreadData.objects.create(supplier=supplier, package=None, thread_uid='by_supplier')
        OriginalThreadData.objects.create(supplier=supplier, package=package, thread_uid='by_package')

        assert OriginalThreadData.objects.count() == 2

        OriginalThreadData.fast_delete_by_package_id(package.id)

        remaining_thread_uid = OriginalThreadData.objects.get().thread_uid
        assert remaining_thread_uid == 'by_supplier'
