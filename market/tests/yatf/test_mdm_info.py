# coding: utf-8

import pytest
import time
from hamcrest import assert_that

from yt.wrapper import ypath_join

from market.idx.input.mdm_dumper.yatf.resources.config import LbDumperConfig
from market.idx.input.mdm_dumper.yatf.resources.yt_tables import MdmReferenceItemTable
from market.idx.input.mdm_dumper.yatf.resources.tokens import YtTokenStub
from market.idx.input.mdm_dumper.yatf.matchers.env_matchers import HasMdmInfoYtRows
from market.idx.input.mdm_dumper.yatf.test_envs.test_env import LbDumperTestEnv
from market.idx.input.mdm_dumper.yatf.utils.fixtures import (
    Dimension,
    Weight,
    MdmItemInformation,
    MdmItemInformationList,
    create_mdm_reference_item_batch_for_lb,
    create_mdm_reference_item_for_yt,
)

from market.idx.datacamp.yatf.utils import create_update_meta, create_ts
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    WeightAndDimensions,
    MARKET_MDM,
)
from market.proto.indexer.mdm_item_pb2 import (
    MdmItemInformation as MdmItemInformationProto,
    MdmItemForWarehouse as MdmItemForWarehouseProto,
    OfferMdmInfo,
)

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.resources.yt_tables.lbdumper_tables import LbDumperMdmTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


SUPPLIER_ID = 1
SUPPLIER_ID_FROM_TABLE = 2
SUPPLIER_ID_FROM_LB = 3


@pytest.fixture(scope='module')
def lbk_input_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.fixture(scope='module', params=[1000])
def batch_size(request):
    return request.param


@pytest.yield_fixture(scope="module")
def yt_mdm_info_table_path():
    return ypath_join(get_yt_prefix(), 'lbdumper', 'mdm')


@pytest.yield_fixture(scope="module")
def yt_mdm_info_table(yt_server, yt_mdm_info_table_path):
    return LbDumperMdmTable(
        yt_stuff=yt_server,
        path=yt_mdm_info_table_path,
        data=[{
            'supplier_id': SUPPLIER_ID,
            'shop_sku': 'shop_sku_existed_without_version',
            'updated_ts': 10000,
            'write_ts': 10000,
            'mdm_info': MdmItemInformationProto(
                warehouses=[
                    MdmItemForWarehouseProto(
                        information=OfferMdmInfo(
                            dimensions=WeightAndDimensions(
                                weight=1,
                                length=2,
                                width=3,
                                height=4,
                            ),
                            meta=create_update_meta(10, source=MARKET_MDM)
                        ),
                    )
                ],
                timestamp=create_ts(10),
            ).SerializeToString()
        }, {
            'supplier_id': SUPPLIER_ID,
            'shop_sku': 'shop_sku_existed_with_greater_version',
            'updated_ts': 10000,
            'write_ts': 10000,
            'mdm_info': MdmItemInformationProto(
                warehouses=[
                    MdmItemForWarehouseProto(
                        information=OfferMdmInfo(
                            dimensions=WeightAndDimensions(
                                weight=1,
                                length=2,
                                width=3,
                                height=4,
                            ),
                            meta=create_update_meta(10, source=MARKET_MDM),
                        ),
                        version=1000,
                    )
                ],
                timestamp=create_ts(10),
                version=1000,
            ).SerializeToString(),
        }, {
            'supplier_id': SUPPLIER_ID,
            'shop_sku': 'shop_sku_existed_with_lower_version',
            'updated_ts': 10000,
            'write_ts': 10000,
            'mdm_info': MdmItemInformationProto(
                warehouses=[
                    MdmItemForWarehouseProto(
                        information=OfferMdmInfo(
                            dimensions=WeightAndDimensions(
                                weight=1,
                                length=2,
                                width=3,
                                height=4,
                            ),
                            meta=create_update_meta(10, source=MARKET_MDM),
                        ),
                        version=1,
                    )
                ],
                timestamp=create_ts(10),
                version=1,
            ).SerializeToString()
        }]
    )


