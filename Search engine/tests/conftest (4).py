# -*- coding: utf-8 -*-
import pytest
import os


@pytest.fixture(scope="session")
def test_dir():
    return os.path.dirname(os.path.abspath(__file__))


@pytest.fixture(scope="session")
def base_dir():
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


@pytest.fixture(scope="session")
def data_dir(base_dir):
    return os.path.join(base_dir, "data")


@pytest.fixture(scope="session")
def test_data_dir(base_dir):
    return os.path.join(base_dir, "tests", "test_data")


@pytest.fixture(scope="session")
def prototypes_dir(base_dir):
    return os.path.join(base_dir, "tests", "prototypes")


@pytest.fixture(scope="session")
def test_name(request):
    return request.param


def get_parsers(dir):
    RKUB_suffix = "_rkub.txt"
    suffix_len = len(RKUB_suffix)
    rkub_files = [x for x in os.listdir(dir) if x.endswith(RKUB_suffix)]
    return [rkub_file[:-suffix_len] for rkub_file in rkub_files]


def pytest_generate_tests(metafunc):
    if 'test_name' in metafunc.fixturenames:
        metafunc.parametrize("test_name", get_parsers(prototypes_dir(base_dir())), indirect=True)
