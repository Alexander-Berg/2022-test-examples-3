# coding: utf-8

import pytest
import requests

from market.idx.yatf.test_envs.report_env import ReportTestEnv


@pytest.yield_fixture(scope="module")
def report():
    with ReportTestEnv(**{}) as env:
        env.execute()
        env.verify()
        yield env


def test_report_ok(report):
    resp = requests.get(report.meta_search_url, params={
        'admin_action': 'versions',
    })
    assert resp.status_code == requests.codes.ok


def test_report_feed(report):
    resp = requests.get(report.meta_search_url, params={
        'place': 'offerinfo',
        'pp': 18,
        'rids': 213,
        'regset': 1,
        'show-urls': 'decrypted',
        'feed_shoffer_id': '101967-1'
    })
    assert 'error' not in resp.text.lower()
