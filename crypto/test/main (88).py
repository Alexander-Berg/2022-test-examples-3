import itertools
import json
import logging
import time

from google.protobuf import json_format

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.common.segmentation import command_helpers


logger = logging.getLogger(__name__)
seq_no_counter = itertools.count(1)


def write_to_lb(producer, message):
    msg = json.dumps(json_format.MessageToDict(message))
    logger.info("Send %s", msg)
    assert producer.write(seq_no_counter.next(), msg).result(timeout=10).HasField("ack")


def test_basic(local_segmentator, segmentate_log_producer, local_ydb, segmentate_log_logbroker_client):
    user_set_id = test_helpers.get_random_id()
    message = command_helpers.create_segmentate_command(user_set_id=user_set_id, segment_id=4)

    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.upload_user_sets_table(local_ydb, [test_helpers.generate_user_set_db_row(user_set_id)])

    test_helpers.create_user_set(local_ydb, user_set_id)
    test_helpers.upload_user_set_from_yaml_dir(local_ydb, user_set_id, "data/test_basic")
    write_to_lb(segmentate_log_producer, message)
    time.sleep(10)

    result = local_ydb.dump_dir(test_helpers.get_user_set_path(user_set_id))
    result["lb_topic"] = consumer_utils.read_all(segmentate_log_logbroker_client.create_consumer())

    return result


def test_invalid_command(local_segmentator, segmentate_log_producer, segmentate_log_logbroker_client):
    segmentate_log_producer.write(seq_no_counter.next(), "xxx").result(timeout=10).HasField("ack")
    time.sleep(10)
    assert not consumer_utils.read_all(segmentate_log_logbroker_client.create_consumer())
