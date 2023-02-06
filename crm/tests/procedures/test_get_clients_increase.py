
import pytest
import datetime
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetClientsIncrease()


@pytest.mark.parametrize(('request_params', 'expected'), [
    (
        {
            'left_date_from': datetime.datetime(2020, 3, 1),
            'left_date_to': datetime.datetime(2020, 4, 1),
            'right_date_from': datetime.datetime(2020, 4, 1),
            'right_date_to': datetime.datetime(2020, 5, 1)
        },
        structs.GetClientsIncreaseResponse(
            other=[structs.ClientsIncreasePart(new_customers=0, customers=1, new_customers_prev=0, customers_prev=0)],
            current_at_right_date=structs.ClientsIncreasePart(
                new_customers=0,
                customers=1,
                new_customers_prev=0,
                customers_prev=0
            ),
            current_at_left_date=structs.ClientsIncreasePart(
                new_customers=1,
                customers=1,
                new_customers_prev=0,
                customers_prev=0
            ),
            percent_less=Decimal('0.000')
        )
    )
])
async def test_get_clients_increase(procedure, fixture_agency2, fixture_analytics, request_params, expected):
    result = await procedure(
        structs.GetClientsIncreaseRequest(
            agency_id=fixture_agency2.id,
            **request_params
        )
    )
    assert result == expected
