import pytest

from edera import coroutine


def test_coroutine_can_be_created_from_function():

    @coroutine
    def some_function(cc):
        invoked[0] = cc is not None

    invoked = [False]
    some_function()
    assert invoked[0]


def test_coroutine_can_be_created_from_generator_function():

    @coroutine
    def some_generator_function(cc):
        yield
        invoked[0] = cc is not None

    invoked = [False]
    some_generator_function()
    assert invoked[0]


def test_coroutine_context_is_ignored_by_simple_function():

    @coroutine
    def some_function(cc):
        invoked[0] = cc is not None

    def poller():
        raise RuntimeError

    invoked = [False]
    some_function[poller]()
    assert invoked[0]


def test_coroutine_invokes_pollers_on_yield():

    @coroutine
    def some_generator_function(cc, count):
        try:
            for _ in range(count):
                yield
        except RuntimeError:
            pass

    def poller():
        counter[0] += 1
        if counter[0] == 3:
            raise RuntimeError

    counter = [0]
    some_generator_function[poller](5)
    assert counter[0] == 3


def test_coroutine_calls_can_be_nested():

    @coroutine
    def routine(cc, count):
        for index in range(count):
            nested_routine[cc](index)

    @coroutine
    def nested_routine(cc, count):
        for _ in range(count):
            yield

    def poller():
        counter[0] += 1

    counter = [0]
    routine[poller](5)
    assert counter[0] == 10


def test_coroutine_context_can_be_extended():

    @coroutine
    def routine(cc, count):
        for index in range(count):
            nested_routine[cc + additional_poller](index)

    @coroutine
    def nested_routine(cc, count):
        for _ in range(count):
            yield

    def poller():
        counter[0] += 1

    def additional_poller():
        counter[0] -= 2

    counter = [0]
    routine[poller](5)
    assert counter[0] == -10


def test_coroutine_context_can_be_temporarily_extended():

    @coroutine
    def routine(cc, count):
        for index in range(count):
            with cc.extend(additional_poller):
                nested_routine[cc](index)

    @coroutine
    def nested_routine(cc, count):
        for _ in range(count):
            yield

    def poller():
        counter[0] += 1

    def additional_poller():
        counter[0] -= 2

    counter = [0]
    routine[poller](5)
    assert counter[0] == -10


def test_coroutine_context_can_embrace_coroutines_and_regular_functions():

    @coroutine
    def routine(cc):
        cc.embrace(nested_routine)()

    @coroutine
    def nested_routine(cc):
        cc.embrace(function)()

    def function():
        pass

    def poller():
        pass

    routine[poller]()
