import os.path

import pytest
import six

import yamarec1.config.loader

from yamarec1.config.exceptions import ConfigLoadingError


def test_config_can_be_loaded_from_string():
    config = yamarec1.config.loader.load_from_string("x:\n y = 1\nz = x.y + 1")
    assert config.x.y == 1
    assert config.z == 2


def test_config_can_be_loaded_from_stream():
    stream = six.StringIO("x:\n y = 1\nz = x.y + 1")
    config = yamarec1.config.loader.load_from_stream(stream)
    assert config.x.y == 1
    assert config.z == 2


def test_loading_from_stream_fails_if_stream_is_invalid(tmpdir):
    path = os.path.join(str(tmpdir), "test.config")
    with open(path, "w") as stream:
        with pytest.raises(ConfigLoadingError):
            yamarec1.config.loader.load_from_stream(stream)


def test_config_can_be_loaded_from_file(tmpdir):
    path = os.path.join(str(tmpdir), "test.config")
    with open(path, "w") as stream:
        stream.write("x:\n y = 1\nz = x.y + 1")
    config = yamarec1.config.loader.load_from_file(path)
    assert config.x.y == 1
    assert config.z == 2


def test_loading_from_nonexisting_file_fails(tmpdir):
    path = os.path.join(str(tmpdir), "test.config")
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_file(path)


def test_config_can_be_loaded_from_directory(tmpdir):
    path = str(tmpdir)
    with open(os.path.join(path, "__root__"), "w") as stream:
        stream.write("x:\n y = 1\nz = x.y + 1")
    with open(os.path.join(path, "regular.config"), "w") as stream:
        stream.write("ok = True")
    os.mkdir(os.path.join(path, "nested"))
    with open(os.path.join(path, "nested", "__root__"), "w") as stream:
        stream.write("ok = True")
    config = yamarec1.config.loader.load_from_directory(path)
    assert config.x.y == 1
    assert config.z == 2
    assert config.regular.ok is True
    assert config.nested.ok is True


def test_loading_from_directory_ignores_unmatched_subpaths(tmpdir):
    path = str(tmpdir)
    with open(os.path.join(path, "unmatched_extension.cfg"), "w") as stream:
        stream.write("ignored = True")
    with open(os.path.join(path, "unmatched-pattern.config"), "w") as stream:
        stream.write("ignored = True")
    os.mkdir(os.path.join(path, "unmatched-pattern"))
    assert not list(yamarec1.config.loader.load_from_directory(path))


def test_loading_from_nonexisting_directory_fails(tmpdir):
    path = os.path.join(str(tmpdir), "test")
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_directory(path)


def test_loading_from_directory_with_root_subfile_conflicts_fails(tmpdir):
    path = str(tmpdir)
    with open(os.path.join(path, "__root__"), "w") as stream:
        stream.write("x:\n y = 1\nz = x.y + 1")
    with open(os.path.join(path, "x.config"), "w") as stream:
        stream.write("conflict = True")
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_directory(path)


def test_loading_from_directory_with_root_subdirectory_conflicts_fails(tmpdir):
    path = str(tmpdir)
    with open(os.path.join(path, "__root__"), "w") as stream:
        stream.write("x:\n y = 1\nz = x.y + 1")
    os.mkdir(os.path.join(path, "x"))
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_directory(path)


def test_loading_from_directory_with_subfile_subdirectory_conflicts_fails(tmpdir):
    path = str(tmpdir)
    with open(os.path.join(path, "x.config"), "w") as stream:
        stream.write("conflict = True")
    os.mkdir(os.path.join(path, "x"))
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_directory(path)


def test_duplicate_entries_are_not_allowed():
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_string("y = 1\ny = 2")
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_string("x.a = 3\nx.b = 5")
    with pytest.raises(ConfigLoadingError):
        yamarec1.config.loader.load_from_string("x.y = 8\nx.y = 13")
