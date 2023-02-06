# coding: utf-8

import pytest
import six
from hamcrest import assert_that, empty, equal_to

import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
import market.idx.datacamp.proto.errors.Explanation_pb2 as EXP
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.proto.api.GetOfferRecommendations_pb2 import (
    GetRecommendationsResponse,
    GetRecommendationsBatchRequest,
    GetRecommendationsBatchResponse
)
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.pylibrary.proto_utils import message_from_data


def gen_message(code, level):
    params = [
        DTC.Explanation.Param(
            name='{}_param'.format(code),
            value='{}_param_value'.format(code)
        )
    ]
    return DTC.Explanation(
        code=code,
        params=params,
        level=level
    )


def gen_recommendation(status, features, messages):
    return DTC.ValidationResult(
        recommendation_status=status,
        applications=features,
        messages=messages
    )


def gen_verdict(messages):
    return DTC.ValidationResult(
        messages=messages
    )


def gen_verdict_by_source(source, verdicts):
    return DTC.Verdicts(
        meta=DTC.UpdateMeta(
            source=source
        ),
        verdict=[
            DTC.Verdict(
                results=verdicts
            )
        ]
    )


@pytest.fixture(scope='module')
def basic_offers():
    return [
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_with_single_source_recommendations'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdict_by_source(
                        source=DTC.MARKET_MBO,
                        verdicts=[
                            # одна фича в рекомендации
                            gen_recommendation(
                                status=DTC.FINE,
                                features=[DTC.FBS],
                                messages=[]
                            ),
                            # две фичи в рекомендации
                            gen_recommendation(
                                status=DTC.RESOLVE_BLOCKERS,
                                features=[DTC.DBS, DTC.FBY],
                                messages=[
                                    gen_message(
                                        code='resolve_blockers_error',
                                        level=EXP.Explanation.Level.ERROR
                                    ),
                                    gen_message(
                                        code='resolve_blockers_warning',
                                        level=EXP.Explanation.Level.WARNING
                                    )
                                ]
                            ),
                        ]
                    )
                ]
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_with_recommendation_and_verdict'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdict_by_source(
                        source=DTC.MARKET_MBO,
                        verdicts=[
                            # одна рекомендация
                            gen_recommendation(
                                status=DTC.FINE,
                                features=[DTC.FBS],
                                messages=[]
                            ),
                            # вердикт
                            gen_verdict(
                                messages=[
                                    gen_message(
                                        code='verdict_error',
                                        level=EXP.Explanation.Level.ERROR
                                    )
                                ]
                            )
                        ]
                    )
                ]
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_with_multi_source_recommendations'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    # рекомендации от одного источника
                    gen_verdict_by_source(
                        source=DTC.MARKET_MBO,
                        verdicts=[
                            gen_recommendation(
                                status=DTC.FINE,
                                features=[DTC.DBS],
                                messages=[]
                            ),
                            gen_recommendation(
                                status=DTC.RESOLVE_BLOCKERS,
                                features=[DTC.FBY],
                                messages=[
                                    gen_message(
                                        code='resolve_blockers_error',
                                        level=EXP.Explanation.Level.ERROR
                                    )
                                ]
                            ),
                        ]
                    ),
                    # рекомендации от другого источника
                    gen_verdict_by_source(
                        source=DTC.MARKET_MDM,
                        verdicts=[
                            gen_recommendation(
                                status=DTC.RESOLVE_BLOCKERS,
                                features=[DTC.DBS],  # та же модель, но другая рекомендация
                                messages=[
                                    gen_message(
                                        code='resolve_blockers_error',
                                        level=EXP.Explanation.Level.ERROR
                                    )
                                ]
                            ),
                            gen_recommendation(
                                status=DTC.RESOLVE_BLOCKERS,
                                features=[DTC.FBY],  # такая же рекомендация, как и от первого источника
                                messages=[
                                    gen_message(
                                        code='resolve_blockers_error',
                                        level=EXP.Explanation.Level.ERROR
                                    )
                                ]
                            )
                        ]
                    )
                ]
            )
        )),
        offer_to_basic_row(DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=1,
                offer_id='offer_without_recommendations'
            ),
            resolution=DTC.Resolution(
                by_source=[
                    gen_verdict_by_source(
                        source=DTC.MARKET_MBO,
                        verdicts=[
                            # вердикт
                            gen_verdict(
                                messages=[
                                    gen_message(
                                        code='verdict_error',
                                        level=EXP.Explanation.Level.ERROR
                                    )
                                ]
                            )
                        ]
                    )
                ]
            )
        ))
    ]


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        basic_offers_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            shopsdat_cacher=True,
            basic_offers_table=basic_offers_table,
    ) as stroller_env:
        yield stroller_env


