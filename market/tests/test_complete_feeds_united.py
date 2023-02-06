# coding: utf-8

import pytest
from hamcrest import assert_that, is_not, calling, raises
from datetime import datetime, timedelta

from market.idx.datacamp.controllers.piper.yatf.test_env_old import PiperTestEnv
from market.idx.datacamp.controllers.piper.yatf.resources.config_mock import PiperConfigMock
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.common.SchemaType_pb2 import PULL_TO_PUSH
from market.idx.datacamp.proto.offer import DataCampOffer_pb2
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DataCampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import OfferStatus, Flag, UpdateMeta, OfferStockInfo, OfferStocks
from market.idx.datacamp.proto.offer import TechCommands_pb2
from market.idx.datacamp.proto.offer.TechCommands_pb2 import TechCommand, TechCommandParams, CompleteFeedCommandParams
from market.idx.datacamp.proto.tables.Partner_pb2 import PartnerAdditionalInfo
from market.idx.pylibrary.datacamp.utils import wait_until
from market.idx.datacamp.yatf.utils import dict2tskv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampPartersYtRows, HasOffers
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable
from market.idx.yatf.resources.lbk_topic import LbkTopic
from market.idx.yatf.utils.utils import create_timestamp_from_json

from market.pylibrary.proto_utils import message_from_data


OLD_TIME_UTC = (datetime.utcnow() - timedelta(minutes=45))
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
COMPLETE_FEED_DT = datetime.utcnow()
COMPLETE_FEED_TIME = COMPLETE_FEED_DT.strftime(time_pattern)

