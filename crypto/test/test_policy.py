import pytest
from requests import codes

from crypta.lib.proto.identifiers import id_pb2
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.siberia_client import SiberiaClient


def create_materialized_user_set(siberia_client):
    return siberia_client.user_sets_add("xxx", ttl=1).UserSetId


def create_materialized_ready_user_set(siberia_client):
    user_set_id = create_materialized_user_set(siberia_client)
    test_helpers.ready_user_set(siberia_client, user_set_id)
    return user_set_id


def create_not_materialized_user_set(siberia_client):
    ids = id_pb2.TIds(Ids=[id_pb2.TId(Type="yandexuid", Value="1")])
    return siberia_client.user_sets_describe_ids(ids).UserSetId


def run_test(siberia_client, create_user_set, method, kwargs):
    user_set_id = create_user_set(siberia_client)
    test_helpers.assert_http_error(codes.not_allowed, method, siberia_client, user_set_id, **kwargs)


@pytest.mark.parametrize("create_user_set", [
    pytest.param(create_materialized_ready_user_set, id="materialized_ready"),
    pytest.param(create_not_materialized_user_set, id="not_materialized"),
])
@pytest.mark.parametrize("method,kwargs", [
    pytest.param(SiberiaClient.users_add, dict(users=test_helpers.generate_add_users_request(1)), id="users_add"),
])
def test_must_be_not_ready(siberia_client, create_user_set, method, kwargs):
    run_test(siberia_client, create_user_set, method, kwargs)


@pytest.mark.parametrize("create_user_set", [
    pytest.param(create_materialized_user_set, id="materialized_not_ready"),
])
@pytest.mark.parametrize("method,kwargs", [
    pytest.param(SiberiaClient.user_sets_get_stats, {}, id="user_sets_get_stats"),
])
def test_must_have_meta_data(siberia_client, create_user_set, method, kwargs):
    run_test(siberia_client, create_user_set, method, kwargs)


@pytest.mark.parametrize("create_user_set", [
    pytest.param(create_materialized_user_set, id="materialized_not_ready"),
    pytest.param(create_not_materialized_user_set, id="not_materialized"),
])
@pytest.mark.parametrize("method,kwargs", [
    pytest.param(SiberiaClient.user_sets_describe, {}, id="user_sets_describe"),
    pytest.param(SiberiaClient.users_search, {}, id="users_search"),
    pytest.param(SiberiaClient.segments_search, {}, id="segments_search"),
    pytest.param(SiberiaClient.segments_make, dict(title="x", rule="z"), id="segments_make"),
    pytest.param(SiberiaClient.segments_describe, dict(segment_id="1"), id="segments_describe"),
    pytest.param(SiberiaClient.segments_get_stats, dict(segment_id="1"), id="segments_get_stats"),
    pytest.param(SiberiaClient.segments_list_users, dict(segment_id="1"), id="segments_list_users"),
    pytest.param(SiberiaClient.segments_remove, dict(segment_ids=["1"]), id="segments_remove"),
])
def test_must_be_materialized_ready(siberia_client, create_user_set, method, kwargs):
    run_test(siberia_client, create_user_set, method, kwargs)
