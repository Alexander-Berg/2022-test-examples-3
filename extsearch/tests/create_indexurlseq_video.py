# coding: utf-8

from yatest import common


def test_create_idexurlseq_video():
    return common.canonical_execute(common.binary_path("extsearch/video/indexer/indexurlseq/create/create_indexurlseq"),
        [
            "-u", common.data_path("video/create_indexurlseq/url.dat"), "-q", common.data_path("wizard/language"),
            "-g", common.data_path("video/create_indexurlseq/geoa.c2p"), "-i", "index.urlseq",
            "--debug-output",
        ])
