from search.martylib.db_utils import prepare_db, session_scope, clear_db
from search.martylib.http.exceptions import NotFound
from search.martylib.test_utils import TestCase
from search.mon.warden.proto.structures import component_pb2, user_pb2
from search.mon.warden.proto.structures.component import common_pb2, curator_pb2
from search.mon.warden.sqla.warden.model import Component, User
from search.mon.warden.src.services import Warden
from search.mon.warden.src.services.component import ComponentApiService
from sqlalchemy.orm import lazyload
from sqlalchemy.sql import and_

COMPONENT_API_CLIENT = ComponentApiService()
WARDEN_CLIENT = Warden()


class TestWardenUpdateComponentCurators(TestCase):

    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        prepare_db()

        with session_scope() as session:
            session.add(Component(
                name='component',
                parent_component_name='parent',
                slug='parent__component',
            ))

    @classmethod
    def tearDownClass(cls):
        clear_db()

    def tearDown(self):
        with session_scope() as session:
            for user in session.query(User).all():
                session.delete(user)

    @TestCase.mock_auth(login='test-user')
    def test_add_new_curator(self):
        request = curator_pb2.UpdateComponentCuratorsRequest(
            component=common_pb2.ComponentFilter(
                component_name='component',
                parent_component_name='parent'
            ),
            curators=[
                user_pb2.User(login='new_user')
            ],
        )
        COMPONENT_API_CLIENT.update_component_curators(request=request, context=None)

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(
            name='component',
            parent_component_name='parent'
        ), context=None).component
        curator_names = [c.login for c in component.curator_list]
        self.assertEqual(curator_names, ['new_user'])

        with session_scope() as session:
            users = session.query(User).all()
            self.assertEqual(len(users), 1)
            self.assertEqual(users[0].login, 'new_user')

    @TestCase.mock_auth(login='test-user')
    def test_add_existing_curator(self):
        with session_scope() as session:
            session.add(User(login='another'))

        request = curator_pb2.UpdateComponentCuratorsRequest(
            component=common_pb2.ComponentFilter(
                component_name='component',
                parent_component_name='parent'
            ),
            curators=[
                user_pb2.User(login='another')
            ],
        )
        COMPONENT_API_CLIENT.update_component_curators(request=request, context=None)

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(
            name='component',
            parent_component_name='parent'
        ), context=None).component
        curator_names = [c.login for c in component.curator_list]
        self.assertEqual(curator_names, ['another'])

        with session_scope() as session:
            users = session.query(User).all()
            self.assertEqual(len(users), 1)
            self.assertEqual(users[0].login, 'another')

    @TestCase.mock_auth(login='test-user')
    def test_remove_curators(self):
        with session_scope() as session:
            component = (
                session
                .query(Component)
                .filter(and_(
                    Component.name == 'component',
                    Component.parent_component_name == 'parent'
                ))
                .options(lazyload('*'))
                .one_or_none()
            )
            component.curator_list.append(User(login='another'))

        request = curator_pb2.UpdateComponentCuratorsRequest(
            component=common_pb2.ComponentFilter(
                component_name='component',
                parent_component_name='parent'
            ),
            curators=[],
        )
        COMPONENT_API_CLIENT.update_component_curators(request=request, context=None)

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(
            name='component',
            parent_component_name='parent'
        ), context=None).component
        curator_names = [c.login for c in component.curator_list]
        self.assertEqual(curator_names, [])

        with session_scope() as session:
            users = session.query(User).all()
            self.assertEqual(len(users), 1)
            self.assertEqual(users[0].login, 'another')

    @TestCase.mock_auth(login='test-user')
    def test_bad_component(self):
        request = curator_pb2.UpdateComponentCuratorsRequest(
            component=common_pb2.ComponentFilter(
                component_name='bad',
                parent_component_name='very'
            ),
            curators=[
                user_pb2.User(login='new_user')
            ],
        )
        self.assertRaises(
            NotFound,
            lambda: COMPONENT_API_CLIENT.update_component_curators(request=request, context=None)
        )

        with session_scope() as session:
            users = session.query(User).all()
            self.assertEqual(len(users), 0)

    @TestCase.mock_auth(login='test-user')
    def test_update_curators(self):
        with session_scope() as session:
            component = (
                session
                .query(Component)
                .filter(and_(
                    Component.name == 'component',
                    Component.parent_component_name == 'parent'
                ))
                .options(lazyload('*'))
                .one_or_none()
            )
            component.curator_list.append(User(login='first'))
            component.curator_list.append(User(login='second'))

        request = curator_pb2.UpdateComponentCuratorsRequest(
            component=common_pb2.ComponentFilter(
                component_name='component',
                parent_component_name='parent'
            ),
            curators=[
                user_pb2.User(login='second'),
                user_pb2.User(login='third'),
            ],
        )
        COMPONENT_API_CLIENT.update_component_curators(request=request, context=None)

        component = WARDEN_CLIENT.get_component(component_pb2.GetComponentRequest(
            name='component',
            parent_component_name='parent'
        ), context=None).component
        curator_names = [c.login for c in component.curator_list]
        self.assertEqual(curator_names, ['second', 'third'])

        with session_scope() as session:
            users = session.query(User).all()
            self.assertEqual(len(users), 3)
            first = session.query(User).filter(User.login == 'first').one_or_none()
            self.assertNotEqual(first, None)
            second = session.query(User).filter(User.login == 'second').one_or_none()
            self.assertNotEqual(second, None)
            third = session.query(User).filter(User.login == 'third').one_or_none()
            self.assertNotEqual(third, None)
