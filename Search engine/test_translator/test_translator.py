# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import mock
import operator
import jinja2
import requests_mock
import six

if six.PY2:
    from itertools import izip
else:
    izip = zip

from google.protobuf.json_format import MessageToDict
from typing import AnyStr, List

from search.martylib.test_utils import TestCase

from search.priemka.yappy.proto.structures.beta_pb2 import Beta
from search.priemka.yappy.proto.structures.beta_component_pb2 import BetaComponent, ComponentType
from search.priemka.yappy.proto.structures.bolver_pb2 import BolverSpec, BolverSpecList, Header
from search.priemka.yappy.proto.structures.patch_pb2 import Patch
from search.priemka.yappy.proto.structures.resources_pb2 import (
    Container,
    CoredumpPolicy,
    CoredumpProcessor,
    CustomCommandCoredumpProcessor,
    DockerImage,
    EnvVar,
    EnvVarSource,
    InstanceCtl,
    InstanceSpec,
    KeychainSecret,
    LocalSandboxFile,
    Resources,
    SecretEnvSelector,
    LiteralEnvSelector,
    StaticFile,
    UrlFile,
    YasmUnistatEndpoint,
)
from search.priemka.yappy.proto.structures.scheduler_pb2 import ProcessorTask
from search.priemka.yappy.proto.structures.state_pb2 import TargetComponentState, CurrentComponentState, TargetBetaState
from search.priemka.yappy.proto.structures.slot_pb2 import Slot
from search.priemka.yappy.src.processor.modules.translator.beta_translator import BetaTranslator
from search.priemka.yappy.src.processor.modules.translator.component_translator import ComponentTranslator
from search.priemka.yappy.src.processor.modules.translator.exceptions import (
    ResourceMissingInParent,
    ResourceMissingInCurrentState,
    InvalidPatch,
)
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config
from search.priemka.yappy.src.yappy_lib.diff import states_are_equal
from search.priemka.yappy.src.yappy_lib.nanny import YappyNannyClientMock


