import datetime

import iso8601
import pytest
import six

from edera.exceptions import ValueQualificationError
from edera.qualifiers import Any
from edera.qualifiers import Boolean
from edera.qualifiers import Date
from edera.qualifiers import DateTime
from edera.qualifiers import Instance
from edera.qualifiers import Integer
from edera.qualifiers import List
from edera.qualifiers import Mapping
from edera.qualifiers import Optional
from edera.qualifiers import Set
from edera.qualifiers import String
from edera.qualifiers import Text
from edera.qualifiers import TimeDelta


def test_any_qualifier_works_correctly():
    assert Any.qualify("123").value == "123"
    assert repr(Any.qualify("123")) == repr("123")
    assert Any.qualify([1, 2, 3]).value == [1, 2, 3]
    assert repr(Any.qualify([1, 2, 3])) == repr([1, 2, 3])


def test_instance_qualifier_works_correctly():
    assert Instance[int].qualify(123).value == 123
    assert repr(Instance[int].qualify(123)) == repr(123)
    assert Instance[list].qualify([1, 2, 3]).value == [1, 2, 3]
    assert repr(Instance[list].qualify([1, 2, 3])) == repr([1, 2, 3])
    with pytest.raises(ValueQualificationError):
        Instance[list].qualify(123)
    with pytest.raises(ValueQualificationError):
        Instance[str].qualify([1, 2, 3])
    with pytest.raises(ValueQualificationError):
        Instance[int].qualify("123")


def test_integer_qualifier_works_correctly():
    assert Integer.qualify(123).value == 123
    assert repr(Integer.qualify(123)) == "123"
    assert Integer.qualify(-123).value == -123
    assert repr(Integer.qualify(-123)) == "-123"
    with pytest.raises(ValueQualificationError):
        Integer.qualify(+123.4)
    with pytest.raises(ValueQualificationError):
        Integer.qualify("123")
    with pytest.raises(ValueQualificationError):
        Integer.qualify(None)


def test_integer_qualifier_handles_longs_correctly():
    if not six.PY2:
        return
    assert Integer.qualify(long(123)).value == long(123)
    assert repr(Integer.qualify(long(123))) == "123"  # not "123L"


def test_list_qualifier_works_correctly():
    assert List[Integer].qualify([]).value == []
    assert repr(List[Integer].qualify([])) == "[]"
    assert List[Integer].qualify([1, 2, 3]).value == [1, 2, 3]
    assert repr(List[Integer].qualify([1, 2, 3])) == "[1, 2, 3]"
    assert List[List[Integer]].qualify([[1, 2], [3], []]).value == [[1, 2], [3], []]
    assert repr(List[List[Integer]].qualify([[1, 2], [3], []])) == "[[1, 2], [3], []]"
    with pytest.raises(ValueQualificationError):
        List[Integer].qualify([1, 2, "3"])
    with pytest.raises(ValueQualificationError):
        List[Integer].qualify(123)


def test_list_qualifier_accepts_arbitrary_iterables():
    assert List[Integer].qualify((1, 2)).value == [1, 2]
    assert repr(List[Integer].qualify((1, 2))) == "[1, 2]"
    assert List[Integer].qualify((x for x in range(3))).value == [0, 1, 2]
    assert repr(List[Integer].qualify((x for x in range(3)))) == "[0, 1, 2]"


def test_mapping_qualifier_works_correctly():
    assert Mapping[Integer, List[Integer]].qualify({}).value == {}
    assert repr(Mapping[Integer, List[Integer]].qualify({})) == "{}"
    assert Mapping[Integer, List[Integer]].qualify({2: [2], 1: [1]}).value == {2: [2], 1: [1]}
    assert repr(Mapping[Integer, List[Integer]].qualify({2: [2], 1: [1]})) == "{1: [1], 2: [2]}"
    with pytest.raises(ValueQualificationError):
        Mapping[Integer, List[Integer]].qualify({1: [1], 2: [None]})
    with pytest.raises(ValueQualificationError):
        Mapping[Integer, List[Integer]].qualify({1: [1], 2: None})
    with pytest.raises(ValueQualificationError):
        Mapping[Integer, List[Integer]].qualify({"1": [2, 3]})
    with pytest.raises(ValueQualificationError):
        Mapping[Integer, List[Integer]].qualify([1, 2, 3])
    with pytest.raises(ValueQualificationError):
        Mapping[Integer, List[Integer]].qualify(123)


def test_set_qualifier_works_correctly():
    assert Set[Integer].qualify([]).value == set()
    assert repr(Set[Integer].qualify([])) == "{}"
    assert Set[Integer].qualify([3, 2, 1]).value == {1, 2, 3}
    assert repr(Set[Integer].qualify([3, 2, 1])) == "{1, 2, 3}"
    with pytest.raises(ValueQualificationError):
        Set[Integer].qualify([1, 2, "3"])
    with pytest.raises(ValueQualificationError):
        Set[Integer].qualify(123)


