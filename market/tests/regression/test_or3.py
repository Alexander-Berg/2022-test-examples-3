# coding=utf-8

import base64
import calendar
import collections
import hashlib
import json
import os
import subprocess
import time
import md5
import yatest
from market.pylibrary.yatestwrap.yatestwrap import source_path
from market.pylibrary.lenval_stream import iter_file
from market.pylibrary.lenval_stream import write as lenval_write
from StringIO import StringIO

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig

from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    OfferExtraIdentifiers,
    OfferMeta,
    OfferPrice,
    OfferPromos,
    MarketPromos,
    Promos,
    OfferStatus,
    ContentBinding,
    PartnerInfo,
    UpdateMeta,
    OfferIdentifiers,
    OfferContent,
    Mapping,
)
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Offer as DatacampOffer
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import PriceBundle, MarketContent, OfferDelivery, DeliveryInfo
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import WHITE, BLUE, MARKET_IDX
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import Flag as DatacampFlag
from market.idx.datacamp.proto.offer.OfferPromos_pb2 import Promo
from market.idx.datacamp.proto.category.PartnerCategory_pb2 import PartnerCategory
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.pylibrary.datacamp.schema import (
    categories_attributes,
    united_offers_indexation_out_table,
)
from market.proto.common.common_pb2 import PriceExpression
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.feedparser.OffersData_pb2 import (
    OfferOR2SC,
    Category,
)
from market.proto.ir.UltraController_pb2 import EnrichedOffer
from market.proto.ir.UltraController_pb2 import FormalizedParamPosition
from market.proto.content.pictures_pb2 import Picture

YT_SERVER = None

philimonov_url = "https://staff.yandex-team.ru/philimonov"
kgorelov_url = "https://staff.yandex-team.ru/kgorelov"
alekswn_url = "https://staff.yandex-team.ru/alekswn"
ellpic_url = "https://ellpic.yandex.ru/ellpic"
ydx_url = "yandex-team.ru"
nickderev_url = "https://staff.yandex-team.ru/nickderev"
nickderev_dup_url = "https://staff.yandex-team.ru/nickderev_dup"
nickderev_descr_url = "https://staff.yandex-team.ru/nickderev_descr_url"
nickderev_main_pic_url = "https://staff.yandex-team.ru/nickderev_main_pic_url"
fess7_good_pic_url = "https://staff.yandex-team.ru/fess7_good_pic_url"
fess7_bad_pic1_url = "https://staff.yandex-team.ru/fess7_bad_pic1_url"
fess7_bad_pic2_url = "https://staff.yandex-team.ru/fess7_bad_pic2_url"
bzz13_url = "http://staff.yandex-team.ru/bzz13"

_DATACAMP_TEST_BUSINESS_ID = 12345
_DATACAMP_TEST_STORE_WHITE_SHOP_ID = 11000821
_DATACAMP_TEST_STORE_WHITE_FEED_ID = 200323258

_DATACAMP_TEST_STORE_BLUE_SHOP_ID = 10296179
_DATACAMP_TEST_STORE_BLUE_FEED_ID = 200398706

_DATACAMP_TEST_ENRICHED_MODEL_ID = 12349
_DATACAMP_TEST_ENRICHED_CATEGORY = 3

_REAL_DELIVERYCALC_GENERATION = 524524
SUPPLIER_TYPE = 1

FILE_ALLOWED_SESSIONS = "allowed_sessions.json"
FILE_TMP_ALLOWED_SESSIONS = "tmp_allowed_sessions.json"
FILE_TMP_PUSH_FEEDS = "tmp_push_feeds.txt"
FILE_TMP_PUSH_FEEDS_BY_BUSINESS = "tmp_push_feeds_groupped_by_business.txt"


RETCODE_STOP_PIPELINE = 1
DEPOT = 1
AVAILABLE = 1 << 3
AUTOBROKER_ENABLED = 1 << 4
STORE = 1 << 7
MODEL_COLOR_WHITE = 1 << 8
MODEL_COLOR_BLUE = 1 << 9
OFFER_HAS_GONE = 1 << 11
IS_PUSH_PARTNER = 1 << 12
IS_FULFILLMENT = 1 << 14
CPC = 1 << 16
IS_BLUE_OFFER = 1 << 18
HAS_AUTOMAIC_TRANSLATIONS = 1 << 23
CPA = 1 << 24
POST_TERM = 1 << 26
HAS_PICTURES = 1 << 28
DISABLED_MARKET_IDX = 1 << MARKET_IDX


def parse_sc_offer(proto_str):
    proto = OfferOR2SC.FromString(proto_str)
    return proto.feed_id, proto.yx_shop_offer_id, proto.picURLS, proto.shop_sku, proto.ware_md5, \
           proto.yx_ds_id, proto.offer_flags, proto.market_sku, proto.is_blue_offer


def parse_uc_offer(proto_str):
    proto = EnrichedOffer.FromString(proto_str)
    return proto.category_id, proto.model_id


def parse_pictures(pictures):
    if not isinstance(pictures, str):
        return tuple(sorted(pictures))

    def load_pic_data(pic_proto):
        pic = Picture()
        pic.ParseFromString(pic_proto)
        return make_uid_from_binary(pic.md5), pic.width

    return tuple(sorted(
        load_pic_data(pic_proto)
        for pic_proto in iter_file(StringIO(pictures))
    ))


