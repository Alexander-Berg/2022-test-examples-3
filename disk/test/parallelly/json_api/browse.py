# -*- coding: utf-8 -*-
import mock
import pytest
import urlparse

from nose_parameterized import parameterized

from mpfs.core.address import ResourceId
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.base import parse_open_url_call
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES

import mpfs.engine.process

from mpfs.common.errors import ResourceNotFound, UrlPathError, UrlNotFound
from mpfs.common.util import to_json
from mpfs.config import settings
from mpfs.core import factory
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.quota import Quota
from mpfs.core.services.search_service import SearchDB
from mpfs.metastorage.mongo.collections.filesystem import UserDataCollection
from mpfs.metastorage.mongo.util import decompress_data, compress_data, parent_for_key

USE_DYNAMIC_PREVIEWS = settings.feature_toggles['dynamicpreviews']
db = CollectionRoutedDatabase()
SUPPORTED_PREVIEW_TYPES = settings.system['preview_types']


class BrowseJsonApiShareTestCase(CommonSharingMethods):
    def test_dir_size_for_subdir_in_shared_dir(self):
        owner_folder = '/disk/Shared'
        invited_folder = '/disk/MyShared'
        invited_subdir_in_shared_dir = '/disk/MyShared/new'

        self.json_ok('mkdir', {'uid': self.uid, 'path': owner_folder})
        self.create_share_for_guest(self.uid, owner_folder, self.uid_3, self.email_3)
        self.json_ok('move', {'uid': self.uid_3, 'src': owner_folder, 'dst': invited_folder})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': invited_subdir_in_shared_dir})
        info_result = self.json_ok(
            'info', {'uid': self.uid_3, 'path': invited_subdir_in_shared_dir, 'meta': 'resource_id'}
        )

        with mock.patch.object(SearchDB, 'folder_size') as mock_obj:
            search_ret = '{"/disk/":{"size":1291,"count":1}}'
            mock_obj.return_value = search_ret
            self.json_ok('dir_size', {'uid': self.uid_3, 'path': invited_subdir_in_shared_dir})
            mock_obj.assert_called_with(
                self.uid_3,
                invited_subdir_in_shared_dir,
                ResourceId.parse(info_result['meta']['resource_id'])
            )


