# coding: utf-8

import pytest
from hamcrest import assert_that
from yatest.common.network import PortManager

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1
WHITE_SHOP_ID = 111
WHITE_FEED_ID = 222
BLUE_SHOP_ID = 333
BLUE_FEED_ID = 444
DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': offer_id,
                    'extra': {
                        'classifier_good_id': classifier_good_id,
                    } if classifier_good_id else None
                },
                'content': {
                    'binding': {
                        'approved': {
                            'market_sku_id': 552690647,
                        }
                    },
                    'partner': {
                        'original': {
                            'name': {
                                'value': 'Monitor 1',
                            },
                            'category': {
                                'id': 1,
                            }
                        }
                    }
                }
            },
            'service': {
                BLUE_SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'shop_id': BLUE_SHOP_ID,
                        'offer_id': offer_id,
                        'feed_id': BLUE_FEED_ID,
                        'warehouse_id': 172,
                    },
                    'meta': {
                        'rgb': DTC.BLUE
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True,
                        }
                    },
                },
                WHITE_SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'shop_id': WHITE_SHOP_ID,
                        'feed_id': WHITE_FEED_ID,
                        'warehouse_id': 0,
                    },
                    'meta': {
                        'rgb': DTC.WHITE
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'url': {
                                    'value': 'http://www.1.ru/?ID=1',
                                },
                            }
                        }
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True,
                        }
                    },
                },
            }
        } for offer_id, classifier_good_id in (
            ('2889548', None),
            ('2889549', '8b5e9cedcbb2a6f471edc8f10680d21b'),
            ('2889550', 'dacdc45d3e4bbd3ee67ea9026ceb7cbb')
        )]
    }]
}]


@pytest.yield_fixture(scope='module')
def uc_server():
    with PortManager() as pm:
        port = pm.get_port()
        server = UcHTTPData.from_dict([], port=port)
        yield server


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, uc_server):
    cfg = MinerConfig()

    cfg.create_miner_initializer()
    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    uc_enricher = cfg.create_uc_enricher_processor(uc_server)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, uc_enricher)
    cfg.create_link(uc_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic):

    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic):
    for datacamp_message in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(datacamp_message, DatacampMessage()).SerializeToString())
    yield output_topic.read(count=len(DATACAMP_MESSAGES))


def test_classifier_good_id_init(workflow):
    """ Первичная установка classifier_good_id
        Синий и белый процессоры не мешают друг другу уставнавливать classifier_good_id в сервисные части - будут разные значения.
        В базовой части будет одно значение рассчитаное по белому алгоритму
    """
    offer_id = '2889548'
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'extra': {
                            'classifier_good_id': 'dacdc45d3e4bbd3ee67ea9026ceb7cbb'
                        }
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': BLUE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                })
            }]
        }]
    }]))


def test_classifier_good_id_update(workflow):
    """ Установка classifier_good_id, если у оффера он уже стоит
        Синий и белый процессоры не мешают друг другу
        В базовой части будет значение рассчитаное по белому алгоритму
    """
    # для синего не меняется (потому что нет полей которые бы зацепили его изменение), для белого и базового меняется
    offer_id = '2889549'
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'extra': {
                            'classifier_good_id': 'dacdc45d3e4bbd3ee67ea9026ceb7cbb',
                        }
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': BLUE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                })
            }]
        }]
    }]))

    # для синего меняется, для белого и базового не меняется (потому что нет полей которые бы зацепили его изменение)
    offer_id = '2889550'
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'basic': {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'extra': {
                            'classifier_good_id': 'dacdc45d3e4bbd3ee67ea9026ceb7cbb',
                        }
                    },
                },
                'service': IsProtobufMap({
                    WHITE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': WHITE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                    BLUE_SHOP_ID: {
                        'identifiers': {
                            'business_id': BUSINESS_ID,
                            'offer_id': offer_id,
                            'shop_id': BLUE_SHOP_ID,
                            'extra': {
                                'classifier_good_id': None,
                            }
                        },
                    },
                })
            }]
        }]
    }]))
