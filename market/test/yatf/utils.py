# -*- coding: utf-8 -*-

from deepdiff import DeepDiff
from google.protobuf.json_format import MessageToDict, ParseDict
from market.proto.feedparser.Promo_pb2 import PromoDetails


def compare_promo_details_collections(result_list, expected_list):

    assert isinstance(result_list, list)
    assert isinstance(expected_list, list)

    # двойное преобразование - чтобы убрать юникод

    result = dict()
    for promo in result_list:
        assert isinstance(promo, dict)
        pb = ParseDict(promo, PromoDetails())
        result[pb.shop_promo_id] = MessageToDict(pb)

    expected = dict()
    for promo in expected_list:
        assert isinstance(promo, dict)
        pb = ParseDict(promo, PromoDetails())
        expected[pb.shop_promo_id] = MessageToDict(pb)

    for k, v in result.iteritems():
        assert k in expected
        assert DeepDiff(expected[k], v) == {}

    for k, v in expected.iteritems():
        assert k in result
        # сами словари уже проверены выше
