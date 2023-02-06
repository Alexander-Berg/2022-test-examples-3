# -*- coding: utf-8 -*-
import pytest

from urlparse import urlparse

import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url
from mpfs.common.util import from_json
from mpfs.config import settings
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class MediaSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(MediaSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_invite_to_photostream(self):
        self.mail_ok('mksysdir', {'uid' : self.uid, 'type':'photostream'})
        result = self.mail_ok('share_create_group', {'uid': self.uid, 'path': u'/disk/Фотокамера/'})
        for each in result.getchildren():
            if each.tag == 'gid' and each.text and isinstance(each.text, str):
                gid = each.text
        self.assertTrue(gid)

        hsh = self.invite_user(path=u'/disk/Фотокамера/', uid=self.uid_3, email=self.email_3)
        args = {'hash': hsh, 'uid' : self.uid_3,}
        folder_info = self.mail_ok('share_activate_invite', args)
        name = folder_info.find('folder').find('name')
        self.assertEqual(name.text, u'Фотокамера (mpfs-test)')

    def test_resolve_photostream_conflicts(self):
        # Создаем обычную папку Фотокамера
        self.json_ok('mkdir', {'uid': self.uid, 'path': u'/disk/Фотокамера/'})

        # Создаем группу Фотокамера
        result = self.mail_ok('share_create_group', {'uid' : self.uid, 'path': u'/disk/Фотокамера/'})
        for each in result.getchildren():
            if each.tag == 'gid' and each.text and isinstance(each.text, str):
                gid = each.text
        self.assertTrue(gid)

        # Приглашаем, проверяем, что принятая папка называется Фотокамера
        hsh = self.invite_user(uid=self.uid_3, path=u'/disk/Фотокамера/', rights='640')
        args = {'hash': hsh, 'uid': self.uid_3}
        folder_info = self.mail_ok('share_activate_invite', args)
        name = folder_info.find('folder').find('name')
        self.assertEqual(name.text, u'Фотокамера')

        # Создаем особую папку Фотокамеры у приглашенного
        self.mail_ok('mksysdir', {'uid': self.uid_3, 'type': 'photostream'})

        # Проверяем
        result = self.mail_ok('info', {'uid': self.uid_3, 'path': u'/disk/Фотокамера/'})
        name = result.find('folder').find('name')
        self.assertEqual(name.text, u'Фотокамера')

        result = self.mail_ok('info', {'uid': self.uid_3, 'path': u'/disk/Фотокамера 1/'})
        name = result.find('folder').find('name')
        self.assertEqual(name.text, u'Фотокамера 1')

        owner = result.find('folder').find('meta').find('group').find('owner').find('login')
        self.assertEqual(owner.text, u'mpfs-test')
