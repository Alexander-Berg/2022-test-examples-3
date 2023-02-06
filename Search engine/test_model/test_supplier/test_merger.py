import uuid

from search.martylib.db_utils import session_scope, to_model, generate_field_name as F

from search.morty.proto.structures import component_pb2, supplier_pb2
from search.morty.sqla.morty import model

from search.morty.src.model.supplier.merger import SupplierMerger
from search.morty.tests.utils.test_case import MortyTestCase


class TestSupplierMerger(MortyTestCase):
    def test_run_once(self):
        creator = SupplierMerger()

        component = component_pb2.Component(
            id=str(uuid.uuid4()),
            flows=component_pb2.FlowList(
                objects=[
                    component_pb2.Flow(
                        id='no_auto',
                    ),
                    component_pb2.Flow(
                        id='auto',
                        auto_release=component_pb2.AutoRelease(
                            enabled=True,
                        )
                    ),
                    component_pb2.Flow(
                        id='filter',
                        auto_release=component_pb2.AutoRelease(
                            enabled=True,
                            parts=[
                                component_pb2.AutoRelease.ReleaseFilter(
                                    title_regexp=[
                                        "^valid$",
                                    ],
                                ),
                            ]
                        ),
                    ),
                    component_pb2.Flow(
                        id='parts',
                        auto_release=component_pb2.AutoRelease(
                            enabled=True,
                            union_type=component_pb2.AutoRelease.UnionType.title,
                            parts=[
                                component_pb2.AutoRelease.ReleaseFilter(
                                    title_regexp=[
                                        "^valid_1$",
                                    ],
                                ),
                                component_pb2.AutoRelease.ReleaseFilter(
                                    title_regexp=[
                                        "^valid_2$",
                                    ],
                                ),
                            ]
                        ),
                    ),
                    component_pb2.Flow(
                        id='filter2',
                        auto_release=component_pb2.AutoRelease(
                            enabled=True,
                            parts=[
                                component_pb2.AutoRelease.ReleaseFilter(
                                    title_regexp=[
                                        "^valid$",
                                    ],
                                    sandbox_release_type=[
                                        "stable",
                                    ],
                                ),
                            ]
                        ),
                    ),
                ],
            )
        )
        with session_scope() as session:
            session.merge(to_model(component))

        # test no endpoints - nothing
        with session_scope() as session:
            creator.run_once()
            assert session.query(model.Event).count() == 0

        # test endpoints, no auto release - nothing
        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test1',
            supplier_item=supplier_pb2.SupplierItem(
                id='test1',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test1'
                ),
            ),
            flow='no_auto',
            version='1',
        )
        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component), ))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 0

        # test endpoints, auto release, no parts - create event for endpoint
        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test2',
            supplier_item=supplier_pb2.SupplierItem(
                id='test2',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test2'
                ),
            ),
            flow='auto',
            version='2',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 1

        # test endpoints, auto release, parts, no union - create event if valid
        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test3',
            supplier_item=supplier_pb2.SupplierItem(
                id='test3',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test3',
                ),
                title='not_valid',
            ),
            flow='filter',
            version='3',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 1

        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test4',
            supplier_item=supplier_pb2.SupplierItem(
                id='test4',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test4',
                ),
                title='valid',
            ),
            flow='filter',
            version='4',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 2

        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test5',
            supplier_item=supplier_pb2.SupplierItem(
                id='test5',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test5',
                ),
                title='valid_1',
            ),
            flow='parts',
            version='5',
        )

        # test parts, but 1 / 2
        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 2

        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test7',
            supplier_item=supplier_pb2.SupplierItem(
                id='test7',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test7',
                ),
                title='valid_2',
            ),
            flow='parts',
            version='5',
        )

        # test parts, but 2 / 2
        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 3

        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test6',
            supplier_item=supplier_pb2.SupplierItem(
                id='test6',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test6',
                ),
                title='valid_1',
            ),
            flow='parts',
            version='5',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 3

        # test difficult release filter - not matched
        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test7',
            supplier_item=supplier_pb2.SupplierItem(
                id='test7',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test7',
                    sandbox_release_type='prestable',
                ),
                title='valid',
            ),
            flow='filter2',
            version='7',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 3

        # test difficult release filter - matched
        endpoint = supplier_pb2.SupplierItemEndpoint(
            id='test8',
            supplier_item=supplier_pb2.SupplierItem(
                id='test8',
                nanny=supplier_pb2.NannySupplierItem(
                    id='test8',
                    sandbox_release_type='stable',
                ),
                title='valid',
            ),
            flow='filter2',
            version='8',
        )

        with session_scope() as session:
            m = to_model(endpoint, exclude=(F(model.SupplierItemEndpoint.component),))
            m.morty__Component_id = component.id
            session.merge(m)
            creator.run_once()
            assert session.query(model.Event).count() == 4
