# from search.martylib.db_utils import session_scope
# from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, owner_pb2
# from search.mon.warden.src.services.model import Warden
# from search.mon.warden.sqla.warden.model import Component

from search.mon.warden.tests.utils.creators import create_components
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
# from search.mon.warden.tests.utils.clients import Clients


class TestCreateGoal(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='test_create_goal',
                    abc_service_slug='test_create_goal',
                    owner_list=[owner_pb2.Owner(login='test-user')],
                ),
            ),
        )

    # @TestCase.mock_auth(login='test-user', roles=['warden/admin'])
    # def test_create_goal(self):
    #     client = Warden()
    #     client.goals_reducer.clients = Clients()
    #     goal = client.create_or_update_goal(goals_pb2.GoalCreationRequest(component_name='test_create_goal', tier='C'), context=None)
    #     self.assertEqual(goal.id, 'GOALSVAULTIV-0')
    #
    #     with session_scope() as session:
    #         component = session.query(Component).filter(Component.name == 'test_create_goal').one()
    #         self.assertEqual(component.goal_url, 'https://goals.yandex-team.ru/filter?goal=0')
