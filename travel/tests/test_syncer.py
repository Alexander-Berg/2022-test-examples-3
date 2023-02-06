# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta

import mock
import six
if six.PY2:
    import pathlib2 as pathlib
else:
    import pathlib

import pytz
from django.conf import settings

from travel.rasp.library.python.common23.db.mds.syncer import MdsSyncer


class TestMdsSyncer(object):
    def test_update_file_no_local_file(self, tmpdir):
        local_file = tmpdir / 'path' / 'to' / 'file' / 'myfile.txt'

        # файла нет, качаем безусловно
        syncer = MdsSyncer([], interval=2)
        with mock.patch.object(syncer, 'mds_client') as m_client:
            syncer.update_file({
                'local_path': str(local_file),
                'mds_key': 'somepath/myfile.txt',
                'mds_bucket': 'somebucket',
            })
            assert not m_client.head_object.call_args_list
            assert pathlib.Path(local_file).parent.exists()  # проверяем, что все папки создались
            m_client.download_file.assert_called_once_with('somebucket', 'somepath/myfile.txt', str(local_file))

    def test_update_file_newer_local_file(self, tmpdir):
        local_file = tmpdir / 'myfile.txt'
        local_file.write('123')
        mds_modify_time = pytz.UTC.localize(datetime.utcnow() - timedelta(seconds=10))
        syncer = MdsSyncer([], interval=2)
        with mock.patch.object(syncer, 'mds_client') as m_client:
            m_client.head_object.return_value = {'LastModified': mds_modify_time}

            syncer.update_file({
                'local_path': str(local_file),
                'mds_key': 'somepath/myfile.txt',
                'mds_bucket': 'somebucket',
            })

            m_client.head_object.assert_called_once_with(Bucket='somebucket', Key='somepath/myfile.txt')
            assert not m_client.download_file.call_args_list

    def test_update_file_older_local_file(self, tmpdir):
        local_file = tmpdir / 'myfile.txt'
        local_file.write('123')
        mds_modify_time = pytz.UTC.localize(datetime.utcnow() + timedelta(seconds=10))
        syncer = MdsSyncer([], interval=2)
        with mock.patch.object(syncer, 'mds_client') as m_client:
            m_client.head_object.return_value = {'LastModified': mds_modify_time}

            syncer.update_file({
                'local_path': str(local_file),
                'mds_key': 'somepath/myfile.txt',
            })

            m_client.head_object.assert_called_once_with(
                Bucket=settings.MDS_RASP_COMMON_BUCKET, Key='somepath/myfile.txt')
            m_client.download_file.assert_called_once_with(
                settings.MDS_RASP_COMMON_BUCKET, 'somepath/myfile.txt', str(local_file))

    def test_update_files(self):
        conf = [
            {
                'local_path': 'path1',
                'mds_key': 'mds_path1',
                'mds_bucket': 'somebucket',
            },
            {
                'local_path': 'path2',
                'mds_key': 'mds_path2',
                'mds_bucket': 'somebucket',
            },
            {
                'local_path': 'path3',
                'mds_key': 'mds_path3',
            },
        ]
        syncer = MdsSyncer(conf, interval=2)
        with mock.patch.object(syncer, 'update_file') as m_update_file:
            m_update_file.side_effect = [None, Exception, None]

            syncer.update_files()

            calls = m_update_file.call_args_list
            assert calls == [mock.call(c) for c in conf]
