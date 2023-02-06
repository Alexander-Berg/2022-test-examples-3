import re

import pytest

from sendr_utils import get_subclasses


@pytest.mark.parametrize('pattern', (
    None,
    'somethingsomethingsomething'
))
def test_get_subclasses(pattern):
    class A:
        pass

    class B(A):
        pass

    class C(A):
        pass

    class X:
        pass

    assert set(get_subclasses(A, pattern)) == set([B, C])


def test_get_subclasses_nested():
    class A:
        pass

    class B(A):
        pass

    class C(A):
        pass

    class D(C):
        pass

    assert set(get_subclasses(A)) == set([B, C, D])


@pytest.mark.parametrize('pattern', (
    re.compile(r'.*\.test_.*'),
    re.compile(r'\.test_'),
    '.test_',
))
def test_get_subclasses__when_filter_matches(pattern):
    class A:
        pass

    class B(A):
        pass

    class C(A):
        pass

    assert set(get_subclasses(A, pattern)) == set()
