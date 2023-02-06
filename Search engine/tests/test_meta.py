# coding=utf-8
from test_outentity import run_subtest


def test_meta():
    return run_subtest('meta')


def test_meta_names():
    return run_subtest('meta.names')


def test_meta_facts():
    return run_subtest('meta.facts')


def test_meta_wikisnippet():
    return run_subtest('meta.wikisnippet')


def test_meta_image():
    return run_subtest('meta.image')


def test_meta_descr():
    return run_subtest('meta.descr')


def test_meta_search_req():
    return run_subtest('meta.search_req')
