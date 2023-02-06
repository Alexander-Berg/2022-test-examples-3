# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import httpretty

from travel.rasp.library.python.api_clients.ticket_daemon import TicketDaemonClient


HOST = 'http://test-ticket-daemon.ru'


def register_query(path, responses, status_code=200):
    httpretty.register_uri(
        httpretty.GET, '{}/jsendapi/{}'.format(HOST, path),
        responses=[
            httpretty.Response(body=json.dumps({'status': 'success', 'data': response_data}))
            for response_data in responses
        ],
        status=status_code,
        content_type='application/json'
    )


def get_ticket_daemon_client():
    return TicketDaemonClient(
        host=HOST,
        disable_retry_config=True,
        disable_timeout=True,
        disable_circuit_breaker_config=True
    )


@httpretty.activate
def test_init_search():
    qid = 'search_qid'
    register_query(path='init_search/', responses=[{'qid': qid}])
    result = get_ticket_daemon_client().init_search({})
    assert result['qid'] == qid


@httpretty.activate
def test_init_search_no_content():
    register_query(path='init_search/', responses=[], status_code=204)
    result = get_ticket_daemon_client().init_search({})
    assert result is None
