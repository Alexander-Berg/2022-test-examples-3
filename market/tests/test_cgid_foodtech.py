# coding: utf-8

import pytest
from hamcrest import assert_that

from yatest.common.network import PortManager

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffersBatch, UnitedOffer
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper import ypath_join


SHOP1_ID = 11
SHOP2_ID = 12
OFFERS = [
    dict(
        name='lavka offer',
        identifiers=dict(
            business_id=1,
            shop_id=SHOP1_ID,
            offer_id='lavka',
        ),
        meta=dict(rgb=DTC.LAVKA)
    ),
    dict(
        name='eda offer',
        identifiers=dict(
            business_id=1,
            shop_id=SHOP2_ID,
            offer_id='eda',
        ),
        meta=dict(rgb=DTC.EDA),
    )
]

EXPECTED_CGID = ['525118c785047f803d84c307f62f0a52', 'ec637e68d6871a83ac39e18c57963537']
EXPECTED_CMID = ['bbeb24fae64d9d92044179d34c680e04', '083ffb9f8616a40442917e32417648ad']
EXPECTED_BASIC_CMID = ['d361f65118ee1f6203c8b8a0d7245378', '12c348d095232f689c6a98da6a2c4395']


@pytest.fixture(scope='module')
def partners_table(yt_server):
    rows = [
        {
            'shop_id': SHOP1_ID,
            'mbi':  dict2tskv({
                'business_id': 1,
                'shop_id': SHOP1_ID,
                'datafeed_id': 111,
                'is_lavka': 'true',
            }),
            'status': 'publish'
        },
        {
            'shop_id': SHOP2_ID,
            'mbi':  dict2tskv({
                'business_id': 1,
                'shop_id': SHOP2_ID,
                'datafeed_id': 222,
                'is_eats': 'true',
            }),
            'status': 'publish'
        }
    ]

    return DataCampPartnersTable(
        yt_stuff=yt_server,
        path=ypath_join(get_yt_prefix(), 'datacamp', 'partners'),
        data=rows
    )


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict([], port=port)
        yield server


@pytest.fixture(scope='module')
def miner_config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic, partners_table, uc_server):
    cfg = MinerConfig()

    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
        partners_table_path=partners_table.get_path(),
    )

    cfg.create_miner_initializer()
    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_uc_enricher_processor(uc_server=uc_server, allow_types='LAVKA;EDA')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, partners_table):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'partners_table': partners_table,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def write_read_offer_lbk(offer_data, input_topic, output_topic):
    message = DatacampMessage(united_offers=[UnitedOffersBatch(
        offer=[UnitedOffer(
            basic=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer_data['identifiers']['business_id'],
                    offer_id=offer_data['identifiers']['offer_id'],
                ),
                content=DTC.OfferContent(
                    partner=DTC.PartnerContent(
                        original=DTC.OriginalSpecification(
                            name=DTC.StringValue(
                                value=offer_data.get('name', '')
                            ),
                            offer_params=DTC.ProductYmlParams(
                                param=[DTC.OfferYmlParam(name=param['name'], value=param['value']) for param in offer_data.get('params', [])]
                            )
                        )
                    )
                )
            ),
            service={offer_data['identifiers']['shop_id']: DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=offer_data['identifiers']['business_id'],
                    shop_id=offer_data['identifiers']['shop_id'],
                    offer_id=offer_data['identifiers']['offer_id'],
                ),
                meta=DTC.OfferMeta(rgb=offer_data['meta']['rgb'])
            )}
        )]
    )])
    input_topic.write(message.SerializeToString())

    return output_topic.read(count=1)


def test_cgid_and_cmid(miner, input_topic, output_topic):
    data = [write_read_offer_lbk(offer, input_topic, output_topic)[0] for offer in OFFERS]

    shops = [SHOP1_ID, SHOP2_ID]
    for offer, shop_id, cgid, cmid, basic_cmid in zip(data, shops, EXPECTED_CGID, EXPECTED_CMID, EXPECTED_BASIC_CMID):
        assert_that(
            offer,
            IsSerializedProtobuf(DatacampMessage, {
                'united_offers': [{
                    'offer': [{
                        'basic': {
                            'identifiers': {
                                'extra': {
                                    'classifier_good_id': cgid,
                                    'classifier_magic_id2': basic_cmid,
                                }
                            },
                        },
                        'service': IsProtobufMap({
                            shop_id: {
                                'identifiers': {
                                    'extra': {
                                        'classifier_good_id': None,
                                        'classifier_magic_id2': None
                                    }
                                },
                            },
                        })
                    }]
                }]
            })
        )
