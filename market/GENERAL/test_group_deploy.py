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
    def test_deploy(self):
        self.access.create_publisher(name='group_miner')
        self.access.create_resource(publisher_name='group_miner', name='group_gold')
        self.access.create_resource(publisher_name='group_miner', name='group_silver')
        self.access.create_resource(publisher_name='group_miner', name='group_bronze')
        self.access.create_consumer('group_gold_seeker')
        self.access.create_consumer('group_silver_seeker')
        self.access.create_consumer('group_bronze_seeker')
        self.access.create_consumer_group('seekers', {
            'group_gold' : 'group_gold_seeker',
            'group_silver' : 'group_silver_seeker',
            'group_bronze' : 'group_bronze_seeker'})

        self.access.create_version(resource_name='group_gold')
        self.access.create_version(resource_name='group_silver', dependencies=[('group_gold', '1.0.0')])
        self.access.create_version(resource_name='group_bronze', dependencies=[('group_silver', '1.0.0')])

        consumers = {
            'group_gold_seeker' : {'nodes' : ['0', '1', '2'], 'resource' : 'group_gold'},
            'group_silver_seeker' : {'nodes' : ['3', '4', '5'], 'resource' : 'group_silver'},
            'group_bronze_seeker' : {'nodes' : ['6', '7', '8'], 'resource' : 'group_bronze'}
        }
        for consumer in consumers:
            nodes = consumers[consumer]['nodes']
            for node in nodes:
                self.access.update_consumer_node(node, consumer)
                response = self.access.get_consumer_node(node, consumer)
                self.assertFragmentIn(response, {'state': {'resource': {}}})

        # Signal that nodes are watching for updates
        for consumer in consumers:
            nodes = consumers[consumer]['nodes']
            resource = consumers[consumer]['resource']
            for node in nodes:
                self.access.update_consumer_node(
                    node_id=node,
                    consumer_name=consumer,
                    state={'resource': {resource: Cr(wait={'for_watch': True})}}
                )

        # Wait nodes deploy first version
        for consumer in ['group_gold_seeker', 'group_silver_seeker', 'group_bronze_seeker']:
            self._wait_consumer_target(consumer, consumers, prev_version=None, next_version='1.0.0')
            self._set_consumer_state(consumer, consumers, '1.0.0')

        self._poll_group_deployments_finished('seekers', 1)

        # Create new group deployment versions
        self.access.create_version(resource_name='group_gold')
        self.access.create_version(resource_name='group_silver', dependencies=[('group_gold', '2.0.0')])
        self.access.create_version(resource_name='group_bronze', dependencies=[('group_silver', '2.0.0')])

        # test that scheduler orders to install & download desired resource for group_gold_seeker nodes
        self._wait_consumer_target('group_gold_seeker', consumers, prev_version='1.0.0', next_version='2.0.0')

        # test that other consumers does not recieved target
        for consumer in ['group_silver_seeker', 'group_bronze_seeker']:
            self._check_consumer_does_not_have_target(consumer, consumers, prev_version='1.0.0', next_version='2.0.0')

        # test that scheduler moves to next consumer
        self._set_consumer_state('group_gold_seeker', consumers, '2.0.0')

        # Meanwhile lets create new juicy versions
        self.access.create_version(resource_name='group_gold')
        self.access.create_version(resource_name='group_silver', dependencies=[('group_gold', '3.0.0')])
        self.access.create_version(resource_name='group_bronze', dependencies=[('group_silver', '3.0.0')])

        # test that scheduler orders to install & download desired resource for group_silver_seeker nodes
        self._wait_consumer_target('group_silver_seeker', consumers, prev_version='1.0.0', next_version='2.0.0')

        # but it is not the time for bronze
        self._check_consumer_does_not_have_target('group_bronze_seeker', consumers, prev_version='1.0.0', next_version='2.0.0')

        # and nobody rush for newest version
        self._check_consumer_does_not_have_target('group_gold_seeker', consumers, prev_version='2.0.0', next_version='3.0.0')

        # lets finish with this group deployment
        self._set_consumer_state('group_silver_seeker', consumers, '2.0.0')
        self._wait_consumer_target('group_bronze_seeker', consumers, prev_version='1.0.0', next_version='2.0.0')
        self._set_consumer_state('group_bronze_seeker', consumers, '2.0.0')

        # and expect that new deployment has come
        self._wait_consumer_target('group_gold_seeker', consumers, prev_version='2.0.0', next_version='3.0.0')

    def _make_target(self, resource, version):
        if version is None:
            return EmptyDict()
        return {resource : {'to_install': {version: {}}, 'to_download': {version: {}}}}

    def _wait_consumer_target(self, consumer_name, consumers, prev_version, next_version):
        resource = consumers[consumer_name]['resource']
        for node in consumers[consumer_name]['nodes']:
            self._poll_node_target(
                consumer_name=consumer_name,
                node_id=node,
                prev_target=self._make_target(resource, prev_version),
                next_target=self._make_target(resource, next_version)
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

if __name__ == '__main__':
    env.main()
