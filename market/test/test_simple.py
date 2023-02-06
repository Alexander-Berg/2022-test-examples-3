from __future__ import print_function

import collections
import itertools
import logging
import os
import random
import re
import yt.wrapper

import jinja2
import pytest
import yatest.common
import ujson as json
# import yt.yson as yson
from market.backbone.offers_store.proto.offer_pb2 import (
    TContentOffer,
    TServiceOffer,
    TOfferPrice,
)
# from market.backbone.offers_store.profiles.proto.service_offer_pb2 import (
#     TServiceOfferProfileProto
# )

import market.backbone.offers_store.profiles.python as caesar_profiles

# from market.backbone.offers_store.profiles.python import (
#     extract_content_offer_profile_proto_as_json,
#     extract_service_offer_profile_proto_as_json,
# )
from ads.bsyeti.caesar.tests.lib.b2b import utils

from ads.bsyeti.big_rt.py_test_lib import (
    BulliedProcess,
    create_yt_queue,
    execute_cli,
    launch_bullied_processes_reading_queue,
    make_json_file,
    make_namedtuple,
)
from library.python.sanitizers import asan_is_on
from .conftest import (
    offer_update_event,
    pack_message
)

log = logging.getLogger(__name__)


def select_rows(yt_client, query):
    _format = yt.wrapper.format.YsonFormat(encoding=None)
    return yt_client.select_rows(query, yt.wrapper.SYNC_LAST_COMMITED_TIMESTAMP, format=_format)


def create_state_table(yt_client, path, schema_generator, shard_count):
    log.info("Creating state table %s", path)
    schema = schema_generator(shard_count, False, 0, "farm_hash", "Hash")
    yt_client.create(
        "table",
        path,
        recursive=True,
        attributes={
            "dynamic": True,
            "schema": schema
        },
    )
    log.info("Mounting state table %s", path)
    yt_client.mount_table(path, sync=True)
    log.info("Mounted state table %s", path)


def extract_all_states(yt_client, path, extractor):
    query = "* from [{table}]".format(table=path)
    rows = select_rows(yt_client, query)
    # TODO: on of the way to obtain row from state table for udf tests
    # with open('/tmp/logs'.format(), 'a') as f:
    #     f.write(path)
    #     f.write('\n')
    #     for row in rows:
    #         f.write(yson.dumps(row).decode('utf-8'))
    #         f.write('\n')

    rows = [json.loads(utils.safe_binaryjson_decode(extractor(row))) for row in rows]
    rows.sort(key=lambda p: json.dumps(p, sort_keys=True))
    return rows


@pytest.fixture()
def stand(
    request,
    fully_ready_yt,
    primary_yt_cluster,
    yt_ready_env,
    port_manager,
    config_test_default_enabled,
):
    if not asan_is_on():
        input_shards_count = 10
        data_part_length = 500
        restart_max_seconds = 40
    else:
        input_shards_count = 3
        data_part_length = 100
        restart_max_seconds = 100

    test_id = re.sub(r"[^\w\d]", "_", request.node.name)
    input_queue_path = "//tmp/input_queue_" + test_id
    consuming_system_path = "//tmp/test_consuming_system_" + test_id
    content_offers_state_table_path = "//tmp/states/ContentOfferState_" + test_id
    service_offers_state_table_path = "//tmp/states/ServiceOfferState_" + test_id

    input_yt_queue = create_yt_queue(primary_yt_cluster.get_yt_client(), input_queue_path, input_shards_count)

    queue_consumer = "stateful_cow"
    execute_cli(["consumer", "create", input_yt_queue["path"], queue_consumer, "--ignore-in-trimming", "0"])

    yt_client = fully_ready_yt["clusters"][0].get_yt_client()

    create_state_table(
        yt_client,
        content_offers_state_table_path,
        caesar_profiles.content_offer_table_schema,
        input_shards_count,
    )
    create_state_table(
        yt_client,
        service_offers_state_table_path,
        caesar_profiles.service_offer_table_schema,
        input_shards_count,
    )

    return make_namedtuple("offers_store", **locals())


def gen_shard_testing_schemas(length):
    for j in range(1, 1000):
        cnt = int(2.5**j)
        yield [i % cnt for i in range(length)]
        yield [random.randint(0, cnt - 1) for i in range(length)]


def gen_testing_shard_ids(shards_count, length):
    schemas = itertools.islice(gen_shard_testing_schemas(length), shards_count)

    data = {shard: ["%d" % ((u + 1) * shards_count + shard) for u in schema] for shard, schema in enumerate(schemas)}
    test_ids = collections.Counter(uid for ids in data.values() for uid in ids)

    flag_ids = ["%d" % int(shards_count * 1e9 + shard) for shard in range(shards_count)]
    for i, flag_uid in enumerate(flag_ids):
        data[i].append(flag_uid)
    return data, dict(test_ids), set(flag_ids)


