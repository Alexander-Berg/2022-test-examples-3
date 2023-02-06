from dataclasses import dataclass
from sqlalchemy import and_

from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.celery.tasks.calculator.actual.base.synchronizer import \
    BaseBusinessActualDataSynchronizer


@dataclass
class BusinessDataStruct:
    dt: str
    predict_business_grade_a: float
    predict_business_grade_b: float
    predict_business_grade_c: float
    predict_business_grade_d: float


async def test_add_calculator_data_business(base_contract):
    await BaseBusinessActualDataSynchronizer().process_data(
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
            'predict': False
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
            'predict': False
        }
    ]}
