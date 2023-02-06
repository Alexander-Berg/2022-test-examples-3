# -*- coding: utf-8 -*-
import pytest

from feature_flag_client import Context
from feature_flag_client.ab_content import ABFlagsContent
from tests.utils import flag_test_set


@pytest.fixture()
def old_context():
    return Context(['TEST1', 'TEST2'])


@pytest.fixture()
def ab_context():
    return Context(['TEST1', 'TEST2'], ['AB_TEST1', 'AB_TEST2'])


def test_creation_old_context(old_context):
    assert old_context is not None


def test_flag_enabled(old_context):
    assert old_context.flag_enabled('TEST1')
    assert not old_context.flag_enabled('TEST')


def test_creation_context_with_ab(ab_context):
    assert ab_context is not None


def test_use_ab(ab_context):
    assert ab_context.use_ab('AB_TEST1')
    assert not ab_context.use_ab('NOT_AB')


@flag_test_set
def test_logic_of_ab_flags(ab_context, key, value, expected):
    ab_content = ABFlagsContent({key: value})
    assert expected == ab_context.flag_enabled(key, ab_content)
