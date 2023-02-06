# -*- coding: utf-8 -*-

import pytest
import yt.wrapper as yt

# from protobuf_to_dict import protobuf_to_dict
from market.idx.promos.promo_matcher.yatf.matcher_env import PromoMatcherTestEnv
from market.idx.yatf.resources.tovar_tree_pb import MboCategory
from market.proto.common.promo_pb2 import ESourceType
from market.proto.common.common_pb2 import ESupplierFlag
from market.proto.content.mbo import TovarTree_pb2
from market.proto.feedparser.Promo_pb2 import PromoDetails
from market.pylibrary.const.offer_promo import PromoType
from market.pylibrary.promo.utils import PromoDetailsHelper, exclude


@pytest.yield_fixture(scope="module")
def express_warehouses():
    return (
        (1,),
        (70,),
    )


@pytest.yield_fixture(scope="module")
def shopinfo():
    return (
        # feed_id, warehouse_id, is_dsbs
        (100, 200, False),
        (101, 201, True),
        (1, 70, False),
    )


@pytest.yield_fixture(scope="module")
def tovar_tree():
    def __make_mbo(data):
        hid, name, parent = data
        return (
            MboCategory(hid, 0, name, name, parent_hid=parent[0] if isinstance(parent, tuple) else None)
            .as_pb()
            .SerializeToString(),
        )

    #  категория, название, родительская категория
    HID_01 = (90401, "Всё и сразу", None)
    HID_02 = (90402, "Мандарины", HID_01)
    HID_03 = (90403, "Выключатели", HID_01)
    HID_04 = (90404, "Сладкие мандарины", HID_02)
    HID_05 = (90405, "Кислые мандарины", HID_02)
    return (
        __make_mbo(HID_01),
        __make_mbo(HID_02),
        __make_mbo(HID_03),
        __make_mbo(HID_04),
        __make_mbo(HID_05),
    )


@pytest.fixture(scope="module")
def promo_details_info():
    def __make_pd(promo, part_id=0):
        return (promo.promo_key, promo.protobuf.SerializeToString(), part_id)

    promo1 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.BLUE_FLASH, 'promo_id_1').add_omr(
        categories=[90405],
        excluded_categories=[90404],
        suppliers=(123, 345),
        mskus=exclude(1),
        vendors=exclude(2),
        warehouses=(70,),
        feed_offer_ids=exclude((1, '123')),
        supplier_flags=ESupplierFlag.EXPRESS_WAREHOUSE,
        excluded_supplier_flags=ESupplierFlag.DSBS_SUPPLIER,
    )
    promo1.blue_flash = {
        'items': [
            dict(price=dict(value=100, currency='RUB'), offer=dict(feed_id=1, offer_id='offer1')),
        ]
    }

    promo2 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.DIRECT_DISCOUNT, 'promo_id_dd').add_omr(
        feed_offer_ids=((2, 'offer_part1'),),
    )
    promo2.direct_discount = {
        'items': [
            dict(feed_id=2, offer_id='offer_part1', discount_price=dict(value=10000, currency='RUB')),
        ]
    }

    promo3 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.DIRECT_DISCOUNT, 'promo_id_dd').add_omr(
        feed_offer_ids=((3, 'offer_part2'),),
    )
    promo3.direct_discount = {
        'items': [
            dict(feed_id=3, offer_id='offer_part2', discount_price=dict(value=10000, currency='RUB')),
        ]
    }

    return (
        __make_pd(promo1),
        __make_pd(promo2, 0),
        __make_pd(promo3, 1),
    )


@pytest.yield_fixture(scope="module")
def offers():
    # feed_id, shop_sku, category_id, market_sku, shop_id, vendor_id, warehouse_id, anaplan_promos, is_eda_offer, is_lavka_offer, supplier_type
    return (
        (1, 'offer1', 90405, 1122, 123, 0, 70, None, False, False, 1),
        (1, 'offer2', 90405, 1122, 123, 0, 70, None, True, False, 0),
        (1, 'offer3', 90405, 1122, 123, 0, 70, None, False, True, 0),
        (2, 'offer_part1', 90405, 222, 123, 0, 70, None, False, False, 1),
        (3, 'offer_part2', 90405, 333, 123, 0, 70, None, False, False, 1),
    )


@pytest.yield_fixture(scope="module")
def workflow_mstat(yt_server, express_warehouses, shopinfo, tovar_tree, promo_details_info, offers):
    with PromoMatcherTestEnv(
        operation_mode='mstat',
        yt_stuff=yt_server,
        express_wh_data=express_warehouses,
        shopsdat=shopinfo,
        tovar_tree_data=tovar_tree,
        promodetails_data=promo_details_info,
        offers_data=offers,
    ) as env:
        env.execute(keep_temporaries=False)
        env.verify()
        yield env


