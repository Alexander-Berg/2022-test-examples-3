import typing

import pytest
import os
from unittest import mock
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/{agency_id}/users/{user_id}'


def permissions_js_to_struct(js: typing.List[typing.Dict]):
    return [
        grants_structs.InputRole(
            role_id=role['role_id'],
            permissions=[
                grants_structs.Permission(name=permission) for permission in role['permissions']
            ]
        ) for role in js
    ]


async def test_edit_user(
    client: BaseTestClient,
    yandex_uid: int,
    service_discovery: ServiceDiscovery
):
    js = [
        {
            'permissions': ['perm1', 'perm2'],
            'name': 'owner',
            'role_id': 1
        },
    ]
    await client.post(
        URL.format(agency_id=1, user_id=1),
        expected_status=200,
        json=js
    )

    service_discovery.grants.edit_user.assert_awaited_with(
        agency_id=1,
        yandex_uid=yandex_uid,
        user_id=1,
        roles=permissions_js_to_struct(js),
        partner_id=None
    )


@pytest.mark.parametrize(
    'request_json',
    [

        [
            {
                'permissions': [],
                'name': 'owner',
                'role_id': 1
            },
        ],
        [
            {
                'permissions': [],
                'name': 'owner',
                'role_id': 1
            },
            {
                'permissions': ['rewards'],
                'name': 'owner',
                'role_id': 2
            },
        ],
    ]
)
async def test_zero_permissions_for_one_role(
    client: BaseTestClient,
    yandex_uid: int,
    request_json
):
    await client.post(
        URL.format(agency_id=1, user_id=1),
        expected_status=422,
        json=request_json
    )


async def test_revoke_all_roles(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
):
    await client.post(
        URL.format(agency_id=1, user_id=1),
        expected_status=200,
        json=[
        ]
    )

    service_discovery.grants.edit_user.assert_awaited_with(
        agency_id=1,
        yandex_uid=yandex_uid,
        user_id=1,
        roles=[],
        partner_id=None
    )


async def test_edit_user_partner_id(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
):
    with mock.patch.dict(os.environ, {"USE_REPORTS_API_NEW_GRANTS": 'True', "USE_AGENCY_ID_AS_PARTNER_ID": 'True'},
                         clear=True):
        agency_id = 1
        js = [
            {
                'permissions': ['perm1', 'perm2'],
                'name': 'owner',
                'role_id': 1
            },
        ]
        await client.post(
            URL.format(agency_id=agency_id, user_id=1),
            expected_status=200,
            json=js
        )

        service_discovery.grants.edit_user.assert_awaited_with(
            agency_id=None,
            partner_id=agency_id,
            yandex_uid=yandex_uid,
            user_id=1,
            roles=permissions_js_to_struct(js),
        )