class TestTranslator(TestCase):
    def setUp(self):
        # self.maxDiff = None
        self.deploy_patch = mock.patch('search.priemka.yappy.src.yappy_lib.clients.ClientsMock.deploy')
        self.deploy_client = self.deploy_patch.start()

        config = get_test_config()
        self.beta_translator = BetaTranslator(config)
        self.component_translator = ComponentTranslator(config)

    def tearDown(self):
        self.deploy_patch.stop()

    @staticmethod
    def get_sorted_bolver_headers(headers):
        """
        :type headers: Iterable[Header]
        :rtype: headers: Iterable[Header]
        """
        return sorted(headers, key=operator.attrgetter('key'))

    def get_new_bolver_headers(self, beta):
        new_state = TargetBetaState()
        self.beta_translator.compile_bolver_configuration(beta, new_state)
        return [header.value for header in self.get_sorted_bolver_headers(new_state.bolver_configuration.headers)]

    def additional_bolver_headers(self, beta):
        return [
            Header(
                key='X-Yandex-Yappy-Beta',
                value='{}:'.format(beta.name),
            ),
            Header(
                key='x-yandex-rps-limiter-quota',
                value=self.beta_translator.RPS_LIMITER_QUOTA,
            ),
            Header(
                key='X-Rpslimiter-Balancer',
                value=self.beta_translator.RPS_LIMITER_QUOTA,
            ),
        ]

    @staticmethod
    def render_components_headers(beta):
        rendered_headers = []
        for component in beta.components:
            values = {
                'instances': component.slot.instances,
                'balancer_url': component.slot.balancer_url,
                'balancerUrl': component.slot.balancer_url,
                'beta_name': beta.name,
            }
            for spec in component.type.bolver_specs.objects:
                rendered_headers += [
                    Header(
                        key=header.key,
                        value=jinja2.Template(header.value).render(**values)
                    )
                    for header in spec.headers
                ]
        return rendered_headers

    def test_headers_no_template(self):
        bolver_headers = [
            Header(
                key='header-1',
                value="whatever-{{instances|join(';')}}-{{balancerUrl}}",
            ),
        ]

        beta = Beta(
            components=[
                BetaComponent(
                    type=ComponentType(
                        bolver_specs=BolverSpecList(objects=[
                            BolverSpec(
                                use_header_templates=False,
                                headers=bolver_headers,
                            ),
                        ]),
                    ),
                    slot=Slot(
                        balancer_url='balancer.yandex-team.ru',
                        instances=[
                            'vla1-1234.search.yandex.net:24055',
                        ],
                    ),
                ),
            ],
        )

        add_headers = self.additional_bolver_headers(beta)

        expected = [header.value for header in self.get_sorted_bolver_headers(bolver_headers + add_headers)]
        template_header_values = self.get_new_bolver_headers(beta)
        self.assertEqual(template_header_values, expected)

    def test_templated_headers(self):
        bolver_headers = [
            Header(
                key='header-1',
                value="whatever-{{beta_name}}-{{instances|join(';')}}-{{balancerUrl}}",
            ),
        ]

        beta = Beta(
            name='test_beta_name',
            components=[
                BetaComponent(
                    type=ComponentType(
                        bolver_specs=BolverSpecList(objects=[
                            BolverSpec(
                                use_header_templates=True,
                                headers=bolver_headers,
                            ),
                        ]),
                    ),
                    slot=Slot(
                        balancer_url='balancer.yandex-team.ru',
                        instances=[
                            'vla1-9943.search.yandex.net:24055',
                            'vla2-0049.search.yandex.net:24055',
                            'vla2-0620.search.yandex.net:24055',
                        ],
                    ),
                ),
            ],
        )

        expected_headers = self.render_components_headers(beta) + self.additional_bolver_headers(beta)
        expected = [header.value for header in self.get_sorted_bolver_headers(expected_headers)]
        template_header_values = self.get_new_bolver_headers(beta)
        self.assertEqual(template_header_values, expected)

    def test_push_nanny_target_state_comment(self):
        beta_components = [
            BetaComponent(
                slot=Slot(id='test_id', type=Slot.Type.NANNY),
            ),
        ]

        reqid = 'EXPECTED_REQID'
        expected_values = [
            'Yappy resources; component #; reqid={}'.format(reqid),
        ]

        nanny_mock = YappyNannyClientMock()
        nanny_mock.load_services(
            {
                'test_id': {
                    'runtime_attrs': {
                        '_id': '',
                        'content': {
                            'resources': {
                                'sandbox_files': [],
                                'static_files': [],
                                'url_files': [],
                            },
                        },
                    },
                    'target_state': {
                        'content': {
                            'is_enabled': True,
                            'snapshot_id': '',
                        }
                    },
                    'current_state': {
                        'content': {
                            'active_snapshots': []
                        }
                    }
                },
            },
        )

        with \
                requests_mock.Mocker(),\
                mock.patch.object(
                    self.component_translator.storage.thread_local.__class__, 'request_id',
                    new_callable=mock.PropertyMock, return_value=reqid
                ):
            for beta_component, expected in izip(beta_components, expected_values):
                self.component_translator.clients.nanny = nanny_mock
                self.component_translator.push_nanny_target_state(beta_component, TargetComponentState())
                self.assertEqual(self.component_translator.clients.nanny.mocker.last_request.json()['comment'], expected)

    def test_copy_cdump_policy_no_parent(self):
        component = BetaComponent()
        component.patch.copy_coredump_policy = True
        self.assertRaises(
            InvalidPatch,
            self.component_translator.process,
            ProcessorTask(component=component)
        )

    def test_copy_cdump_policy_ignore_instance_spec(self):
        component = BetaComponent()
        component.patch.copy_coredump_policy = True
        component.patch.ignore_parent_instance_spec = True
        component.patch.parent_external_id = 'parent_id'
        with mock.patch.object(self.component_translator.clients.nanny, 'get_state') as get_state:
            get_state.return_value = CurrentComponentState()
            self.assertRaises(
                InvalidPatch,
                self.component_translator.process,
                ProcessorTask(component=component)
            )

    def test_push_deploy_state_comment(self):
        reqid = 'EXPECTED_ID'
        expected = 'Yappy state update; component #; reqid={}'.format(reqid)
        with mock.patch.object(
                self.component_translator.storage.thread_local.__class__, 'request_id',
                new_callable=mock.PropertyMock, return_value=reqid,
        ):
            self.component_translator.push_deploy_target_state(BetaComponent(), TargetComponentState(), [])
            result = self.deploy_client.update_resources.call_args.kwargs['comment']
            self.assertEqual(result, expected)


