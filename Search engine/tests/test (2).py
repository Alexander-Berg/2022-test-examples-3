# coding: utf-8

from yatest import common


def test():
    return common.canonical_execute(common.binary_path("search/geo/tools/golovan/extevlogproc/extevlogproc"),
        ["-p", "geometasearch", "-z", "--hr", common.source_path("search/geo/tools/golovan/extevlogproc/tests/test1_middle_geometasearch_eventlog")])
