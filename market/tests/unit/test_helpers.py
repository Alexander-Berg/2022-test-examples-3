import abc

import pytest
import six

import edera.helpers

from edera.helpers import ClassFactory
from edera.helpers import InheritorTracker
from edera.helpers import Lazy
from edera.helpers import Phony
from edera.helpers import memoized
from edera.helpers import phony


def test_functions_can_be_memoized():

    @memoized
    def fibonacci(n):
        fibonacci.calls += 1
        return 1 if n <= 2 else fibonacci(n - 1) + fibonacci(n - 2)

    fibonacci.calls = 0
    assert fibonacci(100) == 354224848179261915075
    assert fibonacci.calls == 100
    assert fibonacci(5) == 5
    assert fibonacci.calls == 100


def test_methods_can_be_memoized():

    class Fibonacci(object):

        def __init__(self):
            self.calls = 0

        @memoized
        def compute(self, n):
            self.calls += 1
            return 1 if n <= 2 else self.compute(n - 1) + self.compute(n - 2)

    fibonacci = Fibonacci()
    assert fibonacci.compute(100) == 354224848179261915075
    assert fibonacci.calls == 100
    assert fibonacci.compute(5) == 5
    assert fibonacci.calls == 100
    assert Fibonacci().compute(5) == 5
    assert fibonacci.calls == 100


def test_phony_works_as_callable_singleton():
    assert Phony() is None
    assert Phony(2) is None
    assert Phony(2, test="test") is None


def test_phony_decorator_makes_everything_phony():

    @phony
    def square(x):
        return x**2

    assert square is Phony


def test_name_squashing_works_correctly():
    assert edera.helpers.squash_names([]) == []
    assert edera.helpers.squash_names(["!@#"]) == ["!@#"]
    assert edera.helpers.squash_names(["!@#a", "!@%a"]) == ["!@#a", "!@%a"]
    assert edera.helpers.squash_names(["!@#a_b-c", "!@%x_y-z"]) == ["a_b-c", "x_y-z"]
    assert edera.helpers.squash_names(["a", "a b", "b c"]) == ["a", "a\nb", "b"]
    assert edera.helpers.squash_names(["a b c", "a b d", "a b e"]) == ["a\nc", "a\nd", "a\ne"]
    assert edera.helpers.squash_names(["a", "a b"]) == ["a", "a\nb"]
    with pytest.raises(AssertionError):
        edera.helpers.squash_names(["a", "a"])
    with pytest.raises(AssertionError):
        edera.helpers.squash_names(["a", ""])


def test_class_factory_works_correctly():

    @six.add_metaclass(abc.ABCMeta)
    class Object(object):

        @abc.abstractmethod
        def show(self):
            pass

    class InvalidFactory(ClassFactory[Object]):

        def snow(self):
            return "snow?"

    class ValidFactory(ClassFactory[Object]):

        def show(self):
            return "cargo: %s" % str(self.cargo)

    with pytest.raises(TypeError):
        InvalidFactory[1, 2, 3]()
    assert ValidFactory[1]().show() == "cargo: 1"
    assert ValidFactory[1, 2, 3]().show() == "cargo: (1, 2, 3)"


def test_lazy_delays_instantiation_and_can_be_destroyed():

    class Object(object):

        def __init__(self, x):
            collector.append(x)

    collector = []
    lazy = Lazy[Object]("so lazy")
    assert not collector
    assert isinstance(lazy.instance, Object)
    assert collector == ["so lazy"]
    lazy.destroy()
    assert lazy.instance is not None
    assert collector == ["so lazy", "so lazy"]


def test_inheritor_tracker_works_correctly():

    class Plugin(InheritorTracker):

        pass

    class FirstPlugin(Plugin):

        pass

    class SecondPlugin(Plugin):

        pass

    class ThirdPlugin(SecondPlugin):

        pass

    assert set(Plugin.inheritors) == {FirstPlugin, SecondPlugin, ThirdPlugin}
