# -*- coding: utf-8 -*-
import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


class ModifyUidTestCase(CommonSharingMethods):
    SHARE_FOLDER = '/disk/share'
    RESOURCE_NAME = 'folder_or_file'

    def setup_method(self, method):
        super(ModifyUidTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_3})

        self.OWNER = self.uid
        self.INVITED = self.uid_3

        self.json_ok('mkdir', {'uid': self.OWNER, 'path': self.SHARE_FOLDER})
        gid = self.json_ok('share_create_group', {'uid': self.OWNER, 'path': self.SHARE_FOLDER})['gid']
        hsh = self.json_ok('share_invite_user', {'uid': self.OWNER, 'gid': gid, 'universe_login': self.email_3, 'universe_service': 'email', 'rights': '660'})['hash']
        self.json_ok('share_activate_invite', {'uid': self.INVITED, 'hash': hsh})

    def get_modify_uid(self, uid, path):
        return self.json_ok('info', {'uid': uid, 'path': path, 'meta': 'modify_uid'})['meta']['modify_uid']

    def get_modify_uid_from_db(self, path):
        db = CollectionRoutedDatabase()
        doc_data = db.user_data.find_one({'uid': self.OWNER, 'path': path})['data']
        if 'modify_uid' in doc_data:
            return doc_data['modify_uid']

    def assert_modify_uid(self, resource_path, expected_uid, help_text=None):
        try:
            # проверяем, что отдается правильный modify_uid со стороны
            # владельца и приглашенного
            for uid in (self.OWNER, self.INVITED):
                assert self.get_modify_uid(uid, resource_path) == expected_uid
            # проверяем на правильность хранения в БД
            ## Если ресурс создан как обычный, а потом он попал в группу, то
            ## modify_uid не будет в БД(т.к. ресурс не пересохраняется) - это ок
            modify_uid_in_db = self.get_modify_uid_from_db(resource_path)
            assert modify_uid_in_db == expected_uid
        except AssertionError:
            if help_text:
                print help_text
            raise


class CreateResourceModifyUidTestCase(ModifyUidTestCase):
    """Создание ресурсов"""

    def _test_store(self, uid):
        file_path = '%s/1.txt' % self.SHARE_FOLDER
        self.upload_file(uid, file_path)
        self.assert_modify_uid(file_path, uid)

    def test_owner_store(self):
        self._test_store(self.OWNER)

    def test_invited_store(self):
        self._test_store(self.INVITED)

    def _test_hardlink(self, uid):
        file_path = '%s/somefile.txt' % self.SHARE_FOLDER
        hardlink_path = "%s.hardlinked" % file_path

        # грузим файл
        self.upload_file(self.uid, file_path)
        # получаем его данные
        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})
        size, sha256, md5 = info['meta']['size'], info['meta']['sha256'], info['meta']['md5']

        with self.patch_mulca_is_file_exist(func_resp=True):
            # делаем загрузку хардлинком в attach
            first_result = self.json_ok(
                'store', {
                    'uid': uid,
                    'path': hardlink_path,
                    'md5': md5,
                    'sha256': sha256,
                    'size': size
                }
            )
            assert first_result == {u'status': u'hardlinked'}

        self.assert_modify_uid(hardlink_path, uid)

    def test_owner_harldlink(self):
        self._test_hardlink(self.OWNER)

    def test_invited_hardlink(self):
        self._test_hardlink(self.INVITED)

    def _test_mkdir(self, uid):
        dir_path = '%s/dir' % self.SHARE_FOLDER
        self.json_ok('mkdir', {'uid': uid, 'path': dir_path})
        self.assert_modify_uid(dir_path, uid)

    def test_owner_mkdir(self):
        self._test_mkdir(self.OWNER)

    def test_invited_mkdir(self):
        self._test_mkdir(self.INVITED)


