import pytest

from sendr_utils.schemas.one_of_schema import SchemaMappingWithFallback


def test_schema_mapping_with_fallback():
    fallback = 1
    foo_schema = 2
    manual_default = 3

    mapping = SchemaMappingWithFallback(fallback, foo=foo_schema)

    assert mapping.get('foo') == foo_schema
    assert mapping.get('bar') == fallback
    with pytest.raises(AssertionError):
        assert mapping.get('bar', manual_default)
