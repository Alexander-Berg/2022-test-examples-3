# -*- encoding: utf-8 -*-
import os
import logging
import yatest
import pytest
import time
import shutil
import six
if six.PY2:
    import subprocess32 as subprocess
else:
    import subprocess
from typing import Tuple  # noqa
from _pytest.monkeypatch import MonkeyPatch  # noqa
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.pylibrary.s3.s3.stub.s3_bucket_emulation import StubS3Client, S3BucketFSEmulation

from market.idx.datacamp.parser.lib.parser_engine.session_logs_uploader import (
    CONFIG_SECTION,
    DEFAULT_EXTENSION_FOR_COMPRESSED_FILE,
    FileToUpload,
    SessionLogsUploader,
    UploadInfo,
    UploadStatus,
    UploaderType,
)
from market.idx.datacamp.parser.lib.parser_engine import utils as orutil
from market.idx.datacamp.parser.lib.parser_engine.feed import FeedData

MI_TYPE = 'test.only'
TIMEOUT = 2
TIME_TO_SLEEP = TIMEOUT * 10
FS_FALLBACK_DIR = 'fs_fallback_dir'


class StubS3Config(object):
    """Stub config class, only needed because object() doesn't have __dict__.
    """
    def __init__(self):
        self.s3_bucket_emulation = None  # type: S3BucketFSEmulation


def get_tmp_dir_full_path(dir_name):
    return yatest.common.test_output_path(dir_name)


def create_session_dir(session_name):
    """A local directory that substitutes for /var/lib/yandex/indexer/market.
    """
    dir_path = yatest.common.test_output_path(session_name)
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path)
    return dir_path


def create_fs_fallback_dir():
    dir_path = yatest.common.test_output_path('fs_fallback_dir')
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path)
    return dir_path


def mds_store_dir():
    dir_path = yatest.common.test_output_path('mds_store')
    if os.path.exists(dir_path):
        shutil.rmtree(dir_path)
    os.makedirs(dir_path)
    return dir_path


@pytest.fixture(scope="module")
def file_content():
    return "test file content"


@pytest.fixture(scope="module")
def host_name():
    return "test_host"


@pytest.fixture(scope="module")
def bucket_name():
    return "test_bucket"


@pytest.fixture(scope="module")
def log():
    """A real logger that passes all requests to a null handler.
    """
    logger = logging.getLogger()
    # logger = logging.Logger('')
    # logger.propagate = False
    # logger.setLevel(logging.DEBUG)
    # logger.addHandler(logging.NullHandler())
    return logger


