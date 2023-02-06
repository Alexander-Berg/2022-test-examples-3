# -*- coding: utf-8 -*-
import urllib

from hamcrest import assert_that, is_, empty, has_length
from mock import mock
from nose_parameterized import parameterized
from requests import Response

from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
from test.parallelly.office_suit import OfficeTestCase
from test.fixtures.users import user_1, turkish_user

from mpfs.common.errors import AddressError, Forbidden
from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.core import factory
from mpfs.core.bus import Bus
from mpfs.core.filesystem.symlinks import Symlink
from mpfs.core.office.logic.base_editor import Editor
from mpfs.core.office.logic.microsoft import MicrosoftEditor
from mpfs.core.office.logic.only_office import OnlyOfficeEditor
from mpfs.core.office.util import SharingURLAddressHelper, build_office_online_url
from mpfs.core.user.anonymous import AnonymousUID
from mpfs.core.user.base import User
from mpfs.core.office.errors import (
    OfficeFileIsReadOnlyError,
    OfficeEditorNotSupportedForSharedEditError,
    OfficeEditorLockedForSharedEditError,
    OfficeRegionNotSupportedForSharedEditError)
from mpfs.core.office.static import OfficeAccessStateConst, OfficeServiceIDConst, OfficeClientIDConst
from mpfs.core.social.share.constants import SharedFolderRights


