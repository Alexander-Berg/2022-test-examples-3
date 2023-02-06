# -*- coding: utf-8 -*-
import mpfs.engine.process

from mpfs.common.static import codes
from nose_parameterized import parameterized
from hamcrest import (
    assert_that,
    empty,
    is_,
)

from test.parallelly.json_api.base import CommonJsonApiTestCase


class CommentsTestCase(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(CommentsTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/nested'})
        self.upload_file(self.uid, '/disk/dir/1.txt')
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.entities = ['/disk/dir', '/disk/dir/1.txt', '/disk/dir/nested']

    def test_comments_owner(self):
        """
        Тестирование ручки comments_owner
        """
        dir_meta = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'comment_ids'})['meta']
        self.json_error('comments_owner', {'uid': self.uid, 'entity_type': 'public_resource',
                                           'entity_id': dir_meta['comment_ids']['public_resource']})
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        resp = self.json_ok('comments_owner', {'uid': self.uid, 'entity_type': 'public_resource',
                                               'entity_id': dir_meta['comment_ids']['public_resource']})
        assert resp == {"is_owner": True}
        resp = self.json_ok('comments_owner', {'uid': self.uid_3, 'entity_type': 'public_resource',
                                               'entity_id': dir_meta['comment_ids']['public_resource']})
        assert resp == {"is_owner": False}
        self.json_error('comments_owner',
                        {'uid': self.uid, 'entity_type': 'public_resource', 'entity_id': '1234:43121'}, )

    @parameterized.expand([
        ('folder', '/disk/dir'),
        ('file', '/disk/dir/1.txt'),
    ])
    def test_comments_permissions_on_public_resource(self, case_name, public_resource_path):
        u"""
        Тестирование ручки comments_permissions на публичных ресурсах
        """
        entity_type = 'public_resource'
        dir_meta = self.json_ok('info', {'uid': self.uid, 'path': public_resource_path, 'meta': 'comment_ids'})['meta']

        self.json_error('comments_permissions', {'uid': self.uid, 'entity_type': entity_type,
                                                 'entity_id': dir_meta['comment_ids']['public_resource']})

        self.json_ok('set_public', {'uid': self.uid, 'path': public_resource_path})
        args = {'uid': self.uid, 'path': public_resource_path, 'meta': 'comment_ids'}

        correct_answer = {'view': True, 'comment': True, 'clean': True, 'disable': True}
        entity_id = self.json_ok('info', args)['meta']['comment_ids']['public_resource']
        self._check_comments_permissions(self.uid, entity_type, entity_id, correct_answer)

        correct_answer = {'view': True, 'comment': True, 'clean': False, 'disable': False}
        entity_id = self.json_ok('info', args)['meta']['comment_ids']['public_resource']
        self._check_comments_permissions(self.uid_3, entity_type, entity_id, correct_answer)

    @parameterized.expand([
        ('read_write', 660, {'view': True, 'comment': True, 'clean': False, 'disable': False}),
        ('read_only', 640, {'view': True, 'comment': True, 'clean': False, 'disable': False}),
    ])
    def test_comments_permissions_on_shared_folder_with_owner_entity_id(self, case_name, rights, correct_permissions):
        u"""
        Тестирование ручки comments_permissions на шаренной папке с id владельца и uid приглашенного
        """
        public_resource_path = '/disk/dir'
        entity_type = 'private_resource'
        self.share_dir(self.uid, self.uid_1, self.email_1, public_resource_path, rights=rights)
        owner_dir_meta = self.json_ok('info',
                                      {'uid': self.uid, 'path': public_resource_path, 'meta': 'comment_ids'})['meta']
        self._check_comments_permissions(self.uid_1, entity_type,
                                         owner_dir_meta['comment_ids']['public_resource'], correct_permissions)

    def test_comments_permissions_on_shared_resources_for_owner(self):
        u"""
        Тестирование ручки comments_permissions на шаренной папке для владельца
        """
        public_resource_path = '/disk/dir'
        entity_type = 'private_resource'
        correct_answer = {'view': True, 'comment': True, 'clean': True, 'disable': False}
        self._check_all_own_comments_permissions(self.uid, entity_type, correct_answer)
        self.share_dir(self.uid, self.uid_1, self.email_1, public_resource_path, 660)
        self._check_all_own_comments_permissions(self.uid, entity_type, correct_answer)

    @parameterized.expand([
        ('read_write', 660, {'view': True, 'comment': True, 'clean': False, 'disable': False}),
        ('read_only', 640, {'view': True, 'comment': True, 'clean': False, 'disable': False}),
    ])
    def test_comments_permissions_on_shared_resources_for_invitees(self, case_name, rights, correct_permissions):
        u"""
        Тестирование ручки comments_permissions на шаренной папке для приглашенных
        """
        public_resource_path = '/disk/dir'
        entity_type = 'private_resource'
        owner_dir_meta = self.json_ok(
            'info', {'uid': self.uid, 'path': public_resource_path, 'meta': 'comment_ids'})['meta']
        resp = self.json_error('comments_permissions', {'uid': self.uid_1, 'entity_type': entity_type,
                                                        'entity_id': owner_dir_meta['comment_ids']['public_resource']})
        assert resp['code'] == codes.RESOURCE_NOT_FOUND
        self.share_dir(self.uid, self.uid_1, self.email_1, public_resource_path, rights)
        self._check_all_own_comments_permissions(self.uid_1, entity_type, correct_permissions)

    @parameterized.expand([
        ('public_resource', 'public_resource'),
        ('private_resource', 'private_resource'),
    ])
    def test_comments_permissions_nonexistent_resource(self, case_name, entity_type):
        u"""
        Тестирование ручки comments_permissions на непривильном entity_id
        """
        self.json_error('comments_permissions',
                        {'uid': self.uid, 'entity_type': entity_type, 'entity_id': '1234:43121'})

    def _check_all_own_comments_permissions(self, uid, entity_type, correct_answer):
        for i in self.entities:
            entity_id = self.json_ok(
                'info', {'uid': self.uid, 'path': i, 'meta': 'comment_ids'})['meta']['comment_ids']['public_resource']
            self._check_comments_permissions(uid, entity_type, entity_id, correct_answer)

    def _check_comments_permissions(self, uid, entity_type, entity_id, correct_answer):
        resp = self.json_ok(
            'comments_permissions', {'uid': uid, 'entity_type': entity_type, 'entity_id': entity_id})
        assert resp == correct_answer


