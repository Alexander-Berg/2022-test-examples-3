import logging

import pytest
from requests import codes

from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.data.proto import user_pb2
from crypta.siberia.bin.core.proto import add_users_request_pb2


logger = logging.getLogger(__name__)


VALID_CREATE_DATA = test_helpers.generate_add_users_request(1)


def test_add_positive(siberia_client, user_set_id):
    siberia_client.users_add(user_set_id, VALID_CREATE_DATA)
    test_helpers.ready_user_set(siberia_client, user_set_id)

    users = siberia_client.users_search(user_set_id).Users
    assert len(users) == 1

    ref = user_pb2.TUser(
        Id=users[0].Id,
        Info=user_pb2.TUser.TInfo(
            Status="status-0",
            Attributes={
                "attribute-1": user_pb2.TUser.TInfo.TAttributeValues(Values=["value-1.1.0", "value-1.2.0"]),
                "attribute-2": user_pb2.TUser.TInfo.TAttributeValues(Values=["value-2.1.0"]),
            }
        )
    )

    assert ref == users[0]


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id], ), VALID_CREATE_DATA), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, add_users_request_pb2.TAddUsersRequest(), ), id="without clients"),
])
def test_add_negative(siberia_client, user_set_id, status_code, get_args):
    test_helpers.assert_http_error(status_code, siberia_client.users_add, *get_args(user_set_id))

    test_helpers.ready_user_set(siberia_client, user_set_id)
    assert 0 == len(siberia_client.users_search(user_set_id).Users)


def test_search_paging(siberia_client, user_set_id):
    user_number = 10
    siberia_client.users_add(user_set_id, test_helpers.generate_add_users_request(user_number))
    test_helpers.ready_user_set(siberia_client, user_set_id)
    users = list(siberia_client.users_search(user_set_id).Users)

    limit = 4
    pages = []
    last_user_id = None
    for _ in range(user_number / limit + 1):
        page = list(siberia_client.users_search(user_set_id, limit, last_user_id).Users)
        assert len(page) <= limit
        last_user_id = page[-1].Id
        pages.append(page)

    assert 0 == len(siberia_client.users_search(user_set_id, limit, last_user_id).Users)
    assert users == sum(pages, [])
