import datetime
import logging
import os

import pytest
import yatest.common
from yt import yson
import yt.wrapper as yt

from crypta.dmp.adobe.bin.transfer_bindings_to_yt.lib import transfer
from crypta.lib.python import time_utils
from crypta.lib.python.logging import logging_helpers
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests
)

import boto_mock


RAW_BINDINGS_DIR = "//adobe/raw_ext_id_bindings"
DESTINATION_ID = 1
BACKUP_DIR = "//adobe/backup"
BACKUP_TTL = datetime.timedelta(days=1)


def get_output_tables(yt_client):
    for tablename in yt_client.list(RAW_BINDINGS_DIR, absolute=False):
        local_path = yatest.common.test_output_path("{}.yson".format(tablename))
        yt_path = yt.ypath_join(RAW_BINDINGS_DIR, tablename)
        yield tables.YsonTable(local_path, yt_path, yson_format="pretty")


def get_result(yt_client, bucket):
    output_files = []
    diff = tests.Diff()
    output_tables = list(get_output_tables(yt_client))
    for table in output_tables:
        output_files += diff.teardown(table, yt_client)
    result = {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}
    result["attributes"] = {}
    for table in output_tables:
        result["attributes"][os.path.basename(table.file_path)] = table.get_attr_from_local("upload", yt_client)
    result["backup"] = sorted(yt_client.list(BACKUP_DIR, absolute=False))
    result["s3_bucket"] = sorted(key.name for key in bucket)
    return result


def check_result(result, yt_client):
    for attributes in result["attributes"].values():
        assert isinstance(attributes["timestamp"], yson.YsonUint64)

    for filename in result["backup"]:
        tests.ExpirationTime(ttl=BACKUP_TTL).teardown(files.YtFile(None, yt.ypath_join(BACKUP_DIR, filename)), yt_client)


@pytest.mark.parametrize("dirname,delete_s3_files", [
    ("Mix", True),
    ("Mix", False)
], ids=[
    "Basic_test_with_processed_files_removing",
    "Basic_test_without_processed_files_removing"
])
def test_transfer(yt_stuff, dirname, delete_s3_files):
    yt_client = yt_stuff.get_yt_client()
    logging_helpers.configure_stdout_logger(logging.getLogger())
    bucket = boto_mock.get_bucket_from_dir(yatest.common.test_source_path(os.path.join("data", dirname)))
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1500000000"
    transfer.run(bucket, yt_client, DESTINATION_ID, RAW_BINDINGS_DIR, BACKUP_DIR, BACKUP_TTL, delete_s3_files)
    result = get_result(yt_client, bucket)
    check_result(result, yt_client)
    return result
