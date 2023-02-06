# -*- coding: utf-8 -*-
from mpfs.core.albums.static import GeneratedAlbumType
from test.parallelly.json_api.base import CommonJsonApiTestCase


class AlbumsExcludeFromGeneratedTestCase(CommonJsonApiTestCase):
    def test_default(self):
        path = '/disk/raccoon.jpg'
        self.upload_file(self.uid, path)
        self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ','})

        info_result = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ','})
        assert 'albums_exclusions' not in info_result['meta']

        result = self.json_ok('albums_exclude_from_generated',
                              {'uid': self.uid, 'path': path, 'album_type': 'beautiful'})
        assert GeneratedAlbumType.BEAUTIFUL.value in result['items']

        info_result = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ','})
        assert GeneratedAlbumType.BEAUTIFUL.value in info_result['meta']['albums_exclusions']
