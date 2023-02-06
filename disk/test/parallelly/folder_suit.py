# -*- coding: utf-8 -*-
import os
import pytest

from test.base import DiskTestCase

from mpfs.core.address import Address
from mpfs.core.filesystem.resources.root import RootFolder
from mpfs.core.filesystem.resources.disk import DiskFolder
from mpfs.core.filesystem.resources.fotki import FotkiFolder
from mpfs.core.filesystem.resources.attach import AttachFolder


class FoldersTestCase(DiskTestCase):
    def test_root(self):
        from mpfs.core.services.stock_service import _setup
        _setup()
        fld = RootFolder(self.uid, Address('%s:/' % self.uid))
        self.assertNotEqual(fld, None)
        ls = fld.list()
        self.assertEqual(len(ls['list']), len(RootFolder.ROOT_CHILD_FOLDERS))
        for service_name in ('/narod', '/disk', '/mail', '/fotki', '/trash', '/attach', '/photounlim'):
            self.assertTrue(service_name in fld.child_folders)
            self.assertEqual(fld.child_folders[service_name]['type'], 'dir')

    def test_disk(self):
        fld = DiskFolder(self.uid, Address('%s:/disk/' % self.uid))
        self.assertTrue(fld)
        ls = fld.list()
        self.assertEqual(len(ls['list']), 0)
        self.assertTrue(os.path.sep not in fld.child_folders)

    @pytest.mark.skipif(True, reason='Fotki has been closed: https://st.yandex-team.ru/IAAS-7318#1531222926000')
    def test_fotki(self):
        fld = FotkiFolder(self.uid, Address('%s:/fotki/' % self.uid))
        self.assertTrue(fld)
        ls = fld.list()
        self.assertEqual(len(ls['list']), 1)
        self.assertTrue(os.path.sep not in fld.child_folders)
        
    @pytest.mark.skipif(True, reason='Fotki has been closed: https://st.yandex-team.ru/IAAS-7318#1531222926000')
    def test_mail(self):
        fld = FotkiFolder(self.uid, Address('%s:/mail/' % self.uid))
        self.assertTrue(fld)
        ls = fld.list()
        self.assertEqual(len(ls['list']), 1)
        self.assertTrue(os.path.sep not in fld.child_folders)
        
    def test_attach(self):
        fld = AttachFolder(self.uid, Address('%s:/attach' % self.uid))
        self.assertTrue(fld)
        ls = fld.list()
        self.assertEqual(len(ls['list']), 0)
        self.assertTrue(os.path.sep not in fld.child_folders)

