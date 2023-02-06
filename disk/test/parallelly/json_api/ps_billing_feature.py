# coding: utf-8
import mock

from mpfs.core.user.base import User
from mpfs.metastorage.mongo.collections.base import UserIndexCollection
from test.helpers.stubs.services import DirectoryServiceStub
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin


class UnlimitTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):

    def test_set_ps_billing_feature(self):
        from mpfs.core.office.util import ONLY_OFFICE_FEATURE, ONLINE_EDITOR_FEATURE
        from mpfs.engine.process import reset_cached
        self.create_user(self.uid)

        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLY_OFFICE_FEATURE, 'value': 1})
        reset_cached()
        assert User(self.uid).get_only_office_enabled()
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLY_OFFICE_FEATURE, 'value': 0})
        reset_cached()
        assert not User(self.uid).get_only_office_enabled()

        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLINE_EDITOR_FEATURE, 'value': 1})
        reset_cached()
        assert User(self.uid).get_online_editor_enabled()
        assert not User(self.uid).get_only_office_enabled()
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLINE_EDITOR_FEATURE, 'value': 0})
        reset_cached()
        assert not User(self.uid).get_online_editor_enabled()

    def test_set_ps_billing_advertising_feature(self):
        from mpfs.core.base import ADVERTISING_FEATURE
        from mpfs.engine.process import reset_cached
        self.create_user(self.uid)

        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ADVERTISING_FEATURE, 'value': 1})
        reset_cached()
        assert User(self.uid).get_advertising_enabled()

        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ADVERTISING_FEATURE, 'value': 0})
        reset_cached()
        assert not User(self.uid).get_advertising_enabled()

    def test_set_ps_billing_public_settings_feature(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        from mpfs.engine.process import reset_cached
        self.create_user(self.uid)
        # check enable
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        reset_cached()
        assert User(self.uid).get_public_settings_enabled()
        # check disable
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 0})
        reset_cached()
        assert not User(self.uid).get_public_settings_enabled()

    def test_set_ps_billing_unkonnw_feature(self):
        self.create_user(self.uid)
        self.json_error('set_ps_billing_feature', {'uid': self.uid, 'feature_name': 'UNKNOWN', 'value': 1}, status=400)

    def test_set_ps_billing_update_b2b(self):
        from mpfs.core.office.util import ONLY_OFFICE_FEATURE

        self.create_user(self.uid)
        with DirectoryServiceStub() as stub:
            self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLY_OFFICE_FEATURE, 'value': 1})
            assert stub.get_organizations_by_uids.called
            assert stub.get_organizations_by_uids.call_args[0][0] == [self.uid]
            user = User(self.uid)
            assert user.b2b_key

    def test_set_ps_billing_dont_update_b2b(self):
        from mpfs.core.office.util import ONLY_OFFICE_FEATURE

        self.create_user(self.uid)
        user = User(self.uid)
        user.make_b2b('12345678')
        with DirectoryServiceStub() as stub:
            self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ONLY_OFFICE_FEATURE, 'value': 1})
            assert not stub.get_organizations_by_uids.called

    def test_set_ps_billing_set_unlimited_photo_autouploading_allowed(self):
        from mpfs.core.base import UNLIMITED_PHOTO_AUTOUPLOADING_FEATURE

        self.create_user(self.uid)
        assert not User(self.uid).get_unlimited_photo_autouploading_allowed()

        self.json_ok('set_ps_billing_feature',
                     {'uid': self.uid, 'feature_name': UNLIMITED_PHOTO_AUTOUPLOADING_FEATURE, 'value': 1})
        UserIndexCollection.reset()
        assert User(self.uid).get_unlimited_photo_autouploading_allowed()

        self.json_ok('set_ps_billing_feature',
                     {'uid': self.uid, 'feature_name': UNLIMITED_PHOTO_AUTOUPLOADING_FEATURE, 'value': 0})
        UserIndexCollection.reset()
        assert not User(self.uid).get_unlimited_photo_autouploading_allowed()

    def test_set_ps_billing_set_unlimited_video_autouploading_allowed(self):
        from mpfs.core.base import UNLIMITED_VIDEO_AUTOUPLOADING_FEATURE

        self.create_user(self.uid)
        assert not User(self.uid).get_unlimited_video_autouploading_allowed()

        self.json_ok('set_ps_billing_feature',
                     {'uid': self.uid, 'feature_name': UNLIMITED_VIDEO_AUTOUPLOADING_FEATURE, 'value': 1})
        UserIndexCollection.reset()
        assert User(self.uid).get_unlimited_video_autouploading_allowed()

        self.json_ok('set_ps_billing_feature',
                     {'uid': self.uid, 'feature_name': UNLIMITED_VIDEO_AUTOUPLOADING_FEATURE, 'value': 0})
        UserIndexCollection.reset()
        assert not User(self.uid).get_unlimited_video_autouploading_allowed()
