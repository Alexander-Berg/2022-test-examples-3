from contextlib import contextmanager
from flask import g
from flask_principal import Identity, RoleNeed

from travel.rasp.bus.admin.app import create_bus_admin
from travel.rasp.bus.roles import ROLES


@contextmanager
def client_context(roles):
    identity = Identity('id')
    for role in roles:
        identity.provides.add(RoleNeed(role.identifier))
    g.identity = identity
    yield
    g.identity = None


def check_cases(roles, cases):
    bus_admin = create_bus_admin()
    with bus_admin.app_context(),\
            client_context(roles):
        views = bus_admin.extensions['admin'][0]._views
        for view in views:
            view_name = type(view).__name__
            assert view_name in cases
            assert view.is_accessible() is cases[view_name]


def test_no_roles():
    check_cases([], {
        'AdminUserView': False,
        'CarrierView': False,
        'CarrierMatchingView': False,
        'PointMatchingView': False,
        'PointMatchingLogsView': False,
        'AdminIndexView': False,
        'CarrierSearchView': False,
        'RegisterTypeView': False,
        'SupplierView': False,
    })


def test_admin_role():
    check_cases([ROLES.Admin], {
        'AdminUserView': True,
        'CarrierView': True,
        'CarrierMatchingView': True,
        'PointMatchingView': True,
        'PointMatchingLogsView': True,
        'AdminIndexView': True,
        'CarrierSearchView': True,
        'RegisterTypeView': True,
        'SupplierView': True,
    })


def test_pointmatching_role():
    check_cases([ROLES.PointMatching], {
        'AdminUserView': False,
        'CarrierView': False,
        'CarrierMatchingView': False,
        'PointMatchingView': True,
        'PointMatchingLogsView': True,
        'AdminIndexView': True,
        'CarrierSearchView': False,
        'RegisterTypeView': False,
        'SupplierView': False,
    })