def new_mdm_items(supplier_id, mdm_item_creator):
    return [
        mdm_item_creator(
            supplier_id=supplier_id,
            shop_sku='shop_sku_new',
            information_fragments=[
                MdmItemInformation(
                    weight=Weight(2700000, 1000),
                    width=Dimension(27000, 2000),
                    height=Dimension(23000, 3000),
                    length=Dimension(21100, 4000),
                    version=10,
                )
            ]
        ),
    ]


def existed_mdm_items():
    mdm_item_creator = create_mdm_reference_item_batch_for_lb
    return [
        mdm_item_creator(
            supplier_id=SUPPLIER_ID,
            shop_sku='shop_sku_existed_without_version',
            information_fragments=[
                MdmItemInformation(
                    weight=Weight(2700000, 1000),
                    width=Dimension(27000, 2000),
                    height=Dimension(23000, 3000),
                    length=Dimension(21100, 4000),
                )
            ]
        ),
        mdm_item_creator(
            supplier_id=SUPPLIER_ID,
            shop_sku='shop_sku_existed_with_greater_version',
            information_fragments=[
                MdmItemInformation(
                    weight=Weight(2700000, 1000),
                    width=Dimension(27000, 2000),
                    height=Dimension(23000, 3000),
                    length=Dimension(21100, 4000),
                    version=10,
                )
            ]
        ),
        mdm_item_creator(
            supplier_id=SUPPLIER_ID,
            shop_sku='shop_sku_existed_with_lower_version',
            information_fragments=[
                MdmItemInformation(
                    weight=Weight(2700000, 1000),
                    width=Dimension(27000, 2000),
                    height=Dimension(23000, 3000),
                    length=Dimension(21100, 4000),
                    version=10,
                )
            ]
        ),
    ]


@pytest.yield_fixture(scope="module")
def yt_mdm_reference_item_table_path(batch_size):
    return ypath_join(get_yt_prefix(), 'mstat', 'mdm_reference_item', '2020-09-13')


@pytest.fixture(scope='module')
def yt_mdm_reference_item_table(
    yt_server,
    yt_mdm_reference_item_table_path
):
    return MdmReferenceItemTable(
        yt_stuff=yt_server,
        path=yt_mdm_reference_item_table_path,
        data=new_mdm_items(SUPPLIER_ID_FROM_TABLE, create_mdm_reference_item_for_yt),
        )


@pytest.fixture(scope='module')
def lbdumper_config(
    log_broker_stuff,
    lbk_input_topic,
    yt_server,
    yt_token,
    yt_mdm_info_table_path,
    yt_mdm_reference_item_table_path,
    batch_size,
):
    cfg = LbDumperConfig()

    lbk_reader = cfg.create_lbk_topic_input_processor(log_broker_stuff, lbk_input_topic)
    proto_unpacker = cfg.create_proto_unpacker_processor()
    yt_writer = cfg.create_yt_dyn_table_sender_processor(
        yt_server,
        yt_token.path,
        yt_mdm_info_table_path,
    )
    yt_node_unpacker = cfg.create_yt_node_unpacker_processor()
    yt_reader = cfg.create_yt_static_table_input_processor(
        yt_server,
        yt_token.path,
        yt_mdm_reference_item_table_path,
        batch_size=batch_size
    )

    cfg.create_link(lbk_reader, proto_unpacker)
    cfg.create_link(proto_unpacker, yt_writer)
    cfg.create_link(yt_reader, yt_node_unpacker)
    cfg.create_link(yt_node_unpacker, yt_writer)
    return cfg


@pytest.yield_fixture(scope='module')
def workflow(
    yt_server,
    lbdumper_config,
    lbk_input_topic,
    yt_mdm_info_table,
    yt_mdm_reference_item_table
):
    resources = {
        'lbdumper_config': lbdumper_config,
        'lbk_input_topic': lbk_input_topic,
        'yt_mdm_info_table': yt_mdm_info_table,
        'yt_mdm_reference_item_table': yt_mdm_reference_item_table,
    }

    with LbDumperTestEnv(yt_server, **resources) as lbdumper:
        lbdumper.verify()

        for item in new_mdm_items(SUPPLIER_ID_FROM_LB, create_mdm_reference_item_batch_for_lb):
            lbk_input_topic.write(item.SerializeToString())
        for item in existed_mdm_items():
            lbk_input_topic.write(item.SerializeToString())
        time.sleep(5)

        yield lbdumper


