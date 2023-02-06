from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import owner_pb2, component_pb2
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components

WARDEN_CLIENT = Warden()


def update_component(update_component_request):
    try:
        return Warden().update_component(request=update_component_request, context=None).error
    except Exception as e:
        raise e


class TestComponentOwnerList(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_update_owner_list',
                    description='description',
                    abc_service_slug='test_update_owner_list_abc_slug',
                    owner_list=[
                        owner_pb2.Owner(
                            abc_service_slug='apphost',
                            abc_role_scope='devops',
                        ),
                        owner_pb2.Owner(login='ziyatov'),
                    ]
                )
            )
        )

    @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    def test_component_owner_list_case(self):
        component_pb2.CreateComponentRequest(
            component=component_pb2.Component(
                slug='test_update_owner_list',
                description='description',
                owner_list=[
                    owner_pb2.Owner(
                        abc_service_slug='apphost',
                        abc_role_scope='devops',
                    ),
                    owner_pb2.Owner(
                        login='ziyatov',
                    ),
                ]
            )
        )

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(name='test_update_owner_list'), context=None).component
        self.assertEqual(len(component.owner_list), 2)
        self.assertEqual(component.owner_list[0].abc_service_slug, 'apphost')
        self.assertEqual(component.owner_list[0].abc_role_scope, 'devops')
        self.assertEqual(component.owner_list[1].login, 'ziyatov')

        request = component_pb2.UpdateComponentRequest(
            component=component_pb2.Component(
                slug='test_update_owner_list',
                name='test_update_owner_list',
                abc_service_slug='apphost',
                owner_list=[
                    owner_pb2.Owner(
                        abc_service_slug='apphost',
                        abc_role_scope='devops',
                    ),
                    owner_pb2.Owner(
                        login='ziyatov',
                    ),
                    owner_pb2.Owner(
                        abc_service_slug='toloka',
                        abc_role_scope='devops',
                    ),
                    owner_pb2.Owner(
                        login='lebedev-aa',
                    ),
                ]
            )
        )

        self.assertEqual(update_component(request), '')

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(name='test_update_owner_list'), context=None).component
        self.assertEqual(len(component.owner_list), 4)
        self.assertEqual(component.owner_list[0].abc_service_slug, 'apphost')
        self.assertEqual(component.owner_list[0].abc_role_scope, 'devops')
        self.assertEqual(component.owner_list[1].login, 'ziyatov')
        self.assertEqual(component.owner_list[2].abc_service_slug, 'toloka')
        self.assertEqual(component.owner_list[3].login, 'lebedev-aa')
