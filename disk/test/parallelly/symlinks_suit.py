# -*- coding: utf-8 -*-
from test.base import DiskTestCase

from mpfs.core.filesystem.symlinks import Symlink
from mpfs.core.address import Address, SymlinkAddress
from mpfs.common import errors


class TestSymlinks(DiskTestCase):
    def test_create_symlink(self):
        tgt = Address.Make(self.uid, '/disk/filesystem test file')
        linkc = Symlink.Create(tgt)
        self.assertNotEqual(linkc, None)
        self.assertEqual(linkc.target_addr(), tgt.id)
        symlink_addr = linkc.address.id
        linkn = Symlink(SymlinkAddress(symlink_addr))
        self.assertNotEqual(linkn, None)
        self.assertEqual(linkn.target_addr(), tgt.id)
        linkn.delete()
        
    def test_get_non_existing_symlink(self):
        self.assertRaises(
            errors.SymlinkNotFound,
            Symlink,
            SymlinkAddress.Make(self.uid, '32323')
        )

    def test_update_symlink(self):
        tgt  = Address.Make(self.uid, '/disk/filesystem test file')
        link = Symlink.Create(tgt)
        link.update({'tgt': 'BS'})
        symlink_addr = link.address.id
        linkn = Symlink(SymlinkAddress(symlink_addr))
        self.assertEqual(linkn.target_addr(), 'BS')
        linkn.delete()
