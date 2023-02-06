# coding: utf-8

import json
import logging
import os

import flask

log = logging.getLogger()
page = flask.Blueprint('ping', __name__)
content_type = 'text/plain; charset=utf-8'


def _getstate():
    return flask.current_app.state


@page.route('/ping')
def ping():
    try:
        state = flask.current_app.state
        body = state.ping()
        status = 200 if state.opened else 500
        return flask.Response(body, status=status, content_type=content_type)
    except Exception as e:
        log.exception(e)


def _is_localhost():
    return flask.request.remote_addr in [
        None,  # for tests
        '127.0.0.1',
        '::1',
    ]


@page.route('/close')
def close_balancer():
    if not _is_localhost():
        return flask.Response('Allowed only for localhost.\n', status=403, content_type=content_type)
    state = _getstate()
    state.close_balancer()
    body = state.ping()
    return flask.Response(body, status=200, content_type=content_type)


@page.route('/open')
def open_balancer():
    if not _is_localhost():
        return flask.Response('Allowed only for localhost.\n', status=403, content_type=content_type)
    state = _getstate()
    state.open_balancer()
    body = state.ping()
    return flask.Response(body, status=200, content_type=content_type)


@page.route('/status')
def status():
    try:
        env = {}
        for key, val in os.environ.iteritems():
            if 'token' in key.lower():
                val = '*'
            env[key] = val
        curapp = flask.current_app
        state = _getstate()
        data = {
            'opened': state.opened,
            'version': curapp.version,
            'args': curapp.settings.args_as_dict,
            # 'env': env,
        }
        body = json.dumps(data, indent=2, sort_keys=True)
        return flask.Response(body, status=200, content_type='application/json; charset=utf-8')
    except Exception as e:
        log.exception(e)


@page.route('/sleep/<int:seconds>', methods=['GET'])
def sleep(seconds=1):
    import time
    time.sleep(seconds)
    return flask.Response('ok\n', status=200, content_type='text/plain; charset=utf-8')
