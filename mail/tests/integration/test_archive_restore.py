import pytest
import random
import time
import json
from itertools import cycle
from library.python.testing.pyremock.lib.pyremock import MatchRequest, MockResponse, HttpMethod
from hamcrest import (
    assert_that,
    equal_to,
    has_entries
)

from .conftest import change_archive_state, random_mix, get_folder_by_type
from pymdb.vegetarian import SAMPLE_STIDS, SAMPLE_WINDAT_STIDS
from mail.barbet.devpack.components.barbet import BarbetDevpack as Barbet

SAMPLE_ABSENT_STIDS = [
    "100.absent_stid.E100:241046198820547232969001058",
    "101.absent_stid.E101:134090406637961488505874094",
]

SAMPLE_BAD_STIDS = [
    "100.bad_stid.E100:241046198820547232969001058",
    "101.bad_stid.E101:134090406637961488505874094",
    "102.bad_stid.E102:833108344210793547739411603",
]


def get_s3_bucket(context):
    return context.barbet.components[Barbet].s3_bucket()


def get_task_max_tries(context):
    return context.barbet.components[Barbet].archive_restore_max_tries()


def s3_list_objects_v2_success(context, uid, keys, times=1):
    prefix = f'{uid}/'.encode('utf-8')
    contents = ''
    for key in keys:
        contents += f'''
        <Contents>
            <Key>{uid}/{key}</Key>
            <LastModified>2021-08-12T17:50:30.000Z</LastModified>
            <ETag/>
            <Size>434</Size>
            <StorageClass>STANDARD</StorageClass>
        </Contents>'''

    body = f'''<?xml version="1.0" encoding="UTF-8"?>
    <ListBucketResult>
        <Name>mail-archive-test</Name>
        <Prefix>1111</Prefix>
        <KeyCount>2</KeyCount>
        <MaxKeys>1000</MaxKeys>
        <IsTruncated>false</IsTruncated>
        {contents}
    </ListBucketResult>
    '''
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/' + get_s3_bucket(context)),
        params=has_entries({'list-type': [b'2'], 'prefix': [prefix]}),
    ), response=MockResponse(status=200, body=body), times=times)


def s3_list_objects_v2_fails(context, uid, http_code=500, times=1):
    prefix = f'{uid}/'.encode('utf-8')
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to('/' + get_s3_bucket(context)),
        params=has_entries({'list-type': [b'2'], 'prefix': [prefix]}),
    ), response=MockResponse(status=http_code, body='{}'), times=times)


def s3_get_object_success(context, uid, key, value, times=1):
    body = json.dumps(value).encode('utf-8')

    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to(f'/{get_s3_bucket(context)}/{uid}/{key}'),
    ), response=MockResponse(status=200, body=body), times=times)


def s3_get_object_fails(context, uid, key, http_code=500, times=1):
    pyremock = context.coordinator.pyremock
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.GET),
        path=equal_to(f'/{get_s3_bucket(context)}/{uid}/{key}'),
    ), response=MockResponse(status=http_code, body=''), times=times)


def get_messages_count(context, uid):
    res = context.maildb.query('''
                SELECT COUNT(*)
                  FROM mail.messages
                 WHERE uid = %(uid)s
            ''', uid=uid)
    return res[0][0]


def get_archive_status(context, uid):
    res = context.maildb.query('''
                SELECT uid, state, message_count, restored_message_count
                  FROM mail.archives
                 WHERE uid = %(uid)s
            ''', uid=uid)
    return res[0]


def generate_s3_data(chunks_count, msgs_per_chunk, stids):
    stids = cycle(stids)
    chunks = [
        [
            {'received_date': 1631023740 + chunk_id + msg_id, 'st_id': next(stids), 'folder_type': 'inbox', 'is_shared': msg_id % 2 == 0}
            for msg_id in range(msgs_per_chunk)
        ]
        for chunk_id in range(chunks_count)
    ]
    s3_data = {}
    for idx, chunk in enumerate(chunks):
        s3_data['{}_{}'.format(idx, len(chunk))] = chunk

    return s3_data


