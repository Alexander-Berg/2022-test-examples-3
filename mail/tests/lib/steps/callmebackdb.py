from datetime import timedelta
from tests_common.pytest_bdd import given, then
from mail.pypg.pypg.common import transaction

DEFAULT_GROUP_KEY = 'test_group_key'
DEFAULT_OWNER_CLIENT_ID = 42


def get_dsn(context):
    return context.config['callmebackdb']


@given('there is event in DB with key "{event_key}"')
def step_given_some_event_with_key(context, event_key):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    INSERT INTO reminders.events(event_key, group_key, owner_client_id, run_at, originally_run_at)
                    VALUES (%(event_key)s, %(group_key)s, %(owner_client_id)s, now(), now())
                ''',
                dict(
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )


@given('event "{event_key}" has status "{status}"')
def step_given_event_has_status(context, event_key, status):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    UPDATE reminders.events
                    SET status = %(status)s
                    WHERE event_key = %(event_key)s
                    AND group_key = %(group_key)s
                    AND owner_client_id = %(owner_client_id)s
                ''',
                dict(
                    status=status,
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )


@given('event "{event_key}" run_at "{days:d}" days ago')
def step_given_event_run_at_days_ago(context, event_key, days):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    UPDATE reminders.events
                    SET run_at = now() - %(days)s
                    WHERE event_key = %(event_key)s
                    AND group_key = %(group_key)s
                    AND owner_client_id = %(owner_client_id)s
                ''',
                dict(
                    days=timedelta(days),
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )


def is_event_in_callmebackdb(context, event_key):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    SELECT event_id
                    FROM reminders.events
                    WHERE event_key = %(event_key)s
                    AND group_key = %(group_key)s
                    AND owner_client_id = %(owner_client_id)s
                ''',
                dict(
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )
            return cur.fetchone() is not None


@then('event "{event_key}" is absent in callmebackdb')
def step_then_event_is_absent_in_callmebackdb(context, event_key):
    assert not is_event_in_callmebackdb(context, event_key), (
        'Expected: the event is absent in callmebackdb, but he is present'
    )


@then('event "{event_key}" is present in callmebackdb')
def step_then_event_is_present_in_callmebackdb(context, event_key):
    assert is_event_in_callmebackdb(context, event_key), (
        'Expected: the event is present in callmebackdb, but he is absent'
    )


@given('there is record in change_log for event "{event_key}"')
def step_given_some_record_in_change_log_with_key(context, event_key):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    INSERT INTO reminders.change_log(event_key, group_key, owner_client_id)
                    VALUES (%(event_key)s, %(group_key)s, %(owner_client_id)s)
                ''',
                dict(
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )


@given('change for "{event_key}" was made "{days:d}" days ago')
def step_given_change_was_made_days_ago(context, event_key, days):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    UPDATE reminders.change_log
                    SET at = now() - %(days)s
                    WHERE event_key = %(event_key)s
                    AND group_key = %(group_key)s
                    AND owner_client_id = %(owner_client_id)s
                ''',
                dict(
                    days=timedelta(days),
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )


def is_change_in_change_log(context, event_key):
    with transaction(get_dsn(context)) as conn:
        with conn.cursor() as cur:
            cur.execute(
                '''
                    SELECT cid
                    FROM reminders.change_log
                    WHERE event_key = %(event_key)s
                    AND group_key = %(group_key)s
                    AND owner_client_id = %(owner_client_id)s
                ''',
                dict(
                    event_key=event_key,
                    group_key=DEFAULT_GROUP_KEY,
                    owner_client_id=DEFAULT_OWNER_CLIENT_ID,
                )
            )
            return cur.fetchone() is not None


@then('change for "{event_key}" is absent in change_log')
def step_then_change_is_absent_in_change_log(context, event_key):
    assert not is_change_in_change_log(context, event_key), (
        'Expected: record for event is absent in change_log, but he is present'
    )


@then('change for "{event_key}" is present in change_log')
def step_then_change_is_present_in_change_log(context, event_key):
    assert is_change_in_change_log(context, event_key), (
        'Expected: record for event is present in change_log, but he is absent'
    )
