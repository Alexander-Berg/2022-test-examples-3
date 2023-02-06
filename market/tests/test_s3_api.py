# -*- coding: utf-8 -*-

import logging
import math
import pytest
from boto.exception import S3ResponseError
from datetime import datetime
from market.pylibrary.s3.yatf.utils.s3_client import create_s3_test_client
from market.pylibrary.s3.s3.s3_api import clean_bucket, KeyLock
from market.pylibrary.s3.s3.s3_file_writer import BadBuffersSizeException

BUCKET_NAME = 's3api-test'

log = logging.getLogger()


@pytest.fixture()
def client():
    client = create_s3_test_client()
    clean_bucket(client, BUCKET_NAME)
    yield client
    clean_bucket(client, BUCKET_NAME)


@pytest.fixture()
def client_skip_check_bucket():
    client = create_s3_test_client(skip_check_bucket=True)
    clean_bucket(client, BUCKET_NAME)
    yield client
    clean_bucket(client, BUCKET_NAME)


@pytest.fixture()
def clean_lifecycle(client):
    client.get_or_create(BUCKET_NAME)
    client.clear_lifecycle_config(BUCKET_NAME)
    pytest.raises(S3ResponseError, client.get_lifecycle_config, BUCKET_NAME)
    yield
    client.clear_lifecycle_config(BUCKET_NAME)


def test_bucket(client):
    b = client.get_or_create(BUCKET_NAME)
    assert b.name == BUCKET_NAME


@pytest.mark.parametrize('path', [
    '1.txt',
    'folder/2.txt',
])
def test_write_read(client, path):
    content = 'some message to write'
    assert client.write(BUCKET_NAME, path, content)
    assert client.read(BUCKET_NAME, path) == content


def test_read_not_exists(client):
    assert client.read(BUCKET_NAME, 'anyfolder/anypath') is None


def test_list_all_buckets(client):
    level0 = client.list()
    assert level0
    assert BUCKET_NAME in level0


def test_list_bucket_dirs(client):
    path = 'folder1/1.txt'
    assert client.write(BUCKET_NAME, path, 'some message')
    log.info('{0} created'.format(path))

    path = 'folder1/subfolder1/2.txt'
    assert client.write(BUCKET_NAME, path, 'some another message')
    log.info('{0} created'.format(path))

    level1 = client.list(BUCKET_NAME, 'folder1/')
    level2 = client.list(BUCKET_NAME, 'folder1/subfolder1/')
    assert level1 == ['folder1/1.txt', 'folder1/subfolder1/']
    assert level2 == ['folder1/subfolder1/2.txt']


def test_delete_objects(client):
    path = 'folder1/1.txt'
    assert client.write(BUCKET_NAME, path, 'some message')
    log.info('{0} created'.format(path))

    path = 'folder1/subfolder1/2.txt'
    assert client.write(BUCKET_NAME, path, 'some another message')
    log.info('{0} created'.format(path))

    client.delete(BUCKET_NAME, 'folder1/')

    assert client.list(BUCKET_NAME, 'folder1/', True) == []


def test_deep_list_bucket_dirs(client):
    path = 'folder1/1.txt'
    assert client.write(BUCKET_NAME, path, 'some message')
    log.info('{0} created'.format(path))

    path = 'folder1/subfolder1/2.txt'
    assert client.write(BUCKET_NAME, path, 'some another message')
    log.info('{0} created'.format(path))

    assert client.list(BUCKET_NAME, 'folder1/', True) == ['folder1/1.txt', 'folder1/subfolder1/2.txt']


def test_upload(client, tmpdir):
    path = 'folder2/1.txt'
    content = 'MESSAGE'
    f = tmpdir.join('upload.txt')
    f.write(content)

    assert client.upload_file(BUCKET_NAME, path, str(f))
    assert content == client.read(BUCKET_NAME, path)


def test_download(client, tmpdir):
    path = 'folder3/1.txt'
    content = 'MESSAGE'
    assert client.write(BUCKET_NAME, path, content)

    downloaded_path = str(tmpdir.join('download.txt'))
    assert client.download_file(BUCKET_NAME, path, downloaded_path) is True

    with open(downloaded_path, 'r') as f:
        assert f.read() == content


def test_download_not_exists(client, tmpdir):
    path = 'folder4/1.txt'
    downloaded_path = str(tmpdir.join('download.txt'))
    assert client.download_file(BUCKET_NAME, path, downloaded_path) is False

    with pytest.raises(IOError) as error:
        with open(downloaded_path, 'r') as f:
            f.read()
    assert 'No such file or directory' in str(error.value)


def test_symlink(client):
    origin_path = 'folder5/folder1/1.txt'
    symlink_path = 'folder5/meta'
    url = client.write(BUCKET_NAME, origin_path, '123456')
    assert url

    assert client.make_symlink(BUCKET_NAME, origin_path, symlink_path)

    data = client.read(BUCKET_NAME, symlink_path)
    assert data == url


def test_url(client):
    path = 'folder6/1.txt'
    write_url = client.write(BUCKET_NAME, path, 'MESSAGE')
    get_url = client.get_url(BUCKET_NAME, path)

    assert write_url == get_url


def test_lock(client):
    path = 'folder7/1.txt'

    client.write(BUCKET_NAME, path, 'MESSAGE')

    assert client.acquireLock(BUCKET_NAME, path)

    client.releaseLock(BUCKET_NAME, path)


