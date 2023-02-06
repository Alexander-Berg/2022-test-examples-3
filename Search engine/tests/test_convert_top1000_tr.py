# coding=utf-8
from test_convert_outentity import run_subtest


def test_top1000_names_tr():
    return run_subtest('top1000.names.tr', False)


def test_top1000_names_tr_diff():
    return run_subtest('top1000.names.tr', True)


def test_top1000_facts_tr():
    return run_subtest('top1000.facts.tr', False)


def test_top1000_facts_tr_diff():
    return run_subtest('top1000.facts.tr', True)


def test_top1000_wikisnippet_tr():
    return run_subtest('top1000.wikisnippet.tr', False)


def test_top1000_wikisnippet_tr_diff():
    return run_subtest('top1000.wikisnippet.tr', True)


def test_top1000_descr_tr():
    return run_subtest('top1000.descr.tr', False)


def test_top1000_descr_tr_diff():
    return run_subtest('top1000.descr.tr', True)


def test_top1000_search_req_tr():
    return run_subtest('top1000.search_req.tr', False)


def test_top1000_search_req_tr_diff():
    return run_subtest('top1000.search_req.tr', True)


def test_top1000_image_tr():
    return run_subtest('top1000.image.tr', False)


def test_top1000_image_tr_diff():
    return run_subtest('top1000.image.tr', True)
