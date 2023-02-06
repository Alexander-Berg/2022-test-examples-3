# -*- coding: utf-8 -*-

''' Проверяем что промоакции корректно матчатся к dsbs-офферам в main-idx
'''

import pytest
import datetime
import time
from hamcrest import (
    assert_that,
    has_entries,
    all_of,
    is_not,
    has_key,
    contains_inanyorder,
)

from yt.wrapper import ypath_join

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.offers.yatf.test_envs.main_idx import Or3MainIdxTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.blue_promo_table import (
    BluePromoDetailsTable
)
from market.idx.offers.yatf.resources.idx_prepare_offers.data_raw_tables import (
    OffersRawTable,
    BlueOffersRawTable,
    Offers2ModelTable,
    Offers2ParamTable
)
from market.idx.generation.yatf.resources.prepare.in_picrobot_success import PicrobotSuccessTable
from market.idx.generation.yatf.resources.prepare.offer2pic import Offer2PicTable
from market.idx.generation.yatf.utils.fixtures import (
    CpaStatus,
    make_offer_proto_str,
    make_uc_proto_str,
)
from market.idx.offers.yatf.utils.fixtures import (
    make_proto_lenval_pictures,
    genererate_default_pictures
)

from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.proto.feedparser.Promo_pb2 import (
    PromoDetails
)
from market.pylibrary.const.offer_promo import PromoType

from market.idx.yatf.resources.msku_table import MskuContexTable
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)

GENERATION = datetime.datetime.now().strftime('%Y%m%d_%H%M')
SESSION_ID = int(time.time())
MI3_TYPE = 'main'
MSKU_FEED_ID = 9999

# обычный белый фид
WHITE_FEED_ID = 666
# белый фид с DSBS офферами у которых есть модель
DSBS_MODEL_FEED_ID = 667
# белый фид с DSBS офферами у которых есть и мску и модель
DSBS_MSKU_MODEL_FEED_ID = 668
# белый фид с DSBS офферами у которых нет ни модели (0) ни мску (-1)
DSBS_FEED_ID = 669

# пары из подходящих и неподходящих под условия акци параметров
VENDOR_I = 153061
VENDOR_II = VENDOR_I - 1

MSKU_I = 2519137
MSKU_II = 2519138

SHOP_I = 286639
SHOP_II = 286640

CATEGORY_I = 237418
CATEGORY_II = CATEGORY_I - 1

# offer id
WHITE_OFFER_ID = "simple_white_offer"
DSBS_MODEL_OFFER_ID = "dsbs_model_offer_id"
DSBS_MSKU_MODEL_OFFER_ID = "dsbs_msku_model_offer_id"
DSBS_OFFER_ID = "dsbs_offer_id"

I = "_i"
II = "_ii"

offers_data = {
    # обычный белый оффер. Кешбэка на нём не должно быть
    WHITE_FEED_ID : [{
        'offer_id': WHITE_OFFER_ID + suffix,
        'vendor_id': vendor_id,
        'model_id': 14197631,
        'is_dsbs': False,
        'category_id': category_id
    } for suffix, vendor_id, category_id in
        [(I, VENDOR_I, CATEGORY_I),
         (II, VENDOR_II, CATEGORY_II)]
    ],
    # DSBS оффер с информацией о модели. Данные берём из UC (Для общности логики всех dsbs)
    DSBS_MODEL_FEED_ID : [{
        'offer_id': DSBS_MODEL_OFFER_ID + suffix,
        'vendor_id': vendor_id,
        'model_id': 14197631,
        'is_dsbs': True,
        'category_id': category_id,
        'yx_ds_id' : shop,
    } for suffix, vendor_id, category_id, shop in
        [(I, VENDOR_I, CATEGORY_I, SHOP_I),
         (II, VENDOR_II, CATEGORY_II, SHOP_II)]
    ],
    # DSBS оффер с информацией о модели и MSKU. Данные берём из UC (Для общности логики всех dsbs)
    DSBS_MSKU_MODEL_FEED_ID : [{
        'offer_id': DSBS_MSKU_MODEL_OFFER_ID + suffix,
        'vendor_id': vendor_id,
        'model_id': 14197631,
        'msku_id': msku,
        'is_dsbs': True,
        'category_id': category_id,
        'yx_ds_id' : shop,
    } for suffix, vendor_id, category_id, shop, msku in
        [(I, VENDOR_I, CATEGORY_I, SHOP_I, MSKU_I),
         (II, VENDOR_II, CATEGORY_II, SHOP_II, MSKU_II)]
    ],
    # DSBS без информации о модели и MSKU. Данные берём из UC (Для общности логики всех dsbs)
    DSBS_FEED_ID : [{
        'offer_id': DSBS_OFFER_ID + suffix,
        'vendor_id': vendor_id,
        'is_dsbs': True,
        'category_id': category_id,
        'yx_ds_id' : shop,
    } for suffix, vendor_id, category_id, shop in
        [(I, VENDOR_I, CATEGORY_I, SHOP_I),
         (II, VENDOR_II, CATEGORY_II, SHOP_II)]],
}

