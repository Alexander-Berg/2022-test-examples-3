# -*- coding: utf-8 -*-

import logging
import os
import unittest

import context

import market.idx.pylibrary.mindexer_core.mmapconverter.mmapconverter as mmapconverter
from market.idx.pylibrary.mindexer_core.mmapconverter.mmapconverter import (
    MultiProcessError,
    Converter,
    multisubprocess_call,
    check_return_codes,
    convert,
)

rootdir = context.rootdir


class TestConverter(unittest.TestCase):
    converter1 = Converter('cat', 'from', 'to')
    converter2 = Converter('cat', ['from1', 'from2'], 'to')

    def test_converter(self):
        cmd = self.converter1.build_cmd(bindir=None, srcdir='/src', dstdir='/dst')
        self.assertEqual(cmd, ['cat', '/src/from', '/dst/to'])

        cmd = self.converter2.build_cmd(bindir=None, srcdir='/src', dstdir='/dst')
        self.assertEqual(cmd, ['cat', '/src/from1', '/src/from2', '/dst/to'])


class TestMultisubprocess(unittest.TestCase):
    def test(self):
        tts = 0

        args_list = ['true', 'false', 'true']
        results = zip(args_list, [0, 1, 0])
        retcodes = multisubprocess_call(args_list, tts=tts)
        self.assertEqual(retcodes, results)
        self.assertRaises(MultiProcessError, check_return_codes, results)

        args_list = ['true', 'true']
        results = zip(args_list, [0, 0])
        retcodes = multisubprocess_call(args_list, tts=tts)
        self.assertEqual(retcodes, results)
        check_return_codes(results)


class TestConvertSimple(unittest.TestCase):
    def test(self):
        converter = mmapconverter.Converter('true', 'hello', 'world')
        convert([converter], bindir=None, srcdir='', dstdir='', tts=0)


class TestConvert(unittest.TestCase):
    def setUp(self):
        context.setup()

    def tearDown(self):
        context.cleanup()

    def test(self):
        converters = []
        for c in mmapconverter.all_converters():
            converters.append(Converter('cp', c.src[0:1], c.dst))
            path = os.path.join(rootdir, c.src[0])
            with open(path, 'w'):
                pass

        srcdir = rootdir
        dstdir = os.path.join(srcdir, mmapconverter.MMAP_DIR)
        convert(converters, bindir=None, srcdir=srcdir, dstdir=dstdir, tts=0)
        for c in converters:
            path = os.path.join(rootdir, mmapconverter.MMAP_DIR, c.dst)
            self.assertTrue(os.path.exists(path))


if __name__ == '__main__':
    logging.basicConfig(level=logging.CRITICAL)
    unittest.main()
