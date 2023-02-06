from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Dict, Any
from urllib.parse import quote_plus

import requests
from dateutil.tz import tzutc
from hamcrest import is_, all_of, has_entry, equal_to_ignoring_case
from library.python.testing.pyremock.lib.pyremock import MatchRequest

import backoff
from mail.callmeback.callmeback.detail.http_helpers.middleware import validate
from mail.callmeback.callmeback.swagger import SWAGGER_SCHEMA

CALLBACK_URI = '/callback'

TOMORROW = datetime.now(tz=tzutc()) + timedelta(days=1)
BASE_EVENT_START = datetime.now(tz=tzutc()) - timedelta(days=1)
OUTDATED_EVENT_START = datetime.now(tz=tzutc()) - timedelta(days=100)


@dataclass
class RequestInfo:
    group_key: str = None
    event_key: str = None
    method: str = 'get'
    headers: Dict[str, str] = field(default_factory=dict)
    query_params: Dict[str, str] = field(default_factory=dict)


def make_match_request(target_uri, req=RequestInfo()):
    return MatchRequest(
        method=is_(req.method),
        path=is_(target_uri),
        headers=all_of(*[has_entry(equal_to_ignoring_case(name), [value]) for name, value in req.headers.items()]),
        params=all_of(*[has_entry(name, [value.encode()]) for name, value in req.query_params.items()]),
    )


def create_notification_post_data(target_uri, req=RequestInfo(), run_at=BASE_EVENT_START, retry_params=None):
    post_data = {
        "run_at": run_at.strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
        "cb_url": target_uri,
        "context": {
            "_http": {
                "method": req.method,
                "headers": req.headers,
                "params": req.query_params,
            }
        }
    }
    if retry_params:
        post_data['context']['retry_params'] = retry_params
    return post_data


def create_notification(
        api_url: str,
        post_data: Dict,
        group_key,
        event_key,
):
    resp = requests.post(f'{api_url}/v1/event/add/{group_key}/{event_key}', json=post_data)
    return resp


def fetch_notification(api_url, group_key: str = None, event_key: str = None):
    assert group_key is not None or event_key is not None
    return requests.get(f'{api_url}/v1/event/{group_key}/{event_key}')


def wait_for_param(
        param_name: str, param_value: Any, api_url: str, group_key: str = None, event_key: str = None,
        min_timeout=timedelta(milliseconds=10), max_timeout=timedelta(seconds=1)
):
    def retry(resp):
        try:
            return resp.json()['data'][param_name] != param_value
        except:
            return False

    @backoff.on_predicate(backoff.expo, retry, max_time=max_timeout.total_seconds(), factor=min_timeout.total_seconds())
    def wait():
        return fetch_notification(api_url, group_key=group_key, event_key=event_key)
    resp = wait()
    real_value = resp.json()['data'][param_name]
    assert resp.json()['data'][param_name] == param_value, f'Could not wait for param {param_name}={param_value}, {real_value}'
    return resp


def wait_for_status(
        status: str, api_url: str, group_key: str = None, event_key: str = None,
        min_timeout=timedelta(milliseconds=10), max_timeout=timedelta(seconds=1)
):
    return wait_for_param(
        param_name='status', param_value=status, api_url=api_url, group_key=group_key, event_key=event_key, min_timeout=min_timeout, max_timeout=max_timeout
    )


def create_and_fetch(api_url, target, group_key, event_key, run_at=BASE_EVENT_START):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    callback_uri = f'{CALLBACK_URI}/{quote_plus(group_key)}'
    target.expect(make_match_request(callback_uri, req))

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, req, run_at=run_at),
        req.group_key,
        req.event_key,
    )
    return fetch_notification(api_url, group_key, event_key)


def find_notifications(api_url, group_key: str, event_key_prefix: str):
    url = f'{api_url}/v1/find/{group_key}'
    if event_key_prefix:
        url = f'{url}?event_key_prefix={event_key_prefix}'
    return requests.get(url)


def list_notifications(api_url, group_key: str):
    return requests.get(f'{api_url}/v1/event/{group_key}')


def cancel_notification(api_url, group_key: str, event_key: str):
    return requests.post(f'{api_url}/v1/event/cancel/{group_key}/{event_key}')


def delete_notification(api_url, group_key: str, event_key: str, force: bool = False):
    return requests.post(f'{api_url}/v1/event/delete/{group_key}/{event_key}?force={int(force)}')


def update_notification(api_url, group_key: str, event_key: str, ensure_pending: bool = False, **kwargs):
    return requests.post(
        f'{api_url}/v1/event/update/{group_key}/{event_key}',
        params=dict(ensure_pending=int(ensure_pending)),
        json=kwargs,
    )


def then_receive_notification(target, **wait_kwargs):
    target.wait_for(**wait_kwargs)
    target.assert_expectations()


def validate_response(resp, path, method='get'):
    return validate(resp.json(), SWAGGER_SCHEMA['paths'][path][method]['responses']['200']['schema'])


def cleanup(api_url, group_key: str):
    for event in list_notifications(api_url, group_key).json()['data']:
        delete_notification(api_url, group_key, event['event_key'], force=True)


__all__ = [
    'CALLBACK_URI',
    'EPOCH_START',
    'TOMORROW',
    'RequestInfo',
    'make_match_request',
    'create_notification_post_data',
    'create_notification',
    'fetch_notification',
    'wait_for_status',
    'wait_for_param',
    'create_and_fetch',
    'find_notifications',
    'list_notifications',
    'cancel_notification',
    'delete_notification',
    'update_notification',
    'then_receive_notification',
    'validate_response',
    'cleanup',
]
