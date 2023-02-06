from StringIO import StringIO  # noqa

from typing import Union, List  # noqa

from logradaptor import read_logrotate_status, find_gz_files, find_improperly_compressed_gz_files


def test_read_logrotate_status(logrotate_status, logrotate_status_file_paths):
    # type: (Union[file, StringIO], List[unicode]) -> None
    actual = read_logrotate_status(logrotate_status)
    assert sorted(actual) == sorted(logrotate_status_file_paths)


def test_find_gz_files(logrotate_status_file_paths, gz_file_paths):
    # type: (List[unicode], List[unicode]) -> None
    actual = find_gz_files(logrotate_status_file_paths)
    assert sorted(actual) == sorted(gz_file_paths)


def test_find_gz_files_with_gz(logrotate_status_file_paths_with_gz):
    # type: (List[unicode]) -> None
    actual = find_gz_files(logrotate_status_file_paths_with_gz)
    assert len(actual) == 1


def test_find_improperly_compressed_gz_files(gz_file_paths, improperly_compressed_gz_files):
    # type: (List[unicode], List[unicode]) -> None
    actual = find_improperly_compressed_gz_files(gz_file_paths)
    assert sorted(actual) == sorted(improperly_compressed_gz_files)
