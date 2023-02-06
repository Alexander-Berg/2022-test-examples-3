# -*- coding: utf-8 -*-

from __future__ import unicode_literals
import mock

from test.parallelly.api.disk.base import DiskApiTestCase

from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.parallelly.api.base import ApiTestCase, InternalPlatformTestClient, ExternalPlatformTestClient
from mpfs.common.util import from_json
from mpfs.common.static import tags


class CommonSchemaTestCase(ApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL

    schema_fields = {'basePath', 'swaggerVersion', 'apiVersion', 'apis'}
    api_item_fields = {'path', 'description'}
    resource_fields = schema_fields | {'consumes', 'resourcePath', 'produces', 'models', 'description'}

    def __init__(self, *args, **kwargs):
        super(CommonSchemaTestCase, self).__init__(*args, **kwargs)
        base_uri = 'http://localhost'
        self.internal_client = InternalPlatformTestClient(base_uri)
        self.external_client = ExternalPlatformTestClient(base_uri)

    def test_root(self):
        resp = self.client.request('GET', '/')
        assert resp.status_code == 200
        assert 'build' in resp.content
        assert 'api_version' in resp.content

    def test_root_auth(self):
        with mock.patch('mpfs.core.services.passport_service.blackbox.check_oauth_token', return_value=True) as m:
            headers = {'Authorization': 'OAuth test'}
            resp = self.client.request('GET', '/', headers=headers)
            m.assert_not_called()
            assert resp.status_code == 200
            assert 'build' in resp.content
            assert 'api_version' in resp.content

    def test_v1_schema(self):
        resp = self.client.request('GET', '/v1/schema')
        assert resp.status_code == 200
        body = from_json(resp.content)
        assert not(body.viewkeys() ^ self.schema_fields)
        apis = body['apis']
        for item in body['apis']:
            assert not(item.viewkeys() ^ self.api_item_fields)

        paths = {api['path'] for api in apis}
        assert '/v1/disk/public/video' not in paths
        assert '/v1/disk/public/video/streams' not in paths

    def test_v1_schema_resource(self):
        resp = self.client.request('GET', '/v1/schema')
        assert resp.status_code == 200
        apis = from_json(resp.content)['apis']
        for api in apis:
            current_path = '/v1/schema/resources%s' % api['path']
            if current_path != '/v1/schema/resources/v1/disk/resources':
                continue
            resp = self.client.request('GET', current_path)
            assert resp.status_code == 200, 'Failed on "%s"' % current_path
            body = from_json(resp.content)
            assert not(body.viewkeys() ^ self.resource_fields)


class GetSchemaIndexHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    def get_visible_paths(self, resource_path):
        resource_path = resource_path.strip('/')
        response = self.client.get('schema/resources/%s' % resource_path)
        assert response.status_code == 200
        data = from_json(response.content)
        return [api['path'] for api in data['apis']]

    def get_schema_paths(self):
        response = self.client.get('schema')
        data = from_json(response.content)
        return [api['path'] for api in data['apis']]


class GetSchemaIndexHandlerInternalTestCase(GetSchemaIndexHandlerTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_v1_disk_resources_visibility(self):
        """Проверить видимость ресурсов `/v1/disk/resources`."""
        visible_paths = self.get_visible_paths('/v1/disk/resources')
        assert '/v1/disk/resources/{resource_id}' in visible_paths
        assert '/v1/disk/resources/{resource_id}/dimensions' in visible_paths

        all_visible_paths = self.get_schema_paths()
        assert '/v1/chaining/request' in all_visible_paths

    def test_v1_notifier_resources_visibility(self):
        self.get_visible_paths('/v1/notifier/notifications/{service}/{notification_type}/{notification_id}/mark-as-read')

    def test_v1_disk_public_video_streams_resources_visibility(self):
        visible_paths = self.get_visible_paths('/v1/disk/public/video/streams')
        assert '/v1/disk/public/video/streams' in visible_paths


class GetSchemaIndexHandlerExternalTestCase(GetSchemaIndexHandlerTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def test_v1_disk_resources_visibility(self):
        """Проверить видимость ресурсов `/v1/disk/resources`."""
        visible_paths = self.get_visible_paths('/v1/disk/resources')
        assert '/v1/disk/resources/{resource_id}' not in visible_paths
        assert '/v1/disk/resources/{resource_id}/dimensions' not in visible_paths

        all_visible_paths = self.get_schema_paths()
        assert '/v1/chaining/request' not in all_visible_paths

    def test_v1_notifier_resources_visibility(self):
        self.assertRaises(
            AssertionError,
            lambda: self.get_visible_paths(
                '/v1/notifier/notifications/{service}/{notification_type}/{notification_id}/mark-as-read'
            )
        )
