# -*- coding: utf-8 -*-

from os.path import join as _join
import os
import shutil
import unittest

import context
from pyb.plugin import marketsearch


ROOTDIR = context.TMP_DIR
SEARCHDIR = _join(ROOTDIR, 'search')

MARKETSEARCHDIR = _join(SEARCHDIR, 'marketsearch')
INDEXDIR = _join(SEARCHDIR, 'index')
MMAP_NAMES = ['group_region_stats.mmap']

UNITED_DISTS = {
    'search-report-data': _join(SEARCHDIR, 'report-data'),
    'search-part-0': _join(INDEXDIR, 'part-0'),
    'search-part-8': _join(INDEXDIR, 'part-8'),
    'search-part-blue': _join(INDEXDIR, 'part-blue'),
    'model-part-0': _join(INDEXDIR, 'model', 'part-0'),
    'book-part-0': _join(INDEXDIR, 'book', 'part-0'),
    'search-stats': _join(INDEXDIR, 'mmap'),
}

META_DISTS = {
    'search-meta-report-data': _join(SEARCHDIR, 'report-data'),
    'search-meta-stats': _join(INDEXDIR, 'mmap'),
}


def makedirs(dirname):
    if not os.path.exists(dirname):
        os.makedirs(dirname)


def touch(filepath, *args, **kwds):
    filepath = _join(filepath, *args)
    dirname = os.path.dirname(filepath)
    makedirs(dirname)
    with open(filepath, 'w') as f:
        for k, v in list(kwds.items()):
            f.write('%s: %s\n' % (k, v))


def tomap(path):
    res = {}
    with open(path) as f:
        for s in f:
            key, val = s.split(': ', 1)
            res[key] = val.rstrip()
    return res


def exists_and_islink(path):
    return os.path.exists(path) and os.path.islink(path)


def create_generation(generation, dists):
    gendir = _join(MARKETSEARCHDIR, generation)
    makedirs(gendir)

    for dist in dists:
        touch(gendir, dist, dist, generation=generation, name=dist)

    if 'search-stats' in dists:
        stats_dir = 'search-stats'
    else:
        stats_dir = 'search-meta-stats'

    mmap_dir = _join(gendir, stats_dir)
    for name in MMAP_NAMES:
        touch(mmap_dir, name, generation=generation, name=name)


def switch(generation, dists_):
    class Config(object):
        download_dir = MARKETSEARCHDIR
        dists_root_dir = ''
        dists = list(dists_.items())
        uc_bin = None
        index_dir = _join(MARKETSEARCHDIR, generation, 'index')
        report_data = _join(SEARCHDIR, 'report-data')
        backup_dir = _join(SEARCHDIR, 'backup')
        experimental_unpack = False
        clean_recipes_xml = False
        index_unpacker = None
        experiment_flags_reader = None
    copier = marketsearch.copiers.IndexCopier(Config(), generation)
    copier.run()


class Test(unittest.TestCase):
    def setUp(self):
        self.init()

    def tearDown(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)

    def init(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        makedirs(INDEXDIR)

    def run_test(self, dists):
        self.init()

        def do_test(generation):
            for dist, dirname in list(dists.items()):
                path = _join(dirname, dist)
                data = tomap(path)
                self.assertEqual(data['generation'], generation)
                self.assertEqual(data['name'], dist)

        def do_test_mmap(generation, has_shm):
            if 'search-report-data' in dists:
                dirname = dists['search-report-data']
            else:
                dirname = dists['search-meta-report-data']

            self.assertTrue(exists_and_islink(dirname))
            for name in MMAP_NAMES:
                path = _join(dirname, name)
                self.assertTrue(exists_and_islink(path))
                if has_shm:
                    self.assertEqual(os.readlink(path), os.path.abspath(_join(INDEXDIR, 'mmap', name)))
                else:
                    self.assertEqual(os.readlink(path), os.path.abspath(_join(dirname, 'mmap', name)))
                data = tomap(path)
                self.assertEqual(data['generation'], generation)
                self.assertEqual(data['name'], name)

        generation0 = '20140511_1822'
        generation1 = '20140512_1822'
        generation2 = '20140513_1822'

        create_generation(generation0, dists)
        create_generation(generation1, dists)
        create_generation(generation2, dists)

        for generation in [generation0, generation1, generation2, generation0]:
            switch(generation, dists)
            do_test(generation)
            do_test_mmap(generation, has_shm=True)

    def test(self):
        self.run_test(UNITED_DISTS)
        self.run_test(META_DISTS)


if __name__ == '__main__':
    unittest.main()
