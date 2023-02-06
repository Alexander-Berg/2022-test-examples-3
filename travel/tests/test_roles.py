# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.bus.roles import ROLES


def test_roles_identities():
    for role_name, role in ROLES.__dict__.items():
        assert role_name == role.identifier, "Role identifier must match property name!"
