# coding: utf-8

import pytest
import yatest.common
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.resources.offers_indexer.promo_details import PromoDetails
from market.idx.offers.yatf.utils.fixtures import(
    default_shops_dat,
    generate_binary_price_dict,
    generate_default_promo,
    generate_default_blue_3p_promo,
    generate_default_msku,
    default_blue_genlog,
    default_genlog,
    binary_promos_md5_base64
)
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.pylibrary.const.offer_promo import PromoType, MechanicsPaymentType

from market.idx.yatf.utils.mmap.promo_indexer_write_mmap import write_promo_json_to_mmap
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


DT_FORMAT = '%Y%m%d_%H%M'
FEED_ID=101967
FEED_ID_DSBS=212121

LITERAL_STRING = '#match_blue_promo="{promo}'
ANAPLAN_LITERAL_STRING = '#anaplan_promo_id="{id}'
PARENT_PROMO_LITERAL_STRING = '#parent_promo_id="{parent_id}'
PROMO_TYPE_LITERAL_STRING = '#promo_type="{promo_type}'

SECRET_SALE_PROMO_KEY = 'secret_sale_promo_key'
SECRET_SALE_DISCOUNT_PERCENT = 10

NEW_SECRET_SALE_PROMO_KEY = 'new_secret_sale_promo_key'
NEW_SECRET_SALE_DISCOUNT_PERCENT = 15

FLASH_DISCOUNT_PROMO_KEY = 'flash_discount_promo_key'

GENERIC_BUNDLE_SHOP_PROMO_ID = 'generic_bundle_shop_promo_id'
GENERIC_BUNDLE_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'generic_bundle_binary_promo_md5')

CHEAPEST_AS_GIFT_SHOP_PROMO_ID = 'cheapest_as_gift_shop_promo_id'
CHEAPEST_AS_GIFT_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'cheapest_as_gift_binary_promo_md5')

BLUE_FLASH_SHOP_PROMO_ID = 'blue_flash_shop_promo_id'
BLUE_FLASH_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'blue_flash_binary_promo_md5')

BLUE_SET_SHOP_PROMO_ID = 'blue_set_shop_promo_id'
BLUE_SET_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'blue_set_binary_promo_md5')

DIRECT_DISCOUNT_SHOP_PROMO_ID = 'direct_discount_shop_promo_id'
DIRECT_DISCOUNT_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'direct_discount_binary_promo_md5')

CHEAPEST_AS_GIFT_DSBS_PROMO_KEY = 'CHEAPEST_AS_GIFT_DSBS_PROMO_KEY'

DIRECT_DISCOUNT_2_SHOP_PROMO_ID = 'direct_discount_2_shop_promo_id'
DIRECT_DISCOUNT_2_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'direct_discount_2_binary_promo_md5')

PROMOCODE_SHOP_PROMO_ID = 'promocode_shop_promo_id'
PROMOCODE_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'promocode_binary_promo_md5')

BLUE_CASHBACK_SHOP_PROMO_ID = 'blue_cashback_shop_promo_id'
BLUE_CASHBACK_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'blue_cashback_binary_promo_md5')

DIRECT_DISCOUNT_PARENT_PROMO_ID_SHOP_PROMO_ID = 'direct_discount_parent_shop_promo_id'
DIRECT_DISCOUNT_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'direct_discount_parent_binary_promo_md5')

PROMOCODE_PARENT_PROMO_ID_SHOP_PROMO_ID = 'promocode_parent_shop_promo_id'
PROMOCODE_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'promocode_parent_binary_promo_md5')

BLUE_CASHBACK_PARENT_PROMO_ID_SHOP_PROMO_ID = 'blue_cashback_parent_shop_promo_id'
BLUE_CASHBACK_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64 = binary_promos_md5_base64(b'blue_cashback_parent_binary_promo_md5')


ANAPLAN_ID = 'anaplan_id'
PARENT_PROMO_ID = 'parent_promo'

SECRET_SALE_OFFER = default_blue_genlog(
    offer_id='secret_sale_shop_offer_id',
    title="Тестовый синий оффер по распродаже",
    market_sku=123456,
    ware_md5='secretsaleofferwaremd5'
)

FLASH_DISCOUNT_OFFER = default_blue_genlog(
    offer_id='flash_discount_shop_offer_id',
    title="Тестовый синий оффер с флеш-скидкой",
    market_sku=123457,
    ware_md5='-flashdiscountwaremd5-',
    binary_price=generate_binary_price_dict(150)
)

