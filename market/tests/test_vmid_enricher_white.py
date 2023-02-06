# coding: utf-8

import pytest
from hamcrest import assert_that, is_not

from yatest.common.network import PortManager

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv

from market.idx.yatf.resources.lbk_topic import LbkTopic

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferIdentifiers,
    Offer as DatacampOffer
)
from market.idx.datacamp.proto.offer.OfferContent_pb2 import OfferContent, MarketContent, PartnerContent, ProcessedSpecification
from market.idx.datacamp.proto.offer.OfferMeta_pb2 import OfferMeta, MarketColor, Flag
from market.idx.datacamp.proto.offer.OfferMapping_pb2 import ContentBinding, Mapping
from market.idx.datacamp.proto.offer.OfferStatus_pb2 import OfferStatus
from market.idx.datacamp.proto.offer.PartnerInfo_pb2 import PartnerInfo
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage

from market.proto.ir.UltraController_pb2 import EnrichedOffer

from market.idx.feeds.feedparser.yatf.resources.vmid_server import VMIdHTTPServer, VIRTUAL_MODEL_ID


WHITELIST_CATEGORY = 7286125


def make_offer(offer_id, feed_id, model_id=None, unpublished_model_id=0, msku=None, cluster_id=None,
               category_id=WHITELIST_CATEGORY, is_blue=False, is_resale=False, is_dsbs=False, cpa=1):
    return {
        'offer_id': offer_id,
        'feed_id': feed_id,
        'model_id': model_id,
        'unpublished_model_id': unpublished_model_id,
        'market_sku_id': msku,
        'cluster_id': cluster_id,
        'category_id': category_id,
        'is_blue': is_blue,
        'is_resale': is_resale,
        'is_dsbs': is_dsbs,
        'cpa': cpa,
    }


def create_united_offer(offer):
    united_offer = UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                offer_id=offer['offer_id']
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=EnrichedOffer(
                        category_id=offer['category_id'],
                        matched_id=offer['model_id'],
                        model_id=offer['unpublished_model_id'],
                        market_sku_id=offer['market_sku_id'] if not offer['is_blue'] else None,
                        cluster_id=offer['cluster_id'],
                    )
                ),
                binding=ContentBinding(
                    approved=Mapping(
                        market_sku_id=offer['market_sku_id'] if offer['is_blue'] else None,
                    ),
                ),
                partner=PartnerContent(
                    actual=ProcessedSpecification(
                        is_resale=Flag(
                            flag=offer['is_resale']
                        )
                    )
                ),
            ),
        ),
        service={
            1: DatacampOffer(
                identifiers=OfferIdentifiers(
                    offer_id=offer['offer_id'],
                    feed_id=offer['feed_id'],
                    shop_id=1,
                ),
                meta=OfferMeta(
                    rgb=MarketColor.BLUE if offer['is_blue'] else MarketColor.WHITE,
                ),
                partner_info=PartnerInfo(
                    is_dsbs=offer['is_dsbs']
                ),
                status=OfferStatus(
                    actual_cpa=Flag(flag=True if offer['cpa'] == 4 else False),
                ),
            )
        }
    )

    return united_offer


