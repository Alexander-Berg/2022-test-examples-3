# -*- coding: utf-8 -*-

from mpfs.common.static import codes
from mpfs.core.filesystem.hardlinks.common import construct_hid
from mpfs.core.metastorage.control import support_blocked_hids

from test.parallelly.json_api.base import CommonJsonApiTestCase


class BlockedHidsTestCase(CommonJsonApiTestCase):
    def test_move_blocked_hid_fails(self):
        self.upload_file(self.uid, '/disk/f')
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/f', 'meta': ''})
        md5, size, sha256 = info['meta']['md5'], info['meta']['size'], info['meta']['sha256']
        hid = construct_hid(md5, size, sha256)

        support_blocked_hids.put(hid, 'test')
        self.json_error('move', {'uid': self.uid, 'src': '/disk/f', 'dst': '/disk/g'}, code=codes.HID_BLOCKED)

    def test_move_blocked_hid_with_disabled_check_succeeds(self):
        self.upload_file(self.uid, '/disk/f')
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/f', 'meta': ''})
        md5, size, sha256 = info['meta']['md5'], info['meta']['size'], info['meta']['sha256']
        hid = construct_hid(md5, size, sha256)

        support_blocked_hids.put(hid, 'test')
        self.json_ok('move', {'uid': self.uid, 'src': '/disk/f', 'dst': '/disk/g', 'check_hids_blockings': 0})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/g'})
