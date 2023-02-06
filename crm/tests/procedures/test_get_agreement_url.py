import typing

import pytest
from crm.agency_cabinet.documents.common.structs import (
    GetAgreementUrlInput
)
from crm.agency_cabinet.documents.server.src.procedures import GetAgreementUrl
from crm.agency_cabinet.documents.server.src.exceptions import FileNotFound, UnsuitableAgencyException
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return GetAgreementUrl()


async def test_get_agreement_url(
    procedure,
    fixture_agreements: typing.List[RowProxy],
):
    with pytest.raises(FileNotFound):
        await procedure(GetAgreementUrlInput(
            agency_id=321,
            agreement_id=fixture_agreements[1].id,)

        )


async def test_get_agreement_url_unsuitable_agency(
    procedure,
    fixture_agreements: typing.List[RowProxy],
):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(GetAgreementUrlInput(
            agency_id=123,
            agreement_id=fixture_agreements[1].id,)

        )