class TestTranslatorConvertResources(TestCase):
    @classmethod
    def setUpClass(cls):
        config = get_test_config()
        cls.component_translator = ComponentTranslator(config)

    def test_parent(self):
        beta_component = BetaComponent()
        parent_state = CurrentComponentState(
            resources=Resources(
                sandbox_files=[
                    LocalSandboxFile(
                        local_path='/path/to/parent/sandbox_file',
                        box_ref='box_ref',
                    ),
                ],
                static_files=[
                    StaticFile(
                        local_path='/path/to/parent/static_file',
                        content='static file content',
                    ),
                ],
                url_files=[
                    UrlFile(
                        local_path='/path/to/parent/url_file',
                        url='url',
                    ),
                ],
            ),
        )

        new_state = self.component_translator.convert_resources(beta_component, parent_state)
        resources = new_state.resources
        expected = parent_state.resources
        self.assertEqual(resources, expected)

    def test_delete(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.DELETE,
                        local_path='/path/to/deleted/*'
                    ),
                ],
            ),
            current_state=CurrentComponentState(),
        )
        parent_state = CurrentComponentState(
            resources=Resources(
                sandbox_files=[
                    LocalSandboxFile(
                        local_path='/path/to/parent/sandbox_file',
                        box_ref='box_ref',
                    ),
                    LocalSandboxFile(
                        local_path='/path/to/deleted/sandbox_file',
                        box_ref='box_ref',
                    ),
                ],
                static_files=[
                    StaticFile(
                        local_path='/path/to/parent/static_file',
                        content='static file content',
                    ),
                    StaticFile(
                        local_path='/path/to/deleted/static_file',
                        content='static file content',
                    ),
                ],
                url_files=[
                    UrlFile(
                        local_path='/path/to/parent/url_file',
                        url='url',
                    ),
                    UrlFile(
                        local_path='/path/to/deleted/url_file',
                        url='url',
                    ),
                ],
            ),
        )

        # Parent's resources without '/path/to/deleted/*'
        expected = Resources(
            sandbox_files=[
                LocalSandboxFile(
                    local_path='/path/to/parent/sandbox_file',
                    box_ref='box_ref',
                ),
            ],
            static_files=[
                StaticFile(
                    local_path='/path/to/parent/static_file',
                    content='static file content',
                ),
            ],
            url_files=[
                UrlFile(
                    local_path='/path/to/parent/url_file',
                    url='url',
                ),
            ],
        )

        new_state = self.component_translator.convert_resources(beta_component, parent_state)
        resources = new_state.resources
        self.assertEqual(resources, expected)

    def test_copy(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.COPY,
                        local_path='/path/to/parent/sandbox_file',
                    ),
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.COPY,
                        local_path='/path/to/parent/static_file',
                    ),
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.COPY,
                        local_path='/path/to/parent/url_file',
                    ),
                ],
            ),
        )
        parent_state = CurrentComponentState(
            resources=Resources(
                sandbox_files=[
                    LocalSandboxFile(
                        local_path='/path/to/parent/sandbox_file',
                        box_ref='box_ref',
                    ),
                ],
                static_files=[
                    StaticFile(
                        local_path='/path/to/parent/static_file',
                        content='static file content',
                    ),
                ],
                url_files=[
                    UrlFile(
                        local_path='/path/to/parent/url_file',
                        url='url',
                    ),
                ],
            ),
        )

        new_state = self.component_translator.convert_resources(beta_component, parent_state)
        resources = new_state.resources
        expected = parent_state.resources
        self.assertEqual(resources, expected)

    def test_current(self):
        beta_component = BetaComponent(
            patch=Patch(),
            current_state=CurrentComponentState(
                resources=Resources(
                    sandbox_files=[
                        LocalSandboxFile(
                            local_path='/path/to/current/sandbox_file',
                            box_ref='box_ref',
                        ),
                    ],
                    static_files=[
                        StaticFile(
                            local_path='/path/to/current/static_file',
                            content='static file content',
                        ),
                    ],
                    url_files=[
                        UrlFile(
                            local_path='/path/to/current/url_file',
                            url='url',
                        ),
                    ],
                ),
            ),
        )

        new_state = self.component_translator.convert_resources(beta_component)
        resources = new_state.resources
        expected = Resources()
        self.assertEqual(resources, expected)

    def test_copy_missed(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.COPY,
                        local_path='no_such_file',
                    ),
                ],
            ),
        )
        parent_state = CurrentComponentState()

        self.assertRaises(
            ResourceMissingInParent,
            self.component_translator.convert_resources,
            beta_component,
            parent_state,
        )

    def test_no_manage(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.NO_MANAGE,
                        local_path='/path/to/copied/sandbox_file',
                    ),
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.NO_MANAGE,
                        local_path='/path/to/copied/static_file',
                    ),
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.NO_MANAGE,
                        local_path='/path/to/copied/url_file',
                    ),
                ],
            ),
            current_state=CurrentComponentState(
                resources=Resources(
                    sandbox_files=[
                        LocalSandboxFile(
                            local_path='/path/to/current/sandbox_file',
                            box_ref='box_ref',
                        ),
                        LocalSandboxFile(
                            local_path='/path/to/copied/sandbox_file',
                            box_ref='box_ref',
                        ),
                    ],
                    static_files=[
                        StaticFile(
                            local_path='/path/to/current/static_file',
                            content='static file content',
                        ),
                        StaticFile(
                            local_path='/path/to/copied/static_file',
                            content='copied static file content',
                        ),
                    ],
                    url_files=[
                        UrlFile(
                            local_path='/path/to/current/url_file',
                            url='url',
                        ),
                        UrlFile(
                            local_path='/path/to/copied/url_file',
                            url='url',
                        ),
                    ],
                ),
            ),
        )

        # Current state resources' copied filed
        expected = Resources(
            sandbox_files=[
                LocalSandboxFile(
                    local_path='/path/to/copied/sandbox_file',
                    box_ref='box_ref',
                ),
            ],
            static_files=[
                StaticFile(
                    local_path='/path/to/copied/static_file',
                    content='copied static file content',
                ),
            ],
            url_files=[
                UrlFile(
                    local_path='/path/to/copied/url_file',
                    url='url',
                ),
            ],
        )

        new_state = self.component_translator.convert_resources(beta_component)
        resources = new_state.resources
        self.assertEqual(resources, expected)

    def test_no_manage_missed(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.NO_MANAGE,
                        local_path='no_such_file',
                    ),
                ],
            ),
            current_state=CurrentComponentState(),
        )

        self.assertRaises(
            ResourceMissingInCurrentState,
            self.component_translator.convert_resources,
            beta_component,
        )

    def test_static_content(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.STATIC_CONTENT,
                        local_path='/path/to/static/file',
                        content='static content',
                    ),
                ],
            ),
        )

        expected = Resources(
            static_files=[
                StaticFile(
                    local_path='/path/to/static/file',
                    content='static content',
                ),
            ],
        )

        new_state = self.component_translator.convert_resources(beta_component)
        resources = new_state.resources
        self.assertEqual(resources, expected)

    def test_url_file(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.URL_FILE,
                        local_path='/path/to/url/file',
                        url='url',
                    ),
                ],
            ),
        )

        expected = Resources(
            url_files=[
                UrlFile(
                    local_path='/path/to/url/file',
                    url='url',
                ),
            ],
        )

        new_state = self.component_translator.convert_resources(beta_component)
        resources = new_state.resources
        self.assertEqual(resources, expected)

    def test_override_parent(self):
        beta_component = BetaComponent(
            patch=Patch(
                resources=[
                    Patch.Resource(
                        manage_type=Patch.Resource.ManageType.STATIC_CONTENT,
                        local_path='/path/to/override/static_file',
                        content='new static file content',
                    ),
                ],
            ),
        )

        parent_state = CurrentComponentState(
            resources=Resources(
                static_files=[
                    StaticFile(
                        local_path='/path/to/override/static_file',
                        content='old static file content',
                    ),
                ],
            ),
        )

        expected = Resources(
            static_files=[
                StaticFile(
                    local_path='/path/to/override/static_file',
                    content='new static file content',
                ),
            ],
        )

        new_state = self.component_translator.convert_resources(beta_component, parent_state)
        resources = new_state.resources
        self.assertEqual(resources, expected)

    def test_clear_deploy_secrets_no_parent(self):
        beta_component = BetaComponent()
        secret_key = 'secret-key'
        beta_component.current_state.resources.secrets[secret_key].secret_id = 'secret_id'
        beta_component.current_state.resources.secrets[secret_key].secret_version = 'secret_version'

        new_state = self.component_translator.convert_resources(beta_component)
        result = new_state.resources.secrets
        expected = Resources().secrets

        self.assertEqual(result, expected)

    def test_clear_deploy_secrets_no_parent_translate(self):
        beta_component = BetaComponent(translate_secrets=True)
        secret_key = 'secret-key'
        beta_component.current_state.resources.secrets[secret_key].secret_id = 'secret_id'
        beta_component.current_state.resources.secrets[secret_key].secret_version = 'secret_version'

        new_state = self.component_translator.convert_resources(beta_component)
        result = new_state.resources.secrets
        expected = Resources().secrets

        self.assertEqual(result, expected)

    def test_translate_deploy_secrets_override(self):
        beta_component = BetaComponent(translate_secrets=True)
        secret_key = 'secret-key'
        beta_component.current_state.resources.secrets[secret_key].secret_id = 'secret_id'
        beta_component.current_state.resources.secrets[secret_key].secret_version = 'secret_version'

        parent = CurrentComponentState()
        parent_secret_key = 'parent-secret'
        parent.resources.secrets[parent_secret_key].secret_id = 'parent_secret_id'
        parent.resources.secrets[parent_secret_key].secret_version = 'parent_secret_version'

        resources = Resources()
        resources.secrets[parent_secret_key].CopyFrom(parent.resources.secrets[parent_secret_key])
        expected = resources.secrets

        new_state = self.component_translator.convert_resources(beta_component, parent)
        result = new_state.resources.secrets

        self.assertEqual(result, expected)

    def test_dont_translate_deploy_secrets(self):
        beta_component = BetaComponent()
        secret_key = 'secret-key'
        beta_component.current_state.resources.secrets[secret_key].secret_id = 'secret_id'
        beta_component.current_state.resources.secrets[secret_key].secret_version = 'secret_version'

        parent = CurrentComponentState()
        parent_secret_key = 'parent-secret'
        parent.resources.secrets[parent_secret_key].secret_id = 'parent_secret_id'
        parent.resources.secrets[parent_secret_key].secret_version = 'parent_secret_version'

        new_state = self.component_translator.convert_resources(beta_component, parent)
        result = new_state.resources.secrets
        expected = Resources().secrets

        self.assertEqual(result, expected)