OFFERS = [
    # проверяем, что оффер без флажков отключения будет отключен флажком с типом PUSH_PARTNER_FEED
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferWithoutDisableFlags',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # Удаленный оффер не будет модифицирован
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'RemovedOffer',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'removed': {
                'flag': True,
            },
        },
    },
    # проверяем, что оффер, включенный ранее через партнерское апи, будет отключен и тип будет PUSH_PARTNER_FEED
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferEnabledByPushApiBeforeComplete',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(minutes=30)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # проверяем, что оффер, включенный через партнерское апи позже комплит фида, не будет отключен
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferEnabledByPushApiAfterComplete',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                        'timestamp': (COMPLETE_FEED_DT + timedelta(minutes=30)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # проверяем, что у оффера выключенного позже появится еще один флажок отключения PUSH_PARTNER_FEED
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferDisabledByOtherReason',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': DataCampOffer_pb2.MARKET_STOCK,
                        'timestamp': (COMPLETE_FEED_DT + timedelta(minutes=30)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # проверяем, что оффер включенные в комплит фиде не будет отключен
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferEnabledByCompleteFeed',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': COMPLETE_FEED_TIME,
                    },
                },
            ],
        },
    },
    # проверяем, что недавно отключенный оффер, будет повторно отключен
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferAlreadyDisabledFresh',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(minutes=30)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # проверяем, что давно отключенный оффер, не будет повторно отключен
    {
        'identifiers': {
            'shop_id': 1,
            'business_id': 111,
            'offer_id': 'OfferAlreadyDisabledOld',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(days=4)).strftime(time_pattern),
                    },
                },
            ],
        },
    },

    # = = shop_id=3 - офферы для проверки ситуации с несколькими командами завершения фида
    # оффер из комплит фида shop_id=3
    {
        'identifiers': {
            'shop_id': 3,
            'business_id': 333,
            'offer_id': 'TestCompleteFeedOffer01',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': COMPLETE_FEED_TIME,
                    },
                },
            ],
        },
    },
    # оффер не из комплит фида shop_id=3
    {
        'identifiers': {
            'shop_id': 3,
            'business_id': 333,
            'offer_id': 'TestCompleteFeedOffer02',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # оффер из комплит фида shop_id=3
    {
        'identifiers': {
            'shop_id': 3,
            'business_id': 333,
            'offer_id': 'TestCompleteFeedOffer03',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': COMPLETE_FEED_TIME,
                    },
                },
            ],
        },
    },
    # оффер не из комплит фида shop_id=3
    {
        'identifiers': {
            'shop_id': 3,
            'business_id': 333,
            'offer_id': 'TestCompleteFeedOffer04',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },

    # shop_id=4 - офферы для проверки случая, когда команда завершения пришла раньше оффера
    # оффер, которого не будет в комплит фиде shop_id=4
    {
        'identifiers': {
            'shop_id': 4,
            'business_id': 444,
            'offer_id': 'TestCompleteFeedOffer00',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # оффер, который будет в комплит фиде shop_id=4, включен когда-то давно
    {
        'identifiers': {
            'shop_id': 4,
            'business_id': 444,
            'offer_id': 'TestCompleteFeedOffer01',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(minutes=30)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # оффер, которого не будет в комплит фиде shop_id=4
    {
        'identifiers': {
            'shop_id': 4,
            'business_id': 444,
            'offer_id': 'TestCompleteFeedOffer02',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # оффер, который будет в комплит фиде shop_id=4, вообще без флажков
    {
        'identifiers': {
            'shop_id': 4,
            'business_id': 444,
            'offer_id': 'TestCompleteFeedOffer03',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # оффер, которого не будет в комплит фиде shop_id=4
    {
        'identifiers': {
            'shop_id': 4,
            'business_id': 444,
            'offer_id': 'TestCompleteFeedOffer04',
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # проверяем, что оффер (со складом) без флажков отключения будет отключен флажком с типом PUSH_PARTNER_FEED
    {
        'identifiers': {
            'shop_id': 5,
            'business_id': 555,
            'offer_id': 'OfferWithoutDisableFlags',
            'warehouse_id': 42,
        },
        'meta': {
            'ts_created': OLD_TIME_UTC.strftime(time_pattern),
        },
    },
    # проверяем, что давно отключенный оффер, будет повторно отключен аплоадным комплит фидом
    {
        'identifiers': {
            'shop_id': 6,
            'business_id': 666,
            'offer_id': 'RefreshDisabledFlag',
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(days=4)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    # набор офферов для проверки работы complete command с real_feed_id
    # офферы, чей real_feed_id совпадает с тем, что пришел в complete command
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_1_with_rf_id',
            'real_feed_id': 77,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_2_with_rf_id',
            'real_feed_id': 77,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # офферы, чей real_feed_id пустой или 0
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_3_no_rf_id'
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_4_no_rf_id',
            'real_feed_id': 0,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    # офферы с real_feed_id, отличающимся от полученного в complete command
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_5_different_rf_id',
            'real_feed_id': 78,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'shop_id': 7,
            'business_id': 777,
            'offer_id': 'rf_id_test_6_different_rf_id',
            'real_feed_id': 78,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
        'status': {
            'disabled': [
                {
                    'flag': True,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': (COMPLETE_FEED_DT - timedelta(days=1)).strftime(time_pattern),
                    },
                },
            ],
        },
    },
    {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_keep',
            'warehouse_id': 1,
            'real_feed_id': 123,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_keep',
            'warehouse_id': 2,
            'real_feed_id': 123,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_partially_remove',
            'warehouse_id': 1,
            'real_feed_id': 123,
        },
        'stock_info': {
            'partner_stocks': {
                'count': 10,
            },
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_partially_remove',
            'warehouse_id': 2,
            'real_feed_id': 123,
        },
        'stock_info': {
            'partner_stocks': {
                'count': 20,
            },
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
    {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_remove',
            'warehouse_id': 1,
            'real_feed_id': 123,
        },
        'meta': {
            'scope': DataCampOffer_pb2.SELECTIVE,
        },
    },
]

TECH_COMMANDS = [
    # команда завершения комплит фида для shop_id = 1
    # у нее всего 1 шард, и в фиде пришел только 1 оффер OfferEnabledByCompleteFeed
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=111,
                    shop_id=1,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['OfferEnabledByCompleteFeed'],
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        )
                    )
                )
            )
        ]
    ),
    # команда завершения комплит фида для shop_id = 3
    # работает с множеством (-infinity; TestCompleteFeedOffer02)
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=333,
                    shop_id=3,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['TestCompleteFeedOffer01'],
                        last_offer_id='TestCompleteFeedOffer02',
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        )
                    )
                )
            )
        ]
    ),
    # команда завершения комплит фида для shop_id = 3
    # работает с множеством [TestCompleteFeedOffer02; +infinity)
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=333,
                    shop_id=3,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['TestCompleteFeedOffer03'],
                        start_offer_id='TestCompleteFeedOffer02',
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        )
                    )
                )
            )
        ]
    ),
    # команда завершения комплит фида для shop_id = 5 & supplemental_id = 42
    # у нее всего 1 шард, и в фиде пришел только 1 оффер OfferEnabledByCompleteFeed
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=555,
                    shop_id=5,
                    supplemental_id=42,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['OfferEnabledByCompleteFeed'],
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        )
                    )
                )
            )
        ]
    ),
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=666,
                    shop_id=6,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['OfferEnabledByCompleteFeed'],
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        ),
                        is_upload_feed=True
                    )
                )
            )
        ]
    ),
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=777,
                    shop_id=7,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=['rf_id_test_1_with_rf_id'],
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            )
                        ),
                        real_feed_id=77,
                    )
                )
            )
        ]
    ),
    # команда завершения мультискладового комплит фида для business_id = 888, shop_id = 8
    DatacampMessage(
        tech_command=[
            TechCommand(
                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                command_type=TechCommands_pb2.COMPLETE_FEED_FINISHED,
                command_params=TechCommandParams(
                    business_id=888,
                    shop_id=8,
                    supplemental_id=0,
                    complete_feed_command_params=CompleteFeedCommandParams(
                        untouchable_offers=[
                            'offer_id_to_keep'
                        ],
                        untouchable_multi_wh_offers={
                            'offer_id_to_partially_remove': CompleteFeedCommandParams.WarehouseIds(warehouse_ids=[2])
                        },
                        default_offer_values=DataCampOffer(
                            status=OfferStatus(
                                disabled=[
                                    Flag(
                                        flag=True,
                                        meta=UpdateMeta(
                                            source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                        )
                                    ),
                                ]
                            ),
                            stock_info=OfferStockInfo(
                                partner_stocks=OfferStocks(
                                    count=0,
                                    meta=UpdateMeta(
                                        source=DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                        timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                                    )
                                )
                            )
                        ),
                        real_feed_id=123,
                    )
                )
            )
        ]
    )
]


