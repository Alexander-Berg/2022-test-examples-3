import pytest
from fan.utils.emails import get_domain, get_local_part, is_valid, is_valid_localpart


@pytest.mark.parametrize(
    "email",
    [
        "a@example.com",
        (64 * "a") + "@example.com",
        "aBc@example.com",
        "аБв@example.com",
    ],
)
def test_valid_email(email):
    assert is_valid(email) == True


@pytest.mark.parametrize(
    "email",
    [
        "",
        "@example.com",
        (65 * "a") + "@example.com",
        "localpart@.com",
    ],
)
def test_invalid_email(email):
    assert is_valid(email) == False


@pytest.mark.parametrize(
    "localpart",
    [
        "a",
        64 * "a",
        "aBc",
        "аБв",
    ],
)
def test_valid_localpart(localpart):
    assert is_valid_localpart(localpart) == True


@pytest.mark.parametrize(
    "localpart",
    [
        "",
        65 * "a",
    ],
)
def test_invalid_localpart(localpart):
    assert is_valid_localpart(localpart) == False


@pytest.mark.parametrize(
    "email, expected",
    [
        ("", ""),
        ("good@email.com", "good"),
        ("word", "word"),
        ("@", ""),
        ("login@", "login"),
        ("@domain", ""),
        ("@@", ""),
        ("a@b@c", "a"),
        ("Upper@Case", "Upper"),
    ],
)
def test_get_local_part(email, expected):
    assert get_local_part(email) == expected


@pytest.mark.parametrize(
    "email, expected",
    [
        ("", ""),
        ("good@email.com", "email.com"),
        ("word", ""),
        ("@", ""),
        ("login@", ""),
        ("@domain", "domain"),
        ("@@", ""),
        ("a@b@c", "c"),
        ("Upper@Case", "case"),
    ],
)
def test_get_domain(email, expected):
    assert get_domain(email) == expected
