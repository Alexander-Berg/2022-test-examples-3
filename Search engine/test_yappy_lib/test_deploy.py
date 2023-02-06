# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import randomproto
import six

if six.PY2:
    import mock
    from contextlib2 import ExitStack
else:
    import unittest.mock as mock
    from contextlib import ExitStack

from yp.data_model import TStage, TPodSpec, TDeployUnitSpec, TSecretRef, TPodAgentSpec

from search.priemka.yappy.proto.structures.resources_pb2 import Resources

from search.priemka.yappy.src.yappy_lib import deploy
from search.priemka.yappy.src.yappy_lib.config_utils import get_config

from search.priemka.yappy.tests.utils.test_cases import TestCase


class DeployTestCase(TestCase):

    @property
    def stage(self):
        # type: () -> TStage
        stage = TStage()
        stage.spec.deploy_units.get_or_create('dummy_unit')
        stage.meta.acl.add()
        return stage

    @property
    def deploy_unit(self):
        # type: () -> TDeployUnitSpec
        return TDeployUnitSpec()

    @property
    def pod_spec(self):
        # type: () -> TPodSpec
        spec = TPodSpec()
        for i in range(3):
            spec.secrets['secret-{}'.format(i)].CopyFrom(randomproto.randproto(TPodSpec.TSecret))
        for i in range(5):
            spec.secret_refs['secret-ref-{}'.format(i)].CopyFrom(randomproto.randproto(TSecretRef))
        return spec

    @property
    def agent_spec(self):
        # type: () -> TPodAgentSpec
        return TPodAgentSpec()

    @property
    def secrets(self):
        if not getattr(self, '_secrets', None):
            resources = Resources()
            resources.secrets['secret'].CopyFrom(
                TSecretRef(
                    secret_id='dummy-secret',
                    secret_version='version',
                )
            )
            self._secrets = resources.secrets

        # make sure returned value can't be used to alter original value
        new_resources = Resources()
        for key, value in self._secrets.items():
            new_resources.secrets[key].CopyFrom(value)
        return new_resources.secrets

    def _fake_get_secrets(self, stage_, deploy_unit, resources=None):
        if not resources:
            resources = Resources()
        secrets = self.secrets
        for key, value in secrets.items():
            resources.secrets[key].CopyFrom(value)
        return resources

    @classmethod
    def setUpClass(cls):
        cls.deploy = deploy.DeployClient.from_config(get_config())


