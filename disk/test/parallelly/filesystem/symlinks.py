# -*- coding: utf-8 -*-
import copy

from base import CommonFilesystemTestCase

import mpfs.engine.process

from mpfs.core.bus import Bus
from mpfs.common import errors
from mpfs.core.address import Address
from mpfs.core.filesystem import symlinks
from mpfs.core.filesystem.symlinks import Symlink
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class SymlinksFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(SymlinksFilesystemTestCase, self).setup_method(method)
        self.file_data = copy.deepcopy(SymlinksFilesystemTestCase.file_data)
        self._mkdirs()
        self._mkfiles()

    def test_rm_file_rm_symlink(self):
        # simple: rm file -> rm symlink
        faddr = Address.Make(self.uid, '/disk/filesystem test file').id
        symlink = Bus().symlink(self.uid, faddr)
        self.assertEqual(symlink.target_addr(), faddr)
        Bus().rm(self.uid, faddr)
        self.assertRaises(errors.SymlinkNotFound, symlinks.Symlink, symlink.address)

    def test_move_file_update_symlink(self):
        # complicated: move file -> update symlink
        inner_file = Address.Make(self.uid, '/disk/filesystem test folder' + '/inner file').id
        moved_inner_file = Address.Make(self.uid, '/disk/filesystem test folder moved' + '/inner file').id
        symlink = Bus().symlink(self.uid, inner_file)
        symlink_addr = symlink.address

        src = Address.Make(self.uid, '/disk/filesystem test folder')
        dst = Address.Make(self.uid, '/disk/filesystem test folder moved')
        Bus().move_resource(self.uid, src.id, dst.id, force=True)
        self.assertEquals(Symlink(symlink_addr).target_addr(), moved_inner_file)
        Bus().move_resource(self.uid, dst.id, src.id, force=True)

    def test_copy_file_do_not_copy_symlink(self):
        # complicated: copy file -> do not copy symlink
        inner_file = Address.Make(self.uid, '/disk/filesystem test folder/inner file').id
        src = Address.Make(self.uid, '/disk/filesystem test folder')
        dst = Address.Make(self.uid, '/disk/filesystem test folder moved')

        copied_inner_file = Address.Make(self.uid, '/disk/filesystem test folder moved' + '/inner file').id
        link = Bus().symlink(self.uid, inner_file)
        Bus().copy_resource(self.uid, src.id, dst.id, force=False)
        self.assertEqual(Bus().resource(self.uid, copied_inner_file).get_symlink(), None)

    def test_overwrite_file_without_force_do_not_leave_symlink(self):
        # complicated: overwrite -> do not leave symlink
        address = Address.Make(self.uid, '/disk/overwritten file')
        Bus().mkfile(self.uid, address.id, data=self.file_data)
        symlink = Bus().symlink(self.uid, address.id)
        Bus().mkfile(self.uid, address.id, data=self.file_data)
        self.assertRaises(errors.SymlinkNotFound, symlinks.Symlink, symlink.address)

    def test_overwrite_file_with_force_leave_symlink(self):
        # leave symlink if file is overrided
        faddr = Address.Make(self.uid, '/disk/overrided_file').id
        Bus().mkfile(self.uid, faddr, data=self.file_data)
        symlink = Bus().symlink(self.uid, faddr)
        Bus().mkfile(self.uid, faddr, data=self.file_data, keep_symlinks=True)
        self.assertEqual(Bus().resource(self.uid, faddr).get_symlink(), symlinks.Symlink(symlink.address).address.id)

    def test_rm_folder_with_symlink_rm_symlink(self):
        # complicated: rm folder with symlinked file -> rm symlink
        faddr = Address.Make(self.uid, '/disk/filesystem test folder' + '/inner file').id
        symlink = Bus().symlink(self.uid, faddr)
        self.assertEqual(symlink.target_addr(), faddr)
        faddr = Address.Make(self.uid, '/disk/filesystem test folder').id
        Bus().rm(self.uid, faddr)
        self.assertRaises(errors.SymlinkNotFound, symlinks.Symlink, symlink.address)

    def test_move_file_to_trash_do_not_remove_symlink(self):
        # complicated: move to trash -> do not remove symlink
        inner_file = Address.Make(self.uid, '/disk/filesystem test folder' + '/inner file').id
        symlink = Bus().symlink(self.uid, inner_file)

        src = Address.Make(self.uid, '/disk/filesystem test folder')
        Bus().trash_append(self.uid, src.id)
        symlinks.Symlink(symlink.address)