class SCOfferRow(object):
    FIELDS = collections.OrderedDict([
        ('feed_id', long),
        ('session_id', long),
        ('offer_id', str),
        ('offer', parse_sc_offer),
        ('uc', parse_uc_offer),
        ('pic', parse_pictures),
        ('finish_time', long)
    ])

    def __init__(self, **kwargs):
        for name in self.FIELDS.iterkeys():
            setattr(self, name, None)

        for name, value in kwargs.iteritems():
            if value is not None:
                setattr(self, name, self.FIELDS[name](value))

    def __eq__(self, other):
        if type(self) != type(other):
            return False

        return all(
            getattr(self, name) == getattr(other, name)
            for name in self.FIELDS.iterkeys()
        )

    def __hash__(self):
        return (
            hash(self.feed_id) ^
            hash(self.session_id) ^
            hash(self.offer_id) ^
            hash(self.offer) ^
            hash(self.uc) ^
            hash(self.finish_time)
        )

    def __repr__(self):
        return 'SCOfferRow({})'.format(
            ', '.join(
                '{}={!r}'.format(name, getattr(self, name))
                for name in self.FIELDS.iterkeys()
            )
        )


CategoryRow = collections.namedtuple('CategoryRow', 'feed_id session_id category_id parent_id name')


def setup_module(module):
    module.YT_SERVER = YtStuff(YtConfig(wait_tablet_cell_initialization=True))
    module.YT_SERVER.start_local_yt()


def teardown_module(module):
    if module.YT_SERVER:
        module.YT_SERVER.stop_local_yt()


def setup_function(f):
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    for table in yt.list("//home"):
        yt.remove("//home/{table}".format(table=table), recursive=True, force=True)

    indexer_group = "idm-group:69548"
    groups = yt.list("//sys/groups")
    if indexer_group not in groups:
        yt.create("group", attributes={"name": "idm-group:69548"}, force=True)


def create_table(yt, table_name, schema=None, blue=False):
    if schema is None:
        schema = [
            dict(name='feed_id', type='uint64'),
            dict(name='session_id', type='uint64'),
            dict(name='offer_id', type='string'),
            dict(name='offer', type='string'),
            dict(name='recs', type='string'),
            dict(name='promo', type='string'),
            dict(name='uc', type='string'),
        ]
    if blue:
        schema.append(dict(name='msku', type='uint64'))

    attributes = dict()
    if schema:
        attributes['schema'] = schema

    yt.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=attributes
    )


def write_static_tables(yt, data):
    for k, v in data.iteritems():
        yt.write_table(k, v)


def gen_session_id(session):
    return calendar.timegm(time.strptime(session, "%Y%m%d_%H%M"))


def gen_offer(feed_id=1069, session="20170505_1650", offer_id="1",
              offer="some_data", recs="recs", promo="promo", uc="uc"):
    return dict(feed_id=feed_id, session_id=gen_session_id(session), offer_id=offer_id,
                offer=offer, recs=recs, promo=promo, uc=uc)


def gen_blue_offer(feed_id=1069, session="20170505_1650", offer_id="1",
                   offer="some_data", recs="recs", promo="promo", uc="uc", msku=1):
    return dict(feed_id=feed_id, session_id=gen_session_id(session), offer_id=offer_id,
                offer=offer, recs=recs, promo=promo, uc=uc, msku=msku)


def write_session_info_row(file, feed_id, session_id, finish_time=None):
    data = {
        "feed_id": feed_id,
        "published_ts": session_id,
    }
    if finish_time is not None:
        data.update({
            "finish_time": finish_time,
        })

    row = json.dumps(data)
    row += '\n'
    file.write(row)


def make_from_dict(dict_data, proto_cls):
    return proto_cls(**dict_data).SerializeToString()


def gen_offer_proto(**kwargs):
    return make_from_dict(kwargs, Offer)


def gen_uc_offer_proto(**kwargs):
    return make_from_dict(kwargs, EnrichedOffer)


def gen_sc_offer_proto(**kwargs):
    return make_from_dict(kwargs, OfferOR2SC)


def check_kwargs(kwargs, supported_keys):
    for key in kwargs.iterkeys():
        if key not in supported_keys:
            raise Exception("Unsuported field: {f}".format(f=key))


def gen_category_with_holes(**kwargs):
    check_kwargs(
        kwargs,
        [
            "feed_id",
            "session",
            "category_id",
            "cat"
        ]
    )

    if "session" in kwargs:
        kwargs["session_id"] = gen_session_id(kwargs["session"])
        del kwargs["session"]

    if "cat" in kwargs:
        kwargs["cat"] = make_from_dict(kwargs["cat"], Category)

    return kwargs


