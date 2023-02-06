# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
import mock

from travel.library.python.rasp_vault import api
from travel.library.python.rasp_vault.api import _retry_on_exception


def test_retry_on_exception_success():
    def f(a):
        return a + 1

    assert _retry_on_exception(f, args=(10,)) == 11


def test_retry_on_exception_fail():
    class SomeError(Exception):
        pass

    def f(a):
        raise SomeError

    with mock.patch.object(api, 'sleep') as m_sleep, \
            pytest.raises(SomeError):
        _retry_on_exception(f, args=(10,))

    m_sleep.assert_has_calls([mock.call(1), mock.call(2), mock.call(4), mock.call(8)])


@pytest.mark.parametrize('args, kwargs, call', [
    ((1,), {}, mock.call(1)),
    ((), {}, mock.call()),
    ((), {'a': 1}, mock.call(a=1)),
    ((3, 4), {'a': 1}, mock.call(3, 4, a=1)),
])
def test_call_variants(args, kwargs, call):
    func = mock.Mock()
    _retry_on_exception(func, args=args, kwargs=kwargs)
    assert func.call_args == call
