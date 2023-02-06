#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
import pytest
import subprocess
import shutil

import yatest.common

fast_diff = yatest.common.binary_path('search/panther/tests/diff_tool/diff_tool')

idx_convert = yatest.common.binary_path('search/panther/tools/idx_convert/idx_convert')
idx_print   = yatest.common.binary_path('tools/idx_print/idx_print')

indexpanther_key_model = 'indexpanther.offroad.key.model'
indexpanther_hit_model = 'indexpanther.offroad.inv.model'


class ConvertOptions:
    def __init__(self, version='0.1', offroad_panther=False):
        self.version = version
        self.offroad_panther = offroad_panther
        self.counts_prefix = 'indexcounts.'
        self.panther_prefix = 'indexpanther.'


def convert(directory, test_name, options):
    cmd = [
        idx_convert,
        '-i', os.path.join(directory, 'index'),
        '-f', options.version,
        '-CP',
        '-vv',
        '--superlemmer-version', 'none',
        '--check-panther'
    ]
    if options.offroad_panther:
        cmd.extend([
            '--panther-key-model', indexpanther_key_model,
            '--panther-hit-model', indexpanther_hit_model
        ])
    else:
        cmd.extend([
            '--yndex-panther'
        ])

    fout = yatest.common.output_path('idx_convert.{}.out'.format(test_name))
    ferr = yatest.common.output_path('idx_convert.{}.err'.format(test_name))
    cout = open(fout, 'w')
    cerr = open(ferr, 'w')
    yatest.common.execute(cmd, stdout=cout, stderr=cerr)


def print_hits(directory, input_file, output_file, docids=None):
    cmd = [
        idx_print,
        '-i', os.path.join(directory, input_file),
        '--print-hits'
    ]

    if docids is not None:
        cmd += [
            '-d', ','.join(str(docid) for docid in docids)
        ]

    cout = open(output_file, 'w')
    yatest.common.execute(cmd, stdout=cout)


def convert_and_print(directory, test_name, options, docids=None):
    tmp = yatest.common.output_path("{}.tmp".format(test_name))
    os.mkdir(tmp)

    index = os.path.join(tmp, "index_copy")
    shutil.copytree(directory, index)

    convert(index, test_name, options)

    counts_file = yatest.common.output_path('countshits.{}.txt'.format(test_name))
    panther_file = yatest.common.output_path('pantherhits.{}.txt'.format(test_name))
    print_hits(index, options.counts_prefix, counts_file, docids)
    print_hits(index, options.panther_prefix, panther_file, docids)

    return [
        yatest.common.canonical_file(counts_file, diff_tool=[fast_diff]),
        yatest.common.canonical_file(panther_file, diff_tool=[fast_diff])
    ]


def test_convert_and_print_fresh():
    return convert_and_print(
        test_name='Fresh',
        directory=yatest.common.data_path("rtyserver/test_data/fresh/minimal"),
        options=ConvertOptions(
            version='0.1'
        )
    )


def test_convert_and_print_images0_1():
    return convert_and_print(
        test_name='Sample_Images0_1',
        directory=yatest.common.data_path("images/panther_convert_test_data/images0_1"),
        options=ConvertOptions(
            version='images0.1'
        )
    )


def test_convert_and_print_offroad_images0_1():
    return convert_and_print(
        test_name='Sample_Offroad_Images0_1',
        directory=yatest.common.data_path("images/panther_convert_test_data/offroad_images0_1"),
        options=ConvertOptions(
            version='images0.1',
            offroad_panther=True
        )
    )

