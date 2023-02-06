from __future__ import print_function

import extsearch.images.robot.rt.tests.bindings as bindings

from extsearch.images.robot.rt.usersessions.protos.state_pb2 import TSessionsInputQueuePB

from quality.user_sessions.rt.protos.public.images.request_profile_pb2 import TImagesRequestProfileMessage

from quality.user_sessions.rt.protos.public.images.notification_queue_pb2 import TImagesNotificationQueueMessage

from quality.user_sessions.rt.protos.common.enum_pb2 import EActionType

from extsearch.images.robot.rt.tests.lib.bigrt_helper import TStatefulProcessTester, stateful_launchers

import pytest
import json
import yatest.common
import time
import sys
import random
import string


def rand_crc(length):
    digits = string.digits
    return '11' + ''.join(random.choice(digits) for i in range(length))


def sort_dict(input_dict):
    for key in input_dict:
        input_dict[key] = sorted(input_dict[key])


def create_test_queue_data(shard_ids, module):
    '''
    shard_ids: Dict<int,[float]>
      shard_id to some testing ids. Needed to create more specified test data
      module: shards module
    return type: [Dict<int,[str]>, Dict<int, [str]>]
      first - testing input to queue shard_id to serialised QueueValueMessage protobuf
      second - result data, key to supposed protobuf message, serialized
    '''

    usersession_lib = bindings.TUsersessionBindings()
    expected_rows = {}
    data = {}

    for shard, ids in shard_ids.items():
        data[shard] = []
        for identifier in ids:
            # fill testing data
            queueValueMessage = TImagesNotificationQueueMessage()
            queueValueMessage.UserID = str(identifier) + "_user"
            queueValueMessage.RequestID = str(identifier) + "_request"
            queueValueMessage.ActionType = EActionType.Value('Click')
            queueValueMessage.ClickInfo.Crc = rand_crc(18)

            queueValueMessage.ClickInfo.Url = "http://900igr.net/up/datas/71460/click.jpg"
            queueValueMessage.Timestamp = int(time.time())

            data[shard].append(queueValueMessage.SerializeToString())

            output_shard = usersession_lib.get_hash_crc(queueValueMessage.ClickInfo.Crc) % module
            if expected_rows.get(output_shard) is None:
                expected_rows[output_shard] = []
            expected_rows[output_shard].append(
                (queueValueMessage.ClickInfo.Crc, "query " + str(identifier))
            )

            queueValueMessage = TImagesNotificationQueueMessage()
            queueValueMessage.UserID = str(identifier) + "_user"
            queueValueMessage.RequestID = str(identifier) + "_request"
            queueValueMessage.ActionType = EActionType.Value('ResultShowsBatch')
            queueValueMessage.Timestamp = int(time.time())
            for i in range(3):
                shows = queueValueMessage.ResultShowsInfo.Documents.add()
                shows.Crc = rand_crc(18)
                shows.Url = "http://900igr.net/up/datas/71460/show" + str(i) + ".jpg"

                output_shard = usersession_lib.get_hash_crc(shows.Crc) % module
                if expected_rows.get(output_shard) is None:
                    expected_rows[output_shard] = []
                expected_rows[output_shard].append((shows.Crc, "query " + str(identifier)))
            data[shard].append(queueValueMessage.SerializeToString())

    return data, expected_rows


def convert_output_queue(rows):
    '''
    rows: [Tuple] shard number, output queue table of testing in current program processor
      shard_id to some testing ids. Needed to create more specified test data
    return type: [Dict<int,[str]>] or something else same as supposed result type.
      key to protobuf message, serialized. To be compared with supposed result data.
    '''
    result = {}
    for shard, row in rows:
        message = TSessionsInputQueuePB()
        message.ParseFromString(row)

        if result.get(shard) is None:
            result[shard] = []
        result[shard].append((str(message.Crc), message.Query))

    return result


def create_state_table_data(shard_ids):
    '''
    shard_ids: Dict<int,[float]>
      shard_id to some testing ids. Needed to create more specified test data
    return type: list[dict[Any, str]]
      testing state data. Each row in list is mapping of table column name to column value.
      It will be written into state dyntable as yson format.
    '''

    read_only_states = []
    for ids in shard_ids.itervalues():
        for identifier in ids:
            message_profile = TImagesRequestProfileMessage()
            message_profile.UserID = str(identifier) + "_user"
            message_profile.RequestID = str(identifier) + "_request"
            message_profile.CorrectedQuery = "query " + str(identifier)
            read_only_states.append(
                dict(
                    user_id=message_profile.UserID,
                    reqid=message_profile.RequestID,
                    corrected_query=message_profile.CorrectedQuery,
                )
            )

    return read_only_states


@pytest.fixture()
def table_config():
    with open(yatest.common.source_path('extsearch/images/robot/rt/tests/usersessions/table_config.json')) as json_file:
        return json.load(json_file)


@pytest.fixture()
def bigrt_config_path():
    return yatest.common.source_path("extsearch/images/robot/rt/tests/usersessions/bigrt_config.json")


@pytest.fixture()
def config_option():
    return "--config-json"


@pytest.mark.parametrize("stateful_launcher", stateful_launchers)
def test_stateful(testing_stand, stateful_launcher):
    print(">>> OUTPUT DIR: " + yatest.common.output_path(), file=sys.stderr)
    stand = testing_stand
    process_tester = TStatefulProcessTester(stand)

    # generate test data & shard id
    shard_ids = TStatefulProcessTester.gen_shard_testing_schemas(
        shards_count=stand.input_yt_queue["shards"], data_part_length=stand.data_part_length
    )

    # create testing input data and needed result
    module = process_tester.get_output_queue_shards_number("queue_output_notification")
    data, supposed_result = create_test_queue_data(shard_ids, module)
    process_tester.write_input_queue(data)

    # fill state with testing data
    read_only_states = create_state_table_data(shard_ids)
    process_tester.write_into_state(read_only_states, "images_request_profile")

    # launch and wait stateful
    process_tester.process(
        stateful_launcher,
        binary_path=yatest.common.binary_path("extsearch/images/robot/rt/usersessions/resharder/resharder"),
    )

    # get result in necessary format
    rows = process_tester.read_from_queue("queue_output_notification")
    result = convert_output_queue(rows)

    sort_dict(supposed_result)
    sort_dict(result)
    assert supposed_result == result
