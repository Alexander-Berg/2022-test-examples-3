# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

from collections import defaultdict
from google.protobuf.json_format import MessageToDict
from google.protobuf.message import Message
from randomproto import randproto
from requests.models import Response
from typing import AnyStr

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.martylib.core.date_utils import now
from search.martylib.http.exceptions import BadRequest
from search.martylib.protobuf_utils.repeated import replace_in_repeated
from search.martylib.protobuf_utils import clear_empty_fields

from search.priemka.yappy.src.yappy_lib.nanny import YappyNannyClientMock, DuplicateError
from search.priemka.yappy.src.yappy_lib.sandbox.mock import YappySandboxClientMock
from search.priemka.yappy.proto.structures.auth_pb2 import Role, StaffUnion
from search.priemka.yappy.proto.structures.resources_pb2 import (
    Container,
    CoredumpPolicy,
    InstanceSpec,
    LocalSandboxFile,
    Resources,
    Shard,
    StaticFile,
)
from search.priemka.yappy.proto.structures.sandbox_pb2 import SandboxFile

from search.priemka.yappy.tests.utils.test_cases import ServerTestCase, TestCase


class TestNannyClient(ServerTestCase):
    def setUp(self):
        _now = int(now().timestamp())
        self.nanny = YappyNannyClientMock()
        self.nanny.load_services(
            {
                'my-service': {
                    'auth_attrs': {
                        '_id': 'snapshot-id',
                        'content': {
                            'owners': {
                                'groups': [],
                                'logins': [],
                            }
                        }
                    },
                    'runtime_attrs': {
                        '_id': 'snapshot-id',
                        'content': {
                            'resources': {
                                'sandbox_files': [],
                                'static_files': [],
                                'url_files': [],
                            },
                            'instance_spec': {
                                'dockerImage': {
                                    'registry': 'yandex',
                                    'name': 'docker_name',
                                },
                                'instancectl': {},
                            }
                        }
                    }
                }
            },
        )
        self.nanny.load_services(
            {
                'service-1': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/mock',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'OFFLINE',
                            },
                        },
                    },
                },
                'service-2': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/mock',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'ONLINE',
                                'entered': (_now - 600) * 1000,
                            },
                        },
                    },
                },
                'service-3': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/mock',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'OFFLINE',
                            },
                        },
                    },
                },
                'service-4': {
                    'info_attrs': {
                        'content': {
                            'category': '/category/mock-1',
                        },
                    },
                    'current_state': {
                        'content': {
                            'summary': {
                                'value': 'ONLINE',
                            },
                        },
                    },
                },
            }
        )

    def test_update_resource_sandbox_files(self):
        self.nanny.update_resources(
            'my-service',
            Resources(
                sandbox_files=[
                    LocalSandboxFile(
                        local_path='file.txt',
                        is_dynamic=False,
                        sandbox_file=SandboxFile(
                            task_type='my-task-type',
                            task_id='123',
                            resource_type='my-resource-type',
                            rbtorrent='rbtorrent://123',
                        )
                    )
                ]
            ),
            [],
            'update for test'
        )

        self.assertEqual(2, self.nanny.mocker.call_count)
        self.assertEqual(
            {
                'content': {
                    'sandbox_files': [
                        {
                            'is_dynamic': False,
                            'local_path': 'file.txt',
                            'resource_id': '',
                            'resource_type': 'my-resource-type',
                            'task_id': '123',
                            'task_type': 'my-task-type',
                            'storage': '',
                        }
                    ],
                    'static_files': [],
                    'url_files': [],
                },
                'comment': 'update for test',
            },
            self.nanny.mocker.last_request.json()
        )

    def test_update_resources_static_files(self):
        self.nanny.update_resources(
            'my-service',
            Resources(
                static_files=[
                    StaticFile(
                        is_dynamic=False,
                        local_path='file.txt',
                        content='blabla content'
                    )
                ]
            ),
            [],
            'update static file for test'
        )

        self.assertEqual(2, self.nanny.mocker.call_count)
        self.assertEqual(
            {
                'content': {
                    'sandbox_files': [],
                    'url_files': [],
                    'static_files': [
                        {
                            'is_dynamic': False,
                            'local_path': 'file.txt',
                            'content': 'blabla content',
                            'storage': '',
                        }
                    ],
                },
                'comment': 'update static file for test',
            },
            self.nanny.mocker.last_request.json()
        )

    def test_update_resources_sandbox_shard(self):
        self.nanny.update_resources(
            'my-service',
            Resources(
                shard=Shard(
                    local_path='my-shard',
                    storage='/ssd',
                    shard_type=Shard.ShardType.SANDBOX_SHARD,
                    sandbox_file=SandboxFile(
                        task_type='my-task-type',
                        task_id='123',
                        resource_type='my-resource-type',
                        rbtorrent='rbtorrent://123'
                    )
                )
            ),
            [],
            'update sandbox shard for test'
        )

        self.assertEqual(2, self.nanny.mocker.call_count)
        self.assertEqual(
            {
                'content': {
                    'sandbox_files': [],
                    'static_files': [],
                    'url_files': [],
                    'sandbox_bsc_shard':
                        {
                            'local_path': 'my-shard',
                            'chosen_type': 'SANDBOX_SHARD',
                            'storage': '/ssd',
                            'task_type': 'my-task-type',
                            'task_id': '123',
                            'resource_type': 'my-resource-type'
                        }
                },
                'comment': 'update sandbox shard for test',
            },
            self.nanny.mocker.last_request.json()
        )

    def test_update_resources_sandbox_shardmap(self):
        self.nanny.update_resources(
            'my-service',
            Resources(
                shard=Shard(
                    local_path='my-shard',
                    storage='/ssd',
                    shard_type=Shard.ShardType.SANDBOX_SHARDMAP,
                    sandbox_file=SandboxFile(
                        task_type='my-task-type',
                        task_id='123',
                        resource_type='my-resource-type',
                        rbtorrent='rbtorrent://123',
                    ),
                )
            ),
            [],
            'update sandbox shardmap for test'
        )

        self.assertEqual(2, self.nanny.mocker.call_count)
        self.assertEqual(
            {
                'content': {
                    'sandbox_files': [],
                    'static_files': [],
                    'url_files': [],
                    'sandbox_bsc_shard': {
                        'local_path': 'my-shard',
                        'chosen_type': 'SANDBOX_SHARDMAP',
                        'storage': '/ssd',
                        'sandbox_shardmap': {
                            'task_type': 'my-task-type',
                            'task_id': '123',
                            'resource_type': 'my-resource-type',
                        },
                    },
                },
                'comment': 'update sandbox shardmap for test',
            },
            self.nanny.mocker.last_request.json()
        )

    def test_update_auth_attrs(self):
        self.nanny.update_auth_attrs(
            'my-service',
            [
                Role(
                    type=Role.Type.OWNER,
                    staff=StaffUnion(
                        groups=[
                            999999999,
                        ],
                        logins=[
                            'robot-yappy',
                            'me',
                        ],
                    ),
                ),
            ],
            'update auth attributes for test',
        )

        self.assertEqual(2, self.nanny.mocker.call_count)

        updated_attrs = self.nanny.mocker.last_request.json()
        updated_attrs['content']['owners']['groups'] = list(map(int, updated_attrs['content']['owners']['groups']))
        self.assertEqual(
            {
                'content': {
                    'conf_managers': {u'groups': [], u'logins': []},
                    'observers': {u'groups': [], u'logins': []},
                    'ops_managers': {u'groups': [], u'logins': []},
                    'owners': {
                        'groups': [999999999],
                        'logins': ['me', 'robot-yappy'],
                    },
                },

                'snapshot_id': 'snapshot-id',
                'comment': 'update auth attributes for test',
            },
            updated_attrs
        )

    def test_list_category(self):
        category = self.nanny.list_category('/category/mock')
        self.assertEqual(len(category), 3)

    def test_list_category_empty(self):
        category = self.nanny.list_category('/no/such/category')
        self.assertEqual(len(category), 0)

    def test_state_coredump_policy(self):
        coredump_policy = CoredumpPolicy()
        coredump_policy.type = CoredumpPolicy.Type.COREDUMP
        coredump_policy.coredump_processor.path = 'path'
        coredump_policy.coredump_processor.count_limit = 3

        with mock.patch.object(self.nanny, 'get_auth_attrs') as auth_attrs_mock, \
                mock.patch.object(self.nanny, 'get_runtime_attrs') as runtime_mock:
            auth_attrs_mock.return_value = {'content': {'owners': defaultdict(list)}}
            runtime_mock.return_value = {
                'content': {
                    'resources': defaultdict(list),
                    'instance_spec': {
                        'containers': [
                            {"coredumpPolicy": MessageToDict(coredump_policy)},
                        ]
                    }
                },
                'meta_info': {'conf_id': None},  # avoiding NotDeployed
            }

            state = self.nanny.get_state('service', sandbox_client=None)
            result = state.instance_spec.containers[0].coredump_policy

        self.assertEqual(result, coredump_policy)

    @mock.patch('search.priemka.yappy.src.yappy_lib.nanny.NannyClient.copy_service')
    def test_copy_service_duplicate_error(self, copy_service):
        err = BadRequest(response=Response())
        copy_service.side_effect = err
        with mock.patch.object(err.response, 'json') as mocked_err:
            mocked_err.return_value = {'error': self.nanny.DUPLICATE_ERROR}
            self.assertRaises(
                DuplicateError,
                self.nanny.copy_service,
                'src',
                'tgt',
            )

    @mock.patch('search.priemka.yappy.src.yappy_lib.nanny.NannyClient.copy_service')
    def test_copy_service_no_duplicate_error(self, copy_service):
        err = BadRequest(response=Response())
        copy_service.side_effect = err
        with mock.patch.object(err.response, 'json') as mocked_err:
            mocked_err.return_value = {'error': 'some error'}
            try:
                self.nanny.copy_service('src', 'tgt')
            except BadRequest as err:
                self.assertNotIsInstance(err, DuplicateError)
            else:
                self.failureException("copy_service didn't raise expected `BadRequest`")


class NannyClientConveratationsTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        super(NannyClientConveratationsTestCase, cls).setUpClass()
        cls.nanny = YappyNannyClientMock()
        cls.sb = YappySandboxClientMock()

    def ensure_string_field_not_empty(self, msg, field, prefix='dummy-', allow_empty_msg=False):
        # type: (Message, AnyStr, AnyStr, bool) -> None
        if msg.ByteSize() > 0 or allow_empty_msg is False:
            val = getattr(msg, field)
            setattr(msg, field, '{}{}'.format(prefix, val))

    def normalize_container(self, container):
        # type: (Container) -> None
        """ Get rid of randomness that makes put/get put data differ """
        # Sort what should be sorted
        container.unistat_endpoints.sort(key=lambda x: x.port)
        container.env.sort(key=lambda x: x.name)

        # Get rid of dangerous empty strings
        for unistat in container.unistat_endpoints:
            self.ensure_string_field_not_empty(unistat, 'port')

        # Clear Yappy's internal fields
        container.ClearField('deleted')

    def normalize_instance_spec(self, spec):
        # type: (InstanceSpec) -> None
        """ Get rid of randomness that makes put/get put data differ

        Issues fixed by this normalization:
        - some fields get ordered in translation;
        - some fields presence is checked by a specific property (e.g. name or version)
          which supposed to be defined always;
        - some fields are used as dict keys after message-to-dict translation and when missed -- lead to ```KeyError``
        - some fields will not survive the translation for they are Yappy's internals
        - for some fields only a few properties will survive the translation (like in ``init_containers``)
        """
        # Sort what should be sorted
        spec.containers.sort(key=lambda x: x.name)
        spec.volume.sort(key=lambda x: x.name)

        # Get rid of what will get lost in translation
        init_containers = [Container(name=c.name, command=c.command) for c in spec.init_containers]
        replace_in_repeated(spec.init_containers, init_containers)

        # Get rid of dangerous empty strings
        self.ensure_string_field_not_empty(spec.docker_image, 'name', allow_empty_msg=True)
        self.ensure_string_field_not_empty(spec.instancectl, 'version', allow_empty_msg=True)
        for volume in spec.volume:
            self.ensure_string_field_not_empty(volume, 'name')

        # Normalize subfields
        for container in spec.containers:
            self.normalize_container(container)

        clear_empty_fields(spec)

    def test_push_get_instance_spec(self):
        """ ToDo: move data convertors outside update (put) / get methods of Nanny client """
        for _ in range(5):   # random data used, so let's run it a number of times
            random_spec = randproto(InstanceSpec)
            self.normalize_instance_spec(random_spec)

            put_patch = mock.patch.object(self.nanny, 'put_instance_spec')
            get_patch = mock.patch.object(self.nanny, 'get_instance_spec')
            with put_patch as put, get_patch as get:
                get.return_value = defaultdict(dict)
                self.nanny.update_instance_spec('dummy', random_spec, 'comment')
                instance_spec_passed = put.call_args.args[1]

            get_runtime_patch = mock.patch.object(self.nanny, 'get_runtime_attrs')
            get_auth_patch = mock.patch.object(self.nanny, 'get_auth_attrs')
            sb_patch = mock.patch.object(self.sb, 'get_resource_proto')
            with get_runtime_patch as get_runtime, get_auth_patch as get_auth, sb_patch as get_sb_resource:
                get_auth.return_value = {'content': {'owners': defaultdict(list)}}
                get_runtime.return_value = {
                    'meta_info': {'conf_id': None},    # avoiding NotDeployed
                    'content': {
                        'resources': defaultdict(list),
                        'instance_spec': instance_spec_passed['content'],
                    }
                }
                get_sb_resource.return_value = random_spec.instancectl.sandbox_file
                state = self.nanny.get_state('dummy', sandbox_client=self.sb)
                result_spec = state.instance_spec

            clear_empty_fields(result_spec)
            self.assertEqual(result_spec, random_spec)