def convert_promo_from_dict(promo_dict, set_meta=False) :
    meta = None
    if set_meta and 'meta' in promo_dict and 'removed' in promo_dict['meta']:
        removed = promo_dict['meta']['removed']
        meta = UpdateMeta(removed=removed)
    discount_price = None
    if 'discount_price' in promo_dict:
        if 'price' in promo_dict['discount_price'] and 'id' in promo_dict['discount_price']:
            discount_price_value = promo_dict['discount_price']['price']
            discount_price_currency = promo_dict['discount_price']['id']
            discount_price = PriceExpression(price=discount_price_value, id=discount_price_currency)
    discount_oldprice = None
    if 'discount_oldprice' in promo_dict:
        if 'price' in promo_dict['discount_oldprice'] and 'id' in promo_dict['discount_oldprice']:
            discount_oldprice_value = promo_dict['discount_oldprice']['price']
            discount_oldprice_currency = promo_dict['discount_oldprice']['id']
            discount_oldprice = PriceExpression(price=discount_oldprice_value, id=discount_oldprice_currency)
    direct_discount_price = None
    if 'direct_discount' in promo_dict and 'price' in promo_dict['direct_discount']:
        direct_price = promo_dict['direct_discount']['price']
        if 'price' in direct_price and 'id' in direct_price:
            discount_price_value = direct_price['price']
            discount_price_currency = direct_price['id']
            direct_discount_price = PriceExpression(price=discount_price_value, id=discount_price_currency)
    direct_discount_base_price = None
    if 'direct_discount' in promo_dict and 'base_price' in promo_dict['direct_discount']:
        if 'price' in promo_dict['direct_discount']['base_price'] and 'id' in promo_dict['direct_discount']['base_price']:
            discount_price_value = promo_dict['direct_discount']['base_price']['price']
            discount_price_currency = promo_dict['direct_discount']['base_price']['id']
            direct_discount_base_price = PriceExpression(price=discount_price_value, id=discount_price_currency)
    direct_discount = None
    if direct_discount_price is not None or direct_discount_base_price is not None:
        direct_discount=Promo.DirectDiscount(price=direct_discount_price, base_price=direct_discount_base_price)

    promo = Promo(
        id=promo_dict['id'],
        active=promo_dict.get('active', False),
        discount_price=discount_price,
        discount_oldprice=discount_oldprice,
        direct_discount=direct_discount,
        meta=meta,
    )
    return promo


def get_offer_version(rgb):
    if rgb == WHITE:
        return 777
    if rgb == BLUE:
        return 333

    return 1


def gen_datacamp_offer_column(
        shop_id,
        offer_id,
        feed_id,
        ware_md5,
        price=100,
        market_sku_id=None,
        rgb=WHITE,
        uc=False,
        real_deliverycalc_generation=_REAL_DELIVERYCALC_GENERATION,
        disabled_by=None,
        market_category_id=None,
        warehouse_id=145,
        promo_dict=None,
        anaplan_promo_active=None,
        anaplan_promo_all=None,
):
    offer = DatacampOffer(
        identifiers=OfferIdentifiers(
            business_id=_DATACAMP_TEST_BUSINESS_ID,
            shop_id=shop_id,
            offer_id=offer_id,
            extra=OfferExtraIdentifiers(
                ware_md5=ware_md5,
                recent_feed_id=feed_id,
                recent_warehouse_id=warehouse_id,
            ),
        ),
        content=OfferContent(
            binding=ContentBinding(
                approved=Mapping(
                    market_sku_id=market_sku_id,
                ),
            ),
        ),
        meta=OfferMeta(
            rgb=rgb,
        ),
        price=OfferPrice(
            basic=PriceBundle(
                binary_price=PriceExpression(price=price, id='RUR'),
            ),
        ),
        delivery=OfferDelivery(
            delivery_info=DeliveryInfo(
                real_deliverycalc_generation=real_deliverycalc_generation,
            )
        ),
        tech_info=DTC.OfferTechInfo(
            last_mining=DTC.MiningTrace(
                original_partner_data_version=DTC.VersionCounter(counter=get_offer_version(rgb))
            ),
        )
    )

    if rgb == BLUE:
        offer.partner_info.CopyFrom(PartnerInfo(
            is_blue_offer=True,
            supplier_id=shop_id,
            supplier_type=SUPPLIER_TYPE,
        ))

    if uc:
        offer.content.market.CopyFrom(MarketContent(
            enriched_offer=EnrichedOffer(
                model_id=_DATACAMP_TEST_ENRICHED_MODEL_ID,
                category_id=_DATACAMP_TEST_ENRICHED_CATEGORY
            )
        ))

    if disabled_by:
        offer.status.CopyFrom(OfferStatus(
            disabled=[
                DatacampFlag(
                    flag=True,
                    meta=UpdateMeta(source=disabled_by)
                )
            ]
        ))

    if market_category_id is not None:
        offer.content.CopyFrom(OfferContent(
            binding=ContentBinding(
                smb_partner=Mapping(
                    market_category_id=market_category_id
                )
            )
        ))
        offer.partner_info.CopyFrom(PartnerInfo(
            direct_product_mapping=True
        ))

    is_good_promo = isinstance(promo_dict, dict)
    if is_good_promo:
        offer.promos.CopyFrom(OfferPromos(
            promo=[
                convert_promo_from_dict(promo_dict, set_meta=True),
            ]
        ))

    is_anaplan_promo_active = isinstance(anaplan_promo_active, dict)
    is_anaplan_promo_all = isinstance(anaplan_promo_all, dict)

    if is_anaplan_promo_active or is_anaplan_promo_all :
        offer.promos.CopyFrom(OfferPromos(
            anaplan_promos=MarketPromos(
            )
        ))

    if is_anaplan_promo_active:
        offer.promos.anaplan_promos.active_promos.CopyFrom(Promos(
            promos=[
                convert_promo_from_dict(anaplan_promo_active),
            ]
        ))
    if is_anaplan_promo_all:
        offer.promos.anaplan_promos.all_promos.CopyFrom(Promos(
            promos=[
                convert_promo_from_dict(anaplan_promo_all),
            ]
        ))

    return {
        'business_id': _DATACAMP_TEST_BUSINESS_ID,
        'offer_id': offer_id,
        'shop_id': shop_id,
        'warehouse_id': warehouse_id,
        'offer': offer.SerializeToString()
    }


def gen_datacamp_category_column(business_id, category_id, parent_id, name):
    return {
        'business_id': business_id,
        'category_id': category_id,
        'content': PartnerCategory(id=category_id, name=name, parent_id=parent_id).SerializeToString()
    }


