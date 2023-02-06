import pytest
import datetime
from decimal import Decimal
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetMarketSituation()


@pytest.mark.parametrize(('request_params', 'expected'), [
    (
        {
            'left_date_from': datetime.datetime(2020, 3, 1),
            'left_date_to': datetime.datetime(2020, 4, 1),
            'right_date_from': datetime.datetime(2020, 1, 1),
            'right_date_to': datetime.datetime(2020, 5, 1)
        },
        structs.GetMarketSituationResponse(
            other=[structs.MarketSituationPart(average_budget=Decimal('325.000'), customers=2)],
            current_at_left_date=structs.MarketSituationPart(average_budget=Decimal('10.000'), customers=1),
            current_at_right_date=structs.MarketSituationPart(average_budget=Decimal('505.000'), customers=1),
            percent_less=Decimal('100.000')
        )
    )
])
async def test_get_market_situation(procedure, fixture_agency2, fixture_analytics, request_params, expected):
    result = await procedure(
        structs.GetMarketSituationRequest(
            agency_id=fixture_agency2.id,
            **request_params
        )
    )
    assert result == expected
