# -*- coding: utf-8 -*-
import contextlib
import mock
import pytest
import urlparse

from test.base import DiskTestCase, patch_open_url

from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import SearchIndexerStub, DiskSearchStub

from mpfs.common.util import from_json
from mpfs.frontend.api.disk.photoslice import Photoslice


@contextlib.contextmanager
def patch_photoslice_dummies_toggle(value=True):
    import mpfs.core.photoslice.interface
    original = mpfs.core.photoslice.interface.PHOTOSLICE_DUMMIES_TOGGLE
    try:
        mpfs.core.photoslice.interface.PHOTOSLICE_DUMMIES_TOGGLE = value
        yield
    finally:
        mpfs.core.photoslice.interface.PHOTOSLICE_DUMMIES_TOGGLE = original


class PhotosliceTestCase(DiskTestCase):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {DiskSearchStub})

    def photoslice_ok(self, method, opts={}):
        photoslice_api = Photoslice()
        return self.json_ok(method, opts, api=photoslice_api)

    def test_periods_counts(self):
        body = '''{"list":[{"year":2014,"list":[{"month":9,"amount":5},{"month":8,"amount":31},{"month":6,"amount":1},{"month":5,"amount":4},{"month":4,"amount":616},{"month":3,"amount":26},{"month":2,"amount":81}]},{"year":2013,"list":[{"month":12,"amount":10},{"month":11,"amount":13},{"month":10,"amount":6},{"month":4,"amount":2}]},{"year":2012,"list":[{"month":8,"amount":6},{"month":4,"amount":5}]},{"year":2011,"list":[{"month":6,"amount":3},{"month":2,"amount":1}]}]}'''

        with patch_open_url(body):
            result = self.photoslice_ok('periods_counts', {'uid': self.uid})
            assert 'version' in result
            assert 'list' in result
            assert [x for x in result['list'] if x.get('year') == 2014]
            assert 'amount' in result['list'][0]['list'][0]

    def test_periods_ids(self):
        body = '''{"list":[{"year":2014,"list":[{"month":7,"ids":["1cd8a0c96718c5f8c0d9fa4bc9321e3decfcf72d1824b859c26f99fb09e7f787"]}]},{"year":2013,"list":[{"month":10,"ids":["aad5b292ebf1c7f4d3950b4a6855790e5fb2b1abadaddaf4b0c4306f101accda"]},{"month":6,"ids":["f1db713902eb2a5351e9e2678275fc9264baef6564c4acbdda0f2cb0d897ec45","ebfde734b62e430a0f6e0f486538eca077df067abf58c72e1be980a2d15dec63","e2d954b2149bb2d40841dcf990dd8e306551548bf01a318fb7667789450217a1","d3aa721b0707cfed9e6067e89e5c570ba46e83e6d93da937d55e3d6ecf8a9821","5f309f4ef28d93f57662aee48112879fa8f3c5df11d4bc1b2df19b08671abe5f","2f05c116e9db6509ed9f649057223a929f1593d1423b6f259a24326976c71782","02cdd0732514e568cdf4ce9ad6691ddbbc39b8320cf9bf391721982e6f05d636"]},{"month":5,"ids":["b36d24299e1327981d996ebce3186cf2c4abe34710ff24dea17790636ce94ef5"]},{"month":4,"ids":["ba1e9e488f16b07b1cd9472bb9c9b9912655d0d81c8c6e90e63033315dc6bdbe","18eb0b8b34bb3c4a6a98f515a7de2d511b736b4f7de8cfe5c5ca23347d0ad1c7","006908894d9d275be6ffa1357fb6391469ec3301cbb73efa836d4843a13e3bd5"]},{"month":3,"ids":["44960824e069d8a27d63cc5a8522582df5974eebf460da7f36cd51cf60dfba4f"]},{"month":1,"ids":["f8c28a2b46a55508c1cf9ad4ae1384b5b47f1c0b381450ce7fa42a494113d809","f058d2d8a8cbe350dd168740e82a8053ffda245c3550c31a325fd36dbe1cfa89","e85149fc1c9864175455dbcfcbf6c12510d896d979f8e839dec9524775510af4","d39f41e355fb524f9fcb5cb4e5d4727c17ad2076fc27c23f61fddd96dda85ed7","d174b06418bfd814eb7720bb63f1c660bf8335f9ee181e3fc899a47d25b5f3f2","c71285239ae2b1a4dd31883c7b1429055832250c1d0304bdcd6f3e2f58efd997","c66d2fcd23f87da918584297c7ea73702ddc409d2679f217d5ddfbb914e52060","b9986e4fac692d1f72122aadafee7d5eb1b2aea24c48d5db07db4f6c6d57dcda","b89d15a30a63057a6acd0b7587e7b0d390ec9bb8c2f2ad1f9818987a725d3fca","b342892f9a80d617e404e6231260afda74967d2879f4811af7fa49476ce65913","a038ee1b83dbab038d66769879ec145d773cf2701e16ff97d24689c038936725","95c23ef992298d0582394d13b349c8c5b0b2d13624d6ea6dde8a7d72a46b4b7c","89cf1b77b22e9676f7299c55f736eb47619fdcc58b4b77843cf8be642260b4e3","8782da5287ebb70b857b03f23d524e6f6561b332f6d3799379e284307fb59f5b","83a32dfde8efcb48a24970f3afb83889852668e9d3c78e128b860c5d0837aeb6","82c38f0794a14fd6c830c77c7303c5df0b2de20c13dc1a2eaceabba6af6fbbfb","7b643215e20b30755c7afc9d57dd5e837afc50b37eed3e6f4974578d6ec25d0b","7ac5744b2f47416173794d06c065d6e0e7842f779b9e144759691b29472144bc","741c44b41bfa621f9460b73665961614c98a13b45651902002108890de8f13a1","706b77042b9e83417bd1dcfe8a9d4e9bf47c72cbc54b70e5f07d7f285498123f","5da2ca84e3112eba111b1f12c5dde47aac2d40ed49713bbe9d28e20004e2b0de","5a1d2157048828dee21b537e3133971970a6cf2ffb69bd644bec93082dcb0ba5","588f97f6ba3732e0abf9d301c98c076e3af9e0d0ac0b7b1f81395228ea0ea317","53738a764af410f4646b8e7283499cd2884dd585b8e279783b10e054448f85ff","43271a7ba577e627c28f4c8514bc956e05341645c5e8eb454fd2f53aa80f0b3b","412fc9d1d4b4fc66125ef5b775ecf2c2591e0648adb46984aa095c58a98d570e","3b94e5447cc586b7d37e73b1cbbfbbd48818f1aa361a2770428054453e75515c","2108ddd456640ef13177fcaab095c95206e25e590f452011beafd0dfeeeaf0e0","181eacd16518d01043ecb57d3ae566ef9a87750b4b44b0d91e502de249a7c3d1","120aa76f863522b056cdc9bd5d61fb5d643a2f93b998b4209fe4a546fcf0873f","0d937014cecd7ad8f0200d084d343c7c7aefd7bff6cd4db46963322d8160bd48","059195aa63e608310fe1f7b014030f09b58462606fc515148f732cf4bb5be361"]}]},{"year":2012,"list":[{"month":9,"ids":["a892c7a62a7e7fb61881650e2176b690d62ad8bd2798ae813bc4b8251a8ac32e","71bae2068a54143b5e37c782c724b1b404b9121422fb2891322953d52d3a99d9","51ffb649a839bb20d38d755d283273955483c5a30e38e7471e268b4cd40e6fec","1c374c05d2506741f03a03958dbfbcb115011df94ea99080418e338c62cc6afc","19cae776fb51bbccaad5f380a11b4d779577e881881384e84dd79c6963ffba5c"]}]},{"year":2007,"list":[{"month":9,"ids":["d2d89922f91deba1098d39a599a1709a75fa2b7d40c296806dcbce77a22d1964"]}]}]}'''

        with patch_open_url(body):
            result = self.photoslice_ok('periods_ids', {'uid': self.uid})
            assert 'version' in result
            assert 'list' in result
            assert [x for x in result['list'] if x.get('year') == 2014]
            assert 'ids' in result['list'][0]['list'][0]

    def test_resources(self):
        self.upload_file(self.uid, '/disk/image1.jpg')
        self.upload_file(self.uid, '/disk/image2.jpg')

        search_body = '''[
        {
            "id": "a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d2",
            "key": "/disk/image1.jpg"
        },
        {
            "id": "a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d3",
            "key": "/disk/image2.jpg"
        },
        ]'''
        with patch_open_url(search_body):
            args = {
                'uid': self.uid,
                'amount': 2,
                'offset': 2,
                'version': 1,
            }
            result = self.photoslice_ok('resources', args)
            assert 'version' in result
            assert 'list' in result
            assert [x for x in result['list'] if x.get('path') == '/disk/image2.jpg']

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_dead_resources(self):
        """
        Проверяем, что для удаленных ресурсов мы выдаем заглушку

        https://st.yandex-team.ru/CHEMODAN-21299
        """
        self.upload_file(self.uid, '/disk/image1.jpg')
        self.upload_file(self.uid, '/disk/image2.jpg')

        search_body = '''[
        {
            "id": "a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d2",
            "key": "/disk/image1.jpg"
        },
        {
            "id": "a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d3",
            "key": "/disk/image2.jpg"
        },
        {
            "id": "a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d4",
            "key": "/disk/image3.jpg"
        },
        ]'''
        with patch_open_url(search_body):
            args = {
                'uid': self.uid,
                'amount': 3,
                'offset': 0,
                'version': 1,
            }
            print "Новое поведение - отдаем заглушки"
            with patch_photoslice_dummies_toggle(True):
                result = self.photoslice_ok('resources', args)
            assert 'version' in result
            assert 'list' in result

            # ищем заглушку удаленного ресурса
            found_dead_resource = False
            for item in result['list']:
                if item['path'] == '/disk/image3.jpg':
                    found_dead_resource = True
                    assert item['photoslice_lost'] is True
                    assert item['name'] == 'image3.jpg'
                    assert item['meta']['file_id'] == 'a9223699ee2e6b4b5b95d8f3226e53f11214714188228a4cf2b9be55238d81d4'
            assert found_dead_resource is True

            print "Старое поведение - отдаем как есть"
            with patch_photoslice_dummies_toggle(False):
                result = self.photoslice_ok('resources', args)
            assert 'version' in result
            assert 'list' in result
            assert len(result['list']) == 2

    def test_resources_empty_list(self):
        with patch_open_url('[]'):
            args = {
                'uid': self.uid,
                'amount': 2,
                'offset': 2,
                'version': 1,
            }
            result = self.photoslice_ok('resources', args)
            assert 'version' in result
            assert result['list'] == []


