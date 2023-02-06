import logging
import os
import random

from library.python.protobuf.json import proto2json
import pytest
import requests
import yaml
import yatest.common

from crypta.lib.native.http.proto import simple_response_pb2
from crypta.lib.python import time_utils
from crypta.siberia.bin.common import ydb_schemas
from crypta.siberia.bin.common.data.proto import (
    user_pb2,
    user_set_status_pb2,
    user_set_type_pb2,
)
from crypta.siberia.bin.common.ydb.paths.proto import ydb_paths_pb2
from crypta.siberia.bin.core.proto import add_users_request_pb2


logger = logging.getLogger(__name__)
YDB_PATHS = ydb_paths_pb2.TYdbPaths()


def assert_http_error(status_code, f, *args, **kwargs):
    with pytest.raises(requests.HTTPError) as excinfo:
        f(*args, **kwargs)

    response = excinfo.value.response
    assert status_code == response.status_code, response.text


def assert_segments_equal(ref, value):
    def key(x):
        return x.Id

    assert sorted(ref, key=key) == sorted(value, key=key)


def assert_segments_is(ref, siberia_client, user_set_id):
    return assert_segments_equal(ref, list(siberia_client.segments_search(user_set_id).Segments))


def ready_user_set(siberia_client, user_set_id):
    siberia_client.user_sets_update(user_set_id, status=user_set_status_pb2.TUserSetStatus().Ready)


def generate_user(i):
    return user_pb2.TUser.TInfo(
        Status="status-{}".format(i),
        Attributes={
            "attribute-1": user_pb2.TUser.TInfo.TAttributeValues(Values=["value-1.1.{}".format(i), "value-1.2.{}".format(i)]),
            "attribute-2": user_pb2.TUser.TInfo.TAttributeValues(Values=["value-2.1.{}".format(i)]),
        }
    )


def generate_add_users_request(number):
    return add_users_request_pb2.TAddUsersRequest(Users=[generate_user(i) for i in range(number)])


def make_simple_response(message):
    return proto2json.proto2json(simple_response_pb2.TSimpleResponse(Message=message))


def generate_user_set_db_row(id, title=None, type=None, status=None, expiration_time=None):
    return {
        "id": id,
        "title": title or "xxx",
        "type": type or user_set_type_pb2.TUserSetType().Materialized,
        "status": status or user_set_status_pb2.TUserSetStatus().Ready,
        "expiration_time": expiration_time or time_utils.get_current_time(),
    }


def generate_segment_db_row(segment_id):
    return {
        "id": int(segment_id),
        "title": "title-{}".format(segment_id),
        "rule": "rule-{}".format(segment_id),
        "status": "status-{}".format(segment_id),
        "size": 0,
        "creation_ts": time_utils.get_current_time(),
    }


def get_user_segments_db_row(user_id, segment_id):
    return {
        "user_id": int(user_id),
        "segment_id": int(segment_id),
        "ts": time_utils.get_current_time(),
    }


def get_segment_users_db_row(segment_id, user_id):
    return {
        "segment_id": int(segment_id),
        "user_id": int(user_id),
    }


def get_random_id():
    return random.getrandbits(64)


def get_unknown_id(known_ids):
    while True:
        unknown_id = str(get_random_id())
        if unknown_id not in known_ids:
            return unknown_id


def get_user_set_path(user_set_id, relative_path=None):
    user_set_dir = str(user_set_id)
    return user_set_dir if relative_path is None else os.path.join(user_set_dir, relative_path)


def create_crypta_id_user_data_dir(local_ydb):
    local_ydb.client.make_directory(YDB_PATHS.CryptaIdUserDataDir)


def create_experimental_crypta_id_user_data_root_dir(local_ydb):
    local_ydb.client.make_directory(YDB_PATHS.ExperimentalCryptaIdUserDataRootDir)


def create_crypta_id_user_data_table(local_ydb, name, version=None):
    path = os.path.join(YDB_PATHS.CryptaIdUserDataDir, name) if version is None else os.path.join(YDB_PATHS.ExperimentalCryptaIdUserDataRootDir, version, name)
    local_ydb.client.create_table(path, ydb_schemas.CryptaIdUserData.columns, ydb_schemas.CryptaIdUserData.primary_key)


def create_id_to_crypta_id_dir(local_ydb):
    local_ydb.client.make_directory(YDB_PATHS.IdToCryptaIdDir)


def create_id_to_crypta_id_table(local_ydb, name):
    path = os.path.join(YDB_PATHS.IdToCryptaIdDir, name)
    local_ydb.client.create_table(path, ydb_schemas.Id2CryptaId.columns, ydb_schemas.Id2CryptaId.primary_key)


def create_segment_stats_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentStatsTable)
    local_ydb.client.create_table(path, ydb_schemas.SegmentStats.columns, ydb_schemas.SegmentStats.primary_key)


def create_segment_users_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentUsersTable)
    local_ydb.client.create_table(path, ydb_schemas.SegmentUsers.columns, ydb_schemas.SegmentUsers.primary_key)


