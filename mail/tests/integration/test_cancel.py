from hamcrest import assert_that, has_entries
from library.python.testing.pyremock.lib.pyremock import MockResponse

from mail.callmeback.tests.integration.helpers import (
    TOMORROW,
    OUTDATED_EVENT_START,
    make_match_request,
    create_notification_post_data,
    create_notification,
    wait_for_status,
    create_and_fetch,
    cancel_notification,
    then_receive_notification,
)


def test_cancel_pending(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=TOMORROW),
        group_key,
        event_key,
    )
    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
                'status': 'cancelled',
            })
        })
    )


def test_cancel_nonexist(api_url, target, group_key, event_key):
    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'not_found',
        })
    )


def test_cancel_already_cancelled(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=TOMORROW),
        group_key,
        event_key,
    )
    cancel_notification(api_url, group_key, event_key)

    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'already_cancelled',
            'data': has_entries({
                'event_status': 'cancelled',
            })
        })
    )


def test_cancel_completed(api_url, target, group_key, event_key):
    create_and_fetch(api_url, target, group_key, event_key)
    then_receive_notification(target)
    wait_for_status('notified', api_url, group_key=group_key, event_key=event_key)

    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'illegal_status',
            'data': has_entries({
                'event_status': 'notified',
            })
        })
    )


def test_cancel_failed(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=OUTDATED_EVENT_START),
        group_key,
        event_key,
    )

    wait_for_status('failed', api_url, group_key=group_key, event_key=event_key)

    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'illegal_status',
            'data': has_entries({
                'event_status': 'failed',
            })
        })
    )


def test_cancel_rejected(api_url, target, group_key, event_key, callback_uri):
    reject_header = {'X-Ya-CallMeBack-Notify-Reject': '1'}
    target.expect(make_match_request(callback_uri), MockResponse(headers=reject_header))
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        event_key,
    )

    then_receive_notification(target)
    wait_for_status('rejected', api_url, group_key=group_key, event_key=event_key)

    resp = cancel_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'illegal_status',
            'data': has_entries({
                'event_status': 'rejected',
            })
        })
    )
