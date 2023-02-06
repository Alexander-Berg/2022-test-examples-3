# coding: utf-8

from datetime import datetime, timedelta
from hamcrest import assert_that, has_entries, equal_to, greater_than, has_items, is_not, has_item, has_entry, empty
import pytest
import time

import logging

from google.protobuf.timestamp_pb2 import Timestamp
from google.protobuf.json_format import MessageToDict

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.yatf.utils import (
    create_flag,
    create_meta,
    create_update_meta,
    dict2tskv,
)
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampBasicOffersTable,
    DataCampServiceOffersTable
)
from market.idx.datacamp.routines.yatf.test_env import UnitedDatacampDumperEnv, HttpRoutinesTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.offer.UnitedOffer_pb2 as UO
import market.idx.datacamp.routines.tasks.dumper.proto.Stats_pb2 as Stats
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows, HasDatacampYtOfferDiffRows
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap

from market.idx.yatf.utils.utils import create_timestamp_from_json, create_pb_timestamp
import modadvert.bigmod.protos.interface.verdict_pb2 as BM

from market.pylibrary.proto_utils import message_from_data


from robot.sortdc.protos.user_pb2 import EUser

NOW_TIME_UTC = datetime.utcnow()
OLD_TIME_UTC = (datetime.utcnow() - timedelta(days=10))
time_pattern = "%Y-%m-%dT%H:%M:%SZ"
NOW_TIME = NOW_TIME_UTC.strftime(time_pattern)
NOW_TS = create_timestamp_from_json(NOW_TIME)
OLD_TIME = OLD_TIME_UTC.strftime(time_pattern)
OLD_TS = create_timestamp_from_json(OLD_TIME)
SHOP_ID_DBS = 28
MM_BUSINESS_ID = 10802217
MM_SHOP_ID = 10806910
BUSINESS_FOR_INVISIBLE = 1090
SHOP_FOR_INVISIBLE = 1091

log = logging.getLogger()

DEFAULT_PRICE = DTC.OfferPrice(basic=DTC.PriceBundle(binary_price=DTC.PriceExpression(price=10), meta=create_update_meta(10)))

DEFAULT_PARTNER_CONTENT = DTC.PartnerContent(
    original=DTC.OriginalSpecification(
        name=DTC.StringValue(
            value="name",
            meta=create_update_meta(10)
        ),
    ),
    actual=DTC.ProcessedSpecification(
        title=DTC.StringValue(
            value="title",
            meta=create_update_meta(10)
        ),
        url=DTC.StringValue(
            value="url",
            meta=create_update_meta(10)
        )
    )
)

# buisnes_id, shop_id, color, is_for_vertical, sortdc_context_user, bids_data
OFFERS_INFO = [
    (1, 1, DTC.WHITE, True, EUser.GOODS, True),
    (88, 8, DTC.WHITE, False, EUser.UNKNOWN, False),
    (1, 2, DTC.BLUE, False, EUser.UNKNOWN, False),
    (2, 2, DTC.BLUE, True, EUser.GOODS, True),
    (7, 7, DTC.DIRECT, True, EUser.GOODS, True),
    (9, 9, DTC.DIRECT, False, EUser.UNKNOWN, False),
    (10, 10, DTC.DIRECT, False, EUser.UNKNOWN, True),
    (11, 11, DTC.DIRECT, False, EUser.UNKNOWN, False),
    (12, 12, DTC.DIRECT, False, EUser.UNKNOWN, False),
    (13, 13, DTC.LAVKA, False, EUser.UNKNOWN, True),
    (14, 14, DTC.EDA, False, EUser.UNKNOWN, True),
    (21, 21, DTC.WHITE, False, EUser.UNKNOWN, True),
    (22, 22, DTC.WHITE, False, EUser.UNKNOWN, False),
]


PARTNERS = [
    {
        'shop_id': 1,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 1, 'business_id': 1}),
        ]),
    },
    {
        'shop_id': 2,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 2, 'business_id': 2}),
        ]),
    },
    {
        # disabled but alive shop
        'shop_id': 5,
        'mbi': '\n\n'.join([
            dict2tskv({'#shop_id': 5, 'business_id': 4, 'feed_id': 111, 'is_enabled': False, 'is_alive': True}),
        ]),
        'status': 'publish'
    },
    {
        'shop_id': 7,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 7, 'business_id': 7, 'direct_status': 'REAL', 'direct_search_snippet_gallery': True}),
        ]),
    },
    {
        'shop_id': 9,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 9, 'business_id': 9, 'direct_status': 'REAL', 'direct_search_snippet_gallery': True}),
        ]),
    },
    {
        'shop_id': 10,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 10, 'business_id': 10, 'direct_status': 'REAL', 'direct_search_snippet_gallery': True}),
        ]),
    },
    {
        'shop_id': 11,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 11, 'business_id': 11, 'direct_status': 'REAL', 'direct_search_snippet_gallery': True}),
        ]),
    },
    {
        'shop_id': 12,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 12, 'business_id': 12, 'direct_status': 'REAL'}),
        ]),
    },
    {  # магазин dsbs
        'shop_id': 8,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 8, 'business_id': 88, 'is_dsbs': 'true'}),
        ]),
    },
    {
        'shop_id': 13,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 13, 'business_id': 13, 'is_lavka': 'true'}),
        ]),
    },
    {
        'shop_id': 14,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 14, 'business_id': 14, 'is_eda': 'true'}),
        ]),
    },
    {
        'shop_id': 21,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 21, 'business_id': 21, 'ignore_stocks': 'false'}),
        ]),
    },
    {
        'shop_id': 22,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 22, 'business_id': 22, 'ignore_stocks': 'true'}),
        ]),
    },
    {  # магазин dsbs (для проверки вердиктов)
        'shop_id': SHOP_ID_DBS,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': SHOP_ID_DBS, 'business_id': 4, 'is_dsbs': 'true'}),
        ]),
    },
    {
        'shop_id': 10806905,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 10806905, 'business_id': 10802213, 'feed_id': 200886090, 'is_enabled': True}),
        ]),
        'status': 'publish'
    },
    {
        'shop_id': 10806906,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 10806906, 'business_id': 10802214, 'feed_id': 200886091, 'is_enabled': True}),
        ]),
        'status': 'publish'
    },
    {
        'shop_id': 10806907,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 10806907, 'business_id': 10802215, 'feed_id': 200886092, 'is_enabled': True, 'direct_goods_ads': True}),
        ]),
        'status': 'publish'
    },
    {
        'shop_id': 10806908,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': 10806908, 'business_id': 10802216, 'feed_id': 200886093, 'is_enabled': True}),
        ]),
        'status': 'publish'
    },
    # для проверки маркировки на перемайнинг офферов после завершения процесса модерации маппинга
    {
        'shop_id': MM_SHOP_ID,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': MM_SHOP_ID, 'business_id': MM_BUSINESS_ID, 'feed_id': 200886094, 'is_enabled': True}),
        ]),
        'status': 'publish'
    },
    # для проверки невидимых офферов
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'mbi': '\n\n'.join([
            dict2tskv({'shop_id': SHOP_FOR_INVISIBLE, 'business_id': BUSINESS_FOR_INVISIBLE, 'is_enabled': True}),
        ]),
        'status': 'publish'
    },
]


BASIC_OFFERS_VERICAL = [
    {
        'business_id': 10802215,
        'shop_sku': 'VerticalDirect',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802215,
            offer_id='VerticalDirect'
        ).SerializeToString(),
        'original_partner_content': DEFAULT_PARTNER_CONTENT.original.SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'master_data': DTC.MarketMasterData(
            dimensions=DTC.PreciseDimensions(
                length_mkm=123456,
                width_mkm=123456,
                height_mkm=123456
            ),
            weight_gross=DTC.PreciseWeight(
                value_mg=123456
            )
        ).SerializeToString()
    },
    {
        'business_id': 10802216,
        'shop_sku': 'VerticalGoodsAds',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802216,
            offer_id='VerticalGoodsAds'
        ).SerializeToString(),
        'original_partner_content': DEFAULT_PARTNER_CONTENT.original.SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'master_data': DTC.MarketMasterData(
            dimensions=DTC.PreciseDimensions(
                length_mkm=123456,
                width_mkm=123456,
                height_mkm=123456
            ),
            weight_gross=DTC.PreciseWeight(
                value_mg=123456
            )
        ).SerializeToString()
    },
]


BASIC_OFFERS = [
    {
        'business_id': business_id,
        'shop_sku': 'T600',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id='T600',
                feed_id=1000,
            ),
            price=DEFAULT_PRICE,
            content=DTC.OfferContent(
                partner=DEFAULT_PARTNER_CONTENT,
                master_data=DTC.MarketMasterData(
                    dimensions=DTC.PreciseDimensions(
                        length_mkm=123456,
                        width_mkm=123456,
                        height_mkm=123456
                    ),
                    weight_gross=DTC.PreciseWeight(
                        value_mg=123456
                    )
                )
            ),
        ).SerializeToString()
    } for business_id in (1, 2, 7, 9, 10, 11, 12, 13, 14, 88)
]

# basic offers for testing partner disables
BASIC_OFFERS_FOR_PARTNER_DISABLES_CHECK = [
    {
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=3,
                offer_id='OfferForTestingPartnerDisables',
                feed_id=1000
            ),
            price=DEFAULT_PRICE,
            content=DTC.OfferContent(
                partner=DEFAULT_PARTNER_CONTENT
            ),
        ).SerializeToString()
    },
]


def create_basic_offer(business_id, offer_id, feed_id=1021):
    return {
        'business_id': business_id,
        'shop_sku': offer_id,
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id=offer_id,
                feed_id=feed_id
            ),
            price=DEFAULT_PRICE,
            content=DTC.OfferContent(
                partner=DEFAULT_PARTNER_CONTENT
            ),
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
    }


# basic offers for testing partner disables
BASIC_OFFERS_FOR_PARTNER_IGNORE_STOCK_CHECK = [
    # offers of parntner who not ignore stock
    create_basic_offer(21, 'OfferForTestingIgnoreStocks'),
    create_basic_offer(21, 'OfferForTestingIgnoreStocks_NotDisabled'),
    create_basic_offer(21, 'OfferForTestingIgnoreStocks_Disabled'),
    # offers of parntner who ignore stock
    create_basic_offer(22, 'OfferForTestingIgnoreStocks'),
    create_basic_offer(22, 'OfferForTestingIgnoreStocks_NotDisabled'),
    create_basic_offer(22, 'OfferForTestingIgnoreStocks_Disabled'),
]

BASIC_OFFERS_FOR_SKIP_INVALID_OFFERS = [
    {
        'business_id': 4,
        'shop_sku': 'InvalidBlueOffer',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='InvalidBlueOffer',
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString()
    },
    {
        'business_id': 4,
        'shop_sku': 'BlueFulfillemtOfferWithoutActual',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='BlueFulfillemtOfferWithoutActual',
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString()
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutTitle',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutTitle',
        ).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithInvalidWareMd5',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithInvalidWareMd5',
        ).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithValidWareMd5',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithValidWareMd5',
        ).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutActual',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutActual',
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString()
    },
]

BASIC_OFFERS_REMOVED = [
    {
        'business_id': 4,
        'shop_sku': offer_id,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
        'status': status
    } for offer_id, status in [
        ('RemovedOfferByBasic', DTC.OfferStatus(removed=DTC.Flag(flag=True)).SerializeToString()),
        ('RemovedOfferByService', None)
    ]
]

