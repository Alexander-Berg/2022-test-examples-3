# coding: utf-8

import pytest

from hamcrest import assert_that, has_key, equal_to

from market.idx.generation.yatf.test_envs.msku_reducer import MskuReducerTestEnv, MskuReducerMode
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    make_params,
    make_param_entry
)

import yt.wrapper as yt


FEED_ID=1

MSKU1=1
MSKU1_WAREMD5='hc1cVZiClnllcxjhGX0_m1'

MSKU1_OFFER_ID1 = '1'
MSKU1_OFFER_ID2 = '2'
MSKU1_OFFER_ID3 = '3'
MSKU1_OFFER_ID4 = '4'

OFFER1_SHOP_ID = 10
OFFER1_WAREHOUSE_ID = 20
OFFER1_SUPPLIER_ID = 30
OFFER1_BUSINESS_ID = 40

OFFER2_SHOP_ID = 11
OFFER2_WAREHOUSE_ID = 21
OFFER2_SUPPLIER_ID = 31
OFFER2_BUSINESS_ID = 41

MSKU2=2
MSKU2_WAREMD5='hc1cVZiClnllcxjhGX0_m2'

MSKU3=3
MSKU3_WAREMD5='hc1cVZiClnllcxjhGX0_m3'
MSKU3_OFFER_ID1 = '1'
MSKU3_OFFER_ID2 = '2'
MSKU3_OFFER_ID3 = '3'
MSKU3_SHOP_ID = 33
MSKU3_WAREHOUSE_ID = 44
MSKU3_SUPPLIER_ID = 55
MSKU3_BUSINESS_ID = 66

MSKU4=4
MSKU4_WAREMD5='hc1cVZiClnllcxjhGX0_m4'
MSKU4_OFFER_ID1 = '1'
MSKU4_OFFER_ID2 = '2'
MSKU4_OFFER_ID3 = '3'
MSKU4_SHOP_ID = 333
MSKU4_WAREHOUSE_ID = 444
MSKU4_SUPPLIER_ID = 555
MSKU4_BUSINESS_ID = 666

MSKU5=5
MSKU5_WAREMD5='hc1cVZiClnllcxjhGX0_m5'
MSKU5_OFFER_ID1 = '1'
MSKU5_OFFER_ID2 = '2'
MSKU5_OFFER_ID3 = '3'
MSKU5_SHOP_ID = 334
MSKU5_WAREHOUSE_ID = 445
MSKU5_SUPPLIER_ID = 556
MSKU5_BUSINESS_ID = 667
MSKU5_PROMO_TYPE1 = 1 << 1
MSKU5_PROMO_TYPE2 = 1 << 2
MSKU5_PROMO_TYPE3 = 1 << 3

MSKU6=6
MSKU6_WAREMD5='hc1cVZiClnllcxjhGX0_m6'
MSKU7=7
MSKU7_WAREMD5='hc1cVZiClnllcxjhGX0_m7'


# amore_beru_vendor_data (ABVD) - вендорские автостратегии (из них берём вендорские ставки)
# amore_data (AD) - автостратегии мерчей (из них берём shop_fee)
# Информация об автостратегии хранится в структуре
# https://a.yandex-team.ru/arcadia/market/amore/data/strategies.h?rev=r8631490#L87
# Здесь формируется строка, из которой структура заполняется

# Первые 4 байта - Id (он здесь не важен)
# Следующие 4 байта - сама автостратегия
# Последние 4 байта - экспериментальная автостратегия (здесь не важна)
# 2050 = POSITIONAL-автостратегия со ставкой (для вендора)
ABVD_POSITIONAL_NONZERO = b'\x00\x00\x00\x00\x02\x00\x05\x00\x00\x00\x00\x00'
# 5050 = CPA-автостратегия со ставкой (сейчас в репорте не используется для вендора)
ABVD_CPA_NONZERO = b'\x00\x00\x00\x00\x05\x00\x50\x00\x00\x00\x00\x00'
# 2000 = POSITIONAL-автостратегия со ставкой (для вендора)
ABVD_POSITIONAL_ZERO = b'\x00\x00\x00\x00\x02\x00\x00\x00\x00\x00\x00\x00'
# 5100 = CPA-автостратегия со ставкой (для мерча)
AD_CPA_NONZERO = b'\x00\x00\x00\x00\x05\x01\x00\x00\x00\x00\x00\x00'
# 5000 = CPA-автостратегия с нулевой ставкой (для мерча)
AD_CPA_ZERO = b'\x00\x00\x00\x00\x05\x00\x00\x00\x00\x00\x00\x00'

