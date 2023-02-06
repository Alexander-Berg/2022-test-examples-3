from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.base.synchronizer import \
    BaseZenActualDataSynchronizer


@dataclass
class ZenDataStruct:
    dt: str
    revenue: float


async def test_add_calculator_data_zen(base_contract):
    await BaseZenActualDataSynchronizer().process_data(
        [
            (base_contract.id, [ZenDataStruct('2022-01-01', 100),
                                ZenDataStruct('2022-02-01', 400)]),
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
                    'index_id': 'revenue',
                    'revenue': 100.0
                },
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
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': False
        }
    ]}
