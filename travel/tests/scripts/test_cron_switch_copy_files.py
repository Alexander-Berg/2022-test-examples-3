# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import os

from django.conf import settings

from common.db.mds.clients import mds_s3_public_client, mds_s3_common_client
from common.settings.configuration import Configuration
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.admin.scripts.cron_switch_copy_files import upload_rasp_root, FILE_DIR_PATH, FILE_PATH, FileType, copy_export_data, copy_media_data


@replace_setting('MDS_ENABLE_WRITING', True)
@replace_setting('APPLIED_CONFIG', Configuration.TESTING)
def test_upload_rasp_root(tmpdir):
    tmpdir.join('root/myfile.txt').write_binary(b'myfile.txt', ensure=True)
    tmpdir.join('root/ReaDme.md').ensure()
    assert tmpdir.join('root/ReaDme.md').check()
    with replace_setting('MEDIA_ROOT', str(tmpdir)), \
            mock.patch.dict(FILE_DIR_PATH, {FileType.MEDIA_RASP_ROOT: os.path.join(settings.MEDIA_ROOT, 'root')}), \
            mock.patch.object(mds_s3_public_client.client, 'upload_file') as m_upload_file:
        upload_rasp_root()

    m_upload_file.assert_called_once_with(Key='rasp-root/myfile.txt', Filename=str(tmpdir.join('root/myfile.txt')),
                                          Bucket=mds_s3_public_client.bucket)


@replace_setting('MDS_ENABLE_WRITING', True)
@replace_setting('APPLIED_CONFIG', Configuration.TESTING)
def test_copy_export_data(tmpdir):
    tmpdir.join('t_types_by_geoid.json').write('123', ensure=True)
    tmpdir.join('public/stations.xml.gz').write('456', ensure=True)
    tmpdir.join('bus_station_codes.json').write('789', ensure=True)

    with replace_setting('EXPORT_PATH', str(tmpdir)), \
            mock.patch.dict(FILE_PATH, {
                FileType.STATION_XML_GZ: os.path.join(settings.EXPORT_PATH, 'public', 'stations.xml.gz'),
                FileType.T_TYPES_BY_GEOID: os.path.join(settings.EXPORT_PATH, 't_types_by_geoid.json'),
                FileType.BUS_STATION_CODES: os.path.join(settings.EXPORT_PATH, 'bus_station_codes.json')
            }), \
            mock.patch.object(mds_s3_common_client.client, 'upload_file') as m_upload_file:
        copy_export_data()

    assert m_upload_file.mock_calls == [
        mock.call(
            Key='rasp-export/public/stations.xml.gz',
            Filename=str(tmpdir.join('public/stations.xml.gz')),
            Bucket=mds_s3_common_client.bucket
        ),
        mock.call(
            Key='rasp-export/t_types_by_geoid.json',
            Filename=str(tmpdir.join('t_types_by_geoid.json')),
            Bucket=mds_s3_common_client.bucket
        ),
        mock.call(
            Key='rasp-export/bus_station_codes.json',
            Filename=str(tmpdir.join('bus_station_codes.json')),
            Bucket=mds_s3_common_client.bucket
        )
    ]


@replace_setting('MDS_ENABLE_WRITING', True)
@replace_setting('APPLIED_CONFIG', Configuration.TESTING)
def test_copy_media_data(tmpdir):
    tmpdir.join('data/export/suburban_cities_ru_ru.xml').write('123', ensure=True)

    with replace_setting('MEDIA_ROOT', str(tmpdir)), \
            mock.patch.dict(FILE_DIR_PATH, {FileType.MEDIA_DATA_EXPORT: os.path.join(settings.MEDIA_ROOT, 'data/export')}), \
            mock.patch.object(mds_s3_common_client, 'download_directory') as m_download_directory, \
            mock.patch.object(mds_s3_common_client.client, 'upload_file') as m_upload_file:
        copy_media_data()

    assert m_download_directory.mock_calls == [
        mock.call(
            prefix='rasp-media-export',
            directory_path=str(tmpdir.join('data/export')),
            only_new=False,
            remove_base_path='rasp-media-export'
        )
    ]

    assert m_upload_file.mock_calls == [
        mock.call(
            Key='rasp-media-export/suburban_cities_ru_ru.xml',
            Filename=str(tmpdir.join('data/export/suburban_cities_ru_ru.xml')),
            Bucket=mds_s3_common_client.bucket
        )
    ]
