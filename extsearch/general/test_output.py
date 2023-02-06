#!/usr/bin/python

import yatest.common

def test_output():
    query_filter = yatest.common.binary_path("extsearch/images/tools/query_filter/query_filter")
    test_file = yatest.common.source_path("extsearch/images/tools/query_filter/ut/data/queries.txt")
    with open(test_file) as test_input:
        return yatest.common.canonical_execute(query_filter, stdin=test_input)
