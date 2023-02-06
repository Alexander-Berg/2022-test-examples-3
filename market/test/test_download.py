import os
import pytest
from market.idx.pylibrary.download_blob_table_files import download
import yt.wrapper as yt


@pytest.fixture(scope='module')
def prepare_table():
    table_name = '//home/blob_table_for_download'
    yt
    yt.create(
        'table',
        table_name,
        attributes={
            'schema': [
                {'name': 'filename', 'type': 'string', 'sort_order': 'ascending'},
                {'name': 'part_index', 'type': 'int64', 'sort_order': 'ascending'},
                {'name': 'data', 'type': 'string'}
            ]
        }
    )
    yt.write_table(
        table_name, [
            {
                'filename': 'test' + str(i) + '/' + str(j),
                'part_index': k,
                'data': 'data' + str(k) + str(j)
            } for i in range(3) for j in range(3) for k in range(3)
        ]
    )
    return table_name


def test_download(prepare_table):
    download.download_files_from_blob_talbe(yt, prepare_table, './test_download')
    assert sorted(os.listdir('./test_download')) == ['test0', 'test1', 'test2']
    for i in range(3):
        assert sorted(os.listdir('./test_download/test{}'.format(i))) == ['0', '1', '2']
    for i in range(3):
        for j in range(3):
            with open('./test_download/test{i}/{j}'.format(i=i, j=j)) as f:
                assert f.read() == 'data0{j}data1{j}data2{j}'.format(j=j)


def test_download_prefix(prepare_table):
    download.download_files_from_blob_talbe(yt, prepare_table, './test_download_with_prefix', file_path_prefix='test1')
    assert sorted(os.listdir('./test_download_with_prefix')) == ['0', '1', '2']
    for j in range(3):
        with open('./test_download_with_prefix/{j}'.format(j=j)) as f:
            assert f.read() == 'data0{j}data1{j}data2{j}'.format(j=j)