def gen_offer_with_holes(**kwargs):
    check_kwargs(kwargs, [
                 "feed_id", "session", "offer_id",
                 "offer", "recs", "promo", "uc", "msku", "pic"
                 ])

    if "session" in kwargs:
        kwargs["session_id"] = gen_session_id(kwargs["session"])
        del kwargs["session"]

    # [pic_dict1, pic_dict2, ...] => lenval(protopic1, protopic2, ...)
    if "pic" in kwargs:
        f = StringIO()
        lenval_write(f, (make_from_dict(pic_dict, Picture) for pic_dict in kwargs["pic"]))
        kwargs["pic"] = f.getvalue()

    return kwargs


def make_uid_from_binary(binary):
    return base64.b64encode(binary, altchars='-_')[:22]


def make_pic_id(url):
    h = hashlib.md5()
    h.update(url)
    return make_uid_from_binary(h.digest())


def gen_picture(**kwargs):
    check_kwargs(kwargs, [
        "id",
        "url",
        "status",
        "group_id",
        "thumbnails",
        "width",
        "height",
        "imagename",
        "http_code",
        "crc",
        "colorness",
        "colorness_avg",
        "mds_namespace",
    ])

    return dict(**kwargs)


def gen_offer2pic(**kwargs):
    check_kwargs(kwargs, [
        "id", "feed_id", "offer_id", "ts", "descr_url"
    ])
    return dict(**kwargs)


def create_picrobot_table(yt, table_path):
    create_table(yt, table_path,
                 schema=[
                     dict(name='id', type='string', sort_order='ascending'),
                     dict(name='url', type='string'),
                     dict(name='status', type='string'),
                     dict(name='group_id', type='uint64'),
                     dict(name='thumbnails', type='uint64'),
                     dict(name='width', type='uint64'),
                     dict(name='height', type='uint64'),
                     dict(name='http_code', type='uint64'),
                     dict(name='crc', type='string'),
                     dict(name='colorness', type='double'),
                     dict(name='colorness_avg', type='double'),
                     dict(name='mds_namespace', type='string'),
                 ])


def create_offer2pic_table(yt, table_path):
    create_table(yt, table_path,
                 schema=[
                     dict(name='id', type='string', sort_order='ascending'),
                     dict(name='feed_id', type='uint64'),
                     dict(name='offer_id', type='string'),
                     dict(name='descr_url', type='string'),
                     dict(name='is_main_pic', type='boolean'),
                 ])


def create_successful_pics_table(yt, table_path):
    create_table(yt, table_path,
                 schema=[
                     dict(name='id', type='string', sort_order='ascending'),
                     dict(name='pic', type='string'),
                 ])


def create_offer2pic_history_table(yt, table_path):
    create_table(yt, table_path,
                 schema=[
                     dict(name='id', type='string', sort_order='ascending'),
                     dict(name='feed_id', type='uint64'),
                     dict(name='offer_id', type='string'),
                     dict(name='ts', type='uint64'),
                     dict(name='descr_url', type='string'),
                 ])


def create_indexer_table(yt, table_path):
    create_table(yt, table_path,
                 schema=[
                     dict(name='feed_id', type='uint64'),
                     dict(name='session_id', type='uint64'),
                     dict(name='offer_id', type='string'),
                     dict(name='offer', type='string'),
                     dict(name='recs', type='string'),
                     dict(name='promo', type='string'),
                     dict(name='uc', type='string'),
                     dict(name='pic', type='string'),
                     dict(name='diff_type', type='string'),
                 ])


def create_datacamp_offers_table(yt, table_path):
    create_table(yt, table_path,
                 schema=united_offers_indexation_out_table()['schema'])


def create_datacamp_categories_table(yt, table_path):
    create_table(yt, table_path,
                 schema=categories_attributes()['schema'])


