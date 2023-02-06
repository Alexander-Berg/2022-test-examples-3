# coding=utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.bus.db.models.admin_user import AdminUser
from travel.rasp.bus.db.models.matching import PointMatching
from travel.rasp.bus.db.models.supplier import Supplier
from travel.rasp.bus.db.tests.factories import AdminUserRoleFactory, PointMatchingFactory, SupplierFactory


def test(session):
    admin_user_role = AdminUserRoleFactory()
    admin_user = session.query(AdminUser).one()
    assert admin_user.roles == [admin_user_role, ]

    supplier = SupplierFactory()
    assert session.query(Supplier).one().name == supplier.name

    point_matching = PointMatchingFactory(supplier=supplier, updated_by=admin_user)
    assert session.query(PointMatching).one().title == point_matching.title
    assert session.query(PointMatching).one().supplier.register_type.code == supplier.register_type.code
    assert session.query(PointMatching).one().updated_by == admin_user
    assert session.query(Supplier).one().point_matchings == [point_matching, ]
