# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from django.conf import settings

from travel.rasp.library.python.common23.db.mds.clients import mds_s3_public_client, mds_s3_common_client


def test_clients():
    assert mds_s3_public_client.bucket == settings.MDS_RASP_PUBLIC_BUCKET
    assert mds_s3_common_client.bucket == settings.MDS_RASP_COMMON_BUCKET