ROOT_HID = 90401


def make_offer(feed_id, offer_id, params, blue=True):
    offer_params = {
        "cpa": CpaStatus.REAL if params.get("is_dsbs", False) else CpaStatus.NO,
        "offer_flags": params.get("flags", 0),
        "offer_flags64": params.get("flags", 0),
        "is_blue_offer": False,
        "is_fake_msku_offers": False,
        "yx_ds_id": params.get("yx_ds_id", 0)
    }
    if "msku_id" in params and blue:
        offer_params["msku_id"] = params["msku_id"]

    uc = {
        "market_sku_id": params.get("msku_id", -1),
        "model_id": params.get("model", -1),
        "vendor_id": params["vendor_id"],
        "category_id": params["category_id"]
    }
    result = {
        'feed_id': feed_id,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(**offer_params),
        'uc': make_uc_proto_str(**uc),
    }
    if "msku_id" in params and blue:
        result["msku"] = params["msku_id"]

    return result


@pytest.yield_fixture(scope="module")
def source_white_offers():
    return [
        make_offer(feed_id, offer_params['offer_id'], offer_params, blue=False)
        for feed_id, offers_params in offers_data.iteritems()
            for offer_params in offers_params
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers():
    feeds_for_blue = [DSBS_MSKU_MODEL_FEED_ID]
    return [
        make_offer(feed_id, offer_params['offer_id'], offer_params)
        for feed_id in feeds_for_blue
            for offer_params in offers_data[feed_id]
    ]

PROMO_BY_OFFER_ID = '1_bluecashback_by_offerid'
PROMO_BY_CATEGORY = '2_bluecashback_by_category'
PROMO_BY_VENDOR = '3_bluecashback_by_vendor'
PROMO_BY_SHOP = '4_bluecashback_by_shop'
PROMO_BY_MSKU = '5_bluecashback_by_msku'
PROMO_BY_MSKU_EXCLUDED = '6_bluecashback_by_msku_excluded'


@pytest.yield_fixture(scope="module")
def collected_promo_details_data():
    promo_details = [PromoDetails(  # Принимаем все офферы I группы
        shop_promo_id=PROMO_BY_OFFER_ID,
        binary_promo_md5='bluecashback_by_offerid',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        # не должно привязаться
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id=WHITE_OFFER_ID + I),
                        # должно привязаться
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=DSBS_MODEL_FEED_ID, offer_id=DSBS_MODEL_OFFER_ID + I),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=DSBS_MSKU_MODEL_FEED_ID, offer_id=DSBS_MSKU_MODEL_OFFER_ID + I),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=DSBS_FEED_ID, offer_id=DSBS_OFFER_ID + I),
                    ]
                )
            ),
        ]
    ),
    # принимаем все офферы I группы
    PromoDetails(
        shop_promo_id=PROMO_BY_CATEGORY,
        binary_promo_md5='bluecashback_by_category',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[CATEGORY_I, ]
                )
            ),
        ]
    ),
    # принимаем все офферы II категории
    PromoDetails(
        shop_promo_id=PROMO_BY_VENDOR,
        binary_promo_md5='bluecashback_by_vendor',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(vendors=PromoDetails.OffersMatchingRule.IdsList(ids=[VENDOR_II, ])),
        ]
    ),
    # принимаем все офферы II категории
    PromoDetails(
        shop_promo_id=PROMO_BY_SHOP,
        binary_promo_md5='bluecashback_by_shop',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                suppliers=PromoDetails.OffersMatchingRule.IdsList(ids=[SHOP_II, ])
            ),
        ]
    ),
    # msku у dsbs не заполнен.
    # принимаем все офферы I категории (только при заполненном MSKU)
    PromoDetails(
        shop_promo_id=PROMO_BY_MSKU,
        binary_promo_md5='bluecashback_by_msku',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[CATEGORY_I, CATEGORY_II, ]
                ),
                mskus=PromoDetails.OffersMatchingRule.IdsList(ids=[MSKU_I, ])
            ),
        ]
    ),
    # принимаем все офферы II категории через exclude (только при заполненном MSKU)
    PromoDetails(
        shop_promo_id=PROMO_BY_MSKU_EXCLUDED,
        binary_promo_md5='bluecashback_by_msku_excluded',
        type=PromoType.BLUE_CASHBACK,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[CATEGORY_I, CATEGORY_II, ]
                ),
                excluded_mskus=PromoDetails.OffersMatchingRule.IdsList(ids=[MSKU_I, ])
            ),
        ]
    )]

    return [
        {
            'feed_id': 0,
            'session_id': SESSION_ID,
            'promo_id': promo.shop_promo_id,
            'promo': promo.SerializeToString(),
        }
        for promo in promo_details
    ]


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=ROOT_HID,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=CATEGORY_I,
            tovar_id=1,
            parent_hid=ROOT_HID,
            unique_name="Good hid 1",
            name="Good hid 1",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=CATEGORY_II,
            tovar_id=2,
            parent_hid=ROOT_HID,
            unique_name="Bad hid 2",
            name="Bad hid 2",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.yield_fixture(scope="module")
