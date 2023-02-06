# -*- coding: utf-8 -*-

""" Проверяем что белые cpa офера из синих табличке проходят main-idx нормально,
    не обогащаются из msku, не дублируются в белом шарде и офера одного msku лежат рядом """

import pytest
import datetime
import time
from hamcrest import assert_that, has_entries, has_item, only_contains, all_of, any_of, is_not, has_key, equal_to, contains_inanyorder

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
    Offers2ParamTable,
    HonestDiscountTable
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

BLUE_FEED_ID = 8765
WHITE_FEED_ID = 666
MSKU_FEED_ID = 9999

ROOT_HID = 90401
DEFAULT_HID = 13

ACME_CORP_VENDOR = 7


def make_offer(feed, offer_id, msku, flags, for_white_table=False):
    flags = flags if flags else 0
    result = {
        'feed_id': feed,
        'offer_id': offer_id,
        'session_id': SESSION_ID,
        'offer': make_offer_proto_str(
            market_sku=msku,
            offer_flags=flags % (1 << 32),
            offer_flags64=flags,
            cpa=CpaStatus.REAL,
            is_blue_offer=bool(flags & OfferFlags.BLUE_OFFER.value),
            is_fake_msku_offer=False,
        ),
        'uc': make_uc_proto_str(
            market_sku_id=msku,
            category_id=DEFAULT_HID,
        ),
    }
    if not for_white_table and msku is not None:
        result['msku'] = msku

    return result


def make_blue_offer(feed, offer_id, msku, flags=None):
    flags = (flags if flags else 0) | OfferFlags.BLUE_OFFER.value
    return make_offer(feed, offer_id, msku, flags)


def make_white_offer(feed, offer_id, msku=None, flags=None, for_white_table=False):
    return make_offer(feed, offer_id, msku, flags, for_white_table)


@pytest.yield_fixture(scope="module")
def source_white_offers():
    return [
        make_white_offer(WHITE_FEED_ID, 'white_offer_1', 1, OfferFlags.ADULT.value, for_white_table=True),
        make_white_offer(WHITE_FEED_ID, 'white_offer_2', None, 0, for_white_table=True),
    ]


@pytest.yield_fixture(scope="module")
def source_blue_offers():
    return [
        make_blue_offer(BLUE_FEED_ID, 'blue_offer_1', 1),
        make_white_offer(WHITE_FEED_ID, 'white_offer_1', 1, OfferFlags.PICKUP.value),
        make_white_offer(WHITE_FEED_ID, 'white_offer_3', 2),
        make_white_offer(WHITE_FEED_ID, 'eda_offer', 1, OfferFlags.IS_EDA.value),
        make_white_offer(WHITE_FEED_ID, 'lavka_offer', 1, OfferFlags.IS_LAVKA.value),
    ]


@pytest.yield_fixture(scope="module")
def promo_details_cheapest_as_a_gift():
    return PromoDetails(
        shop_promo_id='promo_details_cheapest_as_a_gift',
        binary_promo_md5='promo_details_cheapest_as_a_gift_md5',
        type=PromoType.CHEAPEST_AS_GIFT,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                category_restriction=PromoDetails.OffersMatchingRule.CategoryRestriction(
                    categories=[
                        DEFAULT_HID,
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def promo_details_generic_bundle():
    return PromoDetails(
        shop_promo_id='promo_details_generic_bundle',
        binary_promo_md5='promo_details_generic_bundle_md5',
        type=PromoType.GENERIC_BUNDLE,
        offers_matching_rules=[
            PromoDetails.OffersMatchingRule(
                feed_offer_ids=PromoDetails.OffersMatchingRule.FeedOfferIdsList(
                    ids=[
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=BLUE_FEED_ID, offer_id='blue_offer_1'),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id='white_offer_1'),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id='eda_offer'),
                        PromoDetails.OffersMatchingRule.FeedOfferId(feed_id=WHITE_FEED_ID, offer_id='lavka_offer'),
                    ]
                )
            ),
        ]
    )


@pytest.yield_fixture(scope="module")
def collected_promo_details_data(promo_details_cheapest_as_a_gift, promo_details_generic_bundle):
    return [
        {
            'feed_id': BLUE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_cheapest_as_a_gift.shop_promo_id,
            'promo': promo_details_cheapest_as_a_gift.SerializeToString(),
        },
        {
            'feed_id': BLUE_FEED_ID,
            'session_id': SESSION_ID,
            'promo_id': promo_details_generic_bundle.shop_promo_id,
            'promo': promo_details_generic_bundle.SerializeToString(),
        },
    ]