BASIC_OFFERS_FOR_PERESORT = [
    {
        'business_id': 4,
        'shop_sku': offer_id,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            peresort=DTC.Peresort(
                content_peresort_ts=Timestamp(seconds=ts),
            )
        ).SerializeToString() if ts else None,
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
    } for offer_id, ts in [
        ('OfferSetPeresort', NOW_TS.seconds - 10),
        ('OfferCantSetPeresortBecauseItIsTooLate', NOW_TS.seconds - 24 * 60 * 60 - 1),
        ('OfferCantSetPeresortBecauseOfEmptyContentFlap', None),
        ('OfferUnsetPeresort', NOW_TS.seconds - 10),
        ('OfferUnsetPeresortLonely', NOW_TS.seconds - 10),
        ('OfferDiffPeresort', NOW_TS.seconds - 10)
    ]
]

BASIC_OFFERS_FOR_VERDICTS = [
    {
        'business_id': 4,
        'shop_sku': offer_id,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'resolution': DTC.Resolution(
            by_source=[
                DTC.Verdicts(
                    meta=create_update_meta(ts, source=source),
                    verdict=[
                        DTC.Verdict(
                            results=[
                                DTC.ValidationResult(
                                    is_banned=True,
                                    messages=[DTC.Explanation(code='common.error.code')],
                                    applications=[DTC.FULFILLMENT, DTC.CPA, DTC.MARKET, DTC.DBS]
                                )
                            ]
                        )
                    ]
                )
            ]
        ).SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
    } for offer_id, ts, source in [
        ('OfferSetMdmVerdict', NOW_TS.seconds - 10, DTC.MARKET_MDM),
        ('OfferSetMdmVerdictForDBS', NOW_TS.seconds - 10, DTC.MARKET_MDM),
        ('OfferSameMdmVerdict', NOW_TS.seconds - 10, DTC.MARKET_MDM),
        ('OfferSetMbiMigratorVerdict', NOW_TS.seconds - 10, DTC.MARKET_MBI_MIGRATOR),
        ('OfferSameMbiMigratorVerdict', NOW_TS.seconds - 10, DTC.MARKET_MBI_MIGRATOR),
        ('OfferSetGutginVerdict', NOW_TS.seconds - 10, DTC.MARKET_GUTGIN),
        ('OfferSameGutginVerdict', NOW_TS.seconds - 10, DTC.MARKET_GUTGIN),
    ]
]

BASIC_OFFERS_FOR_INTEGRAL_STATUS = [
    {
        'business_id': 4,
        'shop_sku': 'OfferForIntegralStatus',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferForIntegralStatus',
        ).SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'content_status': DTC.ContentStatus(
            content_system_status=DTC.ContentSystemStatus(
                status_content_version=DTC.VersionCounter(
                    counter=11
                )
            )
        ).SerializeToString(),
        'binding': DTC.ContentBinding(
            approved=DTC.Mapping(
                market_sku_id=12345,
                meta=create_update_meta(10),
            ),
            meta=create_update_meta(10),
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            version=DTC.VersionStatus(
                actual_content_version=DTC.VersionCounter(
                    counter=11
                )
            )
        ).SerializeToString()
    }
]

BASIC_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK = [
    {
        'business_id': 10802213,
        'shop_sku': 'OfferForClassififerGoodIdCheck',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802213,
            offer_id='OfferForClassififerGoodIdCheck',
            extra=DTC.OfferExtraIdentifiers(
                classifier_good_id='5793d36dd1a3cadd5b43a640960dd60a',
            ),
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
    }
]

BASIC_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK = [
    {
        'business_id': 10802214,
        'shop_sku': 'OfferForClassififerMagicIdCheck',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802214,
            offer_id='OfferForClassififerMagicIdCheck',
            extra=DTC.OfferExtraIdentifiers(
                classifier_magic_id2='2d15e94d598540df785ab10dfc262d89',
            ),
        ).SerializeToString(),
        'actual_partner_content': DEFAULT_PARTNER_CONTENT.actual.SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
    }
]

BASIC_OFFERS_FOR_MAPPING_MODERATION = [
    {
        'business_id': MM_BUSINESS_ID,
        'shop_sku': offer_id,
        'identifiers': DTC.OfferIdentifiers(
            business_id=MM_BUSINESS_ID,
            offer_id=offer_id,
        ).SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
        'binding': DTC.ContentBinding(
            approved=DTC.Mapping(
                market_sku_id=approved["market_sku_id"],
                msku_change_ts_from_mboc=approved["msku_change_ts_from_mboc"]
            ),
            partner_mapping_moderation=DTC.PartnerMappingModeration(
                partner_decision=DTC.PartnerDecision(
                    market_sku_id=partner_mm["partner_decision"]["market_sku_id"],
                    value=partner_mm["partner_decision"]["value"],
                    meta=DTC.UpdateMeta(
                        timestamp=partner_mm["partner_decision"]["ts"] if "ts" in partner_mm["partner_decision"] else create_pb_timestamp(10)
                    )
                ),
                remapping_result=DTC.RemappingResult(
                    value=partner_mm["remapping_result"]["value"]
                )
            )
        ).SerializeToString()
    } for offer_id, approved, partner_mm in [
        (
            'MMOfferWithMMDisableTimeToEnable',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(int(time.time())-60*60*3)},  # > mapping_moderation_timeout_hours
            {
                "partner_decision": {"market_sku_id": 2, "value": 2},  # 1 approve;  2 deny
                "remapping_result": {"value": 1}  # 1 not checked;  2 still approved
            }
        ),
        (
            'MMOfferWithMMDisableButCantEnable',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(int(time.time()))},
            {
                "partner_decision": {"market_sku_id": 2, "value": 2},
                "remapping_result": {"value": 1}
            }
        ),
        (
            'MMOfferWithMMDisableShouldEnableByRemappingResult',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(10)},
            {
                "partner_decision": {"market_sku_id": 100, "value": 2},
                "remapping_result": {"value": 2}
            }
        ),
        (
            'MMOfferWithNoMMDisable',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(10)},
            {
                "partner_decision": {"market_sku_id": 2, "value": 2},
                "remapping_result": {"value": 1}
            }
        ),
        (
            'MMOfferWithMMDisable_RightNowPartnerApprovedNewMapping_CantEnable',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(10)},
            {
                "partner_decision": {"market_sku_id": 100, "value": 1, "ts": create_pb_timestamp(int(time.time()))},
                "remapping_result": {"value": 1}
            }
        ),
        (
            'MMOfferWithMMDisable_LongTimeAgoPartnerApprovedNewMapping_ShouldEnable',
            {"market_sku_id": 100, "msku_change_ts_from_mboc": create_pb_timestamp(10)},
            {
                "partner_decision": {"market_sku_id": 100, "value": 1, "ts": create_pb_timestamp(10)},
                "remapping_result": {"value": 1}
            }
        ),
    ]
]

# business_id, msku_id, category_id, shop_id, program_type, title, version_id
OFFERS_FOR_DEEPMIND =[
    (3001, 4001, 5001, 6001, 1, "Title-1", 7001),  # Оффер должен попасть в выгрузку РАЗУМа
    (3002, None, 5002, 6002, 1, "Title-2", 7002),  # Не должен попасть в выгрузку, т.к. msku_id=None
    (3003, 4003, None, 6003, 1, "Title-3", 7003),  # Не должен попасть в выгрузку, т.к. category_id=None
    (3004, 4004, 5004, 6004, 2, "Title-4", 7004),  # Не должен попасть в выгрузку, т.к. program_type != 1(fulfillment) и != 4(crossdock)
    (3005, 4005, 5005, 6005, 4, "Title-5", 7005),  # Оффер должен попасть в выгрузку РАЗУМа
]

BASIC_OFFERS_FOR_DEEPMIND = [
    {
        'business_id': business_id,
        'shop_sku': 'OfferForTestingDeepmindDump',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=business_id,
                offer_id='OfferForTestingDeepmindDump',
                feed_id=1000,
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_sku_id=msku_id,
                        market_category_id=category_id
                    )
                ),
                partner=DTC.PartnerContent(
                    actual=DTC.ProcessedSpecification(
                        title=DTC.StringValue(
                            value=title
                        )
                    )
                )
            ),
            status=DTC.OfferStatus(
                version=DTC.VersionStatus(
                    actual_content_version=DTC.VersionCounter(
                        counter=version_id
                    )
                )
            )
        ).SerializeToString(),
    } for business_id, msku_id, category_id, _, _, title, version_id in OFFERS_FOR_DEEPMIND
]

BASIC_OFFERS_FOR_INVISIBLE = [
    {
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'InvisibleOffer' + str(color),
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer' + str(color),
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            invisible=DTC.Flag(
                flag=True,
                meta=create_update_meta(10)
            ),
        ).SerializeToString(),
        'meta': create_meta(10, scope=DTC.BASIC).SerializeToString(),
    } for color in [DTC.BLUE, DTC.WHITE]
]

SERVICE_OFFERS = [
    {
        'shop_id': shop_id,
        'business_id': business_id,
        'shop_sku': 'T600',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id='T600',
            warehouse_id=0,
            shop_id=shop_id,
        ).SerializeToString(),
        'bids': DTC.OfferBids(
            fee=DTC.Ui32Value(
                meta=create_update_meta(10),
                value=4,
            ),
            amore_data=DTC.AmoreDataValue(
                value="amore_data".encode(),
                meta=DTC.UpdateMeta(
                    timestamp=create_pb_timestamp(1)
                )
            )
        ).SerializeToString() if bids_data is True else None,
        'price': DTC.OfferPrice(
            price_by_warehouse={
                (shop_id + 1) % 2: DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=20),
                    meta=create_update_meta(10, removed=bool(shop_id % 2))
                ),
            },
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=2,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, color, vertical_approved_flag=create_flag(flag=vertical_approved), sortdc_user=sortdc_user).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
        ).SerializeToString(),
    } for business_id, shop_id, color, vertical_approved, sortdc_user, bids_data in OFFERS_INFO if shop_id != 8 and color != DTC.DIRECT   # dsbs и direct оффер опишем ниже
]

SERVICE_OFFERS_VERTICAL = [
    {
        'business_id': 10802215,
        'shop_sku': 'VerticalDirect',
        'shop_id': 10806907,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802215,
            offer_id='VerticalDirect',
            shop_id=10806907
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_GOODS_ADS, scope=DTC.SERVICE, vertical_approved_flag=DTC.Flag(flag=True), sortdc_user=EUser.GOODS).SerializeToString(),
        'price': DEFAULT_PRICE.SerializeToString()
    },
    {
        'business_id': 10802216,
        'shop_sku': 'VerticalGoodsAds',
        'shop_id': 10806908,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802216,
            offer_id='VerticalGoodsAds',
            shop_id=10806908
        ).SerializeToString(),
        'meta': create_meta(10, DTC.VERTICAL_GOODS_ADS, scope=DTC.SERVICE, vertical_approved_flag=DTC.Flag(flag=True), sortdc_user=EUser.GOODS).SerializeToString(),
        'price': DEFAULT_PRICE.SerializeToString()
    },
]

