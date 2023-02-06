# -*- coding: utf-8 -*-

import time
import pytest

from itertools import count
from mock import patch
from nose_parameterized import parameterized

from test.base import CommonDiskTestCase
from test.base import DiskTestCase
from test.fixtures.users import turkish_user
from test.helpers.stubs.resources import users_info
from test.parallelly.yateam_suit import BaseYaTeamTestCase
from test.parallelly.billing.base import BaseBillingTestCase
from test.common.sharing import CommonSharingMethods
from test.conftest import REAL_MONGO
from test.helpers.stubs.services import RateLimiterStub
from mpfs.config import settings
from mpfs.core.organizations.dao.organizations import OrganizationDAO
from mpfs.core.user.base import User


class CheckFolderContentMediaTypeHandleTestCase(CommonSharingMethods):
    test_folder = '/disk/test'

    def setup_method(self, method):
        super(CheckFolderContentMediaTypeHandleTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.test_folder})
        self.json_ok('set_public', {'uid': self.uid, 'path': self.test_folder})

    def _get_private_hash(self):
        return self.json_ok('info', {'uid': self.uid, 'path': self.test_folder, 'meta': 'public_hash'})['meta']['public_hash']

    def test_empty_folder(self):
        resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder})
        assert resp['only_image_and_video'] == True

    def test_public_empty_folder(self):
        private_hash = self._get_private_hash()
        resp = self.json_ok('public_check_folder_content_media_type', {'private_hash': private_hash})
        assert resp['only_image_and_video'] == True

    def test_with_subfolder(self):
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '1.jpg'), media_type='image')
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder})
        assert resp['only_image_and_video'] == False

    def test_public_with_subfolder(self):
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '1.jpg'), media_type='image')
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        private_hash = self._get_private_hash()
        resp = self.json_ok('public_check_folder_content_media_type', {'private_hash': private_hash})
        assert resp['only_image_and_video'] == False

    def test_only_video_and_photo(self):
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '1.jpg'), media_type='image')
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '2.jpg'), media_type='image')
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '3.avi'), media_type='video')
        resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder})
        assert resp['only_image_and_video'] == True

    def test_video_photo_documet_media_types(self):
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '1.jpg'), media_type='image')
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '2.jpg'), media_type='image')
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '3.avi'), media_type='video')
        self.upload_file(self.uid, "%s/%s" % (self.test_folder, '1.odt'), media_type='document')
        resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder})
        assert resp['only_image_and_video'] == False

    def test_shared_root_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        self.create_share_for_guest(self.uid, self.test_folder, self.uid_3, self.email_3)
        self.json_ok('move', {'uid': self.uid_3, 'src': self.test_folder, 'dst': '/disk/member_folder'})
        # owner root
        owner_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder})
        assert owner_resp['only_image_and_video'] is False
        # member root
        member_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid_3, 'path': '/disk/member_folder'})
        assert member_resp['only_image_and_video'] is False

    def test_shared_sub_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        self.create_share_for_guest(self.uid, self.test_folder, self.uid_3, self.email_3)
        self.json_ok('move', {'uid': self.uid_3, 'src': self.test_folder, 'dst': '/disk/member_folder'})
        # owner
        owner_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        assert owner_resp['only_image_and_video'] is True
        # member
        member_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid_3, 'path': '/disk/member_folder/dir'})
        assert member_resp['only_image_and_video'] is True

        self.upload_file(self.uid, "%s/dir/%s" % (self.test_folder, '1.odt'), media_type='document')
        # owner
        owner_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid, 'path': self.test_folder + '/dir'})
        assert owner_resp['only_image_and_video'] is False
        # member
        member_resp = self.json_ok('check_folder_content_media_type', {'uid': self.uid_3, 'path': '/disk/member_folder/dir'})
        assert member_resp['only_image_and_video'] is False