class CommentsIdsTestCase(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(CommentsIdsTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/1.txt')

    def test_comment_ids_for_private_resource(self):
        """
        Проверяем возвращаемые comments_ids для приватных ресурсов
        """
        self._validate_comment_ids_for_common_resource('/disk/dir', self.uid)
        self._validate_comment_ids_for_common_resource('/disk/dir/1.txt', self.uid)

    def test_common_storage_folders(self):
        """
        Проверяем  возвращаемые comments_ids для доменных папок
        """
        for path in ['/narod', '/disk', '/trash', '/attach']:
            resource_meta = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'comment_ids'})['meta']
            assert_that(resource_meta['comment_ids'], is_(empty()))

    def test_share_storage_folder(self):
        """
        Проверяем  возвращаемые comments_ids для доменной папки /share
        """
        import mpfs.core.services.stock_service
        share_user = mpfs.engine.process.share_user()
        mpfs.engine.process.usrctl().create(share_user)
        mpfs.core.services.stock_service._setup()
        resource_meta = self.json_ok('info', {'uid': share_user, 'path': '/share', 'meta': 'comment_ids'})['meta']
        assert_that(resource_meta['comment_ids'], is_(empty()))

    def test_comment_ids_for_public_resource(self):
        """
        Проверяем возвращаемые comments_ids для публичных ресурсов
        """
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})

        self._validate_comment_ids_for_common_resource('/disk/dir', self.uid)
        self._validate_comment_ids_for_common_resource('/disk/dir/1.txt', self.uid)

    def test_comment_ids_for_shared_resource(self):
        """
        Проверяем возвращаемые comments_ids для общих ресурсов
        """
        self.json_ok('user_init', {'uid': self.uid_1})
        self.share_dir(self.uid, self.uid_1, self.email_1, '/disk/dir')

        self._validate_comment_ids_for_share_root_folder('/disk/dir', self.uid)
        self._validate_comment_ids_for_share_root_folder('/disk/dir', self.uid_1)

        self._validate_comment_ids_for_common_resource('/disk/dir/1.txt', self.uid)
        self._validate_comment_ids_for_common_resource('/disk/dir/1.txt', self.uid_1)

    def _validate_comment_ids_for_common_resource(self, path, uid):
        resource_meta = self.json_ok('info', {'uid': uid, 'path': path, 'meta': 'resource_id'})['meta']
        correct_comment_ids = {
            'public_resource': resource_meta['resource_id'],
            'private_resource': resource_meta['resource_id'],
        }
        r = self.json_ok('info', {'uid': uid, 'path': path, 'meta': 'comment_ids'})
        assert r['meta']['comment_ids'] == correct_comment_ids

    def _validate_comment_ids_for_share_root_folder(self, path, incomer_uid):
        owner_resource_meta = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'resource_id'})['meta']
        invitee_resource_meta = self.json_ok('info', {'uid': incomer_uid, 'path': path, 'meta': 'resource_id'})['meta']
        invitee_correct_comment_ids = {
            'public_resource': invitee_resource_meta['resource_id'],
            'private_resource': owner_resource_meta['resource_id'],
        }

        r = self.json_ok('info', {'uid': incomer_uid, 'path': path, 'meta': 'comment_ids'})
        assert r['meta']['comment_ids'] == invitee_correct_comment_ids


