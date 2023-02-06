import pytest

from datetime import date
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient


URL = '/api/agencies/{agency_id}/analytics/graphs/clients_increase'


@pytest.mark.parametrize(
    ('analytics_return_value', 'expected'),
    [
        (
            agencies_structs.GetClientsIncreaseResponse(
                other=[agencies_structs.ClientsIncreasePart(new_customers=0, customers=1, new_customers_prev=0, customers_prev=0)],
                current_at_right_date=agencies_structs.ClientsIncreasePart(new_customers=0, customers=1, new_customers_prev=0, customers_prev=0),
                current_at_left_date=agencies_structs.ClientsIncreasePart(new_customers=1, customers=1, new_customers_prev=0, customers_prev=0),
                percent_less=Decimal('0.000')
            ),
            {
                'current_at_left_date': {
                    'customers': 1,
                    'new_customers': 1,
                    'increase_percent': 100.0,
                    'new_customers_prev': 0
                },
                'current_at_right_date': {
                    'customers': 1,
                    'new_customers': 0,
                    'increase_percent': 0.0,
                    'new_customers_prev': 0
                },
                'description': {
                    'increase_for_period': 0.0,
                    'percentage_of_agencies_with_less_increase': 0.0
                },
                'other': [
                    {
                        'customers': 1,
                        'new_customers': 0,
                        'increase_percent': 0.0,
                        'new_customers_prev': 0
                    }
                ]
            }
        ),
    ]
)
async def test_get_clients_increase(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    analytics_return_value,
    expected
):
    service_discovery.agencies.get_clients_increase.return_value = analytics_return_value

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
