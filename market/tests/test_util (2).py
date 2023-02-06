#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest
import os

import context
from market.pylibrary.mindexerlib import util

import shutil

rootdir = context.rootdir


class TestFindBrokenSymlinks(unittest.TestCase):
    def setUp(self):
        context.setup()

        def symlink(src, dst):
            os.symlink(src, os.path.join(rootdir, dst))

        def touch(name):
            open(os.path.join(rootdir, name), 'w').close()

        symlink('bad', 'symlink.bad.1')
        symlink('symlink.bad.1', 'symlink.bad.2')
        touch('good')
        symlink('good', 'symlink.good.1')

        self.bad_symlinks = ['symlink.bad.1', 'symlink.bad.2']

    def tearDown(self):
        context.cleanup()

    def test(self):
        broken_symlinks = util.find_broken_symlinks(rootdir)
        bad_symlinks = [os.path.join(rootdir, name) for name in self.bad_symlinks]
        self.assertEquals(sorted(broken_symlinks), sorted(bad_symlinks))


class TestLazy(unittest.TestCase):
    def test(self):
        @util.lazy
        def foo():
            return object()

        self.assertEqual(foo(), foo())


class TestCropGenerations(unittest.TestCase):
    def setUp(self):
        self.path = 'crop_test'
        util.makedirs(self.path)

    def test(self):
        generations = ['20160318_2027', '20160318_2007', '20160318_1947', '20160318_1928', '20160318_1919', '20160317_2356', '20160317_2332',
                       '20160317_0007', '20160316_2345', '20160316_2326', '20160316_0535', '20160315_2350', '20160318_2027', '20160318_2007', '20160313_2355',
                       '20160312_0919', '20160306_2341', '20160224_0038', '20160224_0009', '20160222_0213', '20160222_0149', 'data', 'data_20160222_0149']
        first_expected = ['20160318_2027', '20160318_2007', '20160318_1947',
                          '20160317_2356', '20160316_2345', '20160315_2350',
                          '20160313_2355',
                          'data', 'data_20160222_0149']
        second_expected = ['20160318_2027', 'data', '20160318_1947', '20160318_2007', 'data_20160222_0149']

        for name in generations:
            util.makedirs(os.path.join(self.path, name))

        util.crop_generations(self.path, keep=3, keep_daily=4, keep_weekly=2)
        result = os.listdir(self.path)
        self.assertEquals(len(result), len(first_expected))
        for x in first_expected:
            self.assertTrue(x in result)

        util.crop_generations_older_than(self.path, '20160318_1947')
        result = os.listdir(self.path)
        self.assertEquals(len(result), len(second_expected))
        for x in second_expected:
            self.assertTrue(x in result)

        for name in generations:
            util.makedirs(os.path.join(self.path, name))
        util.mark_recent(self.path)
        self.assertEqual(open(os.path.join(self.path, 'generation')).readline(), '20160318_2027\n')

    def tearDown(self):
        shutil.rmtree(self.path)


if __name__ == '__main__':
    unittest.main()
