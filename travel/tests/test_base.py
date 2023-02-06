# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os
from io import BytesIO

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.common23.db.mds.base import MDSS3Wrapper
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


class TestMDSS3Wrapper(object):
    def test_save(self):
        wrapper = MDSS3Wrapper('babucket')
        key, data = 'key', b'data'

        with mock.patch.object(wrapper.client, 'put_object', autospec=True) as m_put_object:
            wrapper.save_data(key, data)
            assert not m_put_object.call_args_list

        with mock.patch.object(wrapper.client, 'put_object', autospec=True) as m_put_object, \
                replace_setting('MDS_ENABLE_WRITING', True):

            wrapper.save_data(key, data)
            m_put_object.assert_called_once_with(Body=data, Key=key, Bucket='babucket')

    def test_get(self):
        wrapper = MDSS3Wrapper('babucket')
        key, data = 'key', b'data'
        with mock.patch.object(wrapper.client, 'get_object',
                               return_value={'Body': BytesIO(data)}) as m_get_object:
            assert wrapper.get_data(key).read() == data
            m_get_object.assert_called_once_with(Key=key, Bucket='babucket')

    @pytest.mark.parametrize('files, ignores, ignore_case, result', [
        (['1/2.txt'], [], False, ['1/2.txt']),
        (['1/2.txt', '1/2.obj'], ['*.obj'], False, ['1/2.txt']),
        (['1/2.txt', '1/2.obj'], ['*.obJ'], False, ['1/2.txt', '1/2.obj']),
        (['1/2.txt', '1/2.obJ'], ['*.obj'], False, ['1/2.txt', '1/2.obJ']),
        (['1/2.txt', '1/2.obj'], ['*.obJ'], True, ['1/2.txt']),
        (['1/2.txt', '1/2.obJ'], ['*.obj'], True, ['1/2.txt']),
        (['1/2.txt', '1.obj', '2/1.obj'], ['/1.obj'], False, ['1/2.txt', '2/1.obj']),
        (['1/2.txt', '1.obj', '2/1.obj'], ['1.obj'], False, ['1/2.txt']),
    ])
    @replace_setting('MDS_ENABLE_WRITING', True)
    def test_upload_directory(self, files, ignores, ignore_case, result, tmpdir):
        for f in files:
            tmpdir.join(f).ensure().write('test_data')

        wrapper = MDSS3Wrapper('babucket')
        with mock.patch.object(wrapper.client, 'upload_file') as m_upload_file:
            wrapper.upload_directory(str(tmpdir), 'base-mds/path', ignores, ignore_case)

        assert all(ca[1]['Bucket'] == 'babucket' for ca in m_upload_file.call_args_list)

        uploaded_files = []
        for ca in m_upload_file.call_args_list:
            assert os.path.relpath(ca[1]['Key'], 'base-mds/path') == \
                os.path.relpath(ca[1]['Filename'], str(tmpdir))
            uploaded_files.append(os.path.relpath(ca[1]['Key'], 'base-mds/path'))

        assert sorted(result) == sorted(uploaded_files)

    @replace_setting('MDS_ENABLE_WRITING', True)
    def test_upload_directory_empty_or_full_file(self, tmpdir):
        tmpdir.join('empty.file.txt').ensure()
        tmpdir.join('full.file.txt').ensure().write(b'data')

        wrapper = MDSS3Wrapper('babucket')
        with mock.patch.object(wrapper.client, 'upload_file') as m_upload_file:
            wrapper.upload_directory(str(tmpdir), 'base-mds/path', skip_empty_files=True)
        uploaded_files = []
        for ca in m_upload_file.call_args_list:
            uploaded_files.append(os.path.relpath(ca[1]['Key'], 'base-mds/path'))

        assert {'full.file.txt'} == set(uploaded_files)

        with mock.patch.object(wrapper.client, 'upload_file') as m_upload_file:
            wrapper.upload_directory(str(tmpdir), 'base-mds/path', skip_empty_files=False)
        uploaded_files = []
        for ca in m_upload_file.call_args_list:
            uploaded_files.append(os.path.relpath(ca[1]['Key'], 'base-mds/path'))

        assert {'empty.file.txt', 'full.file.txt'} == set(uploaded_files)

    def test_download_directory(self, tmpdir):
        def download_file(bucket, key, dir_key):
            tmpdir.join(dir_key.split('/')[-1]).ensure().write('some data {}'.format(key))

        wrapper = MDSS3Wrapper('bucket')
        key_1, key_2, key_3 = 'key_1.json', 'key_2.svg', 'key_3.xml'
        with open(os.path.join(tmpdir.strpath, key_3), 'w') as f:
            f.write('old data')

        with mock.patch.object(wrapper.client, 'download_file', side_effect=download_file, autospec=True), \
                mock.patch.object(wrapper.client, 'list_objects_v2') as m_list_objects:
            m_list_objects.side_effect = [
                {
                    'NextContinuationToken': 'nex_token',
                    'Contents': [{'Key': key_1}]
                },
                {
                    'Contents': [{'Key': key_2}]
                }
            ]

            wrapper.download_directory(prefix='mds/path', directory_path=tmpdir.strpath, only_new=True)
            assert m_list_objects.call_args_list == [
                mock.call(Prefix='mds/path', Bucket='bucket'),
                mock.call(Prefix='mds/path', Bucket='bucket', ContinuationToken='nex_token')
            ]

            with open(os.path.join(tmpdir.strpath, key_1)) as f:
                assert f.read() == 'some data {}'.format(key_1)
            with open(os.path.join(tmpdir.strpath, key_2)) as f:
                assert f.read() == 'some data {}'.format(key_2)
            with open(os.path.join(tmpdir.strpath, key_3)) as f:
                assert f.read() == 'old data'

            m_list_objects.side_effect = [
                {
                    'Contents': [{'Key': key_3}]
                }
            ]
            wrapper.download_directory(prefix='mds/path', directory_path=tmpdir.strpath)
            with open(os.path.join(tmpdir.strpath, key_3)) as f:
                assert f.read() == 'some data {}'.format(key_3)

            base_path_key = 'base_path/_key_1'
            m_list_objects.side_effect = [
                {
                    'Contents': [{'Key': base_path_key}]
                }
            ]
            wrapper.download_directory(prefix='mds/path', directory_path=tmpdir.strpath, remove_base_path='base_path')
            with open(os.path.join(tmpdir.strpath, '_key_1')) as f:
                assert f.read() == 'some data {}'.format(base_path_key)

    def test_delete_prefix_keys(self):
        wrapper = MDSS3Wrapper('bucket')
        key_1, key_2 = 'mds/path/key_1.json', 'mds/path/key_2.xml'

        with mock.patch.object(wrapper.client, 'delete_objects',  autospec=True) as m_delete_objects, \
                mock.patch.object(wrapper.client, 'list_objects_v2') as m_list_objects:
            m_list_objects.side_effect = [
                {
                    'NextContinuationToken': 'nex_token',
                    'Contents': [{'Key': key_1}]
                },
                {
                    'Contents': [{'Key': key_2}]
                }
            ]

            wrapper.delete_prefix_keys(prefix='mds/path')
            assert m_list_objects.call_args_list == [
                mock.call(Prefix='mds/path', Bucket='bucket'),
                mock.call(Prefix='mds/path', Bucket='bucket', ContinuationToken='nex_token')
            ]
            assert m_delete_objects.call_args_list == [
                mock.call(
                    Bucket='bucket',
                    Delete={'Objects': [{'Key': key_1}]}
                ),
                mock.call(
                    Bucket='bucket',
                    Delete={'Objects': [{'Key': key_2}]}
                )
            ]

    def test_delete_keys(self):
        wrapper = MDSS3Wrapper('bucket')
        keys = ['path/key_1.json', 'path/key_2.xml', 'path/key_3.html']
        objs_keys = [{'Key': k} for k in keys]

        with mock.patch.object(wrapper.client, 'delete_objects', autospec=True) as m_delete_objects:
            m_delete_objects.side_effect = [
             {
                 'Deleted': objs_keys[:2],
             },
             {
                 'Deleted': objs_keys[2:]
             }
            ]

            deleted_keys = wrapper.delete_keys(objs_keys=objs_keys, chunk_size=2)

            assert m_delete_objects.call_args_list == [
             mock.call(
                 Bucket='bucket',
                 Delete={'Objects': objs_keys[:2]}
             ),
             mock.call(
                 Bucket='bucket',
                 Delete={'Objects': objs_keys[2:]}
             )
            ]
            assert_that(deleted_keys, contains_inanyorder(*objs_keys))
