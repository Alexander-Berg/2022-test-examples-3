from yt.yson.yson_types import YsonUint64
import pytest
import lib.cache as ch


@pytest.mark.parametrize(
    "old, new, expected_result",
    [
        (
            '123123__321321',
            YsonUint64(123123),
            True
        ),
        (
            '123123__321321',
            YsonUint64(564646464),
            False
        ),
        (
            None,
            YsonUint64(564646464),
            False
        ),
        (
            '0_321321',
            YsonUint64(564646464),
            False
        ),
        (
            '123123_321321',
            YsonUint64(0),
            False
        ),
        (
            '0_321321',
            YsonUint64(0),
            False
        )
    ]
)
def test_revision_old_equal_to_new(old, new, expected_result):
    actual = ch.is_cached_revision_equal_to_new(old, new, '//test/tbl')
    assert actual == expected_result
