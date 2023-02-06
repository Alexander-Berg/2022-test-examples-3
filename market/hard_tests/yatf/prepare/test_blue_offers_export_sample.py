# coding: utf-8
from hamcrest import assert_that, has_item, has_entries

from market.idx.generation.yatf.envs.or3_offers_export import BlueOffersExportTestEnv
from market.idx.generation.yatf.resources.prepare.allowed_sessions import \
    AllowedSessions
from market.idx.generation.yatf.resources.prepare.in_picrobot_full import \
    PicrobotFullTable

from market.proto.feedparser.OffersData_pb2 import OfferOR2SC
from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC

from market.idx.generation.yatf.resources.prepare.push_feeds_file import PushFeedsFile, PushFeedsByBusinessFile
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable, DataCampOutOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.utils import dict2tskv, create_meta

import yt.wrapper as yt

import calendar
import pytest
import time


BUSINESS_ID = 1
PUSH_SHOP_ID = 11111
PUSH_FEED_ID = 22222

FF_FEED_ID = 3000
FF_SHOP_ID = 300
SUPPLIER_SHOP_ID = 3001


@pytest.fixture(scope="module")
def yt_home_dir_path():
    return get_yt_prefix()


def gen_session_id(session_id):
    return calendar.timegm(time.strptime(session_id, "%Y%m%d_%H%M"))


def parse_exported_offer(proto_str):
    proto = OfferOR2SC.FromString(proto_str)
    return proto.feed_id, proto.yx_shop_offer_id, proto.picURLS, proto.shop_sku


generation = '20180916_1001'


sessions_data = [
    {
        'feed_id': 31000,
        'published_ts': gen_session_id('20180916_0001'),
        'finish_time': 123,
    },
    {
        'feed_id': PUSH_FEED_ID,
        'published_ts': gen_session_id(generation),
        'finish_time': 123,
    },
]


push_feeds = [
    PUSH_FEED_ID
]

push_feeds_by_business = [
    (BUSINESS_ID, [PUSH_FEED_ID])
]

PARTNERS_TABLE_DATA = [
    {
        'shop_id': PUSH_SHOP_ID,
        'mbi': dict2tskv(
            {
                'business_id': BUSINESS_ID,
                'shop_id': PUSH_SHOP_ID,
                'warehouse_id': 100,
                'datafeed_id': PUSH_FEED_ID
            })
    }
]

DATACAMP_TABLE_DATA = [
    {
        'business_id': BUSINESS_ID,
        'offer_id': 'T1001',
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                shop_id=PUSH_SHOP_ID,
                business_id=BUSINESS_ID,
                offer_id='T1001',
                extra=DTC.OfferExtraIdentifiers(
                    recent_feed_id=PUSH_FEED_ID,
                    recent_warehouse_id=145,
                    shop_sku='T1001',
                    ware_md5='101010101010101010101',
                )
            ),
            content=DTC.OfferContent(
                binding=DTC.ContentBinding(
                    approved=DTC.Mapping(
                        market_sku_id=12345,
                    ),
                ),
            ),
            partner_info=DTC.PartnerInfo(
                fulfillment_feed_id=FF_FEED_ID,
                fulfillment_virtual_shop_id=FF_SHOP_ID,
                supplier_id=SUPPLIER_SHOP_ID,
            ),
            meta=create_meta(10, color=DTC.BLUE),
        ).SerializeToString()
    },
]


@pytest.yield_fixture(scope="module")
def blue_offers_export_workflow(yt_server, yt_home_dir_path):
    resources = {
        'picrobot_full': PicrobotFullTable(
            yt_stuff=yt_server,
            path=yt.ypath_join(yt_home_dir_path,
                               'in',
                               'picrobot',
                               'full',
                               'recent'),
            data=[]
        ),
        'allowed_sessions': AllowedSessions(
            sessions_data
        ),
        'push_feeds': PushFeedsFile(
            push_feeds
        ),
        'push_feeds_by_business': PushFeedsByBusinessFile(
            push_feeds_by_business
        ),
        'datacamp-offers-table': DataCampOutOffersTable(
            yt_stuff=yt_server,
            path=yt.ypath_join(yt_home_dir_path, 'datacamp', 'offers'),
            data=DATACAMP_TABLE_DATA
        ),
        'datacamp-partners-table': DataCampPartnersTable(
            yt_stuff=yt_server,
            path=yt.ypath_join(yt_home_dir_path, 'datacamp', 'partners'),
            data=PARTNERS_TABLE_DATA
        ),
    }
    with BlueOffersExportTestEnv(yt_server, generation, yt_home_dir_path,
                                 **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_blue_output_table_not_empty(blue_offers_export_workflow):
    """Таблица есть и она не пустая, в ней оффер - из пуш модели из таблицы хранилища
    """
    assert len(blue_offers_export_workflow.output_table) == 1


def test_blue_sc_output_table_push(blue_offers_export_workflow):
    """Проверяем, что офер из пуш модели есть в выгрузки"""
    expected_row = {
        'feed_id': FF_FEED_ID,
        'offer_id': '{}.T1001'.format(PUSH_FEED_ID),
        'offer': IsSerializedProtobuf(OfferOR2SC, {
            'feed_id': FF_FEED_ID,
            'yx_ds_id': FF_SHOP_ID,
            'yx_shop_offer_id': '{}.T1001'.format(PUSH_FEED_ID),
            'shop_sku': 'T1001',
            'market_sku': 12345
        }),
        'pic': None,
        'session_id': gen_session_id(generation),
        'uc': None,
        'finish_time': 123
    }

    assert_that(blue_offers_export_workflow.output_table, has_item(has_entries(expected_row)))