class MoveCopyResourceModifyUidTestCase(ModifyUidTestCase):
    """Перемещаем/копируем ресурс"""

    def _test_copy_move(self, uid, oper_name, resource_type):
        for src_modify_uid in (self.OWNER, self.INVITED):
            src_path = '%s/src_%s_%s' % (self.SHARE_FOLDER, resource_type, src_modify_uid)
            dst_path = '%s/dst_%s_%s' % (self.SHARE_FOLDER, resource_type, src_modify_uid)

            if resource_type == 'dir':
                self.json_ok('mkdir', {'uid': src_modify_uid, 'path': src_path})
            elif resource_type == 'file':
                self.upload_file(src_modify_uid, src_path)
            else:
                raise NotImplementedError()
            self.assert_modify_uid(src_path, src_modify_uid)

            self.json_ok(oper_name, {'uid': uid, 'src': src_path, 'dst': dst_path})

            self.assert_modify_uid(dst_path, uid)
            if oper_name == 'copy':
                self.assert_modify_uid(src_path, src_modify_uid)

    def test_owner_move_file(self):
        self._test_copy_move(self.OWNER, 'move', 'file')

    def test_invited_move_file(self):
        self._test_copy_move(self.INVITED, 'move', 'file')

    def test_owner_move_dir(self):
        self._test_copy_move(self.OWNER, 'move', 'dir')

    def test_invited_move_dir(self):
        self._test_copy_move(self.INVITED, 'move', 'dir')

    def test_owner_copy_file(self):
        self._test_copy_move(self.OWNER, 'copy', 'file')

    def test_invited_copy_file(self):
        self._test_copy_move(self.INVITED, 'copy', 'file')

    def test_owner_copy_dir(self):
        self._test_copy_move(self.OWNER, 'copy', 'dir')

    def test_invited_copy_dir(self):
        self._test_copy_move(self.INVITED, 'copy', 'dir')


class CopyMoveFolderModifyUidTestCase(ModifyUidTestCase):
    """Перемещаем/копируем папку с подресурсами"""

    def _test_copy_move_folder(self, uid, oper_name, resource_type):
        for src_modify_uid in (self.OWNER, self.INVITED):
            src_folder_path = '%s/src_folder_%s' % (self.SHARE_FOLDER, src_modify_uid)
            dst_folder_path = '%s/dst_folder_%s' % (self.SHARE_FOLDER, src_modify_uid)
            src_sub_resource_path = '%s/%s_%s' % (src_folder_path, resource_type, src_modify_uid)
            dst_sub_resource_path = '%s/%s_%s' % (dst_folder_path, resource_type, src_modify_uid)

            self.json_ok('mkdir', {'uid': src_modify_uid, 'path': src_folder_path})
            if resource_type == 'dir':
                self.json_ok('mkdir', {'uid': src_modify_uid, 'path': src_sub_resource_path})
            elif resource_type == 'file':
                self.upload_file(src_modify_uid, src_sub_resource_path)
            else:
                raise NotImplementedError()
            self.assert_modify_uid(src_folder_path, src_modify_uid, help_text='У созданной папки modify_uid = uid создателя(%s)' % src_modify_uid)
            self.assert_modify_uid(src_sub_resource_path, src_modify_uid, help_text='У созданного подресурса modify_uid = uid создателя(%s)' % src_modify_uid)

            self.json_ok(oper_name, {'uid': uid, 'src': src_folder_path, 'dst': dst_folder_path})

            self.assert_modify_uid(dst_folder_path, uid, help_text='У папки modify_uid должен стать %s' % uid)
            self.assert_modify_uid(dst_sub_resource_path, src_modify_uid, help_text='У подресурса modify_uid не должен меняться')
            if oper_name == 'copy':
                self.assert_modify_uid(src_folder_path, src_modify_uid, help_text='Источник папка не должен меняться')
                self.assert_modify_uid(src_sub_resource_path, src_modify_uid, help_text='Источник ресурс не должен меняться')

    def test_owner_move_folder_with_file(self):
        self._test_copy_move_folder(self.OWNER, 'move', 'file')

    def test_invited_move_folder_with_file(self):
        self._test_copy_move_folder(self.INVITED, 'move', 'file')

    def test_owner_move_folder_with_dir(self):
        self._test_copy_move_folder(self.OWNER, 'move', 'dir')

    def test_invited_move_folder_with_dir(self):
        self._test_copy_move_folder(self.INVITED, 'move', 'dir')

    def test_owner_copy_folder_with_file(self):
        self._test_copy_move_folder(self.OWNER, 'copy', 'file')

    def test_invited_copy_folder_with_file(self):
        self._test_copy_move_folder(self.INVITED, 'copy', 'file')

    def test_owner_copy_folder_with_dir(self):
        self._test_copy_move_folder(self.OWNER, 'copy', 'dir')

    def test_invited_copy_folder_with_dir(self):
        self._test_copy_move_folder(self.INVITED, 'copy', 'dir')
