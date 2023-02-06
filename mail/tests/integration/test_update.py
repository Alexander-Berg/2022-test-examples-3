from datetime import timedelta

from _pytest.mark import param
from hamcrest import assert_that, has_entries
from library.python.testing.pyremock.lib.pyremock import MockResponse
from pytest import mark

from mail.callmeback.tests.integration.helpers import (
    CALLBACK_URI,
    TOMORROW,
    OUTDATED_EVENT_START,
    RequestInfo,
    make_match_request,
    create_notification_post_data,
    create_notification,
    wait_for_status,
    create_and_fetch,
    cancel_notification,
    update_notification,
    then_receive_notification,
)


NEW_CB_URL = 'http://new_url/'


@mark.parametrize(
    'new_fields',
    (
        param({}, id='empty'),
        param({'cb_url': NEW_CB_URL}, id='cb_url'),
        param({'run_at': TOMORROW + timedelta(days=1)}, id='run_at'),
        param({'cb_url': NEW_CB_URL, 'run_at': TOMORROW + timedelta(days=1)}, id='both'),
    ),
)
def test_update_pending(api_url, target, group_key, event_key, new_fields):
    if 'run_at' in new_fields:
        new_fields['run_at'] = new_fields['run_at'].strftime('%Y-%m-%dT%H:%M:%S.%fZ')
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        req.group_key,
        req.event_key,
    )
    resp = update_notification(api_url, group_key, event_key, **new_fields)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'old_status': 'pending',
                'rec': has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                    **new_fields
                }),
            })
        })
    )


def test_update_do_not_nullify_other_fields(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_data = create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW)
    create_notification(api_url, create_data, req.group_key, req.event_key)
    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'old_status': 'pending',
                'rec': has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                    'context': create_data['context'],
                }),
            })
        })
    )


@mark.parametrize(
    'context',
    (
        param({}, id='empty'),
        param({'foo': 'bar'}, id='cb_url'),
    ),
)
def test_update_context(api_url, target, group_key, event_key, context):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        group_key,
        event_key,
    )
    resp = update_notification(api_url, group_key, event_key, context=context)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'old_status': 'pending',
                'rec': has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                    'context': has_entries(context)
                }),
            })
        })
    )


def test_update_nonexist(api_url, target, group_key, event_key):
    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'not_found',
        })
    )


def test_update_already_cancelled(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        group_key,
        event_key,
    )
    cancel_notification(api_url, group_key, event_key)

    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)

    assert_that(
        resp.json(),
        has_entries({
            'code': 400,
            'status': 'illegal_status',
            'data': has_entries({
                'event_status': 'cancelled',
            })
        })
    )


def test_update_already_cancelled_force(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        req.group_key,
        req.event_key,
    )
    cancel_notification(api_url, group_key, event_key)

    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL, ensure_pending=True)

    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'old_status': 'cancelled',
                'rec': has_entries({
                    'group_key': group_key,
                    'event_key': event_key,
                    'cb_url': NEW_CB_URL,
                })
            })
        })
    )


def test_update_completed(api_url, target, group_key, event_key):
    create_and_fetch(api_url, target, group_key, event_key)
    then_receive_notification(target)
    wait_for_status('notified', api_url, group_key=group_key, event_key=event_key)

    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)

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


def test_update_failed(api_url, target, group_key, event_key):
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, run_at=OUTDATED_EVENT_START),
        group_key,
        event_key,
    )

    wait_for_status('failed', api_url, group_key=group_key, event_key=event_key)

    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)

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


def test_update_rejected(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    reject_header = {'X-Ya-CallMeBack-Notify-Reject': '1'}
    target.expect(make_match_request(CALLBACK_URI, req), MockResponse(headers=reject_header))
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req),
        req.group_key,
        req.event_key,
    )

    then_receive_notification(target)
    wait_for_status('rejected', api_url, group_key=group_key, event_key=event_key)

    resp = update_notification(api_url, group_key, event_key, cb_url=NEW_CB_URL)

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


def test_update_extra_fields(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        group_key,
        event_key,
    )
    resp = update_notification(api_url, group_key, event_key, fake_field='blah')

    assert resp.status_code == 200
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
        })
    )


def test_update_originally_run_at(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        group_key,
        event_key,
    )
    new_run_at = (TOMORROW + timedelta(days=1))
    resp = update_notification(api_url, group_key, event_key, run_at=new_run_at.strftime('%Y-%m-%dT%H:%M:%S.%fZ'))

    assert resp.status_code == 200
    assert_that(
        resp.json(),
        has_entries({
            'code': 200,
            'status': 'success',
            'data': has_entries({
                'rec': has_entries({
                    'run_at': new_run_at.strftime('%Y-%m-%dT%H:%M:%S.%fZ'),
                    'originally_run_at': int(new_run_at.timestamp()),
                })
            })
        })
    )


def test_update_bad_cb_url(api_url, target, group_key, event_key):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    create_notification(
        api_url,
        create_notification_post_data(target.url + CALLBACK_URI, req, run_at=TOMORROW),
        group_key,
        event_key,
    )

    resp = update_notification(api_url, group_key, event_key, cb_url="bad url")

    assert_that(resp.status_code, 400)
    assert_that(resp.json(), has_entries({'status': "fail"}))
