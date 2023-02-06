import logging

import pytest
from requests import codes

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.common.mutations.python as mutations


logger = logging.getLogger(__name__)


def test_remove_positive(siberia_client, ready_user_set_id, segment, change_log_logbroker_client):
    siberia_client.segments_remove(ready_user_set_id, [segment.Id])
    assert 0 == len(siberia_client.segments_search(ready_user_set_id).Segments)

    messages = consumer_utils.read_all(change_log_logbroker_client.create_consumer())
    assert len(messages) == 1
    assert mutations.create_remove_segment_data_command(user_set_id=int(ready_user_set_id), segment_id=int(segment.Id)) == mutations.from_json(messages[0])


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id, segment: (test_helpers.get_unknown_id([user_set_id]), [segment.Id]), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id, _: (user_set_id, []), id="empty body"),
    # TODO(kolontaev,dkuksa): Может возвращать ошибку, если пытаются удалить несуществующий сегмент?
    # pytest.param(codes.bad_request, lambda user_set_id, segment: (user_set_id, [test_helpers.get_unknown_id([segment.Id])]), id="unknown segment id"),
])
def test_remove_negative(siberia_client, ready_user_set_id, segment, status_code, get_args, change_log_logbroker_client):
    test_helpers.assert_http_error(status_code, siberia_client.segments_remove, *get_args(ready_user_set_id, segment))
    test_helpers.assert_segments_is([segment], siberia_client, ready_user_set_id)
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


def test_remove_multiple(local_ydb, siberia_client, ready_user_set_id, change_log_logbroker_client):
    segments = generate_segments(local_ydb, siberia_client, ready_user_set_id, 10)
    segment_ids = [segment.Id for segment in segments]

    siberia_client.segments_remove(ready_user_set_id, segment_ids[:5])
    test_helpers.assert_segments_is(segments[5:], siberia_client, ready_user_set_id)

    siberia_client.segments_remove(ready_user_set_id, segment_ids[5:])

    assert 0 == len(siberia_client.segments_search(ready_user_set_id).Segments)

    def get_segment_id(x):
        return x.RemoveSegmentDataCommand.SegmentId

    messages = consumer_utils.read_all(change_log_logbroker_client.create_consumer())
    messages = sorted([mutations.from_json(item) for item in messages], key=get_segment_id)

    expected_messages = sorted([mutations.create_remove_segment_data_command(user_set_id=int(ready_user_set_id), segment_id=int(segment_id)) for segment_id in segment_ids], key=get_segment_id)

    assert expected_messages == messages


def test_search_paging(local_ydb, siberia_client, ready_user_set_id):
    segment_number = 10
    segments = generate_segments(local_ydb, siberia_client, ready_user_set_id, segment_number)

    limit = 4
    pages = []
    last_segment_id = None
    for _ in range(segment_number / limit + 1):
        page = list(siberia_client.segments_search(ready_user_set_id, limit, last_segment_id).Segments)
        assert len(page) <= limit
        last_segment_id = page[-1].Id
        pages.append(page)

    assert 0 == len(siberia_client.segments_search(ready_user_set_id, limit, last_segment_id).Segments)
    test_helpers.assert_segments_equal(segments, sum(pages, []))


def generate_segments(local_ydb, siberia_client, user_set_id, number):
    test_helpers.upload_segments_table(local_ydb, user_set_id, [test_helpers.generate_segment_db_row(i) for i in range(number)])
    return list(siberia_client.segments_search(user_set_id).Segments)
