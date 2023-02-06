from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.base.synchronizer import \
    BaseDirectActualDataSynchronizer


@dataclass
class DirectDataStruct:
    dt: str
    revenue: float
    early_payment: float
    predict_m_amt_rsya: float
    predict_q_amt_auto_Q3: float
    predict_q_amt_goal_Q3: float
    predict_q_amt_metrika: float
    predict_q_amt_retargeting: float


async def test_add_calculator_data_direct(base_contract):
    await BaseDirectActualDataSynchronizer().process_data(
        [
            (base_contract.id, [DirectDataStruct(
                '2022-01-01',
                15451.06,
                14451.06,
                1,
                2,
                3,
                4,
                5
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

    assert data.data == {
        'months': [
            {
                'grades': [],
                'indexes': [
                    {'index_id': 'revenue', 'revenue': 15451.06},
                    {'index_id': 'early_payment', 'revenue': 14451.06},
                    {'index_id': 'rsya', 'revenue': 1.0},
                    {'index_id': 'conversion_autostrategy', 'revenue': 2.0},
                    {'index_id': 'key_goals', 'revenue': 3.0},
                    {'index_id': 'metrica', 'revenue': 4.0},
                    {'index_id': 'retargeting', 'revenue': 5.0}
                ],
                'period_from': '2022-01-01T00:00:00',
                'predict': False
            }
        ]}