SHOPS = [
    {
        'shop_id': 2,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 2, 'datafeed_id': 1}),
            dict2tskv({'shop_id': 2, 'datafeed_id': 2, 'is_default': False}),
            dict2tskv({'shop_id': 2, 'datafeed_id': 3, 'is_default': True})
        ]),
        'partner_additional_info': message_from_data(
            {
                'partner_change_schema_process': {
                    'change_schema_type': PULL_TO_PUSH,
                    'start_ts': COMPLETE_FEED_TIME
                }
            },
            PartnerAdditionalInfo()
        ).SerializeToString(),
        'status': 'publish'
    }
]


@pytest.fixture(scope='function')
def offers():
    return [message_from_data(offer, DataCampOffer()) for offer in OFFERS]


@pytest.fixture(scope='function')
def partners_table(yt_server, config):
    return DataCampPartnersTable(
        yt_server,
        config.yt_partners_tablepath,
        data=SHOPS,
    )


@pytest.fixture(scope='function')
def lbk_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='function')
def datacamp_messages_topic(log_broker_stuff):
    topic = LbkTopic(log_broker_stuff)
    return topic


@pytest.fixture(scope='function')
def config(yt_server, log_broker_stuff, lbk_topic, datacamp_messages_topic):
    cfg = {
        'logbroker': {
            'offers_topic': lbk_topic.topic,
            'datacamp_messages_topic': datacamp_messages_topic.topic,
        },
        'general': {
            'color': 'white',
            'select_rows_limit': 3,
            'insert_rows_limit': 2,
        },
    }
    piper_cfg = PiperConfigMock(yt_server=yt_server,
                                log_broker_stuff=log_broker_stuff,
                                config=cfg)

    return piper_cfg


