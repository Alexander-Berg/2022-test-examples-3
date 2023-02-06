import pytest

from fan.testutils.utils import rndstr
from fan.links.unsubscribe import (
    decode_unsubscribe_code2,
    encode_unsubscribe_code2,
    get_unsubscribe_link,
)

pytestmark = pytest.mark.django_db


def test_unsubscribe_link_encript():
    data = ("a@b.c", 42, 43, rndstr())

    assert data + (False,) == decode_unsubscribe_code2(encode_unsubscribe_code2(*data))


def test_unsubscribe_test_link_encript():
    data = ("a@b.c", 42, 43, rndstr())

    assert data + (True,) == decode_unsubscribe_code2(
        encode_unsubscribe_code2(*data, for_testing=True)
    )


def test_unsubscribe_link():
    assert get_unsubscribe_link(secret_code="XXX")
