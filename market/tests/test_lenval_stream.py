# -*- coding: utf-8 -*-

import six
import unittest
from six import BytesIO as StringIO

import market.pylibrary.lenval_stream as lenval_stream


_NUMBER = 0x00000102
_STRING = '\x02\x01\x00\x00'
_SOURCE = _STRING + (_NUMBER * 'a') + _STRING + (_NUMBER * 'b')


def lv(s):
    return list(
        six.ensure_str(msg)
        for msg in lenval_stream.iter_file(StringIO(six.ensure_binary(s)))
    )


def to_lv(xs):
    f = StringIO()
    lenval_stream.write(f, xs)
    s = f.getvalue()
    f.close()
    return six.ensure_str(s)


class TestLengthEncoding(unittest.TestCase):
    """Tests that length serialization and deserialization is LE"""
    def test_encoding(self):
        self.assertEqual(_NUMBER, lenval_stream.decode_length(_STRING))
        self.assertEqual(lenval_stream.encode_length(_NUMBER), six.ensure_binary(_STRING))


class TestLenvalDecoding(unittest.TestCase):
    """Tests that lenval streams are properly decoded, and that
    in case of errors, the appropriate exception is raised"""
    def test_nil_message(self):
        self.assertEqual(lv(''), [])

    def test_ok_message(self):
        xs = lv(_SOURCE)
        self.assertEqual(len(xs), 2)
        self.assertEqual(len(xs[0]), _NUMBER)
        self.assertEqual(xs[0][0], 'a')
        self.assertEqual(len(xs[1]), _NUMBER)
        self.assertEqual(xs[1][0], 'b')

    def test_bad_message(self):
        bare_length_source = _SOURCE[0:4]
        with self.assertRaises(lenval_stream.Error):
            lv(bare_length_source)

        cut_value_source = _SOURCE[0:len(_SOURCE)-1]
        with self.assertRaises(lenval_stream.Error):
            lv(cut_value_source)

        cut_length_source = _SOURCE[0:4 + _NUMBER + 1]
        with self.assertRaises(lenval_stream.Error):
            lv(cut_length_source)


class TestLenvalEncoding(unittest.TestCase):
    """Tests that messages are serialized properly"""
    def test_nil_message(self):
        self.assertEqual(to_lv([]), '')

    def test_ok_message(self):
        self.assertEqual(to_lv([_NUMBER * 'a', _NUMBER * 'b']), _SOURCE)

if __name__ == '__main__':
    unittest.main()
