from search.martylib.db_utils import session_scope
from search.martylib.http.exceptions import BadRequest
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, owner_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components

WARDEN_CLIENT = Warden()


@TestCase.mock_auth(login='test-user')
def update_component(update_component_request):
    try:
        Warden().update_component(request=update_component_request, context=None)
    except Exception as e:
        raise e


class TestWardenUpdateComponent(BaseTestCase):

    @staticmethod
    def load_to_db():
        request = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_root_component__test_just_component',
                weight=0.1,
                abc_service_slug='mops',
                owner_list=[owner_pb2.Owner(login='test-user')],
                loading_dashboard_url='http://www.example1.com/index?search=src',
            )
        )

        request_1 = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_root_component__test_just_component_2',
                weight=0.1,
                abc_service_slug='akita',
                owner_list=[owner_pb2.Owner(login='test-user')],
                loading_dashboard_url='http://www.example2.com/index?search=src',
            )
        )

        root_component = component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_root_component',
                # Important part of test, because update handler verify if same abc_service_slug exist on root_component level
                abc_service_slug='test_root_component_abc',
                owner_list=[owner_pb2.Owner(login='test-user')],
                loading_dashboard_url='http://www.example3.com/index?search=src',
            )
        )

        create_components(root_component, request, request_1)

    @TestCase.mock_auth(login='test-user')
    def test_edition_component(self):
        """
        Test 5. Good. Just update simple component.
        """
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name='test_just_component',
                slug='test_root_component__test_just_component',
                parent_component_name='test_root_component',
                weight=0.4,
                human_readable_name='updated by test',
                owner_list=[owner_pb2.Owner(login='test-user')],
                loading_dashboard_url='http://www.example4.com/index?search=src',
            )
        )

        update_component(request)

        with session_scope() as session:
            component_model = (
                session.query(Component)
                .filter(
                    Component.name == 'test_just_component',
                    Component.parent_component_name == 'test_root_component',
                ).one_or_none()
            )

            component_proto = component_model.to_protobuf()

            self.assertEqual(component_proto.weight, 0.4)
            self.assertEqual(component_proto.abc_service_slug, '')
            self.assertEqual(component_proto.human_readable_name, 'updated by test')
            self.assertEqual(component_proto.loading_dashboard_url, 'http://www.example4.com/index?search=src')

    @TestCase.mock_auth(login='test-user')
    def test_edition_component_without_parent(self):
        """
        If parent_component_name is not set so cannot be updated
        """
        updated_component_slug = 'test_just_component'
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name=updated_component_slug,
                slug=updated_component_slug,
                weight=0.4,
                human_readable_name='updated by test',
                tier='B',
                owner_list=[owner_pb2.Owner(login='test-user')],
            )
        )

        response = WARDEN_CLIENT.update_component(request, context=None)
        if not response.error:
            raise BadRequest('Update request without parent_component_name was successful')

    @TestCase.mock_auth(login='test-user')
    def test_root_component_update(self):
        #  Check that root component with abc service would be successfully updated
        updated_component_slug = 'test_root_component'
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name=updated_component_slug,
                slug=updated_component_slug,
                weight=0.4,
                human_readable_name='updated by test',
                abc_service_slug='test_root_component_abc',
                tier='C',
                loading_dashboard_url='http://www.example5.com/index?search=src',
            )
        )

        response = WARDEN_CLIENT.update_component(request, context=None)
        if response.error:
            raise BadRequest(f'Update was not successful: {response.error}')

        with session_scope() as session:
            component = session.query(Component).filter(Component.slug == updated_component_slug).one()
            self.assertEqual(component.weight, 0.4)
            self.assertEqual(component.human_readable_name, 'updated by test')
            self.assertEqual(component.tier, 'C')
            self.assertEqual(component.abc_service_slug, 'test_root_component_abc')
            self.assertEqual(component.loading_dashboard_url, 'http://www.example5.com/index?search=src')

    @TestCase.mock_auth(login='test-user')
    def test_root_component_update_with_invalid_loading_dashboard_url(self):
        updated_component_slug = 'test_root_component'
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name=updated_component_slug,
                slug=updated_component_slug,
                weight=0.4,
                human_readable_name='updated by test',
                abc_service_slug='test_root_component_abc',
                tier='C',
                loading_dashboard_url='invalid_url',
            )
        )

        response = WARDEN_CLIENT.update_component(request, context=None)
        self.assertEqual(response.error, 'Invalid loading dashboard url')

    @TestCase.mock_auth(login='test-user')
    def test_root_component_update_with_empty_component_name(self):
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name='',
                slug='',
                weight=0.4,
                human_readable_name='updated by test',
                abc_service_slug='test_root_component_abc',
                tier='C',
                loading_dashboard_url='',
            )
        )

        response = WARDEN_CLIENT.update_component(request, context=None)
        self.assertEqual(response.error, 'Empty name')

    @TestCase.mock_auth(login='test-user')
    def test_root_component_update_with_invalid_component_name(self):
        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                name='test component',
                slug='test component',
                weight=0.4,
                human_readable_name='updated by test',
                abc_service_slug='test_root_component_abc',
                tier='C',
                loading_dashboard_url='',
            )
        )

        response = WARDEN_CLIENT.update_component(request, context=None)
        self.assertEqual(response.error, 'Spaces in component name are forbidden')
