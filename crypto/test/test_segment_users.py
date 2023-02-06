import logging

import pytest
from requests import codes

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.segmentation import command_helpers


logger = logging.getLogger(__name__)


@pytest.fixture(scope="function")
def segment_with_10_users(local_ydb, siberia_client, user_set_id):
    siberia_client.users_add(user_set_id, test_helpers.generate_add_users_request(10))
    test_helpers.ready_user_set(siberia_client, user_set_id)
    users = list(siberia_client.users_search(user_set_id).Users)

    user_ids = [user.Id for user in users]
    segment_id = "1"

    test_helpers.upload_segments_table(local_ydb, user_set_id, [test_helpers.generate_segment_db_row(segment_id)])
    test_helpers.upload_user_segments_table(local_ydb, user_set_id, [test_helpers.get_user_segments_db_row(user_id, segment_id) for user_id in user_ids])
    test_helpers.upload_segment_users_table(local_ydb, user_set_id, [test_helpers.get_segment_users_db_row(segment_id, user_id) for user_id in user_ids])

    return (
        list(siberia_client.segments_search(user_set_id).Segments)[0],
        list(siberia_client.segments_list_users(user_set_id, segment_id).Users),
    )


def test_list_paging(siberia_client, user_set_id, segment_with_10_users):
    segment, users_with_data = segment_with_10_users

    limit = 4
    pages = []
    last_user_id = None
    for _ in range(len(users_with_data) / limit + 1):
        page = list(siberia_client.segments_list_users(user_set_id, segment.Id, limit, last_user_id).Users)
        assert len(page) <= limit
        last_user_id = page[-1].Id
        pages.append(page)

    assert 0 == len(siberia_client.segments_list_users(user_set_id, segment.Id, limit, last_user_id).Users)
    assert users_with_data == sum(pages, [])


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), "SegmentTitle", 'field == "xyz"'), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, "", 'field == "xyz"'), id="empty title"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, "SegmentTitle", ""), id="empty rule"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, "SegmentTitle", "field !!!"), id="invalid rule"),
])
def test_make_negative(siberia_client, user_set_id, status_code, get_args):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    test_helpers.assert_http_error(status_code, siberia_client.segments_make, *get_args(user_set_id))
    assert 0 == len(siberia_client.segments_search(user_set_id).Segments)


def test_make_positive(siberia_client, user_set_id, segmentate_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)

    rule = 'age == "21"'
    title = "SegmentTitle"
    segment = siberia_client.segments_make(user_set_id, title, rule)

    assert title == segment.Title
    assert rule == segment.Rule
    assert "in_progress" == segment.Status

    segment_id = segment.Id

    messages = consumer_utils.read_all(segmentate_log_logbroker_client.create_consumer())
    assert len(messages) == 1
    assert command_helpers.create_segmentate_command(user_set_id=int(user_set_id), segment_id=int(segment_id)) == command_helpers.from_json(messages[0])
