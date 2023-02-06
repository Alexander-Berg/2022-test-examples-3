# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import uuid

from collections import namedtuple

from search.priemka.yappy.src.yappy_lib import utils
from search.priemka.yappy.src.yappy_lib.constants import NIL_UUID
from search.priemka.yappy.proto.structures.resources_pb2 import Container, CoredumpPolicy
from search.priemka.yappy.sqla.yappy import model

from search.priemka.yappy.tests.utils.test_cases import TestCase, TestCaseWithDB

SubTest = namedtuple('SubTest', 'func args')


class TestUtilsClearMarkedFields(TestCase):

    def test_all_fields_not_marked(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP))
        expected = Container()
        expected.CopyFrom(msg)
        utils.clear_marked_fields(msg)

        self.assertEqual(msg, expected)

    def test_all_fields_marked(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP, deleted=True))
        expected = Container()
        expected.CopyFrom(msg)
        expected.ClearField('coredump_policy')
        utils.clear_marked_fields(msg)

        self.assertEqual(msg, expected)

    def test_field_marked(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP, deleted=True))
        expected = Container()
        expected.CopyFrom(msg)
        expected.ClearField('coredump_policy')
        utils.clear_marked_fields(msg, ('coredump_policy', ))

        self.assertEqual(msg, expected)

    def test_field_not_marked(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP))
        expected = Container()
        expected.CopyFrom(msg)
        utils.clear_marked_fields(msg, ('coredump_policy', ))

        self.assertEqual(msg, expected)

    def test_different_field(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP, deleted=True))
        expected = Container()
        expected.CopyFrom(msg)
        utils.clear_marked_fields(msg, ('no_such_field', ))

        self.assertEqual(msg, expected)

    def test_different_flag(self):
        msg = Container(name='c', coredump_policy=CoredumpPolicy(type=CoredumpPolicy.Type.COREDUMP, deleted=True))
        expected = Container()
        expected.CopyFrom(msg)
        utils.clear_marked_fields(msg, marker='no_such_flag')

        self.assertEqual(msg, expected)


class ObsoleteChecksTestCase(TestCaseWithDB):
    BETAS = ['beta-1', 'beta-2']
    COMPONENTS = [uuid.uuid4(), uuid.uuid4()]
    N_BETA_CHECKS = 3
    N_COMPONENT_CHECKS = 2

    def create_test_data(self):
        checks = []
        required_params = {
            'hash': 'not_empty_hash',
            'last_run': 111,
            'success': True,
        }

        for i, beta in enumerate(self.BETAS):
            checks += [
                model.Check(
                    id='beta_check_id-{}-{}'.format(i, j),
                    beta=model.Beta(name=beta),
                    **required_params
                )
                for j in range(self.N_BETA_CHECKS)
            ]
        for i, cid in enumerate(self.COMPONENTS):
            checks += [
                model.Check(
                    id='component_check_id-{}-{}'.format(i, j),
                    beta_component=model.BetaComponent(id=cid),
                    **required_params
                )
                for j in range(self.N_COMPONENT_CHECKS)
            ]
        with utils.session_scope() as session:
            for check in checks:
                session.merge(check)

    @mock.patch('search.priemka.yappy.src.yappy_lib.utils.uuid')
    @mock.patch('search.priemka.yappy.src.yappy_lib.utils.now')
    @mock.patch('search.priemka.yappy.src.yappy_lib.utils.STORAGE')
    def test_obsolete_checks(self, storage, now, uuid_):
        ts = 123456234
        reqid = 'my-test-reqid'
        etag = uuid.uuid4()
        uuid_.uuid4.return_value = etag
        message = 'check obsoleteon reason'
        now().timestamp.return_value = ts
        storage.thread_local.request_id = reqid
        updated = utils.obsolete_checks(
            model.Check.yappy__BetaComponent_id,
            [self.COMPONENTS[0]],
            message,
        )
        expected_values = {
            'error': 'outdated',
            'etag': etag,
            'hash': '',
            'last_update': ts,
            'last_update_reqid': reqid,
            'last_run': 0,
            'message': message,
            'related_etag': NIL_UUID,
            'success': False,
        }

        with utils.session_scope() as session:
            checks = session.query(model.Check).filter(model.Check.yappy__BetaComponent_id == self.COMPONENTS[0]).all()
            other_checks = (
                session.query(model.Check).filter(model.Check.yappy__BetaComponent_id != self.COMPONENTS[0]).all()
            )
            subtests = {
                'test_{}'.format(param): SubTest(
                    self.assertEqual,
                    ([getattr(c, param) for c in checks], [expected] * self.N_COMPONENT_CHECKS)
                )
                for param, expected in expected_values.items()
            }
            subtests.update({
                'test_not_changed_{}'.format(param): SubTest(
                    self.assertNotIn,
                    (expected, [getattr(c, param) for c in other_checks])
                )
                for param, expected in expected_values.items()
            })

        subtests['test_n_updated'] = SubTest(self.assertEqual, (updated, self.N_COMPONENT_CHECKS))
        errors = []
        failed_subtests = []
        for title, subtest in subtests.items():
            try:
                subtest.func(*subtest.args)
            except self.failureException as err:
                failed_subtests.append(title)
                errors.append('\n---\nSubtest({}) failed with:\n{}'.format(title, err))

        if errors:
            message = 'failed subtests: {}\nDetails:'.format(failed_subtests) + '\n'.join(errors)
            self.fail(message)
