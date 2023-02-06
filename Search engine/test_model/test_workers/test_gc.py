import uuid
import typing
import time

from search.martylib.db_utils import session_scope
from search.martylib.db_utils.base_model import BaseModel

from search.morty.sqla.morty import model

from search.morty.src.model.workers.gc.main import GarbageCollector
from search.morty.src.common.config import Config
from search.morty.tests.utils.test_case import MortyTestCase


def create_supplier(session, _id, component_id=None, **kwargs):
    session.merge(
        model.Supplier(
            id=_id,
            **kwargs
        )
    )
    if component_id:
        session.commit()
        session.execute(
            model.morty__Supplier__components.insert(),
            {
                'morty__Supplier_id': _id,
                'morty__Component_id': component_id,
            },
        )


def create_supplier_item(session, _id, supplier_id=None, created_at=0.0, **kwargs):
    session.merge(
        model.SupplierItem(
            id=_id,
            created_at=created_at,
            **kwargs,
        )
    )
    if supplier_id:
        session.commit()
        assign_supplier_item(session, _id, supplier_id)


def assign_supplier_item(session, supplier_item_id, supplier_id):
    session.execute(
        model.morty__SupplierItem__suppliers.insert(),
        {
            'morty__Supplier_id': supplier_id,
            'morty__SupplierItem_id': supplier_item_id,
        },
    )


def create_supplier_item_endpoint(session, _id, supplier_item_id=None, **kwargs):
    session.merge(
        model.SupplierItemEndpoint(
            id=_id,
            morty__SupplierItem_id=supplier_item_id,
            **kwargs
        )
    )


class TrackDBRecord:
    """
    Context manager to track changes in the number of database records
    Number of records should change by `delta`
    """
    def __init__(self, _model: typing.Type[BaseModel], delta):
        self.delta = delta
        self.model = _model
        self.initial_amount = None

    def __enter__(self):
        with session_scope() as session:
            self.initial_amount = session.query(self.model).count()

    def __exit__(self, exc_type, exc_val, exc_tb):
        with session_scope() as session:
            current_amount = session.query(self.model).count()
            expected_amount = self.delta + self.initial_amount
            assert current_amount == expected_amount, \
                f'Database number of records "{self.model}" error. Expected: {expected_amount}, got: {current_amount}'


class TestGarbageCollector(MortyTestCase):
    config = Config().config

    def test_run_once(self):
        gc = GarbageCollector()

        with session_scope() as session:
            create_supplier(session, 'supplier1')

            # gc should delete supplier
            with TrackDBRecord(model.Supplier, -1):
                gc.run_once()

        with session_scope() as session:
            component_id = str(uuid.uuid4())
            session.merge(model.Component(
                id=component_id,
            ))
            create_supplier(session, _id='supplier_with_component', component_id=component_id)

            # gc should not delete supplier
            with TrackDBRecord(model.Supplier, 0):
                gc.run_once()

        with session_scope() as session:
            create_supplier_item(session, _id='supplier_item1', created_at=time.time())

            # gc should not delete supplier item
            with TrackDBRecord(model.SupplierItem, 0):
                gc.run_once()

        with session_scope() as session:
            creation_time = time.time() - 2 * self.config.model.workers.gc.supplier_item_ttl
            create_supplier_item(session, _id='supplier_item1', supplier_id='supplier_with_component', created_at=creation_time)

            # gc should delete supplier item, since it has expired
            with TrackDBRecord(model.SupplierItem, -1):
                gc.run_once()

        with session_scope() as session:
            create_supplier(session, 'supplier1')
            for i in range(5):
                create_supplier_item(session, _id=f'supplier_item{i}', supplier_id='supplier1', created_at=time.time())

            # gc should delete supplier and its item
            with TrackDBRecord(model.Supplier, -1):
                with TrackDBRecord(model.morty__SupplierItem__suppliers, -5):
                    with TrackDBRecord(model.SupplierItem, -5):
                        gc.run_once()

        with session_scope() as session:
            create_supplier(session, 'supplier1')
            create_supplier_item(session, _id='supplier_item1', supplier_id='supplier1', created_at=time.time())
            assign_supplier_item(session, supplier_item_id='supplier_item1', supplier_id='supplier_with_component')

            # gc should delete supplier, connection to item,
            # but should not delete supplier item, since it is connected to `supplier_with_component`
            with TrackDBRecord(model.Supplier, -1):
                with TrackDBRecord(model.morty__SupplierItem__suppliers, -1):
                    with TrackDBRecord(model.SupplierItem, 0):
                        gc.run_once()

        with session_scope() as session:
            create_supplier(session, 'supplier2')
            create_supplier_item(session, _id='supplier_item2', supplier_id='supplier2', created_at=time.time())
            create_supplier_item(session, _id='supplier_item_without_supplier', created_at=time.time())

            # gc should delete only one item but should not delete `supplier_item_without_supplier`
            with TrackDBRecord(model.Supplier, -1):
                with TrackDBRecord(model.morty__SupplierItem__suppliers, -1):
                    with TrackDBRecord(model.SupplierItem, -1):
                        gc.run_once()

        with session_scope() as session:
            create_supplier(session, 'supplier3')
            create_supplier_item(session, _id='supplier_item3', supplier_id='supplier3', created_at=time.time())
            for i in range(5):
                create_supplier_item_endpoint(session, f'endpoint{i}', supplier_item_id='supplier_item3')

            # gc should delete all endpoints
            with TrackDBRecord(model.SupplierItemEndpoint, -5):
                gc.run_once()
