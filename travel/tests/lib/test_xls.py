# -*- coding: utf-8 -*-

import os.path
import unittest

import xlrd.xldate

from travel.rasp.admin.lib.xls import XlsParser, XlsDictParser


def get_test_filepath(filename):
    return os.path.join('travel', 'rasp', 'admin', 'tests', 'lib', 'data', 'xls', filename)


def get_test_file_obj(filename):

    return open(get_test_filepath(filename))


class XlsParserTest(unittest.TestCase):

    def testReadRow(self):
        parser = XlsParser(get_test_filepath('test_xls_parser.xls'))
        first_row = parser.next()
        second_row = parser.next()
        self.assertEqual("string", first_row[0])
        self.assertEqual(u" Русская строка", second_row[3])

    def testStripValuesReadRow(self):
        parser = XlsParser(get_test_filepath('test_xls_parser.xls'), strip_values=True)
        first_row = parser.next()
        second_row = parser.next()
        self.assertEqual("string", first_row[0])
        self.assertEqual(u"Русская строка", second_row[3])

    def testReadRowNoTransformToText(self):
        parser = XlsParser(get_test_filepath('test_xls_parser.xls'))
        parser.next()
        second_row = parser.next()

        self.assertEqual(200, second_row[1])
        self.assertEqual(xlrd.xldate.xldate_from_time_tuple((18, 30, 00)), second_row[2])
        self.assertEqual(10.205, second_row[6])

    def testReadRowTransformToText(self):
        parser = XlsParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True)
        parser.next()
        second_row = parser.next()
        third_row = parser.next()

        self.assertEqual(u"200", second_row[1])
        self.assertEqual(u"18:30:00", second_row[2])
        self.assertEqual(u"2011-10-10 10:10:00", second_row[4])
        self.assertEqual(u"2011-10-10 00:00:00", second_row[5])
        self.assertEqual(u"10.205", second_row[6])
        self.assertEqual(u"", second_row[7])
        self.assertEqual(u"1", second_row[8])
        self.assertEqual(u"0", third_row[8])

    def testIter(self):
        parser = XlsParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True)

        data = []
        for row in parser:
            data.append(row)

        self.assertEqual(3, len(data), u"Не все данные в итераторе")


class XlsDictParserTest(unittest.TestCase):
    def testParserWithFieldNamesWithoutTail(self):
        fieldnames = ('string', 'int', 'time', 'rus_string', 'datetime', 'date', 'float', 'empty',
                      'boolean')

        parser = XlsDictParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True,
                               fieldnames=fieldnames)

        rowdict = parser.next()

        self.assertEqual("string", rowdict['string'])

        rowdict = parser.next()

        self.assertEqual("18:30:00", rowdict['time'])

    def testParserWithFieldNamesWithTail(self):
        fieldnames = ('string', 'int', 'time', 'rus_string', 'datetime', 'date')

        parser = XlsDictParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True,
                               fieldnames=fieldnames)

        rowdict = parser.next()
        self.assertEqual(('float', 'empty', 'boolean'), rowdict['tail'])

    def testParserWithFieldNamesOverFlow(self):
        fieldnames = ('string', 'int', 'time', 'rus_string', 'datetime', 'date', 'float', 'empty',
                      'boolean', 'overflow')

        parser = XlsDictParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True,
                               fieldnames=fieldnames)

        rowdict = parser.next()
        self.assertEqual(None, rowdict['overflow'])

    def testParserWithOutFieldNames(self):

        parser = XlsDictParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True)

        rowdict = parser.next()

        self.assertEqual("test_string", rowdict['string'])
        self.assertEqual("18:30:00", rowdict['time'])

    def testIter(self):
        parser = XlsDictParser(get_test_filepath('test_xls_parser.xls'), transform_to_text=True)

        data = []
        for rowdict in parser:
            data.append(rowdict)

        self.assertEqual(2, len(data), u"Не все данные в итераторе")


