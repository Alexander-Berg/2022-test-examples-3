from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.base.synchronizer import \
    BaseMediaActualDataSynchronizer


@dataclass
class MediaDataStruct:
    dt: str
    early_payment: float
    revenue: float


async def test_add_calculator_data_media(base_contract):
    data = await models.CalculatorData.query.where(
        and_(
            models.CalculatorData.contract_id == base_contract.id,
            models.CalculatorData.service == 'media',
            models.CalculatorData.version == '2022'
        )
    ).gino.first()
    if data is not None:
        await data.delete()  # TODO: better fixtures

    await BaseMediaActualDataSynchronizer().process_data(
        [
            (base_contract.id, [MediaDataStruct('2022-01-01', 100, 200),
                                MediaDataStruct('2022-02-01', 400, 500)]),
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
                {
                    'index_id': 'early_payment',
                    'revenue': 100.0
                },
                {
                    'index_id': 'revenue',
                    'revenue': 200.0
                },
            ],
            'period_from': '2022-01-01T00:00:00',
            'predict': False
        },
        {
            'grades': [],
            'indexes': [
                {
                    'index_id': 'early_payment',
                    'revenue': 400.0
                },
                {
                    'index_id': 'revenue',
                    'revenue': 500.0
                },
            ],
            'period_from': '2022-02-01T00:00:00',
            'predict': False
        }
    ]}
