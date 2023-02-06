from ora2pg.tools.unicode_helpers import sanitize_unicode
from pytest import raises, mark
import six


def test_for_empty_returns_equal():
    value = u''
    result = sanitize_unicode(value)
    assert result == value


def test_for_not_unicode_raises_assertion_error():
    with raises(AssertionError):
        sanitize_unicode(b'')


def test_for_valid_unicode_returns_equal():
    value = u'\u043a\u0430\u043a\u0430\u044f-\u0442\u043e \u30e9\u30a4\u30f3'
    result = sanitize_unicode(value)
    assert result == value


def test_for_unicode_with_null_returns_without_null():
    value = u'\u0000'
    result = sanitize_unicode(value)
    assert result == u''


if six.PY2:
    @mark.parametrize(('value',), [u'\ud800', u'\udbff'])
    def test_for_unicode_with_low_surrogate_returns_without_low_surrogate(value):
        result = sanitize_unicode(value)
        assert result == u''

    @mark.parametrize(('value',), [u'\udc00', u'\udfff'])
    def test_for_unicode_with_high_surrogate_returns_without_high_surrogate(value):
        result = sanitize_unicode(value)
        assert result == u''
