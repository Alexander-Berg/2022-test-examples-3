from dataclasses import dataclass

from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.grants.server.src.celery.tasks.load_users.synchronizer import UsersSynchronizer


@dataclass
class UserStruct:
    uid: int
    email: str
    login: str
    fio: str
    rep_type: str


async def test_new_agency(fixture_lu_partners_roles_map, fixture_lu_role_permissions, fixture_lu_user_permissions,
                          fixture_lu_user_roles, fixture_lu_users):
    await UsersSynchronizer().process_data(
        [
            (11, [
                UserStruct(100, 'test@test.ru', 'test', 'Test', 'chief'),
                UserStruct(101, 'test2@test.ru', 'test2', 'Test2', 'main'),
            ])
        ]
    )

    user_100 = await models.User.query.where(models.User.yandex_uid == 100).gino.first()
    user_101 = await models.User.query.where(models.User.yandex_uid == 101).gino.first()

    assert user_100 is not None
    assert user_101 is not None

    roles_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == user_100.id
    ).gino.all()
    assert len(roles_permissions) == 2
    for role_permission in roles_permissions:
        assert role_permission.role_id == fixture_lu_user_roles[3].id
        assert role_permission.is_active is True

    roles_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == user_101.id
    ).gino.all()
    assert len(roles_permissions) == 1
    assert roles_permissions[0].role_id == fixture_lu_user_roles[5].id
    assert roles_permissions[0].permission_id is None
    assert roles_permissions[0].is_active is True


async def test_no_changes(fixture_lu_partners_roles_map, fixture_lu_role_permissions, fixture_lu_user_permissions,
                          fixture_lu_user_roles, fixture_lu_users):
    await UsersSynchronizer().process_data(
        [
            (1, [
                UserStruct(1, 'owner@agency1', 'owner_agency_1', 'Owner agency 1', 'chief'),
                UserStruct(2, 'manager@agency1', 'manager_agency_1', 'Manager agency 1', 'main'),
            ])
        ]
    )

    initial_owner_permissions = [p.id for p in fixture_lu_user_permissions if p.user_id == fixture_lu_users[0].id]
    current_owner_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == fixture_lu_users[0].id
    ).gino.all()
    assert [p.id for p in current_owner_permissions] == initial_owner_permissions


async def test_owner_changed(fixture_lu_partners_roles_map, fixture_lu_role_permissions,
                             fixture_lu_user_permissions, fixture_lu_user_roles, fixture_lu_users):
    await UsersSynchronizer().process_data(
        [
            (1, [
                UserStruct(100, 'test@test.ru', 'test', 'Test', 'chief'),
                UserStruct(2, 'manager@agency1', 'manager_agency_1', 'Manager agency 1', 'main'),
            ])
        ]
    )

    previous_owner_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == fixture_lu_users[0].id
    ).gino.all()
    assert len(previous_owner_permissions) == 0

    new_owner = await models.User.query.where(models.User.yandex_uid == 100).gino.first()
    assert new_owner is not None
    new_owner_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == new_owner.id
    ).gino.all()
    assert len(new_owner_permissions) == 2
    for role_permission in new_owner_permissions:
        assert role_permission.role_id == fixture_lu_user_roles[0].id


async def test_user_removed_from_agency(fixture_lu_partners_roles_map, fixture_lu_role_permissions,
                                        fixture_lu_user_permissions, fixture_lu_user_roles,
                                        fixture_lu_users):
    await UsersSynchronizer().process_data(
        [
            (1, [
                UserStruct(1, 'owner@agency1', 'owner_agency_1', 'Owner agency 1', 'chief'),
            ])
        ]
    )

    removed_user_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == fixture_lu_users[1].id
    ).gino.all()
    assert len(removed_user_permissions) == 0


async def test_not_active_roles_for_agency_wo_contract(fixture_lu_partners_roles_map, fixture_lu_role_permissions,
                                                       fixture_lu_user_permissions, fixture_lu_user_roles, fixture_lu_users):
    await UsersSynchronizer().process_data(
        [
            (22, [
                UserStruct(100, 'test@test.ru', 'test', 'Test', 'chief'),
                UserStruct(101, 'test2@test.ru', 'test2', 'Test2', 'main'),
            ])
        ]
    )

    user_100 = await models.User.query.where(models.User.yandex_uid == 100).gino.first()
    user_101 = await models.User.query.where(models.User.yandex_uid == 101).gino.first()

    roles_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == user_100.id
    ).gino.all()
    assert len(roles_permissions) == 2
    for role_permission in roles_permissions:
        assert role_permission.is_active is False

    roles_permissions = await models.UsersRolesPermissionsMap.query.where(
        models.UsersRolesPermissionsMap.user_id == user_101.id
    ).gino.all()
    assert roles_permissions[0].is_active is False
