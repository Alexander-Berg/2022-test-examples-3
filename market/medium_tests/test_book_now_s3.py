# -*- coding: utf-8 -*-

import logging

from mock import patch, create_autospec
from market.pylibrary.s3.s3.s3_api import Client

from market.idx.pylibrary.mindexer_core.book_now.book_now import (
    book_now_shop_upload_with_s3,
    make_book_now_s3_path,
    S3_BOOK_NOW_BUCKET,
)

log = logging.getLogger()
bucket = S3_BOOK_NOW_BUCKET + '-test'


def make_mock_s3_client():
    stored_data = {}

    def mock_upload(bucket, path, uploading_path):
        if stored_data.get(bucket, None) is None:
            stored_data[bucket] = {}
        with open(uploading_path, 'r') as content_file:
            stored_data[bucket][path] = content_file.read()

    def mock_make_symlink(bucket, path, symlinkname):
        stored_data[bucket][symlinkname] = bucket + '/' + path

    def mock_read(bucket, path):
        return stored_data[bucket][path]

    def mock_list(bucket):
        return list(stored_data[bucket].keys())

    def mock_get_url(bucket, path):
        return bucket + '/' + path

    mock_client = create_autospec(Client)
    mock_client.upload_file.side_effect = mock_upload
    mock_client.make_symlink.side_effect = mock_make_symlink
    mock_client.read.side_effect = mock_read
    mock_client.list.side_effect = mock_list
    mock_client.get_url.side_effect = mock_get_url

    return mock_client


def test_write_book_now_to_mds(tmpdir):
    # prepearing to execution
    # initiate here because it isn't necessary default test's fork and need execution in one thread
    # https://wiki.yandex-team.ru/yatool/test/#testynapytest see SPLIT_FACTOR(N)

    client = make_mock_s3_client()

    # prepearing data to upload
    content = 'content'
    f = tmpdir.join('upload.txt')
    f.write(content)

    generations = ['20170715_1237', '20170715_1456']
    with patch('market.idx.pylibrary.mindexer_core.book_now.book_now.S3_BOOK_NOW_BUCKET', bucket):
        for generation_name in generations:
            book_now_shop_upload_with_s3(client, generation_name, str(f))

            path = make_book_now_s3_path(generation_name)
            readed = client.read(bucket, path)
            assert readed == content
            readed = client.read(bucket, 'recent')
            url = client.get_url(bucket, path)
            assert readed == url

    level = client.list(bucket)
    assert sorted(level) == sorted(['recent', '20170715_1456/book_now_shop.tsv', '20170715_1237/book_now_shop.tsv'])