# MSKU 8-10 - для проверки литерала has_adv_bid
MSKU8 = 8  # MSKU с офферами без ставок
MSKU8_WAREMD5 = 'hc1cVZiClnllcxjhGX0_m8'
MSKU8_OFFER_ID1 = '1'
MSKU8_OFFER_ID2 = '2'

MSKU9 = 9  # MSKU с оффером со ставкой мерча
MSKU9_WAREMD5 = 'hc1cVZiClnllcxjhGX0_m9'
MSKU9_OFFER_ID1 = '1'
MSKU9_OFFER_ID2 = '2'

MSKU10 = 10  # MSKU с оффером с вендорской ставкой
MSKU10_WAREMD5 = 'hc1cVZiClnllcxhGX0_m10'
MSKU10_OFFER_ID1 = '1'
MSKU10_OFFER_ID2 = '2'

MSKU11 = 11  # MSKU с офферами без ставок, но с 1p оффером
MSKU11_WAREMD5 = 'hc1cVZiClnllcxhGX0_m11'
MSKU11_OFFER_ID1 = '1'


INSTALLMENT_OPTIONS = [
    {
        'bnpl_available': True,
        'installment_time_in_days': [yt.yson.YsonUint64(45)],
        'group_name': 'group for category'
    },
    {
        'bnpl_available': False,
        'installment_time_in_days': [yt.yson.YsonUint64(60), yt.yson.YsonUint64(90)],
        'group_name': 'group for vendor'
    }
]

SUPER_HYPE_MSKU_PARAMS = make_params(
    model=1,
    category=1,
    values=[
        make_param_entry(key=29565810, id=1),
    ]
)

NOT_SUPER_HYPE_MSKU_PARAMS = make_params(
    model=1,
    category=1,
    values=[
        make_param_entry(key=29565810, id=0),
    ]
)

URL = 'www.netshopping.ru/vcd-27953-1-59775/GoodsInfo.html'
WARE_MD5 = '7JKXvUokpz7OCIUxJjeu-g'