def test_or3_offers_export():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    create_table(yt, "//home/or3/offers/20170518_0400")
    create_table(yt, "//home/or3/offers/20170518_0800")
    yt.create("map_node", "//home/or3/offers/tmp")

    create_picrobot_table(yt, '//home/picrobot/export/20170807_1800')
    create_offer2pic_history_table(yt, '//home/mi3/offer2pic_history/0')
    yt.link('//home/mi3/offer2pic_history/0', '//home/mi3/offer2pic_history/recent')

    datacamp_united_offer_1 = gen_sc_offer_proto(
        feed_id=1074,
        yx_shop_offer_id='datacamp_offer_from_united_table_01',
        binary_price=PriceExpression(price=100, id='RUR'),
        ware_md5='t64cn4P9vKkqTKYL91bEPg',
        yx_ds_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID,
        offer_flags=(DEPOT | STORE | IS_PUSH_PARTNER),
    )

    philimonov = make_pic_id(philimonov_url)
    alekswn = make_pic_id(alekswn_url)
    ellpic = make_pic_id(ellpic_url)
    ydx = make_pic_id(ydx_url)
    kgorelov = make_pic_id(kgorelov_url)
    datacamp_offers_table = "//home/datacamp/test_or3_export/united_offers"
    create_datacamp_offers_table(yt, datacamp_offers_table)

    data = {
        "//home/picrobot/export/20170807_1800": sorted([
            gen_picture(id=philimonov,
                        url=philimonov_url,
                        status="S",
                        group_id=1,
                        thumbnails=7,
                        width=4320,
                        height=3840,
                        http_code=200,
                        crc="",
                        colorness=0.0,
                        colorness_avg=0.0,
                        ),
            gen_picture(id=alekswn,
                        url=alekswn_url,
                        status="N",
                        group_id=2,
                        thumbnails=15,
                        width=3840,
                        height=1080,
                        http_code=200,
                        crc="",
                        colorness=0.0,
                        colorness_avg=0.0,
                        ),
            gen_picture(id=ellpic,
                        url=ellpic_url,
                        status="S",
                        group_id=0,
                        thumbnails=7,
                        width=4320,
                        height=3840,
                        http_code=200,
                        crc="",
                        colorness=0.0,
                        colorness_avg=0.0,
                        ),
            gen_picture(id=ydx,
                        url=ydx_url,
                        status="F",
                        group_id=3,
                        thumbnails=31,
                        width=100,
                        height=100,
                        http_code=304,),
            gen_picture(id=kgorelov,
                        url=kgorelov_url,
                        status="S",
                        group_id=5,
                        thumbnails=18,
                        width=5000,
                        height=5000,
                        http_code=200,
                        crc="IG",
                        colorness=0.5,
                        colorness_avg=0.5,
                        ),

        ], key=lambda x: x.get('id')),
        '//home/mi3/offer2pic_history/0': sorted([
            gen_offer2pic(id=kgorelov,
                          feed_id=1074,
                          offer_id="123",
                          ts=1234),
            gen_offer2pic(id=kgorelov,
                          feed_id=1069,
                          offer_id="123",
                          ts=2345),
        ], key=lambda x: x.get('id')),
        datacamp_offers_table: [
            gen_datacamp_offer_column(
                offer_id='datacamp_offer_from_united_table_01',
                shop_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID,
                feed_id=1074,
                ware_md5='t64cn4P9vKkqTKYL91bEPg',
            ),
        ]
    }

    write_static_tables(yt, data)
    yt.link("//home/picrobot/export/20170807_1800", "//home/picrobot/export/recent")

    fs_path = yatest.common.test_output_path(FILE_ALLOWED_SESSIONS)
    with open(fs_path, 'w') as fs_file:
        write_session_info_row(fs_file, 1069, gen_session_id("20170518_0434"), 123)
        write_session_info_row(fs_file, 1071, gen_session_id("20170518_0836"), 456)
        write_session_info_row(fs_file, 1072, gen_session_id("20170518_0838"), 789)
        write_session_info_row(fs_file, 1073, gen_session_id("20200804_1121"), 1596529260)
        write_session_info_row(fs_file, 1074, gen_session_id("20200804_1122"), 1596529320)

    fs_path_push_feeds = yatest.common.test_output_path(FILE_TMP_PUSH_FEEDS)
    feeds = [1073, 1074]
    with open(fs_path_push_feeds, 'w') as push_feeds_file:
        for feed_id in feeds:
            push_feeds_file.write("{feed_id}\n".format(feed_id=feed_id))

    fs_path_push_feeds_by_business_id = yatest.common.test_output_path(FILE_TMP_PUSH_FEEDS_BY_BUSINESS)
    with open(fs_path_push_feeds_by_business_id, 'w') as _f:
        # business_id -> feed_id1 feed_id2 ...
        _f.write("{business_id} {feeds}\n".format(business_id=_DATACAMP_TEST_BUSINESS_ID, feeds=" ".join(
            str(x) for x in feeds
        )))

    work_dir = yatest.common.test_output_path('wd')
    os.mkdir(work_dir)

    cfg = """
[yt]
home_dir = //home
yt_picrobot_meta = //home/picrobot/export/recent
yt_proxy = {proxy}
"""

    pic_cfg_path = yatest.common.test_output_path("pic-maker.cfg")
    with open(pic_cfg_path, 'w') as _f:
        _f.write(cfg.format(proxy=YT_SERVER.get_server()))

    subprocess.check_call([
        yatest.common.binary_path('market/idx/pictures/pic-maker/pic-maker'),
        "-c", pic_cfg_path
    ])

    generation = "20170706_2007"
    cmdlist = [
        yatest.common.binary_path('market/idx/export/or3-offers-export/or3-offers-export'),
        "--work-dir", work_dir,
        "--allowed-sessions", fs_path,
        "--generation", generation,
        "--yt-home-dir", "//home",
        "--yt-proxy", YT_SERVER.get_server(),
        "--push-feeds", fs_path_push_feeds,
        "--push-feeds-by-business", fs_path_push_feeds_by_business_id,
        "--datacamp-offers-table", datacamp_offers_table,
    ]
    subprocess.check_call(cmdlist)

    all_tables = yt.list("//home/out/supercontroller")
    assert [generation, "recent", "tmp"] == all_tables

    expected_offers = set([
        SCOfferRow(
            feed_id=1074,
            session_id=gen_session_id("20200804_1122"),
            offer_id='datacamp_offer_from_united_table_01',
            offer=datacamp_united_offer_1,
            finish_time=1596529320,
        )
    ])
    actual_offers = set(
        SCOfferRow(**row)
        for row in yt.read_table("//home/out/supercontroller/{}".format(generation))
    )

    assert expected_offers == actual_offers