def test_get_offer_recommendations_batch(stroller):
    request = message_from_data(
        {'offer_ids': [
            'offer_with_single_source_recommendations',
            'offer_with_recommendation_and_verdict',
            'offer_with_multi_source_recommendations',
            'offer_without_recommendations'
        ]},
        GetRecommendationsBatchRequest()
    )
    response = stroller.post('/v1/partners/1/offers/model_recommendations', data=request.SerializeToString())

    assert_that(response, HasStatus(200))

    assert_that(response.data, IsSerializedProtobuf(GetRecommendationsBatchResponse, {
        'entries': [
            {
                'offer_recommendations': {
                    'offer_id': 'offer_with_single_source_recommendations',
                    'model_recommendations': [
                        {
                            'model': DTC.FBS,
                            'recommendations': [
                                {
                                    'recommendation_status': DTC.FINE,
                                    'source': DTC.MARKET_MBO
                                }
                            ]
                        }
                    ] + [
                        # Рекомендации из списка фич раскидываем отдельно по моделям (если вдруг они будут приходить так)
                        {
                            'model': model,
                            'recommendations': [
                                {
                                    'recommendation_status': DTC.RESOLVE_BLOCKERS,
                                    'source': DTC.MARKET_MBO,
                                    'messages': [
                                        {
                                            'code': 'resolve_blockers_error',
                                            'level': EXP.Explanation.Level.ERROR,
                                            'params': [
                                                {
                                                    'name': 'resolve_blockers_error_param',
                                                    'value': 'resolve_blockers_error_param_value'
                                                }
                                            ]
                                        },
                                        {
                                            'code': 'resolve_blockers_warning',
                                            'level': EXP.Explanation.Level.WARNING,
                                            'params': [
                                                {
                                                    'name': 'resolve_blockers_warning_param',
                                                    'value': 'resolve_blockers_warning_param_value'
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        } for model in [DTC.DBS, DTC.FBY]
                    ]
                }
            },
            {
                'offer_recommendations': {
                    'offer_id': 'offer_with_recommendation_and_verdict',
                    # Рекомендация вернулась, обычного вердикта нет
                    'model_recommendations': [
                        {
                            'model': DTC.FBS,
                            'recommendations': [
                                {
                                    'recommendation_status': DTC.FINE,
                                    'source': DTC.MARKET_MBO
                                }
                            ]
                        }
                    ]
                }
            },
            {
                'offer_recommendations': {
                    'offer_id': 'offer_with_multi_source_recommendations',
                    # Рекомендации от разных источников объединяются под одной моделью
                    'model_recommendations': [
                        {
                            'model': DTC.DBS,
                            'recommendations': [
                                {
                                    'recommendation_status': DTC.FINE,
                                    'source': DTC.MARKET_MBO
                                },
                                {
                                    'recommendation_status': DTC.RESOLVE_BLOCKERS,
                                    'source': DTC.MARKET_MDM,
                                    'messages': [
                                        {
                                            'code': 'resolve_blockers_error',
                                            'level': EXP.Explanation.Level.ERROR,
                                            'params': [
                                                {
                                                    'name': 'resolve_blockers_error_param',
                                                    'value': 'resolve_blockers_error_param_value'
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            'model': DTC.FBY,
                            'recommendations': [
                                {
                                    'recommendation_status': DTC.RESOLVE_BLOCKERS,
                                    'source': source,
                                    'messages': [
                                        {
                                            'code': 'resolve_blockers_error',
                                            'level': EXP.Explanation.Level.ERROR,
                                            'params': [
                                                {
                                                    'name': 'resolve_blockers_error_param',
                                                    'value': 'resolve_blockers_error_param_value'
                                                }
                                            ]
                                        }
                                    ]
                                } for source in [DTC.MARKET_MBO, DTC.MARKET_MDM]
                            ]
                        }
                    ]
                }
            },
            {
                'offer_recommendations': {
                    'offer_id': 'offer_without_recommendations',
                    # Оффер, у которого нет рекомендаций
                    'model_recommendations': empty()
                }
            }
        ]
    }))


def test_get_offer_recommendations(stroller):
    response = stroller.get('/v1/partners/1/offers/model_recommendations?offer_id=offer_with_single_source_recommendations')

    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(GetRecommendationsResponse, {
        'offer_id': 'offer_with_single_source_recommendations',
        'model_recommendations': [
            {
                'model': DTC.FBS,
                'recommendations': [
                    {
                        'recommendation_status': DTC.FINE,
                        'source': DTC.MARKET_MBO
                    }
                ]
            }
        ] + [
            # Рекомендации из списка фич раскидываем отдельно по моделям (если вдруг они будут приходить так)
            {
                'model': model,
                'recommendations': [
                    {
                        'recommendation_status': DTC.RESOLVE_BLOCKERS,
                        'source': DTC.MARKET_MBO,
                        'messages': [
                            {
                                'code': 'resolve_blockers_error',
                                'level': EXP.Explanation.Level.ERROR,
                                'params': [
                                    {
                                        'name': 'resolve_blockers_error_param',
                                        'value': 'resolve_blockers_error_param_value'
                                    }
                                ]
                            },
                            {
                                'code': 'resolve_blockers_warning',
                                'level': EXP.Explanation.Level.WARNING,
                                'params': [
                                    {
                                        'name': 'resolve_blockers_warning_param',
                                        'value': 'resolve_blockers_warning_param_value'
                                    }
                                ]
                            }
                        ]
                    }
                ]
            } for model in [DTC.DBS, DTC.FBY]
        ]
    }))


def test_get_non_existent_offers(stroller):
    # Проверяем, что ручка адекватно работает при попытке запросить несуществующие оффера
    response = stroller.get('/v1/partners/1/offers/model_recommendations?offer_id=aaaaabbbbxxx')
    assert_that(response, HasStatus(400))

    request = message_from_data(
        {'offer_ids': [
            'aaaaaaaaaaa',
            'bbbbbbbbbbbbbb'
        ]},
        GetRecommendationsBatchRequest()
    )
    response = stroller.post('/v1/partners/1/offers/model_recommendations', data=request.SerializeToString())
    assert_that(response, HasStatus(200))
    assert_that(six.ensure_str(response.data), equal_to(''))