SALE_OFFER_WITH_FLASH_DISCOUNT = default_blue_genlog(
    offer_id='sale_offer_with_flash_discount',
    title="Тестовый синий оффер по распродаже и с флеш-скидкой",
    market_sku=123458,
    ware_md5='flashsaleofferwaremd5-',
    binary_price=generate_binary_price_dict(150)
)

PLAIN_OFFER = default_blue_genlog(
    offer_id='plain_offer',
    title="Тестовый синий оффер",
    market_sku=123459,
    ware_md5='--plainofferwaremd5---'
)

NEW_AND_OLD_SECRET_SALE_OFFER = default_blue_genlog(
    offer_id='new_and_old_sale_shop_offer_id',
    title="Тестовый синий оффер, участвующий в новой и старой распродаже",
    market_sku=123460,
    ware_md5='oldnewsaleofferwaremd5'
)

GENERIC_BUNDLE_OFFER_1 = default_blue_genlog(
    offer_id='GENERIC_BUNDLE_OFFER_1',
    title="GENERIC_BUNDLE_OFFER_1",
    market_sku=123461,
    ware_md5='GENERIC_BUNDLE_OFFER_1',
    promo_type=PromoType.GENERIC_BUNDLE,
    binary_promos_md5_base64=[GENERIC_BUNDLE_BINARY_PROMO_MD5_BASE64],
)

GENERIC_BUNDLE_OFFER_2 = default_blue_genlog(
    offer_id='GENERIC_BUNDLE_OFFER_2',
    title="GENERIC_BUNDLE_OFFER_2",
    market_sku=123462,
    ware_md5='GENERIC_BUNDLE_OFFER_2',
)

CHEAPEST_AS_GIFT_OFFER_1 = default_blue_genlog(
    offer_id='CHEAPEST_AS_GIFT_OFFER_1',
    title="CHEAPEST_AS_GIFT_OFFER_1",
    market_sku=123463,
    ware_md5='CHEAP_AS_GIFT_OFFER__1',
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    binary_promos_md5_base64=[CHEAPEST_AS_GIFT_BINARY_PROMO_MD5_BASE64],
)

CHEAPEST_AS_GIFT_OFFER_2 = default_blue_genlog(
    offer_id='CHEAPEST_AS_GIFT_OFFER_2',
    title="CHEAPEST_AS_GIFT_OFFER_2",
    market_sku=123464,
    ware_md5='CHEAP_AS_GIFT_OFFER__2',
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    binary_promos_md5_base64=[CHEAPEST_AS_GIFT_BINARY_PROMO_MD5_BASE64],
)

BLUE_FLASH_OFFER_1 = default_blue_genlog(
    offer_id='BLUE_FLASH_OFFER_1',
    title="BLUE_FLASH_OFFER_1",
    market_sku=123465,
    ware_md5='BLUE_FLASH____OFFER__1',
    promo_type=PromoType.BLUE_FLASH,
    binary_promos_md5_base64=[BLUE_FLASH_BINARY_PROMO_MD5_BASE64],
)

BLUE_SET_OFFER_1 = default_blue_genlog(
    offer_id='BLUE_SET_OFFER_1',
    title="BLUE_SET_OFFER_1",
    market_sku=123466,
    ware_md5='BLUE_SET_______OFFER_1',
    promo_type=PromoType.BLUE_SET,
    binary_promos_md5_base64=[BLUE_SET_BINARY_PROMO_MD5_BASE64],
)

BLUE_SET_OFFER_2 = default_blue_genlog(
    offer_id='BLUE_SET_OFFER_2',
    title="BLUE_SET_OFFER_2",
    market_sku=123467,
    ware_md5='BLUE_SET_______OFFER_2',
    promo_type=PromoType.BLUE_SET,
    binary_promos_md5_base64=[BLUE_SET_BINARY_PROMO_MD5_BASE64],
)

DIRECT_DISCOUNT_OFFER_1 = default_blue_genlog(
    offer_id='DIRECT_DISCOUNT_OFFER_1',
    title='DIRECT_DISCOUNT_OFFER_1',
    market_sku=123468,
    ware_md5='DIRECT_DISCOUNTOFFER_1',
    promo_type=PromoType.DIRECT_DISCOUNT,
    binary_promos_md5_base64=[DIRECT_DISCOUNT_BINARY_PROMO_MD5_BASE64],
)

