# -*- coding: utf-8 -*-

import os
import pytest
from hamcrest import assert_that, is_, equal_to

from market.pylibrary.config_templator.generate_config import (
    generate,
    load_substitutions,
)
from six.moves.configparser import ConfigParser

import yatest.common


@pytest.fixture()
def template_path():
    return yatest.common.source_path(
        'market/pylibrary/config_templator/tests/data/templates/template'
    )


@pytest.fixture()
def srcdir(template_path):
    return os.path.dirname(template_path)


@pytest.fixture()
def generated_name(template_path):
    return os.path.basename(template_path)


@pytest.fixture()
def dstdir(tmpdir_factory):
    return str(tmpdir_factory.mktemp('dstdir'))


@pytest.fixture()
def substitutions_path():
    return yatest.common.source_path('market/pylibrary/config_templator/tests/data/substitutions')


def read_config(path):
    config = ConfigParser()
    config.read(path)
    return {
        section: dict(config.items(section))
        for section in config.sections()
    }


def test_load_substitutions(substitutions_path):
    actual = load_substitutions(substitutions_path)
    expected = {
        'a': {
            'c': '1',
            'b': 'lalala',
        },
        'd': {
            'e': 'test',
        },
    }
    assert_that(actual, is_(equal_to(expected)))


def test_generate(srcdir, dstdir, substitutions_path, generated_name):
    substitutions = load_substitutions(substitutions_path)
    generate(srcdir, dstdir, substitutions)
    generated_path = os.path.join(dstdir, generated_name)
    assert os.path.exists(generated_path)

    actual = read_config(generated_path)
    expected = {
        'main': {
            'y': '1',
            'x': 'lalala',
        },
        'other': {
            'z': 'some/test',
        },
    }
    assert_that(actual, is_(equal_to(expected)))
