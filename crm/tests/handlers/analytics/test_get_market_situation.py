import pytest

from datetime import date
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient


URL = '/api/agencies/{agency_id}/analytics/graphs/market_situation'


@pytest.mark.parametrize(
    ('analytics_return_value', 'expected'),
    [
        (
            agencies_structs.GetMarketSituationResponse(
                other=[agencies_structs.MarketSituationPart(average_budget=Decimal('150.000'), customers=2)],
                current_at_left_date=agencies_structs.MarketSituationPart(
                    average_budget=Decimal('10.000'),
                    customers=1
                ),
                current_at_right_date=agencies_structs.MarketSituationPart(
                    average_budget=Decimal('505.000'),
                    customers=1
                ),
                percent_less=Decimal('100.000')
            ),
            {
                'current_at_right_date': {
                    'average_budget': 505.0,
                    'customers': 1
                },
                'current_at_left_date': {
                    'average_budget': 10.0,
                    'customers': 1
                },
                'description': {
                    'agencies_with_less_average_budget_percent': 100.0
                },
                'other': [
                    {
                        'average_budget': 150.0,
                        'customers': 2
                    }
                ]
            },
        ),
    ]
)
async def test_get_market_situation(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    analytics_return_value,
    expected
):
    service_discovery.agencies.get_market_situation.return_value = analytics_return_value

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
