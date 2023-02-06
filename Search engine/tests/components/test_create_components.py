from search.martylib.db_utils import session_scope
from search.martylib.http.exceptions import BadRequest
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, owner_pb2, user_pb2
from search.mon.warden.proto.structures.component import zbp_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components

WARDEN_CLIENT = Warden()


class TestWardenCreateComponent(BaseTestCase):

    @TestCase.mock_auth(login='test-user')
    def test_creation_component(self):
        """
        Test 1. Good. Just creating simple component
        """
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_just_component',
                weight=0.1,
                human_readable_name='Рабочее место дежурного',
                loading_dashboard_url='http://www.example.com/index?search=src',
                abc_service_slug='workplace',
                owner_list=[owner_pb2.Owner(login='harond1'), owner_pb2.Owner(login='talion')],
                curator_list=[user_pb2.User(login='curator1'), user_pb2.User(login='curator2')],
                zbp_settings_list=[zbp_pb2.ZBPSettings()],
            )
        )

        request_mail = component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='mail', abc_service_slug='mail'))
        request_disk = component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='disk', abc_service_slug='disk_root'))

        create_components(request, request_mail, request_disk)
        request_2 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_create_component_2',
                abc_service_slug='test_create_component_2',
                zbp_settings_list=[zbp_pb2.ZBPSettings()],
            )
        )
        create_components(request_2)

        with session_scope() as session:
            component_model = session.query(Component).filter(Component.name == 'test_just_component').one_or_none()
            component_proto = component_model.to_protobuf()
            self.assertEqual(component_proto.weight, 0.1)
            self.assertEqual(component_proto.abc_service_slug, 'workplace')
            self.assertEqual(component_proto.human_readable_name, 'Рабочее место дежурного')
            self.assertEqual(component_proto.loading_dashboard_url, 'http://www.example.com/index?search=src')
            self.assertEqual(set(curator.login for curator in component_proto.curator_list), {'curator1', 'curator2'})

    @TestCase.mock_auth(login='test-user')
    def test_creation_names_duplicate(self):
        """
        Test 2. Good. Different parent_component_name
        """
        request_1 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(slug='mail__test_different_name_component', abc_service_slug='calendar'),
        )

        request_2 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(slug='disk__test_different_name_component', abc_service_slug='disk'),
        )

        create_components(request_1, request_2)

        """
        Test 3. Must fail. The same parent_component_name. Raises BadRequest if test didnt fail.
        """

        request_1 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='mail__test_same_name_component',
                abc_service_slug='messenger',
            )
        )

        request_2 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='mail__test_same_name_component',
                abc_service_slug='apphost',
            )
        )

        WARDEN_CLIENT.create_component(request_1, context=None)

        # Create component with same Primary Key
        response = WARDEN_CLIENT.create_component(request_2, context=None)
        if not response.error:
            raise BadRequest('created components with the same abc slugs')

    @TestCase.mock_auth(login='test-user')
    def test_creation_abc_duplicate(self):
        """
        Test 4. Good. Different abc slugs. And then try to create request_2 one more time and fail.
        If second attempt didnt fail then raises BadRequest
        """
        request_1 = component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_different_abc_component_1', abc_service_slug='telemost'))
        request_2 = component_pb2.CreateComponentRequest(component=component_pb2.Component(slug='test_different_abc_component_2', abc_service_slug='sepepersonal'))

        with TestCase.mock_auth(login='test-user', roles=['warden/admin']):
            for new_component in [request_1, request_2]:
                response = WARDEN_CLIENT.create_component(new_component, context=None)
                if response.error:
                    raise BadRequest(f'Error with component "{new_component.component.name}" creation: {response.error}')

        # Create same component again
        response = WARDEN_CLIENT.create_component(request_2, context=None)
        if not response.error:
            raise BadRequest('created components with the same abc slugs')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_creation_component_with_invalid_loading_dashboard_url(self):
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_different_abc_component_1',
                abc_service_slug='abs',
                loading_dashboard_url='invalid_url'
            )
        )

        # Create same component again
        response = WARDEN_CLIENT.create_component(request, context=None)
        self.assertEqual(response.error, 'Invalid loading dashboard url')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_creation_component_with_empty_component_name(self):
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='',
                abc_service_slug='abs',
                loading_dashboard_url=''
            )
        )

        # Create same component again
        response = WARDEN_CLIENT.create_component(request, context=None)
        self.assertEqual(response.error, 'Slug or component name must be specified')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_creation_component_with_spaces_in_component_name(self):
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test component',
                abc_service_slug='abs',
                loading_dashboard_url=''
            )
        )

        # Create same component again
        response = WARDEN_CLIENT.create_component(request, context=None)
        self.assertEqual(response.error, 'Spaces in component name are forbidden')

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_create_child_component(self):
        WARDEN_CLIENT.create_component(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(slug='test_child_component_creation', abc_service_slug='test_child_component_creation')
            ),
            context=None,
        )

        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_child_component_creation__test_component',
                tags=[component_pb2.ComponentTag(tag='tag1'), component_pb2.ComponentTag(tag='tag2')],
            )
        )

        WARDEN_CLIENT.create_component(request, context=None)
        with session_scope() as session:
            component = (
                session.query(Component)
                .filter(Component.name == 'test_component', Component.parent_component_name == 'test_child_component_creation')
                .one()
            )
            self.assertEqual(len(component.tags), 2)
            tags = [t.tag for t in component.tags]
            self.assertIn('tag1', tags)
            self.assertIn('tag2', tags)

    @TestCase.mock_auth(login='test-user', roles=['warden/component_editor'])
    def test_component_editor_creating_component(self):
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_component_editor_creating_component',
                weight=0.1,
                human_readable_name='Рабочее место дежурного',
                loading_dashboard_url='http://www.example.com/index?search=src',
                abc_service_slug='workplace3',
                owner_list=[owner_pb2.Owner(login='harond1'), owner_pb2.Owner(login='talion')],
                curator_list=[user_pb2.User(login='curator1'), user_pb2.User(login='curator2')],
                zbp_settings_list=[zbp_pb2.ZBPSettings()],
            )
        )

        response = Warden().create_component(request, context=None)

        self.assertEqual(response.error, '')

        with session_scope() as session:
            component_model = session.query(Component).filter(Component.name == 'test_component_editor_creating_component').one_or_none()
            component_proto = component_model.to_protobuf()
            self.assertEqual(component_proto.weight, 0.1)
            self.assertEqual(component_proto.abc_service_slug, 'workplace3')
            self.assertEqual(component_proto.human_readable_name, 'Рабочее место дежурного')
            self.assertEqual(component_proto.loading_dashboard_url, 'http://www.example.com/index?search=src')
            self.assertEqual(set(curator.login for curator in component_proto.curator_list), {'curator1', 'curator2'})

    @TestCase.mock_auth(login='test-user', roles=['warden/incident_manager'])
    def test_wrong_role_creating_component(self):
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_incident_manager_creating_component',
                weight=0.1,
                human_readable_name='Рабочее место дежурного',
                loading_dashboard_url='http://www.example.com/index?search=src',
                abc_service_slug='workplace4',
                owner_list=[owner_pb2.Owner(login='harond1'), owner_pb2.Owner(login='talion')],
                curator_list=[user_pb2.User(login='curator1'), user_pb2.User(login='curator2')],
                zbp_settings_list=[zbp_pb2.ZBPSettings()],
            )
        )

        response = Warden().create_component(request, context=None)

        self.assertEqual(response.error, 'Role `warden/admin` or `warden/component_editor` is required')
