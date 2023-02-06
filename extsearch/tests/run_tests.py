#!/usr/bin/env python

import yatest.common
import subprocess


class SnippetizerTest(object):
    CSNIP_BIN = yatest.common.binary_path("tools/snipmake/csnip/csnip")
    OUTPUT_FORMAT = '{}.output.txt'

    def __init__(self, contexts):
        self.contexts = contexts

    def __call__(self, f):
        def result():
            output_filename = SnippetizerTest.OUTPUT_FORMAT.format(self.contexts)

            with open(output_filename, 'w') as f:
                subprocess.check_call(
                    [SnippetizerTest.CSNIP_BIN, '-i', self.contexts],
                    stdout=f,
                )
            return yatest.common.canonical_file(output_filename)

        return result


@SnippetizerTest('default.ctx')
def test_default():
    pass


@SnippetizerTest('page_relevance.ctx')
def test_page_relevance():
    pass


@SnippetizerTest('bad_quality_boost_ranker.ctx')
def test_bad_quality_boost_ranker():
    pass
