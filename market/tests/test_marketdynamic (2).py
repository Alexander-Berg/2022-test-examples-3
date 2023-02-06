# coding: utf-8

import logging
from mock import Mock
import pytest

from market.pylibrary.marketdynamic import get_latest_archive


def make_s3_client(filetree):

    def ls(_, path):
        return filetree[path]

    def get_url(_, path):
        return path

    s3_mock = Mock()
    s3_mock.list = Mock(side_effect=ls)
    s3_mock.get_url = Mock(side_effect=get_url)

    return s3_mock


@pytest.fixture()
def log():
    return logging.getLogger()


def test_take_latest_file(log):
    """ Возвращает путь к лексикографически самому свежему файлу """
    s3_host = 'host'
    bucket_name = 'bucket'
    bucket_folder_prefix = 'dynamic/'
    access_keys_path = 'nowhere'

    s3_mock = make_s3_client({
        'dynamic/': ['dynamic/2022-04-07/', 'dynamic/2022-04-06/'],
        'dynamic/2022-04-06/': ['dynamic/2022-04-06/2022-06-06-23-39'],
        'dynamic/2022-04-07/': ['dynamic/2022-04-07/2022-04-07-16-39',
                                'dynamic/2022-04-07/2022-04-07-16-29']
    })

    latest = get_latest_archive(s3_host, bucket_name, bucket_folder_prefix, access_keys_path, log, s3_client=s3_mock)

    assert latest == 'dynamic/2022-04-07/2022-04-07-16-39'


def test_take_prev_folder_if_latest_empty(log):
    """ Если в самой свежей папке - пусто, берем файл из предыдущей """
    s3_host = 'host'
    bucket_name = 'bucket'
    bucket_folder_prefix = 'dynamic/'
    access_keys_path = 'nowhere'

    s3_mock = make_s3_client({
        'dynamic/': ['dynamic/2022-04-07/', 'dynamic/2022-04-06/'],
        'dynamic/2022-04-06/': ['dynamic/2022-04-06/2022-06-06-23-39'],
        'dynamic/2022-04-07/': [],
    })

    latest = get_latest_archive(s3_host, bucket_name, bucket_folder_prefix, access_keys_path, log, s3_client=s3_mock)

    assert latest == 'dynamic/2022-04-06/2022-06-06-23-39'


def test_no_files(log):
    """ Не падаем, если файлов вообще нет """
    s3_host = 'host'
    bucket_name = 'bucket'
    bucket_folder_prefix = 'dynamic/'
    access_keys_path = 'nowhere'

    s3_mock = make_s3_client({'dynamic/': []})

    latest = get_latest_archive(s3_host, bucket_name, bucket_folder_prefix, access_keys_path, log, s3_client=s3_mock)

    assert latest is None