def pipeline_params():
    return dict(
        shards=8,
        blue_shards=8,
    )


@pytest.fixture(scope='module')
def collected_promo_details_output_dir():
    return 'collected_promo_details'


@pytest.yield_fixture(scope="module")
def or3_config(collected_promo_details_output_dir):
    misc = {
        'blue_promo_reduce_enabled': True,
    }

    home_dir = get_yt_prefix()

    return Or3Config(**{
        'yt': {
            'home_dir': home_dir,
            'yt_blue_promo_gwp_table': ypath_join(home_dir, 'promos', 'blue', 'in', 'recent'),
            'yt_blue_cashback_table': ypath_join(home_dir, 'promos', 'blue', 'cashback', 'recent'),
            'yt_collected_promo_details_output_dir': collected_promo_details_output_dir,
        },
        'misc': misc,
    })


@pytest.yield_fixture(scope="module")
def shops_dat():
    return ShopsDat([
        {"datafeed_id": WHITE_FEED_ID},
        {"datafeed_id": DSBS_MODEL_FEED_ID},
        {"datafeed_id": DSBS_FEED_ID},
        {"datafeed_id": DSBS_MSKU_MODEL_FEED_ID},
    ])


@pytest.yield_fixture(scope="module")
def source_msku_contex():
    return [
        {
            'msku': id,
            'msku_exp': 0,
            'msku_experiment_id': '',
            'experimental_model_id': 0,
            'feed_id': MSKU_FEED_ID,
            'offer_id': "msku_" + str(id),
            'session_id': SESSION_ID,
            'offer': make_offer_proto_str(
                market_sku=id,
                offer_flags=OfferFlags.MARKET_SKU.value,
                category_id=CATEGORY_I,
                is_fake_msku_offer=True,
            ),
            'uc': make_uc_proto_str(
                category_id=category,
                vendor_id=vendor,
            ),
            'pic': make_proto_lenval_pictures(genererate_default_pictures()),
        }
        for id, category, vendor in [
            (MSKU_I, CATEGORY_I, VENDOR_I),
            (MSKU_II, CATEGORY_II, VENDOR_II)
        ]
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config,
                     source_white_offers,
                     source_blue_offers,
                     source_msku_contex,
                     collected_promo_details_output_dir,
                     collected_promo_details_data
                     ):
    yt_home_path = or3_config.options['yt']['home_dir']

    return {
        'offers_raw': OffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offers_raw'),
            data=source_white_offers
        ),
        'offer2pic': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic'),
            data=[],
            sort_key="id"
        ),
        'blue_offers_raw': BlueOffersRawTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'blue_offers_raw'),
            data=source_blue_offers
        ),
        'offer2pic_unsorted': Offer2PicTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2pic_unsorted'),
            data={}
        ),
        'offer2model_unsorted': Offers2ModelTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2model_unsorted'),
            data={}
        ),
        'offer2param_unsorted': Offers2ParamTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'work', 'offer2param_unsorted'),
            data={}
        ),
        'msku': MskuContexTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'mi3', MI3_TYPE, GENERATION, 'input', 'msku_contex'),
            data=source_msku_contex,
        ),
        'in_picrobot_success': PicrobotSuccessTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'picrobot', 'success', 'recent'),
            data=[]
        ),
        'collected_promo_details_table': BluePromoDetailsTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, collected_promo_details_output_dir, 'recent'),
            data=collected_promo_details_data,
        ),
    }


@pytest.yield_fixture(scope="module")
def main_idx(yt_server, or3_config, source_yt_tables, shops_dat, pipeline_params, tovar_tree):
    resources = {
        'config': or3_config,
        'shops_dat': shops_dat,
        'tovar_tree_pb': TovarTreePb(tovar_tree)
    }
    resources.update(source_yt_tables)

    with Or3MainIdxTestEnv(
            yt_stuff=yt_server,
            generation=GENERATION,
            mi3_type=MI3_TYPE,
            shards=pipeline_params['shards'],
            half_mode=False,
            blue_shards=pipeline_params['blue_shards'],
            **resources
    ) as mi:
        mi.execute()
        mi.verify()
        yield mi