@pytest.fixture(scope='module')
def collected_promo_details_output_dir():
    return 'collected_promo_details'


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
            hid=DEFAULT_HID,
            tovar_id=1,
            parent_hid=ROOT_HID,
            unique_name="Leaf hid 1",
            name="Leaf hid 1",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


@pytest.yield_fixture(scope="module")
def pipeline_params():
    return dict(
        shards=8,
        blue_shards=8,
    )


@pytest.fixture(
    scope='module',
    params=[
        None,
        True,
        False
    ],
    ids=[
        'apply_dsbs_promos_flag_is_None',
        'apply_dsbs_promos_flag_is_True',
        'apply_dsbs_promos_flag_is_False'
    ]
)
def apply_promos_for_dsbs_offers_with_msku(request):
    return request.param


@pytest.fixture(
    scope="module",
    params=[
        None,
        True,
        False
    ],
    ids=[
        'add_white_cpa_to_blue_shard_is_None',
        'add_white_cpa_to_blue_shard_is_True',
        'add_white_cpa_to_blue_shard_is_False'
    ]
)
def add_white_cpa_to_blue_shard(request):
    return request.param


@pytest.fixture(scope="module")
def source_honest_discount():
    return [
        {'msku': 1, 'max_price': 3000000000., 'max_old_price': 4000000000.},  # поля должны приклеиться к синему офферу и дсбсу с мскю
        {'feed_id': WHITE_FEED_ID, 'offer_id': 'white_offer_2', 'max_price': 600., 'max_old_price': 666.},  # поля должны приклеиться к дсбсу без мскю
    ]


@pytest.yield_fixture(scope="module")
def or3_config(apply_promos_for_dsbs_offers_with_msku, add_white_cpa_to_blue_shard, collected_promo_details_output_dir):
    home_dir = get_yt_prefix()
    misc = {
        'blue_promo_reduce_enabled': True,
    }
    if add_white_cpa_to_blue_shard is not None:
        misc['add_white_cpa_to_blue_shard'] = add_white_cpa_to_blue_shard
    if apply_promos_for_dsbs_offers_with_msku is not None:
        misc['apply_promos_for_dsbs_offers_with_msku'] = apply_promos_for_dsbs_offers_with_msku

    return Or3Config(**{
        'yt': {
            'home_dir': home_dir,
            'yt_collected_promo_details_output_dir': collected_promo_details_output_dir,
            'yt_honest_discount_table': ypath_join(home_dir, 'in', 'blue', 'honest_discount', 'recent'),
            'yt_use_new_honest_discount_algo': 'true',
        },
        'lavka': {
            'indexation_enabled': 'true',
        },
        'eda': {
            'indexation_enabled': 'true',
        },
        'misc': misc,
    })


@pytest.yield_fixture(scope="module")
def shops_dat():
    return ShopsDat([
        {"datafeed_id": BLUE_FEED_ID},
        {"datafeed_id": WHITE_FEED_ID},
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
                category_id=DEFAULT_HID,
                is_fake_msku_offer=True,
            ),
            'uc': make_uc_proto_str(
                category_id=DEFAULT_HID,
                vendor_id=ACME_CORP_VENDOR
            ),
            'pic': make_proto_lenval_pictures(genererate_default_pictures()),
        }
        for id in [1, 2]
    ]