@pytest.fixture(scope="module")
def offers_batches():
    return [
        [
            make_offer('OfferWithoutModelIDAndMsku0', 100),
            make_offer('OfferWithoutModelIDAndMsku1', 200),
            make_offer('OfferWithZeroModelID', 700, model_id=0),
            make_offer('OfferWithZeroModelIdAndMSKU', 800, model_id=0, msku=-1),
            make_offer('OfferWithZeroModelIdAndMSKUAndCluster', 900, model_id=0, msku=-1, cluster_id=-1),
            make_offer('OfferWithZeroUnpublishedModelID', 1000, unpublished_model_id=-1),
            make_offer('OfferWithUnpublishedModelID', 550, unpublished_model_id=322),
            make_offer('OfferWithClusterID', 500, cluster_id=222),  # больше не смотрим на кластер
        ],
        [
            make_offer('OfferWithoutModelIDAndMsku2', 300),
            make_offer('OfferWithoutMappingButBadCategory', 350, category_id=111),
            make_offer('OfferWithModelID', 300, model_id=111),
            make_offer('OfferWithMSKU', 400, msku=555),
            make_offer('OfferWithMskuBLUE', 600, msku=333, is_blue=True),
            make_offer('OfferWithoutMskuBLUE', 650, is_blue=True),
            make_offer('OfferWhiteCpaWithoutMSKUwithUnpublishedModel', 1060, unpublished_model_id=322, msku=-1, cpa=4),

            make_offer('OfferWhiteCpaWithoutMapping', 999, cpa=4),
            make_offer('OfferWhiteCpaWithoutMSKUwithModel', 1000, model_id=1234, msku=-1, cpa=4),
            make_offer('OfferWhiteCpaWithoutMSKUwithCluster', 1070, cluster_id=1234, cpa=4),
            make_offer('OfferWhiteCpaWithMSKU', 1050, msku=1233, cpa=4),
            make_offer('OfferWhiteCpcWithoutMSKU', 1150, cluster_id=1234, msku=-1, cpa=1),

            make_offer('OfferResale', 1500, is_resale=True),
            make_offer('OfferResaleDsbs', 1550, is_resale=True, is_dsbs=True),
        ]
    ]


@pytest.yield_fixture(scope='module')
def vmid_server():
    with PortManager() as pm:
        server = VMIdHTTPServer(port=pm.get_port())
        yield server


@pytest.yield_fixture(scope='module')
def vmid_server_wo_exp():
    with PortManager() as pm:
        server = VMIdHTTPServer(port=pm.get_port())
        yield server


@pytest.yield_fixture(scope='module')
def vmid_server_only_white_cpa():
    with PortManager() as pm:
        server = VMIdHTTPServer(port=pm.get_port())
        yield server


