import datetime
import logging
import os

import pytest

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python import yaml2proto
from crypta.siberia.bin.common import test_helpers
from crypta.siberia.bin.core.lib.configs.proto import config_pb2


logger = logging.getLogger(__name__)
pytest_plugins = [
    "crypta.lib.python.juggler.test_utils.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.siberia.bin.core.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="function", autouse=True)
def setup(local_ydb, change_log_logbroker_client, describe_log_logbroker_client, describe_slow_log_logbroker_client, segmentate_log_logbroker_client):
    logger.info("SETUP")

    consumer_utils.read_all(change_log_logbroker_client.create_consumer(), timeout=1)
    consumer_utils.read_all(describe_log_logbroker_client.create_consumer(), timeout=1)
    consumer_utils.read_all(describe_slow_log_logbroker_client.create_consumer(), timeout=1)
    consumer_utils.read_all(segmentate_log_logbroker_client.create_consumer(), timeout=1)

    local_ydb.remove_all()
    # TODO(kolontaev): Унести это в Siberia
    test_helpers.create_user_sets_table(local_ydb)
    test_helpers.create_user_set_stats_table(local_ydb)


@pytest.fixture(scope="function")
def siberia_config(local_siberia):
    os.environ.update(local_siberia.env)
    config = config_pb2.TConfig()

    with open(local_siberia.config_path) as f:
        yaml2proto.yaml2proto(f.read(), config)

    return config


@pytest.fixture(scope="session")
def user_set_ttl():
    return int(datetime.timedelta(days=4).total_seconds())


@pytest.fixture(scope="function")
def user_set_id(setup, siberia_client, user_set_ttl):
    return siberia_client.user_sets_add(title="user-set-xyz", ttl=user_set_ttl).UserSetId


@pytest.fixture(scope="function")
def ready_user_set_id(siberia_client, user_set_id):
    test_helpers.ready_user_set(siberia_client, user_set_id)
    return user_set_id


@pytest.fixture(scope="function")
def segment(local_ydb, siberia_client, ready_user_set_id):
    segment_id = "1"
    test_helpers.upload_segments_table(local_ydb, ready_user_set_id, [test_helpers.generate_segment_db_row(segment_id)])

    segments = siberia_client.segments_search(ready_user_set_id).Segments
    assert len(segments) == 1

    segment = segments[0]
    assert segment_id == segment.Id

    return segment
