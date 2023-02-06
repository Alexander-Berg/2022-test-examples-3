from market.dynamic_pricing.pricing.common.prepare_data_for_axapta.prepare_data import (
    select_price,
)
from market.dynamic_pricing.pricing.library.types import PriceGroupType
from market.dynamic_pricing.pricing.library.constants import PRICE_GROUP_PRIORITY


def test_priorities():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'warehouse_id': -1
            },
            [
                {
                    'new_price': 1,
                    'price_group_type': PriceGroupType.DEADSTOCK,
                    'price_group_priority': PRICE_GROUP_PRIORITY[PriceGroupType.DEADSTOCK],
                },
                {
                    'new_price': 10,
                    'price_group_type': PriceGroupType.EXPIRING_GOODS,
                    'price_group_priority': PRICE_GROUP_PRIORITY[PriceGroupType.EXPIRING_GOODS],
                },
                {
                    'new_price': 2,
                    'price_group_type': PriceGroupType.DCO,
                    'price_group_priority': PRICE_GROUP_PRIORITY[PriceGroupType.DCO],
                },
            ]
        )
    ]
    rec = list(select_price(input_data))
    assert len(rec) == 1
    r = rec[0]
    assert (r.new_price, r.warehouse_id, r.price_group_type) == (10, -1, PriceGroupType.EXPIRING_GOODS)


def test_price_cancelling():
    input_data = [
        (
            {
                'market_sku': 1,
                'shop_sku': '1',
                'warehouse_id': 1
            },
            [
                {
                    'new_price': -1,
                    'price_group_type': PriceGroupType.EXPIRING_GOODS,
                    'price_group_priority': PRICE_GROUP_PRIORITY[PriceGroupType.EXPIRING_GOODS],
                },
                {
                    'new_price': 2,
                    'price_group_type': PriceGroupType.DEADSTOCK,
                    'price_group_priority': PRICE_GROUP_PRIORITY[PriceGroupType.DEADSTOCK],
                },
            ]
        )
    ]
    rec = list(select_price(input_data))
    assert len(rec) == 2
    for r in rec:
        if r.price_group_type == PriceGroupType.DEADSTOCK:
            assert (r.new_price, r.warehouse_id, r.price_group_type) == (2, 1, PriceGroupType.DEADSTOCK)
        else:
            assert (r.new_price, r.warehouse_id, r.price_group_type) == (-1, 1, PriceGroupType.EXPIRING_GOODS)
