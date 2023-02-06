# coding: utf-8

import pytest
from hamcrest import assert_that, empty

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.msku_table import MskuExtTable
from market.pylibrary.proto_utils import message_from_data


BUSINESS_ID = 1
BLUE_SHOP_ID = 2
WHITE_SHOP_ID = 3
DATACAMP_MESSAGES = [{
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'offer_id': offer_id,
                },
                'content': {
                    'partner': {
                        'actual': {
                            'weight': {
                                'value_mg': 405700000,
                            }
                        },
                    },
                    'binding': {
                        'approved': {
                            'market_sku_id': msku_id
                        }
                    },
                    'master_data': {
                        'cargo_type': {
                            'value': [998, 999]
                        }
                    }
                }
            },
            'service': {
                BLUE_SHOP_ID: {
                    'identifiers': {
                        'business_id': BUSINESS_ID,
                        'offer_id': offer_id,
                        'shop_id': BLUE_SHOP_ID,
                        'warehouse_id': 0,
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
                        'warehouse_id': 0,
                    },
                    'meta': {
                        'rgb': DTC.WHITE
                    },
                    'status': {
                        'united_catalog': {
                            'flag': True,
                        }
                    },
                    'partner_info': {
                        'is_dsbs': True,
                    },
                },
            }
        } for offer_id, msku_id in (('withcargotype1', 10), ('withcargotype2', 20), ('nomskucargotype', 30))]
    }]
}]


@pytest.yield_fixture(scope="module")
def msku_ext_table_path():
    return ypath_join('datacamp', 'blue', 'in', 'msku_ext')


@pytest.fixture(scope='module')
def msku_ext_table_data():
    return [
        {
            'msku': 10,
            'cargo_types': [123, 456],
        }, {
            'msku': 20,
            'cargo_types': [987]
        }
    ]


@pytest.fixture(scope='module')
def miner_config(yt_server, log_broker_stuff, input_topic, output_topic, msku_ext_table_path, yt_token):
    cfg = MinerConfig()

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cargo_types_enricher = cfg.create_cargo_types_enricher(
        yt_server,
        yt_token.path,
        get_yt_prefix(),
        msku_ext_table_path,
        enabled_fast_cargo_types_pipeline=False
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, cargo_types_enricher)
    cfg.create_link(cargo_types_enricher, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, yt_server, msku_ext_table_data, msku_ext_table_path):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'msku_ext_table': MskuExtTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), msku_ext_table_path),
            data=msku_ext_table_data,
        ),
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


@pytest.yield_fixture(scope='module')
def workflow(miner, input_topic, output_topic):
    for datacamp_message in DATACAMP_MESSAGES:
        input_topic.write(message_from_data(datacamp_message, DatacampMessage()).SerializeToString())
    yield output_topic.read(count=len(DATACAMP_MESSAGES), wait_timeout=5)


def test_cargo_type_enricher(workflow):
    """ Проверяем, что обогащение карго типами по msku работает
        Старые карготипы удаляются
        Карготипами обогащаются только синие офферы,
        а белый DSBS оффер должен обогатиться майнером КГТ карготипа 300
    """
    assert_that(workflow, HasSerializedDatacampMessages([{
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                            },
                            'content': {
                                'master_data': {
                                    'cargo_type': {
                                        'value': cargo_type
                                    }
                                }
                            }
                        },
                        'service': IsProtobufMap({
                            BLUE_SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': BLUE_SHOP_ID,
                                },
                                'meta': {
                                    'rgb': DTC.BLUE
                                },
                                'delivery': {
                                    'delivery_info': {
                                        'cargo_type': cargo_type
                                    },
                                }
                            },
                            WHITE_SHOP_ID: {
                                'identifiers': {
                                    'business_id': BUSINESS_ID,
                                    'offer_id': offer_id,
                                    'shop_id': WHITE_SHOP_ID,
                                },
                                'meta': {
                                    'rgb': DTC.WHITE
                                },
                                'delivery': {
                                    'market': {
                                        'cargo_type': {
                                            'value': [300]
                                        }
                                    },
                                }
                            }
                        })
                    } for offer_id, cargo_type in (
                        ('withcargotype1', [123, 456]),
                        ('withcargotype2', [987]),
                        ('nomskucargotype', empty()),
                    )
                ]
            }
        ]
    }]))
