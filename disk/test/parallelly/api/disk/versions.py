# -*- coding: utf-8 -*-
import datetime

from httplib import CREATED, OK, NOT_FOUND, FORBIDDEN

import mock

from mpfs.common.util import from_json
from mpfs.core.social.share.constants import SharedFolderRights
from test.common.sharing import CommonSharingMethods
from test.fixtures.users import user_6
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin

from mpfs.core.factory import get_resource
from mpfs.core.versioning.logic.version import Version
from mpfs.core.versioning.logic.version_chain import VersionChain
from mpfs.core.versioning.dao.version_data import VersionType
from test.parallelly.versioning_suit import RelativeTimeDelta


class VersionsTestCase(CommonSharingMethods, DiskApiTestCase):
    def setup_method(self, method):
        super(VersionsTestCase, self).setup_method(method)
        self.create_user(uid=self.uid, locale='ru')
        self.file_path = '/disk/1.txt'
        self.size_v1 = 123
        self.upload_file(self.uid, self.file_path, file_data={'size': self.size_v1})
        self.size_v2 = 765
        self.upload_file(self.uid, self.file_path, file_data={'size': self.size_v2})
        self.resource = get_resource(self.uid, self.file_path)

    def test_get_versions(self):
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        assert resp.status_code == OK
        assert len(from_json(resp.content)) == 2

    def test_get_version(self):
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        version_id = from_json(resp.content)['items'][-1]['id']
        resp = self.client.get('disk/resources/%s/versions/%s' % (self.resource.resource_id.serialize(), version_id),
                               uid=self.uid)

        assert resp.status_code == OK

    def test_restore_version(self):
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        # Берем самую первую версию
        version_id = from_json(resp.content)['items'][-1]['id']

        resp = self.client.put('disk/resources/%s/versions/%s/restore' % (self.resource.resource_id.serialize(),
                                                                          version_id),
                               uid=self.uid)

        assert resp.status_code == CREATED
        assert from_json(resp.content)['size'] == self.size_v1

    def test_restore_version_in_read_only_folder(self):
        invited_uid = user_6.uid
        self.create_user(invited_uid)

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared_dir'})
        hsh = self.invite_user(uid=invited_uid, owner=self.uid,
                               email=user_6.email, rights=SharedFolderRights.READ_ONLY_INT, path='/disk/shared_dir')
        self.activate_invite(uid=invited_uid, hash=hsh)
        self.upload_file(self.uid, '/disk/shared_dir/file.ext', file_data={'size': 100})
        self.upload_file(self.uid, '/disk/shared_dir/file.ext', file_data={'size': 101})
        shared_resource = get_resource(invited_uid, '/disk/shared_dir/file.ext')

        resp = self.client.get('disk/resources/%s/versions/checkpoints' % shared_resource.resource_id.serialize(),
                               uid=invited_uid)
        # Берем самую первую версию
        version_id = from_json(resp.content)['items'][-1]['id']

        resp = self.client.put('disk/resources/%s/versions/%s/restore' % (shared_resource.resource_id.serialize(),
                                                                          version_id),
                               uid=invited_uid)

        assert resp.status_code == FORBIDDEN

    def test_restore_non_restorable_version(self):
        """Проверяем ответ при восстановление версии, которая не может быть восстановлена."""
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        # Берем самую последнюю версию
        version_id = from_json(resp.content)['items'][0]['id']

        resp = self.client.put('disk/resources/%s/versions/%s/restore' % (self.resource.resource_id.serialize(),
                                                                          version_id),
                               uid=self.uid)

        assert resp.status_code == NOT_FOUND

    def test_copy_version(self):
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        # Берем самую первую версию
        version_id = from_json(resp.content)['items'][-1]['id']

        resp = self.client.post('disk/resources/%s/versions/%s/copy' % (self.resource.resource_id.serialize(),
                                                                        version_id),
                                uid=self.uid)

        assert resp.status_code == CREATED

        items = self.json_ok('list', opts={'uid': self.uid, 'path': '/disk/'})
        # 3: папка, оригинальный файл, новая копия
        assert len(items) == 3

    def test_list_versions(self):
        # Заводим много версий, чтобы не вмещались на одну страницу
        VERSIONS_AMOUNT = 50
        versions = []
        day_delta = datetime.timedelta(days=1)
        rd = RelativeTimeDelta()
        for _ in range(VERSIONS_AMOUNT):
            versions.append(
                Version.create_fake(self.uid, VersionType.trashed, rd.step(day_delta), rd.cur_dt)
            )
        vl = VersionChain.ensure(self.resource.resource_id)
        vl.append_versions(versions)

        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)

        result = from_json(resp.content)
        assert len(result['iteration_key'])

        listed_versions = {version['id'] for version in result['items']}
        while result['iteration_key']:
            resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                                   uid=self.uid, query={'iteration_key': result['iteration_key']})
            result = from_json(resp.content)
            listed_versions |= {version['id'] for version in result['items']}

        # 2 версии создаем в setup'е и 50 прямо в тесте
        assert len(listed_versions) == VERSIONS_AMOUNT + 2

    def test_list_folded_versions(self):
        for _ in range(5):
            self.upload_file(self.uid, self.file_path, force=1)
        resource = get_resource(self.uid, self.file_path)

        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)

        result = from_json(resp.content)
        assert result['items'][-1]['folded_items_iteration_key']

        resp = self.client.get('disk/resources/%s/versions/folded' % self.resource.resource_id.serialize(),
                               uid=self.uid,
                               query={'iteration_key': result['items'][-1]['folded_items_iteration_key']})
        result = from_json(resp.content)
        assert len(result['items'])

    def test_version_format(self):
        for _ in range(2):
            self.upload_file(self.uid, self.file_path, force=1)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})
        self.json_ok('trash_restore', {'uid': self.uid, 'path': self.file_path.replace('/disk/', '/trash/')})
        # get checkpoint items
        resp = self.client.get('disk/resources/%s/versions/checkpoints' % self.resource.resource_id.serialize(),
                               uid=self.uid)
        items = from_json(resp.content)['items']
        # get folded items
        resp = self.client.get('disk/resources/%s/versions/folded' % self.resource.resource_id.serialize(),
                               uid=self.uid,
                               query={'iteration_key': items[1]['folded_items_iteration_key']})
        items += from_json(resp.content)['items']

        common_fields = {
            "uid_created", "folded_items_iteration_key",
            "created", "can_be_restored",
            "type", "platform_created", "id"
        }
        binary_fields = common_fields | {"file", "sha256", "md5", "size"}

        for version_obj in items:
            assert version_obj['type'] in ('current', 'restored', 'trashed', 'binary')
            if version_obj['type'] in ('restored', 'trashed'):
                assert not version_obj.viewkeys() ^ common_fields
            else:
                assert not version_obj.viewkeys() ^ binary_fields

    def test_params_for_folded_versions(self):
        resp = self.client.get('disk/resources/%s/versions/folded' % self.resource.resource_id.serialize(),
                               uid=self.uid,
                               query={'iteration_key': ''})
        assert resp.status_code == 400
