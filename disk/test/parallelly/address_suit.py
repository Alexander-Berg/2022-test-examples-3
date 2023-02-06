# -*- coding: utf-8 -*-

from test.base import DiskTestCase

from mpfs.core.address import Address, SymlinkAddress, PublicAddress
import mpfs.common.errors as errors
import time


class AddressTestCase(DiskTestCase):
    def test_root_address(self):
        address = Address('232:/')
        self.assertNotEqual(address, None)
        self.assertEqual(address.storage_id, '232:/')
        self.assertEqual(address.uid, '232')

    def test_system_folder(self):
        address = Address('232:/narod/')
        self.assertNotEqual(address, None)
        self.assertEqual(address.parent_id, '232:/')
        self.assertEqual(address.storage_id, '232:/narod')
        self.assertEqual(address.name, 'narod')
        self.assertEqual(address.uid, '232')

    def test_bad_system_folder(self):
        self.assertRaises(errors.AddressError, Address, 'foto')
        self.assertRaises(errors.AddressError, Address, '23323:foto')
        self.assertRaises(errors.AddressError, Address, '23323')

    def test_user_folder_address(self):
        address = Address('323:/video/stuff/test/')
        self.assertNotEqual(address, None)
        self.assertEqual(address.uid, '323')
        self.assertEqual(address.parent_id, '323:/video/stuff')
        self.assertEqual(address.parent_path, '/video/stuff')
        self.assertEqual(address.parent_name, 'stuff')
        self.assertEqual(address.storage_id, '323:/video')
        self.assertEqual(address.storage_path, '/video')
        self.assertEqual(address.storage_name, 'video')
        self.assertEqual(address.name, 'test')
        self.assertEqual(address.path, '/video/stuff/test')
        self.assertEqual(address.id, '323:/video/stuff/test/')
        self.assertEqual(address.is_folder, True)
        self.assertEqual(address.is_file, False)

    def test_user_file_path(self):
        address = Address(u'343:/disk/Документы/Важное/porn.avi', is_file=True)
        self.assertNotEqual(address, None)
        self.assertEqual(address.storage_id, '343:/disk')
        self.assertEqual(address.parent_id, u'343:/disk/Документы/Важное')
        self.assertEqual(address.name, 'porn.avi')
        self.assertEqual(address.is_file, True)

    def test_properties(self):
        self.assertEqual(Address('323:/video/').is_storage, True)
        self.assertEqual(Address('323:/').is_root, True)

    def test_clone_parent(self):
        address = Address('323:/disk/folder/stuff/')
        new_parent = Address('323:/disk/folder2/megastuff')
        new_address = address.clone_to_parent(new_parent)
        self.assertEqual(new_address.id, '323:/disk/folder2/megastuff/stuff')
        self.assertEqual(new_address.path, '/disk/folder2/megastuff/stuff')
        self.assertEqual(new_address.name, 'stuff')
        self.assertEqual(new_address.parent_id, '323:/disk/folder2/megastuff')
        self.assertEqual(new_address.parent_path, '/disk/folder2/megastuff')
        self.assertEqual(new_address.parent_name, 'megastuff')

    def test_get_child_address(self):
        address = Address('323:/disk/folder/stuff')
        child = address.get_child_resource('booo')
        self.assertEqual(child.uid, '323')
        self.assertEqual(child.id, '323:/disk/folder/stuff/booo')
        self.assertEqual(child.name, 'booo')
        self.assertEqual(child.path, '/disk/folder/stuff/booo')
        self.assertEqual(child.parent_id, '323:/disk/folder/stuff')
        self.assertEqual(child.parent_name, 'stuff')
        self.assertEqual(child.parent_path, '/disk/folder/stuff')
        self.assertEqual(child.storage_id, '323:/disk')
        self.assertEqual(child.storage_path, '/disk')
        self.assertEqual(child.storage_name, 'disk')

    def test_change_storage(self):
        address = Address('323:/disk/folder/stuff')
        address.change_storage('video')
        self.assertEqual(address.uid, '323')
        self.assertEqual(address.id, '323:/video/folder/stuff')
        self.assertEqual(address.name, 'stuff')
        self.assertEqual(address.path, '/video/folder/stuff')
        self.assertEqual(address.parent_id, '323:/video/folder')
        self.assertEqual(address.parent_name, 'folder')
        self.assertEqual(address.parent_path, '/video/folder')
        self.assertEqual(address.storage_id, '323:/video')
        self.assertEqual(address.storage_path, '/video')
        self.assertEqual(address.storage_name, 'video')

        address = Address('323:/narod/')
        address.change_storage('lnarod')
        self.assertEqual(address.storage_id, '323:/lnarod')
        self.assertEqual(address.storage_path, '/lnarod')
        self.assertEqual(address.storage_name, 'lnarod')
        self.assertEqual(address.id, '323:/lnarod/')

    def test_change_uid(self):
        address = Address('222:/disk/folder/stuff')
        address.change_uid('333')
        self.assertEqual(address.uid, '333')
        self.assertEqual(address.id, '333:/disk/folder/stuff')
        self.assertEqual(address.name, 'stuff')
        self.assertEqual(address.path, '/disk/folder/stuff')
        self.assertEqual(address.parent_id, '333:/disk/folder')
        self.assertEqual(address.parent_name, 'folder')
        self.assertEqual(address.parent_path, '/disk/folder')
        self.assertEqual(address.storage_id, '333:/disk')
        self.assertEqual(address.storage_path, '/disk')
        self.assertEqual(address.storage_name, 'disk')

    def test_change_parent_with_old(self):
        address = Address('222:/disk/old/folder/stuff')
        old_parent = Address('222:/disk/old')
        new_parent = Address('333:/disk/new')
        address.change_parent(new_parent, old_parent)
        self.assertEqual(address.uid, '333')
        self.assertEqual(address.id, '333:/disk/new/folder/stuff')
        self.assertEqual(address.name, 'stuff')
        self.assertEqual(address.path, '/disk/new/folder/stuff')
        self.assertEqual(address.parent_id, '333:/disk/new/folder')
        self.assertEqual(address.parent_name, 'folder')
        self.assertEqual(address.parent_path, '/disk/new/folder')
        self.assertEqual(address.storage_id, '333:/disk')
        self.assertEqual(address.storage_path, '/disk')
        self.assertEqual(address.storage_name, 'disk')

    def test_change_parent_without_old(self):
        address = Address('222:/disk/old/folder/stuff')
        new_parent = Address('333:/disk/new')
        address.change_parent(new_parent)
        self.assertEqual(address.uid, '333')
        self.assertEqual(address.id, '333:/disk/new/stuff')
        self.assertEqual(address.name, 'stuff')
        self.assertEqual(address.path, '/disk/new/stuff')
        self.assertEqual(address.parent_id, '333:/disk/new')
        self.assertEqual(address.parent_name, 'folder')
        self.assertEqual(address.parent_path, '/disk/new')
        self.assertEqual(address.storage_id, '333:/disk')
        self.assertEqual(address.storage_path, '/disk')
        self.assertEqual(address.storage_name, 'disk')

    def test_drop_path_to_root(self):
        address = Address('222:/disk/old/folder/stuff')
        address.change_storage('trash')
        address.drop_path_to_root()
        self.assertEqual(address.uid, '222')
        self.assertEqual(address.id, '222:/trash/stuff')
        self.assertEqual(address.name, 'stuff')
        self.assertEqual(address.path, '/trash/stuff')
        self.assertEqual(address.parent_id, '222:/trash')
        self.assertEqual(address.parent_name, 'folder')
        self.assertEqual(address.parent_path, '/trash')
        self.assertEqual(address.storage_id, '222:/trash')
        self.assertEqual(address.storage_path, '/trash')
        self.assertEqual(address.storage_name, 'trash')

    def test_make_address(self):
        address = Address.Make('222', '/disk/var', type='file')
        self.assertEqual(address.uid, '222')
        self.assertEqual(address.id, '222:/disk/var')
        self.assertEqual(address.name, 'var')
        self.assertEqual(address.path, '/disk/var')

    def test_symlink_address(self):
        self.assertRaises(errors.AddressError, SymlinkAddress, 'foto')
        self.assertRaises(errors.AddressError, SymlinkAddress, '4324324:foto_')
        self.assertRaises(errors.AddressError, SymlinkAddress, '4324324')
        self.assertRaises(errors.AddressError, SymlinkAddress, '4324324:3242ss_')
        symlink = SymlinkAddress.Make('222', '333')
        self.assertEqual(symlink.uid, '222')
        self.assertEqual(symlink.id, '222:333')
        self.assertEqual(symlink.path, '333')

    def test_mulca_address(self):
        address = Address.Make('222', '/mulca/1000008.yadisk:89031628.39832963849526532621169531905', type='file')
        self.assertEqual(address.uid, '222')
        self.assertEqual(address.id, '222:/mulca/1000008.yadisk:89031628.39832963849526532621169531905')
        self.assertEqual(address.name, 'unknown')
        address = Address.Make('222', '/mulca/1000008.yadisk:89031628.39832963849526532621169531905:1.2', type='file')
        self.assertEqual(address.uid, '222')
        self.assertEqual(address.id, '222:/mulca/1000008.yadisk:89031628.39832963849526532621169531905:1.2')

    def test_public_address(self):
        self.assertRaises(errors.AddressError, PublicAddress, 'XXX:xxx')

        address2 = PublicAddress('XXX')
        self.assertEqual(address2.hash, 'XXX')
        self.assertEqual(address2.id, 'XXX')

        address3 = PublicAddress('XXX:/local/pub')
        self.assertEqual(address3.hash, 'XXX')
        self.assertEqual(address3.path, '/local/pub')
        self.assertEqual(address3.relative.path, '/local/pub')

        address3 = PublicAddress('XXX:/local/pub/folder')
        relative = PublicAddress.MakeRelative(address3.hash, '/local/pub', '/local/pub/folder')
        self.assertEqual(relative.path, '/folder')

    def test_wise_ext_safe_rename(self):
        address = Address('222:/disk/folder/supername.ext').add_suffix(' (1)')
        self.assertEqual(address.id, '222:/disk/folder/supername (1).ext')

        address = Address('222:/disk/folder/supername').add_suffix(' (1)')
        self.assertEqual(address.id, '222:/disk/folder/supername (1)')

        address = Address('222:/disk/folder/supername.S0102.info.avi').add_suffix(' (1)')
        self.assertEqual(address.id, '222:/disk/folder/supername.S0102.info (1).avi')

    def test_locked_top_parent(self):
        address = Address('222:/disk/top/middle/bottom')
        top_locked = address.get_locked_top()
        self.assertEqual(top_locked.id, '222:/disk/top')

        address = Address('222:/disk/top')
        top_locked = address.get_locked_top()
        self.assertEqual(top_locked.id, '222:/disk/top')

    def test_trash_suffix_different_for_different_files(self):
        file1 = '/disk/file1.png'
        file2 = '/disk/other/file1.png'

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/other'})
        self.upload_file(self.uid, file1)
        self.upload_file(self.uid, file2)

        address1 = Address(self.uid + ':' + file1)
        address2 = Address(self.uid + ':' + file2)

        address1.add_trash_suffix()
        address2.add_trash_suffix()
        self.assertNotEqual(address1.id, address2.id)

    def test_get_path_without_area(self):
        address = Address('/disk/folder/stuff', uid='323')
        path_without_area = address.get_path_without_area()
        self.assertEqual(path_without_area, 'folder/stuff')
