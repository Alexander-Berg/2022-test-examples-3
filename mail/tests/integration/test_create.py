# coding: utf-8
from datetime import timedelta

from hamcrest import is_, all_of, assert_that, has_entry, has_entries
from library.python.testing.pyremock.lib.matchers import is_json_serialized
from pytest import mark, param

from mail.callmeback.tests.integration.helpers import (
    BASE_EVENT_START,
    TOMORROW,
    RequestInfo,
    make_match_request,
    create_notification_post_data,
    create_notification,
    create_and_fetch,
    then_receive_notification,
)


@mark.parametrize(
    'req',
    (
        param(RequestInfo(method='get'), id='get'),
        param(RequestInfo(method='get', headers={'X-Some-H': 'Foo', 'X-Another-H': 'Bar'}), id='get-with-headers'),
        param(RequestInfo(method='get', query_params={'q': '1', 'b': 'foo'}), id='get-with-params'),
        param(RequestInfo(method='post'), id='post'),
        param(RequestInfo(method='post', headers={'X-Some-H': 'Foo', 'X-Another-H': 'Bar'}), id='post-with-headers'),
        param(RequestInfo(method='post', query_params={'q': '1', 'b': 'foo'}), id='post-with-params'),
    ),
)
def test_basic(api_url, target, group_key, event_key, req, callback_uri):
    """Check that creation of notify leads to HTTP callback"""
    target.expect(make_match_request(callback_uri, req))

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, req),
        group_key,
        event_key,
    )

    then_receive_notification(target)


def test_add_callmeback_headers(api_url, target, group_key, event_key, callback_uri):
    """Check that passing `context.add_callmeback_headers: True` leads to X-Ya-CallMeBack headers in callback request"""
    expected_req = RequestInfo(
        headers={
            'X-Ya-CallMeBack-Group-Key': group_key,
            'X-Ya-CallMeBack-Event-Key': event_key,
        }
    )
    target.expect(make_match_request(callback_uri, expected_req))

    post_data = create_notification_post_data(target.url + callback_uri)
    post_data['context']['_http']['add_callmeback_headers'] = True
    create_notification(api_url, post_data, group_key, event_key)

    then_receive_notification(target)


def test_post_json(api_url, target, group_key, event_key, callback_uri):
    """Check passing post data in `context.json = obj`"""
    json_body = {'a': 'b'}
    req = RequestInfo(method='post')
    match_req = make_match_request(callback_uri, req)
    match_req.body = is_json_serialized(has_entries(json_body))
    target.expect(match_req)

    post_data = create_notification_post_data(target.url + callback_uri, req)
    post_data['context']['_http']['json'] = json_body
    create_notification(api_url, post_data, group_key, event_key)

    then_receive_notification(target)


def test_post_data(api_url, target, group_key, event_key, callback_uri):
    """Check passing post data in `context.data = text`"""
    data = 'Secret level'
    req = RequestInfo(method='post')
    match_req = make_match_request(callback_uri, req)
    match_req.body = is_(data)
    target.expect(match_req)

    post_data = create_notification_post_data(target.url + callback_uri, req)
    post_data['context']['_http']['data'] = data
    create_notification(api_url, post_data, group_key, event_key)

    then_receive_notification(target)


def test_merge_query_params(api_url, target, group_key, event_key, callback_uri):
    query_params = {'a': 'bar', 'b': 'baz'}
    req = RequestInfo(query_params=query_params)
    match_req = make_match_request(callback_uri, req)
    match_req.params = all_of(
        has_entry('b', [b'baz']),
        has_entry('a', [b'foo', b'bar'])
    )
    target.expect(match_req)

    post_data = create_notification_post_data(target.url + callback_uri + '?a=foo', req)
    create_notification(api_url, post_data, group_key, event_key)

    then_receive_notification(target)


def test_create_duplicates_fails(api_url, target, group_key, event_key, callback_uri):
    resp = create_and_fetch(api_url, target, group_key, event_key, run_at=TOMORROW)
    assert_that(resp.status_code, is_(200))

    resp = create_notification(
        api_url,
        create_notification_post_data(
            target.url + callback_uri,
            RequestInfo(group_key=group_key, event_key=event_key),
        ),
        group_key,
        event_key,
    )
    assert_that(resp.status_code, is_(400))
    assert_that(
        resp.json(),
        has_entries({
            'status': "already_exists",
            'message': "Event already exists",
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
            })
        })
    )


def test_create_late_then_soon(api_url, target, group_key, event_key, callback_uri):
    late_req = RequestInfo(group_key=group_key, event_key=f'{event_key}_1')
    target.expect(make_match_request(callback_uri, late_req))
    create_notification(
        api_url,
        create_notification_post_data(
            target.url + callback_uri, late_req, run_at=BASE_EVENT_START + timedelta(minutes=10)
        ),
        group_key,
        late_req.event_key,
    )

    soon_req = RequestInfo(group_key=group_key, event_key=f'{event_key}_2')
    target.expect(make_match_request(callback_uri, soon_req))
    create_notification(
        api_url,
        create_notification_post_data(
            target.url + callback_uri, soon_req, run_at=BASE_EVENT_START
        ),
        group_key,
        soon_req.event_key,
    )

    then_receive_notification(target)


@mark.parametrize(
    "retry_params",
    [
        {'stop_after_delay': '3600sadfg'},
        {'quick_retries_count': '11asd'},
        {'strategy': 'exponential', 'base_delay': 130, 'delay_growth_rate': 'saf'},
        {'strategy': 'exponential', 'base_delay': 130},
        {'strategy': 'constant', 'base_delay': 'asd'},
        {'strategy': 'constant'},
        {'strategy': 'unknown_strategy'},
    ],
)
def test_create_with_bad_retry_params(api_url, target, group_key, event_key, callback_uri, retry_params):
    resp = create_notification(
        api_url,
        create_notification_post_data(
            target.url + callback_uri,
            retry_params=retry_params,
        ),
        group_key,
        event_key,
    )
    assert_that(resp.status_code, is_(400))
    assert_that(resp.json(), has_entries({'status': "fail"}))


@mark.parametrize(
    "callback_url",
    [
        'ya.ru',
        '1234',
        'iva.qloud-c.yandex.net',
    ],
)
def test_create_with_bad_cb_url(api_url, target, group_key, event_key, callback_url):
    resp = create_notification(
        api_url,
        create_notification_post_data(
            callback_url
        ),
        group_key,
        event_key,
    )
    assert_that(resp.status_code, is_(400))
    assert_that(resp.json(), has_entries({'status': "fail"}))