# Оффер участвует в прямой скидке на категорию
DIRECT_DISCOUNT_OFFER_2 = default_blue_genlog(
    offer_id='DIRECT_DISCOUNT_OFFER_2',
    title='DIRECT_DISCOUNT_OFFER_2',
    market_sku=123469,
    ware_md5='DIRECT_DISCOUNTOFFER_2',
    promo_type=PromoType.DIRECT_DISCOUNT,
    binary_promos_md5_base64=[DIRECT_DISCOUNT_2_BINARY_PROMO_MD5_BASE64],
)

DIRECT_DISCOUNT_PARENT_PROMO_ID_OFFER_3 = default_blue_genlog(
    offer_id='DIRECT_DISCOUNT_PARENT_PROMO_ID_OFFER_2',
    title='DIRECT_DISCOUNT_PARENT_PROMO_ID_OFFER_2',
    market_sku=123469,
    ware_md5='DIRECT_DISCOUNTOFFER_3',
    promo_type=PromoType.DIRECT_DISCOUNT,
    binary_promos_md5_base64=[DIRECT_DISCOUNT_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64],
)

PROMOCODE_OFFER_1 = default_blue_genlog(
    offer_id='PROMOCODE_OFFER_1',
    title='PROMOCODE_OFFER_1',
    market_sku=123470,
    ware_md5='PROMOCODE_OFFER_000001',
    promo_type=PromoType.PROMO_CODE,
    binary_promos_md5_base64=[PROMOCODE_BINARY_PROMO_MD5_BASE64],
)

PROMOCODE_PARENT_PROMO_ID_OFFER_2 = default_blue_genlog(
    offer_id='PROMOCODE_PARENT_PROMO_ID_OFFER_1',
    title='PROMOCODE_PARENT_PROMO_ID_OFFER_1',
    market_sku=723470,
    ware_md5='PROMOCODE_OFFER_000002',
    promo_type=PromoType.PROMO_CODE,
    binary_promos_md5_base64=[PROMOCODE_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64],
)

BLUE_CASHBACK_OFFER_1 = default_blue_genlog(
    offer_id='BLUE_CASHBACK_OFFER_1',
    title='BLUE_CASHBACK_OFFER_1',
    market_sku=123471,
    ware_md5='BLUE_CASHBACK__OFFER_1',
    promo_type=PromoType.BLUE_CASHBACK,
    binary_promos_md5_base64=[BLUE_CASHBACK_BINARY_PROMO_MD5_BASE64],
)

BLUE_CASHBACK_PARENT_PROMO_ID_OFFER_2 = default_blue_genlog(
    offer_id='BLUE_CASHBACK_PARENT_PROMO_ID_OFFER_1',
    title='BLUE_CASHBACK_PARENT_PROMO_ID_OFFER_1',
    market_sku=723471,
    ware_md5='BLUE_CASHBACK__OFFER_2',
    promo_type=PromoType.BLUE_CASHBACK,
    binary_promos_md5_base64=[BLUE_CASHBACK_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64],
)

MULTIPROMO_OFFER_1 = default_blue_genlog(
    offer_id='MULTIPROMO_OFFER_1',
    title='MULTIPROMO_OFFER_1',
    market_sku=123472,
    ware_md5='MULTIPROMO_____OFFER_1',
    promo_type=PromoType.PROMO_CODE | PromoType.DIRECT_DISCOUNT | PromoType.BLUE_CASHBACK,
    binary_promos_md5_base64=[PROMOCODE_BINARY_PROMO_MD5_BASE64, DIRECT_DISCOUNT_2_BINARY_PROMO_MD5_BASE64, BLUE_CASHBACK_BINARY_PROMO_MD5_BASE64],
)

