from search.martylib.test_utils import TestCase
from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.mon.warden.proto.structures import component_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.sqla.warden.model import Component, Incident

WARDEN_CLIENT = Warden()


class TestGetComponentsPage(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            component = Component(
                name='test_component_1',
                description='test_description_1',
                abc_service_slug='test_abc_1',
                slug='test_component_1',
            )
            session.add(component)

            component = Component(
                name='test_component_2',
                description='test_description_2',
                abc_service_slug='test_abc_2',
                slug='test_component_2',
            )
            session.add(component)

            component = Component(
                name='test_component_3',
                description='test_description_3',
                abc_service_slug='test_abc_3',
                slug='test_component_3',
            )
            session.add(component)

            component = Component(
                name='test_component_4',
                description='test_description_4',
                abc_service_slug='test_abc_4',
                slug='test_component_4',
            )
            incident = Incident(
                key='test_incident',
                component=[component]
            )
            component.incidents = [incident]
            session.add(component)

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @TestCase.mock_auth(login='test-user')
    def test_get_components_page_case(self):
        result = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(
            root_only=False,
            horizontal=False,
            horizontal_component_name='',
            page=2,
            page_size=2,
            fields=['name', 'description']
        ), context=None)

        self.assertEqual(len(result.components), 2)
        self.assertEqual(result.total_objects, 4)
        self.assertEqual(result.components[0].name, 'test_component_3')
        self.assertEqual(result.components[0].description, 'test_description_3')
        self.assertEqual(result.components[0].abc_service_slug, '')
        self.assertEqual(result.components[1].name, 'test_component_4')
        self.assertEqual(result.components[1].description, 'test_description_4')
        self.assertEqual(result.components[1].abc_service_slug, '')

        result = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(
            root_only=False,
            horizontal=False,
            horizontal_component_name='',
            page=100,
            page_size=2,
            fields=['name']
        ), context=None)

        self.assertEqual(len(result.components), 0)
        self.assertEqual(result.total_objects, 4)

        result = WARDEN_CLIENT.get_component_list(component_pb2.GetComponentListRequest(
            root_only=False,
            horizontal=False,
            horizontal_component_name='',
            page=4,
            page_size=1,
            fields=['name', 'incidents']
        ), context=None)

        self.assertEqual(len(result.components), 1)
        self.assertEqual(result.total_objects, 4)
        self.assertEqual(result.components[0].name, 'test_component_4')
        self.assertEqual(result.components[0].description, '')
        self.assertEqual(result.components[0].abc_service_slug, '')
        self.assertEqual(len(result.components[0].incidents), 1)
        self.assertEqual(result.components[0].incidents[0].key, 'test_incident')
