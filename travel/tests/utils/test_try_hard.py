# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.utils.try_hard import try_hard


class _TestException(Exception):
    pass


def create_test_func(m, *args, **kwargs):
    @try_hard(*args, **kwargs)
    def mock_wrapper(*m_args, **m_kwargs):
        return m(*m_args, **m_kwargs)
    return mock_wrapper


@mock.patch('common.utils.try_hard.sleep')
def test_ok(m_sleep):
    func_mock = mock.Mock(side_effect=[_TestException(''), 1])
    decorated = create_test_func(func_mock, retriable_exceptions=(_TestException,))
    assert decorated() == 1


@mock.patch('common.utils.try_hard.sleep')
def test_catching_exception_if_no_retriable_exceptions_passed(m_sleep):
    func_mock = mock.Mock(side_effect=[_TestException(''), 1])
    decorated = create_test_func(func_mock)
    assert decorated() == 1


@mock.patch('common.utils.try_hard.sleep')
def test_reraising_exception_if_retries_limit_exceeded(m_sleep):
    func_mock = mock.Mock(side_effect=[_TestException('')] * 5)
    with pytest.raises(_TestException):
        decorated = create_test_func(func_mock, retriable_exceptions=(_TestException,))
        decorated()


@mock.patch('common.utils.try_hard.sleep')
def test_do_not_catch_not_specified_exception(m_sleep):
    func_mock = mock.Mock(side_effect=[Exception(''), 1])
    with pytest.raises(Exception):
        decorated = create_test_func(func_mock, retriable_exceptions=(_TestException,))
        decorated()


@mock.patch('common.utils.try_hard.sleep')
def test_callback_on_exception(m_sleep):
    test_exc = _TestException('')
    func_mock = mock.Mock(side_effect=[test_exc, test_exc, 1])
    m_on_exception = mock.Mock()

    decorated = create_test_func(
        func_mock,
        max_retries=3,
        retriable_exceptions=(_TestException,),
        on_exception=m_on_exception
    )

    assert decorated(42, 43, foo=44) == 1
    assert m_on_exception.call_args_list == [
        mock.call(test_exc, 2, args=(42, 43), kwargs={'foo': 44}),
        mock.call(test_exc, 1, args=(42, 43), kwargs={'foo': 44}),
    ]
