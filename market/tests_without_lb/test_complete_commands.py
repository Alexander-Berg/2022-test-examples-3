# coding: utf-8

from datetime import datetime, timedelta
from hamcrest import assert_that, has_entries, equal_to, has_items
import pytest
from yt.wrapper import ypath_join

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.yatf.utils import create_meta_dict
from market.idx.datacamp.routines.yatf.test_env import CompleteCommandsEnv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.offer.TechCommands_pb2 import CompleteFeedCommandParams
from market.idx.yatf.utils.utils import create_timestamp_from_json
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf, IsProtobufMap
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import UnitedOffer

from market.idx.yatf.utils.utils import create_pb_timestamp

# buisnes_id, shop_id, offer id, is_removed
OFFERS_INFO = [
    (1, 2, "1", False),
    (1, 2, "2", False),
    (1, 4, "2", False),
    (1, 2, "3", False),
    (1, 2, "8", True),  # removed offer should not be disabled
    (1, 3, "4", False),
    (1, 3, "5", False),
    (2, 2, "6", False),
    (2, 2, "7", False),
]

time_pattern = "%Y-%m-%dT%H:%M:%SZ"
COMPLETE_FEED_DT = datetime.utcnow()
COMPLETE_FEED_TIME = COMPLETE_FEED_DT.strftime(time_pattern)
COMPLETE_FEED_DT_OLD = COMPLETE_FEED_DT - timedelta(hours=1)
COMPLETE_FEED_TIME_OLD = COMPLETE_FEED_DT_OLD.strftime(time_pattern)

COMPLETE_COMMANDS_DATA = [
    {
        'business_id': 1,
        'shop_id': 2,
        'tech_command' : CompleteFeedCommandParams(
            untouchable_offers=['1'],
            default_offer_values=DTC.Offer(
                status=DTC.OfferStatus(
                    disabled=[
                        DTC.Flag(
                            flag=True,
                            meta=DTC.UpdateMeta(
                                source=DTC.PUSH_PARTNER_FEED,
                                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME),
                            )
                        ),
                    ]
                )
            )
        ).SerializeToString()
    },
    {
        'business_id': 1,
        'shop_id': 2,
        'tech_command' : CompleteFeedCommandParams(
            untouchable_offers=['2'],
            default_offer_values=DTC.Offer(
                status=DTC.OfferStatus(
                    disabled=[
                        DTC.Flag(
                            flag=True,
                            meta=DTC.UpdateMeta(
                                source=DTC.PUSH_PARTNER_FEED,
                                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME_OLD),
                            )
                        ),
                    ]
                )
            )
        ).SerializeToString()
    },
    {
        'business_id': 1,
        'shop_id': 4,
        'tech_command' : CompleteFeedCommandParams(
            untouchable_offers=['3'],
            default_offer_values=DTC.Offer(
                status=DTC.OfferStatus(
                    disabled=[
                        DTC.Flag(
                            flag=True,
                            meta=DTC.UpdateMeta(
                                source=DTC.PUSH_PARTNER_FEED,
                                timestamp=create_timestamp_from_json(COMPLETE_FEED_TIME_OLD),
                            )
                        ),
                    ]
                )
            )
        ).SerializeToString()
    }
]


def commands_table_attributes():
    schema = [
        dict(name='business_id', type='uint64'),
        dict(name='shop_id', type='uint64'),
        dict(name='feed_id', type='uint64'),
        dict(name='tech_command', type='string')
    ]

    return {
        'schema': schema,
        'dynamic': True,
    }


class CommandsTable(YtDynTableResource):
    def __init__(self, yt_stuff, path, data=None):
        super(CommandsTable, self).__init__(
            yt_stuff=yt_stuff,
            path=path,
            attributes=commands_table_attributes(),
            data=data
        )


@pytest.fixture(scope='module')
def commands_table(yt_server, config):
    data = COMPLETE_COMMANDS_DATA
    return CommandsTable(yt_server, config.cc_commands_table_path, data=data)


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'yt_home': '//home/datacamp/united'
            },
            'yt': {
                'meta_proxy': yt_server.get_yt_client().config["proxy"]["url"],
            },
            'complete_commands' : {
                'enable' : True,
                'commands_table_path' : "//home/datacamp/complete_commands/queue",
                'primary_mr_proxy' : yt_server.get_yt_client().config["proxy"]["url"],
                'mr_input_table_path' : "//home/datacamp/mr_input/input",
                'mr_output_table_path' : "//home/datacamp/mr_input/output",
                'mr_output_dir_path' : "//home/datacamp/mr_outputs/outputs",
                'meta_lock_path' : "//home/datacamp/sync/meta_lock",
                'enable_deduplication' : True,
                'force_deduplication' : True,
            }
        })
    return config


@pytest.fixture(scope='module')
def service_offers_table_data():
    return [
        {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'warehouse_id': 0,
                'shop_id': shop_id,
                'feed_id': 1000,
            },
            'meta': create_meta_dict(10, DTC.WHITE),
            'status': {
                'publish': DTC.HIDDEN,
                'removed': {
                    'flag': is_removed,
                }
            },
        } for business_id, shop_id, offer_id, is_removed in OFFERS_INFO
    ] + [
        {
            'identifiers': {
                'business_id': 1,
                'offer_id': "666",
                'warehouse_id': 0,
                'shop_id': 2,
                'feed_id': 1000
            },
            'meta': create_meta_dict(10, DTC.WHITE),
            'status': {
                'disabled': [{
                    'flag': True,
                    'meta': {
                        'source': DTC.PUSH_PARTNER_FEED,
                        'timestamp': create_pb_timestamp(10).ToJsonString(),
                    },
                }]
            },
        }
    ]


@pytest.yield_fixture(scope='module')
def routines(
    yt_server,
    config,
    service_offers_table,
    commands_table
):
    resources = {
        'service_offers_table': service_offers_table,
        'commands_table' : commands_table,
        'config': config,
    }
    with CompleteCommandsEnv(yt_server, **resources) as routines_env:
        yield routines_env


def test_simple(yt_server, commands_table, routines, config):
    yt_client = yt_server.get_yt_client()
    out_dir_content = yt_client.list(config.cc_mr_output_dir_path)
    assert_that(len(out_dir_content), equal_to(1))
    out_table_name = ypath_join(config.cc_mr_output_dir_path, out_dir_content[0])
    hidden_offers = list(yt_client.read_table(out_table_name))
    assert_that(len(hidden_offers), equal_to(2))
    assert_that(hidden_offers, has_items(
        has_entries({
            'business_id': 1,
            'shop_sku': '2',
            'offer': IsSerializedProtobuf(UnitedOffer, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': '2',
                    },
                },
                'service': IsProtobufMap({
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': '2',
                            'shop_id': 2
                        },
                    },
                    4: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': '2',
                            'shop_id': 4
                        },
                    },
                })
            })
        }),
        has_entries({
            'business_id': 1,
            'shop_sku': '3',
            'offer': IsSerializedProtobuf(UnitedOffer, {
                'basic': {
                    'identifiers': {
                        'business_id': 1,
                        'offer_id': '3',
                    },
                },
                'service': IsProtobufMap({
                    2: {
                        'identifiers': {
                            'business_id': 1,
                            'offer_id': '3',
                            'shop_id': 2
                        },
                    },
                })
            })
        }),
    ))