def get_api_and_pyremock(context, uid):
    barbet = context.barbet.components[Barbet]
    barbet_api = barbet.api(suid=uid, mdb='pg', uid=uid)
    pyremock = context.coordinator.pyremock
    return barbet_api, pyremock


@pytest.mark.parametrize(
    "http_code, times",
    [(200, 1), (400, 1), (500, 2)],
)
def test_proxy_hound_response(context, newly_created_uid, http_code, times):
    uid = newly_created_uid
    barbet_api, pyremock = get_api_and_pyremock(context, uid)

    reason = "some reason, {}, {}".format(http_code, times)

    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/v2/archive_change'),
        params=has_entries({'uid': [str(uid).encode('utf-8')]}),
    ), response=MockResponse(status=http_code, body=json.dumps({"error": {"reason": reason}}))
    , times=times)

    change_archive_state(context, uid, 'archivation_complete')

    status = barbet_api.archive_restore(uid=uid)
    assert_that(status.status_code, equal_to(http_code), status.text)
    if http_code != 200:
        status_j = status.json()
        assert_that(status_j, has_entries(reason=reason), status.text)

    context.coordinator.pyremock.assert_expectations()


def wait_for_complete(context, uid, required_state, failed_state=None, times=30):
    is_complete = lambda resp: resp and resp[1] == required_state
    is_failed = lambda resp: resp and failed_state and resp[1] == failed_state
    status = None
    for _ in range(times):
        status = get_archive_status(context, uid)
        assert not is_failed(status)
        if is_complete(status):
            return
        time.sleep(1)
    assert is_complete(status)


def hound_success(pyremock, uid, times=1):
    pyremock.expect(request=MatchRequest(
        method=equal_to(HttpMethod.POST),
        path=equal_to('/v2/archive_change'),
        params=has_entries({'uid': [str(uid).encode('utf-8')]}),
    ), response=MockResponse(status=200, body='{}'), times=times)


def start_restoration(context, uid, barbet_api, concurrent_tasks=1):
    change_archive_state(context, uid, 'archivation_complete')
    status = None
    for _ in range(concurrent_tasks):
        status = barbet_api.archive_restore(uid=uid)
    change_archive_state(context, uid, 'restoration_in_progress')
    return status


@pytest.mark.parametrize(
    "chunks_count, msgs_per_chunk, mulca_count",
    [
        (10, 1, 0),
        (1, 10, 1),
        (2, 5, 2),
        (3, 7, 9),
    ],
)
@pytest.mark.parametrize(
    "task_restart_count",
    [0, 1, 4, 11],
)
def test_messages_restored(context, newly_created_uid, chunks_count, msgs_per_chunk, task_restart_count, mulca_count):
    uid = newly_created_uid
    assert task_restart_count < get_task_max_tries(context), 'task restart count should be less than available retries'
    task_restart_count = min(task_restart_count, chunks_count)
    concurrent_tasks = max(1, task_restart_count)

    expected_msg_count = chunks_count * msgs_per_chunk - mulca_count
    stids = random_mix(SAMPLE_WINDAT_STIDS, SAMPLE_STIDS, expected_msg_count, mulca_count)

    barbet_api, pyremock = get_api_and_pyremock(context, uid)
    hound_success(pyremock, uid, times=concurrent_tasks)

    s3_data = generate_s3_data(chunks_count, msgs_per_chunk, stids)

    s3_list_objects_v2_fails(context, uid, http_code=500, times=2)
    s3_list_objects_v2_success(context, uid, s3_data.keys(), times=1 + task_restart_count)

    for i, (key, messages) in enumerate(s3_data.items()):
        s3_get_object_fails(context, uid, key, http_code=500, times=1)

        if (i + task_restart_count) >= chunks_count and task_restart_count > 0:
            s3_get_object_fails(context, uid, key, http_code=404, times=1)
            task_restart_count -= 1

        s3_get_object_success(context, uid, key, messages)

    status = start_restoration(context, uid, barbet_api, concurrent_tasks)
    assert_that(status.status_code, equal_to(200), status.text)

    wait_for_complete(context, uid, 'restoration_complete', 'restoration_error')

    messages_count = get_messages_count(context, uid)
    assert_that(messages_count, equal_to(expected_msg_count))

    restored_folder = get_folder_by_type(context, uid, 'restored')
    assert_that(restored_folder.message_seen, equal_to(expected_msg_count))
    assert_that(restored_folder.message_count, equal_to(expected_msg_count))

    status = get_archive_status(context, uid)
    assert_that(status[3], equal_to(expected_msg_count + mulca_count))

    pyremock.assert_expectations()


