from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.prof.synchronizer import \
    ProfZenActualDataSynchronizer


@dataclass
class ZenDataStruct:
    dt: str
    revenue: float
    revenue_prev: float


async def test_add_calculator_data_zen(contract):
    await ProfZenActualDataSynchronizer().process_data(
        [
            (contract.id, [ZenDataStruct('2022-01-01', 100, 200),
                           ZenDataStruct('2022-02-01', 400, 500)]),
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
                {
                    'index_id': 'revenue',
                    'revenue': 100.0
                },
                {
                    'index_id': 'revenue_prev',
                    'revenue': 200.0
                }
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': False
        },
        {
            'grades': [],
            'indexes': [
                {
                    'index_id': 'revenue',
                    'revenue': 400.0
                },
                {
                    'index_id': 'revenue_prev',
                    'revenue': 500.0
                }
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': False
        }
    ]}
