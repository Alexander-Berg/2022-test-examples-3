# coding: utf-8

import pytest

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import EDA
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.pylibrary.proto_utils import message_from_data
from google.protobuf.json_format import MessageToDict


PIC_ID = 'ba1c9ba1c58547d7b973f9e84dea498f'
GROUP_ID = '2805921'
EDA_AVA_HOST = 'avatars.mds.yandex.net'
EDA_AVA_NAMESPACE = 'eda'
PIC_WIDTH_1 = 1600
PIC_WIDTH_2 = 1500
PIC_HEIGHT_1 = 1200
PIC_HEIGHT_2 = 1125
EDA_AVA_SIZE_1 = '{}x{}'.format(PIC_WIDTH_1, PIC_HEIGHT_1)
EDA_AVA_SIZE_2 = '{}x{}'.format(PIC_WIDTH_2, PIC_HEIGHT_2)

EDA_PIC = 'https://testing.eda.tst.yandex.net/get-eda/{}/{}/orig'.format(GROUP_ID, PIC_ID)


def make_eda_pic(size='orig'):
    return '//{host}/get-{ns}/{group}/{img}/{size}'.format(host=EDA_AVA_HOST, ns=EDA_AVA_NAMESPACE, group=GROUP_ID, img=PIC_ID, size=size)


DATACAMP_MESSAGE = {
    'united_offers': [{
        'offer': [{
            'basic': {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': 'o1',
                },
                'pictures': {
                    'partner': {
                        'original': {
                            'source': [
                                {'url': EDA_PIC}
                            ]
                        },
                    }
                }
            },
            'service': {
                1: {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': 'o1',
                        'shop_id': 1,
                        'warehouse_id': 0,
                    },
                    'meta': {
                        'rgb': EDA
                    },
                    'partner_info': {
                        'is_eda': True
                    }
                },
            }
        }]
    }]
}


@pytest.fixture(scope='module')
def yt_token():
    return YtTokenStub()


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    return LbkTopic(log_broker_stuff)


@pytest.fixture(scope='module')
def miner_config(yt_server, yt_token, log_broker_stuff, input_topic, output_topic):
    cfg = MinerConfig()
    cfg.create_miner_initializer(
        yt_server=yt_server,
        token=yt_token.path,
    )

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, True)
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    enricher = cfg.create_pictures_enricher(
        eda_ava_host=EDA_AVA_HOST,
        eda_ava_namespace=EDA_AVA_NAMESPACE,
        eda_pic_sizes=','.join([EDA_AVA_SIZE_1, EDA_AVA_SIZE_2])
    )

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, enricher)
    cfg.create_link(enricher, writer)

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
def release_message(miner, input_topic, output_topic):
    input_topic.write(message_from_data(DATACAMP_MESSAGE, DatacampMessage()).SerializeToString())

    data = output_topic.read(count=1)
    message = DatacampMessage()
    message.ParseFromString(data[0])

    return MessageToDict(message)


def test_pictures(release_message):
    '''
    Тест проверят, что для оффера Еды сформировалась мета картинки
    '''
    actual = release_message['unitedOffers'][0]['offer'][0]['basic']['pictures']
    expected = {
        'partner': {
            'original': {
                'source': [
                    {
                        'url': EDA_PIC
                    }
                ]
            },
            'actual': {
                EDA_PIC: {
                    'status': 'AVAILABLE',
                    'namespace': EDA_AVA_NAMESPACE,
                    'id': PIC_ID,
                    'mdsHost': EDA_AVA_HOST,
                    'groupId': GROUP_ID,
                    'thumbnails': [
                        {
                            'width': PIC_WIDTH_1,
                            'height': PIC_HEIGHT_1,
                            'containerHeight': PIC_HEIGHT_1,
                            'containerWidth': PIC_WIDTH_1,
                        },
                        {
                            'width': PIC_WIDTH_2,
                            'height': PIC_HEIGHT_2,
                            'containerHeight': PIC_HEIGHT_2,
                            'containerWidth': PIC_WIDTH_2,
                        }
                    ],
                    'original': {
                        'url': make_eda_pic()
                    }
                }
            }
        }
    }

    assert actual == expected