@pytest.yield_fixture(scope='function')
def piper(yt_server, log_broker_stuff, config, lbk_topic, partners_table, datacamp_messages_topic):
    resources = {
        'config': config,
        'offers_topic': lbk_topic,
        'partners_table': partners_table,
        'datacamp_messages_topic': datacamp_messages_topic,
    }
    with PiperTestEnv(yt_server, log_broker_stuff, **resources) as piper_env:
        piper_env.verify()
        yield piper_env


@pytest.fixture(scope='function')
def inserter(offers, piper, lbk_topic, datacamp_messages_topic):
    for offer in offers:
        lbk_topic.write(offer.SerializeToString())

    wait_until(lambda: piper.united_offers_processed >= len(offers))

    for tech_command in TECH_COMMANDS:
        datacamp_messages_topic.write(tech_command.SerializeToString())

    datacamp_messages_topic.write(tech_command.SerializeToString())
    disabled_offers_count = 18
    wait_until(lambda: piper.united_offers_processed >= disabled_offers_count + len(offers))


# ----------------------------- helpers ------------------------------


def _generate_status_for_disabled_from_complete_feed(shop_id, offer_id, supplemental_id=0):
    expected_offer_status = {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                    'timestamp': COMPLETE_FEED_TIME,
                }
            }
        ],
    }

    expected_offer = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
            'warehouse_id': supplemental_id
        },
        'status': expected_offer_status,
    }

    return expected_offer_status, expected_offer


def _generate_status_for_enable_from_complete_feed(shop_id, offer_id):
    expected_offer_status = {
        'disabled': [
            {
                'flag': False,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                    'timestamp': COMPLETE_FEED_TIME
                }
            }
        ],
    }

    expected_offer = {
        'identifiers': {
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': expected_offer_status,
    }

    return expected_offer_status, expected_offer


def _test_offer_in_datacamp_table(piper, business_id, shop_id, offer_id, expected_offer_status):
    expected = message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': expected_offer_status
    }, DataCampOffer())
    assert_that(piper.service_offers_table.data, HasOffers([expected]))


def _test_actual_offer_in_datacamp_table(piper, business_id, shop_id, offer_id, expected_offer_status):
    expected = message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': expected_offer_status
    }, DataCampOffer())
    assert_that(piper.actual_service_offers_table.data, HasOffers([expected]))


def _test_offer_disabled_by_complete_feed(piper, business_id, shop_id, offer_id, supplemental_id=0):
    expected_status, expected_offer = _generate_status_for_disabled_from_complete_feed(shop_id=shop_id,
                                                                                       offer_id=offer_id,
                                                                                       supplemental_id=supplemental_id)
    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=business_id,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  expected_offer_status=expected_status)


def _test_offer_enable_by_complete_feed(piper, business_id, shop_id, offer_id):
    expected_status, expected_offer = _generate_status_for_enable_from_complete_feed(shop_id=shop_id,
                                                                                     offer_id=offer_id)
    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=business_id,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  expected_offer_status=expected_status)


def _test_that_offer_exists_and_not_disabled_by_feed(piper, business_id, shop_id, offer_id):
    expected_offer = message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        }
    }, DataCampOffer())
    assert_that(piper.service_offers_table.data, HasOffers([expected_offer]))

    not_expected_status, not_expected_offer = _generate_status_for_disabled_from_complete_feed(shop_id=shop_id,
                                                                                               offer_id=offer_id)
    unexpected_offer = message_from_data({
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
        },
        'status': not_expected_status,
    }, DataCampOffer())
    assert_that(piper.service_offers_table.data, is_not(HasOffers([unexpected_offer])))


# ----------------------------- tests ------------------------------


