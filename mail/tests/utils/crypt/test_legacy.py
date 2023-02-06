import pytest
from fan.utils.crypt.legacy import Encoder


@pytest.mark.parametrize(
    "decoded,salt,expected",
    (
        ("a@ya.ru:list-1", 3257, "RGc0VEtrdzJSVWhVRXlvSGJsWT06MzI1Nzow"),
        ("a@ya.ru:list-1", 78, "VTNRYkVCWUhCV0pZSGhjWmFRWT06Nzg6MA=="),
    ),
)
def test_encode(decoded, salt, expected):
    assert Encoder().encode(decoded, salt=salt) == expected


@pytest.mark.parametrize(
    "encoded,expected",
    (
        ("RGc0VEtrdzJSVWhVRXlvSGJsWT06MzI1Nzow", "a@ya.ru:list-1"),
        ("VTNRYkVCWUhCV0pZSGhjWmFRWT06Nzg6MA==", "a@ya.ru:list-1"),
    ),
)
def test_decode(encoded, expected):
    assert Encoder().decode(encoded) == expected
