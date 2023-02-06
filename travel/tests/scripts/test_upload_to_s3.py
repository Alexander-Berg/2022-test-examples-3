# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from mock import mock

from common.db.mds.clients import mds_s3_common_client
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.admin.scripts import upload_to_s3


@replace_setting('MDS_ENABLE_WRITING', True)
def test_main(tmpdir):
    tmpdir.join('export.csv').ensure().write('1')
    tmpdir.join('sodl22.sql.gz').ensure().write('1')
    tmpdir.join('2345/schema.sql.gz').ensure().write('1')
    with replace_setting('EXPORT_PATH', str(tmpdir)), \
            mock.patch.object(mds_s3_common_client.client, 'upload_file') as m_upload_file:
        upload_to_s3.main()

    m_upload_file.assert_called_once_with(Key='rasp-export/export.csv', Filename=str(tmpdir.join('export.csv')),
                                          Bucket=mds_s3_common_client.bucket)