def test_or3_shop_categories_export():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    schema = [
        dict(name="feed_id", type="uint64"),
        dict(name="session_id", type="uint64"),
        dict(name="category_id", type="string"),
        dict(name="cat", type="string"),
    ]

    create_table(yt, "//home/or3/categories/20170518_0400", schema)
    create_table(yt, "//home/or3/categories/20170518_0800", schema)
    yt.create("map_node", "//home/or3/categories/tmp")

    datacamp_offers_table = "//home/datacamp/test_categories/offers"
    create_datacamp_offers_table(yt, datacamp_offers_table)
    datacamp_categories_table = "//home/datacamp/test_categories/categories"
    create_datacamp_categories_table(yt, datacamp_categories_table)

    data = {
        "//home/or3/categories/20170518_0400": [
            gen_category_with_holes(feed_id=1069, session="20170518_0434",
                                    category_id="foo", cat=dict(id="foo", name="Foo")),
            gen_category_with_holes(feed_id=1069, session="20170518_0434", category_id="bar",
                                    cat=dict(id="bar", parent_id="foo", name="Bar")),
        ],
        "//home/or3/categories/20170518_0800": [
            gen_category_with_holes(feed_id=1071, session="20170518_0836",
                                    category_id="foo", cat=dict(id="foo", name="Foo")),
            gen_category_with_holes(feed_id=1071, session="20170518_0836", category_id="awol",
                                    cat=dict(id="awol", parent_id="foo", name="Awol")),
            gen_category_with_holes(feed_id=1072, session="20170518_0838",
                                    category_id="baz", cat=dict(id="baz", name="Baz")),
        ],
        datacamp_offers_table: [
            # datacamp offer without category mapping
            gen_datacamp_offer_column(shop_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID,
                                      offer_id="datacamp_offer_no_category_mapping",
                                      feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID,
                                      ware_md5="C8xDiNmU85M1pqWKySk-SZ=="),
            # datacamp offer with category mapping
            gen_datacamp_offer_column(shop_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID + 1,
                                      offer_id="datacamp_offer_with_category_mapping",
                                      feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1,
                                      ware_md5="C8xDiNmU85M1pqWKySk-SM==",
                                      market_category_id=90914),
            # datacamp offer with category mapping on category without parent id
            gen_datacamp_offer_column(shop_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID + 2,
                                      offer_id="datacamp_offer_with_category_mapping_on_cat_wihtout_parent",
                                      feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 2,
                                      ware_md5="C8xDiNmU85M1pqWKySk-SN==",
                                      market_category_id=90401),
            # datacamp offer with category mapping on same categories as previous one
            gen_datacamp_offer_column(shop_id=_DATACAMP_TEST_STORE_WHITE_SHOP_ID + 1,
                                      offer_id="datacamp_offer_with_category_mapping_same",
                                      feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1,
                                      ware_md5="C8xDiNmU85M1pqWKySk-SO==",
                                      market_category_id=90914),
        ],
        datacamp_categories_table: [
            # три магазина, которые принадлежат одному business_id
            # строки таблицы должны быть отсортированы по business_id и category_id
            gen_datacamp_category_column(business_id=_DATACAMP_TEST_BUSINESS_ID,
                                         category_id=90401,
                                         parent_id=None,
                                         name="category 90401"),
            # категории брались не из офферов, а из всей выгрузки дерева категорий магазина,
            # поэтому могут быть и категории без офферов
            gen_datacamp_category_column(business_id=_DATACAMP_TEST_BUSINESS_ID,
                                         category_id=90829,  # книги
                                         parent_id=90401,
                                         name="category 90829"),
            gen_datacamp_category_column(business_id=_DATACAMP_TEST_BUSINESS_ID,
                                         category_id=90914,  # книги по искусству
                                         parent_id=90829,
                                         name="category 90914"),
            # магазин без офферов с другим business_id
            gen_datacamp_category_column(business_id=_DATACAMP_TEST_BUSINESS_ID + 1,
                                         category_id=12345,
                                         parent_id=None,
                                         name="category 12345"),
        ]
    }
    write_static_tables(yt, data)

    fs_path = yatest.common.test_output_path(FILE_ALLOWED_SESSIONS)
    with open(fs_path, 'w') as fs_file:
        write_session_info_row(fs_file, 1069, gen_session_id("20170518_0434"))
        write_session_info_row(fs_file, 1071, gen_session_id("20170518_0836"))
        write_session_info_row(fs_file, 1072, gen_session_id("20170518_0838"))
        write_session_info_row(fs_file, _DATACAMP_TEST_STORE_WHITE_FEED_ID, gen_session_id("20200421_1957"))
        write_session_info_row(fs_file, _DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, gen_session_id("20200421_1958"))
        write_session_info_row(fs_file, _DATACAMP_TEST_STORE_WHITE_FEED_ID + 2, gen_session_id("20200421_2332"))
        write_session_info_row(fs_file, _DATACAMP_TEST_STORE_WHITE_FEED_ID + 3, gen_session_id("20200421_2335"))

    push_feeds_path = yatest.common.test_output_path(FILE_TMP_PUSH_FEEDS)
    with open(push_feeds_path, 'w') as _f:
        feed_id = _DATACAMP_TEST_STORE_WHITE_FEED_ID
        for i in range(3):
            _f.write("{feed_id}\n".format(feed_id=feed_id + i))

    push_feeds_by_business_path = yatest.common.test_output_path(FILE_TMP_PUSH_FEEDS_BY_BUSINESS)
    with open(push_feeds_by_business_path, 'w') as _f:
        # business_id -> feed_id1 feed_id2 ...
        feeds = [str(_DATACAMP_TEST_STORE_WHITE_FEED_ID + i) for i in range(3)]
        _f.write("{business_id} {feeds}\n".format(business_id=_DATACAMP_TEST_BUSINESS_ID, feeds=" ".join(feeds)))
        _f.write("{business_id} {feeds}\n".format(business_id=_DATACAMP_TEST_BUSINESS_ID + 1, feeds=str(_DATACAMP_TEST_STORE_WHITE_FEED_ID + 3)))

    work_dir = yatest.common.test_output_path('wd')
    os.mkdir(work_dir)

    generation = "20170706_2007"
    cmdlist = [
        yatest.common.binary_path('market/idx/export/shop-categories-export/or3-shop-categories-export'),
        "--work-dir", work_dir,
        "--allowed-sessions", fs_path,
        "--push-feeds", push_feeds_path,
        "--push-feeds-by-business", push_feeds_by_business_path,
        "--generation", generation,
        "--yt-home-dir", "//home",
        "--yt-proxy", YT_SERVER.get_server(),
        "--datacamp-offers-table", datacamp_offers_table,
        "--datacamp-categories-table", datacamp_categories_table,
        "--tovar-tree", source_path('market/idx/feeds/cleaner/tests/regression/data/tovar-tree.pb')
    ]
    subprocess.check_call(cmdlist)

    all_tables = yt.list("//home/out/shop_categories")
    assert [generation, "recent"] == all_tables

    expected_categories = set([
        CategoryRow(feed_id=1069L, session_id=long(gen_session_id("20170518_0434")),
                    category_id="foo", parent_id=None, name="Foo"),
        CategoryRow(feed_id=1069L, session_id=long(gen_session_id("20170518_0434")),
                    category_id="bar", parent_id="foo", name="Bar"),
        CategoryRow(feed_id=1071L, session_id=long(gen_session_id("20170518_0836")),
                    category_id="foo", parent_id=None, name="Foo"),
        CategoryRow(feed_id=1071L, session_id=long(gen_session_id("20170518_0836")),
                    category_id="awol", parent_id="foo", name="Awol"),
        CategoryRow(feed_id=1072L, session_id=long(gen_session_id("20170518_0838")),
                    category_id="baz", parent_id=None, name="Baz"),
        CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, session_id=long(gen_session_id("20200421_1958")),
                    category_id="90914", parent_id="90829", name="Книги по искусству и культуре"),
        CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, session_id=long(gen_session_id("20200421_1958")),
                    category_id="90829", parent_id="90801", name="Книги"),
        CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, session_id=long(gen_session_id("20200421_1958")),
                    category_id="90801", parent_id="90401", name="Досуг и развлечения"),
        CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, session_id=long(gen_session_id("20200421_1958")),
                    category_id="90401", parent_id=None, name="Все товары"),
        CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 2, session_id=long(gen_session_id("20200421_2332")),
                    category_id="90401", parent_id=None, name="Все товары"),
    ])
    # Категории из Хранилища. Одинаковы для всех фидов всех магазинов одного business_id
    for feed_id, session in [(_DATACAMP_TEST_STORE_WHITE_FEED_ID, "20200421_1957"),
                             (_DATACAMP_TEST_STORE_WHITE_FEED_ID + 1, "20200421_1958"),
                             (_DATACAMP_TEST_STORE_WHITE_FEED_ID + 2, "20200421_2332")]:
        # партнер с 3 магазинами, 3 фидами и 3 категориями
        for category_id, parent_id in [("90401", None), ("90829", "90401"), ("90914", "90829")]:
            expected_categories.add(CategoryRow(feed_id=feed_id, session_id=long(gen_session_id(session)),
                                                category_id=category_id, parent_id=parent_id, name="category {}".format(category_id)))
        # партнер с 1 магазином, фидом и категорией
        expected_categories.add(CategoryRow(feed_id=_DATACAMP_TEST_STORE_WHITE_FEED_ID + 3, session_id=long(gen_session_id("20200421_2335")),
                                            category_id="12345", parent_id=None, name="category 12345"))

    actual_categories = set(
        CategoryRow(**row)
        for row in yt.read_table("//home/out/shop_categories/{}".format(generation))
    )
    assert expected_categories == actual_categories