@pytest.mark.parametrize("supplier_id", [SUPPLIER_ID_FROM_TABLE, SUPPLIER_ID_FROM_LB])
def test_new_mdm_item(workflow, supplier_id):
    """ Проверяем создание новых записей о ВГХ
    """
    assert_that(
        workflow.yt_mdm_info_table_data,
        HasMdmInfoYtRows([
            {
                'supplier_id': supplier_id,
                'shop_sku': 'shop_sku_new',
                'mdm_info': IsSerializedProtobuf(
                    MdmItemInformationProto,
                    MdmItemInformationList([
                        MdmItemInformation(
                            weight=Weight(2700000, 1000),
                            width=Dimension(27000, 2000),
                            height=Dimension(23000, 3000),
                            length=Dimension(21100, 4000),
                            version=10,
                        )
                    ]).as_json()
                ),
                'updated_ts': 4000,
            }
        ])
    )


@pytest.mark.parametrize("supplier_id", [SUPPLIER_ID])
def test_mdm_item_with_existed_with_greater_version(workflow, supplier_id):
    """ Проверяем, что запись с большей версией не изменяется
    """
    assert_that(
        workflow.yt_mdm_info_table_data,
        HasMdmInfoYtRows([
            {
                'supplier_id': supplier_id,
                'shop_sku': 'shop_sku_existed_with_greater_version',
                'updated_ts': 10000,
                'write_ts': 10000,
                'mdm_info': IsSerializedProtobuf(
                    MdmItemInformationProto,
                    MdmItemInformationList([
                        MdmItemInformation(
                            weight=Weight(1000000, 10000),
                            length=Dimension(20000, 10000),
                            width=Dimension(30000, 10000),
                            height=Dimension(40000, 10000),
                            version=1000,
                        ),
                    ]).as_json()
                ),
            }
        ])
    )


@pytest.mark.parametrize("supplier_id", [SUPPLIER_ID])
def test_mdm_item_with_existed_without_version(workflow, supplier_id):
    """ Проверяем, что неверсионированные изменения не применяются
    """
    assert_that(
        workflow.yt_mdm_info_table_data,
        HasMdmInfoYtRows([
            {
                'supplier_id': supplier_id,
                'shop_sku': 'shop_sku_existed_without_version',
                'updated_ts': 10000,
                'write_ts': 10000,
                'mdm_info': IsSerializedProtobuf(
                    MdmItemInformationProto,
                    MdmItemInformationList([
                        MdmItemInformation(
                            weight=Weight(1000000, 10000),
                            length=Dimension(20000, 10000),
                            width=Dimension(30000, 10000),
                            height=Dimension(40000, 10000),
                        ),
                    ]).as_json()
                ),
            }
        ])
    )


@pytest.mark.parametrize("supplier_id", [SUPPLIER_ID])
def test_mdm_item_with_existed_with_lower_version(workflow, supplier_id):
    """ Проверяем, что записи с меньшими версиями обновляются
    """
    assert_that(
        workflow.yt_mdm_info_table_data,
        HasMdmInfoYtRows([
            {
                'supplier_id': supplier_id,
                'shop_sku': 'shop_sku_existed_with_lower_version',
                'updated_ts': 4000,
                'mdm_info': IsSerializedProtobuf(
                    MdmItemInformationProto,
                    MdmItemInformationList([
                        MdmItemInformation(
                            weight=Weight(2700000, 1000),
                            width=Dimension(27000, 2000),
                            height=Dimension(23000, 3000),
                            length=Dimension(21100, 4000),
                            version=10,
                        ),
                    ]).as_json()
                ),
            }
        ])
    )