SERVICE_OFFERS_DIRECT = [
    {
        'shop_id': shop_id,
        'business_id': shop_id,
        'shop_sku': 'T600',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=shop_id,
            offer_id='T600',
            warehouse_id=0,
            shop_id=shop_id,
            feed_id=1000
        ).SerializeToString(),
        'price': DTC.OfferPrice(
            price_by_warehouse={
                0: DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=20),
                    meta=create_update_meta(10, removed=True)
                ),
            },
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=2,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_SEARCH_SNIPPET_GALLERY).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
            version=DTC.VersionStatus(
                direct_search_snippet_moderation_subscription_version=DTC.VersionCounter(
                    counter=status_counter
                )
            ),
        ).SerializeToString(),
        'resolution': DTC.Resolution(
            direct=DTC.Verdicts(
                meta=create_update_meta(10),
                verdict_version=DTC.VersionCounter(
                    counter=2
                ),
                bigmod_verdict=BM.TDataCampVerdict(
                    Verdict=verdict,
                    Reasons=[1, 2, 3, 4, 5],
                    Flags=[6, 501, 502],
                    MinusRegions=[8, 9, 10],
                    Timestamp=90000
                )
            )
        ).SerializeToString()
    } for shop_id, status_counter, verdict in [
        (7, 2, 1),  # all good
        (9, 1, 1),  # wrong direct_search_snippet_moderation_subscription_version counter
        (10, 2, 0),  # not ok verdict
        (12, 1, 0)  # not ok, but is going to be exported: offer is not TGO
    ]
]

SERVICE_OFFERS_DIRECT_11 = [
    {
        'shop_id': 11,
        'business_id': 11,
        'shop_sku': 'T600',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=11,
            offer_id='T600',
            warehouse_id=0,
            shop_id=11,
            feed_id=1000
        ).SerializeToString(),
        'price': DTC.OfferPrice(
            price_by_warehouse={
                0: DTC.PriceBundle(
                    binary_price=DTC.PriceExpression(price=20),
                    meta=create_update_meta(10, removed=True)
                ),
            },
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=2,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_SEARCH_SNIPPET_GALLERY).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
        ).SerializeToString()
    },
]

SERVICE_OFFERS_DSBS = [
    {
        'shop_id': 8,
        'business_id': 88,
        'shop_sku': 'T600',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=88,
            offer_id='T600',
            warehouse_id=0,
            shop_id=8,
            feed_id=1000
        ).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            is_dsbs=True,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString()
    }
]

SERVICE_OFFERS_FOR_PARTNER_DISABLES_CHECK = [
    # offer with fresh partner disable
    {
        'shop_id': 3,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=3,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish_by_partner=DTC.HIDDEN,
            disabled_by_partner_since_ts=Timestamp(seconds=NOW_TS.seconds),
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(NOW_TS.seconds, DTC.PUSH_PARTNER_FEED)
                ),
            ]
        ).SerializeToString()
    },
    # offer with old partner disable
    {
        'shop_id': 4,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=4,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish_by_partner=DTC.HIDDEN,
            disabled_by_partner_since_ts=Timestamp(seconds=OLD_TS.seconds),
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(OLD_TS.seconds, DTC.PUSH_PARTNER_FEED)
                ),
            ]
        ).SerializeToString(),
    },
    # offer with old and fresh partner disables
    {
        'shop_id': 5,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=5,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish_by_partner=DTC.HIDDEN,
            disabled_by_partner_since_ts=Timestamp(seconds=OLD_TS.seconds),
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(NOW_TS.seconds, DTC.PUSH_PARTNER_OFFICE)
                ),
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(OLD_TS.seconds, DTC.PUSH_PARTNER_FEED)
                ),
            ]
        ).SerializeToString(),
    },
    # offer with old not partner disable
    {
        'shop_id': 6,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=6,
            feed_id=1000
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            publish_by_partner=DTC.AVAILABLE,
            # в актуальной части описан disable c MARKET_IDX
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
    },
]

SERVICE_OFFERS_FOR_DEEPMIND = [
    {
        'shop_id': shop_id,
        'business_id': business_id,
        'shop_sku': 'OfferForTestingDeepmindDump',
        'identifiers': DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id='OfferForTestingDeepmindDump',
            shop_id=shop_id,
            feed_id=1000
        ).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            program_type=program_type
        ).SerializeToString(),
        'content_status': DTC.ContentStatus(
            content_system_status=DTC.ContentSystemStatus(
                offer_acceptance_status=DTC.OfferAcceptanceStatus.ACCEPTANCE_STATUS_OK
            )
        ).SerializeToString(),
    } for business_id, _, _, shop_id, program_type, _, _ in OFFERS_FOR_DEEPMIND
]

SERVICE_OFFERS_FOR_INVISIBLE = [
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'InvisibleOffer' + str(color),
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer' + str(color),
            warehouse_id=0,
            shop_id=SHOP_FOR_INVISIBLE,
        ).SerializeToString(),
        'price': DEFAULT_PRICE.SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            partner_stocks=DTC.OfferStocks(
                count=2,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, color, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.AVAILABLE,
        ).SerializeToString(),
    } for color in [DTC.BLUE, DTC.WHITE]
]


def create_service_offer(shop_id, business_id, offer_id, feed_id=1021, disabled_flag=None, disabled_flag_source=DTC.MARKET_STOCK, parner_info_ignore_stocks=False):
    offer = {
        'shop_id': shop_id,
        'business_id': business_id,
        'shop_sku': offer_id,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id=offer_id,
            warehouse_id=0,
            shop_id=shop_id,
            feed_id=feed_id
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString()
    }

    offer['partner_info'] = DTC.PartnerInfo(
        is_ignore_stocks=parner_info_ignore_stocks
    ).SerializeToString()

    if disabled_flag is not None:
        offer['status'] = DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=disabled_flag,
                    meta=create_update_meta(NOW_TS.seconds, disabled_flag_source)
                ),
            ]).SerializeToString()
    return offer


SERVICE_OFFERS_FOR_IGNORE_STOCK_CHECK = [
    # offer with default status
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks'),
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks_NotDisabled', disabled_flag=False),
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks_Disabled', disabled_flag=True),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks', parner_info_ignore_stocks=True),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks_NotDisabled', disabled_flag=False, parner_info_ignore_stocks=True),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks_Disabled', disabled_flag=True, parner_info_ignore_stocks=True),
]

SERVICE_OFFERS_FOR_SKIP_INVALID_OFFERS = [
    {
        'business_id': 4,
        'shop_sku': 'OfferWithInvalidWareMd5',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithInvalidWareMd5',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithValidWareMd5',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithValidWareMd5',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutTitle',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutTitle',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutTitle',
        'shop_id': 401,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutTitle',
            shop_id=401,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'InvalidBlueOffer',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='InvalidBlueOffer',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'BlueFulfillemtOfferWithoutActual',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='BlueFulfillemtOfferWithoutActual',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutActual',
        'shop_id': 500,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutActual',
            shop_id=500,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
    },
]

SERVICE_OFFERS_REMOVED = [
    {
        'shop_id': shop_id,
        'business_id': 4,
        'shop_sku': offer_id,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
            shop_id=shop_id,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'status': status
    } for offer_id, shop_id, status in [
        ('RemovedOfferByBasic', 400, None),
        ('RemovedOfferByService', 400, DTC.OfferStatus(removed=DTC.Flag(flag=True)).SerializeToString()),
        ('RemovedOfferByService', 401, None)
    ]
]

SERVICE_OFFERS_FOR_PERESORT = [
    {
        'shop_id': shop_id,
        'business_id': 4,
        'shop_sku': offer_id,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
            shop_id=shop_id,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'price': DTC.OfferPrice(
            basic=DTC.PriceBundle(
                binary_price=DTC.PriceExpression(
                    price=price * 10000000,
                ),
            )
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=disabled,
                    meta=create_update_meta(NOW_TS.seconds, DTC.MARKET_IDX_PERESORT)
                ),
            ]
        ).SerializeToString() if disabled is not None else None,
        'resolution': DTC.Resolution(
            by_source=[
                DTC.Verdicts(
                    meta=create_update_meta(NOW_TS.seconds, DTC.MARKET_IDX_PERESORT),
                    verdict=[DTC.Verdict(results=[DTC.ValidationResult(
                        is_banned=resolution,
                        messages=[DTC.Explanation(code='49a')]
                    )])]
                )
            ]
        ).SerializeToString() if resolution is not None else None,
    } for offer_id, shop_id, price, disabled, resolution in [
        ('OfferSetPeresort', 400, 1000, None, None),
        ('OfferSetPeresort', 401, 3000, None, None),
        ('OfferCantSetPeresortBecauseItIsTooLate', 400, 1000, None, None),
        ('OfferCantSetPeresortBecauseItIsTooLate', 401, 3000, None, None),
        ('OfferCantSetPeresortBecauseOfEmptyContentFlap', 400, 1000, None, None),
        ('OfferCantSetPeresortBecauseOfEmptyContentFlap', 401, 3000, None, None),
        ('OfferUnsetPeresort', 400, 1000, True, True),
        ('OfferUnsetPeresort', 401, 1300, True, True),
        ('OfferUnsetPeresortLonely', 400, 1000, True, True),
        ('OfferDiffPeresort', 400, 1000, True, True),
        ('OfferDiffPeresort', 401, 3000, True, True),
    ]
]

SERVICE_OFFERS_FOR_VERDICTS = [
    {
        'shop_id': shop_id,
        'business_id': 4,
        'shop_sku': offer_id,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
            shop_id=shop_id,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE if shop_id != SHOP_ID_DBS else DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            is_dsbs=(True if shop_id == SHOP_ID_DBS else False),
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            united_catalog=DTC.Flag(flag=True),
            disabled=[
                DTC.Flag(
                    flag=disabled,
                    meta=create_update_meta(NOW_TS.seconds - 20, source)
                ),
            ] if disabled is not None else None
        ).SerializeToString(),
    } for offer_id, shop_id, disabled, source in [
        ('OfferSetMdmVerdict', 400, None, DTC.MARKET_MDM),
        ('OfferSetMdmVerdictForDBS', SHOP_ID_DBS, None, DTC.MARKET_MDM),
        ('OfferSameMdmVerdict', 400, True, DTC.MARKET_MDM),
        ('OfferSetMbiMigratorVerdict', 400, None, DTC.MARKET_MBI_MIGRATOR),
        ('OfferSameMbiMigratorVerdict', 400, True, DTC.MARKET_MBI_MIGRATOR),
        ('OfferSetGutginVerdict', 400, None, DTC.MARKET_GUTGIN),
        ('OfferSameGutginVerdict', 400, True, DTC.MARKET_GUTGIN),
    ]
]

SERVICE_OFFERS_FOR_INTEGRAL_STATUS = [
    {
        'business_id': 4,
        'shop_id': 5,
        'warehouse_id': 0,
        'shop_sku': 'OfferForIntegralStatus',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            shop_id=5,
            warehouse_id=0,
            offer_id='OfferForIntegralStatus',
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'content_status': DTC.ContentStatus(
            content_system_status=DTC.ContentSystemStatus(
                service_offer_state=DTC.CONTENT_STATE_READY
            )
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            version=DTC.VersionStatus(
                original_partner_data_version=DTC.VersionCounter(
                    counter=22
                ),
                actual_content_version=DTC.VersionCounter(
                    counter=12
                )
            )
        ).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            is_disabled=True
        ).SerializeToString()
    },
]

SERVICE_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK = [
    {
        'shop_id': 10806905,
        'business_id': 10802213,
        'shop_sku': 'OfferForClassififerGoodIdCheck',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802213,
            shop_id=10806905,
            offer_id='OfferForClassififerGoodIdCheck',
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
    }
]

SERVICE_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK = [
    {
        'shop_id': 10806906,
        'business_id': 10802214,
        'shop_sku': 'OfferForClassififerMagicIdCheck',
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802214,
            shop_id=10806906,
            offer_id='OfferForClassififerMagicIdCheck',
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
    }
]

