# -*- coding: utf-8 -*-

import csv
import logging
import os
import os.path
import shutil
import unittest

from getter import util

import yatest.common


logging.basicConfig(format='%(asctime)s [%(levelname)s] %(message)s', level=logging.DEBUG)


class Test(unittest.TestCase):
    rootdir = 'util-tmp'

    def setUp(self):
        util.makedirs(self.rootdir)

    def tearDown(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)

    def _create_dir_structure(self, d):
        """Create dirs and files for the test"""
        for i in range(10):
            d = os.path.join(d, str(i))
            util.makedirs(d)
            with open(os.path.join(d, 'file-%d' % i), 'w') as fd:
                fd.write('file-%d' % i)

    def _assert_links(self, ddir):
        """Make sure all files under ddir are hardlinks"""
        for root, dirs, files in os.walk(ddir):
            for f in files:
                fn = os.path.join(root, f)
                self.assertTrue(os.stat(fn).st_nlink > 1)

    def test_cmptree(self):
        """Test util.cmptree"""
        srcdir = os.path.join(self.rootdir, 'source')
        dstdir = os.path.join(self.rootdir, 'destination')
        self._create_dir_structure(os.path.join(self.rootdir, 'source'))

        # make a copy of the directory
        args = ['cp', '-rl', srcdir, dstdir]
        util.subprocess_call(args)

        # make sure directories are equal
        self.assertTrue(util.cmptree(srcdir, dstdir))
        self._assert_links(dstdir)


class TestMergeCSV(unittest.TestCase):
    @staticmethod
    def save_rows(dst_filename, rows):
        with util.get_opener(dst_filename)(dst_filename, 'wb') as dst_fn:
            writer = csv.writer(dst_fn, lineterminator='\n')
            for row in rows:
                writer.writerow(row)

    @staticmethod
    def load_rows(src_filename):
        with util.get_opener(src_filename)(src_filename) as src_fn:
            reader = csv.reader(src_fn)
            return list(reader)

    def test_merge_csv__base(self, filename_suffix=""):
        base_filename = yatest.common.output_path("test_merge_csv_base__base{}".format(filename_suffix))
        extra_filename = yatest.common.output_path("test_merge_csv_base__extra{}".format(filename_suffix))
        merged_filename = yatest.common.output_path("test_merge_csv_base__merged{}".format(filename_suffix))

        base_rows = [
            ["a", "b", "c"],
            ["q", "w", "e"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"],
        ]

        extra_rows = [
            ["a", "s", "d"],
            ["b", "z", "z"],
            ["m", "r", "k"],
        ]

        expected_rows = [
            ["a", "s", "d"],
            ["b", "z", "z"],
            ["m", "r", "k"],
            ["q", "w", "e"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"],
        ]

        self.save_rows(base_filename, base_rows)
        self.save_rows(extra_filename, extra_rows)
        util.merge_csv(merged_filename, base_filename, extra_filename)
        merged_rows = self.load_rows(merged_filename)

        assert merged_rows == expected_rows

    def test_merge_csv__gz(self):
        return self.test_merge_csv__base(filename_suffix=".gz")

    def test_merge_csv__multirow_key(self, filename_suffix=""):
        base_filename = yatest.common.output_path("test_merge_csv__multirow_key__base{}".format(filename_suffix))
        extra_filename = yatest.common.output_path("test_merge_csv__multirow_key__extra{}".format(filename_suffix))
        merged_filename = yatest.common.output_path("test_merge_csv__multirow_key__merged{}".format(filename_suffix))

        base_rows = [
            ["a", "b", "c"],
            ["q", "w", "e"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"]
        ]

        extra_rows = [
            ["a", "s", "d"],
            ["a", "b", "z"],
            ["m", "r", "k"],
        ]

        expected_rows = [
            ["a", "s", "d"],
            ["a", "b", "z"],
            ["m", "r", "k"],
            ["q", "w", "e"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"],
        ]

        self.save_rows(base_filename, base_rows)
        self.save_rows(extra_filename, extra_rows)
        util.merge_csv(merged_filename, base_filename, extra_filename, key_columns=[0, 1])
        merged_rows = self.load_rows(merged_filename)

        assert merged_rows == expected_rows

    def test_merge_csv__multirow_key_gz(self):
        return self.test_merge_csv__multirow_key(filename_suffix=".gz")

    def test_merge_csv__extra_is_greater(self, filename_suffix=""):
        base_filename = yatest.common.output_path("test_merge_csv__extra_is_greater__base{}".format(filename_suffix))
        extra_filename = yatest.common.output_path("test_merge_csv__extra_is_greater__extra{}".format(filename_suffix))
        merged_filename = yatest.common.output_path("test_merge_csv__extra_is_greater__merged{}".format(filename_suffix))

        base_rows = [
            ["a", "b", "c"],
            ["q", "w", "e"],
        ]

        extra_rows = [
            ["a", "s", "d"],
            ["b", "z", "z"],
            ["m", "r", "k"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"],
        ]

        expected_rows = [
            ["a", "s", "d"],
            ["b", "z", "z"],
            ["m", "r", "k"],
            ["aaa", "bbb", "ccc"],
            ["ddd", "ddd", "ddd"],
            ["eee", "eee", "eee"],
            ["mmm", "qqq"],
            ["q", "w", "e"],
        ]

        self.save_rows(base_filename, base_rows)
        self.save_rows(extra_filename, extra_rows)
        util.merge_csv(merged_filename, base_filename, extra_filename)
        merged_rows = self.load_rows(merged_filename)

        assert merged_rows == expected_rows

    def test_merge_csv__extra_is_greater_gz(self):
        return self.test_merge_csv__extra_is_greater(filename_suffix=".gz")


if __name__ == '__main__':
    unittest.main()
