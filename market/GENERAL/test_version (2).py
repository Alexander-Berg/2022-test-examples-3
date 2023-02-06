# coding: utf-8

from version import debian_version
from hamcrest import assert_that


def test_version():
    assert_that(debian_version())