class BrowseJsonApiTestCase(CommonJsonApiTestCase):
    def test_resource_id_in_meta(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/1.txt')
        folder_file_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'file_id'})['meta']['file_id']
        folder_raw_resource_id = "%s:%s" % (self.uid, folder_file_id)
        file_file_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/1.txt', 'meta': 'file_id'})['meta']['file_id']
        file_raw_resource_id = "%s:%s" % (self.uid, file_file_id)

        # info
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'resource_id'})
        assert resp['meta']['resource_id'] == folder_raw_resource_id
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/1.txt', 'meta': 'resource_id'})
        assert resp['meta']['resource_id'] == file_raw_resource_id
        # list
        resp = self.json_ok('list', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'resource_id'})
        assert resp[0]['meta']['resource_id'] == folder_raw_resource_id
        assert resp[1]['meta']['resource_id'] == file_raw_resource_id
        # bulk_info_by_resource_ids
        body = [folder_raw_resource_id, file_raw_resource_id]
        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': 'resource_id'}, json=body)
        assert resp[0]['meta']['resource_id'] == folder_raw_resource_id
        assert resp[1]['meta']['resource_id'] == file_raw_resource_id

    def test_list_amount_offset_and_order_by_name(self):
        folder_path = '/disk/dir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        files_count = 20
        file_names = []
        for i in xrange(files_count):
            file_name = 'file-%i.txt' % i
            self.upload_file(self.uid, folder_path + '/' + file_name)
            file_names.append(file_name)

        has_items = True
        offset = 0
        bulk_size = 6
        all_items = []
        while has_items:
            result = self.json_ok('list', {
                'uid': self.uid, 'path': folder_path,
                'order': '0', 'amount': bulk_size, 'offset': offset
            })
            items = result[1:]
            all_items.extend(items)
            offset += bulk_size
            has_items = len(items) == bulk_size

            assert offset < files_count + bulk_size

        assert len(all_items) == files_count

        file_names = sorted(file_names, reverse=True)
        assert [i['name'] for i in all_items] == file_names

    def test_etime_field_in_response(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/1.txt', file_data={'etime': None})
        self.upload_file(self.uid, '/disk/dir/2.txt')
        resp = self.json_ok('list', {'uid': self.uid, 'path': '/disk/dir', 'meta': 'etime'})
        assert resp[1]['name'] == '1.txt'
        # почему то etime и в теле, и в meta ресурса
        assert 'etime' not in resp[1]
        assert 'etime' not in resp[1]['meta']
        assert resp[2]['name'] == '2.txt'
        assert isinstance(resp[2]['etime'], (int, long))
        assert isinstance(resp[2]['meta']['etime'], (int, long))

    def test_etime_sorting_for_files_without_etime(self):
        # тест кейс проверяет, что если в папке запрашиваем отсортированные в обратном порядке по etime файлы
        # возвращаются так: сначала идут файлы с etime, потом - без
        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        files_with_etime_count = 5
        for i in xrange(files_with_etime_count):
            self.upload_file(self.uid, folder_path + '/file-%d.jpg' % i)
        file_without_etime_path = folder_path + '/file-without-etime.jpg'
        self.upload_file(self.uid, file_without_etime_path)

        db = CollectionRoutedDatabase()
        item = db.user_data.find_one({'uid': self.uid, 'key': file_without_etime_path})
        item['data'].pop('etime')
        item['parent'] = parent_for_key(item['key'])
        db.user_data.update({'path': item['key'], 'uid': item['uid']}, item, upsert=True)

        result = self.json_ok('list', {
            'uid': self.uid, 'path': folder_path, 'meta': 'etime',
            'offset': '0', 'amount': '40',
            'sort': 'etime', 'order': '0'
        })
        listing_files = result[1:]
        assert len(listing_files) == files_with_etime_count + 1
        assert listing_files[-1]['path'] == file_without_etime_path

        result = self.json_ok('list', {
            'uid': self.uid, 'path': folder_path, 'meta': 'etime',
            'offset': '2', 'amount': '40',
            'sort': 'etime', 'order': '0'
        })
        listing_files = result[1:]
        assert len(listing_files) == files_with_etime_count + 1 - 2
        assert listing_files[-1]['path'] == file_without_etime_path

        result = self.json_ok('list', {
            'uid': self.uid, 'path': folder_path, 'meta': 'etime',
            'offset': '0', 'amount': '40',
            'sort': 'etime', 'order': '1'
        })
        listing_files = result[1:]
        assert len(listing_files) == files_with_etime_count + 1
        assert listing_files[0]['path'] == file_without_etime_path

        result = self.json_ok('list', {
            'uid': self.uid, 'path': folder_path, 'meta': 'etime',
            'offset': '2', 'amount': '40',
            'sort': 'etime', 'order': '1'
        })
        listing_files = result[1:]
        assert len(listing_files) == files_with_etime_count + 1 - 2
        assert listing_files[0]['path'] != file_without_etime_path

        result = self.json_ok('list', {
            'uid': self.uid, 'path': folder_path, 'meta': 'etime',
            'offset': '2', 'amount': '40'
        })
        listing_files = result[1:]
        assert len(listing_files) == files_with_etime_count + 1 - 2

    def test_preview_quality(self):
        def resource_preview_value(resource):
            for preview_field in ('custom_preview', 'preview', 'thumbnail'):
                if preview_field in resource['meta']:
                    yield resource['meta'][preview_field]

            if 'sizes' in resource['meta']:
                for size in resource['meta']['sizes']:
                    yield size['url']

        self.upload_file(self.uid, '/disk/1.jpg')
        dummy = '100'

        # если preview_quality не передан, то нет quality и в урле
        result = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': ''})
        for url in resource_preview_value(result):
            assert '&quality=%s' % dummy not in url

        # для файла
        for method in ('list', 'info'):
            result = self.json_ok(method, {'uid': self.uid, 'path': '/disk/1.jpg', 'preview_quality': dummy, 'meta': ''})
            for url in resource_preview_value(result):
                assert '&quality=%s' % dummy in url

        # для папки
        for method in ('list', 'timeline'):
            result = self.json_ok(method, {'uid': self.uid, 'path': '/disk', 'preview_quality': dummy, 'meta': ''})
            for resource in result:
                if resource['type'] != 'file':
                    continue
                for url in resource_preview_value(resource):
                    assert '&quality=%s' % dummy in url

        # публичные файлы
        hsh = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.jpg'})['hash']
        for method in ('public_list', 'public_info'):
            result = self.json_ok(method, {'uid': self.uid, 'private_hash': hsh, 'preview_quality': dummy, 'meta': ''})
            if 'resource' in result:
                result = result['resource']
            for url in resource_preview_value(result):
                assert '&quality=%s' % dummy in url

    def test_print_root(self):
        disk_info_root = db.disk_info.find_one({'uid': str(self.uid), 'key': '/'})
        self.assertNotEqual(disk_info_root, None)
        self.assertEqual(disk_info_root['data'], {})
        from mpfs.core.metastorage.control import disk_info as control

        result = control.value(str(self.uid), '/')
        self.assertNotEqual(result.value, None)

    def test_list_folder_wo_meta_w_amount(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})

        opts = {
            'uid': self.uid,
            'path': '/disk',
            'amount': 1,
            'offset': 0,
        }
        list_result = self.json_ok('list', opts)

        self.assertEqual(len([x for x in list_result if x.get('path') != '/disk']), 1)

        opts = {
            'uid': self.uid,
            'path': '/disk',
        }
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test_folder'})
        list_result = self.json_ok('list', opts)
        self.assertEqual(len([x for x in list_result if x.get('path') != '/disk']), 2)

    def test_list_folder_w_meta(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file'})
        self.upload_file(self.uid, '/disk/folder_with_file/file')

        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file',
            #'meta': '',
            'meta': 'mimetype,drweb,sha256,group,custom_preview,resource_id,short_url,custom_properties,file_url,numchildren,etime,public_hash,comment_ids,autouploaded,media_type,revision,md5,size'
        }

        list_result = self.json_ok('list', opts)
        self.assertEqual(len(list_result), 2)
        self.assertEqual(list_result[0]['id'], '/disk/folder_with_file/')
        self.assertNotEqual(list_result[1]['meta']['sha256'], None)

    def test_info_file_w_meta(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file'})
        self.upload_file(self.uid, '/disk/folder_with_file/file.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': '',
        }
        result = self.json_ok('info', opts)
        self.assertEqual(result['meta']['mediatype'], 'document')

    def test_info_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file'})
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file',
            'meta': '',
        }
        result = self.json_ok('info', opts)
        self.assertTrue(result['id'].endswith('/'))

    def test_unit_separators_name_CHEMODAN_16185(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-16185
        """
        opts = {
            'uid': self.uid,
            'path': u'/disk/' + unichr(31) + u'name' + unichr(21)
        }
        self.json_ok('mkdir', opts)

        result = self.json_ok('info', opts)
        self.assertEqual(result['name'], u' name ')
        self.json_ok('rm', opts)

    def test_etag_param_in_zaberun_url(self):
        """
        Тестируется наличие параметра etag в урлах на заберун для файлов

        https://st.yandex-team.ru/CHEMODAN-22562
        """
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)
        result = self.json_ok('url', {'uid': self.uid, 'path': file_path})
        assert 'etag=' not in result['digest']
        assert 'etag=' in result['file']
        result = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'preview,file_url'})
        assert 'etag=' in result['meta']['file_url']
        assert 'etag=' not in result['meta']['preview']

    @pytest.mark.skip(reason='upload to /lnarod forbidden')
    def test_wrong_url(self):
        lnarod_file_path = '/lnarod/1.txt'
        self.json_error('url', {'uid': self.uid, 'path': lnarod_file_path}, code=UrlPathError.code)

        unexist_file_path = '/disk/fake_file.txt'
        self.json_error('url', {'uid': self.uid, 'path': unexist_file_path}, code=UrlNotFound.code)

    def test_custom_previews(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_with_file/'})
        self.upload_file(self.uid, '/disk/folder_with_file/file.txt')

        def check_preview_url(data, use_custom=True, preview_type=None, animate=False):
            if USE_DYNAMIC_PREVIEWS and use_custom:
                url = data['meta']['sizes'][-1]['url']
                self.assertTrue('size=200x200' in url)
                self.assertTrue('crop=1' in url)
                if preview_type is not None:
                    self.assertIn('preview_type=%s' % preview_type, url)
                if animate:
                    self.assertIn('animate=true', url)
                else:
                    self.assertNotIn('animate=', url)
            else:
                for item in data['meta']['sizes']:
                    self.assertTrue(item['name'] in (
                        'ORIGINAL', 'DEFAULT',
                        'XXXS', 'XXS', 'XS',
                        'S', 'M', 'L', 'XL',
                        'XXL', 'XXXL'
                    ))

        # list с кастомным
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
        }
        result = self.json_ok('list', opts)[1]
        check_preview_url(result)

        # list без кастома
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/',
            'meta': 'sizes',
        }
        result = self.json_ok('list', opts)[1]
        check_preview_url(result, use_custom=False)

        # info с кастомным
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result)
        for preview_type in SUPPORTED_PREVIEW_TYPES:
            # info с кастомным type
            opts = {
                'uid': self.uid,
                'path': '/disk/folder_with_file/file.txt',
                'meta': 'sizes',
                'preview_size': '200x200',
                'preview_crop': '1',
                'preview_type': preview_type,
            }
            result = self.json_ok('info', opts)
            check_preview_url(result, preview_type=preview_type)

        # info с кастомным неправильным type
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
            'preview_type': 'doc',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result)

        # info с кастомным animate
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
            'preview_animate': '1',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result, animate=True)

        # info с кастомным animate=0
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
            'preview_animate': '0',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result)

        # info с кастомным animate и type
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
            'preview_type': 'gif',
            'preview_animate': '1',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result, preview_type='gif', animate=True)

        # info без кастома
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
            'meta': 'sizes',
        }
        result = self.json_ok('info', opts)
        check_preview_url(result, use_custom=False)

        # fulltree с кастомным
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/',
            'meta': 'sizes',
            'preview_size': '200x200',
            'preview_crop': '1',
        }
        result = self.json_ok('fulltree', opts)['list'][0]['this']
        check_preview_url(result)

        # fulltree без кастома
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/',
            'meta': 'sizes',
        }
        result = self.json_ok('fulltree', opts)['list'][0]['this']
        check_preview_url(result, use_custom=False)

    @parameterized.expand([
        ('image/jpg', True),
        ('image/gif', True),
        ('image/svg+xml', False),
    ])
    def test_inline_only_allowed_mimetypes(self, mimetype, inline_allowed):
        self.upload_file(self.uid, '/disk/i.jpg', file_data={'mimetype': mimetype})
        info = self.json_ok('info', {
            'uid': self.uid,
            'path': '/disk/i.jpg',
            'meta': '',
        })
        original_sizes = filter(lambda s: s['name'] == 'ORIGINAL', info['meta']['sizes'])
        assert len(original_sizes) == 1
        original_url = original_sizes[0]['url']
        parsed_url = urlparse.urlparse(original_url)
        qs_params = urlparse.parse_qs(parsed_url.query, keep_blank_values=True)
        assert len(qs_params['disposition']) == 1
        disposition = qs_params['disposition'][0]
        assert (disposition == 'inline') == inline_allowed

    def test_check_content_type_encoded(self):
        path = '/disk/science_fiction.epub'
        file_data = {
            'mimetype': 'application/epub+zip',
        }
        self.upload_file(self.uid, path, file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': path,
            'meta': '',
        }
        file_info = self.json_ok('info', opts)
        self.assertIn('content_type=application%2Fepub%2Bzip', file_info['meta']['file_url'])

    def test_sort_name(self):
        opts = {
            'uid': self.uid,
            'path': '/disk/sorted_folder',
        }
        self.json_ok('mkdir', opts)
        for i in (1, 2, 11):
            opts = {
                'uid': self.uid,
                'path': '/disk/sorted_folder/folder %s' % i,
            }
            self.json_ok('mkdir', opts)
        for i in (1, 2, 11):
            self.upload_file(self.uid, '/disk/sorted_folder/file %s' % i, )
        opts = {
            'uid': self.uid,
            'path': '/disk/sorted_folder',
        }
        result = self.json_ok('list', opts)
        for i, j in enumerate((1, 11, 2)):
            self.assertEqual(result[i + 1]['name'], 'folder %s' % j)
        for i, j in enumerate((1, 11, 2)):
            self.assertEqual(result[i + 4]['name'], 'file %s' % j)

    def test_media_type_filter_list(self):
        """
        Список значений в фильтре по media_type
        """
        self.upload_file(self.uid, '/disk/file.jpg')
        self.upload_file(self.uid, '/disk/file.mp3')

        filelist = self.json_ok(
            'list',
            {
                'uid': self.uid,
                'path': '/disk',
                'meta': '',
                'media_type': 'audio,image',
            }
        )

        assert 'audio' in [x['meta'].get('media_type') for x in filelist]
        assert 'image' in [x['meta'].get('media_type') for x in filelist]

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_info_with_unzip_file_id(self):
        """Проверить что в ручке info с флагом unzip_file_id
        происходит пересохранение файла с раззипованным file_id
        """
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)

        collection = UserDataCollection()
        collection = collection.db[collection.name]

        # Лучше способ не придумал как создать файл,
        # у которого file_id в meta
        doc = collection.find_one({'uid': self.uid, 'key': file_path})

        data = decompress_data(doc.pop('zdata'))
        data['meta']['file_id'] = doc['data'].pop('file_id')
        doc['zdata'] = compress_data(data)

        collection.update({'_id': doc['_id'], 'uid': self.uid}, doc, upsert=True)

        resource = factory.get_resource(self.uid, file_path)
        assert resource.is_file_id_zipped()

        self.json_ok('info', {'uid': self.uid, 'path': file_path, 'unzip_file_id': 1})

        resource = factory.get_resource(self.uid, file_path)
        assert not resource.is_file_id_zipped()

    def test_dir_size_root(self):
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
            if reindex_result['result']['is_reindexed']:
                info_result = self.json_ok('info', {'uid': self.uid, 'path': '/disk', 'meta': 'resource_id'})
                search_ret = '{"%s":{"size":1291,"count":1}}' % info_result['meta']['resource_id']
            else:
                search_ret = '{"/disk/":{"size":1291,"count":1}}'

            mock_obj.return_value = search_ret
            result = self.json_ok('dir_size', {'uid': self.uid, 'path': '/disk'})
            assert result == {
                "path": "/disk",
                "size": 1291,
                "files_count": 1
            }

    def test_dir_size_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
            if reindex_result['result']['is_reindexed']:
                info_result = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': 'resource_id'})
                search_ret = '{"%s":{"size":1291,"count":1}}' % info_result['meta']['resource_id']
            else:
                search_ret = '{"/disk/folder/":{"size":1291,"count":1}}'

            mock_obj.return_value = search_ret
            result = self.json_ok('dir_size', {'uid': self.uid, 'path': '/disk/folder'})
            assert result == {
                "path": "/disk/folder",
                "size": 1291,
                "files_count": 1
            }

    def test_dir_size_lnarod_path(self):
        self.json_error('dir_size', {'uid': self.uid, 'path': '/lnarod/folder'}, code=UrlPathError.code)

    def test_search_sizes_per_mediatype(self):
        raw_response = open('fixtures/json/search_sizes_per_mediatype_response.json').read()
        with mock.patch.object(SearchDB, 'open_url', return_value=raw_response):
            result = self.json_ok('search_sizes_per_mediatype', {'uid': self.uid})
            assert result == {u'items': [{u'mediatype': u'image', u'size': 16847520012},
                                         {u'mediatype': u'video', u'size': 13986994946},
                                         {u'mediatype': u'compressed', u'size': 351428636},
                                         {u'mediatype': u'data', u'size': 142305744},
                                         {u'mediatype': u'audio', u'size': 43386775},
                                         {u'mediatype': u'document', u'size': 30605837},
                                         {u'mediatype': u'backup', u'size': 28501},
                                         {u'mediatype': u'spreadsheet', u'size': 7396},
                                         {u'mediatype': u'development', u'size': 5783}], u'total': 9}

    def test_public_dir_size(self):
        path = '/disk/upload'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        response = self.json_ok('set_public', {'uid': self.uid, 'path': path})
        private_hash = response['hash']
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
            if reindex_result['result']['is_reindexed']:
                info_result = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'resource_id'})
                mock_obj.return_value = '{"%s": {"size":100500,"count":300}}' % info_result['meta']['resource_id']
            else:
                mock_obj.return_value = '{"/disk/upload/": {"size":100500,"count":300}}'

            result = self.json_ok(
                'public_dir_size',
                {
                    'uid': self.uid,
                    'private_hash': private_hash
                }
            )
            assert result == {
                'path': path,
                'size': 100500,
                'files_count': 300
            }

    def test_list_subresources(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.upload_file(self.uid, '/disk/test/1.jpg')
        self.upload_file(self.uid, '/disk/test/1.txt')

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value='{"hitsArray":[{"key":"/disk/test/1.jpg","type":"file","ctime":"1446749715","name":"1.jpg"},{"key":"/disk/test/1.txt","type":"file","ctime":"1446749715","name":"1.txt"}],"hitsCount":2}'
            result = self.json_ok('list_subresources', {'uid': self.uid, 'path': '/disk/test'})
            assert 'total' in result
            assert 'items' in result
            url_info = parse_open_url_call(mock_obj)
            assert url_info['page'] == '/listing'
            assert url_info['params']['sort'][0] == 'name'
            assert url_info['params']['key'][0] == '/disk/test/'

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value='{"hitsArray":[{"key":"/disk/test/1.jpg","type":"file","ctime":"1446749715","name":"1.jpg"},{"key":"/disk/test/1.txt","type":"file","ctime":"1446749715","name":"1.txt"}],"hitsCount":2}'
            result = self.json_ok('list_subresources', {'uid': self.uid, 'path': '/disk/test', 'extended_response': 1, 'meta': 'mediatype,mimetype'})
            assert 'total' in result
            assert 'items' in result
            assert 'meta' in result['items'][0]
            assert 'mediatype' in result['items'][0]['meta']
            url_info = parse_open_url_call(mock_obj)
            assert url_info['page'] == '/listing'
            assert url_info['params']['sort'][0] == 'name'
            assert url_info['params']['key'][0] == '/disk/test/'

    def test_search_bulk_info(self):
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value='{"hitsArray":[{"key":"/disk/test/1.jpg","type":"file","ctime":"1446749715","name":"1.jpg"},{"key":"/disk/test/1.txt","type":"file","ctime":"1446749715","name":"1.txt"}],"hitsCount":2}'
            req_params = {
                'uid': self.uid,
                'file_ids': '1,2,3',
                'search_meta': 'path,file_id,width,size',
                'sort': 'size',
                'order': 0
            }
            result = self.json_ok('search_bulk_info', req_params)
            assert 'total' in result
            assert 'items' in result
            url_info = parse_open_url_call(mock_obj)
            assert url_info['page'] == '/get'
            assert len(url_info['params']['id']) == 3
            assert url_info['params']['uid'][0] == self.uid
            assert 'asc' not in url_info['params']
            assert url_info['params']['get'][0] == 'key,id,width,size'

            req_params['order'] = 1
            self.json_ok('search_bulk_info', req_params)
            url_info = parse_open_url_call(mock_obj)
            assert len(url_info['params']['asc']) == 1

    def test_search_bulk_info_empty_file_id(self):
        with mock.patch.object(SearchDB, 'resources_info_by_file_ids') as mock_obj:
            req_params = {
                'uid': self.uid,
                'file_ids': '1,2,3,,',
                'search_meta': 'path,file_id,width,size',
                'sort': 'size',
                'order': 0
            }
            self.json_ok('search_bulk_info', req_params)
            assert '' not in mock_obj.call_args[0][1]

            req_params['file_ids'] = ''
            result = self.json_ok('search_bulk_info', req_params)
            assert 'total' in result
            assert 'items' in result
            assert result['total'] == 0
            assert result['items'] == []


    def test_public_search_bulk_info(self):
        path = '/disk/test'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        response = self.json_ok('set_public', {'uid': self.uid, 'path': path})
        private_hash = response['hash']
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value='{"hitsArray":[{"key":"/disk/test/1.jpg","type":"file","ctime":"1446749715","name":"1.jpg"},{"key":"/disk/test/1.txt","type":"file","ctime":"1446749715","name":"1.txt"}],"hitsCount":2}'
            req_params = {
                'private_hash': private_hash,
                'file_ids': '1,2,3',
                'search_meta': 'path,file_id,width,size',
                'sort': 'size',
                'order': 0
            }
            result = self.json_ok('public_search_bulk_info', req_params)
            assert 'total' in result
            assert 'items' in result
            url_info = parse_open_url_call(mock_obj)
            assert url_info['page'] == '/get'
            assert len(url_info['params']['id']) == 3
            assert url_info['params']['uid'][0] == self.uid
            assert 'asc' not in url_info['params']
            assert url_info['params']['get'][0] == 'key,id,width,size'

            req_params['order'] = 1
            self.json_ok('public_search_bulk_info', req_params)
            url_info = parse_open_url_call(mock_obj)
            assert len(url_info['params']['asc']) == 1

    def test_public_search_bulk_info_search_meta_without_path_1(self):
        """Протестировать, что даже если path не передается в `search_meta`,
        то динамически добавляется (а из ответа выпиливается). """
        path = '/disk/test'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        response = self.json_ok('set_public', {'uid': self.uid, 'path': path})
        private_hash = response['hash']
        with mock.patch('mpfs.core.base._search_resources_info_by_file_ids') as mock_search_func:
            self.json_ok('public_search_bulk_info', {
                'private_hash': private_hash,
                'file_ids': '1,2,3',
                'search_meta': 'path,file_id,width,size',
                'sort': 'size',
                'order': 0
            })
            args, kwargs = mock_search_func.call_args
            assert 'fields' in kwargs
            assert 'path' in kwargs['fields']

    def test_search_public_list(self):
        # prepare
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        folder_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test'})['hash']
        self.upload_file(self.uid, '/disk/test/1.txt')
        file_hash = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/test/1.txt'})['hash']
        self.upload_file(self.uid, '/disk/test/2.txt')

        # test
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            rv = {
                'hitsArray': [
                    {
                        'key': '/disk/test/1.txt',
                        'type': 'file',
                        'ctime': '1446749715',
                        'name': '1.txt'
                    },
                    {
                        'key': '/disk/test/2.txt',
                        'type': 'file',
                        'ctime': '1446749715',
                        'name': '2.txt'
                    }
                ],
                'hitsCount': 2
            }
            mock_obj.return_value = to_json(rv)
            resp = self.json_ok('search_public_list', {'private_hash': folder_hash})
            for item in resp:
                assert item['id'].startswith(folder_hash)
        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value='{"hitsArray":[{"key":"/disk/test/1.txt","type":"file","ctime":"1446749715","name":"1.txt"}],"hitsCount":1}'
            resp = self.json_ok('search_public_list', {'private_hash': "%s:/1.txt" % folder_hash})
            assert resp['id'].startswith(folder_hash)
            resp = self.json_ok('search_public_list', {'private_hash': file_hash})
            assert resp['id'].startswith(file_hash)

    def test_dir_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.upload_file(self.uid, '/disk/test/1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/c'})
        self.upload_file(self.uid, '/disk/test/c/1.txt')

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value = '{"hitsArray":[{"key":"/disk/test/a"},{"key":"/disk/test/a/b"},{"key":"/disk/test/a/b/d"},{"key":"/disk/test/c"}],"hitsCount":4444}'
            req_params = {
                'uid': self.uid,
                'path': '/disk/test',
                'meta': 'hasfolders,file_id',
            }
            result = self.json_ok('dir_list', req_params)
            assert len(result) == 3
            for folder in result:
                assert folder['type'] == 'dir'
            assert result[0]['path'] == '/disk/test'
            assert result[0]['meta']['hasfolders'] == 1

            assert result[1]['path'] == '/disk/test/a'
            assert result[1]['meta']['hasfolders'] == 1

            assert result[2]['path'] == '/disk/test/c'
            assert result[2]['meta']['hasfolders'] == 0

    def test_dir_list_partial(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.upload_file(self.uid, '/disk/test/1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/c'})
        self.upload_file(self.uid, '/disk/test/c/1.txt')

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value = '{"hitsArray":[{"key":"/disk/test/a/b"},{"key":"/disk/test/a/b/d"},{"key":"/disk/test/c"}],"hitsCount":4444}'
            req_params = {
                'uid': self.uid,
                'path': '/disk/test',
                'meta': 'hasfolders,file_id',
            }
            result = self.json_ok('dir_list', req_params)
            assert len(result) == 3
            for folder in result:
                assert folder['type'] == 'dir'
            assert result[0]['path'] == '/disk/test'
            assert result[0]['meta']['hasfolders'] == 1

            assert result[1]['path'] == '/disk/test/a'
            assert result[1]['meta']['hasfolders'] == 1

            assert result[2]['path'] == '/disk/test/c'
            assert result[2]['meta']['hasfolders'] == 0

    def test_dir_list_partial_2(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.upload_file(self.uid, '/disk/test/1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/a/b/b'})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/b'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/b/c'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/b/d'})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test/c'})
        self.upload_file(self.uid, '/disk/test/c/1.txt')

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value = '{"hitsArray":[{"key":"/disk/test/a/b"},{"key":"/disk/test/a/b/d"},{"key":"/disk/test/c"},{"key":"/disk/test/c/d"}],"hitsCount":4444}'
            req_params = {
                'uid': self.uid,
                'path': '/disk/test',
                'meta': 'hasfolders,file_id',
            }
            result = self.json_ok('dir_list', req_params)
            assert len(result) == 4
            for folder in result:
                assert folder['type'] == 'dir'
            assert result[0]['path'] == '/disk/test'
            assert result[0]['meta']['hasfolders'] == 1

            assert result[1]['path'] == '/disk/test/a'
            assert result[1]['meta']['hasfolders'] == 1

            assert result[2]['path'] == '/disk/test/b'
            assert result[2]['meta']['hasfolders'] == 0

            assert result[3]['path'] == '/disk/test/c'
            assert result[3]['meta']['hasfolders'] == 1

    def test_list_folder_of_overdrawn_user(self):
        Quota().set_limit(100000, uid=self.uid)
        public_folder = '/disk/folder/'
        self.json_ok('mkdir', {'uid': self.uid, 'path': public_folder})
        file_data = {'size': 100000}
        path = public_folder + 'testfile.txt'
        self.upload_file(self.uid, path, file_data=file_data)
        Quota().set_limit(100, uid=self.uid)

        with mock.patch.object(mpfs.core.filesystem.resources.disk.BlockingsMixin, '_is_blockings_needed', return_value=True):
            self.json_ok('list', {'uid': self.uid, 'path': public_folder})

class InfoByResourceIdTestCase(CommonJsonApiTestCase):
    """Тесты ручки info_by_resource_id"""
    def setup_method(self, method):
        super(InfoByResourceIdTestCase, self).setup_method(method)
        self.folder_path = '/disk/test'
        self.file_path = '%s/123.txt' % self.folder_path
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.folder_path})
        self.upload_file(self.uid, self.file_path)
        self.resource_id = self.json_ok(
            'info', {'uid': self.uid, 'path': self.file_path, 'meta': 'resource_id'}
        )['meta']['resource_id']

    def test_get(self):
        """Общая проверка"""
        resp = self.json_ok(
            'info_by_resource_id',
            {'uid': self.uid, 'resource_id': self.resource_id, 'meta': 'resource_id'})
        assert resp['meta']['resource_id'] == self.resource_id
        assert resp['path'] == self.file_path

    def test_not_found(self):
        """Отсутствующий resource_id"""
        self.json_ok('rm', {'uid': self.uid, 'path': self.file_path})
        self.json_error(
            'info_by_resource_id',
            {'uid': self.uid, 'resource_id': self.resource_id, 'meta': 'resource_id'},
            code=ResourceNotFound.code
        )

    def test_shared_resource(self):
        """Запрос общего ресурса не владльцем"""
        shared_uid = self.uid_1
        self.create_user(shared_uid, noemail=1)
        self.json_ok('share_create_group', {'uid': self.uid, 'path': self.folder_path})

        opts = {
            'rights': 660,
            'universe_login': 'boo@boo.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': self.uid,
            'path': self.folder_path,
        }
        result = self.json_ok('share_invite_user', opts)
        hash_ = result['hash']
        assert hash_

        folder_info = self.json_ok('share_activate_invite', {'hash': hash_, 'uid': shared_uid})
        assert folder_info

        resp = self.json_ok(
            'info_by_resource_id',
            {'uid': shared_uid, 'resource_id': self.resource_id, 'meta': ''})

        assert resp['meta']['group']['owner']['uid'] == self.uid
        assert resp['meta']['group']['is_owned'] == 0
        assert resp['path'] == self.file_path

    def test_public_resource(self):
        """Запрос счетчика просмотров публичного ресурса владельцем"""
        self.json_ok('set_public', {'uid': self.uid, 'path': self.file_path})
        response = self.json_ok(
            'info_by_resource_id',
            {'uid': self.uid, 'resource_id': self.resource_id, 'meta': ''})
        assert 'views_counter' in response['meta']

        response = self.json_ok(
            'info_by_resource_id',
            {'uid': self.uid, 'resource_id': self.resource_id, 'meta': 'views_counter,size'})
        assert 'views_counter' in response['meta']

        response = self.json_ok(
            'info_by_resource_id',
            {'uid': self.uid, 'resource_id': self.resource_id, 'meta': 'size'})
        assert 'views_counter' not in response['meta']
