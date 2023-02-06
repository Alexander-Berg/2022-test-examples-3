# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from datetime import datetime
from collections import OrderedDict

from django.conf import settings

from common.db.mds.clients import mds_s3_common_client
from travel.rasp.admin.mds_files_viewer.views import get_listing_context


def test_listing():
    mds_client = settings.MDS_TMP_LISTING_CLIENT
    with mock.patch.object(mds_s3_common_client.client, 'list_objects_v2') as m_list_objects, \
            mock.patch.object(mds_s3_common_client, 'url', 'http://s3.mds.yandex.net'):
        m_list_objects.side_effect = [
            {
                'NextContinuationToken': 'nex_token',
                'Contents': [
                    {'Key': 'schedule-temporary/package/666/key_1', 'LastModified': datetime(2021, 5, 10, 14), 'Size': 123},

                ]
            },
            {
                'Contents': [
                    {'Key': 'schedule-temporary/package/666/sub_dir/key_2', 'LastModified': datetime(2021, 3, 2, 1), 'Size': 456}
                ]
            },
            {
                'Contents': [
                    {'Key': 'schedule-temporary/package/666/sub_dir/key_3', 'LastModified': datetime(2021, 1, 1, 1), 'Size': 789}
                ]
            }
        ]

        context = get_listing_context(request_path='/admin/mds_files_viewer/schedule-temporary/package/666', mds_client=mds_client)
        assert context == {
            'current_level': '/admin/mds_files_viewer/schedule-temporary/package/666',
            'list': OrderedDict([
                ('. . /', {
                    'last_modified': '-',
                    'size': '-',
                    'ref': '/admin/mds_files_viewer/schedule-temporary/package'
                }),
                ('key_1', {
                    'last_modified': '2021-05-10T14:00:00',
                    'size': 123,
                    'ref': 'http://s3.mds.yandex.net/rasp-test-bucket/schedule-temporary/package/666/key_1'
                }),
                ('sub_dir', {
                    'last_modified': '-',
                    'size': '-',
                    'ref': '/admin/mds_files_viewer/schedule-temporary/package/666/sub_dir'
                })
            ])
        }

        context = get_listing_context(request_path='/admin/mds_files_viewer/schedule-temporary/package', mds_client=mds_client)
        assert context == {
            'current_level': '/admin/mds_files_viewer/schedule-temporary/package',
            'list': OrderedDict([
                ('. . /', {
                    'last_modified': '-',
                    'size': '-',
                    'ref': '/admin/mds_files_viewer/schedule-temporary'
                }),
                ('666', {
                    'last_modified': '-',
                    'size': '-',
                    'ref': '/admin/mds_files_viewer/schedule-temporary/package/666'
                })
            ])
        }
