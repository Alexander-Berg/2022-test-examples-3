import datetime
import logging
import os
import tarfile

import yatest.common

from crypta.dmp.yandex.bin.common.python import (
    config_fields,
    errors_schema
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.ftp.client.ftp_client import FtpClient
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tests
)


FTP_USER = "dmp-test"
FTP_PASSWORD = "test"
FTP_HOST = "localhost"

RAW_SEGMENTS_DIR = "//dmp/dmp-test/raw_segments"
FTP_BACKUP_DIR = "//dmp/dmp-test/backup"
QUARANTINE_DIR = "//dmp/dmp-test/quarantine/new"
BACKUP_TTL_DAYS = 1

logger = logging.getLogger(__name__)


def execute_binary(ftp_server, yt_stuff, delete_files):
    config = get_config(ftp_server, yt_stuff, delete_files)
    ftp_client = FtpClient(FTP_HOST, ftp_server.port, FTP_USER, FTP_PASSWORD)

    diff = tests.Diff()

    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/ftp_to_yt/bin/crypta-dmp-yandex-ftp-to-yt"),
        args=["--config", yaml_config.dump(config)],
        env={
            "CRYPTA_DMP_FTP_PASSWORD": FTP_PASSWORD,
            time_utils.CRYPTA_FROZEN_TIME_ENV: "1500000000"
        },
        output_tables=[
            (cypress.CypressNode(QUARANTINE_DIR), tests.TestNodesInMapNodeChildren([tests.SchemaEquals(errors_schema.get()), diff], tag="quarantine")),
            (cypress.CypressNode(FTP_BACKUP_DIR), tests.TestNodesInMapNode([tests.ExpirationTime(datetime.timedelta(days=BACKUP_TTL_DAYS))], tag="backup")),
            (cypress.CypressNode(RAW_SEGMENTS_DIR), tests.TestNodesInMapNodeChildren([diff], tag="fresh"))
        ]
    )
    result = {os.path.basename(output_file["file"]["uri"]): output_file for output_file in output_files}
    result["backup"] = yt_stuff.yt_wrapper.list(FTP_BACKUP_DIR, absolute=False, sort=True)
    result["ftp"] = sorted(ftp_client.nlst(''))
    if yt_stuff.yt_wrapper.exists(RAW_SEGMENTS_DIR):
        result["raw_segments"] = yt_stuff.yt_wrapper.list(RAW_SEGMENTS_DIR, absolute=False, sort=True)
    return result


def get_config(ftp_server, yt_stuff, delete_files):
    return {
        config_fields.FTP_HOST: FTP_HOST,
        config_fields.FTP_PORT: ftp_server.port,
        config_fields.FTP_USER: FTP_USER,
        config_fields.FTP_DIR: "/",
        config_fields.FTP_DELETE_FILES: delete_files,
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.FTP_BACKUP_DIR: FTP_BACKUP_DIR,
        config_fields.FTP_BACKUP_TTL_DAYS: BACKUP_TTL_DAYS,
        config_fields.RAW_SEGMENTS_DIR: RAW_SEGMENTS_DIR,
        config_fields.QUARANTINE_DIR: QUARANTINE_DIR
    }


def upload_archives(test_name, ftp_server):
    ftp_client = FtpClient(FTP_HOST, ftp_server.port, FTP_USER, FTP_PASSWORD)
    test_dir = yatest.common.test_source_path(os.path.join("data", test_name))
    archives = os.listdir(test_dir)
    for archive in archives:
        archive_path = os.path.join(test_dir, archive)
        if os.path.isfile(archive_path):
            ftp_client.upload(archive_path, archive)
        elif os.path.isdir(archive_path):
            with tarfile.open(archive, "w|gz") as f:
                for filename in os.listdir(archive_path):
                    f.add(os.path.join(archive_path, filename), arcname=filename)
            ftp_client.upload(archive, archive)
            os.remove(archive)
    logging.info("FTP dir contains %s", ftp_client.nlst(""))
