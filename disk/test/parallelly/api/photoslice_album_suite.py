# -*- coding: utf-8 -*-
import mock
import pytest

from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.core.photoslice.albums.static import PhotosliceAlbumTypeConst
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.photoslice_album.base import PhotosliceAlbumBaseTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
class PhotosliceAlbumInternalAPITestCase(PhotosliceAlbumBaseTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_can_read_photoslice_album_type(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)

        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk/resources', uid=self.uid, query={'path': '1.jpg'})
        response = from_json(resp.content)
        assert response['photoslice_album_type'] == PhotosliceAlbumTypeConst.CAMERA


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
class PhotosliceAlbumExternalAPITestCase(PhotosliceAlbumBaseTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def test_can_read_photoslice_album_type(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)

        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk/resources', uid=self.uid, query={'path': '1.jpg'})
        response = from_json(resp.content)
        assert 'photoslice_album_type' not in response

    def test_photoslice_album_type_presented_for_native_clients(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)

        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id='disk_verstka'):
            resp = self.client.get('disk/resources', uid=self.uid, query={'path': '1.jpg'})
        response = from_json(resp.content)
        assert response['photoslice_album_type'] == PhotosliceAlbumTypeConst.CAMERA

    def test_photoslice_fileds(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)
        with open('fixtures/json/photoslice.json') as f:
            photoslice_resp = f.read()
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id='disk_verstka'), \
             mock.patch('mpfs.core.services.smartcache_service.smartcache.open_url', return_value=(200, photoslice_resp, {})):
            resp = self.client.get('disk/photoslice/1', uid=self.uid, query={'path': '1.jpg'})
        response = from_json(resp.content)
        assert response['index']['items'][0]['albums'] == {
            "unbeautiful": 1,
            "beautiful": 1
          }
        assert response['albums']['items'][0] == {
          "album": "beautiful",
          "previews": [
            "/disk/file1.jpg",
            "/disk/file2.jpg"
          ],
          "count": 2
        }
        assert response['clusters']['items'][0]['items'][0]['albums'] == ['beautiful']
        assert response['clusters']['items'][0]['items'][1]['albums'] == ['unbeautiful']

    def test_photoslice_delta_fileds(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)
        with open('fixtures/json/photoslice_delta.json') as f:
            photoslice_resp = f.read()
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id='disk_verstka'), \
             mock.patch('mpfs.core.services.smartcache_service.smartcache.open_url', return_value=(200, photoslice_resp, {})):
            resp = self.client.get('disk/photoslice/1/deltas', uid=self.uid, query={'path': '1.jpg', 'base_revision': 4600})
        response = from_json(resp.content)
        assert response['items'][0]['index_changes'][0]['data']['albums'][0] == {
                  "album": "beautiful",
                  "change_type": "update",
                  "count": 1
                }
        assert response['items'][0]['index_changes'][0]['data']['albums'][1] == {
                  "album": "beautiful2",
                  "change_type": "update",
                  "count": 1
                }
        assert response['items'][0]['albums_changes'][0] == {
            "change_type": "insert",
            "album": "beautiful",
            "data": {
              "previews": [
                "/disk/file3.jpg"
              ],
              "count": 3
            }
          }

    def test_photoslice_fileds(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)
        with open('fixtures/json/photoslice.json') as f:
            photoslice_resp = f.read()
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id='disk_verstka'), \
             mock.patch('mpfs.core.services.smartcache_service.smartcache.open_url', return_value=(200, photoslice_resp, {})):
            resp = self.client.get('disk/photoslice/1', uid=self.uid, query={'path': '1.jpg'})
        response = from_json(resp.content)
        assert response['index']['items'][0]['albums'] == {
            "unbeautiful": 1,
            "beautiful": 1
          }
        assert response['albums']['items'][0] == {
          "album": "beautiful",
          "previews": [
            "/disk/file1.jpg",
            "/disk/file2.jpg"
          ],
          "count": 2
        }
        assert response['clusters']['items'][0]['items'][0]['albums'] == ['beautiful']
        assert response['clusters']['items'][0]['items'][1]['albums'] == ['unbeautiful']

    def test_photoslice_delta_fileds(self):
        path = '/disk/1.jpg'
        self._upload_file_with_photoslice_album_type(self.uid, path)
        with open('fixtures/json/photoslice_delta.json') as f:
            photoslice_resp = f.read()
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid, id='disk_verstka'), \
             mock.patch('mpfs.core.services.smartcache_service.smartcache.open_url', return_value=(200, photoslice_resp, {})):
            resp = self.client.get('disk/photoslice/1/deltas', uid=self.uid, query={'path': '1.jpg', 'base_revision': 4600})
        response = from_json(resp.content)
        assert response['items'][0]['index_changes'][0]['data']['albums'][0] == {
                  "album": "beautiful",
                  "change_type": "update",
                  "count": 1
                }
        assert response['items'][0]['index_changes'][0]['data']['albums'][1] == {
                  "album": "beautiful2",
                  "change_type": "update",
                  "count": 1
                }
        assert response['items'][0]['albums_changes'][0] == {
            "change_type": "insert/delete/update/set",
            "album": "beautiful",
            "data": {
              "previews": [
                "/disk/file3.jpg"
              ],
              "count": 3
            }
          }
