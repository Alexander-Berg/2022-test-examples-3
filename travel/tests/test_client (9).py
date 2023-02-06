# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from json import dumps as json_dumps

import httpretty

from travel.rasp.library.python.api_clients.juggler import JugglerClient

JUGGLER_TEST_HOST = 'http://juggler.ru'
TEST_SOURCE = 'source'


def _register_handler(response=None, status_code=200, path='/events'):
    def request_callback(request, uri, response_headers):
        return [status_code, response_headers, json_dumps(response).encode('utf-8')]

    httpretty.register_uri(
        httpretty.POST,
        JUGGLER_TEST_HOST + path,
        status=status_code,
        content_type='application/json; charset=UTF-8',
        body=request_callback
    )


@httpretty.activate
def test_ok():
    _register_handler({
        'events' : [
            {'code' : 200, 'message' : 'OK'}
        ],
        'accepted_events' : 1,
        'success' : True
    })

    result = JugglerClient(host=JUGGLER_TEST_HOST, source=TEST_SOURCE).send([{
        'description': 'test description',
        'host': 'test.host',
        'instance': '',
        'service': 'test.service',
        'status': 'OK',
        'tags': ['test.tag']
    }])

    assert result


@httpretty.activate
def test_fail():
    _register_handler({
        'message' : 'Expecting value: line 1 column 1 (char 0)',
        'success' : False
    }, 400)

    result = JugglerClient(host=JUGGLER_TEST_HOST, source=TEST_SOURCE).send([{
        'description': 'test description',
        'host': 'test.host',
        'instance': '',
        'service': 'test.service',
        'status': 'OK',
        'tags': ['test.tag']
    }])

    assert not result


@httpretty.activate
def test_partial_fail():
    _register_handler({
        'events' : [
            {'code' : 200, 'message' : 'OK'},
            {'code' : 400, 'message' : 'service not provided'}
        ],
        'accepted_events' : 1,
        'success' : True
    }, 200)

    result = JugglerClient(host=JUGGLER_TEST_HOST, source=TEST_SOURCE).send([{
        'description': 'test description',
        'host': 'test.host',
        'instance': '',
        'service': 'test.service',
        'status': 'OK',
        'tags': ['test.tag']
    }, {
        'description': 'test description',
        'host': 'test.host',
        'instance': '',
        'status': 'OK',
        'tags': ['test.tag']
    }
    ])

    assert result
