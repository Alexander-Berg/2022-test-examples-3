from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.certificates.common.structs import (
    CertifiedEmployee,
    CertifiedEmployees,
    EmployeeCertificate,
    EmployeeCertificateStatus,
    ListEmployeesCertificatesRequest,
    Pagination,
)
from crm.agency_cabinet.certificates.proto.certificates_pb2 import (
    CertifiedEmployee as CertifiedEmployeePb,
    CertifiedEmployees as CertifiedEmployeesPb,
    EmployeeCertificate as EmployeeCertificatePb,
    ListEmployeesCertificatesRequest as ListEmployeesCertificatesRequestPb,
    ListEmployeesCertificatesResponse as ListEmployeesCertificatesResponsePb,
)
from crm.agency_cabinet.certificates.proto.common_pb2 import Pagination as PaginationPb
from crm.agency_cabinet.certificates.proto.request_pb2 import RpcRequest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = CertifiedEmployees(
        employees=[
            CertifiedEmployee(
                name="Альпак Альпакыч",
                email="alpaca@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Дзен",
                        start_time=dt("2020-01-06 18:00:00"),
                        expiration_time=dt("2020-02-06 18:00:00"),
                        external_id="some_id",
                        status=EmployeeCertificateStatus.EXPIRED,
                    ),
                    EmployeeCertificate(
                        project="Директ",
                        start_time=dt("2020-07-06 18:00:00"),
                        expiration_time=dt("2020-10-06 18:00:00"),
                        external_id="some_id",
                        status=EmployeeCertificateStatus.ACTIVE,
                    ),
                ],
            ),
            CertifiedEmployee(
                name=None,
                email="capibara@yandex.ru",
                agency_id=8765,
                certificates=[
                    EmployeeCertificate(
                        project="Директ",
                        start_time=dt("2020-05-06 18:00:00"),
                        expiration_time=dt("2020-08-06 18:00:00"),
                        external_id="some_id",
                        status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                    ),
                ],
            ),
        ]
    )

    mocker.patch(
        "crm.agency_cabinet.certificates.server.lib.handler.ListEmployeesCertificates",
        return_value=mock,
    )

    return mock


@pytest.mark.parametrize(
    ("params", "expected"),
    (
        (dict(), dict(project=None, search_query=None, status=None)),
        (dict(project=None), dict(project=None, search_query=None, status=None)),
        (dict(project=""), dict(project="", search_query=None, status=None)),
        (dict(project="direct"), dict(project="direct", search_query=None, status=None)),
        (dict(project="direct", status="active"), dict(project="direct", search_query=None, status="active")),
        (
            dict(project="zen", search_query="alpaca", status="active"),
            dict(project="zen", search_query="alpaca", status="active")
        ),
    ),
)
async def test_calls_procedure(procedure, handler, params, expected):
    input_pb = RpcRequest(
        list_employees_certificates=ListEmployeesCertificatesRequestPb(
            agency_id=8765, pagination=PaginationPb(limit=100, offset=0), **params
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=ListEmployeesCertificatesRequest(
            agency_id=8765, pagination=Pagination(limit=100, offset=0), **expected
        )
    )


async def test_returns_serialized_details(handler):
    input_pb = RpcRequest(
        list_employees_certificates=ListEmployeesCertificatesRequestPb(
            agency_id=8765, pagination=PaginationPb(limit=100, offset=0)
        )
    )

    got = await handler(input_pb.SerializeToString())

    assert ListEmployeesCertificatesResponsePb.FromString(
        got
    ) == ListEmployeesCertificatesResponsePb(
        employees=CertifiedEmployeesPb(
            employees=[
                CertifiedEmployeePb(
                    name="Альпак Альпакыч",
                    email="alpaca@yandex.ru",
                    agency_id=8765,
                    certificates=[
                        EmployeeCertificatePb(
                            project="Дзен",
                            start_time=dt("2020-01-06 18:00:00", as_proto=True),
                            expiration_time=dt("2020-02-06 18:00:00", as_proto=True),
                            external_id="some_id",
                            status=EmployeeCertificatePb.StatusType.EXPIRED,
                        ),
                        EmployeeCertificatePb(
                            project="Директ",
                            start_time=dt("2020-07-06 18:00:00", as_proto=True),
                            expiration_time=dt("2020-10-06 18:00:00", as_proto=True),
                            external_id="some_id",
                            status=EmployeeCertificatePb.StatusType.ACTIVE,
                        ),
                    ],
                ),
                CertifiedEmployeePb(
                    email="capibara@yandex.ru",
                    agency_id=8765,
                    certificates=[
                        EmployeeCertificatePb(
                            project="Директ",
                            start_time=dt("2020-05-06 18:00:00", as_proto=True),
                            expiration_time=dt("2020-08-06 18:00:00", as_proto=True),
                            external_id="some_id",
                            status=EmployeeCertificatePb.StatusType.EXPIRES_IN_SEMIYEAR,
                        )
                    ],
                ),
            ]
        )
    )


async def test_returns_serialized_empty_list_if_no_certificates_for_agency(
    procedure, handler
):
    procedure.return_value = CertifiedEmployees(employees=[])

    input_pb = RpcRequest(
        list_employees_certificates=ListEmployeesCertificatesRequestPb(
            agency_id=8765, pagination=PaginationPb(limit=100, offset=0)
        )
    )

    got = await handler(input_pb.SerializeToString())

    assert ListEmployeesCertificatesResponsePb.FromString(
        got
    ) == ListEmployeesCertificatesResponsePb(
        employees=CertifiedEmployeesPb(employees=[])
    )