def create_segments_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentsTable)
    local_ydb.client.create_table(path, ydb_schemas.Segments.columns, ydb_schemas.Segments.primary_key)


def create_user_attributes_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.UserAttributesTable)
    local_ydb.client.create_table(path, ydb_schemas.UserAttributes.columns, ydb_schemas.UserAttributes.primary_key)


def create_user_segments_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.UserSegmentsTable)
    local_ydb.client.create_table(path, ydb_schemas.UserSegments.columns, ydb_schemas.UserSegments.primary_key)


def create_user_set_dir(local_ydb, user_set_id):
    local_ydb.client.make_directory(get_user_set_path(user_set_id))


def create_user_set_stats_table(local_ydb):
    local_ydb.client.create_table(YDB_PATHS.UserSetStatsTable, ydb_schemas.UserSetStats.columns, ydb_schemas.UserSetStats.primary_key)


def create_user_sets_table(local_ydb):
    local_ydb.client.create_table(YDB_PATHS.UserSetsTable, ydb_schemas.UserSets.columns, ydb_schemas.UserSets.primary_key)


def create_users_table(local_ydb, user_set_id):
    path = get_user_set_path(user_set_id, YDB_PATHS.UsersTable)
    local_ydb.client.create_table(path, ydb_schemas.Users.columns, ydb_schemas.Users.primary_key)


def upload_crypta_id_user_data_table(local_ydb, name, data, version=None):
    path = os.path.join(YDB_PATHS.CryptaIdUserDataDir, name) if version is None else os.path.join(YDB_PATHS.ExperimentalCryptaIdUserDataRootDir, version, name)
    local_ydb.upload_data(path, data)


def upload_id_to_crypta_id_table(local_ydb, name, data):
    path = os.path.join(YDB_PATHS.IdToCryptaIdDir, name)
    local_ydb.upload_data(path, data)


def upload_segment_stats_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentStatsTable)
    local_ydb.upload_data(path, data)


def upload_segment_users_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentUsersTable)
    local_ydb.upload_data(path, data)


def upload_segments_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.SegmentsTable)
    local_ydb.upload_data(path, data)


def upload_user_attributes_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.UserAttributesTable)
    local_ydb.upload_data(path, data)


def upload_user_segments_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.UserSegmentsTable)
    local_ydb.upload_data(path, data)


def upload_user_sets_table(local_ydb, data):
    local_ydb.upload_data(YDB_PATHS.UserSetsTable, data)


def upload_users_table(local_ydb, user_set_id, data):
    path = get_user_set_path(user_set_id, YDB_PATHS.UsersTable)
    local_ydb.upload_data(path, data)


def dump_crypta_id_user_data_dir(local_ydb):
    return local_ydb.dump_dir(YDB_PATHS.CryptaIdUserDataDir)


def dump_experimental_crypta_id_user_data_dir(local_ydb, version):
    return local_ydb.dump_dir(os.path.join(YDB_PATHS.ExperimentalCryptaIdUserDataRootDir, version))


def dump_id_to_crypta_id_dir(local_ydb):
    return local_ydb.dump_dir(YDB_PATHS.IdToCryptaIdDir)


def dump_user_set_stats_table(local_ydb):
    return local_ydb.dump_table(YDB_PATHS.UserSetStatsTable)


def create_user_set(local_ydb, user_set_id):
    create_user_set_dir(local_ydb, user_set_id)

    create_user_attributes_table(local_ydb, user_set_id)
    create_user_segments_table(local_ydb, user_set_id)
    create_users_table(local_ydb, user_set_id)
    create_segments_table(local_ydb, user_set_id)
    create_segment_stats_table(local_ydb, user_set_id)
    create_segment_users_table(local_ydb, user_set_id)


def upload_user_set(local_ydb, user_set_id, segment_stats=None, segment_users=None, segments=None, user_attributes=None, user_segments=None, users=None):
    upload_segment_stats_table(local_ydb, user_set_id, segment_stats or [])
    upload_segment_users_table(local_ydb, user_set_id, segment_users or [])
    upload_segments_table(local_ydb, user_set_id, segments or [])
    upload_user_attributes_table(local_ydb, user_set_id, user_attributes or [])
    upload_user_segments_table(local_ydb, user_set_id, user_segments or [])
    upload_users_table(local_ydb, user_set_id, users or [])


def upload_user_set_from_yaml_dir(local_ydb, user_set_id, local_dir):
    def read_data(filename):
        path = yatest.common.test_source_path(os.path.join(local_dir, filename))

        if not os.path.exists(path):
            return None

        with open(path) as f:
            return yaml.safe_load(f)

    upload_user_set(
        local_ydb,
        user_set_id,
        segment_stats=read_data("segment_stats.yaml"),
        segment_users=read_data("segment_users.yaml"),
        segments=read_data("segments.yaml"),
        user_attributes=read_data("user_attributes.yaml"),
        user_segments=read_data("user_segments.yaml"),
        users=read_data("users.yaml"),
    )
