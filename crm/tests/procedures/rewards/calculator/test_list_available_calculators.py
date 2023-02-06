import pytest
import crm.agency_cabinet.rewards.common.structs as rewards_structs
from crm.agency_cabinet.common.consts import ContractType
from crm.agency_cabinet.gateway.server.src.procedures.rewards.calculator import ListAvailableCalculators
from crm.agency_cabinet.grants.common.structs import AccessLevel

CALCULATORS = [
    rewards_structs.CalculatorDescription(
        contract_id=123456,
        service='media',
        contract_type=ContractType.base.value,
        version='2021'
    ),
    rewards_structs.CalculatorDescription(
        contract_id=123456,
        service='video',
        contract_type=ContractType.base.value,
        version='2021'
    ),
]

RESPONSE = rewards_structs.ListAvailableCalculatorsResponse(
    calculators_descriptions=CALCULATORS,
)


@pytest.fixture
def procedure(service_discovery):
    return ListAvailableCalculators(service_discovery)


async def test_returns_rows_all_params(
    procedure, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.rewards.list_available_calculators.return_value = RESPONSE

    got = await procedure(yandex_uid=123, agency_id=1, contract_id=1)

    assert got == RESPONSE


async def test_returns_rows_no_optional_params(
    procedure, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.rewards.list_available_calculators.return_value = RESPONSE

    got = await procedure(yandex_uid=123, agency_id=22)

    assert got == RESPONSE
