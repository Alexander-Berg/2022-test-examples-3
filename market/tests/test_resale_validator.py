# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.utils import dict2tskv

from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffer, UnitedOffersBatch
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

# offer_id, shop_id, is_resale, disabled_flag_to_check
TEST_OFFERS =[
    ("o1", 1, True,  False),
    ("o2", 1, False, False),
    ("o3", 2, True,  True),
    ("o4", 2, False, False),
]

DATACAMP_MESSAGES = [
    DatacampMessage(
        united_offers=[UnitedOffersBatch(
            offer=[UnitedOffer(
                basic=DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id=offer_id,
                    ),
                    content=DTC.OfferContent(
                        partner=DTC.PartnerContent(
                            actual=DTC.ProcessedSpecification(
                                is_resale=DTC.Flag(
                                    flag=is_resale
                                )
                            )
                        )
                    )
                ),
                service={
                    shop_id: DTC.Offer(
                        identifiers=DTC.OfferIdentifiers(
                            business_id=1,
                            offer_id=offer_id,
                            shop_id=shop_id,
                            warehouse_id=1,
                        )
                    )
                }
            ) for offer_id, shop_id, is_resale, _ in TEST_OFFERS]
        )]
    )
]


@pytest.fixture(scope='module')
def partner_data():
    return [
        {
            'shop_id': shop_id,
            'mbi': dict2tskv({
                'shop_id': shop_id,
                'business_id': 1,
                'is_enabled': True,
                'cut_price':  cut_price
            }),
            'status': 'publish'
        } for shop_id, cut_price in [
            (1, 'REAL'),
            (2, 'NO')
        ]
    ]


@pytest.fixture(scope='module')
def miner_config(yt_server, log_broker_stuff, input_topic, output_topic, yt_token, partner_info_table_path):
    cfg = MinerConfig()
    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partner_info_table_path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    resale_validator = cfg.create_resale_validator_processor()

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, resale_validator)
    cfg.create_link(resale_validator, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(yt_server, miner_config, input_topic, output_topic, partner_info_table_path, partner_data):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=partner_info_table_path,
            data=partner_data
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(input_topic, output_topic):
    for m in DATACAMP_MESSAGES:
        input_topic.write(m.SerializeToString())

    return output_topic.read(len(DATACAMP_MESSAGES))


def test_remove_active_promos_not_included_in_all_promos(miner, input_topic, output_topic):
    data = write_read_offer_lbk(input_topic, output_topic)
    assert_that(data[0], IsSerializedProtobuf(DatacampMessage, {
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    shop_id: {
                        'identifiers': {
                            'shop_id': shop_id,
                            'offer_id': offer_id,
                        },
                        'status': {
                            'disabled': [{
                                'flag': disabled_flag_to_check
                            }]
                        }
                    },
                })
            } for offer_id, shop_id, _, disabled_flag_to_check in TEST_OFFERS]
        }]
    }))