class TestWebFrontendApi(CommonSharingMethods):

    def setup_method(self, method):
        super(TestWebFrontendApi, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})

        opts = {
            'uid' : self.uid,
            'path' : '/disk/photo',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid' : self.uid,
            'path' : '/disk/music',
        }
        self.json_ok('mkdir', opts)

        for i in xrange(1, 3):
            file_path = '/disk/photo/img_%s.jpg' % i
            file_data = {
                'mimetype' : 'image/jpeg',
            }
            self.upload_file(self.uid, file_path, file_data=file_data)
            opts = {
                'uid' : self.uid,
                'path' : file_path,
            }
            self.json_ok('set_public', opts)
            file_path = '/disk/music/audio_%s.jpg' % i
            file_data = {
                'mimetype' : 'audio/mp3',
            }
            self.upload_file(self.uid, file_path, file_data=file_data)
            opts = {
                'uid' : self.uid,
                'path' : file_path,
            }
            self.json_ok('set_public', opts)

    def test_timeline(self):
        opts = {
            'uid' : self.uid,
            'path' : '/disk/music'
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 3)
        opts = {
            'uid' : self.uid,
            'path' : '/disk/photo'
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 3)
        opts = {
            'uid' : self.uid,
            'path' : '/disk',
            'meta' : 'drweb,short_url,public,public_hash,group,with_shared,\
                    shared_rights,sizes,systemLabel,from,to,mid,subject, \
                    mediatype,original_parent_id,size,fullname, \
                    download_counter,blocked,folder_type,etime,visible',
            'offset' : 0,
            'amount' : 5,
            'order' : '1',
            'public' : '1',
            'sort' : 'name',
            'visible' : '1',
        }
        timeline_result = self.json_ok('timeline', opts)
        self.assertEqual(len(timeline_result), 5)
        for each in timeline_result[1:3]:
            self.assertTrue(each['name'].startswith('audio_'))
        for each in timeline_result[3:]:
            self.assertTrue(each['name'].startswith('img_'))

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_get_last_files(self):
        """Протестировать количество и порядок возвращаемых файлов ручки get_last_files"""
        # Ожидаем обработки всех файлов из setup'а
        time.sleep(1)
        files = ['/disk/test%d.txt' % i for i in xrange(5)]
        now = time.time()
        for i, file_ in enumerate(files):
            with patch.object(time, 'time', return_value=now + i):
                self.upload_file(self.uid, file_)
        result = self.json_ok('get_last_files', {'uid': self.uid, 'amount': 5})
        assert len(result) == 5 + 1  # + корень диска
        assert sorted(files, reverse=True) == [f['path'] for f in result[1:]]

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_get_last_files_shared_success(self):
        """Протестировать количество последних файлов и их порядок,
        включая файлы из общей папки.
        """
        shared_folder = '/disk/Shared/'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder})
        gid = self.create_group(path=shared_folder)
        hsh = self.invite_user(uid=self.uid_1, path=shared_folder, email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        files = ['/disk/test%d.txt' % i for i in xrange(5)]
        files.extend(shared_folder + 'test%d.txt' % i for i in xrange(5, 10))

        now = time.time()
        for i in range(5):
            with patch.object(time, 'time', return_value=now + i):
                self.upload_file(self.uid_1, files[i])
        for i in range(5, 10):
            with patch.object(time, 'time', return_value=now + i):
                self.upload_file(self.uid, files[i])

        result = self.json_ok('get_last_files', {'uid': self.uid_1, 'amount': 20})
        assert len(result) == 10 + 1  # + корень диска
        assert sorted(files, key=lambda p: p.split('/')[-1], reverse=True) == [f['path'] for f in result[1:]]

    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_get_last_files_shared_duplicates(self):
        """Протестировать, что нет дубликатов в ответе, когда запрашиваются гостем
        последние файлы из двух папок с именами которые начинаются одинаково, т.е. /abc /abc2
        """

        files = []
        now = time.time()
        clock = count()
        for shared_folder in ('/disk/Shared/', '/disk/Shared2/'):
            self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder})
            gid = self.create_group(path=shared_folder)
            hsh = self.invite_user(uid=self.uid_1, path=shared_folder, email=self.email_1, ext_gid=gid)
            self.activate_invite(uid=self.uid_1, hash=hsh)

            for i in xrange(2):
                file_ = '%stest%d.txt' % (shared_folder, i)
                with patch.object(time, 'time', return_value=now + next(clock)):
                    self.upload_file(self.uid, file_)
                    files.append(file_)

        result = self.json_ok('get_last_files', {'uid': self.uid_1, 'amount': 10, 'meta': 'file_id'})
        assert len(result) == len(files) + 1  # + корень диска
        assert files[::-1] == [f['path'] for f in result[1:]]
        assert len(files) == len({f['meta']['file_id'] for f in result[1:]})

    @parameterized.expand([
        ('list', {'path': '/disk/photo'}),
        ('timeline', {'path': '/disk', 'offset': 0, 'public': '1', 'sort': 'name', 'visible': '1'}),
        ('new_get_last_files', {}),
    ])
    def test_views_counter(self, handler, opts):
        opts['uid'] = self.uid
        listing = self.json_ok(handler, opts)
        for item in listing:
            self.assertNotIn('meta', item)
        opts['meta'] = ''
        listing = self.json_ok(handler, opts)
        for item in listing:
            self.assertIn('meta', item)
            if item['meta'].get('public_hash'):
                self.assertIn('views_counter', item['meta'])


