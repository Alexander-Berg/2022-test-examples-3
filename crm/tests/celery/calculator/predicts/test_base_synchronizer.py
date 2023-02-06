from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.predicts.base.synchronizer import \
    BaseMediaPredictsSynchronizer, BaseBusinessPredictsSynchronizer, BaseVideoPredictsSynchronizer, \
    BaseZenPredictsSynchronizer, BaseDirectPredictsSynchronizer


@dataclass
class DataStruct:
    dt: str
    revenue: float
    outstream: float = 0
    early_payment: float = 0


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


async def test_add_calculator_data_media(base_contract):
    await BaseMediaPredictsSynchronizer().process_data(
        [
            (base_contract.id, [DataStruct('2022-01-01', 15451.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
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
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_video(base_contract):
    await BaseVideoPredictsSynchronizer().process_data(
        [
            (base_contract.id, [DataStruct('2022-01-01', 15451.06, 11)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
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
                {'index_id': 'outstream', 'revenue': 11.0}
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_zen(base_contract):
    await BaseZenPredictsSynchronizer().process_data(
        [
            (base_contract.id, [DataStruct('2022-01-01', 15451.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
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
                {
                    'index_id': 'revenue', 'revenue': 15451.06
                },
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_direct(base_contract):
    await BaseDirectPredictsSynchronizer().process_data(
        [
            (base_contract.id, [DirectDataStruct('2022-01-01',
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
                                                 10
                                                 )]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
            models.CalculatorData.service == 'direct',
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
                {'index_id': 'early_payment', 'revenue': 15451.06},
                {'index_id': 'rsya', 'revenue': 14451.06},
                {'index_id': 'conversion_autostrategy', 'revenue': 1.0},
                {'index_id': 'key_goals', 'revenue': 2.0},
                {'index_id': 'metrica', 'revenue': 3.0},
                {'index_id': 'retargeting', 'revenue': 7.0}
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        }
    ]}


async def test_update_calculator_data_media(base_contract, calculator_data_media_base):
    await BaseMediaPredictsSynchronizer().process_data(
        [
            (base_contract.id, [DataStruct('2022-01-01', 654321.12, 644321.12), DataStruct('2022-02-01', 123456.06)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        models.CalculatorData.id == calculator_data_media_base.id
    ).gino.first()

    assert data.data == {'months': [
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 654321.12},
                {'index_id': 'revenue', 'revenue': 654321.12},
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': True
        },
        {
            'grades': [],
            'indexes': [
                {'index_id': 'early_payment', 'revenue': 123456.06},
                {'index_id': 'revenue', 'revenue': 123456.06},
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': True
        }
    ]}


async def test_add_calculator_data_business(base_contract):
    await BaseBusinessPredictsSynchronizer().process_data(
        [
            (base_contract.id, [BusinessDataStruct('2022-01-01', 40, 30, 20, 10),
                                BusinessDataStruct('2022-02-01', 400, 300, 200, 100)]),
        ]
    )

    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
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
