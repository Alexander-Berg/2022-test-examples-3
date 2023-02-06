from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.predicts.aggregator.synchronizer import \
    MetaAggregatorPredictsSynchronizer, AggregatorBusinessPredictsSynchronizer


@dataclass
class DataStruct:
    dt: str

    predict_m_uniqd_aggr_d_150k: float
    predict_m_amt_aggr_d_150k: float
    predict_m_uniqd_aggr_c_150k_1m: float
    predict_m_amt_aggr_c_150k_1m: float
    predict_m_uniqd_aggr_b_1m_5m: float
    predict_m_amt_aggr_b_1m_5m: float
    predict_m_uniqd_aggr_a_5m: float
    predict_m_amt_aggr_a_5m: float

    predict_q_amt_autostrategy: float
    revenue: float
    predict_q_amt_k50: float
    predict_q_amt_goal: float
    predict_q_amt_metrika: float
    predict_q_amt_retargeting: float
    predict_m_amt_rsya: float
    predict_q_amt_search_autotargeting_not_uac: float
    predict_amt_media_video_zen: float
    amt_media_video_zen_prev: float


@dataclass
class BusinessDataStruct:
    dt: str
    predict_business_grade_a: float
    predict_business_grade_b: float
    predict_business_grade_c: float
    predict_business_grade_d: float


