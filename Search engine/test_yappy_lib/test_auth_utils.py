# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from typing import Any, Union   # noqa

from search.martylib.core.exceptions import NotAuthorized
from search.martylib.test_utils import TestCase

from search.priemka.yappy.proto.structures.api_pb2 import ApiBeta, CreateBetaFromConfig
from search.priemka.yappy.proto.structures.auth_pb2 import AuthObject, StaffUnion
from search.priemka.yappy.proto.structures.conf_pb2 import AuthConfiguration
from search.priemka.yappy.proto.structures.payload_pb2 import Lineage2Response
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.src.yappy_lib import auth_utils
from search.priemka.yappy.src.yappy_lib.utils import session_scope

from search.priemka.yappy.tests.utils.test_cases import TestCaseWithDB

AUTH_CONFIG = AuthConfiguration(
    default_roles=StaffUnion(
        logins=['test-admin', 'admin-2'],
        groups=[123, 234],
    )
)


class AuthTest(TestCaseWithDB):
    TEST_USER = 'test-user'
    related_id = 'related-id'

    auth = auth_utils.Auth()

    @classmethod
    def setUpClass(cls):
        super(AuthTest, cls).setUpClass()
        cls.auth.config.auth.CopyFrom(AUTH_CONFIG)

    def tearDown(self):
        with session_scope() as session:
            session.query(model.AuthObject).delete()

    @staticmethod
    def normalize_auth_object(auth):
        auth.staff.logins.sort()
        auth.staff.groups.sort()
        return auth


class AddAuthForTest(AuthTest):

    @auth_utils.add_role_for
    def _wrapped_func(self, request, status, *args, **kwargs):
        # type: (Union[ApiBeta, CreateBetaFromConfig], Lineage2Response.Status, *Any, **Any) -> Lineage2Response

        return Lineage2Response(related_id=self.related_id, status=status)

    def test_beta_not_authenticated(self):
        self.assertRaises(
            NotAuthorized,
            self._wrapped_func,
            ApiBeta(),
            Lineage2Response.Status.SUCCESS,
        )

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_beta_success(self):
        self._wrapped_func(ApiBeta(), Lineage2Response.Status.SUCCESS)
        with session_scope() as session:
            auth = session.query(model.AuthObject).first()
            if auth:
                auth = self.normalize_auth_object(auth.to_protobuf())
        expected = self.normalize_auth_object(
            AuthObject(
                type=AuthObject.Type.BETA,
                name=self.related_id,
                staff=StaffUnion(
                    logins=([self.TEST_USER] + list(AUTH_CONFIG.default_roles.logins)),
                    groups=AUTH_CONFIG.default_roles.groups,
                ),
            )
        )
        self.assertEqual(auth, expected)

    @TestCase.mock_auth(login=AUTH_CONFIG.default_roles.logins[0])
    def test_beta_admin(self):
        self._wrapped_func(ApiBeta(), Lineage2Response.Status.SUCCESS)
        with session_scope() as session:
            auth = session.query(model.AuthObject).first()
            if auth:
                auth = self.normalize_auth_object(auth.to_protobuf())
        expected = self.normalize_auth_object(
            AuthObject(
                type=AuthObject.Type.BETA,
                name=self.related_id,
                staff=StaffUnion(
                    logins=list(AUTH_CONFIG.default_roles.logins),
                    groups=AUTH_CONFIG.default_roles.groups,
                ),
            )
        )
        self.assertEqual(auth, expected)

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_beta_failed(self):
        self._wrapped_func(ApiBeta(), Lineage2Response.Status.FAILED)
        with session_scope() as session:
            auth = session.query(model.AuthObject).first()
            self.assertIsNone(auth)

    def test_config_beta_not_authenticated(self):
        self.assertRaises(
            NotAuthorized,
            self._wrapped_func,
            CreateBetaFromConfig(),
            Lineage2Response.Status.SUCCESS,
        )

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_config_beta_failed(self):
        self._wrapped_func(CreateBetaFromConfig(), Lineage2Response.Status.FAILED)
        with session_scope() as session:
            auth = session.query(model.AuthObject).first()
            self.assertIsNone(auth)

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_config_beta_success(self):
        self._wrapped_func(CreateBetaFromConfig(), Lineage2Response.Status.SUCCESS)
        with session_scope() as session:
            auth = session.query(model.AuthObject).first()
            if auth:
                auth = self.normalize_auth_object(auth.to_protobuf())
        expected = self.normalize_auth_object(
            AuthObject(
                type=AuthObject.Type.BETA,
                name=self.related_id,
                staff=StaffUnion(
                    logins=([self.TEST_USER] + list(AUTH_CONFIG.default_roles.logins)),
                    groups=AUTH_CONFIG.default_roles.groups,
                ),
            )
        )
        self.assertEqual(auth, expected)


class AuthUtilsTest(AuthTest):

    def test_default_template(self):
        result = self.normalize_auth_object(self.auth._default_template_auth())
        expected = self.normalize_auth_object(AuthObject(staff=AUTH_CONFIG.default_roles))
        self.assertEqual(result, expected)

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_add_auth_no_template(self):
        with session_scope() as session:
            self.auth.add_auth(self.related_id, AuthObject.Type.BETA, session)
            session.flush()
            auth = session.query(model.AuthObject).first()
            auth = self.normalize_auth_object(auth.to_protobuf())

        expected = self.normalize_auth_object(
            AuthObject(
                name=self.related_id,
                type=AuthObject.Type.BETA,
                staff=StaffUnion(
                    logins=set(list(AUTH_CONFIG.default_roles.logins) + [self.TEST_USER]),
                    groups=AUTH_CONFIG.default_roles.groups,
                ),
            )
        )
        self.assertEqual(auth, expected)

    @TestCase.mock_auth(login=AuthTest.TEST_USER)
    def test_add_auth_with_template(self):
        template = AuthObject(
            staff=StaffUnion(
                logins=['login-1', 'login-2'],
                groups=[1],
            )
        )
        with session_scope() as session:
            self.auth.add_auth(self.related_id, AuthObject.Type.BETA, session, template=template)
            session.flush()
            auth = session.query(model.AuthObject).first()
            auth = self.normalize_auth_object(auth.to_protobuf())

        expected = self.normalize_auth_object(
            AuthObject(
                name=self.related_id,
                type=AuthObject.Type.BETA,
                staff=StaffUnion(
                    logins=set(list(template.staff.logins) + [self.TEST_USER]),
                    groups=template.staff.groups,
                ),
            )
        )
        self.assertEqual(auth, expected)