SERVICE_OFFERS_FOR_MAPPING_MODERATION = [
    {
        'shop_id': MM_SHOP_ID,
        'business_id': MM_BUSINESS_ID,
        'shop_sku': offer['offer_id'],
        'identifiers': DTC.OfferIdentifiers(
            business_id=MM_BUSINESS_ID,
            shop_id=MM_SHOP_ID,
            offer_id=offer['offer_id'],
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=offer['disabled'],
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX_MAPPING_MODERATION)
                ),
            ]
        ).SerializeToString(),
    } for offer in [
        {'offer_id': 'MMOfferWithMMDisableTimeToEnable', 'disabled': True},
        {'offer_id': 'MMOfferWithMMDisableButCantEnable', 'disabled': True},
        {'offer_id': 'MMOfferWithMMDisableShouldEnableByRemappingResult', 'disabled': True},
        {'offer_id': 'MMOfferWithNoMMDisable', 'disabled': False},
        {'offer_id': 'MMOfferWithMMDisable_RightNowPartnerApprovedNewMapping_CantEnable', 'disabled': True},
        {'offer_id': 'MMOfferWithMMDisable_LongTimeAgoPartnerApprovedNewMapping_ShouldEnable', 'disabled': True},
    ]
]

ACTUAL_SERVICE_OFFERS = [
    {
        'shop_id': shop_id,
        'business_id': business_id,
        'shop_sku': 'T600',
        'warehouse_id': (shop_id + 1) % 2,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id='T600',
            warehouse_id=(shop_id + 1) % 2,
            outlet_id=0,
            shop_id=shop_id,
            feed_id=1000
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            market_stocks=DTC.OfferStocks(
                count=4,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'delivery': DTC.OfferDelivery(
            specific=DTC.SpecificDeliveryOptions(
                delivery_currency='1',
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, color).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    } for business_id, shop_id, color, _, _, _ in OFFERS_INFO if shop_id != 8  # dsbs оффер опишем ниже
]

ACTUAL_SERVICE_OFFERS_VERTICAL = [
    {
        'business_id': 10802215,
        'shop_sku': 'VerticalDirect',
        'shop_id': 10806907,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802215,
            offer_id='VerticalDirect',
            shop_id=10806907
        ).SerializeToString(),
        'meta': create_meta(10, DTC.DIRECT_GOODS_ADS, scope=DTC.SERVICE, vertical_approved_flag=DTC.Flag(flag=True), sortdc_user=EUser.GOODS).SerializeToString(),
    },
    {
        'business_id': 10802216,
        'shop_sku': 'VerticalGoodsAds',
        'shop_id': 10806908,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802216,
            offer_id='VerticalGoodsAds',
            shop_id=10806908
        ).SerializeToString(),
        'meta': create_meta(10, DTC.VERTICAL_GOODS_ADS, scope=DTC.SERVICE, vertical_approved_flag=DTC.Flag(flag=True), sortdc_user=EUser.GOODS).SerializeToString(),
    },
    {
        'business_id': 10802217,
        'shop_sku': 'VerticalGoodsAds',
        'shop_id': 10806909,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802217,
            offer_id='VerticalGoodsAds',
            shop_id=10806909
        ).SerializeToString(),
        'meta': create_meta(10, DTC.VERTICAL_GOODS_ADS, scope=DTC.SERVICE, vertical_approved_flag=DTC.Flag(flag=True), sortdc_user=EUser.CBIR).SerializeToString(),
    },
    {  # SortDC Stub
        'business_id': 10802218,
        'shop_sku': 'VerticalGoodsAds',
        'shop_id': 10806910,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802217,
            offer_id='VerticalGoodsAds',
            shop_id=10806909
        ).SerializeToString(),
        'tech_info': DTC.OfferTechInfo(
            last_parsing=DTC.ParserTrace(
                start_parsing=create_pb_timestamp(10)
            )
        ).SerializeToString(),
        'meta': message_from_data({
            'ts_created': create_pb_timestamp(10).ToJsonString(),
            'rgb': DTC.VERTICAL_GOODS_ADS,
            'vertical_approved_flag': MessageToDict(DTC.Flag(flag=True)),
            'sortdc_context': {
                'export_items': [{
                    'offer_type': DTC.TSortDCExportContext.TExportItem.EOfferType.FEED_OFFER,
                    'user': EUser.GOODS
                }]
            },
        }, DTC.OfferMeta()).SerializeToString()
    },
]

ACTUAL_SERVICE_OFFERS_FOR_SKIP_INVALID_OFFERS = [
    {
        'business_id': 4,
        'shop_sku': 'OfferWithInvalidWareMd5',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithInvalidWareMd5',
            shop_id=400,
            warehouse_id=145,
            extra=DTC.OfferExtraIdentifiers(
                ware_md5='asd'
            )
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithValidWareMd5',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithValidWareMd5',
            shop_id=400,
            warehouse_id=145,
            extra=DTC.OfferExtraIdentifiers(
                ware_md5='Y3eCeETbMFCS38cAcMg4FA'
            )
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutTitle',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutTitle',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'OfferWithoutTitle',
        'shop_id': 401,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferWithoutTitle',
            shop_id=401,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'InvalidBlueOffer',
        'shop_id': 400,
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='InvalidBlueOffer',
            shop_id=400,
            warehouse_id=0,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            market_stocks=DTC.OfferStocks(
                count=4,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
    },
    {
        'business_id': 4,
        'shop_sku': 'InvalidBlueOffer',
        'shop_id': 400,
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='InvalidBlueOffer',
            shop_id=400,
            warehouse_id=145,
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
        'delivery': DTC.OfferDelivery(
            specific=DTC.SpecificDeliveryOptions(
                delivery_currency='1',
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
    },
]

ACTUAL_SERVICE_OFFERS_REMOVED = [
    {
        'shop_id': shop_id,
        'business_id': 4,
        'shop_sku': offer_id,
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
            warehouse_id=145,
            shop_id=shop_id,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    } for offer_id, shop_id in [
        ('RemovedOfferByBasic', 400),
        ('RemovedOfferByService', 400),
        ('RemovedOfferByService', 401)
    ]
]

ACTUAL_SERVICE_OFFERS_FOR_PERESORT = [
    {
        'shop_id': shop_id,
        'business_id': 4,
        'shop_sku': offer_id,
        'warehouse_id': 145,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id=offer_id,
            warehouse_id=145,
            shop_id=shop_id,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
    } for offer_id, shop_id in [
        ('OfferSetPeresort', 400),
        ('OfferSetPeresort', 401),
        ('OfferCantSetPeresortBecauseItIsTooLate', 400),
        ('OfferCantSetPeresortBecauseItIsTooLate', 401),
        ('OfferCantSetPeresortBecauseOfEmptyContentFlap', 400),
        ('OfferCantSetPeresortBecauseOfEmptyContentFlap', 401),
        ('OfferUnsetPeresort', 400),
        ('OfferUnsetPeresort', 401),
        ('OfferUnsetPeresortLonely', 400),
        ('OfferDiffPeresort', 400),
        ('OfferDiffPeresort', 401),
    ]
]

ACTUAL_SERVICE_OFFERS_FOR_PARTNER_DISABLES_CHECK = [
    # offer with fresh partner disable
    {
        'shop_id': 3,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=3,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    },
    # offer with old partner disable
    {
        'shop_id': 4,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=4,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    },
    # offer with old and fresh partner disables
    {
        'shop_id': 5,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=5,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    },
    # offer with old not partner disable
    {
        'shop_id': 6,
        'business_id': 3,
        'shop_sku': 'OfferForTestingPartnerDisables',
        'warehouse_id': 0,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=3,
            offer_id='OfferForTestingPartnerDisables',
            warehouse_id=0,
            shop_id=6,
            feed_id=1000
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            publish=DTC.HIDDEN,
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE).SerializeToString(),
    },
]

ACTUAL_SERVICE_OFFERS_FOR_IGNORE_STOCK_CHECK = [
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks'),
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks_NotDisabled', disabled_flag=False),
    create_service_offer(shop_id=21, business_id=21, offer_id='OfferForTestingIgnoreStocks_Disabled', disabled_flag=True),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks'),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks_NotDisabled', disabled_flag=False),
    create_service_offer(shop_id=22, business_id=22, offer_id='OfferForTestingIgnoreStocks_Disabled', disabled_flag=True),
]

ACTUAL_SERVICE_OFFERS_DSBS = [
    {
        'shop_id': 8,
        'business_id': 88,
        'shop_sku': 'T600',
        'warehouse_id': 1,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=88,
            offer_id='T600',
            warehouse_id=1,
            shop_id=8,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            is_dsbs=True,
        ).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=True,
                    meta=create_update_meta(NOW_TS.seconds, DTC.MARKET_IDX)
                ),
            ],
            ready_for_publication=DTC.ReadinessForPublicationStatus(
                value=DTC.ReadinessForPublicationStatus.NOT_READY
            ),
        ).SerializeToString(),
        'resolution': DTC.Resolution(
            by_source=[
                DTC.Verdicts(
                    meta=create_update_meta(NOW_TS.seconds, DTC.MARKET_IDX),
                    verdict=[
                        DTC.Verdict(
                            results=[
                                DTC.ValidationResult(
                                    is_banned=True,
                                    messages=[
                                        DTC.Explanation(
                                            code='49r',
                                            level=DTC.Explanation.ERROR,
                                        )
                                    ]
                                )
                            ]
                        )
                    ]
                )
            ]
        ).SerializeToString(),
    },
    {
        'shop_id': SHOP_ID_DBS,
        'business_id': 4,
        'shop_sku': 'OfferSetMdmVerdictForDBS',
        'warehouse_id': 1,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            offer_id='OfferSetMdmVerdictForDBS',
            warehouse_id=1,
            shop_id=SHOP_ID_DBS,
            feed_id=1000
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
        'partner_info': DTC.PartnerInfo(
            is_dsbs=True,
        ).SerializeToString()
    }
]

ACTUAL_SERVICE_OFFERS_FOR_INTEGRAL_STATUS = [
    {
        'business_id': 4,
        'shop_id': 5,
        'warehouse_id': 145,
        'shop_sku': 'OfferForIntegralStatus',
        'identifiers': DTC.OfferIdentifiers(
            business_id=4,
            shop_id=5,
            warehouse_id=145,
            offer_id='OfferForIntegralStatus',
            feed_id=111
        ).SerializeToString(),
        'meta': create_meta(10, DTC.BLUE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            ready_for_publication=DTC.ReadinessForPublicationStatus(
                value=DTC.ReadinessForPublicationStatus.READY
            ),
            publication=DTC.PublicationStatus(
                value=DTC.PublicationStatus.PUBLISHED
            ),
            version=DTC.VersionStatus(
                actual_content_version=DTC.VersionCounter(
                    counter=12
                )
            )
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            market_stocks=DTC.OfferStocks(
                count=10
            )
        ).SerializeToString(),
        'tech_info': DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                original_partner_data_version=DTC.VersionCounter(
                    counter=22
                )
            )
        ).SerializeToString()
    }
]

ACTUAL_SERVICE_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK = [
    {
        'business_id': 10802213,
        'shop_sku': 'OfferForClassififerGoodIdCheck',
        'shop_id': 10806905,
        'warehouse_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802213,
            shop_id=10806905,
            warehouse_id=0,
            offer_id='OfferForClassififerGoodIdCheck',
            extra=DTC.OfferExtraIdentifiers(
                classifier_good_id='2bed6528e3d2de88c3f7ba9e606f99be',
            ),
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    }
]