def test_try_lock(client):
    path = 'folder8/1.txt'

    client.write(BUCKET_NAME, path, 'MESSAGE')
    client.acquireLock(BUCKET_NAME, path)

    assert not client.acquireLock(BUCKET_NAME, path, timeout=1)

    client.releaseLock(BUCKET_NAME, path)

    assert client.acquireLock(BUCKET_NAME, path)


def test_keylock(client):
    path = 'folder9/1.txt'

    with KeyLock(client, BUCKET_NAME, path):
        assert not client.acquireLock(BUCKET_NAME, path, timeout=1)


def test_get_last_modified(client):
    from pytz import timezone

    path = 'folder10/1.txt'

    before = datetime.now(tz=timezone('GMT')).replace(microsecond=0)

    client.write(BUCKET_NAME, path, 'MESSAGE')

    after = datetime.now(tz=timezone('GMT'))

    last_modified = client.get_last_modified(BUCKET_NAME, path)

    assert last_modified >= before
    assert last_modified <= after


def test_stream_upload(client):
    """
    Тест проверяет работоспособность upload'a по частям в s3
    """
    path = 'folder11/1.txt'

    buffer_size = 5 * 1024 ** 2  # 5 MB

    # ~23 MB
    content = ['message' + str(i) for i in range(buffer_size // 3)]

    with client.open_write(BUCKET_NAME, path, buffer_size=buffer_size) as s3_file:
        for c in content:
            s3_file.write(c)

    assert ''.join(content) == client.read(BUCKET_NAME, path)


def test_stream_upload_chunks_count(client):
    """
    Тест проверяет, что upload был в несколько частей
    """
    path = 'folder12/1.txt'

    buffer_size = 5 * 1024 ** 2  # 5 MB

    # ~23 MB
    content = ['message' + str(i) for i in range(buffer_size // 3)]

    writer = client.open_write(BUCKET_NAME, path, buffer_size=buffer_size)
    for c in content:
        writer.write(c)
    writer.close()

    assert math.ceil(float(len(''.join(content))) / buffer_size) == writer.parts


def test_stream_upload_small_buffer(client):
    """
    Тест проверяет, что при буффере меньше 5 МБ вызывается исключение
    """
    path = 'folder13/1.txt'
    buffer_size = 5 * 1024 ** 2 - 1

    with pytest.raises(BadBuffersSizeException):
        writer = client.open_write(BUCKET_NAME, path, buffer_size=buffer_size)
        writer.close()


def test_stream_upload_large_buffer(client):
    """
    Тест проверяет, что при буффере больше 5 GБ вызывается исключение
    """
    path = 'folder14/1.txt'
    buffer_size = 5 * 1024 ** 3 + 1

    with pytest.raises(BadBuffersSizeException):
        writer = client.open_write(BUCKET_NAME, path, buffer_size=buffer_size)
        writer.close()


def test_upload_skip_check_bucket(client_skip_check_bucket, tmpdir):
    path = 'folder15/1.txt'
    content = 'MESSAGE'
    f = tmpdir.join('upload.txt')
    f.write(content)

    assert client_skip_check_bucket.upload_file(BUCKET_NAME, path, str(f))
    assert content == client_skip_check_bucket.read(BUCKET_NAME, path)


def test_set_expiration(client, clean_lifecycle):
    """
    Тест проверяет установку протухания
    """
    client.set_expire_in_days(BUCKET_NAME, 1, rule_id='TestRule', prefix='prefix')

    config = client.get_lifecycle_config(BUCKET_NAME)
    assert len(config) == 1
    rule = config[0]
    assert rule.expiration.days == 1
    assert rule.id == 'TestRule'
    assert rule.prefix == 'prefix'


def test_reset_expiration(client, clean_lifecycle):
    """
    Тест проверяет переустановку протухания
    """
    client.set_expire_in_days(BUCKET_NAME, 1)
    client.set_expire_in_days(BUCKET_NAME, 2)

    config = client.get_lifecycle_config(BUCKET_NAME)
    assert len(config) == 1
    assert config[0].expiration.days == 2


def test_append_expiration(client, clean_lifecycle):
    """
    Тест проверяет добавление правила протухания
    """
    client.set_expire_in_days(BUCKET_NAME, 1)
    client.set_expire_in_days(BUCKET_NAME, 2, append=True)

    config = client.get_lifecycle_config(BUCKET_NAME)
    assert len(config) == 2
    assert config[0].expiration.days == 1
    assert config[1].expiration.days == 2


def test_clear_expiration(client):
    """
    Тест проверяет обнуление правил протухания
    """
    client.set_expire_in_days(BUCKET_NAME, 1)
    client.set_expire_in_days(BUCKET_NAME, 2, append=True)

    client.clear_lifecycle_config(BUCKET_NAME)

    pytest.raises(S3ResponseError, client.get_lifecycle_config, BUCKET_NAME)


def test_append_expiration_with_same_id(client, clean_lifecycle):
    """
    Тест проверяет добавление правила с существующим id (так можно)
    """
    client.set_expire_in_days(BUCKET_NAME, 1, rule_id='TestRule', prefix='prefix')
    client.set_expire_in_days(BUCKET_NAME, 2, rule_id='TestRule', prefix='prefix', append=True)

    config = client.get_lifecycle_config(BUCKET_NAME)
    assert len(config) == 2
    assert config[0].expiration.days == 1
    assert config[0].id == 'TestRule'
    assert config[0].prefix == 'prefix'
    assert config[1].expiration.days == 2
    assert config[1].id == 'TestRule'
    assert config[1].prefix == 'prefix'
