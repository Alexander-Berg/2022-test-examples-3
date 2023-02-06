# coding: utf-8

"""
ToDo:
 - add tests for other reducers
 - extend test cases to cover not only `create_*` methods
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.core.date_utils import mock_now
from search.martylib.db_utils import clear_db, prepare_db, session_scope

from search.priemka.yappy.proto.structures.api_pb2 import ApiBetaComponent, ApiComponentType, ApiQuota
from search.priemka.yappy.proto.structures.auth_pb2 import AuthObject
from search.priemka.yappy.proto.structures.check_pb2 import Check
from search.priemka.yappy.proto.structures.payload_pb2 import Snapshot
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.src.model.lineage2_service.event_reducer.beta_component_reducer import (
    DEFAULT_BETA_COMPONENT_CHECKS,
    DEFAULT_BETA_COMPONENT_INFO_CHECKS,
)
from search.priemka.yappy.src.model.lineage2_service.event_reducer import (
    BetaComponentReducer,
    ComponentTypeReducer,
    QuotaReducer,
)
from search.priemka.yappy.tests.utils.test_cases import TestCaseWithDB


class LineageIIReducerTest(TestCaseWithDB):
    TEST_USER = 'test-user'

    ctype_reducer = ComponentTypeReducer
    quota_reducer = QuotaReducer
    component_resucer = BetaComponentReducer

    def setUp(self):
        super(LineageIIReducerTest, self).setUp()
        prepare_db()

    def tearDown(self):
        super(LineageIIReducerTest, self).tearDown()
        clear_db()


class ComponentTypeReducerTest(LineageIIReducerTest):
    TEST_TYPE = 'test-component-type'

    def test_create_component_type(self):
        request = ApiComponentType(name=self.TEST_TYPE)
        with session_scope() as session:
            self.ctype_reducer.create_component_type(request, Snapshot(), session)
            session.flush()
            created = session.query(model.ComponentType).filter(model.ComponentType.name == self.TEST_TYPE).first()
            self.assertIsNotNone(created)

    def test_create_component_type_last_update_ts(self):
        request = ApiComponentType(name=self.TEST_TYPE)
        with mock_now(123), session_scope() as session:
            self.ctype_reducer.create_component_type(request, Snapshot(), session)
            session.flush()
            created = session.query(model.ComponentType).filter(model.ComponentType.name == self.TEST_TYPE).first()
            self.assertEqual(created.last_update, 123)

    @LineageIIReducerTest.mock_auth(login=LineageIIReducerTest.TEST_USER)
    def test_auto_created_component_type_auth(self):
        request = ApiComponentType(name=self.TEST_TYPE, auto_created=True)
        with session_scope() as session:
            self.ctype_reducer.create_component_type(request, Snapshot(), session)
            session.flush()
            auth = session.query(model.AuthObject).filter(
                model.AuthObject.type == AuthObject.Type[AuthObject.Type.COMPONENT_TYPE],
                model.AuthObject.name == request.name,
            ).first()
            self.assertIsNotNone(auth)
            auth = auth.to_protobuf()

        self.assertIn(self.TEST_USER, auth.staff.logins)

    @LineageIIReducerTest.mock_auth(login=LineageIIReducerTest.TEST_USER)
    def test_normal_component_type_auth(self):
        request = ApiComponentType(name=self.TEST_TYPE)
        with session_scope() as session:
            self.ctype_reducer.create_component_type(request, Snapshot(), session)
            session.flush()
            auth = session.query(model.AuthObject).filter(
                model.AuthObject.type == AuthObject.Type[AuthObject.Type.COMPONENT_TYPE],
                model.AuthObject.name == request.name,
            ).first()
            self.assertIsNone(auth)


class QuotaReducerTest(LineageIIReducerTest):
    TEST_QUOTA = 'test-quota'

    def test_create_quota(self):
        request = ApiQuota(name=self.TEST_QUOTA)
        with session_scope() as session:
            self.quota_reducer.create_quota(request, Snapshot(), session)
            session.flush()
            created = session.query(model.Quota).filter(model.Quota.name == self.TEST_QUOTA).first()
            self.assertIsNotNone(created)

    def test_create_quota_last_update_ts(self):
        request = ApiQuota(name=self.TEST_QUOTA)
        with mock_now(123), session_scope() as session:
            self.quota_reducer.create_quota(request, Snapshot(), session)
            session.flush()
            created = session.query(model.Quota).filter(model.Quota.name == self.TEST_QUOTA).first()
            self.assertEqual(created.last_update, 123)

    @LineageIIReducerTest.mock_auth(login=LineageIIReducerTest.TEST_USER)
    def test_auto_created_quota_auth(self):
        request = ApiQuota(name=self.TEST_QUOTA, auto_created=True)
        with session_scope() as session:
            self.quota_reducer.create_quota(request, Snapshot(), session)
            session.flush()
            auth = session.query(model.AuthObject).filter(
                model.AuthObject.type == AuthObject.Type[AuthObject.Type.QUOTA],
                model.AuthObject.name == request.name,
            ).first()
            self.assertIsNotNone(auth)
            auth = auth.to_protobuf()

        self.assertIn(self.TEST_USER, auth.staff.logins)

    @LineageIIReducerTest.mock_auth(login=LineageIIReducerTest.TEST_USER)
    def test_normal_quota_auth(self):
        request = ApiQuota(name=self.TEST_QUOTA)
        with session_scope() as session:
            self.quota_reducer.create_quota(request, Snapshot(), session)
            session.flush()
            auth = session.query(model.AuthObject).filter(
                model.AuthObject.type == AuthObject.Type[AuthObject.Type.QUOTA],
                model.AuthObject.name == request.name,
            ).first()
            self.assertIsNone(auth)


class ComponentReducerTest(LineageIIReducerTest):

    @mock.patch('search.priemka.yappy.src.model.lineage2_service.event_reducer.beta_component_reducer.add_default_checks')
    def test_create_component_default_checks(self, mocked):
        dummy_session = mock.MagicMock()
        request = ApiBetaComponent()
        self.component_resucer.create_beta_component(request, Snapshot(), dummy_session)
        mocked.assert_any_call(request, DEFAULT_BETA_COMPONENT_CHECKS)

    @mock.patch('search.priemka.yappy.src.model.lineage2_service.event_reducer.beta_component_reducer.add_default_checks')
    def test_create_component_default_info_checks(self, mocked):
        dummy_session = mock.MagicMock()
        request = ApiBetaComponent()
        self.component_resucer.create_beta_component(request, Snapshot(), dummy_session)
        mocked.assert_any_call(request, DEFAULT_BETA_COMPONENT_INFO_CHECKS, Check.Severity.INFO)