DSBS_OFFER = default_genlog(
    offer_id='DSBS_OFFER',
    shop_id=4242,  # white shop with cpa=real
    feed_id=FEED_ID_DSBS,
    cpa=4,
    ware_md5='offerXdsbsXXXXXXXXXXXg',
    market_sku=1234567892,
)


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        SECRET_SALE_OFFER,
        FLASH_DISCOUNT_OFFER,
        SALE_OFFER_WITH_FLASH_DISCOUNT,
        PLAIN_OFFER,
        NEW_AND_OLD_SECRET_SALE_OFFER,
        GENERIC_BUNDLE_OFFER_1,
        GENERIC_BUNDLE_OFFER_2,
        CHEAPEST_AS_GIFT_OFFER_1,
        CHEAPEST_AS_GIFT_OFFER_2,
        BLUE_FLASH_OFFER_1,
        BLUE_SET_OFFER_1,
        BLUE_SET_OFFER_2,
        DIRECT_DISCOUNT_OFFER_1,
        DIRECT_DISCOUNT_OFFER_2,
        DIRECT_DISCOUNT_PARENT_PROMO_ID_OFFER_3,
        PROMOCODE_OFFER_1,
        PROMOCODE_PARENT_PROMO_ID_OFFER_2,
        BLUE_CASHBACK_OFFER_1,
        BLUE_CASHBACK_PARENT_PROMO_ID_OFFER_2,
        MULTIPROMO_OFFER_1,
        DSBS_OFFER,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope='module')
def promo_details():
    details = [
        generate_default_promo(
            promo_md5=SECRET_SALE_PROMO_KEY,
            type=PromoType.SECRET_SALE,
            source_promo_id=SECRET_SALE_PROMO_KEY,
            secret_sale_details={
                'offer_discounts': [
                    {
                        'msku': SECRET_SALE_OFFER['market_sku'],
                        'percent': SECRET_SALE_DISCOUNT_PERCENT
                    },
                    {
                        'msku': NEW_AND_OLD_SECRET_SALE_OFFER['market_sku'],
                        'percent': NEW_SECRET_SALE_DISCOUNT_PERCENT
                    },
                    {
                        'msku': SALE_OFFER_WITH_FLASH_DISCOUNT['market_sku'],
                        'percent': SECRET_SALE_DISCOUNT_PERCENT
                    }
                ]
            }
        ),
        generate_default_promo(
            promo_md5=NEW_SECRET_SALE_PROMO_KEY,
            type=PromoType.SECRET_SALE,
            source_promo_id=NEW_SECRET_SALE_PROMO_KEY,
            secret_sale_details={
                'offer_discounts': [
                    {
                        'msku': NEW_AND_OLD_SECRET_SALE_OFFER['market_sku'],
                        'percent': NEW_SECRET_SALE_DISCOUNT_PERCENT
                    }
                ]
            }
        ),
        generate_default_blue_3p_promo(promo_md5=FLASH_DISCOUNT_PROMO_KEY),
        {
            'msku': str(FLASH_DISCOUNT_OFFER['market_sku']),
            'msku_details': generate_default_msku(
                market_promo_price=150,
                market_old_price=1500,
                source_promo_id=FLASH_DISCOUNT_PROMO_KEY
            )
        },
        {
            'msku': str(SALE_OFFER_WITH_FLASH_DISCOUNT['market_sku']),
            'msku_details': generate_default_msku(
                market_promo_price=150,
                market_old_price=1500,
                source_promo_id=FLASH_DISCOUNT_PROMO_KEY
            )
        }
    ]

    json_path = yatest.common.output_path('yt_promo_details.json')

    return PromoDetails(write_promo_json_to_mmap, json_path, details)


