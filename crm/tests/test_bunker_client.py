import pytest

from aiohttp.web import json_response

from crm.agency_cabinet.common.bunker import BunkerError, BunkerNotFoundError


@pytest.mark.asyncio
async def test_normal_request_normal_response(aresponses, bunker_host, bunker_project, bunker_client):
    node = 'active-sections'
    version = '2'

    expected_data = {
        "certificates": {
            "agencies": ["7756631"],
            "env": "test"
        },
        "id": f'bunker:/{bunker_project}/{node}#'
    }

    aresponses.add(
        bunker_host, '/v1/cat', 'GET', json_response(data=expected_data, headers={'Etag': f'W/"{version}"'})
    )

    result = await bunker_client.cat(node=node, version=version)

    assert result['data'] == expected_data
    assert result['version'] == version


@pytest.mark.asyncio
async def test_multiple_projects(aresponses, bunker_host, bunker_project, bunker_client):
    project = 'agency-cabinet'
    node = 'active-sections'
    version = '16'

    expected_data = {
        'data': {
            'sections': [
                {'agencies': [1001368, 882154], 'env': ['development', 'testing'], 'name': 'certificates'},
                {'agencies': [882154], 'env': ['development', 'testing'], 'name': 'bonuses'},
                {'agencies': [882154], 'env': ['development', 'testing'], 'name': 'documents'},
                {'agencies': [882154], 'env': ['development', 'testing'], 'name': 'contract'},
                {'agencies': [882154], 'env': ['development', 'testing'], 'name': 'invoice'}
            ],
            'id': f'bunker:/{project}/{node}#'
        },
        'version': version
    }

    aresponses.add(
        bunker_host, f'/v1/cat?node=/{project}/{node}&version={version}', 'GET',
        json_response(data=expected_data, headers={'Etag': f'"{version}"'}),
        match_querystring=True
    )

    result = await bunker_client.cat(project=project, node=node, version=version)

    assert result['data'] == expected_data
    assert result['version'] == version

    aresponses.add(
        bunker_host, f'/v1/cat?node=/{project}/{node}&version={version}', 'GET',
        json_response(data=expected_data, headers={'Etag': f'"{version}"'}),
        match_querystring=True
    )

    result_without_project = await bunker_client.cat(node=node, version=version)

    assert result_without_project['data'] == expected_data
    assert result_without_project['version'] == version

    project = 'ow-bot-test'
    node = '123'
    version = '2'

    expected_data = {
        'data': {
            'id': f'bunker:/{project}/{node}#'
        },
        'version': version
    }

    aresponses.add(
        bunker_host, f'/v1/cat?node=/{project}/{node}&version={version}', 'GET',
        json_response(data=expected_data, headers={'Etag': f'{version}'}),
        match_querystring=True
    )

    result = await bunker_client.cat(project=project, node=node, version=version)

    assert result['data'] == expected_data
    assert result['version'] == version


@pytest.mark.asyncio
async def test_node_not_found(aresponses, bunker_host, bunker_project, bunker_client):
    node = 'unreal_project_name'
    version = 7777

    aresponses.add(
        bunker_host, '/v1/cat', 'GET', json_response(status=404, text='Not Found')
    )

    with pytest.raises(BunkerNotFoundError) as ex:
        await bunker_client.cat(node=node, version=version)

    assert ex.value.node == node
    assert ex.value.version == str(version)


@pytest.mark.asyncio
async def test_bunker_internal_server_error(aresponses, bunker_host, bunker_client):
    node = 'active-sections'
    version = '15'

    aresponses.add(
        bunker_host, '/v1/cat', 'GET', json_response(status=500, text='Internal Server Error')
    )

    with pytest.raises(BunkerError) as ex:
        await bunker_client.cat(node=node, version=version)

    assert ex.value.node == node
    assert ex.value.version == version
