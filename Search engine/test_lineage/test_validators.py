# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import uuid

from search.priemka.yappy.proto.structures.api_pb2 import ApiBetaComponent, ApiComponentType, ApiQuota
from search.priemka.yappy.proto.structures.beta_component_pb2 import ComponentType
from search.priemka.yappy.proto.structures.payload_pb2 import (
    BetaComponentEvent,
    Lineage2Response,
    Lineage2ValidationError,
    Snapshot,
)
from search.priemka.yappy.proto.structures.quota_pb2 import Quota

from search.priemka.yappy.src.model.lineage2_service.event_validator import BetaComponentValidator
from search.priemka.yappy.tests.utils.test_cases import TestCaseWithDB


class ValidatorTest(TestCaseWithDB):
    # ToDo: automate tests creation from data
    # ToDo: add tests for other validators and methods

    @classmethod
    def create_test_data(cls):
        cls.test_data = {
            'beta_component': {
                '_validator': BetaComponentValidator(),
                '_event': BetaComponentEvent,
                'create_beta_component': {
                    'with_id': {
                        'payload': ApiBetaComponent(id=str(uuid.uuid4())),
                        'expected_errors': [Lineage2ValidationError(field='id', msg='cannot manage beta_component id')],
                    },
                    'default_empty': {
                        'payload': ApiBetaComponent(),
                        'expected_errors': [Lineage2ValidationError(field='type', msg='field is required')],
                    },
                    'default_with_type': {
                        'payload': ApiBetaComponent(type='type'),
                        'snapshot': Snapshot(component_types={'type': ComponentType(name='type')}),
                        'expected_errors': [Lineage2ValidationError(field='quota', msg='field is required')],
                    },
                    'default_with_type_and_quota': {
                        'payload': ApiBetaComponent(type='type', quota='quota'),
                        'snapshot': Snapshot(
                            component_types={'type': ComponentType(name='type')},
                            quotas={'quota': Quota(name='quota')},
                        ),
                    },
                    'auto_create_without_specs': {
                        'payload': ApiBetaComponent(should_autocreate=True),
                        'expected_errors': [
                            Lineage2ValidationError(field='type_spec', msg='field is required for autocreation'),
                        ],
                    },
                    'auto_create_with_type_spec': {
                        'payload': ApiBetaComponent(
                            should_autocreate=True,
                            type_spec=ApiComponentType(name='new-component-type'),
                        ),
                        'expected_errors': [
                            Lineage2ValidationError(field='quota_spec', msg='field is required for autocreation'),
                        ],
                    },
                    'auto_create_with_specs': {
                        'payload': ApiBetaComponent(
                            should_autocreate=True,
                            type_spec=ApiComponentType(name='new-component-type'),
                            quota_spec=ApiQuota(name='new-quota'),
                        ),
                    },
                },
            },
        }

    def _test(self, validator_name, method, test_name):
        try:
            test_data = self.test_data[validator_name][method][test_name]
            validator = self.test_data[validator_name]['_validator']
            event = self.test_data[validator_name]['_event']
        except KeyError:
            raise NotImplementedError("No data for '{}' test for {}::{}".format(test_name, validator_name, method))

        response = Lineage2Response()
        validator(
            payload=event(**{method: test_data['payload']}),
            snapshot=test_data.get('snapshot', Snapshot()),
            result=response,
        )
        self.assertEqual(list(response.validation_errors), test_data.get('expected_errors', []))

    def test_create_beta_component_with_id(self):
        self._test('beta_component', 'create_beta_component', 'with_id')

    def test_create_beta_component_default_empty(self):
        self._test('beta_component', 'create_beta_component', 'default_empty')

    def test_create_beta_component_default_with_type(self):
        self._test('beta_component', 'create_beta_component', 'default_with_type')

    def test_create_beta_component_default_with_type_and_quota(self):
        self._test('beta_component', 'create_beta_component', 'default_with_type_and_quota')

    def test_create_beta_component_auto_create_without_specs(self):
        self._test('beta_component', 'create_beta_component', 'auto_create_without_specs')

    def test_create_beta_component_auto_create_with_type_spec(self):
        self._test('beta_component', 'create_beta_component', 'auto_create_with_type_spec')

    def test_create_beta_component_auto_create_with_specs(self):
        self._test('beta_component', 'create_beta_component', 'auto_create_with_specs')
