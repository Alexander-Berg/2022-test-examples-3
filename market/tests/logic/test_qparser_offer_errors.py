# coding: utf-8

import pytest

from hamcrest import assert_that

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds, DEFAULT_MARKET_SKU
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
from market.idx.datacamp.proto.offer.TechCommands_pb2 import COMPLETE_FEED_FINISHED
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap


BUSINESS_ID = 10
SHOP_ID = 111
FEED_ID = 100


@pytest.fixture(scope='module')
def input_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture(scope='module')
def datacamp_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic
        }
    }

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


@pytest.fixture()
def mds(tmpdir_factory, config):
    return FakeMds(tmpdir_factory.mktemp('mds'), config)


@pytest.fixture(
    params=[
        'xml',
        'csv'
    ],
    ids=[
        'xml',
        'csv'
    ]
)
def feed_format(request):
    return request.param


@pytest.fixture(
    params=[
        True,
        False
    ],
    ids=[
        'blue',
        'white'
    ]
)
def is_blue(request):
    return request.param


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff, datacamp_output_topic, is_blue, feed_format):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        qparser_config={
            'logbroker': {
                'enable_verdicts': True,
                'complete_feed_finish_command_batch_size': 1000,
                'datacamp_messages_topic': datacamp_output_topic.topic,
                'datacamp_messages_writers_count': 1,
            }
        },
        color='blue' if is_blue else 'white',
        feed_format=feed_format,
    )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def resolution(errors):
    return {
        'by_source': [
            {
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                },
                'verdict': [
                    {
                        'results': [
                            {
                                'messages': [{'code': error} for error in errors]
                            }
                        ]
                    }
                ]
            }
        ]
    }


def datacamp_offer_msg(offer_id, errors):
    result = {
        'offers': [{
            'offer': [{
                'identifiers': {'offer_id': offer_id}
            }]
        }]
    }
    if errors is not None:
        result['offers'][0]['offer'][0]['resolution'] = resolution(errors)
    return result


def datacamp_united_offer_msg(offer_id):
    service_part = {
        'identifiers': {
            'offer_id': offer_id,
        },
    }

    if offer_id != '600':
        service_part.update({
            'price': {
                'basic': {
                    'binary_price': {
                        'price': int(offer_id) * 10000000,
                    }
                }
            },
        })
    else:
        service_part.update({
            'price': {
                'basic': {
                    'binary_price': None
                }
            },
            'status': {
                'disabled': [
                    {
                        'flag': True,
                        'meta': {
                            'source': DataCampOffer_pb2.MARKET_IDX,
                        },
                    },
                ],
            }
        })

    result = {
        'united_offers': [
            {
                'offer': [
                    {
                        'basic': {
                            'identifiers': {
                                'business_id': BUSINESS_ID,
                                'offer_id': offer_id,
                            }
                        },
                        'service': IsProtobufMap({
                            SHOP_ID: service_part
                        })
                    }
                ]
            }
        ]
    }

    return result


def create_offer(**kwargs):
    result = dict(kwargs)
    result.update({
        'categoryId': '1',
        'name': 'offer {}'.format(result['id']),
        'shop-sku': result['id'],
        'market_sku': DEFAULT_MARKET_SKU,
    })
    return result


def _create_offers_for_test():
    return [
        create_offer(id='100', price='100', barcode=('1' * 25)),  # баркод больше 20 символов
        create_offer(id='200', price='200', dimensions='10,20'),  # некорректные размеры, должно быть 3 значения
        create_offer(id='300', price='300', url='ddgdfgdgf', disabled='abc'),  # несколько некорректных значений
        create_offer(id='400', price='400', downloadable='abc'),
        create_offer(id='500', price='500', age={'value': '12', 'unit': 'abcde'}),  # не корректный unit в возрасте
        create_offer(id='600', price='0'),  # плохая цена

        create_offer(id='900', price='900'),  # единственный нормальный оффер
    ]


# TODO MARKETINDEXER-35056
@pytest.mark.skip(reason="before moving resolution to united offer")
def test_qparser(push_parser, input_topic, output_topic, mds, is_blue, feed_format):
    is_csv = feed_format == 'csv'
    offers = _create_offers_for_test()
    mds.generate_feed(FEED_ID, force_offers=offers, is_blue=is_blue, categories_count=5, is_csv=is_csv)
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=len(offers))
    assert_that(data, HasSerializedDatacampMessages([
        datacamp_offer_msg(offer_id='100', errors={'355'}),
        datacamp_offer_msg(offer_id='200', errors={'358'}),
        datacamp_offer_msg(offer_id='300', errors={'35S', '358'}),
        datacamp_offer_msg(offer_id='400', errors={'45D'}),
        datacamp_offer_msg(offer_id='500', errors={'35B'}),
        datacamp_offer_msg(offer_id='600', errors={'453'}),
        datacamp_offer_msg(offer_id='900', errors=None),
    ]))


def test_qparser_test_broken_offers_not_disabled_by_complete_feed(push_parser, input_topic, output_topic, mds, datacamp_output_topic, is_blue, feed_format):
    """Проверяем, что в комплит команде не отключаются оффера с ошибками"""
    is_csv = feed_format == 'csv'
    categories_count = 1  # dont change https://st.yandex-team.ru/MARKETINDEXER-36993
    offers = _create_offers_for_test()
    mds.generate_feed(FEED_ID, force_offers=offers, is_blue=is_blue, categories_count=categories_count, is_csv=is_csv)
    input_topic.write(
        make_input_task(
            mds,
            FEED_ID,
            BUSINESS_ID,
            SHOP_ID,
            task_type=FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE
        ).SerializeToString()
    )

    push_parser.run(total_sessions=1)

    data = output_topic.read(count=len(offers))
    assert_that(data, HasSerializedDatacampMessages([
        datacamp_united_offer_msg(offer_id='100'),
        datacamp_united_offer_msg(offer_id='200'),
        datacamp_united_offer_msg(offer_id='300'),
        datacamp_united_offer_msg(offer_id='400'),
        datacamp_united_offer_msg(offer_id='500'),
        datacamp_united_offer_msg(offer_id='600'),
        datacamp_united_offer_msg(offer_id='900'),
    ]))

    datacamp_output_topic_messages = datacamp_output_topic.read(count=categories_count + 1)
    assert_that(datacamp_output_topic_messages[categories_count:], HasSerializedDatacampMessages([{
        "tech_command": [
            {
                "command_type": COMPLETE_FEED_FINISHED,
                "command_params": {
                    "feed_id": FEED_ID,
                    "shop_id": SHOP_ID,
                    "business_id": BUSINESS_ID,
                    "complete_feed_command_params": {
                        "start_offer_id": None,
                        "last_offer_id": None,
                        "untouchable_offers": [offer['id'] for offer in offers if offer['id'] is not None]
                    }
                }
            }
        ]
    }]))