class PhotosliceInterfaceTestCase(DiskTestCase):
    mobile_headers = {'Yandex-Cloud-Request-ID': 'ios-test'}

    def test_photoslice_time_for_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        # image
        self.upload_file(self.uid, '/photounlim/1.jpg', file_data={'mimetype': 'image/tiff'}, headers=self.mobile_headers)
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''})
        assert 'photoslice_time' in info['meta']

        # video
        self.upload_file(self.uid, '/photounlim/1.mpeg', file_data={'mimetype': 'video/mpeg'}, headers=self.mobile_headers)
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.mpeg', 'meta': ''})
        assert 'photoslice_time' in info['meta']

        # .avi игнорируем
        self.upload_file(self.uid, '/photounlim/1.avi', file_data={'mimetype': 'video/mpeg'}, headers=self.mobile_headers)
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.avi', 'meta': ''})
        assert 'photoslice_time' not in info['meta']

        # игнорируем не video и не image
        self.upload_file(self.uid, '/photounlim/1.txt', headers=self.mobile_headers)
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.avi', 'meta': ''})
        assert 'photoslice_time' not in info['meta']

    def test_photoslice_time(self):
        # загружаем фото с etime просто так (etime == 1333620000)
        self.upload_file(
            self.uid, '/disk/photo_with_etime.raw',
            file_data={'mimetype': 'image/tiff', 'etime': '2012-04-05T10:00:00Z'}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/photo_with_etime.raw', 'meta': ''})
        assert 'photoslice_time' in info['meta']
        assert info['meta']['photoslice_time'] == 1333620000
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/photo_with_etime.raw':
                assert 'photoslice_time' in item['meta']
                assert item['meta']['photoslice_time'] == 1333620000

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/photo_with_etime.raw'})
        trash_info = self.json_ok('info', {'uid': self.uid, 'path': '/trash/photo_with_etime.raw', 'meta': ''})
        assert 'photoslice_time' not in trash_info['meta']

        # загружаем фото без etime просто так
        self.upload_file(self.uid, '/disk/photo_without_etime.jpg',
                         file_data={'mimetype': 'image/jpeg', 'etime': None})
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/photo_without_etime.jpg', 'meta': ''})
        assert 'photoslice_time' not in info['meta']
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/photo_without_etime.jpg':
                assert 'photoslice_time' not in item['meta']

        # загружаем фото без etime в Фотокамеру
        self.upload_file(
            self.uid, '/photostream/photo_without_etime.nef',
            file_data={'mimetype': 'image/tiff', 'etime': None}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/photo_without_etime.nef', 'meta': ''})
        assert 'photoslice_time' in info['meta']
        assert info['meta']['photoslice_time'] == info['ctime']
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/Фотокамера/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/Фотокамера/photo_without_etime.nef':
                assert 'photoslice_time' in item['meta']
                assert item['meta']['photoslice_time'] == item['ctime']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/Фотокамера/photo_without_etime.nef'})
        trash_info = self.json_ok('info', {'uid': self.uid, 'path': '/trash/photo_without_etime.nef', 'meta': ''})
        assert 'photoslice_time' not in trash_info['meta']

        # загружаем видео с etime просто так
        self.upload_file(
            self.uid, '/disk/video_with_etime.vid',
            file_data={'mimetype': 'video/x-video', 'etime': '2012-04-05T10:00:00Z'}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/video_with_etime.vid', 'meta': ''})
        assert 'photoslice_time' not in info['meta']
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/video_with_etime.vid':
                assert 'photoslice_time' not in item['meta']

        # загружаем видео без etime просто так
        self.upload_file(
            self.uid, '/disk/video_without_etime.mp4',
            file_data={'mimetype': 'video/all-video', 'etime': None}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/video_without_etime.mp4', 'meta': ''})
        assert 'photoslice_time' not in info['meta']
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/video_without_etime.mp4':
                assert 'photoslice_time' not in item['meta']

        # загружаем видео без etime в Фотокамеру
        self.upload_file(
            self.uid, '/photostream/video_without_etime.mkv',
            file_data={'mimetype': 'video/mkv', 'etime': None}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/video_without_etime.mkv', 'meta': ''})
        assert 'photoslice_time' in info['meta']
        assert info['meta']['photoslice_time'] == info['ctime']
        contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/Фотокамера/', 'meta': ''})
        for item in contents:
            if item['id'] == '/disk/video_without_etime.mkv':
                assert 'photoslice_time' not in item['meta']
                assert item['meta']['photoslice_time'] == item['ctime']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/Фотокамера/video_without_etime.mkv'})
        trash_info = self.json_ok('info', {'uid': self.uid, 'path': '/trash/video_without_etime.mkv', 'meta': ''})
        assert 'photoslice_time' not in trash_info['meta']

        # загружаем png картинку в Фотокамеру
        self.upload_file(
            self.uid, '/photostream/photo_without_etime.png',
            file_data={'mimetype': 'image/png', 'etime': None}
        )
        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/photo_without_etime.png', 'meta': ''})
        assert 'photoslice_time' in info['meta']
        assert info['meta']['photoslice_time'] == info['ctime']


class PhotosliceCallbackParamsInterfaceTestCase(DiskTestCase):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    def test_photoslice_callback_format(self):
        with mock.patch('mpfs.core.services.index_service.SearchIndexer.open_url') as open_url_mock, \
                mock.patch('mpfs.core.job_handlers.indexer.INDEXER_PHOTOSLICE_NOTIFICATION_ON_INDEXER_SIDE', True):
            self.upload_file(
                self.uid, '/disk/photo_with_etime.jpg',
                file_data={'mimetype': 'image/jpeg', 'etime': '2012-04-05T10:00:00Z'}
            )
            file_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/photo_with_etime.jpg', 'meta': ''})
            indexer_push_url = open_url_mock.call_args[0][0]
            indexer_push_qs = urlparse.parse_qs(urlparse.urlparse(indexer_push_url).query)
            assert 'callback' in indexer_push_qs
            callback_qs = urlparse.parse_qs(urlparse.urlparse(indexer_push_qs['callback'][0]).query)

            for i in ('etime', 'mtime', 'ctime'):
                assert int(callback_qs[i][0]) == file_info[i]
            assert 'request_ts' in callback_qs
            assert 'action' in callback_qs
            assert 'operation' in callback_qs
            # assert 'timestamp' in callback_qs
