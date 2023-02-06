# -*- coding: utf-8 -*-
import cjson
import mock

from hamcrest import assert_that, calling, raises
from httplib import REQUEST_ENTITY_TOO_LARGE
from nose_parameterized import parameterized

from mpfs.common import errors
from mpfs.common.static import codes
from mpfs.core.operations.filesystem.bulk import BulkActionOperation
from test.base import DiskTestCase

from mpfs.config import settings
from mpfs.core.address import Address
from mpfs.core.operations import manager
from mpfs.common.static.codes import *
from mpfs.common.static.tags import *
from mpfs.core.bus import Bus


FEATURE_TOGGLES_BULK_ACTIONS_LIMIT = settings.feature_toggles['bulk_actions_limit']


class TestBulkAction(DiskTestCase):
    
    nonempty_fields = ('id', 'uid', 'type', 'ctime', 'mtime', 'name',)
    
    file_fields = nonempty_fields + ('visible', 'labels', 'meta',)
    
    folder_fields = file_fields
    
    file_data = {
        "meta": {
            "file_mid": "1000003.yadisk:89031628.249690056312488962060095667221",
            "digest_mid": "1000005.yadisk:89031628.3983296384177350807526090116783",
            "md5": "83e5cd52e94e3a41054157a6e33226f7",
            "sha256": "4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865",
        },
        "size": 10000,
        "mimetype": "text/plain",
    }
    
    def setup_method(self, method):
        super(TestBulkAction, self).setup_method(method)

        def _assert_make_folder(fid):
            faddr = Address.Make(self.uid, fid).id
            result = Bus().mkdir(self.uid, faddr)
            self.assertEqual(fid, result['id'])
            for field in self.folder_fields:
                self.assertTrue(field in result)
                if field in self.nonempty_fields:
                    self.assertNotEqual(result[field], None)

        def _assert_make_file(fid):
            faddr = Address.Make(self.uid, fid).id
            result = Bus().mkfile(self.uid, faddr, data=self.file_data).dict()
            _file = Bus().resource(self.uid, faddr)
            self.assertEqual(fid, result['id'])
            for field in self.file_fields:
                self.assertTrue(field in result)
                if field in self.nonempty_fields:
                    self.assertNotEqual(result[field], None)
            for k, v in self.file_data.iteritems():
                if k == 'meta':
                    for _k, _v in v.iteritems():
                        self.assertEqual(_v, result[k][_k])
                else:
                    self.assertEqual(v, result[k])

        for fid in ('/disk/filesystem test folder',
                    '/disk/filesystem test folder/inner folder',
                    '/disk/filesystem test folder/inner folder/subinner folder'):
            _assert_make_folder(fid)

        for fid in ('/disk/filesystem test file', '/disk/filesystem test folder/inner file'):
            _assert_make_file(fid)

    def test_good_operations(self):
        """Проверяем нормальный запрос и успешность выполнения"""
        bulk_action = cjson.encode(
            [
                {
                    'action': 'move', 
                    'params': {
                      'uid'  : self.uid,
                      'src'  : self.uid + ':/disk/filesystem test folder',
                      'dst'  : self.uid + ':/disk/filesystem test folder moved',
                      'force': None,
                    }
                },
                {
                    'action': 'move',
                    'params': {
                      'uid'  : self.uid,
                      'src'  : self.uid + ':/disk/filesystem test folder moved',
                      'dst'  : self.uid + ':/disk/filesystem test folder',
                      'force': None,
                    }
                },
                {
                    'action': 'rm',
                    'params': {
                      'uid' : self.uid,
                      'path': self.uid + ':' + '/disk/filesystem test folder',
                    }
                },
            ]
        )
        
        operation = manager.create_operation(
            self.uid,
            'bulk',
            'filesystem',
            odata=dict(cmd=bulk_action),
        )
        operation = manager.get_operation(operation.uid, operation.id)
        result = operation.get_status()
        self.assertEqual(result[STATUS], COMPLETED)

        for item in result[PROTOCOL]:
            self.assertEqual(item[STATUS], 'DONE')
            
    def test_bad_description(self):
        """Проверяем плохой запрос"""
        bulk_action_bad_params = '[ {\\\} ]'
        
        operation = manager.create_operation(
            self.uid,
            'bulk',
            'filesystem',
            odata=dict(cmd=bulk_action_bad_params),
        )

        operation = manager.get_operation(operation.uid, operation.id)
        result = operation.get_status()
        self.assertEqual(result[STATUS], FAILED)

    def test_validator_in_handler(self):
        with mock.patch('mpfs.core.operations.filesystem.bulk.FEATURE_TOGGLES_BULK_ACTIONS_CHECK_DRY_RUN', False):
            self.json_error('iddqd', {'uid': self.uid,
                                      'cmd': cjson.encode([{'action': 'move'}] * (FEATURE_TOGGLES_BULK_ACTIONS_LIMIT + 1))},
                            code=codes.BULK_ACTIONS_LIMIT_EXCEEDED, status=REQUEST_ENTITY_TOO_LARGE)


class ValidatorTestCase(DiskTestCase):
    @parameterized.expand([
        ('too_many_actions', cjson.encode([{'action': 'move'}] * (FEATURE_TOGGLES_BULK_ACTIONS_LIMIT + 1)),
         errors.BulkActionsLimitExceeded),
        ('non_json', "' OR '1'='1' --",
         errors.BadRequestError),
    ])
    def test_validator_raises(self, case_name, body, expected_exception):
        u"""Проверяем валидатор данных на кейсах, когда нужно бросить исключение"""
        with mock.patch('mpfs.core.operations.filesystem.bulk.FEATURE_TOGGLES_BULK_ACTIONS_CHECK_DRY_RUN', False):
            assert_that(calling(BulkActionOperation.check_actions_limit).with_args(self.uid, body),
                        raises(expected_exception))

    @parameterized.expand([
        ('actions_around_limimt', cjson.encode([{'action': 'move'}] * FEATURE_TOGGLES_BULK_ACTIONS_LIMIT),
         False),
        ('too_many_actions_dry_run', cjson.encode([{'action': 'move'}] * (FEATURE_TOGGLES_BULK_ACTIONS_LIMIT + 1)),
         True),
        ('non_json_dry_run', "' OR '1'='1' --",
         True),
    ])
    def test_validator_pass(self, case_name, body, dry_run):
        u"""Проверяем валидатор данных на кейсах, когда нужно бросить исключение"""
        with mock.patch('mpfs.core.operations.filesystem.bulk.FEATURE_TOGGLES_BULK_ACTIONS_CHECK_DRY_RUN', dry_run):
            BulkActionOperation.check_actions_limit(self.uid, body)