@pytest.fixture(scope='module')
def promo_details_gb():
    details = [
        generate_default_promo(
            promo_md5=GENERIC_BUNDLE_BINARY_PROMO_MD5_BASE64,
            type=PromoType.GENERIC_BUNDLE,
            feed_id=FEED_ID,
            shop_promo_id=GENERIC_BUNDLE_SHOP_PROMO_ID,
            anaplan_promo_id=ANAPLAN_ID,
            bundles_content=[
                {
                    'primary_item': {
                        'offer_id': GENERIC_BUNDLE_OFFER_1['offer_id'],  # ну вот так, а не offer_id
                        'count': 1
                    },
                    'secondary_item': {
                        'item': {
                            'offer_id': GENERIC_BUNDLE_OFFER_2['offer_id'],
                            'count': 1
                        },
                        'discount_price': {
                            'value': 777 * 100,
                            'currency': 'RUR'
                        }
                    },
                },
            ],
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [{'feed_id': FEED_ID, 'offer_id': GENERIC_BUNDLE_OFFER_1['offer_id']}]},
                }
            ],
        ),
        generate_default_promo(
            promo_md5=CHEAPEST_AS_GIFT_BINARY_PROMO_MD5_BASE64,
            type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=FEED_ID,
            shop_promo_id=CHEAPEST_AS_GIFT_SHOP_PROMO_ID,
            cheapest_as_gift={
                'feed_offer_ids': [
                    {'feed_id': FEED_ID, 'offer_id': CHEAPEST_AS_GIFT_OFFER_1['offer_id']},
                    {'feed_id': FEED_ID, 'offer_id': CHEAPEST_AS_GIFT_OFFER_2['offer_id']},
                ],
                'count': 3,
                'promo_url': '',
                'link_text': '',
                'allow_berubonus': True,
                'allow_promocode': True,
            },
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [
                        {'feed_id': FEED_ID, 'offer_id': CHEAPEST_AS_GIFT_OFFER_1['offer_id']},
                        {'feed_id': FEED_ID, 'offer_id': CHEAPEST_AS_GIFT_OFFER_2['offer_id']}
                    ]},
                }
            ],
        ),
        generate_default_promo(
            promo_md5=BLUE_FLASH_BINARY_PROMO_MD5_BASE64,
            type=PromoType.BLUE_FLASH,
            feed_id=FEED_ID,
            shop_promo_id=BLUE_FLASH_SHOP_PROMO_ID,
            blue_flash={
                'items': [
                    {'offer_id': BLUE_FLASH_OFFER_1['offer_id'], 'price': {'value': 1000, 'currency': 'RUR'}},
                ],
                'allow_berubonus': True,
                'allow_promocode': True,
            },
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [{'feed_id': FEED_ID, 'offer_id': BLUE_FLASH_OFFER_1['offer_id']}]},
                }
            ],
        ),
        generate_default_promo(
            promo_md5=BLUE_SET_BINARY_PROMO_MD5_BASE64,
            type=PromoType.BLUE_SET,
            feed_id=FEED_ID,
            shop_promo_id=BLUE_SET_SHOP_PROMO_ID,
            blue_set={
                'sets_content': [
                    {
                        'items': [
                            {'offer_id': BLUE_SET_OFFER_1['offer_id'], 'discount': 5},
                            {'offer_id': BLUE_SET_OFFER_2['offer_id'], 'discount': 10},
                        ],
                        'linked': True,
                    },
                ],
                'restrict_refund': True,
                'allow_berubonus': True,
                'allow_promocode': True,
            },
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [{'feed_id': FEED_ID, 'offer_id': BLUE_SET_OFFER_1['offer_id']}, {'feed_id': FEED_ID, 'offer_id': BLUE_SET_OFFER_2['offer_id']}]},
                }
            ],
        ),
        generate_default_promo(
            promo_md5=DIRECT_DISCOUNT_BINARY_PROMO_MD5_BASE64,
            type=PromoType.DIRECT_DISCOUNT,
            feed_id=FEED_ID,
            shop_promo_id=DIRECT_DISCOUNT_SHOP_PROMO_ID,
            direct_discount={
                'items': [
                    {
                        'feed_id': FEED_ID,
                        'offer_id': DIRECT_DISCOUNT_OFFER_1['offer_id'],
                        'old_price': {
                            'value': 1 * (10 ** 7),
                            'currency': 'RUB',
                        },
                        'discount_price': {
                            'value': 2 * (10 ** 7),
                            'currency': 'RUB',
                        },
                    },
                ],
                'allow_berubonus': True,
                'allow_promocode': True,
            },
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [{'feed_id': FEED_ID, 'offer_id': DIRECT_DISCOUNT_OFFER_1['offer_id']}]},
                }
            ],
        ),
        generate_default_promo(
            promo_md5=DIRECT_DISCOUNT_2_BINARY_PROMO_MD5_BASE64,
            type=PromoType.DIRECT_DISCOUNT,
            shop_promo_id=DIRECT_DISCOUNT_2_SHOP_PROMO_ID,
            anaplan_promo_id=ANAPLAN_ID,
        ),
        generate_default_promo(
            promo_md5=DIRECT_DISCOUNT_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64,
            type=PromoType.DIRECT_DISCOUNT,
            shop_promo_id=DIRECT_DISCOUNT_PARENT_PROMO_ID_SHOP_PROMO_ID,
            parent_promo_id=PARENT_PROMO_ID,
        ),
        generate_default_promo(
            promo_md5=PROMOCODE_BINARY_PROMO_MD5_BASE64,
            type=PromoType.PROMO_CODE,
            promo_code='promocode_1',
            discount_value=100,
            discount_currency='RUR',
            mechanics_payment_type=MechanicsPaymentType.CPA,
            shop_promo_id=PROMOCODE_SHOP_PROMO_ID,
            anaplan_promo_id=ANAPLAN_ID,
        ),
        generate_default_promo(
            promo_md5=PROMOCODE_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64,
            type=PromoType.PROMO_CODE,
            promo_code='promocode_2',
            discount_value=100,
            discount_currency='RUR',
            mechanics_payment_type=MechanicsPaymentType.CPA,
            shop_promo_id=PROMOCODE_PARENT_PROMO_ID_SHOP_PROMO_ID,
            parent_promo_id=PARENT_PROMO_ID,
        ),
        generate_default_promo(
            promo_md5=BLUE_CASHBACK_PARENT_PROMO_ID_BINARY_PROMO_MD5_BASE64,
            type=PromoType.BLUE_CASHBACK,
            shop_promo_id=BLUE_CASHBACK_PARENT_PROMO_ID_SHOP_PROMO_ID,
            parent_promo_id=PARENT_PROMO_ID,
            share=0.15,
            version=1,
            priority=2,
        ),
        generate_default_promo(
            promo_md5=BLUE_CASHBACK_BINARY_PROMO_MD5_BASE64,
            type=PromoType.BLUE_CASHBACK,
            shop_promo_id=BLUE_CASHBACK_SHOP_PROMO_ID,
            anaplan_promo_id=ANAPLAN_ID,
            share=0.15,
            version=1,
            priority=2,
        ),
        generate_default_promo(
            promo_md5=CHEAPEST_AS_GIFT_DSBS_PROMO_KEY,
            type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=0,
            shop_promo_id='CHEAPEST_AS_GIFT_DSBS_PROMO',
            cheapest_as_gift={
                'feed_offer_ids': [
                    {'feed_id': DSBS_OFFER['feed_id'], 'offer_id': DSBS_OFFER['offer_id']},
                ],
                'count': 3,
                'promo_url': '',
                'link_text': '',
                'allow_berubonus': True,
                'allow_promocode': True,
            },
            offers_matching_rules=[
                {
                    'feed_offer_ids': {'ids': [{'feed_id': DSBS_OFFER['feed_id'], 'offer_id': DSBS_OFFER['offer_id']}]},
                }
            ],
        ),
    ]

    json_path = yatest.common.output_path('yt_promo_details_generic_bundle.json')

    return PromoDetails(write_promo_json_to_mmap, json_path, details)


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds([1, 2, 3, 11, 12, 666], blue_ids=[4, 5])


