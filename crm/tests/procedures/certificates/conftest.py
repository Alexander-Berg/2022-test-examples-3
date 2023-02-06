import pytest

from crm.agency_cabinet.agencies.common.structs import AgencyInfo


@pytest.fixture
def agency_info():
    return AgencyInfo(
        agency_id=1234,
        name="SomeAgency",
        email="some@email.com",
        legal_address="BlagBlah",
        actual_address="FooBar",
        phone="+12345678",
        site="wow.com",
    )