def test_mstat_matcher(workflow_mstat, express_warehouses, shopinfo, tovar_tree, promo_details_info, offers):

    express_wh_table_data = workflow_mstat.express_wh_table_data
    assert len(express_wh_table_data) == len(express_warehouses)
    assert express_wh_table_data[0] == dict(warehouse_id=1)
    assert express_wh_table_data[1] == dict(warehouse_id=70)

    tovar_tree_table_data = workflow_mstat.tovar_tree_table_data
    tovar_tree_len = len(tovar_tree)
    assert len(tovar_tree_table_data) == tovar_tree_len
    hids = (90401, 90402, 90403, 90404, 90405)
    parent_hids = (0, 90401, 90401, 90402, 90402)
    for i in range(tovar_tree_len):
        pb = TovarTree_pb2.TovarCategory.FromString(yt.yson.get_bytes(tovar_tree_table_data[i]['data']))
        assert pb.hid == hids[i]
        assert pb.parent_hid == parent_hids[i]

    shopsdat_table_data = workflow_mstat.shopsdat_table_data
    assert len(shopsdat_table_data) == len(shopinfo)
    assert shopsdat_table_data == [dict(datafeed_id=feed, warehouse_id=wh, is_dsbs=dsbs) for feed, wh, dsbs in shopinfo]

    promodetails_table_data = workflow_mstat.promodetails_table_data
    promo_details_info_len = len(promo_details_info)
    assert len(promodetails_table_data) == promo_details_info_len
    for i in range(promo_details_info_len):
        actual_pb = PromoDetails.FromString(yt.yson.get_bytes(promodetails_table_data[i]['promo']))
        expect_pb = PromoDetails.FromString(promo_details_info[i][1])
        assert actual_pb == expect_pb

    offers_table_data = workflow_mstat.offers_table_data
    offers_len = len(offers)
    assert len(offers_table_data) == offers_len
    for i in range(offers_len):
        offer_row = offers_table_data[i]
        assert offer_row['feed_id'] == offers[i][0]

    result_offers_table_data = workflow_mstat.result_offers_table_data
    assert len(result_offers_table_data) == offers_len
    found = 0
    for i in range(offers_len):
        offer_row = result_offers_table_data[i]
        if offer_row['shop_sku'] == 'offer1':
            assert offer_row['available_promo_ids'] == 'promo_id_1'
            assert offer_row['promo_parameters'] == [
                {
                    'blue_flash': {
                        'old_price': {'currency': None, 'value': None},
                        'price': {'currency': 'RUR', 'value': 1},
                    },
                    'direct_discount': None,
                    'shop_promo_id': 'promo_id_1',
                }
            ]
            found += 1
        if offer_row['shop_sku'] == 'offer2':
            assert offer_row['available_promo_ids'] is None
            found += 1
        if offer_row['shop_sku'] == 'offer3':
            assert offer_row['available_promo_ids'] is None
            found += 1
        if offer_row['shop_sku'] == 'offer_part1':
            assert offer_row['promo_parameters'] == [
                {
                    'blue_flash': None,
                    'direct_discount': {
                        'discount_percent': None,
                        'discount_price': {'currency': 'RUR', 'value': 0},
                        'max_discount': {'currency': None, 'value': None},
                        'max_discount_percent': None,
                        'old_price': {'currency': None, 'value': None},
                        'subsidy': {'currency': None, 'value': None},
                    },
                    'shop_promo_id': 'promo_id_dd',
                }
            ]
            found += 1
        if offer_row['shop_sku'] == 'offer_part2':
            assert offer_row['promo_parameters'] == [
                {
                    'blue_flash': None,
                    'direct_discount': {
                        'discount_percent': None,
                        'discount_price': {'currency': 'RUR', 'value': 0},
                        'max_discount': {'currency': None, 'value': None},
                        'max_discount_percent': None,
                        'old_price': {'currency': None, 'value': None},
                        'subsidy': {'currency': None, 'value': None},
                    },
                    'shop_promo_id': 'promo_id_dd',
                }
            ]
            found += 1
    assert found == 5


@pytest.yield_fixture(scope="module")
def workflow_idx(yt_server, express_warehouses, shopinfo, tovar_tree, promo_details_info, offers):
    with PromoMatcherTestEnv(
        operation_mode='idx',
        yt_stuff=yt_server,
        express_wh_data=express_warehouses,
        shopsdat=shopinfo,
        tovar_tree_data=tovar_tree,
        promodetails_data=promo_details_info,
        offers_data=offers,
    ) as env:
        env.execute(keep_temporaries=False)
        env.verify()
        yield env


def test_idx_matcher(workflow_idx, promo_details_info, offers):

    # 'offer2' и 'offer3' не имеют промок, потому (в режиме IDX) - не должны попасть в выходную таблицу
    offers_len = len(offers) - 2

    result_offers_table_data = workflow_idx.result_offers_table_data
    assert len(result_offers_table_data) == offers_len
    found = 0
    for i in range(offers_len):
        offer_row = result_offers_table_data[i]
        if offer_row['offer_id'] == 'offer1':
            assert offer_row['promo_keys'] == [promo_details_info[0][0]]
            assert offer_row['shop_promo_ids'] == [PromoDetails.FromString(promo_details_info[0][1]).shop_promo_id]
            found += 1
    assert found == 1

    promo_details_info_len = len(promo_details_info)
    result_stats_table_data = workflow_idx.result_stats_table_data
    # промо-деталька 'promo_id_dd' разрезана на 2 части
    assert len(result_stats_table_data) == promo_details_info_len - 1
    found = 0
    for row in result_stats_table_data:
        if row['shop_promo_id'] == 'promo_id_1':
            assert row['promo_key'] == promo_details_info[0][0]
            assert row['offers_count_1p'] == 1
            assert row['offers_count_3p'] == 0
            assert row['partners_count'] == 1
            found += 1
    assert found == 1
