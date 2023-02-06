import datetime
import logging

import pytest
from requests import codes

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.data.proto import (
    user_set_pb2,
    user_set_status_pb2,
    user_set_type_pb2,
)
import crypta.siberia.bin.common.mutations.python as mutations


logger = logging.getLogger(__name__)

FROZEN_TIME = 1500000000

TITLE = "X"
STATUS = user_set_status_pb2.TUserSetStatus().Ready
TTL = int(datetime.timedelta(hours=7).total_seconds())
VALID_UPDATE_KWARGS = dict(title=TITLE, status=STATUS, ttl=TTL)


@pytest.fixture(scope="function")
def ref(user_set_id, user_set_ttl):
    return user_set_pb2.TUserSet(
        Id=str(user_set_id),
        Status=user_set_status_pb2.TUserSetStatus().NotReady,
        ExpirationTime=FROZEN_TIME + user_set_ttl,
        Title="user-set-xyz",
        Type=user_set_type_pb2.TUserSetType().Materialized,
    )


def test_add_positive(siberia_client, user_set_id, ref):
    assert ref == siberia_client.user_sets_get(user_set_id)


def test_min_ttl(siberia_client, siberia_config):
    user_set_id = siberia_client.user_sets_add("xxx", ttl=0).UserSetId
    user_set = siberia_client.user_sets_get(user_set_id)
    assert user_set.ExpirationTime == FROZEN_TIME + siberia_config.Processors.UserSet.MinMaterializedTtlSeconds


@pytest.mark.parametrize("status_code,title", [
    pytest.param(codes.bad_request, "", id="empty title"),
    pytest.param(codes.bad_request, None, id="without title"),
])
def test_add_negative(siberia_client, status_code, title):
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_add, title)


def test_get_positive(siberia_client, user_set_id, ref):
    assert ref == siberia_client.user_sets_get(user_set_id)


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), ), id="unknown user set id"),
])
def test_get_negative(siberia_client, user_set_id, status_code, get_args):
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_get, *get_args(user_set_id))


def test_update_positive(siberia_client, user_set_id, ref):
    ref.Title = TITLE
    ref.Status = STATUS
    ref.ExpirationTime = FROZEN_TIME + TTL

    siberia_client.user_sets_update(user_set_id, **VALID_UPDATE_KWARGS)
    assert ref == siberia_client.user_sets_get(user_set_id)


@pytest.mark.parametrize("status_code,get_args,kwargs", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), ), VALID_UPDATE_KWARGS, id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: ("-1", ), VALID_UPDATE_KWARGS, id="invalid user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, ), {"title": ""}, id="empty title"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, ), {"status": "xyz"}, id="invalid status"),
    pytest.param(codes.bad_request, lambda user_set_id: (user_set_id, ), {"ttl": -1}, id="invalid ttl"),
])
def test_update_negative(siberia_client, user_set_id, status_code, get_args, kwargs, ref):
    assert ref == siberia_client.user_sets_get(user_set_id)
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_update, *get_args(user_set_id), **kwargs)
    assert ref == siberia_client.user_sets_get(user_set_id)


def test_not_allowed(siberia_client, tvm_api, tvm_ids):
    tvm_ticket = tvm_api.get_service_ticket(tvm_ids.version_only, tvm_ids.api)
    test_helpers.assert_http_error(codes.forbidden, siberia_client.user_sets_add, "title-xxx", tvm_ticket=tvm_ticket)


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), ), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: ("-1", ), id="invalid user set id"),
])
def test_remove_negative(siberia_client, status_code, get_args, user_set_id, change_log_logbroker_client):
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_remove, *get_args(user_set_id))
    siberia_client.user_sets_get(user_set_id)
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


def test_remove_positive(siberia_client, user_set_id, change_log_logbroker_client):
    siberia_client.user_sets_get(user_set_id)
    siberia_client.user_sets_remove(user_set_id)
    test_helpers.assert_http_error(codes.not_found, siberia_client.user_sets_get, user_set_id)

    messages = consumer_utils.read_all(change_log_logbroker_client.create_consumer())
    assert len(messages) == 1
    assert mutations.create_remove_user_set_data_command(user_set_id=int(user_set_id)) == mutations.from_json(messages[0])


@pytest.mark.parametrize("user_set_type,user_set_status", [
    pytest.param(user_set_type_pb2.TUserSetType().NotMaterialized, user_set_status_pb2.TUserSetStatus().Ready, id="not materialized"),
    pytest.param(user_set_type_pb2.TUserSetType().Materialized, user_set_status_pb2.TUserSetStatus().MetaDataOnly, id="meta data only"),
])
def test_remove_without_mutation_command(local_ydb, siberia_client, user_set_type, user_set_status, change_log_logbroker_client):
    user_set_id = "1"
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(id=int(user_set_id), type=user_set_type, status=user_set_status)])

    siberia_client.user_sets_remove(user_set_id)
    test_helpers.assert_http_error(codes.not_found, siberia_client.user_sets_get, user_set_id)
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), ), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id: ("-1", ), id="invalid user set id"),
])
def test_remove_data_negative(siberia_client, status_code, get_args, user_set_id, change_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_remove_data, *get_args(user_set_id))
    assert user_set_status_pb2.TUserSetStatus().Ready == siberia_client.user_sets_get(user_set_id).Status
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


def test_remove_data_positive(siberia_client, user_set_id, change_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    siberia_client.user_sets_remove_data(user_set_id)
    assert user_set_status_pb2.TUserSetStatus().MetaDataOnly == siberia_client.user_sets_get(user_set_id).Status

    messages = consumer_utils.read_all(change_log_logbroker_client.create_consumer())
    assert len(messages) == 1
    assert mutations.create_remove_user_set_data_command(user_set_id=int(user_set_id)) == mutations.from_json(messages[0])