ACTUAL_SERVICE_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK = [
    {
        'business_id': 10802214,
        'shop_sku': 'OfferForClassififerMagicIdCheck',
        'shop_id': 10806906,
        'warehouse_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=10802214,
            shop_id=10806906,
            warehouse_id=0,
            offer_id='OfferForClassififerMagicIdCheck',
            extra=DTC.OfferExtraIdentifiers(
                classifier_magic_id2='5ccbc240473659cd1df2d5ed5bc8c1c0',
            ),
        ).SerializeToString(),
        'meta': create_meta(10, DTC.WHITE, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    }
]

ACTUAL_SERVICE_OFFERS_FOR_INVISIBLE = [
    {
        'shop_id': SHOP_FOR_INVISIBLE,
        'business_id': BUSINESS_FOR_INVISIBLE,
        'shop_sku': 'WhiteInvisibleOffer',
        'warehouse_id': (SHOP_FOR_INVISIBLE + 1) % 2,
        'outlet_id': 0,
        'identifiers': DTC.OfferIdentifiers(
            business_id=BUSINESS_FOR_INVISIBLE,
            offer_id='InvisibleOffer' + str(color),
            warehouse_id=(SHOP_FOR_INVISIBLE + 1) % 2,
            outlet_id=0,
            shop_id=SHOP_FOR_INVISIBLE,
        ).SerializeToString(),
        'stock_info': DTC.OfferStockInfo(
            market_stocks=DTC.OfferStocks(
                count=4,
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'delivery': DTC.OfferDelivery(
            specific=DTC.SpecificDeliveryOptions(
                delivery_currency='1',
                meta=create_update_meta(10)
            )
        ).SerializeToString(),
        'meta': create_meta(10, color, scope=DTC.SERVICE).SerializeToString(),
        'status': DTC.OfferStatus(
            disabled=[
                DTC.Flag(
                    flag=False,
                    meta=create_update_meta(OLD_TS.seconds, DTC.MARKET_IDX)
                ),
            ]
        ).SerializeToString(),
    } for color in [DTC.BLUE, DTC.WHITE]
]


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp/united'
            },
            'routines': {
                'enable_united_datacamp_dumper': True,
                'days_number_to_take_disabled_offer_in_index': 5,
                'enable_united_datacamp_export_dumper': True,
                'peresort_enabled': True,
                'enable_verditcs_applier': True,
                'drop_offers_without_title_in_dumper': True,
                'enable_mapping_moderation_force_mine_applier': True,
                'mapping_moderation_timeout_hours': 2,
                'enable_dump_verdicts_hash': True,
                'enable_dump_amore_out': True,
                'enable_dump_deepmind_out': True
            },
            'yt': {
                'white_out': 'white_out',
                'blue_out': 'blue_out',
                'turbo_out': 'turbo_out',
                'blue_turbo_out': 'blue_turbo_out',
                'lavka_out': 'lavka_out',
                'eda_out': 'eda_out',
                'amore_out': 'amore_out',
                'deepmind_out': 'deepmind_out',
                'dumper_log': 'dumper_log',
                'vertical_shop_id': 'vertical_shop_id',
                'verdicts_hash_out': 'verdicts',
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'export_dir': '//home/ecom/offers',
                'key_sample_period': 1,
                'key_shard_size': 2,
                'key_shards_out': 'key_shards'
            }
        })
    return config


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=PARTNERS)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, config):
    data = BASIC_OFFERS +\
        BASIC_OFFERS_FOR_PARTNER_DISABLES_CHECK +\
        BASIC_OFFERS_FOR_SKIP_INVALID_OFFERS +\
        BASIC_OFFERS_REMOVED +\
        BASIC_OFFERS_FOR_PERESORT +\
        BASIC_OFFERS_FOR_VERDICTS +\
        BASIC_OFFERS_FOR_PARTNER_IGNORE_STOCK_CHECK +\
        BASIC_OFFERS_FOR_INTEGRAL_STATUS + \
        BASIC_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK + \
        BASIC_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK + \
        BASIC_OFFERS_VERICAL + \
        BASIC_OFFERS_FOR_MAPPING_MODERATION + \
        BASIC_OFFERS_FOR_DEEPMIND + \
        BASIC_OFFERS_FOR_INVISIBLE
    return DataCampBasicOffersTable(yt_server, config.yt_basic_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def service_offers_table(yt_server, config):
    data = SERVICE_OFFERS +\
        SERVICE_OFFERS_FOR_PARTNER_DISABLES_CHECK +\
        SERVICE_OFFERS_FOR_SKIP_INVALID_OFFERS +\
        SERVICE_OFFERS_REMOVED +\
        SERVICE_OFFERS_FOR_PERESORT +\
        SERVICE_OFFERS_FOR_VERDICTS +\
        SERVICE_OFFERS_DSBS +\
        SERVICE_OFFERS_DIRECT_11 +\
        SERVICE_OFFERS_DIRECT +\
        SERVICE_OFFERS_FOR_IGNORE_STOCK_CHECK +\
        SERVICE_OFFERS_FOR_INTEGRAL_STATUS + \
        SERVICE_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK + \
        SERVICE_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK + \
        SERVICE_OFFERS_VERTICAL + \
        SERVICE_OFFERS_FOR_MAPPING_MODERATION + \
        SERVICE_OFFERS_FOR_DEEPMIND + \
        SERVICE_OFFERS_FOR_INVISIBLE
    return DataCampServiceOffersTable(yt_server, config.yt_service_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def actual_service_offers_table(yt_server, config):
    data = ACTUAL_SERVICE_OFFERS + \
        ACTUAL_SERVICE_OFFERS_FOR_SKIP_INVALID_OFFERS + \
        ACTUAL_SERVICE_OFFERS_REMOVED + \
        ACTUAL_SERVICE_OFFERS_FOR_PERESORT + \
        ACTUAL_SERVICE_OFFERS_FOR_PARTNER_DISABLES_CHECK + \
        ACTUAL_SERVICE_OFFERS_DSBS +\
        ACTUAL_SERVICE_OFFERS_FOR_IGNORE_STOCK_CHECK +\
        ACTUAL_SERVICE_OFFERS_FOR_INTEGRAL_STATUS + \
        ACTUAL_SERVICE_OFFERS_FOR_CLASSIFIER_GOOD_ID_CHECK + \
        ACTUAL_SERVICE_OFFERS_FOR_CLASSIFIER_MAGIC_ID_CHECK + \
        ACTUAL_SERVICE_OFFERS_VERTICAL + \
        ACTUAL_SERVICE_OFFERS_FOR_INVISIBLE
    return DataCampServiceOffersTable(yt_server, config.yt_actual_service_offers_tablepath, data=data)


@pytest.fixture(scope='module')
def history(yt_server, config):
    yt_client = yt_server.get_yt_client()
    for output_dir in [config.yt_white_output_dir, config.yt_blue_output_dir, config.yt_turbo_output_dir]:
        for i in range(7):
            yt_client.create('table', yt.ypath_join(output_dir, '20190101_010' + str(i)), recursive=True)
            yt_client.create('table', yt.ypath_join(output_dir, 'stats', '20190101_010' + str(i)), recursive=True)
            yt_client.create('table', yt.ypath_join(output_dir, 'removed', '20190101_010' + str(i)), recursive=True)


@pytest.yield_fixture(scope='module')
def routines(
    yt_server,
    config,
    partners_table,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    history
):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'config': config,
    }
    with UnitedDatacampDumperEnv(yt_server, **resources) as routines_env:
        yield routines_env


@pytest.yield_fixture(scope='module')
def routines_http(
    yt_server,
    config,
    partners_table,
    basic_offers_table,
    service_offers_table,
    actual_service_offers_table,
    history
):
    resources = {
        'partners_table': partners_table,
        'basic_offers_table': basic_offers_table,
        'service_offers_table': service_offers_table,
        'actual_service_offers_table': actual_service_offers_table,
        'config': config,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.mark.parametrize(
    'output_dir, n, rgb, expected_offes_count',
    [
        ('//home/datacamp/united/white_out', 1, DTC.WHITE, 13),
        ('//home/datacamp/united/blue_out', 2, DTC.BLUE, 24),
    ]
)
def test_dumper(yt_server, routines, output_dir, n, rgb, expected_offes_count):
    yt_client = yt_server.get_yt_client()

    tables = yt_client.list(output_dir)
    assert_that(len(tables), equal_to(9))

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offes_count))

    assert_that(results, has_items(
        has_entries({
            'business_id': n,
            'offer_id': 'T600',
            'shop_id': n,
            'warehouse_id': (n + 1) % 2,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': n,
                    'offer_id': 'T600',
                    'shop_id': n,
                    'warehouse_id': (n + 1) % 2,
                },
                'meta': {
                    'rgb': rgb,
                    'ts_created': {
                        'seconds': 10
                    }
                },
                'content': {
                    'partner': {
                        'original': {
                            'name': {
                                'value': 'name',
                                'meta': {
                                    'timestamp': {
                                        'seconds': 10
                                    }
                                }
                            }
                        },
                        'actual': {
                            'title': {
                                'value': 'title',
                                'meta': {
                                    'timestamp': {
                                        'seconds': 10
                                    }
                                }
                            }
                        }
                    }
                },
                'status': {
                    'publish': DTC.HIDDEN
                },
                'stock_info': {
                    'partner_stocks': {
                        'count': 2
                    },
                    'market_stocks': {
                        'count': 4
                    },
                },
                'delivery': {
                    'specific': {
                        'delivery_currency': '1'
                    }
                },
                'price': {
                    'basic': {
                        'meta': {
                            'timestamp': {
                                'seconds': 10
                            }
                        },
                        'binary_price': {
                            'price': 10 if (n % 2) else 20
                        }
                    }
                }})
        }),
    ))


def test_turbo_dumper(yt_server, routines):
    output_dir = '//home/datacamp/united/turbo_out'
    expected_offes_count = 3
    yt_client = yt_server.get_yt_client()

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offes_count))

    for business_id, shop_id, offer_id, rgb in [(10802215, 10806907, 'VerticalDirect', DTC.DIRECT_GOODS_ADS),
                                                (10802216, 10806908, 'VerticalGoodsAds', DTC.VERTICAL_GOODS_ADS)]:
        assert_that(results, has_items(
            has_entries({
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
                'warehouse_id': 0,
                'offer': IsSerializedProtobuf(DTC.Offer, {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': offer_id,
                        'shop_id': shop_id,
                        'warehouse_id': 0,
                    },
                    'meta': {
                        'rgb': rgb,
                        'vertical_approved_flag': {
                            'flag': True
                        }
                    }
                })
            }),
        ))

    for business_id, shop_id, rgb in [(1, 1, DTC.WHITE)]:
        assert_that(results, has_items(
            has_entries({
                'business_id': business_id,
                'offer_id': 'T600',
                'shop_id': shop_id,
                'warehouse_id': (business_id + 1) % 2,
                'offer': IsSerializedProtobuf(DTC.Offer, {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': 'T600',
                        'shop_id': shop_id,
                        'warehouse_id': (business_id + 1) % 2,
                    },
                    'meta': {
                        'rgb': rgb,
                        'vertical_approved_flag': {
                            'flag': True
                        },
                        'ts_created': {
                            'seconds': 10
                        }
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'name',
                                    'meta': {
                                        'timestamp': {
                                            'seconds': 10
                                        }
                                    }
                                }
                            },
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': {
                                            'seconds': 10
                                        }
                                    }
                                }
                            }
                        }
                    },
                    'status': {
                        'publish': DTC.HIDDEN
                    },
                    'stock_info': {
                        'partner_stocks': {
                            'count': 2
                        },
                        'market_stocks': {
                            'count': 4
                        },
                    },
                    'delivery': {
                        'specific': {
                            'delivery_currency': '1'
                        }
                    },
                    'price': {
                        'basic': {
                            'meta': {
                                'timestamp': {
                                    'seconds': 10
                                }
                            },
                            'binary_price': {
                                'price': 10 if (business_id % 2) else 20
                            }
                        }
                    }})
            }),
        ))


