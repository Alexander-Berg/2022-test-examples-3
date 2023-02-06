# coding: utf-8

import pytest
from yatest import common


test_data = [
    ["/dev/null", "-"],
    ["-", "/dev/null"],
]


@pytest.mark.parametrize("gzt, dst", test_data, ids=["db", "gzt"])
def test(gzt, dst):
    return common.canonical_execute(
        common.binary_path("search/wizard/entitysearch/tools/convertor/convertor"),
        [
            "--no-full-db",
            "--gzt",
            gzt,
            "--output-type",
            "json",
            "--dst",
            dst,
            "-j",
            "1",
            "--src",
            common.source_path("search/wizard/entitysearch/data/convertor_test_data/db.trie"),
        ],
    )
