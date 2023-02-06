from search.martylib.db_utils import clear_db, prepare_db, session_scope
from search.martylib.http.exceptions import BadRequest
from search.martylib.http.exceptions import NotAuthorized
from search.martylib.test_utils import TestCase

from search.mon.warden.proto.structures import component_pb2, owner_pb2
from search.mon.warden.proto.structures import functionality_pb2
from search.mon.warden.sqla.warden.model import Functionality, Component
from search.mon.warden.src.services.model import Warden
from search.mon.warden.tests.utils.creators import create_components


def create_functs(*args):
    for funct in args:
        try:
            Warden().add_functionality(request=funct, context=None)
        except Exception as e:
            raise e


class TestWardenCreateFunctionality(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()
        with session_scope() as session:
            session.execute('alter table "warden__Functionality" ALTER COLUMN  "slug" set DEFAULT NULL;')
        cls.load_to_db()

    @classmethod
    def tearDownClass(cls):
        clear_db()

    @staticmethod
    def load_to_db():
        create_components(
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_1',
                    weight=0.1,
                    parent_component_name='',
                    abc_service_slug='hound',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_2',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='xiva',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_3',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='xeno',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_4',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='wabbajack',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_5',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='search-wizard',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_6',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='report',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            ),
            component_pb2.CreateComponentRequest(
                component=component_pb2.Component(
                    slug='create_funct_test_7',
                    weight=0.5,
                    parent_component_name='',
                    abc_service_slug='video',
                    functionality_list=[],
                    owner_list=[owner_pb2.Owner(login='test-user')],
                )
            )
        )

    @TestCase.mock_auth(login='test-user')
    def test_creation_functs(self):
        request = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test',
                description='test funct',
                weight=0.2,
            ),
            component_name='create_funct_test_1'
        )

        create_functs(request)

        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'create_funct_test_1').one_or_none()

            if not component_model:
                raise BadRequest("cant create functionality")

            funct_id = component_model.functionality_list[0].id
            functionality = session.query(Functionality).filter(
                Functionality.id == funct_id).one_or_none()

            if not functionality:
                raise BadRequest("cant create functionality")

    @TestCase.mock_auth(login='test-user')
    def test_creation_functs_to_child(self):
        request = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test',
                description='test funct',
                weight=0.2,
            ),
            component_name='create_funct_test_2'
        )
        try:
            create_functs(request)
        except BadRequest:
            pass

    @TestCase.mock_auth(login='test-user')
    def test_creation_duplicate_functs(self):
        request_1 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test',
                description='test funct',
                weight=0.2,
            ),
            component_name='create_funct_test_3'
        )

        request_2 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test',
                description='test funct 2',
                weight=0.2,
            ),
            component_name='create_funct_test_3'
        )

        try:
            create_functs(request_1, request_2)
        except BadRequest:
            pass
        else:
            raise BadRequest('created functs with the same names')

    @TestCase.mock_auth(login='test-user')
    def test_creation_no_slug(self):
        request_1 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test-4-1',
                description='test funct',
                weight=0.2,
            ),
            component_name='create_funct_test_4'
        )

        request_2 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test-4-2',
                description='test funct 2',
                weight=0.2,
            ),
            component_name='create_funct_test_4'
        )

        try:
            create_functs(request_1, request_2)
        except BadRequest:
            pass

    @TestCase.mock_auth(login='test-user')
    def test_creation_bad_slug(self):
        request_1 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                id='38277945-c399-4e83-8de5-7622fa4d1940',
                name='test',
                description='test funct',
                weight=0.2,
                slug='test'
            ),
            component_name='create_funct_test_4'
        )

        try:
            # create_functs(request_1)
            result = Warden().add_functionality(request=request_1, context=None)
            print(result)
        except BadRequest:
            pass
        else:
            created_funct = Warden().get_functionality(
                functionality_pb2.GetFunctionalityRequest(functionality_id=request_1.functionality.id),
                context=None
            )
            raise BadRequest(
                f'created funct {created_funct.functionality.id} with invalid slug "{created_funct.functionality.slug}"'
            )

    @TestCase.mock_auth(login='test-user')
    def test_creation_same_slug(self):
        request_1 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test',
                description='test funct',
                weight=0.2,
                slug='create_funct_test_5_slug'
            ),
            component_name='create_funct_test_5'
        )

        request_2 = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test_2',
                description='test funct 2',
                weight=0.2,
                slug='create_funct_test_5_slug'
            ),
            component_name='create_funct_test_5'
        )

        try:
            create_functs(request_1)
            create_functs(request_2)
        except BadRequest:
            pass
        else:
            raise BadRequest('created functs with the same slugs')

    @TestCase.mock_auth(login='test-user')
    def test_creation_funct_with_slug(self):
        request = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test_6',
                description='test funct 6',
                weight=0.2,
                slug='create_funct_test_6_slug'
            ),
            component_name='create_funct_test_6'
        )

        create_functs(request)

        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'create_funct_test_6').one_or_none()

            if not component_model:
                raise BadRequest("cant create functionality with slug")

            funct_id = component_model.functionality_list[0].id
            functionality = session.query(Functionality).filter(
                Functionality.id == funct_id).one_or_none()

            if not functionality:
                raise BadRequest("cant create functionality with slug")

    @TestCase.mock_auth(login='test-user')
    def test_change_slug(self):

        request = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                id='38277945-c399-4e83-8de5-7622fa4d1941',
                name='test_7',
                description='test funct 7',
                weight=0.2
            ),
            component_name='create_funct_test_7'
        )
        create_functs(request)
        request_2 = functionality_pb2.UpdateFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                id='38277945-c399-4e83-8de5-7622fa4d1941',
                name='test_change_slug_name',
                slug='create_funct_test_7_slug'
            )
        )
        Warden().update_functionality(request_2, context=None)

        with session_scope() as session:
            component_model = session.query(Component).filter(
                Component.name == 'create_funct_test_7').one_or_none()

            if not component_model:
                raise BadRequest("cant update functionality with slug")

            funct_id = component_model.functionality_list[0].id
            functionality = session.query(Functionality).filter(
                Functionality.id == funct_id).one_or_none()

            if not functionality:
                raise BadRequest("cant update functionality with slug")

            if not functionality.slug == request_2.functionality.slug:
                raise BadRequest("cant update functionality with slug")

    @TestCase.mock_auth(login='test-user-2')
    def test_creation_functs_no_auth(self):
        request = functionality_pb2.AddFunctionalityRequest(
            functionality=functionality_pb2.Functionality(
                name='test-no-auth',
                description='test funct',
                weight=0.2,
            ),
            component_name='create_funct_test_1'
        )
        try:
            Warden().add_functionality(request=request, context=None)
        except NotAuthorized:
            pass
        except Exception as e:
            raise e
        else:
            self.assertFalse('ACL test failed, functionality has been created')
