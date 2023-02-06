# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import patch

from travel.rasp.bus.api.tests.factories import EndpointFactory
from travel.rasp.bus.db.tests.factories import AdminUserFactory, PointMatchingFactory, SupplierFactory

from travel.rasp.bus.api.connectors.client import MetaClient
from travel.rasp.bus.db.models.matching import PointMatching
from travel.rasp.bus.scripts.endpoints_updater import EndpointsUpdater


def test_created(session):

    supplier = SupplierFactory()
    admin_user = AdminUserFactory()

    count = 100
    endpoints = EndpointFactory.create_batch(size=count)

    with patch.object(MetaClient, 'endpoints', return_value=endpoints):
        u = EndpointsUpdater()
        u.run(login=admin_user.login, supplier_codes=[supplier.code])

    assert u.stat.processed == count
    assert len(u.stat.created) == count
    assert len(u.stat.outdated) == 0
    assert len(u.stat.updated) == 0
    for e in endpoints:
        point_matching = session.query(PointMatching).filter(PointMatching.supplier_point_id == e['supplier_id']).one()
        assert point_matching.outdated is False
        assert point_matching.title == e['title']


def test_outdated(session):  # noqa: RedefinedWhileUnused

    supplier = SupplierFactory()
    admin_user = AdminUserFactory()

    count = 100
    endpoints = [EndpointFactory()]
    PointMatchingFactory.create_batch(count, supplier=supplier)

    with patch.object(MetaClient, 'endpoints', return_value=endpoints):
        u = EndpointsUpdater()
        u.run(login=admin_user.login, supplier_codes=[supplier.code])

    assert u.stat.processed == 1
    assert len(u.stat.created) == 1
    assert len(u.stat.outdated) == count
    assert len(u.stat.updated) == count
    assert all([s.outdated for s in session.query(PointMatching) if s.supplier_point_id != endpoints[0]['supplier_id']])


def test_updated(session):  # noqa: RedefinedWhileUnused

    supplier = SupplierFactory()
    admin_user = AdminUserFactory()

    count = 100
    endpoints = EndpointFactory.create_batch(count)
    for e in endpoints:
        PointMatchingFactory(supplier=supplier, supplier_point_id=e['supplier_id'], outdated=True)

    with patch.object(MetaClient, 'endpoints', return_value=endpoints):
        u = EndpointsUpdater()
        u.run(login=admin_user.login, supplier_codes=[supplier.code])

    assert u.stat.processed == count
    assert len(u.stat.created) == 0
    assert len(u.stat.outdated) == 0
    assert len(u.stat.updated) == count
    assert all([s.outdated is False for s in session.query(PointMatching)])


def test_updated_by_heuristics(session):  # noqa: RedefinedWhileUnused

    supplier = SupplierFactory()
    admin_user = AdminUserFactory()

    count = 100
    endpoints = EndpointFactory.create_batch(count)
    for e in endpoints:
        PointMatchingFactory(supplier=supplier, title=e['title'], outdated=True,
                             latitude=e['latitude'], longitude=e['longitude'])

    with patch.object(MetaClient, 'endpoints', return_value=endpoints):
        u = EndpointsUpdater()
        u.run(login=admin_user.login, supplier_codes=[supplier.code])

    assert u.stat.processed == count
    assert len(u.stat.created) == 0
    assert len(u.stat.outdated) == 0
    assert len(u.stat.updated) == count
    for point_matching in session.query(PointMatching):
        assert point_matching.outdated is False
        assert len([1 for e in endpoints if point_matching.supplier_point_id == e['supplier_id']]) == 1
