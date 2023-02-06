# -*- coding: utf-8 -*-

import os
import unittest
import shutil
import subprocess

import context
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.idx.marketindexer.marketindexer import miconfig

import market.idx.pylibrary.mindexer_core.mmapconverter.mmapconverter as mmapconverter

_exists = os.path.exists
_join = os.path.join


class TestConvertToMmap(unittest.TestCase):
    generation = 'generation'

    def setUp(self):
        config = miconfig.default()
        wdir = config.working_dir
        gdir = os.path.join(wdir, self.generation)
        shutil.rmtree(gdir, ignore_errors=True)

        rdata_dir = os.path.join(gdir, 'stats')
        os.makedirs(rdata_dir)

        idata_dir = os.path.join(gdir, 'input')
        os.makedirs(idata_dir)

        # симлинк на `hard_tests/converters_stub.py`
        names = set(c.bin for c in mmapconverter.all_converters())
        for name in names:
            if os.path.isabs(name):
                # absolute paths refer to system tools
                continue
            os.symlink(source_path('market/idx/marketindexer/hard_tests/converters_stub.py'), os.path.join(config.ybin_dir, name))

        # исходные файлы
        for c in mmapconverter.all_converters():
            context.touch(os.path.join(rdata_dir, c.src[0]))

    def tearDown(self):
        wdir = miconfig.default().working_dir
        gdir = os.path.join(wdir, self.generation)
        shutil.rmtree(gdir, ignore_errors=True)

    def test(self):
        common_ini_path = source_path('market/idx/marketindexer/hard_tests/common.ini')
        cmd = 'IL_CONFIG_PATH={} ./mindexer_clt.py convert_to_mmap --generation={}'.format(common_ini_path, self.generation)
        subprocess.check_call(cmd, shell=True)

        wdir = miconfig.default().working_dir
        converters = mmapconverter.default_converters()

        # проверяем существование mmap/* файлов
        for c in converters:
            if len(c.src) != 1:
                continue
            path = os.path.join(wdir, self.generation, 'search-stats-mmap', c.dst)
            self.assertTrue(os.path.exists(path))


if '__main__' == __name__:
    unittest.main()
