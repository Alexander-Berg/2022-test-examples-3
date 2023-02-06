#!/usr/bin/env python
# -*- coding: utf-8 -*-
# based on https://wiki.yandex-team.ru/yatool/test/#testynapytest
# to run:
#  ya make -trAP --test-stderr --keep-temps -v
# to run and canonize:
#  ya make -trAP --test-stderr --keep-temps -v -Z
#  svn commit ...

import pytest
import yatest.common
import os
import sys
import subprocess


class TestHtmlParser(object):
    resource_path = 'html10k'
    list_path = 'list.tsv'
    bin_path = yatest.common.binary_path("extsearch/images/robot/parsers/html_parser/imagelib/ut2/runner/html_parser_imagelib_fat_test_runner")
    number_tests = 0

    def setup_class(self):
        process = subprocess.Popen('tar -xzf {}'.format(self.resource_path), shell=True)
        process.wait()
        with open(self.list_path) as input_ptr:
            for line in input_ptr:
                self.number_tests += 1

    def test_number_tests(self):
        result = {
            'number': self.number_tests
        }
        return result

    def common_impl(self, result_dir, test_id):
        os.mkdir(result_dir)
        process = subprocess.Popen('{} {} {} {}'.format(self.bin_path, test_id, self.list_path, result_dir), shell=True)
        process.wait()
        canonical_files = {}
        yacf = yatest.common.canonical_file
        for f in os.listdir(result_dir):
            canonical_files[f.replace(':', '/')] = yacf(os.path.join(result_dir, f))
        result = {
            'files': canonical_files,
            'exit_code': process.returncode,
            'number_results': len(canonical_files)
        }
        return result

    def test_document_builder(self):
        return self.common_impl('canondata_document_builder', 'document_builder')

    def test_image_handler(self):
        return self.common_impl('canondata_image_handler', 'image_handler')

