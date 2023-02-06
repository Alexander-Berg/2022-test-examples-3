# coding: utf-8

import logging
from contextlib import contextmanager
import tempfile
import shutil
import os.path

from market.pylibrary.s3.s3.s3_api import clean_bucket

from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import create_s3_awaps_test_client
from market.idx.pylibrary.s3_awaps.yatf.utils.s3_awaps_uploader import s3_client, s3_awaps_uploader, BUCKET_NAME

assert s3_awaps_uploader
assert s3_client


log = logging.getLogger()

GENERATION = '20170801_0101'


@contextmanager
def data_to_upload(file_names):
    def touch(filepath):
        open(filepath, 'w').close()

    pathh = tempfile.mkdtemp()
    for f in file_names:
        touch(os.path.join(pathh, f))

    yield pathh
    shutil.rmtree(pathh)


def test_upload(s3_client, s3_awaps_uploader):
    with data_to_upload(('a.txt', 'b.txt')) as dir_path:
        s3_awaps_uploader.upload_generation(GENERATION, dir_path)

    keys = s3_client.list(bucket=BUCKET_NAME, path='awaps/%s/' % GENERATION)
    assert 'awaps/%s/a.txt' % GENERATION in keys
    assert 'awaps/%s/b.txt' % GENERATION in keys

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.generation_staged)
    assert content == GENERATION

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.generations_list)
    assert content is None


def test_commit(s3_client, s3_awaps_uploader):
    with data_to_upload(('c.txt', 'e.txt')) as dir_path:
        s3_awaps_uploader.upload_generation(GENERATION, dir_path)
    s3_awaps_uploader.commit_generation(GENERATION)

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.generation_staged)
    assert content == ''

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.generations_list)
    assert content == GENERATION


def test_filelist(s3_client, s3_awaps_uploader):
    with data_to_upload(('f.txt', 'g.txt')) as dir_path:
        log.info(dir_path)
        s3_awaps_uploader.upload_generation(GENERATION, dir_path)

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.files_list_file(GENERATION))
    files = content.split('\n')

    assert set(files) == set(('f.txt', 'g.txt'))


def test_cleanup(s3_client, s3_awaps_uploader):
    for i in range(3):
        up = create_s3_awaps_test_client(BUCKET_NAME, nthreads=2)
        generation = str(i)
        with data_to_upload(('a.txt', 'b.txt')) as dir_path:
            up.upload_generation(generation, dir_path)
            up.commit_generation(generation)

    s3_awaps_uploader.clean_up(keep_count=2)

    keys = s3_client.list(bucket=BUCKET_NAME, path='awaps/')
    assert 'awaps/0/' not in keys
    assert 'awaps/1/' in keys
    assert 'awaps/2/' in keys

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.generations_list)
    assert content == '1\n2'


def test_append_upload(s3_client):
    generation = '20171221_2030'
    with data_to_upload(('a.txt', 'b.txt')) as dir_path:
        s3_awaps_uploader = create_s3_awaps_test_client(BUCKET_NAME)
        s3_awaps_uploader.upload_generation(generation, dir_path)

    with data_to_upload(['c.txt']) as dir_path:
        s3_awaps_uploader = create_s3_awaps_test_client(BUCKET_NAME)

        s3_awaps_uploader.upload_generation(generation, dir_path, append=True)

    content = s3_client.read(bucket=BUCKET_NAME, path=s3_awaps_uploader.files_list_file(generation))
    files = content.split('\n')

    assert set(files) == set(('a.txt', 'b.txt', 'c.txt'))


def test_upload_old_generation(s3_client, s3_awaps_uploader):
    with data_to_upload(('a.txt', 'b.txt')) as dir_path:
        s3_awaps_uploader.upload_generation(GENERATION, dir_path)
    s3_awaps_uploader.commit_generation(GENERATION)

    up = create_s3_awaps_test_client(BUCKET_NAME)
    assert not up.pre_check(generation='19841221_0101')


def test_sync_generations(s3_client, s3_awaps_uploader):
    clean_bucket(s3_client, BUCKET_NAME)

    with data_to_upload(('a.txt', 'b.txt')) as dir_path:
        s3_awaps_uploader.upload_generation('20200519_0000', dir_path)
    s3_awaps_uploader.commit_generation('20200519_0000')

    with data_to_upload(('a.txt', 'b.txt')) as dir_path:
        s3_awaps_uploader.upload_generation('20200520_0000', dir_path)
    s3_awaps_uploader.commit_generation('20200520_0000')

    # simulate removal on expiry
    s3_client.delete(BUCKET_NAME, path='{}/{}'.format(s3_awaps_uploader.prefix, '20200519_0000'))

    up = create_s3_awaps_test_client(BUCKET_NAME)
    assert up.fetch_generations() == ['20200520_0000']
