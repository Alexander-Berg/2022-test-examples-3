#!/usr/bin/python

import yatest.common

def test_cbircomparer():
    cbircomparer = yatest.common.binary_path("extsearch/images/tools/cbircomparer/cbir_comparer")
    with open("normal.data") as test_input:
        return yatest.common.canonical_execute(cbircomparer, stdin=test_input)