@pytest.fixture(scope='module')
def input_topic_wo_exp(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic_wo_exp(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def input_topic_only_white_cpa(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic_only_white_cpa(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, vmid_server):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    vmid_enricher = cfg.create_vmid_enricher_processor(vmid_server, color='white')

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, vmid_enricher)
    cfg.create_link(vmid_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, vmid_server):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'vmid_server': vmid_server,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic, offers_batches):

    for batch in offers_batches:
        request = UnitedOffersBatch()
        request.offer.extend([create_united_offer(offer) for offer in batch])

        input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    yield output_topic.read(count=len(offers_batches))


@pytest.fixture(scope='module')
def miner_config_wo_exp(log_broker_stuff, input_topic_wo_exp, output_topic_wo_exp, vmid_server_wo_exp):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic_wo_exp, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic_wo_exp)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    vmid_enricher = cfg.create_vmid_enricher_processor(vmid_server_wo_exp, color='white', enable_category_exp=False, check_resale_offers=False, disable_resale_dsbs_offers=False)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, vmid_enricher)
    cfg.create_link(vmid_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner_wo_exp(miner_config_wo_exp, input_topic_wo_exp, output_topic_wo_exp, vmid_server_wo_exp):
    resources = {
        'miner_cfg': miner_config_wo_exp,
        'input_topic': input_topic_wo_exp,
        'output_topic': output_topic_wo_exp,
        'vmid_server': vmid_server_wo_exp,
    }
    with MinerTestEnv(**resources) as miner_wo_exp:
        miner_wo_exp.verify()
        yield miner_wo_exp


@pytest.yield_fixture(scope='module')
def workflow_wo_exp(miner_wo_exp, input_topic_wo_exp, output_topic_wo_exp, offers_batches):
    for batch in offers_batches:
        request = UnitedOffersBatch()
        request.offer.extend([create_united_offer(offer) for offer in batch])

        input_topic_wo_exp.write(DatacampMessage(united_offers=[request]).SerializeToString())

    yield output_topic_wo_exp.read(count=len(offers_batches))


@pytest.fixture(scope='module')
def miner_config_only_white_cpa(log_broker_stuff, input_topic_only_white_cpa, output_topic_only_white_cpa, vmid_server_only_white_cpa):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic_only_white_cpa, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic_only_white_cpa)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    vmid_enricher = cfg.create_vmid_enricher_processor(vmid_server_only_white_cpa, color='white', enable_only_white_cpa=True)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, vmid_enricher)
    cfg.create_link(vmid_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner_only_white_cpa(miner_config_only_white_cpa, input_topic_only_white_cpa, output_topic_only_white_cpa, vmid_server_only_white_cpa):
    resources = {
        'miner_cfg': miner_config_only_white_cpa,
        'input_topic': input_topic_only_white_cpa,
        'output_topic': output_topic_only_white_cpa,
        'vmid_server': vmid_server_only_white_cpa,
    }
    with MinerTestEnv(**resources) as miner_only_white_cpa:
        miner_only_white_cpa.verify()
        yield miner_only_white_cpa


@pytest.yield_fixture(scope='module')
def workflow_only_white_cpa(miner_only_white_cpa, input_topic_only_white_cpa, output_topic_only_white_cpa, offers_batches):
    for batch in offers_batches:
        request = UnitedOffersBatch()
        request.offer.extend([create_united_offer(offer) for offer in batch])

        input_topic_only_white_cpa.write(DatacampMessage(united_offers=[request]).SerializeToString())

    yield output_topic_only_white_cpa.read(count=len(offers_batches))


def assert_vmid(now_workflow, offer_id, feed_id, need_vmid=False, need_disable=False):
    if (need_vmid):
        assert_that(now_workflow, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'offer_id': offer_id,
                        },
                    },
                    'service': IsProtobufMap({
                        1: {
                            'identifiers': {
                                'offer_id': offer_id,
                                'feed_id': feed_id,
                                'shop_id': 1,
                            },
                            "content": {
                                "binding": {
                                    "vmid_mapping": {
                                        # Тестовый сервер всем раздает одинаковый vmid
                                        # TODO поменять это!
                                        'virtual_model_id': VIRTUAL_MODEL_ID,
                                    }
                                },
                            },
                        }
                    })
                }]
            }]
        }]))
    else:
        # сам оффер должен быть
        assert_that(now_workflow, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'offer_id': offer_id,
                        },
                    },
                    'service': IsProtobufMap({
                        1: {
                            'identifiers': {
                                'offer_id': offer_id,
                                'feed_id': feed_id,
                                'shop_id': 1,
                            },
                        }
                    })
                }]
            }]
        }]))
        # но маппинга на vmid быть не должно
        assert_that(now_workflow, is_not(HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'basic': {
                        'identifiers': {
                            'offer_id': offer_id,
                        },
                    },
                    'service': IsProtobufMap({
                        1: {
                            'identifiers': {
                                'offer_id': offer_id,
                                'feed_id': feed_id,
                                'shop_id': 1,
                            },
                            "content": {
                                "binding": {
                                    "vmid_mapping": {
                                        'virtual_model_id': VIRTUAL_MODEL_ID,
                                    }
                                },
                            },
                        }
                    })
                }]
            }]
        }])))

    if (need_disable):
        assert_that(now_workflow, HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'service': IsProtobufMap({
                        1: {
                            "status": {
                                "disabled": [{
                                    "flag": True
                                }]
                            },
                        }
                    })
                }]
            }]
        }]))


def test_vmid_enrichment(workflow, offers_batches):
    """ 1) Проверяем, что у офферов без модели, msku проставляется virtual_model_id
           Если есть хотя бы что-нибудь из перечисленного, то он не проставляется
        2) Еще мы обогащаем cpa оффера с кластером
    """

    for offer in offers_batches[0]:
        assert_vmid(workflow, offer['offer_id'], offer['feed_id'], need_vmid=True)

    assert_vmid(workflow, 'OfferWithoutModelIDAndMsku2', 300, need_vmid=True)
    assert_vmid(workflow, 'OfferWithModelID', 300)
    assert_vmid(workflow, 'OfferWithoutMappingButBadCategory', 350)
    assert_vmid(workflow, 'OfferWithMSKU', 400)
    assert_vmid(workflow, 'OfferWithMskuBLUE', 600)
    # Синие вообще не обогащаем
    assert_vmid(workflow, 'OfferWithoutMskuBLUE', 650)

    # Сpa оффера
    assert_vmid(workflow, 'OfferWhiteCpaWithoutMapping', 999, need_vmid=True)
    assert_vmid(workflow, 'OfferWhiteCpaWithoutMSKUwithCluster', 1070, need_vmid=True)  # на кластер больше не смотрим
    assert_vmid(workflow, 'OfferWhiteCpaWithoutMSKUwithModel', 1000)
    assert_vmid(workflow, 'OfferWhiteCpaWithMSKU', 1050)
    assert_vmid(workflow, 'OfferWhiteCpcWithoutMSKU', 1150, need_vmid=True)
    assert_vmid(workflow, 'OfferWhiteCpaWithoutMSKUwithUnpublishedModel', 1060, need_vmid=True)

    # Б/у не обогащаем
    assert_vmid(workflow, 'OfferResale', 1500)
    assert_vmid(workflow, 'OfferResaleDsbs', 1550, need_disable=True)