@pytest.yield_fixture(scope="module")
def source_yt_tables(yt_server,
                     or3_config,
                     source_white_offers,
                     source_blue_offers,
                     collected_promo_details_data,
                     collected_promo_details_output_dir,
                     source_msku_contex,
                     source_honest_discount,
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
        'honest_discount': HonestDiscountTable(
            yt_stuff=yt_server,
            path=ypath_join(yt_home_path, 'in', 'blue', 'honest_discount', 'recent'),
            data=source_honest_discount,
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


def test_len(
    main_idx,
    source_blue_offers,
    source_white_offers,
    source_msku_contex,
    add_white_cpa_to_blue_shard,
):
    """ Проверям что в выходной синей табличке 5 оферов - 2 белых, 1 синий и 2 мскю
        В выходной белой 5 оферов - 1 белый из белых табличек (white_offer_2)
        1 синий из синих и 2 msku
        Если флаг выключен - в белом шарде 6 оферов (добавляются 2 белых офера из синих табличек)"""

    actual_count = len(main_idx.outputs['blue_offers'])
    assert actual_count > 0
    expected_cout = len(source_blue_offers) + len(source_msku_contex)
    assert expected_cout > 0
    assert actual_count == expected_cout

    actual_count = len(main_idx.outputs['offers'])
    assert actual_count > 0
    if add_white_cpa_to_blue_shard:
        expected_cout = len(source_white_offers) + 1 + len(source_msku_contex)
    else:
        expected_cout = len(source_white_offers) + len(source_blue_offers) - 1 + len(source_msku_contex)

    assert expected_cout > 0
    assert actual_count == expected_cout


def test_enrich(
    main_idx
):
    """ Проверям что в выходной синей табличке для белых оферов не копируются клонки
        'offers', 'uc', 'pic' из msku, а для синих - копируются """

    assert_that(
        main_idx.outputs['blue_offers'],
        has_item(has_entries({
            'feed_id': BLUE_FEED_ID,
            'offer_id': "blue_offer_1",
            'offer': has_entries({
                'offer_flags64': str(OfferFlags.BLUE_OFFER.value),  # offer_flags64 is str? wtf?
                'market_sku': '1',
                'is_blue_offer': True,
            }),
            'uc': has_entries({
                'vendor_id': ACME_CORP_VENDOR,
                'category_id': DEFAULT_HID,
                'market_sku_id': '1'
            }),
            'pic': only_contains(has_entries({
                "thumb_mask": "292591",
                "height": 500,
                "width": 500,
                "group_id": 5,
            })),
        })),
        'blue offer doesn\'t enriched'
    )

    assert_that(
        main_idx.outputs['blue_offers'],
        has_item(has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': "white_offer_1",
            'offer': has_entries({
                'offer_flags64': str(OfferFlags.PICKUP.value),
                'market_sku': '1',
                'is_blue_offer': False,
                'cpa': 4
            }),
            'uc': all_of(
                has_entries({'market_sku_id': '1'}),
                is_not(any_of(has_key('vendor_id'),)),
            ),
            'pic': only_contains(has_entries({
                "thumb_mask": "292591",
                "height": 500,
                "width": 500,
                "group_id": 5,
            })),
        })),
        'white offer enriched'
    )


def test_promo(
    main_idx,
    promo_details_generic_bundle,
    promo_details_cheapest_as_a_gift,
    apply_promos_for_dsbs_offers_with_msku,
):
    """ Проверям что в выходной синей табличке для белых оферов не применяются промки
        а для синих - применяются """

    assert_that(
        main_idx.outputs['blue_offers'],
        has_item(has_entries({
            'feed_id': BLUE_FEED_ID,
            'offer_id': "blue_offer_1",
            'promo': has_entries({
                "shop_promo_ids": contains_inanyorder(promo_details_cheapest_as_a_gift.shop_promo_id, promo_details_generic_bundle.shop_promo_id),
            })
        })),
        'blue offer has promo'
    )

    if apply_promos_for_dsbs_offers_with_msku is True:
        assert_that(
            main_idx.outputs['blue_offers'],
            has_item(
                has_entries({
                    'feed_id': WHITE_FEED_ID,
                    'offer_id': "white_offer_1",
                    'promo': has_entries({
                        "shop_promo_ids": contains_inanyorder(promo_details_cheapest_as_a_gift.shop_promo_id, promo_details_generic_bundle.shop_promo_id),
                    })
                }),
            ),
            'white offer has promo with apply_dsbs_promos flag'
        )
    else:
        assert_that(
            main_idx.outputs['blue_offers'],
            has_item(
                all_of(
                    has_entries({
                        'feed_id': WHITE_FEED_ID,
                        'offer_id': "white_offer_1",
                    }),
                    is_not(has_key('promo'))
                )
            ),
            'white offer does not have promo without apply_dsbs_promos flag'
        )

    # для офферов из еды и лавки промо не применяется
    for offer_type in ('lavka', 'eda'):
        assert_that(
            main_idx.outputs['blue_offers'],
            has_item(
                all_of(
                    has_entries({
                        'feed_id': WHITE_FEED_ID,
                        'offer_id': '{}_offer'.format(offer_type),
                    }),
                    is_not(has_key('promo'))
                )
            ),
            '{} offers can\'t have promo'.format(offer_type)
        )


