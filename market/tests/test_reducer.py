# -*- coding: utf-8 -*-

import pytest

import yt.wrapper as yt

from google.protobuf.json_format import MessageToDict, MessageToJson

from market.idx.datacamp.proto.promo.Promo_pb2 import PromoType as DataCampPromoType
# from market.idx.datacamp.proto.offer.OfferPromos_pb2 import OfferPromos, MarketPromos, Promos, Promo
from market.idx.datacamp.proto.offer.OfferPromos_pb2 import Promo
from market.idx.promos.promo_reducer.yatf.reducer_env import PromoReducerTestEnv
from market.proto.common.promo_pb2 import ESourceType
# from market.proto.common.common_pb2 import ESupplierFlag
from market.proto.feedparser.Promo_pb2 import PromoDetails

# from market.idx.promos.yatf import make_datacamp_promo
from market.idx.promos.yatf.utils import make_datacamp_promo, DT_NOW, DT_DELTA

# from market.proto.feedparser.Promo_pb2 import PromoDetails
from market.pylibrary.const.offer_promo import PromoType
from market.pylibrary.promo.utils import PromoDetailsHelper
from yt.yson import json_to_yson
# from yt.yson.convert import json_to_yson
from cyson import dumps
import json
from deepdiff import DeepDiff


@pytest.yield_fixture(scope="module")
def datacamp_offers():
    def ax_promo(promoid):
        pb = Promo(id=promoid, active=True, cheapest_as_gift=Promo.CheapestAsGift())
        return dumps(json_to_yson([json.loads(MessageToJson(pb))]), format='binary')

    # feed_id, shop_sku, supplier_type, warehouse_id, anaplan_promos, anaplan_promos_active, dco_promos, partner_cashback_promos, partner_promos, category_interface_promos
    return (
        (1, 'offer1', 3, 70, None, ax_promo('promo_id_1'), None, None, None, None),
        (32, 'offer2', 1, 70, None, None, None, None, None, ax_promo('lalala')),
    )


@pytest.yield_fixture(scope="module")
def datacamp_promos():
    return [(0, d['source'], d['promo_id'], make_datacamp_promo(d)) for d in (
        {
            'promo_id': 'promo_id_1',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.PARTNER_SOURCE,
        },
        {
            'promo_id': 'lalala',
            'type': DataCampPromoType.CHEAPEST_AS_GIFT,
            'source': ESourceType.ANAPLAN,
        },
    )]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, datacamp_offers, datacamp_promos):
    with PromoReducerTestEnv(
        yt_stuff=yt_server,
        datacamp_offers_data=datacamp_offers,
        datacamp_promos_data=datacamp_promos,
        cashback_restrictions_data=[],
        cashback_properties_data=[],
        cashback_priorities_data=[],
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def result_promo_details():

    pd1 = PromoDetailsHelper(ESourceType.PARTNER_SOURCE, PromoType.CHEAPEST_AS_GIFT, 'promo_id_1').add_omr(feed_offer_ids=[(1, 'offer1')])
    pd1.start_date = DT_NOW - DT_DELTA
    pd1.end_date = DT_NOW + DT_DELTA
    pd1.generation_ts = DT_NOW - DT_DELTA * 2
    pd1.cheapest_as_gift = PromoDetails.CheapestAsGift(feed_offer_ids=[PromoDetails.FeedOfferId(feed_id=1, offer_id='offer1')], count=42)

    pd2 = PromoDetailsHelper(ESourceType.ANAPLAN, PromoType.CHEAPEST_AS_GIFT, 'lalala').add_omr(feed_offer_ids=[(32, 'offer2')])
    pd2.anaplan_promo_id = 'lalala'
    pd2.start_date = DT_NOW - DT_DELTA
    pd2.end_date = DT_NOW + DT_DELTA
    pd2.generation_ts = DT_NOW - DT_DELTA * 2
    pd2.cheapest_as_gift = PromoDetails.CheapestAsGift(feed_offer_ids=[PromoDetails.FeedOfferId(feed_id=32, offer_id='offer2')], count=42)

    return {
        pd1.shop_promo_id: pd1,
        pd2.shop_promo_id: pd2,
    }


def test_promo_reducer(workflow, datacamp_offers, datacamp_promos, result_promo_details):

    datacamp_offers_table_data = workflow.datacamp_offers_table_data
    assert len(datacamp_offers_table_data) == len(datacamp_offers)

    datacamp_promos_table_data = workflow.datacamp_promos_table_data
    assert len(datacamp_promos_table_data) == len(datacamp_promos)

    result_reject_table_data = workflow.result_reject_table_data
    assert result_reject_table_data == []

    result_promos_table_data = workflow.result_promos_table_data
    result_promo_details_len = len(result_promo_details)
    assert len(result_promos_table_data) == result_promo_details_len
    for i in range(result_promo_details_len):
        actual_pb = PromoDetails.FromString(yt.yson.get_bytes(result_promos_table_data[i]['promo']))
        expect_pb = result_promo_details[actual_pb.shop_promo_id].protobuf
        atext = f"Promo {actual_pb.shop_promo_id} diffs from expected one"
        assert DeepDiff(MessageToDict(actual_pb), MessageToDict(expect_pb)) == {}, atext
        assert actual_pb == expect_pb, atext
