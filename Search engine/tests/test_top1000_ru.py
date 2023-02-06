# coding=utf-8
from test_outentity import run_subtest


def test_top1000_names_ru():
    return run_subtest('top1000.names.ru')


def test_top1000_facts_ru():
    return run_subtest('top1000.facts.ru')


def test_top1000_wikisnippet_ru():
    return run_subtest('top1000.wikisnippet.ru')


def test_top1000_descr_ru():
    return run_subtest('top1000.descr.ru')


def test_top1000_search_req_ru():
    return run_subtest('top1000.search_req.ru')


def test_top1000_image_ru():
    return run_subtest('top1000.image.ru')