@pytest.fixture
def config(tmpdir_factory, log_broker_stuff, yt_server, host_name, bucket_name):
    cfg = {
        CONFIG_SECTION: {
            's3_host': host_name,
            's3_access_key_path': 'some_path',
            's3_bucket_name': bucket_name,
            'fs_fallback_dir': create_fs_fallback_dir(),
            'cleaner_workers': 2
        }
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture
def s3_config():
    # type: () -> StubS3Config
    """Default S3 client config."""
    config = StubS3Config()
    config.s3_bucket_emulation = S3BucketFSEmulation(mds_store_dir())
    return config


@pytest.fixture
def feed():
    feed = FeedData()
    feed.feed_id = 456
    feed.real_feed_id = feed.feed_id
    feed.session_name = orutil.get_session_name(orutil.ts2datetime(time.time()))
    feed.session_dir = create_session_dir(feed.session_name)
    return feed


@pytest.fixture
def mi_type():
    return MI_TYPE


def raise_(ex):
    raise ex


@pytest.fixture
def s3_client_with_exception(monkeypatch, s3_config, log):
    monkeypatch.setattr(
        'market.pylibrary.s3.s3.stub.s3_bucket_emulation.StubS3Client.upload_file',
        lambda _, bucket, path, src_file_path: raise_(Exception("Some exception"))
    )
    return StubS3Client(s3_config, log)


@pytest.fixture
def s3_client_with_hang(monkeypatch, s3_config, log):
    monkeypatch.setattr(
        'market.pylibrary.s3.s3.stub.s3_bucket_emulation.StubS3Client.upload_file',
        lambda _, bucket, path, src_file_path: time.sleep(TIME_TO_SLEEP)
    )
    return StubS3Client(s3_config, log)


def setup_module():
    os.environ['MI_TYPE'] = MI_TYPE


def get_date_dir_from_session_name(session_name):
    return session_name.split('_')[0]


def get_hour_dir_from_session_name(session_name):
    return session_name.split("_")[1][0:2]


def get_mds_path_for_file(mi_type, feed, file_name):
    # type: (str, FeedData, str) -> str
    def get_last_to_digits(id):
        if id < 10:
            return "{:02}".format(id)
        else:
            return str(id)[-2:]

    if feed.feed_id:
        mds_feed_subpath = "{mds_feed_group_name}/{feed_id}".format(
            mds_feed_group_name=get_last_to_digits(feed.feed_id),
            feed_id=feed.feed_id
        )
    else:
        mds_feed_subpath = "business/{mds_feed_group_name}/{business_id}".format(
            mds_feed_group_name=get_last_to_digits(feed.business_id),
            business_id=feed.business_id
        )

    return "{mi_type}/{date_dir}/{hour_dir}/{mds_feed_subpath}/{session_name}/{file_name}".format(
        mi_type=mi_type,
        date_dir=get_date_dir_from_session_name(feed.session_name),
        hour_dir=get_hour_dir_from_session_name(feed.session_name),
        session_name=feed.session_name,
        file_name=file_name,
        mds_feed_subpath=mds_feed_subpath
    )


def create_test_files(base_dir, files, content):
    # type: (str, Tuple[FileToUpload, ...], str) -> None
    for file_to_upload in files:
        with open(os.path.join(base_dir, file_to_upload.file_name), 'w') as fp:
            fp.write(content)


def decompress_string(content):
    # type: (str) -> str
    cmd = ['gzip', '--to-stdout', '--decompress']
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, stdin=subprocess.PIPE)
    stdout, stderr = p.communicate(input=content)
    if p.returncode not in [0, 2]:
        raise Exception('Failed to decompress file. Retcode: {}, Stderr: {}'.format(p.returncode, stderr))

    return stdout


def get_fallback_file_full_path(feed, file_to_upload):
    # type: (FeedData, FileToUpload) -> str
    if feed.feed_id:
        subpath = str(feed.feed_id)
    else:
        subpath = 'business.' + str(feed.business_id)

    file_full_path = '{}.{}.{}'.format(subpath, feed.session_name, file_to_upload.file_name)

    return os.path.join(get_tmp_dir_full_path(FS_FALLBACK_DIR), file_full_path)


def get_session_logs_uploader(feed, config, s3_client, log):
    uploader_type = UploaderType.COMMON_FFED
    id = feed.feed_id
    if not id:
        id = feed.business_id
        uploader_type = UploaderType.BUSINESS_FEED

    return SessionLogsUploader(config, uploader_type, id, feed.session_name, s3_client, log)


def test_get_url_to_download(config, s3_config, host_name, bucket_name, mi_type, feed, log):
    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))

    for file_to_upload in test_files:
        file_name = file_to_upload.file_name

        if file_to_upload.need_compress:
            file_name += DEFAULT_EXTENSION_FOR_COMPRESSED_FILE

        expected = os.path.join(
            'https://' + host_name, bucket_name,
            get_mds_path_for_file(mi_type, feed, file_name)
        )
        actual = session_logs_uploader.get_url_to_download(file_to_upload)
        assert actual == expected


