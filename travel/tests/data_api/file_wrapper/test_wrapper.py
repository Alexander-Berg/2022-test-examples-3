# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
import mock

from common.data_api.file_wrapper.wrappers import SandboxFileWrapper, MdsFileWrapper


def download_file(path, directory, data):
    file_name = path.split('/')[-1]
    directory.join(file_name).write(data)


def test_sandbox_wrapper(tmpdir):
    res_type = 'RES'
    file_name_1, file_name_2 = 'dump.sql.gz', 'dump2.sql.gz'
    dump_data, another_dump_data, rewrite = 'new_dump', 'yet_another_dump', 'rewrite'
    directory = tmpdir.mkdir('xxx')
    file_path = os.path.join(directory.strpath, file_name_1)

    def download_sandbox(self):
        download_file(self.path, directory, dump_data)

    with mock.patch.object(SandboxFileWrapper, 'download_from_sandbox', side_effect=download_sandbox, autospec=True):
        # файла нет, скачиваем новый
        assert os.path.exists(file_path) is False
        file_wrapper = SandboxFileWrapper(file_path, resource_type=res_type)
        assert file_wrapper.resource_type == res_type
        assert file_wrapper.path == file_path
        with file_wrapper.open(mode='r') as f:
            assert f.read() == dump_data

    # файл есть, открываем его
    directory.join(file_name_2).write(another_dump_data)
    file_wrapper = SandboxFileWrapper(os.path.join(directory.strpath, file_name_2), resource_type=res_type)
    with file_wrapper.open(mode='r') as f:
        assert f.read() == another_dump_data

    file_wrapper = SandboxFileWrapper(os.path.join(directory.strpath, file_name_2), resource_type=res_type)
    with file_wrapper.open(mode='w') as f:
        f.write(rewrite)

    with file_wrapper.open(mode='r') as f:
        assert f.read() == rewrite


def test_mds_wrapper(tmpdir):
    file_name_1, file_name_2 = 'export.json', 'export.xml'
    json_data, xml_data = 'export_json_data', 'export_xml_data'
    directory = tmpdir.mkdir('xxx')
    file_path = os.path.join(directory.strpath, file_name_1)
    bucket = 'bucket'

    def download_mds(self):
        download_file(self.path, directory, json_data)

    with mock.patch.object(MdsFileWrapper, 'download_from_mds', side_effect=download_mds, autospec=True):
        # файла нет, скачиваем новый
        assert os.path.exists(file_path) is False
        file_wrapper = MdsFileWrapper(path=file_path, bucket=bucket,)
        with file_wrapper.open(mode='r') as f:
            assert f.read() == json_data
        assert file_wrapper.bucket == bucket
        assert file_wrapper.path == file_path
        assert file_wrapper.key == file_path

    # файл есть, открываем его
    key = 'mds_key'
    directory.join(file_name_2).write(xml_data)
    file_wrapper = MdsFileWrapper(os.path.join(directory.strpath, file_name_2), bucket, key=key)
    with file_wrapper.open(mode='r') as f:
        assert f.read() == xml_data
    assert file_wrapper.key == key
