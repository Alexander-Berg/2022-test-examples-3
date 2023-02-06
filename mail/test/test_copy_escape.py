# coding: utf-8

from datetime import datetime
import pytz
import pytest

from mail.pypg.pypg.copy_escape import pgcopy, BNULL
from mail.pypg.pypg.types.adapted import BaseAdaptedComposite
from mail.pypg.pypg.types.db_enums import DBEnum


@pytest.mark.parametrize(('orig', 'escaped'), [
    (None, BNULL),
    (u'Hello Kitty', b'Hello Kitty'),
    (u'Hello\tKitty!', b'Hello\\tKitty!'),
    ('ORLY\n?', b'ORLY\\n?'),
    (42, b'42'),
    (True, b'True'),
    ])
def test_simple_values(orig, escaped):
    assert pgcopy(orig) == escaped


def test_seq_simple_string():
    assert pgcopy([u'"a"']) == br'{"\\"a\\""}'


def test_datetime_should_be_in_iso_format():
    dt = datetime(2016, 2, 17, 13, 27, 14, 585027)
    assert pgcopy(dt) == b'2016-02-17 13:27:14.585027'

DATE_WITH_TZ = datetime(
    2017, 4, 6, 14, 47, 21, 69340, tzinfo=pytz.FixedOffset(180)
)
DATE_WITH_TZ_IN_POSTGRE = b'2017-04-06 14:47:21.069340+03:00'


def test_datetime_with_tz_should_be_in_iso_format():
    assert pgcopy(DATE_WITH_TZ) == DATE_WITH_TZ_IN_POSTGRE


class ChangeType(DBEnum):
    store = 'store'
    delete = 'delete'

    def name_in_db(self):
        return 'mail.change_type'


def test_escape_enum():
    assert pgcopy(ChangeType.store) == b'store'


@pytest.mark.parametrize(('orig', 'escaped'), [
    ([1, 2, 3], b'{1,2,3}'),
    ([-1, 0, 1], b'{-1,0,1}'),
])
def test_seq_with_numbers(orig, escaped):
    assert pgcopy(orig) == escaped


@pytest.mark.parametrize(('orig', 'escaped'), [
    (['aaa', 'bbb', 'ccc'], b'{aaa,bbb,ccc}'),
    (['aaa aaa', 'bbb', 'ccc'], b'{"aaa aaa",bbb,ccc}'),
    (['{', 'bbb}', 'ccc'], b'{"{","bbb}",ccc}'),
])
def test_seq_with_str(orig, escaped):
    assert pgcopy(orig) == escaped


def test_empty_seq():
    assert pgcopy([]) == b'{}'


def test_unsupported_case_with_null_as_array_element():
    with pytest.raises(NotImplementedError):
        pgcopy(['', None])


def test_seq_with_NULL_str_as_array_element():
    assert pgcopy(['NULL']) == br'{"NULL"}'


def test_seq_with_elements_for_escape():
    assert pgcopy([u'\\']) == br'{"\\\\"}'


class SampleComposite(BaseAdaptedComposite):
    __slots__ = ('_conn', 'x', 'y')
    pg_type_name = 'x.y'


def mc(x, y):
    return SampleComposite(x=x, y=y)


@pytest.mark.parametrize(('composite', 'escaped'), [
    (mc(None, None), b'{"(,)"}'),
    (mc(None, ""), br'{"(,\\"\\")"}'),
    (mc(None, u""), br'{"(,\\"\\")"}'),
    (mc('foo\rbar\tbaz', 'FBB'), br'{"(\\"foo\rbar\tbaz\\",FBB)"}'),
    (mc('"X Y"', b'x,y'), b'{"(\\\\"\\\\"\\\\"X Y\\\\"\\\\"\\\\",\\\\"x,y\\\\")"}'),
    ])
def test_composite(composite, escaped):
    assert pgcopy([composite]) == escaped


def test_composite_with_datetime():
    assert pgcopy(mc(DATE_WITH_TZ, 'foo')) == b'("%s",foo)' % DATE_WITH_TZ_IN_POSTGRE


def test_compsite_with_enum():
    assert pgcopy(mc(ChangeType.store, 'message')) == b'(store,message)'