class TestTranslatorMergeContainers(TestCase):

    def merge(self, containers, patches):
        # type: (List[Container], List[Container]) -> List[Container]
        return ComponentTranslator.merge_instance_spec_containers(containers, patches, 'name')

    def get_container_env(self, containers, cname, env_name):
        # type: (List[Container], AnyStr, AnyStr) -> EnvVar
        for c in containers:
            if c.name != cname:
                continue
            for v in c.env:
                if v.name == env_name:
                    return v

    def get_container_cmd(self, containers, cname):
        # type: (List[Container], AnyStr) -> List[AnyStr]
        for c in containers:
            if c.name == cname:
                return c.command

    def get_container_unistat(self, containers, cname):
        # type: (List[Container], AnyStr) -> List[YasmUnistatEndpoint]
        for c in containers:
            if c.name == cname:
                return c.unistat_endpoints

    def get_container_cdump(self, containers, cname):
        # type: (List[Container], AnyStr) -> CoredumpPolicy
        for c in containers:
            if c.name == cname:
                return c.coredump_policy

    def literal_env_var(self, name, value):
        # type: (AnyStr, AnyStr) -> EnvVar
        return EnvVar(
            name=name,
            value_from=EnvVarSource(
                type=EnvVarSource.SourceType.LITERAL_ENV,
                literal_env=LiteralEnvSelector(value=value),
            ),
        )

    def test_override_env(self):
        cname = 'name'
        var_name = 'var'
        container = Container(
            name=cname,
            env=[self.literal_env_var(var_name, 'old_val')],
        )

        patch_container = Container(
            name=cname,
            env=[self.literal_env_var(var_name, 'new_val')],
        )

        expected_env = EnvVar()
        expected_env.CopyFrom(patch_container.env[0])

        result = self.merge([container], [patch_container])

        new_env = self.get_container_env(result, cname, var_name)

        self.assertEqual(new_env, expected_env)

    def test_keep_env(self):
        cname = 'name'
        var_name = 'var'
        container = Container(
            name=cname,
            env=[self.literal_env_var(var_name, 'old_val')],
        )

        patch_container = Container(
            name=cname,
            env=[self.literal_env_var('new_var', 'new_val')],
        )

        expected_env = EnvVar()
        expected_env.CopyFrom(container.env[0])

        result = self.merge([container], [patch_container])

        new_env = self.get_container_env(result, cname, var_name)

        self.assertEqual(new_env, expected_env)

    def test_add_env(self):
        cname = 'name'
        var_name = 'var'
        new_var_name = 'new_var'
        container = Container(
            name=cname,
            env=[self.literal_env_var(var_name, 'old_val')],
        )

        patch_container = Container(
            name=cname,
            env=[self.literal_env_var(new_var_name, 'new_val')],
        )

        expected_env = EnvVar()
        expected_env.CopyFrom(patch_container.env[0])

        result = self.merge([container], [patch_container])

        new_env = self.get_container_env(result, cname, new_var_name)

        self.assertEqual(new_env, expected_env)

    def test_override_cmd(self):
        cname = 'name'
        new_cmd = ['another_cmd', 'arg']
        container = Container(name=cname, command=['cmd', 'arg1', 'arg2'])
        patch_container = Container(name=cname, command=new_cmd)
        expected_cmd = list(new_cmd)

        result = self.merge([container], [patch_container])
        cmd = self.get_container_cmd(result, cname)

        self.assertEqual(cmd, expected_cmd)

    def test_keep_cmd(self):
        cname = 'name'
        cmd = ['cmd', 'arg1', 'arg2']
        container = Container(name=cname, command=cmd)
        patch_container = Container(name=cname, env=[self.literal_env_var('var', 'val')])
        expected_cmd = list(cmd)

        result = self.merge([container], [patch_container])
        result_cmd = self.get_container_cmd(result, cname)

        self.assertEqual(result_cmd, expected_cmd)

    def test_override_unistat(self):
        cname = 'name'
        container = Container(name=cname, unistat_endpoints=[YasmUnistatEndpoint(port='1234', path='path')])
        patch = Container(name=cname, unistat_endpoints=[YasmUnistatEndpoint(port='4321', path='new_path')])
        expected = list(map(MessageToDict, patch.unistat_endpoints))

        result = self.merge([container], [patch])
        result_unistat = list(map(MessageToDict, self.get_container_unistat(result, cname)))

        self.assertEqual(result_unistat, expected)

    def test_keep_unistat(self):
        cname = 'name'
        container = Container(name=cname, unistat_endpoints=[YasmUnistatEndpoint(port='1234', path='path')])
        patch = Container(name=cname, env=[self.literal_env_var('var', 'val')])
        expected = list(map(MessageToDict, container.unistat_endpoints))

        result = self.merge([container], [patch])
        result_unistat = list(map(MessageToDict, self.get_container_unistat(result, cname)))

        self.assertEqual(result_unistat, expected)

    def test_override_cdump_policy(self):
        cname = 'name'
        container = Container(
            name=cname,
            coredump_policy=CoredumpPolicy(
                type=CoredumpPolicy.Type.COREDUMP,
                coredump_processor=CoredumpProcessor(path='path'),
            ),
        )
        patch = Container(
            name=cname,
            coredump_policy=CoredumpPolicy(
                type=CoredumpPolicy.Type.CUSTOM_CORE_COMMAND,
                custom_processor=CustomCommandCoredumpProcessor(command='cmd arg1 arg2'),
            ),
        )
        expected = CoredumpPolicy()
        expected.CopyFrom(patch.coredump_policy)

        result = self.merge([container], [patch])
        result_policy = self.get_container_cdump(result, cname)

        self.assertEqual(result_policy, expected)

    def test_keep_cdump_policy(self):
        cname = 'name'
        container = Container(
            name=cname,
            coredump_policy=CoredumpPolicy(
                type=CoredumpPolicy.Type.COREDUMP,
                coredump_processor=CoredumpProcessor(path='path'),
            ),
        )
        patch = Container(name=cname, env=[self.literal_env_var('var', 'val')])
        expected = CoredumpPolicy()
        expected.CopyFrom(container.coredump_policy)

        result = self.merge([container], [patch])
        result_policy = self.get_container_cdump(result, cname)

        self.assertEqual(result_policy, expected)

    def test_remove_cdump_policy(self):
        cname = 'name'
        container = Container(
            name=cname,
            coredump_policy=CoredumpPolicy(
                type=CoredumpPolicy.Type.COREDUMP,
                coredump_processor=CoredumpProcessor(path='path'),
            ),
        )
        patch = Container(name=cname, coredump_policy=CoredumpPolicy(deleted=True))

        result = self.merge([container], [patch])
        result_policy = self.get_container_cdump(result, cname)

        self.assertEqual(result_policy.ByteSize(), 0)


