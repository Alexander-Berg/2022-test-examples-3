# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import mock
import hashlib
import time
import urlparse
import re

import pytest

from mpfs.common.static import codes
from test.fixtures.users import usr_1
from test.helpers.stubs.services import SearchIndexerStub
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.conftest import INIT_USER_IN_POSTGRES


class YaFotkiClosingTestCase(CommonJsonApiTestCase):
    """Набор тестов для проверки функциональности необходимой для закрытия Яндекс.Фотки.

    Проект: https://st.yandex-team.ru/CHE-174.
    """
    mkdir_attach_endpoint = 'mkdir'
    fotki_mkfile_endpoint = 'fotki_mkfile'

    def __init__(self, *args, **kwargs):
        from mpfs.core.event_history.logger import _log_raw_event_message
        super(YaFotkiClosingTestCase, self).__init__(*args, **kwargs)
        self.event_log_patch = mock.patch(
            'mpfs.core.event_history.logger._log_raw_event_message',
            wraps=_log_raw_event_message
        )

    def test_mkdir_attach_ya_fotki(self):
        """Проверить возможность создания папки /attach/YaFotki."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        result = self.json_ok('list', {
            'uid': uid,
            'path': '/attach'
        })
        paths = [r['path'] for r in result]
        assert '/attach' in paths
        assert '/attach/YaFotki' in paths

    def test_mkdir_attach_folder_inside_ya_fotki(self):
        """Проверить возможность создания папки внутри /attach/YaFotki (т.е. /attach/YaFotki/folder)."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki/Pron'
        })
        result = self.json_ok('list', {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        paths = [r['path'] for r in result]
        assert '/attach/YaFotki' in paths
        assert '/attach/YaFotki/Pron' in paths

    def test_mkdir_attach_folder_inside_folder_inside_ya_fotki(self):
        """Проверить возможности создания папки внутри подпапки /attach/YaFotki
        (т.е. /attach/YaFotki/folder/sub_folder)"""
        uid = self.uid
        # создаем вложенные папки
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki/Disk'
        })
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki/Disk/Kek'
        })

        # получаем инфу по каждой папке
        self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/Disk'
        })
        self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/Disk/Kek'
        })

    def test_mkdir_attach_custom_folder(self):
        """Проверить невозможность создания папки в /attach отличной от YaFotki."""
        uid = self.uid
        self.json_error(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/CustomName'
        }, code=codes.MKDIR_PERMISSION_DENIED)

    def test_mkdir_attach_custom_folder_with_initialized_attach_dir(self):
        """Проверить невозможность создания папки в /attach отличной от YaFotki, если у
        пользователя уже инициализирован раздел /attach."""
        from mpfs.core.user import base as user
        uid = self.uid
        if user.NeedInit(uid, type='attach'):
            user.Create(uid, type='attach')

        uid = self.uid
        self.json_error(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/CustomName'
        }, code=codes.MKDIR_PERMISSION_DENIED)

    def test_no_messages_in_event_history_after_mkdir(self):
        """Проверить что при создании папки в разделе /attach ничего не пишется в event лог (а значит и в Ленту)."""
        uid = self.uid
        mocked_log = self.event_log_patch.start()
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        assert not mocked_log.called
        self.event_log_patch.stop()

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='No need to test mongo anymore')
    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.push_tree', return_value=None)
    def test_push_to_index_for_attach_mkdir(self, mocked_push_tree):
        """Проверить что при создании папки внутри раздела /attach данные уходят в индексер."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        assert mocked_push_tree.call_count == 2

        ids = set()
        for args, kwargs in mocked_push_tree.call_args_list:
            assert kwargs['operation'] == 'mkdir'
            assert kwargs['storage_name'] == 'attach'
            uid, data_lst, _ = args
            assert len(data_lst) == 1
            (data,) = data_lst
            ids.add(data['id'])

        assert ids == {'/attach', '/attach/YaFotki'}

    def test_no_push_to_xiva_for_attach_mkdir(self):
        """Проверить что при создании папки внутри раздела /attach данные не уходят в Xiva."""
        uid = self.uid
        mocked_xiva_helper = mock.MagicMock()

        with mock.patch('mpfs.core.filesystem.base.XivaHelper', return_value=mocked_xiva_helper):
            # сначала удостоверимся что наш мок вообще работает (создаем папку в диске)
            self.json_ok(self.mkdir_attach_endpoint, {
                'uid': uid,
                'path': '/disk/YaFotki'
            })
            assert mocked_xiva_helper.method_calls
            mocked_xiva_helper.add_to_xiva.assert_called_once()

            mocked_xiva_helper.reset_mock()
            self.json_ok(self.mkdir_attach_endpoint, {
                'uid': uid,
                'path': '/attach/YaFotki'
            })
            assert not mocked_xiva_helper.method_calls

    def create_common_fotki_mk_file_request(self, uid=None, path=None, preview_mid=None, timestamp=None):
        uid = uid or self.uid
        random_text = 'ka28Ls72mmLs72k13laQDsKmal123'
        file_mid = '100000.yadisk:138710986.398329638417174619733664706476'
        digest_mid = '100000.yadisk:138710986.398329638417174619733664706477'
        md5 = hashlib.md5(random_text).hexdigest()
        sha256 = hashlib.sha256(random_text).hexdigest()
        assert len({file_mid, digest_mid, preview_mid}) == 3
        if path is None:
            path = '/attach/test.jpg'
        now = timestamp or time.time()
        data = {
            'uid': uid,
            'md5': md5,
            'sha256': sha256,
            'size': 100500,
            'file_mid': file_mid,
            'digest_mid': digest_mid,
            'path': path,
            'mime_type': 'image/jpeg',
            'ctime': int(now),
            'mtime': int(now),
            'etime': str(int(now)) + '000',
        }
        if preview_mid:
            data['preview_mid'] = preview_mid

        return data

    def make_common_mk_file_request(self, uid=None, path=None, preview_mid=None, timestamp=None):
        """Возвращает кортеж (результат, кварги для mk_file)."""
        data = self.create_common_fotki_mk_file_request(uid=uid, path=path, preview_mid=preview_mid, timestamp=timestamp)
        result = self.json_ok(self.fotki_mkfile_endpoint, data)
        return result, data

    def test_mk_file_without_preview(self):
        """Проверить создание файла без переданного стида превью."""
        now = time.time()
        uid = self.uid
        folder_path = '/attach/YaFotki'
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': folder_path
        })
        file_path = '%s/test.jpg' % folder_path
        result, params = self.make_common_mk_file_request(uid=uid, path=file_path, timestamp=now)

        info = self.json_ok('info', {
            'uid': params['uid'],
            'path': file_path,
            'meta': ''
        })
        assert info['ctime'] == info['mtime']
        assert info['name'] == 'test.jpg'
        assert info['path'] == file_path
        assert info['type'] == 'file'
        assert 'utime' in info
        assert info['utime']

        meta = info['meta']
        assert meta['uid'] == params['uid']
        assert meta['size'] == int(params['size'])
        assert meta['file_mid'] == params['file_mid']
        assert meta['digest_mid'] == params['digest_mid']
        assert meta['media_type'] == meta['mediatype'] == 'image'
        assert meta['mimetype'] == params['mime_type']
        assert meta['md5'] == params['md5']
        assert meta['sha256'] == params['sha256']
        assert meta['etime'] == int(now)
        assert 'pmid' not in meta

    def test_mk_file_with_preview(self):
        """Проверить создание файла с переданным стидом превью."""
        preview_mid = '100000.yadisk:138710986.398329638417174619733664706478'
        uid = self.uid
        folder_path = '/attach/YaFotki'
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': folder_path
        })
        file_path = '%s/test.jpg' % folder_path
        result, params = self.make_common_mk_file_request(uid=uid, path=file_path, preview_mid=preview_mid)

        info = self.json_ok('info', {
            'uid': params['uid'],
            'path': params['path'],
            'meta': ''
        })

        meta = info['meta']
        assert meta['pmid'] == preview_mid

    def test_mk_file_return_data(self):
        """Проверить данные, которые возвращаются в ответе при создании файла."""
        uid = self.uid
        folder_path = '/attach/YaFotki'
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': folder_path
        })
        file_path = '%s/test.jpg' % folder_path
        result, params = self.make_common_mk_file_request(uid=uid, path=file_path)
        assert result == {}

    def test_mk_file_attach_ya_fotki_push_to_indexer(self):
        """Проверить что в индексер уходят данные при создании файла внутри /attach/YaFotki."""
        uid = self.uid
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.data2chunks', return_value=[]) as mocked_data2chunks:
            result, params = self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
            mocked_data2chunks.assert_called_once()
            args, kwargs = mocked_data2chunks.call_args
        (chunk,) = args
        assert len(chunk) == 1
        [data] = chunk
        assert data['mimetype'] == 'image/jpeg'
        # BUG: Почему мы шлем в индексер интовый uid?
        assert str(data['uid']) == params['uid']
        assert data['stid'] == params['file_mid']
        assert data['size'] == int(params['size'])
        assert data['mediatype'] == 'image'
        assert 'file_id' in data
        assert data['id'] == params['path']
        assert data['md5'] == params['md5']
        assert data['name'] == 'test.jpg'

    def test_no_messages_in_event_history_after_mk_file_attach_ya_fotki(self):
        """Проверить что после создания файла в /attach/YaFotki ничего не пишется в лог событий."""
        mocked_log = self.event_log_patch.start()
        uid = self.uid
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
        self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'meta': ''
        })
        assert not mocked_log.called
        self.event_log_patch.stop()

    def test_no_push_to_xiva_for_attach_mk_file_ya_fotki(self):
        """Проверить что после создания файла в /attach/YaFotki ничего не шлется в Xiva."""
        uid = self.uid
        mocked_xiva_helper = mock.MagicMock()

        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        with mock.patch('mpfs.core.filesystem.base.XivaHelper', return_value=mocked_xiva_helper):
            self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
            assert not mocked_xiva_helper.method_calls

    def test_mk_file_wrong_path(self):
        """Проверить что нельзя создать файл не в разрешенной директории (сейчас YaFotki внутри /attach)."""
        uid = self.uid
        data = self.create_common_fotki_mk_file_request(uid=uid, path='/disk/test.jpg')
        self.json_error(self.fotki_mkfile_endpoint, data, code=codes.BAD_REQUEST_ERROR)

        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/disk/YaFotki'
        })
        data = self.create_common_fotki_mk_file_request(uid=uid, path='/disk/YaFotki/test.jpg')
        self.json_error(self.fotki_mkfile_endpoint, data, code=codes.BAD_REQUEST_ERROR)

    def test_service_create_fotki_closing_compensation(self):
        """Проверить создание услуги на выдачу дополнительного места в качестве компенсации
        за закрытие сервиса Я.Фотки."""
        uid = self.uid
        result = self.billing_ok('service_create', {
            'uid': uid,
            'line': 'bonus',
            'pid': 'fotki_closing_compensation',
            'ip': '127.0.0.1',
            'product.amount': 7777777
        })
        assert 'sid' in result
        sid = result['sid']
        assert sid

        result = self.billing_ok('service_list', {
            'uid': uid,
            'ip': '127.0.0.1'
        })
        filtered = filter(lambda x: x.get('sid') == sid, result)
        assert filtered
        assert len(filtered) == 1
        [service] = filtered
        assert service['expires'] is None
        assert service['free'] is True
        assert service['name'] == 'fotki_closing_compensation'
        assert service['removes'] is None
        assert service['size'] == 7777777
        assert service['subscription'] is False
        assert service['names']['ru'] == 'Для снимков из Яндекс.Фоток'
        assert service['names']['en'] == 'For photos from Yandex.Fotki'
        assert service['names']['tr'] == "Yandex.Foto'daki fotoğraflar için"
        assert service['names']['uk'] == 'Для знімків із Яндекс.Фоток'

    def test_info_meta_contains_fotki_data_url(self):
        """Проверить, что если запросить правильную мету, то она содержит `fotki_data_stid` и
        `fotki_data_url`, если перед этим был стид записан через setprop."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
        # просто левая строка, формат не факт что такой
        ya_fotki_stid = '320.ya_fotki:122625849.E127514:188122853138022590258949570058'
        self.json_ok('setprop', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'fotki_data_stid': ya_fotki_stid
        })
        info = self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'meta': 'fotki_data_stid,fotki_data_url'
        })
        assert 'meta' in info
        assert 'fotki_data_stid' in info['meta']
        assert info['meta']['fotki_data_stid'] == ya_fotki_stid
        assert 'fotki_data_url' in info['meta']

    def test_set_fotki_tags_push_to_indexer(self):
        """Проверить что при установке свойства fotki_tags оно в правильном формате уходит в индексер."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
        tags_string = 'машина,bmw,2017 год,красотка'
        with SearchIndexerStub() as search_stub:
            self.json_ok('setprop', {
                'uid': uid,
                'path': '/attach/YaFotki/test.jpg',
                'fotki_tags': tags_string
            })

            search_stub.push_change.assert_called_once()
            args, kwargs = search_stub.push_change.call_args
            assert len(args) == 1
            [docs] = args
            assert len(docs) == 1
            doc = [docs]
            [[doc]] = doc
            assert 'action' in doc
            assert doc['action'] == 'modify'
            # не должен попасть в фотосрез
            assert 'etime' not in doc
            assert 'uid' in doc
            assert 'type' in doc
            assert 'id' in doc
            assert 'version' in doc
            assert 'stid' in doc
            assert 'mimetype' in doc
            assert 'mediatype' in doc
            assert 'fotki_tags' in doc
            assert doc['fotki_tags'] == 'машина\nbmw\n2017 год\nкрасотка'

        info = self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'meta': 'fotki_tags'
        })
        assert 'meta' in info
        assert 'fotki_tags' in info['meta']
        assert info['meta']['fotki_tags'] == tags_string

    def test_info_meta_does_not_contain_ya_fotki_dat_url_without_setprop(self):
        """Проверить, что мета не содержит `fotki_data_stid` и `fotki_data_url`, если перед этим
        фоточный стид не был записан через setprop."""
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.make_common_mk_file_request(uid=uid, path='/attach/YaFotki/test.jpg')
        # просто левая строка, формат не факт что такой
        self.json_ok('setprop', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'top': 'kek'
        })
        info = self.json_ok('info', {
            'uid': uid,
            'path': '/attach/YaFotki/test.jpg',
            'meta': 'fotki_data_stid,fotki_data_url'
        })
        assert 'meta' in info
        assert 'fotki_data_stid' not in info['meta']
        assert 'fotki_data_url' not in info['meta']

    def test_albums_create_with_items_with_fotki_album_identifier(self):
        """Проверить создание альбома через `albums_create_with_items`
        с переданным идентификатором альбома в Я.Фотки."""
        uid = self.uid

        album = self.json_ok('albums_create_with_items', {'uid': uid}, json={
            'title': 'Мальчишник в Вегасе',
            'layout': 'waterfall',
            'flags': ['show_frames', 'show_dates'],
            'items': [],
            'description': 'Описалово',
        })

        assert 'fotki_album_id' not in album

        fotki_album_identifier = '123456789'  # random int32 (специально передаем строкой в тесте)
        album = self.json_ok('albums_create_with_items', {'uid': uid}, json={
            'title': 'Мальчишник в Вегасе 2',
            'layout': 'waterfall',
            'flags': ['show_frames', 'show_dates'],
            'items': [],
            'description': 'Описалово',
            'fotki_album_id': fotki_album_identifier,
        })
        assert 'fotki_album_id' in album
        assert album['fotki_album_id'] == int(fotki_album_identifier)  # строка преобразуется в int и в базе лежит int
        album_2_id = album['id']

        album_2_info = self.json_ok('album_get', {
            'uid': uid,
            'album_id': album_2_id,
        })
        assert 'fotki_album_id' in album_2_info
        assert album_2_info['fotki_album_id'] == int(fotki_album_identifier)

    def test_fotki_album_public_url(self):
        """Проверить ручку получения публичной ссылки на основе идентификатора альбома в Фотках и uid."""
        uid = self.uid
        fotki_album_identifier = 123456  # random int32
        album = self.json_ok('albums_create_with_items', {'uid': uid}, json={
            'title': 'Мальчишник в Вегасе 2',
            'layout': 'waterfall',
            'flags': ['show_frames', 'show_dates'],
            'items': [],
            'description': 'Описалово',
            'fotki_album_id': fotki_album_identifier,
        })
        for req_uid in (uid, usr_1.uid, '0'):
            result = self.json_ok('fotki_album_public_url', {
                'uid': req_uid,
                'owner_uid': uid,
                'fotki_album_id': fotki_album_identifier,
            })
            assert 'url' in result
            url = result['url']
            parsed_url = urlparse.urlparse(url)
            assert re.match(r'/a/[\w]+', parsed_url.path)
            assert parsed_url.netloc == 'dummy.ya.net'

        self.json_ok('album_unpublish', {'uid': uid, 'album_id': album['id']})
        self.json_error('fotki_album_public_url', {
            'uid': '0',
            'owner_uid': uid,
            'fotki_album_id': fotki_album_identifier,
        }, code=codes.ALBUMS_IS_NOT_PUBLIC)


    def test_fotki_album_public_url_with_wrong_fotki_album_id(self):
        uid = self.uid
        fotki_album_identifier = '123456789'  # несуществующий рандомный идентификатор

        self.json_error('fotki_album_public_url', {
            'uid': uid,
            'owner_uid': uid,
            'fotki_album_id': fotki_album_identifier,
        }, code=codes.ALBUM_NOT_FOUND)

    def test_migrate_one_album_scenario(self):
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        album_path = '/attach/YaFotki/Мальчишник в Вегасе'
        photo_1_path = album_path + '/test1.jpg'
        photo_2_path = album_path + '/test2.jpg'
        photo_3_path = album_path + '/test3.jpg'
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': album_path
        })

        self.make_common_mk_file_request(uid=uid, path=photo_1_path)
        self.make_common_mk_file_request(uid=uid, path=photo_2_path)
        self.make_common_mk_file_request(uid=uid, path=photo_3_path)

        ya_fotki_stid_1 = '320.ya_fotki:122625849.E127514:188122853138022590258949570058'
        ya_fotki_stid_2 = '320.ya_fotki:122625849.E127514:188122853138022590258949570059'
        ya_fotki_stid_3 = '320.ya_fotki:122625849.E127514:188122853138022590258949570050'

        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_1_path,
            'fotki_data_stid': ya_fotki_stid_1
        })
        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_2_path,
            'fotki_data_stid': ya_fotki_stid_2
        })
        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_3_path,
            'fotki_data_stid': ya_fotki_stid_3
        })

        photo_1_info = self.json_ok('info', {
            'uid': uid,
            'path': photo_1_path,
            'meta': 'fotki_data_stid,fotki_data_url,file_id'
        })

        photo_2_info = self.json_ok('info', {
            'uid': uid,
            'path': photo_2_path,
            'meta': 'fotki_data_stid,fotki_data_url,file_id'
        })

        photo_3_info = self.json_ok('info', {
            'uid': uid,
            'path': photo_3_path,
            'meta': 'fotki_data_stid,fotki_data_url,file_id'
        })

        assert photo_1_info['meta']['fotki_data_stid'] == ya_fotki_stid_1
        assert 'fotki_data_url' in photo_1_info['meta']
        assert photo_2_info['meta']['fotki_data_stid'] == ya_fotki_stid_2
        assert 'fotki_data_url' in photo_2_info['meta']
        assert photo_3_info['meta']['fotki_data_stid'] == ya_fotki_stid_3
        assert 'fotki_data_url' in photo_3_info['meta']

        file_ids = [photo_1_info['meta']['file_id'], photo_2_info['meta']['file_id'], photo_3_info['meta']['file_id']]

        # на этом этапе мы создали 1 альбом (ресурсы) с 3 фотками
        # создаем альбом

        fotki_album_identifier = '123456'
        album = self.json_ok('albums_create_with_items', {'uid': uid}, json={
            'title': 'Мальчишник в Вегасе',
            'layout': 'waterfall',
            'flags': ['show_frames', 'show_dates'],
            'items': [
                {'type': 'resource', 'path': photo_1_path},
                {'type': 'resource', 'path': photo_2_path},
                {'type': 'resource', 'path': photo_3_path},
            ],
            'description': 'Описалово',
            'fotki_album_id': fotki_album_identifier,
        })
        album_id = album['id']
        assert len(album['items']) == len((photo_1_path, photo_2_path, photo_3_path))
        items_obj_ids = [i['obj_id'] for i in album['items']]
        assert sorted(file_ids) == sorted(items_obj_ids)

        album_info = self.json_ok('album_get', {
            'uid': uid,
            'album_id': album_id,
        })
        assert 'items' in album_info
        assert len(album_info['items']) == len((photo_1_path, photo_2_path, photo_3_path))

        result = self.json_ok('fotki_album_public_url', {
            'uid': uid,
            'owner_uid': uid,
            'fotki_album_id': fotki_album_identifier,
        })
        assert 'url' in result
        url = result['url']
        parsed_url = urlparse.urlparse(url)
        assert re.match(r'/a/[\w]+', parsed_url.path)
        assert parsed_url.netloc == 'dummy.ya.net'

    def test_fotki_album_item_public_url(self):
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        album_path = '/attach/YaFotki/Мальчишник в Вегасе'
        photo_1_path = album_path + '/test1.jpg'
        photo_2_path = album_path + '/test2.jpg'
        photo_3_path = album_path + '/test3.jpg'
        self.json_ok(self.mkdir_attach_endpoint, {'uid': uid, 'path': album_path})

        self.make_common_mk_file_request(uid=uid, path=photo_1_path)
        self.make_common_mk_file_request(uid=uid, path=photo_2_path)
        self.make_common_mk_file_request(uid=uid, path=photo_3_path)

        ya_fotki_stid_1 = '320.ya_fotki:122625849.E127514:188122853138022590258949570058'
        ya_fotki_stid_2 = '320.ya_fotki:122625849.E127514:188122853138022590258949570059'
        ya_fotki_stid_3 = '320.ya_fotki:122625849.E127514:188122853138022590258949570050'

        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_1_path,
            'fotki_data_stid': ya_fotki_stid_1
        })
        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_2_path,
            'fotki_data_stid': ya_fotki_stid_2
        })
        self.json_ok('setprop', {
            'uid': uid,
            'path': photo_3_path,
            'fotki_data_stid': ya_fotki_stid_3
        })
        # на этом этапе мы создали 1 альбом (ресурсы) с 3 фотками
        # создаем альбом

        fotki_album_identifier = '123456'
        album = self.json_ok('albums_create_with_items', {'uid': uid}, json={
            'title': 'Мальчишник в Вегасе',
            'layout': 'waterfall',
            'flags': ['show_frames', 'show_dates'],
            'items': [
                {'type': 'resource', 'path': photo_1_path},
                {'type': 'resource', 'path': photo_2_path},
                {'type': 'resource', 'path': photo_3_path},
            ],
            'description': 'Описалово',
            'fotki_album_id': fotki_album_identifier,
        })

        # owner или посторонний могут получить ссылку на фото из публичного альбома
        for req_uid in (uid, usr_1.uid, "0"):
            for photo_path in (photo_1_path, photo_2_path, photo_3_path):
                result = self.json_ok('fotki_album_item_public_url', {
                    'uid': req_uid,
                    'owner_uid': uid,
                    'path': photo_path,
                    'fotki_album_id': fotki_album_identifier,
                })
                assert 'url' in result
                url = result['url']
                assert album['public']['short_url'] in url

        self.json_ok('album_unpublish', {'uid': uid, 'album_id': album['id']})

        # посторонний не может получить ссылку на фото из приватного альбома
        for req_uid in (usr_1.uid, "0"):
            self.json_error('fotki_album_item_public_url', {
                'uid': req_uid,
                'owner_uid': uid,
                'path': photo_1_path,
                'fotki_album_id': fotki_album_identifier,
            }, code=codes.ALBUMS_IS_NOT_PUBLIC)

        # а owner по-прежнему может
        result = self.json_ok('fotki_album_item_public_url', {
            'uid': uid,
            'owner_uid': uid,
            'path': photo_1_path,
            'fotki_album_id': fotki_album_identifier,
        })
        assert 'url' in result
        url = result['url']
        assert album['public']['short_url'] in url

    def test_move_file_from_attach_removes_fotki_proxy_url_from_proxy(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki'})
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/1.jpg')
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/1.jpg', 'fotki_proxy_url': '/proxy/1'})
        with mock.patch('mpfs.core.services.fotki_proxy_service.fotki_proxy_service.delete_access') as m:
            self.json_ok('async_move', {'uid': self.uid, 'src': '/attach/YaFotki/1.jpg', 'dst': '/disk/1.jpg'})
            assert m.call_count == 1
            assert m.call_args_list[0][0][0] == '/proxy/1'
        disk_listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk'})
        assert len([x for x in disk_listing if x['type'] == 'file']) == 1
        assert [x['path'] for x in disk_listing if x['type'] == 'file'][0] == '/disk/1.jpg'

    def test_move_folder_from_attach_removes_fotki_proxy_url_from_proxy(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki/f'})
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/f/1.jpg')
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/f/2.jpg')
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/f/1.jpg', 'fotki_proxy_url': '/proxy/f/1'})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/f/2.jpg', 'fotki_proxy_url': '/proxy/f/2'})
        with mock.patch('mpfs.core.services.fotki_proxy_service.fotki_proxy_service.delete_access') as m:
            self.json_ok('async_move', {'uid': self.uid, 'src': '/attach/YaFotki/f', 'dst': '/disk/f'})
            assert m.call_count == 2
            assert {m.call_args_list[0][0][0], m.call_args_list[1][0][0]} == {'/proxy/f/1', '/proxy/f/2'}
        disk_listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk/f'})
        assert len([x for x in disk_listing if x['type'] == 'file']) == 2
        assert {x['path'] for x in disk_listing if x['type'] == 'file'} == {'/disk/f/1.jpg', '/disk/f/2.jpg'}

    def test_trash_append_file_from_attach_removes_fotki_proxy_url_from_proxy(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki'})
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/1.jpg')
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/1.jpg', 'fotki_proxy_url': '/proxy/1'})
        with mock.patch('mpfs.core.services.fotki_proxy_service.fotki_proxy_service.delete_access') as m:
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/attach/YaFotki/1.jpg'})
            assert m.call_count == 1
            assert m.call_args_list[0][0][0] == '/proxy/1'
        trash_listing = self.json_ok('list', {'uid': self.uid, 'path': '/trash'})
        assert len([x for x in trash_listing if x['type'] == 'file']) == 1
        assert [x['path'] for x in trash_listing if x['type'] == 'file'][0] == '/trash/1.jpg'

    def test_copy_folder_attach_does_not_remove_fotki_proxy_url_from_proxy(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki/f'})
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/f/1.jpg')
        self.make_common_mk_file_request(uid=self.uid, path='/attach/YaFotki/f/2.jpg')
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/f/1.jpg', 'fotki_proxy_url': '/proxy/f/1'})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/attach/YaFotki/f/2.jpg', 'fotki_proxy_url': '/proxy/f/2'})
        with mock.patch('mpfs.core.services.fotki_proxy_service.fotki_proxy_service.delete_access') as m:
            self.json_ok('async_copy', {'uid': self.uid, 'src': '/attach/YaFotki/f', 'dst': '/disk/f'})
            assert m.call_count == 0
        disk_listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk/f'})
        assert len([x for x in disk_listing if x['type'] == 'file']) == 2
        assert {x['path'] for x in disk_listing if x['type'] == 'file'} == {'/disk/f/1.jpg', '/disk/f/2.jpg'}

    def test_attach_folder_publication(self):
        uid = self.uid
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki/public'
        })
        # Папка непубличная
        assert None == self.json_ok('list', {
            'uid': uid,
            'path': '/attach/YaFotki/public',
            'meta': 'public'})[0]['meta'].get('public')

        self.json_ok('set_public', {
            'uid': uid,
            'path': '/attach/YaFotki/public'
        })
        # Стала публичной
        assert 1 == self.json_ok('list', {
            'uid': uid,
            'path': '/attach/YaFotki/public',
            'meta': 'public'})[0]['meta'].get('public')

        self.json_ok('set_private', {
            'uid': uid,
            'path': '/attach/YaFotki/public'
        })
        # Снова непубличная
        assert None == self.json_ok('list', {
            'uid': uid,
            'path': '/attach/YaFotki/public',
            'meta': 'public'})[0]['meta'].get('public')

    def test_attach_file_publication(self):
        uid = self.uid
        fpath = '/attach/YaFotki/test.jpg'
        self.json_ok(self.mkdir_attach_endpoint, {
            'uid': uid,
            'path': '/attach/YaFotki'
        })
        data = self.create_common_fotki_mk_file_request(uid=uid, path=fpath)
        self.json_ok(self.fotki_mkfile_endpoint, data)

        self.json_ok('set_public', {
            'uid': uid,
            'path': fpath
        })

        assert 1 == self.json_ok('list', {
            'uid': uid,
            'path': fpath,
            'meta': 'public'
        })['meta'].get('public')

        self.json_ok('set_private', {
            'uid': uid,
            'path': fpath
        })

        assert None == self.json_ok('list', {
            'uid': uid,
            'path': fpath,
            'meta': 'public'
        })['meta'].get('public')