def test_vmid_enrichment_wo_category_exp(workflow_wo_exp, offers_batches):
    """ Проверяем, что без экспа нет фильтрации по категории """
    assert_vmid(workflow_wo_exp, 'OfferWithoutMappingButBadCategory', 350, need_vmid=True)

    # Остальное должно остаться без изменений
    for offer in offers_batches[0]:
        assert_vmid(workflow_wo_exp, offer['offer_id'], offer['feed_id'], need_vmid=True)

    assert_vmid(workflow_wo_exp, 'OfferWithoutModelIDAndMsku2', 300, need_vmid=True)
    assert_vmid(workflow_wo_exp, 'OfferWithModelID', 300)
    assert_vmid(workflow_wo_exp, 'OfferWithMSKU', 400)
    assert_vmid(workflow_wo_exp, 'OfferWithMskuBLUE', 600)
    # Синие вообще не обогащаем
    assert_vmid(workflow_wo_exp, 'OfferWithoutMskuBLUE', 650)

    # Сpa оффера
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpaWithoutMapping', 999, need_vmid=True)
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpaWithoutMSKUwithCluster', 1070, need_vmid=True)
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpaWithoutMSKUwithModel', 1000)
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpaWithMSKU', 1050)
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpcWithoutMSKU', 1150, need_vmid=True)
    assert_vmid(workflow_wo_exp, 'OfferWhiteCpaWithoutMSKUwithUnpublishedModel', 1060, need_vmid=True)

    # Б/у оффера обогащаются только при откл. флаге CheckResaleOffers
    assert_vmid(workflow_wo_exp, 'OfferResale', 1500, need_vmid=True)
    assert_vmid(workflow_wo_exp, 'OfferResaleDsbs', 1550, need_vmid=True)


def test_vmid_enrichment_only_white_cpa(workflow_only_white_cpa, offers_batches):
    """ Проверяем, что при включенном EnableOnlyWhiteCpa мы обогащаем только cpa оффера """

    # Тут все оффера - cpc, поэтому vmid-а нет
    for offer in offers_batches[0]:
        assert_vmid(workflow_only_white_cpa, offer['offer_id'], offer['feed_id'])

    assert_vmid(workflow_only_white_cpa, 'OfferWithoutModelIDAndMsku2', 300)
    assert_vmid(workflow_only_white_cpa, 'OfferWithModelID', 300)
    assert_vmid(workflow_only_white_cpa, 'OfferWithoutMappingButBadCategory', 350)
    assert_vmid(workflow_only_white_cpa, 'OfferWithMSKU', 400)
    assert_vmid(workflow_only_white_cpa, 'OfferWithMskuBLUE', 600)
    # Синие вообще не обогащаем
    assert_vmid(workflow_only_white_cpa, 'OfferWithoutMskuBLUE', 650)

    # С флагом EnableOnlyWhiteCpa обогащаем только белые cpa
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpaWithoutMapping', 999, need_vmid=True)
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpaWithoutMSKUwithCluster', 1070, need_vmid=True)
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpaWithoutMSKUwithModel', 1000)
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpaWithMSKU', 1050)
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpcWithoutMSKU', 1150)
    assert_vmid(workflow_only_white_cpa, 'OfferWhiteCpaWithoutMSKUwithUnpublishedModel', 1060, need_vmid=True)

    # Б/у не обогащаем
    assert_vmid(workflow_only_white_cpa, 'OfferResale', 1500)
    assert_vmid(workflow_only_white_cpa, 'OfferResaleDsbs', 1550)