@pytest.fixture(scope="module")
def custom_shops_dat():
    shops = [
        default_shops_dat(
            name="Shop_cpa_real",
            fesh=4242,
            priority_region=213,
            regions=[225],
            home_region=225,
            datafeed_id=FEED_ID_DSBS,
            cpa='REAL'
        )
    ]
    return ShopsDat(shops)


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table, promo_details, promo_details_gb, model_ids, custom_shops_dat):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'shops_dat': custom_shops_dat,
        'model_ids': model_ids,
        'yt_promo_details_mmap': promo_details,
        'yt_promo_details_gb_mmap': promo_details_gb,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, offers_processor_workflow):
    resources = {
    }

    with MrMindexerBuildTestEnv(**resources) as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexarc'].load()
        yield env


@pytest.fixture(scope="module")
def doc_id_by_offer_id(mr_mindexer_direct_arc):
    mapping = {}
    arc = mr_mindexer_direct_arc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


def test_has_secret_sale_literal(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что литералы для закрытой распродажи проставляются корректно и не перезаписывают
    литералы от других акций
    """
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=SECRET_SALE_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                SECRET_SALE_OFFER,
                SALE_OFFER_WITH_FLASH_DISCOUNT,
                NEW_AND_OLD_SECRET_SALE_OFFER
            ]]
        )
    )


def test_has_new_secret_sale_literal(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что литералы для новой закрытой распродажи проставляются корректно вместе с литералами
    для старой
    """
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=NEW_SECRET_SALE_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                NEW_AND_OLD_SECRET_SALE_OFFER
            ]]
        )
    )


