#!/usr/bin/env python
# -*- coding: utf-8 -*-

import gzip
import six
import unittest
import yatest

from market.idx.yatf.utils.mmap.mmapviewer import plain_view

CONTEX_EXPERIMENTS_TXTGZ = yatest.common.output_path("contex_experiments.txt.gz")
CONTEX_EXPERIMENTS_MMAP = yatest.common.output_path("contex_experiments.mmap")

CONTEXT_TXT_2_MMAP_CONVERTER = yatest.common.binary_path("market/idx/models/bin/contex-tsv2mmap/contex-tsv2mmap")

CANONICAL_OUTPUT = """
Experiments:
EXPERIMENT_ID:contex_1\tBASE_MODEL_ID:121\tEXPERIMENT_MODEL_ID:120
EXPERIMENT_ID:contex_1\tBASE_MODEL_ID:111\tEXPERIMENT_MODEL_ID:110
EXPERIMENT_ID:contex_1\tBASE_MODEL_ID:21\tEXPERIMENT_MODEL_ID:20
EXPERIMENT_ID:contex_1\tBASE_MODEL_ID:11\tEXPERIMENT_MODEL_ID:10
ExperimentModelId -> BaseModelId:
EXPERIMENT_MODEL_ID:120\tBASE_MODEL_ID:121
EXPERIMENT_MODEL_ID:110\tBASE_MODEL_ID:111
EXPERIMENT_MODEL_ID:20\tBASE_MODEL_ID:21
EXPERIMENT_MODEL_ID:10\tBASE_MODEL_ID:11
"""


class T(unittest.TestCase):
    maxDiff = None

    @classmethod
    def setUpClass(cls):
        with gzip.open(CONTEX_EXPERIMENTS_TXTGZ, "wb") as fn_out:
            # last column is random value, which is ignored in contex-tsv2mmap
            fn_out.write(six.ensure_binary("contex_1\t11\t10\t123123\n"))
            fn_out.write(six.ensure_binary("contex_1\t21\t20\t123123\n"))

            # compability test
            fn_out.write(six.ensure_binary("contex_1\t111\t110\n"))
            fn_out.write(six.ensure_binary("contex_1\t121\t120\n"))

    def test_delivery_services_generator(self):
        yatest.common.execute([
            CONTEXT_TXT_2_MMAP_CONVERTER,
            "--src", CONTEX_EXPERIMENTS_TXTGZ,
            "--dst", CONTEX_EXPERIMENTS_MMAP,
        ])

        _, resource = plain_view(CONTEX_EXPERIMENTS_MMAP)
        assert resource.strip() == CANONICAL_OUTPUT.strip()