def test_blue_turbo_dumper(yt_server, routines):
    output_dir = '//home/datacamp/united/blue_turbo_out'
    expected_offes_count = 1
    yt_client = yt_server.get_yt_client()

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offes_count))

    for business_id, shop_id, rgb in [(2, 2, DTC.BLUE)]:
        assert_that(results, has_items(
            has_entries({
                'business_id': business_id,
                'offer_id': 'T600',
                'shop_id': shop_id,
                'warehouse_id': (business_id + 1) % 2,
                'offer': IsSerializedProtobuf(DTC.Offer, {
                    'identifiers': {
                        'business_id': business_id,
                        'offer_id': 'T600',
                        'shop_id': shop_id,
                        'warehouse_id': (business_id + 1) % 2,
                    },
                    'meta': {
                        'rgb': rgb,
                        'vertical_approved_flag': {
                            'flag': True
                        },
                        'ts_created': {
                            'seconds': 10
                        }
                    },
                    'content': {
                        'partner': {
                            'original': {
                                'name': {
                                    'value': 'name',
                                    'meta': {
                                        'timestamp': {
                                            'seconds': 10
                                        }
                                    }
                                }
                            },
                            'actual': {
                                'title': {
                                    'value': 'title',
                                    'meta': {
                                        'timestamp': {
                                            'seconds': 10
                                        }
                                    }
                                }
                            }
                        }
                    },
                    'status': {
                        'publish': DTC.HIDDEN
                    },
                    'stock_info': {
                        'partner_stocks': {
                            'count': 2
                        },
                        'market_stocks': {
                            'count': 4
                        },
                    },
                    'delivery': {
                        'specific': {
                            'delivery_currency': '1'
                        }
                    },
                    'price': {
                        'basic': {
                            'meta': {
                                'timestamp': {
                                    'seconds': 10
                                }
                            },
                            'binary_price': {
                                'price': 10 if (business_id % 2) else 20
                            }
                        }
                    }})
            }),
        ))


@pytest.mark.parametrize(
    'output_dir, n, expected_offers_count',
    [
        ('//home/datacamp/united/white_out', 1, 9),
        ('//home/datacamp/united/blue_out', 2, 4),
    ]
)
def test_dumper_stats(yt_server, routines, output_dir, n, expected_offers_count):
    yt_client = yt_server.get_yt_client()
    output_dir = yt.ypath_join(output_dir, 'stats')
    tables = yt_client.list(output_dir)
    assert_that(len(tables), equal_to(7))

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offers_count))

    assert_that(results, has_items(
        has_entries({
            'business_id': n,
            'shop_id': n,
            'feed_stats': has_items(
                has_entries({
                    'key': 1000,
                    'value': IsSerializedProtobuf(Stats.FeedStats, {
                        'feed_id': 1000,
                        'offers_count': 1,
                        'error_offers': 0,
                        'valid_offers_with_stocks_and_sizes': 1
                    })
                })
            )
        })
    ))


def test_dump_by_http_request(routines_http, config):
    """
    Пример вызова http-ручки dump
    """
    response = routines_http.post('/dump?cluster={}'.format(
        config.yt_map_reduce_proxies[0],
    ))
    assert_that(response, HasStatus(200))


@pytest.mark.parametrize(
    'output_dir, business_id, rgb, expected_offers_count',
    [
        ('//home/datacamp/united/white_out', 3, DTC.WHITE, 13),
    ]
)
def test_not_take_in_dump_offers_which_are_disabled_by_partners_too_long(yt_server, routines, output_dir, business_id, rgb, expected_offers_count):
    """
    Проверяем, что в выгрузку не попадают оффера отключеные партнером очень давно
    (промежуток задается через конфиг)
    """
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offers_count))

    assert_that(results, HasDatacampYtUnitedOffersRows([
        {
            'business_id': business_id,
            'offer_id': 'OfferForTestingPartnerDisables',
            'shop_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'OfferForTestingPartnerDisables',
                    'shop_id': 3,
                },
                'meta': {
                    'rgb': rgb,
                    'ts_created': {
                        'seconds': 10
                    }
                },
                'status': {
                    'publish': DTC.HIDDEN,
                    'disabled': [
                        {
                            'flag': True,
                            'meta': {
                                'source': DTC.PUSH_PARTNER_FEED,
                                'timestamp': {
                                    'seconds': NOW_TS.seconds,
                                }
                            },
                        },
                    ]
                },
            }),
        },
        {
            'business_id': business_id,
            'offer_id': 'OfferForTestingPartnerDisables',
            'shop_id': 6,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'OfferForTestingPartnerDisables',
                    'shop_id': 6,
                },
                'meta': {
                    'rgb': rgb,
                    'ts_created': {
                        'seconds': 10
                    }
                },
                'status': {
                    'publish': DTC.HIDDEN,
                    'disabled': [
                        {
                            'flag': True,
                            'meta': {
                                'source': DTC.MARKET_IDX,
                                'timestamp': {
                                    'seconds': OLD_TS.seconds,
                                }
                            },
                        },
                    ]
                },
            }),
        }
    ]))
    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': business_id,
            'offer_id': 'OfferForTestingPartnerDisables',
            'shop_id': 4,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'OfferForTestingPartnerDisables',
                    'shop_id': 4,
                },
            }),
        },
        {
            'business_id': business_id,
            'offer_id': 'OfferForTestingPartnerDisables',
            'shop_id': 5,

            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'OfferForTestingPartnerDisables',
                    'shop_id': 5,
                },
            }),
        },
    ])))


def test_blue_fulfillemt_without_warehouse(yt_server, routines):
    """
    Проверяем, что в выгрузку попадают данные синих офферов без actual-части (для построения отчета по скрытым).
    Все остальные офферы без актуальной части в выгрузку не попадают, т.к. без этого в поколение могут попасть
    непомайненные офферы.
    """
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))

    assert_that(results, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'BlueFulfillemtOfferWithoutActual',
            'shop_id': 400,
            'warehouse_id': 0,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'BlueFulfillemtOfferWithoutActual',
                    'shop_id': 400,
                    'warehouse_id': 0,
                },
                'meta': {
                    'rgb': DTC.BLUE,
                },
            }),
        },
    ]))
    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'OfferWithoutActual',
            'shop_id': 500,
        },
    ])))


def test_invalid_ware_md5(yt_server, routines):
    """
    Проверяем, что в выгрузку не попадают данные маркетных офферов с битым ware_md5
    """
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))

    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'OfferWithInvalidWareMd5',
            'shop_id': 400,
        },
    ])))
    assert_that(results, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'OfferWithValidWareMd5',
            'shop_id': 400,
            'warehouse_id': 145,
        },
    ]))


def test_invalid_blue_skip(yt_server, routines):
    """
    Проверяем, что в выгрузку не попадают данные синих офферов actual таблицы, у которых не уазан склад
    """
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))

    assert_that(results, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'InvalidBlueOffer',
            'shop_id': 400,
            'warehouse_id': 145,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'InvalidBlueOffer',
                    'shop_id': 400,
                    'warehouse_id': 145,
                },
                'meta': {
                    'rgb': DTC.BLUE,
                },
                # Данные из некорректного оффера не попадают
                'stock_info': None,
                # Из корректного - попадают
                'delivery': {
                    'specific': {
                        'delivery_currency': '1'
                    }
                },
            }),
        },
    ]))
    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'InvalidBlueOffer',
            'shop_id': 400,
            'warehouse_id': 0,
        },
    ])))


def test_removed_offers(yt_server, routines):
    """
    Не берем в поколение офферы помеченные как удаленные:
      - если признак удаления в базовой, то не берем все сервисные
      - если удаление в сервисной, то не берем только помеченную сервсиную
    """
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'offer_id': 'RemovedOfferByBasic',
        },
    ])))
    assert_that(results, is_not(HasDatacampYtUnitedOffersRows([
        {
            'shop_id': 400,
            'offer_id': 'RemovedOfferByService',
        },
    ])))
    assert_that(results, HasDatacampYtUnitedOffersRows([
        {
            'shop_id': 401,
            'offer_id': 'RemovedOfferByService',
        },
    ]))


def test_removed_dump(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    output_dir = yt.ypath_join('//home/datacamp/united/blue_out/removed')
    tables = yt_client.list(output_dir)
    assert_that(len(tables), equal_to(7))

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(2))

    assert_that(results, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'RemovedOfferByBasic',
        'shop_id': 400,
        'warehouse_id': 145,
    }, {
        'business_id': 4,
        'offer_id': 'RemovedOfferByService',
        'shop_id': 400,
        'warehouse_id': 145,
    }]))


def test_export_dump_only_turbo_and_direct(yt_server, routines):
    yt_client = yt_server.get_yt_client()

    assert_that(yt_client.exists('//home/ecom/offers/turbo/recent'), equal_to(True))
    assert_that(yt_client.exists('//home/ecom/offers/direct/recent'), equal_to(True))
    assert_that(yt_client.exists('//home/ecom/offers/white/recent'), equal_to(False))
    assert_that(yt_client.exists('//home/ecom/offers/blue/recent'), equal_to(False))
    assert_that(yt_client.exists('//home/ecom/offers/lavka/recent'), equal_to(False))
    assert_that(yt_client.exists('//home/ecom/offers/eda/recent'), equal_to(False))


@pytest.mark.parametrize(
    'output_path',
    [
        '//home/datacamp/united/white_out/recent',
        '//home/datacamp/united/blue_out/recent',
        '//home/datacamp/united/direct_out/recent',
        '//home/datacamp/united/turbo_out/recent',
        '//home/datacamp/united/lavka_out/recent',
        '//home/datacamp/united/eda_out/recent',
    ]
)
def test_dumper_schema(yt_server, routines, output_path):
    yt_client = yt_server.get_yt_client()
    real_path = yt_client.get(output_path + '/@path')
    schema = yt_client.get(real_path + '/@schema')

    assert_that(
        schema,
        has_item(
            has_entries(
                {
                    'name': 'offer',
                    'type_v3': has_entry('item', 'string')
                }
            )
        )
    )


@pytest.mark.parametrize(
    'output_path',
    [
        '//home/datacamp/united/white_out/recent',
        '//home/datacamp/united/blue_out/recent',
        '//home/datacamp/united/direct_out/recent',
        '//home/datacamp/united/lavka_out/recent',
        '//home/datacamp/united/eda_out/recent',
    ]
)
def test_dumper_yql_schema(yt_server, routines, output_path):
    yt_client = yt_server.get_yt_client()
    attr = yt_client.get(output_path + '/@_yql_proto_field_offer')

    assert_that(attr, is_not(None))


def test_export_dumper(yt_server, routines):
    yt_client = yt_server.get_yt_client()

    results = list(yt_client.read_table('//home/ecom/offers/direct/recent'))
    assert_that(len(results), equal_to(2))


