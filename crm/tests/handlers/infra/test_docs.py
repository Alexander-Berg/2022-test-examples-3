from crm.agency_cabinet.common.testing import BaseTestClient

URL = '/docs/swagger.json'


async def test_get_swagger_json(
    client: BaseTestClient,
):
    got = await client.get(URL, expected_status=200)

    assert got is not None
    definitions = got.get('definitions')
    assert 'OrdReportsList' in definitions
