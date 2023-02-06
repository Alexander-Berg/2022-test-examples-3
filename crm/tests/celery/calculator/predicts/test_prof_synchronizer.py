from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.predicts.prof.synchronizer import \
    ProfMediaPredictsSynchronizer, ProfBusinessPredictsSynchronizer, ProfVideoPredictsSynchronizer, \
    ProfZenPredictsSynchronizer, ProfDirectPredictsSynchronizer


@dataclass
class DataStruct:
    dt: str
    revenue: float
    revenue_prev: float
    outstream: float = 0


@dataclass
class BusinessDataStruct:
    dt: str
    predict_business_grade_a: float
    predict_business_grade_b: float
    predict_business_grade_c: float
    predict_business_grade_d: float


@dataclass
class DirectDataStruct:
    dt: str
    revenue: float
    predict_m_amt_rsya: float
    predict_q_amt_auto_Q3: float
    predict_q_amt_goal_Q3: float
    predict_q_amt_metrika: float
    predict_q_amt_video_cpc_Q3: float
    predict_q_amt_smart_banner: float
    predict_q_amt_rmp: float
    predict_q_amt_retargeting: float
    predict_q_amt_k50: float
    predict_q_amt_product_gallery: float
    predict_q_amt_search_autotargeting_not_uac: float
    predict_m_uniqd_prof_d_150k: float
    predict_m_amt_prof_d_150k: float
    predict_m_uniqd_prof_c_150k_800k: float
    predict_m_amt_prof_c_150k_800k: float
    predict_m_uniqd_prof_b_800k_5m: float
    predict_m_amt_prof_b_800k_5m: float
    predict_m_uniqd_prof_a_5m: float
    predict_m_amt_prof_a_5m: float


async def test_add_calculator_data_media(contract):
    await ProfMediaPredictsSynchronizer().process_data(
        [
            (contract.id, [DataStruct('2022-01-01', 15451.06, 14451.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
            models.CalculatorData.service == 'media',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 15451.06},
                {'index_id': 'revenue', 'revenue': 15451.06},
                {'index_id': 'revenue_prev', 'revenue': 14451.06}
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_video(contract):
    await ProfVideoPredictsSynchronizer().process_data(
        [
            (contract.id, [DataStruct('2022-01-01', 15451.06, 14451.06, 11)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
            models.CalculatorData.service == 'video',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 15451.06},
                {'index_id': 'revenue', 'revenue': 15451.06},
                {'index_id': 'revenue_prev', 'revenue': 14451.06},
                {'index_id': 'outstream', 'revenue': 11.0}
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_zen(contract):
    await ProfZenPredictsSynchronizer().process_data(
        [
            (contract.id, [DataStruct('2022-01-01', 15451.06, 14451.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
            models.CalculatorData.service == 'zen',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [
        {
            'grades': [],
            'indexes': [
                {'index_id': 'revenue', 'revenue': 15451.06},
                {'index_id': 'revenue_prev', 'revenue': 14451.06},
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_direct(contract):
    await ProfDirectPredictsSynchronizer().process_data(
        [
            (contract.id, [DirectDataStruct('2022-01-01',
                                            15451.06,
                                            14451.06,
                                            1,
                                            2,
                                            3,
                                            4,
                                            5,
                                            6,
                                            7,
                                            8,
                                            9,
                                            10,
                                            11,
                                            12,
                                            13,
                                            14,
                                            15,
                                            16,
                                            17,
                                            18
                                            )]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
            models.CalculatorData.service == 'direct',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()

    assert data is not None
    await data.delete()

    assert data.data == {'months': [{'grades': [{'domains_count': 11,
                         'grade_id': 'D',
                                                 'revenue_average': 1.0909090909090908},
                                                {'domains_count': 13,
                                                 'grade_id': 'C',
                                                 'revenue_average': 150001.07692307694},
                                                {'domains_count': 15,
                                                 'grade_id': 'B',
                                                 'revenue_average': 800001.0666666667},
                                                {'domains_count': 17,
                                                 'grade_id': 'A',
                                                 'revenue_average': 5000001.05882353}],
                                     'indexes': [{'index_id': 'early_payment', 'revenue': 15451.06},
                                                 {'index_id': 'rsya', 'revenue': 14451.06},
                                                 {'index_id': 'conversion_autostrategy',
                                                  'revenue': 1.0},
                                                 {'index_id': 'key_goals', 'revenue': 2.0},
                                                 {'index_id': 'metrica', 'revenue': 3.0},
                                                 {'index_id': 'video_cpc', 'revenue': 4.0},
                                                 {'index_id': 'smart_banners', 'revenue': 5.0},
                                                 {'index_id': 'rmp', 'revenue': 6.0},
                                                 {'index_id': 'retargeting', 'revenue': 7.0},
                                                 {'index_id': 'k50', 'revenue': 8.0},
                                                 {'index_id': 'product_gallery', 'revenue': 9.0},
                                                 {'index_id': 'search_autotargeting', 'revenue': 10.0}],
                                     'period_from': '2022-01-01T00:00:00',
                                     'predict': True}]}


async def test_update_calculator_data_media(contract, calculator_data_media):
    await ProfMediaPredictsSynchronizer().process_data(
        [
            (contract.id, [DataStruct('2022-01-01', 654321.12, 644321.12), DataStruct('2022-02-01', 123456.06, 113456.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        models.CalculatorData.id == calculator_data_media.id
    ).gino.first()

    assert data.data == {'months': [
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 654321.12},
                {'index_id': 'revenue', 'revenue': 654321.12},
                {'index_id': 'revenue_prev', 'revenue': 644321.12}
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        },
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 123456.06},
                {'index_id': 'revenue', 'revenue': 123456.06},
                {'index_id': 'revenue_prev', 'revenue': 113456.06}
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_business(contract):
    await ProfBusinessPredictsSynchronizer().process_data(
        [
            (contract.id, [BusinessDataStruct('2022-01-01', 40, 30, 20, 10),
                           BusinessDataStruct('2022-02-01', 400, 300, 200, 100)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == contract.id,
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
