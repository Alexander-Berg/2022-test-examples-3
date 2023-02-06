# -*- coding: utf-8 -*-

import json
import mock

from test.base import DiskTestCase
from test.fixtures import users
from test.fixtures.kladun import KladunMocker
from test.helpers.stubs.services import RestoreDBServiceStub, MulcaServiceStub, KladunStub, PreviewerStub

from mpfs.common.util import to_json
from mpfs.core.address import Address, ResourceId
from mpfs.core.file_recovery import errors
from mpfs.core.file_recovery.logic.reports import RecoveryReport
from mpfs.core.file_recovery.logic.manager import process_raw_reports
from mpfs.core.file_recovery.logic.processors import (
    BaseReportProcessor, FileNotFoundReportProcessor,
    HashConflictReportProcessor, ProcessStatus,
    put_file_on_recovery, PutOnRecoveryStatus
)
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.factory import get_resource
from mpfs.core.services.restore_db_service import restore_db_service


class BaseReportProcessorTestCase(DiskTestCase):
    def setup_method(self, method):
        super(BaseReportProcessorTestCase, self).setup_method(method)
        self.address = Address.Make(self.uid, '/disk/1.txt')
        self.upload_file(self.address.uid, self.address.path)
        resp = self.json_ok('info', {'uid': self.uid, 'path': self.address.path, 'meta': 'resource_id,sha256,md5,size,hid'})
        self.mpfs_checksums = FileChecksums(resp['meta']['md5'], resp['meta']['sha256'], resp['meta']['size'])
        self.resource_id = ResourceId.parse(resp['meta']['resource_id'])
        self.resource = get_resource(self.uid, self.address)


class ReportManagerTestCase(BaseReportProcessorTestCase):
    def build_json_report(self, report_name, address, external_checksums):
        fields = {
            'uid': address.uid,
            'mpfs_path': address.path,
            'report_name': report_name
        }
        fields.update({"local_%s" % k: v for k, v in external_checksums.as_dict().iteritems()})
        return to_json(fields)

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    @mock.patch('mpfs.core.file_recovery.logic.manager.FILE_RECOVERY_ENABLE_REPORT_PROCESSING', new=True)
    def test_process_raw_reports(self):
        raw_reports = [
            self.build_json_report(FileNotFoundReportProcessor.alias, self.address, self.mpfs_checksums),
            self.build_json_report('unknown_report_name', self.address, self.mpfs_checksums),
        ]
        with KladunStub(checksums_obj=self.mpfs_checksums):
            counter = process_raw_reports(raw_reports)
            assert counter['_total_'] == 2


class CommonMethodsTestCase(BaseReportProcessorTestCase):
    def test_load_file_by_resource_id(self):
        not_exists_addr = Address.Make(self.uid, "%s_not_eixsts" % self.address.path)
        rp = BaseReportProcessor(RecoveryReport('', not_exists_addr, self.mpfs_checksums, resource_id=self.resource_id))
        resource = rp._load_file_resource()
        assert resource.address.id == self.address.id

    def test_load_file_resource_id_missmatch(self):
        fake_resource_id = self.resource_id.copy()
        fake_resource_id.uid = '123'
        rp = BaseReportProcessor(RecoveryReport('', self.address, self.mpfs_checksums, resource_id=fake_resource_id))
        with self.assertRaises(errors.ResourceIDMissmatch):
            rp._load_file_resource()

    @RestoreDBServiceStub()
    def test_put_on_personal_recovery(self):
        status = put_file_on_recovery(self.resource)
        assert status == PutOnRecoveryStatus.put_personal
        assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is True
        assert restore_db_service.is_public_exists(self.mpfs_checksums.hid) is False

    @RestoreDBServiceStub()
    def test_put_on_public_recovery(self):
        self.json_ok('user_init', {'uid': users.user_1.uid})
        self.upload_file(users.user_1.uid, '/disk/1.txt', file_data=self.mpfs_checksums.as_dict())

        status = put_file_on_recovery(self.resource)
        assert status == PutOnRecoveryStatus.put_public
        assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False
        assert restore_db_service.is_public_exists(self.mpfs_checksums.hid) is True


class FileNotFoundReportProcessorTestCase(BaseReportProcessorTestCase):
    def setup_method(self, method):
        super(FileNotFoundReportProcessorTestCase, self).setup_method(method)
        self.report = RecoveryReport(FileNotFoundReportProcessor.alias, self.address, external_mpfs_checksums=self.mpfs_checksums)

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_false_alarm(self):
        # все хеши совпадают, файл есть в сторадже - ложная тревога
        with KladunStub(checksums_obj=self.mpfs_checksums):
            assert FileNotFoundReportProcessor(self.report).process()[0] == ProcessStatus.false_alarm
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=False)
    def test_not_in_storage(self):
        # файла нет в сторадже, ставим таск на восстановление
        with KladunStub(checksums_obj=self.mpfs_checksums):
            assert FileNotFoundReportProcessor(self.report).process()[0] == ProcessStatus.no_file_in_storage
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is True

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_file_in_storage(self):
        checksums_obj = self.mpfs_checksums.copy()
        checksums_obj.size = 333
        with KladunStub(checksums_obj=checksums_obj):
            assert FileNotFoundReportProcessor(self.report).process()[0] == ProcessStatus.false_alarm
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False


