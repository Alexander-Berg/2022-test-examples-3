import pytest

from dataclasses import dataclass

from crm.agency_cabinet.grants.common.consts import Roles, Permissions
from crm.agency_cabinet.grants.common.consts import PartnerType
from crm.agency_cabinet.grants.server.src.db import models, queries
from crm.agency_cabinet.grants.server.src.celery.tasks.load_partners.synchronizer import AgencyPartnersSynchronizer


@dataclass
class ContractStruct:
    agency_name: str
    finish_dt: str


async def test_not_enough_permissions(fixture_load_partners_permissions_not_enough):
    with pytest.raises(Exception, match=r"Unable to run.*"):
        await AgencyPartnersSynchronizer().process_data(
            [
                (42, 'agency_name', [ContractStruct('agency_name', '2022-03-01')])
            ]
        )


async def test_create_new_agency(fixture_load_partners_all_permissions, fixture_load_partners_cleanup):
    await AgencyPartnersSynchronizer().process_data(
        [
            (42, 'agency_name', [
                ContractStruct('agency_name_old', '2021-03-01'), ContractStruct('agency_name_new', '2022-03-01')
            ]),
        ]
    )

    partner_42 = await models.Partner.query.where(models.Partner.external_id == '42').gino.first()
    assert partner_42 is not None
    assert partner_42.name == 'agency_name_new'
    assert partner_42.type == PartnerType.agency.value

    roles = await queries.get_partner_roles(partner_42.id)
    assert len(roles) == 4

    owner_role = next((role for role in roles if role.name == Roles.owner.value), None)
    assert owner_role is not None

    permissions = await queries.get_role_permissions(owner_role.id)
    assert len(permissions) == 3
    assert set([Permissions(p.name) for p in permissions]) == \
           {Permissions.roles, Permissions.rewards, Permissions.client_bonuses}


async def test_create_new_agency_wo_contract(fixture_load_partners_all_permissions, fixture_load_partners_cleanup):
    await AgencyPartnersSynchronizer().process_data(
        [
            (43, 'agency_name', None)
        ]
    )

    partner_43 = await models.Partner.query.where(models.Partner.external_id == '43').gino.first()
    assert partner_43 is not None
    assert partner_43.name == 'agency_name'
    assert partner_43.type == PartnerType.agency_wo_contract.value

    roles = await queries.get_partner_roles(partner_43.id)
    assert len(roles) == 4


async def test_update_only_empty_name(fixture_load_partners_all_permissions, fixture_load_partners_empty_name_partner):
    await AgencyPartnersSynchronizer().process_data(
        [
            (44, 'agency_name_updated', None)
        ]
    )

    partner_44 = await models.Partner.query.where(models.Partner.external_id == '44').gino.first()
    assert partner_44 is not None
    assert partner_44.name == 'agency_name_updated'
    assert partner_44.type == PartnerType.agency.value

    roles = await queries.get_partner_roles(partner_44.id)
    assert len(roles) == 0
