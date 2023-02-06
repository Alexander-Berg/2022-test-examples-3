# -*- coding: utf-8 -*-

from test.common.sharing import CommonSharingMethods


class BlockSharingFilesTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(BlockSharingFilesTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()
        self.upload_file(self.uid_1, '/disk/file3')

    def test_source_uid(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file3')
        self.upload_file(self.uid_3, '/disk/file_to_move')
        self.upload_file(self.uid, '/disk/new_folder/folder2/file1')
        self.json_ok('copy', {"uid": self.uid_3, "src": "/disk/file_to_move", "dst": "/disk/folder2/copied"})
        self.json_ok('move', {"uid": self.uid_3, "src": "/disk/file_to_move", "dst": "/disk/folder2/moved"})
        self.json_ok('copy', {"uid": self.uid, "src": "/disk/new_folder/folder2/file3", "dst": "/disk/not_shared"})
        self.json_ok('copy', {"uid": self.uid, "src": "/disk/not_shared", "dst": "/disk/new_folder/folder2/shared_second"})
        for uid, path, have_source_uid in (
            (self.uid, "/disk/new_folder/folder2/file1", False),
            (self.uid, "/disk/new_folder/folder2/file3", True),
            (self.uid_3, "/disk/folder2/file1", False),
            (self.uid_3, "/disk/folder2/file3", True),
            (self.uid_3, "/disk/folder2/shared_second", True),
            (self.uid, "/disk/new_folder/folder2/copied", True),
            (self.uid, "/disk/new_folder/folder2/moved", True),
        ):
            info = self.json_ok('info', {"uid": uid, "path": path, "meta": "source_uid"})
            if have_source_uid:
                assert info['meta']['source_uid'] == self.uid_3
            else:
                assert 'source_uid' not in info['meta']