class MiscDeployTestCase(DeployTestCase):

    def test_stage_put_signature(self):
        """ Test that the `infra.dctl.src.lib.stage.put` signature hasn't changed dramatically """
        with ExitStack() as stack:
            stack.enter_context(
                mock.patch('search.priemka.yappy.src.yappy_lib.deploy.stage.put', autospec=True),
            )
            stack.enter_context(mock.patch.object(self.deploy.context, 'get_client'))
            try:
                self.deploy.put_stage(self.stage)
            except TypeError as err:
                self.fail('unexpected TypeError: {}'.format(err))

    def test_copy_stage_patch_workloads_called(self):
        with ExitStack() as stack:
            get_stage = stack.enter_context(mock.patch.object(self.deploy, 'get_stage'))
            get_stage.return_value = self.stage

            stack.enter_context(mock.patch.object(self.deploy.context, 'get_client'))
            patch_workloads = stack.enter_context(
                mock.patch('infra.dctl.src.lib.helpers.patch_pod_agent_spec_mutable_workloads')
            )

            self.deploy.copy_stage('stage-1', 'stage-1-copy')

            patch_workloads.assert_called()

    def test_get_per_cluster_replica_pod_spec(self):
        du = self.deploy_unit
        spec = self.deploy._get_pod_spec(du)
        expected = du.replica_set.replica_set_template.pod_template_spec.spec
        self.assertIs(spec, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_pod_spec')
    def test_get_agent_spec(self, mocked):
        pod_spec = self.pod_spec
        mocked.return_value = pod_spec
        spec = self.deploy._get_agent_spec(None)
        expected = pod_spec.pod_agent_payload.spec
        self.assertIs(spec, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_agent_spec')
    def test_get_static(self, mocked):
        spec = self.agent_spec
        mocked.return_value = spec
        expected = spec.resources.static_resources
        result = self.deploy._get_static(self.stage, 'dummy')
        self.assertIs(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_agent_spec')
    def test_get_layers(self, mocked):
        spec = self.agent_spec
        mocked.return_value = spec
        expected = spec.resources.layers
        result = self.deploy._get_layers(self.stage, 'dummy')
        self.assertIs(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_agent_spec')
    def test_get_boxes(self, mocked):
        spec = self.agent_spec
        mocked.return_value = spec
        expected = spec.boxes
        result = self.deploy._get_boxes(self.stage, 'dummy')
        self.assertIs(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_agent_spec')
    def test_get_workloads(self, mocked):
        spec = self.agent_spec
        mocked.return_value = spec
        expected = spec.workloads
        result = self.deploy._get_workloads(self.stage, 'dummy')
        self.assertIs(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_agent_spec')
    def test_get_mutable_workloads(self, mocked):
        spec = self.agent_spec
        mocked.return_value = spec
        expected = spec.mutable_workloads
        result = self.deploy._get_mutable_workloads(self.stage, 'dummy')
        self.assertIs(result, expected)


class GetSecretsDeployTestCase(DeployTestCase):

    def test_return_passed_resources(self):
        orig_resources = Resources()
        resources = self.deploy._get_secrets(self.stage, 'dummy', orig_resources)
        self.assertIs(resources, orig_resources)

    def test_return_value_type(self):
        resources = self.deploy._get_secrets(self.stage, 'dummy')
        self.assertIsInstance(resources, Resources)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_pod_spec')
    def test_secrets_cnt(self, mocked):
        resources = Resources()
        spec = self.pod_spec
        expected = len(spec.secrets) + len(spec.secret_refs)
        mocked.return_value = spec
        self.deploy._get_secrets(self.stage, 'dummy', resources)
        n_secrets = len(resources.secrets)
        self.assertEqual(n_secrets, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_pod_spec')
    def test_secret_to_secret_ref(self, mocked):
        resources = Resources()
        spec = self.pod_spec
        mocked.return_value = spec
        self.deploy._get_secrets(self.stage, 'dummy', resources)
        for key, value in spec.secrets.items():
            expected = TSecretRef(
                secret_id=value.secret_id,
                secret_version=value.secret_version,
            )
            result = resources.secrets[key]
            self.assertEqual(result, expected)

    @mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_pod_spec')
    def test_secret_ref_to_secret_ref(self, mocked):
        resources = Resources()
        spec = self.pod_spec
        mocked.return_value = spec
        self.deploy._get_secrets(self.stage, 'dummy', resources)
        for key, value in spec.secret_refs.items():
            expected = value
            result = resources.secrets[key]
            self.assertEqual(result, expected)


class SetSecretsDeployTestCase(DeployTestCase):

    def setUp(self):
        super(SetSecretsDeployTestCase, self).setUp()
        self.get_pod_spec_patch = mock.patch('search.priemka.yappy.src.yappy_lib.deploy.DeployClient._get_pod_spec')
        self.get_pod_spec = self.get_pod_spec_patch.start()
        self.get_pod_spec.return_value = self.pod_spec

    def tearDown(self):
        self.get_pod_spec_patch.stop()
        super(SetSecretsDeployTestCase, self).tearDown()

    def test_set_secrets(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)
        result = self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secret_refs
        expected = self.secrets

        self.assertEqual(result, expected)

    def test_clear_secrets_on_erase(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]

        secret_key = 'secret_key'
        spec = self.deploy._get_pod_spec(stage, du_key)
        spec.secrets[secret_key].secret_id = 'secret_id'
        spec.secrets[secret_key].secret_version = 'secret_version'
        spec.secrets[secret_key].delegation_token = 'delegation_token'

        self.deploy._set_secrets(stage, du_key, Resources().secrets)

        result = list(self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secrets.keys())
        expected = []

        self.assertEqual(result, expected)

    def test_clear_secrets_on_update(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]

        secret_key = 'secret_key'
        spec = self.deploy._get_pod_spec(stage, du_key)
        spec.secrets[secret_key].secret_id = 'secret_id'
        spec.secrets[secret_key].secret_version = 'secret_version'
        spec.secrets[secret_key].delegation_token = 'delegation_token'

        self.deploy._set_secrets(stage, du_key, self.secrets)

        result = list(self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secrets.keys())
        expected = []

        self.assertEqual(result, expected)

    def test_clear_secret_refs_on_erase(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)
        self.deploy._set_secrets(stage, du_key, Resources().secrets)
        result = list(self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secret_refs.keys())
        expected = []

        self.assertEqual(result, expected)

    def test_update_secret(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)

        secrets = self.secrets
        secret_key = list(secrets.keys())[0]
        new_version = 'new-version'
        secrets[secret_key].secret_version = new_version

        self.deploy._set_secrets(stage, du_key, secrets)
        result = self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secret_refs[secret_key].secret_version
        expected = new_version

        self.assertEqual(result, expected)

    def test_override_secrets(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)

        secrets = Resources().secrets
        secret_key = 'new-secret-key'
        secrets[secret_key].CopyFrom(
            TSecretRef(
                secret_id='new-secret-id',
                secret_version='new-secret-version',
            )
        )

        self.deploy._set_secrets(stage, du_key, secrets)
        result = self.deploy._get_pod_spec(stage.spec.deploy_units[du_key]).secret_refs
        expected = secrets

        self.assertEqual(result, expected)


class UnitStateDeployTestCase(DeployTestCase):

    def test_secrets_in_state(self):
        with ExitStack() as stack:
            stack.enter_context(mock.patch.object(self.deploy, 'get_stage', return_value=self.stage))
            stack.enter_context(mock.patch.object(self.deploy, '_get_agent_spec', return_value=self.agent_spec))
            get_secrets = stack.enter_context(mock.patch.object(self.deploy, '_get_secrets'))
            get_secrets.side_effect = self._fake_get_secrets
            state = self.deploy.get_deploy_unit_state('dummy', 'dummy')

        result = state.resources.secrets
        expected = self.secrets

        self.assertEqual(result, expected)


class UpdateResourcesDeployTestCase(DeployTestCase):

    @classmethod
    def setUpClass(cls):
        super(UpdateResourcesDeployTestCase, cls).setUpClass()
        cls.get_stage_patch = mock.patch.object(cls.deploy, 'get_stage')
        cls.put_stage_patch = mock.patch.object(cls.deploy, 'put_stage')

    def setUp(self):
        super(UpdateResourcesDeployTestCase, self).setUp()
        self.get_stage = self.get_stage_patch.start()
        self.put_stage = self.put_stage_patch.start()
        self.get_stage.return_value = self.stage
        self.put_stage.return_value = self.stage.meta.id

    def tearDown(self):
        self.get_stage = self.get_stage_patch.stop()
        self.put_stage = self.put_stage_patch.stop()
        super(UpdateResourcesDeployTestCase, self).tearDown()

    def test_set_secrets(self):
        with mock.patch.object(self.deploy, '_set_secrets') as set_secrets:
            self.deploy.update_resources('dummy', 'dummy', Resources(secrets=self.secrets), [])
            result = set_secrets.call_args.args[2]

        expected = self.secrets
        self.assertEqual(result, expected)

    def test_comment(self):
        expected = 'update resources comment'
        self.deploy.update_resources('dummy', 'dummy', Resources(), [], expected)
        result = self.put_stage.call_args.args[0].spec.revision_info.description
        self.assertEqual(result, expected)


class CopyStageDeployTestCase(DeployTestCase):

    def test_copy_stage_translate_secrets(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)
        expected = self.secrets

        with ExitStack() as stack:
            get_stage = stack.enter_context(mock.patch.object(self.deploy, 'get_stage'))
            put_stage = stack.enter_context(mock.patch.object(self.deploy, 'put_stage'))
            get_stage.return_value = stage

            self.deploy.copy_stage('dummy', 'dummy', True)

        passed_stage = put_stage.call_args.args[0]
        result = self.deploy._get_secrets(passed_stage, du_key).secrets

        self.assertEqual(result, expected)

    def test_copy_stage_dont_translate_secrets(self):
        stage = self.stage
        du_key = list(stage.spec.deploy_units.keys())[0]
        self.deploy._set_secrets(stage, du_key, self.secrets)
        expected = Resources().secrets

        with ExitStack() as stack:
            get_stage = stack.enter_context(mock.patch.object(self.deploy, 'get_stage'))
            put_stage = stack.enter_context(mock.patch.object(self.deploy, 'put_stage'))
            get_stage.return_value = stage

            self.deploy.copy_stage('dummy', 'dummy', False)

        passed_stage = put_stage.call_args.args[0]
        result = self.deploy._get_secrets(passed_stage, du_key).secrets

        self.assertEqual(result, expected)
