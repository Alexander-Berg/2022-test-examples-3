import datetime
import ujson
import uuid

import yatest.common
from search.martylib.db_utils import session_scope, to_model
from search.martylib.core.date_utils import now

from search.morty.proto.structures import component_pb2, supplier_pb2
from search.morty.sqla.morty import model

from search.morty.src.model.supplier.spotter import SupplierSpotter
from search.morty.tests.utils.test_case import MortyTestCase


data_path = yatest.common.source_path('search/morty/tests/test_data/test_nanny/releases')


class TestSupplierSpotter(MortyTestCase):
    def test_run_once(self):
        spotter = SupplierSpotter()
        supplier = supplier_pb2.Supplier(
            id='nanny-test',
            components=[
                component_pb2.Component(
                    id=str(uuid.uuid4()),
                    flows=component_pb2.FlowList(
                        objects=[
                            component_pb2.Flow()
                        ],
                    ),
                ),
                component_pb2.Component(
                    id=str(uuid.uuid4()),
                    flows=component_pb2.FlowList(
                        objects=[
                            component_pb2.Flow()
                        ],
                    ),
                )
            ]
        )

        with open(f'{data_path}/begemot.json') as fd:
            release = ujson.load(fd)
            sandbox_task = release['spec']['sandboxRelease']['taskType']
            sandbox_resource = release['spec']['sandboxRelease']['taskId']
            sandbox_release_type = release['spec']['sandboxRelease']['releaseType']

            spotter.clients.nanny.load_release(release)

        # test empty supplier
        with session_scope() as session:
            session.merge(to_model(supplier))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 0
            assert session.query(model.SupplierItemEndpoint).count() == 0

        with session_scope() as session:
            supplier.nanny.sandbox_task_type = sandbox_task
            supplier.nanny.sandbox_resource_type = sandbox_resource
            session.merge(to_model(supplier))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 0
            assert session.query(model.SupplierItemEndpoint).count() == 0

        release['meta']['creationTime'] = now().replace(microsecond=0).isoformat()
        spotter.clients.nanny.load_release(release)

        # test releaseType
        with session_scope() as session:
            supplier.nanny.sandbox_release_type = 'not_exist'
            session.merge(to_model(supplier))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 0
            assert session.query(model.SupplierItemEndpoint).count() == 0

        # test releaseType
        with session_scope() as session:
            supplier.nanny.sandbox_release_type = sandbox_release_type
            session.merge(to_model(supplier))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 0
            assert session.query(model.SupplierItemEndpoint).count() == 0

        release['meta']['creationTime'] = (now().replace(microsecond=0) - datetime.timedelta(minutes=6)).isoformat()
        spotter.clients.nanny.load_release(release)

        # test lte
        with session_scope() as session:
            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 1
            assert session.query(model.SupplierItemEndpoint).count() == 2

            item_reqid = str(session.query(model.SupplierItem.reqid).first()[0])
            assert item_reqid

            spotter.run_once()
            assert session.query(model.SupplierItem).count() == 1
            assert session.query(model.SupplierItemEndpoint).count() == 2

            # test not update existing supplier item
            assert str(session.query(model.SupplierItem.reqid).first()[0]) == item_reqid

        # test 0d
        with session_scope() as session:
            spotter.run_once()
            assert session.query(model.SupplierItem).count() == 1
            assert session.query(model.SupplierItemEndpoint).count() == 2

        # test different suppliers with same content
        supplier2 = supplier_pb2.Supplier(
            id='nanny-test-2',
            components=[
                component_pb2.Component(
                    id=str(uuid.uuid4()),
                    flows=component_pb2.FlowList(
                        objects=[
                            component_pb2.Flow()
                        ],
                    ),
                ),
            ],
            nanny=supplier_pb2.NannySupplier(
                sandbox_task_type=sandbox_task,
                sandbox_resource_type=sandbox_resource,
                sandbox_release_type=sandbox_release_type,
            )
        )

        with session_scope() as session:
            session.merge(to_model(supplier2))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 1
            assert session.query(model.SupplierItemEndpoint).count() == 3
            assert len(session.query(model.morty__SupplierItem__suppliers).filter(model.morty__SupplierItem__suppliers.c.morty__Supplier_id == supplier2.id).all()) == 1

        # test different suppliers with same content for one component
        supplier3 = supplier_pb2.Supplier(
            id='nanny-test-3',
            components=supplier2.components,
            nanny=supplier_pb2.NannySupplier(
                sandbox_task_type=sandbox_task,
                sandbox_resource_type=sandbox_resource,
                sandbox_release_type=sandbox_release_type,
            )
        )

        with session_scope() as session:
            session.merge(to_model(supplier3))
            session.commit()

            spotter.run_once()
            assert len(session.query(model.SupplierItem).all()) == 1
            assert session.query(model.SupplierItemEndpoint).count() == 3
            assert len(session.query(model.morty__SupplierItem__suppliers).filter(model.morty__SupplierItem__suppliers.c.morty__Supplier_id == supplier3.id).all()) == 1

    def test_create_endpoints(self):
        component = component_pb2.Component(
            id='component',
            flows=component_pb2.FlowList(
                objects=[
                    component_pb2.Flow(
                        id='flow',
                        auto_release=component_pb2.AutoRelease(
                            union_type=component_pb2.AutoRelease.UnionType.title,
                        ),
                    ),
                ],
            ),
        )

        item = supplier_pb2.SupplierItem(
            title='test_item-40',
        )

        assert SupplierSpotter.nanny_supplier_item_version(component, component.flows.objects[0], item) == f'{component.id}-{component.flows.objects[0].id}-{item.title}'
        component.flows.objects[0].auto_release.union_key = r"re.search('.*-(\d+)', item.title).group(1)"
        assert SupplierSpotter.nanny_supplier_item_version(component, component.flows.objects[0], item) == f'{component.id}-{component.flows.objects[0].id}-{40}'
