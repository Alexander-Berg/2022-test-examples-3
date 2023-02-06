# coding=utf-8
from test_outentity import run_subtest


def test_top1000_names_ua():
    return run_subtest('top1000.names.ua')


def test_top1000_facts_ua():
    return run_subtest('top1000.facts.ua')


def test_top1000_wikisnippet_ua():
    return run_subtest('top1000.wikisnippet.ua')


def test_top1000_descr_ua():
    return run_subtest('top1000.descr.ua')


def test_top1000_search_req_ua():
    return run_subtest('top1000.search_req.ua')


def test_top1000_image_ua():
    return run_subtest('top1000.image.ua')
