import typing
import pytest
from datetime import datetime

from crm.agency_cabinet.common.consts.service import Services
from crm.agency_cabinet.rewards.server.src.db import models


@pytest.fixture(scope='module')
async def fixture_contract():
    return await models.Contract.create(
        eid='test123',
        agency_id=123124,
        payment_type='post',
        type='premium',
    )


@pytest.fixture(scope='module')
async def fixture_calculator_data(fixture_contract: models.Contract):
    direct_months = []
    for m in range(1, 13):
        direct_months.append(
            {
                'period_from': datetime(2021, m, 1).strftime('%Y-%m-%d %H:%M:%S'),
                'predict': False,
                'grades': [
                    {
                        'grade_id': 'A',
                        'domains_count': 10,
                        'revenue_average': 10000
                    },
                    {
                        'grade_id': 'C',
                        'domains_count': 100,
                        'revenue_average': 100
                    }

                ],
                'indexes': [
                    {
                        'index_id': 'some_index',
                        'revenue': 5000
                    }
                ]
            }
        )

    media_months = []
    for m in range(1, 13):
        media_months.append(
            {
                'period_from': datetime(2021, m, 1).strftime('%Y-%m-%d %H:%M:%S'),
                'predict': False,
                'indexes': [
                    {
                        'index_id': 'some_index',
                        'revenue': 5000
                    }
                ]
            }
        )

    rows = [
        dict(
            contract_id=fixture_contract.id,
            service=Services.direct.value,
            data={'months': direct_months},
            version='2021'
        ),
        dict(
            contract_id=fixture_contract.id,
            service=Services.media.value,
            data={'months': media_months},
            version='2021'
        )
    ]

    yield await models.CalculatorData.bulk_insert(rows)

    await models.CalculatorData.delete.gino.status()


async def test_update_direct_data(fixture_calculator_data: typing.List[models.CalculatorData]):
    direct_data = fixture_calculator_data[0]

    direct_months = []
    for m in range(1, 13):
        direct_months.append(
            {
                'period_from': datetime(2021, m, 1).strftime('%Y-%m-%d %H:%M:%S'),
                'predict': False,
                'grades': [
                    {
                        'grade_id': 'A',
                        'domains_count': 2,
                        'revenue_average': 10000
                    },
                    {
                        'grade_id': 'B',
                        'domains_count': 5,
                        'revenue_average': 1000
                    },
                ],
                'indexes': [
                    {
                        'index_id': 'some_index_2',
                        'revenue': 500
                    }
                ]
            }
        )

    await models.CalculatorDataUpdater(direct_data).update_data({'months': direct_months})

    expected = []
    for m in range(1, 13):
        expected.append(
            {
                'period_from': datetime(2021, m, 1).strftime('%Y-%m-%d %H:%M:%S'),
                'predict': False,
                'grades': [
                    {
                        'grade_id': 'A',
                        'domains_count': 2,
                        'revenue_average': 10000
                    },
                    {
                        'grade_id': 'B',
                        'domains_count': 5,
                        'revenue_average': 1000
                    },
                    {
                        'grade_id': 'C',
                        'domains_count': 100,
                        'revenue_average': 100
                    }

                ],
                'indexes': [
                    {
                        'index_id': 'some_index',
                        'revenue': 5000
                    },
                    {
                        'index_id': 'some_index_2',
                        'revenue': 500
                    }
                ]
            }
        )

    updated_direct_data = await models.CalculatorData.query.where(
        models.CalculatorData.id == direct_data.id
    ).gino.first()

    assert updated_direct_data.data == {'months': expected}


async def test_add_or_override_month_data(fixture_calculator_data: typing.List[models.CalculatorData]):
    direct_data = fixture_calculator_data[0]

    new_month_data = {
        'period_from': datetime(2021, 6, 1).strftime('%Y-%m-%d %H:%M:%S'),
        'predict': False,
        'grades': [
            {
                'grade_id': 'A',
                'domains_count': 2,
                'revenue_average': 10000
            },
            {
                'grade_id': 'C',
                'domains_count': 5,
                'revenue_average': 1000
            },
        ],
        'indexes': [
            {
                'index_id': 'some_index_2',
                'revenue': 500
            }
        ]
    }

    await models.CalculatorDataUpdater(direct_data).add_or_override_month_data(new_month_data)

    updated_direct_data = await models.CalculatorData.query.where(
        models.CalculatorData.id == direct_data.id
    ).gino.first()

    assert len(updated_direct_data.data['months']) == 12
    assert updated_direct_data.data['months'][5] == new_month_data