bookstuff_maker_config_tmpl = '''
[yt]
yt_proxy = {proxy}
'''


def generate_bookstuff_maker_config(proxy):
    filename = 'test_bookstuff_maker.cfg'
    filepath = os.path.join(os.path.abspath(os.curdir), filename)
    with open(filepath, 'w') as f:
        f.write(bookstuff_maker_config_tmpl.format(proxy=proxy))
    return filepath


def generate_offer2pic_merger_config(proxy):
    return generate_bookstuff_maker_config(proxy)


def gen_indexer_raw(feed_id=1, session_id=1, offer_id='1',
                    offer=None, uc=None, pic=None):
    return dict(feed_id=feed_id, session_id=session_id, offer_id=offer_id,
                offer=offer, uc=uc, pic=pic)


def make_waremd5(oid):
    return base64.b64encode(md5.md5(oid).digest())


def test_or3_parameters_map_maker():
    """
    Проверяем код создания карты параметров из офферов

    Создаём таблицы mi3/offers.
    Проверяем, что на выходе получилась таблица mi3/colors/recent,
    в которой есть все нужные поля.
    """

    id_from = 1000
    id_to = 2000

    def create_offer_proto(offer_name):
        return gen_offer_proto(
            feed_id=1,
            yx_shop_offer_id=offer_name,
            URL="ya.ru/offers/{}".format(offer_name),
            picURLS="ya.ru/{}.jpg".format(offer_name),
            picUrlIds=[make_pic_id("ya.ru/{}.jpg".format(offer_name))],
            shop_name="YandexShop",
            ware_md5=make_waremd5(offer_name),
            description='sample {}'.format(offer_name)
        )

    def create_one_offer(model_id, name, parameter):
        params_map = []
        if len(parameter) == 2:
            if parameter[0]:
                params_map.append(FormalizedParamPosition(param_id=id_from, value_id=parameter[0]))
            if parameter[1]:
                params_map.append(FormalizedParamPosition(param_id=id_to, value_id=parameter[1]))

        uc = EnrichedOffer(model_id=model_id, category_id=3, params=params_map).SerializeToString()

        return gen_indexer_raw(
            feed_id=1, session_id=2, offer_id=name,
            offer=create_offer_proto(name),
            uc=uc,
            pic="PIC_{}".format(name)
        )

    def create_model(base, model_id, parameters):
        return [
            create_one_offer(model_id, str(base + i), parameters[i])
            for i in range(len(parameters))
        ]

    global YT_SERVER
    yt = YT_SERVER.get_yt_client()
    create_indexer_table(yt, "//home/mi3/offers/0000")

    model_parameters = {
        100: [
            (None, None),
            (None, 1),
            (2, None)
        ],
        200: [
            (1, 2),
            (3, 4)
        ],
        300: [
            (1, 2),
            (1, 4),
            (2, 8),
            (2, None)
        ],
        None: [     # no model_id
            (1, 2),
            (3, 4)
        ],
        0: [        # bad model_id
            (1, 2),
            (3, 4)
        ],
    }

    models_data = []
    for model_id, parameters in model_parameters.items():
        models_data.extend(create_model(model_id or 100000, model_id, parameters))

    assert len(models_data) == sum([len(v) for v in model_parameters.values()])

    data = {"//home/mi3/offers/0000": models_data}
    write_static_tables(yt, data)

    config_path = generate_bookstuff_maker_config(proxy=YT_SERVER.get_server())
    result_file_path = "./result.csv"
    cmdlist = [
        yatest.common.binary_path('market/idx/generation/parameters-map-maker/parameters-map-maker'),
        "--config", config_path,
        "--mi3offers", "//home/mi3/offers",
        "--output", "//home/mi3/parameters",
        "--from", str(id_from),
        "--to", str(id_to),
        "--download", result_file_path,
        "--keep", "3",
    ]
    subprocess.check_call(cmdlist)

    result = [r for r in yt.read_table("//home/mi3/parameters/recent")]
    assert len(result) > 0

    expected_table = [
        {
            'model_id': 200L,
            'param_from': 1L,
            'param_to': [2L]
        },
        {
            'model_id': 200L,
            'param_from': 3L,
            'param_to': [4L]
        },
        {
            'model_id': 300L,
            'param_from': 1L,
            'param_to': [2L, 4L]
        },
        {
            'model_id': 300L,
            'param_from': 2L,
            'param_to': [8L]
        }
    ]

    expected_file = "200; 1; 2\n200; 3; 4\n300; 1; 2 4\n300; 2; 8\n"

    assert expected_table == result
    with open(result_file_path) as res:
        data = res.read()
        assert expected_file == data


