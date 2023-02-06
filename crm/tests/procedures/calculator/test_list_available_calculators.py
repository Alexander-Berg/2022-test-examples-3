import pytest
from crm.agency_cabinet.rewards.common import structs
from crm.agency_cabinet.rewards.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.ListAvailableCalculators()


async def test_list_available_calculators(procedure, fixture_calculator_data2, fixture_contracts):
    got = await procedure(structs.ListAvailableCalculatorsInput(
        agency_id=fixture_contracts[3].agency_id,
    ))

    assert got.calculators_descriptions == [
        structs.CalculatorDescription(
            contract_id=fixture_calculator_data2[0].contract_id,
            service=fixture_calculator_data2[0].service,
            version=fixture_calculator_data2[0].version,
            contract_type=fixture_contracts[3].type
        ),
        structs.CalculatorDescription(
            contract_id=fixture_calculator_data2[1].contract_id,
            service=fixture_calculator_data2[1].service,
            version=fixture_calculator_data2[1].version,
            contract_type=fixture_contracts[4].type
        )
    ]


async def test_list_available_calculators_filter_contract_id(procedure, fixture_calculator_data2, fixture_contracts):
    got = await procedure(structs.ListAvailableCalculatorsInput(
        agency_id=fixture_contracts[3].agency_id,
        contract_id=fixture_contracts[3].id
    ))

    assert got.calculators_descriptions == [
        structs.CalculatorDescription(
            contract_id=fixture_calculator_data2[0].contract_id,
            service=fixture_calculator_data2[0].service,
            version=fixture_calculator_data2[0].version,
            contract_type=fixture_contracts[3].type
        ),
    ]
