# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import time

from factory import Faker, LazyFunction, Sequence, SubFactory
from factory.alchemy import SQLAlchemyModelFactory
from factory.fuzzy import FuzzyChoice

from travel.rasp.bus.db.models.admin_user import AdminUser, AdminUserRole
from travel.rasp.bus.db.models.matching import PointMatching, PointType
from travel.rasp.bus.db.models.order import Order
from travel.rasp.bus.db.models.order_log_entry import OrderLogEntry
from travel.rasp.bus.db.models.register_type import RegisterType
from travel.rasp.bus.db.models.supplier import Supplier
from travel.rasp.bus.db.tests import Session
from travel.rasp.bus.roles import ROLES


class AdminUserFactory(SQLAlchemyModelFactory):
    class Meta:
        model = AdminUser
        sqlalchemy_session = Session

    login = Sequence(lambda n: 'login_{}'.format(n))


class AdminUserRoleFactory(SQLAlchemyModelFactory):
    class Meta:
        model = AdminUserRole
        sqlalchemy_session = Session

    user = SubFactory(AdminUserFactory)
    name = ROLES.Admin.identifier


class RegisterTypeFactory(SQLAlchemyModelFactory):
    class Meta:
        model = RegisterType
        sqlalchemy_session = Session

    code = Sequence(lambda n: 'code_{}'.format(n))
    title = Faker('word')
    description = Faker('text', max_nb_chars=200)


class SupplierFactory(SQLAlchemyModelFactory):
    class Meta:
        model = Supplier
        sqlalchemy_session = Session

    code = Sequence(lambda n: 'supplier_code_{}'.format(n))
    name = Faker('first_name')
    register_type = SubFactory(RegisterTypeFactory)
    register_number = Sequence(lambda n: 'register_number_{}'.format(n))
    legal_name = Faker('company')
    actual_address = Faker('address')
    legal_address = Faker('address')
    timetable = ''
    first_name = Faker('first_name')
    middle_name = ''
    last_name = Faker('last_name')
    hidden = False


class PointMatchingFactory(SQLAlchemyModelFactory):
    class Meta:
        model = PointMatching
        sqlalchemy_session = Session

    supplier = SubFactory(SupplierFactory)
    supplier_point_id = Sequence(lambda n: 'supplier_point_id_{}'.format(n))
    type = FuzzyChoice([PointType.STATION, PointType.CITY])
    title = Faker('city')
    description = Faker('address')
    latitude = Faker('pyfloat', positive=False, min_value=-90, max_value=90)
    longitude = Faker('pyfloat', positive=False, min_value=-90, max_value=90)
    country = Faker('country_code')
    point_key = Sequence(lambda n: 'c{}'.format(n))
    disabled = False
    outdated = False
    updated_by = SubFactory(AdminUserFactory)


class OrderFactory(SQLAlchemyModelFactory):
    class Meta:
        model = Order
        sqlalchemy_session = Session

    id = Sequence(lambda n: 'order_{}'.format(n))
    ride = '{}'
    booking = '{}'
    contacts = '{}'
    purchase = '{}'


class OrderLogEntryFactory(SQLAlchemyModelFactory):
    class Meta:
        model = OrderLogEntry
        sqlalchemy_session = Session

    order = SubFactory(OrderFactory)
    timestamp = LazyFunction(lambda: int(time.time() * 1000))
