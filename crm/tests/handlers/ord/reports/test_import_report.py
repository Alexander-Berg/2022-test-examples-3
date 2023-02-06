import io
import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/import'


@pytest.mark.no_patch_setup_s3_client
@pytest.mark.parametrize(
    (
        'grants_return_value',
        'reports_return_value',
        'expected'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            ord_structs.TaskInfo(
                task_id=123,
                status='requested'
            ),
            {
                'id': 123,
                'status': 'requested'
            }
        ),
    ]
)
async def test_import_report(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int, mocker,
                             grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.import_data.return_value = reports_return_value

    mocker.patch(
        'crm.agency_cabinet.gateway.server.src.procedures.ord.import_data.ImportData._upload_to_mds',
        mock=AsyncMock,
        return_value=('stub', 'stub')
    )

    got = await client.post(
        URL.format(agency_id=1, report_id=1),
        data={'upfile': io.BytesIO(b'some test data')}
    )

    assert got == expected