def test_dump_by_http_request_with_custom_out_home(routines_http, config, yt_server):
    """
    Тест http-ручки dump с кастомным yt-путем
    """
    yt_out_home = '//tmp/united/dump'

    response = routines_http.post('/dump?cluster={}&color={}&out_home={}'.format(
        config.yt_map_reduce_proxies[0],
        'white',
        yt_out_home
    ))

    assert_that(response, HasStatus(200))

    expected_white_out = '{}/white_out/recent'.format(yt_out_home)
    assert yt_server.get_yt_client().exists(expected_white_out)


@pytest.mark.parametrize(
    'output_dir, id, rgb',
    [
        ('//home/datacamp/united/lavka_out', 13, DTC.LAVKA),
        ('//home/datacamp/united/eda_out', 14, DTC.EDA),
    ]
)
def test_foodtech_dumper(yt_server, routines, output_dir, id, rgb):
    expected_offes_count = 1
    yt_client = yt_server.get_yt_client()

    results = list(yt_client.read_table(yt.ypath_join(output_dir, 'recent')))
    assert_that(len(results), equal_to(expected_offes_count))

    business_id = id
    shop_id = id
    assert_that(results, has_items(
        has_entries({
            'business_id': business_id,
            'offer_id': 'T600',
            'shop_id': shop_id,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': 'T600',
                    'shop_id': shop_id,
                },
                'meta': {
                    'rgb': rgb,
                },
            })
        }),
    ))


def test_dumper_is_ignore_stocks(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table('//home/datacamp/united/white_out/recent'))
    # partner not ignore stocks
    shop_id = 21
    business_id = 21
    offer_id = 'OfferForTestingIgnoreStocks_Disabled'
    assert_that(results, has_items(
        has_entries({
            'business_id': business_id,
            'offer_id': offer_id,
            'shop_id': shop_id,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'status': {
                    'disabled': [
                        {
                            'flag': True,
                        },
                    ]
                },
            })
        }),
    ))

    # partner stocks ignore/ All offers must not be in result
    shop_id = 22
    business_id = 22

    offer_count = len([1 for x in results if x.get('shop_id') == shop_id])
    # expected count offers
    assert_that(offer_count, 3)
    assert_that(results, has_items(
        has_entries({
            'business_id': business_id,
            'shop_id': shop_id,
        })
    ))
    assert_that(results, has_items(
        has_entries({
            'business_id': business_id,
            'shop_id': shop_id,
            'offer_id': offer_id,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'status': {
                    'disabled': [
                        {
                            'flag': False,
                        },
                    ]
                },
            })
        }),
    ))


def test_offers_diff_size(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(len(results), equal_to(13))


def test_amore_dumper(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/amore_out/recent')))
    assert_that(len(results), equal_to(3))
    assert_that(
        results,
        has_items(
            has_entries(
                {
                    'bids': IsSerializedProtobuf(
                        DTC.OfferBids,
                        {
                            'fee': {
                                'value': 4,
                            },
                        },
                    )
                }
            ),
        ),
    )

    assert_that(
        results,
        has_items(
            has_entries({'feed_id': 1000}),
        ),
    )


def test_deepmind_dumper(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/deepmind_out/recent')))
    assert_that(len(results),  equal_to(2))
    assert_that(results, HasDatacampYtUnitedOffersRows([{
        "business_id": 3001,
        "shop_sku": "OfferForTestingDeepmindDump",
        "supplier_id": 6001,
        "msku_id": 4001,
        "category_id": 5001,
        "title": "Title-1",
        "version_id": 7001,
        "acceptance_status": "ACCEPTANCE_STATUS_OK"
    },
    {
        "business_id": 3005,
        "shop_sku": "OfferForTestingDeepmindDump",
        "supplier_id": 6005,
        "msku_id": 4005,
        "category_id": 5005,
        "title": "Title-5",
        "version_id": 7005,
        "acceptance_status": "ACCEPTANCE_STATUS_OK"
    }]))


def test_dumper_skips_invisible_offers(yt_server, routines):
    """
    Тест проверяет, что в поколение НЕ берутся невидимые оффера
    """
    yt_client = yt_server.get_yt_client()
    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    white_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/white_out/recent')))
    dumper_log = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/dumper_log/recent')))

    assert_that(blue_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_FOR_INVISIBLE,
            'offer_id': 'InvisibleOffer' + str(DTC.BLUE),
        },
    ])))
    assert_that(white_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': BUSINESS_FOR_INVISIBLE,
            'offer_id': 'InvisibleOffer' + str(DTC.WHITE),
        },
    ])))
    for color in [DTC.BLUE, DTC.WHITE]:
        assert_that(dumper_log, has_items(
            has_entries({
                'business_id': BUSINESS_FOR_INVISIBLE,
                'offer_id': 'InvisibleOffer' + str(color),
                'type': 'INVISIBLE',
            }),
        ))


def test_dumper_log(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/dumper_log/recent')))
    assert_that(len(results), greater_than(0))


def test_vertical_shop_ids(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = set([row['shop_id'] for row in yt_client.read_table(yt.ypath_join('//home/datacamp/united/vertical_shop_id/recent'))])
    assert_that(results, has_items(10806908))


def test_peresort_set(yt_server, routines):
    """
    Проверяем установку признака пересорта в таблицы для поколения и для накатки сканнером
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferSetPeresort',
        'shop_id': shop_id,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferSetPeresort',
                'shop_id': shop_id,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT
                    },
                    'flag': True
                }]
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT,
                    },
                    'verdict': [{
                        'results': [{
                            'is_banned': True,
                            'messages': [{
                                'code': '49a',
                                'params': [
                                    {
                                        'name': 'offerId',
                                        'value': 'OfferSetPeresort',
                                    }
                                ],
                            }]
                        }]
                    }]
                }]
            },
        }),
    } for shop_id in [400, 401]]))

    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(results, HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferSetPeresort',
        'offer': IsSerializedProtobuf(UO.UnitedOffer, {
            'basic': {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'OfferSetPeresort',
                },
                'meta': {
                    'scope': DTC.BASIC,
                },
            },
            'service': IsProtobufMap({
                shop_id: {
                    'meta': {
                        'scope': DTC.SERVICE,
                    },
                    'status': {
                        'disabled': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT
                            },
                            'flag': True
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT,
                            },
                            'verdict': [{
                                'results': [{
                                    'is_banned': True,
                                    'messages': [{
                                        'code': '49a'
                                    }]
                                }]
                            }]
                        }]
                    },
                } for shop_id in [400, 401]
            }),
        }),
    }]))


def test_peresort_unset(yt_server, routines):
    """
    Проверяем сброс признака пересорта в таблицы для поколения и для накатки сканнером
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferUnsetPeresort',
        'shop_id': shop_id,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferUnsetPeresort',
                'shop_id': shop_id,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT
                    },
                    'flag': False
                }]
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT,
                    },
                    'verdict': empty()
                }]
            },
        }),
    } for shop_id in [400, 401]]))

    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferUnsetPeresortLonely',
        'shop_id': 400,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferUnsetPeresortLonely',
                'shop_id': 400,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT
                    },
                    'flag': False
                }]
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT,
                    },
                    'verdict': empty()
                }]
            },
        }),
    }]))

    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(results, HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferUnsetPeresort',
        'offer': IsSerializedProtobuf(UO.UnitedOffer, {
            'basic': {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'OfferUnsetPeresort',
                },
                'meta': {
                    'scope': DTC.BASIC,
                },
            },
            'service': IsProtobufMap({
                shop_id: {
                    'meta': {
                        'scope': DTC.SERVICE,
                    },
                    'status': {
                        'disabled': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT
                            },
                            'flag': False
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT,
                            },
                            'verdict': empty()
                        }]
                    },
                } for shop_id in [400, 401]
            })
        }),
    }]))

    assert_that(results, HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferUnsetPeresortLonely',
        'offer': IsSerializedProtobuf(UO.UnitedOffer, {
            'basic': {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'OfferUnsetPeresortLonely',
                },
                'meta': {
                    'scope': DTC.BASIC,
                },
            },
            'service': IsProtobufMap({
                400: {
                    'meta': {
                        'scope': DTC.SERVICE,
                    },
                    'status': {
                        'disabled': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT
                            },
                            'flag': False
                        }]
                    },
                    'resolution': {
                        'by_source': [{
                            'meta': {
                                'source': DTC.MARKET_IDX_PERESORT,
                            },
                            'verdict': empty()
                        }]
                    },
                }
            })
        }),
    }]))


def test_peresort_diff(yt_server, routines):
    """
    Проверяем, что если пересорт не меняется, то он не выгружается в таблицу для сканнера
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferDiffPeresort',
        'shop_id': shop_id,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferDiffPeresort',
                'shop_id': shop_id,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT
                    },
                    'flag': True
                }]
            },
            'resolution': {
                'by_source': [{
                    'meta': {
                        'source': DTC.MARKET_IDX_PERESORT,
                    },
                    'verdict': [{
                        'results': [{
                            'is_banned': True,
                            'messages': [{
                                'code': '49a'
                            }]
                        }]
                    }]
                }]
            },
        }),
    } for shop_id in [400, 401]]))

    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(results, is_not(HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferDiffPeresort',
    }])))


def test_no_peresort_because_content_flap_was_too_long_ago(yt_server, routines):
    """
    Проверяем, что пересорт не устанавливается, если контент флапал слишком давно
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferCantSetPeresortBecauseItIsTooLate',
        'shop_id': shop_id,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferCantSetPeresortBecauseItIsTooLate',
                'shop_id': shop_id,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': empty()
            },
            'resolution': None,
        }),
    } for shop_id in [400, 401]]))

    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(results, is_not(HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferCantSetPeresortBecauseItIsTooLate',
    }])))


