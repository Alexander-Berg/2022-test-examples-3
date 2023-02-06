from urlparse import parse_qs, urlparse

import mock
from hamcrest import assert_that, equal_to, contains_string

from mpfs.common.static.tags import NAME, URL
from mpfs.common.static.tags import experiment_names
from mpfs.core.bus import Bus
from mpfs.core.metastorage.control import groups
from test.parallelly.json_api.base import CommonJsonApiTestCase
from nose_parameterized import parameterized


class InfoTestCase(CommonJsonApiTestCase):

    @parameterized.expand(['/disk', '/disk/'])
    def test_resource_has_no_file_id(self, path):
        resp = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'resource_id,file_id'})
        assert 'file_id' not in resp['meta']
        if 'resource_id' in resp['meta']:
            assert resp['meta']['resource_id'] is not None

    @parameterized.expand([
        ('all_active', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                        experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: True},
         False),
        ('only_set', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                      experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False},
         True),
        ('all_disabled', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: False,
                          experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False},
         True),
    ])
    def test_has_shared_folders(self, case_name, exps, expected_usage_of_common):
        def fake_is_feature_active(_, name):
            return exps.get(name)

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        fake_is_feature_active), \
                mock.patch('mpfs.core.social.share.group.groups.get_all', return_value=[]) as spy_on_groups, \
                mock.patch('mpfs.core.social.share.group.group_links.get_all', return_value=[]) as spy_on_group_links:
            self.json_ok('list', {'uid': self.uid, 'path': '/disk'})

        assert_that(spy_on_groups.called, equal_to(expected_usage_of_common))
        assert_that(spy_on_group_links.called, equal_to(expected_usage_of_common))

    @parameterized.expand([
        ('all_active', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                        experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: True},
         'using flag'),
        ('only_set', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: True,
                      experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False},
         'match with real state'),
        ('all_disabled', {experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_SET_FLAG: False,
                          experiment_names.REQUESTS_TO_COMMON_FOR_GROUPS_USE_FLAG: False},
         'match with real state'),
    ])
    def test_has_shared_folders_exps(self, case_name, exps, pattern):
        def fake_is_feature_active(_, name):
            return exps.get(name)

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                        fake_is_feature_active), \
            mock.patch('mpfs.core.user.common.log_smart_common_requests') as mocked_logger:
            from mpfs.core.user.base import User
            User(self.uid).has_shared_folders()

        assert_that(mocked_logger, contains_string(pattern))

    def test_resource_has_file_id(self):
        path = '/disk/test1.jpg'
        self.upload_file(self.uid, path)
        resp = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'resource_id,file_id'})
        assert 'resource_id' in resp['meta'] and 'file_id' in resp['meta']

    @parameterized.expand([
        (None, ),
        ("", ),
    ])
    def test_mime_type_is_application_octet_stream_if_its_empty_or_none(self, new_mimetype):
        path = '/disk/test1.jpg'
        self.upload_file(self.uid, path)
        Bus().setprop(self.uid, ':'.join([self.uid, path]), {'mimetype': new_mimetype})
        resp = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'mimetype'})
        assert resp['meta']['mimetype'] == 'application/octet-stream'

    def test_preview_allow_big_size_parameter(self):
        hardlink_file_path = '/disk/2.png'
        self.upload_file(self.uid, hardlink_file_path, media_type='image', opts={})

        res = self.json_ok('info', {'uid': self.uid, 'path': hardlink_file_path, 'meta': ''})
        preview_urls = ([res['meta']['preview'], res['meta']['custom_preview'], res['meta']['thumbnail']]
                        + [x[URL] for x in res['meta']['sizes']])
        assert all([parse_qs(urlparse(url).query).get('allow_big_size') is None for url in preview_urls])

        res = self.json_ok('info',
                           {'uid': self.uid, 'path': hardlink_file_path, 'meta': '', 'preview_allow_big_size': '1'})
        preview_urls_no_big_size = ([res['meta']['preview'], res['meta']['thumbnail']]
                                    + [x[URL] for x in res['meta']['sizes'] if x[NAME] != 'DEFAULT'])
        preview_urls_big_size = [res['meta']['custom_preview']] + [x[URL] for x in res['meta']['sizes'] if
                                                                   x[NAME] == 'DEFAULT']
        assert all([parse_qs(urlparse(url).query).get('allow_big_size') is None for url in preview_urls_no_big_size])
        assert all([parse_qs(urlparse(url).query).get('allow_big_size') == ['1'] for url in preview_urls_big_size])