def test_resharding(main_idx):
    """ Проверяем что dsbs оффер c msku лежит в тех же синих шардах что и синие офера
        с тем же msku """

    offerid_to_shard = dict()
    for shard_num, table in enumerate(main_idx.outputs['blue_offers_shards']):
        for offer in table:
            offerid_to_shard[offer['offer_id']] = shard_num

    assert_that(
        offerid_to_shard['white_offer_1'],
        equal_to(offerid_to_shard['blue_offer_1']),
        'blue and white offer with same msku in same shard'
    )

    assert_that(
        offerid_to_shard['white_offer_1'],
        is_not(equal_to(offerid_to_shard['white_offer_3'])),
        'different msku leads to the different shards'
    )


def test_deduplication(main_idx, add_white_cpa_to_blue_shard):
    """ Проверяем что офер с одним и тем же feed-id + offer-id в белой и синей входных табличках не дублируется
        в выходной белой табилчке из-за копирования синих оферов в белые """

    # white_offer_2 - белый офер из белой таблички, должен попасть на выход в любом случае
    assert_that(
        main_idx.outputs['offers'],
        has_item(has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': "white_offer_2",
        })),
        'white output tables has white cpa offer from white source table'
    )

    if add_white_cpa_to_blue_shard:
        # white_offer_1 с флагом ADULT - белый офер из белой таблички, должен попасть на выход
        assert_that(
            main_idx.outputs['offers'],
            has_item(has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': "white_offer_1",
                'offer': has_entries({
                    'offer_flags64': str(OfferFlags.ADULT.value)
                })
            })),
            'white output tables has white cpa offer from white source table'
        )

        # white_offer_1 с флагом PICKUP - белый офер из синей таблички, НЕ должен попасть на выход
        assert_that(
            main_idx.outputs['offers'],
            is_not(has_item(has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': "white_offer_1",
                'offer': has_entries({
                    'offer_flags64': str(OfferFlags.PICKUP.value)
                }),
            }))),
            'white output tables has not white cpa offer from blue source table'
        )

        # white_offer_3 - белый офер из синей таблички, НЕ должен попасть на выход
        assert_that(
            main_idx.outputs['offers'],
            is_not(has_item(has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': "white_offer_3",
            }))),
            'white output tables has not white cpa offer from blue source table'
        )

    else:
        # white_offer_1 (либо с флагом ADULT либо PICKUP) - белый офер из белой таблички, должен попасть на выход
        # но мы не можем точно знать какой именно попадет на выход
        assert_that(
            main_idx.outputs['offers'],
            has_item(has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': "white_offer_1",
            })),
            'white output tables has white cpa offer from blue source table'
        )

        assert_that(
            main_idx.outputs['offers'],
            has_item(has_entries({
                'feed_id': WHITE_FEED_ID,
                'offer_id': "white_offer_3",
            })),
            'white output tables has white cpa offer from blue source table'
        )


def test_honest_discount_merge(main_idx):
    """Проверяем, что к синему, дбсу с мскю и дбсу без мскю приклеиваются max_price и max_old_price"""

    assert_that(
        main_idx.outputs['blue_offers'],
        has_item(has_entries({
            'feed_id': BLUE_FEED_ID,
            'offer_id': "blue_offer_1",
            'offer': has_entries({
                'max_price': 3000000000.,
                'max_old_price': 4000000000.,
            }),
        })),
        'blue offer has max_prices'
    )

    assert_that(
        main_idx.outputs['blue_offers'],
        has_item(has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': "white_offer_1",
            'offer': has_entries({
                'max_price': 3000000000.,
                'max_old_price': 4000000000.,
            }),
        })),
        'dsbs with msku has max_prices'
    )

    assert_that(
        main_idx.outputs['offers'],
        has_item(has_entries({
            'feed_id': WHITE_FEED_ID,
            'offer_id': "white_offer_2",
            'genlog': has_entries({
                'max_price': {
                    'price': '600',
                },
                'max_old_price': {
                    'price': '666',
                },
            }),
        })),
        'dsbs without msku has max_prices'
    )
