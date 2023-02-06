# coding=utf-8
from test_convert_outentity import run_subtest


def test_top1000_names_ru():
    return run_subtest('top1000.names.ru', False)


def test_top1000_names_ru_diff():
    return run_subtest('top1000.names.ru', True)


def test_top1000_facts_ru():
    return run_subtest('top1000.facts.ru', False)


def test_top1000_facts_ru_diff():
    return run_subtest('top1000.facts.ru', True)


def test_top1000_wikisnippet_ru():
    return run_subtest('top1000.wikisnippet.ru', False)


def test_top1000_wikisnippet_ru_diff():
    return run_subtest('top1000.wikisnippet.ru', True)


def test_top1000_descr_ru():
    return run_subtest('top1000.descr.ru', False)


def test_top1000_descr_ru_diff():
    return run_subtest('top1000.descr.ru', True)


def test_top1000_search_req_ru():
    return run_subtest('top1000.search_req.ru', False)


def test_top1000_search_req_ru_diff():
    return run_subtest('top1000.search_req.ru', True)


def test_top1000_image_ru():
    return run_subtest('top1000.image.ru', False)


def test_top1000_image_ru_diff():
    return run_subtest('top1000.image.ru', True)