def test_async_upload__ok(feed, config, s3_config, log, bucket_name, mi_type, file_content):
    # type: (FeedData, ConfigParser, StubS3Config, logging.Logger, str, str, str) -> None
    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.async_upload_with_fallback(feed.session_dir, test_files)
    session_logs_uploader.wait_async_uploading_with_fallback(60)

    failed_files = session_logs_uploader.get_failed_uploads()
    assert not failed_files

    files_in_bucket = s3_config.s3_bucket_emulation.list_files(bucket_name, path=None)
    assert len(files_in_bucket) == len(test_files)
    for file_to_upload in test_files:
        file_name = file_to_upload.file_name

        assert os.path.exists(os.path.join(feed.session_dir, file_name))

        if file_to_upload.need_compress:
            file_name += DEFAULT_EXTENSION_FOR_COMPRESSED_FILE
        target_key_path = get_mds_path_for_file(mi_type, feed, file_name)
        assert target_key_path in files_in_bucket

        file_content_in_s3 = s3_client.read(bucket_name, target_key_path)
        if file_to_upload.need_compress:
            file_content_in_s3 = decompress_string(file_content_in_s3)

        assert six.ensure_str(file_content_in_s3) == file_content


def test_async_upload__exception_while_uploading__fs_fallback_is_done(feed, config, log, file_content,
                                                                      s3_client_with_exception):
    # type: (FeedData, ConfigParser, logging.Logger, str, StubS3Client) -> None
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client_with_exception, log)
    test_files = (
        FileToUpload('test1', True, skip_fs_fallback=False),
        FileToUpload('test2', False, skip_fs_fallback=True)
    )
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.async_upload_with_fallback(feed.session_dir, test_files)
    session_logs_uploader.wait_async_uploading_with_fallback(60)

    for file_to_upload in test_files:
        fallback_file_full_path = get_fallback_file_full_path(feed, file_to_upload)
        assert file_to_upload.skip_fs_fallback or os.path.exists(fallback_file_full_path)


def test_async_upload__uploading_hang__fs_fallback_is_done(feed, config, log, file_content, s3_client_with_hang):
    # type: (FeedData, ConfigParser, logging.Logger, str, StubS3Client) -> None
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client_with_hang, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.async_upload_with_fallback(feed.session_dir, test_files)

    ts_start = time.time()
    session_logs_uploader.wait_async_uploading_with_fallback(TIMEOUT)
    ts_finish = time.time()

    # wait a bit more to avoid flapping
    assert ts_finish - ts_start < TIMEOUT + 3.0

    for file_to_upload in test_files:
        fallback_file_full_path = get_fallback_file_full_path(feed, file_to_upload)
        assert file_to_upload.skip_fs_fallback or os.path.exists(fallback_file_full_path)


def test_async_upload__file_not_present__file_skipped(feed, config, s3_config, log):
    # type: (FeedData, ConfigParser, StubS3Config, logging.Logger) -> None
    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))

    session_logs_uploader.async_upload_with_fallback(feed.session_dir, test_files)
    session_logs_uploader.wait_async_uploading_with_fallback(60)

    not_found_uploads = session_logs_uploader.get_not_found_uploads()
    assert len(not_found_uploads) == len(test_files)

    not_found_files = []
    for not_found_upload in not_found_uploads:
        assert not_found_upload.status is UploadStatus.NOT_FOUND
        not_found_files.append(not_found_upload.config.file_name)
    for file_to_upload in test_files:
        assert file_to_upload.file_name in not_found_files


def test__upload_remaining_with_fallback__ok(feed, config, s3_config, log, bucket_name, mi_type, file_content):
    # type: (FeedData, ConfigParser, StubS3Config, logging.Logger, str, str, str) -> None
    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.upload_remaining_with_fallback(feed.session_dir, test_files)

    failed_files = session_logs_uploader.get_failed_uploads()
    assert not failed_files

    files_in_bucket = s3_config.s3_bucket_emulation.list_files(bucket_name, path=None)
    assert len(files_in_bucket) == len(test_files)
    for file_to_upload in test_files:
        file_name = file_to_upload.file_name
        assert os.path.exists(os.path.join(feed.session_dir, file_name))

        if file_to_upload.need_compress:
            file_name += DEFAULT_EXTENSION_FOR_COMPRESSED_FILE
        target_key_path = get_mds_path_for_file(mi_type, feed, file_name)
        assert target_key_path in files_in_bucket

        file_content_in_s3 = s3_client.read(bucket_name, target_key_path)
        if file_to_upload.need_compress:
            file_content_in_s3 = decompress_string(file_content_in_s3)

        assert six.ensure_str(file_content_in_s3) == file_content


