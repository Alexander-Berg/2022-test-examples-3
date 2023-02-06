from io import StringIO

from travel.rasp.bus.spark_api import csv_serializer


class Some:
    def __init__(self, b, a):
        self.b = b
        self.a = a


def test_empty():
    stream = StringIO()
    csv_serializer.serialize(stream, [])

    assert stream.getvalue() == ''


def test_one_record():
    stream = StringIO()
    csv_serializer.serialize(stream, [Some('b1', 'a1')])

    assert stream.getvalue() == 'a\tb\r\na1\tb1\r\n'


def test_many_record():
    stream = StringIO()
    csv_serializer.serialize(stream, [Some('b1', 'a1'), Some('b2', 'a2')])

    assert stream.getvalue() == 'a\tb\r\na1\tb1\r\na2\tb2\r\n'
