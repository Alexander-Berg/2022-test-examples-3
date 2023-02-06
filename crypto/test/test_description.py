import json
import logging

from google.protobuf import json_format
from library.python.protobuf.json import proto2json
import pytest
from requests import codes

from crypta.lib.proto.identifiers import id_pb2
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.data.proto import (
    user_set_pb2,
    user_set_status_pb2,
    user_set_type_pb2,
)
from crypta.siberia.bin.common.describing.mode.python import describing_mode
from crypta.siberia.bin.common.describing.experiment.proto import describing_experiment_pb2
from crypta.siberia.bin.common.describing.proto import (
    describe_cmd_pb2,
    describe_ids_cmd_pb2,
    describe_user_set_cmd_pb2,
)


logger = logging.getLogger(__name__)

FROZEN_TIME = 1500000000


def stats_to_dict(stats):
    return json.loads(proto2json.proto2json(stats))


def test_users_get_stats(siberia_client):
    test_helpers.assert_http_error(codes.not_implemented, siberia_client.users_get_stats, "1234567")


def test_segments_describe_positive(siberia_client, user_set_id, segment):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    test_helpers.assert_http_error(codes.not_implemented, siberia_client.segments_describe, user_set_id, segment.Id)


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id, segment_id: (test_helpers.get_unknown_id([user_set_id]), segment_id), id="unknown user set id"),
])
def test_segments_describe_negative(siberia_client, user_set_id, segment, status_code, get_args):
    test_helpers.assert_http_error(status_code, siberia_client.segments_describe, *get_args(user_set_id, segment.Id))


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id, segment_id: (test_helpers.get_unknown_id([user_set_id]), segment_id), id="unknown user set id"),
    pytest.param(codes.bad_request, lambda user_set_id, segment_id: (user_set_id, test_helpers.get_unknown_id([segment_id])), id="unknown segment id"),
])
def test_segments_get_stats_negative(siberia_client, user_set_id, segment, status_code, get_args):
    test_helpers.assert_http_error(status_code, siberia_client.segments_get_stats, *get_args(user_set_id, segment.Id))


def test_user_sets_describe_positive(siberia_client, user_set_id, describe_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    siberia_client.user_sets_describe(user_set_id)

    messages = consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    assert len(messages) == 1

    ref_cmd = describe_cmd_pb2.TDescribeCmd(
        DescribeUserSetCmd=describe_user_set_cmd_pb2.TDescribeUserSetCmd(
            UserSetId=int(user_set_id),
        ),
        Timestamp=FROZEN_TIME,
    )

    assert ref_cmd == json_format.Parse(messages[0], describe_cmd_pb2.TDescribeCmd())
    return stats_to_dict(siberia_client.user_sets_get_stats(user_set_id))


@pytest.mark.parametrize("status_code,get_args", [
    pytest.param(codes.not_found, lambda user_set_id: (test_helpers.get_unknown_id([user_set_id]), ), id="unknown user set id"),
])
def test_user_sets_describe_negative(siberia_client, user_set_id, status_code, get_args, describe_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    test_helpers.assert_http_error(status_code, siberia_client.user_sets_describe, *get_args(user_set_id))
    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())


def test_user_sets_describe_conflict(siberia_client, user_set_id, describe_log_logbroker_client):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    siberia_client.user_sets_describe(user_set_id)
    test_helpers.assert_http_error(codes.conflict, siberia_client.user_sets_describe, user_set_id)

    messages = consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    assert len(messages) == 1


@pytest.mark.parametrize("ttl", [
    pytest.param(None, id="without-ttl"),
    pytest.param(10, id="with-ttl"),
])
@pytest.mark.parametrize("mode", [
    pytest.param(describing_mode.SLOW, id="slow"),
    pytest.param(describing_mode.FAST, id="fast"),
    pytest.param(None, id="default fast"),
])
def test_user_sets_describe_ids_positive(siberia_client, describe_log_logbroker_client, describe_slow_log_logbroker_client, mode, ttl, siberia_config):
    ids = id_pb2.TIds(Ids=[
        id_pb2.TId(Type="yandexuid", Value="1"),
        id_pb2.TId(Type="yandexuid", Value="2"),
        id_pb2.TId(Type="yandexuid", Value="3"),
    ])

    user_set_id = int(siberia_client.user_sets_describe_ids(ids, mode=mode, ttl=ttl).UserSetId)

    messages = consumer_utils.read_all(describe_slow_log_logbroker_client.create_consumer() if describing_mode.is_slow(mode) else describe_log_logbroker_client.create_consumer(), timeout=10)
    assert len(messages) == 1

    ref_cmd = describe_cmd_pb2.TDescribeCmd(
        DescribeIdsCmd=describe_ids_cmd_pb2.TDescribeIdsCmd(
            UserSetId=user_set_id,
            Ids=ids.Ids,
            Experiment=describing_experiment_pb2.TDescribingExperiment(),
        ),
        Timestamp=FROZEN_TIME,
    )

    ref_user_set = user_set_pb2.TUserSet(
        Id=str(user_set_id),
        Status=user_set_status_pb2.TUserSetStatus().Ready,
        ExpirationTime=int(FROZEN_TIME + (ttl or siberia_config.Processors.UserSet.NotMaterializedTtlSeconds)),
        Title="not_materialized",
        Type=user_set_type_pb2.TUserSetType().NotMaterialized
    )

    assert ref_cmd == json_format.Parse(messages[0], describe_cmd_pb2.TDescribeCmd())
    assert ref_user_set == siberia_client.user_sets_get(user_set_id)
    return stats_to_dict(siberia_client.user_sets_get_stats(user_set_id))


@pytest.mark.parametrize("status_code,ids,mode,ttl", [
    pytest.param(codes.bad_request, id_pb2.TIds(Ids=[]), describing_mode.FAST, None, id="empty ids"),
    pytest.param(codes.bad_request, id_pb2.TIds(Ids=[id_pb2.TId(Type="yandexuid", Value="1")] * 11), describing_mode.FAST, None, id="too many ids"),
    pytest.param(codes.bad_request, id_pb2.TIds(Ids=[id_pb2.TId(Type="yandexuid", Value="1")]), "XYZ", None, id="invalid mode option"),
    pytest.param(codes.bad_request, id_pb2.TIds(Ids=[id_pb2.TId(Type="yandexuid", Value="1")]), describing_mode.FAST, "XXX", id="invalid ttl"),
])
def test_user_sets_describe_ids_negative(siberia_client, status_code, ids, mode, ttl, describe_log_logbroker_client):
    test_helpers.assert_http_error(status_code, lambda: siberia_client.user_sets_describe_ids(ids, mode=mode, ttl=ttl))
    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())


