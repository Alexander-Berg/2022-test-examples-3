import mock
from test.parallelly.json_api.base import CommonJsonApiTestCase
from mpfs.common.static import codes


class ListTestCase(CommonJsonApiTestCase):
    def test_dav_listing_limit(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/testdir'})
        path = '/disk/testdir/test1.jpg'
        self.upload_file(self.uid, path)
        path = '/disk/testdir/test2.jpg'
        self.upload_file(self.uid, path)
        with mock.patch('mpfs.core.services.disk_service.DAV_MAX_FILES_LISTING_LIMIT', 10):
            self.json_ok('list', {'uid': self.uid, 'path': '/disk/testdir', 'meta': ''},
                         headers={'Yandex-Cloud-Request-ID': 'dav-xxxxxx-sas1-xxxxx'})

        with mock.patch('mpfs.core.services.disk_service.DAV_MAX_FILES_LISTING_LIMIT', 1):
            path = '/disk/testdir/test3.jpg'
            self.upload_file(self.uid, path)
            # listing should not work for webdav- ycrid when limit is reached
            self.json_error('list', {'uid': self.uid, 'path': '/disk/testdir', 'meta': ''},
                            headers={'Yandex-Cloud-Request-ID': 'dav-xxxxxx-sas1-xxxxx'},
                            code=codes.TOO_MANY_FILES_IN_LISTING, status=429)
            # limit should not influence other ycrids
            self.json_ok('list', {'uid': self.uid, 'path': '/disk/testdir', 'meta': ''},
                         headers={'Yandex-Cloud-Request-ID': 'mpfs-xxxxxx-sas1-xxxxx'})
            # requests with amount should work for all ycrids
            self.json_ok('list', {'uid': self.uid, 'path': '/disk/testdir', 'meta': '', 'amount': 1},
                            headers={'Yandex-Cloud-Request-ID': 'dav-xxxxxx-sas1-xxxxx'})