def test_offer_without_any_disabled_flags(inserter, offers, piper):
    """Проверяем, что оффер без флажков отключения будет отключен флажком с типом PUSH_PARTNER_FEED"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=111,
                                          shop_id=1,
                                          offer_id='OfferWithoutDisableFlags')


def test_removed_offer(inserter, offers, piper):
    """Удаленные офферы не модифицируются"""
    assert_that(piper.service_offers_table.data, HasOffers([message_from_data({
        'identifiers': {
            'business_id': 111,
            'shop_id': 1,
            'offer_id': 'RemovedOffer',
        },
        'status': {
            'removed': {
                'flag': True,
            },
        },
    }, DataCampOffer())]))
    assert_that(piper.service_offers_table.data, is_not(HasOffers([message_from_data({
        'identifiers': {
            'business_id': 111,
            'shop_id': 1,
            'offer_id': 'RemovedOffer',
        },
        'status': {
            'disabled': [{
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                }
            }],
        },
    }, DataCampOffer())])))


def test_offer_without_any_disabled_flags_with_whid(inserter, offers, piper):
    """Проверяем, что оффер без флажков отключения будет отключен флажком с типом PUSH_PARTNER_FEED"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          shop_id=5,
                                          business_id=555,
                                          supplemental_id=42,
                                          offer_id='OfferWithoutDisableFlags')


def test_offer_enabled_by_partner_api_before_complete_feed(inserter, offers, piper):
    """Проверяем, что оффер, включенный ранее через партнерское апи до комплит фида, будет отключен и тип будет PUSH_PARTNER_FEED"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=111,
                                          shop_id=1,
                                          offer_id='OfferEnabledByPushApiBeforeComplete')


def test_offer_enabled_by_partner_api_after_complete_feed(inserter, offers, piper):
    """Проверяем, что оффер, включенный через партнерское апи позже комплит фида, не будет отключен"""
    enable_time = (COMPLETE_FEED_DT + timedelta(minutes=30)).strftime(time_pattern)
    shop_id = 1
    business_id = 111
    offer_id = 'OfferEnabledByPushApiAfterComplete'
    expected_status = {
        'disabled': [
            {
                'flag': False,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                    'timestamp': enable_time
                }
            }
        ],
    }

    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=business_id,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  expected_offer_status=expected_status)


def test_offer_disabled_by_other_reason(inserter, offers, piper):
    """Проверяем, что у оффера выключенного позже появится еще один флажок отключения PUSH_PARTNER_FEED"""
    disable_time = (COMPLETE_FEED_DT + timedelta(minutes=30)).strftime(time_pattern)
    shop_id = 1
    business_id = 111
    offer_id = 'OfferDisabledByOtherReason'
    expected_status = {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                    'timestamp': COMPLETE_FEED_TIME
                }
            },
        ],
    }

    expected_actual_status = {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DataCampOffer_pb2.MARKET_STOCK,
                    'timestamp': disable_time
                }
            }
        ],
    }

    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=business_id,
                                  shop_id=shop_id,
                                  offer_id=offer_id,
                                  expected_offer_status=expected_status)

    _test_actual_offer_in_datacamp_table(piper=piper,
                                         business_id=business_id,
                                         shop_id=shop_id,
                                         offer_id=offer_id,
                                         expected_offer_status=expected_actual_status)


def test_offer_enabled_by_complete_feed(inserter, offers, piper):
    """Проверяем, что оффер включенные в комплит фиде не будет отключен"""
    _test_offer_enable_by_complete_feed(piper=piper,
                                        business_id=111,
                                        shop_id=1,
                                        offer_id='OfferEnabledByCompleteFeed')


def test_offer_with_old_disable_dedeup(inserter, offers, piper):
    """Проверяем, что давно выключенный оффер не выключается повторно"""
    disable_time = (COMPLETE_FEED_DT - timedelta(days=4)).strftime(time_pattern)
    expected_status = {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_API,
                    'timestamp': disable_time
                }
            }
        ],
    }
    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=111,
                                  shop_id=1,
                                  offer_id='OfferAlreadyDisabledOld',
                                  expected_offer_status=expected_status)


def test_offer_with_new_disable_dedeup(inserter, offers, piper):
    """Проверяем, что недавно выключенный оффер выключается повторно"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=111,
                                          shop_id=1,
                                          offer_id='OfferAlreadyDisabledFresh')


def test_complete_feed_with_several_finish_command(inserter, offers, piper):
    """Проверяем корректную обработку для комплит команды у которой было несколько частей"""
    # не отключено все, что было включено в фиде
    _test_offer_enable_by_complete_feed(piper=piper,
                                        business_id=333,
                                        shop_id=3,
                                        offer_id='TestCompleteFeedOffer01')

    _test_offer_enable_by_complete_feed(piper=piper,
                                        business_id=333,
                                        shop_id=3,
                                        offer_id='TestCompleteFeedOffer03')

    # отключено все, чего не было в фиде
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=333,
                                          shop_id=3,
                                          offer_id='TestCompleteFeedOffer02')

    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=333,
                                          shop_id=3,
                                          offer_id='TestCompleteFeedOffer04')