class HashConflictReportProcessorTestCase(BaseReportProcessorTestCase):
    def get_report(self, checksums):
        return RecoveryReport(HashConflictReportProcessor.alias, self.address, checksums)

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_false_alarm(self):
        # все хеши совпадают, файл есть в сторадже - ложная тревога
        with KladunStub(checksums_obj=self.mpfs_checksums):
            assert HashConflictReportProcessor(self.get_report(self.mpfs_checksums)).process()[0] == ProcessStatus.false_alarm
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=False)
    def test_not_in_storage(self):
        checksums_obj = self.mpfs_checksums.copy()
        checksums_obj.size = 333
        with KladunStub(checksums_obj=self.mpfs_checksums):
            assert HashConflictReportProcessor(self.get_report(checksums_obj)).process()[0] == ProcessStatus.no_file_in_storage
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is True

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_mpfs_hash_mismatch(self):
        checksums_obj = self.mpfs_checksums.copy()
        checksums_obj.size = 333
        with KladunStub(checksums_obj=checksums_obj), \
            PreviewerStub(checksums_obj=checksums_obj):
            assert HashConflictReportProcessor(self.get_report(checksums_obj)).process()[0] == ProcessStatus.storage_hash_missmatch
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is True

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_bad_storage_hash(self):
        checksums_obj = self.mpfs_checksums.copy()
        checksums_obj.size = 333
        with KladunStub(checksums_obj=checksums_obj):
            # если в mpfs и присланные хеши не совпадают, мы хеши в сторадже не
            # проверяем, т.к. лишняя нагрузка на кладун из-за странных репортов
            assert HashConflictReportProcessor(self.get_report(self.mpfs_checksums)).process()[0] == ProcessStatus.false_alarm
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_bad_external_hash(self):
        checksums_obj = self.mpfs_checksums.copy()
        checksums_obj.size = 333
        with KladunStub(checksums_obj=self.mpfs_checksums), \
            PreviewerStub(checksums_obj=self.mpfs_checksums):
            assert HashConflictReportProcessor(self.get_report(checksums_obj)).process()[0] == ProcessStatus.external_hash_missmatch
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is False

    @RestoreDBServiceStub()
    @MulcaServiceStub(is_file_exist=True)
    def test_missmatch(self):
        checksums_obj_1 = self.mpfs_checksums.copy()
        checksums_obj_1.size = 332
        checksums_obj_2 = self.mpfs_checksums.copy()
        checksums_obj_2.size = 333
        with KladunStub(checksums_obj=checksums_obj_1), \
            PreviewerStub(checksums_obj=checksums_obj_1):
            assert HashConflictReportProcessor(self.get_report(checksums_obj_2)).process()[0] == ProcessStatus.storage_hash_missmatch
            assert restore_db_service.is_personal_exists(self.uid, self.mpfs_checksums.hid) is True


class RestoreFileOperationTestCase(BaseReportProcessorTestCase):
    def get_file_stids(self, address):
        resp = self.json_ok('info', {'uid': address.uid, 'path': address.path, 'meta': ''})
        result = {}
        for mid in 'file_mid', 'digest_mid', 'pmid':
            result[mid] = resp['meta'][mid]
        return result

    def get_file_checksums(self, address):
        resp = self.json_ok('info', {'uid': self.uid, 'path': address.path, 'meta': ''})
        return FileChecksums(resp['meta']['md5'], resp['meta']['sha256'], resp['meta']['size'])

    @RestoreDBServiceStub()
    def test_common(self):
        address = Address.Make(self.uid, '/disk/1.txt')
        self.upload_file(self.address.uid, self.address.path)

        checksums = self.get_file_checksums(address)
        before_stids = self.get_file_stids(address)

        restore_db_service.put_personal(self.uid, checksums)
        resp = self.json_ok('restore_file', {
            'uid': self.uid,
            'md5': checksums.md5,
            'sha256': checksums.sha256,
            'size': checksums.size,
        })
        assert 'upload_url' in resp
        assert 'oid' in resp
        assert resp['type'] == 'store'
        KladunMocker().mock_kladun_callbacks_for_store(self.uid, resp['oid'], checksums_obj=checksums)

        # сняли с восстановления
        assert restore_db_service.is_exists(self.uid, checksums) is False
        after_stids = self.get_file_stids(address)
        # файлу пользователя обновили stid-ы
        assert after_stids['file_mid'] != before_stids['file_mid']
        assert after_stids['digest_mid'] != before_stids['digest_mid']
