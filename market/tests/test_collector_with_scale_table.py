# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, has_key

from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType as DataCampPromoType
from market.idx.promos.promo_details_collector.yatf.collector_env import PromoDetailsCollectorTestEnv
from market.proto.common.promo_pb2 import ESourceType

from market.idx.promos.yatf.utils import make_datacamp_promos, make_datacamp_offers, make_promo_details


@pytest.fixture(scope="module")
def offers():
    return [
        # соотв. параметрам функции make_datacamp_offers()
        # feed_id, offer_id, warehouse_id, shop_id, promo_id, supplier_type, promo_block
        (100, 'offer_id_1', 200, 300, 'promo_id_1', 1, 'anaplan_promos_active'),
        (200, 'offer_id_2', 300, 400, 'promo_id_1_test', 1, 'anaplan_promos_active'),
    ]


@pytest.fixture(scope="module")
def valid_promos():
    return {
        '4C-x9myciHsUX2u01tH_yA': {
            'promo_id': 'promo_id_1',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
        },
    }


@pytest.fixture(scope="module")
def scale_promos():
    return {
        '10HBPsDsgWeNXE28m42VsQ': {
            'promo_id': 'promo_id_1_test',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
        },
    }


@pytest.yield_fixture(scope="module")
def expected_promo_details(valid_promos):
    result = {}
    for key, data in valid_promos.items():
        result.update(make_promo_details(key, data))
    return result


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, valid_promos, scale_promos):
    datacamp_offers = make_datacamp_offers(offers)
    datacamp_promos = make_datacamp_promos(valid_promos)
    datacamp_scale_promos = make_datacamp_promos(scale_promos)
    with PromoDetailsCollectorTestEnv(yt_server, datacamp_offers, datacamp_promos, [], datacamp_scale_promos) as env:
        env.execute()
        env.verify()
        yield env


def test_scale_promo_table(workflow, expected_promo_details, scale_promos):
    assert_that(len(expected_promo_details), equal_to(1))
    assert_that(len(scale_promos), equal_to(1))
    datacamp_promo_details_data = workflow.datacamp_promo_details_data
    assert_that(len(datacamp_promo_details_data), equal_to(len(expected_promo_details)+len(scale_promos)))

    for (promo_key, part_id), _ in expected_promo_details.items():
        assert_that(datacamp_promo_details_data, has_key((promo_key, part_id)), "Not found promo_key from ordinal promos {} in result".format(promo_key))

    for promo_key, _ in scale_promos.items():
        assert_that(datacamp_promo_details_data, has_key((promo_key, 0)), "Not found promo_key from scale promos {} in result".format(promo_key))