def test_no_peresort_because_of_empty_content_flap(yt_server, routines):
    """
    Проверяем, что пересорт не устанавливается, если контент не флапал
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'offer_id': 'OfferCantSetPeresortBecauseOfEmptyContentFlap',
        'shop_id': shop_id,
        'warehouse_id': 145,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'offer_id': 'OfferCantSetPeresortBecauseOfEmptyContentFlap',
                'shop_id': shop_id,
                'warehouse_id': 145,
            },
            'status': {
                'disabled': empty()
            },
            'resolution': None,
        }),
    } for shop_id in [400, 401]]))

    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(results, is_not(HasDatacampYtOfferDiffRows([{
        'business_id': 4,
        'offer_id': 'OfferCantSetPeresortBecauseOfEmptyContentFlap',
    }])))


def test_integral_status(yt_server, routines):
    """
    Проверяем корректность вычисления интегральгого статуса: подтягиваются все части оффера
    """
    yt_client = yt_server.get_yt_client()

    blue_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/blue_out/recent')))
    assert_that(blue_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 4,
        'shop_id': 5,
        'warehouse_id': 145,
        'offer_id': 'OfferForIntegralStatus',
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 4,
                'shop_id': 5,
                'warehouse_id': 145,
                'offer_id': 'OfferForIntegralStatus',
            },
            'status': {
                'result': DTC.OfferStatus.ResultStatus.PUBLISHED,
                # Версия берется только из базовой части
                'version': {'actual_content_version': {'counter': 11}}
            }
        }),
    }]))

    white_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/white_out/recent')))
    assert_that(white_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 88,
        'shop_id': 8,
        'warehouse_id': 1,
        'offer_id': 'T600',
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 88,
                'shop_id': 8,
                'warehouse_id': 1,
                'offer_id': 'T600',
            },
            'resolution': {
                'by_source': [{
                    'verdict': [{
                        'results': [{
                            'is_banned': True,
                            'messages': [{
                                'code': '49r',
                            }],
                        }],
                    }],
                }]
            },
            'status': {
                'disabled': [{
                    'flag': True,
                }],
                'result': DTC.OfferStatus.ResultStatus.NOT_PUBLISHED_CHECKING,
            },
        }),
    }]))


@pytest.mark.parametrize(
    'business_id, shop_id, offer_id, source',
    [
        (4, 400, 'OfferSetMdmVerdict', DTC.MARKET_MDM),
        (4, SHOP_ID_DBS, 'OfferSetMdmVerdictForDBS', DTC.MARKET_MDM),
        (4, 400, 'OfferSetMbiMigratorVerdict', DTC.MARKET_MBI_MIGRATOR),
        (4, 400, 'OfferSetGutginVerdict', DTC.MARKET_GUTGIN),
        (10, 10, 'T600', DTC.DIRECT_BIGMOD),
        (11, 11, 'T600', DTC.DIRECT_BIGMOD),
        (12, 12, 'T600', DTC.DIRECT_BIGMOD),
    ]
)
def test_verdicts_diff_out(yt_server, routines, business_id, shop_id, offer_id, source):
    """
    Проверяем выгрузку изменений по скрытиям для вердиктов базовой части и вердиктов директа в diff-таблицу
    """
    yt_client = yt_server.get_yt_client()
    offers_diff = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(offers_diff, HasDatacampYtOfferDiffRows([{
        'business_id': business_id,
        'offer_id': offer_id,
        'offer': IsSerializedProtobuf(UO.UnitedOffer, {
            'basic': {
                'identifiers': {
                    'business_id': business_id,
                    'offer_id': offer_id,
                },
                'meta': {
                    'scope': DTC.BASIC,
                },
            },
            'service': IsProtobufMap({
                shop_id: {
                    'meta': {
                        'scope': DTC.SERVICE,
                    },
                    'status': {
                        'disabled': [{
                            'meta': {
                                'source': source
                            },
                            'flag': True
                        }]
                    }
                }
            }),
        }),
    }]))


@pytest.mark.parametrize(
    'business_id, shop_id, offer_id, source',
    [
        (4, 400, 'OfferSameMdmVerdict', DTC.MARKET_MDM),
        (4, 400, 'OfferSameMbiMigratorVerdict', DTC.MARKET_MBI_MIGRATOR),
        (4, 400, 'OfferSameGutginVerdict', DTC.MARKET_GUTGIN),
    ]
)
def test_verdicts_no_diff_no_out(yt_server, routines, business_id, shop_id, offer_id, source):
    """
    Проверяем, что если статус по вердикту не подлежит модификации, то он не выгружается в diff-таблицу
    """
    yt_client = yt_server.get_yt_client()
    offers_diff = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))
    assert_that(offers_diff, is_not(HasDatacampYtOfferDiffRows([{
        'business_id': business_id,
        'offer_id': offer_id,
    }])))


@pytest.mark.parametrize(
    'business_id, shop_id, offer_id, source, table_path',
    [
        (4, 400, 'OfferSetMdmVerdict', DTC.MARKET_MDM, '//home/datacamp/united/blue_out/recent'),
        (4, SHOP_ID_DBS, 'OfferSetMdmVerdictForDBS', DTC.MARKET_MDM, '//home/datacamp/united/white_out/recent'),
        (4, 400, 'OfferSetMbiMigratorVerdict', DTC.MARKET_MBI_MIGRATOR, '//home/datacamp/united/blue_out/recent'),
        (4, 400, 'OfferSetGutginVerdict', DTC.MARKET_GUTGIN, '//home/datacamp/united/blue_out/recent'),
        (12, 12, 'T600', DTC.DIRECT_BIGMOD, '//home/datacamp/united/direct_out/recent'),
    ]
)
def test_verdicts_update_status(yt_server, routines, business_id, shop_id, offer_id, source, table_path):
    """
    Проверяем установку скрытий для вердиктов базовой части и директа в out-выгрузках
    """
    yt_client = yt_server.get_yt_client()
    offers = list(yt_client.read_table(yt.ypath_join(table_path)))
    assert_that(offers, HasDatacampYtUnitedOffersRows([{
        'business_id': business_id,
        'offer_id': offer_id,
        'shop_id': shop_id,
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': business_id,
                'offer_id': offer_id,
                'shop_id': shop_id,
            },
            'status': {
                'disabled': [{
                    'meta': {
                        'source': source
                    },
                    'flag': True
                }]
            },
        }),
    }]))


def test_classifier_good_id_from_correct_offer_part(yt_server, routines):
    """
    Тест проверяет, что в поколение берется classifier_good_id из правильной части оффера в зависимости от настройки
    """
    yt_client = yt_server.get_yt_client()

    white_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/white_out/recent')))
    assert_that(white_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 10802213,
        'shop_id': 10806905,
        'warehouse_id': 0,
        'offer_id': 'OfferForClassififerGoodIdCheck',
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 10802213,
                'shop_id': 10806905,
                'warehouse_id': 0,
                'offer_id': 'OfferForClassififerGoodIdCheck',
                'extra': {
                    'classifier_good_id': '5793d36dd1a3cadd5b43a640960dd60a',
                    'classifier_magic_id2': 'b95944716b6108bb03cfd1b0c5ca2a0f',
                }
            },
        }),
    }]))


def test_classifier_magic_id_from_correct_offer_part(yt_server, routines):
    """
    Тест проверяет, что в поколение берется classifier_magic_id из правильной части оффера в зависимости от настройки
    """
    yt_client = yt_server.get_yt_client()

    white_offers = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/white_out/recent')))
    assert_that(white_offers, HasDatacampYtUnitedOffersRows([{
        'business_id': 10802214,
        'shop_id': 10806906,
        'warehouse_id': 0,
        'offer_id': 'OfferForClassififerMagicIdCheck',
        'offer': IsSerializedProtobuf(DTC.Offer, {
            'identifiers': {
                'business_id': 10802214,
                'shop_id': 10806906,
                'warehouse_id': 0,
                'offer_id': 'OfferForClassififerMagicIdCheck',
                'extra': {
                    'classifier_magic_id2': '2d15e94d598540df785ab10dfc262d89',
                }
            },
        }),
    }]))


def test_dump_by_http_request_with_calc_stats(routines_http, config, yt_server):
    """
    Тест http-ручки dump с расчетом статистик
    """
    yt_out_home = '//tmp/united/dump_with_stats'

    response = routines_http.post('/dump?cluster={}&color={}&out_home={}&calc_stats=true'.format(
        config.yt_map_reduce_proxies[0],
        'white',
        yt_out_home
    ))

    assert_that(response, HasStatus(200))

    expected_white_out = '{}/white_out/recent'.format(yt_out_home)
    assert yt_server.get_yt_client().exists(expected_white_out)

    expected_white_stats = '{}/white_out/stats/recent'.format(yt_out_home)
    assert yt_server.get_yt_client().exists(expected_white_stats)


def test_force_send_to_mining_after_mapping_moderation_end(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/offers_diff/recent')))

    def create_offer_with_miner_full_send_mark(offer_id):
        return {
            'business_id': MM_BUSINESS_ID,
            'offer_id': offer_id,
            'offer': IsSerializedProtobuf(UO.UnitedOffer, {
                'basic': {
                    'identifiers': {
                        'business_id': MM_BUSINESS_ID,
                        'offer_id': offer_id,
                    },
                    'meta': {
                        'scope': DTC.BASIC,
                        'miner_full_force_send': {
                            'meta': {
                                'source': DTC.MARKET_IDX
                            }
                        }
                    },
                },
            })
        }

    assert_that(results, HasDatacampYtOfferDiffRows([
        create_offer_with_miner_full_send_mark('MMOfferWithMMDisableTimeToEnable'),
        create_offer_with_miner_full_send_mark('MMOfferWithMMDisableShouldEnableByRemappingResult'),
        create_offer_with_miner_full_send_mark('MMOfferWithMMDisable_LongTimeAgoPartnerApprovedNewMapping_ShouldEnable')
    ]))

    assert_that(results, is_not(HasDatacampYtOfferDiffRows([
        create_offer_with_miner_full_send_mark('MMOfferWithNoMMDisable'),
        create_offer_with_miner_full_send_mark('MMOfferWithMMDisableButCantEnable'),
        create_offer_with_miner_full_send_mark('MMOfferWithMMDisable_RightNowPartnerApprovedNewMapping_CantEnable')
    ])))


def test_verdicts_hash(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = list(yt_client.read_table('//home/datacamp/united/routines/verdicts/recent'))
    hashes = set([row['hash'] for row in yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/verdicts/recent'))])
    codes = set([row['code'] for row in yt_client.read_table(yt.ypath_join('//home/datacamp/united/routines/verdicts/recent'))])
    # log.info("results count: {}".format(len(results)))
    # log.info("!!! codes: {}".format(codes))
    # log.info("!!! results: {}".format(results))
    assert_that(len(results), equal_to(5))
    assert len(hashes) == len(set(hashes)), "Hash codes should be unique"
    assert_that(codes, has_items("49r"))
    assert_that(codes, has_items("49a"))
    assert_that(codes, has_items("common.error.code"))


def test_key_shards(yt_server, routines):
    yt_client = yt_server.get_yt_client()
    results = [(row['business_id'], row['shop_sku']) for row in yt_client.read_table('//home/datacamp/united/routines/key_shards')]
    assert_that(results, has_items(
        (1, 'T600'),
        (2, 'T600'),
        (3, 'OfferForTestingPartnerDisables'),
        (4, 'BlueFulfillemtOfferWithoutActual'),
        (4, 'OfferCantSetPeresortBecauseItIsTooLate'),
        (4, 'OfferDiffPeresort'),
        (4, 'OfferSameGutginVerdict'),
        (4, 'OfferSameMdmVerdict'),
        (4, 'OfferSetMbiMigratorVerdict'),
        (4, 'OfferSetMdmVerdictForDBS'),
        (4, 'OfferUnsetPeresort'),
        (4, 'OfferWithInvalidWareMd5'),
        (4, 'OfferWithoutActual'),
        (4, 'RemovedOfferByBasic'),
        (7, 'T600'),
        (9, 'T600'),
        (10, 'T600'),
        (11, 'T600'),
        (12, 'T600'),
        (13, 'T600'),
        (14, 'T600'),
        (21, 'OfferForTestingIgnoreStocks'),
        (21, 'OfferForTestingIgnoreStocks_NotDisabled'),
        (22, 'OfferForTestingIgnoreStocks'),
        (22, 'OfferForTestingIgnoreStocks_NotDisabled'),
        (88, 'T600'),
        (3001, 'OfferForTestingDeepmindDump'),
        (3002, 'OfferForTestingDeepmindDump'),
        (3003, 'OfferForTestingDeepmindDump'),
        (3004, 'OfferForTestingDeepmindDump'),
        (3005, 'OfferForTestingDeepmindDump'),
        (10802213, 'OfferForClassififerGoodIdCheck'),
        (10802214, 'OfferForClassififerMagicIdCheck'),
        (10802215, 'VerticalDirect'),
        (10802216, 'VerticalGoodsAds'),
        (10802217, 'MMOfferWithMMDisableButCantEnable'),
        (10802217, 'MMOfferWithMMDisableTimeToEnable'),
        (10802217, 'MMOfferWithMMDisable_RightNowPartnerApprovedNewMapping_CantEnable'),
        (10802217, 'VerticalGoodsAds'),
        (10802218, 'VerticalGoodsAds')
    ))
