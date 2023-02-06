# -*- coding: utf-8 -*-
from hamcrest import assert_that, has_entries, calling, raises
from mock import mock
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core import factory
from mpfs.core.office.errors import OfficeRegionNotSupportedForSharedEditError
from mpfs.core.office.logic.only_office_utils import check_country_for_only_office
from test.fixtures import users
from test.helpers.size_units import MB
from test.helpers.utils import ignored
from test.parallelly.office_suit import OfficeTestCase
from mpfs.core.office.static import OfficeSourceConst, SettingsVerstkaEditorConst, EditorConst
from mpfs.core.user.base import User
from mpfs.core.bus import Bus
from mpfs.core.office.logic.microsoft import MicrosoftEditor
from mpfs.core.office.logic.only_office import OnlyOfficeEditor

from mpfs.common.errors import ResourceLocked, NoShardForUidRoutingError


class BaseTestCase(OfficeTestCase):
    def test_check_country(self):
        non_ru_uid = users.turkish_user.uid
        self.create_user(non_ru_uid)
        path = '/disk/meow.docx'
        self.upload_file(non_ru_uid, path)
        resource = factory.get_resource(non_ru_uid, path)

        with mock.patch('mpfs.core.office.util.get_editor',
                        return_value=None):
            assert_that(calling(check_country_for_only_office).with_args(non_ru_uid, OnlyOfficeEditor, resource),
                        raises(OfficeRegionNotSupportedForSharedEditError))

    def test_non_ru_region_for_oo(self):
        path = '/disk/meow.docx'
        self.upload_file(self.uid, path)
        resource = factory.get_resource(self.uid, path)

        with mock.patch('mpfs.core.office.util.get_editor',
                        return_value=MicrosoftEditor):
            assert_that(calling(check_country_for_only_office).with_args(self.uid, OnlyOfficeEditor, resource, region='us'),
                        raises(OfficeRegionNotSupportedForSharedEditError))

    @parameterized.expand([
        'ru',
        '',
    ])
    def test_ok_region_for_oo(self, region):
        path = '/disk/meow.docx'
        self.upload_file(self.uid, path)
        resource = factory.get_resource(self.uid, path)

        with mock.patch('mpfs.core.office.util.get_editor',
                        return_value=MicrosoftEditor):
            check_country_for_only_office(self.uid, OnlyOfficeEditor, resource, region=region)

    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    def test_try_to_migrate_to_oo_only_once_when_office_action_check_from_docs(self):
        User(self.uid).set_setting(SettingsVerstkaEditorConst.TRIED_TO_MIGRATE_TO_ONLY_OFFICE_EDITOR_ON_OPEN_DOCS, '1',
                         namespace='verstka')
        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'source': OfficeSourceConst.DOCS})

        assert_that(resp, has_entries({'office_online_editor_type': MicrosoftEditor.type_label}),
                    u"Если флажок УЖЕ_МИГИРОВАЛИ_ИЗ_ДОКСОВ выставлен, то не должно происходить миграции на OO")

    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    def test_migrate_to_oo_when_office_action_check_from_docs(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid})

        assert_that(resp, has_entries({'office_online_editor_type': MicrosoftEditor.type_label}),
                    u"Не из DOCSов не должно происходить миграции на OO")

        resp = self.json_ok('office_action_check', {'uid': self.uid,
                                                    'source': OfficeSourceConst.DOCS})

        assert_that(resp, has_entries({'office_online_editor_type': OnlyOfficeEditor.type_label}),
                    u"Из DOCSов должна происходить миграция на ОО")

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_set_public_while_editing(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)

        data = {'op_type': 'office',
                'office_online_editor_type': MicrosoftEditor.type_label,
                'office_lock_id': '123-77-9'}
        Bus().set_lock('%s:%s' % (self.uid, file_path), data=data)

        info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})

        assert 'office_access_state' in info
        assert 'office_online_sharing_url' in info

        info = self.json_ok('set_private', {'uid': self.uid, 'path': file_path, 'return_info': '1'})
        assert 'path' in info
        assert info['path'] == file_path

    def test_set_public_while_non_edit_lock(self):
        file_path = '/disk/old.docx'
        public_file_path = '/disk/pold.docx'
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid, public_file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': public_file_path})

        data = {'op_type': 'move'}
        Bus().set_lock('%s:%s' % (self.uid, file_path), data=data)
        data = {'op_type': 'move'}
        Bus().set_lock('%s:%s' % (self.uid, public_file_path), data=data)

        self.json_error('set_public', {'uid': self.uid, 'path': file_path}, code=ResourceLocked.code)
        self.json_error('set_private', {'uid': self.uid, 'path': public_file_path}, code=ResourceLocked.code)


@mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
class DisableDocumentPermissionsTestCase(OfficeTestCase):
    @parameterized.expand([
        ('single',
         'print',
         {"comment": True, "download": True, "edit": True, "fillForms": True, "print": False, "review": True}),
        ('empty',
         '',
         {"comment": True, "download": True, "edit": True, "fillForms": True, "print": True, "review": True}),
        ('double',
         'print,download',
         {"comment": True, "download": False, "edit": True, "fillForms": True, "print": False, "review": True}),
        ('bad_and_good',
         'meow,print',
         {"comment": True, "download": True, "edit": True, "fillForms": True, "print": False, "review": True}),
        ('all',
         'comment,download,fillForms,print,review',
         {"comment": False, "download": False, "edit": True, "fillForms": False, "print": False, "review": False}),
    ])
    def test_default(self, case_name, disable_doc_permissions, expected_permissions):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/EHOT.docx'
        self.upload_file(self.uid, file_path)

        resp = self.json_ok('office_action_data', {'uid': self.uid,
                                                   'action': 'edit',
                                                   'service_id': 'disk',
                                                   'service_file_id': file_path,
                                                   'disable_document_permissions': disable_doc_permissions})

        assert_that(resp['editor_config']['document']['permissions'],
                    has_entries(expected_permissions))


@mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
class MigrateToOnlyOfficeOnOpenFrontClientTestCase(OfficeTestCase):
    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    def test_default(self):
        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert_that(resp, has_entries({'office_online_editor_type': MicrosoftEditor.type_label}),
                    u"Перед тестом должен быть MSO")

        resp = self.json_ok('office_switch_to_onlyoffice', {'uid': self.uid})
        assert_that(resp, has_entries({'switched': True,
                                       'should_show_onboarding': True}))

        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert_that(resp, has_entries({'office_online_editor_type': OnlyOfficeEditor.type_label}))

    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    def test_non_ru(self):
        uid = users.turkish_user.uid
        self.create_user(uid)

        resp = self.json_ok('office_action_check', {'uid': uid})
        assert_that(resp, has_entries({'office_online_editor_type': MicrosoftEditor.type_label}),
                    u"Перед тестом должен быть MSO")

        resp = self.json_ok('office_switch_to_onlyoffice', {'uid': uid})
        assert_that(resp, has_entries({'switched': False,
                                       'should_show_onboarding': False,
                                       'office_online_editor_type': MicrosoftEditor.type_label}))


@mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
class SizeLimitsTestCase(OfficeTestCase):
    def test_default(self):
        file_path = u'/disk/test.xlsx'
        self.upload_file(self.uid, file_path, file_data={'size': 29*MB})

        # OO limit: 30MB
        self.json_ok('office_action_data', {'uid': self.uid,
                                            'action': 'edit',
                                            'service_id': 'disk',
                                            'service_file_id': file_path})

        too_big_file_path = u'/disk/test.xlsx'
        self.upload_file(self.uid, too_big_file_path, file_data={'size': 31*MB})

        self.json_error('office_action_data', {'uid': self.uid,
                                            'action': 'edit',
                                            'service_id': 'disk',
                                            'service_file_id': too_big_file_path},
                        code=codes.OFFICE_FILE_TOO_LARGE)


@mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
class UserInitTestCase(OfficeTestCase):
    @parameterized.expand([
        ('enabled',
         True,
         EditorConst.ONLY_OFFICE),
        ('disabled',
         False,
         EditorConst.MICROSOFT_ONLINE),
    ])
    def test_default(self, case_name, exp_enabled, expected_editor_after_init):
        uid = users.user_7.uid
        # удаляем юзера, если он имеет Диск
        with ignored(NoShardForUidRoutingError):
            self.remove_user(uid)

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        return_value=exp_enabled):
            self.json_ok('user_init', {'uid': uid})

        resp = self.json_ok('office_action_check', {'uid': uid})
        assert_that(resp, has_entries({'office_online_editor_type': expected_editor_after_init}))

    @parameterized.expand([
        ('enabled',
         True),
        ('disabled',
         False),
    ])
    def test_already_inited(self, case_name, exp_enabled):
        u"""Не меняем редактор, если юзер уже инициализирован.

        Если юзер инициализирован, то изменять стратегию выбора редактора должен только один из продуктовых сценариев.
        """
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        return_value=exp_enabled):
            self.json_ok('user_init', {'uid': self.uid})

        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert_that(resp, has_entries({'office_online_editor_type': EditorConst.MICROSOFT_ONLINE}))

    def test_non_ru(self):
        u"""Для не ru-юзеров не меняем редактор"""
        uid = users.turkish_user.uid
        # удаляем юзера, если он имеет Диск
        with ignored(NoShardForUidRoutingError):
            self.remove_user(uid)
        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        return_value=True):
            self.json_ok('user_init', {'uid': self.uid})

        resp = self.json_ok('office_action_check', {'uid': self.uid})
        assert_that(resp, has_entries({'office_online_editor_type': EditorConst.MICROSOFT_ONLINE}))
