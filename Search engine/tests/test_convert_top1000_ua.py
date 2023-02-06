# coding=utf-8
from test_convert_outentity import run_subtest


def test_top1000_names_ua():
    return run_subtest('top1000.names.ua', False)


def test_top1000_names_ua_diff():
    return run_subtest('top1000.names.ua', True)


def test_top1000_facts_ua():
    return run_subtest('top1000.facts.ua', False)


def test_top1000_facts_ua_diff():
    return run_subtest('top1000.facts.ua', True)


def test_top1000_wikisnippet_ua():
    return run_subtest('top1000.wikisnippet.ua', False)


def test_top1000_wikisnippet_ua_diff():
    return run_subtest('top1000.wikisnippet.ua', True)


def test_top1000_descr_ua():
    return run_subtest('top1000.descr.ua', False)


def test_top1000_descr_ua_diff():
    return run_subtest('top1000.descr.ua', True)


def test_top1000_search_req_ua():
    return run_subtest('top1000.search_req.ua', False)


def test_top1000_search_req_ua_diff():
    return run_subtest('top1000.search_req.ua', True)


def test_top1000_image_ua():
    return run_subtest('top1000.image.ua', False)


def test_top1000_image_ua_diff():
    return run_subtest('top1000.image.ua', True)
