from typing import Any, Dict, Callable
from inspect import isgenerator
from functools import WRAPPER_ASSIGNMENTS, partial
from unittest import mock

import pytest

from tractor_disk.disk_error import catch_errors


_SAMPLES: Dict[str, Any] = {
    "class": object,
    "nothing": None,
    "numeric": 0,
    "sequence": [],
    "function": lambda: None,
    "exception": Exception(),
}


class _RegularAndGeneratorMethods:
    @partial(catch_errors, dispatch_error=lambda exc: None)
    def return_nothing(self):
        return

    @partial(catch_errors, dispatch_error=lambda exc: None)
    def return_generator(self):
        return (i for i in range(0))

    @partial(catch_errors, dispatch_error=lambda exc: None)
    def generate(self):
        yield


def test_metadata_preserved():
    def original(self, p, /, rd="rd", *, kad: str = "kad") -> int:
        """Accept 3 parameters: pos-only, regular w/ a default, annotated kw-only w/ a default."""
        return 0

    decorated = catch_errors(original, lambda exc: None)
    for attr in WRAPPER_ASSIGNMENTS:
        assert getattr(decorated, attr) is getattr(original, attr)


def test_preserves_generatorness_of_result():
    assert isgenerator(_RegularAndGeneratorMethods().return_nothing()) is False
    assert isgenerator(_RegularAndGeneratorMethods().return_generator()) is True
    assert isgenerator(_RegularAndGeneratorMethods().generate()) is True


@pytest.mark.parametrize("kwargs", [{}, {"abc": 456}], ids=str)
@pytest.mark.parametrize("args", [(), (123,)], ids=str)
def test_forwards_arguments(args: tuple, kwargs: Dict[str, Any]):
    method_mock = mock.Mock()
    dispatch_error_mock = mock.Mock()
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    instance.method(*args, **kwargs)
    assert method_mock.mock_calls == [mock.call(instance, *args, **kwargs)]
    assert dispatch_error_mock.mock_calls == []


@pytest.mark.parametrize("return_value", _SAMPLES.values(), ids=_SAMPLES.keys())
def test_forwards_result(return_value):
    method_mock = mock.Mock(return_value=return_value)
    dispatch_error_mock = mock.Mock()
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    result = instance.method()
    assert result is return_value
    assert dispatch_error_mock.mock_calls == []


def test_forwards_dispatch_exception_on_failure():
    method_exception = RuntimeError("method")
    dispatch_error_exception = RuntimeError("dispatch_error")
    method_mock = mock.Mock(side_effect=method_exception)
    dispatch_error_mock = mock.Mock(side_effect=dispatch_error_exception)
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    with pytest.raises(RuntimeError) as raised:
        instance.method()
    assert dispatch_error_mock.mock_calls == [mock.call(method_exception)]
    assert raised.value is dispatch_error_exception


@pytest.mark.parametrize("kwargs", [{}, {"abc": 456}], ids=str)
@pytest.mark.parametrize("args", [(), (123,)], ids=str)
def test_forwards_generator_arguments(args: tuple, kwargs: Dict[str, Any]):
    method_mock = mock.Mock(return_value=[])
    dispatch_error_mock = mock.Mock()
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    list(instance.generator_method(*args, **kwargs))
    assert method_mock.mock_calls == [mock.call(instance, *args, **kwargs)]
    assert dispatch_error_mock.mock_calls == []


@pytest.mark.parametrize("yield_value", _SAMPLES.values(), ids=_SAMPLES.keys())
def test_forwards_generator_yield_value(yield_value):
    method_mock = mock.Mock(return_value=[yield_value])
    dispatch_error_mock = mock.Mock()
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    result = list(instance.generator_method())
    assert len(result) == 1
    assert result[0] is yield_value
    assert dispatch_error_mock.mock_calls == []


def test_forwards_dispatch_exception_on_generator_failure():
    method_exception = RuntimeError("method")
    dispatch_error_exception = RuntimeError("dispatch_error")
    method_mock = mock.Mock(return_value=[None])
    dispatch_error_mock = mock.Mock(side_effect=dispatch_error_exception)
    instance = _spawn_instance_with_decorated_methods(method_mock, dispatch_error_mock)
    generator = instance.generator_method()
    next(generator)
    with pytest.raises(RuntimeError) as raised:
        generator.throw(method_exception)
    assert dispatch_error_mock.mock_calls == [mock.call(method_exception)]
    assert raised.value is dispatch_error_exception


def _spawn_instance_with_decorated_methods(
    method: Callable, dispatch_error: Callable[[Exception], Any]
):
    class Class:
        @partial(catch_errors, dispatch_error=dispatch_error)
        def method(self, *args, **kwargs):
            return method(self, *args, **kwargs)

        @partial(catch_errors, dispatch_error=dispatch_error)
        def generator_method(self, *args, **kwargs):
            return (yield from method(self, *args, **kwargs))

    return Class()
