# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.diff import index_by_attr, index_by_attrs, index_by_key, index_by_keys
from search.martylib.test_utils import TestCase
from search.martylib.protobuf_utils import replace_in_repeated

from search.priemka.yappy.src.yappy_lib.diff import states_are_equal
from search.priemka.yappy.proto.structures.state_pb2 import CurrentComponentState
from search.priemka.yappy.proto.structures.resources_pb2 import Shard
from search.priemka.yappy.proto.structures.sandbox_pb2 import SandboxFile
from search.priemka.yappy.proto.structures.auth_pb2 import Role, StaffUnion

from test_yappy_lib.test_yappy_lib.parent_components import RESOURCE_MAP


class Object(object):
    def __init__(self, t1, t2, t3):
        self.t1 = t1
        self.t2 = t2
        self.t3 = t3

    def __eq__(self, other):
        if not isinstance(other, Object):
            raise NotImplementedError

        return self.t1 == other.t1 and self.t2 == other.t2 and self.t3 == other.t3


class TestDiff(TestCase):
    def setUp(self):
        self.o1 = Object(t1=1, t2=2, t3=3)
        self.o2 = Object(t1=4, t2=4, t3=5)
        self.o3 = Object(t1=7, t2=8, t3=9)
        self.o4 = Object(t1=7, t2=8, t3=9)
        self.o5 = Object(t1=11, t2=12, t3=13)
        self.o6 = Object(t1=11, t2=12, t3=14)

    def test_index_by_attr(self):
        map_ = index_by_attr([self.o1, self.o2, self.o3], 't1')
        self.assertEqual(map_, {1: self.o1, 4: self.o2, 7: self.o3})

    def test_index_by_attrs(self):
        map_ = index_by_attrs([self.o1, self.o2, self.o3], ['t1', 't2'])
        self.assertEqual(map_, {(1, 2): self.o1, (4, 4): self.o2, (7, 8): self.o3})

    def test_index_by_key(self):
        o1 = dict(t1=1, t2=2, t3=3)
        o2 = dict(t1=4, t2=4, t3=5)
        o3 = dict(t1=7, t2=8, t3=9)

        map_ = index_by_key([o1, o2, o3], 't1')
        self.assertEqual(map_, {1: o1, 4: o2, 7: o3})

    def test_index_by_keys(self):
        o1 = dict(t1=1, t2=2, t3=3)
        o2 = dict(t1=4, t2=4, t3=5)
        o3 = dict(t1=7, t2=8, t3=9)

        map_ = index_by_keys([o1, o2, o3], ['t1', 't2'])
        self.assertEqual(map_, {(1, 2): o1, (4, 4): o2, (7, 8): o3})

    @staticmethod
    def _create_two_equal_current_states():
        a = CurrentComponentState()
        a.resources.sandbox_files.extend(RESOURCE_MAP['parent2'])

        b = CurrentComponentState()
        b.resources.sandbox_files.extend(RESOURCE_MAP['parent3'])

        return a, b

    def test_compare_states(self):
        a, b = self._create_two_equal_current_states()

        # Parent 2 and 3 have the same resources shuffled in a protobuf way.
        # We still not sure how exactly protobuf shuffles them, though.
        self.assertTrue(states_are_equal(a, b))

        b.resources.sandbox_files.pop()
        self.assertFalse(states_are_equal(a, b))

    def test_compare_states_with_changed_shardmap(self):
        a, b = self._create_two_equal_current_states()

        self.assertFalse(a.resources.HasField('shard'))

        self.assertTrue(states_are_equal(a, b))

        # assert that state comparison has no side effect of initializing shard
        self.assertFalse(a.resources.HasField('shard'))

        sample_shard = Shard(
            storage='/ssd',
            shard_type=Shard.ShardType.SANDBOX_SHARDMAP,
            sandbox_file=SandboxFile(
                task_type='my-task-type',
                task_id='my-task-id',
                resource_type='my-resource-type'
            )
        )

        a.resources.shard.CopyFrom(sample_shard)
        self.assertTrue(a.resources.HasField('shard'))

        self.assertFalse(states_are_equal(a, b))

        b.resources.shard.CopyFrom(sample_shard)

        self.assertTrue(states_are_equal(a, b))

        sample_shard.local_path = 'my-shard'
        b.resources.shard.CopyFrom(sample_shard)

        self.assertFalse(states_are_equal(a, b))

    def test_compare_states_with_changed_auth_attrs(self):
        a, b = self._create_two_equal_current_states()

        sample_roles = [
            Role(
                type=Role.Type.OWNER,
                staff=StaffUnion(
                    groups=[
                        999999999,
                    ],
                    logins=[
                        'me',
                        'robot-yappy',
                    ],
                ),
            ),
        ]

        replace_in_repeated(a.roles, sample_roles)
        self.assertFalse(states_are_equal(a, b, compare_roles=True))
        self.assertTrue(states_are_equal(a, b))

        replace_in_repeated(b.roles, sample_roles)
        self.assertTrue(states_are_equal(a, b, compare_roles=True))

        sample_roles[0].staff.groups[0] = 1
        replace_in_repeated(a.roles, sample_roles)
        self.assertFalse(states_are_equal(a, b, compare_roles=True))
        self.assertTrue(states_are_equal(a, b))

    def compare_states_with_logically_equal_roles(self):
        a, b = self._create_two_equal_current_states()

        roles_a = [
            Role(
                type=Role.Type.OWNER,
                staff=StaffUnion(
                    groups=[
                        999999999,
                        7,
                    ],
                    logins=[
                        'me',
                        'robot-yappy',
                    ],
                ),
            ),
            Role(
                type=Role.Type.OWNER,
                staff=StaffUnion(
                    groups=[],
                    logins=[
                        'robot-yappy',
                        'somebody',
                    ],
                ),
            ),
        ]
        replace_in_repeated(a.roles, roles_a)

        roles_b = [
            Role(
                type=Role.Type.OWNER,
                staff=StaffUnion(
                    groups=[
                        7,
                        999999999,
                    ],
                    logins=[
                        'me',
                        'robot-yappy',
                        'somebody',
                    ],
                ),
            ),
        ]
        replace_in_repeated(b.roles, roles_b)

        self.assertTrue(states_are_equal(a, b, compare_roles=True))