class InfoByCommentIdTestCase(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(InfoByCommentIdTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/nested'})
        self.upload_file(self.uid, '/disk/dir/1.txt')
        self.json_ok('user_init', {'uid': self.uid_1})
        self.entities = ['/disk/dir', '/disk/dir/1.txt', '/disk/dir/nested']

    @parameterized.expand([
        ('folder', '/disk/dir'),
        ('file', '/disk/dir/1.txt'),
    ])
    def test_info_by_comment_id_for_public_resource(self, case_name, path):
        u"""
        Тестируем поведение ручки info_by_comment_id на публичный ресурс
        """
        self.json_ok('set_public', {'uid': self.uid, 'path': path})
        entity_type = 'public_resource'

        self._assert_resource_ids_from_info_by_comment_id(self.uid, path, entity_type)

    def test_info_by_comment_id_for_not_shared_private_resource_and_no_invite(self):
        """
        Тестируем поведение ручки info_by_comment_id в случае, если пришли на нешаренный ресурс с чужим уидом
        """
        entity_type = 'private_resource'

        for path in self.entities:
            self._assert_fail_resource_ids_from_info_by_comment_id(self.uid, self.uid_1, path, entity_type, 71)

    def test_info_by_comment_id_for_not_shared_private_resource_for_owner(self):
        """
        Тестируем поведение ручки info_by_comment_id в случае, если приходит владелец на нерасшаренный ресурс
        """
        entity_type = 'private_resource'

        for path in self.entities:
            self._assert_resource_ids_from_info_by_comment_id(self.uid, path, entity_type)

    def test_info_by_comment_id_for_shared_private_resource_and_no_invite(self):
        """
        Тестируем поведение ручки info_by_comment_id в случае, если приходит пользователь без прав на расшаренный ресурс
        """
        self.share_dir(self.uid, self.uid_1, self.email_1, '/disk/dir', rights=660)
        entity_type = 'private_resource'
        self.json_ok('user_init', {'uid': self.uid_3})

        for path in self.entities:
            self._assert_fail_resource_ids_from_info_by_comment_id(self.uid, self.uid_3, path, entity_type, 71)

    @parameterized.expand([
        ('owner',),
        ('invitee',),
    ])
    def test_info_by_comment_id_for_private_resource(self, case_name):
        u"""
        Тестирование ручки info_by_comment_id на шаренной папке
        """
        self.share_dir(self.uid, self.uid_1, self.email_1, '/disk/dir', rights=660)
        entity_type = 'private_resource'

        if case_name == 'owner':
            uid = self.uid
        else:
            uid = self.uid_1

        for path in self.entities:
            self._assert_resource_ids_from_info_by_comment_id(uid, path, entity_type)

    def _assert_resource_ids_from_info_by_comment_id(self, uid, path, entity_type):
        resource_meta = self.json_ok('info',
                                     {'uid': uid, 'path': path, 'meta': 'resource_id,comment_ids'})['meta']
        correct_resource_id = resource_meta['resource_id']
        comment_id = resource_meta['comment_ids'][entity_type]
        args = {'uid': uid,
                'entity_type': entity_type,
                'entity_id': comment_id,
                'meta': 'resource_id'
                }
        result = self.json_ok('info_by_comment_id', args)
        assert correct_resource_id == result['meta']['resource_id']

    def _assert_fail_resource_ids_from_info_by_comment_id(self, owner_uid, uid, path, entity_type, error):
        resource_meta = self.json_ok('info',
                                     {'uid': owner_uid, 'path': path, 'meta': 'resource_id,comment_ids'})['meta']
        comment_id = resource_meta['comment_ids'][entity_type]
        args = {'uid': uid,
                'entity_type': entity_type,
                'entity_id': comment_id,
                'meta': 'resource_id'
                }
        result = self.json_error('info_by_comment_id', args)
        assert error == result['code']

    def test_info_by_comment_id_for_public_photounlim_file(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        photounlim_file_path = '/photounlim/photounlim_file.jpg'
        self.upload_file(self.uid, photounlim_file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': photounlim_file_path})
        entity_type = 'public_resource'
        self._assert_resource_ids_from_info_by_comment_id(self.uid, photounlim_file_path, entity_type)
