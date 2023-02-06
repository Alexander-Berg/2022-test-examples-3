# coding: utf-8

import logging
import pytest
import requests
import six

from market.idx.api.backend.report import (
    BASE_WHITE,
    DEBUG_YES,
    MAGIC_PP,
    RIDS_MOSCOW,
    CLIENT_ID,
    ReportClient,
    check_contains,
    check_not_contains,
)
from core.matcher import Greater, Regex


@pytest.fixture()
def report_client():
    return ReportClient(
        base_url='http://example.yandex.net',
        generation_name='generation1',
        num_retries=3,
        timeout_seconds=5,
        logger=logging.getLogger(),
    )


def test_response_object():
    response = {'foo': {'bar': 1}}

    check_contains(response, {'bar': 1})
    check_contains(response, {'foo': {'bar': 1}})
    check_not_contains(response, {'bar': 2})
    check_not_contains(response, {'baz': 1})


def test_response_list():
    response = [1, 2, 3]

    check_contains(response, [1, 2])
    check_contains(response, 3)
    check_not_contains(response, 4)


def test_response_combo():
    response = {
        'one': [
            {
                'two': 'two',
                'three': [
                    {'four': 4},
                    {'five': 5},
                ],
                'six': "",
            },
        ]
    }

    check_contains(response, {'one': [{'three': [{'four': 4}]}]})
    check_contains(response, {'three': []})
    check_contains(response, {'four': Greater(3)})
    check_contains(response, {'two': Regex('.+')})
    check_not_contains(response, {'six': Regex('.+')})


def test_report_url(report_client):
    request = report_client.prepare_request('prime', {'foo': 123})
    expected_url = (
        'http://example.yandex.net/yandsearch?'
        'pp={}&place=prime&rids={}&base={}&debug={}&client={}&foo=123'.format(
            MAGIC_PP, RIDS_MOSCOW, BASE_WHITE, DEBUG_YES, CLIENT_ID
        )
    )
    assert expected_url == request.url


def test_report_request(report_client, monkeypatch):
    def send(obj, request, timeout):
        response = requests.Response()
        response.status_code = 200
        response._content = six.ensure_binary(
            '{"debug":{"brief":{"generation":"generation1"}},"bar":123}'
        )
        return response

    monkeypatch.setattr(requests.Session, 'send', send)

    response = report_client.request('prime', {'foo': 123})
    assert 123 == response['bar']
