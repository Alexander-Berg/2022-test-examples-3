import pytest

from sendr_utils.abc import abstractclass, isabstract


def test_is_abstract():
    @abstractclass
    class A:
        pass

    assert isabstract(A)


def test_is_not_abstract():
    class A:
        pass

    assert not isabstract(A)


def test_cannot_instantiate_abstract():
    @abstractclass
    class A:
        pass

    with pytest.raises(RuntimeError) as exc_info:
        A()

    assert 'Class A is an abstract class and cannot be instantiated' in str(exc_info.value)


def test_can_instantiate_concrete_subclass_of_abstract_class():
    @abstractclass
    class A:
        pass

    class B(A):
        pass

    B()


def test_mro_not_messed_up():
    """
    abstractclass подменяет конструктор. Если у абстрактного класса не было конструктора,
    нужно правильно определять, какой конструктор вызывать следующим. Иначе этот тест свалится,
    потому что конструктор класса B не вызовется.
    """
    @abstractclass
    class A:
        pass

    class B:
        def __init__(self):
            self.x = 1

    class C(A, B):
        pass

    assert C().x