# sequence_number здесь называется id
@pytest.fixture(scope="module")
def offers(request):
    return [
        # MSKU1 и его офферы (белые и синий)
        GenlogRow(shard_id=0, id=0, feed_id=FEED_ID, offer_id=MSKU1_OFFER_ID1, is_fake_msku_offer=False, market_sku=MSKU1, shop_id=OFFER1_SHOP_ID,
                  warehouse_id=OFFER1_WAREHOUSE_ID, supplier_id=OFFER1_SUPPLIER_ID, business_id=OFFER1_BUSINESS_ID, is_express=True, downloadable=True,
                  installment_options=INSTALLMENT_OPTIONS, url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=1, feed_id=FEED_ID, offer_id=MSKU1_OFFER_ID2, is_fake_msku_offer=False, market_sku=MSKU1, shop_id=OFFER2_SHOP_ID,
                  warehouse_id=OFFER2_WAREHOUSE_ID, supplier_id=OFFER2_SUPPLIER_ID, business_id=OFFER2_BUSINESS_ID, is_express=False, downloadable=False,
                  prefer_earth_over_orig_regions=True, url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=2, feed_id=FEED_ID, offer_id=MSKU1_OFFER_ID3, is_fake_msku_offer=False, market_sku=MSKU1, shop_id=OFFER2_SHOP_ID,
                  warehouse_id=OFFER2_WAREHOUSE_ID, supplier_id=OFFER2_SUPPLIER_ID, business_id=OFFER2_BUSINESS_ID, is_express=False, downloadable=False,
                  orig_regions_literals=[yt.yson.YsonUint64(213), yt.yson.YsonUint64(2)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=3, feed_id=FEED_ID, offer_id='4', ware_md5=MSKU1_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU1),

        # мску без офферов
        GenlogRow(shard_id=0, id=4, feed_id=FEED_ID, offer_id='5', is_fake_msku_offer=True, market_sku=MSKU2, ware_md5=MSKU2_WAREMD5),

        # MSKU3 и его офферы (белые, с разными регионами - города должны поредьюситься в регион "Россия")
        GenlogRow(shard_id=0, id=5, feed_id=FEED_ID, offer_id=MSKU3_OFFER_ID1, is_fake_msku_offer=False, market_sku=MSKU3, downloadable=False,
                  shop_id=MSKU3_SHOP_ID, warehouse_id=MSKU3_WAREHOUSE_ID, supplier_id=MSKU3_SUPPLIER_ID, business_id=MSKU3_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(225)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=6, feed_id=FEED_ID, offer_id=MSKU3_OFFER_ID2, is_fake_msku_offer=False, market_sku=MSKU3, downloadable=False,
                  shop_id=MSKU3_SHOP_ID, warehouse_id=MSKU3_WAREHOUSE_ID, supplier_id=MSKU3_SUPPLIER_ID, business_id=MSKU3_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(213)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=7, feed_id=FEED_ID, offer_id=MSKU3_OFFER_ID3, is_fake_msku_offer=False, market_sku=MSKU3, downloadable=False,
                  shop_id=MSKU3_SHOP_ID, warehouse_id=MSKU3_WAREHOUSE_ID, supplier_id=MSKU3_SUPPLIER_ID, business_id=MSKU3_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(2)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=8, feed_id=FEED_ID, offer_id='8', ware_md5=MSKU3_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU3),

        # MSKU4 и его офферы (белые, с разными регионами - России нет, остаются города)
        GenlogRow(shard_id=0, id=9, feed_id=FEED_ID, offer_id=MSKU4_OFFER_ID1, is_fake_msku_offer=False, market_sku=MSKU4, downloadable=False,
                  shop_id=MSKU4_SHOP_ID, warehouse_id=MSKU4_WAREHOUSE_ID, supplier_id=MSKU4_SUPPLIER_ID, business_id=MSKU4_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(213)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=10, feed_id=FEED_ID, offer_id=MSKU4_OFFER_ID2, is_fake_msku_offer=False, market_sku=MSKU4, downloadable=False,
                  shop_id=MSKU4_SHOP_ID, warehouse_id=MSKU4_WAREHOUSE_ID, supplier_id=MSKU4_SUPPLIER_ID, business_id=MSKU4_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(2)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=11, feed_id=FEED_ID, offer_id='11', ware_md5=MSKU4_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU4),

        # офферa без мску
        GenlogRow(shard_id=0, id=12, feed_id=FEED_ID, offer_id='12', is_fake_msku_offer=False, market_sku=54321, bid=0),
        GenlogRow(shard_id=0, id=13, feed_id=FEED_ID, offer_id='13', is_fake_msku_offer=False, market_sku=None, bid=0),

        # MSKU5 и его офферы (белые и синий)
        GenlogRow(shard_id=0, id=14, feed_id=FEED_ID, offer_id=MSKU5_OFFER_ID1, is_fake_msku_offer=False, market_sku=MSKU5, shop_id=MSKU5_SHOP_ID,
                  warehouse_id=MSKU5_WAREHOUSE_ID, supplier_id=MSKU5_SUPPLIER_ID, business_id=MSKU5_BUSINESS_ID, downloadable=False,
                  promo_type_literals=[yt.yson.YsonUint64(MSKU5_PROMO_TYPE1)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=15, feed_id=FEED_ID, offer_id=MSKU5_OFFER_ID2, is_fake_msku_offer=False, market_sku=MSKU5, shop_id=MSKU5_SHOP_ID,
                  warehouse_id=MSKU5_WAREHOUSE_ID, supplier_id=MSKU5_SUPPLIER_ID, business_id=MSKU5_BUSINESS_ID, downloadable=False,
                  promo_type_literals=[yt.yson.YsonUint64(MSKU5_PROMO_TYPE2), yt.yson.YsonUint64(MSKU5_PROMO_TYPE3)], url=URL, ware_md5=WARE_MD5,
                  bid=0),
        GenlogRow(shard_id=0, id=16, feed_id=FEED_ID, offer_id=MSKU5_OFFER_ID3, is_fake_msku_offer=False, market_sku=MSKU5, shop_id=MSKU5_SHOP_ID,
                  warehouse_id=MSKU5_WAREHOUSE_ID, supplier_id=MSKU5_SUPPLIER_ID, business_id=MSKU5_BUSINESS_ID, downloadable=False,
                  promo_type_literals=[yt.yson.YsonUint64(MSKU5_PROMO_TYPE3)], url=URL, ware_md5=WARE_MD5, bid=0),
        GenlogRow(shard_id=0, id=17, feed_id=FEED_ID, offer_id='17', ware_md5=MSKU5_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU5),

        # мску без офферов - новинка
        GenlogRow(shard_id=0, id=18, feed_id=FEED_ID, offer_id='18', is_fake_msku_offer=True, market_sku=MSKU6, ware_md5=MSKU6_WAREMD5,
                  params_entry=SUPER_HYPE_MSKU_PARAMS.SerializeToString()),
        # мску без офферов - не новинка
        GenlogRow(shard_id=0, id=19, feed_id=FEED_ID, offer_id='19', is_fake_msku_offer=True, market_sku=MSKU7, ware_md5=MSKU7_WAREMD5,
                  params_entry=NOT_SUPER_HYPE_MSKU_PARAMS.SerializeToString()),

        # MSKU 8 - с офферами, ни у одного оффера нет ставки
        GenlogRow(shard_id=0, id=20, feed_id=FEED_ID, offer_id=MSKU8_OFFER_ID1, market_sku=MSKU8, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_POSITIONAL_ZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=21, feed_id=FEED_ID, offer_id=MSKU8_OFFER_ID2, market_sku=MSKU8, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_CPA_NONZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=22, feed_id=FEED_ID, offer_id='22', ware_md5=MSKU8_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU8),

        # MSKU 9 - у одного из офферов есть ненулевая ставка мерча
        GenlogRow(shard_id=0, id=23, feed_id=FEED_ID, offer_id=MSKU9_OFFER_ID1, market_sku=MSKU9, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_NONZERO, amore_beru_vendor_data=ABVD_POSITIONAL_ZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=24, feed_id=FEED_ID, offer_id=MSKU9_OFFER_ID2, market_sku=MSKU9, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_CPA_NONZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=25, feed_id=FEED_ID, offer_id='25', ware_md5=MSKU9_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU9),

        # MSKU 10 - у одного из офферов есть ненулевая вендорская ставка
        GenlogRow(shard_id=0, id=26, feed_id=FEED_ID, offer_id=MSKU10_OFFER_ID1, market_sku=MSKU10, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_POSITIONAL_ZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=27, feed_id=FEED_ID, offer_id=MSKU10_OFFER_ID2, market_sku=MSKU10, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_POSITIONAL_NONZERO, bid=0, fee=0),
        GenlogRow(shard_id=0, id=28, feed_id=FEED_ID, offer_id='28', ware_md5=MSKU10_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU10),

        # MSKU 11 - ни у кого нет ставок, но на msku есть 1p-оффер - он получит рекомендованную ставку в репорте
        GenlogRow(shard_id=0, id=29, feed_id=FEED_ID, offer_id=MSKU11_OFFER_ID1, market_sku=MSKU11, ware_md5=WARE_MD5,
                  amore_data=AD_CPA_ZERO, amore_beru_vendor_data=ABVD_POSITIONAL_ZERO, bid=0, fee=0, supplier_type=1),
        GenlogRow(shard_id=0, id=30, feed_id=FEED_ID, offer_id='28', ware_md5=MSKU11_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU11),
    ]


# sequence_number здесь называется id
@pytest.fixture(scope="module")
def offers_for_sane_size_test(request):
    return [
        # MSKU4 и его офферы (белые, с разными регионами - 2 города схлопываются в "Россию" из-за настройки sane_regions_size=1)
        GenlogRow(shard_id=0, id=1, feed_id=FEED_ID, offer_id=MSKU1_OFFER_ID1, is_fake_msku_offer=False, market_sku=MSKU1, downloadable=False,
                  shop_id=OFFER1_SHOP_ID, warehouse_id=OFFER1_WAREHOUSE_ID, supplier_id=OFFER1_SUPPLIER_ID, business_id=OFFER1_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(213)], bid=0),
        GenlogRow(shard_id=0, id=2, feed_id=FEED_ID, offer_id=MSKU1_OFFER_ID2, is_fake_msku_offer=False, market_sku=MSKU1, downloadable=False,
                  shop_id=OFFER1_SHOP_ID, warehouse_id=OFFER1_WAREHOUSE_ID, supplier_id=OFFER1_SUPPLIER_ID, business_id=OFFER1_BUSINESS_ID,
                  orig_regions_literals=[yt.yson.YsonUint64(2)], bid=0),
        GenlogRow(shard_id=0, id=3, feed_id=FEED_ID, offer_id='3', ware_md5=MSKU1_WAREMD5, is_fake_msku_offer=True, market_sku=MSKU1),
    ]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers):
    with MskuReducerTestEnv(yt_server, offers) as env:
        env.execute(mode=MskuReducerMode.COLLECT_OFFERS_DATA_FOR_MSKU)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_sane_size_1_region(yt_server, offers_for_sane_size_test):
    with MskuReducerTestEnv(yt_server, offers_for_sane_size_test) as env:
        env.execute(mode=MskuReducerMode.COLLECT_OFFERS_DATA_FOR_MSKU, sane_regions_size=1)
        env.verify()
        yield env


