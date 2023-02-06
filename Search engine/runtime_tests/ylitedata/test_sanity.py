# coding=utf-8

from requests import get

import os.path

from . import SerpDataHtml

from runtime.queries import SearchUrl


__author__ = 'aokhotin'


def read_sanity_test_data(path):
    with open(path) as f:
        lines = f.readlines()
    return [line.strip() for line in lines]


def test_sanity(query, stage):
    su = SearchUrl(url_string=stage, query=query)
    resp = get(su.string, verify=False)
    resp.raise_for_status()
    data = SerpDataHtml(resp.content)
    assert data.urls is not None and len(data.urls) != 0, "No results found"


def pytest_generate_tests(metafunc):
    beta = metafunc.config.option.beta
    prod = metafunc.config.option.prod
    data_path = metafunc.config.option.test_data_path
    if "stage" in metafunc.fixturenames:
        metafunc.parametrize("stage", [beta, prod])
    if "query" in metafunc.fixturenames:
        data_file = os.path.join(data_path, "sanity.test.data")
        metafunc.parametrize("query", read_sanity_test_data(data_file))
