# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import uuid

from typing import AnyStr, List

from search.martylib.db_utils import session_scope, to_model

from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.proto.structures.auth_pb2 import AuthObject, StaffUnion

from search.priemka.yappy.src.scheduler import utils

from search.priemka.yappy.tests.utils.test_cases import TestCase, TestCaseWithDB


class SchedulerUtilsDBTest(TestCaseWithDB):

    @classmethod
    def setUpClass(cls):
        super(SchedulerUtilsDBTest, cls).setUpClass()
        cids = ['c-{}'.format(i) for i in range(5)]
        components = {cid: uuid.uuid4() for cid in cids}
        betas = ['beta-1', 'beta-2', 'beta-no-auth']
        logins = ['login-{}'.format(i) for i in range(5)]
        cls.config = {
            'components': components,
            'beta_names': betas,
            'betas': {
                betas[0]: {'components': [cids[0], cids[1]]},
                betas[1]: {'components': [cids[0], cids[2]]},
                betas[2]: {'components': [cids[3]]}
            },
            'auth': [
                {'beta': betas[0], 'logins': logins[:2]},
                {'beta': betas[1], 'logins': logins[1:4]},
            ]
        }

    def create_test_data(self):
        components = {
            component: model.BetaComponent(id=cid)
            for component, cid in self.config['components'].items()
        }
        betas = [
            model.Beta(
                name=name,
                components=[components[c] for c in self.config['betas'][name]['components']]
            )
            for name in self.config['betas']
        ]
        auth = [
            AuthObject(
                type=AuthObject.Type.BETA,
                name=auth_data['beta'],
                staff=StaffUnion(logins=auth_data['logins']),
            )
            for auth_data in self.config['auth']
        ]
        auth = [to_model(a) for a in auth]
        with session_scope() as session:
            session.add_all(*[betas + list(components.values()) + auth])

    def _get_expected_component_auth(self, component):
        # type: (AnyStr) -> List[StaffUnion]
        betas = [name for name, data in self.config['betas'].items() if component in data['components']]
        return utils.merge_auth(
            [
                StaffUnion(logins=auth_data['logins'])
                for auth_data in self.config['auth'] if auth_data['beta'] in betas
            ]
        )

    def test_merge_auth_list(self):
        auth_list = [
            StaffUnion(logins=['login-1', 'login-2']),
            StaffUnion(groups=[1]),
            StaffUnion(logins=['login-1', 'login-3'], groups=[1, 2]),
        ]
        expected = StaffUnion(
            logins=sorted(['login-1', 'login-2', 'login-3']),
            groups=sorted([1, 2])
        )
        result = utils.merge_auth(auth_list)
        self.assertEqual(result, expected)

    def test_get_beta_components_auth(self):
        expected = {
            str(self.config['components'][cid]): self._get_expected_component_auth(cid)
            for cid in self.config['components']
        }
        with session_scope() as session:
            components = session.query(model.BetaComponent).all()
            result = utils.get_beta_components_auth(components, session)

        self.assertEqual(result, expected)

    def test_get_beta_components_auth_no_session(self):
        expected = {
            str(cid): self._get_expected_component_auth(component)
            for component, cid in self.config['components'].items()
        }
        with session_scope() as session:
            components = session.query(model.BetaComponent).all()
            result = utils.get_beta_components_auth(components)

        self.assertEqual(result, expected)


class SchedulerUtilsNoDBTest(TestCase):
    def test_message_to_alchemy_json(self):
        message = AuthObject(type=AuthObject.Type.BETA, name='beta', staff=StaffUnion(logins=['login']))
        result = utils.message_to_alchemy_json(message)
        expected = {
            str('type'): str('BETA'),
            str('name'): str('beta'),
            str('staff'): message.staff.SerializeToString(),
        }
        self.assertEqual(result, expected)
