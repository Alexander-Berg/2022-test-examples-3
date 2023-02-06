# coding: utf-8

import pytest
import time
from hamcrest import assert_that, is_not


from yt.wrapper import ypath_join

from market.amore.output.lbdumper_amore.yatf.resources.config import LbDumperConfig
from market.amore.output.lbdumper_amore.yatf.resources.tokens import YtTokenStub
from market.amore.output.lbdumper_amore.yatf.matchers.env_matchers import HasAmoreInfoYtRows
from market.amore.output.lbdumper_amore.yatf.test_envs.test_env import LbDumperTestEnv
from market.amore.output.lbdumper_amore.yatf.utils.fixtures import (
    create_amore_item_for_lb,
)


from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.yatf.resources.yt_tables.lbdumper_tables import LbDumperAmoreTable
from market.idx.yatf.resources.logbroker_resource import log_broker_stuff  # noqa
from market.idx.yatf.resources.yt_stuff_resource import yt_server  # noqa
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix  # noqa


def current_time_sec():
    return int(round(time.time() * 1000))


@pytest.fixture(scope='module')
def lbk_input_topic(log_broker_stuff):  # noqa
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.fixture(scope='module', params=[None, 0, 1, 2])
def batch_size(request):
    return request.param


@pytest.yield_fixture(scope="module")
def yt_amore_info_table_path(batch_size):
    return ypath_join(get_yt_prefix(), 'lbdumper', 'amore_info_{}'.format(batch_size))


@pytest.yield_fixture(scope="module")
def yt_amore_info_table(yt_server, yt_amore_info_table_path):  # noqa
    return LbDumperAmoreTable(
        yt_stuff=yt_server,
        path=yt_amore_info_table_path,
        data=None
    )


@pytest.yield_fixture(scope="module")
def yt_blue_amore_info_table_path(batch_size):
    return ypath_join(get_yt_prefix(), 'lbdumper', 'blue_amore_info_{}'.format(batch_size))


@pytest.yield_fixture(scope="module")
def yt_blue_amore_info_table(yt_server, yt_blue_amore_info_table_path):  # noqa
    return LbDumperAmoreTable(
        yt_stuff=yt_server,
        path=yt_blue_amore_info_table_path,
        data=None
    )


@pytest.fixture(scope='module')
def lbdumper_config(
        log_broker_stuff,  # noqa
        lbk_input_topic,
        yt_server,  # noqa
        yt_token,
        yt_amore_info_table_path,
        yt_blue_amore_info_table_path,
        batch_size,
):
    cfg = LbDumperConfig()

    lbk_reader = cfg.create_lbk_topic_input_processor(log_broker_stuff, lbk_input_topic)
    proto_unpacker = cfg.create_proto_unpacker_processor()
    yt_writer = cfg.create_yt_dyn_table_sender_processor(
        yt_server,
        yt_token.path,
        yt_amore_info_table_path,
        'YtAmoreInfoSender'
    )
    yt_blue_writer = cfg.create_yt_dyn_table_sender_processor(
        yt_server,
        yt_token.path,
        yt_blue_amore_info_table_path,
        'YtBlueAmoreInfoSender'
    )

    cfg.create_link(lbk_reader, proto_unpacker)
    cfg.create_link(proto_unpacker, yt_writer)
    cfg.create_link(proto_unpacker, yt_blue_writer)
    return cfg

_current_time_sec = current_time_sec()

_items_data = [
    (1, 1, 'offer_not_blue', 8, 9, 'posdrrcpo', _current_time_sec - 1),
    (2, 2, 'offer_blue', 431782, 10, 'blue', _current_time_sec - 3)
]


@pytest.yield_fixture(scope='module')
def workflow(
        yt_server,  # noqa
        lbdumper_config,
        lbk_input_topic,
        yt_amore_info_table,
        yt_blue_amore_info_table
):
    resources = {
        'lbdumper_config': lbdumper_config,
        'lbk_input_topic': lbk_input_topic,
        'yt_amore_info_table': yt_amore_info_table,
        'yt_blue_amore_info_table': yt_blue_amore_info_table
    }

    with LbDumperTestEnv(yt_server, **resources) as lbdumper:
        lbdumper.verify()

        # for item in source_amore_items_common(FEED_ID_FOR_LB_ITEMS, create_amore_item_for_lb):
        for (business_id, feed_id, offer_id, shop_id, warehouse_id, amore_data, amore_ts_sec) in _items_data:
            item = create_amore_item_for_lb(business_id, feed_id, offer_id, shop_id, warehouse_id, amore_data, amore_ts_sec)
            lbk_input_topic.write(item.SerializeToString())
        time.sleep(5)
        yield lbdumper


@pytest.yield_fixture(scope='module')
def empty_workflow():
    yield None


# def test_make_protobuf(empty_workflow):
#     '''
#     Проверяем, что умеем делать протобуфку DatacampOffer
#     '''
#     p = create_amore_item_for_lb(
#         business_id=1,
#         feed_id=1,
#         offer_id='DasIdentifier',
#         shop_id=8,
#         warehouse_id=9,
#         amore_data='posdrrcpo',
#         amore_ts_sec=5000)
#
#     assert_that(p.offer[0].identifiers.feed_id == 1)
#     assert_that(p.offer[0].identifiers.offer_id == 'DasIdentifier')
#     assert_that(p.off
#     er[0].identifiers.shop_id == 8)
#     assert_that(p.offer[0].identifiers.warehouse_id == 9)
#     assert_that(p.offer[0].bids.amore_data.value == 'posdrrcpo')
#     assert_that(p.offer[0].bids.amore_data.meta.timestamp.seconds == 5000)


def test_amore_item_data_simple(workflow):
    """
    Проверяем, что данные из LB попадают в таблицу YT.
    """

    print("[TEST] test_amore_item_data_simple - {}".format(workflow.yt_amore_info_table_data))
    assert_that(
        workflow.yt_amore_info_table_data,
        HasAmoreInfoYtRows([
            {
                'feed_id' : 1,
                'business_id' : 1,
                'offer_id': 'offer_not_blue',
                'shop_id': 8,
                'warehouse_id': 9,
                'amore_data': 'posdrrcpo',
                'amore_ts_sec': _current_time_sec - 1,
            }
        ])
    )
    assert_that(
        workflow.yt_amore_info_table_data,
        HasAmoreInfoYtRows([
            {
                'feed_id': 2,
                'business_id' : 2,
                'offer_id': 'offer_blue',
                'shop_id': 431782,
                'warehouse_id': 10,
                'amore_data': 'blue',
                'amore_ts_sec': _current_time_sec - 3,
            }
        ])
    )

    assert_that(
        workflow.yt_blue_amore_info_table_data,
        is_not(HasAmoreInfoYtRows([
            {
                'feed_id': 1,
                'business_id' : 1,
                'offer_id': 'offer_not_blue',
                'shop_id': 8,
                'warehouse_id': 9,
                'amore_data': 'posdrrcpo',
            }
        ]))
    )
    assert_that(
        workflow.yt_blue_amore_info_table_data,
        HasAmoreInfoYtRows([
            {
                'feed_id': 2,
                'business_id' : 2,
                'offer_id': 'offer_blue',
                'shop_id': 431782,
                'warehouse_id': 10,
                'amore_data': 'blue',
                'amore_ts_sec': _current_time_sec - 3,
            }
        ])
    )
