import pytest
import datetime
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetActiveClients()


@pytest.mark.parametrize(('request_params', 'expected'), [
    (
        {
            'left_date_from': datetime.datetime(2020, 3, 1),
            'left_date_to': datetime.datetime(2020, 4, 1),
            'right_date_from': datetime.datetime(2020, 4, 1),
            'right_date_to': datetime.datetime(2020, 5, 1)
        },
        structs.GetActiveClientsResponse(
            other=[structs.ActiveClientsPart(customers_at_left_date=2, customers_at_right_date=1)],
            current=structs.ActiveClientsPart(customers_at_left_date=1, customers_at_right_date=1),
            percent_less=Decimal('0.000')
        )
    )
])
async def test_get_active_clients(procedure, fixture_agency2, fixture_analytics, request_params, expected):
    result = await procedure(
        structs.GetActiveClientsRequest(
            agency_id=fixture_agency2.id,
            **request_params
        )
    )
    assert result == expected
