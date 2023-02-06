# -*- coding: utf-8 -*-

import gzip
import os
import shutil
import six
import unittest
import zipfile

from market.pylibrary.yatestwrap.yatestwrap import source_path

from market.idx.pylibrary.ymlng import extract_and_convert
import market.idx.pylibrary.ymlng as ymlng

XML_HEADER = '<?xml version="1.0" encoding="utf-8"?>'


def writefile(filepath, content):
    with open(filepath, 'wb') as fobj:
        return fobj.write(
            content if isinstance(content, six.binary_type)
            else six.b(content)
        )


def readfile(filepath):
    with open(filepath) as fobj:
        return fobj.read()


def gzip_file(src, dst):
    with open(src, mode='rb') as fsrc, gzip.GzipFile(dst, mode='wb') as fdst:
        shutil.copyfileobj(fsrc, fdst)


def zip_files(src, dst):
    if isinstance(src, six.string_types):
        src = [src]
    with zipfile.ZipFile(dst, mode='w') as fobj:
        for path in src:
            fobj.write(path)


def gzip_mem(content):
    mem = six.BytesIO()
    with gzip.GzipFile(fileobj=mem, mode='wb') as fobj:
        fobj.write(six.ensure_binary(content))
    return mem.getvalue()


class Test(unittest.TestCase):

    def setUp(self):
        self.rootdir = os.path.join(os.getcwd(), 'tmp')
        self.src = os.path.join(self.rootdir, '1')
        self.dst = os.path.join(self.rootdir, '2')
        self.content = 'hello world'

        shutil.rmtree(self.rootdir, ignore_errors=True)
        if not os.path.exists(self.rootdir):
            os.makedirs(self.rootdir)

    def tearDown(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)

    def make_path(self, name):
        return os.path.join(self.rootdir, name)

    def test_text(self):
        contents = [
            XML_HEADER + '<root/>',
            'some text',
            ymlng._GZIP_MAGIC + 'some text',
            ymlng._ZIP_MAGIC + 'some text',
        ]
        for content in contents:
            src1 = os.path.join(self.rootdir, '1')
            writefile(src1, content)
            src2 = src1 + '.simple_gzip'
            gzip_file(src1, src2)
            src3 = src1 + '.simple_zip'
            zip_files(src1, src3)
            for src, expected_typex in [
                    (src1, []),
                    (src2, [ymlng.TYPE_GZIP]),
                    (src3, [ymlng.TYPE_ZIP]),
            ]:
                typex = []
                extract_and_convert(src, self.dst, typex)
                self.assertEqual(typex, expected_typex)

    def test_excel(self):
        xls_paths = [
            # 'market/idx/pylibrary/ymlng/tests/excel/test_feed_xls95_no_type.xls',
            'market/idx/pylibrary/ymlng/tests/excel/test_feed_xls2003_no_type.xls',
        ]
        xlsx_paths = [
            'market/idx/pylibrary/ymlng/tests/excel/bad_mime_application_octet_stream.xlsx',
            'market/idx/pylibrary/ymlng/tests/excel/test_feed_xlsx_no_type.xlsx',
        ]
        types_path_list = []
        for typex, paths in [
            (ymlng.TYPE_XLS, xls_paths),
            (ymlng.TYPE_XLSX, xlsx_paths),
        ]:
            for path in paths:
                path = source_path(path)
                src = os.path.join(self.rootdir, os.path.basename(path))
                # excel
                shutil.copyfile(path, src)
                types_path_list.append(([typex], src))
                # gzip excel
                dst = src + '.gzip_excel'
                gzip_file(src, dst)
                types_path_list.append(([ymlng.TYPE_GZIP, typex], dst))
                # zip excel
                dst = src + '.zip_excel'
                zip_files(src, dst)
                types_path_list.append(([ymlng.TYPE_ZIP, typex], dst))

        for types, path in types_path_list:
            actual_typelist = []
            try:
                out = extract_and_convert(path, self.dst, actual_typelist)
                self.assertIsNotNone(out)
            except ymlng.NoXls2CsvError:
                pass  # no converter
            self.assertEqual(types, actual_typelist)

    def test_double_gzip(self):
        writefile(self.src, gzip_mem(gzip_mem(self.content)))
        extract_and_convert(self.src, self.dst)
        self.assertEqual(readfile(self.dst), self.content)

    def test_triple_gzip(self):
        writefile(self.src, gzip_mem(gzip_mem(gzip_mem(self.content))))
        self.assertRaises(ymlng.CompressionDepthExceeded, extract_and_convert, self.src, self.dst)

    def test_zip_many_files(self):
        writefile(self.make_path('a'), self.content)
        writefile(self.make_path('b'), self.content)
        zip_files([self.make_path('a'), self.make_path('b')], self.src)
        self.assertRaises(ymlng.TooManyFilesInArchive, extract_and_convert, self.src, self.dst)

    def test__wrong_xls2csv_bin_path__NoXls2CsvError_raised(self):
        input_file_content = '{}HEllo!'.format(ymlng._XLS_MAGIC)
        writefile(self.src, input_file_content)
        self.assertRaises(ymlng.NoXls2CsvError, extract_and_convert, self.src, self.dst, xls2csv_path="wrong_path")

    def test__xls2csv_bin_exist__ok(self):
        xls2csv_path = os.path.join(self.rootdir, 'xls2csv.sh')
        expected_result = 'OK!'
        input_file_content = '{}HEllo!'.format(ymlng._XLS_MAGIC)
        writefile(self.src, input_file_content)

        writefile(xls2csv_path, self.get_xls2csv_bin_content(expected_result))
        os.chmod(xls2csv_path, 0o766)

        stdout_result = ymlng.extract_and_convert(self.src, self.dst, xls2csv_path=xls2csv_path)
        self.assertEqual(readfile(self.dst), expected_result)
        self.assertEqual(stdout_result, six.b(input_file_content))

    def get_xls2csv_bin_content(self, expected_result):
        return """#!/bin/bash
        infile=$1
        outfile=$2
        content=$(cat $infile)
        echo -n "{}" > $outfile
        echo -n $content
        """.format(expected_result)


if '__main__' == __name__:
    unittest.main()
