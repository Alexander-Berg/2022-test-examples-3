from search.martylib.db_utils import session_scope
from search.martylib.test_utils import TestCase
from search.martylib.http.exceptions import NotAuthorized

from search.mon.warden.proto.structures.component import zbp_pb2, common_pb2
from search.mon.warden.sqla.warden import model
from search.mon.warden.src.services.component import ComponentApiService
from search.mon.warden.tests.utils.base_test_case import BaseTestCase


component_api_service_client = ComponentApiService()


class TestCreateGoal(BaseTestCase):
    @staticmethod
    def load_to_db():
        with session_scope() as session:
            session.add(model.Component(name='component'))

    @TestCase.mock_auth(login='test-user', roles=['warden/zbp'])
    def test_update_and_get_zbp_settings(self):
        component_api_service_client.update_zbp_settings(
            zbp_pb2.UpdateZBPSettingsRequest(
                component=common_pb2.ComponentFilter(component_name='component', parent_component_name=''),
                settings=zbp_pb2.ZBPSettings(goal_id=1, goal_responsible='user', review_start='2022-02-24')
            ),
            context=None,
        )

        zbp_settings = component_api_service_client.get_zbp_settings(
            zbp_pb2.ZBPSettingsFilter(
                component=common_pb2.ComponentFilter(component_name='component', parent_component_name=''),
                review_start='2022-02-24',
            ), context=None
        )

        self.assertEqual(zbp_settings.goal_id, 1)
        self.assertEqual(zbp_settings.goal_responsible, 'user')

    @TestCase.mock_auth(login='test-user', roles=['warden/incident_manager'])
    def test_wrong_role_updating_zbp_settings(self):
        try:
            component_api_service_client.update_zbp_settings(
                zbp_pb2.UpdateZBPSettingsRequest(component=common_pb2.ComponentFilter(component_name='component', parent_component_name='')), zbp_pb2.ZBPSettings(goal_id=1, goal_responsible='user')
            )
        except NotAuthorized:
            pass
        else:
            self.assertFalse('Wrong role updated zbp settings')
