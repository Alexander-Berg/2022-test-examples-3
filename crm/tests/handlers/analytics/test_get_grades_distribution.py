import pytest

from datetime import date
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient


URL = '/api/agencies/{agency_id}/analytics/graphs/grades_distribution'


@pytest.mark.parametrize(
    ('analytics_return_value', 'expected'),
    [
        (
            agencies_structs.GetAverageBudgetDistributionResponse(
                current=[
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('100.000'),
                        grade='0-50',
                        customers=1
                    ),
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='50-200',
                        customers=0
                    ),
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='200+',
                        customers=0
                    )
                ],
                other=[
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='0-50',
                        customers=0
                    ),
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('100.000'),
                        grade='50-200',
                        customers=2
                    ),
                    agencies_structs.AverageBudgetMarketPiePart(
                        percent=Decimal('0.000'),
                        grade='200+',
                        customers=0
                    )
                ],
                median_budget_current=Decimal('10.000'),
                median_budget_other=Decimal('150.000')
            ),
            {
                'description': {
                    'is_higher_than_other_agencies': False,
                    'average_budget': 10.0,
                    'most_customers_grade': '0-50'
                },
                'current': [
                    {'customers': 1, 'percent': 100.0, 'grade': '0-50'},
                    {'customers': 0, 'percent': 0.0, 'grade': '50-200'},
                    {'customers': 0, 'percent': 0.0, 'grade': '200+'}
                ],
                'other': [
                    {'customers': 0, 'percent': 0.0, 'grade': '0-50'},
                    {'customers': 2, 'percent': 100.0, 'grade': '50-200'},
                    {'customers': 0, 'percent': 0.0, 'grade': '200+'}
                ]
            },
        ),
    ]
)
async def test_get_grades_distribution(client: BaseTestClient, service_discovery: ServiceDiscovery,
                                       analytics_return_value,
                                       expected):
    service_discovery.agencies.get_average_budget_distribution.return_value = analytics_return_value

    got = await client.get(
        URL.format(agency_id=1), expected_status=200,
        params={
            'left_date': date(2020, 1, 1).isoformat(),
            'right_date': date(2020, 3, 1).isoformat()
        })

    assert got == expected


async def test_validation(client: BaseTestClient):

    await client.get(
        URL.format(agency_id=1), expected_status=422,
        params={
            'left_date': date(2020, 1, 1).isoformat(),
            'right_date': date(2020, 1, 3).isoformat()
        }
    )

    await client.get(
        URL.format(agency_id=1), expected_status=422,
        params={
            'left_date': date(2020, 3, 1).isoformat(),
            'right_date': date(2020, 1, 1).isoformat()
        }
    )

    await client.get(
        URL.format(agency_id=1), expected_status=422,
        params={
            'left_date': date(2020, 3, 1).isoformat(),
        }
    )

    await client.get(
        URL.format(agency_id=1), expected_status=422,
        params={
            'right_date': date(2020, 1, 1).isoformat()
        }
    )
