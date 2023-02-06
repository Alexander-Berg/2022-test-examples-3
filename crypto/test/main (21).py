import pytest

from crypta.lib.python.ftp.testing_server import FtpTestingServer

import helpers


@pytest.fixture(scope="function")
def ftp_server():
    return FtpTestingServer([(helpers.FTP_USER, helpers.FTP_PASSWORD)])


def test_not_delete_files(ftp_server, yt_stuff):
    with ftp_server:
        helpers.upload_archives("one_valid_archive", ftp_server)
        return helpers.execute_binary(ftp_server, yt_stuff, delete_files=False)


@pytest.mark.parametrize("test_name", [
    "archives_with_invalid_names",
    "bindings_only",
    "bindings_with_invalid_format",
    "bindings_with_unknown_segments",
    "bindings_without_segments_for_one_id",
    "broken_archive",
    "empty_archive",
    "empty_meta",
    "meta_only",
    "meta_utf_bom",
    "meta_with_invalid_json",
    "meta_with_invalid_segments",
    "mixed_valid_and_invalid_archives",
    "one_valid_archive",
    "two_valid_archives",
    "unexpected_files",
    "unexpected_subdir",
    "valid_and_incomplete",
])
def test_zero_rc(test_name, ftp_server, yt_stuff):
    with ftp_server:
        helpers.upload_archives(test_name, ftp_server)
        return helpers.execute_binary(ftp_server, yt_stuff, delete_files=True)


def test_duplicate_archive(ftp_server, yt_stuff):
    with ftp_server:
        helpers.upload_archives("one_valid_archive", ftp_server)
        helpers.execute_binary(ftp_server, yt_stuff, delete_files=False)
        return helpers.execute_binary(ftp_server, yt_stuff, delete_files=True)