def test_user_sets_describe_ids_with_experiment_positive(local_ydb, siberia_client, describe_log_logbroker_client, siberia_config):
    experiment = describing_experiment_pb2.TDescribingExperiment(
        CryptaIdUserDataVersion="MEGA_VERSION",
    )

    test_helpers.create_crypta_id_user_data_table(local_ydb, "1500000000", version=experiment.CryptaIdUserDataVersion)

    ids = id_pb2.TIds(Ids=[
        id_pb2.TId(Type="yandexuid", Value="1"),
        id_pb2.TId(Type="yandexuid", Value="2"),
        id_pb2.TId(Type="yandexuid", Value="3"),
    ])

    user_set_id = int(siberia_client.user_sets_describe_ids(ids, experiment=experiment).UserSetId)

    messages = consumer_utils.read_all(describe_log_logbroker_client.create_consumer(), timeout=10)
    assert len(messages) == 1

    ref_cmd = describe_cmd_pb2.TDescribeCmd(
        DescribeIdsCmd=describe_ids_cmd_pb2.TDescribeIdsCmd(
            UserSetId=user_set_id,
            Ids=ids.Ids,
            Experiment=experiment,
        ),
        Timestamp=FROZEN_TIME,
    )

    ref_user_set = user_set_pb2.TUserSet(
        Id=str(user_set_id),
        Status=user_set_status_pb2.TUserSetStatus().Ready,
        ExpirationTime=int(FROZEN_TIME + siberia_config.Processors.UserSet.NotMaterializedTtlSeconds),
        Title="not_materialized",
        Type=user_set_type_pb2.TUserSetType().NotMaterialized
    )

    assert ref_cmd == json_format.Parse(messages[0], describe_cmd_pb2.TDescribeCmd())
    assert ref_user_set == siberia_client.user_sets_get(user_set_id)
    return stats_to_dict(siberia_client.user_sets_get_stats(user_set_id))


def test_user_sets_describe_ids_with_experiment_negative(local_ydb, siberia_client, describe_log_logbroker_client, siberia_config):
    experiment = describing_experiment_pb2.TDescribingExperiment(
        CryptaIdUserDataVersion="MEGA_VERSION",
    )

    ids = id_pb2.TIds(Ids=[
        id_pb2.TId(Type="yandexuid", Value="1"),
        id_pb2.TId(Type="yandexuid", Value="2"),
        id_pb2.TId(Type="yandexuid", Value="3"),
    ])

    test_helpers.assert_http_error(codes.bad_request, siberia_client.user_sets_describe_ids, ids, None, experiment)
    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
