import pytest

from crm.agency_cabinet.certificates.server.lib.celery.tasks.load_certificates_data.processors import (
    AgencyCertificateProcessor,
)
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_agency_certificates_processor_returns_data(
    direct_synchronizer, agency_certificates
):
    result = list(AgencyCertificateProcessor.process_row(agency_certificates[0]))

    assert result == [
        (
            1234,
            "some_id",
            "direct",
            dt("2021-05-31 10:35:23"),
            dt("2022-05-31 10:35:23"),
        )
    ]
