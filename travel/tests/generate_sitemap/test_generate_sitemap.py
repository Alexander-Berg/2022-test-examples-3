# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from django.utils import six

from common.db.mds.clients import mds_s3_public_client
from common.settings.configuration import Configuration
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.trains.scripts.generate_sitemap.generate_sitemap import upload_sitemap


@replace_setting('MDS_ENABLE_WRITING', True)
@replace_setting('APPLIED_CONFIG', Configuration.TESTING)
def test_upload_sitemap(tmpdir):
    trains_path = tmpdir / 'train-root'
    (trains_path / 'myfile.txt').write_binary(b'myfile.txt', ensure=True)
    (trains_path / 'ReaDme.md').ensure()
    assert (trains_path / 'ReaDme.md').check()

    travel_trains_path = tmpdir / 'travel-train-root'
    (travel_trains_path / 'my_travel_file.txt').write_binary(b'my_travel_file.txt', ensure=True)
    (travel_trains_path / 'ReaDme.md').ensure()
    assert (travel_trains_path / 'ReaDme.md').check()

    with mock.patch.object(mds_s3_public_client.client, 'upload_file') as m_upload_file:
        upload_sitemap(trains_path, travel_trains_path)

    assert m_upload_file.mock_calls == [
        mock.call(
            Key='train-root/myfile.txt',
            Filename=six.text_type(trains_path / 'myfile.txt'),
            Bucket=mds_s3_public_client.bucket
        ),
        mock.call(
            Key='travel-train-root/my_travel_file.txt',
            Filename=six.text_type(travel_trains_path / 'my_travel_file.txt'),
            Bucket=mds_s3_public_client.bucket
        )
    ]
