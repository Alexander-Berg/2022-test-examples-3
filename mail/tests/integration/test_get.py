# coding: utf-8
from hamcrest import assert_that, has_entries, contains_inanyorder, is_not

from mail.callmeback.tests.integration.helpers import (
    TOMORROW,
    RequestInfo,
    make_match_request,
    create_notification_post_data,
    create_notification,
    create_and_fetch,
    find_notifications,
    list_notifications,
    validate_response,
)


def test_get_schema(api_url, target, group_key, event_key):
    resp = create_and_fetch(api_url, target, group_key, event_key, run_at=TOMORROW)
    assert resp.status_code == 200
    validate_response(resp, '/v1/event/{group_key}/{event_key}')


def test_get_data(api_url, target, group_key, event_key):
    """Retrieve of just created notification should work"""
    resp = create_and_fetch(api_url, target, group_key, event_key, run_at=TOMORROW)
    assert resp.status_code == 200

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
                'status': 'pending',
            })
        })
    )


def test_find_data(api_url, target, group_key, event_key):
    channels = ["SMS", "XIVA", "SUP"]
    for channel in channels:
        resp = create_and_fetch(api_url, target, group_key, f"{event_key}_{channel}", run_at=TOMORROW)
        assert resp.status_code == 200

    resp = find_notifications(api_url, group_key, event_key)
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_SMS',
                }),
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_XIVA',
                }),
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_SUP',
                })
            )
        })
    )


def test_find_data_with_wildcards(api_url, target, group_key, event_key):
    wildcard_key = f'{event_key}_%_A'
    resp = create_and_fetch(api_url, target, group_key, wildcard_key, run_at=TOMORROW)
    assert resp.status_code == 200

    resp = find_notifications(api_url, group_key, f'{event_key}_%_')
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': wildcard_key,
                })
            )
        })
    )


def test_find_data_different_events(api_url, target, group_key, event_key):
    suffixes = ["ABCD", "ABD"]
    for suffix in suffixes:
        resp = create_and_fetch(api_url, target, group_key, f'{event_key}_{suffix}', run_at=TOMORROW)
        assert resp.status_code == 200

    resp = find_notifications(api_url, group_key, f'{event_key}_ABC')
    json = resp.json()

    assert_that(
        json,
        has_entries({
            'code': 200,
            'status': 'success',
            'data': contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_{suffixes[0]}'
                })
            )
        })
    )
    assert_that(
        json,
        has_entries({
            'data': is_not(contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_{suffixes[1]}'
                }))
            )
        })
    )


def test_find_data_empty_prefix(api_url, target, group_key, event_key):
    resp = create_and_fetch(api_url, target, group_key, event_key, run_at=TOMORROW)
    assert resp.status_code == 200

    resp = find_notifications(api_url, group_key, None)
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                })
            )
        })
    )


def test_list_schema(api_url, target, group_key, event_key, callback_uri):
    target.expect(make_match_request(callback_uri))

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        event_key,
    )

    resp = list_notifications(api_url, group_key)
    assert resp.status_code == 200
    validate_response(resp, '/v1/event/{group_key}')


def test_find_schema(api_url, target, group_key, event_key, callback_uri):
    target.expect(make_match_request(callback_uri))

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        f"{event_key}_SMS",
    )

    resp = find_notifications(api_url, group_key, event_key)
    assert resp.status_code == 200
    validate_response(resp, '/v1/find/{group_key}')


def test_list_data(api_url, target, group_key, event_key, callback_uri):
    target.expect(make_match_request(callback_uri, RequestInfo()), times=2)
    req_data = create_notification_post_data(target.url + callback_uri)
    create_notification(api_url, req_data, group_key=group_key, event_key=f'{event_key}_1')
    create_notification(api_url, req_data, group_key=group_key, event_key=f'{event_key}_2')

    resp = list_notifications(api_url, group_key)
    assert resp.status_code == 200

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': contains_inanyorder(
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_1',
                }),
                has_entries({
                    'group_key': group_key,
                    'event_key': f'{event_key}_2',
                })
            )
        })
    )


def test_run_at(api_url, target, group_key, event_key):
    """Run_at of created notification should return right unixtime"""
    resp = create_and_fetch(api_url, target, group_key, event_key, run_at=TOMORROW)
    assert resp.status_code == 200

    assert_that(
        resp.json(),
        has_entries(
            {'data': has_entries({'run_at': TOMORROW.strftime('%Y-%m-%dT%H:%M:%S.%fZ')})}
        ),
    )