def test_restoration_failed_because_of_s3(context, newly_created_uid):
    uid = newly_created_uid
    barbet_api, pyremock = get_api_and_pyremock(context, uid)

    hound_success(pyremock, uid)
    s3_list_objects_v2_fails(context, uid, http_code=404, times=get_task_max_tries(context))

    status = start_restoration(context, uid, barbet_api)
    assert_that(status.status_code, equal_to(200), status.text)

    wait_for_complete(context, uid, 'restoration_error')

    messages_count = get_messages_count(context, uid)
    expected_msg_count = 0
    assert_that(messages_count, equal_to(expected_msg_count))

    status = get_archive_status(context, uid)
    assert_that(status[3], equal_to(expected_msg_count))

    pyremock.assert_expectations()


@pytest.mark.parametrize(
    "chunk_value",
    [
        [{'folder_type': 'some fields are missing'}],
        [{'received_date': 'wrong_type', 'st_id': 0xDEADBEEF, 'folder_type': 0xDEADBEEF, 'is_shared': 'wrong_type'}],
        []
    ],
)
def test_restoration_failed_because_of_broken_archive_chunk(context, newly_created_uid, chunk_value):
    uid = newly_created_uid
    barbet_api, pyremock = get_api_and_pyremock(context, uid)

    hound_success(pyremock, uid)

    key = '1_1'
    s3_list_objects_v2_success(context, uid, [key], times=get_task_max_tries(context))
    s3_get_object_success(context, uid, key, chunk_value, times=get_task_max_tries(context))

    status = start_restoration(context, uid, barbet_api)
    assert_that(status.status_code, equal_to(200), status.text)

    wait_for_complete(context, uid, 'restoration_error')

    messages_count = get_messages_count(context, uid)
    expected_msg_count = 0
    assert_that(messages_count, equal_to(expected_msg_count))

    status = get_archive_status(context, uid)
    assert_that(status[3], equal_to(expected_msg_count))

    pyremock.assert_expectations()


def test_messages_restored_with_bad_and_absent_messages(context, newly_created_uid):
    uid = newly_created_uid
    good_messages_count = 5
    stids = SAMPLE_WINDAT_STIDS[:good_messages_count] + SAMPLE_ABSENT_STIDS + SAMPLE_BAD_STIDS
    random.shuffle(stids)

    barbet_api, pyremock = get_api_and_pyremock(context, uid)
    hound_success(pyremock, uid)

    s3_data = generate_s3_data(chunks_count=1, msgs_per_chunk=len(stids), stids=stids)
    s3_list_objects_v2_success(context, uid, s3_data.keys())

    for i, (key, messages) in enumerate(s3_data.items()):
        s3_get_object_success(context, uid, key, messages)

    status = start_restoration(context, uid, barbet_api)
    assert_that(status.status_code, equal_to(200), status.text)

    wait_for_complete(context, uid, 'restoration_complete', 'restoration_error')

    messages_count = get_messages_count(context, uid)
    assert_that(messages_count, equal_to(good_messages_count))

    restored_folder = get_folder_by_type(context, uid, 'restored')
    assert_that(restored_folder.message_seen, equal_to(good_messages_count))
    assert_that(restored_folder.message_count, equal_to(good_messages_count))

    status = get_archive_status(context, uid)
    assert_that(status[3], equal_to(len(stids)))

    pyremock.assert_expectations()
