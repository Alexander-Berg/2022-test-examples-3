# -*- coding: utf-8 -*-

from StringIO import StringIO

from travel.avia.admin.tests.lib.unittests.testcase import TestCase
from travel.avia.admin.lib.mysqlutils import MysqlFileWriter, LoadInFileHelper, MysqlFileReader


class MysqlWriterTest(TestCase):
    def testSimple(self):
        stream = StringIO()
        writer = MysqlFileWriter(stream, ('f1', 'f2'))
        writer.writedict({'f1': '23', 'f2': u'sssfff'})
        writer.writedict({'f1': None, 'f2': u'sss\u0000aabb'})
        writer.writedict({'f1': 5, 'f2': True})

        self.assertEqual(
            stream.getvalue(),
            """ "23"\t"sssfff"\nNULL\t"sss\\0aabb"\n"5"\t"1" """.strip()
        )


class LoadInFileHelperTest(TestCase):
    def testQuote(self):
        self.assertEqual('"aaabbb"', LoadInFileHelper.quote('aaabbb'))
        self.assertEqual('"aaa\\nbbb"', LoadInFileHelper.quote('aaa\nbbb'))

    def testEscape(self):
        self.assertEqual('\\0', LoadInFileHelper.escape('\x00'))
        self.assertEqual('\\b', LoadInFileHelper.escape('\b'))
        self.assertEqual('\\n', LoadInFileHelper.escape('\n'))
        self.assertEqual('\\r', LoadInFileHelper.escape('\r'))
        self.assertEqual('\\t', LoadInFileHelper.escape('\t'))
        self.assertEqual('\\Z', LoadInFileHelper.escape('\x1a'))
        self.assertEqual('\\"', LoadInFileHelper.escape('"'))
        self.assertEqual('\\\\', LoadInFileHelper.escape('\\')),
        self.assertEqual('\\\\\\0\\n', LoadInFileHelper.escape('\\\x00\n')),

        self.assertEqual('aaa\\tbbb', LoadInFileHelper.escape('aaa\tbbb')),
        self.assertEqual('aaa\\0bbb', LoadInFileHelper.escape('aaa\x00bbb')),

    def testTransform(self):
        self.assertEqual('NULL', LoadInFileHelper.transform_value(None))
        self.assertEqual('"3"', LoadInFileHelper.transform_value(3))
        self.assertEqual('"1"', LoadInFileHelper.transform_value(True))
        self.assertEqual('"aaabbb"', LoadInFileHelper.transform_value('aaabbb'))
        self.assertEqual('"aaa\\nbbb"', LoadInFileHelper.transform_value('aaa\nbbb'))

    def testUnescape(self):
        self.assertEqual("aaa", LoadInFileHelper.unescape('aaa'))
        self.assertEqual("aaa\n\\", LoadInFileHelper.unescape('aaa\\n\\\\'))
        self.assertRaises(ValueError, LoadInFileHelper.unescape, 'aaa\\n\\')
        self.assertEqual("aaa\n\\bbb\x1a", LoadInFileHelper.unescape('aaa\\n\\\\bbb\\Z'))

    def testSimpleRestore(self):
        self.assertEqual(None, LoadInFileHelper.simple_restore_value('NULL'))
        self.assertEqual(u'aaa\nbbb', LoadInFileHelper.simple_restore_value('"aaa\\nbbb"'))
        self.assertRaises(ValueError, LoadInFileHelper.simple_restore_value, 'aaa\\nbbb')


class MysqlFileReaderTest(TestCase):
    def testSimple(self):
        data = [
            {'f1': '23', 'f2': u'sssfff'},
            {'f1': None, 'f2': u'sss\u0000aabb'},
            {'f1': 5, 'f2': True},
        ]

        out_data = [
            {'f1': '23', 'f2': u'sssfff'},
            {'f1': None, 'f2': u'sss\u0000aabb'},
            {'f1': u'5', 'f2': u'1'},
        ]

        stream = StringIO()
        writer = MysqlFileWriter(stream, ('f1', 'f2'))
        for rowdict in data:
            writer.writedict(rowdict)

        stream.seek(0)

        reader = MysqlFileReader(stream, ('f1', 'f2'))
        read_data = list(reader)

        self.assertListEqual(out_data, read_data)