def test_offer2pic_merger():
    global YT_SERVER
    yt = YT_SERVER.get_yt_client()

    create_offer2pic_history_table(yt, '//home/mi3/offer2pic_history/0')
    yt.link('//home/mi3/offer2pic_history/0', '//home/mi3/offer2pic_history/recent')
    create_offer2pic_table(yt, '//home/mi3/work/offer2pic')
    create_successful_pics_table(yt, '//home/picrobot/success')

    now = int(time.time())
    THREE_WEEKS = 3 * 7 * 24 * 60 * 60
    data = {
        '//home/mi3/offer2pic_history/0': sorted([
            gen_offer2pic(id=make_pic_id('ya.ru/1'),
                          feed_id=1,
                          offer_id='1',
                          ts=now),
            gen_offer2pic(id=make_pic_id('ya.ru/2'),
                          feed_id=1,
                          offer_id='2',
                          ts=(now - THREE_WEEKS)),
            gen_offer2pic(id=make_pic_id('ya.ru/1'),
                          feed_id=1,
                          offer_id='3',
                          ts=now),

        ], key=lambda x: x.get('id')),
        '//home/mi3/work/offer2pic': sorted([
            gen_offer2pic(id=make_pic_id('ya.ru/X'),
                          feed_id=1,
                          offer_id='1',
                          descr_url=""),
            gen_offer2pic(id=make_pic_id('ya.ru/1'),
                          feed_id=1,
                          offer_id='3',
                          descr_url=""),
            gen_offer2pic(id=make_pic_id('ya.ru/1'),
                          feed_id=1,
                          offer_id='4',
                          descr_url=""),
        ], key=lambda x: x.get('id')),

        '//home/picrobot/success': sorted([
            {'id': make_pic_id('ya.ru/1'), 'pic': 'PIC1'},
            {'id': make_pic_id('ya.ru/2'), 'pic': 'PIC2'},
        ], key=lambda x: x.get('id'))
    }
    write_static_tables(yt, data)

    config_path = generate_offer2pic_merger_config(proxy=YT_SERVER.get_server())
    cmdlist = [
        yatest.common.binary_path('market/idx/pictures/offer2pic-merger/or3-offer2pic-merger'),
        '--config', config_path,
        '--o2pic', '//home/mi3/work/offer2pic',
        '--historical', '//home/mi3/offer2pic_history',
        '--successful-pics', '//home/picrobot/success',
        '--keep', '3',
    ]
    subprocess.check_call(cmdlist)

    rows = [r for r in yt.read_table("//home/mi3/offer2pic_history/recent")]
    assert len(rows) == 3  # make sure we don't get duplicate rows
    result = {(r['feed_id'], r['offer_id']): r for r in rows}
    assert len(result) == 3
    assert (1, "1") in result  # record present in history and absent in the current table
    assert (1, "2") not in result  # expired record
    assert (1, "3") in result  # record present in both history and the current table
    assert (1, "4") in result  # record present in the current table and absent in the history
