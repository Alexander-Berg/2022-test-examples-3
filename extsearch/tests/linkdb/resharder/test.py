from __future__ import print_function

import extsearch.images.robot.rt.tests.bindings as bindings

from robot.rthub.yql.protos.queries_pb2 import TImagePageItem

from extsearch.images.robot.rt.tests.lib.bigrt_helper import TStatefulProcessTester, stateful_launchers

from yweb.protos.outlinks.links_pb2 import TOutputLinksData

from yweb.protos.links_pb2 import TLinkTextual


import pytest
import json
import cyson
import yatest.common
import sys
import snappy


def parse_yson_file(file):
    '''
    file: File
      yson file with testing data
    return type: [TImagePageItem]
    '''
    raw_data = open(file, mode='rb').read()
    dd = raw_data.split(b';{')
    image_page_items = []
    for i, line in enumerate(dd[:-1]):
        if i:
            line = b'{' + line
        image_page_items.append(cyson.loads(line)[b'value'])
    return image_page_items


def extract_link_textual_block(blocks, blockType):
    '''
    blocks: TLinkTextual proto
    blockType: TLinkTextual proto EType in Type field
    return type: decompressed field
    '''
    for block in blocks:
        if block.Type == blockType:
            if block.Coder != TLinkTextual.TBlock.SNAPPY:
                raise RuntimeError("Unknown coder block type")
            return snappy.decompress(block.Data).split(b'\x00')
    return None


def extract_links_to_tsv(linksRecord):
    '''
    linksRecord: TOutputLinksData proto
    return type: combined data
    '''
    urls = extract_link_textual_block(linksRecord.Text.Data, TLinkTextual.TBlock.FOREIGNURLS)
    text = extract_link_textual_block(linksRecord.Text.Data, TLinkTextual.TBlock.TEXT)
    for link in linksRecord.Link:
        yield [
            urls[link.OriginalUrlKey],
            text[link.TextKey],
        ]


def create_test_queue_data(shard_ids, module):
    '''
    shard_ids: Dict<int,[float]>
      shard_id to some testing ids. Needed to create more specified test data
    module: shards module
    return type: [Dict<int,[str]>, Dict<int, [str]>]
      first - testing input to queue shard_id to serialised QueueValueMessage protobuf
      second - result data, key to supposed protobuf message, serialized
    '''
    expected_rows = {}
    data = {}
    linkdb_lib = bindings.TLinkDBBindings()

    image_page_items = parse_yson_file(yatest.common.runtime.work_path("data/rthub"))
    ind_image_page_items = 0
    len_image_page_items = len(image_page_items)

    for shard, ids in shard_ids.items():
        data[shard] = []
        for identifier in ids:
            current_data = image_page_items[ind_image_page_items]
            data[shard].append(linkdb_lib.zLib_compress(current_data))
            ind_image_page_items = (ind_image_page_items + 1) % len_image_page_items

            if linkdb_lib.is_valid_rthub(current_data):
                is_good_data = True
                current_subset_expected_rows = {}

                image_page_item = TImagePageItem()
                image_page_item.ParseFromString(current_data)

                image_links = TOutputLinksData()
                image_links.ParseFromString(image_page_item.ImageLinks)

                for link in extract_links_to_tsv(image_links):
                    try:
                        url = link[0].decode('ascii')
                        shard_num = linkdb_lib.get_image_urlid(url) % module
                        if current_subset_expected_rows.get(shard_num) is None:
                            current_subset_expected_rows[shard_num] = set()
                        current_subset_expected_rows[shard_num].add(url)
                    except UnicodeDecodeError:
                        data[shard].pop()
                        is_good_data = False
                        break
                if is_good_data:
                    for key, link_set in current_subset_expected_rows.items():
                        if expected_rows.get(key) is None:
                            expected_rows[key] = link_set
                        else:
                            expected_rows[key].update(link_set)

    return data, expected_rows


def convert_output_queue(rows, module):
    '''
    rows: [Tuple] shard number, output queue table of testing in current program processor
      shard_id to some testing ids. Needed to create more specified test data
    module: shards module
    return type: [Dict<int,[str]>] or something else same as supposed result type.
      key to protobuf message, serialized. To be compared with supposed result data.
    '''
    linkdb_lib = bindings.TLinkDBBindings()
    result = {}
    for shard, row in rows:
        unpacker = linkdb_lib.unpack_packer(row)
        for message in unpacker:
            url = linkdb_lib.get_image_url(message)
            shard_num = linkdb_lib.get_image_urlid(url) % module
            if result.get(shard_num) is None:
                result[shard_num] = set()
            result[shard_num].add(url)
    return result


@pytest.fixture()
def table_config():
    with open(yatest.common.source_path('extsearch/images/robot/rt/tests/linkdb/resharder/table_config.json')) as json_file:
        return json.load(json_file)


@pytest.fixture()
def bigrt_config_path():
    return yatest.common.source_path("extsearch/images/robot/rt/tests/linkdb/resharder/bigrt_config.json")


@pytest.mark.parametrize("stateful_launcher", stateful_launchers)
def test_stateful(testing_stand, stateful_launcher):
    print(">>> OUTPUT DIR: " + yatest.common.output_path(), file=sys.stderr)
    stand = testing_stand
    process_tester = TStatefulProcessTester(stand)

    # generate test data & shard id
    shard_ids = TStatefulProcessTester.gen_shard_testing_schemas(
        shards_count=stand.input_yt_queue["shards"], data_part_length=stand.data_part_length
    )
    module = process_tester.get_output_queue_shards_number("output_queue")
    # create testing input data and needed result
    data, supposed_result = create_test_queue_data(shard_ids, module)
    # print(supposed_result, file=sys.stderr)
    process_tester.write_input_queue(data)

    # launch and wait stateful
    process_tester.process(
        stateful_launcher, binary_path=yatest.common.binary_path("extsearch/images/robot/rt/linkdb/resharder/resharder")
    )

    # get result in necessary format
    rows = process_tester.read_from_queue("output_queue")

    result = convert_output_queue(rows, module)

    assert supposed_result == result