def test_offers_count(
        main_idx,
        source_blue_offers,
        source_white_offers,
        source_msku_contex,
):
    ''' Проверяeм количество офферов в выходной синей и белой таблицах
    '''

    actual_count = len(main_idx.outputs['blue_offers'])
    assert actual_count > 0
    expected_count = len(source_blue_offers) + len(source_msku_contex)
    assert expected_count > 0
    assert actual_count == expected_count

    actual_count = len(main_idx.outputs['offers'])
    assert actual_count > 0
    expected_count = len(source_white_offers) + len(source_blue_offers) + len(source_msku_contex) - 2  # два оффера и в белом и синем
    assert expected_count > 0
    assert actual_count == expected_count


def test_dsbs_promo(main_idx):
    ''' Проверяeм что в выходной белой таблице без флага applyPromosForDsbsOffers и
        с флагом=False промо к белым dsbs-офферам не применяются, а с флагом=True - применяются
    '''

    offers = main_idx.outputs['offers_by_offer_id']
    blue_offers = main_idx.outputs['blue_offers_by_offer_id']
    # PROMO_BY_OFFER_ID      Принимаем все dsbs офферы I группы
    # PROMO_BY_CATEGORY      Принимаем все dsbs офферы I группы
    # PROMO_BY_VENDOR        Принимаем все dsbs офферы II группы
    # PROMO_BY_SHOP          Принимаем все dsbs офферы II группы
    # PROMO_BY_MSKU          Принимаем все dsbs офферы I группы
    # PROMO_BY_MSKU_EXCLUDED Принимаем все dsbs офферы II группы

    def build_entries(promo_ids):
        return has_entries({
            'promo' : has_entries({
                'shop_promo_ids': contains_inanyorder(*promo_ids),
            })
        })

    cases = [
        (
            # массив проверяемых офферов
            [(WHITE_FEED_ID, WHITE_OFFER_ID + I), (WHITE_FEED_ID, WHITE_OFFER_ID + II)],
            # словарь вида (apply_promos_for_dsbs_offers, apply_cashback_for_dsbs_offers) -> predicate
            # None ключ - показывает дефолтное значение
            {
                None : is_not(has_key('promo')),
            },
            # сообщение при ошибке
            'white offer promo check failed',
            # надо ли проверять в синем шарде
            False
        ),
        (
            [(DSBS_FEED_ID, DSBS_OFFER_ID + I), (DSBS_MODEL_FEED_ID, DSBS_MODEL_OFFER_ID + I), ],
            {
                # пока для офферов с незаполненным msku считаем отрицательное условие по msku пройденными
                (True, True) : build_entries([PROMO_BY_OFFER_ID, PROMO_BY_CATEGORY, PROMO_BY_MSKU_EXCLUDED]),
                None : is_not(has_key('promo')),
            },
            'dsbs without msku check failed',
            False
        ),
        (
            [(DSBS_FEED_ID, DSBS_OFFER_ID + II), (DSBS_MODEL_FEED_ID, DSBS_MODEL_OFFER_ID + II), ],
            {
                # пока для офферов с незаполненным msku считаем все условия по msku пройденными
                (True, True) : build_entries([PROMO_BY_SHOP, PROMO_BY_VENDOR, PROMO_BY_MSKU_EXCLUDED]),
                None : is_not(has_key('promo')),
            },
            'dsbs without msku check failed group II check failed',
            False
        ),
        (
            [(DSBS_MSKU_MODEL_FEED_ID, DSBS_MSKU_MODEL_OFFER_ID + I), ],
            {
                (True, True) : build_entries([PROMO_BY_CATEGORY, PROMO_BY_MSKU, PROMO_BY_OFFER_ID]),
                None : is_not(has_key('promo')),
            },
            'dsbs with msku check failed',
            True
        ),
        (
            [(DSBS_MSKU_MODEL_FEED_ID, DSBS_MSKU_MODEL_OFFER_ID + II), ],
            {
                (True, True) : build_entries([PROMO_BY_SHOP, PROMO_BY_VENDOR, PROMO_BY_MSKU_EXCLUDED]),
                None : is_not(has_key('promo')),
            },
            'dsbs with msku group II check failed',
            True
        )
    ]
    for feedofferids, pred_dict, description, blue in cases:
        for feedofferid in feedofferids:
            feed_id, offer_id = feedofferid
            predicate = pred_dict.get((True, True), pred_dict[None])
            shards = [(offers, "[WHITE]"), ]
            if blue:
                shards.append((blue_offers, "[BLUE]"))
            for shard, prefix in shards:
                assert_that(
                    offers[offer_id],
                    all_of(
                        has_entries({
                            'feed_id': feed_id,
                            'offer_id': offer_id,
                        }),
                        predicate
                    ),
                    "{} {}".format(prefix, description)
                )
