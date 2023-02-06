# coding: utf-8

from yatest.common import source_path

from market.idx.marketindexer.marketindexer.mbodata import calc_baby_goods_hids, BABY_GOODS_HID


def test_calc_baby_goods_hids():
    tovar_tree_path = source_path('market/idx/generation/yatf/resources/books_indexer/stubs/tovar-tree.pb')
    hids = calc_baby_goods_hids(tovar_tree_path)

    assert BABY_GOODS_HID in hids
    assert len(hids) == 326
