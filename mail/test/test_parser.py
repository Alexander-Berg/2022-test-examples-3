# coding: utf-8

import pytest
from pymdb.replication.parser import (Parser,
                                      Commit,
                                      Begin,
                                      DML,
                                      Attribute,)


parse_message = Parser().parse_message


def test_parse_message_for_commit():
    assert parse_message(u'COMMIT 42') == Commit(42)


def test_parse_message_for_begin():
    assert parse_message(u'BEGIN 42') == Begin(42)


def test_parse_dml_message():
    message = "table mail.users: INSERT: uid[bigint]:37844 is_here[boolean]:true"
    assert parse_message(message) == DML(
        table=u'mail.users',
        command=u'INSERT',
        attributes={
            u'uid': Attribute(u'bigint', 37844),
            u'is_here': Attribute(
                u'boolean',
                True
            )
        }
    )


def test_dml_without_attributes():
    message = u'table big_brother.watched_uids: DELETE: (no-tuple-data)'
    assert parse_message(message) == DML(
        table=u'big_brother.watched_uids',
        command=u'DELETE',
        attributes={}
    )


@pytest.mark.parametrize(['attributes_str', 'attributes'], [
    ["empty_array[int[]]:'{}'", {"empty_array": Attribute("int[]", "{}")}],
    ["null[int]:null", {"null": Attribute("int", None)}],
    ["empty_str[text]:''", {"empty_str": Attribute("text", "")}],
    ["\"sql_keyword\"[int]:42", {"\"sql_keyword\"": Attribute("int", 42)}],
    ["d[date with time zone]:'2017-08-17'",
     {"d": Attribute("date with time zone", "2017-08-17")}],
    ["multi_line_text[text]:'foo\nbar'",
     {"multi_line_text": Attribute("text", 'foo\nbar')}],
])
def test_parse_non_trivial_attribute(attributes_str, attributes):
    message = "table public.foo: INSERT: " + attributes_str
    assert parse_message(message).attributes == attributes
