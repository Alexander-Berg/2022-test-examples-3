from datetime import timedelta, datetime

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
    delete_notification,
    then_receive_notification,
)


def test_delete_pending(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=TOMORROW),
        group_key,
        event_key,
    )
    resp = delete_notification(api_url, group_key, event_key)

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


def test_delete_nonexist(api_url, target, group_key, event_key):
    resp = delete_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'not_found',
        })
    )


def test_delete_cancelled(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=TOMORROW),
        group_key,
        event_key,
    )
    cancel_notification(api_url, group_key, event_key)

    resp = delete_notification(api_url, group_key, event_key)

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


def test_delete_completed(api_url, target, group_key, event_key):
    create_and_fetch(api_url, target, group_key, event_key)
    then_receive_notification(target)
    wait_for_status('notified', api_url, group_key=group_key, event_key=event_key)

    resp = delete_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
                'status': 'notified',
            })
        })
    )


def test_delete_failed(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=OUTDATED_EVENT_START),
        group_key,
        event_key,
    )

    wait_for_status('failed', api_url, group_key=group_key, event_key=event_key)

    resp = delete_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
                'status': 'failed',
            })
        })
    )


def test_delete_rejected(api_url, target, group_key, event_key, callback_uri):
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

    resp = delete_notification(api_url, group_key, event_key)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'group_key': group_key,
                'event_key': event_key,
                'status': 'rejected',
            })
        })
    )


def test_delete_soon(api_url, target, group_key, event_key, callback_uri):
    create_notification(
        api_url,
        create_notification_post_data(
            target.url + callback_uri, run_at=datetime.utcnow() + timedelta(seconds=30)
        ),
        group_key,
        event_key,
    )

    resp = delete_notification(api_url, group_key, event_key)
    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'too_close_to_run',
            'data': has_entries({
                'rec': has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                    'status': 'pending',
                })
            })
        })
    )
    # Cleanup
    delete_notification(api_url, group_key=group_key, event_key=event_key, force=True)