def test_set_qualifier_produces_frozen_set():
    assert Set[Set[Integer]].qualify([[3, 2], [1], []]).value == set(map(frozenset, [{3, 2}, {1}, ()]))
    assert repr(Set[Set[Integer]].qualify([[3, 2], [1], []])) == "{{1}, {2, 3}, {}}"


def test_set_qualifier_fails_on_unhashable_elements():
    with pytest.raises(ValueQualificationError):
        Set[List[Integer]].qualify([[1], []])


def test_string_qualifier_works_correctly():
    assert String.qualify("hello").value == "hello"
    assert repr(String.qualify("hello")) == "'hello'"
    assert String.qualify(six.b("hello")).value == "hello"
    assert repr(String.qualify(six.b("hello"))) == "'hello'"  # not "u'hello'"
    assert String.qualify(six.u("hello")).value == "hello"
    assert repr(String.qualify(six.u("hello"))) == "'hello'"  # not "b'hello'"
    with pytest.raises(ValueQualificationError):
        String.qualify(six.b("\xd0\xbf\xd1\x80\xd0\xb8\xd0\xb2\xd0\xb5\xd1\x82").decode("utf-8"))
    with pytest.raises(ValueQualificationError):
        String.qualify(six.b("\xd0\xbf\xd1\x80\xd0\xb8\xd0\xb2\xd0\xb5\xd1\x82"))
    with pytest.raises(ValueQualificationError):
        String.qualify([1, 2, 3])
    with pytest.raises(ValueQualificationError):
        String.qualify(123)


def test_string_qualifier_escapes_quotes_consistently():
    assert String.qualify("'hello'").value == "'hello'"
    assert repr(String.qualify("'hello'")) == "\"'hello'\""
    assert String.qualify("'hello\"").value == "'hello\""
    assert repr(String.qualify("'hello\"")) == "'\\'hello\"'"


def test_text_qualifier_works_correctly():
    assert Text.qualify(six.u("hello")).value == six.u("hello")
    assert repr(Text.qualify(six.u("hello"))) == "'hello'"
    assert Text.qualify(six.b("hello")).value == six.u("hello")
    assert repr(Text.qualify(six.b("hello"))) == "'hello'"
    assert Text.qualify("hello").value == six.u("hello")
    assert repr(Text.qualify("hello")) == "'hello'"
    hello = six.b("\xd0\xbf\xd1\x80\xd0\xb8\xd0\xb2\xd0\xb5\xd1\x82")
    assert Text.qualify(hello.decode("utf-8")).value.encode("utf-8") == hello
    assert repr(Text.qualify(hello.decode("utf-8"))) == r"'\\u043f\\u0440\\u0438\\u0432\\u0435\\u0442'"
    with pytest.raises(ValueQualificationError):
        Text.qualify(hello)
    with pytest.raises(ValueQualificationError):
        Text.qualify([1, 2, 3])
    with pytest.raises(ValueQualificationError):
        Text.qualify(123)


def test_optional_qualifier_accepts_none_values():
    assert Optional[String].qualify(None).value is None
    assert repr(Optional[String].qualify(None)) == "None"


def test_optional_qualifier_uses_underlying_qualifier():
    assert Optional[String].qualify("hello").value == String.qualify("hello").value
    assert repr(Optional[String].qualify("hello")) == repr(String.qualify("hello"))
    with pytest.raises(ValueQualificationError):
        Optional[String].qualify(123)


def test_boolean_qualifier_works_correctly():
    assert Boolean.qualify(True).value is True
    assert repr(Boolean.qualify(True)) == "True"
    assert Boolean.qualify(False).value is False
    assert repr(Boolean.qualify(False)) == "False"
    with pytest.raises(ValueQualificationError):
        Boolean.qualify("")


def test_date_qualifier_accepts_date_objects_from_standard_library():
    assert Date.qualify(datetime.date(2017, 3, 15)).value == datetime.date(2017, 3, 15)
    assert repr(Date.qualify(datetime.date(2017, 3, 15))) == "2017-03-15"


def test_date_qualifier_does_not_accept_datetime_objects():
    with pytest.raises(ValueQualificationError):
        Date.qualify(datetime.datetime(2017, 3, 15))


def test_date_qualifier_accepts_strings_in_strict_iso8601_format():
    assert Date.qualify("2017-03-15").value == datetime.date(2017, 3, 15)
    assert repr(Date.qualify("2017-03-15")) == "2017-03-15"
    with pytest.raises(ValueQualificationError):
        Date.qualify("2017-3-15")
    with pytest.raises(ValueQualificationError):
        Date.qualify("20170315")
    with pytest.raises(ValueQualificationError):
        Date.qualify("03-15-2017")
    with pytest.raises(ValueQualificationError):
        Date.qualify("2017-03-15T00:00:00Z")


