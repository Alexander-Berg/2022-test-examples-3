# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from test.base_suit import TrashTestCaseMixin
from test.fixtures.users import user_1

from mpfs.core.bus import Bus
from mpfs.core.address import Address


class CommonFilesystemTestCase(DiskTestCase, TrashTestCaseMixin):
    second_uid = user_1.uid

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

    nonempty_fields = ('id', 'uid', 'type', 'ctime', 'mtime', 'name',)

    file_fields = nonempty_fields + ('visible', 'labels', 'meta',)

    folder_fields = file_fields

    int_fields = ('ctime', 'mtime', 'size')

    list_params = {
        'amt': 0,
        'sort': 'name',
        'order': 1,
    }
    TEST_FOLDERS = ('/disk/filesystem test folder',
                    '/disk/filesystem test folder/inner folder',
                    '/disk/filesystem test folder/inner folder/subinner folder')
    TEST_FILES = ('/disk/filesystem test file', '/disk/filesystem test folder/inner file')

    def _mkdirs(self):
        for folder in self.TEST_FOLDERS:
            self.mkdir(folder)

    def _mkfiles(self):
        for file_path in self.TEST_FILES:
            self.mkfile(file_path)

    def mkdir(self, folder):
        address = Address.Make(self.uid, folder).id
        Bus().mkdir(self.uid, address)

    def mkfile(self, file_path):
        address = Address.Make(self.uid, file_path).id
        Bus().mkfile(self.uid, address, data=self.file_data)
