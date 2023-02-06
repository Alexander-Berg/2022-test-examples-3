import pytest

from crm.agency_cabinet.certificates.common.structs import (
    CertifiedEmployee,
    EmployeeCertificate,
    EmployeeCertificateStatus,
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


@pytest.mark.parametrize("project", [None, "direct"])
@pytest.mark.parametrize("search_query", [None, "alpaca"])
@pytest.mark.parametrize("status", [None, "expired", "expires_in_semiyear", "active"])
async def test_makes_correct_request(client, project, search_query, status):
    client._client.send_proto_message.return_value = (
        ListEmployeesCertificatesResponsePb(
            employees=CertifiedEmployeesPb(employees=[])
        )
    )

    await client.list_employees_certificates(
        agency_id=228,
        offset=0,
        limit=100,
        project=project,
        search_query=search_query,
        status=status,
    )

    client._client.send_proto_message.assert_awaited_with(
        queue_name="certificates",
        message=RpcRequest(
            list_employees_certificates=ListEmployeesCertificatesRequestPb(
                agency_id=228,
                pagination=PaginationPb(offset=0, limit=100),
                project=project,
                search_query=search_query,
                status=status,
            )
        ),
        response_message_type=ListEmployeesCertificatesResponsePb,
    )


async def test_returns_employees_certificates(client):
    client._client.send_proto_message.return_value = ListEmployeesCertificatesResponsePb(
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
                            status=EmployeeCertificatePb.StatusType.ACTIVE,
                        ),
                        EmployeeCertificatePb(
                            project="Директ",
                            start_time=dt("2020-09-06 18:00:00", as_proto=True),
                            expiration_time=dt("2020-10-06 18:00:00", as_proto=True),
                            external_id="another_id",
                            status=EmployeeCertificatePb.StatusType.EXPIRED,
                        ),
                    ],
                ),
                CertifiedEmployeePb(
                    email="capibara@yandex.ru",
                    agency_id=8765,
                    certificates=[
                        EmployeeCertificatePb(
                            project="Директ",
                            start_time=dt("2020-07-06 18:00:00", as_proto=True),
                            expiration_time=dt("2020-08-06 18:00:00", as_proto=True),
                            external_id="very_id",
                            status=EmployeeCertificatePb.StatusType.EXPIRES_IN_SEMIYEAR,
                        )
                    ],
                ),
            ]
        )
    )

    result = await client.list_employees_certificates(
        agency_id=228,
        offset=0,
        limit=100,
        project=None,
        search_query=None,
        status=None,
    )

    assert result == [
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
                    status=EmployeeCertificateStatus.ACTIVE,
                ),
                EmployeeCertificate(
                    project="Директ",
                    start_time=dt("2020-09-06 18:00:00"),
                    expiration_time=dt("2020-10-06 18:00:00"),
                    external_id="another_id",
                    status=EmployeeCertificateStatus.EXPIRED,
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
                    start_time=dt("2020-07-06 18:00:00"),
                    expiration_time=dt("2020-08-06 18:00:00"),
                    external_id="very_id",
                    status=EmployeeCertificateStatus.EXPIRES_IN_SEMIYEAR,
                ),
            ],
        ),
    ]


async def test_returns_empty_list_if_no_certificates_found(client):
    client._client.send_proto_message.return_value = (
        ListEmployeesCertificatesResponsePb(
            employees=CertifiedEmployeesPb(employees=[])
        )
    )

    result = await client.list_employees_certificates(
        agency_id=228,
        offset=0,
        limit=100,
        project=None,
        search_query=None,
        status=None,
    )

    assert result == []
