# -*- coding: utf-8 -*-
import re

from base import CommonFilesystemTestCase

import mpfs.engine.process
from mpfs.core.bus import Bus
from mpfs.common import errors
from mpfs.core.address import Address
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class CopyMoveRemoveFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        """
        Дополнительно создаем пачку файлов
        """
        super(CopyMoveRemoveFilesystemTestCase, self).setup_method(method)
        self._mkdirs()
        self._mkfiles()

    def test_copy(self):
        src_address = Address.Make(self.uid, '/disk/filesystem test folder')
        dst_address = Address.Make(self.uid, '/disk/filesystem test folder copied')
        Bus().copy_resource(self.uid, src_address.id, dst_address.id, force=False)
        result = Bus().content(
            self.uid,
            dst_address.id,
            self.list_params
        )

        self.assertEqual(
            '/disk/filesystem test folder copied/inner folder',
            result['list'][0]['id']
        )
        self.assertEqual(
            '/disk/filesystem test folder copied/inner file',
            result['list'][1]['id']
        )
        inner_result = Bus().content(
            self.uid,
            dst_address.get_child_folder('inner folder').id,
            self.list_params
        )
        self.assertEqual(
            '/disk/filesystem test folder copied' + '/inner folder/subinner folder',
            inner_result['list'][0]['id']
        )
        Bus().rm(self.uid, dst_address.id)

    def test_move_simple(self):
        def _assert_test_move(src, dst):
            Bus().move_resource(
                self.uid,
                Address.Make(self.uid, src).id,
                Address.Make(self.uid, dst).id,
                force=False
            )
            result = Bus().content(
                self.uid,
                Address.Make(self.uid, dst).id,
                self.list_params
            )
            self.assertEqual(dst + '/inner folder', result['list'][0]['id'])
            self.assertEqual(dst + '/inner file', result['list'][1]['id'])

        _assert_test_move('/disk/filesystem test folder', '/disk/filesystem test folder moved')
        _assert_test_move('/disk/filesystem test folder moved', '/disk/filesystem test folder')

    def test_move_complicated(self):
        def _assert_compl_test_move(src, dst):
            Bus().move_resource(
                self.uid,
                Address.Make(self.uid, src).id,
                Address.Make(self.uid, dst).id,
                force=False
            )
            result = Bus().content(
                self.uid,
                Address.Make(self.uid, dst).get_parent().id,
                self.list_params
            )
            ids = map(lambda x: x['id'], result['list'])
            self.assertTrue(dst in ids)

        Bus().mkdir(
            self.uid,
            Address.Make(self.uid, '/disk/complicated move').id,
        )

        _assert_compl_test_move('/disk/filesystem test folder', '/disk/complicated move/filesystem test folder moved')
        _assert_compl_test_move('/disk/complicated move/filesystem test folder moved', '/disk/filesystem test folder')

    def test_hardlink_copy(self):
        result = Bus().hardlink_copy(
            self.uid,
            Address.Make(self.uid, '/disk/filesystem test folder/hardlinked file').id,
            self.file_data['meta']['md5'],
            self.file_data['size'],
            self.file_data['meta']['sha256']
        ).dict()

        self.assertEqual('/disk/filesystem test folder/hardlinked file', result['id'])
        for field in self.file_fields:
            self.assertTrue(field in result)
            if field in self.nonempty_fields:
                self.assertNotEqual(result[field], None, 'Key: %s dict: %s' % (field, result))

        for k, v in self.file_data.iteritems():
            if k == 'meta':
                for _k, _v in v.iteritems():
                    self.assertEqual(_v, result[k][_k])
            else:
                if isinstance(v, int):
                    self.assertTrue(v - result[k] <= 0)
                else:
                    self.assertEqual(v, result[k])

        self.assertTrue('file_id' in result['meta'])

    def test_rm_simple(self):
        Bus().rm(self.uid, Address.Make(self.uid, '/disk/filesystem test file').id)
        try:
            Bus().info(self.uid, Address.Make(self.uid, '/disk/filesystem test file').id)
            self.fail(Address.Make(self.uid, '/disk/filesystem test file').id)
        except Exception:
            pass

        path_hidden_file1 = Address.Make(self.uid, '/disk/filesystem test file')
        path_hidden_file1.change_storage('hidden')
        result = Bus().content(
            self.uid,
            path_hidden_file1.get_parent().id,
            self.list_params
        )
        self.assertTrue(result['list'], result)
        chkd = False
        for elem in result['list']:
            if re.match('%s:\d+.\.\d+$' % path_hidden_file1.path, elem['id']):
                chkd = True
        self.assertTrue(chkd, result)

        f_content = Bus().content(self.uid, Address.Make(self.uid, '/disk/filesystem test folder').id, self.list_params)
        Bus().rm(self.uid, Address.Make(self.uid, '/disk/filesystem test folder').id)

        try:
            Bus().info(self.uid, Address.Make(self.uid, '/disk/filesystem test folder').id)
        except errors.NotFound:
            pass
        else:
            self.fail()

        from mpfs.core.services import disk_service

        for k in map(lambda x: x['id'], f_content['list']):
            try:
                k_data = disk_service.Disk().show_single(Address.Make(self.uid, k))
            except Exception:
                pass
            else:
                self.fail(k_data)

        path_hidden_file2 = Address.Make(self.uid, '/disk/filesystem test folder' + '/inner file')
        path_hidden_file2.change_storage('hidden')
        path_parent = path_hidden_file2.get_parent()

        hidden_folder_content = []
        for each in db.hidden_data.find({'uid': self.uid}):
            if each['key'].startswith(path_parent.path + '/'):
                hidden_folder_content.append(each['key'])

        self.assertTrue(len(hidden_folder_content) > 0)
        result = Bus().content(
            self.uid,
            path_parent.id,
            self.list_params
        )
        self.assertTrue(len(result['list']) > 0)

        chkd = False
        for elem in result['list']:
            if re.match('%s:\d+.\.\d+$' % path_hidden_file2.path, elem['id']):
                chkd = True
        self.assertTrue(chkd)

    def test_rm_while_overwrite(self):
        address = Address.Make(self.uid, '/disk/overwritten file')
        Bus().mkfile(self.uid, address.id, data=self.file_data)
        Bus().mkfile(self.uid, address.id, data=self.file_data)

        address.change_storage('hidden')
        result = Bus().content(
            self.uid,
            address.get_parent().id,
            self.list_params
        )
        chkd = False

        self.assertTrue(result['list'])
        for elem in result['list']:
            if re.match('%s:\d+.\.\d+$' % address.path, elem['id']):
                chkd = True
        self.assertTrue(chkd, '%s not in %s' % (address.path, map(lambda x: x['id'], result['list'])))

    def test_rm_while_copying(self):
        orig_address = Address.Make(self.uid, '/disk/filesystem test folder/overwritten copied file')
        Bus().mkfile(self.uid, orig_address.id, data=self.file_data)

        src_address = Address.Make(self.uid, '/disk/filesystem test folder')
        dst_address = Address.Make(self.uid, '/disk/filesystem test folder copied')

        Bus().copy_resource(self.uid, src_address.id, dst_address.id, force=False)
        Bus().copy_resource(self.uid, src_address.id, dst_address.id, force=True)

        dst_address.change_storage('hidden')
        copied_address = dst_address.get_child_file(orig_address.name)
        result = Bus().content(
            self.uid,
            copied_address.get_parent().id,
            self.list_params
        )
        self.assertTrue(result['list'])
        chkd = False
        for elem in result['list']:
            if re.match('%s:\d+.\.\d+$' % copied_address.path, elem['id']):
                chkd = True
        self.assertTrue(chkd)

    def test_rm_while_moving(self):
        faddr = Address.Make(self.uid, '/disk/filesystem test folder copied').id
        result = Bus().mkdir(self.uid, faddr)

        def check_item_in_list(address):
            chckd = False
            for elem in result['list']:
                if re.search('%s:\d+.\.\d+$' % address.path, elem['id']):
                    chckd = True
            return chckd

        address_a = Address.Make(self.uid, '/disk/filesystem test folder/simply moved file')
        Bus().mkfile(self.uid, address_a.id, data=self.file_data)

        address_b = Address.Make(self.uid, '/disk/filesystem test folder copied/complicated moved file')
        Bus().mkfile(self.uid, address_b.id, data=self.file_data)

        address_c = Address.Make(self.uid, '/disk/filesystem test folder/complicated overwrited file')
        Bus().mkfile(self.uid, address_c.id, data=self.file_data)

        address_d = Address.Make(self.uid, '/disk/filesystem test folder copied/complicated overwrited file')
        Bus().mkfile(self.uid, address_d.id, data=self.file_data)

        src = Address.Make(self.uid, '/disk/filesystem test folder')
        dst = Address.Make(self.uid, '/disk/filesystem test folder copied')
        Bus().move_resource(self.uid, src.id, dst.id, force=True)

        address_b.change_storage('hidden')
        address_d.change_storage('hidden')
        result = Bus().content(
            self.uid,
            address_b.get_parent().id,
            self.list_params
        )

        res = check_item_in_list(address_b)
        self.assertFalse(res, res)
        res = check_item_in_list(address_d)
        self.assertTrue(res, res)

    def test_overwrite_different_resources_with_force(self):
        def _make_folder(fid):
            faddr = Address.Make(self.uid, fid).id
            Bus().mkdir(self.uid, faddr)
            return faddr

        def _make_file(fid):
            faddr = Address.Make(self.uid, fid).id
            Bus().mkfile(self.uid, faddr, data=self.file_data)
            return faddr

        dir_path = _make_folder('/disk/overwrite folder 1')
        dir_path2 = _make_folder('/disk/overwrite folder 2')
        dir_path3 = _make_folder('/disk/overwrite folder 3')

        file_path = _make_file('/disk/overwrite file 1')
        file_path2 = _make_file('/disk/overwrite file 2')
        file_path3 = _make_file('/disk/overwrite file 3')

        # No force
        self.assertRaises(errors.CopyTargetExists, Bus().copy_resource, self.uid, dir_path, file_path, False)
        self.assertRaises(errors.CopyTargetExists, Bus().copy_resource, self.uid, file_path, dir_path, False)
        self.assertRaises(errors.CopyTargetExists, Bus().move_resource, self.uid, dir_path, file_path, False)
        self.assertRaises(errors.CopyTargetExists, Bus().move_resource, self.uid, file_path, dir_path, False)

        # Force
        Bus().copy_resource(self.uid, dir_path, file_path2, True)
        Bus().copy_resource(self.uid, file_path, dir_path2, True)
        Bus().move_resource(self.uid, dir_path, file_path3, True)
        Bus().move_resource(self.uid, file_path, dir_path3, True)

        # Check
        for fpath in file_path2, file_path3:
            self.assertEqual(Bus().info(self.uid, fpath).get('this', None).get('type', None), 'dir')

        for fpath in dir_path2, dir_path3:
            self.assertEqual(Bus().info(self.uid, fpath).get('this', None).get('type', None), 'file')

        # Remove
        for path in dir_path2, dir_path3, file_path2, file_path3:
            Bus().rm(self.uid, path)

        for path in dir_path, file_path:
            self.assertRaises(errors.RmNotFound, Bus().rm, self.uid, path)
