#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.access.server.proto.consumer_pb2 import TConsumerNode, TConsumer
from market.pylibrary.lite.matcher import Capture, CaptureTo, EmptyDict


Cr = TConsumerNode.TState.TResource
Cv = TConsumerNode.TState.TResource.TVersion
Tr = TConsumerNode.TTarget.TResource
Tv = TConsumerNode.TTarget.TResource.TVersion

TResourceOptions = TConsumer.TOptions.TResource

LARGE_TIMEOUT=300


class T(env.AccessSuite):
    def test_schedule(self):
        self.access.create_publisher(name='miner')
        self.access.create_resource(publisher_name='miner', name='gold')
        self.access.create_version(resource_name='gold')
        self.access.create_consumer('argonavt')
        self.access.update_consumer_node('0', 'argonavt')

        response = self.access.get_consumer_node('0', 'argonavt')
        self.assertFragmentIn(response, {'state': {'resource': {}}})

        # test that scheduler orders to install & download desired resource

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt',
            state={'resource': {'gold': Cr(wait={'for_install': True})}}
        )

        self._poll_node_target(
            consumer_name='argonavt',
            node_id='0',
            prev_target=EmptyDict(),
            next_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        )

        # test that scheduler orders to download fresh version of resource
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt',
            state={'resource': {
                'gold': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True)},
                    in_install={'1.0.0': Cv(done=True)},
                    in_load={'1.0.0': Cv(done=True)}
                )
            }}
        )

        self.access.create_version(resource_name='gold')

        self._poll_node_target(
            consumer_name='argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}},
            next_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        )

        # test that scheduler orders to install rollbacked version (1.0.0)
        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt',
            state={'resource': {
                'gold': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True), '2.0.0': Cv(done=True)},
                    in_install={'2.0.0': Cv(done=True)},
                    in_load={'2.0.0': Cv(done=True)}
                )
            }}
        )

        self.access.update_consumer(
            name='argonavt',
            options={
                'resource': {
                    'gold': TResourceOptions(update={'version': '1.0.0'})
                }
            }
        )

        self._poll_node_target(
            consumer_name='argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}},
            next_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        )

        # test that scheduler updates to latest version after forced left
        self.access.update_consumer(
            name='argonavt',
            options={}
        )

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt',
            state={'resource': {
                'gold': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True), '2.0.0': Cv(done=True)},
                    in_install={'1.0.0': Cv(done=True)},
                    in_load={'1.0.0': Cv(done=True)}
                )
            }}
        )

        self._poll_node_target(
            consumer_name='argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}},
            next_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}}
        )

        # test that scheduler compacts extra achives
        self.access.update_consumer(
            name='argonavt',
            options={
                'resource': {'gold': TResourceOptions(retention={'download_count': {'value': 2}})}
            }
        )

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt',
            state={'resource': {
                'gold': Cr(
                    wait={'for_install': True},
                    in_download={'1.0.0': Cv(done=True), '2.0.0': Cv(done=True)},
                    in_install={'2.0.0': Cv(done=True)},
                    in_load={'2.0.0': Cv(done=True)}
                )
            }}
        )

        self.access.create_version(resource_name='gold')
        self._poll_node_target(
            consumer_name='argonavt',
            node_id='0',
            prev_target={'gold': {'to_install': {'2.0.0': {}}, 'to_download': {'1.0.0': {}, '2.0.0': {}}}},
            next_target={'gold': {'to_install': {'3.0.0': {}}, 'to_download': {'2.0.0': {}, '3.0.0': {}}}}
        )

    def test_multi_consumers(self):
        self.access.create_publisher(name='miner2')
        self.access.create_resource(publisher_name='miner2', name='silver')
        self.access.create_version(resource_name='silver')
        self.access.create_consumer('argonavt2')
        self.access.create_consumer('argonavt3')

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt2',
            state={'resource': {'silver': Cr(wait={'for_install': True})}}
        )

        self.access.update_consumer_node(
            node_id='0',
            consumer_name='argonavt3',
            state={'resource': {'silver': Cr(wait={'for_install': True})}}
        )

        self._poll_node_target(
            consumer_name='argonavt2',
            node_id='0',
            prev_target=EmptyDict(),
            next_target={'silver': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        )

        self._poll_node_target(
            consumer_name='argonavt3',
            node_id='0',
            prev_target=EmptyDict(),
            next_target={'silver': {'to_install': {'1.0.0': {}}, 'to_download': {'1.0.0': {}}}}
        )

    def _check_version(self, consumer_name, node_id, resources, version, allow_different_len):
        response = self.access.get_consumer_node(
            consumer_name=consumer_name,
            node_id=node_id
        )
        success = True
        for resource in resources:
            has, _ = response.contains(
                {resource: {'to_download': {version: {}}}},
                allow_different_len=allow_different_len
            )
            success = success and has
            if not success:
                break
        return success

    def _wait_for_version(self, consumer_name, node_id, resources, version, allow_different_len=True, timeout=0):
        poll_once = lambda: self._check_version(consumer_name, node_id, resources, version, allow_different_len)
        self.assertTrue(self._wait_ok(timeout, poll_once))

    def _check_resource_lock(self, resource, version, prev_lock_type, next_lock_type):
        assert prev_lock_type != next_lock_type, 'Lock types must be different'
        response = self.access.get_version(resource, version)
        lock_type = Capture()
        has, _ = response.contains({'lock': CaptureTo(lock_type)})
        if not has or lock_type.value == prev_lock_type:
            return False
        self.assertEqual(lock_type.value, next_lock_type)
        return True

    def _poll_resource_lock(self, resource, version, prev_lock_type, next_lock_type, timeout=0):
        poll_once = lambda: self._check_resource_lock(resource, version, prev_lock_type, next_lock_type)
        self.assertTrue(self._wait_ok(timeout, poll_once))


if __name__ == '__main__':
    env.main()
