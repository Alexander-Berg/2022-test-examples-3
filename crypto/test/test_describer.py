import itertools
import time

from google.protobuf import json_format
import pytest
import yaml
import yatest.common

from crypta.lib.proto.identifiers import id_pb2
from crypta.lib.proto.user_data import user_data_stats_pb2
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.describing.experiment.proto import describing_experiment_pb2
from crypta.siberia.bin.common.describing.proto import (
    describe_cmd_pb2,
    describe_ids_cmd_pb2,
    describe_user_set_cmd_pb2,
)


counter = itertools.count(1)
FROZEN_TIME = 1500000000
EXPERIMENTAL_CRYPTA_ID_USER_DATA_VERSION = "MEGA_VERSION"


def to_bytes(data):
    return data.encode("utf-8")


def prepare_stats_table_data(user_set_stats_table_data):
    for row in user_set_stats_table_data:
        user_data_stats = user_data_stats_pb2.TUserDataStats()
        user_data_stats.ParseFromString(row["stats"])
        row["stats"] = json_format.MessageToJson(user_data_stats)

    return user_set_stats_table_data


def get_describe_ids_message(user_set_id, ids, experiment=None):
    experiment = experiment or describing_experiment_pb2.TDescribingExperiment()
    cmd = describe_ids_cmd_pb2.TDescribeIdsCmd(UserSetId=user_set_id, Ids=ids, Experiment=experiment)
    return json_format.MessageToJson(describe_cmd_pb2.TDescribeCmd(DescribeIdsCmd=cmd, Timestamp=FROZEN_TIME + 10), indent=None)


def test_describer(siberia_describer, describe_log_logbroker_client, describe_log_producer, local_ydb):
    ids_1 = [
        id_pb2.TId(Type="invalid_type", Value="login1@yandex.ru"),
        id_pb2.TId(Type="login", Value="login1@yandex.ru"),
        id_pb2.TId(Type="login", Value="login_wo_crypta_id_1"),
        id_pb2.TId(Type="login", Value="login2@yandex.ru"),
        id_pb2.TId(Type="login", Value="login_wo_crypta_id_2"),
        id_pb2.TId(Type="crypta_id", Value="7"),
    ]

    ids_2 = [
        id_pb2.TId(Type="login", Value="login_wo_crypta_id_3"),
        id_pb2.TId(Type="login", Value="login_wo_crypta_id_4"),
        id_pb2.TId(Type="login", Value="login2@yandex.ru"),
    ]

    idfa_gaid = [
        id_pb2.TId(Type="idfa_gaid", Value=value)
        for value in ('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'invalid')
    ]

    msg_10 = get_describe_ids_message(10, ids_1)
    msg_11 = get_describe_ids_message(11, ids_1 + ids_2)
    msg_12 = get_describe_ids_message(12, [])
    msg_13 = get_describe_ids_message(13, [id_pb2.TId(Type="yandexuid", Value="invalid_yandexuid")])
    msg_14 = get_describe_ids_message(14, idfa_gaid)

    result = describe_log_producer.write(
        next(counter),
        to_bytes("\n".join([msg_10, msg_11, msg_12, msg_13, msg_14])),
    ).result(timeout=10)
    assert result.HasField("ack")

    time.sleep(10)

    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    user_set_stats_table = prepare_stats_table_data(test_helpers.dump_user_set_stats_table(local_ydb))
    return user_set_stats_table


def test_describer_db_set(siberia_describer, describe_log_logbroker_client, describe_log_producer, local_ydb):
    user_set_id = 1

    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(user_set_id)])

    test_helpers.create_user_set_dir(local_ydb, user_set_id)
    test_helpers.create_users_table(local_ydb, user_set_id)
    test_helpers.create_user_attributes_table(local_ydb, user_set_id)

    with open(yatest.common.test_source_path("data/users.yaml")) as f:
        test_helpers.upload_users_table(local_ydb, user_set_id, yaml.safe_load(f))

    with open(yatest.common.test_source_path("data/user_attributes.yaml")) as f:
        test_helpers.upload_user_attributes_table(local_ydb, user_set_id, yaml.safe_load(f))

    msg = json_format.MessageToJson(describe_cmd_pb2.TDescribeCmd(DescribeUserSetCmd=describe_user_set_cmd_pb2.TDescribeUserSetCmd(UserSetId=user_set_id), Timestamp=FROZEN_TIME + 10), indent=None)
    result = describe_log_producer.write(next(counter), to_bytes(msg)).result(timeout=10)
    assert result.HasField("ack")

    time.sleep(10)

    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    user_set_stats_table = prepare_stats_table_data(test_helpers.dump_user_set_stats_table(local_ydb))
    return user_set_stats_table


def test_describer_absent_db_set(siberia_describer, describe_log_logbroker_client, describe_log_producer, local_ydb):
    user_set_id = 1

    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.upload_user_sets_table(local_ydb, [])

    msg = json_format.MessageToJson(describe_cmd_pb2.TDescribeCmd(DescribeUserSetCmd=describe_user_set_cmd_pb2.TDescribeUserSetCmd(UserSetId=user_set_id), Timestamp=FROZEN_TIME + 10), indent=None)
    result = describe_log_producer.write(next(counter), to_bytes(msg)).result(timeout=10)
    assert result.HasField("ack")

    time.sleep(10)

    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    user_set_stats_table = prepare_stats_table_data(test_helpers.dump_user_set_stats_table(local_ydb))
    assert not user_set_stats_table


@pytest.mark.crypta_id_user_data_version(EXPERIMENTAL_CRYPTA_ID_USER_DATA_VERSION)
def test_describer_with_experiment(siberia_describer, describe_log_logbroker_client, describe_log_producer, local_ydb):
    experiment = describing_experiment_pb2.TDescribingExperiment(
        CryptaIdUserDataVersion=EXPERIMENTAL_CRYPTA_ID_USER_DATA_VERSION,
    )

    ids = [
        id_pb2.TId(Type="login", Value="login1@yandex.ru"),
        id_pb2.TId(Type="login", Value="login2@yandex.ru"),
        id_pb2.TId(Type="crypta_id", Value="7"),
    ]

    msg_10 = get_describe_ids_message(10, ids, experiment)

    result = describe_log_producer.write(next(counter), to_bytes("\n".join([msg_10]))).result(timeout=10)
    assert result.HasField("ack")

    time.sleep(10)

    assert not consumer_utils.read_all(describe_log_logbroker_client.create_consumer())
    user_set_stats_table = prepare_stats_table_data(test_helpers.dump_user_set_stats_table(local_ydb))
    return user_set_stats_table
