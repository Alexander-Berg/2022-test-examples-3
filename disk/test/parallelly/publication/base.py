#!/usr/bin/python
# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from test.fixtures.users import user_1

import mpfs.engine.process

from mpfs.core.address import Address
from mpfs.core.bus import Bus

from mpfs.frontend.request import UserRequest
UserRequest.user_ip = ''


class BasePublicationMethods(DiskTestCase):
    uid_1 = user_1.uid
    secret_data = 'this is top secret'
    pub_folder = u'/disk/pub/'
    pub_foldername = u'pub'

    login = 'mpfs-test'

    pub_filename = u'public.some.info file.ext'
    pub_file = pub_folder + pub_filename

    pub_subfolder = u'/disk/pub/subfolder/'
    pub_subfilename = 'pubic subfile'
    pub_subfile = pub_subfolder + pub_subfilename

    relative_subfile_path = '/subfolder/' + pub_subfilename
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

    def make_dir(self, is_public=False):
        opts = {
            'uid': self.uid,
            'path': self.pub_folder,
        }
        try:
            self.json_ok('mkdir', opts)
        except Exception:
            pass
        if is_public:
            opts = {'uid': self.uid, 'path': self.pub_folder, 'connection_id': ''}
            self.json_ok('set_public', opts)

    def make_file(self, is_public=True):
        faddr = Address.Make(self.uid, self.pub_file).id
        self.make_dir()
        Bus().mkfile(self.uid, faddr, data=self.file_data)
        if is_public:
            opts = {'uid': self.uid, 'path': self.pub_file, 'connection_id': ''}
            self.json_ok('set_public', opts)

    def grab_public_file(self):
        self.make_file()
        faddr = Address.Make(self.uid, self.pub_file).id

        resource = Bus().resource(self.uid, faddr)
        hash_ = resource.get_public_hash()
        self.json_ok('public_copy', {
            'uid': self.uid,
            'private_hash': hash_,
            'name': None,
            'connection_id': ''
        })
        self.json_ok('info', {
            'uid': self.uid,
            'path': Address.Make(self.uid, u'/disk/Загрузки/' + self.pub_filename).id,
            'connection_id': ''
        })
