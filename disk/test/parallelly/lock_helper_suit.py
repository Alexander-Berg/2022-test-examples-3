# -*- coding: utf-8 -*-

from test.common.sharing import CommonSharingMethods
from mpfs.core.filesystem.helpers.lock import LockHelper
from mpfs.core.address import Address
from mpfs.common.errors import ResourceNotFound, ResourceLocked, OperationNotFound



class LockHelperForSharingTestCase(CommonSharingMethods):
    def test_invited_lock_first(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/s'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/s/1'})
        gid = self.create_group(path='/disk/s')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660, path='/disk/s')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.json_ok('move', {'uid': self.uid_3, 'src': '/disk/s', 'dst': '/disk/s3'})

        owner_resource_address = Address.Make(self.uid, '/disk/s/2')
        invited_resource_address = Address.Make(self.uid_3, '/disk/s3/2')

        LockHelper.lock_by_address(invited_resource_address)
        with self.assertRaises(ResourceLocked):
            LockHelper.lock_by_address(owner_resource_address)

    def test_owner_lock_first(self):
        self.json_ok('user_init', {'uid': self.uid_3})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/s'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/s/1'})
        gid = self.create_group(path='/disk/s')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660, path='/disk/s')
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.json_ok('move', {'uid': self.uid_3, 'src': '/disk/s', 'dst': '/disk/s3'})

        owner_resource_address = Address.Make(self.uid, '/disk/s/2')
        invited_resource_address = Address.Make(self.uid_3, '/disk/s3/2')

        LockHelper.lock_by_address(owner_resource_address)
        with self.assertRaises(ResourceLocked):
            LockHelper.lock_by_address(invited_resource_address)

    def test_common(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        LockHelper.lock_by_address(Address.Make(self.uid, '/disk/2'))
        self.json_error('move', {'uid': self.uid, 'src': '/disk/1', 'dst': '/disk/2'})