class LimitsSelectedUidsByRateLimiter(CommonDiskTestCase):
    def test_by_private_hash(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        resp = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1'})
        private_hash = resp['hash']
        group_name = settings.rate_limiter['limit_by_uid']['public_group_name']
        with patch('mpfs.frontend.api.RATE_LIMITER_LIMIT_BY_UID_UIDS', new={self.uid}):
            with RateLimiterStub() as stub:
                self.json_ok('public_info', {'private_hash': private_hash})
                stub.rate_limiter_service_is_limit_exceeded.assert_called_once_with(group_name, self.uid)

    def test_by_uid(self):
        group_name = settings.rate_limiter['limit_by_uid']['common_group_name']
        with patch('mpfs.frontend.api.RATE_LIMITER_LIMIT_BY_UID_UIDS', new={self.uid}):
            with RateLimiterStub() as stub:
                self.json_ok('user_info', {'uid': self.uid})
                stub.rate_limiter_service_is_limit_exceeded.assert_called_once_with(group_name, self.uid)

    def test_user_not_in_config(self):
        with patch('mpfs.frontend.api.RATE_LIMITER_LIMIT_BY_UID_UIDS', new=set()):
            with RateLimiterStub() as stub:
                self.json_ok('user_info', {'uid': self.uid})
                stub.rate_limiter_service_is_limit_exceeded.assert_not_called()


class UserFeatureTooglesTestCase(BaseYaTeamTestCase, CommonDiskTestCase):

    def test_fields(self):
        fields = {'versioning_extended_period',
                  'priority_support',
                  'advertising',
                  'antifo',
                  'disk_pro',
                  'disk_pro_without_ps_billing',
                  'online_editor',
                  'desktop_folder_autosave',
                  'unlimited_video_autouploading',
                  'unlimited_photo_autouploading',
                  'promote_mail360',
                  'public_settings'}

        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert not resp.viewkeys() ^ fields
        for value in resp.itervalues():
            assert not {'enabled'} ^ value.viewkeys()

    def test_b2b_paid(self):
        User(self.uid).make_b2b('125')
        OrganizationDAO().set_quota_limits_and_paid('125', 100000, 1000, True)
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        for toogle_name, enabled in (('versioning_extended_period', True),
                                     ('disk_pro', True),
                                     ('priority_support', True),
                                     ('advertising', False),
                                     ('antifo', False),
                                     ('versioning_extended_period', True),
                                     ('public_settings', False)):
            assert resp[toogle_name]['enabled'] == enabled

    def test_paid(self):
        self.billing_ok('service_create', {'uid': self.uid, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        for toogle_name, enabled in (('versioning_extended_period', True),
                                     ('priority_support', True),
                                     ('disk_pro', True),
                                     ('disk_pro_without_ps_billing',True),
                                     ('advertising', False),
                                     ('antifo', False),
                                     ('versioning_extended_period', True)):
            assert resp[toogle_name]['enabled'] == enabled

    def test_disk_pro_without_ps_billing(self):
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

        ps_billing_sid = self.billing_ok('service_create', {'uid': self.uid, 'line': 'partner',
                                                            'pid': 'yandex_b2c_mail_pro', 'ip': 'localhost',
                                                            'product.amount': 7}).get('sid')
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is True

        disk_pro_sid = self.billing_ok(
            'service_create', {'uid': self.uid, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'}).get('sid')
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is True
        assert resp['disk_pro']['enabled'] is True

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': disk_pro_sid})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is True

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': ps_billing_sid})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

    def test_disk_pro_without_ps_billing_empty_setting(self):
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

        ps_billing_sid = self.billing_ok('service_create', {'uid': self.uid, 'line': 'partner',
                                                            'pid': 'yandex_b2c_mail_pro', 'ip': 'localhost',
                                                            'product.amount': 7}).get('sid')
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is True

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': ps_billing_sid})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

    def test_disk_pro_promo_without_ps_billing(self):
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

        ps_billing_sid = self.billing_ok('service_create', {'uid': self.uid, 'line': 'partner',
                                                            'pid': 'yandex_b2c_mail_pro_promo', 'ip': 'localhost',
                                                            'product.amount': 7}).get('sid')
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is True

        disk_pro_sid = self.billing_ok(
            'service_create', {'uid': self.uid, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'}).get('sid')
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is True
        assert resp['disk_pro']['enabled'] is True

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': disk_pro_sid})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is True

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': ps_billing_sid})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['disk_pro_without_ps_billing']['enabled'] is False
        assert resp['disk_pro']['enabled'] is False

    def test_ya_staff(self):
        self._make_yateam(self.uid)
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        for toogle_name, enabled in (('versioning_extended_period', True),
                                     ('priority_support', True),
                                     ('disk_pro', True),
                                     ('advertising', False),
                                     ('antifo', False),
                                     ('versioning_extended_period', True)):
            assert resp[toogle_name]['enabled'] == enabled

    def test_common_user(self):
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        for toogle_name, enabled in self._get_default_features():
            assert resp[toogle_name]['enabled'] == enabled

    def test_blocked_users(self):
        User(self.uid).set_block(True)
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        for toogle_name, enabled in self._get_default_features():
            assert resp[toogle_name]['enabled'] == enabled

    def test_not_init_user(self):
        resp = self.json_ok('user_feature_toggles', {'uid': turkish_user.uid})

        for toogle_name, enabled in self._get_default_features():
            assert resp[toogle_name]['enabled'] == enabled

    def test_advertising_disabled_user(self):
        from mpfs.core.base import ADVERTISING_FEATURE
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': ADVERTISING_FEATURE, 'value': 0})

        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['advertising']['enabled'] is False

    def test_legacy_endpoint(self):
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        resp_legacy = self.json_ok('user_feature_toogles', {'uid': self.uid})
        assert resp_legacy == resp

    def test_promote_mail360_disabled_user(self):
        users_info.update_info_by_uid(self.uid, has_mail360=True)
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['promote_mail360']['enabled'] is False

    def test_public_settings(self):
        from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
        # enable
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['public_settings']['enabled'] is True
        # disable
        self.json_ok('set_ps_billing_feature', {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 0})
        resp = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert resp['public_settings']['enabled'] is False

    def _get_default_features(self):
        return (('versioning_extended_period', False),
                 ('priority_support', False),
                 ('disk_pro', False),
                 ('advertising', True),
                 ('antifo', True),
                 ('versioning_extended_period', False),
                 ('promote_mail360', True))
