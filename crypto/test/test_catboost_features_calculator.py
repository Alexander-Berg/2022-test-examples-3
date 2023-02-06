#!/usr/bin/env python
# -*- coding: utf-8 -*-

import numpy as np

from crypta.lib.python.bigb_catboost_applier import catboost_features_calculator

from ads.bsyeti.libs.primitives.counter_proto import counter_ids_pb2
from yabs.server.proto.keywords import keywords_data_pb2
from yabs.proto.user_profile_pb2 import Profile


def prepare_profile():
    bb_profile_custom = Profile()

    counter = bb_profile_custom.counters.add()
    counter.counter_id = counter_ids_pb2.ECounterId.CI_QUERY_CATEGORIES_INTEREST
    counter.key.extend([200007098, 200002552, 200004930])
    counter.value.extend([3.46322274208, 8.99720954895, 9.37608909607])

    mobile_models = bb_profile_custom.items.add()
    mobile_models.keyword_id = keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT
    mobile_models.string_value = b'iPhone'

    os = bb_profile_custom.items.add()
    os.keyword_id = keywords_data_pb2.EKeyword.KW_DETAILED_DEVICE_TYPE_BT
    os.uint_values.extend([3])

    for query_text in ('сумки karl lagerfeld карл лагерфельд официальных интернет',
                       'microsoft прекратит производство xbox one x',
                       'новости футбола'):
        query = bb_profile_custom.queries.add()
        query.query_text = query_text

    return bb_profile_custom


def test_catboost_features_calculator():
    profile = prepare_profile()
    calculator = catboost_features_calculator.TCatboostFeaturesCalculator(
        {
            'bindings_200007098': 2,
            'bindings_200002552': 0,
            'bindings_200004930': 1,
            'operating_systems_3': 4,
            'mobile_models_iPhone': 3,
        },
        {
            counter_ids_pb2.ECounterId.CI_QUERY_CATEGORIES_INTEREST: 'bindings',
        },
        {
            keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT: 'mobile_models',
            keywords_data_pb2.EKeyword.KW_DETAILED_DEVICE_TYPE_BT: 'operating_systems',
        },
    )

    reference_text_features = (
        'сумки karl lagerfeld карл лагерфельд официальных интернет. '
        'microsoft прекратит производство xbox one x. новости футбола.'
    )
    reference_float_features = [8.99720954895, 9.37608909607, 3.46322274208, 1, 1]

    assert reference_text_features == calculator.PrepareTextFeatures(profile)
    assert np.allclose(reference_float_features, calculator.PrepareFloatFeatures(profile))
