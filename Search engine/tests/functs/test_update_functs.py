from search.martylib.test_utils import TestCase
from search.martylib.db_utils import session_scope
from search.mon.warden.proto.structures import functionality_pb2
from search.mon.warden.sqla.warden.model import Component
from search.mon.warden.src.services.model import Warden
from search.martylib.http.exceptions import BadRequest, NotAuthorized

from search.mon.warden.proto.structures import component_pb2, duty_pb2, owner_pb2
from search.mon.warden.tests.utils.base_test_case import BaseTestCase
from search.mon.warden.tests.utils.creators import create_components, create_functionalities

WARDEN_CLIENT = Warden()


@TestCase.mock_auth(login='test-user')
def update_funct(update_funct_request):
    try:
        Warden().update_functionality(request=update_funct_request, context=None)
    except Exception as e:
        raise e


class TestWardenUpdateFunctionality(BaseTestCase):

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='update_funct_test_1',
                    weight=0.1,
                    parent_component_name='',
                    abc_service_slug='husky',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            )
        )

        create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test',
                    description='test funct',
                    weight=0.2,
                    ),
                component_name='update_funct_test_1'
                ),
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test_2',
                    description='test funct',
                    weight=0.2,
                ),
                component_name='update_funct_test_1'
            ),
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test_3',
                    description='test funct',
                    weight=0.2,
                ),
                component_name='update_funct_test_1'
            ),
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test_4',
                    description='test funct',
                    weight=0.2,
                    slug='update_funct_test_1_test4',
                    id='38277945-c399-4e83-8de5-7622fa4d1942'
                ),
                component_name='update_funct_test_1'
            )
        )

    @TestCase.mock_auth(login='test-user')
    def test_update_simple_funct(self):

        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'update_funct_test_1').one_or_none()

            funct_id = str(component_model.functionality_list[0].id)

            request = functionality_pb2.UpdateFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    id=funct_id,
                    name='test_update_simple_funct_name',
                    description='updated test funct',
                    weight=0.5
                )
            )

            update_funct(request)

            updated = Warden().get_functionality(
                functionality_pb2.GetFunctionalityRequest(
                    functionality_id=funct_id
                ), context=None
            ).functionality

            self.assertEqual(updated.weight, 0.5)

    @TestCase.mock_auth(login='test-user')
    def test_update_empty_funct(self):
        request = functionality_pb2.UpdateFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                description='no id'
            )
        )
        try:
            update_funct(request)
        except BadRequest:
            pass
        else:
            raise BadRequest("empty funct update was successful")

    @TestCase.mock_auth(login='test-user-2')
    def test_update_functionality_no_auth(self):
        try:
            Warden().update_functionality(
                request=functionality_pb2.UpdateFunctionalityRequest(
                    functionality=functionality_pb2.Functionality(
                        id='38277945-c399-4e83-8de5-7622fa4d1942',
                        name='test_update_functionality_no_auth_name',
                        description='update_no_auth',
                    ),
                ),
                context=None,
            )
        except NotAuthorized:
            pass
        else:
            self.assertFalse('ACL test failed, functionality has been updated')

    @TestCase.mock_auth(login='test-user')
    def test_update_funct_set_incorrect_slug(self):
        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'update_funct_test_1').one_or_none()

            funct_id = str(component_model.functionality_list[0].id)

            request = functionality_pb2.UpdateFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    id=funct_id,
                    description='updated test funct',
                    slug='test'
                )
            )

            try:
                update_funct(request)
            except BadRequest:
                pass
            else:
                raise BadRequest("Incorrect slug set when funct update was successful")

    @TestCase.mock_auth(login='test-user')
    def test_update_funct_set_correct_slug(self):
        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'update_funct_test_1').one_or_none()

            funct_id = str(component_model.functionality_list[0].id)

            request = functionality_pb2.UpdateFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    id=funct_id,
                    name='test_update_funct_set_correct_slug_name',
                    description='updated test funct',
                    slug='update_funct_test_1_test'
                )
            )

            update_funct(request)

            updated = Warden().get_functionality(
                functionality_pb2.GetFunctionalityRequest(
                    functionality_id=funct_id
                ), context=None
            ).functionality

            self.assertEqual(updated.slug, 'update_funct_test_1_test')

    @TestCase.mock_auth(login='test-user')
    def test_update_simple_funct_with_slug(self):

        funct_id = '38277945-c399-4e83-8de5-7622fa4d1942'

        request = functionality_pb2.UpdateFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                id=funct_id,
                name='test_update_simple_funct_with_slug_name',
                description='updated test funct',
                weight=0.5,
                slug='update_funct_test_1_test4'
            )
        )

        update_funct(request)

        updated = Warden().get_functionality(
            functionality_pb2.GetFunctionalityRequest(
                functionality_id=funct_id
            ), context=None
        ).functionality

        self.assertEqual(updated.weight, 0.5)

    @TestCase.mock_auth(login='test-user')
    def test_functionality_duty_update(self):
        functionality_ids = create_functionalities(
            functionality_pb2.AddFunctionalityRequest(
                functionality=functionality_pb2.Functionality(
                    name='test_5',
                    description='test funct',
                    weight=0.2,
                ),
                component_name='update_funct_test_1'
            )
        )

        request = functionality_pb2.UpdateFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                id=functionality_ids[0],
                name='test_functionality_duty_update_name',
                description='updated test funct',
                weight=0.5,
                duty=[duty_pb2.DutyRecord(duty_rule=duty_pb2.DutyRule(abc=duty_pb2.AbcDutyRule(abc_service='workplace')))],
            )
        )

        update_funct(request)
        updated_functionality = WARDEN_CLIENT.get_functionality(
            functionality_pb2.GetFunctionalityRequest(functionality_id=functionality_ids[0]),
            context=None,
        ).functionality
        for duty in updated_functionality.duty:
            self.assertEqual(duty.duty_rule.abc.abc_service, 'workplace')