def test_datetime_qualifier_accepts_datetime_objects_from_standard_library():
    assert DateTime.qualify(datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.UTC)).value == datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.UTC)
    assert repr(DateTime.qualify(datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.UTC))) == "2017-03-15T10:06:02.000789Z"


def test_datetime_qualifier_does_not_accept_date_objects():
    with pytest.raises(ValueQualificationError):
        DateTime.qualify(datetime.date(2017, 3, 15))


def test_datetime_qualifier_accepts_strings_in_strict_iso8601_format():
    assert DateTime.qualify("2017-03-15T10:06:02Z").value == datetime.datetime(2017, 3, 15, 10, 6, 2, 0, tzinfo=iso8601.iso8601.UTC)
    assert repr(DateTime.qualify("2017-03-15T10:06:02Z")) == "2017-03-15T10:06:02Z"
    assert DateTime.qualify("2017-03-15T10:06:02.000789Z").value == datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.UTC)
    assert repr(DateTime.qualify("2017-03-15T10:06:02.000789Z")) == "2017-03-15T10:06:02.000789Z"
    assert DateTime.qualify("2017-03-15T10:06:02.000789+03:00").value == datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.FixedOffset(3, 0, "+03:00"))
    assert repr(DateTime.qualify("2017-03-15T10:06:02.000789+03:00")) == "2017-03-15T07:06:02.000789Z"
    assert DateTime.qualify("2017-03-15T10:06:02.000789-11:30").value == datetime.datetime(2017, 3, 15, 10, 6, 2, 789, tzinfo=iso8601.iso8601.FixedOffset(-11, -30, "-11:30"))
    assert repr(DateTime.qualify("2017-03-15T10:06:02.000789-11:30")) == "2017-03-15T21:36:02.000789Z"
    with pytest.raises(ValueQualificationError):
        DateTime.qualify("2017-03-15")
    with pytest.raises(ValueQualificationError):
        DateTime.qualify("2017-03-15T10:06:02")
    with pytest.raises(ValueQualificationError):
        DateTime.qualify("2017-03-15 10:06:02Z")
    with pytest.raises(ValueQualificationError):
        DateTime.qualify("2017-03-15T10:06:02-25:00")


def test_timedelta_qualifier_accepts_timedelta_objects_from_standard_library():
    assert TimeDelta.qualify(datetime.timedelta(seconds=0.123456)).value == datetime.timedelta(seconds=0.123456)
    assert repr(TimeDelta.qualify(datetime.timedelta(seconds=0.123456))) == "PT0.123456S"
    assert TimeDelta.qualify(datetime.timedelta(days=1)).value == datetime.timedelta(days=1)
    assert repr(TimeDelta.qualify(datetime.timedelta(days=1))) == "PT86400.000000S"
    assert TimeDelta.qualify(datetime.timedelta(days=1, seconds=0.123456)).value == datetime.timedelta(days=1, seconds=0.123456)
    assert repr(TimeDelta.qualify(datetime.timedelta(days=1, seconds=0.123456))) == "PT86400.123456S"
    with pytest.raises(ValueQualificationError):
        TimeDelta.qualify(datetime.timedelta(days=-1, hours=23))


def test_timedelta_qualifier_does_not_accept_datetime_objects():
    with pytest.raises(ValueQualificationError):
        TimeDelta.qualify(datetime.datetime(2017, 3, 15, 10, 6, 2))


def test_timedelta_qualifier_accepts_strings_in_strict_iso8601_format():
    assert TimeDelta.qualify("P1.1W2.2DT3.3H4.4M5.5S").value == datetime.timedelta(weeks=1.1, days=2.2, hours=3.3, minutes=4.4, seconds=5.5)
    assert repr(TimeDelta.qualify("P1.1W2.2DT3.3H4.4M5.5S")) == "PT867509.500000S"
    assert TimeDelta.qualify("P1WT12H").value == datetime.timedelta(weeks=1, hours=12)
    assert repr(TimeDelta.qualify("P1WT12H")) == "PT648000.000000S"
    assert TimeDelta.qualify("P6D").value == datetime.timedelta(days=6)
    assert repr(TimeDelta.qualify("P6D")) == "PT518400.000000S"
    assert TimeDelta.qualify("P").value == datetime.timedelta()
    assert repr(TimeDelta.qualify("P")) == "PT0.000000S"
    with pytest.raises(ValueQualificationError):
        TimeDelta.qualify("P1Y")
    with pytest.raises(ValueQualificationError):
        TimeDelta.qualify("P1M")
    with pytest.raises(ValueQualificationError):
        TimeDelta.qualify("0")
