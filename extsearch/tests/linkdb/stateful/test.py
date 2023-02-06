from __future__ import print_function

import extsearch.images.robot.rt.tests.bindings as bindings

from extsearch.images.robot.rt.tests.lib.bigrt_helper import TStatefulProcessTester, stateful_launchers

from extsearch.images.protos.linkdb_pb2 import TPageAttrsPB, TImageLinkPB


import pytest
import json
import yatest.common
import sys
import random


def create_test_queue_data(shard_ids):
    '''
    shard_ids: Dict<int,[float]>
      shard_id to some testing ids. Needed to create more specified test data
    return type: [Dict<int,[str]>, Dict<int, [str]>]
      first - testing input to queue shard_id to serialised QueueValueMessage protobuf
      second - result data, key to supposed protobuf message, serialized
    '''
    links_count = 10
    links_to_keep = 5

    expected_rows = {}
    data = {}
    linkdb_lib = bindings.TLinkDBBindings()

    for shard, ids in shard_ids.items():
        data[shard] = []
        for identifier in ids:
            url = "https://yandex.ru/" + str(identifier) + ".jpg"
            image_url_id = linkdb_lib.get_image_urlid(url)
            if expected_rows.get(image_url_id) is None:
                expected_rows[image_url_id] = []

            pageAttrsPBs = []
            imageLinkPBs = []
            for rank in range(links_count):
                pageAttrsPB = TPageAttrsPB()
                pageAttrsPB.WebAttrs.MetaRank = random.uniform(-10, 10)
                pageAttrsPBs.append(pageAttrsPB.SerializeToString())

                imageLinkPB = TImageLinkPB()
                imageLinkPB.Url = url
                imageLinkPBs.append(imageLinkPB.SerializeToString())

                expected_rows[image_url_id].append((url, pageAttrsPB.WebAttrs.MetaRank))

            result = linkdb_lib.create_and_pack_singlelink(pageAttrsPBs, imageLinkPBs)
            if result:
                data[shard].append(result)

            expected_rows[image_url_id] = sorted(expected_rows[image_url_id], key=lambda x: -x[1])[:links_to_keep - 1]
    return data, expected_rows


def convert_output(rows):
    '''
    rows: output state table of testing in current program processor
    return type: [Dict<int,[str]>] or something else same as supposed result type.
      key to protobuf message, serialized. To be compared with supposed result data.
    '''
    result = {}
    linkdb_lib = bindings.TLinkDBBindings()

    for row in rows:
        result[row['UrlId']] = []
        print(row['Url'], file=sys.stderr)
        url = row['Url']
        image_url_id = linkdb_lib.get_image_urlid(url)
        result[image_url_id] = []
        single_links = linkdb_lib.get_singlelinks_from_linkrt(row['CompressedLinks'], row['CompressedLinksPatch'], row['Codec'])
        for single_link in single_links:
            result[image_url_id].append((linkdb_lib.get_image_url(single_link), linkdb_lib.get_image_rank(single_link)))

        result[image_url_id] = sorted(result[image_url_id], key=lambda x: -x[1])
    return result


@pytest.fixture()
def table_config():
    with open(yatest.common.source_path('extsearch/images/robot/rt/tests/linkdb/stateful/table_config.json')) as json_file:
        return json.load(json_file)


@pytest.fixture()
def bigrt_config_path():
    return yatest.common.source_path("extsearch/images/robot/rt/tests/linkdb/stateful/bigrt_config.json")


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
    data, supposed_result = create_test_queue_data(shard_ids)

    process_tester.write_input_queue(data)

    # launch and wait stateful
    process_tester.process(
        stateful_launcher, binary_path=yatest.common.binary_path("extsearch/images/robot/rt/linkdb/stateful/stateful")
    )

    # get result in necessary format
    rows = process_tester.get_state_result("image_linksdb")

    result = convert_output(rows)
    assert supposed_result == result
