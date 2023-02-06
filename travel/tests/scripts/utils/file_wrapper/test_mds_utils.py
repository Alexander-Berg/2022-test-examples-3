# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import os

from common.settings import ServiceInstance
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.admin.scripts.utils.file_wrapper.mds_utils import (
    get_rasp_export_key, get_rasp_media_export_key, DIR_PATH, MDS_RASP_MEDIA_EXPORT, MDS_RASP_EXPORT
)


def test_get_path():
    export_file_path = os.path.join(DIR_PATH[MDS_RASP_EXPORT], 'some_path')
    assert get_rasp_export_key() == 'rasp-export'
    assert get_rasp_export_key(force_service=True) == 'service-admin/rasp-export'
    assert get_rasp_export_key(path=export_file_path) == 'rasp-export/some_path'
    assert get_rasp_export_key(path=export_file_path, force_service=True) == 'service-admin/rasp-export/some_path'

    media_export_file_path = os.path.join(DIR_PATH[MDS_RASP_MEDIA_EXPORT], 'some_path')
    assert get_rasp_media_export_key() == 'rasp-media-export'
    assert get_rasp_media_export_key(force_service=True) == 'service-admin/rasp-media-export'
    assert get_rasp_media_export_key(path=media_export_file_path) == 'rasp-media-export/some_path'
    assert get_rasp_media_export_key(path=media_export_file_path, force_service=True) == 'service-admin/rasp-media-export/some_path'

    with replace_setting('INSTANCE_ROLE', ServiceInstance):
        assert get_rasp_export_key() == 'service-admin/rasp-export'
        assert get_rasp_media_export_key() == 'service-admin/rasp-media-export'

        assert get_rasp_export_key(force_work=True) == 'rasp-export'
        assert get_rasp_media_export_key(force_work=True) == 'rasp-media-export'
