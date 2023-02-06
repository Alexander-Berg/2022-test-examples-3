# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import Mock, patch

from factory.random import reseed_random
from pytest import fixture
from sqlalchemy import create_engine

from travel.rasp.bus.db.models.admin_user import AdminUser, AdminUserRole
from travel.rasp.bus.db.models.carrier import Carrier
from travel.rasp.bus.db.models.carrier_matching import CarrierMatching
from travel.rasp.bus.db.models.matching import PointMatching, PointMatchingLog
from travel.rasp.bus.db.models.order import Order
from travel.rasp.bus.db.models.order_log_entry import OrderLogEntry
from travel.rasp.bus.db.models.register_type import RegisterType
from travel.rasp.bus.db.models.shared import Base
from travel.rasp.bus.db.models.storage import Storage
from travel.rasp.bus.db.models.supplier import Supplier
from travel.rasp.bus.db.tests import Session


@fixture(scope="session")
def engine():
    engine = create_engine("postgres://rasp:@localhost:6432/rasp_postgres_test")
    engine.execute("create schema buses")
    engine.execute("create schema point_matching")
    engine.execute("create schema yabus")
    Base.metadata.create_all(
        engine,
        tables=[
            model.__table__
            for model in (
                AdminUser,
                AdminUserRole,
                Carrier,
                CarrierMatching,
                Order,
                OrderLogEntry,
                PointMatching,
                PointMatchingLog,
                RegisterType,
                Storage,
                Supplier,
            )
        ],
    )
    Session.configure(bind=engine)


@fixture
def session(engine):
    reseed_random(0)
    session = Session()
    session.commit = Mock(return_value=None)
    session.close = Mock(return_value=None)
    with patch("travel.rasp.bus.db.Session", return_value=session):
        try:
            yield session
        finally:
            session.rollback()
            Session.remove()