def make_stateful_config(stand, worker_minor_name="", workers=1):
    shards_count = stand.input_yt_queue["shards"]
    with open(yatest.common.source_path("market/backbone/offers_store/test/config.json.j2")) as f:
        conf_s = jinja2.Template(f.read()).render(
            shards_count=shards_count,
            max_shards=int((shards_count + workers - 1) / workers),
            port=stand.port_manager.get_port(),
            consuming_system_main_path=stand.consuming_system_path,
            consumer=stand.queue_consumer,
            input_queue=stand.input_yt_queue["path"],
            yt_cluster=os.environ["YT_PROXY"],
            global_log=os.path.join(yatest.common.output_path(), "global_{}.log".format(worker_minor_name)),
            worker_minor_name=worker_minor_name,
            content_offers_state_table_path=stand.content_offers_state_table_path,
            service_offers_state_table_path=stand.service_offers_state_table_path,
            max_inflight_bytes=str(int(200 * shards_count / workers)),
        )
    return make_namedtuple(
        "StatefulConfig",
        path=make_json_file(conf_s, name_template="sharding_config_{json_hash}.json"),
    )


def decode_proto_state(state, state_type):
    state_proto = state_type()
    state_proto.ParseFromString(state)
    return state_proto


class StatefulProcess(BulliedProcess):
    def __init__(self, config_path):
        super(StatefulProcess, self).__init__(
            launch_cmd=[
                yatest.common.binary_path("market/backbone/offers_store/bin/offers_store"),
                "--config-json",
                config_path,
            ]
        )


def stateful_launcher(stand, data):
    config = make_stateful_config(stand, worker_minor_name=str(1), workers=1)
    statefuls = [StatefulProcess(config.path)]
    launch_bullied_processes_reading_queue(
        statefuls,
        stand.input_yt_queue,
        stand.queue_consumer,
        data,
        timeout=600,
    )


def test_stateful_keys(stand):
    all_data = {i: [] for i in range(stand.input_yt_queue["shards"])}
    data = {0: [
        pack_message(offer_update_event(
            yabs_id=1,
            business_id=1,
            content_offer=TContentOffer(
                FeedId=1000,
            )
        )),
        pack_message(offer_update_event(
            yabs_id=2,
            business_id=2,
            content_offer=TContentOffer(FeedId=2000),
            subkeys=[[2, 2], [3, 3]],
            service_offers=[
                TServiceOffer(BasicPrice=TOfferPrice(Vat=20)),
                TServiceOffer(BasicPrice=TOfferPrice(Vat=30))
            ],
        )),
    ]}
    for k in data:
        all_data[k].extend(data[k])

    stand.input_yt_queue["queue"].write(all_data)
    # launch and wait stateful
    stateful_launcher(stand=stand, data=all_data)
    log.info("Stateful processor read all messages and was finished")

    content_offers_state = extract_all_states(
        stand.yt_client,
        stand.content_offers_state_table_path,
        caesar_profiles.extract_content_offer_profile_proto_as_json
    )
    service_offers_state = extract_all_states(
        stand.yt_client,
        stand.service_offers_state_table_path,
        caesar_profiles.extract_service_offer_profile_proto_as_json,
    )

    # extract counters
    expected_primary_key = set([1, 2])
    expected_feed_ids = set([1000, 2000])
    actual_yabs = set()
    actual_business = set()
    actual_feed_ids = set()
    for state in content_offers_state:
        actual_yabs.add(state['YabsId'])
        actual_business.add(state['BusinessId'])
        actual_feed_ids.add(state['Offer']['FeedId'])
    assert actual_yabs == expected_primary_key
    assert actual_business == expected_primary_key
    assert actual_feed_ids == expected_feed_ids

    expected_primary_key = set([2])
    expected_secondary_key = set([2, 3])
    expected_vat = set([20, 30])
    actual_yabs = set()
    actual_business = set()
    actual_shop = set()
    actual_warehouse = set()
    actual_vat = set()
    for state in service_offers_state:
        actual_yabs.add(state['YabsId'])
        actual_business.add(state['BusinessId'])
        actual_shop.add(state['ShopId'])
        actual_warehouse.add(state['WarehouseId'])
        actual_vat.add(state['Offer']['BasicPrice']['Vat'])
    assert actual_yabs == expected_primary_key
    assert actual_business == expected_primary_key
    assert actual_shop == expected_secondary_key
    assert actual_warehouse == expected_secondary_key
    assert actual_vat == expected_vat