async def test_add_calculator_data(contract_aggregator):
    await MetaAggregatorPredictsSynchronizer().process_data(
        [
            (
                contract_aggregator.id,
                [
                    DataStruct(
                        '2022-01-01',
                        1,
                        100,
                        2,
                        200,
                        3,
                        300,
                        4,
                        400,
                        100000,
                        200000,
                        300000,
                        400000,
                        500000,
                        600000,
                        700000,
                        800000,
                        100000,
                        100000
                    )
                ]
            ),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract_aggregator.id,
            models.CalculatorData.service == 'aggregator',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None

    await data.delete()

    assert data.data == {'months': [
        {
            'indexes': [
                {
                    'index_id': 'conversion_autostrategy',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'early_payment',
                    'revenue': 200000.0
                },
                {
                    'index_id': 'k50',
                    'revenue': 300000.0
                },
                {
                    'index_id': 'key_goals',
                    'revenue': 400000.0
                },
                {
                    'index_id': 'metrica',
                    'revenue': 500000.0
                },
                {
                    'index_id': 'retargeting',
                    'revenue': 600000.0
                },
                {
                    'index_id': 'revenue_without_direct',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'revenue_without_direct_prev',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'rsya',
                    'revenue': 700000.0
                },
                {
                    'index_id': 'search_autotargeting',
                    'revenue': 800000.0
                }
            ],

            'grades': [
                {
                    'domains_count': 4,
                    'grade_id': 'A',
                    'revenue_average': 5000100.0
                },
                {
                    'domains_count': 3,
                    'grade_id': 'B',
                    'revenue_average': 1000100.0
                },
                {
                    'domains_count': 2,
                    'grade_id': 'C',
                    'revenue_average': 150100.0
                },
                {
                    'domains_count': 1,
                    'grade_id': 'D',
                    'revenue_average': 100.0
                },
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_update_calculator_data(contract_aggregator, calculator_data_meta_aggregator):
    await MetaAggregatorPredictsSynchronizer().process_data(
        [
            (
                contract_aggregator.id,
                [
                    DataStruct(
                        '2022-01-01',
                        10,
                        1000,
                        20,
                        2000,
                        30,
                        3000,
                        40,
                        4000,
                        100001,
                        200002,
                        300003,
                        400004,
                        500005,
                        600006,
                        700007,
                        800008,
                        100001,
                        100001,
                    ),
                    DataStruct(
                        '2022-02-01',
                        1,
                        100,
                        2,
                        200,
                        3,
                        300,
                        4,
                        400,
                        100000,
                        200000,
                        300000,
                        400000,
                        500000,
                        600000,
                        700000,
                        800000,
                        100000,
                        100000,
                    )
                ]
            ),
        ]
    )

    data = await models.CalculatorData.query.where(
        models.CalculatorData.id == calculator_data_meta_aggregator.id
    ).gino.first()

    assert data.data == {'months': [
        {
            'grades': [
                {
                    'domains_count': 40,
                    'grade_id': 'A',
                    'revenue_average': 5000100.0
                },
                {
                    'domains_count': 30,
                    'grade_id': 'B',
                    'revenue_average': 1000100.0
                },
                {
                    'domains_count': 20,
                    'grade_id': 'C',
                    'revenue_average': 150100.0
                },
                {
                    'domains_count': 10,
                    'grade_id': 'D',
                    'revenue_average': 100.0
                },
            ],
            'indexes': [
                {
                    'index_id': 'conversion_autostrategy',
                    'revenue': 100001.0
                },
                {
                    'index_id': 'early_payment',
                    'revenue': 200002.0
                },
                {
                    'index_id': 'k50',
                    'revenue': 300003.0
                },
                {
                    'index_id': 'key_goals',
                    'revenue': 400004.0
                },
                {
                    'index_id': 'metrica',
                    'revenue': 500005.0
                },
                {
                    'index_id': 'retargeting',
                    'revenue': 600006.0
                },
                {
                    'index_id': 'revenue_without_direct',
                    'revenue': 100001.0
                },
                {
                    'index_id': 'revenue_without_direct_prev',
                    'revenue': 100001.0
                },
                {
                    'index_id': 'rsya',
                    'revenue': 700007.0
                },
                {
                    'index_id': 'search_autotargeting',
                    'revenue': 800008.0
                }
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        },
        {
            'grades': [
                {
                    'domains_count': 4,
                    'grade_id': 'A',
                    'revenue_average': 5000100.0
                },
                {
                    'domains_count': 3,
                    'grade_id': 'B',
                    'revenue_average': 1000100.0
                },
                {
                    'domains_count': 2,
                    'grade_id': 'C',
                    'revenue_average': 150100.0
                },
                {
                    'domains_count': 1,
                    'grade_id': 'D',
                    'revenue_average': 100.0
                },
            ],
            'indexes': [
                {
                    'index_id': 'conversion_autostrategy',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'early_payment',
                    'revenue': 200000.0
                },
                {
                    'index_id': 'k50',
                    'revenue': 300000.0
                },
                {
                    'index_id': 'key_goals',
                    'revenue': 400000.0
                },
                {
                    'index_id': 'metrica',
                    'revenue': 500000.0
                },
                {
                    'index_id': 'retargeting',
                    'revenue': 600000.0
                },
                {
                    'index_id': 'revenue_without_direct',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'revenue_without_direct_prev',
                    'revenue': 100000.0
                },
                {
                    'index_id': 'rsya',
                    'revenue': 700000.0
                },
                {
                    'index_id': 'search_autotargeting',
                    'revenue': 800000.0
                }
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_business(contract_aggregator):
    await AggregatorBusinessPredictsSynchronizer().process_data(
        [
            (contract_aggregator.id, [BusinessDataStruct('2022-01-01', 40, 30, 20, 10),
                                      BusinessDataStruct('2022-02-01', 400, 300, 200, 100)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract_aggregator.id,
            models.CalculatorData.service == 'business',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [
        {'grades': [
            {
                'grade_id': 'A',
                'revenue': 40.0,
            },
            {
                'grade_id': 'B',
                'revenue': 30.0,
            },
            {
                'grade_id': 'C',
                'revenue': 20.0,
            },
            {
                'grade_id': 'D',
                'revenue': 10.0,
            }
        ],
            'indexes': [],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        },
        {
            'grades': [
                {
                    'grade_id': 'A',
                    'revenue': 400.0,
                },
                {
                    'grade_id': 'B',
                    'revenue': 300.0,
                },
                {
                    'grade_id': 'C',
                    'revenue': 200.0,
                },
                {
                    'grade_id': 'D',
                    'revenue': 100.0,
                }
            ],
            'indexes': [],
            'period_from': '2022-02-01T00:00:00',
            'predict': True
        }
    ]}
