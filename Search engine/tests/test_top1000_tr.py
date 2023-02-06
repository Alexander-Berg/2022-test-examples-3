# coding=utf-8
from test_outentity import run_subtest


def test_top1000_names_tr():
    return run_subtest('top1000.names.tr')


def test_top1000_facts_tr():
    return run_subtest('top1000.facts.tr')


def test_top1000_wikisnippet_tr():
    return run_subtest('top1000.wikisnippet.tr')


def test_top1000_descr_tr():
    return run_subtest('top1000.descr.tr')


def test_top1000_search_req_tr():
    return run_subtest('top1000.search_req.tr')


def test_top1000_image_tr():
    return run_subtest('top1000.image.tr')
