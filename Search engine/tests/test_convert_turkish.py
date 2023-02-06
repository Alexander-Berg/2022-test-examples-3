# coding=utf-8
from test_convert_outentity import run_subtest


def test_turkish():
    return run_subtest('turkish', False)


def test_turkish_diff():
    return run_subtest('turkish', True)


def test_turkish_names():
    return run_subtest('turkish.names', False)


def test_turkish_names_diff():
    return run_subtest('turkish.names', True)


def test_turkish_facts():
    return run_subtest('turkish.facts', False)


def test_turkish_facts_diff():
    return run_subtest('turkish.facts', True)


def test_turkish_wikisnippet():
    return run_subtest('turkish.wikisnippet', False)


def test_turkish_wikisnippet_diff():
    return run_subtest('turkish.wikisnippet', True)


def test_turkish_image():
    return run_subtest('turkish.image', False)


def test_turkish_image_diff():
    return run_subtest('turkish.image', True)


def test_turkish_descr():
    return run_subtest('turkish.descr', False)


def test_turkish_descr_diff():
    return run_subtest('turkish.descr', True)


def test_turkish_search_req():
    return run_subtest('turkish.search_req', False)


def test_turkish_search_req_diff():
    return run_subtest('turkish.search_req', True)