def test_has_flash_discount_literal(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что литералы для blue-3p-flash скидкой проставляются корректно
    """
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=FLASH_DISCOUNT_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                FLASH_DISCOUNT_OFFER,
                SALE_OFFER_WITH_FLASH_DISCOUNT
            ]]
        )
    )


def test_no_search_literal(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что офферы, не попадающие под действие акций, остаются неразмеченными
    """
    assert_that(
        mr_mindexer_direct,
        HasNoLiterals(
            literal_name=LITERAL_STRING.format(promo=SECRET_SALE_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                FLASH_DISCOUNT_OFFER,
                PLAIN_OFFER,
                GENERIC_BUNDLE_OFFER_2,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasNoLiterals(
            literal_name=LITERAL_STRING.format(promo=FLASH_DISCOUNT_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                SECRET_SALE_OFFER,
                NEW_AND_OLD_SECRET_SALE_OFFER,
                PLAIN_OFFER,
                GENERIC_BUNDLE_OFFER_2,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasNoLiterals(
            literal_name=LITERAL_STRING.format(promo=NEW_SECRET_SALE_PROMO_KEY),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                FLASH_DISCOUNT_OFFER,
                SECRET_SALE_OFFER,
                SALE_OFFER_WITH_FLASH_DISCOUNT,
                PLAIN_OFFER,
                GENERIC_BUNDLE_OFFER_2,
            ]]
        )
    )


def test_blue_promo_literals(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что литералы для всех типов синих акций проставляются корректно
    """
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=GENERIC_BUNDLE_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                GENERIC_BUNDLE_OFFER_1,  # только основной оффер, не подарок
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasNoLiterals(
            literal_name=LITERAL_STRING.format(promo=GENERIC_BUNDLE_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                GENERIC_BUNDLE_OFFER_2,  # на подарке литерала быть не должно
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=CHEAPEST_AS_GIFT_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                CHEAPEST_AS_GIFT_OFFER_1,
                CHEAPEST_AS_GIFT_OFFER_2,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=BLUE_FLASH_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                BLUE_FLASH_OFFER_1,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=BLUE_SET_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                BLUE_SET_OFFER_1,  # и первичный и вторичный офферы, т.к. комплект связан
                BLUE_SET_OFFER_2,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=DIRECT_DISCOUNT_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                DIRECT_DISCOUNT_OFFER_1,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=DIRECT_DISCOUNT_2_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                DIRECT_DISCOUNT_OFFER_2,
                MULTIPROMO_OFFER_1,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=PROMOCODE_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                PROMOCODE_OFFER_1,
                MULTIPROMO_OFFER_1,
            ]]
        )
    )
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=BLUE_CASHBACK_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                BLUE_CASHBACK_OFFER_1,
                MULTIPROMO_OFFER_1,
            ]]
        )
    )

    # проверяем поисковый литерал для ID анаплана
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=ANAPLAN_LITERAL_STRING.format(id=ANAPLAN_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                GENERIC_BUNDLE_OFFER_1,
                PROMOCODE_OFFER_1,
                DIRECT_DISCOUNT_OFFER_2,
                BLUE_CASHBACK_OFFER_1,
                MULTIPROMO_OFFER_1,
            ]]
        )
    )

    # проверяем поисковый литерал для ID родительской акции
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=PARENT_PROMO_LITERAL_STRING.format(parent_id=PARENT_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                PROMOCODE_PARENT_PROMO_ID_OFFER_2,
                DIRECT_DISCOUNT_PARENT_PROMO_ID_OFFER_3,
                BLUE_CASHBACK_PARENT_PROMO_ID_OFFER_2,
            ]]
        )
    )

    # проверяем поисковый литерал по типу акции
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=PROMO_TYPE_LITERAL_STRING.format(promo_type=PromoType.PROMO_CODE),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                PROMOCODE_PARENT_PROMO_ID_OFFER_2,
                MULTIPROMO_OFFER_1,
            ]]
        )
    )

    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=PROMO_TYPE_LITERAL_STRING.format(promo_type=PromoType.BLUE_CASHBACK),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                MULTIPROMO_OFFER_1
            ]]
        )
    )

    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=PROMO_TYPE_LITERAL_STRING.format(promo_type=PromoType.DIRECT_DISCOUNT),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                MULTIPROMO_OFFER_1
            ]]
        )
    )


def test_dsbs_promo_literals(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что литералы для DSBS офферов проставляются корректно
    """
    assert_that(
        mr_mindexer_direct,
        HasLiterals(
            literal_name=LITERAL_STRING.format(promo=GENERIC_BUNDLE_SHOP_PROMO_ID),
            doc_ids=[doc_id_by_offer_id[offer['offer_id']] for offer in [
                GENERIC_BUNDLE_OFFER_1,  # только основной оффер, не подарок
            ]]
        )
    )