class TestTranslatorConvertInstanceSpec(TestCase):
    @classmethod
    def setUpClass(cls):
        config = get_test_config()
        cls.component_translator = ComponentTranslator(config)
        cls._setup_test_data()

    @classmethod
    def _setup_test_data(cls):
        pass

    def test_convert_instance_spec(self):
        beta_component = BetaComponent(
            patch=Patch(
                parent_external_id='parent',
                instance_spec=InstanceSpec(
                    docker_image=DockerImage(
                        registry='yandex',
                        name='docker_name',
                    ),
                    containers=[
                        Container(
                            name='test',
                            env=[
                                EnvVar(
                                    name='env_0',
                                    value_from=EnvVarSource(
                                        type=EnvVarSource.SourceType.SECRET_ENV,
                                        secret_env=SecretEnvSelector(
                                            secret_name='secret_0',
                                            keychain_secret=KeychainSecret(
                                                keychain_id='keychain_0',
                                                secret_id='secret_0',
                                                secret_revision_id='revision_0',
                                            ),
                                        ),
                                    ),
                                ),
                                EnvVar(
                                    name='env_1',
                                    value_from=EnvVarSource(
                                        type=EnvVarSource.SourceType.SECRET_ENV,
                                        secret_env=SecretEnvSelector(
                                            secret_name='secret_1',
                                            keychain_secret=KeychainSecret(
                                                keychain_id='keychain_1',
                                                secret_id='secret_1',
                                                secret_revision_id='revision_1',
                                            ),
                                        ),
                                    ),
                                ),
                                EnvVar(
                                    name='env_2',
                                ),
                            ],
                        ),
                    ],
                ),
            ),
            current_state=CurrentComponentState(),
            slot=Slot(
                type=Slot.Type.NANNY,
                id='slot_id_0',
            ),
        )
        parent_component = BetaComponent(
            current_state=CurrentComponentState(
                instance_spec=InstanceSpec(
                    containers=[
                        Container(
                            name='test',
                            command=[
                                'command_1',
                                'echo',
                                'command_2',
                            ],
                            env=[
                                EnvVar(
                                    name='env_0',
                                    value_from=EnvVarSource(
                                        type=EnvVarSource.SourceType.SECRET_ENV,
                                        secret_env=SecretEnvSelector(
                                            secret_name='not_env',
                                            keychain_secret=KeychainSecret(
                                                keychain_id='not_env',
                                                secret_id='not_env',
                                                secret_revision_id='not_env',
                                            ),
                                        ),
                                    ),
                                ),
                                EnvVar(
                                    name='env_2',
                                ),
                            ],
                        ),
                    ],
                ),
            ),
            slot=Slot(
                type=Slot.Type.NANNY,
                id='parent',
            ),
        )
        res_target_state = TargetComponentState(
            instance_spec=InstanceSpec(
                instancectl=InstanceCtl(),
                docker_image=DockerImage(
                    registry='yandex',
                    name='docker_name',
                ),
                containers=[
                    Container(
                        name='test',
                        command=[
                            'command_1',
                            'echo',
                            'command_2',
                        ],
                        env=[
                            EnvVar(
                                name='env_0',
                                value_from=EnvVarSource(
                                    type=EnvVarSource.SourceType.SECRET_ENV,
                                    secret_env=SecretEnvSelector(
                                        secret_name='secret_0',
                                        keychain_secret=KeychainSecret(
                                            keychain_id='keychain_0',
                                            secret_id='secret_0',
                                            secret_revision_id='revision_0',
                                        ),
                                    ),
                                ),
                            ),
                            EnvVar(
                                name='env_1',
                                value_from=EnvVarSource(
                                    type=EnvVarSource.SourceType.SECRET_ENV,
                                    secret_env=SecretEnvSelector(
                                        secret_name='secret_1',
                                        keychain_secret=KeychainSecret(
                                            keychain_id='keychain_1',
                                            secret_id='secret_1',
                                            secret_revision_id='revision_1',
                                        ),
                                    ),
                                ),
                            ),
                            EnvVar(
                                name='env_2',
                            ),
                        ],
                    ),
                ],
            ),
        )

        new_state = TargetComponentState()
        self.component_translator.convert_instance_spec(beta_component, [], new_state, parent_component.current_state)
        self.assertEqual(states_are_equal(new_state, res_target_state), True)

    def test_convert_cdump_policy(self):
        component = BetaComponent()
        component.patch.copy_coredump_policy = True
        parent = CurrentComponentState(
            instance_spec=InstanceSpec(
                containers=[
                    Container(
                        name='name',
                        coredump_policy=CoredumpPolicy(
                            type=CoredumpPolicy.Type.COREDUMP,
                            coredump_processor=CoredumpProcessor(path='path'),
                        ),
                    ),
                ],
            ),
        )
        expected = CoredumpPolicy()
        expected.CopyFrom(parent.instance_spec.containers[0].coredump_policy)

        state = TargetComponentState()
        self.component_translator.convert_instance_spec(component, [], state, parent)
        policy = state.instance_spec.containers[0].coredump_policy

        self.assertEqual(policy, expected)

    def test_dont_convert_cdump_policy(self):
        component = BetaComponent()
        component.patch.copy_coredump_policy = False
        parent = CurrentComponentState(
            instance_spec=InstanceSpec(
                containers=[
                    Container(
                        name='name',
                        coredump_policy=CoredumpPolicy(
                            type=CoredumpPolicy.Type.COREDUMP,
                            coredump_processor=CoredumpProcessor(path='path'),
                        ),
                    ),
                ],
            ),
        )
        state = TargetComponentState()
        self.component_translator.convert_instance_spec(component, [], state, parent)
        policy = state.instance_spec.containers[0].coredump_policy

        self.assertEqual(policy.ByteSize(), 0)