class SharingURLTestCase(OfficeTestCase):
    def test_sharing_url_fields_when_owner_has_no_editor(self):
        invited_uid = user_1.uid
        self.create_user(invited_uid)
        shared_dir = '/disk/shared-folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], invited_uid, rights=660)
        self.json_ok('share_activate_invite', {'uid': invited_uid, 'hash': invite_hash})
        file_path = '%s/file.docx' % shared_dir
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})

        # офисных полей для Совместного Редактирования не должно быть
        with mock.patch('mpfs.core.office.util.get_editor',
                        side_effect=lambda uid: None if uid == self.uid else OnlyOfficeEditor):
            res = self.json_ok('list', {
                'uid': invited_uid,
                'path': shared_dir,
                'meta': ''
            })

        file_item = None
        for item in res:
            if item['id'] == file_path:
                file_item = item
                break

        assert file_item is not None
        assert 'office_access_state' not in file_item['meta']

    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    def test_locked_with_mso_2nd_confirm(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.get_default_value()
        resource_id = info['meta']['resource_id']
        data = {'op_type': 'office',
                'office_online_editor_type': MicrosoftEditor.type_label}
        Bus().set_lock('%s:%s' % (self.uid, file_path), data=data)
        self.json_error('office_set_access_state', {'uid': self.uid,
                                                    'resource_id': resource_id,
                                                    'access_state': OfficeAccessStateConst.ALL,
                                                    'set_office_selection_strategy': 'force_oo'},
                        code=OfficeEditorLockedForSharedEditError.code)

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_set_public_office_fields(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        info = self.json_ok('set_public', {'uid': self.uid, 'path': file_path})

        assert 'office_access_state' in info
        assert 'office_online_sharing_url' in info

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_resource_id_file_id(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})['meta']['resource_id']
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        resp = self.json_ok('bulk_info_by_resource_ids',
                            {'uid': self.uid,
                             'meta': 'file_id,office_online_sharing_url,office_access_state,office_online_url'},
                            json=[resource_id])
        assert resp[0]['meta']['office_online_sharing_url']
        assert resp[0]['meta']['office_access_state']
        assert resp[0]['meta']['office_online_url']

    def test_office_set_access_state(self):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.get_default_value()

        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})

        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.ALL

    def test_office_access_state_on_set_private(self):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        self.json_ok('set_private', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})

        assert 'office_access_state' not in info['meta']

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})

        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.DISABLED

    def test_office_doc_short_id_inheritance(self):
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        expected_office_doc_short_id = self.json_ok('info',
                                                    {'uid': self.uid,
                                                     'path': file_path,
                                                     'meta': 'office_doc_short_id'})['meta']['office_doc_short_id']
        self.json_ok('set_private', {'uid': self.uid, 'path': file_path})
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': 'office_doc_short_id'})

        assert 'office_doc_short_id' in info['meta']
        assert info['meta']['office_doc_short_id'] == expected_office_doc_short_id

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_office_action_data_for_sharing_url(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        resp = self.json_ok('office_action_data', {
            'uid': self.uid,
            'action': 'edit',
            'service_file_id': office_doc_id,
            'service_id': OfficeServiceIDConst.SHARING_URL
        })
        assert resp['editor_config']

    @mock.patch('mpfs.core.office.logic.only_office_utils.OFFICE_SHARED_EDIT_MIGRATE_USERS_TO_ONLYOFFICE', True)
    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_office_action_data_for_sharing_url_for_non_ru_user(self):
        non_ru_uid = turkish_user.uid
        self.create_user(non_ru_uid)

        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        self.json_error('office_action_data', {'uid': non_ru_uid,
                                               'action': 'edit',
                                               'service_file_id': office_doc_id,
                                               'service_id': OfficeServiceIDConst.SHARING_URL},
                        code=OfficeRegionNotSupportedForSharedEditError.code)

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_office_action_data_for_anonymous(self):
        anonymous_yandexuid = '11223344556677'
        anonymous_uid = AnonymousUID.to_anonymous_uid(anonymous_yandexuid)
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        User(self.uid).set_only_office_enabled(True)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        orchestrator_resp = Response()
        orchestrator_resp._content = '{"container": "container_host:80"}'
        with enable_experiment_for_uid('only_office_orchestrator', self.uid), \
             mock.patch('mpfs.core.services.orchestrator_service.OrchestratorService.request',
                        return_value=orchestrator_resp):
                resp = self.json_ok('office_action_data', {
                'uid': anonymous_uid,
                'action': 'edit',
                'service_file_id': office_doc_id,
                'service_id': OfficeServiceIDConst.SHARING_URL
            })
        assert resp['editor_config']['editorConfig']['user']['id'] == anonymous_uid

    @parameterized.expand([
        ('sharing_url',
         '/disk/raccoon-story.docx', None, OfficeServiceIDConst.SHARING_URL),
        ('old_disk',
         '/disk/raccoon-story.docx', None, OfficeServiceIDConst.DISK),
        ('sharing_url_with_another_uid',
         '/disk/raccoon-story.docx', user_1.uid, OfficeServiceIDConst.SHARING_URL),
    ])
    def test_office_action_check_for_sharing_url(self, case_name, file_path, actor_uid, service_id):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        with mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True):
            info = self.json_ok('info', {'uid': self.uid,
                                         'path': file_path,
                                         'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        opts = {'uid': actor_uid or self.uid,
                'action': 'edit'}

        if service_id == OfficeServiceIDConst.SHARING_URL:
            opts['service_file_id'] = office_doc_id
            opts['service_id'] = OfficeServiceIDConst.SHARING_URL
        else:
            opts['service_file_id'] = file_path
            opts['service_id'] = OfficeServiceIDConst.DISK

        with mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True):
            resp = self.json_ok('office_action_check', opts)
        assert 'office_online_url' in resp
        # на старом месте всегда ссылки старого образца
        expected_uri = 'https://disk.yandex.ru/edit/disk/' + urllib.quote(file_path[1:], safe='')
        assert expected_uri == resp['office_online_url']
        # должны вернуть ссылку с новой адресацией
        assert 'office_online_sharing_url' in resp
        expected_uri = 'https://disk.yandex.ru/edit/d/' + office_doc_id
        assert expected_uri == resp['office_online_sharing_url']

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_office_action_check_for_sharing_url_with_another_uid(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        self.json_error('office_action_check', {'uid': self.OTHER_UID,
                                                'action': 'edit',
                                                'service_file_id': 'abcde' + office_doc_id,
                                                'service_id': OfficeServiceIDConst.SHARING_URL},
                        code=AddressError.code)

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_office_action_check_with_incorrect_sharing_url(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        self.json_error('office_action_check', {'uid': self.uid,
                                                'action': 'edit',
                                                'service_file_id': 'abcde' + office_doc_id,
                                                'service_id': OfficeServiceIDConst.SHARING_URL},
                        code=AddressError.code)

    def test_set_access_state_by_invited_user(self):
        other_uid = user_1.uid
        self.create_user(other_uid)
        shared_dir = '/disk/Shared'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], other_uid, rights=SharedFolderRights.READ_WRITE_INT)
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})

        shared_file = '/disk/Shared/test.docx'
        self.upload_file(self.uid, shared_file)
        self.json_ok('set_public', {'uid': self.uid, 'path': shared_file})
        info = self.json_ok('info', {'uid': other_uid,
                                     'path': shared_file,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_ok('office_set_access_state', {'uid': other_uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

    def test_set_access_state_by_invited_user_without_write_permissions(self):
        other_uid = user_1.uid
        self.create_user(other_uid)
        shared_dir = '/disk/Shared'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        invite_hash = self.share_invite(group['gid'], other_uid, rights=SharedFolderRights.READ_ONLY_INT)
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})

        shared_file = '/disk/Shared/test.docx'
        self.upload_file(self.uid, shared_file)
        self.json_ok('set_public', {'uid': self.uid, 'path': shared_file})
        info = self.json_ok('info', {'uid': other_uid,
                                     'path': shared_file,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_error('office_set_access_state', {'uid': other_uid,
                                                    'resource_id': resource_id,
                                                    'access_state': OfficeAccessStateConst.ALL},
                        code=OfficeFileIsReadOnlyError.code)

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_get_access_state(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].rsplit('/', 1)[-1]

        actual = self.json_ok('office_get_access_state', {'uid': self.uid,
                                                          'office_doc_id': office_doc_id})

        assert 'office_access_state' in actual
        assert actual['office_access_state'] == OfficeAccessStateConst.ALL

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_office_online_sharing_urls(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_online_sharing_url = info['meta']['office_online_sharing_url']
        body = [office_online_sharing_url]
        actual = self.json_ok('bulk_info_by_office_online_sharing_urls', {'uid': self.OTHER_UID, 'meta': ','}, json=body)

        assert actual[0]['meta']['office_online_sharing_url'] == office_online_sharing_url

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_office_online_sharing_urls_two_symlinks_with_different_short_doc_id(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        old_office_doc_short_id = info['meta']['office_doc_short_id']

        # break public status
        resource = factory.get_resource(self.uid, file_path)
        resource.meta['symlink'] = None
        resource.save()

        # make another active link_data-record
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        new_symlink = next(symlink
                           for symlink in Symlink.find_by_file_id(self.uid, resource.meta['file_id'])
                           if symlink.get_office_doc_short_id() != old_office_doc_short_id)
        new_symlink.update_office_fields({'office_access_state': OfficeAccessStateConst.ALL})
        sharing_url_addr = SharingURLAddressHelper.build_sharing_url_addr(
            resource.owner_uid,
            new_symlink.get_office_doc_short_id()
        )
        new_office_online_url = build_office_online_url(
            client_id=OfficeClientIDConst.SHARING_URL,
            document_id=sharing_url_addr.serialize(),
            tld='ru'
        )

        # trying to get info by new office_online_url
        body = [new_office_online_url]
        actual = self.json_ok('bulk_info_by_office_online_sharing_urls', {'uid': self.OTHER_UID, 'meta': ','}, json=body)

        assert_that(actual, is_(empty()))

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_office_online_sharing_urls_two_symlinks_with_different_access_state(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_online_sharing_url = info['meta']['office_online_sharing_url']

        # make another active link_data-record
        resource = factory.get_resource(self.uid, file_path)
        symlink = Symlink.find_by_file_id(self.uid, resource.meta['file_id'])[0]
        new_symlink = Symlink.Create(resource.storage_address, resource_id=resource.resource_id)
        new_symlink.update_office_fields({'office_access_state': OfficeAccessStateConst.DISABLED,
                                          'office_doc_short_id': symlink.get_office_doc_short_id()})

        # before test we should ensure
        # there are two active link_data-records for the same file_id
        assert_that(Symlink.find_by_file_id(self.uid, resource.meta['file_id']), has_length(2))

        body = [office_online_sharing_url]
        actual = self.json_ok('bulk_info_by_office_online_sharing_urls', {'uid': self.OTHER_UID, 'meta': ','}, json=body)

        assert_that(actual, is_(empty()))

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_office_online_sharing_urls_not_exists(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_online_sharing_url = info['meta']['office_online_sharing_url']
        body = [office_online_sharing_url]
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.DISABLED})
        actual = self.json_ok('bulk_info_by_office_online_sharing_urls', {'uid': self.OTHER_UID, 'meta': ','}, json=body)

        assert len(actual) == 0

    @mock.patch('mpfs.core.office.logic.only_office.OFFICE_ONLY_OFFICE_ENABLED', True)
    def test_bulk_info_by_office_online_sharing_urls_large_test(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                         'path': file_path,
                                         'meta': ','})
        resource_id = info['meta']['resource_id']

        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_online_sharing_url = info['meta']['office_online_sharing_url']
        body = [office_online_sharing_url] * 100
        actual = self.json_ok('bulk_info_by_office_online_sharing_urls', {'uid': self.OTHER_UID, 'meta': ','}, json=body)

        assert body == [res['meta']['office_online_sharing_url'] for res in actual]


class OnlyOfficeTestCase(OfficeTestCase):

    @mock.patch('mpfs.core.user.common.CommonUser.get_online_editor', return_value='only_office')
    def test_only_office_action_data(self, get_online_editor):
        """
        Проверяет что конфиг формируется  правильный
        """
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})

        from mpfs.core.office.logic import only_office
        only_office.OFFICE_ONLY_OFFICE_ENABLED = True
        only_office.OFFICE_ONLY_OFFICE_ENABLED_FOR_YANDEX_NETS = False

        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)
        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        resource_id = info['meta']['resource_id']
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        office_doc_id = info['meta']['office_online_sharing_url'].split('/')[-1]

        resp = self.json_ok('office_action_data', {
            'uid': self.uid,
            'action': 'edit',
            'service_file_id': office_doc_id,
            'service_id': OfficeServiceIDConst.SHARING_URL
        })


class SetSelectionStrategyTestCase(OfficeTestCase):
    def test_office_set_selection_strategy(self):
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': Editor.STRATEGY_FORCE_OO})
        actual_selection_strategy = self.json_ok('user_info', {'uid': self.uid})['office_selection_strategy']
        assert actual_selection_strategy == 'force_oo'

    def test_invalid_value(self):
        self.json_error('office_set_selection_strategy', {'uid': self.uid,
                                                          'selection_strategy': 'optional_oo'},
                        status=400)

    def test_locked_with_oo(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        data = {'op_type': 'office',
                'office_online_editor_type': OnlyOfficeEditor.type_label}
        Bus().set_lock('%s:%s' % (self.uid, file_path), data=data)
        self.json_error('office_set_selection_strategy', {'uid': self.uid,
                                                          'selection_strategy': 'default'},
                        code=OfficeEditorNotSupportedForSharedEditError.code)

    def test_non_ru(self):
        file_path = '/disk/old.docx'
        uid = turkish_user.uid
        self.create_user(uid)

        self.upload_file(uid, file_path)
        self.json_ok('office_set_selection_strategy', {'uid': uid,
                                                       'selection_strategy': 'force_oo'})

        actual_selection_strategy = self.json_ok('user_info', {'uid': uid})['office_selection_strategy']
        assert not actual_selection_strategy

    def test_no_error_if_locked_with_mso(self):
        file_path = '/disk/old.docx'
        self.upload_file(self.uid, file_path)
        data = {'op_type': 'office',
                'office_online_editor_type': MicrosoftEditor.type_label,
                'office_lock_id': '123-77-9'}
        Bus().set_lock('%s:%s' % (self.uid, file_path), data=data)
        self.json_ok('office_set_selection_strategy', {'uid': self.uid,
                                                       'selection_strategy': 'force_oo'})


class PublicSettingsTestCase(OfficeTestCase):
    def test_office_set_access_state_and_read_only(self):
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        file_path = '/disk/raccoon-story.docx'
        self.upload_file(self.uid, file_path)

        self.json_ok('set_public', {'uid': self.uid, 'path': file_path})
        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})
        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.get_default_value()

        # пока не установили доступ office_set_access_state, можно выставить
        self.json_ok(
            'set_public_settings',
            {'uid': self.uid, 'path': file_path},
            json={'read_only': True},
        )
        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': file_path})
        assert response['read_only']

        resource_id = info['meta']['resource_id']
        # устанавливаю редактирование
        self.json_ok('office_set_access_state', {'uid': self.uid,
                                                 'resource_id': resource_id,
                                                 'access_state': OfficeAccessStateConst.ALL})

        # read_only должен затереться
        response = self.json_ok('get_public_settings', {'uid': self.uid, 'path': file_path})
        assert not response['read_only']

        info = self.json_ok('info', {'uid': self.uid,
                                     'path': file_path,
                                     'meta': ','})

        assert 'office_access_state' in info['meta']
        assert info['meta']['office_access_state'] == OfficeAccessStateConst.ALL

        # нельзя ставить ro флаг, если включено совместное редактирование
        self.json_error(
            'set_public_settings',
            {'uid': self.uid, 'path': file_path},
            json={'read_only': True},
            code=Forbidden.code
        )
