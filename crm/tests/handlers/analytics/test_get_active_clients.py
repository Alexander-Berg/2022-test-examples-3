import pytest

from datetime import date
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs as agencies_structs
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient


URL = '/api/agencies/{agency_id}/analytics/graphs/active_clients'


@pytest.mark.parametrize(
    ('analytics_return_value', 'expected'),
    [
        (
            agencies_structs.GetActiveClientsResponse(
                other=[agencies_structs.ActiveClientsPart(customers_at_left_date=2, customers_at_right_date=1)],
                current=agencies_structs.ActiveClientsPart(customers_at_left_date=1, customers_at_right_date=1),
                percent_less=Decimal('0.000')
            ),
            {
                'current':
                    {
                        'customers_at_left_date': 1,
                        'customers_at_right_date': 1,
                        'increase': 0
                    },
                'description':
                    {
                        'percentage_of_agencies_with_less_clients': 0.0
                    },
                'other': [
                    {
                        'customers_at_left_date': 2,
                        'customers_at_right_date': 1,
                        'increase': -1
                    }
                    ]
            },
        ),
    ]
)
async def test_get_active_clients(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    analytics_return_value,
    expected
):
    service_discovery.agencies.get_active_clients.return_value = analytics_return_value

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
