# coding=utf-8
from test_outentity import run_subtest


def test_turkish():
    return run_subtest('turkish')


def test_turkish_names():
    return run_subtest('turkish.names')


def test_turkish_facts():
    return run_subtest('turkish.facts')


def test_turkish_wikisnippet():
    return run_subtest('turkish.wikisnippet')


def test_turkish_image():
    return run_subtest('turkish.image')


def test_turkish_descr():
    return run_subtest('turkish.descr')


def test_turkish_search_req():
    return run_subtest('turkish.search_req')