"""
Более подробные юнит-тесты на региональную логику:
https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/offers/tests/ut/test_RegionsReduce.cpp#L37
https://a.yandex-team.ru/arc/trunk/arcadia/market/library/geo/GeoTest.cpp#L61
"""
MSKU1_EXPECTED_DATA = {
    'ware_md5': MSKU1_WAREMD5,
    'offer_search_literals': [
        # offer1_1
        {'name': 'yx_ds_id', 'value': str(OFFER1_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(OFFER1_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(OFFER1_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(OFFER1_BUSINESS_ID)}, {'name': 'is_express', 'value': '1'}, {'name': 'downloadable', 'value': '1'},
        {'name': 'has_installment', 'value': '1'}, {'name': 'bnpl_available', 'value': '1'},
        # offer1_2
        {'name': 'yx_ds_id', 'value': str(OFFER2_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(OFFER2_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(OFFER2_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(OFFER2_BUSINESS_ID)}, {'name': 'is_express', 'value': '0'},
        {'name': 'offer_region', 'value': '10000'},   # offer_region=Earth для синего оффера MSKU1_OFFER_ID2
        # offer1_3 - offer_regions редьюсятся, т.к. есть регион "Земля". offer_region_exp остаются
        {'name': 'offer_region_exp', 'value': '2'}, {'name': 'offer_region_exp', 'value': '213'},
        # всем офферам мску проставляется
        {'name': 'is_b2c', 'value': '1'},
        # признак наличие офферов у мску
        {'name': 'is_not_empty_metadoc', 'value': '1'}
    ],
}

MSKU2_EXPECTED_DATA = {
    'ware_md5': MSKU2_WAREMD5,
    'offer_search_literals': [
        # т.к. set-earth-for-msku-without-offer-regions
        {'name': 'offer_region', 'value': '10000'}, {'name': 'offer_region_exp', 'value': '10000'},
    ],
}

MSKU3_EXPECTED_DATA = {
    'ware_md5': MSKU3_WAREMD5,
    'offer_search_literals': [
        {'name': 'yx_ds_id', 'value': str(MSKU3_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(MSKU3_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(MSKU3_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(MSKU3_BUSINESS_ID)}, {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        # offer_region, offer_region_exp - остается только 225, дочерние 213 и 2 поредьюсились
        {'name': 'offer_region', 'value': '225'},
        {'name': 'offer_region_exp', 'value': '225'},
    ],
}

MSKU4_EXPECTED_DATA = {
    'ware_md5': MSKU4_WAREMD5,
    'offer_search_literals': [
        {'name': 'yx_ds_id', 'value': str(MSKU4_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(MSKU4_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(MSKU4_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(MSKU4_BUSINESS_ID)}, {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        # offer_region, offer_region_exp - остается только 213 и 2, как и были заданы
        {'name': 'offer_region', 'value': '213'}, {'name': 'offer_region', 'value': '2'},
        {'name': 'offer_region_exp', 'value': '213'}, {'name': 'offer_region_exp', 'value': '2'},
    ],
}

MSKU5_EXPECTED_DATA = {
    'ware_md5': MSKU5_WAREMD5,
    'offer_search_literals': [
        {'name': 'yx_ds_id', 'value': str(MSKU5_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(MSKU5_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(MSKU5_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(MSKU5_BUSINESS_ID)}, {'name': 'is_b2c', 'value': '1'},
        {'name': 'promo_type', 'value': str(MSKU5_PROMO_TYPE1)},
        {'name': 'promo_type', 'value': str(MSKU5_PROMO_TYPE2)},
        {'name': 'promo_type', 'value': str(MSKU5_PROMO_TYPE3)},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        # т.к. set-earth-for-msku-without-offer-regions
        {'name': 'offer_region', 'value': '10000'}, {'name': 'offer_region_exp', 'value': '10000'},
    ],
}

MSKU1_EXPECTED_DATA_SANE_SIZE = {
    'ware_md5': MSKU1_WAREMD5,
    'offer_search_literals': [
        # offer1_1
        {'name': 'yx_ds_id', 'value': str(OFFER1_SHOP_ID)},
        {'name': 'warehouse_id', 'value': str(OFFER1_WAREHOUSE_ID)}, {'name': 'supplier_id', 'value': str(OFFER1_SUPPLIER_ID)},
        {'name': 'bsid', 'value': str(OFFER1_BUSINESS_ID)}, {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        {'name': 'offer_region', 'value': '10000'},  # offer_region - поредьюсились до "Земли" из-за настройки sane_regions_size = 1
        {'name': 'offer_region_exp', 'value': '225'},  # offer_region - поредьюсились до "России" из-за настройки sane_regions_size = 1
    ],
}

MSKU6_EXPECTED_DATA = {
    'ware_md5': MSKU6_WAREMD5,
    'offer_search_literals': [
        # непустой метадок - т.к. новинка
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        # т.к. set-earth-for-msku-without-offer-regions
        {'name': 'offer_region', 'value': '10000'}, {'name': 'offer_region_exp', 'value': '10000'},
        # на msku нет оффера со ставкой => литерал has_adv_bid не проставлен (офферов нет)
    ],
}

MSKU7_EXPECTED_DATA = {
    'ware_md5': MSKU7_WAREMD5,
    'offer_search_literals': [
        # т.к. set-earth-for-msku-without-offer-regions
        {'name': 'offer_region', 'value': '10000'}, {'name': 'offer_region_exp', 'value': '10000'},
    ],
}


MSKU8_EXPECTED_DATA = {
    'ware_md5': MSKU8_WAREMD5,
    'offer_search_literals': [
        {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        {'name': 'offer_region', 'value': '10000'},
        {'name': 'offer_region_exp', 'value': '10000'},
        {'name': 'yx_ds_id', 'value': '1380'},
        # на msku нет оффера со ставкой => литерал has_adv_bid не проставлен
    ],
}


MSKU9_EXPECTED_DATA = {
    'ware_md5': MSKU9_WAREMD5,
    'offer_search_literals': [
        {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        {'name': 'offer_region', 'value': '10000'},
        {'name': 'offer_region_exp', 'value': '10000'},
        {'name': 'yx_ds_id', 'value': '1380'},
        # на msku есть оффер со ставкой мерча => проставляется литерал has_adv_bid
        {'name': 'has_adv_bid', 'value': '1'},
    ],
}


MSKU10_EXPECTED_DATA = {
    'ware_md5': MSKU10_WAREMD5,
    'offer_search_literals': [
        {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        {'name': 'offer_region', 'value': '10000'},
        {'name': 'offer_region_exp', 'value': '10000'},
        {'name': 'yx_ds_id', 'value': '1380'},
        # на msku есть оффер со ставкой вендора => проставляется литерал has_adv_bid
        {'name': 'has_adv_bid', 'value': '1'},
    ],
}


MSKU11_EXPECTED_DATA = {
    'ware_md5': MSKU10_WAREMD5,
    'offer_search_literals': [
        {'name': 'is_b2c', 'value': '1'},
        {'name': 'is_not_empty_metadoc', 'value': '1'},
        {'name': 'offer_region', 'value': '10000'},
        {'name': 'offer_region_exp', 'value': '10000'},
        {'name': 'yx_ds_id', 'value': '1380'},
        # на msku есть 1p оффер (он получит ставку в репорте) => проставляется литерал has_adv_bid
        {'name': 'has_adv_bid', 'value': '1'},
    ],
}


def test_msku_count(workflow):
    """Проверяем число записей в выходной таблице с мску с офферыми литералами.
    В нее попадают все мску (с офферами или без) и не попадают офферы
    """
    assert_that(len(workflow.result_genlog_data), equal_to(11))


@pytest.mark.parametrize(
    'msku_expected_result',
    [MSKU1_EXPECTED_DATA, MSKU2_EXPECTED_DATA, MSKU3_EXPECTED_DATA, MSKU4_EXPECTED_DATA, MSKU5_EXPECTED_DATA, MSKU6_EXPECTED_DATA, MSKU7_EXPECTED_DATA,
     MSKU8_EXPECTED_DATA, MSKU9_EXPECTED_DATA, MSKU10_EXPECTED_DATA, MSKU11_EXPECTED_DATA]
)
def test_msku_search_literals(workflow, msku_expected_result):
    """Проверяем проставление офферных литералов в поле генлога msku_offers_info.literals мску
    """
    assert_that(workflow.result_genlog_data, has_key(msku_expected_result['ware_md5']), "No expected msku")

    actual_row = workflow.result_genlog_data[msku_expected_result['ware_md5']]
    actual_search_literals = actual_row["msku_offers_info"]["literals"]
    assert_that(sorted(actual_search_literals), equal_to(sorted(msku_expected_result["offer_search_literals"])))


@pytest.mark.parametrize('msku_expected_result', [MSKU1_EXPECTED_DATA_SANE_SIZE])
def test_msku_search_literals_sane_size(workflow_sane_size_1_region, msku_expected_result):
    """Проверяем проставление офферных литералов в поле генлога msku_offers_info.literals мску
    """
    assert_that(workflow_sane_size_1_region.result_genlog_data, has_key(msku_expected_result['ware_md5']), "No expected msku")

    actual_row = workflow_sane_size_1_region.result_genlog_data[msku_expected_result['ware_md5']]
    actual_search_literals = actual_row["msku_offers_info"]["literals"]

    assert_that(sorted(actual_search_literals), equal_to(sorted(msku_expected_result["offer_search_literals"])))
