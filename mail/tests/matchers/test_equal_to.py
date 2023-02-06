from dataclasses import dataclass, field

import pytest

from sendr_pytest.matchers.equal_to import equal_to

from hamcrest import assert_that, has_entry, match_equality


class TestMatchDicts:
    def test_missing_key(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                {'a': 1},
                equal_to({'a': 1, 'b': 2})
            )

        assert "<{'a': 1}> is missing keys <{'b'}>" in str(exc_info.value.args[0])

    def test_extra_key(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                {'a': 1, 'b': 2},
                equal_to({'a': 1})
            )

        assert "<{'a': 1, 'b': 2}> has extra keys <{'b'}>" in str(exc_info.value.args[0])

    def test_different_keys(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                {'a': 2},
                equal_to({'a': 1})
            )

        assert "value of key 'a' was <2>" in str(exc_info.value.args[0])

    def test_submatching(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                {'a': {'x': 2}},
                equal_to({'a': {'x': 1}})
            )

        assert "value of key 'a' value of key 'x' was <2>" in str(exc_info.value.args[0])

    def test_equality_matcher(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                {'a': {'x': 2}},
                equal_to({
                    'a': match_equality(
                        has_entry('x', 1)
                    )
                })
            )

        # у-у, слабовато. Надо обновить hamcrest
        assert "value of key 'a' was <{'x': 2}>" in str(exc_info.value.args[0])


class TestMatchDataclasses:
    def test_property_mismatch(self):
        @dataclass
        class DT:
            x: int

        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                DT(x=2),
                equal_to(DT(x=1)),
            )

        assert "value of property 'x' was <2>" in str(exc_info.value.args[0])

    def test_submatching(self):
        @dataclass
        class DT:
            x: dict

        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                DT(x={'a': 2}),
                equal_to(DT(x={'a': 1}))
            )

        assert "value of property 'x' value of key 'a' was <2>" in str(exc_info.value.args[0])

    def test_ignores_nocompare_properties(self):
        @dataclass
        class DT:
            x: int = field(compare=False)
            y: int

        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                DT(x=2, y=6),
                equal_to(DT(x=1, y=5)),
            )

        assert "value of property 'y' was <6>" in str(exc_info.value.args[0])


class TestMatchSequences:
    def test_length_mismatch(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                [1, 2],
                equal_to([1])
            )

        assert 'sequence expected to be of size <1> but actual sequence has size <2>' in str(exc_info.value.args[0])

    def test_value_mismatch(self):
        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                [1, 3, 3],
                equal_to([1, 2, 3]),
            )

        assert "value at index <1> was <3>" in str(exc_info.value.args[0])

    def test_submatching(self):
        @dataclass
        class DT:
            x: int

        with pytest.raises(AssertionError) as exc_info:
            assert_that(
                [DT(x=2)],
                equal_to([DT(x=1)]),
            )

        assert "value at index <0> value of property 'x' was <2>" in str(exc_info.value.args[0])
