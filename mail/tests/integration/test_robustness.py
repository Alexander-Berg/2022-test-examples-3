from datetime import timedelta, datetime

import pytest
from hamcrest import assert_that, has_entries
from library.python.testing.pyremock.lib.pyremock import MockResponse

# from mail.callmeback.callmeback.stages.worker.settings.event_marker import EventMarkerSettings
from mail.callmeback.tests.integration.helpers import (
    OUTDATED_EVENT_START,
    RequestInfo,
    make_match_request,
    create_notification_post_data,
    create_notification,
    wait_for_status,
    wait_for_param,
    then_receive_notification,
    delete_notification,
)
from mail.callmeback.tests.integration.helpers.autocheck import is_autocheck


def test_mark_event_failed_when_run_after_default_max_delay(api_url, target, group_key, event_key, callback_uri):
    # Expect no target calls for outdated events
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=OUTDATED_EVENT_START),
        group_key,
        event_key,
    )

    resp = wait_for_status('failed', api_url, group_key=group_key, event_key=event_key)
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


def test_mark_event_failed_when_run_after_specified_max_delay(api_url, target, group_key, event_key, callback_uri):
    event_time = datetime.utcnow() - timedelta(seconds=3600)
    retry_params = {'stop_after_delay': 3500}
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=event_time, retry_params=retry_params),
        group_key,
        event_key,
    )

    resp = wait_for_status('failed', api_url, group_key=group_key, event_key=event_key)
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


def test_mark_event_notified_when_run_before_specified_max_delay(api_url, target, group_key, event_key, callback_uri):
    req = RequestInfo(group_key=group_key, event_key=event_key)
    target.expect(make_match_request(callback_uri, req))
    event_time = datetime.utcnow() - timedelta(seconds=3600)
    retry_params = {'stop_after_delay': 3700}

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, run_at=event_time, retry_params=retry_params),
        group_key,
        event_key,
    )

    then_receive_notification(target)

    resp = wait_for_status('notified', api_url, group_key=group_key, event_key=event_key)

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


@pytest.mark.parametrize('quick_retries_count', [0, 3])
def test_postpone_event_with_quick_retries_count(api_url, target, group_key, event_key, callback_uri, quick_retries_count):
    target.expect(make_match_request(callback_uri), MockResponse(status=500), times=quick_retries_count+1)
    retry_params = {'quick_retries_count': quick_retries_count}

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri, retry_params=retry_params),
        group_key,
        event_key,
    )

    then_receive_notification(target)

    resp = wait_for_param('tries', 1, api_url, group_key=group_key, event_key=event_key)

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


# max_timeout must be greater than CallbackHeraldSettings.delay_on_fail but less than EventMarkerSettings.postpone_by
@pytest.mark.no_smoke
@pytest.mark.skipif(is_autocheck(), reason='Sensitive test, prone to timing errors')
def test_retry_after_failed_notification(api_url, target, group_key, event_key, callback_uri):
    target.expect(make_match_request(callback_uri), MockResponse(status=500))
    target.expect(make_match_request(callback_uri), MockResponse(status=200))

    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        event_key,
    )
    then_receive_notification(target, max_timeout=timedelta(milliseconds=500))


# max_timeout must be greater than CallbackHeraldSettings.delay_on_fail but less than EventMarkerSettings.postpone_by
@pytest.mark.no_smoke
@pytest.mark.skipif(is_autocheck(), reason='Sensitive test, prone to timing errors')
def test_giveup_after_max_tries(api_url, target, group_key, event_key, callback_uri):
    target.expect(make_match_request(callback_uri), MockResponse(status=500), times=5)
    # Expect no more calls after CallbackHerald._max_tries
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        event_key,
    )

    then_receive_notification(target, max_timeout=timedelta(milliseconds=500))
    # Cleanup
    delete_notification(api_url, group_key=group_key, event_key=event_key, force=True)


@pytest.mark.parametrize('response_status', [200, 400, 500])
def test_mark_event_rejected_by_reject_response_header(api_url, target, group_key, event_key, callback_uri, response_status):
    reject_header = {'X-Ya-CallMeBack-Notify-Reject': '1'}
    target.expect(
        make_match_request(callback_uri),
        MockResponse(status=response_status, headers=reject_header)
    )
    # Expect no more calls after responsing with reject header
    create_notification(
        api_url,
        create_notification_post_data(target.url + callback_uri),
        group_key,
        event_key,
    )

    then_receive_notification(target)
    resp = wait_for_status('rejected', api_url, group_key=group_key, event_key=event_key)
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
