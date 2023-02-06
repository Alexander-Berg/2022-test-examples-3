# -*- coding: utf-8 -*-
from test.common.sharding import CommonShardingMethods


class BaseShardingMethods(CommonShardingMethods):

    def upload_and_hardlink(self, uid1, uid2):
        self.upload_file(uid1, '/disk/random.file')
        file_info = self.json_ok('info', {
            'uid': uid1,
            'path': '/disk/random.file',
            'meta': '',
        })

        with self.patch_mulca_is_file_exist(func_resp=True):
            res = self.json_ok('store', {'uid': uid2,
                                         'path': '/disk/random.file.hardlinked',
                                         'sha256': file_info['meta']['sha256'],
                                         'md5': file_info['meta']['md5'],
                                         'size': file_info['meta']['size']})
        self.assertEqual(res['status'], 'hardlinked')

        new_file_info = self.json_ok('info', {
            'uid': uid2,
            'path': '/disk/random.file.hardlinked',
            'meta': '',
        })

        self.assertNotEqual(new_file_info, None)

    def create_shared_folder(self, uid1, uid2, path=None, rights=660):
        opts = {'uid': uid1, 'path': path}
        self.json_ok('mkdir', opts)
        self.json_ok('share_create_group', opts)

        opts = {
            'rights': rights,
            'universe_login': 'boo@boo.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': uid1,
            'path': path,
        }
        result = self.mail_ok('share_invite_user', opts)

        hsh = None
        for each in result.getchildren():
            if each.tag == 'hash' and each.text and isinstance(each.text, str):
                hsh = each.text
        assert hsh

        opts = {
            'hash': hsh,
            'uid': uid2,
        }
        folder_info = self.json_ok('share_activate_invite', opts)
        assert folder_info is not None