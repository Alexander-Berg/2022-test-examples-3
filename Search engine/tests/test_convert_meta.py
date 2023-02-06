# coding=utf-8
from test_convert_outentity import run_subtest


def test_meta():
    return run_subtest('meta', False)


def test_meta_diff():
    return run_subtest('meta', True)


def test_meta_names():
    return run_subtest('meta.names', False)


def test_meta_names_diff():
    return run_subtest('meta.names', True)


def test_meta_facts():
    return run_subtest('meta.facts', False)


def test_meta_facts_diff():
    return run_subtest('meta.facts', True)


def test_meta_wikisnippet():
    return run_subtest('meta.wikisnippet', False)


def test_meta_wikisnippet_diff():
    return run_subtest('meta.wikisnippet', True)


def test_meta_image():
    return run_subtest('meta.image', False)


def test_meta_image_diff():
    return run_subtest('meta.image', True)


def test_meta_descr():
    return run_subtest('meta.descr', False)


def test_meta_descr_diff():
    return run_subtest('meta.descr', True)


def test_meta_search_req():
    return run_subtest('meta.search_req', False)


def test_meta_search_req_diff():
    return run_subtest('meta.search_req', True)
