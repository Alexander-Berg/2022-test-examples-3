# -*- coding: utf-8 -*-

from market.idx.pylibrary.mindexer_core.qbid.yt_cluster import BidsCluster


def test_list_tables(yt_stuff):
    yt = yt_stuff.get_yt_client()
    yt.create('table', '//foo/offer/bar', recursive=True)
    yt.create('table', '//foo/offer/201704', recursive=True)
    yt.create('table', '//foo/offer/201703', recursive=True)
    yt.create('table', '//foo/model/2017035', recursive=True)
    yt.create('table', '//foo/model/201702', recursive=True)

    cluster = BidsCluster(
        proxy=yt_stuff.get_server(),
        table_dir='//foo',
    )

    expected_tables = frozenset([
        'model/201702',
        'offer/201703',
        'offer/201704',
    ])
    assert frozenset(cluster.list_tables()) == expected_tables
