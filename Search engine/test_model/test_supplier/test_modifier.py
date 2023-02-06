import ujson
import uuid

import yatest.common
from search.martylib.db_utils import session_scope, to_model

from search.morty.proto.structures import component_pb2
from search.morty.sqla.morty import model

from search.morty.src.model.supplier.modifier import SupplierModifier, NannyReleaseRule
from search.morty.tests.utils.test_case import MortyTestCase


data_path = yatest.common.source_path('search/morty/tests/test_data/test_nanny/dashboards')


class TestSupplierModifier(MortyTestCase):
    @staticmethod
    def component_suppliers_count(session, component_id):
        return (
            session.query(model.morty__Supplier__components)
            .filter(model.morty__Supplier__components.c.morty__Component_id == component_id)
            .count()
        )

    def test_nanny_supplier_id(self):
        rule = NannyReleaseRule('TEST1', 'test2', 'stable')
        assert SupplierModifier.nanny_supplier_id(rule) == 'nanny-test1-test2-stable'

    def test_select_components(self):
        with session_scope() as session:
            session.merge(to_model(component_pb2.Component()))

        modifier = SupplierModifier()
        assert len(modifier.select_components()) == 1

    def test_release_rule_generator(self):
        with open(f'{data_path}/morty_dashboard_services.json') as fd:
            data = ujson.load(fd)
        assert data['services']['morty-prod']['info_attrs']['content']['tickets_integration']['service_release_rules'] == list(SupplierModifier.release_rule_generator(data, {"morty-prod"}))

    def test_process(self):
        component = component_pb2.Component(id=str(uuid.uuid4()))
        modifier = SupplierModifier()

        with open(f'{data_path}/morty_dashboard.json') as fd:
            dashboard_id = 'morty'
            modifier.clients.nanny.load_dashboard(ujson.load(fd))

        with open(f'{data_path}/morty-prod_recipe.json') as fd:
            morty_recipe_id = 'morty-prod'
            modifier.clients.nanny.load_recipe(ujson.load(fd))

        with open(f'{data_path}/sawmill-dev_recipe.json') as fd:
            sawmill_recipe_id = 'sawmill-dev'
            modifier.clients.nanny.load_recipe(ujson.load(fd))

        with open(f'{data_path}/morty_dashboard_services.json') as fd:
            modifier.clients.nanny.load_dashboard_services(ujson.load(fd), dashboard_id)

        with session_scope() as session:
            session.merge(to_model(component))

        # test empty component
        with session_scope() as session:
            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 0

        # test not nanny flow
        with session_scope() as session:
            component.flows.objects.extend((component_pb2.Flow(id='default'), ))
            session.merge(to_model(component))
            session.commit()

            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 0

        # test one flow with 4 supplier - for each release_type
        with session_scope() as session:
            component.flows.objects[0].nanny.dashboard = dashboard_id
            component.flows.objects[0].nanny.recipe = morty_recipe_id
            session.merge(to_model(component))
            session.commit()

            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 4

            supplier = session.query(model.Supplier).first().to_protobuf()
            assert supplier.nanny.sandbox_task_type == 'BUILD_MORTY'
            assert supplier.nanny.sandbox_resource_type == 'MORTY_BINARY'
            assert supplier.nanny.sandbox_release_type in ["stable", "prestable", "testing", "unstable"]

        # test add new flow & test flow with many suppliers
        with session_scope() as session:
            component.flows.objects.extend((component_pb2.Flow(nanny=component_pb2.NannyFlow(dashboard=dashboard_id, recipe=sawmill_recipe_id)), ))
            session.merge(to_model(component))
            session.commit()

            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 12

        # test remove flow -> remove suppliers
        with session_scope() as session:
            component.flows.objects.pop()
            session.merge(to_model(component))
            session.commit()

            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 4

            component.flows.objects.pop()
            session.merge(to_model(component))
            session.commit()

            modifier.run_once()
            assert self.component_suppliers_count(session, component.id) == 0
