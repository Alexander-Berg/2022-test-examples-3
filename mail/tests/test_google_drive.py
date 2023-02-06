from tractor_disk.google_drive import (
    GoogleBuildPathMappingOp,
    _dispatch_disk_error,
    SHARED_WITH_ME_DIR,
    FOLDER_MIME_TYPE,
    USER_NOT_FOUND_MSG,
)
from tractor_disk.disk_error import ExternalUserNotFound
from google.auth.exceptions import RefreshError
from pytest import fixture, mark, raises
from copy import deepcopy
import re

ROOT_FOLDER_ID = "root_folder_id"
GOOGLE_DOC_EXT = ".docx"
NON_GOOGLE_EXT = ".ext"
ID_HASH_REGEXP = r"(\d|[a-f]){10}"
TEST_FILE_NAME = "test_file"
ID_COUNTER = 0


@fixture
def file_without_ext():
    return make_file(TEST_FILE_NAME, mimetype="application/custom")


@fixture
def file_with_ext():
    return make_file(TEST_FILE_NAME + NON_GOOGLE_EXT, mimetype="application/custom")


@fixture
def google_doc_without_ext():
    return make_file(TEST_FILE_NAME, mimetype="application/vnd.google-apps.document")


@fixture
def google_doc_with_ext():
    return make_file(
        TEST_FILE_NAME + GOOGLE_DOC_EXT, mimetype="application/vnd.google-apps.document"
    )


@fixture
def folder():
    return make_file("test_folder", mimetype=FOLDER_MIME_TYPE)


def make_file(
    name=TEST_FILE_NAME, mimetype="application/custom", parents=[ROOT_FOLDER_ID], shared=False
):
    return {
        "id": gen_id(),
        "name": name,
        "mimeType": mimetype,
        "parents": parents,
        "shared": shared,
        "root_folder_id": ROOT_FOLDER_ID,
    }


def gen_id():
    global ID_COUNTER
    ID_COUNTER += 1
    return str(ID_COUNTER)


def test_build_path_for_file_without_ext(file_without_ext):
    mapping = build_mapping([file_without_ext])
    assert mapping[file_without_ext["id"]] == file_without_ext["name"]


def test_dont_touch_non_google_extentions(file_with_ext):
    assert path_after_mapping(file_with_ext) == file_with_ext["name"]


def test_add_ext_for_google_file_without_ext(google_doc_without_ext):
    assert (
        path_after_mapping(google_doc_without_ext)
        == google_doc_without_ext["name"] + GOOGLE_DOC_EXT
    )


def test_dont_touch_ext_for_google_file_if_already_exists(google_doc_with_ext):
    assert path_after_mapping(google_doc_with_ext) == google_doc_with_ext["name"]


def test_build_path_for_file_inside_folder(file_without_ext, folder):
    file_without_ext["parents"] = [folder["id"]]
    mapping = build_mapping([file_without_ext, folder])
    assert mapping[file_without_ext["id"]] == f'{folder["name"]}/{file_without_ext["name"]}'


def test_build_path_for_shared_file(file_without_ext):
    file_without_ext["shared"] = True
    del file_without_ext["parents"]
    mapping = build_mapping([file_without_ext])
    assert mapping[file_without_ext["id"]] == f'{SHARED_WITH_ME_DIR}/{file_without_ext["name"]}'


@mark.parametrize(
    "mimetype, original_ext, resulting_ext",
    [
        ("text/plain", "", ""),
        ("text/plain", NON_GOOGLE_EXT, NON_GOOGLE_EXT),
        ("application/vnd.google-apps.document", "", GOOGLE_DOC_EXT),
        ("application/vnd.google-apps.document", GOOGLE_DOC_EXT, GOOGLE_DOC_EXT),
    ],
)
def test_equally_named_files_resolved_correctly_by_appending_id(
    mimetype, original_ext, resulting_ext
):
    pattern = r"{name} \({id_hash}\){ext}".format(
        name=TEST_FILE_NAME,
        id_hash=ID_HASH_REGEXP,
        ext=re.escape(resulting_ext),
    )
    files = make_list_of_files_with_equal_path(
        make_file(TEST_FILE_NAME + original_ext, mimetype=mimetype)
    )
    mapping = build_mapping(files)
    assert_paths_unique(mapping)
    assert_paths_match_pattern(mapping, pattern)


def test_mapping_is_deterministic(file_without_ext):
    files_with_equal_path = make_list_of_files_with_equal_path(file_without_ext)

    first_mapping = build_mapping(files_with_equal_path)
    second_mapping = build_mapping(files_with_equal_path)

    assert first_mapping == second_mapping


def test_dispatch_user_not_found_error_by_message():
    err = RefreshError(USER_NOT_FOUND_MSG)
    with raises(ExternalUserNotFound):
        _dispatch_disk_error(err)


def test_dispatch_user_not_found_error_by_message_inside_tuple():
    err = RefreshError(USER_NOT_FOUND_MSG, {})
    with raises(ExternalUserNotFound):
        _dispatch_disk_error(err)


def path_after_mapping(file):
    mapping = build_mapping([file])
    return mapping[file["id"]]


def build_mapping(file_list: list):
    op = GoogleBuildPathMappingOp(file_list)
    mapping, _ = op()
    return mapping


def make_list_of_files_with_equal_path(file):
    files = [deepcopy(file) for i in range(5)]
    for idx, file in enumerate(files):
        file["id"] += str(idx)
    return files


def assert_paths_unique(mapping):
    assert len(mapping.values()) == len(set(mapping.values()))


def assert_paths_match_pattern(mapping, pattern):
    for path in mapping.values():
        assert re.fullmatch(pattern, path) is not None
