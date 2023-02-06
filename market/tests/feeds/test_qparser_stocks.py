# coding: utf-8

import pytest
import six

from hamcrest import assert_that, is_not

from market.idx.datacamp.parser.yatf.env import WorkersEnv, make_input_task, UpdateTaskServiceMock
from market.idx.datacamp.parser.yatf.fake_mds import FakeMds
from market.idx.datacamp.parser.yatf.qp_mocks import QParserTestLauncherMock
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.datacamp.proto.api.UpdateTask_pb2 import (
    ShopsDatParameters,
    FEED_CLASS_UPDATE,
    FEED_CLASS_COMPLETE,
    FEED_CLASS_STOCK,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_COMPLETE,
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages
from market.idx.yatf.matchers.lb_matchers import HasNoUnreadData
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap
from market.idx.yatf.resources.lbk_topic import LbkTopic

import yatest.common

BUSINESS_ID = 10
SHOP_ID = 111
WAREHOUSE_ID = 150
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


@pytest.fixture(scope='module')
def stocks_output_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    topic.create()
    return topic


@pytest.fixture()
def config(tmpdir_factory, log_broker_stuff, yt_server, output_topic, input_topic, stocks_output_topic, datacamp_output_topic):
    cfg = {
        'logbroker_in': {
            'topic': input_topic.topic,
        },
        'general': {
            'complete_feed_finish_command_batch_size': 1000,
        },
        'logbroker': {
            'topic': output_topic.topic,
            'datacamp_messages_topic': datacamp_output_topic.topic,
            'stocks_topic' : stocks_output_topic.topic,
            'enable_sending_stocks_to_separate_topic': True,
            'complete_feed_finish_command_batch_size': 1000
        },
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


@pytest.yield_fixture()
def qp_runner(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv'
    )


@pytest.yield_fixture()
def qp_runner_yml(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config
    )


@pytest.fixture()
def push_parser_yml(monkeypatch, config, qp_runner_yml):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_yml.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture()
def push_parser(monkeypatch, config, qp_runner):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


@pytest.fixture(scope='function', params=[
    FEED_CLASS_STOCK,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE
])
def feed_type(request):
    return request.param


def test_qparser_stocks(push_parser, input_topic, output_topic, mds, feed_type):
    """
    Проверяем, что количество стоков парсится из фида
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_advanced_blue=True,
        is_csv=True
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=True
        ),
        is_regular_parsing=False,
        task_type=feed_type
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': 1
                            }
                        }
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    assert_that(data, matcher)


def test_qparser_stocks_feed(push_parser, input_topic, output_topic, mds, feed_type):
    mds.setup_push_feed(
        FEED_ID,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_feed.csv')
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        task_type=feed_type,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=True
        ),
        is_regular_parsing=False,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)

    expected = {
        '10193': 5,
        '10194': 5,
        '10196': 5,
        '10198': 5,
        '10200': 3,
        '10201': 5,
        '10203': 5,
        '10205': 5,
        '10206': 5,
        '10207': 5,
        '10208': 5,
        '10211': 10,
        '10214': 5,
        '10215': 2,
        '10216': 5,
        '10217': 5,
        '10220': 2,
        '10221': 2,
        'KF-ЛОФТ-1': 5,
        'KF-ЛОФТ-1-ограничитель': 0,
        'KF-ЛОФТ-2': 5,
        'KF-ЛОФТ-2-ограничитель': 0,
        'KF-белый-ограничитель': 0,
    }

    data = output_topic.read(count=len(expected))

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': offer_id,
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': count
                            }
                        }
                    }
                })
            }]
        }]
    } for offer_id, count in list(expected.items())])
    assert_that(data, matcher)


def test_qparser_stocks_warehouses_group_feed(push_parser, input_topic, output_topic, mds):
    """
    Проверяем, что при парсинге стокового фида для группы складов стоки размножаются на все склады из группы
    в актуальную сервисную часть
    """
    partner_whs_group = [
        # feed_id, shop_id, warehouse_id, color
        (1111, 11, 111, DTC.BLUE),
        (2222, 22, 222, DTC.BLUE),
        (3333, 33, 333, DTC.WHITE)
    ]
    main_feed_id, main_shop_id, main_warehouse_id, main_color = partner_whs_group[1]

    mds.setup_push_feed(
        main_feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_warehouses_group_feed.csv')
    )

    feed_parsing_task = make_input_task(
        mds,
        main_feed_id,
        BUSINESS_ID,
        main_shop_id,
        warehouse_id=main_warehouse_id,
        task_type=FEED_CLASS_STOCK,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=True,
            color=main_color
        ),
        is_regular_parsing=False,
        partner_whs_group=partner_whs_group
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)

    expected = {
        'o1': 5,
        'o2': 6,
    }

    data = output_topic.read(count=len(expected))

    united_offers_batch = []
    for offer_id, stock_count in expected.items():
        basic = {
            'identifiers': {
                'business_id': BUSINESS_ID,
                'offer_id': offer_id,
            },
            'meta': {
                'scope': DTC.BASIC,
            }
        }
        services = {}
        actual_services = {}
        for feed_id, shop_id, warehouse_id, color in partner_whs_group:
            services[shop_id] = {
                'identifiers': {
                    'business_id': BUSINESS_ID,
                    'shop_id': shop_id,
                    'offer_id': offer_id,
                },
                'meta': {
                    'scope': DTC.SERVICE,
                    'rgb': color,
                    'platforms': IsProtobufMap({
                        color: True
                    })
                }
            }
            actual_services[shop_id] = {
                'warehouse': IsProtobufMap({
                    warehouse_id: {
                        'identifiers': {
                            'feed_id': feed_id,
                            'business_id': BUSINESS_ID,
                            'shop_id': shop_id,
                            'offer_id': offer_id,
                            'warehouse_id': warehouse_id,
                        },
                        'meta': {
                            'scope': DTC.SERVICE,
                            'rgb': color,
                            'platforms': IsProtobufMap({
                                color: True
                            })
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': stock_count,
                            }
                        }
                    }
                })
            }

        united_offer = {
            'offer': [{
                'basic': basic,
                'service': IsProtobufMap(services),
                'actual': IsProtobufMap(actual_services)
            }]
        }
        united_offers_batch.append(united_offer)

    assert_that(data, HasSerializedDatacampMessages([{'united_offers': [uo]} for uo in united_offers_batch]))

    # проверяем, что в сервисной части стоков нет
    for offer_id in expected.keys():
        assert_that(data, is_not(HasSerializedDatacampMessages([{
            'united_offers': [{
                'offer': [{
                    'service': IsProtobufMap({
                        shop_id: {
                            'identifiers': {
                                'offer_id': offer_id,
                                'shop_id': shop_id,
                                'business_id': BUSINESS_ID
                            },
                            'stock_info': {
                                'partner_stocks': {
                                    'meta': {
                                        'source': DTC.PUSH_PARTNER_FEED
                                    },
                                }
                            }
                        }
                    })
                }]
            }]
        }] for feed_id, shop_id, _, __ in partner_whs_group)))


def test_qparser_stocks_warehouses_group_feed_with_single_whs(push_parser, input_topic, output_topic, mds):
    """
    Проверяем, что парсинг стокового фида для группы из единственного склада равносилен парсингу обычного стокового фида
    Стоки пишутся в сервисную часть
    """
    partner_whs_group = [
        # feed_id, shop_id, warehouse_id, color
        (4444, 44, 444, DTC.BLUE)
    ]
    main_feed_id, main_shop_id, main_warehouse_id, main_color = partner_whs_group[0]

    mds.setup_push_feed(
        main_feed_id,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_warehouses_group_feed.csv')
    )

    feed_parsing_task = make_input_task(
        mds,
        main_feed_id,
        BUSINESS_ID,
        main_shop_id,
        warehouse_id=main_warehouse_id,
        task_type=FEED_CLASS_STOCK,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=True,
            color=main_color
        ),
        is_regular_parsing=False,
        partner_whs_group=partner_whs_group
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)

    expected = {
        'o1': 5,
        'o2': 6,
    }

    data = output_topic.read(count=len(expected))

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    main_shop_id: {
                        'identifiers': {
                            'feed_id': main_feed_id,
                            'business_id': BUSINESS_ID,
                            'shop_id': main_shop_id,
                            'warehouse_id': main_warehouse_id,
                            'offer_id': offer_id,
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': count
                            }
                        }
                    }
                })
            }]
        }]
    } for offer_id, count in expected.items()])
    assert_that(data, matcher)


@pytest.yield_fixture()
def qp_runner_white(config, log_broker_stuff):
    yield QParserTestLauncherMock(
        log_broker_stuff=log_broker_stuff,
        config=config,
        feed_format='csv',
        color='white'
    )


@pytest.fixture()
def push_parser_white(monkeypatch, config, qp_runner_white):
    with monkeypatch.context() as m:
        m.setattr("market.idx.datacamp.parser.lib.worker.ParsingTaskWorker.process_task", qp_runner_white.process_task)

        yield WorkersEnv(
            config=config,
            parsing_service=UpdateTaskServiceMock
        )


def test_qparser_stocks_dsbs_feed(push_parser_white, input_topic, output_topic, mds):
    mds.setup_push_feed(
        FEED_ID,
        yatest.common.source_path('market/idx/datacamp/parser/tests/feeds/data/stock_dsbs_feed.csv')
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        task_type=FEED_CLASS_STOCK,
        shops_dat_parameters=ShopsDatParameters(
            vat=7,
            color=DTC.WHITE,
            is_mock=False,
            is_upload=True,
            local_region_tz_offset=10800,
        ),
        is_regular_parsing=False,
        is_dbs=True,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser_white.run(total_sessions=1)

    expected = {
        '1297': 334,
    }

    data = output_topic.read(count=len(expected))

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': offer_id,
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': count
                            }
                        }
                    }
                })
            }]
        }]
    } for offer_id, count in list(expected.items())])
    assert_that(data, matcher)


def test_qparser_stocks_from_not_upload(push_parser, input_topic, output_topic, stocks_output_topic,  mds, feed_type):
    """
    Проверяем, что количество стоков не парсится из фида при is_regular_parsing = true для csv фида (в нем не существует yml_date в принципе)
    плюс проверка стокового топика: для is_regular_parsing = true и при включенном флаге писания в отдельный стоковый топик выписываем стоки
    (partner_stock_info), а в основной топик стоки НЕ пишутся
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_advanced_blue=True,
        is_csv=True
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=False
        ),
        task_type=feed_type
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)
    stock_data = stocks_output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks': {}
                        }
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    stock_matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'actual': IsProtobufMap({
                    SHOP_ID: {
                        'warehouse': IsProtobufMap({
                            WAREHOUSE_ID: {
                                'identifiers': {
                                    'feed_id': FEED_ID,
                                    'business_id': BUSINESS_ID,
                                    'shop_id': SHOP_ID,
                                    'warehouse_id': WAREHOUSE_ID,
                                    'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                                },
                                'stock_info': {
                                    'partner_stocks': {
                                        'count': 0
                                    }
                                }
                            }
                        })
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    assert_that(data, matcher)
    assert_that(stock_data, stock_matcher)


def test_qparser_stocks_without_yml_date(push_parser_yml, input_topic, output_topic, mds, feed_type):
    """
    Проверяем, что количество стоков не парсится из фида при отсутствии yml_date и is_regular_parsing = true
    """

    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_csv=False,
        shop_dict={
            'name': six.ensure_text('Магазин  Audio-Video'),
            'company': 'Audio-Video',
            'url': 'http://www.aydio-video.ru',
            'local_delivery_cost': '100',
            'cpa': '1'
        }
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=False
        ),
        task_type=feed_type
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser_yml.run(total_sessions=1)
    data = output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': 0
                            }
                        }
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    assert_that(data, matcher)


def test_qparser_stocks_from_not_upload_without_whid(push_parser, input_topic, output_topic, stocks_output_topic, mds, feed_type):
    """
    Проверяем, что несмотря на то, что даже если флаг стоков включен и isUpload = False при отсутствии
    warehouse_id, стоки не будут писаться в отдельный топик
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_advanced_blue=True,
        is_csv=True
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=False
        ),
        task_type=feed_type
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': {
                            'partner_stocks': {
                                'count': 0
                            }
                        }
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    assert_that(stocks_output_topic, HasNoUnreadData())
    assert_that(data, matcher)


@pytest.mark.parametrize('task_type', [
    FEED_CLASS_UPDATE,
    FEED_CLASS_COMPLETE,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_FULL_COMPLETE,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_UPDATE,
    FEED_CLASS_SELECTIVE_BASIC_PATCH_UPDATE_SERVICE_PATCH_COMPLETE,
])
def test_skip_stocks_1(push_parser, input_topic, output_topic, stocks_output_topic, mds, task_type):
    """
    Проверяем, что при флаге skip_stocks стоки никуда не отправляются
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_advanced_blue=True,
        is_csv=True
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=False
        ),
        task_type=task_type,
        skip_stocks=True,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)
    data = output_topic.read(count=1)

    matcher = HasSerializedDatacampMessages([{
        'united_offers': [{
            'offer': [{
                'service': IsProtobufMap({
                    SHOP_ID: {
                        'identifiers': {
                            'feed_id': FEED_ID,
                            'business_id': BUSINESS_ID,
                            'shop_id': SHOP_ID,
                            'warehouse_id': WAREHOUSE_ID,
                            'offer_id': '{}xXx{}'.format(FEED_ID, offer_id),
                        },
                        'stock_info': None,
                    }
                })
            } for offer_id in range(1)]
        }]
    }])
    assert_that(stocks_output_topic, HasNoUnreadData())
    assert_that(data, matcher)


@pytest.mark.parametrize('task_type', [
    FEED_CLASS_STOCK,
])
def test_skip_stocks_2(push_parser, input_topic, output_topic, stocks_output_topic, mds, task_type):
    """
    Проверяем, что при флаге skip_stocks стоки никуда не отправляются
    """
    mds.generate_feed(
        FEED_ID,
        is_blue=True,
        offer_count=1,
        is_advanced_blue=True,
        is_csv=True
    )

    feed_parsing_task = make_input_task(
        mds,
        FEED_ID,
        BUSINESS_ID,
        SHOP_ID,
        warehouse_id=WAREHOUSE_ID,
        shops_dat_parameters=ShopsDatParameters(
            is_upload=False
        ),
        task_type=task_type,
        skip_stocks=True,
    )
    input_topic.write(feed_parsing_task.SerializeToString())
    push_parser.run(total_sessions=1)
    assert_that(stocks_output_topic, HasNoUnreadData())
    assert_that(output_topic, HasNoUnreadData())