def test_real_feed_id_complete_command(inserter, offers, piper):
    """Тест проверяет работу complete command с real_feed_id"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=777,
                                          shop_id=7,
                                          offer_id='rf_id_test_2_with_rf_id')

    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=777,
                                          shop_id=7,
                                          offer_id='rf_id_test_3_no_rf_id')

    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=777,
                                          shop_id=7,
                                          offer_id='rf_id_test_4_no_rf_id')

    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=777,
                                                     shop_id=7,
                                                     offer_id='rf_id_test_1_with_rf_id')

    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=777,
                                                     shop_id=7,
                                                     offer_id='rf_id_test_5_different_rf_id')

    """Оффер с real_feed_id != real_feed_id из complete command остается с тем же статусом, что и был"""
    disable_time = (COMPLETE_FEED_DT - timedelta(days=1)).strftime(time_pattern)
    expected_status = {
        'disabled': [
            {
                'flag': True,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                    'timestamp': disable_time
                }
            }
        ],
    }
    _test_offer_in_datacamp_table(piper=piper,
                                  business_id=777,
                                  shop_id=7,
                                  offer_id='rf_id_test_6_different_rf_id',
                                  expected_offer_status=expected_status)


@pytest.mark.skip(reason='MARKETINDEXER-42190 мигает и блочит релизы, будем фиксить отдельно')
def test_complete_feed_command_arrives_earliers_than_feed_offers(inserter, offers, piper,
                                                                 lbk_topic, datacamp_messages_topic):
    """Проверяем корректную обработку случая, когда команда завершения комплит фида приходит раньше, чем оффера из фида"""
    shop_id = 4
    business_id = 444
    supplemental_id = 0

    # шаг 1 - посылаем команду о завершении обработки комплит-фида по одной части офферов фида: [TestCompleteFeedOffer03, +infinity)
    tech_command01 = {
        'tech_command': [
            {
                'timestamp': COMPLETE_FEED_TIME,
                'command_type': TechCommands_pb2.COMPLETE_FEED_FINISHED,
                'command_params': {
                    'business_id': business_id,
                    'shop_id': shop_id,
                    'supplemental_id': supplemental_id,
                    'complete_feed_command_params': {
                        'start_offer_id': 'TestCompleteFeedOffer03',
                        'untouchable_offers': ['TestCompleteFeedOffer03'],
                        'default_offer_values': {
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            'timestamp': COMPLETE_FEED_TIME
                                        }
                                    }
                                ]
                            }
                        }
                    },
                }
            }
        ]
    }

    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(tech_command01, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 1)

    # шаг 2 - проверяем, что untouchable offer-а из команды не были изменены
    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=business_id,
                                                     shop_id=shop_id,
                                                     offer_id='TestCompleteFeedOffer03')

    # шаг 3 - проверяем, что оффера не из комлит фида, но относящиеся к текущему батчу были отключены
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=business_id,
                                          shop_id=shop_id,
                                          offer_id='TestCompleteFeedOffer04')

    # шаг 4 - проверяем, что все офера не из текущего батча не были изменены
    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=business_id,
                                                     shop_id=shop_id,
                                                     offer_id='TestCompleteFeedOffer00')
    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=business_id,
                                                     shop_id=shop_id,
                                                     offer_id='TestCompleteFeedOffer01')
    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=business_id,
                                                     shop_id=shop_id,
                                                     offer_id='TestCompleteFeedOffer02')

    # шаг 5 - отправляем изменение оффера из комплит фида по предыдущему батчу
    update_offer_03 = {
        'identifiers': {
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': 'TestCompleteFeedOffer03',
        },
        'status': {
            'disabled': [
                {
                    'flag': False,
                    'meta': {
                        'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                        'timestamp': COMPLETE_FEED_TIME,
                    },
                },
            ],
        },
    }

    lbk_topic.write(message_from_data(update_offer_03, DataCampOffer()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 2)

    # шаг 6 - проверяем, что оффер обновился
    _test_offer_enable_by_complete_feed(piper=piper,
                                        business_id=business_id,
                                        shop_id=shop_id,
                                        offer_id='TestCompleteFeedOffer03')

    # шаг 7 - посылаем команду о завершении обработки комплит-фида по одной части офферов фида: [-infinity; TestCompleteFeedOffer03)
    tech_command02 = {
        'tech_command': [
            {
                'timestamp': COMPLETE_FEED_TIME,
                'command_type': TechCommands_pb2.COMPLETE_FEED_FINISHED,
                'command_params': {
                    'business_id': business_id,
                    'shop_id': shop_id,
                    'supplemental_id': supplemental_id,
                    'complete_feed_command_params': {
                        'last_offer_id': 'TestCompleteFeedOffer03',
                        'untouchable_offers': ['TestCompleteFeedOffer01'],
                        'default_offer_values': {
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            'timestamp': COMPLETE_FEED_TIME
                                        }
                                    }
                                ]
                            }
                        }
                    },
                }
            }
        ]
    }
    datacamp_messages_topic.write(message_from_data(tech_command02, DatacampMessage()).SerializeToString())
    wait_until(lambda: piper.united_offers_processed >= united_offers_processed + 4)

    # шаг 8 - проверяем, что untouchable offer-а из команды не были изменены
    _test_that_offer_exists_and_not_disabled_by_feed(piper=piper,
                                                     business_id=business_id,
                                                     shop_id=shop_id,
                                                     offer_id='TestCompleteFeedOffer01')

    # шаг 9 - проверяем, что оффера не из комлит фида, но относящиеся к текущему из батчу были отключены
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=business_id,
                                          shop_id=shop_id,
                                          offer_id='TestCompleteFeedOffer00')
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=business_id,
                                          shop_id=shop_id,
                                          offer_id='TestCompleteFeedOffer02')


def test_change_schema_process(piper, partners_table, datacamp_messages_topic):
    shop_id = 2
    supplemental_id01 = 100
    feed_id01 = 1
    supplemental_id02 = 200
    feed_id02 = 2

    # шаг 1 - посылаем команду о завершении обработки комплит-фида, но старую
    tech_command01 = {
        'tech_command': [
            {
                'timestamp': OLD_TIME_UTC.strftime(time_pattern),
                'command_type': TechCommands_pb2.COMPLETE_FEED_FINISHED,
                'command_params': {
                    'shop_id': shop_id,
                    'supplemental_id': supplemental_id01,
                    'feed_id': feed_id01,
                    'complete_feed_command_params': {
                        'default_offer_values': {
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            'timestamp': OLD_TIME_UTC.strftime(time_pattern)
                                        }
                                    }
                                ]
                            }
                        }
                    },
                }
            }
        ]
    }

    united_offers_processed = piper.united_offers_processed
    datacamp_messages_topic.write(message_from_data(tech_command01, DatacampMessage()).SerializeToString())
    assert_that(calling(wait_until).with_args(lambda: piper.united_offers_processed >= united_offers_processed + 1, 10),
                raises(RuntimeError))

    # шаг 2 - проверяем, что в партнерской таблице не появились изменения
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows([
                    {
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': create_timestamp_from_json(COMPLETE_FEED_TIME).seconds,
                                },
                                'parsed_complete_feeds': [],
                                'is_ready_to_index_from_datacamp': False
                            }
                        })
                    }
                ]))

    # шаг 3 - посылаем команду о завершении обработки комплит-фида для одного из складов
    tech_command02 = {
        'tech_command': [
            {
                'timestamp': COMPLETE_FEED_TIME,
                'command_type': TechCommands_pb2.COMPLETE_FEED_FINISHED,
                'command_params': {
                    'shop_id': shop_id,
                    'supplemental_id': supplemental_id01,
                    'feed_id': feed_id01,
                    'complete_feed_command_params': {
                        'default_offer_values': {
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            'timestamp': COMPLETE_FEED_TIME,
                                        }
                                    }
                                ]
                            }
                        }
                    },
                }
            }
        ]
    }
    datacamp_messages_topic.write(message_from_data(tech_command02, DatacampMessage()).SerializeToString())
    assert_that(calling(wait_until).with_args(lambda: piper.united_offers_processed >= united_offers_processed + 1, 10),
                raises(RuntimeError))

    # шаг 4 - проверяем, что в партнерской таблице появились изменения
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows([
                    {
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': create_timestamp_from_json(COMPLETE_FEED_TIME).seconds,
                                },
                                'parsed_complete_feeds': [feed_id01],
                                'is_ready_to_index_from_datacamp': False
                            }
                        })
                    }
                ]))

    # шаг 5 - посылаем команду о завершении обработки комплит-фида для другого склада
    tech_command03 = {
        'tech_command': [
            {
                'timestamp': COMPLETE_FEED_TIME,
                'command_type': TechCommands_pb2.COMPLETE_FEED_FINISHED,
                'command_params': {
                    'shop_id': shop_id,
                    'supplemental_id': supplemental_id02,
                    'feed_id': feed_id02,
                    'complete_feed_command_params': {
                        'default_offer_values': {
                            'status': {
                                'disabled': [
                                    {
                                        'flag': True,
                                        'meta': {
                                            'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                                            'timestamp': COMPLETE_FEED_TIME,
                                        }
                                    }
                                ]
                            }
                        }
                    },
                }
            }
        ]
    }
    datacamp_messages_topic.write(message_from_data(tech_command03, DatacampMessage()).SerializeToString())
    assert_that(calling(wait_until).with_args(lambda: piper.united_offers_processed >= united_offers_processed + 1, 10),
                raises(RuntimeError))

    # шаг 6 - проверяем, что теперь партнер готов к индексации из хранилища
    partners_table.load()
    assert_that(partners_table.data,
                HasDatacampPartersYtRows([
                    {
                        'shop_id': shop_id,
                        'partner_additional_info': IsSerializedProtobuf(PartnerAdditionalInfo, {
                            'partner_change_schema_process': {
                                'change_schema_type': PULL_TO_PUSH,
                                'start_ts': {
                                    'seconds': create_timestamp_from_json(COMPLETE_FEED_TIME).seconds,
                                },
                                'parsed_complete_feeds': [feed_id01, feed_id02],
                                'is_ready_to_index_from_datacamp': True
                            }
                        })
                    }
                ]))


def test_refresh_disabled_flag_from_upload_feed(inserter, offers, piper):
    """Проверяем, что недавно выключенный оффер выключается повторно"""
    _test_offer_disabled_by_complete_feed(piper=piper,
                                          business_id=666,
                                          shop_id=6,
                                          offer_id='RefreshDisabledFlag')


def test_multiwarehouse_complete_feed(inserter, offers, piper):
    """Тест проверяет работу complete command для мультискладовых фидов"""

    _test_offer_disabled_by_complete_feed(
        piper=piper,
        business_id=888,
        shop_id=8,
        offer_id='offer_id_to_remove',
        supplemental_id=1,
    )

    _test_that_offer_exists_and_not_disabled_by_feed(
        piper=piper,
        business_id=888,
        shop_id=8,
        offer_id='offer_id_to_keep',
    )

    _test_that_offer_exists_and_not_disabled_by_feed(
        piper=piper,
        business_id=888,
        shop_id=8,
        offer_id='offer_id_to_partially_remove',
    )

    expected = {
        'identifiers': {
            'business_id': 888,
            'shop_id': 8,
            'offer_id': 'offer_id_to_partially_remove',
            'warehouse_id': 2,
        },
        'stock_info': {
            'partner_stocks': {
                'count': 0,
                'meta': {
                    'source': DataCampOffer_pb2.PUSH_PARTNER_FEED,
                    'timestamp': COMPLETE_FEED_TIME
                }
            }
        }
    }
    assert_that(piper.actual_service_offers_table.data, is_not(HasOffers([message_from_data(expected, DataCampOffer())])))

    expected['identifiers']['warehouse_id'] = 1
    expected['stock_info'] = None
    assert_that(piper.actual_service_offers_table.data, HasOffers([message_from_data(expected, DataCampOffer())]))
