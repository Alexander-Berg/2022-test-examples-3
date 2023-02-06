#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.access.server.mt.env as env
from market.access.server.proto.consumer_pb2 import TConsumerNode, TConsumer, EDSTATE_FINISHED
from market.pylibrary.lite.matcher import EmptyDict


Cr = TConsumerNode.TState.TResource
Cv = TConsumerNode.TState.TResource.TVersion
Tr = TConsumerNode.TTarget.TResource
Tv = TConsumerNode.TTarget.TResource.TVersion

TResourceOptions = TConsumer.TOptions.TResource
TUpdateOptions = TConsumer.TOptions.TGlobal.TUpdate


class T(env.AccessSuite):
    def test_same_consumer_deploy(self):
        self.access.create_publisher(name='solo_miner')
        self.access.create_resource(publisher_name='solo_miner', name='solo_gold')
        self.access.create_resource(publisher_name='solo_miner', name='solo_silver')
        self.access.create_resource(publisher_name='solo_miner', name='solo_bronze')
        self.access.create_consumer('solo_seeker')
        self.access.create_consumer_group('solo_group', {
            'solo_gold' : 'solo_seeker',
            'solo_silver' : 'solo_seeker',
            'solo_bronze' : 'solo_seeker'})

        self.access.create_version(resource_name='solo_gold')
        self.access.create_version(resource_name='solo_silver', dependencies=[('solo_gold', '1.0.0')])
        self.access.create_version(resource_name='solo_bronze', dependencies=[('solo_silver', '1.0.0')])

        nodes = ['solo_0', 'solo_1', 'solo_2']
        resources = ['solo_gold', 'solo_silver', 'solo_bronze']
        consumer = 'solo_seeker'

        for node in nodes:
            self.access.update_consumer_node(node, consumer)
            response = self.access.get_consumer_node(node, consumer)
            self.assertFragmentIn(response, {'state': {'resource': {}}})

        # Signal that nodes are watching for updates
        current_state = {
            'solo_gold': Cr(wait={'for_watch': True}),
            'solo_silver': Cr(wait={'for_watch': True}),
            'solo_bronze': Cr(wait={'for_watch': True})
        }

        for node in nodes:
            self.access.update_consumer_node(
                node_id=node,
                consumer_name=consumer,
                state={'resource': current_state}
            )

        # Wait nodes deploy first version
        for resource in resources:
            consumers = self._make_solo_consumers(consumer, resource, nodes)
            self._wait_consumer_target(consumer, consumers, prev_version=None, next_version='1.0.0', allow_different_len=True)
            self._set_consumer_state(consumer, consumers, '1.0.0', current_state)

        self._poll_group_deployments_finished('solo_group', 1)

        gold_consumers = self._make_solo_consumers(consumer, 'solo_gold', nodes)
        silver_consumers = self._make_solo_consumers(consumer, 'solo_silver', nodes)
        bronze_consumers = self._make_solo_consumers(consumer, 'solo_bronze', nodes)

        # Create new solo deployment versions
        self.access.create_version(resource_name='solo_gold')
        self.access.create_version(resource_name='solo_silver', dependencies=[('solo_gold', '2.0.0')])
        self.access.create_version(resource_name='solo_bronze', dependencies=[('solo_silver', '2.0.0')])

        # test that scheduler orders to install & download desired resource for nodes
        self._wait_consumer_target(consumer, gold_consumers, prev_version='1.0.0', next_version='2.0.0')

        # test that other consumers does not received target
        for resource in ['solo_silver', 'solo_bronze']:
            consumers = self._make_solo_consumers(consumer, resource, nodes)
            self._check_consumer_does_not_have_target(consumer, consumers, prev_version='1.0.0', next_version='2.0.0')

        # test that scheduler moves to next consumer
        self._set_consumer_state(consumer, gold_consumers, '2.0.0', current_state)

        # Meanwhile lets create new juicy versions
        self.access.create_version(resource_name='solo_gold')
        self.access.create_version(resource_name='solo_silver', dependencies=[('solo_gold', '3.0.0')])
        self.access.create_version(resource_name='solo_bronze', dependencies=[('solo_silver', '3.0.0')])

        # test that scheduler orders to install & download desired resource for solo_silver_seeker nodes
        self._wait_consumer_target(consumer, silver_consumers, prev_version='1.0.0', next_version='2.0.0')

        # but it is not the time for bronze
        self._check_consumer_does_not_have_target(consumer, bronze_consumers, prev_version='1.0.0', next_version='2.0.0')

        # and nobody rush for newest version
        self._check_consumer_does_not_have_target(consumer, gold_consumers, prev_version='2.0.0', next_version='3.0.0')

        # lets finish with this solo deployment
        self._set_consumer_state(consumer, silver_consumers, '2.0.0', current_state)
        self._wait_consumer_target(consumer, bronze_consumers, prev_version='1.0.0', next_version='2.0.0')
        self._set_consumer_state(consumer, bronze_consumers, '2.0.0', current_state)

        # and expect that new deployment has come
        self._wait_consumer_target(consumer, gold_consumers, prev_version='2.0.0', next_version='3.0.0')

    def _make_target(self, resource, version):
        if version is None:
            return EmptyDict()
        return {resource : {'to_install': {version: {}}, 'to_download': {version: {}}}}

    def _wait_consumer_target(self, consumer_name, consumers, prev_version, next_version, allow_different_len=False):
        resource = consumers[consumer_name]['resource']
        for node in consumers[consumer_name]['nodes']:
            self._poll_node_target(
                consumer_name=consumer_name,
                node_id=node,
                prev_target=self._make_target(resource, prev_version),
                next_target=self._make_target(resource, next_version),
                allow_different_len=allow_different_len
            )

    def _check_consumer_does_not_have_target(self, consumer_name, consumers, prev_version=None, next_version=None):
        resource = consumers[consumer_name]['resource']
        nodes = consumers[consumer_name]['nodes']
        for node in nodes:
            self.assertFalse(self._poll_node_target_once(
                consumer_name=consumer_name,
                node_id=node,
                prev_target=self._make_target(resource, prev_version),
                next_target=self._make_target(resource, next_version),
                allow_different_len=False))

    def _set_consumer_state(self, consumer_name, consumers, version, current_state={}):
        resource = consumers[consumer_name]['resource']
        current_state[resource] = Cr(
            wait={'for_watch': True},
            in_download={version: Cv(done=True)},
            in_install={version: Cv(done=True)},
            in_load={version: Cv(done=True)}
        )
        for node in consumers[consumer_name]['nodes']:
            self.access.update_consumer_node(
                node_id=node,
                consumer_name=consumer_name,
                state={'resource': current_state}
            )

    def _poll_group_deployments_finished_once(self, consumer_group, deploy_count):
        response = self.access.list_group_deployments(consumer_group).obj
        if len(response.deployment) != deploy_count:
            return False
        for dep in response.deployment:
            if dep.state != EDSTATE_FINISHED:
                return False
        return True

    def _poll_group_deployments_finished(self, consumer_group, deploy_count, timeout=0, allowed_errors=True):
        poll_once = lambda: self._poll_group_deployments_finished_once(consumer_group, deploy_count)
        self.assertTrue(self._wait_ok(timeout, poll_once))

    def _make_solo_consumers(self, consumer, resource, nodes):
        consumers = {
            consumer : {'nodes' : nodes, 'resource' : resource},
        }
        return consumers


if __name__ == '__main__':
    env.main()
