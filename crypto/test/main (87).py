import itertools
import json
import logging
import time

from google.protobuf import json_format
import pytest

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
import crypta.siberia.bin.common.mutations.python as mutations


logger = logging.getLogger(__name__)
seq_no_counter = itertools.count(1)


def write_to_lb(producer, mutation):
    msg = json.dumps(json_format.MessageToDict(mutation))
    logger.info("Send %s", msg)
    assert producer.write(seq_no_counter.next(), msg).result(timeout=10).HasField("ack")


def test_remove_user_set_data__basic(local_mutator, change_log_producer, local_ydb, change_log_logbroker_client):
    user_set_id = test_helpers.get_random_id()
    mutation = mutations.create_remove_user_set_data_command(user_set_id=user_set_id)

    test_helpers.create_user_set(local_ydb, user_set_id)
    test_helpers.create_user_sets_table(local_ydb)

    write_to_lb(change_log_producer, mutation)
    time.sleep(10)
    assert not local_ydb.client.is_path_exists(test_helpers.get_user_set_path(user_set_id))
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


def test_remove_user_ser_data__meta_data_only(local_mutator, change_log_producer, local_ydb, change_log_logbroker_client):
    user_set_id = test_helpers.get_random_id()
    mutation = mutations.create_remove_user_set_data_command(user_set_id=user_set_id)

    test_helpers.create_user_set(local_ydb, user_set_id)
    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(user_set_id, status="meta_data_only")])

    write_to_lb(change_log_producer, mutation)
    time.sleep(10)
    assert not local_ydb.client.is_path_exists(test_helpers.get_user_set_path(user_set_id))
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


def test_remove_user_set_data__skip_if_exists(local_mutator, change_log_producer, local_ydb, change_log_logbroker_client):
    user_set_id = test_helpers.get_random_id()
    mutation = mutations.create_remove_user_set_data_command(user_set_id=user_set_id)

    test_helpers.create_user_set(local_ydb, user_set_id)
    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(user_set_id)])

    write_to_lb(change_log_producer, mutation)
    time.sleep(10)
    assert local_ydb.client.is_path_exists(test_helpers.get_user_set_path(user_set_id))
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())


@pytest.mark.parametrize("input_data_dir", [
    pytest.param("data/test_remove_segment_data__Basic", id="Basic"),
    pytest.param("data/test_remove_segment_data__SkipIfSegmentExists", id="SkipIfSegmentExists"),
])
def test_remove_segment_data(local_mutator, change_log_producer, local_ydb, change_log_logbroker_client, input_data_dir):
    user_set_id = test_helpers.get_random_id()
    mutation = mutations.create_remove_segment_data_command(user_set_id=user_set_id, segment_id=4)

    test_helpers.create_user_set(local_ydb, user_set_id)
    test_helpers.upload_user_set_from_yaml_dir(local_ydb, user_set_id, input_data_dir)
    write_to_lb(change_log_producer, mutation)
    time.sleep(10)

    result = local_ydb.dump_dir(test_helpers.get_user_set_path(user_set_id))
    result["lb_topic"] = consumer_utils.read_all(change_log_logbroker_client.create_consumer())

    return result


def test_invalid_command(local_mutator, change_log_producer, change_log_logbroker_client):
    change_log_producer.write(seq_no_counter.next(), "xxx").result(timeout=10).HasField("ack")
    time.sleep(10)
    assert not consumer_utils.read_all(change_log_logbroker_client.create_consumer())