def test__upload_remaining_with_fallback__exception_while_uploading__fs_fallback_is_done(
        feed, config, log, file_content, s3_client_with_exception):
    # type: (FeedData, ConfigParser, logging.Logger, str, StubS3Client) -> None
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client_with_exception, log)
    test_files = (
        FileToUpload('test1', True, skip_fs_fallback=False),
        FileToUpload('test2', False, skip_fs_fallback=True)
    )
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.upload_remaining_with_fallback(feed.session_dir, test_files)

    for file_to_upload in test_files:
        fallback_file_full_path = get_fallback_file_full_path(feed, file_to_upload)
        assert file_to_upload.skip_fs_fallback or os.path.exists(fallback_file_full_path)


def test__upload_remaining_with_fallback__file_not_present__file_skipped(feed, config, s3_config, log):
    # type: (FeedData, ConfigParser, StubS3Config, logging.Logger) -> None
    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client, log)
    test_files = (FileToUpload('test1', True), FileToUpload('test2', False))

    session_logs_uploader.upload_remaining_with_fallback(feed.session_dir, test_files)

    not_found_uploads = session_logs_uploader.get_not_found_uploads()
    assert len(not_found_uploads) == len(test_files)

    not_found_files = []
    for not_found_upload in not_found_uploads:
        assert not_found_upload.status is UploadStatus.NOT_FOUND
        not_found_files.append(not_found_upload.config.file_name)
    for file_to_upload in test_files:
        assert file_to_upload.file_name in not_found_files


def test__upload_remaining_with_fallback__file_already_processed__file_skipped(
        monkeypatch, feed, config, log, file_content, s3_client_with_exception):
    # type: (MonkeyPatch, FeedData, ConfigParser, logging.Logger, str, StubS3Client) -> None
    session_logs_uploader = get_session_logs_uploader(feed, config, s3_client_with_exception, log)
    test_files = (
        FileToUpload('test1', True, skip_fs_fallback=False),
        FileToUpload('test2', False, skip_fs_fallback=True)
    )
    create_test_files(feed.session_dir, test_files, file_content)

    session_logs_uploader.upload_remaining_with_fallback(feed.session_dir, test_files)

    monkeypatch.setattr(
        'market.idx.datacamp.parser.lib.parser_engine.session_logs_uploader.SessionLogsUploader._upload_file_with_fallback',
        lambda _, file_config: raise_(Exception('This code should not be executed'))
    )

    session_logs_uploader.upload_remaining_with_fallback(feed.session_dir, test_files)


def test__retry_upload_files__ok(feed, config, log, file_content, s3_config, bucket_name, mi_type):
    # type: (FeedData, ConfigParser, logging.Logger, str, StubS3Config, str, str) -> None
    test_files = (
        FileToUpload('test1', True),
        FileToUpload('test2', False)
    )
    create_test_files(feed.session_dir, test_files, file_content)

    s3_client = StubS3Client(s3_config, log)
    session_logs_uploader = SessionLogsUploader(config, s3_client=s3_client, log=log)

    for file_config in test_files:
        id = None
        catalog_name = None
        if feed.business_id:
            id = feed.business_id
            catalog_name = 'business'
        else:
            id = feed.feed_id
            catalog_name = ''

        upload_info = UploadInfo(file_config, id, catalog_name, feed.session_name, feed.session_dir)
        session_logs_uploader._copy_to_fallback_dir(upload_info)

    session_logs_uploader.retry_upload_files(test_files)

    files_in_bucket = s3_config.s3_bucket_emulation.list_files(bucket_name, path=None)
    assert len(files_in_bucket) == len(test_files)
    for file_to_upload in test_files:
        file_name = file_to_upload.file_name_in_s3
        target_key_path = get_mds_path_for_file(mi_type, feed, file_name)
        assert target_key_path in files_in_bucket

        file_content_in_s3 = s3_client.read(bucket_name, target_key_path)
        if file_to_upload.need_compress:
            file_content_in_s3 = decompress_string(file_content_in_s3)

        assert six.ensure_str(file_content_in_s3) == file_content
